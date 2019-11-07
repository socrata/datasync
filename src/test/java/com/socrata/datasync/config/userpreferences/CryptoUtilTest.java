package com.socrata.datasync.config.userpreferences;

import junit.framework.TestCase;
import org.junit.Test;

public class CryptoUtilTest {
    @Test
    public void testObfuscationRoundtrips() {
        for(String x : new String[] { "hello", "smiling gnus are happy", "¥€$", "", "bleh" }) {
            TestCase.assertEquals(x, CryptoUtil.deobfuscate(CryptoUtil.obfuscate(x), null));
        }
    }
}
