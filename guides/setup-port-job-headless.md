---
layout: with-sidebar
title: Setup a Port Job (Headlessly)
bodyclass: homepage
---

Port jobs are used for moving data that is already on the Socrata platform. Users that have publisher rights can make copies of existing datasets using this this tool. Port jobs allow the copying of both dataset schemas (metadata and columns) and data (rows).  This guide shows how to setup and run a Port Job using the command line interface.

### Step 1: Establish “global” configuration (e.g. authentication details)
Please refer to Step 1 in the [documentation for setting up a Standard job]({{ site.root }}/guides/setup-standard-job-headless.html).  This will result in a global configuration file that you'll use later in step 3. 

### Step 2: Obtain the Dataset ID(s)
You will need the dataset ID of the source dataset that you will be copying. To obtain the dataset ID navigate to the dataset in your web browser and inspect the address bar.  The dataset ID can be found at end of the URL in the form (xxxx-xxxx). For example for the following URL to a dataset:

https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw

The dataset ID is: m985-ywaw

If you are copying data from one dataset to another existing dataset, you will also need the dataset ID of the destination dataset.

### Step 3: Configure job details
For general help using DataSync in headless/command-line mode run:

```
java -jar datasync.jar --help
```

To run a job execute the following command, replacing `<..>` with the appropriate values (flags explained below):

```
java -jar datasync.jar -c <CONFIG FILE> -t PortJob -pm copy_all -pd1 <SOURCE DOMAIN> -pi1 <SOURCE DATASET ID> -pd2 <DESTINATION DOMAIN>  -pdt <TITLE OF NEW DATASET> -pp true
```

Explanation of flags:
`*` = required flag

<table>
  <thead>
    <tr>
      <th>Flag - Short Name</th>
      <th>Flag - Long Name</th>
      <th>Example Values</th>
      <th>Description</th>
    </tr>
  </thead>
  <tbody>
    <tr>
      <td style='text-align: left;'><code>-t</code></td>
      <td style='text-align: left;'><code>--jobType</code></td>
      <td style='text-align: left;'>PortJob</td>
      <td style='text-align: left;'>Specifies the type of job to run.</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-c</code></td>
      <td style='text-align: left;'><code>--config</code></td>
      <td style='text-align: left;'>/Users/home/config.json</td>
      <td style='text-align: left;'>Points to the config.json file you created in Step 1 or if not supplied configuration in DataSync &#8216;memory&#8217; is used</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-pm *</code></td>
      <td style='text-align: left;'><code>--portMethod</code></td>
      <td style='text-align: left;'>copy_all</td>
      <td style='text-align: left;'>One of <code>copy_all</code>, <code>copy_schema</code> or <code>copy_data</code></td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>--pd1 *</code></td>
      <td style='text-align: left;'><code>--sourceDomain</code></td>
      <td style='text-align: left;'>https://opendata.socrata.com</td>
      <td style='text-align: left;'>The scheme and domain to which the source dataset belongs.</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-pi1 *</code></td>
      <td style='text-align: left;'><code>--sourceDatasetId</code></td>
      <td style='text-align: left;'>m985-ywaw</td><td style='text-align: left;'>The identifier of the source dataset.</td></tr>
    <tr>
      <td style='text-align: left;'><code>--pd2 *</code></td>
      <td style='text-align: left;'><code>--destinationDomain</code></td>
      <td style='text-align: left;'>https://opendata.socrata.com</td>
      <td style='text-align: left;'>The scheme and domain where the destination dataset should be copied.</td></tr>
    <tr>
      <td style='text-align: left;'><code>-pi2 *</code></td>
      <td style='text-align: left;'><code>--destinationDatasetId</code></td>
      <td style='text-align: left;'>m985-ywaw</td>
      <td style='text-align: left;'>The identifier of the destination dataset; only relevant if choosing <code>copy_data</code> for the <code>portMethod</code></td></tr>
    <tr>
      <td style='text-align: left;'><code>-pdt</code></td>
      <td style='text-align: left;'><code>--destinationDatasetTitle</code></td>
      <td style='text-align: left;'>"Crimes 2014"</td>
      <td style='text-align: left;'>The title to give the destination dataset; only relevant if the destination set is being created by either choosing <code>copy_all</code> or <code>copy_schema</code> for the <code>portMethod</code></td></tr>
    <tr>
      <td style='text-align: left;'><code>-ph</code></td>
      <td style='text-align: left;'><code>--publishViaHttp</code></td>
      <td style='text-align: left;'>true</td>
      <td style='text-align: left;'>Set this to <code>true</code> to use replace-via-http, which is the preferred update method because is highly efficient and can reliably handle very large files (1 million+ rows). If <code>false</code> and <code>publishViaFTP</code> is <code>false</code>, perform the dataset update using Soda2. (<code>false</code> is the default value)</td>(<code>false</code> is the default value)</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-pp</code></td>
      <td style='text-align: left;'><code>--publishDestinationDataset</code></td>
      <td style='text-align: left;'>true</td>
      <td style='text-align: left;'>Set this to <code>true</code> to have the destination dataset published before the Port Job completes; only relevant if the destination set is being created by either choosing <code>copy_all</code> or <code>copy_schema</code> for the <code>portMethod</code>. If <code>false</code>, the destination dataset will be left as a working copy. (<code>false</code> is the default value)</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-ppm *</code></td>
      <td style='text-align: left;'><code>--portPublishMethod</code></td>
      <td style='text-align: left;'>replace</td>
      <td style='text-align: left;'>Specifies the publish method to use (<code>replace</code> or <code>upsert</code>). For details on the publishing methods refer to Step 3 of the <a href='http://socrata.github.io/datasync/guides/setup-port-job.html'>Setup a Port Job (GUI)</a></td>
    </tr>
  </tbody>
