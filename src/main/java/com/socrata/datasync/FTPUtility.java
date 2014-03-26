package com.socrata.datasync;

import com.socrata.api.HttpLowLevel;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.*;
import org.apache.log4j.Logger;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPOutputStream;

/**
 * @author Adrian Laurenzi
 *
 * A utility class for operations that make use of FTP
 */
public class FTPUtility {
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
    private static final int NUM_BYTES_OUT_BUFFER = 1024;
    private static final int TIME_BETWEEN_FTP_STATUS_POLLS_MS = 1000;

    private FTPUtility() {
        throw new AssertionError("Never instantiate utility classes!");
    }

    /**
     * Publishes the given CSV/TSV file to the dataset with given datasetId
     * using FTP Dropbox v2.0 (a.k.a. "SmartUpdate").
     *
     * @param userPrefs object containing the user preferences
     * @param ddl Soda 2 ddl object
     * @param datasetId id of the Socrata dataset to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param controlFile Control.json file to configure FTP dropbox v2
     * @return JobStatus containing success or error information
     */
    public static JobStatus publishViaFTPDropboxV2(final UserPreferences userPrefs, final SodaDdl ddl,
                                                   final String datasetId, final File csvOrTsvFile,
                                                   final File controlFile) {
        try {
            InputStream inputControlFile = new FileInputStream(controlFile);
            return publishViaFTPDropboxV2(userPrefs, ddl, datasetId, csvOrTsvFile, inputControlFile);
        } catch (Exception e) {
            e.printStackTrace();
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Error uploading control file: " + e.getMessage());
            return status;
        }


    }

    /**
     * Publishes the given CSV/TSV file to the dataset with given datasetId
     * using FTP Dropbox v2.0 (a.k.a. "SmartUpdate").
     *
     * @param userPrefs object containing the user preferences
     * @param ddl Soda 2 ddl object
     * @param datasetId id of the Socrata dataset to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param controlFileContent content of Control file to configure FTP dropbox v2
     * @return JobStatus containing success or error information
     */
    public static JobStatus publishViaFTPDropboxV2(final UserPreferences userPrefs, final SodaDdl ddl,
                                                   final String datasetId, final File csvOrTsvFile,
                                                   final String controlFileContent) {
        try {
            InputStream inputControlFile = new ByteArrayInputStream(controlFileContent.getBytes("UTF-8"));
            return publishViaFTPDropboxV2(userPrefs, ddl, datasetId, csvOrTsvFile, inputControlFile);
        } catch (Exception e) {
            e.printStackTrace();
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Error uploading control file content: " + e.getMessage());
            return status;
        }
    }

