package com.socrata.datasync.utilities;

import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.DatasetUtils;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.Utils;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.publishers.Soda2Publisher;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertError;
import com.socrata.model.UpsertResult;
import com.socrata.model.importer.Dataset;
import junit.framework.TestCase;
import org.apache.http.HttpException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

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
        UpsertResult result = Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsCreated());
        TestCase.assertEquals(2, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceNewNoHeader() throws LongRunningQueryException, SodaError, IOException, InterruptedException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        File twoRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.csv");
        UpsertResult result = Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, false);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(3, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceNewTSVFile() throws LongRunningQueryException, SodaError, IOException, InterruptedException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.tsv");
        UpsertResult result = Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, true);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(3, result.getRowsCreated());
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceNewTSVFileNoHeader() throws LongRunningQueryException, SodaError, IOException, InterruptedException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.tsv");
        UpsertResult result = Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, false);

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
        UpsertResult result = Soda2Publisher.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, zeroRowsFile, 0, true);

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
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        UpsertResult result = Soda2Publisher.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, 0, true);

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
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.csv");
        UpsertResult result = Soda2Publisher.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, 0, false);

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
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.tsv");
        UpsertResult result = Soda2Publisher.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, 0, true);

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
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_no_header.tsv");
        UpsertResult result = Soda2Publisher.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, 0, false);

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
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        int numRowsPerChunk = 2;
        UpsertResult result = Soda2Publisher.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, numRowsPerChunk, true);

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
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, true);

        File deleteTwoRowsFile = new File("src/test/resources/datasync_unit_test_delete_two_rows_no_header.csv");
        UpsertResult result = Soda2Publisher.deleteRows(producer, ddl, UNITTEST_DATASET_ID, deleteTwoRowsFile, 0, false);

        TestCase.assertEquals(0, result.errorCount());
        TestCase.assertEquals(2, result.getRowsDeleted());
        TestCase.assertEquals(1, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testDeleteWithHeaderAndNonExistantRowIDs() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (3 rows)
        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, true);

        File deleteTwoRowsFile = new File("src/test/resources/datasync_unit_test_delete_two_rows_with_header.csv");
        UpsertResult result = Soda2Publisher.deleteRows(producer, ddl, UNITTEST_DATASET_ID, deleteTwoRowsFile, 0, true);

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
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_invalid_date.csv");
        int numRowsPerChunk = 0;
        UpsertResult result = Soda2Publisher.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, numRowsPerChunk, true);

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
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_DATASET_ID, twoRowsFile, true);

        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows_multiple_invalid_dates.csv");
        int numRowsPerChunk = 2;
        UpsertResult result = Soda2Publisher.appendUpsert(producer, ddl, UNITTEST_DATASET_ID, threeRowsFile, numRowsPerChunk, true);

        TestCase.assertEquals(2, result.errorCount());
        TestCase.assertEquals(1, result.getRowsUpdated());
        TestCase.assertEquals(0, result.getRowsCreated());
        TestCase.assertEquals(2, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testReplaceNewWithInvalidData() {

    }

    @Test
    public void testAddLogEntry() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
        final Soda2Producer producer = createProducer();
        final SodaDdl ddl = createSodaDdl();

        // Ensures dataset is in known state (1 row)
        File oneRowFile = new File("src/test/resources/log_dataset_one_row.csv");
        Soda2Publisher.replaceNew(producer, ddl, UNITTEST_LOG_DATASET_ID, oneRowFile, true);

        IntegrationJob job = new IntegrationJob();
        UpsertResult result = new UpsertResult(1, 1, 1, new ArrayList<UpsertError>());
        SocrataConnectionInfo connectionInfo = new SocrataConnectionInfo(DOMAIN, USERNAME, PASSWORD, API_KEY);
        String logPublishingErrorMessage = job.addLogEntry(
                UNITTEST_LOG_DATASET_ID, connectionInfo, job, JobStatus.INVALID_DATASET_ID, result);

        TestCase.assertEquals(null, logPublishingErrorMessage);
        TestCase.assertEquals(2, getTotalRows(UNITTEST_LOG_DATASET_ID));
    }

    @Test
    public void testAddLogEntryInvalidLogDatasetId() {
        IntegrationJob job = new IntegrationJob();
        UpsertResult result = new UpsertResult(1, 1, 1, new ArrayList<UpsertError>());
        SocrataConnectionInfo connectionInfo = new SocrataConnectionInfo(DOMAIN, USERNAME, PASSWORD, API_KEY);
        String logPublishingErrorMessage = job.addLogEntry(
                "xxxx-xxxx", connectionInfo, job, JobStatus.SUCCESS, result);

        TestCase.assertEquals("Not found", logPublishingErrorMessage);
    }

    @Test
    public void testGetDatasetFieldNames() throws InterruptedException, HttpException, URISyntaxException, IOException, SodaError {
        //UserPreferences userPrefs = new UserPreferencesJava();
        //System.out.println(IntegrationUtility.getFieldNamesString(ddl, "6qkn-8xvw"));
        final SodaDdl ddl = createSodaDdl();
        String datasetFieldNamesString = DatasetUtils.getFieldNamesString(new UserPreferencesJava(), UNITTEST_DATASET_ID);
        TestCase.assertEquals("\"id\",\"name\",\"another_name\",\"date\"", datasetFieldNamesString);

        Dataset datasetInfo = (Dataset) ddl.loadDatasetInfo(UNITTEST_DATASET_ID);
        String[] datasetFieldNames = DatasetUtils.getFieldNamesArray(datasetInfo);
        TestCase.assertEquals("id", datasetFieldNames[0]);
        TestCase.assertEquals("name", datasetFieldNames[1]);
        TestCase.assertEquals("another_name", datasetFieldNames[2]);
    }

    @Test
    public void testGetDatasetHasLocationColumn() throws IOException, SodaError, InterruptedException {
        final SodaDdl ddl = createSodaDdl();
        Dataset datasetInfoNoLocation = (Dataset) ddl.loadDatasetInfo(UNITTEST_DATASET_ID);
        Dataset datasetInfoWithLocation = (Dataset) ddl.loadDatasetInfo(UNITTEST_DATASET_ID_LOCATION_COL);
        TestCase.assertFalse(DatasetUtils.hasLocationColumn(datasetInfoNoLocation));
        TestCase.assertTrue(DatasetUtils.hasLocationColumn(datasetInfoWithLocation));
    }


    @Test
    public void testUidIsValid() {
        TestCase.assertFalse(Utils.uidIsValid("hello"));
        TestCase.assertFalse(Utils.uidIsValid("abcd/1234"));
        TestCase.assertFalse(Utils.uidIsValid("6thm1hz4z"));

        TestCase.assertTrue(Utils.uidIsValid("abcd-1234"));
        TestCase.assertTrue(Utils.uidIsValid("vysc-frub"));
        TestCase.assertTrue(Utils.uidIsValid("6thm-hz4z"));
    }
}
