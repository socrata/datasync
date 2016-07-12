package com.socrata.datasync;

import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.HttpEntity;
import org.apache.http.util.EntityUtils;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.utils.URIBuilder;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatasetUtils {

    private static final String LOCATION_DATATYPE_NAME = "location";

    private static ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    public static Dataset getDatasetInfo(UserPreferences userPrefs, String viewId) throws URISyntaxException, IOException, HttpException {
        String justDomain = getDomainWithoutScheme(userPrefs);
        URI absolutePath = new URIBuilder()
                .setScheme("https")
                .setHost(justDomain)
                .setPath("/api/views/" + viewId)
                .build();

        ResponseHandler<Dataset> handler = new ResponseHandler<Dataset>() {
            @Override
            public Dataset handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                StatusLine statusLine = response.getStatusLine();
                int status = statusLine.getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? mapper.readValue(entity.getContent(), Dataset.class) : null;
                } else {
                    throw new ClientProtocolException(statusLine.toString());
                }
            }
        };

        HttpUtility util = new HttpUtility(userPrefs, true);
        Dataset datasetInfo = util.get(absolutePath, "application/json", handler);
        util.close();
        return datasetInfo;
    }

    public static String getDatasetSample(UserPreferences userPrefs, String viewId, int rowsToSample) throws URISyntaxException, IOException, HttpException {
        String justDomain = getDomainWithoutScheme(userPrefs);
        URI absolutePath = new URIBuilder()
                .setScheme("https")
                .setHost(justDomain)
                .setPath("/resource/" + viewId + ".csv")
                .addParameter("$limit",""+rowsToSample)
                .build();

        ResponseHandler<String> handler = new ResponseHandler<String>() {
            @Override
            public String handleResponse(
                    final HttpResponse response) throws ClientProtocolException, IOException {
                StatusLine statusLine = response.getStatusLine();
                int status = statusLine.getStatusCode();
                if (status >= 200 && status < 300) {
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity) : null;
                } else {
                    throw new ClientProtocolException(statusLine.toString());
                }
            }
        };

        HttpUtility util = new HttpUtility(userPrefs, true);
        String sample = util.get(absolutePath, "application/csv", handler);
        util.close();
        return sample;
    }

    private static String getDomainWithoutScheme(UserPreferences userPrefs){
        String[] schemaAndDomain = userPrefs.getDomain().trim().split("//");
        String justDomain = schemaAndDomain[schemaAndDomain.length -1];
        return justDomain;
    }



    /**
     * Retruns the field name of the row identifier, if there is one, else null
     */
    public static String getRowIdentifierName(Dataset schema) {
        Column rowIdentifierColumn = schema.lookupRowIdentifierColumn();
        if (rowIdentifierColumn == null) {
            return null;
        } else {
            return rowIdentifierColumn.getFieldName();
        }
    }


    /**
     * Returns list of dataset field names in the form: "col1","col2",...
     *
     * @param datasetId
     * @return list of field names or null if there
     */
    public static String getFieldNamesString(UserPreferences userPrefs, String datasetId) throws HttpException, IOException, URISyntaxException {
        Dataset datasetInfo = getDatasetInfo(userPrefs, datasetId);
        return getFieldNamesString(datasetInfo);
    }

    /**
     * Returns list of dataset field names in the form: "col1","col2",...
     *
     * @param datasetInfo
     * @return list of field names or null if there are none
     */
    public static String getFieldNamesString(Dataset datasetInfo) {
        String columnsValue = "";
        List<Column> columns = datasetInfo.getColumns();
        for(int i = 0; i < columns.size(); i++) {
            if(i > 0)
                columnsValue += ",";
            columnsValue += "\"" + columns.get(i).getFieldName() + "\"";
        }
        return columnsValue;
    }

    /**
     * Returns an array of dataset field names.
     *
     * @param datasetInfo
     * @return array of field names or null if there are none
     */
    public static String[] getFieldNamesArray(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        String[] columnsArray = new String[columns.size()];
        for(int i = 0; i < columns.size(); i++) {
            columnsArray[i] = columns.get(i).getFieldName();
        }
        return columnsArray;
    }

    /**
     * Returns the set of dataset field names.
     *
     * @param datasetInfo
     * @return array of field names or null if there are none
     */
    public static Set<String> getFieldNamesSet(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        Set<String> fields = new HashSet<>();
        for(int i = 0; i < columns.size(); i++) {
            fields.add(columns.get(i).getFieldName());
        }
        return fields;
    }

    /**
     * Returns a mapping of dataset field names to their type.
     *
     * @param datasetInfo
     * @return a map of dataset field names to datatypes
     */
    public static Map<String,String> getDatasetTypeMapping(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        Map<String,String> mapping = new HashMap<>();
        for(Column c : columns) {
            mapping.put(c.getFieldName(), c.getDataTypeName());
        }
        return mapping;
    }

    /**
     * @return true if given dataset has one or more Location columns, false otherwise
     */
    public static boolean hasLocationColumn(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        for(int i = 0; i < columns.size(); i++) {
            if(columns.get(i).getDataTypeName().equals(LOCATION_DATATYPE_NAME))
                return true;
        }
        return false;
    }
}
