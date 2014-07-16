---
layout: with-sidebar
title: Setup a Port Job (GUI)
bodyclass: homepage
---

Port jobs are used for moving data around that is already on the Socrata platform. Users that have publisher rights can make copies of existing datasets using this this tool. Port jobs allow the copying of both dataset schemas (metadata and columns) and data (rows).  This guide shows how to setup and run a Port Job using the graphical user interface.

### Step 1: Create a new Port Job.

In the DataSync UI go to `File -> New... -> Port Job`. This will open up a new Port Job

### Step 2: Enter authentication details
Enter your authentication details at the bottom left of DataSync (domain, username, password, and app token). The domain is the root domain of your data site and must begin with https:// (i.e. [https://data.cityofchicago.org](https://data.cityofchicago.org)). The username and password are those of a Socrata account that has a Publisher role. Enter your App token or if you have not yet created one read [how to obtain an App token](http://dev.socrata.com/docs/app-tokens.html). After you enter these details they will be saved and used to run every job you save using DataSync. We recommend creating a dedicated Socrata account (with a Publisher role or Owner permissions to specific datasets) to use with DataSync rather than tie DataSync to a particular person’s primary account.

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.

### Step 3:  Configure the Port Job.

The configurable options to run a Port Job are:

- `Port Method`:  Choose one of the following:
  - a) Copy schema only:  This will copy the metadata and columns of the source dataset into a new data.  No row data is copied over.
  - b) Copy schema and data:  This copies both the metadata/column info and all row data, effectively making a duplicate of the source dataset.
  - c) Copy data only:  This copies the row data from the source dataset into the destination dataset.  The effect to the destination dataset is informed by the `Publish Method` option below.  Please note, this option will only succeed if the schemas of the source and destination dataset agree.
- `Source Domain`:  The domain to which the source dataset belongs.
- `Source Dataset ID`:  The identifier of the source dataset. To obtain the dataset ID navigate to the dataset in your web browser and in the address bar the dataset ID is the code at the end of the URL in the form (xxxx-xxxx). For example for the following URL to a dataset:

    https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw
    The dataset ID is: m985-ywaw

- `Destination Domain`:  The domain where the source dataset will be copied to; this will become the domain to which the destination dataset belong.
- `Destination Dataset ID`:  Only needed if selecting "Copy data only" as the `PortMethod`.  Similar to the `Source Dataset ID`, this is the identifier of the destination dataset.
- `Publish Method`:  Only relevant if selecting "Copy data only" as the `PortMethod`. Choose one of the following:
  - a) upsert:  This will upsert the data from the source dataset into the destination dataset, thereby adding to or updating data in the destination dataset
  - b) replace: This will replace the data in the destination dataset with that in the source dataset.
- `Publish Destination Dataset`:  Only relevant if not copying the schema via "Copy schema only" or "Copy schema and data" as the `PortMethod`. Choose one of the following:
  - a) Yes:  This will publish the destination dataset to complete the Port Job.
  - b) No, create a working copy: This will leave the destination dataset as a working copy.


### Step 4:  Run, save and schedule the Job.

Please refer to Steps 4 and 5 of the [Setting a Standard Job (GUI)]({{ site.root }}/guides/setup-standard-job.html) Guide.
