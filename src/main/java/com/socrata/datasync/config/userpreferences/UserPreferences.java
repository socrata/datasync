package com.socrata.datasync.config.userpreferences;

import com.socrata.datasync.SocrataConnectionInfo;

/**
 * Author: Adrian Laurenzi
 * Date: 12/2/13
 */
public interface UserPreferences {
    public String getDomain();

    public String getHost();

    public String getUsername();

    public String getPassword();

    public String getProxyHost();

    public String getProxyPort();

    public String getProxyPassword();

    public String getProxyUsername();

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

    public SocrataConnectionInfo getConnectionInfo();

    public void setProxyPassword(String password);

    public void setProxyUsername(String username);

}
