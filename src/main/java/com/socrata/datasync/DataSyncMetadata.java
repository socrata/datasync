package com.socrata.datasync;

/**
 * Author: Adrian Laurenzi
 * Date: 3/25/14
 */
public class DataSyncMetadata {
    private static final String VERSION = "0.3";
    private static final String METADATA_DOMAIN = "https://adrian.demo.socrata.com";
    private static final String METADATA_DATASET_ID = "7w7i-q9n6";

    private DataSyncMetadata() {
        throw new AssertionError("Never instantiate utility classes!");
    }

    public static String getVersion() {
        return VERSION;
    }

    public static String getMetadataDomain() {
        return METADATA_DOMAIN;
    }

    public static String getMetadataDatasetId() {
        return METADATA_DATASET_ID;
    }
}
