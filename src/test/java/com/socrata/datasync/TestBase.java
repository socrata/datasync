package com.socrata.datasync;

import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Consumer;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.soql.SoqlQuery;
import com.sun.jersey.api.client.ClientResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A Base class that pulls the config information for running the "unit" tests
 */
public class TestBase
{
    public static final String DOMAIN = "https://sandbox.demo.socrata.com";
    public static final String USERNAME = "testuser@gmail.com";
    public static final String PASSWORD = "OpenData";
    public static final String API_KEY = "D8Atrg62F2j017ZTdkMpuZ9vY";

    public static final String UNITTEST_DATASET_ID = "8gex-q4ds";

    protected void setTestUserPreferences() {
        UserPreferences userPrefs = new UserPreferences();
        userPrefs.saveDomain(DOMAIN);
        userPrefs.saveUsername(USERNAME);
        userPrefs.savePassword(PASSWORD);
        userPrefs.saveAPIKey(API_KEY);
    }

    protected Soda2Producer createProducer() throws IOException {
        return Soda2Producer.newProducer(DOMAIN, USERNAME, PASSWORD, API_KEY);
    }

    protected SodaDdl createSodaDdl() throws IOException {
        return SodaDdl.newDdl(DOMAIN, USERNAME, PASSWORD, API_KEY);
    }

    protected int getTotalRowsUnitTestDataset() throws LongRunningQueryException, SodaError {
        final Soda2Consumer consumer = Soda2Consumer.newConsumer(DOMAIN, USERNAME, PASSWORD, API_KEY);

        ClientResponse response = consumer.query(UNITTEST_DATASET_ID, HttpLowLevel.JSON_TYPE, "select count(*)");

        ArrayList results = response.getEntity(ArrayList.class);
        Map count = (HashMap<String,String>) results.get(0);
        return Integer.parseInt((String) count.get("count"));
    }
}
