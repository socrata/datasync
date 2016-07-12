package com.socrata.datasync.job;

import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PublishDataset;
import com.socrata.datasync.config.CommandLineOptions;
import junit.framework.TestCase;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class PortJobTest {

    private PortJob job;
    CommandLineParser parser;
    CommandLineOptions cmd;

    public static final String PATH_TO_SAVED_SPJ_V0dot3 = "src/test/resources/job_saved_v0.3.spj";

    @Before
    public void initialize() {
        job = new PortJob();
        parser = new PosixParser();
        cmd = new CommandLineOptions();
    }


    @Test
    public void testNewJobFileDeserialization() throws IOException {
        job = new PortJob(PATH_TO_SAVED_SPJ_V0dot3);

        TestCase.assertEquals(PortMethod.copy_all, job.getPortMethod());
        TestCase.assertEquals("https://louis.demo.socrata.com", job.getSourceSiteDomain());
        TestCase.assertEquals("w8e5-buaa", job.getSourceSetID());
        TestCase.assertEquals("https://louis.demo.socrata.com", job.getSinkSiteDomain());
        TestCase.assertEquals(PublishDataset.publish, job.getPublishDataset());
    }

    @Test
    public void testValidationOfArgs() throws ParseException {

        String[] goodArgs = {"-pm", "copy_all", "-pd1", "srcDomain", "-pi1", "4x4", "-pd2", "sinkDomain"};
        String[] incompleteArgs1 = {"-pd1", "srcDomain", "-pi1", "4x4", "-pd2", "sinkDomain"};
        String[] incompleteArgs2 = {"-pm", "copy_all", "-pi1", "4x4", "-pd2", "sinkDomain"};
        String[] incompleteArgs3 = {"-pm", "copy_all", "-pd1", "srcDomain", "-pd2", "sinkDomain"};
        String[] incompleteArgs4 = {"-pm", "copy_all", "-pd1", "srcDomain", "-pi1", "4x4"};
        String[] invalidArgs1 = {"-pm", "invalid", "-pd1", "srcDomain", "-pi1", "4x4", "-pd2", "sinkDomain"};

        TestCase.assertTrue(job.validateArgs(parser.parse(cmd.options, goodArgs)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs1)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs2)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs3)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, incompleteArgs4)));
        TestCase.assertFalse(job.validateArgs(parser.parse(cmd.options, invalidArgs1)));
    }

    @Test
    public void testConfiguration() throws ParseException {
        String[] args = {"-pm", "copy_all", "-pd1", "srcDomain", "-pi1", "4x4", "-pd2", "sinkDomain"};
        job.configure(parser.parse(cmd.options, args));

        TestCase.assertEquals(job.getPortMethod().toString(), PortMethod.copy_all.toString());
        TestCase.assertEquals(job.getSourceSiteDomain(), args[3]);
        TestCase.assertEquals(job.getSourceSetID(), args[5]);
        TestCase.assertEquals(job.getSinkSiteDomain(), args[7]);
    }
}
