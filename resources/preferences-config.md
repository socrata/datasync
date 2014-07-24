---
layout: with-sidebar
title: Preferences Configuration
bodyclass: homepage
---

### Contents
- [Set up logging (using a dataset)](#setup-logging)
- [Error Notification Auto-Email Setup](#error-notification)
- [Chunking Configuration](#chunking-config)
- [Proxy Configuration](#proxy-config)

### Setting up logging (using a dataset)

You can set up a Socrata dataset to store log information each time a DataSync jobs runs. This is especially useful if you will be [scheduling your jobs]({{ site.root }}/resources/schedule-job.html) to run automatically at some specified interval. You first need to manually create a log dataset. You should probably keep this dataset private (rather than set it as public). The easiest way to se this up is to run a DataSync Port Job that copies the schema from [this example log dataset](https://adrian.demo.socrata.com/dataset/DataSync-Log/aywp-657c).

To run a Port Job in DataSync go to File -> New... -> Port Job and fill out the following fields as noted below:

**Port Method:** Copy schema only
**Source Domain:** https://adrian.demo.socrata.com
**Source Dataset ID:** aywp-657c
**Destination Domain:** `[YOUR DOMAIN]`
**Publish Destination Dataset?:** Yes

Then click the "Run Job Now" button and it will automatically create an empty log dataset on the destination domain you entered.

Alternatively to using a Port Job, if you wish to create the log dataset manually (unlikely, but just in case), download the following CSV file and upload it to your Socrata datasite using the manual upload process in a web browser:
[https://docs.google.com/file/d/0B-VEFikh3T6dX2VyWTZXZW55SFU/edit?usp=sharing](https://docs.google.com/file/d/0B-VEFikh3T6dX2VyWTZXZW55SFU/edit?usp=sharing)

Be sure that you set the column data types to match those listed below:

**Date:** Date & Time
**DatasetID:** Plain Text
**FileToPublish:** Plain Text
**PublishMethod:** Plain Text
**JobFile:** Plain Text
**RowsUpdated:** Number
**RowsCreated:** Number
**RowsDeleted:** Number
**Success:** Checkbox
**Errors:** Plain Text

After you have created the log dataset, In DataSync go to Edit -> Preferences. In the popup window enter the dataset ID of the log dataset you just uploaded or created via DataSync Port Job.

### Error Notification Auto-Email Setup

If you wish for emails to be automatically sent to an administrator if an error occurs when any DataSync job is run enter the administrator’s email address and check the box check the box labeled "Auto-email admin upon error". The same log dataset and administrator email is used for all DataSync jobs (i.e. it is a global setting like the authentication details). For auto-emailing to work you must configure the SMTP settings to point to a server you have access to.

**NOTICE:** Just like with the the authentication details, the SMTP password is stored unencrypted in the Registry on Windows platforms and in analogous locations on Mac and Linux.

If you do not know of an existing SMTP you can use you can register a free GMail account to act as your SMTP server. [Register a new account](https://accounts.google.com/SignUp?service=mail).

After registering your account enter the following details to configure the SMTP settings for your new GMail account:

**Outgoing Mail Server:** smtp.gmail.com
**SMTP Port:** 587
Check the “Use SSL” box
**SSL Port:** 465
**SMTP Username:** `[your GMail username]`
**SMTP Password:** `[your GMail password]`

Once you have entered all the SMTP settings, you should test they are valid by clicking “Test SMTP Settings”. If all goes well click “Save” in the preferences window. Finally, test running your job to make sure both the target dataset and the log dataset get properly updated (one new row will be created in the log dataset each time a job is run).

### Chunking Configuration

Chunking is handled automatically according to the defaults set in Datasync, though in some cases it may be necessary or preferable to adjust the defaults. Two options are avaible:

  - `Chunking filesize threshold`: If the CSV/TSV file size is less than this, the entire file will be sent in one chunk.  The default value is 10 MB.
  - `Chunk size`:  The number of rows to send in each chunk.  The default value is 10,000 rows.  This is only respected if the entire file is not sent in a single chunk because of the `Chunking filesize threshold`.  

 To modify the defaults go to Edit -> Preferences and modify the numbers.

### Proxy Configuration

You can configure DataSync ot use an authenticated or unauthenticated proxy server. Please note, this option is only available for [Standard replace jobs]({{ site.root }}/guides/setup-standard-job.html) and only if choosing 'via HTTP using Delta-importer-2'.  At minimum, the following options will need to be set:

  - `Proxy Host`: The fully qualified host name of the proxy server, e.g. https://myProxyServer.com.
  - `Proxy Port`:  The port that the proxy server listens on, e.g. 8080

If the proxy server is authenticated, you may also set:

  - `Proxy Username`: The username needed to log into the proxy server.
  - `Proxy Password`: The password needed to log into the proxy server.

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may instead [run the job headlessly]({{ site.root }}/guides/setup-standard-job-headless.html), in order to pass the needed credentials in via the commandline.
