package com.socrata.datasync.utilities;

import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.datasync.PortUtility;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.publishers.Soda2Publisher;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        String newDatasetID = PortUtility.portSchema(sourceDdl, sinkDdl, UNITTEST_DATASET_ID, "", false);
        //System.out.println(newDatasetID);
        validatePortedSchema(newDatasetID);
    }

    @Test
    public void testPortSchemaNewBackend() throws SodaError, InterruptedException {
        // TODO remove conditional check
        // onyl necessary because New Backend is ONLY enabled on staging
        if(testOnStaging) {
            // Perform the test operation, saving the String return value.
            String newDatasetID = PortUtility.portSchema(sourceDdl, sinkDdl, UNITTEST_DATASET_ID, "", true);
            System.out.println("New Backend dataset: " + newDatasetID);

            // TODO validate that metadata denoting New Backend is true by hitting:
                // https://opendata.test-socrata.com/api/views/s5r5-8cth.json
                //DatasetInfo sinkMeta = sinkDdl.loadDatasetInfo(newDatasetID);

            validatePortedSchema(newDatasetID);
        }
    }

    private void validatePortedSchema(final String newDatasetID) throws SodaError, InterruptedException {
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
                //TestCase.assertEquals(sourceColumns.get(i).getPosition(), sinkColumns.get(i).getPosition());
            }

        } finally {
            sinkDdl.deleteDataset(newDatasetID);
        }
    }

    @Test
    public void testPortSchemaRenameDataeset() throws SodaError, InterruptedException {
        String destinationDatasetName = "New Dataset";
        // Perform the test operation, saving the String return value.
        String newDatasetID = PortUtility.portSchema(sourceDdl, sinkDdl, UNITTEST_DATASET_ID, destinationDatasetName, false);

        // Grab the necessary objects for testing.
        DatasetInfo sourceMeta = sourceDdl.loadDatasetInfo(UNITTEST_DATASET_ID);
        DatasetInfo sinkMeta = sinkDdl.loadDatasetInfo(newDatasetID);

        try {
            // Test the metadata (just the basics) via DatasetInfo.
            TestCase.assertEquals(destinationDatasetName, sinkMeta.getName());
        } finally {
            sinkDdl.deleteDataset(newDatasetID);
        }
    }

    @Test
    public void testPublishDataset() throws SodaError, InterruptedException {
        // Port a dataset's schema and confirm that it is unpublished by default.
        String unpublishedID = PortUtility.portSchema(sourceDdl, sinkDdl, UNITTEST_DATASET_ID, "", false);
        DatasetInfo source = sourceDdl.loadDatasetInfo(unpublishedID);
        TestCase.assertEquals("unpublished", source.getPublicationStage());

        // Perform the test operation.  Confirm the dataset is published afterwards.
        String publishedID = PortUtility.publishDataset(sinkDdl, unpublishedID);
        DatasetInfo sink = sinkDdl.loadDatasetInfo(publishedID);
        TestCase.assertEquals("published", sink.getPublicationStage());

        sinkDdl.deleteDataset(publishedID);
    }

    @Test
    public void testPortContentsUpsert() throws SodaError, InterruptedException, LongRunningQueryException, IOException {
        // Query for the rows of the source dataset.
        List<UnitTestDataset> sourceRows = sinkProducer.query(UNITTEST_DATASET_ID, SoqlQuery.SELECT_ALL, UnitTestDataset.LIST_TYPE);

        // Port a dataset's schema to get an empty copy to test with.
        String newDatasetID = PortUtility.portSchema(sourceDdl, sinkDdl, UNITTEST_DATASET_ID, "", false);

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
                TestCase.assertEquals(sourceRows.get(i).getId(), sinkRows.get(i).getId());
                TestCase.assertEquals(sourceRows.get(i).getName(), sinkRows.get(i).getName());
                TestCase.assertEquals(sourceRows.get(i).getAnother_name(), sinkRows.get(i).getAnother_name());
                TestCase.assertEquals(sourceRows.get(i).getDate(), sinkRows.get(i).getDate());
            }

        } finally {
            sinkDdl.deleteDataset(newDatasetID);
        }
    }

    @Test
    public void testPortContentsReplace() throws SodaError, InterruptedException, IOException, LongRunningQueryException {
        File threeRowsFile = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        Soda2Publisher.replaceNew(sinkProducer, sinkDdl, UNITTEST_DATASET_ID, threeRowsFile, true);

        // Query for the rows of the source dataset.
        int sourceTotalRows = getTotalRows(UNITTEST_DATASET_ID);

        // Use one of our CSVs from Integration tests to reset the dataset (in case it's been changed).
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        Soda2Publisher.replaceNew(sinkProducer, sinkDdl, UNITTEST_PORT_RESULT_DATASET_ID, twoRowsFile, true);
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
            TestCase.assertEquals(sourceRows.get(i).getId(), sinkRows.get(i).getId());
            TestCase.assertEquals(sourceRows.get(i).getName(), sinkRows.get(i).getName());
            TestCase.assertEquals(sourceRows.get(i).getAnother_name(), sinkRows.get(i).getAnother_name());
            TestCase.assertEquals(sourceRows.get(i).getDate(), sinkRows.get(i).getDate());
        }
    }

    @Test
    public void testSchemaAdaptation() {
        // set up original dataset schema
        String groupingKey = "grouping_aggregate";
        Dataset schema = new Dataset();

        Column colNoFormatting = new Column();
        colNoFormatting.setFieldName("col_without_formatting");

        Column colIgnorableFormatting = new Column();
        colIgnorableFormatting.setFieldName("col_with_ignorable_formatting");
        Map<String, String> ignorableFormatting = new HashMap<String,String>();
        ignorableFormatting.put("drill_down", "true");
        colIgnorableFormatting.setFormat(ignorableFormatting);

        Column colAggregatedFormatting = new Column();
        colAggregatedFormatting.setFieldName("col_with_aggregated_formatting");
        Map<String, String> aggregateFormatting = new HashMap<String,String>();
        aggregateFormatting.put(groupingKey, "count");
        colAggregatedFormatting.setFormat(aggregateFormatting);

        List<Column> columns = new ArrayList<Column>();
        columns.add(colNoFormatting);
        columns.add(colIgnorableFormatting);
        columns.add(colAggregatedFormatting);
        schema.setColumns(columns);

        // edit schema
        PortUtility.adaptSchemaForAggregates(schema);

        // test that edits are as expected
        List<Column> editedColumns = schema.getColumns();
        Column col1 = editedColumns.get(0);
        Column col2 = editedColumns.get(1);
        Column col3 = editedColumns.get(2);

        TestCase.assertNotNull(col1);
        TestCase.assertNotNull(col2);
        TestCase.assertNotNull(col3);

        // columns without formatting should be left alone
        TestCase.assertEquals(colNoFormatting, col1);

        // columns with ignorable formatting should be left alone
        TestCase.assertEquals(colIgnorableFormatting, col2);

        // columns with aggregation info in the formatting should have the aggregation info removed
        // and prepended to the field name:
        TestCase.assertFalse(col3.getFormat().containsKey(groupingKey));
        TestCase.assertEquals("count_col_with_aggregated_formatting", col3.getFieldName());
    }
}

