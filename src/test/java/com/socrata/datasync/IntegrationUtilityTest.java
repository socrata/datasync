package com.socrata.datasync;

import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.UpsertResult;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import test.model.UnitTestDataset;

import java.io.File;
import java.io.IOException;

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
    public void testUpsertNoDeletes() throws IOException, SodaError, InterruptedException, LongRunningQueryException {
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
    public void testUpsertWithBadData() {

    }

    @Test
    public void testReplaceNewWithBadData() {

    }

    @Test
    public void testAddLogEntry() {

    }
}
