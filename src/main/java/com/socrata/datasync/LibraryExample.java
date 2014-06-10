package com.socrata.datasync;

import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.config.userpreferences.UserPreferencesLib;
import com.socrata.datasync.job.JobStatus;

import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 6/13/14
 */
public class LibraryExample {

    public static void main(String[] args) throws IOException {
        // Establish "global" configuration (User Preferences)
        UserPreferencesLib userPrefs = new UserPreferencesLib();
        userPrefs.setDomain("https://some.domain.org"); // must include the 'https://'
        userPrefs.setUsername("USERNAME");
        userPrefs.setPassword("PASSWORD");
        userPrefs.setAppToken("APP_TOKEN");

        // Optional uUser Preferences
        //userPrefs.setAdminEmail("admin@domain.org");
        //userPrefs.setEmailUponError(true);
        //userPrefs.setLogDatasetID("abcd-1234");
        //userPrefs.setOutgoingMailServer("host.com");
        //userPrefs.setSmtpPort("427");
        //userPrefs.setSslPort("225");
        //userPrefs.setSmtpUsername("someone@host.com");
        //userPrefs.setSmtpPassword("SMTP_PASSWORD");
        //userPrefs.setFilesizeChunkingCutoffMB("10");
        //userPrefs.setNumRowsPerChunk("10000");

        // Set up job parameters
        IntegrationJob jobToRun = new IntegrationJob(userPrefs);
        jobToRun.setDatasetID("abcd-1234");
        jobToRun.setFileToPublish("data_file.csv");
        jobToRun.setPublishMethod(PublishMethod.replace);
        jobToRun.setFileToPublishHasHeaderRow(true);

        // Uncommend to do replace via FTP (SmartUpdate)
        //jobToRun.setPublishViaFTP(true);
        //jobToRun.setPathToFTPControlFile("control.json");

        JobStatus status = jobToRun.run();
        if(status.isError()) {
            System.err.println("Job failed: " + status.getMessage());
        } else {
            System.out.println("Job ran successfully!");
        }
    }
}
