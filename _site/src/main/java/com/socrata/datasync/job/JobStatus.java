package com.socrata.datasync.job;

/**
 * @author Adrian Laurenzi
 */
public enum JobStatus {
	/**
     * Job completed
     */
    SUCCESS("Success", false),

    /**
     * Job details are valid
     */
    VALID("Valid", false),

    /**
     * This is when an error is returned from the publish API call.
     * The error message varies so it is set with setMessage()
     */
    PUBLISH_ERROR("", true),

    /**
     * This is when an error is returned upon running a DataPort job.
     * The error message varies so it is set with setMessage()
     */
    PORT_ERROR("", true),

    INVALID_DOMAIN("Invalid Domain", true),

    INVALID_DATASET_ID("Invalid Dataset ID", true),

    MISSING_FILE_TO_PUBLISH("No File To Publish selected", true),

    FILE_TO_PUBLISH_DOESNT_EXIST("File To Publish does not exist", true),

    FILE_TO_PUBLISH_INVALID_FORMAT("Incorrect File Format: File to publish must be a .CSV or .TSV file.", true),

    INVALID_PUBLISH_METHOD("Invalid publish method", true),

    INVALID_PORT_METHOD("Invalid port method", true),

    INVALID_SCHEMAS("Schemas do not match; cannot port rows.", true),

    VERSION_OUT_OF_DATE("Must download new version of DataSync before jobs can be run (critical update)", true),

    MISSING_METADATA_TITLE("Title is Required", true),

    MISSING_COLUMNS("All fields in the dataset must be mapped",true),

    ROWS_DO_NOT_CONTAIN_SAME_NUMBER_OF_COLUMNS("CSV does not contain a consistent number of columns", true),

    INVALID_DATETIME("Cannot parse the datetime values given the current formatting.  Please check your formatting values under advanced options and try again.", true);

    /**
     * The human-readable message to store.
     */
    private String message;

    /**
     * Whether this status indicates an error condition.
     */
    private boolean isError;

    public Integer rowsUpdated;
    public Integer rowsCreated;
    public Integer rowsDeleted;
    public Integer errors;

    /**
     * Private constructor (to prevent re-construction).
     *
     * @param newMessage
     *            the message associated with this enum value
     * @param newIsError
     *            whether this enum represents an error condition
     */

    private JobStatus(final String newMessage, final boolean newIsError) {
        message = newMessage;
        isError = newIsError;
    }



    public void setMessage(final String newMessage) {
    	message = newMessage;
    }

    /**
     * @return a human-readable message for this enum value
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return true if the enum represents an error condition, false otherwise
     */
    public boolean isError() {
        return isError;
    }
}