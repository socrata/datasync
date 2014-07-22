---
layout: with-sidebar
title: Setup a Standard Job (GUI)
bodyclass: homepage
---

This guide covers how to set up a job using Socrata DataSync with its graphical user interface. DataSync can also be run [headlessly (in command-line mode)]({{ site.root }}/guides/setup-standard-job-headless.html).

### Step 1: Enter authentication details
Enter your authentication details at the bottom left of DataSync (domain, username, password, and app token). The domain is the root domain of your data site and must begin with https:// (e.g. [https://data.cityofchicago.org](https://data.cityofchicago.org)). The username and password are those of a Socrata account that has a Publisher role. Enter your App token or if you have not yet created one read [how to obtain an App token](http://dev.socrata.com/docs/app-tokens.html). After you enter these details they will be saved and used to run every job you save using DataSync. We recommend creating a dedicated Socrata account (with a Publisher role or Owner permissions to specific datasets) to use with DataSync rather than tie DataSync to a particular person’s primary account.

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.


### Step 2: Enter job details

Upon opening the program a new 'Untitled job' appears in a tab. You can have any number of jobs open and each will be in their own tab.

**Choose the CSV or TSV File to Publish**

Select the CSV or TSV file on your local machine or networked folder that you wish to publish by clicking the “Browse...” button and browsing to find the CSV or TSV file.

**Does the CSV/TSV Have a Header Row ?**

If so, keep "File to publish contains header row" checked if the CSV/TSV contains a header row. The header row should contain the identifiers of the columns (a.k.a. API Field Names) in the same order as the Socrata dataset you are publishing to. However, for certain update operations you can supply a subset of the columns or order the columns differently than the order in the dataset (more details below). To get a list of column identifiers for a dataset click the 'Get Column IDs' button after entering the Dataset ID.
<br><br>
If not, uncheck "File to publish contains header row". In this case the order of the columns in the CSV/TSV must exactly match that of Socrata dataset.


**Obtain and Enter the Dataset ID ...**

You will need the identifier of the dataset (dataset ID) you wish to publish to. To obtain the dataset ID navigate to the dataset in your web browser and in the address bar the dataset ID is the code at the end of the URL in the form (xxxx-xxxx). For example for the following URL to a dataset:

https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw

The dataset ID is: m985-ywaw

Enter your dataset's ID into the Data


**Choose the Publish Method ...**

Select the 'Publish method' by selecting one of the following options:

- `replace`: simply replaces the dataset with the data in the CSV/TSV file to publish which can be performed in one of three ways:
    1. via HTTP using Delta-importer-2 (keep 'HTTP' checked): This is the preferred option because it
      - it minimizes the amount of data sent by only sending the changes since the last update, rather than the complete dataset
      - it can reliably handle very large files (1 million+ rows)
      - it allows configuration of the way the CSV/TSV file is read and processed through the use of a [control file]({{ site.root }}/resources/ftp-control-config.html)
      *This option is available only in DataSync versions 1.5 and higher.*
    2. via FTP: This functions in much the same way as the HTTP variant, with 2 notable differences:
      - the entire CSV/TSV file will transfered
      - if you are running DataSync behind a firewall it must be configured to allow FTP traffic through ports 22222 (for the control connection) and all ports within the range of 3131 to 3141 (for data connection)
    3. via Soda2: *Deprecated.*  This method is not recommended because of its inefficiencies and file size limitations (< 5 MB).  This does operate over HTTP.

- `upsert`: updates any rows that already exist and appends any new rows. This option is ideal if you have a dataset that requires very frequent updates or in cases where doing a complete replace is problematic.

