package com.socrata.datasync;

import junit.framework.TestCase;
import org.junit.Test;

import java.net.URISyntaxException;

public class VersionProviderTest {

    @Test
    public void testVersionStripper() {
        String string1 = "Datasync 1.0";
        String string2 = "Datasync 1.0.2";
        String string3 = "Datasync 1.0.3-SNAPSHOT";
        String string4 = "Datasync 2.2.22";

        TestCase.assertEquals("1.0", VersionProvider.stripVersion(string1));
        TestCase.assertEquals("1.0.2", VersionProvider.stripVersion(string2));
        TestCase.assertEquals("1.0.3", VersionProvider.stripVersion(string3));
        TestCase.assertEquals("2.2.22", VersionProvider.stripVersion(string4));
    }

    @Test
    public void testVersionMajorNumbers() {
        String version1 = "1.0.2";
        String version2 = "3";
        String version3 = "2.1";
        String version4 = "2.0.9-SNAPSHOT";

        TestCase.assertEquals(1, VersionProvider.getMajorVersion(version1));
        TestCase.assertEquals(3, VersionProvider.getMajorVersion(version2));
        TestCase.assertEquals(2, VersionProvider.getMajorVersion(version3));
        TestCase.assertEquals(2, VersionProvider.getMajorVersion(version4));
    }

    @Test
    public void testGetLatestVersion() throws URISyntaxException {
        TestCase.assertEquals("1.5.4", VersionProvider.getLatestVersion());
    }

    @Test
    public void testIsLatestVersion() throws URISyntaxException {
        TestCase.assertEquals(VersionProvider.VersionStatus.LATEST, VersionProvider.isLatestMajorVersion());
    }

    @Test
    public void testDownloadUrl() { TestCase.assertNotNull(VersionProvider.getDownloadUrlForLatestVersion()); }

    @Test
    public void testGetThisVersion() {
        TestCase.assertEquals("1.7.1", VersionProvider.getThisVersion());
    }
}
