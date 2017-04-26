package com.socrata.datasync.job;

import com.socrata.api.SodaImporter;
import com.socrata.datasync.*;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.publishers.GISPublisher;
import com.socrata.datasync.validation.GISJobValidity;
import com.socrata.model.importer.Dataset;
import com.socrata.model.importer.GeoDataset;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
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
    private Map<String, String> layerMap = new HashMap<>();

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
        userPrefs = new UserPreferencesJava();

        // first try reading the 'current' format
        ObjectMapper mapper = new ObjectMapper();
        GISJob loadedJob;

        loadedJob = mapper.readValue(new File(pathToFile), GISJob.class);

        loadedJob.setPathToSavedFile(pathToFile);
        setDatasetID(loadedJob.getDatasetID());
        setFileToPublish(loadedJob.getFileToPublish());
        setPublishMethod(loadedJob.getPublishMethod());
        setPathToSavedFile(pathToFile);
        setLayerMap(loadedJob.getLayerMap());

        setUserAgentSijFile();
    }

    @JsonProperty("fileVersionUID")
    public long getFileVersionUID() {
        return fileVersionUID;
    }

    public ControlFile getControlFile() {
        return controlFile;
    }

    public void setControlFile(ControlFile cf) {
        controlFile = cf;
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
        publishMethod = newPublishMethod.replace;
    }

    @JsonProperty("publishMethod")
    public PublishMethod getPublishMethod() {
        return publishMethod;
    }

    @JsonProperty("layerMap")
    public void setLayerMap(Map<String, String> layerMap) {
        this.layerMap = layerMap;
    }

    @JsonProperty("layerMap")
    public Map<String, String> getLayerMap() { return layerMap; }

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
     * Configures a GIS job prior to running it; in particular, the fields we need are
     * set from the cmd line and the controlFile contents are deserialized
     * NB: This should be run after 'validateArgs' and before 'run'
     * @param cmd the commandLine object constructed from the user's options
     */
    public void configure(CommandLine cmd) {
        String method = cmd.getOptionValue(CommandLineOptions.PUBLISH_METHOD_FLAG);
        setDatasetID(cmd.getOptionValue(CommandLineOptions.DATASET_ID_FLAG));
        setFileToPublish(cmd.getOptionValue(CommandLineOptions.FILE_TO_PUBLISH_FLAG));
        if (method != null) {
            setPublishMethod(PublishMethod.valueOf(method));
        }

        String userAgentName = cmd.getOptionValue(CommandLineOptions.USER_AGENT_FLAG);
        if (Utils.nullOrEmpty(userAgentName)) {
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
        String publishExceptions = "";
        JobStatus runStatus = JobStatus.SUCCESS;

        JobStatus validationStatus = GISJobValidity.validateJobParams(connectionInfo, this);
        JobStatus layerMappingStatus = GISJobValidity.validateLayerMapping(this);
        if (validationStatus.isError()) {
            runStatus = validationStatus;
        } else if (layerMappingStatus.isError()) {
            runStatus = layerMappingStatus;
        } else {
            try {
                File fileToPublishFile = new File(fileToPublish);
                if (publishMethod == PublishMethod.replace) {
                    runStatus = GISPublisher.replaceGeo(fileToPublishFile, connectionInfo, datasetID, layerMap, userPrefs);
                } else {
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

    /**
     * This method attempts to map layers in the existing dataset to layers found in a shapefile,
     * so that we replace existing layers where possible instead of creating new ones and changing
     * the 4x4 / API endpoint of the dataset.
     */
    public void initializeLayerMapping() throws URISyntaxException, IOException, HttpException {
        String fileToPublish = getFileToPublish();
        String fileExtension = FilenameUtils.getExtension(fileToPublish);

        if (fileExtension.equals(GISJobValidity.ZIP_EXT)) {
            GeoDataset dataset = DatasetUtils.getDatasetInfo(userPrefs, getDatasetID(), GeoDataset.class);

            // Get map of existing layer UIDs/names by looking at child views
            Map<String, String> existingLayers = getLayerListFromExistingDataset(userPrefs, dataset);
            // Get list of layer names in file by looking at .shp file names inside the zip
            List<String> fileLayers = getLayerListFromShapefile(fileToPublish);

            if (existingLayers.size() == 1 && fileLayers.size() == 1) {
                // If there's one layer in the existing dataset and one layer in the file,
                // assume we want to replace the existing layer.
                for (String key : existingLayers.keySet()) {
                    getLayerMap().put(fileLayers.get(0), existingLayers.get(key));
                }
            } else {
                // Otherwise, make a best effort to match file layers to existing layers by name.
                for (String fileLayer : fileLayers) {
                    if (existingLayers.containsKey(fileLayer)) {
                        getLayerMap().put(fileLayer, existingLayers.get(fileLayer));
                    }
                }
            }
        } else {
            setLayerMap(new HashMap<String, String>());
        }
    }

    private static Map<String, String> getLayerListFromExistingDataset(UserPreferences userPrefs, GeoDataset dataset) {
        List<String> existingLayersUids = dataset.getChildViews();
        Map<String, String> existingLayerInfo = new HashMap<>();

        for (String uid : existingLayersUids) {
            try {
                Dataset child = DatasetUtils.getDatasetInfo(userPrefs, uid, Dataset.class);
                existingLayerInfo.put(child.getName(), uid);
            } catch (Exception e) {
                // there’s no way for the client to recover,
                // so a checked exception is not necessary
                throw new RuntimeException(e);
            }
        }

        return existingLayerInfo;
    }

    private static List<String> getLayerListFromShapefile(String filePath) throws IOException {
        try (ZipFile zipFile = new ZipFile(filePath)) {
            List<String> layers = new ArrayList<>();
            Enumeration entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) entries.nextElement();
                if (FilenameUtils.getExtension(entry.getName()).equals("shp")) {
                    String layerName = FilenameUtils.getBaseName(entry.getName());
                    layers.add(layerName);
                }
            }

            return layers;
        }
    }

    private void sendErrorNotificationEmail(final String adminEmail,
                                            final SocrataConnectionInfo connectionInfo,
                                            final JobStatus runStatus,
                                            final String runErrorMessage,
                                            final String logDatasetID) {
        String errorEmailMessage = "";
        String urlToLogDataset = connectionInfo.getUrl() + "/d/" + logDatasetID;
        if (runStatus.isError()) {
            errorEmailMessage += "There was an error updating a dataset.\n"
                + "\nDataset: " + connectionInfo.getUrl() + "/d/" + getDatasetID()
                + "\nFile to publish: " + fileToPublish
                + "\nFile to publish has header row: " + fileToPublishHasHeaderRow
                + "\nPublish method: " + publishMethod
                + "\nJob File: " + pathToSavedJobFile
                + "\nError message: " + runErrorMessage
                + "\nLog dataset: " + urlToLogDataset + "\n\n";

            try {
                SMTPMailer.send(adminEmail, "Socrata DataSync Error", errorEmailMessage);
            } catch (Exception e) {
                System.out.println("Error sending email to: " + adminEmail + "\n" + e.getMessage());
            }
        }
    }

    private void emailAdmin(JobStatus status) {
        String adminEmail = userPrefs.getAdminEmail();
        String logDatasetID = userPrefs.getLogDatasetID();
        SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();

        if (userPrefs.emailUponError() && adminEmail != null && !adminEmail.equals("")) {
            sendErrorNotificationEmail(
                adminEmail, connectionInfo, status, status.getMessage(), logDatasetID);
        }
    }

    public class ControlDisagreementException extends Exception {
        public ControlDisagreementException(String msg) {
            super(msg);
        }
    }
}