</table>

DataSync can also be run using a global configuration stored in memory as part of a LoadPreferences job.  To run a port job that uses global configuration simply omit the `-c config.json` flag.

### Step 4: Job output

Information about the status of the job will be output to STDOUT. If the job runs successfully a ‘Success’ message will be output to STDOUT, the destination dataset id will be printed out and the program will exit with a normal status code (0). If there was a problem running the job a detailed error message will be output to STDERR and the program will exit with an error status code (1). You can capture the exit code to configure error handling logic within your ETL process.

### Complete example job

```
java -jar datasync.jar -c config.json -t PortJob -pm copy_schema -pd1 https://data.cityofchicago.org -pi1 97wa-y6ff -pd2 https://data.cityofchicago.org -pdt ‘Port Job Test Title’ -pp true
```

config.json contents:
```json
{
    "domain": "https://data.cityofchicago.org",
    "username": "john.smith@cityofchicago.org",
    "password": "secret_password",
    "appToken": "fPsJQRDYN9KqZOgEZWyjoa1SG",
}
```

**Running a previously saved job file (.spj file)**

Simply run:

```
java -jar datasync.jar <.spj FILE TO RUN>
```

For example:

```
java -jar datasync.jar /Users/john/Desktop/business_licenses.spj
```

**NOTE:** you can also create an .spj file directly (rather than saving a job using the DataSync UI) which stores the job details in JSON format. Here is an example:

```json
{
  "portMethod": "copy_all",
  "sourceSiteDomain": "https://louis.demo.socrata.com",
  "sourceSetID": "w8e5-buaa",
  "sinkSiteDomain": "https://louis.demo.socrata.com",
  "sinkSetID": "",
  "publishMethod": "upsert",
  "publishDataset": "publish",
  "portResult": "",
  "jobFilename": "job_saved_v0.3.spj",
  "fileVersionUID": 1,
  "pathToSavedJobFile": "/home/louis/Socrata/Github/datasync/src/test/resources/job_saved_v0.3.spj"
}
```
