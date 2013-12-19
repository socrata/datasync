package com.socrata.datasync.preferences;

import com.socrata.datasync.SocrataConnectionInfo;

import java.util.prefs.Preferences;

public class UserPreferencesJava implements UserPreferences {
	/**
	 * @author Adrian Laurenzi
	 * 
	 * This class saves and retrieves (global) user preferences using the
     * Java Preferences class (which stores/retrieves data from standard
     * locations that vary depending on the platform).
	 */

	private static Preferences userPrefs;
	// Preference keys for saving user data
	private static final String DOMAIN = "domain";
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String API_KEY = "api_key";
	
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
	
	private final String DEFAULT_DOMAIN = "https://";
	private final String DEFAULT_SSL_PORT = "465";
    
	public UserPreferencesJava() {
		userPrefs = Preferences.userRoot().node("SocrataIntegrationPrefs");
	}
	
	public void saveDomain(String domain) {
		userPrefs.put(DOMAIN, domain);
	}
	
	public void saveUsername(String username) {
		userPrefs.put(USERNAME, username);
	}
	
	public void savePassword(String password) {
		userPrefs.put(PASSWORD, password);
	}

    // API key a.k.a. App token
	public void saveAPIKey(String apiKey) {
		userPrefs.put(API_KEY, apiKey);
	}
	
	public void saveAdminEmail(String adminEmail) {
		userPrefs.put(ADMIN_EMAIL, adminEmail);
	}
	
	public void saveEmailUponError(boolean emailUponError) {
		String prefString = "";
		if(emailUponError) {
			prefString = "YES";
		}
		userPrefs.put(EMAIL_UPON_ERROR, prefString);
	}
	
	public void saveLogDatasetID(String datasetID) {
		userPrefs.put(LOG_DATASET_ID, datasetID);
	}
	
	public void saveOutgoingMailServer(String mailServer) {
		userPrefs.put(OUTGOING_MAIL_SERVER, mailServer);
	}
	
	public void saveSMTPPort(String port) {
		userPrefs.put(SMTP_PORT, port);
	}
	
	public void saveSSLPort(String port) {
		userPrefs.put(SSL_PORT, port);
	}

	public void saveFilesizeChunkingCutoffBytes(int numBytes) {
		userPrefs.put(FILESIZE_CHUNKING_CUTOFF_MB, Integer.toString(numBytes));
	}

	public void saveNumRowsPerChunk(int numRows) {
		userPrefs.put(NUM_ROWS_PER_CHUNK, Integer.toString(numRows));
	}

    public void saveSMTPUsername(String username) {
        userPrefs.put(SMTP_USERNAME, username);
    }

    public void saveSMTPPassword(String password) {
        userPrefs.put(SMTP_PASSWORD, password);
    }
	
	public String getDomain() {
		return userPrefs.get(DOMAIN, DEFAULT_DOMAIN);
	}
	
	public String getUsername() {
		return userPrefs.get(USERNAME, "");
	}
	
	public String getPassword() {
		return userPrefs.get(PASSWORD, "");
	}

    // API key a.k.a. App token
	public String getAPIKey() {
		return userPrefs.get(API_KEY, "");
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
	
	public String getSMTPPort() {
		return userPrefs.get(SMTP_PORT, "");
	}
	
	public String getSSLPort() {
		return userPrefs.get(SSL_PORT, DEFAULT_SSL_PORT);
	}
	
	public String getSMTPUsername() {
		return userPrefs.get(SMTP_USERNAME, "");
	}

	public String getSMTPPassword() {
		return userPrefs.get(SMTP_PASSWORD, "");
	}

    public String getFilesizeChunkingCutoffMB() {
        return userPrefs.get(
                FILESIZE_CHUNKING_CUTOFF_MB, DEFAULT_FILESIZE_CHUNK_CUTOFF_MB);
    }

    public String getNumRowsPerChunk() {
        return userPrefs.get(NUM_ROWS_PER_CHUNK, DEFAULT_NUM_ROWS_PER_CHUNK);
    }

    public SocrataConnectionInfo getConnectionInfo() {
        return new SocrataConnectionInfo(
                this.getDomain(), this.getUsername(), this.getPassword(), this.getAPIKey());
    }
}
