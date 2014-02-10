package com.socrata.datasync.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.socrata.datasync.JobStatus;
import com.socrata.datasync.LicenseType;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesJava;

import junit.framework.TestCase;

import org.junit.Test;

public class MetadataJobTest extends TestBase {
	
	private static final String RESET_CATEGORY = null;
	private static final List<String> RESET_KEYWORDS = new ArrayList<String>();
	private static final LicenseType RESET_LICENSE_TYPE = LicenseType.cc0_10;
	private static final String RESET_DATA_PROVIDED_BY = "test";
	private static final String RESET_SOURCE_LINK = null;
	private static final String RESET_CONTACT_INFO = "";
	//private static final String RESET_LICENSE_TYPE_ID = LicenseType.cc0_10.getValue();
	private static final String RESET_TITLE = "DataSync Unit Test Dataset";
	private static final String RESET_DESCRIPTION = null;
	
	private static final String CATEGORY = "Government";
	private static final List<String> KEYWORDS = Collections.unmodifiableList(
			Arrays.asList("Metadata", "Update", "Test"));
	private static final LicenseType LICENSE_TYPE = LicenseType.cc_30_by_aus;
	private static final String DATA_PROVIDED_BY = "test updated";
	private static final String SOURCE_LINK = "Centers for Disease Control and Prevention";
	private static final String CONTACT_INFO = "bwk8@cdc.gov";
	//private static final String LICENSE_TYPE_ID = LicenseType.cc_30_by_aus.getValue();
	private static final String TITLE = "DataSync Unit Test Dataset Updated";
	private static final String DESCRIPTION = "DataSync Unit Test Dataset Description Updated";
	
	private static final String RESET_FILE_PATH = "src/test/resources/metadata_test_reset.smj";
	private static final String UPDATE_FILE_PATH = "src/test/resources/metadata_test.smj";
	private static final String TEMP_FILE_PATH = "src/test/resources/metadata_test_temp.smj";
	
	public static final String INVALID_DOMAIN = "htps://sandbox.demo.socrata.com";
	public static final String INVALID_DATASET_ID = "8gex-q4dsx";

	private UserPreferencesJava setupUserPreferencesForTest() {
		UserPreferencesJava userPrefs = new UserPreferencesJava();
		userPrefs.saveAPIKey(API_KEY);
		userPrefs.saveDomain(DOMAIN);
		userPrefs.saveUsername(USERNAME);
		userPrefs.savePassword(PASSWORD);	
		return userPrefs;
	}
	
	@Test
	public void testReadingJobFile() throws IOException {
		MetadataJob metadataJob = new MetadataJob(UPDATE_FILE_PATH);
		testUpdate(metadataJob);
	}
	
	@Test
	public void testWritingJobFile() throws IOException {
		MetadataJob metadataJob = new MetadataJob();
		metadataJob.setCategory(CATEGORY);
		metadataJob.setKeywords(KEYWORDS);
		metadataJob.setLicenseType(LICENSE_TYPE);
		metadataJob.setDataProvidedBy(DATA_PROVIDED_BY);
		metadataJob.setSourceLink(SOURCE_LINK);
		metadataJob.setContactInfo(CONTACT_INFO);
		metadataJob.setTitle(TITLE);
		metadataJob.setDescription(DESCRIPTION);
		
		metadataJob.writeToFile(TEMP_FILE_PATH);
		
		metadataJob = new MetadataJob(TEMP_FILE_PATH);
		testUpdate(metadataJob);
	}
	
	@Test
	public void testValidation() {
		MetadataJob metadataJob = new MetadataJob();
		UserPreferencesJava prefs = setupUserPreferencesForTest();
		
		//Base Validation
		metadataJob.setDatasetID(UNITTEST_DATASET_ID);
		metadataJob.setTitle(RESET_TITLE);
		TestCase.assertEquals(JobStatus.VALID, metadataJob.validate(prefs.getConnectionInfo()));
		
		//Invalid Domain
		prefs.saveDomain(INVALID_DOMAIN);
		TestCase.assertEquals(JobStatus.INVALID_DOMAIN, metadataJob.validate(prefs.getConnectionInfo()));
		prefs.saveDomain(DOMAIN);
		
		//Invalid Dataset ID
		metadataJob.setDatasetID(INVALID_DATASET_ID);
		TestCase.assertEquals(JobStatus.INVALID_DATASET_ID, metadataJob.validate(prefs.getConnectionInfo()));
		metadataJob.setDatasetID(UNITTEST_DATASET_ID);
		
		//Invalid Title
		metadataJob.setTitle("");
		TestCase.assertEquals(JobStatus.MISSING_METADATA_TITLE, metadataJob.validate(prefs.getConnectionInfo()));
		metadataJob.setTitle(RESET_TITLE);
	}
	
	@Test
	public void testMetadataUpdate() {
		
		MetadataJob metadataJob = new MetadataJob(setupUserPreferencesForTest());
		
		//Update metadata based on resource file
		
		//Load current metadata
		
		//Assert all values have been updated as expected
		
		//Reset metadata based on reset resource file
	}
	
	@Test
	public void testLoadCurrentMetadata() {
		MetadataJob metadataJob = new MetadataJob(setupUserPreferencesForTest());
		
		//Load current metadata
		metadataJob.setDatasetID(this.UNITTEST_DATASET_ID);
		metadataJob.loadCurrentMetadata();
		
		//Assert all values are as expected
		testReset(metadataJob);
	}
	
	//TODO : Test logging dataset is updated?
	
	//TODO : Test edge cases?
	
	private void testReset(MetadataJob metadataJob) {
		TestCase.assertEquals(RESET_CATEGORY, metadataJob.getCategory());
		//TODO: Probably need to check the types of the two lists here, equality needs same items in same order.
		TestCase.assertEquals(RESET_KEYWORDS, metadataJob.getKeywords());
		TestCase.assertEquals(RESET_LICENSE_TYPE, metadataJob.getLicenseType());
		TestCase.assertEquals(RESET_DATA_PROVIDED_BY, metadataJob.getDataProvidedBy());
		TestCase.assertEquals(RESET_SOURCE_LINK, metadataJob.getSourceLink());
		TestCase.assertEquals(RESET_CONTACT_INFO, metadataJob.getContactInfo());
		TestCase.assertEquals(RESET_TITLE, metadataJob.getTitle());
		TestCase.assertEquals(RESET_DESCRIPTION, metadataJob.getDescription());		
	}
	
	private void testUpdate(MetadataJob metadataJob) {
		TestCase.assertEquals(CATEGORY, metadataJob.getCategory()); 
		//TODO: Probably need to check the types of the two lists here, equality needs same items in same order.
		TestCase.assertEquals(KEYWORDS, metadataJob.getKeywords());
		TestCase.assertEquals(LICENSE_TYPE, metadataJob.getLicenseType());
		TestCase.assertEquals(DATA_PROVIDED_BY, metadataJob.getDataProvidedBy());
		TestCase.assertEquals(SOURCE_LINK, metadataJob.getSourceLink());
		TestCase.assertEquals(CONTACT_INFO, metadataJob.getContactInfo());
		TestCase.assertEquals(TITLE, metadataJob.getTitle());
		TestCase.assertEquals(DESCRIPTION, metadataJob.getDescription());		
	}
}
