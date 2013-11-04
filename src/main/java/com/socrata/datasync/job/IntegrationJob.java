package com.socrata.datasync.job;

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
import com.socrata.datasync.*;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class IntegrationJob implements Job { //, Serializable {
	/**
	 * @author Adrian Laurenzi
	 *
	 * Stores a single integration job that can be opened/run in the GUI
	 * or in command-line mode.
	 */
    private static final String DELETE_ZERO_ROWS = "";

    // When a file to be published is larger than this value (in bytes), file is chunked
    private static final int FILESIZE_CHUNK_CUTOFF_BYTES = 78643200; // = 75 MB
    // During chunking files are uploaded NUM_ROWS_PER_CHUNK rows per chunk
    private static final int NUM_ROWS_PER_CHUNK = 10000;

    // TODO move this somewhere else (or remove it)
    private static final int DATASET_ID_LENGTH = 9;

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 1L;

	private String datasetID;
	private String fileToPublish;
	private PublishMethod publishMethod;
	private String fileRowsToDelete;
	private String pathToSavedJobFile;
	
	private static final String DEFAULT_JOB_NAME = "Untitled Standard Job";
	
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
	public IntegrationJob(String pathToFile) throws IOException {
        // first try reading the 'current' format
        ObjectMapper mapper = new ObjectMapper();
        try {
            IntegrationJob loadedJob = mapper.readValue(new File(pathToFile), IntegrationJob.class);
            setPathToSavedFile(loadedJob.getPathToSavedFile());
            setDatasetID(loadedJob.getDatasetID());
            setFileToPublish(loadedJob.getFileToPublish());
            setPublishMethod(loadedJob.getPublishMethod());
            setFileRowsToDelete(loadedJob.getFileRowsToDelete());
        } catch (IOException e) {
            // if reading new format fails...try reading old format into this object
            try {
                InputStream file = new FileInputStream(pathToFile);
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput input = new ObjectInputStream (buffer);
                try{
                    com.socrata.datasync.IntegrationJob loadedJobOld = (com.socrata.datasync.IntegrationJob) input.readObject();
                    // Load data into this object
                    setPathToSavedFile(loadedJobOld.getPathToSavedFile());
                    setDatasetID(loadedJobOld.getDatasetID());
                    setFileToPublish(loadedJobOld.getFileToPublish());
                    setPublishMethod(loadedJobOld.getPublishMethod());
                    setFileRowsToDelete(loadedJobOld.getFileRowsToDelete());
                }
                finally{
                    input.close();
                }
            } catch(Exception e2){
                throw new IOException(e.toString());
            }
        }
	}
	
	/**
	 * 
	 * @return an error JobStatus if any input is invalid, otherwise JobStatus.VALID
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
		
		return JobStatus.VALID;
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
			boolean noPublishExceptions = false;
			try {
				if(publishMethod.equals(PublishMethod.upsert)) {
                    if(fileToPublishFile.length() > FILESIZE_CHUNK_CUTOFF_BYTES) {
                        result = IntegrationUtility.upsertInChunks(
                                NUM_ROWS_PER_CHUNK, producer, importer, datasetID, deleteRowsFile, fileToPublishFile);
                    } else {
					    result = IntegrationUtility.upsert(producer, importer, datasetID, deleteRowsFile, fileToPublishFile);
                    }
                    noPublishExceptions = true;
				}
				else if(publishMethod.equals(PublishMethod.append)) {
                    if(fileToPublishFile.length() > FILESIZE_CHUNK_CUTOFF_BYTES) {
                        result = IntegrationUtility.appendInChunks(
                                NUM_ROWS_PER_CHUNK, producer, datasetID, fileToPublishFile);
                    } else {
                        result = IntegrationUtility.append(producer, datasetID, fileToPublishFile);
                    }
                    noPublishExceptions = true;
				}
				else if(publishMethod.equals(PublishMethod.replace)) {
					result = IntegrationUtility.replaceNew(producer, datasetID, fileToPublishFile);
					noPublishExceptions = true;
				} else {
					errorMessage = JobStatus.INVALID_PUBLISH_METHOD.toString();
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
				errorMessage = other.toString() + ": " + other.getMessage();
			}
			finally {
				if(noPublishExceptions) {
                    // Check for upsert errors (only for upsert and append publish methods)
					if(result != null) {
                        if(result.errorCount() > 0) {
							for (UpsertError upsertErr : result.getErrors()) {
								errorMessage += upsertErr.getError() + " (line "
										+ (upsertErr.getIndex() + 1) + " of file) \n";
							}
							runStatus = JobStatus.PUBLISH_ERROR;
							runStatus.setMessage(errorMessage);
						} else {
                            //if(sodaError.getMessage().equals("Not found"))
                            //runStatus = JobStatus.PUBLISH_ERROR;
                            //runStatus.setMessage("Dataset with that ID does not exist or you do not have permission to publish to it");
                        }
					} else {
						runStatus = JobStatus.SUCCESS;
					}
				} else {
					runStatus = JobStatus.PUBLISH_ERROR;
					runStatus.setMessage(errorMessage);
				}
			}
		}
		
		String adminEmail = userPrefs.getAdminEmail();
		String logDatasetID = userPrefs.getLogDatasetID();
		JobStatus logStatus = JobStatus.SUCCESS;
		if(!logDatasetID.equals("")) {
			logStatus = IntegrationUtility.addLogEntry(logDatasetID, connectionInfo, this, runStatus, result);
		}
		// Send email if there was an error updating log or target dataset
		if(userPrefs.emailUponError() && !adminEmail.equals("")) {
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
			if(runStatus.isError() || logStatus.isError()) {
				try {
					SMTPMailer.send(adminEmail, "Socrata DataSync Error", errorEmailMessage);
				} catch (Exception e) {
					System.out.println("Error sending email to: " + adminEmail + "\n" + e.getMessage());
				}
			}
		}
		return runStatus;
	}
	
	/**
	 * Saves this object as a file at given filepath
	 */
	public void writeToFile(String filepath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(filepath), this);
	}

    @JsonProperty("fileVersionUID")
    public long getFileVersionUID() {
        return fileVersionUID;
    }

    @JsonProperty("pathToSavedJobFile")
	public void setPathToSavedFile(String newPath) {
		pathToSavedJobFile = newPath;
	}

    @JsonProperty("pathToSavedJobFile")
	public String getPathToSavedFile() {
		return pathToSavedJobFile;
	}

    @JsonProperty("datasetID")
	public void setDatasetID(String newDatasetID) {
		datasetID = newDatasetID;
	}

    @JsonProperty("datasetID")
	public String getDatasetID() {
		return datasetID;
	}

    @JsonProperty("fileToPublish")
	public void setFileToPublish(String newFileToPublish) {
		fileToPublish = newFileToPublish;
	}

    @JsonProperty("fileToPublish")
	public String getFileToPublish() {
		return fileToPublish;
	}

    @JsonProperty("publishMethod")
	public void setPublishMethod(PublishMethod newPublishMethod) {
		publishMethod = newPublishMethod;
	}

    @JsonProperty("publishMethod")
	public PublishMethod getPublishMethod() {
		return publishMethod;
	}

    @JsonProperty("fileRowsToDelete")
	public void setFileRowsToDelete(String newFileRowsToDelete) {
		fileRowsToDelete = newFileRowsToDelete;
	}

    @JsonProperty("fileRowsToDelete")
	public String getFileRowsToDelete() {
		return fileRowsToDelete;
	}
	
	public String getJobFilename() {
		if(pathToSavedJobFile.equals("")) {
			return DEFAULT_JOB_NAME;
		}
		return new File(pathToSavedJobFile).getName();
	}
}
