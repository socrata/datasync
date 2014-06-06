package com.socrata.datasync.job;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.socrata.api.Soda2Consumer;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.Utils;
import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PortUtility;
import com.socrata.datasync.PublishDataset;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;

import org.apache.commons.cli.CommandLine;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class PortJob extends Job {
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


    public boolean validateArgs(CommandLine cmd) {
        // TODO Validate optional parameters
        return validatePortMethodArg(cmd) &&
                validateSourceDomainArg(cmd) &&
                validateSourceIdArg(cmd) &&
                validateDestinationDomainArg(cmd);
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

	/**
	 * Loads port job data from a file and uses the saved data to populate the
	 * fields of this object
	 */
	public PortJob(String pathToFile) throws IOException {
        // first try reading the 'current' format
        ObjectMapper mapper = new ObjectMapper();
        // if reading new format fails...try reading old format into this object
        try {
            PortJob loadedJob = mapper.readValue(new File(pathToFile), PortJob.class);
            setPathToSavedFile(loadedJob.getPathToSavedFile());
            setSourceSiteDomain(loadedJob.getSourceSiteDomain());
            setSourceSetID(loadedJob.getSourceSetID());
            setSinkSiteDomain(loadedJob.getSinkSiteDomain());
            setSinkSetID(loadedJob.getSinkSetID());
            setPortMethod(loadedJob.getPortMethod());
            setPublishMethod(loadedJob.getPublishMethod());
            setPublishDataset(loadedJob.getPublishDataset());
            setDestinationDatasetTitle(loadedJob.getDestinationDatasetTitle());
        } catch(IOException e){
            throw new IOException(e.toString());
        }
	}

	public JobStatus validate(SocrataConnectionInfo connectionInfo) {
		if (connectionInfo.getUrl().equals("")
				|| connectionInfo.getUrl().equals("https://")) {
			return JobStatus.INVALID_DOMAIN;
		}
		if (!Utils.uidIsValid(sourceSetID)) {
			return JobStatus.INVALID_DATASET_ID;
		}
		if (portMethod.equals(PortMethod.copy_data)
				&& !Utils.uidIsValid(sinkSetID)) {
			return JobStatus.INVALID_DATASET_ID;
		}

		if (sourceSiteDomain.equals("") || sourceSiteDomain.equals("https://")) {
			return JobStatus.INVALID_DOMAIN;
		}
		if (sinkSiteDomain.equals("") || sinkSiteDomain.equals("https://")) {
			return JobStatus.INVALID_DOMAIN;
		}
		if (!portMethod.equals(PortMethod.copy_all)
				&& !portMethod.equals(PortMethod.copy_schema)
				&& !portMethod.equals(PortMethod.copy_data)) {
			return JobStatus.INVALID_PORT_METHOD;
		}
		if (!publishMethod.equals(PublishMethod.upsert)
				&& !publishMethod.equals(PublishMethod.replace)) {
			return JobStatus.INVALID_PUBLISH_METHOD;
		}
		return JobStatus.SUCCESS;
	}

	public JobStatus run() {
		SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();

		JobStatus runStatus;
		JobStatus validationStatus = validate(connectionInfo);
		if (validationStatus.isError()) {
			runStatus = validationStatus;
		} else {
			// loader "loads" the source dataset metadata and schema
			final SodaDdl loader = SodaDdl.newDdl(sourceSiteDomain,
					connectionInfo.getUser(), connectionInfo.getPassword(),
					connectionInfo.getToken());

            // special feature to enable porting datasets to Staging (where app token is different)
            String portDestinationDomainAppToken = connectionInfo.getToken();
            if(userPrefs.getPortDestinationDomainAppToken() != null && !userPrefs.getPortDestinationDomainAppToken().equals("")) {
                portDestinationDomainAppToken = userPrefs.getPortDestinationDomainAppToken();
            }

			// creator "creates" a new dataset on the sink site (and publishes
			// if applicable)
            final SodaDdl creator = SodaDdl.newDdl(sinkSiteDomain,
					connectionInfo.getUser(), connectionInfo.getPassword(),
                    portDestinationDomainAppToken);
			// streamExporter "exports" the source dataset rows
			final Soda2Consumer streamExporter = Soda2Consumer.newConsumer(
					sourceSiteDomain, connectionInfo.getUser(),
					connectionInfo.getPassword(), connectionInfo.getToken());
			// streamUpserter "upserts" the rows exported to the created dataset
			final Soda2Producer streamUpserter = Soda2Producer.newProducer(
					sinkSiteDomain, connectionInfo.getUser(),
					connectionInfo.getPassword(), portDestinationDomainAppToken);
			String errorMessage = "";
			boolean noPortExceptions = false;
			try {
				if (portMethod.equals(PortMethod.copy_schema)) {
					sinkSetID = PortUtility.portSchema(loader, creator,
                            sourceSetID, destinationDatasetTitle, userPrefs.getUseNewBackend());
					noPortExceptions = true;
				} else if (portMethod.equals(PortMethod.copy_all)) {
					sinkSetID = PortUtility.portSchema(loader, creator,
							sourceSetID, destinationDatasetTitle, userPrefs.getUseNewBackend());
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
		}
		return runStatus;
	}

    private boolean validateDestinationDomainArg(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        if(cmd.getOptionValue(options.DESTINATION_DOMAIN_FLAG) == null) {
            System.err.println("Missing required argument: -pd2,--" + options.DESTINATION_DOMAIN_FLAG + " is required");
            return false;
        }
        return true;
    }

    private boolean validateSourceIdArg(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        if(cmd.getOptionValue(options.SOURCE_DATASET_ID_FLAG) == null) {
            System.err.println("Missing required argument: -pi1,--" + options.SOURCE_DATASET_ID_FLAG + " is required");
            return false;
        }
        return true;
    }

    private boolean validateSourceDomainArg(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        if(cmd.getOptionValue(options.SOURCE_DOMAIN_FLAG) == null) {
            System.err.println("Missing required argument: -pd1,--" + options.SOURCE_DOMAIN_FLAG + " is required");
            return false;
        }
        return true;
    }

    private boolean validatePortMethodArg(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        String portMethod = cmd.getOptionValue(options.PORT_METHOD_FLAG);
        if(portMethod == null) {
            System.err.println("Missing required argument: -pm,--" + options.PORT_METHOD_FLAG + " is required");
            return false;
        }

        boolean portMethodValid = false;
        for(PortMethod m : PortMethod.values()) {
            if(portMethod.equals(m.name()))
                portMethodValid = true;
        }
        if(!portMethodValid) {
            System.err.println("Invalid argument: -pm,--" + options.PORT_METHOD_FLAG + " must be: " +
                    Arrays.toString(PortMethod.values()));
            return false;
        }
        return true;
    }
}
