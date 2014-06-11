package com.socrata.datasync.publishers;

import com.socrata.datasync.BlobId;
import com.socrata.datasync.JobId;
import com.socrata.datasync.CommitMessage;
import com.socrata.datasync.DatasyncDirectory;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.HttpUtility;
import com.socrata.datasync.job.JobStatus;
import com.socrata.ssync.PatchComputer;
import com.socrata.ssync.SignatureComputer;
import com.socrata.ssync.SignatureTable;
import com.socrata.ssync.exceptions.input.InputException;
import com.socrata.ssync.exceptions.signature.SignatureException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.codehaus.jackson.map.ObjectMapper;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

public class DeltaImporter2Publisher {

    private static final String datasyncPath = "/datasync/id";
    private static final String statusPath = "/status";
    private static final String commitPath = "/commit";
    private static final String ssigContentType = "application/x-socrata-ssig";
    private static final String patchExtenstion = ".sdiff";
    private static final String compressionExtenstion = ".xz";
    private static final int httpRetries = 3;


    private static String domain;
    private static HttpUtility http;
    private static URIBuilder baseUri;
    private ObjectMapper mapper = new ObjectMapper();
    private File patchFile = null;
    private long diffSizeBytes = 0L;
    private String pathToSignature = null;
    CloseableHttpResponse signatureResponse = null;

    public DeltaImporter2Publisher(UserPreferences userPrefs) {
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

        System.out.println("Publishing " + csvOrTsvFile.getName() + " via delta-importer-2 over HTTP");
        DatasyncDirectory datasyncDir = new DatasyncDirectory(http, domain, datasetId);
        boolean useCompression = true;
        InputStream previousSignature = null;
        InputStream patch = null;
        JobStatus jobStatus;

        try {
            // get signature of previous csv/tsv file
            pathToSignature = datasyncDir.getPathToSignature();
            previousSignature = getPreviousSignature(pathToSignature);

            // compute the patch between the csv/tsv file and its previous signature
            patch = getPatch(new FileInputStream(csvOrTsvFile), previousSignature, useCompression);

            // post the patch file in blobby chunks - ewww
            List<String> blobIds = postPatchBlobs(patch, datasetId);

            // commit the chunks, thereby applying the diff
            CommitMessage commit = new CommitMessage()
                    .filename(csvOrTsvFile.getName() + patchExtenstion + (useCompression ? compressionExtenstion : ""))
                    .relativeTo(pathToSignature)
                    .chunks(blobIds)
                    .control(controlFile);
            String jobId = commitBlobPostings(commit, datasetId);

            // return status
            jobStatus = getJobStatus(datasetId, jobId);

        } catch (ParseException | NoSuchAlgorithmException | InputException | URISyntaxException |
                SignatureException |InterruptedException | HttpException e) {
            e.printStackTrace();
            jobStatus = JobStatus.PUBLISH_ERROR;
            jobStatus.setMessage(e.getMessage());
        } finally {
            if (previousSignature != null) { previousSignature.close(); }
            if (patch != null) { patch.close(); }
            if (signatureResponse != null) { signatureResponse.close(); }
        }
        return jobStatus;
    }


