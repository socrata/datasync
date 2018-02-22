package com.socrata.datasync.job;

import com.socrata.api.Soda2Consumer;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.DatasetUtils;
import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PortUtility;
import com.socrata.datasync.PublishDataset;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.Utils;
import com.socrata.datasync.config.controlfile.PortControlFile;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.publishers.DeltaImporter2Publisher;
import com.socrata.datasync.validation.PortJobValidity;
import org.apache.commons.cli.CommandLine;
import org.apache.http.HttpException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class PortJob extends Job {
    static AtomicInteger jobCounter = new AtomicInteger(0);
    int jobNum = jobCounter.getAndIncrement();
    private String defaultJobName = "Unsaved Port Job" + " (" + jobNum + ")";
    private UserPreferences userPrefs;

    private PortMethod portMethod = PortMethod.copy_all;
    private String sourceSiteDomain ="https://";
    private String sourceSetID = "";
    private String sinkSiteDomain= "https://";
    private String sinkSetID = "";
    private PublishMethod publishMethod = PublishMethod.upsert;
    private PublishDataset publishDataset = PublishDataset.working_copy;
    private String portResult = "";
    private String destinationDatasetTitle = "";
    private boolean useNewBackend = false;


    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 2L;

    private static final String DEFAULT_JOB_NAME = "Untitled Port Job";

    public PortJob() {
        userPrefs = new UserPreferencesJava();
    }

    /*
     * This is a method that enables DataSync preferences to be established
     * directly when DataSync is used in "library mode" or "command-line mode"
     * (rather than being loaded from Preferences class)
     */
    public PortJob(UserPreferences userPrefs) {
        this.userPrefs = userPrefs;
    }

    @JsonProperty("fileVersionUID")
    public long getFileVersionUID() {
        return fileVersionUID;
    }

    @JsonProperty("sinkSetID")
    public String getSinkSetID() {
        return sinkSetID;
    }

    @JsonProperty("sinkSetID")
    public void setSinkSetID(String sinkSetID) {
        this.sinkSetID = sinkSetID;
    }

    @JsonProperty("sourceSiteDomain")
    public String getSourceSiteDomain() {
        return sourceSiteDomain;
    }

    @JsonProperty("sourceSiteDomain")
    public void setSourceSiteDomain(String sourceSiteDomain) {
        this.sourceSiteDomain = sourceSiteDomain;
    }

    @JsonProperty("sourceSetID")
    public String getSourceSetID() {
        return sourceSetID;
    }

    @JsonProperty("sourceSetID")
    public void setSourceSetID(String sourceSetID) {
        this.sourceSetID = sourceSetID;
    }

    @JsonProperty("sinkSiteDomain")
    public String getSinkSiteDomain() {
        return sinkSiteDomain;
    }

    @JsonProperty("sinkSiteDomain")
    public void setSinkSiteDomain(String sinkSiteDomain) {
        this.sinkSiteDomain = sinkSiteDomain;
    }

    @JsonProperty("publishMethod")
    public PublishMethod getPublishMethod() {
        return publishMethod;
    }

    @JsonProperty("publishMethod")
    public void setPublishMethod(PublishMethod publishMethod) {
        this.publishMethod = publishMethod;
    }

    @JsonProperty("portMethod")
    public PortMethod getPortMethod() {
        return portMethod;
    }

    @JsonProperty("portMethod")
    public void setPortMethod(PortMethod portMethod) {
        this.portMethod = portMethod;
    }

    @JsonProperty("publishDataset")
    public PublishDataset getPublishDataset() {
        return publishDataset;
    }

    @JsonProperty("publishDataset")
    public void setPublishDataset(PublishDataset publishDataset) {
        this.publishDataset = publishDataset;
    }

    @JsonProperty("destinationDatasetTitle")
    public String getDestinationDatasetTitle() {
        return destinationDatasetTitle;
    }

    @JsonProperty("destinationDatasetTitle")
    public void setDestinationDatasetTitle(String destinationDatasetTitle) {
        this.destinationDatasetTitle = destinationDatasetTitle;
    }

    @JsonProperty("portResult")
    public String getPortResult() {
        return portResult;
    }

    @JsonProperty("portResult")
    public void setPortResult(String portResult) {
        this.portResult = portResult;
    }

    @JsonProperty("useNewBackend")
    public boolean getUseNewBackend() {
        return useNewBackend;
    }

    @JsonProperty("useNewBackend")
    public void setUseNewBackend(boolean useNewBackend) {
        this.useNewBackend = useNewBackend;
    }

    public String getDefaultJobName() { return defaultJobName; }

    public boolean validateArgs(CommandLine cmd) {
        return PortJobValidity.validateArgs(cmd);
    }

    /**
     * Loads port job data from a file and uses the saved data to populate the
     * fields of this object
     */
    public PortJob(String pathToFile) throws IOException {
        userPrefs = new UserPreferencesJava();

        // first try reading the 'current' format
        ObjectMapper mapper = new ObjectMapper();
        // if reading new format fails...try reading old format into this object
        try {
            PortJob loadedJob = mapper.readValue(new File(pathToFile), PortJob.class);
            loadedJob.setPathToSavedFile(pathToFile);
            setPathToSavedFile(loadedJob.getPathToSavedFile());
            setSourceSiteDomain(loadedJob.getSourceSiteDomain());
            setSourceSetID(loadedJob.getSourceSetID());
            setSinkSiteDomain(loadedJob.getSinkSiteDomain());
            setSinkSetID(loadedJob.getSinkSetID());
            setPortMethod(loadedJob.getPortMethod());
            setPublishMethod(loadedJob.getPublishMethod());
            setPublishDataset(loadedJob.getPublishDataset());
            setDestinationDatasetTitle(loadedJob.getDestinationDatasetTitle());
            setUseNewBackend(loadedJob.getUseNewBackend());
        } catch(IOException e){
            throw new IOException(e.toString());
        }
    }

    // TODO: when get around to fixing up cmd line options, should take out the hard-coding here.
    public void configure(CommandLine cmd) {
        setPortMethod(PortMethod.valueOf(cmd.getOptionValue("pm")));
        setSourceSiteDomain(cmd.getOptionValue("pd1"));
        setSourceSetID(cmd.getOptionValue("pi1"));
        setSinkSiteDomain(cmd.getOptionValue("pd2"));

        if (cmd.getOptionValue("pi2") != null)
            setSinkSetID(cmd.getOptionValue("pi2"));
        if (cmd.getOptionValue("ppm") != null)
            setPublishMethod(PublishMethod.valueOf(cmd.getOptionValue("ppm")));
        if (cmd.getOptionValue("pp") != null) {
            if (cmd.getOptionValue("pp").equalsIgnoreCase("true")) {
                setPublishDataset(PublishDataset.publish);
            } else { // cmd.getOptionValue("pp") == "false"
                setPublishDataset(PublishDataset.working_copy);
            }
        }
        if (cmd.getOptionValue("pdt") != null)
            setDestinationDatasetTitle(cmd.getOptionValue("pdt"));
    }



    public JobStatus run() {
        SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();

        JobStatus runStatus;
        JobStatus validationStatus = PortJobValidity.validateJobParams(connectionInfo, this);
        if (validationStatus.isError()) {
            runStatus = validationStatus;
        } else {
            boolean useOldCodePath;
            try {
                useOldCodePath = !Utils.regionOfDomain(userPrefs, sourceSiteDomain).equals(Utils.regionOfDomain(userPrefs, sinkSiteDomain));
            } catch(URISyntaxException | IOException e) {
                runStatus = JobStatus.PORT_ERROR;
                runStatus.setMessage(e.getMessage());
                return runStatus;
            }

            // loader "loads" the source dataset metadata and schema
            final SodaDdl loader = SodaDdl.newDdl(sourceSiteDomain,
                                                  connectionInfo.getUser(), connectionInfo.getPassword(),
                                                  connectionInfo.getToken());

            // creator "creates" a new dataset on the sink site (and publishes if applicable)
            final SodaDdl creator = SodaDdl.newDdl(sinkSiteDomain,
                                                   connectionInfo.getUser(), connectionInfo.getPassword(),
                                                   connectionInfo.getToken());


            if(useOldCodePath) {
                // streamExporter "exports" the source dataset rows
                final Soda2Consumer streamExporter = Soda2Consumer.newConsumer(
                                                                               sourceSiteDomain, connectionInfo.getUser(),
                                                                               connectionInfo.getPassword(), connectionInfo.getToken());
                // streamUpserter "upserts" the rows exported to the created dataset
                final Soda2Producer streamUpserter = Soda2Producer.newProducer(
                                                                               sinkSiteDomain, connectionInfo.getUser(),
                                                                               connectionInfo.getPassword(), connectionInfo.getToken());
                String errorMessage = "";
                boolean noPortExceptions = false;
                try {
                    if (portMethod.equals(PortMethod.copy_schema)) {
                        sinkSetID = PortUtility.portSchema(loader, creator,
                                                           sourceSetID, destinationDatasetTitle, useNewBackend);
                        noPortExceptions = true;
                    } else if (portMethod.equals(PortMethod.copy_all)) {
                        sinkSetID = PortUtility.portSchema(loader, creator,
                                                           sourceSetID, destinationDatasetTitle, useNewBackend);
                        PortUtility.portContents(streamExporter, streamUpserter,
                                                 sourceSetID, sinkSetID, PublishMethod.upsert);
                        noPortExceptions = true;
                    } else if (portMethod.equals(PortMethod.copy_data)) {
                        JobStatus schemaCheck = PortUtility.assertSchemasAreAlike(loader, creator, sourceSetID, sinkSetID);
                        if (schemaCheck.isError()) {
                            errorMessage = schemaCheck.getMessage();
                        } else {
                            PortUtility.portContents(streamExporter, streamUpserter,
                                                     sourceSetID, sinkSetID, publishMethod);
                            noPortExceptions = true;
                        }
                    } else {
                        errorMessage = JobStatus.INVALID_PORT_METHOD.toString();
                    }
                    try {
                        if (publishDataset.equals(PublishDataset.publish)) {
                            sinkSetID = PortUtility.publishDataset(creator,
                                                                   sinkSetID);
                        }
                    } catch (Exception publishE) {
                        errorMessage += "\n" + publishE.getMessage();
                    }
                } catch (Exception exception) {
                    errorMessage = exception.getMessage();
                } finally {
                    if (noPortExceptions) {
                        // TODO (maybe) more DataPort error checking...?
                        runStatus = JobStatus.SUCCESS;
                    } else {
                        runStatus = JobStatus.PORT_ERROR;
                        runStatus.setMessage(errorMessage);
                    }
                }
            } else {
                try {
                    if (portMethod.equals(PortMethod.copy_schema)) {
                        sinkSetID = PortUtility.portSchema(loader, creator,
                                                           sourceSetID, destinationDatasetTitle, useNewBackend);
                    } else if (portMethod.equals(PortMethod.copy_all)) {
                        sinkSetID = PortUtility.portSchema(loader, creator,
                                                           sourceSetID, destinationDatasetTitle, useNewBackend);
                    } else if (portMethod.equals(PortMethod.copy_data)) {
                        JobStatus schemaCheck = PortUtility.assertSchemasAreAlike(loader, creator, sourceSetID, sinkSetID);
                        if (schemaCheck.isError()) {
                            runStatus = JobStatus.PORT_ERROR;
                            runStatus.setMessage(schemaCheck.getMessage());
                            return runStatus;
                        }
                    } else {
                        runStatus = JobStatus.PORT_ERROR;
                        runStatus.setMessage(JobStatus.INVALID_PORT_METHOD.toString());
                        return runStatus;
                    }

                    PortControlFile control = new PortControlFile(new URI("https://" + DatasetUtils.getDomainWithoutScheme(sourceSiteDomain)).getHost(),
                                                                  sourceSetID,
                                                                  destinationDatasetTitle,
                                                                  portMethod,
                                                                  publishDataset.equals(PublishDataset.publish));

                    // ok, what we need to do is send the JSONized control
                    // file to di2, get a job ID back, and poll the status in
                    // exactly the manner of all other di2 jobs

                    DeltaImporter2Publisher publisher = new DeltaImporter2Publisher(userPrefs, "fixme");
                    runStatus = publisher.copyWithDi2(sinkSetID, control);
                    if(runStatus == JobStatus.SUCCESS) {
                        // Urrrrghghghgh
                        Pattern p = Pattern.compile("The new dataset id is (....-....)");
                        Matcher m = p.matcher(runStatus.getMessage());
                        if(m.find()) {
                            sinkSetID = m.group(1);
                        } else {
                            runStatus = JobStatus.PORT_ERROR;
                            runStatus.setMessage("Unable to find newly-created dataset");
                        }
                    }
                } catch(Exception e) {
                    runStatus = JobStatus.PORT_ERROR;
                    runStatus.setMessage(e.getMessage());
                }
            }
        }
        return runStatus;
    }
}
