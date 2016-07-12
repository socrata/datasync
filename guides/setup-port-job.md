---
layout: with-sidebar
title: Setup a Port Job (GUI)
bodyclass: homepage
---

Port jobs are used for copying datasets that are already on the Socrata platform. Port jobs allow users with publishing rights to copy both dataset schemas (metadata and columns) and data (rows). This guide shows how to setup and run a Port Job using the graphical user interface.

### Step 1: Download DataSync
Navigate to the DataSync [download page](https://github.com/socrata/datasync/releases), and download the latest version.

### Step 2: Launching DataSync
Launch DataSync navigating to the folder containing the Datasync JAR file that you downloaded previously and either double-click the jar or run the following command:

    java -jar <DATASYNC_JAR>


### Step 3: Create a new Port Job.

In the DataSync UI go to `File -> New... -> Port Job`. This will open up a new Port Job.

### Step 4: Enter authentication details
Enter your authentication details at the bottom left of DataSync (domain, username, password, and app token). The domain is the root domain of your data site and must begin with https:// (i.e. [https://data.cityofchicago.org](https://data.cityofchicago.org)). The username and password are those of a Socrata account that has a Publisher role. Enter your App token.  If you do not yet have an app token, please see [how to obtain an App token](http://dev.socrata.com/docs/app-tokens.html). The username, password and application token will be saved as part of the job configuration.  We recommend creating a dedicated Socrata account (with a Publisher role or Owner permissions to specific datasets) to use with DataSync rather than tying DataSync to a particular person’s account.

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.

### Step 5:  Configure the Port Job.

The configurable options to run a Port Job are:

- *Port Method*:  Choose one of the following:
  1. `Copy schema only`:  This will copy the metadata and columns of the source dataset into a new dataset.  No row data is copied over.
  2. `Copy schema and data`:  This copies both the metadata/column info and all row data, effectively making a duplicate of the source dataset.
  3. `Copy data only`:  This copies the row data from the source dataset into the destination dataset.  The effect on the destination dataset is determined by the `Publish Method` option below.  Please note, this option will only succeed if the schemas of the source and destination dataset agree.

- *Source Domain*:  The domain to which the source dataset belongs.

- *Source Dataset ID*:  The [dataset identifier](http://socrata.github.io/datasync/resources/fac-common-problems.html#what-is-the-id-of-my-dataset) of the source dataset.

- *Destination Domain*:  The domain where the source dataset will be copied to

- *Destination Dataset ID*:  The [dataset identifier](http://socrata.github.io/datasync/resources/fac-common-problems.html#what-is-the-id-of-my-dataset) of the destination dataset. This is only needed if selecting `Copy data only` as the PortMethod.

- *Publish Method*:  Only relevant if selecting `Copy data only` as the PortMethod. Choose one of the following:
  1. `upsert`:  This will upsert the data from the source dataset into the destination dataset, updating rows that exist already, inserting those that do not.
  2. `replace`: This will replace the data in the destination dataset with that in the source dataset.

- *Publish Destination Dataset*:  Only relevant if copying the schema via `Copy schema only` or `Copy schema and data` as the PortMethod. Choose one of the following:
  1. `Yes`:  This will publish the destination dataset to complete the Port Job.
  2. `No`, create a working copy: This will leave the destination dataset as a working copy.


### Step 6:  Run, save and schedule the Job.
Once your job is setup, you can run it like any other job.  For more details, please see steps 4 and 5 of the [Setting up a Standard Job (GUI)]({{ site.baseurl }}/guides/setup-standard-job.html) Guide.
