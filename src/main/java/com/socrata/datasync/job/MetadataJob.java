package com.socrata.datasync.job;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.api.SodaImporter;
import com.socrata.api.SodaWorkflow;
import com.socrata.datasync.*;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesFile;
import com.socrata.datasync.preferences.UserPreferencesJava;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import com.socrata.model.importer.DatasetInfo;
import com.socrata.model.importer.License;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class MetadataJob implements Job {
	/**
	 * @author Brian Williamson
	 *
	 * Stores a single metadata job that can be opened/run in the GUI
	 * or in command-line mode.
	 */
    private UserPreferences userPrefs;

    private static final int DATASET_ID_LENGTH = 9;

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 1L;

	private String datasetID;
	private String title;
	private String description;
	private String category;
	private List<String> keywords;
	private LicenseType licenseType;
	private String dataProvidedBy;
	private String sourceLink;
	private String contactInfo;
	private String pathToSavedJobFile;
	
	private static final String DEFAULT_JOB_NAME = "Untitled Metadata Job";
    public static final List<String> allowedFileToPublishExtensions = Arrays.asList("csv", "tsv");
	
	public MetadataJob() {
        userPrefs = new UserPreferencesJava();
        setDefaultParams();
	}

    /*
     * This is a method that enables DataSync preferences to be loaded from
     * a .json file instead of Java Preferences class
     */
    public MetadataJob(File preferencesConfigFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            userPrefs = mapper.readValue(preferencesConfigFile, UserPreferencesFile.class);
        } catch (IOException e) {
            // TODO add log entry???
            throw new IOException(e.toString());
        }
        setDefaultParams();
    }
    
    public MetadataJob(UserPreferences userPreferences) {
    	userPrefs = userPreferences;
    	setDefaultParams();
    }

    private void setDefaultParams() {
        pathToSavedJobFile = "";
        datasetID = "";
    	title = "";
    	description = "";
    	category = "";
    	keywords = new ArrayList<String>();
    	//licenseType = "";
    	licenseType = LicenseType.no_license;
    	dataProvidedBy = "";
    	sourceLink = "";
    	contactInfo = "";
    }
	
	/**
	 * Loads metadata job data from a file and
	 * uses the saved data to populate the fields
	 * of this object
	 */
	public MetadataJob(String pathToFile) throws IOException {
        userPrefs = new UserPreferencesJava();

        ObjectMapper mapper = new ObjectMapper();
        try {
            MetadataJob loadedJob = mapper.readValue(new File(pathToFile), MetadataJob.class);
            setDatasetID(loadedJob.getDatasetID());
            setTitle(loadedJob.getTitle());
            setDescription(loadedJob.getDescription());
            setCategory(loadedJob.getCategory());
            setKeywords(loadedJob.getKeywords());
            setLicenseType(LicenseType.getLicenseTypeForValue(loadedJob.getLicenseTypeId()));
            setDataProvidedBy(loadedJob.getDataProvidedBy());
            setSourceLink(loadedJob.getSourceLink());
            setContactInfo(loadedJob.getContactInfo());
            setPathToSavedFile(pathToFile);
        } catch (Exception e) {
        	throw new IOException(e.toString());
        }
	}
	
	/**
	 * 
	 * @return an error JobStatus if any input is invalid, otherwise JobStatus.VALID
	 */
	//TODO: Need to get info on max sizes on fields and validate here.
	public JobStatus validate(SocrataConnectionInfo connectionInfo) {
		
		if(!(connectionInfo.getUrl().startsWith("http://")
				|| connectionInfo.getUrl().startsWith("https://"))) {
			return JobStatus.INVALID_DOMAIN;
		}
		if(datasetID.length() != DATASET_ID_LENGTH) {
			return JobStatus.INVALID_DATASET_ID;
		}
		if(StringUtils.isBlank(title)) {
			return JobStatus.MISSING_METADATA_TITLE;
		}
		
		return JobStatus.VALID;
	}
	
	public JobStatus run() {
		SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
		JobStatus runStatus = JobStatus.SUCCESS;
        String runErrorMessage = null;
		JobStatus validationStatus = validate(connectionInfo);
		//boolean workingCopyCreated = false;
		String workingCopyDatasetId = null;
		
		if(validationStatus.isError()) {
			runStatus = validationStatus;
		} else {
			
			final SodaDdl updater = SodaDdl.newDdl(connectionInfo.getUrl(), connectionInfo.getUser(), 
					connectionInfo.getPassword(), connectionInfo.getToken());
			final SodaWorkflow workflower = SodaWorkflow.newWorkflow(connectionInfo.getUrl(), connectionInfo.getUser(), 
					connectionInfo.getPassword(), connectionInfo.getToken());

			boolean noExceptions = false;
			try {
				DatasetInfo datasetInfo = updater.loadDatasetInfo(datasetID);
				
				if (datasetInfo == null) {
					runErrorMessage = "Dataset with that ID does not exist or you do not have permission to publish to it";
					runStatus = JobStatus.PUBLISH_ERROR;
				}
				else {
					if (datasetInfo.PUBLISHED.equals(datasetInfo.getPublicationStage())) {
						DatasetInfo workingCopyDatasetInfo = workflower.createWorkingCopy(datasetInfo.getId());
						datasetInfo = updater.loadDatasetInfo(workingCopyDatasetInfo.getId());
						workingCopyDatasetId = datasetInfo.getId();
					}				
					
					datasetInfo.setName(title);
					datasetInfo.setDescription(description);
					datasetInfo.setCategory(category); 
					if (keywords != null && !keywords.isEmpty()) {
						datasetInfo.setTags(keywords);
					}
					else {
						datasetInfo.setTags(new ArrayList<String>());
					}
					if (licenseType != null) {
						//TODO: Once issue with setting no license via api is resolved, update below to handle
						if (licenseType == LicenseType.no_license) {
							datasetInfo.setLicenseId(""); //null, "", "''", "\"\"", Tried all of these, no luck, validation errors on all, so 
						}
						else {
							datasetInfo.setLicenseId(licenseType.getValue());
						}
					}
					datasetInfo.setAttribution(dataProvidedBy);
					datasetInfo.setAttributionLink(sourceLink);
					Map<String, Object> privateMetadata = datasetInfo.getPrivateMetadata();
					if (privateMetadata == null) {
						privateMetadata = new HashMap<String, Object>();
					}
					privateMetadata.put("contactEmail", contactInfo);
					
					updater.updateDatasetInfo(datasetInfo);
					
					if (!StringUtils.isBlank(workingCopyDatasetId)) {
						workflower.publish(datasetInfo.getId());
						workingCopyDatasetId = null;
					}
					noExceptions = true;
				}
			} 
			catch (SodaError sodaError) {
                runErrorMessage = sodaError.getMessage();
                runStatus = JobStatus.PUBLISH_ERROR;
			} 
			catch (InterruptedException intrruptException) {
				runErrorMessage = intrruptException.getMessage();
				runStatus = JobStatus.PUBLISH_ERROR;
			}
			catch (Exception other) {
				runErrorMessage = other.toString() + ": " + other.getMessage() + " \r\n " + ExceptionUtils.getStackTrace(other);
				runStatus = JobStatus.PUBLISH_ERROR;
			}
			finally {
				try {
					if (!StringUtils.isBlank(workingCopyDatasetId))	{
						workflower.publish(workingCopyDatasetId);
					}
				}
				catch(Exception e) {
					runErrorMessage += " | Unable to publish dataset after updates";
					runStatus = JobStatus.PUBLISH_ERROR;
				}
			}
		}
		
		String adminEmail = userPrefs.getAdminEmail();
		String logDatasetID = userPrefs.getLogDatasetID();
		JobStatus logStatus = JobStatus.SUCCESS;
		if(!logDatasetID.equals("")) {
            if(runErrorMessage != null)
                runStatus.setMessage(runErrorMessage);
			logStatus = MetadataUtility.addLogEntry(logDatasetID, connectionInfo, this, runStatus);
		}
		//Send email if there was an error updating log or target dataset
		if(userPrefs.emailUponError() && !adminEmail.equals("")) {
			String errorEmailMessage = "";
			String urlToLogDataset = connectionInfo.getUrl() + "/d/" + logDatasetID;
			if(runStatus.isError()) {
				errorEmailMessage += "There was an error updating dataset metadata.\n"
						+ "\nDataset: " + connectionInfo.getUrl() + "/d/" + getDatasetID()
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
        if(runErrorMessage != null)
            runStatus.setMessage(runErrorMessage);

		return runStatus;
	}
	
	public String loadCurrentMetadata() {
		try {
			SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
			final SodaDdl sodaDdl = SodaDdl.newDdl(connectionInfo.getUrl(), connectionInfo.getUser(), 
					connectionInfo.getPassword(), connectionInfo.getToken());	
			readDatasetInfo(sodaDdl.loadDatasetInfo(datasetID));
			return "";
		}
		catch(Exception e) {
			return e.getMessage();
		}
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
    
    @JsonProperty("title")
	public String getTitle() {
		return title;
	}

    @JsonProperty("title")
	public void setTitle(String title) {
		this.title = title;
	}

    @JsonProperty("description")
	public String getDescription() {
		return description;
	}

    @JsonProperty("description")
    public void setDescription(String description) {
		this.description = description;
	}

    @JsonProperty("category")
	public String getCategory() {
		return category;
	}

    @JsonProperty("category")
	public void setCategory(String category) {
		this.category = category;
	}

    @JsonProperty("keywords")
	public List<String> getKeywords() {
		return keywords;
	}

    @JsonProperty("keywords")
	public void setKeywords(List<String> keywords) {
		this.keywords = keywords;
	}

    @JsonProperty("license_type_id")
	public String getLicenseTypeId() {
    	if (licenseType != null) {
    		return licenseType.getValue();
    	}
    	return licenseType.no_license.getValue();
	}
    
    @JsonProperty("license_type_id")
	public void setLicenseTypeId(String licenseTypeId) {
		this.licenseType = LicenseType.getLicenseTypeForValue(licenseTypeId); 
	}
    
    public LicenseType getLicenseType() {
    	return this.licenseType;
    }
    
    public void setLicenseType(LicenseType licenseType) {
    	this.licenseType = licenseType;
    }

    @JsonProperty("data_provided_by")
	public String getDataProvidedBy() {
		return dataProvidedBy;
	}

    @JsonProperty("data_provided_by")
	public void setDataProvidedBy(String dataProvidedBy) {
		this.dataProvidedBy = dataProvidedBy;
	}

    @JsonProperty("source_link")
	public String getSourceLink() {
		return sourceLink;
	}
    
    @JsonProperty("source_link")
	public void setSourceLink(String sourceLink) {
		this.sourceLink = sourceLink;
	}

    @JsonProperty("contact_info")
	public String getContactInfo() {
		return contactInfo;
	}

    @JsonProperty("contact_info")
	public void setContactInfo(String contactInfo) {
		this.contactInfo = contactInfo;
	}
    
    public void setPathToSavedFile(String newPath) {
        pathToSavedJobFile = newPath;
    }

    public String getPathToSavedFile() {
        return pathToSavedJobFile;
    }    

	public String getJobFilename() {
		if(StringUtils.isBlank(pathToSavedJobFile)) {
			return DEFAULT_JOB_NAME;
		}
		return new File(pathToSavedJobFile).getName();
	}
	
	private void readDatasetInfo(DatasetInfo datasetInfo) {
		if (datasetInfo == null) {
			return;
		}
		this.datasetID = datasetInfo.getId();
		this.title = datasetInfo.getName();
		this.description = datasetInfo.getDescription();
		this.category = datasetInfo.getCategory();
		this.keywords = datasetInfo.getTags();
		this.licenseType = LicenseType.getLicenseTypeForValue(datasetInfo.getLicenseId());
		this.dataProvidedBy = datasetInfo.getAttribution();
		this.sourceLink = datasetInfo.getAttributionLink();
		Map<String, Object> privateMetadata = datasetInfo.getPrivateMetadata();
		if (privateMetadata != null) {
			Object contactInfoObj = privateMetadata.get("contactEmail");
			if (contactInfoObj instanceof String) {
				this.contactInfo = (String) contactInfoObj;
			}
		}
	}
}
