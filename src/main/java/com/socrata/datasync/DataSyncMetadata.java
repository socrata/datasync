package com.socrata.datasync;

import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Consumer;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.soql.SoqlQuery;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;

import java.util.List;
import java.util.Map;

/**
 * Author: Adrian Laurenzi
 * Date: 3/25/14
 */
public class DataSyncMetadata {
    private static final String DATASYNC_VERSION = "0.4";
    private static final String METADATA_DOMAIN = "https://adrian.demo.socrata.com";
    private static final String METADATA_DATASET_ID = "7w7i-q9n6";

    private DataSyncMetadata() {
        throw new AssertionError("Never instantiate utility classes!");
    }

    public static String getDatasyncVersion() {
        return DATASYNC_VERSION;
    }

    public static String getMetadataDomain() {
        return METADATA_DOMAIN;
    }

    public static String getMetadataDatasetId() {
        return METADATA_DATASET_ID;
    }

    /**
     *  Returns DataSync version metadata info from special Socrata dataset
     *  (e.g. "0.3")
     *
     * @throws com.socrata.exceptions.LongRunningQueryException
     * @throws com.socrata.exceptions.SodaError
     */
    public static Map<String, String> getDataSyncMetadata() throws LongRunningQueryException, SodaError {
        final Soda2Consumer consumer = Soda2Consumer.newConsumer(METADATA_DOMAIN);

        ClientResponse response = consumer.query(METADATA_DATASET_ID, HttpLowLevel.JSON_TYPE, SoqlQuery.SELECT_ALL);
        final List<Object> results = response.getEntity(new GenericType<List<Object>>() {
        });
        return (Map<String, String>) results.get(0);
    }

    /**
     *  Returns true if current JAR/code represents the most recent version of DataSync
     *
     * @throws com.socrata.exceptions.LongRunningQueryException
     * @throws com.socrata.exceptions.SodaError
     */
    public static boolean isLatestVersion(Map<String, String> dataSyncVersionMetadata) {
        String currentVersion = getCurrentVersion(dataSyncVersionMetadata);
        return currentVersion.equals(DATASYNC_VERSION);
    }

    /**
     *  Returns true if current JAR/code represents the most recent *major* version of DataSync
     *  (or has a higher major version), otherwise return false.
     *  A major version is the version immediately before the first period '.' (e.g. if current
     *  version is "0.3", the major version is "0")
     *
     * @throws com.socrata.exceptions.LongRunningQueryException
     * @throws com.socrata.exceptions.SodaError
     */
    public static boolean isLatestMajorVersion(Map<String, String> dataSyncVersionMetadata) {
        String currentVersion = getCurrentVersion(dataSyncVersionMetadata);
        double currentMajorVersion =  Double.parseDouble(getMajorVersion(currentVersion));
        double jarMajorVersion = Double.parseDouble(getMajorVersion(DATASYNC_VERSION));
        return currentMajorVersion <= jarMajorVersion;
    }

    /**
     *
     * @param versionString e.g. "0.3" (to be valid must contain at least one period '.')
     * @return major version (as String) given full version (e.g. given "0.3" returns "0")
     */
    private static String getMajorVersion(String versionString) {
        String[] versionSplit = versionString.split("\\.");
        return  versionSplit[0];
    }

    public static String getCurrentVersion(Map<String, String> dataSyncVersionMetadata) {
        return dataSyncVersionMetadata.get("current_version").toString();
    }

    public static String getCurrentVersionDownloadUrl(Map<String, String> dataSyncVersionMetadata) {
        return dataSyncVersionMetadata.get("current_version_download_url").toString();
    }
}
