package com.socrata.datasync.utilities;

import com.socrata.datasync.BlobId;
import com.socrata.datasync.JobId;
import com.socrata.datasync.CommitMessage;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.controlfile.FileTypeControl;
import junit.framework.TestCase;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;

public class PublishUtilityTest extends TestBase {
    ObjectMapper mapper = new ObjectMapper();

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
        ControlFile cf = new ControlFile("Replace", ftc, null);
        CommitMessage commit = new CommitMessage()
                .filename("hoo-ya.csv")
                .relativeTo("datasync/id/some-4by4/completed/2014/6/2/hoo-ya.csv")
                .chunks(Arrays.asList("1234", "2345"))
                .control(cf);
        String expectedJson = "{" +
                "\"chunks\":[\"1234\",\"2345\"]," +
                "\"control\":{" +
                    "\"action\":\"Replace\"," +
                    "\"csv\":{" +
                        "\"floatingTimestampFormat\":[\"ISO8601\"]" +
                    "}" +
                "}," +
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
}
