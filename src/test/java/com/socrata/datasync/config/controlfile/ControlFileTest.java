package com.socrata.datasync.config.controlfile;

import com.socrata.datasync.PublishMethod;
import com.socrata.exceptions.SodaError;
import junit.framework.TestCase;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ControlFileTest {

    Integer skip = 0;
    Boolean emptyTextIsNull = true;
    Boolean trimSpace = true;
    Boolean useGeocoding = true;
    Boolean ignoreServerLatLong = false;
    String encoding = "utf-8";
    String timezone = "UTC";
    String[] columns = {"boo", "bar", "baz"};

    String fileTypeInnardsStart = "{" +
            "\"columns\":[\"boo\",\"bar\",\"baz\"]," +
            "\"emptyTextIsNull\":" + emptyTextIsNull + "," +
            "\"encoding\":\"" + encoding + "\"," +
            "\"fixedTimestampFormat\":[\"ISO8601\",\"MM/dd/yy\",\"MM/dd/yyyy\",\"dd-MMM-yyyy\"]," +
            "\"floatingTimestampFormat\":[\"ISO8601\",\"MM/dd/yy\",\"MM/dd/yyyy\",\"dd-MMM-yyyy\"],";
    String fileTypeInnardsEnd =
            "\"skip\":" + skip + "," +
            "\"timezone\":\"" + timezone + "\"," +
            "\"trimServerWhitespace\":" + trimSpace + "," +
            "\"trimWhitespace\":" + trimSpace +"," +
            "\"useSocrataGeocoding\":" + useGeocoding +
        "}";

    ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    @Test
    public void testDefaultControlFileGenerationCsv() throws SodaError, InterruptedException, IOException {
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
        TestCase.assertNull(cf.csv.ignoreServerLatLong);
        TestCase.assertNull(cf.csv.overrides);
        TestCase.assertNull(cf.csv.syntheticLocations);
    }

    @Test
    public void testDeserializationCompleteControlFile() throws IOException {
        String state = "WA";
        String otherTimezone = "PDT";

        LocationColumns locations = new LocationColumns()
                .address("1234 Lane")
                .city("Seattle")
                .state(state)
                .zip("98125");

        ColumnOverride override = new ColumnOverride()
                .emptyTextIsNull(true)
                .fixedTimestampFormat(new String[]{"MM/YYYY", "YYYY/MM"})
                .floatingTimestampFormat(new String[]{"MM/YYYY", "YYYY/MM"})
                .ignoreServerLatLong(false)
                .trimWhitespace(false)
                .timezone(otherTimezone);


        Map<String, LocationColumns> syntheticLocations = new HashMap<String, LocationColumns>();
        syntheticLocations.put("loc1", locations);

        Map<String, ColumnOverride> overrides = new HashMap<String, ColumnOverride>();
        overrides.put("column1", override);
        overrides.put("column2", override);

        String controlFileJson = "{" +
                "\"action\":\"Delete\"," +
                "\"csv\":" +
                fileTypeInnardsStart +
                "\"escape\":\"\\\\\\\\\"," +
                "\"ignoreServerLatLong\":" + ignoreServerLatLong + "," +
                "\"separator\":\",\"," +
                "\"overrides\":" + mapper.writeValueAsString(overrides) + "," +
                "\"syntheticLocations\":" + mapper.writeValueAsString(syntheticLocations) + "," +
                fileTypeInnardsEnd +
                "}";

        ControlFile cf = mapper.readValue(controlFileJson, ControlFile.class);
        TestCase.assertEquals("Delete", cf.action);
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
        TestCase.assertEquals("\\\\", cf.csv.escape);
        TestCase.assertEquals(ignoreServerLatLong, cf.csv.ignoreServerLatLong);
        TestCase.assertNotNull(cf.csv.overrides);
        TestCase.assertNotNull(cf.csv.syntheticLocations);
        TestCase.assertFalse(cf.csv.overrides.get("column1").ignoreServerLatLong);

        ColumnOverride co = cf.csv.overrides.get("column1");
        TestCase.assertTrue(co.emptyTextIsNull);
        TestCase.assertNull(co.trimServerWhitespace);
        TestCase.assertEquals(2, co.fixedTimestampFormat.length);
        TestCase.assertEquals(otherTimezone, co.timezone);

        LocationColumns lc = cf.csv.syntheticLocations.get("loc1");
        TestCase.assertEquals(state, lc.state);
        TestCase.assertNull(lc.latitude);
    }

    @Test
    public void testHasColumns() {
        ControlFile noColumns = ControlFile.generateControlFile("blah.csv", PublishMethod.replace, null, false);
        ControlFile emptyColumns = ControlFile.generateControlFile("blah.csv", PublishMethod.replace, new String[]{}, false);
        ControlFile columnsInCsv = ControlFile.generateControlFile("blah.csv", PublishMethod.replace, new String[]{"col1"}, false);
        ControlFile columnsInTsv = ControlFile.generateControlFile("blah.tsv", PublishMethod.replace, new String[]{"col1"}, false);

        TestCase.assertFalse(noColumns.hasColumns());
        TestCase.assertFalse(emptyColumns.hasColumns());
        TestCase.assertTrue(columnsInCsv.hasColumns());
        TestCase.assertTrue(columnsInTsv.hasColumns());
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
}

