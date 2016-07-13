package com.socrata.datasync.config.userpreferences;

import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.job.LoadPreferencesJob;

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
    private String proxyHost;
    private String proxyPort;
    private String proxyUsername;
    private String proxyPassword;
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

    public UserPreferencesLib domain(String domain) { setDomain(domain); return this; }

    public String getUsername() { return username; }

    public void setUsername(String username) {
        this.username = username;
    }

    public UserPreferencesLib username(String username) { setUsername(username); return this; }

    public String getPassword() { return password; }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserPreferencesLib password(String password) { setPassword(password); return this; }

    public String getAPIKey() { return appToken; }

    public String getAppToken() {
        return appToken;
    }

    public void setAppToken(String appToken) {
        this.appToken = appToken;
    }

    public UserPreferencesLib appToken(String appToken) { setAppToken(appToken); return this; }

    public String getProxyHost() { return proxyHost; }

    public void setProxyHost(String host) { this.proxyHost = host; }

    public UserPreferencesLib proxyHost(String host) { setProxyHost(host); return this; }

    public String getProxyPort() { return proxyPort; }

    public void setProxyPort(String port) { this.proxyPort = port; }

    public UserPreferencesLib proxyPort(String port) { setProxyPort(port); return this; }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(String username) {
        this.proxyUsername = username;
    }

    public UserPreferencesLib proxyUsername(String username) { setProxyUsername(username); return this; }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String password) {
        this.proxyPassword = password;
    }

    public UserPreferencesLib proxyPassword(String password) { setProxyPassword(password); return this; }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public UserPreferencesLib adminEmail(String adminEmail) { setAdminEmail(adminEmail); return this; }

    public boolean emailUponError() {
        return emailUponError;
    }

    public void setEmailUponError(boolean emailUponError) {
        this.emailUponError = emailUponError;
    }

    public UserPreferencesLib emailUponError(boolean emailUponError) { setEmailUponError(emailUponError); return this; }

    public String getLogDatasetID() {
        return logDatasetID;
    }

    public void setLogDatasetID(String logDatasetID) {
        this.logDatasetID = logDatasetID;
    }

    public UserPreferencesLib logDatasetID(String logDatasetID) { setLogDatasetID(logDatasetID); return this; }

    public String getOutgoingMailServer() {
        return outgoingMailServer;
    }

    public void setOutgoingMailServer(String mailServer) {
        this.outgoingMailServer = mailServer;
    }

    public UserPreferencesLib outgoingMailServer(String mailServer) { setOutgoingMailServer(mailServer); return this; }

    public String getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
    }

    public UserPreferencesLib smtpPort(String smtpPort) { setSmtpPort(smtpPort); return this; }

    public String getSslPort() {
        return sslPort;
    }

    public void setSslPort(String sslPort) {
        this.sslPort = sslPort;
    }

    public UserPreferencesLib sslPort(String sslPort) { setSslPort(sslPort); return this; }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public UserPreferencesLib smtpUsername(String smtpUsername) { setSmtpUsername(smtpUsername); return this; }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public UserPreferencesLib smtpPassword(String smtpPassword) { setSmtpPassword(smtpPassword); return this; }

    public String getFilesizeChunkingCutoffMB() {
        return filesizeChunkingCutoffMB;
    }

    public void setFilesizeChunkingCutoffMB(String mb) { this.filesizeChunkingCutoffMB = mb; }

    public UserPreferencesLib filesizeChunkingCutoffMB(String mb) { setFilesizeChunkingCutoffMB(mb); return this; }

    public String getNumRowsPerChunk() {
        return numRowsPerChunk;
    }

    public void setNumRowsPerChunk(String numRowsPerChunk) {
        this.numRowsPerChunk = numRowsPerChunk;
    }

    public UserPreferencesLib numRowsPerChunk(String numRows) { setNumRowsPerChunk(numRows); return this; }

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

    public String getHost() {
        if (domain != null) {
            String[] schemeAndHost = domain.split("//");
            return schemeAndHost[schemeAndHost.length - 1];
        } else {
            return null;
        }
    }

    public UserPreferencesLib load() {
        LoadPreferencesJob j = new LoadPreferencesJob();
        j.setUserPrefs(this);
        j.run();
        return this;
    }
}
