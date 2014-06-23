package com.socrata.datasync;

import com.google.common.net.HttpHeaders;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import org.apache.commons.net.util.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;

public class HttpUtility {

    private CloseableHttpClient httpClient = null;
    private RequestConfig proxyConfig = null;
    private String authHeader;
    private String appToken;
    private boolean authRequired = false;
    private static final String appHeader = "X-App-Token";
    private static final String userAgent = "datasync";

    public HttpUtility() { this(null, false); }

    public HttpUtility(UserPreferences userPrefs, boolean useAuth) {
        if (useAuth) {
            authHeader = getAuthHeader(userPrefs.getUsername(), userPrefs.getPassword());
            appToken = userPrefs.getAPIKey();
        }
        authRequired = useAuth;
        if(userPrefs != null && userPrefs.getProxyHost() != null && userPrefs.getProxyPort() != null) {
            HttpHost proxy = new HttpHost(userPrefs.getProxyHost(), Integer.valueOf(userPrefs.getProxyPort()));
            proxyConfig = RequestConfig.custom().setProxy(proxy).build();
            if (userPrefs.getProxyUsername() != null && userPrefs.getProxyPassword() != null) {
                CredentialsProvider credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(
                        new AuthScope(userPrefs.getProxyHost(), Integer.valueOf(userPrefs.getProxyPort())),
                        new UsernamePasswordCredentials(userPrefs.getProxyUsername(), userPrefs.getProxyPassword()));
                httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
            }
        }
        if (httpClient == null) httpClient = HttpClientBuilder.create().build();
    }

    /**
     * Conducts a basic get, passing the auth information in the header.
     * @param uri the uri from which the get will be made
     * @param contentType the expected contentType of the return value
     * @return the unproccessed query results
     */
    public CloseableHttpResponse get(URI uri, String contentType) throws IOException {
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader(HttpHeaders.USER_AGENT, userAgent);
        httpGet.addHeader(HttpHeaders.ACCEPT, contentType);
        if (proxyConfig != null)
            httpGet.setConfig(proxyConfig);
        if (authRequired) {
            httpGet.setHeader(appHeader, appToken);
            httpGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
        return httpClient.execute(httpGet);
    }

    /**
     * Conducts a basic post of the given entity; auth information is passed in the header.
     * @param uri  the uri to which the entity will be posted
     * @param entity an entity to post
     * @return the unprocessed results of the post
     */
    public CloseableHttpResponse post(URI uri, HttpEntity entity) throws IOException {
        HttpPost httpPost = new HttpPost(uri);
        httpPost.setHeader(HttpHeaders.USER_AGENT, userAgent);
        httpPost.setHeader(entity.getContentType());
        httpPost.setEntity(entity);
        if (proxyConfig != null)
            httpPost.setConfig(proxyConfig);
        if (authRequired) {
            httpPost.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
            httpPost.setHeader(appHeader, appToken);
        }
        return httpClient.execute(httpPost);
    }

    public void close() throws IOException {
        httpClient.close();
    }

    private String getAuthHeader(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
        return "Basic " + new String(encodedAuth);
    }
}
