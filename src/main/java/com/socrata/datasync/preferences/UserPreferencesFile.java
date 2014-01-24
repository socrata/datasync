package com.socrata.datasync.preferences;

import com.socrata.datasync.SocrataConnectionInfo;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.File;
import java.io.IOException;

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
    private String appToken;
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
    private String portDestinationDomainAppToken;

    // Anytime a @JsonProperty is added/removed/updated in this class add 1 to this value
    private static final long fileVersionUID = 3L;

    @JsonProperty("domain")
    public String getDomain() {
        return domain;
    }

    @JsonProperty("username")
    public String getUsername() {
        return username;
    }

    @JsonProperty("password")
    public String getPassword() {
        return password;
    }

    @JsonProperty("appToken")
    public String getAppToken() {
        return appToken;
    }
    // Alias for getAppToken
    public String getAPIKey() {
        return appToken;
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
    public String getSMTPPort() {
        return smtpPort;
    }

    @JsonProperty("sslPort")
    public String getSSLPort() {
        return sslPort;
    }

    @JsonProperty("smtpUsername")
    public String getSMTPUsername() {
        return smtpUsername;
    }

    @JsonProperty("smtpPassword")
    public String getSMTPPassword() {
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

    @JsonProperty("portDestinationDomainAppToken")
    public String getPortDestinationDomainAppToken() {
        return portDestinationDomainAppToken;
    }

    public SocrataConnectionInfo getConnectionInfo() {
        return new SocrataConnectionInfo(
                this.getDomain(), this.getUsername(), this.getPassword(), this.getAPIKey());
    }

}
