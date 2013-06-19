package com.socrata.datasync;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaImporter;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertResult;

public class IntegrationJob implements Serializable {
	/**
	 * @author Adrian Laurenzi
	 *
	 * Stores a single integration job that can be opened/run in the GUI
	 * or in command-line mode.
	 */
	
	/**
     * Part of serializability, this id tracks if a serialized object can be
     * deserialized using this version of the class.
     * 
     * NOTE: Please add 1 to this number every time you change the readObject()
     * or writeObject() methods, so we don't have old-version IntegrationJob 
     * objects being made into new-version IntegrationJob objects.
     */
    private static final long serialVersionUID = 2L;

    private final String DELETE_ZERO_ROWS = "";
    private final int DATASET_ID_LENGTH = 9;
    
	private String datasetID;
	private String fileToPublish;
	private PublishMethod publishMethod;
	private String fileRowsToDelete;
	private String pathToSavedJobFile;
	
	private static final String DEFAULT_JOB_NAME = "Untitled Job";
	
	public IntegrationJob() {
		pathToSavedJobFile = "";
		datasetID = "";
		fileToPublish = "";
		publishMethod = PublishMethod.upsert;
		fileRowsToDelete = DELETE_ZERO_ROWS;
	}
	
	/**
	 * Loads integration job data from a file and
	 * uses the saved data to populate the fields
	 * of this object
	 */
	public IntegrationJob(String pathToFile) {
		try {
			InputStream file = new FileInputStream(pathToFile);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream (buffer);
			try{
				IntegrationJob loadedJob = (IntegrationJob) input.readObject();
				// Load data into this object
				setPathToSavedFile(loadedJob.getPathToSavedFile());
				setDatasetID(loadedJob.getDatasetID());
				setFileToPublish(loadedJob.getFileToPublish());
				setPublishMethod(loadedJob.getPublishMethod());
				setFileRowsToDelete(loadedJob.getFileColsToDelete());
			}
			finally{
				input.close();
			}
		}
		catch(ClassNotFoundException ex){
			System.out.println("Cannot perform input. Class not found.");
		}
		catch(IOException ex){
			System.out.println("Cannot perform input.");
		}
	}
	
	/**
	 * 
	 * @return an error JobStatus if any input is invalid, otherwise JobStatus.SUCCESS 
	 */
	public JobStatus validate(SocrataConnectionInfo connectionInfo) {
		if(connectionInfo.getUrl().equals("")
				|| connectionInfo.getUrl().equals("https://")) {
			return JobStatus.INVALID_DOMAIN;
		}
		if(datasetID.length() != DATASET_ID_LENGTH) {
			return JobStatus.INVALID_DATASET_ID;
		}
		if(fileToPublish.equals("")) {
			return JobStatus.MISSING_FILE_TO_PUBLISH;
		} else {
			File checkFileToPublish = new File(fileToPublish);
			if(!checkFileToPublish.exists()) {
				JobStatus errorStatus = JobStatus.FILE_TO_PUBLISH_DOESNT_EXIST;
				errorStatus.setMessage(fileToPublish + ": File to publish does not exist");
				return errorStatus;
			}
		}
		if(!publishMethod.equals(PublishMethod.upsert)
				&& !publishMethod.equals(PublishMethod.replace)
				&& !publishMethod.equals(PublishMethod.append)) {
			return JobStatus.INVALID_PUBLISH_METHOD;
		}
		
		// TODO add more validation
		
		return JobStatus.SUCCESS;
	}
	
