package com.socrata.datasync.config.controlfile;

import com.socrata.datasync.PublishMethod;
import com.socrata.exceptions.SodaError;
import junit.framework.TestCase;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ControlFileTest {

    Integer skip = 0;
    Boolean emptyTextIsNull = true;
    Boolean trimSpace = true;
    Boolean useGeocoding = true;
    String encoding = "utf-8";
    String timezone = "UTC";
    String[] columns = {"boo", "bar", "baz"};

    String fileTypeInnardsStart = "{" +
            "\"columns\":[\"boo\",\"bar\",\"baz\"]," +
            "\"emptyTextIsNull\":" + emptyTextIsNull + "," +
            "\"encoding\":\"" + encoding + "\"," +
            "\"fixedTimestampFormat\":[\"ISO8601\",\"MM/dd/yy\",\"MM/dd/yyyy\",\"dd-MMM-yyyy\"]," +
            "\"floatingTimestampFormat\":[\"ISO8601\",\"MM/dd/yy\",\"MM/dd/yyyy\",\"dd-MMM-yyyy\"]," +
            "\"ignoreColumns\":[]," +
            "\"overrides\":{},";
    String fileTypeInnardsEnd =
            "\"skip\":" + skip + "," +
            "\"timezone\":\"" + timezone + "\"," +
            "\"trimServerWhitespace\":" + trimSpace + "," +
            "\"trimWhitespace\":" + trimSpace +"," +
            "\"useSocrataGeocoding\":" + useGeocoding +
        "}";

    ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    @Test
    public void testDefaultControlFileGenerationReplaceCsv() throws SodaError, InterruptedException, IOException {
        String expectedJson = "{" +
                "\"action\":\"Replace\"," +
                "\"csv\":" +
                    fileTypeInnardsStart +
                    "\"quote\":\"\\\"\"," +
                    "\"separator\":\",\"," +
                    fileTypeInnardsEnd +
                "}";

        ControlFile control = ControlFile.generateControlFile("some_file.csv", PublishMethod.replace, columns, true);
        String actualJson = mapper.writeValueAsString(control);
        TestCase.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testDefaultControlFileGenerationDeleteCsv() throws SodaError, InterruptedException, IOException {
        String expectedJson = "{" +
                "\"action\":\"Delete\"," +
                "\"csv\":{" +
                    "\"columns\":[\"myid\"]," +
                    "\"encoding\":\"utf-8\"," +
                    "\"quote\":\"\\\"\"" +
                "}}";
        ControlFile control = ControlFile.generateControlFile("some_file.csv", PublishMethod.delete, new String[]{"myid"}, true);
        String actualJson = mapper.writeValueAsString(control);
        TestCase.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testDefaultControlFileGenerationTsv() throws SodaError, InterruptedException, IOException {
        String expectedJson = "{" +
                "\"action\":\"Append\"," +
                "\"tsv\":" +
                fileTypeInnardsStart +
                "\"quote\":\"\\u0000\"," +
                "\"separator\":\"\\t\"," +
                fileTypeInnardsEnd +
                "}";

        ControlFile control = ControlFile.generateControlFile("some_file.tsv", PublishMethod.append, columns, true);
        String actualJson = mapper.writeValueAsString(control);
        TestCase.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testDeserializationDefaultControlFile() throws IOException {
        String controlFileJson = "{" +
                "\"action\":\"Replace\"," +
                "\"csv\":" +
                fileTypeInnardsStart +
                "\"quote\":\"\\\"\"," +
                "\"separator\":\",\"," +
                fileTypeInnardsEnd +
                "}";
        ControlFile cf = mapper.readValue(controlFileJson, ControlFile.class);
        TestCase.assertEquals("Replace", cf.action);
        TestCase.assertNotNull(cf.csv);
        TestCase.assertNull(cf.tsv);
        TestCase.assertEquals(3, cf.csv.columns.length);
        TestCase.assertEquals(skip, cf.csv.skip);
        TestCase.assertEquals(4, cf.csv.fixedTimestampFormat.length);
        TestCase.assertEquals(4, cf.csv.floatingTimestampFormat.length);
        TestCase.assertEquals(encoding, cf.csv.encoding);
        TestCase.assertEquals(emptyTextIsNull, cf.csv.emptyTextIsNull);
        TestCase.assertEquals(trimSpace, cf.csv.trimServerWhitespace);
        TestCase.assertEquals(trimSpace, cf.csv.trimWhitespace);
        TestCase.assertNull(cf.csv.escape);
        TestCase.assertTrue(cf.csv.useSocrataGeocoding);
        TestCase.assertNull(cf.csv.syntheticLocations);
    }

    @Test
    public void testDeserializationCompleteControlFile() throws IOException {
        File controlFile = new File("src/test/resources/datasync_complex_control.json");
        ControlFile cf = mapper.readValue(controlFile, ControlFile.class);

        TestCase.assertEquals("someSillyUUIDforMyInternalUse", cf.opaque);
        TestCase.assertEquals("Delete", cf.action);
        TestCase.assertNotNull(cf.csv);
        TestCase.assertNull(cf.tsv);
        TestCase.assertEquals(3, cf.csv.columns.length);
        TestCase.assertEquals(1, cf.csv.ignoreColumns.length);
        TestCase.assertEquals(skip, cf.csv.skip);
        TestCase.assertEquals(4, cf.csv.fixedTimestampFormat.length);
        TestCase.assertEquals(4, cf.csv.floatingTimestampFormat.length);
        TestCase.assertEquals(encoding, cf.csv.encoding);
        TestCase.assertEquals(emptyTextIsNull, cf.csv.emptyTextIsNull);
        TestCase.assertEquals(trimSpace, cf.csv.trimServerWhitespace);
        TestCase.assertEquals(trimSpace, cf.csv.trimWhitespace);
        TestCase.assertEquals("TenPercent", cf.csv.dropUninterpretableRows);
        TestCase.assertEquals("\\\\", cf.csv.escape);
        TestCase.assertEquals(useGeocoding, cf.csv.useSocrataGeocoding);
        TestCase.assertNotNull(cf.csv.overrides);
        TestCase.assertNotNull(cf.csv.syntheticLocations);
        TestCase.assertFalse(cf.csv.overrides.get("column1").useSocrataGeocoding);

        ColumnOverride co = cf.csv.overrides.get("column1");
        TestCase.assertTrue(co.emptyTextIsNull);
        TestCase.assertNull(co.trimServerWhitespace);
        TestCase.assertEquals(2, co.timestampFormat.length);
        TestCase.assertEquals("PDT", co.timezone);

        LocationColumn lc = cf.csv.syntheticLocations.get("loc1");
        TestCase.assertEquals("WA", lc.state);
        TestCase.assertNull(lc.latitude);
    }

    @Test
    public void testSingleValueAsArray() throws IOException {
        String controlFileJson = "{" +
                "\"action\":\"Replace\"," +
                "\"csv\":" +
                    "{" +
                        "\"fixedTimestampFormat\":\"ISO8601\"" +
                    "}" +
                "}";
        ControlFile cf = mapper.readValue(controlFileJson, ControlFile.class);
        TestCase.assertEquals("ISO8601", cf.csv.fixedTimestampFormat[0]);
    }

    @Test
    public void testLookupTimestampFormatting() throws IOException {
        File controlFile = new File("src/test/resources/datasync_complex_control.json");
        ControlFile cf = mapper.readValue(controlFile, ControlFile.class);
        Set<String> expectedFormats = new HashSet<>(
                Arrays.asList("ISO8601","MM/dd/yy", "MM/dd/yyyy", "dd-MMM-yyyy", "MM/YYYY", "YYYY/MM"));
        Set<String> actualFormats = cf.csv.lookupTimestampFormatting();
        for(String expected : expectedFormats)
            TestCase.assertTrue(actualFormats.remove(expected));

        TestCase.assertEquals(0, actualFormats.size());
    }
}

