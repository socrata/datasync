package com.socrata.datasync.config.userpreferences;

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

    public String getSmtpPort();

    public String getSslPort();

    public String getSmtpUsername();

    public String getSmtpPassword();

    public String getFilesizeChunkingCutoffMB();

    public String getNumRowsPerChunk();

    public String getPortDestinationDomainAppToken();

    public boolean getUseNewBackend();

    public SocrataConnectionInfo getConnectionInfo();

}
