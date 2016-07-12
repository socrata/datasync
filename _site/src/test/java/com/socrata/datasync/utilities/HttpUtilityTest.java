package com.socrata.datasync.utilities;


import com.socrata.datasync.HttpUtility;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class HttpUtilityTest extends TestBase {
    ObjectMapper mapper;
    HttpUtility http;

    @Before
    public void setUp() throws Exception {
        mapper = new ObjectMapper();
        UserPreferences userPrefs = getUserPrefs();
        http = new HttpUtility(userPrefs, true);
    }

    @After
    public void tearDown() throws IOException {
        http.close();
    }

    @Test
    public void testHttpGet() throws Exception {
        URI getUri = new URIBuilder()
                .setScheme("https")
                .setHost(DOMAIN.split("//")[1])
                .setPath("/datasync/version")
                .build();
        try(CloseableHttpResponse response = http.get(getUri, ContentType.APPLICATION_JSON.getMimeType());
            InputStream body = response.getEntity().getContent()) {
            // uncomment the test below, when di2 is running in prod
            /*
            HashMap<String,String> versionNotes = mapper.readValue(body,
                    new TypeReference<HashMap<String,Object>>() {});
            TestCase.assertEquals(200, response.getStatusLine().getStatusCode());
            TestCase.assertTrue(versionNotes.containsKey("version"));
            */
        }
        http.close();
    }

    @Test
    public void testHttpPost() throws Exception {
        URI postUri = new URIBuilder()
                .setScheme("https")
                .setHost(DOMAIN.split("//")[1])
                .setPath("/datasync/id/" + UNITTEST_DATASET_ID)
                .build();
        InputStream is = new FileInputStream(new File("src/test/resources/example_patch.sdiff"));
        byte[] data = IOUtils.toByteArray(is);
        HttpEntity entity = EntityBuilder.create().setBinary(data).build();

        try(CloseableHttpResponse response = http.post(postUri, entity);
            InputStream body = response.getEntity().getContent()) {
            // uncomment the test below, when di2 is running in prod
            /*
            TestCase.assertEquals(201, response.getStatusLine().getStatusCode());
            String blobId = mapper.readValue(response.getEntity().getContent(), BlobId.class).blobId;
            TestCase.assertNotNull(blobId);
            */
        }
    }
}
