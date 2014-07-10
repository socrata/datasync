package com.socrata.datasync;

import junit.framework.TestCase;
import org.junit.Test;

import java.io.ByteArrayInputStream;
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
}
