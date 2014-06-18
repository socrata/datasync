package com.socrata.datasync.utilities;

import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.config.userpreferences.UserPreferencesFile;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.publishers.FTPDropbox2Publisher;
import com.socrata.datasync.publishers.Soda2Publisher;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import junit.framework.TestCase;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Author: Adrian Laurenzi
 * Date: 3/26/14
 */
public class FTPUtilityTest extends TestBase {

    @Test
    public void testDownloadingFileViaFTP() throws IOException, InterruptedException, URISyntaxException {
        // TODO...
    }

    @Test
    public void tesGetFTPHost() throws URISyntaxException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        UserPreferences userPrefs1 = mapper.readValue("{\"domain\": \"https://sandbox.demo.socrata.com\"}",
                UserPreferencesFile.class);
        UserPreferences userPrefs2 = mapper.readValue("{\"domain\": \"https://adrian.demo.socrata.com\"}",
                UserPreferencesFile.class);
        UserPreferences userPrefs3 = mapper.readValue("{\"domain\": \"https://opendata.test-socrata.com\"}",
                UserPreferencesFile.class);
        TestCase.assertEquals("production.ftp.socrata.net",
                FTPDropbox2Publisher.getFTPHost(userPrefs1));
        TestCase.assertEquals("production.ftp.socrata.net",
                FTPDropbox2Publisher.getFTPHost(userPrefs2));
        TestCase.assertEquals("azure-staging.ftp.socrata.net",
                FTPDropbox2Publisher.getFTPHost(userPrefs3));
    }

    @Test
    public void testReplaceViaFTPWithHeaderRow() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final SodaDdl ddl = createSodaDdl();
        final Soda2Producer producer = createProducer();
        final UserPreferences userPrefs = getUserPrefs();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        String controlFileContent = "{\n" +
                "  \"action\" : \"Replace\", \n" +
                "  \"csv\" :\n" +
                "    {\n" +
                "      \"columns\" : null,\n" +
                "      \"skip\" : 0,\n" +
                "      \"fixedTimestampFormat\" : [\"ISO8601\",\"MM/dd/yyyy\",\"MM/dd/yy\"],\n" +
                "      \"floatingTimestampFormat\" : [\"ISO8601\",\"MM/dd/yyyy\",\"MM/dd/yy\"],\n" +
                "      \"timezone\" : \"UTC\",\n" +
                "      \"separator\" : \",\",\n" +
                "      \"quote\" : \"\\\"\",\n" +
                "      \"encoding\" : \"utf-8\",\n" +
                "      \"emptyTextIsNull\" : true,\n" +
                "      \"trimWhitespace\" : true,\n" +
                "      \"trimServerWhitespace\" : true,\n" +
                "      \"overrides\" : {}\n" +
                "    }\n" +
                "}";

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_four_rows_multidate.csv");
        JobStatus result = FTPDropbox2Publisher.publishViaFTPDropboxV2(
                userPrefs, UNITTEST_DATASET_ID, threeRowsFile,
                controlFileContent);

        TestCase.assertEquals(JobStatus.SUCCESS, result);
        TestCase.assertEquals(4, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceViaFTPWithControlFile() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final SodaDdl ddl = createSodaDdl();
        final Soda2Producer producer = createProducer();
        final UserPreferences userPrefs = getUserPrefs();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        JobStatus result = FTPDropbox2Publisher.publishViaFTPDropboxV2(
                userPrefs, UNITTEST_DATASET_ID, threeRowsFile,
                new File("src/test/resources/datasync_unit_test_three_rows_control.json"));

        TestCase.assertEquals(JobStatus.SUCCESS, result);
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceViaFTPWithoutHeader() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final SodaDdl ddl = createSodaDdl();
        final Soda2Producer producer = createProducer();
        final UserPreferences userPrefs = getUserPrefs();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        String controlFileContent = "{\n" +
                "  \"action\" : \"Replace\", \n" +
                "  \"csv\" :\n" +
                "    {\n" +
                "      \"fixedTimestampFormat\" : \"ISO8601\",\n" +
                "      \"separator\" : \",\",\n" +
                "      \"timezone\" : \"UTC\",\n" +
                "      \"encoding\" : \"utf-8\",\n" +
                "      \"overrides\" : {},\n" +
                "      \"quote\" : \"\\\"\",\n" +
                "      \"emptyTextIsNull\" : true,\n" +
                "      \"columns\" : [\"id\",\"name\", \"another_name\", \"date\"],\n" +
                "      \"skip\" : 0,\n" +
                "      \"floatingTimestampFormat\" : \"MM/dd/yyyy\"\n" +
                "    }\n" +
                "}";

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.csv");
        JobStatus result = FTPDropbox2Publisher.publishViaFTPDropboxV2(
                userPrefs, UNITTEST_DATASET_ID, threeRowsFile,
                controlFileContent);

        TestCase.assertEquals(JobStatus.SUCCESS, result);
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceViaFTPWithInvalidData() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final SodaDdl ddl = createSodaDdl();
        final Soda2Producer producer = createProducer();
        final UserPreferences userPrefs = getUserPrefs();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_invalid_date.csv");
        JobStatus result = FTPDropbox2Publisher.publishViaFTPDropboxV2(
                userPrefs, UNITTEST_DATASET_ID, threeRowsFile,
                new File("src/test/resources/datasync_unit_test_three_rows_control.json"));

        TestCase.assertEquals(JobStatus.PUBLISH_ERROR, result);
        TestCase.assertEquals("FAILURE: Processing datasync_unit_test_three_rows_invalid_date.csv failed: Value in column \"date\" uninterpretable as calendar_date in input at record 3: \"invalid_date\"\n",
                result.getMessage());
        TestCase.assertEquals(2, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceViaFTPInvalidControlFile() throws IOException, InterruptedException {
        final UserPreferences userPrefs = getUserPrefs();

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        JobStatus result = FTPDropbox2Publisher.publishViaFTPDropboxV2(
                userPrefs, UNITTEST_DATASET_ID, threeRowsFile,
                new File("src/test/resources/datasync_unit_test_three_rows_control_invalid.json"));

        TestCase.assertEquals(JobStatus.PUBLISH_ERROR, result);
    }
}
