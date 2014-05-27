package com.socrata.datasync.config.userpreferences;

import com.socrata.datasync.TestBase;
import junit.framework.TestCase;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 1/8/14
 */
public class UserPreferencesTest extends TestBase {
    @Test
    public void testLoadUserPreferencesFromFile() throws IOException {
        File configFile = new File(PATH_TO_PROD_CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        UserPreferences userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
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
    public void testLoadUserPreferencesFromJavaPreferences() throws IOException {
        UserPreferencesJava userPrefs = new UserPreferencesJava();

        userPrefs.saveDomain("https://sandbox.demo.socrata.com");
        userPrefs.saveUsername("testuser@gmail.com");
        userPrefs.savePassword("OpenData");
        userPrefs.saveAPIKey("D8Atrg62F2j017ZTdkMpuZ9vY");
        userPrefs.saveAdminEmail("admin@something.com");
        userPrefs.saveEmailUponError(false);
        userPrefs.saveLogDatasetID("abcd-1234");
        userPrefs.saveOutgoingMailServer("smtp.something.com");
        userPrefs.saveSMTPPort("21");
        userPrefs.saveSSLPort("47");
        userPrefs.saveSMTPUsername("test@something.com");
        userPrefs.saveSMTPPassword("smtppass");
        userPrefs.saveFilesizeChunkingCutoffMB(10);
        userPrefs.saveNumRowsPerChunk(10000);

        TestCase.assertEquals("https://sandbox.demo.socrata.com", userPrefs.getDomain());
        TestCase.assertEquals("testuser@gmail.com", userPrefs.getUsername());
        TestCase.assertEquals("OpenData", userPrefs.getPassword());
        TestCase.assertEquals("D8Atrg62F2j017ZTdkMpuZ9vY", userPrefs.getAPIKey());
        TestCase.assertEquals("admin@something.com", userPrefs.getAdminEmail());
        TestCase.assertEquals(false, userPrefs.emailUponError());
        TestCase.assertEquals("abcd-1234", userPrefs.getLogDatasetID());
        TestCase.assertEquals("smtp.something.com", userPrefs.getOutgoingMailServer());
        TestCase.assertEquals("21", userPrefs.getSmtpPort());
        TestCase.assertEquals("47", userPrefs.getSslPort());
        TestCase.assertEquals("test@something.com", userPrefs.getSmtpUsername());
        TestCase.assertEquals("smtppass", userPrefs.getSmtpPassword());
        TestCase.assertEquals("10", userPrefs.getFilesizeChunkingCutoffMB());
        TestCase.assertEquals("10000", userPrefs.getNumRowsPerChunk());
    }
}
