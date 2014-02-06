package com.socrata.datasync;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.socrata.api.Soda2Producer;
import com.socrata.datasync.job.MetadataJob;
import com.socrata.exceptions.SodaError;

public class MetadataUtility {
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
            //TODO: This may change based on how we implement running metadata jobs from the command line.
            return "java -jar " + jarPath + " " + pathToSaveJobFile;
        } catch (UnsupportedEncodingException unsupportedEncoding) {
            return "Error getting path to this executeable: " + unsupportedEncoding.getMessage();
        }
    }
    
	//Probably makes sense to make one generic addLogEntry() for all job types
    public static JobStatus addLogEntry(String logDatasetID, SocrataConnectionInfo connectionInfo,
            MetadataJob job, JobStatus status) {
	    final Soda2Producer producer = Soda2Producer.newProducer(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());
	
	    List<Map<String, Object>> upsertObjects = new ArrayList<Map<String, Object>>();
	    Map<String, Object> newCols = new HashMap<String,Object>();
	
	    // add standard log data
	    Date currentDateTime = new Date();
	    newCols.put("Date", (Object) currentDateTime);
	    newCols.put("DatasetID", (Object) job.getDatasetID());
	    newCols.put("JobFile", (Object) job.getPathToSavedFile());
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

}
