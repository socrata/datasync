package com.socrata.datasync.job;

import java.io.File;
import java.io.IOException;

import com.socrata.api.Soda2Consumer;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.*;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class PortJob implements Job {
    private PortMethod portMethod;
	private String sourceSiteDomain;
	private String sourceSetID;
	private String sinkSiteDomain;
	private String sinkSetID;
	private PublishMethod publishMethod;
    private PublishDataset publishDataset;
	private String portResult;
	private String pathToSavedJobFile;

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 1L;

	private static final String DEFAULT_JOB_NAME = "Untitled Port Job";

	// TODO move this somewhere else (or remove it)
	private static final int DATASET_ID_LENGTH = 9;

	public PortJob() {
		portMethod = PortMethod.copy_all;
		sourceSiteDomain = "https://";
		sourceSetID = "";
		sinkSiteDomain = "https://";
		sinkSetID = "";
		publishMethod = PublishMethod.upsert;
        publishDataset = PublishDataset.working_copy;
		portResult = "";
		pathToSavedJobFile = "";
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
        } catch(IOException e){
            throw new IOException(e.toString());
        }
	}

	public JobStatus validate(SocrataConnectionInfo connectionInfo) {
		if (connectionInfo.getUrl().equals("")
				|| connectionInfo.getUrl().equals("https://")) {
			return JobStatus.INVALID_DOMAIN;
		}
		if (sourceSetID.length() != DATASET_ID_LENGTH) {
			return JobStatus.INVALID_DATASET_ID;
		}
		if (portMethod.equals(PortMethod.copy_data)
				&& sinkSetID.length() != DATASET_ID_LENGTH) {
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
		UserPreferences userPrefs = new UserPreferences();
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
			// creator "creates" a new dataset on the sink site (and publishes
			// if applicable)
			final SodaDdl creator = SodaDdl.newDdl(sinkSiteDomain,
					connectionInfo.getUser(), connectionInfo.getPassword(),
					connectionInfo.getToken());
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
							sourceSetID);
					noPortExceptions = true;
				} else if (portMethod.equals(PortMethod.copy_all)) {
					sinkSetID = PortUtility.portSchema(loader, creator,
							sourceSetID);
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

    @JsonProperty("portResult")
    public String getPortResult() {
        return portResult;
    }

    @JsonProperty("portResult")
    public void setPortResult(String portResult) {
        this.portResult = portResult;
    }

    public String getJobFilename() {
        if (pathToSavedJobFile.equals("")) {
            return DEFAULT_JOB_NAME;
        }
        return new File(pathToSavedJobFile).getName();
    }
}
