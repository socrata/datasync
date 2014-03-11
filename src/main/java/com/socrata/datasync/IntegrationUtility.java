package com.socrata.datasync;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.ImmutableMap;
import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.*;

import javax.net.ssl.SSLContext;

public class IntegrationUtility {
    /**
     * @author Adrian Laurenzi
     *
     * A utility class for the Integration Job Type
     */
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

    private IntegrationUtility() {
        throw new AssertionError("Never instantiate utility classes!");
    }

    /**
     * @param csvOrTsvFile File has rows of data to be published and a :deleted column with TRUE in any column that
     *                should be deleted
     * @return upsert objects to delete the rows corresponding to the IDs within the given deletedIds File
     *
     */
    public static UpsertResult deleteRows(Soda2Producer producer, SodaDdl ddl,
                                          final String id, final File csvOrTsvFile, final int numRowsPerChunk, final boolean containsHeaderRow)
            throws IOException, SodaError, InterruptedException
    {
        List<Map<String, Object>> upsertObjectsChunk = new ArrayList<Map<String, Object>>();
        int totalRowsDeleted = 0;
        List<UpsertError> deleteErrors = new ArrayList<UpsertError>();

        String rowIdentifierName = getDatasetRowId(ddl, id);

        FileReader  fileReader = new FileReader(csvOrTsvFile);
        CSVReader reader = new CSVReader(fileReader);
        String[] currLine;

        // skip first row if there is a header row
        if(containsHeaderRow) {
            reader.readNext();
        }

        do {
            currLine = reader.readNext();
            if(currLine != null) {
                upsertObjectsChunk.add(ImmutableMap.of(rowIdentifierName, (Object)currLine[0], ":deleted", Boolean.TRUE));
            }
            if(upsertObjectsChunk.size() == numRowsPerChunk || currLine == null) {
                UpsertResult chunkResult = producer.upsert(id, upsertObjectsChunk);
                totalRowsDeleted += chunkResult.getRowsDeleted();

                if(chunkResult.errorCount() > 0) {
                    // TODO find a better way to suppress these errors (which are really not errors anyway)
                    for(UpsertError err : chunkResult.getErrors()) {
                        if(!err.getError().contains("no record is found")) {
                            deleteErrors.add(err);
                        }
                    }
                }
                upsertObjectsChunk.clear();
            }
        } while(currLine != null);
        reader.close();

        return new UpsertResult(
                0, 0, totalRowsDeleted, deleteErrors);
    }

    private static String getDatasetRowId(SodaDdl ddl, String id) throws SodaError, InterruptedException {
        Dataset info = (Dataset) ddl.loadDatasetInfo(id);
        Column rowIdentifier = info.lookupRowIdentifierColumn();
        String rowIdentifierName;
        if (rowIdentifier == null) {
            rowIdentifierName = ":id";
        } else {
            rowIdentifierName = rowIdentifier.getFieldName();
        }
        return rowIdentifierName;
    }

