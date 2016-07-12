package com.socrata.datasync.deltaimporter2;

import com.socrata.datasync.HttpUtility;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DatasyncDirectory {
    private static HttpUtility http;
    private static String datasync = "/datasync/id/";
    private static String completedFolder = "completed/";
    private static String signaturesFolder = "signatures/";
    private static ObjectMapper mapper = new ObjectMapper();
    private static String baseFolder;
    private URIBuilder baseUri;

    /**
     * Creates a new DatasyncDirectory object for a given dataset
     * @param http an HttpUtility instance with credentials set up
     * @param domain the host domain in which the dataset can be found, i.e. "opendata.socrata.com"
     * @param datasetId the 4x4 id of the dataset
     */
    public DatasyncDirectory(HttpUtility http, String domain, String datasetId) {
        this.http = http;
        this.baseFolder = datasync + datasetId + "/";
        this.baseUri = new URIBuilder()
                .setScheme("https")
                .setHost(domain);
    }

    /**
     * Returns the complete path if one exists to the most recent signature file, otherwise null.
     * e.g. /datasync/id/some-4by4/completed/2014/6/3/signatures/02:32:42.567-filename.ssig
     */
    public String getPathToSignature() throws ParseException, IOException, URISyntaxException {
        List<String> years = lsDirectory(completedFolder);
        if (years.size() > 0) {
            StringBuilder path = new StringBuilder(completedFolder);
            String year = findMostRecentContent(years, new SimpleDateFormat("yyyy"));
            path.append(year);
            String month = findMostRecentContent(lsDirectory(path.toString()), new SimpleDateFormat("MM"));
            path.append(month);
            String day = findMostRecentContent(lsDirectory(path.toString()), new SimpleDateFormat("dd"));
            path.append(day).append(signaturesFolder);
            String file = findMostRecentContent(lsDirectory(path.toString()), new SimpleDateFormat("hh:mm:ss.SSS"));
            path.append(file);
            return baseFolder + path.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns the contents of the given directory within this DatasyncDirectory.
     * @param path the path to the directory of interest, relative to this DatasyncDirectory,
     *             e.g. '/completed' or '/completed/y/m/d/signatures/'
     * @return a list of the contents within the directory
     */
    public List<String> lsDirectory(String path) throws URISyntaxException, IOException {
        URI uri = baseUri.setPath(baseFolder + path).build();
        try(CloseableHttpResponse response = http.get(uri, ContentType.APPLICATION_JSON.getMimeType())) {
            int status = response.getStatusLine().getStatusCode();
            if (status == HttpStatus.SC_OK || status == HttpStatus.SC_NOT_MODIFIED) {
                return mapper.readValue(response.getEntity().getContent(), ArrayList.class);
            } else {
                // it isn't a show-stopper to be unable to read directories
                return new ArrayList<>();
            }
        }
    }

    /**
     * Finds the most recent item in a directory
     * @param directoryContents the return value from lsDirectory
     * @param df the dateformater to apply to the directory contents, e.g. "yyyy" for the year directory
     *           or "hh:mm:ss.SSS" for the lowest directory
     * @return the url-decoded item in the directory corresponding to the most recent time
     */
    public String findMostRecentContent(List<String> directoryContents, final SimpleDateFormat df) throws ParseException, UnsupportedEncodingException {
        if (directoryContents == null || directoryContents.size() == 0) {
            return null;
        }

        int mostRecentIdx = 0;
        Date mostRecent = df.parse(cleanString(directoryContents.get(0)));
        Date date;
        for(int i=1; i < directoryContents.size(); i++){
            date = df.parse(cleanString(directoryContents.get(i)));
            if (date.compareTo(mostRecent) > 0) {
                mostRecent = date;
                mostRecentIdx = i;
            }
        }
        return URLDecoder.decode(directoryContents.get(mostRecentIdx), "UTF-8");
    }

    /**
     * Cleans the given string to work with the expected date formats needed for ordering the directory contents;
     * @param directoryItem a string, presumably one returned from lsDirectory
     * @return the string url-decoded and stripped of the slash and possble filename, leaving only the date items of the string
     */
    private static String cleanString(String directoryItem) throws UnsupportedEncodingException {
        String withFilenameRemoved = directoryItem.split("-")[0];
        String withTrailingSlashRemoved = withFilenameRemoved.split("/")[0];
        return URLDecoder.decode(withTrailingSlashRemoved, "UTF-8");
    }
}
