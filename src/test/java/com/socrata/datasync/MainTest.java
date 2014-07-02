package com.socrata.datasync;

import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.publishers.Soda2Publisher;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import junit.framework.TestCase;
import org.apache.commons.cli.ParseException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 2/10/14
 *
 * This contains integration tests that verify command line/headless mode functionality
 */
public class MainTest extends TestBase {

    @Test
    public void testHeadlessReplaceViaHTTP() throws ParseException, SodaError, InterruptedException, IOException, LongRunningQueryException {
        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        Soda2Publisher.replaceNew(createProducer(), createSodaDdl(), UNITTEST_DATASET_ID, twoRowsFile, true);

        String[] args = {"-c", PATH_TO_CONFIG_FILE,
                         "-i", UNITTEST_DATASET_ID,
                         "-f", "src/test/resources/datasync_unit_test_three_rows.csv",
                         "-m", PublishMethod.replace.toString(),
                         "-h", "true",
                         "-pf", "false"};
        Main.main(args);
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testHeadlessReplaceViaFTP() throws ParseException, SodaError, InterruptedException, IOException, LongRunningQueryException {
        // Ensures dataset is in known state (2 rows)
        File twoRowsFile = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        Soda2Publisher.replaceNew(createProducer(), createSodaDdl(), UNITTEST_DATASET_ID, twoRowsFile, true);

        String[] args = {"-c", PATH_TO_CONFIG_FILE,
                         "-i", UNITTEST_DATASET_ID,
                         "-f", "src/test/resources/datasync_unit_test_three_rows.csv",
                         "-m", PublishMethod.replace.toString(),
                         "-h", "true",
                         "-pf", "true",
                         "-sc", "src/test/resources/datasync_unit_test_three_rows_control.json"};
        Main.main(args);
        TestCase.assertEquals(3, getTotalRows(UNITTEST_DATASET_ID));
    }

    @Test
    public void testRunCommandOutput() {
         String pathToSaveJobFile = "/Users/someone/somedirectory/some.sij";
         String expected = "java -jar \".*\" \"" + pathToSaveJobFile + "\"";
        TestCase.assertTrue(Utils.getRunJobCommand(pathToSaveJobFile).matches(expected));
    }
}