    /**
     *
     * Publishes the given csvOrTsvFile via SODA 2 replace or upsert/append. Publishes in chunks if using
     * upsert/append and numRowsPerChunk > 0 (all data will be upserted in one chunk if numRowsPerChunk == 0)
     * where each chunk contains numRowsPerChunk rows. Chunking is useful when uploading very large CSV files.
     *
     * @param method to use to publish (upsert, append, or replace; delete not allowed)
     * @param id dataset ID to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param numRowsPerChunk number of rows within CSV to publish in each chunk
     *                        (if numRowsPerChunk == 0 do not use chunking)
     * @param containsHeaderRow if true assume the first row in CSV/TSV file is a list of the dataset columns,
     *                          otherwise upload all rows as new rows (column order must exactly match that of
     *                          Socrata dataset)
     * @return UpsertResult containing success or error information
     * @throws IOException
     * @throws SodaError
     * @throws InterruptedException
     */
    public static UpsertResult publishViaSoda2(Soda2Producer producer, SodaDdl ddl,
                                               final PublishMethod method, final String id, final File csvOrTsvFile,
                                               int numRowsPerChunk, final boolean containsHeaderRow)
            throws IOException, SodaError, InterruptedException
    {
        // If doing a replace force it to upload all data as a single chunk
        if(method.equals(PublishMethod.replace)) {
            System.out.println("WARNING: replace does not support chunking.");
            numRowsPerChunk = 0;
        }

        List<Map<String, Object>> upsertObjectsChunk = new ArrayList<Map<String, Object>>();
        int totalRowsCreated = 0;
        int totalRowsUpdated = 0;
        int totalRowsDeleted = 0;
        List<UpsertError> upsertErrors = new ArrayList<UpsertError>();

        char columnDelimiter = ',';
        if(IntegrationUtility.getFileExtension(csvOrTsvFile.toString()).equals("tsv")) {
            columnDelimiter = '\t';
        }

        int numUploadedChunks = 0;
        FileReader  fileReader = new FileReader(csvOrTsvFile);
        CSVReader reader = new CSVReader(fileReader, columnDelimiter);

        String[] headers;
        if(containsHeaderRow) {
            headers = reader.readNext();
            // trim whitespace from header names
            if (headers != null) {
                for (int i=0; i<headers.length; i++) {
                    headers[i] = headers[i].trim();
                }
            }
        } else {
            // get API field names for each column in dataset
            Dataset info = (Dataset) ddl.loadDatasetInfo(id);
            List<Column> columns = info.getColumns();
            headers = new String[columns.size()];
            for(int i = 0; i < columns.size(); i++) {
                headers[i] = columns.get(i).getFieldName();
            }
        }

        if (headers != null) {
            String[] currLine;
            do {
                currLine = reader.readNext();
                ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
                if(currLine != null) {
                    for (int i=0; i<currLine.length; i++) {
                        if (i < headers.length) {
                            builder.put(headers[i], currLine[i]);
                        }
                    }
                    upsertObjectsChunk.add(builder.build());
                }
                if(upsertObjectsChunk.size() == numRowsPerChunk || currLine == null) {
                    if(numRowsPerChunk == 0) {
                        System.out.println("Publishing entire file via HTTP...");
                    } else {
                        System.out.println("Publishing file in chunks via HTTP (" + numUploadedChunks * numRowsPerChunk + " rows uploaded so far)...");
                    }

                    // upsert or replace current chunk
                    UpsertResult chunkResult;
                    if(method.equals(PublishMethod.upsert) || method.equals(PublishMethod.append)) {
                        chunkResult = producer.upsert(id, upsertObjectsChunk);
                    } else if(method.equals(PublishMethod.replace)) {
                        chunkResult = producer.replace(id, upsertObjectsChunk);
                    } else {
                        reader.close();
                        throw new IllegalArgumentException("Error performing publish: "
                                + method + " is not a valid publishing method");
                    }
                    totalRowsCreated += chunkResult.getRowsCreated();
                    totalRowsUpdated += chunkResult.getRowsUpdated();
                    totalRowsDeleted += chunkResult.getRowsDeleted();
                    numUploadedChunks += 1;

                    if(chunkResult.errorCount() > 0) {
                        if(numRowsPerChunk != 0) {
                            for (UpsertError upsertErr : chunkResult.getErrors()) {
                                int lineIndexOffset = (containsHeaderRow) ? 2 : 1;
                                System.out.println("Error uploading chunk " + numUploadedChunks + ": " +
                                        upsertErr.getError() + " (line " +
                                        (upsertErr.getIndex() + lineIndexOffset + ((numUploadedChunks-1) * numRowsPerChunk)) + " of file)");
                            }
                        }
                        upsertErrors.addAll(chunkResult.getErrors());
                    }

                    if(numRowsPerChunk != 0) {
                        System.out.println("Chunk " + numUploadedChunks + " uploaded: " + chunkResult.getRowsCreated() + " rows created; " +
                            chunkResult.getRowsUpdated() + " rows updated; " + chunkResult.getRowsDeleted() +
                            " rows deleted; " + chunkResult.errorCount() + " rows omitted");
                    }

                    upsertObjectsChunk.clear();
                }
            } while(currLine != null);
        }
        reader.close();
        return new UpsertResult(
                totalRowsCreated, totalRowsUpdated, totalRowsDeleted, upsertErrors);
    }

    /**
     * Get file extension 
     * @param file filename 
     * @return
     */
    public static String getFileExtension(String file) {
        String extension = "";
        int i = file.lastIndexOf('.');
        if (i > 0)
            extension = file.substring(i+1).toLowerCase();
        return extension;
    }

