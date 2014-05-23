package com.socrata.datasync.job;

public enum Jobs {

    INTEGRATION_JOB("IntegrationJob"),
    PORT_JOB("PortJob"),
    LOAD_PREFERENCES_JOB("LoadPreferences");

    private String description;
    private Jobs(String newDescription) { description = newDescription; }

    public String toString() { return description; }
}
