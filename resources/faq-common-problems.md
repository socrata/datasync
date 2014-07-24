---
layout: with-sidebar
title: FAQ / Common Problems 
bodyclass: homepage
---

### DataSync keeps failing with "field-not-in-dataset."  What can I do to fix this? 
Typically, this is caused for one of three reasons:
- You're referencing the display names in your control file
- You haven't specified all of the fields in the dataset.  
- Your CSV contains more columns than the dataset.  

To fix the first two bullets, verify that the column names in your control file match the field names in the dataset, and that the list is comprehensive.  

To fix the latter, either remove the column from your dataset, or use the ignoreColumns option found in the [control file]({{ site.root }}/resources/control-config.html) guide.

### My synthetic location column is causing my job to fail, or resulting in unexpected data in my address column
When all fields are not explicitly specified for the location column, the system will attempt to guess at the components by parsing the address.  While this parsing normally works there are notable places where it will fail. To work around this, we recommend explicitly breaking out your address locations into consistuent columns (e.g. address, city, state, zip) and then passing those directly to the synthetic location. 

### What does this error mean? ‘java.lang.UnsupportedClassVersionError: com/socrata/exceptions/SodaError : Unsupported major.minor version 51.0’
Your version of Java is too old, you should update to at least Java 7. Get the [latest version of Java here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

### After running a job successfully some or all of the columns in the dataset appear as blank instead of the data in the uploaded CSV.
Your header row containing the column names in the dataset does not exactly match the column names in the dataset. Note that the column names are case sensitive. It is best to use the column identifiers (a.k.a. API field names) in your header row, which can be easily obtained for a dataset by clicking the “Get Column ID” button within DataSync.

### What does this error mean? '...PRIX certificate validation failed'
This error is caused by the version of Java being out-of-date and as a result failing to validate the SSL certificate. To correct the issue you must update Java JDK or JRE on the machine running DataSync or, alternatively, specifically add the necessary certificates into their trusted certificate stores. If you are still getting the error please contact Socrata support.
  
### I am getting an ‘invalid/corrupt JAR file’ message when trying to open JAR
Redownload the DataSync JAR from: [https://github.com/socrata/datasync/releases](https://github.com/socrata/datasync/releases)
  
### How do I use DataSync to accept any date/time format?
This is only possible in DataSync version 1.0 and higher. Refer to [this documentation](http://socrata.github.io/datasync/resources/ftp-control-config.html#date-time). 

### I'm still stuck.  What can I do?
Verify that your CSV meets all of the restrictions detailed in the [conditions and restrictions]({{ site.root }}/resources/conditions-restrictions.html) guide.  If you are still having trouble, please contact your Socrata representive for support.
