package com.socrata.datasync.config.userpreferences;

import com.socrata.datasync.SocrataConnectionInfo;

/**
 * Author: Adrian Laurenzi
 * Date: 6/13/14
 *
 * Allows configuring "global" configuration when running
 * DataSync as library.
 */
public class UserPreferencesLib implements UserPreferences {
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

    // When a file to be published is larger than this value (in MB), file is chunked
    private static final String DEFAULT_FILESIZE_CHUNK_CUTOFF_MB = "10";
    // During chunking files are uploaded NUM_ROWS_PER_CHUNK rows per chunk
    private static final String DEFAULT_NUM_ROWS_PER_CHUNK = "10000";

    public UserPreferencesLib() {
        adminEmail = "";
        emailUponError = false;
        logDatasetID = "";
        outgoingMailServer = "";
        smtpPort = "";
        sslPort = "";
        smtpUsername = "";
        smtpPassword = "";
        filesizeChunkingCutoffMB = DEFAULT_FILESIZE_CHUNK_CUTOFF_MB;
        numRowsPerChunk = DEFAULT_NUM_ROWS_PER_CHUNK;
        portDestinationDomainAppToken = "";
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAPIKey() {
        return appToken;
    }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public boolean emailUponError() {
        return emailUponError;
    }

    public void setEmailUponError(boolean emailUponError) {
        this.emailUponError = emailUponError;
    }

    public String getLogDatasetID() {
        return logDatasetID;
    }

    public void setLogDatasetID(String logDatasetID) {
        this.logDatasetID = logDatasetID;
    }

    public String getOutgoingMailServer() {
        return outgoingMailServer;
    }

    public void setOutgoingMailServer(String outgoingMailServer) {
        this.outgoingMailServer = outgoingMailServer;
    }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getSslPort() {
        return sslPort;
    }

    public void setSslPort(String sslPort) {
        this.sslPort = sslPort;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public String getFilesizeChunkingCutoffMB() {
        return filesizeChunkingCutoffMB;
    }

    public void setFilesizeChunkingCutoffMB(String filesizeChunkingCutoffMB) {
        this.filesizeChunkingCutoffMB = filesizeChunkingCutoffMB;
    }

    public String getNumRowsPerChunk() {
        return numRowsPerChunk;
    }

    public void setNumRowsPerChunk(String numRowsPerChunk) {
        this.numRowsPerChunk = numRowsPerChunk;
    }

    public boolean getUseNewBackend() {
        return false;
    }

    public void setPortDestinationDomainAppToken(String portDestinationDomainAppToken) {
        this.portDestinationDomainAppToken = portDestinationDomainAppToken;
    }

    public String getPortDestinationDomainAppToken() {
        return portDestinationDomainAppToken;
    }

    public SocrataConnectionInfo getConnectionInfo() {
        return new SocrataConnectionInfo(
                this.getDomain(), this.getUsername(), this.getPassword(), this.getAPIKey());
    }
}
