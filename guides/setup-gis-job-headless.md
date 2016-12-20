---
layout: with-sidebar
title: Setup a GIS Job (Headlessly)
bodyclass: homepage
---

GIS jobs are used to upload geospatial data (zipped shapefiles, geoJSON, KML, or KMZ files) to replace specific geospatial datasets on the Socrata platform. This guide shows how to setup and run a GIS Job using the command line interface.

### Step 1: Setup your configuration
Information about your domain, username, password and app token is required for all DataSync jobs.  Note that the user running the job must have publisher rights on the dataset. A number of other global settings, such as logging and emailing preferences can also be configured.  Please refer to the [configuration guide]({{ site.baseurl }}/resources/preferences-config.html) to establish your credentials and preferences.

### Step 2: Configure job details
For general help using DataSync in headless/command-line mode run:

    java -jar <DATASYNC_JAR> --help


To run a job execute the following command, replacing `<..>` with the appropriate values (flags explained below):

    java -jar <DATASYNC_JAR> -c <CONFIG FILE> -t GISJob -f <FILE TO PUBLISH> -i <DATASET ID> -m replace


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
      <td style='text-align: left;'>-t <code>*</code></td>
      <td style='text-align: left;'>--jobType</td>
      <td style='text-align: left;'>GISJob</td>
      <td style='text-align: left;'>Specifies the type of job to run.</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-c</td>
      <td style='text-align: left;'>--config</td>
      <td style='text-align: left;'>/Users/home/config.json</td>
      <td style='text-align: left;'>Points to the config.json file you created in Step 1, if you chose to do so.</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-f</td>
      <td style='text-align: left;'>--fileToPublish</td>
      <td style='text-align: left;'>/Users/home/datafile.geosjson</td>
      <td style='text-align: left;'>Zipped shapefile, geoJSON, KML, or KMZ file to publish</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-i</td>
      <td style='text-align: left;'>--datasetID</td>
      <td style='text-align: left;'>m985-ywaw</td>
      <td style='text-align: left;'>The <a href='http://socrata.github.io/datasync/resources/fac-common-problems.html#what-is-the-id-of-my-dataset'>dataset identifier</a> to publish to.</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-m</td>
      <td style='text-align: left;'>--publishMethod</td>
      <td style='text-align: left;'>replace</td>
      <td style='text-align: left;'>Specifies the publish method to use. <code>replace</code> is the only valid method for GIS jobs.</td>
    </tr>
  </tbody>
</table>


### Step 3: Job output

Information about the status of the job will be output to STDOUT. If the job runs successfully a ‘Success’ message will be output to STDOUT, the destination dataset id will be printed out and the program will exit with a normal status code (0). If there was a problem running the job a detailed error message will be output to STDERR and the program will exit with an error status code (1). You can capture the exit code to configure error handling logic within your ETL process.

### Complete example job

    java -jar <DATASYNC_JAR> --config config.json --jobType GISJob --publishMethod replace --datasetID 23rd-dssw --fileToPublish "/Users/snu/Desktop/evergreen_parcel_data.zip"

config.json contents:

    {
        "domain": "https://opendata.socrata.com",
        "username": "snu@socrata.com",
        "password": "secret_password",
        "appToken": "fPsJQRDYN9KqZOgEZWyjoa1SG",
    }


**Running a previously saved job file (.gij file)**

Simply run:

    java -jar <DATASYNC_JAR> <.spj FILE TO RUN>

For example:

    java -jar <DATASYNC_JAR> /Users/snu/Desktop/parcels.gij


**NOTE:** you can also create an .gij file directly (rather than saving a job using the DataSync UI) which stores the job details in JSON format. Here is an example:

    {
      "defaultJobName": "City of Evergreen Parcel Data",
      "datasetID": "23rd-dssw",
      "fileToPublish": "/Users/snu/Desktop/evergreen_parcel_data.zip",
      "publishMethod": "replace",
      "layerMap": {
        "parcels": "gvyj-jpky"
      },
      "fileVersionUID": 4,
      "pathToSavedFile": "/Users/snu/Desktop/parcel_data.gij",
      "jobFilename": "parcel_data.gij"
    }