    /**
     * Returns an input stream to the signature of the previous version of the dataset to be replaced
     * NOTE: this has the side-effect of setting pathToSignature to null if the previous signature cannot be gotten.
     * @param signaturePath the complete path to the signature; i.e. /datasync/id/some-4by4/completed/... or null
     * @return an input stream to the previous signature (possibly the null signature)
     */
    private InputStream getPreviousSignature(String signaturePath) throws
            IOException, URISyntaxException, NoSuchAlgorithmException {
        System.out.println("Acquiring the previous signature or creating a null signature");
        if(signaturePath == null) {
            // if no previously completed signature, return stream of the null signature
            return getNullSignature();
        } else {
            // otherwise, return the completed signature stored by delta-importer-2
            URI absolutePath = baseUri.setPath(signaturePath).build();
            signatureResponse = http.get(absolutePath, ssigContentType);
            int statusCode = signatureResponse.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_NOT_MODIFIED) {
                return signatureResponse.getEntity().getContent();
            } else {
                // if we have a problem getting the signature, we can still use the null signature,
                // but must set the pathToSignature to null for the relativeTo field in the commit
                pathToSignature = null;
                return getNullSignature();
            }
        }
    }


    /**
     * Computes the diff of the csv or tsv file with the most recent completed signature if there is
     * one, else with nothing
     * @param newFile an input stream to the new file that is to replace the old
     * @param previousSignature an input stream to the previous signature
     * @param compress whether to compress the patch using xz compression
     * @return an input stream containing the possibly compressed patch
     */
    private InputStream getPatch(InputStream newFile, InputStream previousSignature, boolean compress) throws
            SignatureException, IOException, InputException, NoSuchAlgorithmException {
        System.out.println("Calculating the diff between the source file and previous signature");
        BufferedInputStream newStream = new BufferedInputStream(newFile);
        BufferedInputStream previousStream = new BufferedInputStream(previousSignature);
        if (!compress) {
            return new PatchComputer.PatchComputerInputStream(newStream, new SignatureTable(previousStream), "MD5", 102400);
        } else {
            patchFile = File.createTempFile("patch", patchExtenstion);
            XZOutputStream patchStream = new XZOutputStream(new FileOutputStream(patchFile), new LZMA2Options());
            PatchComputer.compute(newStream, new SignatureTable(previousStream), "MD5", 102400, patchStream);
            patchStream.close(); // finishes compression and closes underlying stream
            diffSizeBytes = patchFile.length();
            return new FileInputStream(patchFile);
        }
    }


    /**
     * Chunks up the signature patch file into ~4MB chunks and posts these to delta-importer-2
     * @param patchStream an inputStream to the patch
     * @param datasetId the 4x4 of the dataset being patched
     * @return the list of blobIds corresponding to each successful post
     */
    private List<String> postPatchBlobs(InputStream patchStream, String datasetId) throws
            IOException, URISyntaxException, HttpException {
        System.out.println("Chunking and posting the diff");
        if (diffSizeBytes > 0L) System.out.println("\t" + diffSizeBytes + " bytes remain to be sent");

        URI postingPath = baseUri.setPath(datasyncPath + "/" + datasetId).build();
        List<String> blobIds = new LinkedList<>();
        int maxBytes = 1024*4000;  // we could make a call to /datasync/version for this info too
        long bytesRemaining = diffSizeBytes;
        byte[] bytes = new byte[maxBytes];
        StatusLine statusLine;
        int status;

        while (patchStream.read(bytes, 0, bytes.length) != -1) {
            HttpEntity entity = EntityBuilder.create().setBinary(bytes).build();
            int retries = 0;
            do {
                CloseableHttpResponse response = http.post(postingPath, entity);
                statusLine = response.getStatusLine();
                status = statusLine.getStatusCode();
                if (status != HttpStatus.SC_CREATED) {
                    retries += 1;
                } else {
                    String blobId = mapper.readValue(response.getEntity().getContent(), BlobId.class).blobId;
                    blobIds.add(blobId);
                    bytesRemaining = Math.max(bytesRemaining - maxBytes, 0L);
                    if (diffSizeBytes > 0L) System.out.println("\t" + bytesRemaining + " bytes remain to be sent");
                    bytes = new byte[maxBytes];
                }
                response.close();
            } while (status != HttpStatus.SC_CREATED && retries < httpRetries);
            if (retries == 5) throw new HttpException(statusLine.toString());
        }
        return blobIds;
    }

    /**
     * Commits the blobs that were posted.
     * @param msg a message containing the necessary info for applying the patch to the most recent signature file
     * @param datasetId the 4x4 of the dataset to which the blobs belong
     * @return the jobId of the job applying the diff
     */
    private String commitBlobPostings(CommitMessage msg, String datasetId) throws
            IOException, URISyntaxException, HttpException {
        System.out.println("Commiting the chunked diffs to apply the patch");
        URI committingPath = baseUri.setPath(datasyncPath + "/" + datasetId + commitPath).build();
        StringEntity entity = new StringEntity(mapper.writeValueAsString(msg), ContentType.APPLICATION_JSON);
        CloseableHttpResponse response = http.post(committingPath, entity);
        if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            // TODO: note the time the job is sent; note the time the job was received from header (once Robert adds this)
            //       parse logs; from time bounds should be able to determine if job is "committed"; if not can try again.
            response.close();
            throw new HttpException(response.getStatusLine().toString());
        }
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
    private JobStatus getJobStatus(String datasetId, String jobId) throws
            URISyntaxException, IOException, InterruptedException, HttpException {
        JobStatus jobStatus = null;
        String status = null;
        StatusLine statusLine = null;
        URI statusUri = baseUri.setPath(datasyncPath + "/" + datasetId + statusPath + "/" + jobId).build();
        int retries = 0;
        while (jobStatus == null && retries < httpRetries) {
            CloseableHttpResponse response = http.get(statusUri, ContentType.APPLICATION_JSON.getMimeType());
            statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == HttpStatus.SC_OK || statusCode == HttpStatus.SC_NOT_MODIFIED) {
                status = IOUtils.toString(response.getEntity().getContent());
                System.out.println("Polling the job status: " + status);
                if (status.startsWith("SUCCESS")) {
                    jobStatus = JobStatus.SUCCESS;
                } else if (status.startsWith("FAILURE")) {
                    jobStatus = JobStatus.PUBLISH_ERROR;
                } else {
                    Thread.sleep(1000);
                }
            } else {
                retries +=1;
                Thread.sleep(1000);
            }
            response.close();
        }
        if (jobStatus == null) {
            throw new HttpException(statusLine.toString());
        }
        jobStatus.setMessage(status + "(jobId:" + jobId + ")");
        return jobStatus;
    }

    private InputStream getNullSignature() throws IOException, NoSuchAlgorithmException {
        BufferedInputStream nullStream = new BufferedInputStream(new ByteArrayInputStream("" .getBytes()));
        return new SignatureComputer.SignatureFileInputStream("MD5", "MD5", 10240, nullStream);
    }
}
