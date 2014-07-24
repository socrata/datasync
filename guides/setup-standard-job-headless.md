---
layout: with-sidebar
title: Setup a standard job (headless)
bodyclass: homepage
---

NOTICE: Before using DataSync in headless mode, we recommend familiarizing yourself with DataSync through the UI.  For information on using DataSync's UI please see [guide to setup a standard job (GUI)]({{ site.root }}/guides/setup-standard-job.html)

<div class="well">
<strong>NOTICE: The guide below only pertains to DataSync versions 1.0 and higher</strong>
</div>

DataSync's command line interface, or "headless mode," enables easy integration of DataSync into ETL code or other software systems.  DataSync jobs can be run from the command line in one of two ways: (1) passing job parameters as command-line arguments/flags or (2) running an .sij file that was previously saved using the user interface. This guide focuses on (1). 

### Step 1: Establish “global” configuration (e.g. authentication details)
The “global” configuration settings (domain, username, password, logging dataset ID, etc) apply to all DataSync jobs (i.e. they are not specific to a single job).

The configuration settings are stored in a json file, e.g. config.json, within a single object.  The four options in the example below are required:
```json
{
    "domain": "<YOUR DOMAIN>",
    "username": "<YOUR USERNAME>",
    "password": "<YOUR PASSWORD>",
    "appToken": "<YOUR APP TOKEN>",
}
```
The full set of options is:

| Option    | Requirement | Explanation
| ------------- | ------------------------------ | -------------
| domain | required | The scheme and root domain of your data site.  (e.g. https://opendata.socrata.com)
| username | required | Your Socrata username. This user must have a Publisher role or Owner rights to at least one dataset. We recommend creating a dedicated Socrata account (with these permissions) to use with DataSync rather than tie DataSync to a particular person’s primary account. (e.g. publisher@opendata.socrata.com)
| password | required | Your Socrata password. Note that this will be stored in clear-text as part of the file. We recommend taking additional precautions to protect this file, including potentially only adding it when your ETL process runs. 
| appToken | required | An app token.   If do not yet have an app token, please reference [how to obtain an App token](http://dev.socrata.com/docs/app-tokens.html).
| logDatasetID | optional | The dataset indentifier of the log dataset. If you have not provisioned a log dataset and would like to do so, please refer to [Logging documentation]({{ site.root }}/resources/preferences-config.html).
| adminEmail | required only if `emailUponError` is "true" | The email address of the administrator or user that error notifications should be sent to.
| emailUponError | optional | Whether to send email notifications of errors that occurred while running jobs. Defaults to "false".
| outgoingMailServer | required only if `emailUponError` is "true" | The address of your SMTP server
| smtpPort | required only if `emailUponError` is "true" | The port of your SMTP server
| sslPort | required only if `emailUponError` is "true" | If SSL port of your SMTP server
| smtpUsername | required only if `emailUponError` is "true" | Your SMTP username
| smtpPassword | required only if `emailUponError` is "true" | Your SMTP password
| filesizeChunkingCutoffMB | Used only for append, upsert, delete and Soda2-replace jobs | If the CSV/TSV file size is less than this, the entire file will be sent in one chunk.  Defaults to 10 MB.
| numRowsPerChunk | Used only for append, upsert, delete and Soda2-replace jobs | The number of rows to send in each chunk.  If the CSV/TSV file size is less than  `filesizeChunkingCutoffMB`, all rows will be sent in one chunk. Defaults to 10,000 rows.
| proxyHost | required if operating through a proxy | The hostname of the proxy server.
| proxyPort | required if operating through a proxy | The port that the proxy server listens on.
| proxyUsername | optional | The username to use if the proxy is authenticated.  If this information is sensitive, you may instead pass it at runtime via the -pun, --proxyUsername commandline option.
| proxyPassword | optional | The password to use if the proxy is authenticated.  If this information is sensitive, you may instead pass it at runtime via the -ppw, --proxyPassword commandline option.


There are two ways to establish the “global” DataSync configuration:

1. **Load configuration from a .json file when running each job** (RECOMMENDED)
This method of loading configuration requires supplying a flag pointing DataSync to config.json each time you run a job in headless/command-line mode. For example, you would run:

```
java -jar datasync.jar -c config.json <OTHER FLAGS> ...
```

where `<OTHER FLAGS>` are those discussed below

2.**Load configuration into the DataSync "memory"**
DataSync also provides the ability to load your configuration once into memory, at which point it will be used for all future jobs.  To load configuration into DataSync “memory” run this command once:

```
java -jar datasync.jar -t LoadPreferences -c config.json
```

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.


### Step 2: Obtain the Dataset ID
You will need the dataset ID of the dataset you wish to publish to. To obtain the dataset ID navigate to the dataset in your web browser and in the address bar the dataset ID is the code at the end of the URL in the form (xxxx-xxxx). For example for the following URL to a dataset:

https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw

The dataset ID is: m985-ywaw


### Step 3: Configure job details
For general help using DataSync in headless/command-line mode run:

```
java -jar DataSync-1.5-jar-with-dependencies.jar --help
```
To run a job execute the following command, replacing <..> with the appropriate values (flags explained below):
```
java -jar DataSync-1.5-jar-with-dependencies.jar -c <CONFIG.json FILE> -f <FILE TO PUBLISH> -h <HAS HEADER ROW> -i <DATASET ID> -m <PUBLISH METHOD> -pf <PUBLISH VIA FTP> -sc <FTP CONTROL.json FILE>
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
      <td style='text-align: left;'><code>-c</code></td>
      <td style='text-align: left;'><code>--config</code></td>
      <td style='text-align: left;'>/Users/home/config.json</td>
      <td style='text-align: left;'>Points to the config.json file you created in Step 1</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-f *</code></td>
      <td style='text-align: left;'><code>--fileToPublish</code></td>
      <td style='text-align: left;'>/Users/home/data_file.csv</td>
      <td style='text-align: left;'>CSV or TSV file to publish</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-h *</code></td>
      <td style='text-align: left;'><code>--fileToPublishHasHeaderRow</code></td>
      <td style='text-align: left;'>true</td>
      <td style='text-align: left;'>Set this to <code>true</code> if the file to publish has a header row, otherwise set it to <code>false</code></td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-i *</code></td>
      <td style='text-align: left;'><code>--datasetID</code></td>
      <td style='text-align: left;'>m985-ywaw</td>
      <td style='text-align: left;'>The identifier of the dataset to publish to obtained in Step 2</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-m *</code></td>
      <td style='text-align: left;'><code>--publishMethod</code></td>
      <td style='text-align: left;'>replace</td>
      <td style='text-align: left;'>Specifies the publish method to use (<code>replace</code>, <code>upsert</code>, <code>append</code>, and <code>delete</code> are the only acceptable values, for details on the publishing methods refer to Step 3 of the <a href='http://socrata.github.io/datasync/guides/setup-standard-job.html'>Setup a Standard Job (GUI)</a></td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-ph</code></td>
      <td style='text-align: left;'><code>--publishViaHttp</code></td>
      <td style='text-align: left;'>true</td>
      <td style='text-align: left;'>Set this to <code>true</code> to use replace-via-http, which is the preferred update method because is highly efficient and can reliably handle very large files (1 million+ rows). If <code>false</code> and <code>publishViaFTP</code> is <code>false</code>, perform the dataset update using Soda2. (<code>false</code> is the default value)</td>(<code>false</code> is the default value)</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-pf</code></td>
      <td style='text-align: left;'><code>--publishViaFTP</code></td>
      <td style='text-align: left;'>true</td>
      <td style='text-align: left;'>Set this to <code>true</code> to use FTP (currently only works for <code>replace</code>). If <code>false</code> and <code>publishViaHttp</code> is <code>false</code>,perform the dataset update using Soda2. (<code>false</code> is the default value)</td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-cf</code></td>
      <td style='text-align: left;'><code>--pathToControlFile</code></td>
      <td style='text-align: left;'>/Users/home/control.json</td>
      <td style='text-align: left;'>Specifies a Control file that configures &#8216;replace via HTTP&#8217; and &#8216;replace via FTP&#8217; jobs.  Only required when <code>-ph</code>,<code>--publishViaHttp</code> or <code>-pf</code>,<code>--publishViaFTP</code> is set to <code>true</code>. When this flag is set the <em><code>-h</code>,<code>--fileToPublishHasHeaderRow</code></em> and <em><code>-m</code>,<code>--publishMethod</code></em> flags are overridden by the settings in the supplied Control.json file. For more information on creating a control file, please see <a href='http://socrata.github.io/datasync/resources/ftp-control-config.html' Creating your control file></a></td>
    </tr>
    <tr>
      <td style='text-align: left;'><code>-t</code></td>
      <td style='text-align: left;'><code>--jobType</code></td>
      <td style='text-align: left;'>LoadPreferences</td>
      <td style='text-align: left;'>Specifies the type of job to run (default is &#8216;IntegrationJob&#8217;)</td>
    </tr>
  </tbody>
</table>

### Step 4: Job Output
Information about the status of the job will be output to STDOUT. If the job runs successfully a ‘Success’ message will be output to STDOUT.  DataSync will also return with the destination dataset id and exit with a normal status code (0). If there was a problem running the job a detailed error message will be output to STDERR and the program will exit with an error status code (1). You can capture the exit code to configure error handling logic within your ETL process.

### Complete example job

```
java -jar DataSync-1.5-jar-with-dependencies.jar -c config.json -f business_licenses_2014-02-10.csv -h true -i 7tgi-grrk -m replace -pf true -sc control.json
```

config.json contents:
```json
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
    "smtpPassword": "",
    "filesizeChunkingCutoffMB": "10",
    "numRowsPerChunk": "10000"
}
```

control.json contents:
```json
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
```

**Running a previously saved job file (.sij file)**

Simply run:

```
java -jar DataSync-1.5-jar-with-dependencies.jar <.sij FILE TO RUN>
```

For example:

```
java -jar DataSync-1.5-jar-with-dependencies.jar /Users/john/Desktop/business_licenses.sij
```

**NOTE:** you can also create an .sij file directly (rather than saving a job using the DataSync UI) which stores the job details in JSON format. Here is an example:

```json
{
    "datasetID" : "2bw7-dr67",
    "fileToPublish" : "/Users/john/Desktop/building_permits_2014-12-05.csv",
    "publishMethod" : "replace",
    "fileToPublishHasHeaderRow" : true,
    “publishViaFTP” : true,
    “pathToFTPControlFile” : “/Users/john/Desktop/building_permits_control.json”
}
```
