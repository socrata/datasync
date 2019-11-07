package com.socrata.datasync.config.userpreferences;

import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.Utils;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class UserPreferencesJava implements UserPreferences {
    /**
     * @author Adrian Laurenzi
     *
     * Saves and retrieves (global) user preferences using the
     * Java Preferences class (which stores/retrieves data from standard
     * locations that vary depending on the platform).
     */

    private static Preferences userPrefs;
    // Preference keys for saving user data
    private static final String DOMAIN = "domain";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    private static final String PROXY_USERNAME = "proxy_username";
    private static final String PROXY_PASSWORD = "proxy_password";
    private static final String PROXY_HOST = "proxy_host";
    private static final String PROXY_PORT = "proxy_port";

    private static final String ADMIN_EMAIL = "admin_email";
    private static final String EMAIL_UPON_ERROR = "email_upon_error";
    private static final String LOG_DATASET_ID = "log_dataset_id";

    private static final String OUTGOING_MAIL_SERVER = "outgoing_mail_server";
    private static final String SMTP_PORT = "smtp_port";
    // NOTE: if SSL port is set to the empty string then do not use SSL
    private static final String SSL_PORT = "ssl_port";
    private static final String SMTP_USERNAME = "smtp_username";
    private static final String SMTP_PASSWORD = "smtp_password";

    private static final String FILESIZE_CHUNKING_CUTOFF_MB = "filesize_chunking_cutoff_mb";
    private static final String NUM_ROWS_PER_CHUNK = "num_rows_per_chunk";

    // When a file to be published is larger than this value (in MB), file is chunked
    private static final String DEFAULT_FILESIZE_CHUNK_CUTOFF_MB = "10";
    // During chunking files are uploaded NUM_ROWS_PER_CHUNK rows per chunk
    private static final String DEFAULT_NUM_ROWS_PER_CHUNK = "10000";

    private static final String TIME_FORMATS = "time_formats";

    private final String DEFAULT_DOMAIN = "https://";
    private final String DEFAULT_SSL_PORT = "465";

    public UserPreferencesJava() {
        userPrefs = Preferences.userRoot().node("SocrataIntegrationPrefs");
    }

    public void clear() throws BackingStoreException {
        userPrefs.clear();
    }

    // ------------- Save methods ---------------
    public void saveDomain(String domain) {
        saveKeyValuePair(DOMAIN, domain);
    }

    public void saveUsername(String username) {
        saveKeyValuePair(USERNAME, username);
    }

    public void savePassword(String password) {
        saveKeyValuePair(PASSWORD, CryptoUtil.obfuscate(password));
    }

    public void saveProxyHost(String host) { saveKeyValuePair(PROXY_HOST, host); }

    public void saveProxyPort(String port) {
        saveKeyValuePair(PROXY_PORT, port);
    }

    public void saveProxyUsername(String username) { saveKeyValuePair(PROXY_USERNAME, username); }

    public void saveProxyPassword(String password) {
        saveKeyValuePair(PROXY_PASSWORD, CryptoUtil.obfuscate(password));
    }

    public void saveAdminEmail(String adminEmail) { saveKeyValuePair(ADMIN_EMAIL, adminEmail);    }

    public void saveEmailUponError(boolean emailUponError) {
        String prefString = emailUponError ? "YES" : "";
        saveKeyValuePair(EMAIL_UPON_ERROR, prefString);
    }

    public void saveLogDatasetID(String datasetID) {
        saveKeyValuePair(LOG_DATASET_ID, datasetID);
    }

    public void saveOutgoingMailServer(String mailServer) {
        saveKeyValuePair(OUTGOING_MAIL_SERVER, mailServer);
    }

    public void saveSMTPPort(String port) {
        saveKeyValuePair(SMTP_PORT, port);
    }

    public void saveSSLPort(String port) {
        saveKeyValuePair(SSL_PORT, port);
    }

    public void saveFilesizeChunkingCutoffMB(int numMegaBytes) {
        saveKeyValuePair(FILESIZE_CHUNKING_CUTOFF_MB, Integer.toString(numMegaBytes));
    }

    public void saveNumRowsPerChunk(int numRows) {
        saveKeyValuePair(NUM_ROWS_PER_CHUNK, Integer.toString(numRows));
    }

    public void saveSMTPUsername(String username) {
        saveKeyValuePair(SMTP_USERNAME, username);
    }

    public void saveSMTPPassword(String password) {
        saveKeyValuePair(SMTP_PASSWORD, CryptoUtil.obfuscate(password));
    }

    public void saveDefaultTimeFormats(List<String> defaultTimeFormats) {
        saveKeyValuePair(TIME_FORMATS, Utils.commaJoin(defaultTimeFormats));
    }

    public void setProxyUsername(String username) {};  // never save proxy credentials

    public void setProxyPassword(String password) {};  // never save proxy credentials


    // ------------- Get methods ---------------

    public String getDomain() {
        String domain = userPrefs.get(DOMAIN, DEFAULT_DOMAIN);
        if(domain == null) return null;
        return UserPreferencesUtil.prefixDomain(domain.trim());
    }

    public String getUsername() {
        return userPrefs.get(USERNAME, "");
    }

    public String getPassword() {
        String base = userPrefs.get(PASSWORD, "");
        String result = CryptoUtil.deobfuscate(base, "");
        if(base == result) savePassword(result);
        return result;
    }

    public String getProxyHost() { return userPrefs.get(PROXY_HOST, null); }

    public String getProxyPort() {
        return userPrefs.get(PROXY_PORT, null);
    }

    public String getProxyUsername() {
        return userPrefs.get(PROXY_USERNAME, null);
    }

    public String getProxyPassword() {
        String base = userPrefs.get(PROXY_PASSWORD, null);
        String result = CryptoUtil.deobfuscate(base, null);
        if(base == result) saveProxyPassword(result);
        return result;
    }

    public String getAdminEmail() {
        return userPrefs.get(ADMIN_EMAIL, "");
    }

    public boolean emailUponError() {
        String emailAdminPref = userPrefs.get(EMAIL_UPON_ERROR, "");
        return (!emailAdminPref.equals(""));
    }

    public String getLogDatasetID() {
        return userPrefs.get(LOG_DATASET_ID, "");
    }

    public String getOutgoingMailServer() {
        return userPrefs.get(OUTGOING_MAIL_SERVER, "");
    }

    public String getSmtpPort() {
        return userPrefs.get(SMTP_PORT, "");
    }

    public String getSslPort() {
        return userPrefs.get(SSL_PORT, DEFAULT_SSL_PORT);
    }

    public String getSmtpUsername() {
        return userPrefs.get(SMTP_USERNAME, "");
    }

    public String getSmtpPassword() {
        String base = userPrefs.get(SMTP_PASSWORD, "");
        String result = CryptoUtil.deobfuscate(base, "");
        if(base == result) saveSMTPPassword(result);
        return result;
    }

    public String getFilesizeChunkingCutoffMB() {
        return userPrefs.get(
                FILESIZE_CHUNKING_CUTOFF_MB, DEFAULT_FILESIZE_CHUNK_CUTOFF_MB);
    }

    public String getNumRowsPerChunk() {
        return userPrefs.get(NUM_ROWS_PER_CHUNK, DEFAULT_NUM_ROWS_PER_CHUNK);
    }

    /**
     * This preference is for testing usage only (returns empty string because
     * portDestinationDomainAppToken should only be set when DataSync is run
     * in command-line mode).
     */
    public String getPortDestinationDomainAppToken() {
        return "";
    }

    public String getHost() {
        String domain = getDomain();
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

    public List<String> getDefaultTimeFormats() {
        return Collections.unmodifiableList(Arrays.asList(Utils.commaSplit(userPrefs.get(TIME_FORMATS, Utils.commaJoin(DEFAULT_TIME_FORMATS)))));
    }

    @Override
    public String toString() {
        return "domain: " + getDomain() + "\n" +
                "username: " + getUsername() + "\n" +
                "password: " + getPassword().replaceAll(".", "*") +"\n" +
                "proxyHost:" + getProxyHost() + "\n" +
                "proxyPort:" + getProxyPort() + "\n" +
                "adminEmail: " + getAdminEmail() + "\n" +
                "emailUponError: " + emailUponError() + "\n" +
                "logDatasetID: " + getLogDatasetID() + "\n" +
                "outgoingMailServer: " + getOutgoingMailServer() + "\n" +
                "smtpPort: " + getSmtpPort() + "\n" +
                "sslPort: " + getSslPort() + "\n" +
                "smtpUsername: " + getSmtpUsername() + "\n" +
                "smtpPassword: " + getSmtpPassword().replaceAll(".", "*") + "\n" +
                "filesizeChunkingCutoffMB: " + getFilesizeChunkingCutoffMB() + "\n" +
                "numRowsPerChunk: " + getNumRowsPerChunk() + "\n" +
                "defaultTimeFormats: " + getDefaultTimeFormats() + "\n";
    }

    private void saveKeyValuePair(String key, String value) {
        if (value == null) {
            userPrefs.remove(key);
        } else {
            //ONCALL-3204 - Remove leading and trailing whitespace from user entered fields.
            userPrefs.put(key, value.trim());
        }
    }
}
