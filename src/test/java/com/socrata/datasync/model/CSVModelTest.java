package com.socrata.datasync.model;

import com.socrata.datasync.config.controlfile.ControlFile;
import junit.framework.TestCase;
import com.fasterxml.jackson.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Created by franklinwilliams on 7/13/15.
 */
public class CSVModelTest {

    ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    @Test
    public void testColumnCount() throws IOException {
        ControlFile cf = getSimpleTestControlFile();
        CSVModel model = new CSVModel(cf);
        TestCase.assertEquals(model.getColumnCount(), 4);
    }

    @Test
    public void testHeaderParsing() throws IOException {
        ControlFile cf = getSimpleTestControlFile();
        CSVModel model = new CSVModel(cf);
        TestCase.assertEquals(model.getColumnName(0), "ID");
        TestCase.assertEquals(model.getColumnName(1), "Name");
        TestCase.assertEquals(model.getColumnName(2), "Another Name");
        TestCase.assertEquals(model.getColumnName(3), "Date");

        //Test that we generate dummy headers when there is no header row
        cf.getFileTypeControl().hasHeaderRow(false);
        CSVModel anotherModel = new CSVModel(cf);
        TestCase.assertTrue(anotherModel.getColumnName(0).startsWith("column_"));
    }

    @Test
    public void testRowParsing() throws IOException {
        ControlFile cf = getSimpleTestControlFile();
        CSVModel model = new CSVModel(cf);
        TestCase.assertEquals(3, model.getRowCount());
        TestCase.assertEquals("1", model.getValueAt(0, 0));

        //Test that we include the first row when has header row is false
        cf.getFileTypeControl().skip(0);
        cf.getFileTypeControl().hasHeaderRow(false);
        CSVModel anotherModel = new CSVModel(cf);
        TestCase.assertEquals(4, anotherModel.getRowCount());
        TestCase.assertEquals("ID", anotherModel.getValueAt(0, 0));
    }

    //Verify that we're able to initialize the model when there are blank rows
    @Test
    public void testSkippingBlankRows() throws IOException {
        ControlFile cf = getTestControlFileWithBlankRows();
        CSVModel model = new CSVModel(cf);
        TestCase.assertNotNull(model);
        TestCase.assertEquals(model.getRowCount(), 5);
    }

    private ControlFile getTestControlFile(String cfPath, String csvPath) throws IOException {
        File controlFile = new File(cfPath);
        ControlFile cf = mapper.readValue(controlFile, ControlFile.class);
        cf.getFileTypeControl().filePath = csvPath;
        //Setting these here rather than in the model to avoid introducing breaking changes to existing consumers
        //of the ControlFile
        cf.getFileTypeControl().hasHeaderRow(true);
        cf.getFileTypeControl().skip(1);
        return cf;
    }

    private ControlFile getSimpleTestControlFile() throws IOException {
        return getTestControlFile("src/test/resources/datasync_unit_test_three_rows_control.json","src/test/resources/datasync_unit_test_three_rows.csv");
    }

    private ControlFile getTestControlFileWithBlankRows() throws IOException {
        return getTestControlFile("src/test/resources/datasync_unit_test_three_rows_control.json","src/test/resources/datasync_unit_test_blank_rows.csv");
    }
}