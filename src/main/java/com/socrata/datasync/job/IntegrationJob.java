package com.socrata.datasync.job;

import com.google.common.collect.ImmutableMap;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaImporter;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SMTPMailer;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.Utils;
import com.socrata.datasync.VersionProvider;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.publishers.DeltaImporter2Publisher;
import com.socrata.datasync.publishers.FTPDropbox2Publisher;
import com.socrata.datasync.publishers.Soda2Publisher;
import com.socrata.datasync.validation.IntegrationJobValidity;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import org.apache.commons.cli.CommandLine;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class IntegrationJob extends Job {

    static AtomicInteger jobCounter = new AtomicInteger(0);
    int jobNum = jobCounter.getAndIncrement();
    private String defaultJobName = "Unsaved Standard Job" + " (" + jobNum + ")";

    // to upload entire file as a single chunk (numRowsPerChunk == 0)
    private static final int UPLOAD_SINGLE_CHUNK = 0;
    public static final int NUM_BYTES_PER_MB = 1048576;

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 4L;

    private UserPreferences userPrefs;
    private String datasetID = "";
	private String fileToPublish = "";
	private PublishMethod publishMethod = PublishMethod.replace;
    private boolean fileToPublishHasHeaderRow = true;
	private String pathToControlFile = null;
    private String controlFileContent = null;
    private boolean publishViaFTP = false;
    private boolean publishViaDi2Http = false;
    private ControlFile controlFile = null;

    private ObjectMapper controlFileMapper =
            new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    public IntegrationJob() {
        userPrefs = new UserPreferencesJava();
	}

    /*
     * This is a method that enables DataSync preferences to be established
     * directly when DataSync is used in "library mode" or "command-line mode"
     * (rather than being loaded from Preferences class)
     */
    public IntegrationJob(UserPreferences userPrefs) {
        this.userPrefs = userPrefs;
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
            setFileToPublishHasHeaderRow(loadedJob.getFileToPublishHasHeaderRow());
            setPathToSavedFile(pathToFile);
            setPathToControlFile(loadedJob.getPathToControlFile());
            setControlFileContent(loadedJob.getControlFileContent());
            setPublishViaFTP(loadedJob.getPublishViaFTP());
            setPublishViaDi2Http(loadedJob.getPublishViaDi2Http());
        } catch (IOException e) {
            // if reading new format fails...try reading old format into this object
            loadOldSijFile(pathToFile);
        }
	}

    @JsonProperty("fileVersionUID")
    public long getFileVersionUID() {
        return fileVersionUID;
    }

    public ControlFile getControlFile() {
        return controlFile;
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

    @JsonProperty("pathToControlFile")
    public String getPathToControlFile() { return pathToControlFile; }

    @JsonProperty("pathToControlFile")
    public void setPathToControlFile(String newPathToControlFile) {
        pathToControlFile = newPathToControlFile;
    }

    @JsonProperty("pathToFTPControlFile")
    public String getFTPPathToControlFile() { return pathToControlFile; }

    @JsonProperty("pathToFTPControlFile")
    public void setPathToFTPControlFile(String newPathToControlFile) {
        pathToControlFile = newPathToControlFile;
    }

    @JsonProperty("controlFileContent")
    public String getControlFileContent() { return controlFileContent; }

    @JsonProperty("controlFileContent")
    public void setControlFileContent(String newcontrolFileContent) {
        controlFileContent = newcontrolFileContent;
    }

    @JsonProperty("ftpControlFileContent")
    public String getFTPControlFileContent() { return controlFileContent; }

    @JsonProperty("ftpControlFileContent")
    public void setFTPControlFileContent(String newcontrolFileContent) {
        controlFileContent = newcontrolFileContent;
    }

    @JsonProperty("publishViaFTP")
    public boolean getPublishViaFTP() { return publishViaFTP; }

    @JsonProperty("publishViaFTP")
    public void setPublishViaFTP(boolean newPublishViaFTP) { publishViaFTP = newPublishViaFTP; }

    @JsonProperty("publishViaDi2Http")
    public boolean getPublishViaDi2Http() { return publishViaDi2Http; }

    @JsonProperty("publishViaDi2Http")
    public void setPublishViaDi2Http(boolean newPublishViaDi2Http) { publishViaDi2Http = newPublishViaDi2Http; }

    public String getDefaultJobName() { return defaultJobName; }

    /**
     * Checks that the command line arguments are sensible
     * NB: it is expected that this is run before 'configure'.
     * @param cmd the commandLine object constructed from the user's options
     * @return true if the commandLine is approved, false otherwise
     */
    public boolean validateArgs(CommandLine cmd) {
        return IntegrationJobValidity.validateArgs(cmd);
    }

    /**
     * Configures an integration job prior to running it; in particular, the fields we need are
     * set from the cmd line and the controlFile contents are deserialized
     * NB: This should be run after 'validateArgs' and before 'run'
     * @param cmd the commandLine object constructed from the user's options
     */
    public void configure(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();

        setDatasetID(cmd.getOptionValue(options.DATASET_ID_FLAG));
        setFileToPublish(cmd.getOptionValue(options.FILE_TO_PUBLISH_FLAG));
        setPublishMethod(PublishMethod.valueOf(cmd.getOptionValue(options.PUBLISH_METHOD_FLAG, "replace")));
        setFileToPublishHasHeaderRow(Boolean.parseBoolean(cmd.getOptionValue(options.HAS_HEADER_ROW_FLAG, "false")));
        setPublishViaFTP(Boolean.parseBoolean(cmd.getOptionValue(options.PUBLISH_VIA_FTP_FLAG, options.DEFAULT_PUBLISH_VIA_FTP)));
        setPublishViaDi2Http(Boolean.parseBoolean(cmd.getOptionValue(options.PUBLISH_VIA_DI2_FLAG, options.DEFAULT_PUBLISH_VIA_DI2)));
        String controlFilePath = cmd.getOptionValue(options.PATH_TO_CONTROL_FILE_FLAG);
        if (controlFilePath == null) controlFilePath = cmd.getOptionValue(options.PATH_TO_FTP_CONTROL_FILE_FLAG);
        setPathToControlFile(controlFilePath);
    }


    /**
     * Runs an integration job. It is expected that 'configure' was run beforehand.
     * @return
     * @throws IOException
     */
    public JobStatus run() throws IOException {
		SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
		UpsertResult result = null;
		JobStatus runStatus = JobStatus.SUCCESS;

        deserializeControlFile();
        JobStatus validationStatus = IntegrationJobValidity.validateJobParams(connectionInfo, this);
        if(validationStatus.isError()) {
			runStatus = validationStatus;
		} else if((publishViaFTP || publishViaDi2Http) && !publishMethod.equals(PublishMethod.replace)) {
            runStatus = JobStatus.PUBLISH_ERROR;
            runStatus.setMessage("FTP and di2/HTTP does not currently support upsert, append or delete");
        } else {
            File fileToPublishFile = new File(fileToPublish);
            // attach a requestId to all Producer API calls (for error tracking purposes)
            String jobRequestId = Utils.generateRequestId();
            final Soda2Producer producer = Soda2Producer.newProducerWithRequestId(
                    connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken(), jobRequestId);
            final SodaImporter importer = SodaImporter.newImporter(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());
            final DeltaImporter2Publisher publisher = new DeltaImporter2Publisher(userPrefs);
            int filesizeChunkingCutoffBytes = userPrefs.getFilesizeChunkingCutoffMB() == null ? 10 * NUM_BYTES_PER_MB :
                    Integer.parseInt(userPrefs.getFilesizeChunkingCutoffMB()) * NUM_BYTES_PER_MB;
            int numRowsPerChunk = userPrefs.getNumRowsPerChunk() == null ? 10000 :
                    Integer.parseInt(userPrefs.getNumRowsPerChunk());

            String publishExceptions = "";
			try {
                 switch (publishMethod) {
                     case upsert:
                     case append:
                         result = doAppendOrUpsertViaHTTP(
                                 producer, importer, fileToPublishFile, filesizeChunkingCutoffBytes, numRowsPerChunk);
                         break;
                     case replace:
                         if (publishViaFTP) {
                             runStatus = doPublishViaFTPv2(fileToPublishFile);
                         } else if (publishViaDi2Http) {
                             runStatus = publisher.publishWithDi2OverHttp(datasetID, fileToPublishFile, controlFile);
                         } else {   // Publish via old HTTP method
                             result = Soda2Publisher.replaceNew(
                                     producer, importer, datasetID, fileToPublishFile, fileToPublishHasHeaderRow);
                         }
                         break;
                     case delete:
                         result = doDeleteViaHTTP(
                                 producer, importer, fileToPublishFile, filesizeChunkingCutoffBytes, numRowsPerChunk);
                         break;
                     default:
                         runStatus = JobStatus.INVALID_PUBLISH_METHOD;
                 }
			}
			catch (IOException | SodaError | InterruptedException e) {
                publishExceptions = e.getMessage();
                e.printStackTrace();
			}
			finally {
                producer.close();
                publisher.close();
			}

            if (publishExceptions.length() > 0) {
                runStatus = JobStatus.PUBLISH_ERROR;
                runStatus.setMessage(publishExceptions);
            } else if (result != null && result.errorCount() > 0) {  // Check for [row-level] SODA 2 errors
                runStatus = craftSoda2PublishError(result);
            }
		}

        if(runStatus.isError()) {
            System.err.print("Job completed with errors: ");
        } else {
            System.out.println("Job completed successfully");
        }

        String logPublishingErrorMessage = logRunResults(runStatus, result);
        emailAdmin(runStatus, logPublishingErrorMessage);
        return runStatus;
	}


    /**
     * Adds an entry to specified log dataset with given job run information
     *
     * @return null if log entry was added successfully, otherwise return an error message as a String
     */
    public static String addLogEntry(final String logDatasetID, final SocrataConnectionInfo connectionInfo,
                                     final IntegrationJob job, final JobStatus status, final UpsertResult result) {
        final Soda2Producer producer = Soda2Producer.newProducer(connectionInfo.getUrl(), connectionInfo.getUser(),
                connectionInfo.getPassword(), connectionInfo.getToken());

        List<Map<String, Object>> upsertObjects = new ArrayList<>();
        Map<String, Object> newCols = new HashMap<>();

        // add standard log data
        Date currentDateTime = new Date();
        newCols.put("Date", currentDateTime);
        newCols.put("DatasetID", job.getDatasetID());
        newCols.put("FileToPublish", job.getFileToPublish());
        newCols.put("PublishMethod", job.getPublishMethod());
        newCols.put("JobFile", job.getPathToSavedFile());
        if(result != null) {
            newCols.put("RowsUpdated", result.rowsUpdated);
            newCols.put("RowsCreated", result.rowsCreated);
            newCols.put("RowsDeleted", result.rowsDeleted);
        } else {
            newCols.put("RowsUpdated", (status.rowsUpdated == null ? 0 : status.rowsUpdated));
            newCols.put("RowsCreated", (status.rowsCreated == null ? 0 : status.rowsCreated));
            newCols.put("RowsDeleted", (status.rowsDeleted == null ? 0 : status.rowsDeleted));
        }
        if(status.isError()) {
            newCols.put("Errors", status.getMessage());
        } else {
            newCols.put("Success", true);
        }
        newCols.put("DataSyncVersion", VersionProvider.getThisVersion());
        upsertObjects.add(ImmutableMap.copyOf(newCols));

        String logPublishingErrorMessage = null;
        try {
            producer.upsert(logDatasetID, upsertObjects);
        }
        catch (SodaError | InterruptedException e) {
            e.printStackTrace();
            logPublishingErrorMessage = e.getMessage();
        }
        return logPublishingErrorMessage;
    }

    private JobStatus doPublishViaFTPv2(File fileToPublishFile) {
        if((pathToControlFile != null && !pathToControlFile.equals(""))) {
            return FTPDropbox2Publisher.publishViaFTPDropboxV2(
                    userPrefs, datasetID, fileToPublishFile, new File(pathToControlFile));
        } else {
            return FTPDropbox2Publisher.publishViaFTPDropboxV2(
                    userPrefs, datasetID, fileToPublishFile, controlFileContent);
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
        UpsertResult result = Soda2Publisher.appendUpsert(
                producer, importer, datasetID, fileToPublishFile, numberOfRows, fileToPublishHasHeaderRow);
        return result;
    }

    private UpsertResult doDeleteViaHTTP(
            Soda2Producer producer, SodaImporter importer, File fileToPublishFile, int filesizeChunkingCutoffBytes, int numRowsPerChunk)
            throws SodaError, InterruptedException, IOException {
        int numberOfRows = numRowsPerChunk(fileToPublishFile, filesizeChunkingCutoffBytes, numRowsPerChunk);
        UpsertResult result = Soda2Publisher.deleteRows(
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

    private JobStatus craftSoda2PublishError(UpsertResult result) {
        JobStatus error = JobStatus.PUBLISH_ERROR;
        if(result != null && result.errorCount() > 0) {
            int lineIndexOffset = (fileToPublishHasHeaderRow) ? 2 : 1;
            String errMsg = "";
            for (UpsertError upsertErr : result.getErrors()) {
                errMsg += upsertErr.getError() + " (line " + (upsertErr.getIndex() + lineIndexOffset) + " of file) \n";
            }
            error.setMessage(errMsg);
        }
        return error;
    }

    private String logRunResults(JobStatus runStatus, UpsertResult result) {
        String logDatasetID = userPrefs.getLogDatasetID();
        String logPublishingErrorMessage = null;
        SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();

        if (logDatasetID != null && !logDatasetID.equals("")) {
            String logDatasetUrl = userPrefs.getDomain() + "/d/" + userPrefs.getLogDatasetID();
            System.out.println("Publishing results to logging dataset (" + logDatasetUrl + ")...");
            logPublishingErrorMessage = addLogEntry(
                    logDatasetID, connectionInfo, this, runStatus, result);
            if (logPublishingErrorMessage != null) {
                System.out.println("Error publishing results to logging dataset (" + logDatasetUrl + "): " +
                        logPublishingErrorMessage);
            }
        }
        return logPublishingErrorMessage;
    }

    private void emailAdmin(JobStatus status, String logPublishingErrorMessage) {
        String adminEmail = userPrefs.getAdminEmail();
        String logDatasetID = userPrefs.getLogDatasetID();
        SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();

        if(userPrefs.emailUponError() && adminEmail != null && !adminEmail.equals("")) {
            sendErrorNotificationEmail(
                    adminEmail, connectionInfo, status, status.getMessage(), logDatasetID, logPublishingErrorMessage);
        }
    }

    private JobStatus deserializeControlFile() {
        if (controlFile != null)
            return JobStatus.VALID;

        JobStatus controlDeserialization = null;
        if (controlFileContent != null && !controlFileContent.equals(""))
            controlDeserialization = deserializeControlFile(controlFileContent);

        if (pathToControlFile != null && !pathToControlFile.equals(""))
            controlDeserialization = deserializeControlFile(new File(pathToControlFile));

        if (controlDeserialization == null) {
            JobStatus noControl = JobStatus.PUBLISH_ERROR;
            noControl.setMessage("You must generate or select a Control file if publishing via FTP SmartUpdate or delta-importer-2 over HTTP");
            return noControl;
        } else if (controlDeserialization.isError()) {
            return controlDeserialization;
        }
        return JobStatus.VALID;
    }


    private JobStatus deserializeControlFile(String contents) {
        try {
            controlFile = controlFileMapper.readValue(contents, ControlFile.class);
            return JobStatus.SUCCESS;
        } catch (Exception e) {
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Unable to interpret control file contents: " + e);
            return status;
        }
    }

    private JobStatus deserializeControlFile(File controlFilePath) {
        try {
            controlFile = controlFileMapper.readValue(controlFilePath, ControlFile.class);
            return JobStatus.SUCCESS;
        } catch (Exception e) {
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Unable to read in and interpret control file contents: " + e);
            return status;
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
                setPathToControlFile(null);
                setControlFileContent(null);
            }
            finally{
                input.close();
            }
        } catch(Exception e) {
            // TODO add log entry?
            throw new IOException(e.toString());
        }
    }

}
