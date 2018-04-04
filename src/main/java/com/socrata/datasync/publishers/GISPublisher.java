package com.socrata.datasync.publishers;

import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.imports2.*;
import com.socrata.datasync.job.GISJob;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.HttpUtility;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GISPublisher {

    private static final Logger logging = Logger.getLogger(GISJob.class.getName());
    private static String ticket = "";

    public static JobStatus replaceGeo(File file,
                                       SocrataConnectionInfo connectionInfo,
                                       String datasetID,
                                       Map<String, String> layerMap,
                                       UserPreferences userPrefs) {
        try {
            URI scan_url = makeUri(connectionInfo.getUrl(), "scan", "");
            Blueprint blueprint = postRawFile(scan_url, file, userPrefs);
            return replaceGeoFile(blueprint, file, userPrefs, connectionInfo, datasetID, layerMap);
        } catch (IOException e) {
            String message = e.getMessage();
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage(message);
            return status;
        }
    }

    private static JobStatus replaceGeoFile(Blueprint blueprint,
                                            File file,
                                            UserPreferences userPrefs,
                                            SocrataConnectionInfo connectionInfo,
                                            String datasetID,
                                            Map<String, String> layerMap) {
        try {
            if (blueprint.getError() != null) {
                String message = blueprint.getError().getMessage();
                System.out.println(message);
                JobStatus status = JobStatus.PUBLISH_ERROR;
                status.setMessage(message);
                return status;
            }

            ObjectMapper mapper = new ObjectMapper();

            applyLayerMapToBlueprintSummary(blueprint.getSummary(), layerMap);
            String blueprintSummary = mapper.writeValueAsString(blueprint.getSummary());

            String query = "&fileId=" +  URLEncoder.encode(blueprint.getFileId(),"UTF-8");
            query = query + "&name=" + URLEncoder.encode(file.getName(),"UTF-8");
            query = query + "&blueprint=" + URLEncoder.encode(blueprintSummary,"UTF-8");
            query = query + "&viewUid=" + URLEncoder.encode(datasetID,"UTF-8");

            URI uri = makeUri(connectionInfo.getUrl(),"replace",query);
            logging.log(Level.FINE, uri.toString());
            return postReplaceGeoFile(uri, connectionInfo, userPrefs);
        } catch (IOException e) {
            String message = e.getMessage();
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage(message);
            return status;
        }
    }

    private static void applyLayerMapToBlueprintSummary(Summary summary, Map<String, String> layerMap) {
        for (Layer layer : summary.getLayers()) {
            if (layerMap.containsKey(layer.getName())) {
                layer.setReplacingUid(layerMap.get(layer.getName()));
            }
        }
    }

    private static JobStatus postReplaceGeoFile(URI uri,
                                                SocrataConnectionInfo connectionInfo,
                                                UserPreferences userPrefs) {
        HttpUtility httpUtility = new HttpUtility(userPrefs, true, 3, 2);

        try {
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
                return pollForStatus(ticket, userPrefs, connectionInfo, false);
            } catch (InterruptedException e) {
                // This should be very rare, but we should throw if it happens.
                throw new RuntimeException(e);
            }
        } catch (IOException | ParseException e) {
            String message = e.getMessage();
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage(message);
            return status;
        }
    }

    private static JobStatus pollForStatus(String ticket, UserPreferences userPrefs, SocrataConnectionInfo connectionInfo, boolean complete) throws InterruptedException {
        if (!complete) {
            URI status_url = makeUri(connectionInfo.getUrl(), "status", ticket);
            String[] status = getStatus(status_url, userPrefs);
            System.out.println("Polling the job status: " + status[1]);
            Thread.sleep(1000);

            if (status[0].equals("Complete")) {
                return JobStatus.SUCCESS;
            }

            if (status[0].equals("Error")) {
                JobStatus s = JobStatus.PUBLISH_ERROR;
                s.setMessage(status[1]);
                return s;
            }

            pollForStatus(ticket, userPrefs, connectionInfo, false);
        }

        return JobStatus.SUCCESS;
    }

    private static String[] getStatus(URI uri,
                                      UserPreferences userPrefs) {
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

    private static Blueprint postRawFile(URI uri,
                                         File file,
                                         UserPreferences userPrefs) throws IOException {
        HttpUtility httpUtility = new HttpUtility(userPrefs, true, 3, 2);

        System.out.println("Posting file...");
        HttpEntity httpEntity = MultipartEntityBuilder.create()
            .addBinaryBody(file.getName(), file, ContentType.APPLICATION_OCTET_STREAM,file.getName())
            .build();

        HttpResponse response = httpUtility.post(uri, httpEntity);
        HttpEntity resEntity = response.getEntity();
        String result = EntityUtils.toString(resEntity);
        logging.log(Level.FINE, result);

        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(result, Blueprint.class);
    }

    private static URI makeUri(String domain, String method, String query) {
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
