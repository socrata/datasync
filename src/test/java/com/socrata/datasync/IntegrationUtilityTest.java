package com.socrata.datasync;

import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.api.SodaImporter;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesFile;
import com.socrata.datasync.preferences.UserPreferencesJava;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertResult;
import junit.framework.TestCase;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Author: Adrian Laurenzi
 * Date: 10/11/13
 */
public class IntegrationUtilityTest extends TestBase {

    @Test
    public void testReplaceNew() throws LongRunningQueryException, SodaError, IOException, InterruptedException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        UpsertResult result = IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsCreated());
        TestCase.assertEquals(2, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceNewNoHeader() throws LongRunningQueryException, SodaError, IOException, InterruptedException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        File twoRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.csv");
        UpsertResult result = IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, false);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(3, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceNewTSVFile() throws LongRunningQueryException, SodaError, IOException, InterruptedException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.tsv");
        UpsertResult result = IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, true);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(3, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceNewTSVFileNoHeader() throws LongRunningQueryException, SodaError, IOException, InterruptedException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.tsv");
        UpsertResult result = IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, false);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(3, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testUpsertZeroRows() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        int numRowsBegin = getTotalRows(UNITTEST_DATASET_ID);

        File zeroRowsFile = new File("src/test/resources/datasync_unit_test_zero_rows.csv");
        UpsertResult result = IntegrationUtility.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, zeroRowsFile, 0, true);

        int numRowsAfter = getTotalRows(UNITTEST_DATASET_ID);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(numRowsBegin, numRowsAfter);
    }

    @Test
    public void testUpsertNoChunking() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        UpsertResult result = IntegrationUtility.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, 0, true);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsUpdated());
        TestCase.assertEquals(1, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testUpsertNoHeader() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.csv");
        UpsertResult result = IntegrationUtility.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, 0, false);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsUpdated());
        TestCase.assertEquals(1, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testUpsertTSVFile() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.tsv");
        UpsertResult result = IntegrationUtility.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, 0, true);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsUpdated());
        TestCase.assertEquals(1, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testUpsertTSVFileNoHeader() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.tsv");
        UpsertResult result = IntegrationUtility.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, 0, false);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsUpdated());
        TestCase.assertEquals(1, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testUpsertInChunks() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        int numRowsPerChunk = 2;
        UpsertResult result = IntegrationUtility.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, numRowsPerChunk, true);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsUpdated());
        TestCase.assertEquals(1, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testDeleteNoHeader() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (3 rows)
        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, true);

        File deleteTwoRowsFile = new File("src/test/resources/datasync_unit_test_delete_two_rows_no_header.csv");
        UpsertResult result = IntegrationUtility.deleteRows(producer, ddl, UNITTEST_DATASET_ID, deleteTwoRowsFile, 0, false);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsDeleted());
        TestCase.assertEquals(1, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void tesGetFTPHost() throws URISyntaxException, IOException, LongRunningQueryException, SodaError {
        final SodaDdl ddl = createSodaDdl();
        TestCase.assertEquals("production.ftp.socrata.net",
                IntegrationUtility.getFTPHost("https://sandbox.demo.socrata.com", ddl));
        TestCase.assertEquals("production.ftp.socrata.net",
                IntegrationUtility.getFTPHost("https://adrian.demo.socrata.com", ddl));
        TestCase.assertEquals("azure-staging.ftp.socrata.net",
                IntegrationUtility.getFTPHost("https://opendata.test-socrata.com", ddl));
    }

    @Test
    public void testReplaceViaFTPWithControlFile() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final SodaDdl ddl = createSodaDdl();
        final Soda2Producer producer = createProducer();
        final UserPreferences userPrefs = getUserPrefs();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        JobStatus result = IntegrationUtility.publishViaFTPDropboxV2(
                userPrefs, ddl, UNITTEST_DATASET_ID, threeRowsFile,
                new File("src/test/resources/datasync_unit_test_three_rows_control.json"));

        TestCase.assertEquals(JobStatus.SUCCESS, result);
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceViaFTPWithControlFileContent() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final SodaDdl ddl = createSodaDdl();
        final Soda2Producer producer = createProducer();
        final UserPreferences userPrefs = getUserPrefs();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

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
                "      \"skip\" : 1,\n" +
                "      \"floatingTimestampFormat\" : \"MM/dd/yyyy\"\n" +
                "    }\n" +
                "}";

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        JobStatus result = IntegrationUtility.publishViaFTPDropboxV2(
                userPrefs, ddl, UNITTEST_DATASET_ID, threeRowsFile,
                controlFileContent);

        TestCase.assertEquals(JobStatus.SUCCESS, result);
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceViaFTPInvalidControlFile() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final SodaDdl ddl = createSodaDdl();
        final UserPreferences userPrefs = getUserPrefs();

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        JobStatus result = IntegrationUtility.publishViaFTPDropboxV2(
                userPrefs, ddl, UNITTEST_DATASET_ID, threeRowsFile,
                new File("src/test/resources/datasync_unit_test_three_rows_control_invalid.json"));

        TestCase.assertEquals(JobStatus.PUBLISH_ERROR, result);
    }

    @Test
    public void testDownloadingFileViaFTP() throws IOException, SodaError, InterruptedException, LongRunningQueryException, URISyntaxException {
        // TODO...
    }

    @Test
    public void testDeleteWithHeaderAndNonExistantRowIDs() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (3 rows)
        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, true);

        File deleteTwoRowsFile = new File("src/test/resources/datasync_unit_test_delete_two_rows_with_header.csv");
        UpsertResult result = IntegrationUtility.deleteRows(producer, ddl, UNITTEST_DATASET_ID, deleteTwoRowsFile, 0, true);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsDeleted());
        TestCase.assertEquals(1, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testDeleteWithHeader() throws IOException, SodaError, InterruptedException, LongRunningQueryException {

    }

    @Test
    public void testDeleteInChunks() {

    }

    @Test
    public void testUpsertWithInvalidData() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_invalid_date.csv");
        int numRowsPerChunk = 0;
        UpsertResult result = IntegrationUtility.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, numRowsPerChunk, true);

        TestCase.assertEquals(1, result.errorCount());
        TestCase.assertEquals(2, result.getRowsUpdated());
        TestCase.assertEquals(0, result.getRowsCreated());
        TestCase.assertEquals(2, getTotalRows(UNITTEST_DATASET_ID));
        TestCase.assertEquals("Unknown date format 'invalid_date'.", result.getErrors().get(0).getError());
        TestCase.assertEquals(2, result.getErrors().get(0).getIndex());
    }

    @Test
    public void testUpsertInChunksWithInvalidData() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_multiple_invalid_dates.csv");
        int numRowsPerChunk = 2;
        UpsertResult result = IntegrationUtility.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, numRowsPerChunk, true);

        TestCase.assertEquals(2, result.errorCount());
        TestCase.assertEquals(1, result.getRowsUpdated());
        TestCase.assertEquals(0, result.getRowsCreated());
        TestCase.assertEquals(2, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceNewWithInvalidData() {

    }

    @Test
    public void testAddLogEntry() {

    }

    @Test
    public void testGetDatasetFieldNames() throws IOException, SodaError, InterruptedException {
        //UserPreferences userPrefs = new UserPreferencesJava();
        //System.out.println(IntegrationUtility.getDatasetFieldNames(ddl, "6qkn-8xvw"));
        final SodaDdl ddl = createSodaDdl();
        String datasetFieldNames = IntegrationUtility.getDatasetFieldNames(ddl, UNITTEST_DATASET_ID);
        TestCase.assertEquals("[\"id\",\"name\",\"another_name\",\"date\"]", datasetFieldNames);
    }


    @Test
    public void testUidIsValid() {
        TestCase.assertFalse(IntegrationUtility.uidIsValid("hello"));
        TestCase.assertFalse(IntegrationUtility.uidIsValid("abcd/1234"));
        TestCase.assertFalse(IntegrationUtility.uidIsValid("6thm1hz4z"));

        TestCase.assertTrue(IntegrationUtility.uidIsValid("abcd-1234"));
        TestCase.assertTrue(IntegrationUtility.uidIsValid("vysc-frub"));
        TestCase.assertTrue(IntegrationUtility.uidIsValid("6thm-hz4z"));
    }
}
