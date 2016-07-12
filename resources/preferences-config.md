---
layout: with-sidebar
title: Preferences Configuration
bodyclass: homepage
---
### Contents
DataSync preferences determine the global configuration to be used by DataSync when running your job.  These preferences can be set either through the UI, or as part of a JSON formatted file for use when running your jobs headlessly.  The full set of options available to you can be found in the table below. Later sections will detail how to take advantage of these settings in specific scenarios.

- [Available options](#available-configuration)
- [Set up logging (using a dataset)](#setting-up-logging-using-a-dataset)
- [Error Notification Auto-Email Setup](#error-notification-auto-email-setup)
- [Chunking Configuration](#chunking-configuration)
- [Proxy Configuration](#proxy-configuration)

### Available configuration
The following options are available to configure DataSync

| Option    | Requirement | Explanation
| ------------- | ------------------------------ | -------------
| domain | required | The scheme and root domain of your data site.  (e.g. https://opendata.socrata.com)
| username | required | Your Socrata username. This user must have a Publisher role or Owner rights to at least one dataset. We recommend creating a dedicated Socrata account (with these permissions) to use with DataSync rather than tie DataSync to a particular person’s primary account. (e.g. publisher@opendata.socrata.com)
| password | required | Your Socrata password. Note that this will be stored in clear-text as part of the file. We recommend taking additional precautions to protect this file, including potentially only adding it when your ETL process runs.
| appToken | required | An app token.   If do not yet have an app token, please reference [how to obtain an App token](http://dev.socrata.com/docs/app-tokens.html).
| logDatasetID | optional | The dataset indentifier of the log dataset. If you have not provisioned a log dataset and would like to do so, please refer to the [logging documentation]({{ site.baseurl }}/resources/preferences-config.html).
| adminEmail | required only if `emailUponError` is "true" | The email address of the administrator or user that error notifications should be sent to.
| emailUponError | optional | Whether to send email notifications of errors that occurred while running jobs. Defaults to "false".
| outgoingMailServer | required only if `emailUponError` is "true" | The address of your SMTP server
| smtpPort | required only if `emailUponError` is "true" | The port of your SMTP server
| sslPort | required only if `emailUponError` is "true" | If SSL port of your SMTP server
| smtpUsername | required only if `emailUponError` is "true" | Your SMTP username
| smtpPassword | required only if `emailUponError` is "true" | Your SMTP password
| filesizeChunkingCutoffMB | Used only for append, upsert, delete and Soda2-replace jobs | If the CSV/TSV file size is less than this, the entire file will be sent in one chunk.  Defaults to 10 MB.
| numRowsPerChunk | Used only for append, upsert, delete and Soda2-replace jobs | The number of rows to send in each chunk.  If the CSV/TSV file size is less than  `filesizeChunkingCutoffMB`, all rows will be sent in one chunk. Defaults to 10,000 rows.
| proxyHost | required if operating through a proxy | The hostname of the proxy server.
| proxyPort | required if operating through a proxy | The port that the proxy server listens on.
| proxyUsername | optional | The username to use if the proxy is authenticated.  If this information is sensitive, you may instead pass it at runtime via the -pun, --proxyUsername commandline option.
| proxyPassword | optional | The password to use if the proxy is authenticated.  If this information is sensitive, you may instead pass it at runtime via the -ppw, --proxyPassword commandline option.


To access, edit and save these options into DataSync &#8216;memory&#8217; using the GUI, navigate to Edit->Preferences.

To save these options into a file, specify them within a single JSON formatted object.  For example, the smallest possible configuration file would resemble:

    {
        "domain": "<YOUR DOMAIN>",
        "username": "<YOUR USERNAME>",
        "password": "<YOUR PASSWORD>",
        "appToken": "<YOUR APP TOKEN>"
    }

Additional options can be added by simply adding the setting name to the file and setting its value accordingly.

To load these options into DataSync &#8216;memory&#8217; without use of the GUI, you can run a LoadPreferences job:

    java -jar <DATASYNC_JAR> -t LoadPreferences -c <CONFIG_FILE>


### Setting up logging (using a dataset)
You can set up a Socrata dataset to store log information each time a DataSync jobs runs. This is especially useful if you will be [scheduling your jobs]({{ site.baseurl }}/resources/schedule-job.html) to run automatically at some specified interval. You first need to manually create a log dataset. You should probably keep this dataset private (rather than set it as public). The easiest way to set this up is to run a DataSync Port Job that copies the schema from [this example log dataset](https://adrian.demo.socrata.com/dataset/DataSync-Log/aywp-657c).

To run a Port Job in DataSync go to File -> New... -> Port Job and fill out the following fields as noted below:

- **Port Method:** Copy schema only
- **Source Domain:** https://adrian.demo.socrata.com
- **Source Dataset ID:** aywp-657c
- **Destination Domain:** `[YOUR DOMAIN]`
- **Publish Destination Dataset?:** Yes

Then click the "Run Job Now" button and it will automatically create an empty log dataset on the destination domain you entered.

Alternatively to using a Port Job, if you wish to create the log dataset manually (unlikely, but just in case), download the following CSV file and upload it to your Socrata datasite using the manual upload process in a web browser:
[https://docs.google.com/file/d/0B-VEFikh3T6dX2VyWTZXZW55SFU/edit?usp=sharing](https://docs.google.com/file/d/0B-VEFikh3T6dX2VyWTZXZW55SFU/edit?usp=sharing)

Be sure that you set the column data types to match those listed below:

- **Date:** Date & Time
- **DatasetID:** Plain Text
- **FileToPublish:** Plain Text
- **PublishMethod:** Plain Text
- **JobFile:** Plain Text
- **RowsUpdated:** Number
- **RowsCreated:** Number
- **RowsDeleted:** Number
- **Success:** Checkbox
- **Errors:** Plain Text

After you have created the log dataset, In DataSync go to Edit -> Preferences. In the popup window enter the dataset ID of the log dataset you just uploaded or created via DataSync Port Job.

### Error Notification Auto-Email Setup
If you wish for emails to be automatically sent to an administrator if an error occurs when any DataSync job is run enter the administrator’s email address and check the box check the box labeled "Auto-email admin upon error". The same log dataset and administrator email is used for all DataSync jobs (i.e. it is a global setting like the authentication details). For auto-emailing to work you must configure the SMTP settings to point to a server you have access to.

**NOTICE:** Just like with the the authentication details, the SMTP password is stored unencrypted in the Registry on Windows platforms and in analogous locations on Mac and Linux.

If you do not know of an existing SMTP server you can use GMail with the following settings:

- **Outgoing Mail Server:** smtp.gmail.com
- **SMTP Port:** 587
- **SSL Port:** 465 (note that you must check the use SSL box)
- **SMTP Username:** `[your GMail username]`
- **SMTP Password:** `[your GMail password]`

Once you have entered all the SMTP settings, you should test they are valid by clicking “Test SMTP Settings”. If all goes well click “Save” in the preferences window. Finally, test running your job to make sure both the target dataset and the log dataset get properly updated (one new row will be created in the log dataset each time a job is run).

### Chunking Configuration
Chunking is handled automatically according to the defaults set in Datasync, though in some cases it may be necessary or preferable to adjust the defaults. Two options are avaible:

  - **Chunking filesize threshold:** If the CSV/TSV file size is less than this, the entire file will be sent in one chunk.  The default value is 10 MB.
  - **Chunk size:**  The number of rows to send in each chunk.  The default value is 10,000 rows.  This is only respected if the entire file is not sent in a single chunk because of the `Chunking filesize threshold`.

 To modify the defaults go to Edit -> Preferences and modify the numbers.

### Proxy Configuration
You can configure DataSync to use an authenticated or unauthenticated proxy server. Please note, this option is only available if running jobs 'via HTTP using Delta-importer-2'.  At minimum, the following options will need to be set:

  - **Proxy Host:** The host name of the proxy server, e.g. myProxyServer.com.
  - **Proxy Port:**  The port that the proxy server listens on, e.g. 8080

If the proxy server is authenticated, you may also set:

  - `Proxy Username`: The username needed to log into the proxy server.
  - `Proxy Password`: The password needed to log into the proxy server.

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may instead [run the job headlessly]({{ site.baseurl }}/guides/setup-standard-job-headless.html), in order to pass the needed credentials in via the commandline.

