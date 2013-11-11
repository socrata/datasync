package com.socrata.datasync;

public enum PublishDataset {
    publish("Publish Dataset"),

    working_copy("Create Working Copy");

    /**
     * The human-readable message to store.
     */
    private String humanReadableValue;

    /**
     * Private constructor (to prevent re-construction).
     *
     * @param newHumanReadableValue
     * the message associated with this enum value
     */
    private PublishDataset(final String newHumanReadableValue) {
        humanReadableValue = newHumanReadableValue;
    }

    public String toString() {
        return humanReadableValue;
    }
}
