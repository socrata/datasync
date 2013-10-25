package com.socrata.datasync;

/**
 *  This class contains all the information needed to connect to a
 *  Socrata site.
 */
public class SocrataConnectionInfo
{
    public String url;
    public String user;
    public String password;
    public String token;
    
    public SocrataConnectionInfo(String url, String user, String password, String token)
    {
    	this.url = url;
    	this.user = user;
    	this.password = password;
    	this.token = token;
    }

    public String getUrl()
    {
        return url;
    }

    public String getUser()
    {
        return user;
    }

    public String getPassword()
    {
        return password;
    }

    public String getToken()
    {
        return token;
    }
}
