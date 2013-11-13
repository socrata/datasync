package com.socrata.datasync.job;

import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PublishDataset;
import com.socrata.datasync.TestBase;
import junit.framework.TestCase;
import org.junit.Test;

import java.io.IOException;

/**
 * Author: Louis Fettet
 * Date: 11/7/13
 */
public class PortJobTest extends TestBase {

    public static final String PATH_TO_SAVED_SPJ_V0dot3 = "src/test/resources/job_saved_v0.3.spj";

    @Test
    public void testNewJobFileDeserialization() throws IOException {
        PortJob job = new PortJob(PATH_TO_SAVED_SPJ_V0dot3);

        TestCase.assertEquals(PortMethod.copy_all, job.getPortMethod());
        TestCase.assertEquals("https://louis.demo.socrata.com", job.getSourceSiteDomain());
        TestCase.assertEquals("w8e5-buaa", job.getSourceSetID());
        TestCase.assertEquals("https://louis.demo.socrata.com", job.getSinkSiteDomain());
        TestCase.assertEquals(PublishDataset.publish, job.getPublishDataset());
    }
}
