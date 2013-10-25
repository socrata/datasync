package com.socrata.datasync;

import com.socrata.datasync.job.IntegrationJob;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 10/18/13
 */
public class IntegrationJobTest extends TestBase {

    public static final String PATH_TO_SAVED_JOB_FILE = "src/test/resources/job_saved_v0.1.sij";

    @Test
    public void testOldJobFileDeserialization() throws IOException {
        IntegrationJob job = new IntegrationJob(PATH_TO_SAVED_JOB_FILE);

        TestCase.assertEquals(
                "/Users/adrian/Dropbox/Socrata_s/projects/data_integration/datasync/misc/honolulu_report2.csv",
                job.getFileToPublish());
        TestCase.assertEquals("n38h-y5wpx", job.getDatasetID());
        TestCase.assertEquals(PublishMethod.upsert, job.getPublishMethod());
        TestCase.assertEquals(
                "/Users/adrian/Dropbox/Socrata_s/projects/data_integration/datasync/misc/honolulu2.sij",
                job.getPathToSavedFile());
    }

    //@Test
    //public void testNewJobFileUID1Deserialization() throws IOException {

}
