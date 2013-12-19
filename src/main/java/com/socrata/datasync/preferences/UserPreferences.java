package com.socrata.datasync.preferences;

import com.socrata.datasync.SocrataConnectionInfo;

/**
 * Author: Adrian Laurenzi
 * Date: 12/2/13
 */
public interface UserPreferences {

    public String getDomain();

    public String getUsername();

    public String getPassword();

    // API key a.k.a. App token
    public String getAPIKey();

    public String getAdminEmail();

    public boolean emailUponError();

    public String getLogDatasetID();

    public String getOutgoingMailServer();

    public String getSMTPPort();

    public String getSSLPort();

    public String getSMTPUsername();

    public String getSMTPPassword();

    public String getFilesizeChunkingCutoffMB();

    public String getNumRowsPerChunk();

    public SocrataConnectionInfo getConnectionInfo();
}
