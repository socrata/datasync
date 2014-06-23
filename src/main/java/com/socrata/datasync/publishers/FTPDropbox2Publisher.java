package com.socrata.datasync.publishers;

import com.socrata.datasync.HttpUtility;
import com.socrata.datasync.VersionProvider;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.Utils;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

/**
 * @author Adrian Laurenzi
 *
 * A utility class for operations that make use of FTP
 */
public class FTPDropbox2Publisher {
    private static final String VERSION_API_ENDPOINT = "/api/version.json";
    private static final String FTP_HOST_SUFFIX = ".ftp.socrata.net";
    private static final String X_SOCRATA_REGION = "X-Socrata-Region";
    private static final int FTP_HOST_PORT = 22222;
    private static final String FTP_CONTROL_FILENAME = "control.json";
    private static final String FTP_ENQUEUE_JOB_DIRNAME = "move-files-here-to-enqueue-job";
    private static final String SUCCESS_PREFIX = "SUCCESS";
    private static final String FAILURE_PREFIX = "FAILURE";
    private static final String FTP_STATUS_FILENAME = "status.txt";
    private static final String FTP_REQUEST_ID_FILENAME = "requestId";
    private static final String FTP_DATASYNC_VERSION_FILENAME = "datasync-version";
    private static final int NUM_BYTES_OUT_BUFFER = 1024;
    private static final int TIME_BETWEEN_FTP_STATUS_POLLS_MS = 1000;

    private FTPDropbox2Publisher() {
        throw new AssertionError("Never instantiate utility classes!");
    }

    /**
     * Publishes the given CSV/TSV file to the dataset with given datasetId
     * using FTP Dropbox v2.0
     *
     * @param userPrefs object containing the user preferences
     * @param datasetId id of the Socrata dataset to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param controlFile Control.json file to configure FTP dropbox v2
     * @return JobStatus containing success or error information
     */
    public static JobStatus publishViaFTPDropboxV2(final UserPreferences userPrefs, final String datasetId,
                                                   final File csvOrTsvFile, final File controlFile) {
        try {
            InputStream inputControlFile = new FileInputStream(controlFile);
            return publishViaFTPDropboxV2(userPrefs, datasetId, csvOrTsvFile, inputControlFile);
        } catch (Exception e) {
            e.printStackTrace();
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Error uploading control file: " + e.getMessage());
            return status;
        }
    }

    /**
     * Publishes the given CSV/TSV file to the dataset with given datasetId
     * using FTP Dropbox v2.0
     *
     * @param userPrefs object containing the user preferences
     * @param datasetId id of the Socrata dataset to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param controlFileContent content of Control file to configure FTP dropbox v2
     * @return JobStatus containing success or error information
     */
    public static JobStatus publishViaFTPDropboxV2(final UserPreferences userPrefs, final String datasetId,
                                                   final File csvOrTsvFile, final String controlFileContent) {
        try {
            InputStream inputControlFile = new ByteArrayInputStream(controlFileContent.getBytes("UTF-8"));
            return publishViaFTPDropboxV2(userPrefs, datasetId, csvOrTsvFile, inputControlFile);
        } catch (Exception e) {
            e.printStackTrace();
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Error uploading control file content: " + e.getMessage());
            return status;
        }
    }

