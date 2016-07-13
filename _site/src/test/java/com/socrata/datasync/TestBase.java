package com.socrata.datasync;

import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Consumer;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesFile;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.sun.jersey.api.client.ClientResponse;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A Base class that pulls the config information for running the "unit" tests
 */
public class TestBase
{
    public static final boolean testOnStaging = false;

    public static final String DOMAIN = (testOnStaging) ?
            "https://opendata.test-socrata.com" : "https://sandbox.demo.socrata.com";
    public static final String USERNAME = (testOnStaging) ?
            "adrian.laurenzi+nonadmin2@socrata.com" : "testuser@gmail.com";
    public static final String PASSWORD = (testOnStaging) ?
            "rubes94]yokes" : "OpenData";
    public static final String API_KEY = (testOnStaging) ?
            "EKCJmWioetV1B3roSNIQfb7Z7" : "D8Atrg62F2j017ZTdkMpuZ9vY";
    public static final String UNITTEST_DATASET_ID = (testOnStaging) ? "33re-zxku" : "geue-g9cw";
    public static final String UNITTEST_PORT_RESULT_DATASET_ID = (testOnStaging) ? "8cfm-nx8q" : "szbe-ez5m";
    public static final String UNITTEST_LOG_DATASET_ID = (testOnStaging) ? "7mxj-sxrh" : "ufmq-enj6";
    public static final String UNITTEST_DATASET_ID_LOCATION_COL = (testOnStaging) ? "78vi-gt6w" : "qcq7-r62w";

    public static final String PATH_TO_STAGING_CONFIG_FILE = "src/test/resources/basic_test_config_staging.json";
    public static final String PATH_TO_PROD_CONFIG_FILE = "src/test/resources/basic_test_config.json";
    public static final String PATH_TO_SIMPLE_CONFIG_FILE = "src/test/resources/simple_config.json";

    public static final String PATH_TO_CONFIG_FILE = (testOnStaging) ?
            PATH_TO_STAGING_CONFIG_FILE : PATH_TO_PROD_CONFIG_FILE;

    protected Soda2Producer createProducer() throws IOException {
        return Soda2Producer.newProducer(DOMAIN, USERNAME, PASSWORD, API_KEY);
    }

    protected SodaDdl createSodaDdl() throws IOException {
        return SodaDdl.newDdl(DOMAIN, USERNAME, PASSWORD, API_KEY);
    }

    protected int getTotalRows(String UnitTestDataset) throws LongRunningQueryException, SodaError {
        final Soda2Consumer consumer = Soda2Consumer.newConsumer(DOMAIN, USERNAME, PASSWORD, API_KEY);

        ClientResponse response = consumer.query(UnitTestDataset, HttpLowLevel.JSON_TYPE, "select count(*)");

        ArrayList results = response.getEntity(ArrayList.class);
        Map count = (HashMap<String,String>) results.get(0);
        return Integer.parseInt((String) count.get("count"));
    }

    protected com.socrata.datasync.job.IntegrationJob getIntegrationJobWithUserPrefs() throws IOException {
        return new com.socrata.datasync.job.IntegrationJob(
                getUserPrefs());
    }

    protected UserPreferences getUserPrefs() throws IOException {
        File configFile = new File(PATH_TO_CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(configFile, UserPreferencesFile.class);
    }
}