*IMPORTANT NOTE: For updating to work properly you must set a Row Identifier for the dataset. If a Row Identifier is not set then all rows in the CSV/TSV file will be appended to the dataset. [Learn more about Row Identifiers and how to establish them](http://dev.socrata.com/docs/row-identifiers.html)*

- `append`: adds all rows in the CSV/TSV as new rows. The append method cannot be used if a Row Identifier has been established on the dataset.

- `delete`: delete all rows matching Row Identifiers given in CSV/TSV file. The CSV/TSV should only contain a single column listing the Row Identifiers to delete.

*IMPORTANT NOTE: delete will not work unless the dataset has a Row Identifier established.*

<div class="well">
If you are using replace (via Soda2) or upsert or append and your TSV/CSV has a header row then you do not need to supply all columns in the CSV/TSV and the order of columns does not need to match that of the Socrata dataset.

<!--
TODO: WHAT HAPPENS TO OMITTED COLUMNS (TEST THIS!!!)...is this different than DELTA IMPORTER 2??
-->
<br><br>
If you are using replace (via FTP or HTTP) you may reorder columns.  If the dataset you are publishing to has a Row Identifier established then you may also omit columns in the CSV/TSV.
</div>

**Control File Configuration (needed for replace via FTP or HTTP)**

When using replace via any option besides Soda2 you must supply or generate a control file. In many cases simply clicking the 'Generate/Edit' and using the default configuration will be sufficient for the job to run successfully. Do not click 'Generate/Edit' if you want to use a Control file that is saved as a file, instead click 'Browse...' and select the file. The cases where you will need to modify the Control file content include, but are not limited to:

* If your CSV contains date/time data in a format other than: [ISO8601](http://en.wikipedia.org/wiki/ISO_8601), MM/dd/yyyy, MM/dd/yy, or dd-MMM-yyyy (e.g. "2014-04-22", "2014-04-22T05:44:38", "04/22/2014", "4/22/2014", "4/22/14", and "22-Apr-2014" would all be fine).
* Dataset has a Location column that will be populated from existing columns (e.g. address, city, state, zipcode)
* Dataset has a Location column but you are not using Socrata's geocoding (you provide latitude/longitude in CSV/TSV file)
* If you wish to set the timezone of the dates being imported


For more detailed information on establishing configuration in the Control file refer to [Control file configuration]({{ site.root }}/resources/ftp-control-config.html)

### Step 3: Run the job and optionally save it

Now that you have entered all the information for your job you should run it by clicking the “Run Job Now” button or going to File -> Run Job Now. A loading spinner will appear as the job runs (how long depends on the size of the CSV you are uploading). A popup will indicate if the job was successful or notify you of any errors. As long as the job was successful you can save the job to a file on the computer by clicking “Save Job” and selecting a location to save the file on your computer (job files are saved as .sij files). When saving your job we recommend choosing a filename that does not contain spaces (e.g. 'my_job.sij', do not use a name like 'my job.sij'). After saving the file the '*Command to execute with scheduler*' field is populated with the command to run the given job (for scheduling) which is relevant in the next section. You can open a previously saved job by going to File -> Open and finding the job file you saved previously.

**NOTE:** You should always close DataSync after you are finished because it may cause issues if it is open when jobs are run using the scheduler.

<div class="well">
When running jobs in the UI DataSync does not display detailed logging information. To view detailed logging information instead of clicking 'Run Job Now' run the job by copying the 'Command to execute with scheduler' (click 'Copy to clipboard' next to the textbox) and running that command in your Terminal/Command Prompt. The terminal will output detailed logging information as the job runs.
</div>

### Step 4: Scheduling your jobs to run at a specified interval

To automate updating a dataset you must schedule the DataSync job to run automatically at a specified interval (i.e once per day). This can be done with standard tools such as the Windows Task Scheduler or Cron.

[Read the documentation for how to schedule a saved job]({{ site.root }}/resources/schedule-job.html)

### Additional configuration

To take advantage of job logging, automatic email error notification, and file chunking (for publishing large files) in DataSync refer to [Preferences configuration documentation]({{ site.root }}/resources/preferences-config.html).