package com.socrata.datasync.job;

import com.socrata.datasync.Main;
import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PublishDataset;
import com.socrata.datasync.preferences.UserPreferences;
import junit.framework.TestCase;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class LoadPreferencesJobTest {

    private LoadPreferencesJob job;
    CommandLineParser parser;

    public static final String PATH_TO_SAVED_SPJ_V0dot3 = "src/test/resources/job_saved_v0.3.spj";

    @Before
    public void initialize() {
        job = new LoadPreferencesJob();
        parser = new PosixParser();
    }

    @Test
    public void testValidationOfArgs() throws ParseException {

        String[] goodArgs = {"-c", "myConfig.json"};
        String[] incompleteArgs = {"-t", "LoadPreferences"};

        TestCase.assertTrue(job.validateArgs(parser.parse(Main.options, goodArgs)));
        TestCase.assertFalse(job.validateArgs(parser.parse(Main.options, incompleteArgs)));
    }

    @Test
    public void testConfiguration() throws ParseException {
        String[] args = {"-c", "src/test/resources/basic_test_config.json"};
        job.configure(parser.parse(Main.options, args));

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
