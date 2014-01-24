package com.socrata.datasync.job;

import com.socrata.datasync.JobStatus;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesFile;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import junit.framework.TestCase;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 10/18/13
 */
public class IntegrationJobTest extends TestBase {

    public static final String PATH_TO_SAVED_JOB_FILE_V0dot1 = "src/test/resources/job_saved_v0.1.sij";
    public static final String PATH_TO_SAVED_JOB_FILE_V0dot3 = "src/test/resources/job_saved_v0.3.sij";

    @Test
    public void testOldJobFileDeserialization() throws IOException {
        IntegrationJob job = new IntegrationJob(PATH_TO_SAVED_JOB_FILE_V0dot1);

        TestCase.assertEquals(
                "/Users/adrian/Dropbox/Socrata_s/projects/data_integration/datasync/misc/honolulu_report2.csv",
                job.getFileToPublish());
        TestCase.assertEquals("n38h-y5wpx", job.getDatasetID());
        TestCase.assertEquals(PublishMethod.upsert, job.getPublishMethod());
        TestCase.assertEquals(
                "src/test/resources/job_saved_v0.1.sij",
                job.getPathToSavedFile());
    }

    @Test
    public void testNewJobFileDeserialization() throws IOException {
        IntegrationJob job = new IntegrationJob(PATH_TO_SAVED_JOB_FILE_V0dot3);

        TestCase.assertEquals(1, job.getFileVersionUID());
        TestCase.assertEquals(
                "/Users/adrian/Dropbox/Socrata_s/projects/data_integration/datasync/misc/honolulu_report2.csv",
                job.getFileToPublish());
        TestCase.assertEquals("n38h-y5wpx", job.getDatasetID());
        TestCase.assertEquals(PublishMethod.upsert, job.getPublishMethod());
        TestCase.assertEquals(
                "src/test/resources/job_saved_v0.3.sij",
                job.getPathToSavedFile());
    }

    @Test
    public void testIntegrationJobReplace() throws IOException, LongRunningQueryException, SodaError {
        File configFile = new File(PATH_TO_CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        UserPreferences userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
        com.socrata.datasync.job.IntegrationJob jobToRun = new com.socrata.datasync.job.IntegrationJob(userPrefs);
        jobToRun.setDatasetID(UNITTEST_DATASET_ID);
        jobToRun.setFileToPublish("src/test/resources/datasync_unit_test_two_rows.csv");
        jobToRun.setPublishMethod(PublishMethod.replace);
        jobToRun.setFileToPublishHasHeaderRow(true);
        JobStatus status = jobToRun.run();
        TestCase.assertEquals(JobStatus.SUCCESS, status);
        TestCase.assertFalse(status.isError());
        TestCase.assertEquals(2, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testIntegrationJobInvalidDatasetId() throws IOException, LongRunningQueryException, SodaError {
        File configFile = new File(PATH_TO_CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        UserPreferences userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
        com.socrata.datasync.job.IntegrationJob jobToRun = new com.socrata.datasync.job.IntegrationJob(userPrefs);
        jobToRun.setDatasetID("invalidid");
        jobToRun.setFileToPublish("src/test/resources/datasync_unit_test_two_rows.csv");
        jobToRun.setPublishMethod(PublishMethod.replace);
        jobToRun.setFileToPublishHasHeaderRow(true);
        JobStatus status = jobToRun.run();
        TestCase.assertEquals(JobStatus.INVALID_DATASET_ID, status);
        TestCase.assertTrue(status.isError());
    }

    @Test
    public void testIntegrationJobPublishError() throws IOException, LongRunningQueryException, SodaError {
        File configFile = new File(PATH_TO_CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        UserPreferences userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
        com.socrata.datasync.job.IntegrationJob jobToRun = new com.socrata.datasync.job.IntegrationJob(userPrefs);
        jobToRun.setDatasetID(UNITTEST_DATASET_ID);
        jobToRun.setFileToPublish("src/test/resources/datasync_unit_test_invalid_date.csv");
        jobToRun.setPublishMethod(PublishMethod.upsert);
        jobToRun.setFileToPublishHasHeaderRow(true);
        JobStatus status = jobToRun.run();
        TestCase.assertEquals(JobStatus.PUBLISH_ERROR, status);
        TestCase.assertTrue(status.isError());
        // TODO determine what error message should be
        TestCase.assertTrue(status.getMessage().equals("Invalid..."));
    }
}
