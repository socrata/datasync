package com.socrata.datasync;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.ImmutableMap;
import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import com.socrata.model.requests.SodaRequest;
import com.socrata.model.requests.SodaTypedRequest;
import com.socrata.utils.GeneralUtils;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import org.apache.commons.lang3.StringUtils;

public class IntegrationUtility {
    /**
     * @author Adrian Laurenzi
     *
     * A utility class for the Integration Job Type
     */
        
    private IntegrationUtility() {
        throw new AssertionError("Never instantiate utility classes!");
    }

    /**
     * @param csvOrTsvFile File has rows of data to be published and a :deleted column with TRUE in any column that
     *                should be deleted
     * @return upsert objects to delete the rows corresponding to the IDs within the given deletedIds File
     *
     */
    public static List<Map<String, Object>> getDeletedUpsertObjects(SodaDdl ddl, final String id, final File csvOrTsvFile)
            throws IOException, SodaError, InterruptedException
    {
        List<Map<String, Object>> upsertObjects = new ArrayList<Map<String, Object>>();

        // get row identifier of dataset
        Dataset info = (Dataset) ddl.loadDatasetInfo(id);
        Column rowIdentifier = info.lookupRowIdentifierColumn();
        String rowIdentifierName;
        if (rowIdentifier == null) {
            rowIdentifierName = ":id";
        } else {
            rowIdentifierName = rowIdentifier.getFieldName();
        }

        // TODO read in csvFile and extract out :deleted column data to generate list of rows to delete
        // read in rows to be deleted (OLD CODE)
        String line;
        FileReader fileReader = new FileReader(csvOrTsvFile); // USE CSVReader...
        BufferedReader reader = new BufferedReader(fileReader);

        while((line = reader.readLine()) != null) {
            upsertObjects.add(ImmutableMap.of(rowIdentifierName, (Object)line, ":deleted", Boolean.TRUE));
        }
        reader.close();

        return upsertObjects;
    }

    /**
     *
     * Publishes the given csvOrTsvFile via replace or upsert. Publishes in chunks if using upsert
     * and numRowsPerChunk > 0 (all data will be upserted in one chunk if numRowsPerChunk == 0)
     * where each chunk contains numRowsPerChunk rows. Chunking is useful when uploading very large CSV files.
     *
     * @param id dataset ID to publish to
     * @param csvOrTsvFile file containing data in comma- or tab- separated values (CSV or TSV) format
     *
     */
    public static UpsertResult publishDataFile(Soda2Producer producer, SodaDdl ddl,
                                      final PublishMethod method, final String id, final File csvOrTsvFile,
                                      int numRowsPerChunk, boolean containsHeaderRow)
            throws IOException, SodaError, InterruptedException
    {
        // If doing a replace force it to upload all data as a single chunk
        // TODO support chunking for replace publish method
        if(method.equals(PublishMethod.replace))
            numRowsPerChunk = 0;

        List<Map<String, Object>> upsertObjectsChunk = new ArrayList<Map<String, Object>>();
        int totalRowsCreated = 0;
        int totalRowsUpdated = 0;
        int totalRowsDeleted = 0;

        char columnDelimiter = ',';
        if(IntegrationUtility.getFileExtension(csvOrTsvFile.toString()).equals("tsv")) {
            columnDelimiter = '\t';
        }

        // TODO uncomment after getDeletedUpsertObjects is fully implemented (also remove deletedIds param)
        //if(deletedIds != null) {
            //upsertObjects.addAll(getDeletedUpsertObjects(ddl, id, addedUpdatedObjects));
        //}

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
                    // upsert or replace current chunk
                    UpsertResult chunkResult;
                    if(method.equals(PublishMethod.upsert) || method.equals(PublishMethod.upsert)) {
                        chunkResult = producer.upsert(id, upsertObjectsChunk);
                    } else if(method.equals(PublishMethod.replace)) {
                        // TODO need to use the old publisher workflow...
                        chunkResult = producer.replace(id, upsertObjectsChunk);
                    } else {
                        throw new IllegalArgumentException("Error performing publish: "
                                + method + " is not a valid publishing method");
                    }
                    totalRowsCreated += chunkResult.getRowsCreated();
                    totalRowsUpdated += chunkResult.getRowsUpdated();
                    totalRowsDeleted += chunkResult.getRowsDeleted();

                    if(chunkResult.errorCount() > 0) {
                        return new UpsertResult(
                                totalRowsCreated, totalRowsUpdated, totalRowsDeleted, chunkResult.getErrors());
                    }
                    upsertObjectsChunk.clear();
                }
            } while(currLine != null);
        }
        return new UpsertResult(
                totalRowsCreated, totalRowsUpdated, totalRowsDeleted, new ArrayList<UpsertError>());
    }

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
        return publishDataFile(producer, ddl, PublishMethod.upsert, id, file, numRowsPerChunk, containsHeaderRow);
    }

    /**
     * This is a new replace function that does not need a working copy.
     */
    public static UpsertResult replaceNew(Soda2Producer producer, SodaDdl ddl,
                                            final String id, final File file,
                                            boolean containsHeaderRow)
            throws SodaError, InterruptedException, IOException
    {
        return publishDataFile(producer, ddl, PublishMethod.replace, id, file, 0, containsHeaderRow);
    }
    
    /**
     * Adds an entry to specified log dataset with given job run information
     * 
     * @return status for action to create row in log dataset
     */
        public static JobStatus addLogEntry(String logDatasetID, SocrataConnectionInfo connectionInfo,
                    IntegrationJob job, JobStatus status, UpsertResult result) {
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
                errorMessage = sodaError.getMessage();
            }
            catch (InterruptedException intrruptException) {
                errorMessage = intrruptException.getMessage();
            }
            catch (Exception other) {
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
        
}