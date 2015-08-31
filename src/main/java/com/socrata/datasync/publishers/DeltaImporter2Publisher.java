package com.socrata.datasync.publishers;

import com.socrata.datasync.SizeCountingInputStream;
import com.socrata.datasync.Utils;
import com.socrata.datasync.HttpUtility;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.controlfile.FileTypeControl;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.deltaimporter2.*;
import com.socrata.datasync.job.JobStatus;
import com.socrata.ssync.PatchComputer;
import com.socrata.ssync.SignatureComputer;
import com.socrata.ssync.SignatureTable;
import com.socrata.ssync.exceptions.input.InputException;
import com.socrata.ssync.exceptions.signature.SignatureException;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationConfig;
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
import java.text.ParseException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DeltaImporter2Publisher implements AutoCloseable {

    private static final String datasyncBasePath = "/datasync";
    private static final String datasyncPath = datasyncBasePath + "/id";
    private static final String statusPath = "/status";
    private static final String commitPath = "/commit";
    private static final String logPath = "/log";
    private static final String ssigContentType = "application/x-socrata-ssig";
    private static final String patchExtenstion = ".sdiff";
    private static final String compressionExtenstion = ".gz";
    private static final String finishedLogKey = "finished";
    private static final String committingLogKey = "committing-job";
    private static final String committedLogKey = "committed-job";
    private static final int httpRetries = 3;
    private static final int defaultChunkSize = 1024 * 4000;

    private static class CompletelyRestartJob extends Exception {}

    private static String domain;
    private static HttpUtility http;
    private static URIBuilder baseUri;
    private ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    private String pathToSignature = null;
    CloseableHttpResponse signatureResponse = null;

    public DeltaImporter2Publisher(UserPreferences userPrefs, String userAgent) {
        http = new HttpUtility(userPrefs, true, userAgent);
        domain = userPrefs.getHost();
        baseUri = new URIBuilder()
                .setScheme("https")
                .setHost(domain);
    }

    @Override
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
    public JobStatus publishWithDi2OverHttp(String datasetId, final File csvOrTsvFile, ControlFile controlFile) throws
            IOException {

        System.out.println("Publishing " + csvOrTsvFile.getName() + " via delta-importer-2 over HTTP");
        DatasyncDirectory datasyncDir = new DatasyncDirectory(http, domain, datasetId);
        boolean useCompression = true;
        InputStream previousSignature = null;
        SizeCountingInputStream patch = null;
        int chunkSize = fetchDatasyncChunkSize();
        String uuid = controlFile.generateAndAddOpaqueUUID();
        int retryCount = 0;
        String fileExtension = Utils.getFileExtension(csvOrTsvFile.getName());
        FileTypeControl fileControl = fileExtension.equals("csv") ? controlFile.csv : controlFile.tsv;
        boolean fileHasBom = Utils.fileStartsWithBom(csvOrTsvFile, fileControl);
        int bomLength = fileHasBom ? Utils.BOM.getBytes(Utils.getCharset(fileControl)).length : 0;

        do {
            try {
                // get signature of previous csv/tsv file
                pathToSignature = datasyncDir.getPathToSignature();
                previousSignature = getPreviousSignature(pathToSignature);

                final long fileSize = csvOrTsvFile.length() - bomLength;
                final FileInputStream fileStream = new FileInputStream(csvOrTsvFile);
                if (fileHasBom)
                    for (int i=0; i<bomLength; i++)
                        fileStream.read();

                InputStream progressingInputStream = new ProgressingInputStream(fileStream) {
                    @Override
                    protected void progress(long count) {
                        System.out.println("\tRead " + count + " of " + fileSize + " bytes of " + csvOrTsvFile.getName());
                    }
                };
                // compute the patch between the csv/tsv file and its previous signature
                patch = new SizeCountingInputStream(getPatch(progressingInputStream, previousSignature, chunkSize, useCompression));

                // post the patch file in blobby chunks - ewww
                List<String> blobIds = postPatchBlobs(patch, datasetId, chunkSize);

                // commit the chunks, thereby applying the diff
                CommitMessage commit = new CommitMessage()
                        .filename(csvOrTsvFile.getName() + patchExtenstion + (useCompression ? compressionExtenstion : ""))
                        .relativeTo(pathToSignature)
                        .chunks(blobIds)
                        .control(controlFile)
                        .expectedSize(patch.getTotal());
                String jobId = commitBlobPostings(commit, datasetId, uuid);

                // return status
                return getJobStatus(datasetId, jobId);
            } catch (CompletelyRestartJob e) {
                retryCount += 1;
            } catch (ParseException | NoSuchAlgorithmException | InputException | URISyntaxException |
                    SignatureException |InterruptedException | HttpException e) {
                e.printStackTrace();
                JobStatus jobStatus = JobStatus.PUBLISH_ERROR;
                jobStatus.setMessage(e.getMessage());
                return jobStatus;
            } finally {
                if (previousSignature != null) { previousSignature.close(); }
                if (patch != null) { patch.close(); }
                if (signatureResponse != null) { signatureResponse.close(); }
            }
        } while(retryCount < httpRetries);
        JobStatus jobStatus = JobStatus.PUBLISH_ERROR;
        jobStatus.setMessage("Couldn't get the request through; too many retries"); // TODO Better message
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
    private InputStream getPatch(InputStream newFile, InputStream previousSignature, int chunkSize, boolean compress) throws
            SignatureException, IOException, InputException, NoSuchAlgorithmException {
        System.out.println("Calculating the diff between the source file and previous signature");
        BufferedInputStream newStream = new BufferedInputStream(newFile);
        BufferedInputStream previousStream = new BufferedInputStream(previousSignature);
        if (!compress) {
            return new PatchComputer.PatchComputerInputStream(newStream, new SignatureTable(previousStream), "MD5", 102400);
        } else {
            InputStream patchStream = new PatchComputer.PatchComputerInputStream(newStream, new SignatureTable(previousStream), "MD5", 1024000);
            return new GZipCompressInputStream(patchStream, 2 * chunkSize);
            //return new XZCompressInputStream(patchStream, 2 * chunkSize);
        }
    }

    private int fetchDatasyncChunkSize() {
        URI versionServicePath;
        int retryCount = 0;

        try {
            versionServicePath = baseUri.setPath(datasyncBasePath + "/version.json").build();
        } catch (URISyntaxException e) {
            System.out.println("Couldn't construct version.json URI?  Using " + defaultChunkSize + " for the chunk-size");
            return defaultChunkSize;
        }

        while(true) {
            try(CloseableHttpResponse response = http.get(versionServicePath, ContentType.APPLICATION_JSON.getMimeType())) {
                return mapper.readValue(response.getEntity().getContent(), Version.class).maxBlockSize;
            } catch (Exception e) {
                retryCount += 1;
                if(retryCount >= httpRetries) {
                    // The next step will probably fail too if we can't even hit DI's version service,
                    // but might as well soldier on regardless.
                    System.out.println("Unable to detect chunk size; using " + defaultChunkSize);
                    return defaultChunkSize;
                }
            }
        }
    }

    /**
     * Chunks up the signature patch file into ~4MB chunks and posts these to delta-importer-2
     * @param patchStream an inputStream to the patch
     * @param datasetId the 4x4 of the dataset being patched
     * @return the list of blobIds corresponding to each successful post
     */
    private List<String> postPatchBlobs(InputStream patchStream, String datasetId, int chunkSize) throws
            IOException, URISyntaxException, HttpException {
        System.out.println("Chunking and posting the diff");

        URI postingPath = baseUri.setPath(datasyncPath + "/" + datasetId).build();
        List<String> blobIds = new LinkedList<>();
        byte[] bytes = new byte[chunkSize];
        StatusLine statusLine;
        int status;
        int bytesRead;

        while ((bytesRead = Utils.readChunk(patchStream, bytes, 0, bytes.length)) != -1) {
            System.out.println("\tUploading " + bytesRead + " bytes of the diff");
            byte[] chunk = bytesRead == bytes.length ? bytes : Arrays.copyOf(bytes, bytesRead);
            HttpEntity entity = EntityBuilder.create().setBinary(chunk).build();
            int retries = 0;
            do {
                try(CloseableHttpResponse response = http.post(postingPath, entity)) {
                    statusLine = response.getStatusLine();
                    status = statusLine.getStatusCode();
                    if (status != HttpStatus.SC_CREATED) {
                        retries += 1;
                    } else {
                        String blobId = mapper.readValue(response.getEntity().getContent(), BlobId.class).blobId;
                        blobIds.add(blobId);
                    }
                }
            } while (status != HttpStatus.SC_CREATED && retries < httpRetries);
            //We hit the max number of retries without success and should throw an exception accordingly.
            if (retries >= httpRetries) throw new HttpException(statusLine.toString());
            System.out.println("\tUploaded " + bytesRead + " bytes");
        }
        return blobIds;
    }


    /**
     * Commits the blobs that were posted.
     * @param msg a message containing the necessary info for applying the patch to the most recent signature file
     * @param datasetId the 4x4 of the dataset to which the blobs belong
     * @return the jobId of the job applying the diff
     */
    private String commitBlobPostings(final CommitMessage msg, final String datasetId, final String uuid) throws URISyntaxException, IOException, CompletelyRestartJob {
        System.out.println("Commiting the chunked diffs to apply the patch");
        final URI committingPath = baseUri.setPath(datasyncPath + "/" + datasetId + commitPath).build();

        // This is kinda ugly (kinda?) -- since this request isn't idempotent, we're handling retry logic ourselves
        // with checks to make sure that the request didn't actually go through on a failure.
        class PostStateMachine {
            String jobId = null;
            IOException lastPostException = null;
            int retries = 0;

            void go() throws IOException, URISyntaxException, CompletelyRestartJob {
                StringEntity entity = new StringEntity(mapper.writeValueAsString(msg), ContentType.APPLICATION_JSON);
                try (CloseableHttpResponse response = doPost(entity)) {
                    if(response == null) handleIOErrorPath();
                    else handleHttpResponsePath(response);
                }
            }

            CloseableHttpResponse doPost(HttpEntity entity) {
                try {
                    return http.post(committingPath, entity);
                } catch (IOException e) {
                    lastPostException = e;
                    return null;
                }
            }

            void handleIOErrorPath() throws IOException, URISyntaxException {
                checkCommitment();
                if(retries >= httpRetries) throw lastPostException;
            }

            void checkCommitment() throws URISyntaxException {
                Commital commital = getJobCommitment(datasetId, uuid);
                switch(commital.status) {
                    case COMMITTED:
                        jobId = commital.jobId;
                        break;
                    case COMMITTING:
                        retries += httpRetries-1;   // we only retry this case once, since this is typically fatal
                        break;
                    case NOT_COMMITTING:
                    case UNKNOWN:
                        retries++;
                }
            }

            void handleHttpResponsePath(CloseableHttpResponse response) throws IOException, URISyntaxException, CompletelyRestartJob {
                StatusLine statusLine = response.getStatusLine();
                int status = statusLine.getStatusCode();
                if (status != HttpStatus.SC_OK) {
                    DI2Error error = null;
                    if(status == HttpStatus.SC_BAD_REQUEST) {
                        try {
                            error = mapper.readValue(response.getEntity().getContent(), DI2Error.class);
                        } catch (JsonProcessingException e) {
                            // Bad request response which wasn't from DI2, and hence not JSON or not in the
                            // format we require?
                        } catch (IOException e) {
                            lastPostException = e;
                            handleIOErrorPath();
                            return;
                        }
                    }
                    response.close();

                    if(error == null) {
                        // it wasn't a failure due to something we sent (or if it was, it's a cause
                        // we don't know about!), so let's see if the commitment went through anyway.
                        checkCommitment();
                    } else {
                        switch(error.type) {
                            case NON_UNIFORM_CHUNK:
                            case NONEXISTANT_CHUNK:
                            case SIZE_MISMATCH:
                                // these can all be plausibly caused by errors in DataSync's network handling
                                // (non-uniform chunk less so than the others) so we'll retry.
                                // TODO: report this to Socrata!
                                throw new CompletelyRestartJob();
                            case FILENAME_BAD_CHARACTERS:
                            case FILENAME_TOO_LONG:
                                // These are caused by the human and won't be fixed by retrying.  Bad human, no cookie!
                                // But we should give them a human-readable error at least (preferably we should prevent
                                // it from happening in the first place).  That's a TODO
                                //
                                // For now, fall through to the "terminate processing" case.
                            default:
                                // anything else is a bug in datasync.  TODO: report this to Socrata!
                                retries = httpRetries;
                        }
                    }
                    if(retries >= httpRetries) throw new IOException("Unable to commit job. " + statusLine.toString());
                } else {
                    jobId = mapper.readValue(response.getEntity().getContent(), JobId.class).jobId;
                }
            }
        }
        PostStateMachine psm = new PostStateMachine();
        do {
            psm.go();
        } while (psm.jobId == null);
        return psm.jobId;
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
        URI logUri = baseUri.setPath(datasyncPath + "/" + datasetId + logPath + "/" + jobId + ".json").build();
        int retries = 0;
        while (jobStatus == null && retries < httpRetries) {
            try(CloseableHttpResponse response = http.get(statusUri, ContentType.APPLICATION_JSON.getMimeType())) {
                statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                if (statusCode == HttpStatus.SC_OK) {
                    retries = 0; // we got one, so reset the retry count.
                    status = IOUtils.toString(response.getEntity().getContent());
                    System.out.print("Polling the job status: " + status);
                    if (status.startsWith("SUCCESS")) {
                        jobStatus = JobStatus.SUCCESS;
                    } else if (status.startsWith("FAILURE")) {
                        jobStatus = JobStatus.PUBLISH_ERROR;
                    } else {
                        Thread.sleep(1000);
                    }
                } else if (statusCode == HttpStatus.SC_BAD_GATEWAY || statusCode == HttpStatus.SC_SERVICE_UNAVAILABLE) {
                    // No-penalty retry; we're willing to keep doing this forever
                    Thread.sleep(1000);
                } else if (statusCode != HttpStatus.SC_NOT_MODIFIED) {
                    retries += 1;
                    Thread.sleep(1000);
                }
            }
        }
        if (jobStatus == null) {
            throw new HttpException(statusLine.toString());
        }
        jobStatus.setMessage(status + "(jobId:" + jobId + ")");
        if(!jobStatus.isError()) loadStatusWithCRUD(jobStatus, logUri);
        return jobStatus;
    }

    private Commital getJobCommitment(String datasetId, String uuid) throws URISyntaxException {
        URI logUri = baseUri.setPath(datasyncPath + "/" + datasetId + logPath + "/index.json").build();
        try(CloseableHttpResponse response = http.get(logUri, ContentType.APPLICATION_JSON.getMimeType())) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                LogItem[] log = mapper.readValue(response.getEntity().getContent(), LogItem[].class);
                for (int i = 0; i < log.length; i++) {
                    LogItem item = log[i];
                    if (item.type.equalsIgnoreCase(committingLogKey)) {
                        if (uuid.equals(item.getOpaqueUUID())) {
                            String jobId = item.getJobId();
                            Commitment c = isJobCommitted(datasetId, jobId);
                            return new Commital(c, jobId);
                        }
                    }
                }
                return new Commital(Commitment.NOT_COMMITTING, null);
            } else {
                System.err.println("Unable to determine if job was committed from logs");
            }
        } catch (IOException e) {
            System.err.println("Unable to determine if job was committed from logs. " + e.getMessage());
        }
        return new Commital(Commitment.UNKNOWN, null);
    }

    private Commitment isJobCommitted(String datasetId, String jobId) throws URISyntaxException {
        URI logUri = baseUri.setPath(datasyncPath + "/" + datasetId + logPath + "/" + jobId + ".json").build();
        try(CloseableHttpResponse response = http.get(logUri, ContentType.APPLICATION_JSON.getMimeType())) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                LogItem[] log = mapper.readValue(response.getEntity().getContent(), LogItem[].class);
                LogItem committed = getLogItem(log, committedLogKey);
                if (committed != null) {
                    return Commitment.COMMITTED;
                } else {
                    return Commitment.COMMITTING;
                }
            } else {
                System.err.println("Unable to determine if job was committed from logs");
            }
        } catch (IOException e) {
            System.err.println("Unable to determine if job was committed from logs. " + e.getMessage());
        }
        return Commitment.UNKNOWN;
    }


    private void loadStatusWithCRUD(JobStatus status, URI logUri) {
        try(CloseableHttpResponse response = http.get(logUri, ContentType.APPLICATION_JSON.getMimeType())) {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == HttpStatus.SC_OK) {
                // The payload that we get back from DI2 may change over time.  Since we are only looking at the delta
                // section, disable the strict parsing.
                mapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                LogItem[] deltaLog = mapper.readValue(response.getEntity().getContent(), LogItem[].class);
                LogItem deltas = getLogItem(deltaLog, finishedLogKey);
                if (deltas != null) {
                    status.rowsCreated = deltas.getInserted();
                    status.rowsUpdated = deltas.getUpdated();
                    status.rowsDeleted = deltas.getDeleted();
                    status.errors = deltas.getErrors();
                }
            } else {
                System.err.println("Unable to parse out CRUD details from logs");
            }
        } catch (IOException e) {
            System.err.println("Unable to parse out CRUD details from logs");
        }
    }

    private InputStream getNullSignature() throws IOException, NoSuchAlgorithmException {
        BufferedInputStream nullStream = new BufferedInputStream(new ByteArrayInputStream("" .getBytes()));
        return new SignatureComputer.SignatureFileInputStream("MD5", "MD5", 10240, nullStream);
    }

    private LogItem getLogItem(LogItem[] logItems, String key) {
        for (int i = 0; i < logItems.length; i++) {
            LogItem item = logItems[i];
            if (item.type.equalsIgnoreCase(key))
                return item;
        }
        return null;
    }

    private enum Commitment {
        NOT_COMMITTING, COMMITTING, COMMITTED, UNKNOWN;
    }

    private static class Commital {
        public Commitment status;
        public String jobId;

        public Commital(Commitment c, String id) {
            this.status = c;
            this.jobId = id;
        }
    }
}
