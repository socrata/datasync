---
layout: with-sidebar
title: Setup a Standard Job (GUI)
bodyclass: homepage
---

This guide covers how to set up a job using Socrata DataSync's UI. Alternatively, [DataSync can also be run from the command line in headless mode]({{ site.root }}/guides/setup-standard-job-headless.html).

### Step 1: Download DataSync
Navigate to the DataSync [download page](https://github.com/socrata/datasync/releases), and download the latest version.

### Step 2: Launching DataSync
Launch DataSync navigating to the folder containing the Datasync JAR file that you downloaded previously and either double-click the jar or run the following command:

    java -jar <DATASYNC_JAR>


### Step 3: Enter authentication details
Enter your authentication details at the bottom left of DataSync (domain, username, password, and app token). The domain is the root domain of your data site and must begin with https:// (i.e. [https://opendata.socrata.com](https://opendata.socrata.com)). The username and password are those of a Socrata account that has a Publisher role. Enter your App token.  If you do not yet have an app token, please see [how to obtain an App token](http://dev.socrata.com/docs/app-tokens.html). The username, password and application token will be saved as part of the job configuration.  We recommend creating a dedicated Socrata account (with a Publisher role or Owner permissions to specific datasets) to use with DataSync rather than tying DataSync to a particular person’s account.

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.

### Step 4: Enter job details

Upon opening the program a new 'Untitled job' appears in a tab. You can have any number of jobs open and each will be in their own tab.

**Choose the CSV or TSV File to Publish**

Select the CSV or TSV file on your local machine or networked folder that you wish to publish by clicking the “Browse...” button and browsing to find the CSV or TSV file.

**Does the CSV/TSV Have a Header Row ?**

If so, keep "File to publish contains header row" checked. If not, uncheck "File to publish contains header row".

  - *Header naming*

   Header rows should contain the [API field names]({{ site.root }}/resources/faq-common-problems#how-do-i-find-the-api-field-names-for-my-columns.html) of each column.  Please note these may differ from the display names shown in your browser.

  - *Column order*

  Columns can safely be reorderd as long as header information is provided. Header information can either be supplied within the CSV/TSV file or through use of the control file (see below).  If header information is missing and you are publishing via Soda2, the order of the columns in the CSV/TSV must exactly match that of Socrata dataset. Note that header information is a requirement if you are publishing by either FTP or HTTP.

  - *Column inclusion/exclusion*

  If using Soda2, you needn't supply all of the columnns.  If using FTP or HTTP, the ability to exclude columns from the CSV/TSV file is based on whether the Socrata dataset has a [row identifier](http://dev.socrata.com/docs/row-identifiers.html) set. If so, you may omit any columns but the identifier. If not, all columns must be provided.

  If using Soda2, you may include extraneous columns; these will be ignored.  If using FTP or HTTP, you may include extraneous columns, but these must be ignored by listing them in the 'ignoreColumns' option of the control file.


**Enter the Dataset ID ...**

You will need the identifier of the dataset (i.e. [the dataset ID](http://socrata.github.io/datasync/resources/fac-common-problems.html#what-is-the-id-of-my-dataset)) that you want to update. Enter your dataset's ID into the Dataset ID field.


**Choose the Publish Method ...**

Select the 'Publish method' by selecting one of the following options:

- `replace`: Replaces the dataset with the data in the CSV/TSV file.
- `upsert`: Updates any rows that already exist and inserts rows which do not. Ideal if you have a dataset that requires very frequent updates or in cases where doing a complete replace is problematic.

  *IMPORTANT NOTE: For updating to work properly you must set a Row Identifier for the dataset. If a Row Identifier is not set then DataSync will not be able to determine what rows to update and all rows in the CSV/TSV file will be appended to the dataset. [Learn more about Row Identifiers and how to establish them](http://dev.socrata.com/docs/row-identifiers.html).*

- `append`: Same as upsert.  Deprecated.

- `delete`: Delete all rows matching Row Identifiers given in CSV/TSV file. The CSV/TSV should only contain a single column listing the Row Identifiers to delete.

  *IMPORTANT NOTE: delete will not work unless the dataset has a Row Identifier established.*


DataSync offers three ways to upload your data

  1. via HTTP: *This option is available only in DataSync versions 1.5 and higher.*  This is the preferred option because it...
    - gracefully handles network failures
    - minimizes the amount of data sent by only sending the changes since the last update, rather than the complete dataset
    - can reliably handle very large files (1 million+ rows)
    - allows configuration of the way the CSV/TSV file is read and processed through the use of a [control file]({{ site.root }}/resources/control-config.html)
  2. via FTP (only available for `replace`): This functions in much the same way as the HTTP variant, with 2 notable differences:
    - the entire CSV/TSV file will transfered
    - if you are running DataSync behind a firewall it must be configured to allow FTP traffic through ports 22222 (for the control connection) and all ports within the range of 3131 to 3141 (for data connection)
  3. via Soda2: *Deprecated.*  This method is not recommended because of its inefficiencies and file size limitations (< 5 MB).


**Control File Configuration (needed for replace via FTP or HTTP)**

When using replace via FTP or HTTP you must supply or generate a control file. In many cases simply clicking the 'Generate/Edit' button and using the default configuration will be sufficient for the job to run successfully. Do not click 'Generate/Edit' if you want to use a Control file that is saved as a file, instead click 'Browse...' and select the file. The cases where you will need to modify the Control file content include, but are not limited to:

* If your CSV contains date/time data in a format other than: [ISO8601](http://en.wikipedia.org/wiki/ISO_8601), MM/dd/yyyy, MM/dd/yy, or dd-MMM-yyyy (e.g. "2014-04-22", "2014-04-22T05:44:38", "04/22/2014", "4/22/2014", "4/22/14", and "22-Apr-2014" would all be fine).
* The Socrata dataset has a Location column that will be populated from existing columns (e.g. address, city, state, zipcode).
* The Socrata dataset has a Location column and you are <strong>not</strong> using Socrata's geocoding (i.e. you are providing the latitude/longitude coordinates in the CSV/TSV file).
* If you wish to set the timezone of the dates being imported.


For more detailed information on establishing configuration in the Control file refer to [Control file configuration]({{ site.root }}/resources/control-config.html)

### Step 5: Run the job and optionally save it
You can run your job by clicking the “Run Job Now” button. A loading spinner will appear as the job runs.  The length of the job will depend on the size of the CSV / TSV uploaded. Once complete, a popup will indicate if the job was successful or notify you of any errors.

If the job was successful you can save the job to a file on the computer by clicking “Save Job” and selecting a location to save the file on your computer (job files are saved as .sij files). When saving your job we recommend choosing a filename that does not contain spaces (e.g. 'my_job.sij', do not use a name like 'my job.sij'). After saving the file the '*Command to execute with scheduler*' field is populated with the command to run the given job. Customers can use this field to run DataSync from the command line on a scheduled basis. You can later open a previously saved job by going to File -> Open and finding the job file you saved previously.

**NOTE:** You should always close DataSync after you are finished.  Failure to do so may cause issues when jobs are run using the scheduler.

<div class="well">
When running jobs in the UI DataSync does not display detailed logging information. To view detailed logging information you will need to run DataSync from the command line.  To do so, copy the 'Command to execute with scheduler' (click 'Copy to clipboard' next to the textbox) and run that command in your Terminal/Command Prompt. The terminal will output detailed logging information as the job runs.
</div>

### Step 6: Scheduling your jobs to run at a specified interval
To automate updating a dataset you must schedule the DataSync job to run automatically at a specified interval (i.e once per day). This can be done with standard tools such as the Windows Task Scheduler or Cron.

[Read the documentation for how to schedule a saved job]({{ site.root }}/resources/schedule-job.html).

### Additional configuration

To take advantage of job logging, automatic email error notification, proxy support or file chunking (for publishing large files) in DataSync refer to the [preferences configuration documentation]({{ site.root }}/resources/preferences-config.html).
