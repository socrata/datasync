---
layout: with-sidebar
title: Setup a GIS Job (GUI)
bodyclass: homepage
---

GIS jobs are used to upload geospatial data (zipped shapefiles, geoJSON, KML, or KMZ files) to replace specific geospatial datasets on the Socrata platform.

### Step 1: Download DataSync
Navigate to the DataSync [download page](https://github.com/socrata/datasync/releases), and download the latest version.

### Step 2: Launching DataSync
Launch DataSync navigating to the folder containing the Datasync JAR file that you downloaded previously and either double-click the jar or run the following command:

    java -jar <DATASYNC_JAR>


### Step 3: Create a new GIS Job.

In the DataSync UI go to `File -> New... -> GIS Job`. This will open up a new GIS Job.

### Step 4: Enter authentication details
Enter your authentication details at the bottom left of DataSync (domain, username, password, and app token). The domain is the root domain of your data site and must begin with https:// (i.e. [https://data.cityofchicago.org](https://data.cityofchicago.org)). The username and password are those of a Socrata account that has a Publisher role. Enter your App token.  If you do not yet have an app token, please see [how to obtain an App token](http://dev.socrata.com/docs/app-tokens.html). The username, password and application token will be saved as part of the job configuration.  We recommend creating a dedicated Socrata account (with a Publisher role or Owner permissions to specific datasets) to use with DataSync rather than tying DataSync to a particular person’s account.

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.

### Step 5: Enter GIS job details

**Choose the File to Publish**

Select the zipped shapefile, geoJSON, KML, or KMZ file on your local machine or networked folder that you wish to publish by clicking the “Browse...” button and browsing to find file.

**Obtain and Enter the Dataset ID**

You will need the identifier of the dataset (dataset ID) that you want to update. To obtain the dataset ID navigate to the dataset in your web browser and inspect the address bar. The dataset ID can be found at end of the URL in the form (xxxx-xxxx). For example for the following URL to a dataset:

https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw

The dataset ID is: m985-ywaw

Enter your dataset's ID into the Dataset ID field.

### Step 6:  Run, save and schedule the Job.
Once your job is setup, you can run it like any other job.  For more details, please see steps 4 and 5 of the [Setting up a Standard Job (GUI)]({{ site.baseurl }}/guides/setup-standard-job.html) Guide.
