package com.socrata.datasync;

import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import com.socrata.model.importer.DatasetInfo;
import com.socrata.model.soql.SoqlQuery;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import test.model.UnitTestDataset;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Author: Louis Fettet
 * Date: 11/4/13
 */
public class PortUtilityTest extends TestBase {

    static SodaDdl sourceDdl;
    static SodaDdl sinkDdl;
    static Soda2Producer sourceProducer;
    static Soda2Producer sinkProducer;

    @Before
    public void setUpTest() throws IOException {
        sourceDdl = createSodaDdl();
        sinkDdl = createSodaDdl();
        sourceProducer = createProducer();
        sinkProducer = createProducer();
    }

    @Test
    public void testPortSchema() throws SodaError, InterruptedException {
        // Perform the test operation, saving the String return value.
        String newDatasetID = PortUtility.portSchema(sourceDdl, sinkDdl, UNITTEST_DATASET_ID);

        // Grab the necessary objects for testing.
        DatasetInfo sourceMeta = sourceDdl.loadDatasetInfo(UNITTEST_DATASET_ID);
        DatasetInfo sinkMeta = sinkDdl.loadDatasetInfo(newDatasetID);

        try {
            // First, test the metadata (just the basics) via DatasetInfo.
            TestCase.assertEquals(sourceMeta.getViewType(), sinkMeta.getViewType());
            TestCase.assertEquals(sourceMeta.getName(), sinkMeta.getName());
            TestCase.assertEquals(sourceMeta.getDescription(), sinkMeta.getDescription());
            TestCase.assertEquals(sourceMeta.getCategory(), sinkMeta.getCategory());
            TestCase.assertEquals(sourceMeta.getTags(), sinkMeta.getTags());
            TestCase.assertEquals(sourceMeta.getRights(), sinkMeta.getRights());

            // Next, test the schema by grabbing Dataset and Column objects from DatasetInfo.
            Dataset sourceSchema = (Dataset) sourceMeta;
            Dataset sinkSchema = (Dataset) sinkMeta;
            List<Column> sourceColumns = sourceSchema.getColumns();
            List<Column> sinkColumns = sinkSchema.getColumns();

            TestCase.assertEquals(sourceColumns.size(), sinkColumns.size());
            for (int i = 0; i < sourceColumns.size(); i++) {
                TestCase.assertEquals(sourceColumns.get(i).getName(), sinkColumns.get(i).getName());
                TestCase.assertEquals(sourceColumns.get(i).getFieldName(), sinkColumns.get(i).getFieldName());
                TestCase.assertEquals(sourceColumns.get(i).getDescription(), sinkColumns.get(i).getDescription());
                TestCase.assertEquals(sourceColumns.get(i).getDataTypeName(), sinkColumns.get(i).getDataTypeName());
                TestCase.assertEquals(sourceColumns.get(i).getFlags(), sinkColumns.get(i).getFlags());
                TestCase.assertEquals(sourceColumns.get(i).getPosition(), sinkColumns.get(i).getPosition());
            }

        } finally {
            sinkDdl.deleteDataset(newDatasetID);
        }
    }

    @Test
    public void testPublishDataset() throws SodaError, InterruptedException {
        // Port a dataset's schema and confirm that it is unpublished by default.
        String unpublishedID = PortUtility.portSchema(sourceDdl, sinkDdl, UNITTEST_DATASET_ID);
        DatasetInfo source = sourceDdl.loadDatasetInfo(unpublishedID);
        TestCase.assertEquals("unpublished", source.getPublicationStage());

        // Perform the test operation.  Confirm the dataset is published afterwards.
        String publishedID = PortUtility.publishDataset(sinkDdl, unpublishedID);
        DatasetInfo sink = sinkDdl.loadDatasetInfo(publishedID);
        TestCase.assertEquals("published", sink.getPublicationStage());

        sinkDdl.deleteDataset(publishedID);
    }

