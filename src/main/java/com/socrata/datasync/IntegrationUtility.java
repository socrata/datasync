package com.socrata.datasync;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import au.com.bytecode.opencsv.CSVReader;

import com.google.common.collect.ImmutableMap;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;

public class IntegrationUtility {
    /**
     * @author Adrian Laurenzi
     *
     * A utility class for the Integration Job Type
     */
    private static final int NUM_BYTES_OUT_BUFFER = 1024;

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
                                System.err.println("Error uploading chunk " + numUploadedChunks + ": " +
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
     * Get file extension from the given path to a file
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

    /**
     * Returns list of dataset field names in the form '[col1, col2,...]'
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

    private static void setStatusMessage(String datasetId, JobStatus status, String error) {
        status.setMessage("Error retrieving column names from dataset" +
                " with uid '" + datasetId + "': " + error);
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