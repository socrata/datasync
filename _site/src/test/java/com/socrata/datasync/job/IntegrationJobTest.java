package com.socrata.datasync.job;

import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class IntegrationJobTest extends TestBase {

    private IntegrationJob job;
    CommandLineParser parser;
    CommandLineOptions cmd = new CommandLineOptions();

    public static final String PATH_TO_SAVED_JOB_FILE_V0dot1 = "src/test/resources/job_saved_v0.1.sij";
    public static final String PATH_TO_SAVED_JOB_FILE_V0dot3 = "src/test/resources/job_saved_v0.3.sij";
    public static final String PATH_TO_SAVED_JOB_FILE_V0dot4 = "src/test/resources/job_saved_v0.4.sij";
    public static final String PATH_TO_SAVED_JOB_FILE_V0dot4_CONTROL_CONTENT =
            "src/test/resources/job_saved_v0.4_control_content.sij";

    @Before
    public void initialize() {
        job = new IntegrationJob();
        parser = new PosixParser();
    }

    @Test
    public void testValidationOfArgs() throws ParseException {

        String[] goodArgs = {"-i", "some-four", "-f", " ~/././.", "-m", "replace", "-h", "true"};
        String[] incompleteArgs1 = {"-f", " ~/././.", "-m", "replace", "-h", "true"};
        String[] incompleteArgs2 = {"-i", "some-four", "-m", "replace", "-h", "true"};
        String[] incompleteArgs3 = {"-i", "some-four", "-f", " ~/././.", "-h", "true"};
        String[] incompleteArgs4 = {"-i", "some-four", "-f", " ~/././.", "-m", "replace"};
        String[] invalidArgs1 = {"-i", "some-four", "-f", " ~/././.", "-m", "replace", "-h", "true", "-pf", "invalid"};
        String[] invalidArgs2 = {"-i", "4x4", "-f", " ~/.", "-m", "replace", "-h", "true", "-pf", "false", "-sc", "/./."};
        String[] invalidArgs3 = {"-i", "4x4", "-f", " ~/././.", "-m", "invalid", "-h", "true"};

        TestCase.assertTrue(job.validateArgs(parser.parse(cmd.options, goodArgs)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs1)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs2)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs3)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs4)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, invalidArgs1)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, invalidArgs2)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, invalidArgs3)));
    }

    @Test
    public void testConfiguration() throws ParseException {
        String[] args = {"-i", "some-four", "-f", " ~/././.", "-m", "replace", "-h", "true"};
        job.configure(parser.parse(cmd.options, args));

        TestCase.assertEquals(job.getDatasetID(), args[1]);
        TestCase.assertEquals(job.getFileToPublish(), args[3]);
        TestCase.assertEquals(job.getPublishMethod().toString(), args[5]);
        TestCase.assertTrue(job.getFileToPublishHasHeaderRow());
    }

    @Test
    public void testDataSyncV0dot1JobFileDeserialization() throws IOException, IntegrationJob.ControlDisagreementException {
        IntegrationJob job = new IntegrationJob(PATH_TO_SAVED_JOB_FILE_V0dot1);

        TestCase.assertEquals(
                "/Users/adrian/Dropbox/Socrata_s/projects/data_integration/datasync/misc/honolulu_report2.csv",
                job.getFileToPublish());
        TestCase.assertEquals("n38h-y5wpx", job.getDatasetID());
        TestCase.assertEquals(PublishMethod.upsert, job.getPublishMethod());
        TestCase.assertEquals(
                "src/test/resources/job_saved_v0.1.sij",
                job.getPathToSavedFile());
        TestCase.assertEquals(false, job.getPublishViaFTP());
    }

    @Test
    public void testDataSyncJSONJobFileDeserialization() throws IOException, IntegrationJob.ControlDisagreementException {
        IntegrationJob job = new IntegrationJob(PATH_TO_SAVED_JOB_FILE_V0dot3);

        TestCase.assertEquals(
                "/Users/adrian/Dropbox/Socrata_s/projects/data_integration/datasync/misc/honolulu_report2.csv",
                job.getFileToPublish());
        TestCase.assertEquals("n38h-y5wpx", job.getDatasetID());
        TestCase.assertEquals(PublishMethod.upsert, job.getPublishMethod());
        TestCase.assertEquals(
                "src/test/resources/job_saved_v0.3.sij",
                job.getPathToSavedFile());
        TestCase.assertEquals(false, job.getPublishViaFTP());
    }

    @Test
    public void testDataSyncJSONv0dot4JobFileDeserialization() throws IOException, IntegrationJob.ControlDisagreementException {
        IntegrationJob job = new IntegrationJob(PATH_TO_SAVED_JOB_FILE_V0dot4);
        TestCase.assertEquals("E:\\tm\\02.26.2014.17.59.22.0.0695675217148733.csv", job.getFileToPublish());
        TestCase.assertEquals("kwgk-zc5k", job.getDatasetID());
        TestCase.assertEquals(PublishMethod.replace, job.getPublishMethod());
        TestCase.assertEquals("src/test/resources/job_saved_v0.4.sij", job.getPathToSavedFile());
        TestCase.assertEquals(true, job.getFileToPublishHasHeaderRow());
        TestCase.assertEquals(true, job.getPublishViaFTP());
        TestCase.assertEquals("", job.getPathToControlFile());
        TestCase.assertEquals("job_saved_v0.4.sij", job.getJobFilename());
    }

   @Test
    public void testDataSyncJSONv0dot4ControlContentJobFileDeserialization() throws IOException, IntegrationJob.ControlDisagreementException {
        IntegrationJob job = new IntegrationJob(PATH_TO_SAVED_JOB_FILE_V0dot4_CONTROL_CONTENT);
        TestCase.assertEquals("/Users/file.csv", job.getFileToPublish());
        TestCase.assertEquals("geue-g9cw", job.getDatasetID());
        TestCase.assertEquals(PublishMethod.replace, job.getPublishMethod());
        TestCase.assertEquals("src/test/resources/job_saved_v0.4_control_content.sij", job.getPathToSavedFile());
        TestCase.assertEquals(false, job.getFileToPublishHasHeaderRow());
        TestCase.assertEquals(true, job.getPublishViaFTP());
        TestCase.assertEquals("", job.getPathToControlFile());
        TestCase.assertEquals("{\n" +
                "  \"action\" : \"Replace\", \n" +
                "  \"csv\" :\n" +
                "    {\n" +
                "      \"columns\" : [\"id\",\"name\",\"another_name\",\"date\"],\n" +
                "      \"skip\" : 0,\n" +
                "      \"fixedTimestampFormat\" : \"ISO8601\",\n" +
                "      \"floatingTimestampFormat\" : \"ISO8601\",\n" +
                "      \"timezone\" : \"UTC\",\n" +
                "      \"separator\" : \",\",\n" +
                "      \"quote\" : \"\\\"\",\n" +
                "      \"encoding\" : \"utf-8\",\n" +
                "      \"emptyTextIsNull\" : true,\n" +
                "      \"trimWhitespace\" : true,\n" +
                "      \"trimServerWhitespace\" : true,\n" +
                "      \"overrides\" : {}\n" +
                "    }\n" +
                "}", job.getControlFileContent());
        TestCase.assertEquals("job_saved_v0.4_control_content.sij", job.getJobFilename());
    }

    @Test
    public void testIntegrationJobReplaceViaHTTP() throws IOException, LongRunningQueryException, SodaError {
        IntegrationJob jobToRun = getIntegrationJobWithUserPrefs();
        jobToRun.setDatasetID(UNITTEST_DATASET_ID);
        jobToRun.setFileToPublish("src/test/resources/datasync_unit_test_two_rows.csv");
        jobToRun.setPublishMethod(PublishMethod.replace);
        jobToRun.setFileToPublishHasHeaderRow(true);
        jobToRun.setPublishViaFTP(false);
        JobStatus status = jobToRun.run();
        TestCase.assertEquals(false, jobToRun.getPublishViaFTP());
        TestCase.assertEquals(JobStatus.SUCCESS, status);
        TestCase.assertFalse(status.isError());
        TestCase.assertEquals(2, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testIntegrationJobReplaceViaFTP() throws IOException, LongRunningQueryException, SodaError {
        IntegrationJob jobToRun = getIntegrationJobWithUserPrefs();
        jobToRun.setDatasetID(UNITTEST_DATASET_ID);
        jobToRun.setFileToPublish("src/test/resources/datasync_unit_test_three_rows.csv");
        jobToRun.setPathToControlFile("src/test/resources/datasync_unit_test_three_rows_control.json");
        jobToRun.setPublishMethod(PublishMethod.replace);
        jobToRun.setPublishViaFTP(true);
        jobToRun.setFileToPublishHasHeaderRow(false);
        JobStatus status = jobToRun.run();
        TestCase.assertEquals(JobStatus.SUCCESS, status);
        TestCase.assertFalse(status.isError());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testIntegrationJobInvalidDatasetId() throws IOException, LongRunningQueryException, SodaError {
        IntegrationJob jobToRun = getIntegrationJobWithUserPrefs();
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
        IntegrationJob jobToRun = getIntegrationJobWithUserPrefs();
        jobToRun.setDatasetID(UNITTEST_DATASET_ID);
        jobToRun.setFileToPublish("src/test/resources/datasync_unit_test_invalid_date.csv");
        jobToRun.setPublishMethod(PublishMethod.upsert);
        jobToRun.setFileToPublishHasHeaderRow(true);
        jobToRun.setPublishViaFTP(false);
        JobStatus status = jobToRun.run();
        TestCase.assertEquals(JobStatus.PUBLISH_ERROR, status);
        TestCase.assertTrue(status.isError());
        TestCase.assertEquals("Unknown date format 'invalid'. (line 3 of file) \n", status.getMessage());
    }

    @Test
    public void testIntegrationJobFTPPublishError() throws IOException, LongRunningQueryException, SodaError {
        IntegrationJob jobToRun = getIntegrationJobWithUserPrefs();
        jobToRun.setDatasetID(UNITTEST_DATASET_ID);
        jobToRun.setFileToPublish("src/test/resources/datasync_unit_test_invalid_date.csv");
        jobToRun.setPathToControlFile("src/test/resources/datasync_unit_test_three_rows_control.json");
        jobToRun.setFileToPublishHasHeaderRow(true);
        jobToRun.setPublishMethod(PublishMethod.replace);
        jobToRun.setPublishViaFTP(true);
        JobStatus status = jobToRun.run();
        TestCase.assertEquals(JobStatus.PUBLISH_ERROR, status);
        TestCase.assertTrue(status.isError());
        TestCase.assertEquals("There was an error converting the value in the column Date(7) to a calendar_date.\n", status.getMessage());
    }
}
