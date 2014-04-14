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

import com.socrata.datasync.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaImporter;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesJava;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;

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

    public static final int NUM_BYTES_PER_MB = 1048576;
    private static final String DEFAULT_JOB_NAME = "Untitled Standard Job";
    public static final List<String> allowedFileToPublishExtensions = Arrays.asList("csv", "tsv");
    public static final List<String> allowedFtpControlFileExtensions = Arrays.asList("json");

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 3L;

	private String datasetID;
	private String fileToPublish;
	private PublishMethod publishMethod;
    private boolean fileToPublishHasHeaderRow;
	private String pathToSavedJobFile;
    private String pathToFTPControlFile;
    private String ftpControlFileContent;
    private boolean publishViaFTP;
	
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
        publishMethod = PublishMethod.replace;
        fileToPublishHasHeaderRow = true;
        pathToFTPControlFile = null;
        ftpControlFileContent = null;
        publishViaFTP = false;
    }
	
	/**
	 * Loads integration job data from a file and
	 * uses the saved data to populate the fields
	 * of this object
	 */
	public IntegrationJob(String pathToFile) throws IOException {
        setDefaultParams();
        userPrefs = new UserPreferencesJava();
        // first try reading the 'current' format
        ObjectMapper mapper = new ObjectMapper();
        try {
            IntegrationJob loadedJob = mapper.readValue(new File(pathToFile), IntegrationJob.class);
            setDatasetID(loadedJob.getDatasetID());
            setFileToPublish(loadedJob.getFileToPublish());
            setPublishMethod(loadedJob.getPublishMethod());
            setFileToPublishHasHeaderRow(loadedJob.getFileToPublishHasHeaderRow());
            setPathToSavedFile(pathToFile);
            setPathToFTPControlFile(loadedJob.getPathToFTPControlFile());
            setFtpControlFileContent(loadedJob.getFtpControlFileContent());
            setPublishViaFTP(loadedJob.getPublishViaFTP());
        } catch (IOException e) {
            // if reading new format fails...try reading old format into this object
            loadOldSijFile(pathToFile);
        }
	}

    /**
     * This allows backward compatability with DataSync 0.1 .sij file format
     *
     * @param pathToFile .sij file that uses old serialization format (Java native)
     * @throws IOException
     */
    private void loadOldSijFile(String pathToFile) throws IOException {
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
                setPublishViaFTP(false);
                setPathToFTPControlFile(null);
                setFtpControlFileContent(null);
            }
            finally{
                input.close();
            }
        } catch(Exception e) {
            // TODO add log entry?
            throw new IOException(e.toString());
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
			if(!checkFileToPublish.exists() || checkFileToPublish.isDirectory()) {
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
        if(publishViaFTP) {
            if((pathToFTPControlFile == null || pathToFTPControlFile.equals("")) &&
                    (ftpControlFileContent == null || ftpControlFileContent.equals(""))) {
                JobStatus errorStatus = JobStatus.PUBLISH_ERROR;
                errorStatus.setMessage("You must generate or select a Control file if publishing via FTP SmartUpdate");
                return errorStatus;
            } else {
                if(pathToFTPControlFile != null && !pathToFTPControlFile.equals("")) {
                    File controlFile = new File(pathToFTPControlFile);
                    if(!controlFile.exists() || controlFile.isDirectory()) {
                        JobStatus errorStatus = JobStatus.PUBLISH_ERROR;
                        errorStatus.setMessage(pathToFTPControlFile + ": FTP control file does not exist");
                        return errorStatus;
                    }
                }
            }
        }
		
		// TODO add more validation? (e.g. validation of File To Publish header row
		
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
            runErrorMessage = validationStatus.getMessage();
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
                int filesizeChunkingCutoffBytes =
                        Integer.parseInt(userPrefs.getFilesizeChunkingCutoffMB()) * NUM_BYTES_PER_MB;
                int numRowsPerChunk = Integer.parseInt(userPrefs.getNumRowsPerChunk());

                if(publishMethod.equals(PublishMethod.upsert) || publishMethod.equals(PublishMethod.append)) {
                    if(publishViaFTP) {
                        runErrorMessage = "FTP does not currently support upsert or append";
                    } else {
                        result = doAppendOrUpsertViaHTTP(
                                producer, importer, fileToPublishFile, filesizeChunkingCutoffBytes, numRowsPerChunk);
                        noPublishExceptions = true;
                    }
				}
				else if(publishMethod.equals(PublishMethod.replace)) {
                    // TODO clean all of this up (once I refactor JobStatus class)
                    if(publishViaFTP) {
                        // Publish via FTP
                        JobStatus ftpSmartUpdateStatus = doPublishViaFTPv2(importer, fileToPublishFile);
                        if(ftpSmartUpdateStatus.isError()) {
                            runErrorMessage = ftpSmartUpdateStatus.getMessage();
                        } else {
                            noPublishExceptions = true;
                        }
                    } else {
                        // Publish via HTTP
                        result = IntegrationUtility.replaceNew(
                                producer, importer, datasetID, fileToPublishFile, fileToPublishHasHeaderRow);
                        noPublishExceptions = true;
                    }
                }
                else if(publishMethod.equals(PublishMethod.delete)) {
                    if(publishViaFTP) {
                        runErrorMessage = "FTP does not currently support delete";
                    } else {
                        result = doDeleteViaHTTP(
                            producer, importer, fileToPublishFile, filesizeChunkingCutoffBytes, numRowsPerChunk);
                        noPublishExceptions = true;
                    }
                } else {
					runErrorMessage = JobStatus.INVALID_PUBLISH_METHOD.getMessage();
				}
			}
			catch (IOException ioException) {
                ioException.printStackTrace();
				runErrorMessage = "IO exception: " + ioException.getMessage();
			} 
			catch (SodaError sodaError) {
                sodaError.printStackTrace();
                runErrorMessage = sodaError.getMessage();
			} 
			catch (InterruptedException intrruptException) {
                intrruptException.printStackTrace();
				runErrorMessage = "Interrupted exception: " + intrruptException.getMessage();
			}
			catch (Exception other) {
                other.printStackTrace();
				runErrorMessage = "Unexpected exception: " + other.getMessage();
			}
			finally {
				if(noPublishExceptions) {
                    // Check for [row-level] SODA 2 errors
					if(result != null) {
                        if(result.errorCount() > 0) {
                            int lineIndexOffset = (fileToPublishHasHeaderRow) ? 2 : 1;
                            runErrorMessage = "";
							for (UpsertError upsertErr : result.getErrors()) {
								runErrorMessage += upsertErr.getError() + " (line "
										+ (upsertErr.getIndex() + lineIndexOffset) + " of file) \n";
							}
							runStatus = JobStatus.PUBLISH_ERROR;
						}
					}
				} else {
                    runStatus = JobStatus.PUBLISH_ERROR;
                    if(runErrorMessage != null) {
                        if(runErrorMessage.equals("Not found")) {
                            runErrorMessage = "Dataset with that ID does not exist or you do not have permission to publish to it";
                        }
                    }
				}
			}
		}

        // TODO NEED to make this cleaner by turning JobStatus into regular class (rather than enum)
        if(runErrorMessage != null)
            runStatus.setMessage(runErrorMessage);

		String adminEmail = userPrefs.getAdminEmail();
		String logDatasetID = userPrefs.getLogDatasetID();
        String logPublishingErrorMessage = null;
        if(logDatasetID != null && !logDatasetID.equals("")) {
            String logDatasetUrl = userPrefs.getDomain() + "/d/" + userPrefs.getLogDatasetID();
            System.out.println("Publishing results to logging dataset (" + logDatasetUrl + ")...");
            logPublishingErrorMessage = IntegrationUtility.addLogEntry(
                    logDatasetID, connectionInfo, this, runStatus, result);
            if(logPublishingErrorMessage != null) {
                System.out.println("Error publishing results to logging dataset (" + logDatasetUrl + "): " +
                        logPublishingErrorMessage);
            }
		}

		if(userPrefs.emailUponError() && !adminEmail.equals("")) {
            sendErrorNotificationEmail(
                    adminEmail, connectionInfo, runStatus, runErrorMessage, logDatasetID, logPublishingErrorMessage);
		}

        return runStatus;
	}

    private JobStatus doPublishViaFTPv2(SodaImporter importer, File fileToPublishFile) {
        if((pathToFTPControlFile != null && !pathToFTPControlFile.equals(""))) {
            return FTPUtility.publishViaFTPDropboxV2(
                    userPrefs, importer,
                    datasetID, fileToPublishFile, new File(pathToFTPControlFile));
        } else {
            return FTPUtility.publishViaFTPDropboxV2(
                    userPrefs, importer,
                    datasetID, fileToPublishFile, ftpControlFileContent);
        }
    }

    private void sendErrorNotificationEmail(final String adminEmail, final SocrataConnectionInfo connectionInfo, final JobStatus runStatus, final String runErrorMessage, final String logDatasetID, final String logPublishingErrorMessage) {
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
        if(logPublishingErrorMessage != null) {
            errorEmailMessage += "There was an error updating the log dataset: "
                    + urlToLogDataset + "\n"
                    + "Error message: " + logPublishingErrorMessage + "\n\n";
        }
        if(runStatus.isError() || logPublishingErrorMessage != null) {
            try {
                SMTPMailer.send(adminEmail, "Socrata DataSync Error", errorEmailMessage);
            } catch (Exception e) {
                System.out.println("Error sending email to: " + adminEmail + "\n" + e.getMessage());
            }
        }
    }

    private UpsertResult doAppendOrUpsertViaHTTP(Soda2Producer producer, SodaImporter importer, File fileToPublishFile, int filesizeChunkingCutoffBytes, int numRowsPerChunk) throws SodaError, InterruptedException, IOException {
        int numberOfRows = numRowsPerChunk(fileToPublishFile, filesizeChunkingCutoffBytes, numRowsPerChunk);
        UpsertResult result = IntegrationUtility.appendUpsert(
                producer, importer, datasetID, fileToPublishFile, numberOfRows, fileToPublishHasHeaderRow);
        return result;
    }

    private UpsertResult doDeleteViaHTTP(
            Soda2Producer producer, SodaImporter importer, File fileToPublishFile, int filesizeChunkingCutoffBytes, int numRowsPerChunk)
            throws SodaError, InterruptedException, IOException {
        int numberOfRows = numRowsPerChunk(fileToPublishFile, filesizeChunkingCutoffBytes, numRowsPerChunk);
        UpsertResult result = IntegrationUtility.deleteRows(
                producer, importer, datasetID, fileToPublishFile, numberOfRows, fileToPublishHasHeaderRow);
        return result;
    }

    private int numRowsPerChunk(File fileToPublishFile, int filesizeChunkingCutoffBytes, int numRowsPerChunk) {
        int numberOfRows;
        if(fileToPublishFile.length() > filesizeChunkingCutoffBytes) {
            numberOfRows = numRowsPerChunk;
        } else {
            numberOfRows = UPLOAD_SINGLE_CHUNK;
        }
        return numberOfRows;
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
    public void setPathToFTPControlFile(String newPathToFTPControlFile) {
        pathToFTPControlFile = newPathToFTPControlFile; }

    @JsonProperty("ftpControlFileContent")
    public String getFtpControlFileContent() { return ftpControlFileContent; }

    @JsonProperty("ftpControlFileContent")
    public void setFtpControlFileContent(String newFtpControlFileContent) {
        ftpControlFileContent = newFtpControlFileContent; }

    @JsonProperty("publishViaFTP")
    public boolean getPublishViaFTP() { return publishViaFTP; }

    @JsonProperty("publishViaFTP")
    public void setPublishViaFTP(boolean newPublishViaFTP) {
        publishViaFTP = newPublishViaFTP;
    }

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
