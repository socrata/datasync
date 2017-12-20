package com.socrata.datasync;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 *  This class contains all the information needed to connect to a
 *  Socrata site.
 */
public class SocrataConnectionInfo
{
    public String url;
    public String user;
    public String password;
    private static String token;

    public SocrataConnectionInfo(String url, String user, String password)
    {
        this.url = url;
        this.user = user;
        this.password = password;
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

    public String getToken() {
        if(token == null) {
            try(InputStream stream = SocrataConnectionInfo.class.getResourceAsStream("/api-key.txt")) {
                token = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).readLine().trim();
            } catch (Exception e) {
                throw new RuntimeException("Unable to load API key!  If this isn't an official build, you must create\na file called \"api-key.txt\" containing your app token before building.");
            }
        }
        return token;
    }
}
