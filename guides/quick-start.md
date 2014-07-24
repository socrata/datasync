---
layout: with-sidebar
title: Quick start
bodyclass: homepage
---

This guide covers provides an overview of the minimal set of steps to use DataSync to sync a local CSV with a dataset on the Socrata platform.  

**NOTICE:** The steps in this guide will replace the entire contents of the dataset with the contents of the CSV.  For a more in depth guide of using DataSync, including information on how to use DataSync to update and append information to a dataset,  please see [setting up a standard job]({{ site.root }}/guides/setup-standard-job.html).

### Step 1: Download DataSync
Navigate to the DataSync [download page]({{site.root}}/datasync/releases}}), and download the latest version. 

### Step 2: Launching DataSync
Launch DataSync navigating to the folder containing the Datasync JAR file that you downloaded previously and run the following command:

```
java -jar DataSync-1.5-jar-with-dependencies.jar
```

### Step 3: Enter authentication details
Enter your authentication details (domain, username, password, and app token) at the bottom left of DataSync.  The domain is the root domain of your data site and must begin with https:// (i.e. [https://opendata.socrata.com](https://opendata.socrata.com)). The username and password are those of a Socrata account that has a Publisher role. Enter your App token.  If you do not yet have an app token, please see [how to obtain an App token](http://dev.socrata.com/docs/app-tokens.html). The username, password and application token will be saved as part of the job configuration.  We recommend creating a dedicated Socrata account (with a Publisher role or Owner permissions to specific datasets) to use with DataSync rather than tying DataSync to a particular person’s account.

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.

### Step 4: Enter job details

**Choose the CSV or TSV File to Publish**

Select the CSV or TSV file on your local machine or networked folder that you wish to publish by clicking the “Browse...” button and browsing to find the CSV or TSV file.

**Does the CSV/TSV Have a Header Row ?**

If the CSV contains a header row, keep "File to publish contains header row" checked if the CSV/TSV contains a header row. The header row should contain the identifiers of the columns (a.k.a. API Field Names) in the same order as the Socrata dataset you are publishing to. 
<br><br>
If the CSV does not contain a header row, uncheck "File to publish contains header row". In this case the order of the columns in the CSV/TSV must exactly match that of Socrata dataset.


**Obtain and Enter the Dataset ID ...**

You will need the identifier of the dataset (dataset ID) that you want to update. To obtain the dataset ID navigate to the dataset in your web browser and inspect the address bar. The dataset ID can be found at end of the URL in the form (xxxx-xxxx). For example for the following URL to a dataset:

https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw

The dataset ID is: m985-ywaw

Enter your dataset's ID into the Dataset ID field


**Choose the Publish Method ...**

Leave the default option "replace via HTTP" selecte.d  This option will...
      - gracefully handles network failures
      - minimizes the amount of data sent by only sending the changes since the last update, rather than the complete dataset
      - can reliably handle very large files (1 million+ rows)
      - allows configuration of the way the CSV/TSV file is read and processed through the use of a [control file]({{ site.root }}/resources/control-config.html)

**Create a control file**
A control file is needed to help DataSync interpret the data within the CSV. In most cases simply clicking the 'Generate/Edit' button to generate a control file with the default configuration will be sufficient for the job to run successfully. 

For more detailed information on establishing configuration in the Control file refer to [Control file configuration]({{ site.root }}/resources/control-config.html)

### Step 5: Run the job

Run the job by clicking the “Run Job Now” button. A loading spinner will appear as the job runs.  The length of the job will depend on the size of the CSV / TSV uploaded. Once complete, a popup will indicate if the job was successful or notify you of any errors. 

### Step 6: Save the job for later use

If the job was successful you can save the job to a file on the computer by clicking “Save Job” and selecting a location to save the file on your computer (job files are saved as .sij files). When saving your job we recommend choosing a filename that does not contain spaces (e.g. 'my_job.sij', do not use a name like 'my job.sij'). 

After saving the file the '*Command to execute with scheduler*' field is populated with the command to run the given job. Customers can use this field to run DataSync from the command line on a scheduled basis. You can later open a previously saved job by going to File -> Open and finding the job file you saved previously.

**NOTE:** You should always close DataSync after you are finished.  Failure to do so may cause issues when jobs are run using the scheduler.

To automate updating a dataset you must schedule the DataSync job to run automatically at a specified interval (i.e once per day). This can be done with standard tools such as the Windows Task Scheduler or Cron.

[Read the documentation for how to schedule a saved job]({{ site.root }}/resources/schedule-job.html)

### Additional information

To take advantage of DataSync's more advanced features, please see  [setting up a standard job]({{ site.root }}/guides/setup-standard-job.html).
