package com.socrata.datasync;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.codehaus.jackson.map.ObjectMapper;

import java.net.URI;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.regex.MatchResult;

public final class VersionProvider {

    private static final VersionProvider INSTANCE = new VersionProvider();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final HttpUtility http = new HttpUtility();
    private static final String datasyncReleases = "https://api.github.com/repos/socrata/datasync/releases";
    private String version;

    private VersionProvider() {
        ResourceBundle rb;
        rb = ResourceBundle.getBundle("datasync");
        version = rb.getString("version");
    }

    public enum VersionStatus {
        LATEST, NOT_LATEST, UNKNOWN
    }

    public static String getThisVersion() {
        return INSTANCE.version;
    }

    public static VersionStatus isLatestMajorVersion() {
        String latest = getLatestVersion();
        if (latest == null) {
            return VersionStatus.UNKNOWN;
        } else {
            if (getMajorVersion(INSTANCE.version) >= getMajorVersion(latest)) {
                return VersionStatus.LATEST;
            } else {
                return VersionStatus.NOT_LATEST;
            }
        }
    }

    public static int getMajorVersion(String versionString) {
        String[] versionSplit = versionString.split("\\.");
        return Integer.parseInt(versionSplit[0]);
    }

    public static String getLatestVersion() {
        try(CloseableHttpResponse response = http.get(new URI(datasyncReleases), ContentType.APPLICATION_JSON.getMimeType())) {
            DatasyncGithubRelease[] releases = mapper.readValue(response.getEntity().getContent(), DatasyncGithubRelease[].class);
            DatasyncGithubRelease currentRelease = releases[0];
            return stripVersion(currentRelease.name);
        } catch (Exception e) {   // no reason to fail jobs because of a version check
            return null;
        }
    }

    public static String getDownloadUrlForLatestVersion() {
        try(CloseableHttpResponse response = http.get(new URI(datasyncReleases), ContentType.APPLICATION_JSON.getMimeType())) {
            DatasyncGithubRelease[] releases = mapper.readValue(response.getEntity().getContent(), DatasyncGithubRelease[].class);
            DatasyncGithubRelease currentRelease = releases[0];
            return currentRelease.htmlUrl;
        } catch (Exception e) {  // no reason to fail jobs because of a version check
            return null;
        }
    }

    public static String stripVersion(String text) {
        Scanner scanner = new Scanner(text);
        String versionNums = scanner.findWithinHorizon("(\\d+)(\\.\\d+)(\\.\\d+)?", 0);
        String version = "";
        if (versionNums != null) {
            MatchResult groups = scanner.match();
            for (int i = 1; i <= groups.groupCount() && groups.group(i) != null; i++) // yes, truly 1-indexed
                version += groups.group(i);
        }
        return version;
    }

}
