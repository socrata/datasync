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

import com.socrata.api.Soda2Consumer;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.JobStatus;
import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PortUtility;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.UserPreferences;

public class PortJob implements Job, Serializable {

	private PortMethod portMethod;
	private Boolean publish;
	private String sourceSiteDomain;
	private String sourceSetID;
	private String sinkSiteDomain;
	private String sinkSetID;
	private PublishMethod publishMethod;
	private String portResult;
	private String pathToSavedJobFile;

	private static final String DEFAULT_JOB_NAME = "Untitled Port Job";

	// TODO move this somewhere else (or remove it)
	private static final int DATASET_ID_LENGTH = 9;

	public PortJob() {
		portMethod = PortMethod.copy_all;
		publish = false;
		sourceSiteDomain = "https://";
		sourceSetID = "";
		sinkSiteDomain = "https://";
		sinkSetID = "";
		publishMethod = PublishMethod.upsert;
		portResult = "";
		pathToSavedJobFile = "";
	}

	/**
	 * Loads port job data from a file and uses the saved data to populate the
	 * fields of this object
	 */
	public PortJob(String pathToFile) {
		try {
			InputStream file = new FileInputStream(pathToFile);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				PortJob loadedJob = (PortJob) input.readObject();
				// Load data into this object
				setPathToSavedFile(loadedJob.getPathToSavedFile());
				setSourceSiteDomain(loadedJob.getSourceSiteDomain());
				setSourceSetID(loadedJob.getSourceSetID());
				setSinkSiteDomain(loadedJob.getSinkSiteDomain());
				setSinkSetID(loadedJob.getSinkSetID());
				setPortMethod(loadedJob.getPortMethod());
				setPublishMethod(loadedJob.getPublishMethod());
				setPublish(loadedJob.getPublish());
			} finally {
				input.close();
			}
		} catch (ClassNotFoundException ex) {
			System.out
					.println("Error loading Port Job: Cannot perform input. Class not found.");
		} catch (IOException ex) {
			System.out.println("Error loading Port Job: Cannot perform input.");
		}
	}

	public String getJobFilename() {
		if (pathToSavedJobFile.equals("")) {
			return DEFAULT_JOB_NAME;
		}
		return new File(pathToSavedJobFile).getName();
	}

	public void setPathToSavedFile(String newPath) {
		pathToSavedJobFile = newPath;
	}

	public String getPathToSavedFile() {
		return pathToSavedJobFile;
	}

	public String getSinkSetID() {
		return sinkSetID;
	}

	public void setSinkSetID(String sinkSetID) {
		this.sinkSetID = sinkSetID;
	}

	public String getSourceSiteDomain() {
		return sourceSiteDomain;
	}

	public void setSourceSiteDomain(String sourceSiteDomain) {
		this.sourceSiteDomain = sourceSiteDomain;
	}

	public String getSourceSetID() {
		return sourceSetID;
	}

	public void setSourceSetID(String sourceSetID) {
		this.sourceSetID = sourceSetID;
	}

	public String getSinkSiteDomain() {
		return sinkSiteDomain;
	}

	public void setSinkSiteDomain(String sinkSiteDomain) {
		this.sinkSiteDomain = sinkSiteDomain;
	}

	public PublishMethod getPublishMethod() {
		return publishMethod;
	}

	public void setPublishMethod(PublishMethod publishMethod) {
		this.publishMethod = publishMethod;
	}

	public PortMethod getPortMethod() {
		return portMethod;
	}

	public void setPortMethod(PortMethod portMethod) {
		this.portMethod = portMethod;
	}

	public Boolean getPublish() {
		return publish;
	}

	public void setPublish(Boolean publish) {
		this.publish = publish;
	}

	public String getPortResult() {
		return portResult;
	}

	public void setPortResult(String portResult) {
		this.portResult = portResult;
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
					PortUtility.portContents(streamExporter, streamUpserter,
							sourceSetID, sinkSetID, publishMethod);
                    noPortExceptions = true;
				} else {
					errorMessage = JobStatus.INVALID_PORT_METHOD.toString();
				}
				try {
					if (publish) {
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
	 * Saves this object as a file at specified location
	 */
	public void writeToFile(String filepath) {
		try {
			OutputStream file = new FileOutputStream(filepath);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(this);
			} finally {
				output.close();
			}
		} catch (IOException ex) {
			System.out.println("Error writing file: " + filepath);
		}
	}

	/**
	 * Implements a custom serialization of a IntegrationJob object.
	 * 
	 * @param out
	 *            the ObjectOutputStream to write to
	 * @throws IOException
	 *             if the stream fails
	 */
	private void writeObject(final ObjectOutputStream out) throws IOException {
		// Write each field to the stream in a specific order.
		// Specifying this order helps shield the class from problems
		// in future versions.
		// The order must be the same as the read order in readObject()
		out.writeObject(pathToSavedJobFile);
		out.writeObject(sourceSiteDomain);
		out.writeObject(sourceSetID);
		out.writeObject(sinkSiteDomain);
		out.writeObject(sinkSetID);
		out.writeObject(portMethod);
		out.writeObject(publishMethod);
		out.writeObject(publish);
	}

	/**
	 * Implements a custom deserialization of an IntegrationJob object.
	 * 
	 * @param in
	 *            the ObjectInputStream to read from
	 * @throws IOException
	 *             if the stream fails
	 * @throws ClassNotFoundException
	 *             if a class is not found
	 */
	private void readObject(final ObjectInputStream in) throws IOException,
			ClassNotFoundException {
		// Read each field from the stream in a specific order.
		// Specifying this order helps shield the class from problems
		// in future versions.
		// The order must be the same as the writing order in writeObject()
		pathToSavedJobFile = (String) in.readObject();
		sourceSiteDomain = (String) in.readObject();
		sourceSetID = (String) in.readObject();
		sinkSiteDomain = (String) in.readObject();
		sinkSetID = (String) in.readObject();
		portMethod = (PortMethod) in.readObject();
		publishMethod = (PublishMethod) in.readObject();
		publish = (Boolean) in.readObject();
	}
}