    /**
     * This operation will do an append/upsert.
     *
     * IMPORTANT: If you have a row identifier set on the dataset, and this is appending a row that
     * has an identifier of a row that has already been added, this will overwrite that row rather than
     * failing. If you have no row identifier set, this will be a straight append every time.
     */
    public static UpsertResult appendUpsert(Soda2Producer producer, SodaDdl ddl,
                                            final String id, final File file,
                                            int numRowsPerChunk, boolean containsHeaderRow)
                    throws SodaError, InterruptedException, IOException
    {
        return publishViaSoda2(producer, ddl, PublishMethod.upsert, id, file, numRowsPerChunk, containsHeaderRow);
    }

    /**
     * This is a new replace function that does not need a working copy.
     */
    public static UpsertResult replaceNew(Soda2Producer producer, SodaDdl ddl,
                                            final String id, final File file,
                                            boolean containsHeaderRow)
            throws SodaError, InterruptedException, IOException {
        return publishViaSoda2(producer, ddl, PublishMethod.replace, id, file, 0, containsHeaderRow);
    }

    /**
     * Publishes the given CSV/TSV file to the dataset with given datasetId
     * using FTP Dropbox v2.0 (a.k.a. "SmartUpdate").
     *
     * @param userPrefs object containing the user preferences
     * @param ddl Soda 2 ddl object
     * @param publishMethod to use to publish (upsert, append, replace, or delete)
     *               NOTE: this option will be overriden if userPrefs has pathToFTPControlFile set
     * @param datasetId id of the Socrata dataset to publish to
     * @param csvOrTsvFile file to publish containing data in comma- or tab- separated values (CSV or TSV) format
     * @param containsHeaderRow if true assume the first row in CSV/TSV file is a list of the dataset columns,
     *                          otherwise upload all rows as new rows (column order must exactly match that of
     *                          Socrata dataset)
     *                          NOTE: this option will be overriden if userPrefs has pathToFTPControlFile set
     * @param pathToFTPControlFile path to control.json (if this is supplied it will override publishMethod and
     *                             containsHeaderRow)
     * @return JobStatus containing success or error information
     */
    public static JobStatus publishViaFTPDropboxV2(final UserPreferences userPrefs, final SodaDdl ddl,
                                                   final PublishMethod publishMethod, final String datasetId,
                                                   final File csvOrTsvFile, boolean containsHeaderRow,
                                                   final String pathToFTPControlFile) {
        JobStatus status = JobStatus.PUBLISH_ERROR;

        String ftpHost = null;
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

            System.out.println("Connecting to " + ftpHost + ":" + FTP_HOST_PORT); // TODO remove
            ftp.connect(ftpHost, FTP_HOST_PORT);
            SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
            ftp.login(connectionInfo.getUser(), connectionInfo.getPassword());

            // verify connection was successful
            if(FTPReply.isPositiveCompletion(ftp.getReplyCode())) {
                System.out.println("ftp.setFileType(FTP.BINARY_FILE_TYPE);"); // TODO remove
                ftp.setFileType(FTP.BINARY_FILE_TYPE);
                System.out.println("ftp.enterLocalPassiveMode();"); // TODO remove
                ftp.enterLocalPassiveMode();

                // Set protection buffer size (what does this do??)
                //ftp.execPBSZ(0);
                // Set data channel protection to private
                System.out.println("ftp.execPROT(\"P\");"); // TODO remove
                ftp.execPROT("P");

                String pathToDomainRoot = getPathToDomainRoot(ftp, connectionInfo);
                String pathToDatasetDir = pathToDomainRoot + "/" + datasetId;

                // if datasetId does not exist then create the directory
                System.out.println("ftp.listFiles(pathToDatasetDir + \"/\" + FTP_STATUS_FILENAME);"); // TODO remove
                FTPFile[] checkDatasetDirExists = ftp.listFiles(pathToDatasetDir + "/" + FTP_STATUS_FILENAME);
                if(checkDatasetDirExists.length == 0) {
                    System.out.println("ftp.makeDirectory(pathToDatasetDir);"); // TODO remove
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

                InputStream inputControlFile = null;
                try {
                    inputControlFile = getControlFileInputStream(
                            ddl, publishMethod, datasetId, containsHeaderRow, pathToFTPControlFile);
                } catch (Exception e) {
                    closeFTPConnection(ftp);
                    e.printStackTrace();
                    setStatusMessage(datasetId, status, e.getMessage());
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

                System.out.println("Publishing entire file via FTPS...");
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
     * @throws IOException
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

    /**
     *
     * @param fileInput stream to convert to file
     * @return temp file with stream content
     * @throws IOException
     */
    private static File createFileFromStream(InputStream fileInput, String tempFilenameSuffix) throws IOException {
        File tempFile = File.createTempFile("DataSyncTemp_", "_" + tempFilenameSuffix) ;
        byte[] buffer = new byte[NUM_BYTES_OUT_BUFFER];
        FileOutputStream fileOutputStream = new FileOutputStream(tempFile);
        int bytes_read;
        while ((bytes_read = fileInput.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, bytes_read);
        }
        fileInput.close();
        fileOutputStream.close();
        return tempFile;
    }

    public static String getFTPHost(String domain, SodaDdl ddl) throws URISyntaxException, LongRunningQueryException, SodaError {
        HttpLowLevel httpClient = ddl.getHttpLowLevel();
        URI versionApiUri = new URI(domain + VERSION_API_ENDPOINT);
        ClientResponse response = httpClient.queryRaw(versionApiUri, HttpLowLevel.JSON_TYPE);
        String regionName = response.getHeaders().get(X_SOCRATA_REGION).get(0);
        return regionName + FTP_HOST_SUFFIX;
    }

    /**
     * Generates content of control.json file, either from existing file or generates
     * content based on job parameters
     *
     * @param ddl
     * @param publishMethod
     * @param datasetId
     * @param containsHeaderRow
     * @param pathToFTPControlFile
     * @return
     * @throws FileNotFoundException
     * @throws UnsupportedEncodingException
     * @throws SodaError
     * @throws InterruptedException
     */
    private static InputStream getControlFileInputStream(
            SodaDdl ddl, PublishMethod publishMethod, String datasetId, boolean containsHeaderRow, String pathToFTPControlFile) throws FileNotFoundException, UnsupportedEncodingException, SodaError, InterruptedException {
        InputStream inputControlFile;
        if(pathToFTPControlFile != null) {
            // if control file supplied in User Preferences
            inputControlFile = new FileInputStream(pathToFTPControlFile);
        } else {
            // configure SmartUpdate job based on UI input (rather than control.json)
            String skipValue = "0";
            String columnsValue = "null";

            if(!containsHeaderRow) {
                // if no header row get API field names for each column in dataset
                columnsValue = getDatasetFieldNames(ddl, datasetId);
            }

            // In FTP Dropbox v2 there is only Append (append == upsert)
            if(publishMethod.equals(PublishMethod.upsert)) {
                publishMethod = PublishMethod.append;
            }

            String publishMethodCapitalized = publishMethod.name().substring(0, 1).toUpperCase()
                    + publishMethod.name().substring(1);
            String controlFileContent = "{\n" +
                    "  \"action\" : \"" + publishMethodCapitalized + "\", \n" +
                    "  \"csv\" :\n" +
                    "    {\n" +
                    "      \"fixedTimestampFormat\" : \"ISO8601\",\n" +
                    "      \"separator\" : \",\",\n" +
                    "      \"timezone\" : \"UTC\",\n" +
                    "      \"encoding\" : \"utf-8\",\n" +
                    "      \"overrides\" : {},\n" +
                    "      \"quote\" : \"\\\"\",\n" +
                    "      \"emptyTextIsNull\" : true,\n" +
                    "      \"columns\" : " + columnsValue + ",\n" +
                    "      \"skip\" : " + skipValue + ",\n" +
                    "      \"floatingTimestampFormat\" : \"ISO8601\",\n" +
                    "      \"trimWhitespace\" : true\n" +
                    "    },\n" +
                    "  \"tsv\" :\n" +
                    "    {\n" +
                    "      \"fixedTimestampFormat\" : \"ISO8601\",\n" +
                    "      \"separator\" : \"\\t\",\n" +
                    "      \"timezone\" : \"UTC\",\n" +
                    "      \"encoding\" : \"utf-8\",\n" +
                    "      \"overrides\" : {},\n" +
                    "      \"quote\" : \"\\u0000\",\n" +
                    "      \"emptyTextIsNull\" : true,\n" +
                    "      \"columns\" : " + columnsValue + ",\n" +
                    "      \"skip\" : " + skipValue + ",\n" +
                    "      \"floatingTimestampFormat\" : \"ISO8601\",\n" +
                    "      \"trimWhitespace\" : true\n" +
                    "    }\n" +
                    "}";
            inputControlFile = new ByteArrayInputStream(controlFileContent.getBytes("UTF-8"));
        }
        return inputControlFile;
    }

    /**
     * Gets list of dataset field names in the form [col1, col2,...]
     *
     * @param ddl
     * @param datasetId
     * @return list of field names or null if there
     */
    public static String getDatasetFieldNames(SodaDdl ddl, String datasetId) throws SodaError, InterruptedException {
        Dataset info = (Dataset) ddl.loadDatasetInfo(datasetId);
        String columnsValue = "[";
        List<Column> columns = info.getColumns();
        for(int i = 0; i < columns.size(); i++) {
            if(i > 0)
                columnsValue += ",";
            columnsValue += "\"" + columns.get(i).getFieldName() + "\"";
        }
        columnsValue += "]";
        return columnsValue;
    }

    /**
     * Determines path on FTP server to domain root
     *
     * @param ftp
     * @param connectionInfo
     * @return "" if user user, or "/<DOMAIN>/" if user is SuperAdmin or has multi-domain access
     * @throws IOException
     */
    private static String getPathToDomainRoot(FTPSClient ftp, SocrataConnectionInfo connectionInfo) throws IOException {
        String pathToDomainRoot = "";
        System.out.println("ftp.listFiles(FTP_REQUEST_ID_FILENAME);"); // TODO remove
        FTPFile[] checkRequestIdFile = ftp.listFiles(FTP_REQUEST_ID_FILENAME);
        if(checkRequestIdFile.length == 0) { // user is a SuperAdmin or has multi-domain access
            String domainWithoutHTTP = connectionInfo.getUrl().replaceAll("https://", "");
            domainWithoutHTTP = domainWithoutHTTP.replaceAll("/", "");
            pathToDomainRoot = "/" + domainWithoutHTTP;
        }
        return pathToDomainRoot;
    }

    private static void setStatusMessage(String datasetId, JobStatus status, String error) {
        status.setMessage("Error retrieving column names from dataset" +
                " with uid '" + datasetId + "': " + error);
    }

    /**
     * Polls upload status.txt file until ERROR or SUCCESS message (ensuring
     * status.txt contains given requestId)
     *
     * @param ftp authenticated ftps object
     * @param pathToStatusFile absolute path on FTP server to the status.txt file
     * @param requestId requestId that must be present for status.txt content to be valid
     * @return status message (begins with 'SUCCESS: ...' or 'FAILURE: ...')
     * @throws IOException
     */
    private static String pollUploadStatus(FTPSClient ftp, String pathToStatusFile, String requestId) throws IOException {
        String uploadStatus = "";
        do {
            try {
                Thread.sleep(TIME_BETWEEN_FTP_STATUS_POLLS_MS);
            } catch (InterruptedException e) {
                // do nothing
            }
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
            System.out.print("\rPolling upload status..." + uploadStatus);
        } while(!uploadStatus.startsWith(SUCCESS_PREFIX) && !uploadStatus.startsWith(FAILURE_PREFIX));
        return uploadStatus;
    }

    /**
     * Sets (and returns) the FTP requestId to be a random 32 character hexidecimal value
     *
     * @param ftp authenticated ftps object
     * @param pathToRequestIdFile bsolute path on FTP server where requestId file is located
     * @return requestId that was set or if there was an error return error message in the
     *         form 'FAILURE:...'
     * @throws IOException
     */
    private static String setFTPRequestId(FTPSClient ftp, String pathToRequestIdFile) throws IOException {
        String requestId = generateRequestId();
        InputStream inputRequestId = new ByteArrayInputStream(requestId.getBytes("UTF-8"));
        System.out.println("ftp.storeFile(pathToRequestIdFile, inputRequestId)"); // TODO remove
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
     * Adds an entry to specified log dataset with given job run information
     * 
     * @return status for action to create row in log dataset
     */
    public static JobStatus addLogEntry(final String logDatasetID, final SocrataConnectionInfo connectionInfo,
                                        final IntegrationJob job, final JobStatus status, final UpsertResult result) {
        final Soda2Producer producer = Soda2Producer.newProducer(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());

        List<Map<String, Object>> upsertObjects = new ArrayList<Map<String, Object>>();
        Map<String, Object> newCols = new HashMap<String,Object>();

        // add standard log data
        Date currentDateTime = new Date();
        newCols.put("Date", (Object) currentDateTime);
        newCols.put("DatasetID", (Object) job.getDatasetID());
        newCols.put("FileToPublish", (Object) job.getFileToPublish());
        newCols.put("PublishMethod", (Object) job.getPublishMethod());
        newCols.put("JobFile", (Object) job.getPathToSavedFile());
        if(result != null) {
            newCols.put("RowsUpdated", (Object) result.rowsUpdated);
            newCols.put("RowsCreated", (Object) result.rowsCreated);
            newCols.put("RowsDeleted", (Object) result.rowsDeleted);
        }
        if(status.isError()) {
            newCols.put("Errors", (Object) status.getMessage());
        } else {
            newCols.put("Success", (Object) true);
        }
        upsertObjects.add(ImmutableMap.copyOf(newCols));

        JobStatus logStatus = JobStatus.SUCCESS;
        String errorMessage = "";
        boolean noPublishExceptions = false;
        try {
            producer.upsert(logDatasetID, upsertObjects);
            noPublishExceptions = true;
        }
        catch (SodaError sodaError) {
            sodaError.printStackTrace();
            errorMessage = sodaError.getMessage();
        }
        catch (InterruptedException intrruptException) {
            intrruptException.printStackTrace();
            errorMessage = intrruptException.getMessage();
        }
        catch (Exception other) {
            other.printStackTrace();
            errorMessage = other.toString() + ": " + other.getMessage();
        } finally {
            if(!noPublishExceptions) {
                logStatus = JobStatus.PUBLISH_ERROR;
                logStatus.setMessage(errorMessage);
            }
        }
        return logStatus;
    }

    /**
     * Open given uri in local web browser
     * @param uri to open in browser
     */
    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                System.out.println("Error: cannot open web page");
            }
        }
    }

    /**
     * @param pathToSaveJobFile path to a saved job file
     * @return command with absolute paths to execute job file at given path
     */
    public static String getRunJobCommand(String pathToSaveJobFile) {
        String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            jarPath = URLDecoder.decode(jarPath, "UTF-8");
            // Needed correct issue with windows where path includes a leading slash
            if(jarPath.contains(":") && (jarPath.startsWith("/") || jarPath.startsWith("\\"))) {
                jarPath = jarPath.substring(1, jarPath.length());
            }
            return "java -jar " + jarPath + " " + pathToSaveJobFile;
        } catch (UnsupportedEncodingException unsupportedEncoding) {
            return "Error getting path to this executeable: " + unsupportedEncoding.getMessage();
        }
    }

    /**
     * Returns a random 32 character request id
     */
    public static String generateRequestId() {
        String uuid = UUID.randomUUID().toString();
        String requestId = uuid.replace("-", "");
        return requestId;
    }

    /**
     * @param uid to validate
     * @return true if given uid is a valid Socrata uid (e.g. abcd-1234)
     */
    public static boolean uidIsValid(String uid) {
        Matcher uidMatcher = Pattern.compile("[a-z0-9]{4}-[a-z0-9]{4}").matcher(uid);
        return uidMatcher.matches();
    }

    public static String getValidPublishMethods() {
        String validPublishMethods = "";
        int i = 0;
        for(PublishMethod method: PublishMethod.values()) {
            if(i > 0)
                validPublishMethods += ", ";
            validPublishMethods += "'" + method.name() + "'";
            i++;
        }
        return validPublishMethods;
    }

    public static String getValidPortMethods() {
        String validPortMethods = "";
        int i = 0;
        for(PortMethod method : PortMethod.values()) {
            if(i > 0)
                validPortMethods += ", ";
            validPortMethods += method.name();
            i++;
        }
        return validPortMethods;
    }

    public static String getArrayAsQuotedList(String[] array) {
        String list = "";
        for(int i = 0; i < array.length; i++) {
            if(i > 0)
                list += ", ";
            list += "'" + array[i] + "'";
        }
        return list;
    }
}