	public JobStatus run() {
		UserPreferences userPrefs = new UserPreferences();
		SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
		
		UpsertResult result = null;
		JobStatus runStatus = JobStatus.SUCCESS;
		
		JobStatus validationStatus = validate(connectionInfo);
		if(validationStatus.isError()) {
			runStatus = validationStatus;
		} else {
			final Soda2Producer producer = Soda2Producer.newProducer(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());
			final SodaImporter importer = SodaImporter.newImporter(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());
	
			File fileToPublishFile = new File(fileToPublish);
			File deleteRowsFile = null;
			if(!fileRowsToDelete.equals(DELETE_ZERO_ROWS)) {
				deleteRowsFile = new File(fileRowsToDelete);
			}
			
			String errorMessage = "";
			try {
				if(publishMethod.equals(PublishMethod.upsert)) {
					result = IntegrationUtility.upsert(producer, importer, datasetID, deleteRowsFile, fileToPublishFile);
				}
				else if(publishMethod.equals(PublishMethod.append)) {
					result = IntegrationUtility.append(producer, importer, datasetID, fileToPublishFile);
				}
				else if(publishMethod.equals(PublishMethod.replace)) {
					IntegrationUtility.replaceOld(importer, datasetID, fileToPublishFile);
					result = null; // No upsert result for 'replace'
				}
			}
			catch (IOException ioException) {
				errorMessage = ioException.getMessage();
			} 
			catch (SodaError sodaError) {
				errorMessage = sodaError.getMessage();
			} 
			catch (InterruptedException intrruptException) {
				errorMessage = intrruptException.getMessage();
			}
			catch (Exception other) {
				errorMessage = other.getMessage();
			}
			finally {
				if(errorMessage != "") {
					runStatus = JobStatus.PUBLISH_ERROR;
					runStatus.setMessage(errorMessage);
				} else {
					runStatus = JobStatus.SUCCESS;
				}
			}
		}
		
		// TODO check if this may be a better way to check for errors
		//List<UpsertError> upsertErrors = result.getErrors();
		
		String adminEmail = userPrefs.getAdminEmail();
		String logDatasetID = userPrefs.getLogDatasetID();
		JobStatus logStatus = IntegrationUtility.addLogEntry(logDatasetID, connectionInfo, this, runStatus, result);
		// Send email if there was an error updating log or target dataset
		if(userPrefs.emailUponError() && !adminEmail.equals("") && !logDatasetID.equals("")) {
			String errorEmailMessage = "";
			String urlToLogDataset = connectionInfo.getUrl() + "/d/" + logDatasetID;
			if(runStatus.isError()) {
				errorEmailMessage += "There was an error updating a dataset.\n"
						+ "\nDataset: " + connectionInfo.getUrl() + "/d/" + getDatasetID()
						+ "\nFile to be published: " + fileToPublish
						+ "\nPublish method: " + publishMethod
						// TODO + "\nFile with rows to delete: " + fileRowsToDelete
						+ "\nJob File: " + pathToSavedJobFile
						+ "\nError message: " + runStatus.getMessage()
						+ "\nLog dataset: " + urlToLogDataset + "\n\n";
			}
			if(logStatus.isError()) {
				errorEmailMessage += "There was an error updating the log dataset: "
						+ urlToLogDataset + "\n"
						+ "Error message: " + logStatus.getMessage() + "\n\n";
			}
			if(!errorEmailMessage.equals("")) {
				try {
					// TODO do not hard-code email login...
					GoogleMail.send("performancesocrata@gmail.com", "ASPEtmp#7",
							adminEmail, "Socrata DataSync Error", errorEmailMessage);
				} catch (Exception e) {
					System.out.println("Error sending email to: " + adminEmail + "\n" + e.getMessage());
				}
			}
		}
		return runStatus;
	}
	
	/**
	 * Saves this object as a file at specified location
	 */
	public void writeToFile(String filepath) {
		try{
			//use buffering
			OutputStream file = new FileOutputStream(filepath);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(this);
			} finally{
				output.close();
			}
		}  
		catch(IOException ex){
			System.out.println("Cannot perform output.");
		}
	}
	
	public void setPathToSavedFile(String newPath) {
		pathToSavedJobFile = newPath;
	}

	public String getPathToSavedFile() {
		return pathToSavedJobFile;
	}
	
	public void setDatasetID(String newDatasetID) {
		datasetID = newDatasetID;
	}
	
	public String getDatasetID() {
		return datasetID;
	}
	
	public void setFileToPublish(String newFileToPublish) {
		fileToPublish = newFileToPublish;
	}

	public String getFileToPublish() {
		return fileToPublish;
	}
	
	public void setPublishMethod(PublishMethod newPublishMethod) {
		publishMethod = newPublishMethod;
	}
	
	public PublishMethod getPublishMethod() {
		return publishMethod;
	}
	
	public void setFileRowsToDelete(String newFileRowsToDelete) {
		fileRowsToDelete = newFileRowsToDelete;
	}
	
	public String getFileColsToDelete() {
		return fileRowsToDelete;
	}
	
	public String getJobFilename() {
		if(pathToSavedJobFile == "") {
			return DEFAULT_JOB_NAME;
		}
		return new File(pathToSavedJobFile).getName();
	}
	
	/**
     * Implements a custom serialization of a IntegrationJob object.
     * 
     * @param out
     *            the ObjectOutputStream to write to
     * @throws IOException
     *             if the stream fails
     */
    private void writeObject(final ObjectOutputStream out) throws IOException {
        // Write each field to the stream in a specific order.
        // Specifying this order helps shield the class from problems
        // in future versions.
        // The order must be the same as the read order in readObject()
    	out.writeObject(pathToSavedJobFile);
    	out.writeObject(datasetID);
        out.writeObject(fileToPublish);
        out.writeObject(publishMethod);
        out.writeObject(fileRowsToDelete);
    }

    /**
     * Implements a custom deserialization of an IntegrationJob object.
     * 
     * @param in
     *            the ObjectInputStream to read from
     * @throws IOException
     *             if the stream fails
     * @throws ClassNotFoundException
     *             if a class is not found
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Read each field from the stream in a specific order.
        // Specifying this order helps shield the class from problems
        // in future versions.
        // The order must be the same as the writing order in writeObject()
    	pathToSavedJobFile = (String) in.readObject();
    	datasetID = (String) in.readObject();
    	fileToPublish = (String) in.readObject();
    	publishMethod = (PublishMethod) in.readObject();
    	fileRowsToDelete = (String) in.readObject();
    }
}
