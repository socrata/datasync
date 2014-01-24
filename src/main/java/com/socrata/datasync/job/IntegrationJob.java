package com.socrata.datasync.job;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.List;

import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaImporter;
import com.socrata.datasync.*;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesJava;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class IntegrationJob implements Job {
	/**
	 * @author Adrian Laurenzi
	 *
	 * Stores a single integration job that can be opened/run in the GUI
	 * or in command-line mode.
	 */
    private UserPreferences userPrefs;

    // to upload entire file as a single chunk (numRowsPerChunk == 0)
    private static final int UPLOAD_SINGLE_CHUNK = 0;

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 1L;

	private String datasetID;
	private String fileToPublish;
	private PublishMethod publishMethod;
    private boolean fileToPublishHasHeaderRow;
	private String pathToSavedJobFile;
    private String pathToFTPControlFile;
	
	private static final String DEFAULT_JOB_NAME = "Untitled Standard Job";
    public static final List<String> allowedFileToPublishExtensions = Arrays.asList("csv", "tsv");
	
	public IntegrationJob() {
        userPrefs = new UserPreferencesJava();
        setDefaultParams();
	}

    /*
     * This is a method that enables DataSync preferences to be established
     * directly when DataSync is used in "library mode" or "command-line mode"
     * (rather than being loaded from Preferences class)
     */
    public IntegrationJob(UserPreferences userPrefs) {
        this.userPrefs = userPrefs;
        setDefaultParams();
    }

    private void setDefaultParams() {
        pathToSavedJobFile = "";
        datasetID = "";
        fileToPublish = "";
        publishMethod = PublishMethod.upsert;
        fileToPublishHasHeaderRow = true;
        pathToFTPControlFile = null;
    }
	
	/**
	 * Loads integration job data from a file and
	 * uses the saved data to populate the fields
	 * of this object
	 */
	public IntegrationJob(String pathToFile) throws IOException {
        userPrefs = new UserPreferencesJava();

        // first try reading the 'current' format
        ObjectMapper mapper = new ObjectMapper();
        try {
            IntegrationJob loadedJob = mapper.readValue(new File(pathToFile), IntegrationJob.class);
            setDatasetID(loadedJob.getDatasetID());
            setFileToPublish(loadedJob.getFileToPublish());
            setPublishMethod(loadedJob.getPublishMethod());
            setPathToSavedFile(pathToFile);
            setFileToPublishHasHeaderRow(loadedJob.getFileToPublishHasHeaderRow());
        } catch (IOException e) {
            // if reading new format fails...try reading old format into this object
            try {
                InputStream file = new FileInputStream(pathToFile);
                InputStream buffer = new BufferedInputStream(file);
                ObjectInput input = new ObjectInputStream (buffer);
                try{
                    com.socrata.datasync.IntegrationJob loadedJobOld = (com.socrata.datasync.IntegrationJob) input.readObject();
                    setDatasetID(loadedJobOld.getDatasetID());
                    setFileToPublish(loadedJobOld.getFileToPublish());
                    setPublishMethod(loadedJobOld.getPublishMethod());
                    setPathToSavedFile(pathToFile);
                    setFileToPublishHasHeaderRow(true);
                }
                finally{
                    input.close();
                }
            } catch(Exception e2) {
                // TODO add log entry?
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
        if(!IntegrationUtility.uidIsValid(datasetID)) {
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
            // Ensure file extension is an accepted format
            String fileToPublishExtension = IntegrationUtility.getFileExtension(fileToPublish);
            if(!allowedFileToPublishExtensions.contains(fileToPublishExtension)) {
                return JobStatus.FILE_TO_PUBLISH_INVALID_FORMAT;
            }
		}
        if(publishMethod.equals(PublishMethod.append)) {
            // get row identifier of dataset
            final SodaImporter importer = SodaImporter.newImporter(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());
            Dataset info = null;
            try {
                info = (Dataset) importer.loadDatasetInfo(datasetID);
                Column rowIdentifier = info.lookupRowIdentifierColumn();
                if (rowIdentifier != null) {
                    JobStatus status = JobStatus.INVALID_PUBLISH_METHOD;
                    status.setMessage("Append can only be performed on a dataset without a Row Identifier set" +
                            ". Dataset with ID '" + datasetID + "' has '" + rowIdentifier + "' set as the Row " +
                            "Identifier. You probably want to use the upsert method instead." );
                }
            } catch (Exception e) {
                // do nothing; if an exception occurs here it will almost
                // certainly propagate later where the error message will be more appropriate
            }
		}
		
		// TODO add more validation
		
		return JobStatus.VALID;
	}
	
	public JobStatus run() {
		SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
		
		UpsertResult result = null;
		JobStatus runStatus = JobStatus.SUCCESS;
        String runErrorMessage = null;
		JobStatus validationStatus = validate(connectionInfo);
		if(validationStatus.isError()) {
			runStatus = validationStatus;
		} else {
			// attach a requestId to all Producer API calls (for error tracking purposes)
            String jobRequestId = IntegrationUtility.generateRequestId();
            // TODO swap this out...
            //final Soda2Producer producer = Soda2Producer.newProducerWithRequestId(
            //        connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken(), jobRequestId);
            final Soda2Producer producer = Soda2Producer.newProducer(
                    connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());
            final SodaImporter importer = SodaImporter.newImporter(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());
	
			File fileToPublishFile = new File(fileToPublish);
			boolean noPublishExceptions = false;
			try {
                int filesizeChunkingCutoffMB =
                        Integer.parseInt(userPrefs.getFilesizeChunkingCutoffMB()) * 1048576;
                int numRowsPerChunk = Integer.parseInt(userPrefs.getNumRowsPerChunk());

				if(publishMethod.equals(PublishMethod.upsert) || publishMethod.equals(PublishMethod.append)) {
                    if(fileToPublishFile.length() > filesizeChunkingCutoffMB) {
                        result = IntegrationUtility.appendUpsert(
                                producer, importer, datasetID, fileToPublishFile, numRowsPerChunk, fileToPublishHasHeaderRow);
                    } else {
					    result = IntegrationUtility.appendUpsert(
                                producer, importer, datasetID, fileToPublishFile, UPLOAD_SINGLE_CHUNK, fileToPublishHasHeaderRow);
                    }
                    noPublishExceptions = true;
				}
				else if(publishMethod.equals(PublishMethod.replace)) {
					JobStatus ftpSmartUpdateStatus = IntegrationUtility.publishViaFTPDropboxV2(
                            userPrefs, importer, publishMethod, datasetID, fileToPublishFile, fileToPublishHasHeaderRow, pathToFTPControlFile);
					if(ftpSmartUpdateStatus.isError()) {
                        runErrorMessage = ftpSmartUpdateStatus.getMessage();
                    } else {
                        noPublishExceptions = true;
                    }
                }
                else if(publishMethod.equals(PublishMethod.delete)) {
                    // TODO might be a good idea to do deleted in chunks by default
                    if(fileToPublishFile.length() > filesizeChunkingCutoffMB) {
                        result = IntegrationUtility.deleteRows(
                                producer, importer, datasetID, fileToPublishFile, numRowsPerChunk, fileToPublishHasHeaderRow);
                    } else {
                        result = IntegrationUtility.deleteRows(
                                producer, importer, datasetID, fileToPublishFile, UPLOAD_SINGLE_CHUNK, fileToPublishHasHeaderRow);
                    }
                    noPublishExceptions = true;
                } else {
					runErrorMessage = JobStatus.INVALID_PUBLISH_METHOD.toString();
				}
			}
			catch (IOException ioException) {
				runErrorMessage = ioException.getMessage();
			} 
			catch (SodaError sodaError) {
                runErrorMessage = sodaError.getMessage();
			} 
			catch (InterruptedException intrruptException) {
				runErrorMessage = intrruptException.getMessage();
			}
			catch (Exception other) {
				runErrorMessage = other.toString() + ": " + other.getMessage();
			}
			finally {
				if(noPublishExceptions) {
                    // Check for [row-level] SODA 2 errors
					if(result != null) {
                        if(result.errorCount() > 0) {
							for (UpsertError upsertErr : result.getErrors()) {
								runErrorMessage += upsertErr.getError() + " (line "
										+ (upsertErr.getIndex() + 1) + " of file) \n";
							}
							runStatus = JobStatus.PUBLISH_ERROR;
						}
					}
				} else {
                    runStatus = JobStatus.PUBLISH_ERROR;
                    if(runErrorMessage.equals("Not found")) {
                        runErrorMessage = "Dataset with that ID does not exist or you do not have permission to publish to it";
                    }
				}
			}
		}
		
		String adminEmail = userPrefs.getAdminEmail();
		String logDatasetID = userPrefs.getLogDatasetID();
		JobStatus logStatus = JobStatus.SUCCESS;
		if(!logDatasetID.equals("")) {
            if(runErrorMessage != null)
                runStatus.setMessage(runErrorMessage);
			logStatus = IntegrationUtility.addLogEntry(logDatasetID, connectionInfo, this, runStatus, result);
		}
		// Send email if there was an error updating log or target dataset
		if(userPrefs.emailUponError() && !adminEmail.equals("")) {
			String errorEmailMessage = "";
			String urlToLogDataset = connectionInfo.getUrl() + "/d/" + logDatasetID;
			if(runStatus.isError()) {
				errorEmailMessage += "There was an error updating a dataset.\n"
						+ "\nDataset: " + connectionInfo.getUrl() + "/d/" + getDatasetID()
						+ "\nFile to publish: " + fileToPublish
                        + "\nFile to publish has header row: " + fileToPublishHasHeaderRow
						+ "\nPublish method: " + publishMethod
						+ "\nJob File: " + pathToSavedJobFile
						+ "\nError message: " + runErrorMessage
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

        // IMPORTANT because setMessage from Logging dataset interferes with enum
        // TODO NEED to make this cleaner by turning JobStatus into regular class (rather than enum)
        if(runErrorMessage != null)
            runStatus.setMessage(runErrorMessage);

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

    @JsonProperty("fileToPublishHasHeaderRow")
    public boolean getFileToPublishHasHeaderRow() {
        return fileToPublishHasHeaderRow;
    }

    @JsonProperty("fileToPublishHasHeaderRow")
    public void setFileToPublishHasHeaderRow(boolean newFileToPublishHasHeaderRow) {

        fileToPublishHasHeaderRow = newFileToPublishHasHeaderRow;
    }

    @JsonProperty("pathToFTPControlFile")
    public String getPathToFTPControlFile() { return pathToFTPControlFile; }

    @JsonProperty("pathToFTPControlFile")
    public void setPathToFTPControlFile(String newPathToFTPControlFile) { pathToFTPControlFile = newPathToFTPControlFile; }

    public void setPathToSavedFile(String newPath) {
        pathToSavedJobFile = newPath;
    }

    public String getPathToSavedFile() {
        return pathToSavedJobFile;
    }

	public String getJobFilename() {
		if(pathToSavedJobFile.equals("")) {
			return DEFAULT_JOB_NAME;
		}
		return new File(pathToSavedJobFile).getName();
	}
}
