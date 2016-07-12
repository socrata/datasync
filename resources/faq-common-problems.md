---
layout: with-sidebar
title: FAQ / Common Problems
bodyclass: homepage
---

### What is the ID of my dataset?
To obtain the dataset ID navigate to the dataset in your web browser and inspect the address bar.  The dataset ID can be found at end of the URL in the form (xxxx-xxxx). For example, for the following URL to a dataset:

https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw

The dataset ID is: `m985-ywaw`

### How do I find the API field names for my columns?
Columns within a dataset have both a display name and an API field name.  Datasync only operates using API field names.  If using the DataSync GUI, you can get the list of API field names by clicking the 'Get Column IDs' button after entering the Dataset ID.  You may also view the API field names from your browser, by hovering over the information icon on any column.

### What datatypes does DataSync support?

Datasync supports the Text, Formatted Text, Number, Money, Percent, Date & Time (with or without timezone), Location, Website URL, Email, Checkbox, Flag, Star and Phone datatypes.  Please refer to the [conditions/restrictions resource]({{ site.baseurl }}/resources/conditions-restrictions.html) for formatting requirements of each.

### DataSync keeps failing with "field-not-in-dataset."  What can I do to fix this?
Typically, this is caused for one of three reasons:
- You're referencing the display names in your control file
- You haven't specified all of the fields in the dataset.
- Your CSV contains more columns than the dataset.

To fix the first two bullets, verify that the column names in your control file match the field names in the dataset, and that the list is comprehensive.

To fix the latter, either remove the column from your dataset, or use the ignoreColumns option found in the [control file]({{ site.baseurl }}/resources/control-config.html) guide.

### My location column is causing my job to fail, or resulting in unexpected data in my address column
When all fields are not explicitly specified for the location column, the system will attempt to guess at the components by parsing the address.  While this parsing normally works there are notable places where it will fail. To work around this, we recommend explicitly breaking out your address locations into consistuent columns (e.g. address, city, state, zip) and then passing those directly to the synthetic location.

### What does this error mean? ‘java.lang.UnsupportedClassVersionError: com/socrata/exceptions/SodaError : Unsupported major.minor version 51.0’
Your version of Java is too old, you should update to at least Java 7. Get the [latest version of Java here](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

### After running a Soda2 job successfully some or all of the columns in the dataset appear as blank instead of the data in the uploaded CSV.
Your header row containing the column names in the dataset does not exactly match the column names in the dataset. Note that the column names are case sensitive. It is best to use the column identifiers (a.k.a. API field names) in your header row, which can be easily obtained for a dataset by clicking the “Get Column ID” button within DataSync.

### What does this error mean? '... certificate validation failed'
If you receive a SunCertPathBuilderException, there are two typical causes:

  1. Java is out-of-date and as a result is failing to validate the SSL certificate. To correct this issue you must update Java JDK or JRE on the machine running DataSync.
  2. Java does not approve of one of the certificates in the chain between your machine and the domain you're trying to upload to.  The solution is to add the necessary certificates into Java's trusted certificate store. The steps to do this are:

  * Get the certificate chain.
    * Find where Java's keytool is located.
      * On Windows, this is likely at "C:\Program Files\Java\jre7\bin")
      * On Mac OS X, this is likely at "/Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/bin/"
    * Run the following, removing the proxy options if you are not behind a proxy server. You can remove the '-rfc' option to get additional information about each certificate in the chain.

           keytool -J-Dhttps.proxyHost=<PROXY_HOST>
                   -J-Dhttps.proxyPort=<PROXY_PORT>
                   -printcert -rfc
                   -sslserver <DOMAIN>:443

  * **Validate any certificates you plan to add with your IT department !!!!**.  It is a security risk to add unknown certificates.
  * Copy the cert you need to add inclusively from -----BEGIN CERTIFICATE----- to -----END CERTIFICATE----- into a file `<FILENAME>`.cer
  * Run the following, using your keystore password if that has been set up or the default password 'changeit' otherwise.

           keytool -import -keystore cacerts -file <FILENAME>.cer


### "syntheticLocations" is a field, why do I get this error? '...UnrecognizedPropertyException: Unrecognized field "syntheticLocations" ... reference chain: com.socrata.datasync.config.controlfile.ControlFile["syntheticLocations"]'
This error is because the "syntheticLocations" field is in the wrong level of the control file. It needs to be within the "csv" or "tsv" object, since it contains details about how to interpret the CSV or TSV.

### My job is failing because of a "Corrupt SSync patch"
This error is most likely caused by insufficient heap space.  Try starting up DataSync with additional heap space using one of the options below:

    java -jar -Xmx500m <DATASYNC_JAR>

    java -jar -Xmx1g <DATASYNC_JAR>

The former allows java to use 500 MB of space and the latter 1 GB of space.  If the problem persists please contact your Socrata representive for support.

### I am getting an ‘invalid/corrupt JAR file’ message when trying to open JAR
Redownload the DataSync JAR from: [https://github.com/socrata/datasync/releases](https://github.com/socrata/datasync/releases)

### How do I use DataSync to accept any date/time format?
This is only possible in DataSync version 1.0 and higher. Refer to [this documentation](http://socrata.github.io/datasync/resources/control-config.html#date-time).

### I'm getting a ‘Connection refused’ error / I'm having network problems.

Please reference our [Network Considerations resource]({{ site.baseurl }}/resources/network-considerations.html).

### I'm still stuck.  What can I do?
Verify that your CSV meets all of the restrictions detailed in the [conditions and restrictions]({{ site.baseurl }}/resources/conditions-restrictions.html) guide.  If you are still having trouble, please contact your Socrata representive for support.
