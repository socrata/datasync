package com.socrata.datasync.config.userpreferences;

import com.socrata.datasync.SocrataConnectionInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
public class UserPreferencesFile implements UserPreferences {
    /**
     * @author Adrian Laurenzi
     *
     * Allows reading in a config file to retrieve user preferences
     * from that file (preferences encoded as JSON).
     */
    private String domain;
    private String username;
    private String password;
    private String proxyHost;
    private String proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private String adminEmail;
    private boolean emailUponError;
    private String logDatasetID;
    private String outgoingMailServer;
    private String smtpPort;
    private String sslPort;
    private String smtpUsername;
    private String smtpPassword;
    private String filesizeChunkingCutoffMB;
    private String numRowsPerChunk;
    private List<String> timeFormats;

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 5L;

    @JsonProperty("domain")
    public String getDomain() {
        if(domain == null) return null;
        return UserPreferencesUtil.prefixDomain(domain.trim());
    }

    @JsonProperty("username")
    public String getUsername() {
        return username;
    }

    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    @JsonProperty("proxyHost")
    public String getProxyHost() {
        return proxyHost;
    }

    @JsonProperty("proxyPort")
    public String getProxyPort() {
        return proxyPort;
    }

    @JsonProperty("proxyUsername")
    public String getProxyUsername() {
        return proxyUsername;
    }

    @JsonProperty("proxyPassword")
    public String getProxyPassword() {
        return proxyPassword;
    }

    @JsonProperty("adminEmail")
    public String getAdminEmail() {
        return adminEmail;
    }

    @JsonProperty("emailUponError")
    public boolean emailUponError() {
        return emailUponError;
    }

    @JsonProperty("logDatasetID")
    public String getLogDatasetID() {
        return logDatasetID;
    }

    @JsonProperty("outgoingMailServer")
    public String getOutgoingMailServer() {
        return outgoingMailServer;
    }

    @JsonProperty("smtpPort")
    public String getSmtpPort() {
        return smtpPort;
    }

    @JsonProperty("sslPort")
    public String getSslPort() {
        return sslPort;
    }

    @JsonProperty("smtpUsername")
    public String getSmtpUsername() {
        return smtpUsername;
    }

    @JsonProperty("smtpPassword")
    public String getSmtpPassword() {
        return smtpPassword;
    }

    @JsonProperty("filesizeChunkingCutoffMB")
    public String getFilesizeChunkingCutoffMB() {
        return filesizeChunkingCutoffMB;
    }

    @JsonProperty("numRowsPerChunk")
    public String getNumRowsPerChunk() {
        return numRowsPerChunk;
    }

    @JsonProperty("proxyUsername")
    public void setProxyUsername(String username) { proxyUsername = username; }

    @JsonProperty("proxyPassword")
    public void setProxyPassword(String password) { proxyPassword = password; }

    @JsonProperty("defaultTimeFormats")
    public List<String> getDefaultTimeFormats() {
        if(timeFormats == null) return Collections.unmodifiableList(Arrays.asList(DEFAULT_TIME_FORMATS));
        return Collections.unmodifiableList(timeFormats);
    }

    public String getHost() {
        if (domain != null) {
            String[] schemeAndHost = domain.split("//");
            return schemeAndHost[schemeAndHost.length - 1];
        } else {
            return null;
        }
    }

    public SocrataConnectionInfo getConnectionInfo() {
        return new SocrataConnectionInfo(
                this.getDomain(), this.getUsername(), this.getPassword());
    }

}
