package com.socrata.datasync.utilities;

import com.socrata.datasync.BlobId;
import com.socrata.datasync.JobId;
import com.socrata.datasync.CommitMessage;
import com.socrata.datasync.DatasyncDirectory;
import com.socrata.datasync.JobStatus;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.ssync.PatchComputer;
import com.socrata.ssync.SignatureComputer;
import com.socrata.ssync.SignatureTable;
import com.socrata.ssync.exceptions.input.InputException;
import com.socrata.ssync.exceptions.signature.SignatureException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;

public class PublishUtility {

    private static String domain;
    private static HttpUtility http;
    private static URIBuilder baseUri;
    private static final String datasyncPath = "/datasync/id";
    private static final String statusPath = "/status";
    private static final String commitPath = "/commit";
    private static final String ssigContentType = "application/x-socrata-ssig";
    private ObjectMapper mapper = new ObjectMapper();

    public PublishUtility(UserPreferences userPrefs) throws Exception {
        http = new HttpUtility(userPrefs);
        domain = userPrefs.getHost();
        baseUri = new URIBuilder()
                .setScheme("https")
                .setHost(domain);
    }

    public void close() throws IOException {
        http.close();
    }

    /**
     * Publishes a complete csv or tsv file using delta-importer-2 over http.
     * @param datasetId the 4x4 of the dataset to be replaced
     * @param csvOrTsvFile the csv or tsv file that is to replace the dataset with the given 4x4
     * @param controlFile the control file used to specialize the resulting dataset
     * @return a job status indicating success or failure
     */
    public JobStatus publishWithDi2OverHttp(String datasetId, File csvOrTsvFile, ControlFile controlFile) throws
            IOException {

        DatasyncDirectory datasyncDir = new DatasyncDirectory(http, domain, datasetId);
        CloseableHttpResponse signature = null;
        InputStream previousSignature = null;
        InputStream patch = null;
        JobStatus jobStatus;

        try {
            // get signature of previous csv/tsv file
            String pathToSignature = datasyncDir.getPathToSignature();
            if(pathToSignature == null) {
                previousSignature = getNullSignature();
            } else {
                URI absolutePath = baseUri.setPath(pathToSignature).build();
                signature = http.get(absolutePath, ssigContentType);
                previousSignature = signature.getEntity().getContent();
            }

            // compute the patch between the csv/tsv file and its previous signature
            patch = getPatch(new FileInputStream(csvOrTsvFile), previousSignature);

            // post the patch file in blobby chunks - ewww
            List<String> blobIds = postPatchBlobs(patch, datasetId);

            // commit the chunks, thereby applying the diff
            CommitMessage commit = new CommitMessage()
                    .filename(csvOrTsvFile.getName() + ".sdiff")
                    .relativeTo(pathToSignature)
                    .chunks(blobIds)
                    .control(controlFile);
            String jobId = commitBlobPostings(commit, datasetId);

            // return status
            jobStatus = getJobStatus(datasetId, jobId);

        } catch (Exception e) {
            jobStatus = JobStatus.PUBLISH_ERROR;
            jobStatus.setMessage(e.getMessage());
        } finally {
            if (previousSignature != null) { previousSignature.close(); }
            if (patch != null) { patch.close(); }
            if (signature != null) { signature.close(); }
        }
        return jobStatus;
    }


    /**
     * Computes the diff of the csv or tsv file with the most recent completed signature if there is
     * one, else with nothing
     * @return an input stream containing the patch
     */
    private InputStream getPatch(InputStream newSignature, InputStream previousSignature) throws
            SignatureException, IOException, InputException, NoSuchAlgorithmException {
        BufferedInputStream newStream = new BufferedInputStream(newSignature);
        BufferedInputStream previousStream = new BufferedInputStream(previousSignature);
        return new PatchComputer.PatchComputerInputStream(newStream, new SignatureTable(previousStream), "MD5", 102400);
    }


    /**
     * Chunks up the signature patch file into ~4MB chunks and posts these to delta-importer-2
     * @param patchStream an inputStream to the patch
     * @param datasetId the 4x4 of the dataset being patched
     * @return the list of blobIds corresponding to each successful post
     */
    private List<String> postPatchBlobs(InputStream patchStream, String datasetId) throws
            IOException, URISyntaxException {
        URI postingPath = baseUri.setPath(datasyncPath + "/" + datasetId).build();
        List<String> blobIds = new LinkedList<String>();
        int maxBytes = 1024*4000;  // we could make a call to /datasync/version for this info too
        byte[] bytes = new byte[maxBytes];

        while (patchStream.read(bytes, 0, bytes.length) != -1) {
            HttpEntity entity = EntityBuilder.create().setBinary(bytes).build();
            CloseableHttpResponse response = http.post(postingPath, entity);
            String blobId = mapper.readValue(response.getEntity().getContent(), BlobId.class).blobId;
            response.close();
            blobIds.add(blobId);
            bytes = new byte[maxBytes];
        }
        return blobIds;
    }

    /**
     * Commits the blobs that were posted.
     * @param msg a message containing the necessary info for applying the patch to the most recent signature file
     * @param datasetId the 4x4 of the dataset to which the blobs belong
     * @return the jobId of the job applying the diff
     */
    private String commitBlobPostings(CommitMessage msg, String datasetId) throws IOException, URISyntaxException {
        URI committingPath = baseUri.setPath(datasyncPath + "/" + datasetId + commitPath).build();
        StringEntity entity = new StringEntity(mapper.writeValueAsString(msg), ContentType.APPLICATION_JSON);
        CloseableHttpResponse response = http.post(committingPath, entity);
        String jobId = mapper.readValue(response.getEntity().getContent(), JobId.class).jobId;
        response.close();
        return jobId;
    }


    /**
     * Requests the job status for the given jobId associated with given datasetId
     * @param datasetId the 4x4 of the dataset which was (to be) replaced
     * @param jobId the jobId returned from a succesful commit post
     * @return either success or a publish error
     */
    private JobStatus getJobStatus(String datasetId, String jobId) throws URISyntaxException, IOException {
        URI statusUri = baseUri.setPath(datasyncPath + "/" + datasetId + statusPath + "/" + jobId).build();
        CloseableHttpResponse response = http.get(statusUri, ContentType.APPLICATION_JSON.getMimeType());
        String status = IOUtils.toString(response.getEntity().getContent());
        JobStatus jobStatus;
        if (status.startsWith("SUCCESS")) {
            jobStatus = JobStatus.SUCCESS;
        } else {
            jobStatus = JobStatus.PUBLISH_ERROR;
        }
        jobStatus.setMessage(status + "(jobId:" + jobId + ")");
        return jobStatus;
    }

    private InputStream getNullSignature() throws IOException, NoSuchAlgorithmException {
        BufferedInputStream nullStream = new BufferedInputStream(new ByteArrayInputStream("" .getBytes()));
        return new SignatureComputer.SignatureFileInputStream("MD5", "MD5", 10240, nullStream);
    }
}
