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
    Boolean columnStatistics = true;
    Boolean setAsideErrors = false;
    String encoding = "utf-8";
    String timezone = "UTC";
    String[] columns = {"boo", "bar", "baz"};

//    String fileTypeInnardsStart = "{" +
//            "\"columns\":[\"boo\",\"bar\",\"baz\"]," +
//            "\"emptyTextIsNull\":" + emptyTextIsNull + "," +
//            "\"encoding\":\"" + encoding + "\"," +
//            "\"fixedTimestampFormat\":[\"ISO8601\",\"MM/dd/yy\",\"MM/dd/yyyy\",\"dd-MMM-yyyy\",\"MM/dd/yyyy HH:mm:ss a Z\",\"MM/dd/yyyy HH:mm:ss a\"]," +
//            "\"floatingTimestampFormat\":[\"ISO8601\",\"MM/dd/yy\",\"MM/dd/yyyy\",\"dd-MMM-yyyy\",\"MM/dd/yyyy HH:mm:ss a Z\",\"MM/dd/yyyy HH:mm:ss a\"]," +
//            "\"ignoreColumns\":[]," +
//            "\"overrides\":{},";
//    String fileTypeInnardsStartReplace = "{" +
//            "\"columnStatistics\":" + columnStatistics + "," +
//            fileTypeInnardsStart;
//
//    String fileTypeInnardsStartAppend = "{" +
//            fileTypeInnardsStart;


    String fileTypeInnardsEnd =
            "\"setAsideErrors\":" + setAsideErrors + "," +
            "\"skip\":" + skip + "," +
            "\"timezone\":\"" + timezone + "\"," +
            "\"trimServerWhitespace\":" + trimSpace + "," +
            "\"trimWhitespace\":" + trimSpace +"," +
            "\"useSocrataGeocoding\":" + useGeocoding +
        "}";

    ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    private String getFileTypeInnardsStart(PublishMethod method)
    {
        StringBuffer fileTypeInnards = new StringBuffer("{");
        //Replace adds columns statistics to the FTC
        if (method.equals(PublishMethod.replace))
            fileTypeInnards.append("\"columnStatistics\":" + columnStatistics + ",");
        fileTypeInnards.append("\"columns\":[\"boo\",\"bar\",\"baz\"]," +
                "\"emptyTextIsNull\":" + emptyTextIsNull + "," +
                "\"encoding\":\"" + encoding + "\"," +
                "\"fixedTimestampFormat\":[\"ISO8601\",\"MM/dd/yy\",\"MM/dd/yyyy\",\"dd-MMM-yyyy\",\"MM/dd/yyyy HH:mm:ss a Z\",\"MM/dd/yyyy HH:mm:ss a\"]," +
                "\"floatingTimestampFormat\":[\"ISO8601\",\"MM/dd/yy\",\"MM/dd/yyyy\",\"dd-MMM-yyyy\",\"MM/dd/yyyy HH:mm:ss a Z\",\"MM/dd/yyyy HH:mm:ss a\"]," +
                "\"ignoreColumns\":[]," +
                "\"overrides\":{},");
        return fileTypeInnards.toString();
    }

    @Test
    public void testDefaultControlFileGenerationReplaceCsv() throws SodaError, InterruptedException, IOException {
        String expectedJson = "{" +
                "\"action\":\"Replace\"," +
                "\"csv\":" +
                    getFileTypeInnardsStart(PublishMethod.replace) +
                    "\"quote\":\"\\\"\"," +
                    "\"separator\":\",\"," +
                    fileTypeInnardsEnd +
                "}";

        ControlFile control = ControlFile.generateControlFile("some_file.csv", PublishMethod.replace, columns, true, true);
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
        ControlFile control = ControlFile.generateControlFile("some_file.csv", PublishMethod.delete, new String[]{"myid"}, true,true);
        String actualJson = mapper.writeValueAsString(control);
        TestCase.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testDefaultControlFileGenerationTsv() throws SodaError, InterruptedException, IOException {
        String expectedJson = "{" +
                "\"action\":\"Append\"," +
                "\"tsv\":" +
                getFileTypeInnardsStart(PublishMethod.append) +
                "\"quote\":\"\\u0000\"," +
                "\"separator\":\"\\t\"," +
                fileTypeInnardsEnd +
                "}";

        ControlFile control = ControlFile.generateControlFile("some_file.tsv", PublishMethod.append, columns, true, true);
        String actualJson = mapper.writeValueAsString(control);
        TestCase.assertEquals(expectedJson, actualJson);
    }

    @Test
    public void testDeserializationDefaultControlFile() throws IOException {
        String controlFileJson = "{" +
                "\"action\":\"Replace\"," +
                "\"csv\":" +
                getFileTypeInnardsStart(PublishMethod.replace) +
                "\"quote\":\"\\\"\"," +
                "\"separator\":\",\"," +
                fileTypeInnardsEnd +
                "}";
        ControlFile cf = mapper.readValue(controlFileJson, ControlFile.class);
        TestCase.assertEquals("Replace", cf.action);
        TestCase.assertNotNull(cf.getCsvFtc());
        TestCase.assertNull(cf.getTsvFtc());
        TestCase.assertEquals(3, cf.getCsvFtc().columns.length);
        TestCase.assertEquals(skip, cf.getCsvFtc().skip);
        TestCase.assertEquals(6, cf.getCsvFtc().fixedTimestampFormat.length);
        TestCase.assertEquals(6, cf.getCsvFtc().floatingTimestampFormat.length);
        TestCase.assertEquals(encoding, cf.getCsvFtc().encoding);
        TestCase.assertEquals(emptyTextIsNull, cf.getCsvFtc().emptyTextIsNull);
        TestCase.assertEquals(trimSpace, cf.getCsvFtc().trimServerWhitespace);
        TestCase.assertEquals(trimSpace, cf.getCsvFtc().trimWhitespace);
        TestCase.assertNull(cf.getCsvFtc().escape);
        TestCase.assertTrue(cf.getCsvFtc().useSocrataGeocoding);
        TestCase.assertNull(cf.getCsvFtc().syntheticLocations);
    }

    @Test
    public void testDeserializationCompleteControlFile() throws IOException {
        File controlFile = new File("src/test/resources/datasync_complex_control.json");
        ControlFile cf = mapper.readValue(controlFile, ControlFile.class);

        TestCase.assertEquals("someSillyUUIDforMyInternalUse", cf.opaque);
        TestCase.assertEquals("Delete", cf.action);
        TestCase.assertNotNull(cf.getCsvFtc());
        TestCase.assertNull(cf.getTsvFtc());
        TestCase.assertEquals(3, cf.getCsvFtc().columns.length);
        TestCase.assertEquals(1, cf.getCsvFtc().ignoreColumns.length);
        TestCase.assertEquals(skip, cf.getCsvFtc().skip);
        TestCase.assertEquals(4, cf.getCsvFtc().fixedTimestampFormat.length);
        TestCase.assertEquals(4, cf.getCsvFtc().floatingTimestampFormat.length);
        TestCase.assertEquals(encoding, cf.getCsvFtc().encoding);
        TestCase.assertEquals(emptyTextIsNull, cf.getCsvFtc().emptyTextIsNull);
        TestCase.assertEquals(trimSpace, cf.getCsvFtc().trimServerWhitespace);
        TestCase.assertEquals(trimSpace, cf.getCsvFtc().trimWhitespace);
        TestCase.assertEquals("TenPercent", cf.getCsvFtc().dropUninterpretableRows);
        TestCase.assertEquals("\\\\", cf.getCsvFtc().escape);
        TestCase.assertEquals(useGeocoding, cf.getCsvFtc().useSocrataGeocoding);
        TestCase.assertNotNull(cf.getCsvFtc().overrides);
        TestCase.assertNotNull(cf.getCsvFtc().syntheticLocations);
        TestCase.assertFalse(cf.getCsvFtc().overrides.get("column1").useSocrataGeocoding);

        ColumnOverride co = cf.getCsvFtc().overrides.get("column1");
        TestCase.assertTrue(co.emptyTextIsNull);
        TestCase.assertNull(co.trimServerWhitespace);
        TestCase.assertEquals(2, co.timestampFormat.length);
        TestCase.assertEquals("PDT", co.timezone);

        LocationColumn lc = cf.getCsvFtc().syntheticLocations.get("loc1");
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
        TestCase.assertEquals("ISO8601", cf.getCsvFtc().fixedTimestampFormat[0]);
    }

    @Test
    public void testLookupTimestampFormatting() throws IOException {
        File controlFile = new File("src/test/resources/datasync_complex_control.json");
        ControlFile cf = mapper.readValue(controlFile, ControlFile.class);
        Set<String> expectedFormats = new HashSet<>(
                Arrays.asList("ISO8601","MM/dd/yy", "MM/dd/yyyy", "dd-MMM-yyyy", "MM/YYYY", "YYYY/MM"));
        Set<String> actualFormats = cf.getCsvFtc().lookupTimestampFormatting();
        for(String expected : expectedFormats)
            TestCase.assertTrue(actualFormats.remove(expected));

        TestCase.assertEquals(0, actualFormats.size());
    }
}

