package com.socrata.datasync.preferences;

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
        File configFile = new File(PATH_TO_CONFIG_FILE);
        ObjectMapper mapper = new ObjectMapper();
        UserPreferences userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
        TestCase.assertEquals("https://sandbox.demo.socrata.com", userPrefs.getDomain());
        TestCase.assertEquals("testuser@gmail.com", userPrefs.getUsername());
        TestCase.assertEquals("OpenData", userPrefs.getPassword());
        TestCase.assertEquals("D8Atrg62F2j017ZTdkMpuZ9vY", userPrefs.getAPIKey());
        TestCase.assertEquals("admin@something.com", userPrefs.getAdminEmail());
        TestCase.assertEquals(false, userPrefs.emailUponError());
        TestCase.assertEquals("abcd-1234", userPrefs.getLogDatasetID());
        TestCase.assertEquals("smtp.something.com", userPrefs.getOutgoingMailServer());
        TestCase.assertEquals("21", userPrefs.getSMTPPort());
        TestCase.assertEquals("47", userPrefs.getSSLPort());
        TestCase.assertEquals("test@something.com", userPrefs.getSMTPUsername());
        TestCase.assertEquals("smtppass", userPrefs.getSMTPPassword());
        TestCase.assertEquals("10", userPrefs.getFilesizeChunkingCutoffMB());
        TestCase.assertEquals("10000", userPrefs.getNumRowsPerChunk());
    }
}
