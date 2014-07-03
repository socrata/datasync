---
layout: with-sidebar
title: FAQ / Common Problems 
bodyclass: homepage
---

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