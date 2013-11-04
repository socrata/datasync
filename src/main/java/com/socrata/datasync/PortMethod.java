package com.socrata.datasync;

public enum PortMethod {
	copy_data("Copy data only"),
	
	copy_schema("Copy schema only"),

    copy_all("Copy schema and data");

    /**
     * The human-readable message to store.
     */
    private String humanReadableValue;

    /**
     * Private constructor (to prevent re-construction).
     *
     * @param newHumanReadableValue
     *            the message associated with this enum value
     */
    private PortMethod(final String newHumanReadableValue) {
        humanReadableValue = newHumanReadableValue;
    }

    public String toString() {
        return humanReadableValue;
    }
}

