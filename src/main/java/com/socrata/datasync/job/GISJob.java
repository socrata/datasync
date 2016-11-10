package com.socrata.datasync.job;

import com.socrata.api.SodaImporter;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SMTPMailer;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.Utils;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.publishers.GISPublisher;
import com.socrata.datasync.validation.GISJobValidity;
import com.socrata.model.importer.GeoDataset;


import org.apache.commons.cli.CommandLine;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class GISJob extends Job {

    static AtomicInteger jobCounter = new AtomicInteger(0);
    int jobNum = jobCounter.getAndIncrement();
    private String defaultJobName = "Unsaved GIS Job" + " (" + jobNum + ")";

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 4L;

    private UserPreferences userPrefs;
    private String datasetID = "";
	private String fileToPublish = "";
	private PublishMethod publishMethod = PublishMethod.replace;
    private boolean fileToPublishHasHeaderRow = true;
    private ControlFile controlFile = null;

    private String userAgent = "datasync";

    private String userAgentNameClient = "Client";
    private String userAgentNameCli = "CLI";
    private String userAgentNameSijFile = ".gij File";
    

    private ObjectMapper controlFileMapper =
            new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    public GISJob() {
        userPrefs = new UserPreferencesJava();
	}

    /*
     * This is a method that enables DataSync preferences to be established
     * directly when DataSync is used in "library mode" or "command-line mode"
     * (rather than being loaded from Preferences class)
     */
    public GISJob(UserPreferences userPrefs) {
        this.userPrefs = userPrefs;
    }

    /**
	 * Loads GIS job data from a file and
	 * uses the saved data to populate the fields
	 * of this object
	 */
	public GISJob(String pathToFile) throws IOException, ControlDisagreementException {
        this(pathToFile, false);
        setUserAgentSijFile();
	}

    /**
     * Loads GIS job data from a file and
     * uses the saved data to populate the fields
     * of this object
     */
    public GISJob(String pathToFile, boolean ignoreControlInconsistencies) throws IOException, ControlDisagreementException {
        userPrefs = new UserPreferencesJava();

        // first try reading the 'current' format
        ObjectMapper mapper = new ObjectMapper();
        GISJob loadedJob;
        try {
            loadedJob = mapper.readValue(new File(pathToFile), GISJob.class);
        } catch (IOException e) {
            // if reading new format fails...try reading old format into this object
            loadOldSijFile(pathToFile);
            return;
        }
        setDatasetID(loadedJob.getDatasetID());
        setFileToPublish(loadedJob.getFileToPublish());
        setPublishMethod(loadedJob.getPublishMethod());
        setPathToSavedFile(pathToFile);
    }


    @JsonProperty("fileVersionUID")
    public long getFileVersionUID() { return fileVersionUID; }

    public ControlFile getControlFile() { return controlFile; }

    public void setControlFile(ControlFile cf) { controlFile = cf; }

    @JsonProperty("datasetID")
    public void setDatasetID(String newDatasetID) { datasetID = newDatasetID; }

    @JsonProperty("datasetID")
    public String getDatasetID() {
        return datasetID;
    }

    @JsonProperty("fileToPublish")
    public void setFileToPublish(String newFileToPublish) { fileToPublish = newFileToPublish; }

    @JsonProperty("fileToPublish")
    public String getFileToPublish() {
        return fileToPublish;
    }

    @JsonProperty("publishMethod")
    public void setPublishMethod(PublishMethod newPublishMethod) {
        publishMethod = newPublishMethod.replace;
    }

    @JsonProperty("publishMethod")
    public PublishMethod getPublishMethod() {
        return publishMethod;
    }

    public String getDefaultJobName() { return defaultJobName; }

    public void setUserAgent(String usrAgentName) {
        userAgent = Utils.getUserAgentString(usrAgentName);
    }
    public void setUserAgentClient() {
        userAgent = Utils.getUserAgentString(userAgentNameClient);
    }
    public void setUserAgentSijFile() {
        userAgent = Utils.getUserAgentString(userAgentNameSijFile);
    }

    /**
     * Checks that the command line arguments are sensible
     * NB: it is expected that this is run before 'configure'.
     * @param cmd the commandLine object constructed from the user's options
     * @return true if the commandLine is approved, false otherwise
     */
    public boolean validateArgs(CommandLine cmd) {
        return GISJobValidity.validateArgs(cmd);
    }

    /**
     * Configures an integration job prior to running it; in particular, the fields we need are
     * set from the cmd line and the controlFile contents are deserialized
     * NB: This should be run after 'validateArgs' and before 'run'
     * @param cmd the commandLine object constructed from the user's options
     */
    public void configure(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        String method = cmd.getOptionValue(options.PUBLISH_METHOD_FLAG);
        setDatasetID(cmd.getOptionValue(options.DATASET_ID_FLAG));
        setFileToPublish(cmd.getOptionValue(options.FILE_TO_PUBLISH_FLAG));
        if(method != null)
            setPublishMethod(PublishMethod.valueOf(method));
        String userAgentName = cmd.getOptionValue(options.USER_AGENT_FLAG);
        if(Utils.nullOrEmpty(userAgentName)) {
            userAgentName = userAgentNameCli;
        }
        setUserAgent(userAgentName);
    }


    /**
     * Runs an GIS job. It is expected that 'configure' was run beforehand.
     * @return
     * @throws IOException
     */
    public JobStatus run() {
		SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
		GeoDataset result = null;
        String publishExceptions = "";
        JobStatus runStatus = JobStatus.SUCCESS;

        JobStatus validationStatus = GISJobValidity.validateJobParams(connectionInfo, this);
        JobStatus datasetStatus = GISJobValidity.validateDatasetDomain(userPrefs, getDatasetID());
        if (validationStatus.isError()) {
            runStatus = validationStatus;
        } else if (datasetStatus.isError()) {
            runStatus = datasetStatus;
        } else {

            try {
                File fileToPublishFile = new File(fileToPublish);
                    // attach a requestId to all Producer API calls (for error tracking purposes)
                    String jobRequestId = Utils.generateRequestId();
                    final SodaImporter importer = SodaImporter.newImporter(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());

                    switch (publishMethod) {
                        case replace:
                            runStatus = GISPublisher.replaceGeo(fileToPublishFile, connectionInfo, datasetID, userPrefs);
                            break;
                        default:
                            runStatus = JobStatus.INVALID_PUBLISH_METHOD;
                    
                    }

            } catch (Exception e) {
                publishExceptions = e.getMessage();
                e.printStackTrace();
            }
        }
    

        if (publishExceptions.length() > 0) {
            runStatus = JobStatus.PUBLISH_ERROR;
            runStatus.setMessage(publishExceptions);
        }
        emailAdmin(runStatus);
        return runStatus;
	}

    private void sendErrorNotificationEmail(final String adminEmail, final SocrataConnectionInfo connectionInfo, final JobStatus runStatus, final String runErrorMessage, final String logDatasetID) {
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
        if(runStatus.isError()) {
            try {
                SMTPMailer.send(adminEmail, "Socrata DataSync Error", errorEmailMessage);
            } catch (Exception e) {
                System.out.println("Error sending email to: " + adminEmail + "\n" + e.getMessage());
            }
        }
    }

    private String logRunResults(JobStatus runStatus, GeoDataset result) {
        String logDatasetID = userPrefs.getLogDatasetID();
        String logPublishingErrorMessage = null;
        SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
        return logPublishingErrorMessage;
    }

    private void emailAdmin(JobStatus status) {
        String adminEmail = userPrefs.getAdminEmail();
        String logDatasetID = userPrefs.getLogDatasetID();
        SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();

        if(userPrefs.emailUponError() && adminEmail != null && !adminEmail.equals("")) {
            sendErrorNotificationEmail(
                    adminEmail, connectionInfo, status, status.getMessage(), logDatasetID);
        }
    }

    public class ControlDisagreementException extends Exception {
        public ControlDisagreementException(String msg) {
            super(msg);
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
