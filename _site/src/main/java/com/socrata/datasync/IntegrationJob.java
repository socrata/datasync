package com.socrata.datasync;

import com.socrata.datasync.PublishMethod;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/**
 * ******************************************************
 * ******************************************************
 * ************ IMPORTANT NOTICE PLEASE READ ************
 * ******************************************************
 *
 * The purpose of this class is solely for backwards compatability for
 * opening old .sij files (saved from DataSync 0.2 or earlier). The
 * "real"/current implementation of this class is:
 * com.socrata.datasync.job.IntegrationJob
 *
 * ******************************************************
 * ******************************************************
 * ******************************************************
 */
public class IntegrationJob implements Serializable {
    /**
     * Part of serializability, this id tracks if a serialized object can be
     * deserialized using this version of the class.
     */
    private static final long serialVersionUID = 2L;

    private String datasetID;
    private String fileToPublish;
    private PublishMethod publishMethod;
    private String fileRowsToDelete;
    private String pathToSavedJobFile;

    public String getDatasetID() {
        return datasetID;
    }

    public String getFileToPublish() {
        return fileToPublish;
    }

    public PublishMethod getPublishMethod() {
        return publishMethod;
    }

    public String getFileRowsToDelete() {
        return fileRowsToDelete;
    }

    public String getPathToSavedFile() {
        return pathToSavedJobFile;
    }

    /**
     * Implements a custom deserialization of an IntegrationJob object.
     *
     * @param in
     *            the ObjectInputStream to read from
     * @throws java.io.IOException
     *             if the stream fails
     * @throws ClassNotFoundException
     *             if a class is not found
     */
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
        // Read each field from the stream in a specific order.
        // Specifying this order helps shield the class from problems
        // in future versions.
        // The order must be the same as the writing order in writeObject()
        pathToSavedJobFile = (String) in.readObject();
        datasetID = (String) in.readObject();
        fileToPublish = (String) in.readObject();
        publishMethod = (PublishMethod) in.readObject();
        fileRowsToDelete = (String) in.readObject();
    }
}
