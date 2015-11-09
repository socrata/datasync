---
layout: with-sidebar
title: Setup a standard job (headless)
bodyclass: homepage
---

*NOTICE*: The guide below only pertains to DataSync versions 1.0 and higher.

*NOTICE*: Before using DataSync in headless mode, we recommend familiarizing yourself with DataSync through the UI.  For information on using DataSync's UI please see [guide to setup a standard job (GUI)]({{ site.root }}/guides/setup-standard-job.html)


DataSync's command line interface, or "headless mode," enables easy integration of DataSync into ETL code or other software systems.  DataSync jobs can be run from the command line in one of two ways: (1) passing job parameters as command-line arguments/flags or (2) running an .sij file that was previously saved using the user interface. This guide focuses on (1).

### Step 1: Establish your configuration (e.g. authentication details)
Information about your domain, username, password and app token is required for all DataSync jobs.  Note that the user running the job must have publisher rights on the dataset. A number of other global settings, such as logging and emailing preferences can also be configured.  Please refer to the [configuration guide]({{ site.root }}/resources/preferences-config.html) to establish your credentials and preferences.

### Step 2: Configure job details
For general help using DataSync in headless/command-line mode run:

    java -jar <DATASYNC_JAR> --help

To run a job execute the following command, replacing <..> with the appropriate values (flags explained below):

    java -jar <DATASYNC_JAR> -c <CONFIG.json FILE> -f <FILE TO PUBLISH> -h <HAS HEADER ROW> -i <DATASET ID> -m <PUBLISH METHOD> -pf <PUBLISH VIA FTP> -ph <PUBLISH VIA HTTP> -cf <FTP CONTROL.json FILE>


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
      <td style='text-align: left;'>-c</td>
      <td style='text-align: left;'>--config</td>
      <td style='text-align: left;'>/Users/home/config.json</td>
      <td style='text-align: left;'>Points to the config.json file you created in Step 1</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-f <code>*</code></td>
      <td style='text-align: left;'>--fileToPublish</td>
      <td style='text-align: left;'>/Users/home/data_file.csv</td>
      <td style='text-align: left;'>CSV or TSV file to publish</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-h</td>
      <td style='text-align: left;'>--fileToPublishHasHeaderRow</td>
      <td style='text-align: left;'>true</td>
      <td style='text-align: left;'>Set this to <code>true</code> if the file to publish has a header row, otherwise set it to <code>false</code></td>
    </tr>
    <tr>
      <td style='text-align: left;'>-i <code>*</code></td>
      <td style='text-align: left;'>--datasetID</td>
      <td style='text-align: left;'>m985-ywaw</td>
      <td style='text-align: left;'>The <a href='http://socrata.github.io/datasync/resources/fac-common-problems.html#what-is-the-id-of-my-dataset'>dataset identifier</a> to publish to.</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-m</td>
      <td style='text-align: left;'>--publishMethod</td>
      <td style='text-align: left;'>replace</td>
      <td style='text-align: left;'>Specifies the publish method to use (<code>replace</code>, <code>upsert</code>, <code>append</code>, and <code>delete</code> are the only acceptable values, for details on the publishing methods refer to Step 3 of the <a href='http://socrata.github.io/datasync/guides/setup-standard-job.html'>Setup a Standard Job (GUI)</a></td>
    </tr>
    <tr>
      <td style='text-align: left;'>-ph</td>
      <td style='text-align: left;'>--publishViaHttp</td>
      <td style='text-align: left;'>true</td>
      <td style='text-align: left;'>Set this to <code>true</code> to use HTTP (rather than FTP or Soda2); This is the preferred method because is highly efficient and can reliably handle very large files (1 million+ rows). If <code>false</code> and --publishViaFTP is <code>false</code>, perform the dataset update using Soda2. (false is the default value)</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-pf</td>
      <td style='text-align: left;'>--publishViaFTP</td>
      <td style='text-align: left;'>true</td>
      <td style='text-align: left;'>Set this to <code>true</code> to use FTP (currently only works for replace). If <code>false</code> and --publishViaHttp is <code>false</code>,perform the dataset update using Soda2. (false is the default value)</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-cf</td>
      <td style='text-align: left;'>--pathToControlFile</td>
      <td style='text-align: left;'>/Users/home/control.json</td>
      <td style='text-align: left;'>Specifies a <a href='http://socrata.github.io/datasync/resources/control-config.html'>control file></a> that configures HTTP and &#8216;replace via FTP&#8217; jobs.  Only required when --publishViaHttp or --publishViaFTP is set to <code>true</code>. When this flag is set the --fileToPublishHasHeaderRow and --publishMethod flags are overridden by the settings in the supplied control file.</td>
    </tr>
    <tr>
      <td style='text-align: left;'>-t <code>*</code></td>
      <td style='text-align: left;'>--jobType</td>
      <td style='text-align: left;'>LoadPreferences</td>
      <td style='text-align: left;'>Specifies the type of job to run (<code>IntegrationJob</code>, <code>LoadPreferences</code> and <code>PortJob</code> are the only acceptable values)</td>
    </tr>
  </tbody>
</table>

### Step 3: Job Output
Information about the status of the job will be output to STDOUT. If the job runs successfully a ‘Success’ message will be output to STDOUT and the job will exit with a normal status code (0). If there was a problem running the job a detailed error message will be output to STDERR and the program will exit with an error status code (1). You can capture the exit code to configure error handling logic within your ETL process.

### Complete example job

    java -jar <DATASYNC_JAR> -c config.json -f business_licenses_2014-02-10.csv -h true -i 7tgi-grrk -m replace -pf true -sc control.json

config.json contents:

    {
        "domain": "https://opendata.socrata.com",
        "username": "publisher@opendata.socrata.com",
        "password": "secret_password",
        "appToken": "fPsJQRDYN9KqZOgEZWyjoa1SG",
        "adminEmail": "",
        "emailUponError": "false",
        "logDatasetID": "",
        "outgoingMailServer": "",
        "smtpPort": "",
        "sslPort": "",
        "smtpUsername": "",
        "smtpPassword": ""
    }


control.json contents:

    {
      "action" : "Replace",
      "csv" :
        {
          "useSocrataGeocoding" : true,
          "columns" : null,
          "skip" : 0,
          "fixedTimestampFormat" : ["ISO8601","MM/dd/yy","MM/dd/yyyy"],
          "floatingTimestampFormat" : ["ISO8601","MM/dd/yy","MM/dd/yyyy"],
          "timezone" : "UTC",
          "separator" : ",",
          "quote" : "\"",
          "encoding" : "utf-8",
          "emptyTextIsNull" : true,
          "trimWhitespace" : true,
          "trimServerWhitespace" : true,
          "overrides" : {}
        }
    }


**Running a previously saved job file (.sij file)**

Simply run:

    java -jar <DATASYNC_JAR> <.sij FILE TO RUN>


For example:

    java -jar D<DATASYNC_JAR> /Users/john/Desktop/business_licenses.sij

**NOTE:** you can also create an .sij file directly (rather than saving a job using the DataSync UI) which stores the job details in JSON format. Here is an example:

    {
        "datasetID" : "2bw7-dr67",
        "fileToPublish" : "/Users/john/Desktop/building_permits_2014-12-05.csv",
        "publishMethod" : "replace",
        "fileToPublishHasHeaderRow" : true,
        “publishViaFTP” : true,
        “pathToFTPControlFile” : “/Users/john/Desktop/building_permits_control.json”
    }
