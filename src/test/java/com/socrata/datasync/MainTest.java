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
    public void testValidCommandArgsIntegrationJob() throws ParseException {
        CommandLineParser parser = new PosixParser();

        String[] args1 = {"-c", PATH_TO_CONFIG_FILE,
                "-i", "abcd-1234",
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", PublishMethod.delete.toString(),
                "-h", "false"};
        CommandLine cmd1 = parser.parse(Main.options, args1);
        TestCase.assertTrue(Main.commandArgsValid(cmd1));

        String[] args2 = {"-i", "abcd-1234",
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", PublishMethod.delete.toString(),
                "-h", "false"};
        CommandLine cmd2 = parser.parse(Main.options, args2);
        TestCase.assertTrue(Main.commandArgsValid(cmd2));

        String[] args3 = {"-t", "IntegrationJob",
                "-i", "abcd-1234",
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", PublishMethod.delete.toString(),
                "-h", "false"};
        CommandLine cmd3 = parser.parse(Main.options, args3);
        TestCase.assertTrue(Main.commandArgsValid(cmd3));
    }

    @Test
    public void testInvalidCommandArgsIntegrationJob() throws ParseException {
        CommandLineParser parser = new PosixParser();

        // Missing required argument: -i,--datasetID is required
        String[] args1 = {"-c", PATH_TO_CONFIG_FILE,
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", PublishMethod.replace.toString(),
                "-h", "true"};
        CommandLine cmd1 = parser.parse(Main.options, args1);
        TestCase.assertFalse(Main.commandArgsValid(cmd1));

        // Invalid argument: -h,--fileToPublishHasHeaderRow must be 'true' or 'false'
        String[] args2 = {"-c", PATH_TO_CONFIG_FILE,
                "-i", "abcd-1234",
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", PublishMethod.replace.toString(),
                "-h", "invalid"};
        CommandLine cmd2 = parser.parse(Main.options, args2);
        TestCase.assertFalse(Main.commandArgsValid(cmd2));

        // Missing required argument: -f,--fileToPublish is required
        String[] args3 = {"-c", PATH_TO_CONFIG_FILE,
                "-i", "abcd-1234",
                "-m", PublishMethod.replace.toString(),
                "-h", "true"};
        CommandLine cmd3 = parser.parse(Main.options, args3);
        TestCase.assertFalse(Main.commandArgsValid(cmd3));

        // Invalid argument: -m,--publishMethod must be replace,upsert,append,delete
        String[] args4 = {"-c", PATH_TO_CONFIG_FILE,
                "-i", "abcd-1234",
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", "invalid",
                "-h", "false"};
        CommandLine cmd4 = parser.parse(Main.options, args4);
        TestCase.assertFalse(Main.commandArgsValid(cmd4));

        // Invalid argument: -pf,--publishViaFTP must be 'true' or 'false'
        String[] args5 = {"-c", PATH_TO_CONFIG_FILE,
                "-i", "abcd-1234",
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", PublishMethod.delete.toString(),
                "-h", "false",
                "-pf", "invalid"};
        CommandLine cmd5 = parser.parse(Main.options, args5);
        TestCase.assertFalse(Main.commandArgsValid(cmd5));

        // Invalid argument: -t,--jobType must be 'true' or 'false'
        String[] args6 = {"-t", "invalid",
                "-c", PATH_TO_CONFIG_FILE,
                "-i", "abcd-1234",
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", PublishMethod.delete.toString(),
                "-h", "false"};
        CommandLine cmd6 = parser.parse(Main.options, args6);
        TestCase.assertFalse(Main.commandArgsValid(cmd6));
    }

    @Test
    public void testValidCommandArgsPortJob() throws ParseException {
        CommandLineParser parser = new PosixParser();
        String[] args1 = {"-t", "PortJob",
                "-c", PATH_TO_CONFIG_FILE,
                "-pm", "copy_all",
                "-pd1", DOMAIN,
                "-pi1", UNITTEST_DATASET_ID,
                "-pd2", DOMAIN,
                "-pp", "true",
                "-pdt", "New UnitTest Dataset"};
        CommandLine cmd = parser.parse(Main.options, args1);
        TestCase.assertTrue(Main.commandArgsValid(cmd));
    }

    @Test
    public void testInvalidCommandArgsPortJob() throws ParseException {
        // TODO
    }

    @Test
    public void testHeadlessLoadPreferencesJob() throws ParseException {
        UserPreferencesJava userPrefsClearDomain = new UserPreferencesJava();
        userPrefsClearDomain.saveDomain("");

        String[] args = {"-t", "LoadPreferences",
                "-c", PATH_TO_CONFIG_FILE};
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

    @Test
    public void testInvalidCommandArgsLoadPreferencesJob() throws ParseException {
        CommandLineParser parser = new PosixParser();

        // Missing required argument: -i,--datasetID is required
        String[] args1 = {"-c", PATH_TO_CONFIG_FILE,
                "-f", "src/test/resources/datasync_unit_test_two_rows.csv",
                "-m", PublishMethod.replace.toString(),
                "-h", "true"};
        CommandLine cmd = parser.parse(Main.options, args1);
        TestCase.assertFalse(Main.commandArgsValid(cmd));
    }

}