    /**
     * Publishes the given CSV/TSV file to the dataset with given datasetId
     * using FTP Dropbox v2.0
     *
     * @param userPrefs object containing the user preferences
     * @param datasetId id of the Socrata dataset to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param inputControlFile  stream of control.json file content
     * @return JobStatus containing success or error information
     */
    private static JobStatus publishViaFTPDropboxV2(final UserPreferences userPrefs, final String datasetId,
                                                    final File csvOrTsvFile, final InputStream inputControlFile) {
        JobStatus status = JobStatus.PUBLISH_ERROR;

        String ftpHost;
        try {
            ftpHost = getFTPHost(userPrefs);
        } catch (Exception e) {
            e.printStackTrace();
            status.setMessage("Error obtaining FTP host: " + e.getMessage());
            return status;
        }

        FTPSClient ftp = null;
        try {
            ftp = new FTPSClient(false, SSLContext.getDefault());

            System.out.println("Connecting to " + ftpHost + ":" + FTP_HOST_PORT);
            // ADDED connection retry logic
            int tryCount = 0;
            int maxTries = 5;
            boolean connectionSuccessful = false;
            do {
                try {
                    ftp.connect(ftpHost, FTP_HOST_PORT);
                    connectionSuccessful = true;
                } catch (Exception connectException) {
                    // wait 2 secs, then retry connection
                    try {
                        Thread.sleep((long) (Math.pow(2, (tryCount + 1)) * 1000));
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                }
                if (++tryCount > maxTries) {
                    status.setMessage("FTP server refused connection (connection timeout).");
                    return status;
                }
            } while(!connectionSuccessful);
            // END connection retry logic

            SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
            ftp.login(connectionInfo.getUser(), connectionInfo.getPassword());

            // verify connection was successful
            if(FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                System.out.println("ftp.setFileType(FTP.BINARY_FILE_TYPE)");
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                System.out.println("ftp.enterLocalPassiveMode()");
                ftp.enterLocalPassiveMode();

                // Set protection buffer size (what does this do??)
                //ftp.execPBSZ(0);
                // Set data channel protection to private
                System.out.println("ftp.execPROT(\"P\")");
                ftp.execPROT("P");

                String pathToDomainRoot = getPathToDomainRoot(ftp, connectionInfo);
                String pathToDatasetDir = pathToDomainRoot + "/" + datasetId;

                // if datasetId does not exist then create the directory
                System.out.println("ftp.listFiles(" + pathToDatasetDir + "/" + FTP_STATUS_FILENAME + ")");
                FTPFile[] checkDatasetDirExists = ftp.listFiles(pathToDatasetDir + "/" + FTP_STATUS_FILENAME);
                if(checkDatasetDirExists.length == 0) {
                    System.out.println("ftp.makeDirectory(" + pathToDatasetDir + ")");
                    boolean datasetDirCreated = ftp.makeDirectory(pathToDatasetDir);
                    if(!datasetDirCreated) {
                        closeFTPConnection(ftp);
                        status.setMessage("Error creating dataset ID directory at" +
                                " '" + pathToDatasetDir + "': " + ftp.getReplyString());
                        return status;
                    }
                }

                // set request Id for control file upload
                String controlFileRequestId = setFTPRequestId(ftp, pathToDomainRoot + "/" + FTP_REQUEST_ID_FILENAME);
                if(controlFileRequestId.startsWith(FAILURE_PREFIX)) {
                    closeFTPConnection(ftp);
                    status.setMessage("Error setting request Id: " + controlFileRequestId);
                    return status;
                }
                // for tracking DataSync version
                recordDataSyncVersion(ftp, pathToDomainRoot + "/" + FTP_DATASYNC_VERSION_FILENAME);

                // upload control.json file content
                String controlFilePathFTP = pathToDatasetDir + "/" + FTP_CONTROL_FILENAME;
                String controlResponse = uploadAndEnqueue(ftp, inputControlFile, controlFilePathFTP, 0);
                inputControlFile.close();
                if(!controlResponse.equals(SUCCESS_PREFIX)) {
                    closeFTPConnection(ftp);
                    status.setMessage("Error uploading control file: " + controlResponse);
                    return status;
                }
                // ensure control.json was uploaded without issues
                String controlFileUploadStatus = pollUploadStatus(
                        ftp, pathToDatasetDir + "/" + FTP_STATUS_FILENAME, controlFileRequestId);
                if(!controlFileUploadStatus.startsWith(SUCCESS_PREFIX)) {
                    closeFTPConnection(ftp);
                    status.setMessage("Error uploading control file: " + controlFileUploadStatus);
                    return status;
                }

                System.out.println("Publishing entire file via FTPS...");
                // set request Id for this job
                String csvOrTsvFileRequestId = setFTPRequestId(ftp, pathToDomainRoot + "/" + FTP_REQUEST_ID_FILENAME);
                if(csvOrTsvFileRequestId.startsWith(FAILURE_PREFIX)) {
                    closeFTPConnection(ftp);
                    status.setMessage("Error setting request Id: " + csvOrTsvFileRequestId);
                    return status;
                }

                // attempt to gzip CSV/TSV file before uploading
                boolean deleteFileToUpload = false;
                File fileToUpload;
                String dataFilePathFTP;
                try {
                    System.out.println("Gzipping file before uploading...");
                    fileToUpload = createTempGzippedFile(csvOrTsvFile);
                    dataFilePathFTP = pathToDatasetDir + "/" + csvOrTsvFile.getName() + ".gz";
                    deleteFileToUpload = true;
                } catch (IOException ex) {
                    // if gzipping fails revert to sending raw CSV
                    System.out.println("Gzipping failed, uploading CSV directly");
                    fileToUpload = csvOrTsvFile;
                    dataFilePathFTP = pathToDatasetDir + "/" + csvOrTsvFile.getName();
                }

                // upload CSV/TSV file
                long dataFileSizeBytes = fileToUpload.length();
                InputStream inputDataFile = new FileInputStream(fileToUpload);
                String dataFileResponse = uploadAndEnqueue(ftp, inputDataFile, dataFilePathFTP, dataFileSizeBytes);
                inputDataFile.close();
                if(deleteFileToUpload)
                    fileToUpload.delete();
                if(!dataFileResponse.equals(SUCCESS_PREFIX)) {
                    closeFTPConnection(ftp);
                    status.setMessage(dataFileResponse);
                    return status;
                }

                // Poll upload status until complete
                String dataFileUploadStatus = pollUploadStatus(
                        ftp, pathToDatasetDir + "/" + FTP_STATUS_FILENAME, csvOrTsvFileRequestId);
                if(!dataFileUploadStatus.startsWith(SUCCESS_PREFIX)) {
                    status.setMessage(dataFileUploadStatus);
                    return status;
                }
            } else {
                status.setMessage("FTP server refused connection (check your username and password).");
                return status;
            }
        } catch(IOException e) {
            e.printStackTrace();
            status.setMessage("FTP error: " + e.getMessage());
            return status;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            status.setMessage("Java error: " + e.getMessage());
            return status;
        } finally {
            if(ftp != null)
                closeFTPConnection(ftp);
        }
        return JobStatus.SUCCESS;
    }

    /**
     *
     * @param fileToZip file to be compressed
     * @return gzipped version of fileToZip
     * @throws java.io.IOException
     */
    private static File createTempGzippedFile(File fileToZip) throws IOException {
        File tempGzippedFile = File.createTempFile("DataSyncTemp_", "_" + fileToZip.getName() + ".gz");
        try {
            byte[] buffer = new byte[NUM_BYTES_OUT_BUFFER];
            FileOutputStream fileOutputStream = new FileOutputStream(tempGzippedFile);
            GZIPOutputStream gzipOuputStream = new GZIPOutputStream(fileOutputStream);
            FileInputStream fileInput = new FileInputStream(fileToZip);
            int bytes_read;
            while ((bytes_read = fileInput.read(buffer)) > 0) {
                gzipOuputStream.write(buffer, 0, bytes_read);
            }
            fileInput.close();
            gzipOuputStream.finish();
            gzipOuputStream.close();
            return tempGzippedFile;
        } catch (IOException e) {
            tempGzippedFile.delete();
            throw new IOException(e);
        }
    }

    public static String getFTPHost(UserPreferences userPerfs) throws URISyntaxException, IOException {
        HttpUtility http = new HttpUtility(userPerfs, true);
        URI versionApiUri = new URI(userPerfs.getDomain() + VERSION_API_ENDPOINT);
        try(CloseableHttpResponse response = http.get(versionApiUri, ContentType.APPLICATION_JSON.getMimeType())) {
            String regionName = response.getHeaders(X_SOCRATA_REGION)[0].getValue();
            return regionName + FTP_HOST_SUFFIX;
        }
    }


    /**
     * Determines path on FTP server to domain root
     *
     * @param ftp
     * @param connectionInfo
     * @return "" if user user, or "/<DOMAIN>/" if user is SuperAdmin or has multi-domain access
     * @throws java.io.IOException
     */
    private static String getPathToDomainRoot(FTPSClient ftp, SocrataConnectionInfo connectionInfo) throws IOException {
        String pathToDomainRoot = "";
        System.out.println("Obtaining login role - ftp.listFiles(" + FTP_REQUEST_ID_FILENAME + ")");
        FTPFile[] checkRequestIdFile = ftp.listFiles(FTP_REQUEST_ID_FILENAME);
        if(checkRequestIdFile.length == 0) { // user is a SuperAdmin or has multi-domain access
            String domainWithoutHTTP = connectionInfo.getUrl().replaceAll("https://", "");
            domainWithoutHTTP = domainWithoutHTTP.replaceAll("/", "");
            pathToDomainRoot = "/" + domainWithoutHTTP;
        }
        return pathToDomainRoot;
    }

    /**
     * Polls upload status.txt file until ERROR or SUCCESS message (ensuring
     * status.txt contains given requestId)
     *
     * @param ftp authenticated ftps object
     * @param pathToStatusFile absolute path on FTP server to the status.txt file
     * @param requestId requestId that must be present for status.txt content to be valid
     * @return status message (begins with 'SUCCESS: ...' or 'FAILURE: ...')
     * @throws java.io.IOException
     */
    private static String pollUploadStatus(FTPSClient ftp, String pathToStatusFile, String requestId) {
        String uploadStatus = "";
        int numSubsequentFailedPolls = 0;
        int maxSubsequentFailedPolls = 12;
        boolean lastPollFailed = false;
        do {
            try {
                Thread.sleep(TIME_BETWEEN_FTP_STATUS_POLLS_MS);
            } catch (InterruptedException e) { }

            try {
                InputStream in = ftp.retrieveFileStream(pathToStatusFile);
                StringWriter writer = new StringWriter();
                IOUtils.copy(in, writer, "UTF-8");
                uploadStatus = writer.toString();
                in.close();
                ftp.completePendingCommand();

                if(uploadStatus.contains(requestId)) {
                    uploadStatus = uploadStatus.replace(requestId + " : ", "");
                } else {
                    uploadStatus = "";
                }
                lastPollFailed = false;
                System.out.print("\rPolling upload status..." + uploadStatus);
            } catch (IOException e) {
                System.out.print("\rFailed polling upload status...retrying");
                numSubsequentFailedPolls = (lastPollFailed) ? numSubsequentFailedPolls + 1 : 1;
                lastPollFailed = true;
            }
        } while(!uploadStatus.startsWith(SUCCESS_PREFIX) && !uploadStatus.startsWith(FAILURE_PREFIX)
                    && numSubsequentFailedPolls < maxSubsequentFailedPolls);
        return uploadStatus;
    }

    /**
     * Sets (and returns) the FTP requestId to be a random 32 character hexidecimal value
     *
     * @param ftp authenticated ftps object
     * @param pathToRequestIdFile bsolute path on FTP server where requestId file is located
     * @return requestId that was set or if there was an error return error message in the
     *         form 'FAILURE:...'
     * @throws java.io.IOException
     */
    private static String setFTPRequestId(FTPSClient ftp, String pathToRequestIdFile) throws IOException {
        String requestId = Utils.generateRequestId();
        InputStream inputRequestId = new ByteArrayInputStream(requestId.getBytes("UTF-8"));
        System.out.println("Setting job request ID - ftp.storeFile(" + pathToRequestIdFile + ", " + inputRequestId + ")");
        if (!ftp.storeFile(pathToRequestIdFile, inputRequestId)) {
            return FAILURE_PREFIX + ": " + ftp.getReplyString();
        }
        inputRequestId.close();
        return requestId;
    }

    /**
     * Records the DataSync version of this JAR/code (for tracking purposes). If setting the version
     * fails just print a message and do nothing else.
     *
     * @param ftp authenticated ftps object
     * @param pathToDataSyncVersionFile absolute path on FTP server where 'datasync-version' file is located
     */
    private static void recordDataSyncVersion(FTPSClient ftp, String pathToDataSyncVersionFile) {
        try {
            String currentDataSyncVersion = VersionProvider.getThisVersion();
            System.out.println("Recording DataSync version being used (" + currentDataSyncVersion + ")");
            InputStream inputDataSyncVersion = new ByteArrayInputStream(currentDataSyncVersion.getBytes("UTF-8"));
            System.out.println("Setting job request ID - ftp.storeFile(" + pathToDataSyncVersionFile + ", " + inputDataSyncVersion + ")");
            if (!ftp.storeFile(pathToDataSyncVersionFile, inputDataSyncVersion)) {
                System.out.println("Failed to record DataSync version: " + ftp.getReplyString() + " Continuing...");
            }
            inputDataSyncVersion.close();
        } catch (Exception e) {
            System.out.println("Failed to record DataSync version: " + e.getMessage() + ". Continuing...");
        }
    }

    /**
     * Closes the given FTPS connection (if one is open)
     *
     * @param ftp authenticated ftps object
     */
    private static void closeFTPConnection(FTPClient ftp) {
        if(ftp.isConnected()) {
            try {
                ftp.logout();
                ftp.disconnect();
            } catch(IOException ioe) {
                // do nothing
            }
        }
    }

    /**
     *
     * Uploads the given input stream to the working directory of the given FTP object.
     * If transfer was successful (including no partial file transfers), move file to
     * the 'enqueue-job' directory.
     *
     * @param ftp authenticated ftps object
     * @param in input stream with contents to upload as a file
     * @param path absolute path on FTP server where file will be uploaded
     * @param filesize size (in bytes) the uploaded file should be (if filesize == 0, do
     *                 do not check filesize)
     * @return a string in the format of 'SUCCESS', if no errors or 'FAILURE: <error message>',
     * if there was an error during any step of the process
     */
    private static String uploadAndEnqueue(FTPSClient ftp, InputStream in, final String path, long filesize) {
        try {
            if (!ftp.storeFile(path, in)) {
                return FAILURE_PREFIX + ": " + ftp.getReplyString();
            }

            if(filesize != 0) {
                // verify the uploaded filesize == given filesize
                System.out.println("Verifying uploaded filesize of " + path + "...");
                long uploadedFilesize = getFTPFilesize(ftp, path);
                if(filesize != uploadedFilesize) {
                    return String.format(FAILURE_PREFIX + ": uploaded filesize (%d B) " +
                            "did not match local filesize (%d B)", uploadedFilesize, filesize);
                }
            }

            // upload to enqueue directory
            File fileFromPath = new File(path);
            String datasetDirPath = fileFromPath.getParent();
            System.out.println("Enqueing job - ftp.rename("
                    + path + ", " + datasetDirPath + "/" + FTP_ENQUEUE_JOB_DIRNAME + ")");
            issueFtpCommandWithRetries(ftp, "rename", path, datasetDirPath + "/" + FTP_ENQUEUE_JOB_DIRNAME);
        } catch (IOException e) {
            e.printStackTrace();
            return FAILURE_PREFIX + ": " + e.getMessage();
        }
        return SUCCESS_PREFIX;
    }

    /**
     *
     * @param ftp authenticated ftps object
     * @param path absolute path on FTP server where file is located
     * @return filesize of file in bytes
     */
    private static long getFTPFilesize(FTPClient ftp, final String path) throws IOException {
        System.out.println("ftp.sendCommand(\"SIZE\"," + path + ")");
        String replyString = issueFtpCommandWithRetries(ftp, "sendCommand", "SIZE", path);
        String[] replySplit = replyString.trim().split(" ");
        return Long.parseLong(replySplit[1]);
    }

    private static String issueFtpCommandWithRetries(FTPClient ftp, String ftpCommand, String arg1, String arg2) throws IOException {
        int numTries = 0;
        int maxTries = 3;
        boolean commandSucceeded = false;
        int secBetweenRetries = 1;
        do {
            if(numTries > 0) {
                try {
                    Thread.sleep(secBetweenRetries * 1000);
                } catch (InterruptedException e) { }
            }
            numTries += 1;
            try {
                if(ftpCommand.equals("sendCommand")) {
                    ftp.sendCommand(arg1, arg2);
                } else if(ftpCommand.equals("rename")) {
                    ftp.rename(arg1, arg2);
                } else {
                    throw new IllegalArgumentException("'" + ftpCommand + "' is not a valid FTP command");
                }

                if(!FTPReply.isPositiveCompletion(ftp.getReplyCode()))
                    throw new IOException(FAILURE_PREFIX + ": " + String.format(ftp.getReplyString()));
                else
                    commandSucceeded = true;

            } catch (IOException e) {
                if(numTries >= maxTries) {
                    e.printStackTrace();
                    throw new IOException(e);
                } else {
                    System.out.println("FTP command failed (" + e.getMessage() +
                            ")...retrying in " + secBetweenRetries + " secs");
                }
            }

            secBetweenRetries = (int) Math.pow(2, numTries);
        } while(numTries < maxTries && !commandSucceeded);

        return ftp.getReplyString();
    }
}
