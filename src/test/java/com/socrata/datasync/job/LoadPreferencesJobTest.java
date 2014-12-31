package com.socrata.datasync.job;

import com.socrata.datasync.Main;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import junit.framework.TestCase;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.junit.Before;
import org.junit.Test;

import java.util.prefs.BackingStoreException;

public class LoadPreferencesJobTest {

    private LoadPreferencesJob job;
    CommandLineParser parser;
    CommandLineOptions cmd = new CommandLineOptions();

    @Before
    public void initialize() {
        job = new LoadPreferencesJob();
        parser = new PosixParser();
    }

    @Test
    public void testValidationOfArgs() throws ParseException {

        String[] goodArgs = {"-c", "myConfig.json"};
        String[] incompleteArgs = {"-t", "LoadPreferences"};

        TestCase.assertTrue(job.validateArgs(parser.parse(cmd.options, goodArgs)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs)));
    }

    @Test
    public void testLoadCompletePreferencesWithClearStart() throws ParseException, BackingStoreException {
        UserPreferencesJava userPrefs = new UserPreferencesJava();
        userPrefs.clear();

        String[] args = {"-t", "LoadPreferences", "-c", "src/test/resources/basic_test_config.json"};
        Main.main(args);
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
    public void testLoadIncompletePreferencesWithClearStart() throws ParseException, BackingStoreException {
        UserPreferencesJava userPrefs = new UserPreferencesJava();
        userPrefs.clear();

        String[] args = {"-t", "LoadPreferences", "-c", "src/test/resources/simple_config.json"};
        Main.main(args);
        TestCase.assertEquals("https://someDomain.com", userPrefs.getDomain());
        TestCase.assertEquals("some.user@gmail.com", userPrefs.getUsername());
        TestCase.assertEquals("somePassword", userPrefs.getPassword());
        TestCase.assertEquals("5oMeTokEN", userPrefs.getAPIKey());
        TestCase.assertEquals("https://someDomain.com", userPrefs.getProxyHost());
        TestCase.assertEquals("8080", userPrefs.getProxyPort());
        userPrefs.clear();
    }

    @Test
    public void testLoadIncompletePreferencesWithMessyStart() throws ParseException, BackingStoreException {
        // fill up java preference node with old junk
        UserPreferencesJava userPrefs = new UserPreferencesJava();
        String[] argsOld = {"-t", "LoadPreferences", "-c", "src/test/resources/basic_test_config.json"};
        Main.main(argsOld);

        // run new load job
        String[] argsNew = {"-t", "LoadPreferences", "-c", "src/test/resources/simple_config.json"};
        Main.main(argsNew);

        TestCase.assertEquals("https://someDomain.com", userPrefs.getDomain());
        TestCase.assertEquals("some.user@gmail.com", userPrefs.getUsername());
        TestCase.assertEquals("somePassword", userPrefs.getPassword());
        TestCase.assertEquals("5oMeTokEN", userPrefs.getAPIKey());
        TestCase.assertEquals("https://someDomain.com", userPrefs.getProxyHost());
        TestCase.assertEquals("8080", userPrefs.getProxyPort());
        TestCase.assertEquals("", userPrefs.getAdminEmail());
        TestCase.assertFalse(userPrefs.emailUponError());    // has default
        TestCase.assertEquals("", userPrefs.getLogDatasetID());
        TestCase.assertEquals("", userPrefs.getOutgoingMailServer());
        TestCase.assertEquals("", userPrefs.getSmtpPort());
        TestCase.assertEquals("465", userPrefs.getSslPort());    // has default
        TestCase.assertEquals("", userPrefs.getSmtpUsername());
        TestCase.assertEquals("", userPrefs.getSmtpPassword());
        TestCase.assertEquals("10", userPrefs.getFilesizeChunkingCutoffMB());     // has default
        TestCase.assertEquals("10000", userPrefs.getNumRowsPerChunk());     // has default
        userPrefs.clear();
    }

    @Test
    public void testConfiguration() throws ParseException {
        String[] args = {"-c", "src/test/resources/basic_test_config.json"};
        job.configure(parser.parse(cmd.options, args));

        UserPreferences userPrefs = job.getUserPrefs();

        TestCase.assertEquals("https://sandbox.demo.socrata.com", userPrefs.getDomain());
        TestCase.assertEquals("testuser@gmail.com", userPrefs.getUsername());
        TestCase.assertEquals("OpenData", userPrefs.getPassword());
        TestCase.assertEquals("D8Atrg62F2j017ZTdkMpuZ9vY", userPrefs.getAPIKey());
        TestCase.assertEquals("admin@something.com", userPrefs.getAdminEmail());
        TestCase.assertFalse(userPrefs.emailUponError());
        TestCase.assertEquals("smtp.something.com", userPrefs.getOutgoingMailServer());
        TestCase.assertEquals("21", userPrefs.getSmtpPort());
        TestCase.assertEquals("47", userPrefs.getSslPort());
        TestCase.assertEquals("test@something.com", userPrefs.getSmtpUsername());
        TestCase.assertEquals("smtppass", userPrefs.getSmtpPassword());
        TestCase.assertEquals("10", userPrefs.getFilesizeChunkingCutoffMB());
        TestCase.assertEquals("10000", userPrefs.getNumRowsPerChunk());
    }
}
