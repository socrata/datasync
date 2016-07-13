package com.socrata.datasync.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.socrata.datasync.LicenseType;
import com.socrata.datasync.TestBase;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;

import junit.framework.TestCase;

import org.junit.Test;

public class MetadataJobTest extends TestBase {
	
	private static final String RESET_CATEGORY = "";
	private static final List<String> RESET_KEYWORDS = new ArrayList<String>();
	private static final LicenseType RESET_LICENSE_TYPE = LicenseType.cc0_10;
	private static final String RESET_DATA_PROVIDED_BY = "test";
	private static final String RESET_SOURCE_LINK = "";
	private static final String RESET_CONTACT_INFO = "";
	//private static final String RESET_LICENSE_TYPE_ID = LicenseType.cc0_10.getValue();
	private static final String RESET_TITLE = "DataSync Unit Test Dataset";
	private static final String RESET_DESCRIPTION = "";
	
	private static final String CATEGORY = "Government";
	private static final List<String> KEYWORDS = Collections.unmodifiableList(
			Arrays.asList("metadata", "update", "test"));
	private static final LicenseType LICENSE_TYPE = LicenseType.cc_30_by_aus;
	private static final String DATA_PROVIDED_BY = "test updated";
	private static final String SOURCE_LINK = "http://www.socrata.com";
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
		setUpdatedMetadataJob(metadataJob);
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
		UserPreferencesJava prefs = setupUserPreferencesForTest();
		MetadataJob metadataJob = new MetadataJob(prefs);
		metadataJob.setDatasetID(UNITTEST_DATASET_ID);
		setUpdatedMetadataJob(metadataJob);
		
		//Update metadata based on resource file
		JobStatus runResults = metadataJob.run();
		TestCase.assertEquals(JobStatus.SUCCESS, runResults);
		
		//Load current metadata
		String result = metadataJob.loadCurrentMetadata();
		TestCase.assertEquals("", result);
		
		//Assert all values have been updated as expected
		testUpdate(metadataJob);
		
		//Reset metadata based on reset resource file
		setResetMetadataJob(metadataJob);
		JobStatus resetResults = metadataJob.run();
		TestCase.assertEquals(JobStatus.SUCCESS, resetResults);
	}
	
	@Test
	public void testLoadCurrentMetadata() {
		MetadataJob metadataJob = new MetadataJob(setupUserPreferencesForTest());
		
		//Load current metadata
		metadataJob.setDatasetID(UNITTEST_DATASET_ID);
		metadataJob.loadCurrentMetadata();
		
		//Assert all values are as expected
		testReset(metadataJob);
	}
	
	private void testReset(MetadataJob metadataJob) {
		TestCase.assertEquals(RESET_CATEGORY, standardize(metadataJob.getCategory()));
		TestCase.assertEquals(RESET_KEYWORDS, metadataJob.getKeywords());
		TestCase.assertEquals(RESET_LICENSE_TYPE, metadataJob.getLicenseType());
		TestCase.assertEquals(RESET_DATA_PROVIDED_BY, standardize(metadataJob.getDataProvidedBy()));
		TestCase.assertEquals(RESET_SOURCE_LINK, standardize(metadataJob.getSourceLink()));
		TestCase.assertEquals(RESET_CONTACT_INFO, standardize(metadataJob.getContactInfo()));
		TestCase.assertEquals(RESET_TITLE, standardize(metadataJob.getTitle()));
		TestCase.assertEquals(RESET_DESCRIPTION, standardize(metadataJob.getDescription()));		
	}
	
	private void testUpdate(MetadataJob metadataJob) {
		TestCase.assertEquals(CATEGORY, standardize(metadataJob.getCategory())); 
		TestCase.assertEquals(KEYWORDS, metadataJob.getKeywords());
		TestCase.assertEquals(LICENSE_TYPE, metadataJob.getLicenseType());
		TestCase.assertEquals(DATA_PROVIDED_BY, standardize(metadataJob.getDataProvidedBy()));
		TestCase.assertEquals(SOURCE_LINK, standardize(metadataJob.getSourceLink()));
		TestCase.assertEquals(CONTACT_INFO, standardize(metadataJob.getContactInfo()));
		TestCase.assertEquals(TITLE, standardize(metadataJob.getTitle()));
		TestCase.assertEquals(DESCRIPTION, standardize(metadataJob.getDescription()));		
	}
	
	private void setUpdatedMetadataJob(MetadataJob metadataJob) {
		metadataJob.setCategory(CATEGORY);
		metadataJob.setKeywords(KEYWORDS);
		metadataJob.setLicenseType(LICENSE_TYPE);
		metadataJob.setDataProvidedBy(DATA_PROVIDED_BY);
		metadataJob.setSourceLink(SOURCE_LINK);
		metadataJob.setContactInfo(CONTACT_INFO);
		metadataJob.setTitle(TITLE);
		metadataJob.setDescription(DESCRIPTION);
	}
	
	private void setResetMetadataJob(MetadataJob metadataJob) {
		metadataJob.setCategory(RESET_CATEGORY);
		metadataJob.setKeywords(RESET_KEYWORDS);
		metadataJob.setLicenseType(RESET_LICENSE_TYPE);
		metadataJob.setDataProvidedBy(RESET_DATA_PROVIDED_BY);
		metadataJob.setSourceLink(RESET_SOURCE_LINK);
		metadataJob.setContactInfo(RESET_CONTACT_INFO);
		metadataJob.setTitle(RESET_TITLE);
		metadataJob.setDescription(RESET_DESCRIPTION);
	}
	
	//Setting empty values via web interface and API differ in empty strings or null values
	//We will standardize to empty strings for comparisons
	private String standardize(String input) {
		if (input == null) {
			return "";		
		}
		else {
			return input;
		}
	}
}
