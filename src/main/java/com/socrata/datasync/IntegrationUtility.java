package com.socrata.datasync;

import java.awt.Desktop;
import java.io.*;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.socrata.utils.GeneralUtils;
import org.apache.commons.lang3.StringUtils;

public class IntegrationUtility {
	/**
	 * @author Adrian Laurenzi
	 * 
	 * A utility class for the SimpleIntegration application
	 */
	
	private IntegrationUtility() {
        throw new AssertionError("Never instantiate utility classes!");
    }
	
	/**
     * Does an upsert, looking at two files:
     *    Deleted ID file (contains the IDs of objects to delete), if set to null no rows will be deleted
     *    Added or updated objects (contains the objects to add/update)
     *
     * This function will read in the deletions + upserts, put them in a single list and then do the upsert operation.
     */
    public static UpsertResult upsert(Soda2Producer producer, SodaDdl ddl, final String id, final File deletedIds, final File addedUpdatedObjects) 
    		throws IOException, SodaError, InterruptedException, java.net.UnknownHostException
    {
    	List<Map<String, Object>> upsertObjects = new ArrayList<Map<String, Object>>();
        
    	if(deletedIds != null) {
            upsertObjects.addAll(getDeletedUpsertObjects(ddl, id, deletedIds));
        }

        upsertObjects.addAll(GeneralUtils.readInCsv(addedUpdatedObjects));
        return producer.upsert(id, upsertObjects);
    }

    /**
     * @param deletedIds File (contains the IDs of objects to delete), if set to null no rows will be deleted
     * @return upsert objects to delete the rows corresponding to the IDs within the given deletedIds File
     *
     */
    public static List<Map<String, Object>> getDeletedUpsertObjects(SodaDdl ddl, final String id, final File deletedIds)
            throws IOException, SodaError, InterruptedException
    {
        List<Map<String, Object>> upsertObjects = new ArrayList<Map<String, Object>>();

        if(deletedIds != null) {
            // get row identifier of dataset
            Dataset info = (Dataset) ddl.loadDatasetInfo(id);

            Column rowIdentifier = info.lookupRowIdentifierColumn();
            String rowIdentifierName;
            if (rowIdentifier == null) {
                rowIdentifierName = ":id";
            } else {
                rowIdentifierName = rowIdentifier.getFieldName();
            }

            // read in rows to be deleted
            String line;
            FileReader fileReader = new FileReader(deletedIds);
            BufferedReader reader = new BufferedReader(fileReader);

            while((line = reader.readLine()) != null) {
                upsertObjects.add(ImmutableMap.of(rowIdentifierName, (Object)line, ":deleted", Boolean.TRUE));
            }
            reader.close();
        }
        return upsertObjects;
    }

    /**
     *
     * Upserts the given csvFile in chunks where each chunk contains numRowsPerChunk rows.
     * This is useful when uploading very large CSV files. NOTE: The rows to be deleted are
     * all added to the first chunk.
     */
    public static UpsertResult upsertInChunks(int numRowsPerChunk, Soda2Producer producer, SodaDdl ddl, final String id, final File deletedIds, final File csvFile)
            throws IOException, SodaError, InterruptedException
    {
        List<Map<String, Object>> upsertObjectsChunk = new ArrayList<Map<String, Object>>();
        int totalRowsCreated = 0;
        int totalRowsUpdated = 0;
        int totalRowsDeleted = 0;

        if(deletedIds != null) {
            upsertObjectsChunk.addAll(getDeletedUpsertObjects(ddl, id, deletedIds));
        }

        FileReader  fileReader = new FileReader(csvFile);
        CSVReader reader = new CSVReader(fileReader);
        String[] headers = reader.readNext();
        if (headers != null) {
            String[] currLine;
            do {
                currLine = reader.readNext();
                ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
                if(currLine != null) {
                    for (int i=0; i<headers.length; i++) {
                        if (StringUtils.isNotEmpty(currLine[i])) {
                            builder.put(headers[i], currLine[i]);
                        }
                    }
                    upsertObjectsChunk.add(builder.build());
                }
                if(upsertObjectsChunk.size() == numRowsPerChunk || currLine == null) {
                    // upsert current chunk
                    UpsertResult chunkResult = producer.upsert(id, upsertObjectsChunk);
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

    /**
     * This operation will do an append, through using an upsert.
     *
     * IMPORTANT: If you have a row identifier set on the dataset, and this is appending a row that
     * has an identifier of a row that has already been added, this will overwrite that row rather than
     * failing.
     *
     * If you have no row identifier set, this will be a straight append every time.
     */
    public static UpsertResult append(Soda2Producer producer, final String id, final File file)
    		throws SodaError, InterruptedException, IOException
    {
        //Should be able to replace an append with an upsert
        return producer.upsertCsv(id, file);
    }

    public static UpsertResult appendInChunks(int numRowsPerChunk, Soda2Producer producer, final String id, final File file)
            throws SodaError, InterruptedException, IOException
    {
        return upsertInChunks(numRowsPerChunk, producer, null, id, null, file);
    }


    /**
     * This is a new replace function that does not need a working copy.
     */
    public static UpsertResult replaceNew(Soda2Producer producer, final String id, final File file) throws SodaError, InterruptedException, IOException {
        return producer.replaceCsv(id, file);
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
