package com.socrata.datasync.publishers;

import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.job.GISJob;
import com.socrata.datasync.job.JobStatus;
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
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by malinda.curtis on 11/3/16.
 */
public class GISPublisher {

    private static final Logger logging = Logger.getLogger(GISJob.class.getName());
    private static String ticket = "";

    public static JobStatus replaceGeo(File file, SocrataConnectionInfo connectionInfo, String datasetID) {
        String scan_url = makeUri(connectionInfo.getUrl(), "scan");
        String blueprint = "";
        JobStatus status = JobStatus.SUCCESS;
        try {
            blueprint = postRawFile(scan_url, file, connectionInfo);
            status = replaceGeoFile(blueprint, file, connectionInfo, datasetID);
        } catch (IOException e) {
            String message = e.getMessage();
            JobStatus s = JobStatus.PUBLISH_ERROR;
            s.setMessage(message);
            return s;
        }
        return status;
    }

    public static JobStatus replaceGeoFile(String blueprint, File file, SocrataConnectionInfo connectionInfo, String datasetID) {
        JobStatus status = JobStatus.SUCCESS;
        JSONParser parser = new JSONParser();
        try {
            Object obj = parser.parse(blueprint);
            JSONObject array = (JSONObject)obj;
            if (array.containsKey("error")) {
                boolean error = (boolean) array.get("error");
                if(error) {
                    String message = array.get("message").toString();
                    logging.log(Level.INFO,message);
                    JobStatus s = JobStatus.PUBLISH_ERROR;
                    s.setMessage(message);
                    return s;
                }
            }
            String fileId = array.get("fileId").toString();
            String name = file.getName();
            String bluepr = array.get("summary").toString();
            String query = "";

            String url = makeUri(connectionInfo.getUrl(),"replace");
            query = query + "&fileId="+ URLEncoder.encode(fileId,"UTF-8");
            query = query + "&name="+URLEncoder.encode(name,"UTF-8");
            query = query + "&blueprint=" + URLEncoder.encode(bluepr,"UTF-8");
            query = query + "&viewUid=" + URLEncoder.encode(datasetID,"UTF-8");

            url = url + query;
            //logging.log(Level.INFO,url);
            status = postReplaceGeoFile(url, connectionInfo);
        } catch(ParseException | UnsupportedEncodingException e) {
            String message = e.getMessage();
            JobStatus s = JobStatus.PUBLISH_ERROR;
            s.setMessage(message);
            return s;
        }
        return status;
    }

    public static JobStatus postReplaceGeoFile(String url, SocrataConnectionInfo connectionInfo) {
        JobStatus status = JobStatus.SUCCESS;
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            logging.log(Level.INFO, url);
            HttpPost httpPost = new HttpPost(url);
            String to_encode = connectionInfo.getUser() + ":" + connectionInfo.getPassword();
            byte[] bytes = Base64.encodeBase64(to_encode.getBytes());
            String auth = new String(bytes);

            httpPost.setHeader("Authorization","Basic "+auth);
            httpPost.setHeader("X-App-Token", connectionInfo.getToken());

            HttpResponse response = httpClient.execute(httpPost);


            HttpEntity resEntity = response.getEntity();
            String result = EntityUtils.toString(resEntity);
            JSONParser parser = new JSONParser();
            JSONObject resJson = (JSONObject) parser.parse(result);
            logging.log(Level.INFO, result);

            boolean error = (boolean) resJson.get("error");
            if(error) {
                JobStatus s = JobStatus.PUBLISH_ERROR;
                String error_message = (String) resJson.get("message");
                s.setMessage(error_message);
                return s;
            }
            ticket = resJson.get("ticket").toString();

            try {
                logging.log(Level.INFO,"Polling for Status...");
                status = pollForStatus(ticket, connectionInfo,false);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (IOException | ParseException e) {
            String message = e.getMessage();
            JobStatus s = JobStatus.PUBLISH_ERROR;
            s.setMessage(message);
            return s;
        }
        return status;
    }

    public static JobStatus pollForStatus(String ticket, SocrataConnectionInfo connectionInfo, boolean complete) throws InterruptedException {

        String status_url = makeUri(connectionInfo.getUrl(),"status") + ticket;
        String[] status = new String[2];
        if(!complete)
        {
            status = getStatus(status_url,connectionInfo);
            logging.log(Level.INFO,status[1]);
            Thread.sleep(1000);

            if (status[0] == "Complete") {
                return JobStatus.SUCCESS;
            }

            if (status[0] == "Error"){
                JobStatus s = JobStatus.PUBLISH_ERROR;
                s.setMessage(status[1]);
                return s;
            }
            pollForStatus(ticket,connectionInfo,false);
        }
        return JobStatus.SUCCESS;
    }

    public static String[] getStatus(String url, SocrataConnectionInfo connectionInfo) {
        String[] status = new String[2];
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpGet httpGet = new HttpGet(url);
            String to_encode = connectionInfo.getUser() + ":" + connectionInfo.getPassword();
            byte[] bytes = Base64.encodeBase64(to_encode.getBytes());
            String auth = new String(bytes);

            httpGet.setHeader("Authorization","Basic "+auth);
            httpGet.setHeader("X-App-Token", connectionInfo.getToken());

            HttpResponse response = httpClient.execute(httpGet);


            HttpEntity resEntity = response.getEntity();

            String result = EntityUtils.toString(resEntity);
            JSONParser parser = new JSONParser();
            JSONObject resJson = (JSONObject) parser.parse(result);
            try {
                boolean error = (boolean) resJson.get("error");
                JSONObject details = (JSONObject) resJson.get("details");
                if(error){
                    status[0] = "Error";
                    status[1] = resJson.get("message").toString()+" with code: "+resJson.get("code").toString();
                    return status;
                }
                else {
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
        } catch (IOException  e) {
            e.printStackTrace();
        } catch (ParseException e) {
            status[0] = "Complete";
            status[1] = "Complete";
            return status;
        }
        return status;

    }

    public static String postRawFile(String uri, File file, SocrataConnectionInfo connectionInfo) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(uri);
        String to_encode = connectionInfo.getUser() + ":" + connectionInfo.getPassword();
        byte[] bytes = Base64.encodeBase64(to_encode.getBytes());
        String auth = new String(bytes);

        httpPost.setHeader("Authorization","Basic "+auth);
        httpPost.setHeader("X-App-Token", connectionInfo.getToken());
        logging.log(Level.INFO, "Posting file...");
        HttpEntity httpEntity = MultipartEntityBuilder.create()
                .addBinaryBody(file.getName(), file, ContentType.APPLICATION_OCTET_STREAM,file.getName())
                .build();

        httpPost.setEntity(httpEntity);

        HttpResponse response = httpClient.execute(httpPost);
        HttpEntity resEntity = response.getEntity();
        String result = EntityUtils.toString(resEntity);
        logging.log(Level.INFO,result);
        return result;
    }

    public static String makeUri(String domain,String method) {
        switch(method) {
            case "scan":
                return domain + "/api/imports2?method=scanShape";
            case "replace":
                return domain + "/api/imports2?method=replaceShapefile";
            case "status":
                return domain + "/api/imports2?ticket=";
            default:
                return "Method Required";
        }
    }
}
