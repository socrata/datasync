package com.socrata.datasync.publishers;

import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.job.GISJob;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.HttpUtility;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GISPublisher {

    private static final Logger logging = Logger.getLogger(GISJob.class.getName());
    private static String ticket = "";

    public static JobStatus replaceGeo(File file,
                                       SocrataConnectionInfo connectionInfo,
                                       String datasetID,
                                       UserPreferences userPrefs) {
        URI scan_url = makeUri(connectionInfo.getUrl(), "scan","");
        String blueprint = "";
        JobStatus status = JobStatus.SUCCESS;
        try {
            blueprint = postRawFile(scan_url, file, userPrefs, connectionInfo);
            status = replaceGeoFile(blueprint, file, userPrefs, connectionInfo, datasetID);
        } catch (IOException e) {
            String message = e.getMessage();
            JobStatus s = JobStatus.PUBLISH_ERROR;
            s.setMessage(message);
            return s;
        }

        return status;
    }

    public static JobStatus replaceGeoFile(String blueprint,
                                           File file,
                                           UserPreferences userPrefs,
                                           SocrataConnectionInfo connectionInfo,
                                           String datasetID) {
        JobStatus status = JobStatus.SUCCESS;
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(blueprint);
            JSONObject array = (JSONObject)obj;

            if (array.containsKey("error")) {
                boolean error = (boolean) array.get("error");

                if (error) {
                    String message = array.get("message").toString();
                    logging.log(Level.INFO, message);
                    JobStatus s = JobStatus.PUBLISH_ERROR;
                    s.setMessage(message);
                    return s;
                }
            }

            String fileId = array.get("fileId").toString();
            String name = file.getName();
            String bluepr = array.get("summary").toString();
            String query = "";

            query = query + "&fileId=" +  URLEncoder.encode(fileId,"UTF-8");
            query = query + "&name=" + URLEncoder.encode(name,"UTF-8");
            query = query + "&blueprint=" + URLEncoder.encode(bluepr,"UTF-8");
            query = query + "&viewUid=" + URLEncoder.encode(datasetID,"UTF-8");

            URI uri = makeUri(connectionInfo.getUrl(),"replace",query);
            logging.log(Level.INFO, uri.toString());
            status = postReplaceGeoFile(uri, connectionInfo, userPrefs);

            return status;
        } catch (ParseException | UnsupportedEncodingException e) {
            String message = e.getMessage();
            JobStatus s = JobStatus.PUBLISH_ERROR;
            s.setMessage(message);
            return s;
        }
    }

    public static JobStatus postReplaceGeoFile(URI uri,
                                               SocrataConnectionInfo connectionInfo,
                                               UserPreferences userPrefs) {
        HttpUtility httpUtility = new HttpUtility(userPrefs, true, 3, 2);
        JobStatus status = JobStatus.SUCCESS;

        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpEntity empty = MultipartEntityBuilder.create().build();
            HttpResponse response = httpUtility.post(uri,empty);


            HttpEntity resEntity = response.getEntity();
            String result = EntityUtils.toString(resEntity);
            JSONParser parser = new JSONParser();
            JSONObject resJson = (JSONObject) parser.parse(result);
            logging.log(Level.FINE, result);

            boolean error = (boolean) resJson.get("error");

            if (error) {
                JobStatus s = JobStatus.PUBLISH_ERROR;
                String error_message = (String) resJson.get("message");
                s.setMessage(error_message);
                return s;
            }

            ticket = resJson.get("ticket").toString();

            try {
                logging.log(Level.INFO, "Polling for Status...");
                status = pollForStatus(ticket, userPrefs, connectionInfo, false);

                return status;
            } catch (InterruptedException e) {
                // This should be very rare, but we should throw if it happens.
                throw new RuntimeException(e);
            }
        } catch (IOException | ParseException e) {
            String message = e.getMessage();
            JobStatus s = JobStatus.PUBLISH_ERROR;
            s.setMessage(message);
            return s;
        }
    }

    public static JobStatus pollForStatus(String ticket, UserPreferences userPrefs, SocrataConnectionInfo connectionInfo, boolean complete) throws InterruptedException {

        URI status_url = makeUri(connectionInfo.getUrl(), "status", ticket);
        if (!complete) {
            String[] status = getStatus(status_url, userPrefs, connectionInfo);
            logging.log(Level.FINE,status[1]);
            Thread.sleep(1000);

            if (status[0] == "Complete") {
                return JobStatus.SUCCESS;
            }

            if (status[0] == "Error"){
                JobStatus s = JobStatus.PUBLISH_ERROR;
                s.setMessage(status[1]);
                return s;
            }

            pollForStatus(ticket, userPrefs, connectionInfo, false);
        }

        return JobStatus.SUCCESS;
    }

    public static String[] getStatus(URI uri,UserPreferences userPrefs, SocrataConnectionInfo connectionInfo) {
        HttpUtility httpUtility = new HttpUtility(userPrefs, true, 3, 2);
        String[] status = new String[2];

        try {
            HttpResponse response = httpUtility.get(uri,"application/json");
            HttpEntity resEntity = response.getEntity();
            String result = EntityUtils.toString(resEntity);
            JSONParser parser = new JSONParser();
            JSONObject resJson = (JSONObject) parser.parse(result);
            try {
                boolean error = (boolean) resJson.get("error");
                JSONObject details = (JSONObject) resJson.get("details");
                if (error){
                    status[0] = "Error";
                    status[1] = resJson.get("message").toString() + " with code: " + resJson.get("code").toString();
                    return status;
                } else {
                    try {
                        status[0] = "Progress";
                        status[1] = details.get("status").toString() + ": " + details.get("progress").toString() + " features completed";
                    } catch (NullPointerException e) {
                        status[0] = "Progress";
                        status[1] = details.get("stage").toString();

                        return status;
                    }
                }
            } catch (NullPointerException e) {
                // For once the ticket is complete
                status[0] = "Complete";
                status[1] = "Complete";

                return status;
            }

            return status;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            status[0] = "Complete";
            status[1] = "Complete";

            return status;
        }
    }

    public static String postRawFile(URI uri,
                                     File file,
                                     UserPreferences userPrefs,
                                     SocrataConnectionInfo connectionInfo) throws IOException {
        HttpUtility httpUtility = new HttpUtility(userPrefs, true, 3, 2);

        logging.log(Level.INFO, "Posting file...");
        HttpEntity httpEntity = MultipartEntityBuilder.create()
            .addBinaryBody(file.getName(), file, ContentType.APPLICATION_OCTET_STREAM,file.getName())
            .build();

        HttpResponse response = httpUtility.post(uri, httpEntity);
        HttpEntity resEntity = response.getEntity();
        String result = EntityUtils.toString(resEntity);
        logging.log(Level.FINE,result);

        return result;
    }

    public static URI makeUri(String domain,String method,String query) {
        switch(method) {
        case "scan":
            return URI.create(domain + "/api/imports2?method=scanShape");
        case "replace":
            return URI.create(domain + "/api/imports2?method=replaceShapefile" + query);
        case "status":
            return URI.create(domain + "/api/imports2?ticket=" + query);
        default:
            return URI.create("");
        }
    }
}
