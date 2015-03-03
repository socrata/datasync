package com.socrata.datasync.utilities;

import com.socrata.datasync.TestBase;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.controlfile.FileTypeControl;
import com.socrata.datasync.deltaimporter2.BlobId;
import com.socrata.datasync.deltaimporter2.CommitMessage;
import com.socrata.datasync.deltaimporter2.JobId;
import com.socrata.datasync.deltaimporter2.LogItem;
import junit.framework.TestCase;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;

public class DeltaImporter2PublisherTest extends TestBase {
    ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

   @Test
    public void testDeserializationReturnIds() throws IOException {

        String typicalBlobJson = "{\"blobId\":\"xxxx-xxxx\"}";
        BlobId id1 = mapper.readValue(typicalBlobJson, BlobId.class);
        TestCase.assertEquals("xxxx-xxxx", id1.blobId);

        String typicalJobJson = "{\"jobId\":\"yyyyyy\"}";
        JobId id2 = mapper.readValue(typicalJobJson, JobId.class);
        TestCase.assertEquals("yyyyyy", id2.jobId);

        String emptyJson = "{}";
        BlobId id3 = mapper.readValue(emptyJson, BlobId.class);
        JobId id4 = mapper.readValue(emptyJson, JobId.class);
        TestCase.assertNull(id3.blobId);
        TestCase.assertNull(id4.jobId);

        String extraStuffJson = "{\"blobId\":\"some-4by4\", \"junk\":0}";
        BlobId id5 = mapper.readValue(extraStuffJson, BlobId.class);
        TestCase.assertEquals("some-4by4", id5.blobId);
    }

    @Test
    public void testSerializationFullCommitMessage() throws IOException {
        FileTypeControl ftc = new FileTypeControl().floatingTimestampFormat(new String[]{"ISO8601"});
        ControlFile cf = new ControlFile("Replace", null, ftc, null, true);
        CommitMessage commit = new CommitMessage()
                .filename("hoo-ya.csv")
                .relativeTo("datasync/id/some-4by4/completed/2014/6/2/hoo-ya.csv")
                .chunks(Arrays.asList("1234", "2345"))
                .control(cf)
                .expectedSize(11001001L);
        String expectedJson = "{" +
                "\"chunks\":[\"1234\",\"2345\"]," +
                "\"control\":{" +
                    "\"action\":\"Replace\"," +
                    "\"csv\":{" +
                        "\"floatingTimestampFormat\":[\"ISO8601\"]" +
                    "}," +
                    "\"replacePreviousQueued\":true" +
                "}," +
                "\"expectedSize\":11001001," +
                "\"filename\":\"hoo-ya.csv\"," +
                "\"relativeTo\":\"datasync/id/some-4by4/completed/2014/6/2/hoo-ya.csv\"" +
            "}";
        TestCase.assertEquals(expectedJson, mapper.writeValueAsString(commit));
    }

    @Test
    public void testSerializationIncompleteCommitMessage() throws IOException {
        CommitMessage commit = new CommitMessage()
                .filename("hoo-ya.csv")
                .chunks(Arrays.asList("1234", "2345"));
        String expectedJson = "{" +
                "\"chunks\":[\"1234\",\"2345\"]," +
                "\"filename\":\"hoo-ya.csv\"," +
                "\"relativeTo\":null" +
                "}";
        TestCase.assertEquals(expectedJson, mapper.writeValueAsString(commit));
    }

    @Test
    public void testLogDeserialization() throws IOException, URISyntaxException {
        InputStream logText = new FileInputStream(new File("src/test/resources/delta_importer_2_log_text.json"));

        LogItem[] deltaLog = mapper.readValue(logText, LogItem[].class);
        String[] logTypes = {"committing-job", "committed-job", "processing", "applying-diff-time",
                             "counting-records-time", "reading-metadata", "reading-new-data-time",
                             "reading-and-sorting-time", "generating-upsert-time", "finished",
                             "upserting-time", "storing-completed-time", "success", "failure","processing-time"};

        TestCase.assertEquals(logTypes.length, deltaLog.length);
        for (int i = 0; i < deltaLog.length; i++) {
            TestCase.assertEquals(logTypes[i], deltaLog[i].type);
            if (deltaLog[i].type.equalsIgnoreCase("finished")) {
                TestCase.assertEquals(new Integer(1), deltaLog[i].getInserted());
                TestCase.assertEquals(new Integer(0), deltaLog[i].getUpdated());
                TestCase.assertEquals(new Integer(1), deltaLog[i].getDeleted());
                TestCase.assertEquals(new Integer(0), deltaLog[i].getErrors());
            }
        }

    }

}
