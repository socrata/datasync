package com.socrata.datasync;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.api.SodaImporter;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import com.socrata.model.importer.DatasetInfo;
import com.socrata.utils.GeneralUtils;

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
    	Dataset info = (Dataset) ddl.loadDatasetInfo(id);
        
        Column rowIdentifier = info.lookupRowIdentifierColumn();

        String rowIdentifierName;
        if (rowIdentifier == null) {
            rowIdentifierName = ":id";
        } else {
            rowIdentifierName = rowIdentifier.getFieldName();
        }
        
        List<Map<String, Object>> upsertObjects = new ArrayList<Map<String, Object>>();
        
        if(deletedIds != null) {
	        String line;
	        FileReader fileReader = new FileReader(deletedIds);
	        BufferedReader reader = new BufferedReader(fileReader);
	        
	        while((line = reader.readLine()) != null) {
	            upsertObjects.add(ImmutableMap.of(rowIdentifierName, (Object)line, ":deleted", Boolean.TRUE));
	        }
	        reader.close();
        }

        upsertObjects.addAll(GeneralUtils.readInCsv(addedUpdatedObjects));
        return producer.upsert(id, upsertObjects);
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
    public static UpsertResult append(Soda2Producer producer, final SodaImporter importer, final String id, final File file) 
    		throws SodaError, InterruptedException, IOException
    {
        //Should be able to replace an append with an upsert
        return producer.upsertCsv(id, file);
    }


    /**
     * This does the old style of replace, but uses the new library.  This still requires a working copy.
     */
    public static void replaceOld(final SodaImporter importer, final String id, final File file) 
    		throws SodaError, InterruptedException, IOException
    {
        //Currently the old publish way is the only way to do this
        DatasetInfo workingCopy = importer.createWorkingCopy(id);
        try {
            importer.replace(workingCopy.getId(), file, 1, null);
            importer.publish(workingCopy.getId());
            workingCopy = null;

        } finally {
            if (workingCopy != null) {
                importer.deleteDataset(workingCopy.getId());
            }
        }
    }


    /**
     * This is a new replace function that does not need a working copy.  This is going to be added after looking at your
     * use cases, so we need a day or two before it works.
     */
    public UpsertResult replaceNew(Soda2Producer producer, final String id, final File file) throws SodaError, InterruptedException, IOException {
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
		if(status.isError()) {
			newCols.put("Errors", (Object) status.getMessage());
		} else {
			if(result != null) { // replace method was used so no info on num rows updated
				newCols.put("RowsUpdated", (Object) result.rowsUpdated);
				newCols.put("RowsCreated", (Object) result.rowsCreated);
				newCols.put("RowsDeleted", (Object) result.rowsDeleted);
			}
			newCols.put("Success", (Object) true);
		}
		upsertObjects.add(ImmutableMap.copyOf(newCols));

		JobStatus logStatus = JobStatus.SUCCESS;
		String errorMessage = "";
		try {
			producer.upsert(logDatasetID, upsertObjects);
		}
		catch (SodaError sodaError) {
			errorMessage = sodaError.getMessage();
		} 
		catch (InterruptedException intrruptException) {
			errorMessage = intrruptException.getMessage();
		}
		catch (Exception other) {
			errorMessage = other.getMessage();
		} finally {
			if(errorMessage != "") {
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
	
}
