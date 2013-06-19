package com.socrata.datasync;

/**
 * @author Adrian Laurenzi
 */
public enum JobStatus {
	/**
     * Job completed 
     */
    SUCCESS("Success", false),
    
    /**
     * This is when an error is returned from the publish API call.
     * The error message varies so it is set with setMessage()
     */
    PUBLISH_ERROR("", true),
    
    INVALID_DOMAIN("Invalid Domain", true),
    
    INVALID_DATASET_ID("Invalid Dataset ID", true),
    
    MISSING_FILE_TO_PUBLISH("No File To Publish selected", true),

    FILE_TO_PUBLISH_DOESNT_EXIST("File To Publish does not exist", true),
    
    INVALID_PUBLISH_METHOD("Invalid publish method", true);
    
    /**
     * The human-readable message to store.
     */
    private String message;

    /**
     * Whether this status indicates an error condition.
     */
    private boolean isError;

    /**
     * Private constructor (to prevent re-construction).
     * 
     * @param message
     *            the message associated with this enum value
     * @param isError
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