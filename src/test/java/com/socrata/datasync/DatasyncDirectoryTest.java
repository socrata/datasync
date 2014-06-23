package com.socrata.datasync;

import com.socrata.datasync.deltaimporter2.DatasyncDirectory;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DatasyncDirectoryTest extends TestBase {

    HttpUtility http;
    DatasyncDirectory dd;

    @Before
    public void setUp() throws Exception {
        http = new HttpUtility(getUserPrefs(), false);
        dd = new DatasyncDirectory(http, "test.socrata.com", "some-4by4");
    }

    @Test
    public void testFindMostRecent() throws ParseException, UnsupportedEncodingException {
        SimpleDateFormat yearDf = new SimpleDateFormat("yyyy");
        SimpleDateFormat monthDf = new SimpleDateFormat("MM");
        SimpleDateFormat dayDf = new SimpleDateFormat("dd");
        SimpleDateFormat hourDf = new SimpleDateFormat("hh:mm:ss");

        List<String> listing1 = Arrays.asList("2013/");
        String mostRecent1 = listing1.get(0);
        List<String> listing2 = Arrays.asList("02/", "06/", "11/");
        String mostRecent2 = listing2.get(2);
        List<String> listing3 = Arrays.asList("05:10:50.456", "22:11:00.102", "00:01:34.232");
        String mostRecent3 = listing3.get(1);
        List<String> listing4 = Arrays.asList("05%3A10%3A50.546-filename.ssig",
                "02%3A11%3A00.382-filename.ssig", "21%3A01%3A34.999-filename.ssig");
        String mostRecent4 = URLDecoder.decode(listing4.get(2), "UTF-8");

        TestCase.assertNull(dd.findMostRecentContent(null, yearDf));
        TestCase.assertNull(dd.findMostRecentContent(new ArrayList<String>(), yearDf));
        TestCase.assertEquals(mostRecent1, dd.findMostRecentContent(listing1, yearDf));
        TestCase.assertEquals(mostRecent2, dd.findMostRecentContent(listing2, monthDf));
        TestCase.assertEquals(mostRecent2, dd.findMostRecentContent(listing2, dayDf));
        TestCase.assertEquals(mostRecent3, dd.findMostRecentContent(listing3, hourDf));
        TestCase.assertEquals(mostRecent4, dd.findMostRecentContent(listing4, hourDf));
    }
}