    /**
     * Publishes the given CSV/TSV file to the dataset with given datasetId
     * using FTP Dropbox v2.0 (a.k.a. "SmartUpdate").
     *
     * @param userPrefs object containing the user preferences
     * @param ddl Soda 2 ddl object
     * @param datasetId id of the Socrata dataset to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param inputControlFile  stream of control.json file content
     * @return JobStatus containing success or error information
     */
    private static JobStatus publishViaFTPDropboxV2(final UserPreferences userPrefs, final SodaDdl ddl,
                                                   final String datasetId, final File csvOrTsvFile,
                                                   final InputStream inputControlFile) {
        Logger logger = Logger.getRootLogger();
        JobStatus status = JobStatus.PUBLISH_ERROR;

        String ftpHost;
        try {
            ftpHost = getFTPHost(userPrefs.getDomain(), ddl);
        } catch (Exception e) {
            e.printStackTrace();
            status.setMessage("Error obtaining FTP host: " + e.getMessage());
            return status;
        }

        FTPSClient ftp = null;
        try {
            ftp = new FTPSClient(false, SSLContext.getDefault());

            logger.info("Connecting to " + ftpHost + ":" + FTP_HOST_PORT);
            ftp.connect(ftpHost, FTP_HOST_PORT);
            SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
            ftp.login(connectionInfo.getUser(), connectionInfo.getPassword());

            // verify connection was successful
            if(FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                logger.info("ftp.setFileType(FTP.BINARY_FILE_TYPE)");
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                logger.info("ftp.enterLocalPassiveMode()");
                ftp.enterLocalPassiveMode();

                // Set protection buffer size (what does this do??)
                //ftp.execPBSZ(0);
                // Set data channel protection to private
                logger.info("ftp.execPROT(\"P\")");
                ftp.execPROT("P");

                String pathToDomainRoot = getPathToDomainRoot(ftp, connectionInfo);
                String pathToDatasetDir = pathToDomainRoot + "/" + datasetId;

                // if datasetId does not exist then create the directory
                logger.info("ftp.listFiles(" + pathToDatasetDir + "/" + FTP_STATUS_FILENAME + ")");
                FTPFile[] checkDatasetDirExists = ftp.listFiles(pathToDatasetDir + "/" + FTP_STATUS_FILENAME);
                if(checkDatasetDirExists.length == 0) {
                    logger.info("ftp.makeDirectory(" + pathToDatasetDir + ")");
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

                logger.info("Publishing entire file via FTPS...");
                // set request Id for
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
                    logger.info("Gzipping file before uploading...");
                    fileToUpload = createTempGzippedFile(csvOrTsvFile);
                    dataFilePathFTP = pathToDatasetDir + "/" + csvOrTsvFile.getName() + ".gz";
                    deleteFileToUpload = true;
                } catch (IOException ex) {
                    // if gzipping fails revert to sending raw CSV
                    logger.info("Gzipping failed, uploading CSV directly");
                    fileToUpload = csvOrTsvFile;
                    dataFilePathFTP = pathToDatasetDir + "/" + csvOrTsvFile.getName();
                }

                // upload data file
                long dataFileSizeBytes = fileToUpload.length();
                InputStream inputDataFile = new FileInputStream(fileToUpload);
                String dataFileResponse = uploadAndEnqueue(ftp, inputDataFile, dataFilePathFTP, dataFileSizeBytes);
                inputDataFile.close();
                if(!dataFileResponse.equals(SUCCESS_PREFIX)) {
                    closeFTPConnection(ftp);
                    status.setMessage(dataFileResponse);
                    return status;
                }
                inputDataFile.close();
                if(deleteFileToUpload)
                    fileToUpload.delete();

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
    }

    public static String getFTPHost(String domain, SodaDdl ddl) throws URISyntaxException, LongRunningQueryException, SodaError {
        HttpLowLevel httpClient = ddl.getHttpLowLevel();
        URI versionApiUri = new URI(domain + VERSION_API_ENDPOINT);
        ClientResponse response = httpClient.queryRaw(versionApiUri, HttpLowLevel.JSON_TYPE);
        String regionName = response.getHeaders().get(X_SOCRATA_REGION).get(0);
        return regionName + FTP_HOST_SUFFIX;
    }

    /**
     * Generates default content of control.json based on given job parameters
     *
     * @param ddl Soda 2 ddl object
     * @param publishMethod to use to publish (upsert, append, replace, or delete)
     *               NOTE: this option will be overriden if userPrefs has pathToFTPControlFile set
     * @param datasetId id of the Socrata dataset to publish to
     * @param fileToPublish filename of file to publish (.tsv or .csv file)
     * @param containsHeaderRow if true assume the first row in CSV/TSV file is a list of the dataset columns,
     *                          otherwise upload all rows as new rows (column order must exactly match that of
     *                          Socrata dataset)
     * @return content of control.json based on given job parameters
     * @throws com.socrata.exceptions.SodaError
     * @throws InterruptedException
     */
    public static String generateControlFileContent(final SodaDdl ddl,
                                                    final String fileToPublish, final PublishMethod publishMethod,
                                                    final String datasetId, final boolean containsHeaderRow) throws SodaError, InterruptedException {
        String skipValue = "0";
        String columnsValue = "null";
        if(!containsHeaderRow) {
            // if no header row get API field names for each column in dataset
            columnsValue = IntegrationUtility.getDatasetFieldNames(ddl, datasetId);
        }

        // In FTP Dropbox v2 there is only Append (append == upsert)
        PublishMethod ftpDropboxPublishMethod = publishMethod;
        if(publishMethod.equals(PublishMethod.upsert)) {
            ftpDropboxPublishMethod = PublishMethod.append;
        }

        String fileToPublishExtension = IntegrationUtility.getFileExtension(fileToPublish);
        String separator = ",";
        String fileType = "csv";
        String quote = "\\\"";
        if(fileToPublishExtension.equalsIgnoreCase("tsv")) {
            fileType = "tsv";
            separator = "\\t";
            quote = "\\u0000";
        }

        return "{\n" +
                "  \"action\" : \"" + capitalizeFirstLetter(ftpDropboxPublishMethod) + "\", \n" +
                "  \"" + fileType + "\" :\n" +
                "    {\n" +
                "      \"columns\" : " + columnsValue + ",\n" +
                "      \"skip\" : " + skipValue + ",\n" +
                "      \"fixedTimestampFormat\" : \"ISO8601\",\n" +
                "      \"floatingTimestampFormat\" : \"ISO8601\",\n" +
                "      \"timezone\" : \"UTC\",\n" +
                "      \"separator\" : \"" + separator + "\",\n" +
                "      \"quote\" : \"" + quote + "\",\n" +
                "      \"encoding\" : \"utf-8\",\n" +
                "      \"emptyTextIsNull\" : true,\n" +
                "      \"trimWhitespace\" : true,\n" +
                "      \"trimServerWhitespace\" : true,\n" +
                "      \"overrides\" : {}\n" +
                "    }\n" +
                "}";
    }

    private static String capitalizeFirstLetter(PublishMethod ftpDropboxPublishMethod) {
        return ftpDropboxPublishMethod.name().substring(0, 1).toUpperCase()
                + ftpDropboxPublishMethod.name().substring(1);
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
        Logger logger = Logger.getRootLogger();
        String pathToDomainRoot = "";
        logger.info("ftp.listFiles(FTP_REQUEST_ID_FILENAME);"); // TODO remove
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
                System.out.print("\rFailed upload status...retrying");
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
        Logger logger = Logger.getRootLogger();
        String requestId = IntegrationUtility.generateRequestId();
        InputStream inputRequestId = new ByteArrayInputStream(requestId.getBytes("UTF-8"));
        logger.info("ftp.storeFile(pathToRequestIdFile, inputRequestId)"); // TODO remove
        if (!ftp.storeFile(pathToRequestIdFile, inputRequestId)) {
            return FAILURE_PREFIX + ": " + ftp.getReplyString();
        }
        inputRequestId.close();
        return requestId;
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
                long uploadedFilesize = getFTPFilesize(ftp, path);
                if(filesize != uploadedFilesize) {
                    return String.format(FAILURE_PREFIX + ": uploaded filesize (%d B) " +
                            "did not match local filesize (%d B)", uploadedFilesize, filesize);
                }
            }

            // upload to enqueue directory
            File fileFromPath = new File(path);
            String datasetDirPath = fileFromPath.getParent();
            System.out.println("ftp.rename(path, datasetDirPath + \"/\" + FTP_ENQUEUE_JOB_DIRNAME);"); // TODO remove
            ftp.rename(path, datasetDirPath + "/" + FTP_ENQUEUE_JOB_DIRNAME);
            if(!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                return FAILURE_PREFIX + ": " + ftp.getReplyString();
            }
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
        System.out.println("ftp.sendCommand(\"SIZE\", path);"); // TODO remove
        ftp.sendCommand("SIZE", path);
        String replyString = ftp.getReplyString();
        if(!FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            throw new IOException(String.format(replyString));
        }
        String[] replySplit = replyString.trim().split(" ");
        return Long.parseLong(replySplit[1]);
    }

    /**
     * Creates a temporary file with the content of the remote file stored at
     * the given FTP URI
     *
     * @param pathToFTPFile
     * @return
     * @throws java.io.IOException
     */
    /*public static File getFileFromFTP(URI pathToFTPFile) throws Exception {
        if(!pathToFTPFile.getScheme().equals("ftp"))
            throw new IllegalArgumentException("Given URI does not represent an FTP site (URI must begin with 'ftp://')");

        /*pathToFTPFile = "/incoming/PD/PDCrimeData/pdcrimedata.csv";
        String ftpHost = "dsfsdf";
        try {
            pathToFTPFile = new URI("ftp://ftp.kcmo.org/incoming/PD/PDCrimeData/pdcrimedata.csv");
            System.out.println(pathToFTPFile.getScheme());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }*/

        /*
        FTPClient ftp = new FTPClient();
        System.out.println("Connecting to ftp://" + pathToFTPFile.getHost() + "...");
        ftp.connect(pathToFTPFile.getHost());
        if(FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
            ftp.enterLocalPassiveMode();
            ftp.login("anonymous", "");

            FTPFile[] checkRequestIdFile = ftp.listFiles();
            //System.out.println(checkRequestIdFile.toString());

            System.out.println("Downloading " + pathToFTPFile.toString() + "...");
            String relativePathToFTPFile = pathToFTPFile.getPath();
            InputStream ftpFileStream = ftp.retrieveFileStream(relativePathToFTPFile);
            File ftpFile = new File(pathToFTPFile.getPath());

            // verify the downloaded filesize == FTP filesize
            long uploadedFilesize = getFTPFilesize(ftp, relativePathToFTPFile);
            File tempFile = createFileFromStream(ftpFileStream, ftpFile.getName());
            long tempFilesize = tempFile.length();
            if(tempFilesize != uploadedFilesize) {
                throw new Exception(String.format("Error downloading " + pathToFTPFile.toString() + ": uploaded filesize (%d B) " +
                        "did not match local filesize (%d B)", uploadedFilesize, tempFilesize));
            }
            ftp.completePendingCommand();
            closeFTPConnection(ftp);
            return tempFile;
        } else {
            // TODO error
            throw new Exception("Error downloading " + pathToFTPFile.toString()
                    + ": could not connect to FTP server");
        }
    }
    */

    // TODO move to test class
    //getFileFromFTP(URI pathToFTPFile)

        /*final SodaDdl ddl = createSodaDdl();
        final UserPreferences userPrefs = getUserPrefs();

        URI ftpFile = new URI("ftp://ftp.kcmo.org/incoming/PD/PDCrimeData/pdcrimedata.csv");
        File twoRowsFTPFile = IntegrationUtility.getFileFromFTP(ftpFile);

        //System.out.println(twoRowsFTPFile.getName() + "--- " + twoRowsFTPFile.getAbsoluteFile() + "---");
        //twoRowsFTPFile = new File("src/test/resources/datasync_unit_test_three_rows_ISO_dates.csv");
        //System.out.println(twoRowsFTPFile.getName() + "--- " + twoRowsFTPFile.getAbsoluteFile() + "---");

        JobStatus result = IntegrationUtility.publishViaFTPDropboxV2(
                userPrefs, ddl, PublishMethod.replace, UNITTEST_DATASET_ID, twoRowsFTPFile, false, null);

        TestCase.assertEquals(JobStatus.SUCCESS, result);
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));*/

}