    @Test
    public void testPortContentsUpsert() throws SodaError, InterruptedException, LongRunningQueryException {
        // Query for the rows of the source dataset.
        List<UnitTestDataset> sourceRows = sinkProducer.query(UNITTEST_DATASET_ID, SoqlQuery.SELECT_ALL, UnitTestDataset.LIST_TYPE);

        // Port a dataset's schema to get an empty copy to test with.
        String newDatasetID = PortUtility.portSchema(sourceDdl, sinkDdl, UNITTEST_DATASET_ID);

        // Query for the rows (well, lack thereof) of the sink dataset.
        List<UnitTestDataset> sinkRows = sinkProducer.query(newDatasetID, SoqlQuery.SELECT_ALL, UnitTestDataset.LIST_TYPE);
        TestCase.assertEquals(0, sinkRows.size());

        try {
            // Perform the test operation.
            PortUtility.portContents(sourceProducer, sinkProducer, UNITTEST_DATASET_ID, newDatasetID, PublishMethod.upsert);

            // Query for the rows of the sink again.
            sinkRows = sinkProducer.query(newDatasetID, SoqlQuery.SELECT_ALL, UnitTestDataset.LIST_TYPE);

            TestCase.assertEquals(sourceRows.size(), sinkRows.size());
            for (int i = 0; i < sourceRows.size(); i++) {
                TestCase.assertEquals(sourceRows.get(i).getName(), sinkRows.get(i).getName());
                TestCase.assertEquals(sourceRows.get(i).getName_2(), sinkRows.get(i).getName_2());
                TestCase.assertEquals(sourceRows.get(i).getAnother_name(), sinkRows.get(i).getAnother_name());
                TestCase.assertEquals(sourceRows.get(i).getDate(), sinkRows.get(i).getDate());
            }

        } finally {
            sinkDdl.deleteDataset(newDatasetID);
        }
    }

    @Test
    public void testPortContentsReplace() throws SodaError, InterruptedException, IOException, LongRunningQueryException {
        // Query for the rows of the source dataset.
        int sourceTotalRows = getTotalRows(UNITTEST_DATASET_ID);

        // Use one of our CSVs from Integration tests to reset the dataset (in case it's been changed).
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        IntegrationUtility.replaceNew(sinkProducer, sinkDdl, UNITTEST_PORT_RESULT_DATASET_ID, twoRowsFile, true);
        // Query for the rows of the sink dataset
        int sinkTotalRows = getTotalRows(UNITTEST_PORT_RESULT_DATASET_ID);
        // Simple test before we proceed: we want to make sure that the datasets
        // do not have the same amount of rows.
        TestCase.assertEquals(2, sinkTotalRows);
        TestCase.assertNotSame(sourceTotalRows, sinkTotalRows);

        // Perform the test operation...
        PortUtility.portContents(sourceProducer, sinkProducer,
                UNITTEST_DATASET_ID, UNITTEST_PORT_RESULT_DATASET_ID, PublishMethod.replace);
        // Grab both datasets' contents
        List<UnitTestDataset> sourceRows = sinkProducer.query(UNITTEST_DATASET_ID, SoqlQuery.SELECT_ALL, UnitTestDataset.LIST_TYPE);
        List<UnitTestDataset> sinkRows = sinkProducer.query(UNITTEST_PORT_RESULT_DATASET_ID, SoqlQuery.SELECT_ALL, UnitTestDataset.LIST_TYPE);

        // Run tests that ensure that the sink dataset's contents have been completely replaced
        // and are now identical to the source dataset's contents.
        TestCase.assertEquals(sourceRows.size(), sinkRows.size());
        for (int i = 0; i < sourceRows.size(); i++) {
            TestCase.assertEquals(sourceRows.get(i).getName(), sinkRows.get(i).getName());
            TestCase.assertEquals(sourceRows.get(i).getName_2(), sinkRows.get(i).getName_2());
            TestCase.assertEquals(sourceRows.get(i).getAnother_name(), sinkRows.get(i).getAnother_name());
            TestCase.assertEquals(sourceRows.get(i).getDate(), sinkRows.get(i).getDate());
        }
    }

}

