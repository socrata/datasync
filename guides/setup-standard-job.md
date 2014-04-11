---
layout: with-sidebar
title: Setup a Standard Job (GUI)
bodyclass: homepage
---


This guide covers how to set up a job using Socrata DataSync with its graphical user interface. DataSync can also be run [headlessly (in command-line mode)]({{ site.root }}/guides/setup-standard-job-headless.html). 

### Step 1: Enter authentication details
Enter your authentication details at the bottom left of DataSync (domain, username, password, and app token). The domain is the root domain of your data site and must begin with https:// (i.e. [https://data.cityofchicago.org](https://data.cityofchicago.org)). The username and password are those of a Socrata account that has a Publishier role. Enter your App token or if you have not yet created one read [how to obtain an App token](http://beta.dev.socrata.com/docs/app-tokens.html). After you enter these details they will be saved and used to run every job you save using DataSync. We recommend creating a dedicated Socrata account (with a Publisher role or Owner permissions to specific datasets) to use with DataSync rather than tie DataSync to a particular person’s primary account. 

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.


### Step 2: Obtain the Dataset ID
You will need the identifier of the dataset (dataset ID) you wish to publish your CSV to using DataSync. To obtain the dataset ID navigate to the dataset in your web browser and in the address bar the dataset ID is the code at the end of the URL in the form (xxxx-xxxx). For example for the following URL to a dataset:

https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw  
The dataset ID is: m985-ywaw

### Step 3: Enter job details

Upon opening the program a new 'Untitled job' appears in a tab. You can have any number of jobs open and each will be in their own tab. Select the CSV or TSV file on your local machine or networked folder that you wish to publish by clicking the “Browse...” button and browsing to find the CSV or TSV file. 

<div class="well">
<strong>If the CSV/TSV Has a Header Row...</strong><br>  
Keep "File to publish contains header row" checked if the CSV/TSV contains a header row. The header row should contain the identifiers of the columns (a.k.a. API Field Names) in the same order as the Socrata dataset you are publishing to. However, for certain update operations you can supply a subset of the columns or order the columns differently than the order in the dataset (more details below). To get a list of column identifiers for a dataset click the 'Get Column IDs' button after entering the Dataset ID.
<br><br>
<strong>If the CSV/TSV Does NOT Have a Header Row...</strong><br>  
If the TSV/CSV file does not contain a header row then uncheck "File to publish contains header row". In this case the order of the columns in the CSV/TSV must exactly match that of Socrata dataset.
</div>

Next, enter the dataset ID from Step 4. Select the 'Publish method' by selecting one of the following options:

- **replace:** simply replaces the dataset with the data in the CSV/TSV file to publish which can be performed in one of two ways:  
**1) via FTP** (keep 'Publish via FTP' checked): **NOTICE: 'Publish via FTP' is only available in the DataSync version 0.4 Prerelease, which will be formally released mid-April.**  This is the preferred option because is highly efficient (it automatically determines the changes since the last update and only publishes those). It can reliably handle very large files (1 million+ rows).  
**2) via HTTP** (uncheck 'Publish via FTP'): currently replace over HTTP is not recommended for, especially with large datasets. However, if your dataset is small (less than 5 MB) and updated frequently it may be a preferable option.

- **upsert:** updates any rows that already exist and appends any new rows. This option is ideal if you have a dataset that requires very frequent updates or in cases where doing a complete replace is problematic.<br> 
*IMPORTANT NOTE: For updating to work properly you must set a Row Identifier for the dataset. If a Row Identifier is not set then all rows in the CSV/TSV file will be appended to the dataset. [Learn more about Row Identifiers and how to establish them](http://support.socrata.com/entries/24247983-Understanding-and-establishing-row-identifiers)*

- **append:** adds all rows in the CSV/TSV as new rows. The append method cannot be used if a Row Identifier has been established on the dataset.

- **delete:** delete all rows matching Row Identifiers given in CSV/TSV file (delete will not work unless the dataset has a Row Identifier established.

<div class="well">
If you are using replace over HTTPS ('publish via FTP' is unchecked), upsert, or append methods and your TSV/CSV has a header row then you do not need to supply all columns in the CSV/TSV and the order of columns does not need to match that of the Socrata dataset. 

<!--
TODO: WHAT HAPPENS TO OMITTED COLUMNS (TEST THIS!!!)...is this different than DELTA IMPORTER 2??
-->
<br><br>
If you are using 'replace via FTP' and the dataset you are publishing to has a Row Identifier established then you may omit or reorder columns in the CSV/TSV.
</div>

**Control File Configuration (only needed for 'replace via FTP')**  
When using 'replace via FTP' you must supply or generate a control file. In most cases simply clicking the 'Generate/Edit' and using the default configuration will be sufficient for the job to run successfully. Do not click 'Generate/Edit' if you want to use a Control file that is saved as a file, instead click 'Browse...' and select the file. The cases where you will need to modify the Control file content include:

* If your CSV contains date/time data in a format other than: ISO8601, MM/dd/yyyy, or MM/dd/yy
* If you wish to set the timezone of the dates being imported
* If you have a Location column that will be populated from existing columns (e.g. address, city, state, zipcode) 

For more detailed information on establishing configuration in the Control file refer to [Control file configuration](https://docs.google.com/a/socrata.com/document/d/1ddB0pvxEo6pylLtECW2XE9mYYzaW8hA7qlzPgSOQ0wg/edit#heading=h.m3u3dqp3qac3)

### Step 4: Test run and save the job

Now that you have entered all the information for your job you should test run it by clicking the “Run Job Now” button or going to File -> Run Job Now. A loading spinner will appear as the job runs (how long depends on the size of the CSV you are uploading). A popup will indicate if the job was successful or notify you of any errors. As long as the job was successful you can save the job to a file on the computer by clicking “Save Job” and selecting a location to save the file on your computer (job files are saved as .sij files). When saving your job we recommend choosing a filename that does not contain spaces (e.g. 'my_job.sij', do not use a name like 'my job.sij'). After saving the file the '*Command to execute with scheduler*' field is populated with the command to run the given job (for scheduling) which is relevant in the next section. You can open a previously saved job by going to File -> Open and finding the job file you saved previously.

**NOTE:** You should always close DataSync after you are finished because it may cause issues if it is open when jobs are run using the scheduler.

<div class="well">
When running jobs in the UI DataSync does not display detailed logging information. To view detailed logging information instead of clicking 'Run Job Now' run the job by copying the 'Command to execute with scheduler' (click 'Copy to clipboard' next to the textbox) and running that command in your Terminal/Command Prompt. The terminal will output detailed logging information as the job runs.
</div>

### Step 5: Scheduling your jobs to run at a specified interval

To automate updating a dataset you must schedule the DataSync job to run automatically at a specified interval (i.e once per day). This can be done with standard tools such as the Windows Task Scheduler or Cron. 

[Read the documentation for how to schedule a saved job]({{ site.root }}/resources/schedule-job.html)

### Additional configuration

To take advantage of job logging, automatic email error notification, and file chunking (for publishing large files) in DataSync refer to [Preferences configuration documentation]({{ site.root }}/resources/preferences-config.html).


