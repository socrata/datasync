package com.socrata.datasync;

import com.socrata.datasync.config.controlfile.ControlFile;
import junit.framework.TestCase;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

public class UtilsTest {
    private static class ShortReadingByteArrayInputStream extends ByteArrayInputStream {
        private final Random rng;

        ShortReadingByteArrayInputStream(byte[] bytes, Random rng) {
            super(bytes);
            this.rng = rng;
        }

        @Override
        public int read(byte[] buf, int off, int len) {
            int shortLen;
            if(len <= 1) shortLen = len;
            else shortLen = 1 + rng.nextInt(len); // a number in [1, len]
            return super.read(buf, off, shortLen);
        }
    }

    @Test
    public void testReadChunk() throws Exception {
        Long seed = new Random().nextLong();
        Random rng = new Random(seed);

        for(int i = 0; i != 1000; ++i) {
            byte[] data = new byte[rng.nextInt(1000000)];
            rng.nextBytes(data);
            InputStream shortReader = new ShortReadingByteArrayInputStream(data, rng);
            byte[] target = new byte[rng.nextInt(data.length * 2)];
            int read = Utils.readChunk(shortReader, target, 0, target.length);
            int expectedToRead = Math.min(target.length, data.length);
            TestCase.assertEquals("Incorrect amount read; seed " + seed, expectedToRead, read);
            TestCase.assertTrue("Incorrect data read; seed " + seed,
                    Arrays.equals(Arrays.copyOf(data, expectedToRead), Arrays.copyOf(target, expectedToRead)));
        }
    }

    @Test
    public void testReadHeadersFromFile() throws IOException {
        File testFile1 = new File("src/test/resources/datasync_unit_test_three_rows.csv");
        ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        File controlFile = new File("src/test/resources/datasync_complex_control.json");
        ControlFile cf = mapper.readValue(controlFile, ControlFile.class);

        String[] expectedHeaders1 = new String[] {"ID", "Name", "Another Name", "Date"};
        String[] actualHeaders1 = Utils.pullHeadersFromFile(testFile1, cf.getCsvFtc(), 0);
        for(int i=0; i<Math.max(expectedHeaders1.length, actualHeaders1.length); i++)
            TestCase.assertEquals(expectedHeaders1[i], actualHeaders1[i]);

        File testFile2 = new File("src/test/resources/datasync_unit_test_two_rows.csv");
        String[] expectedHeaders2 = new String[] {"1","Food","My Food", "04/30/2011"};
        String[] actualHeaders2 = Utils.pullHeadersFromFile(testFile2, cf.getCsvFtc(), 1);
        for(int i=0; i<Math.max(expectedHeaders2.length, actualHeaders2.length); i++)
            TestCase.assertEquals(expectedHeaders2[i], actualHeaders2[i]);

        File testFile3 = new File("src/test/resources/datasync_unit_test_multiline_record.csv");
        String[] expectedHeaders3 = new String[] {"ID", "Name", "Another Name", "Date"};
        String[] actualHeaders3 = Utils.pullHeadersFromFile(testFile3, cf.getCsvFtc(), 2);
        for(int i=0; i<Math.max(expectedHeaders3.length, actualHeaders3.length); i++)
            TestCase.assertEquals(expectedHeaders3[i], actualHeaders3[i]);

        cf.getCsvFtc().separator(" ");
        String[] expectedHeaders4 = new String[] {"ID,Name,Another", "Name,Date"};
        String[] actualHeaders4 = Utils.pullHeadersFromFile(testFile1, cf.getCsvFtc(), 0);
        for(int i=0; i<Math.max(expectedHeaders4.length, actualHeaders4.length); i++)
            TestCase.assertEquals(expectedHeaders4[i], actualHeaders4[i]);

    }
}
