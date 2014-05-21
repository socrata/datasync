package com.socrata.datasync;

import com.socrata.datasync.preferences.UserPreferencesJava;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import junit.framework.TestCase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
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
        IntegrationUtility.replaceNew(createProducer(), createSodaDdl(), UNITTEST_DATASET_ID, twoRowsFile, true);

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
        IntegrationUtility.replaceNew(createProducer(), createSodaDdl(), UNITTEST_DATASET_ID, twoRowsFile, true);

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
    public void testHeadlessLoadPreferencesJob() throws ParseException {
        UserPreferencesJava userPrefsClearDomain = new UserPreferencesJava();
        userPrefsClearDomain.saveDomain("");

        String[] args = {"-t", "LoadPreferences",
                "-c", PATH_TO_PROD_CONFIG_FILE};
        Main.main(args);
        UserPreferencesJava userPrefs = new UserPreferencesJava();
        TestCase.assertEquals("https://sandbox.demo.socrata.com", userPrefs.getDomain());
        TestCase.assertEquals("testuser@gmail.com", userPrefs.getUsername());
        TestCase.assertEquals("OpenData", userPrefs.getPassword());
        TestCase.assertEquals("D8Atrg62F2j017ZTdkMpuZ9vY", userPrefs.getAPIKey());
        TestCase.assertEquals("admin@something.com", userPrefs.getAdminEmail());
        TestCase.assertEquals(false, userPrefs.emailUponError());
        TestCase.assertEquals("", userPrefs.getLogDatasetID());
        TestCase.assertEquals("smtp.something.com", userPrefs.getOutgoingMailServer());
        TestCase.assertEquals("21", userPrefs.getSmtpPort());
        TestCase.assertEquals("47", userPrefs.getSslPort());
        TestCase.assertEquals("test@something.com", userPrefs.getSmtpUsername());
        TestCase.assertEquals("smtppass", userPrefs.getSmtpPassword());
        TestCase.assertEquals("10", userPrefs.getFilesizeChunkingCutoffMB());
        TestCase.assertEquals("10000", userPrefs.getNumRowsPerChunk());
    }
}
