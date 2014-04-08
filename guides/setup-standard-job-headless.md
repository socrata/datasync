For information on using DataSync in GUI (Graphical User Interface) mode refer to:
LINK

DataSync jobs can be run in headless/command-line mode in one of two ways: (1) passing job parameters as command-line arguments/flags or (2) running an .sij file that was saved using the user interface which contains the job parameters. This guide focuses on (1) which enables configuring and running a DataSync job without any usage of the GUI. This enables complete control to integrate DataSync into ETL code or software systems. It is recommended that you first familiarize yourself with DataSync by using the GUI because it is often easier to start there and then move to using the tool headlessly.

### Step 1: Establish “global” configuration (e.g. authentication details)
The “global” configuration settings (domain, username, password, logging dataset ID, etc) apply to all DataSync jobs (i.e. they are not specific to a single job). 

Create a file called config.json with the contents below: 
```json
{
    "domain": "<YOUR DOMAIN>",
    "username": "<YOUR USERNAME>",
    "password": "<YOUR PASSWORD>",
    "appToken": "<YOUR APP TOKEN>",
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

You must fill in at least the following:  
`<YOUR DOMAIN>` (e.g. https://data.cityofchicago.org)  
`<YOUR USERNAME>` (e.g. john@cityofchicago.org)  
`<YOUR PASSWORD>` (e.g. secret_password)  
`<YOUR APP TOKEN>` (e.g. fPsJQRDYN9KqZOgEZWyjoa1SG)

`<YOUR DOMAIN>` is the root domain of your data site and must begin with https:// (e.g. https://data.cityofchicago.org). The username and password are those of a Socrata account that has a Publisher role or Owner rights to at least one dataset. Enter your App token or if you have not yet created one read [how to obtain an App token](http://beta.dev.socrata.com/docs/app-tokens.html). We recommend creating a dedicated Socrata account (with a Publisher role or Owner permissions to specific datasets) to use with DataSync rather than tie DataSync to a particular person’s primary account.


******** For details on the other global configuration settings refer to: <LINK TO PREFS DOC>

There are two ways to establish the “global” DataSync configuration:

**1) Load configuration from a .json file when running each job**  
This method of loading configuration requires supplying a flag pointing DataSync to config.json each time you run a job in headless/command-line mode. For example, you would run (`<OTHER FLAGS>` is where the other flags discussed in Step 5 are passed):

`java -jar datasync.jar -c config.json <OTHER FLAGS> ...`

**2) Load configuration into the DataSync “memory”**  
If you load configuration this way you only need to load the configuration once and DataSync will remember the configuration (instead of passing config.json as a flag with every job). After loading configuration settings they will be saved and used to connect to the publisher API for every job you run using DataSync. To load configuration into DataSync “memory” run this command once:

`java -jar datasync.jar -t LoadPreferences -c config.json`

**NOTICE:** DataSync stores the authentication details unencrypted in the Registry on Windows platforms (in the following location: HKEY_CURRENT_USER\Software\JavaSoft\Prefs) and in analogous locations on Mac and Linux. If you are concerned about this as a potential security issue you may want to look into alternative publishing methods. Please contact support if you have questions.


### Step 2: Obtain the Dataset ID
You will need the dataset ID of the dataset you wish to publish to. To obtain the dataset ID navigate to the dataset in your web browser and in the address bar the dataset ID is the code at the end of the URL in the form (xxxx-xxxx). For example for the following URL to a dataset:
https://data.seattle.gov/Public-Safety/Fire-911/m985-ywaw
The dataset ID is: m985-ywaw


### Step 3: Configure job details
For general help using DataSync in headless/command-line mode run:

`java -jar datasync.jar --help`

To run a job that uses the settings in config.json as the global configuration run the following command, replacing `<..>` with the appropriate values (flags explained below): 

`java -jar datasync.jar -c config.json -t IntegrationJob -f <FILE TO PUBLISH> -h <HAS HEADER ROW> -i <DATASET ID> -m <PUBLISH METHOD> -pf <PUBLISH VIA FTP> -sc <FTP CONTROL FILE>`

To run a job that uses global configuration previously saved in DataSync “memory” (either via a LoadPreferences job or using the DataSync GUI) simply omit the `-c config.json` flag.

Explanation of flags:
`*` = required flag

| Flag - Short Name  | Flag - Long Name  | Example Values | Description |
| ------------------ |:----------------- |:-------------- |:-----------|
| -c                 | --config          | /Users/home/config.json    | Points to the config.json file you created in Step 3 or if not supplied configuration inDataSync 'memory' is used |
| -t                 | --jobType         | IntegrationJob | Specifies that a standard IntegrationJob should be run (‘IntegrationJob’ is the default so in this case this flag is optional) |
| -f `*`              | --fileToPublish   | /Users/home/data_file.csv | CSV or TSV file to publish (e.g. ‘/Users/home/data_file.csv’) | 
| -h `*`              | --fileToPublishHasHeaderRow | true | Set this to `true` if the file to publish has a header row, otherwise set it to `false` (`true` and `false` are the only acceptable values) | 
| -i `*`              | --datasetID      | m985-ywaw | The identifier of the dataset to publish to obtained in Step 2|
| -m `*`              | --publishMethod | replace | Specifies the publish method to use (`replace`, `upsert`, `append`, and `delete` are the only acceptable values, for details on the publishing methods refer to Step 3 of the [guide to setup a standard job (GUI)]({{ site.root }}/guides/setup-standard-job.html)
|-pf                  | --publishViaFTP | true | Set this to `true` to use FTP (only works for `replace`), which is the preferred option because is highly efficient and can reliably handle very large files (1 million+ rows). If `false`perform the dataset update using HTTPS (`false` is the default value) |
| -sc                 | --pathToFTPControlFile | /Users/home/control.json | Specifies a Control file that configures 'replace via FTP' jobs, and therefore should only be set if -pf,--publishViaFTP is set to `true`. When this flag is set the *-h,--fileToPublishHasHeaderRow* and *-m,--publishMethod* flags are overridden by the settings in the supplied Control.json file. | 


**'Replace via FTP' Configuration (via the Control file)**  
Currently to use SmartUpdate you must supply a control.json file with the *-sc,----pathToFTPControlFile* flag that contains configuration specific to the dataset you are updating. Create a file called control.json according to the Control.json section in this document:

TEMP LINK TO GOOGLE DOC:
[https://docs.google.com/a/socrata.com/document/d/1ddB0pvxEo6pylLtECW2XE9mYYzaW8hA7qlzPgSOQ0wg](https://docs.google.com/a/socrata.com/document/d/1ddB0pvxEo6pylLtECW2XE9mYYzaW8hA7qlzPgSOQ0wg)

<div class="well">
NOTE: the GUI enables generating the Control file with settings appropriate for the dataset you are publishing to. It may be easiest to use the GUI to generate the default Control file content and then make any necessary modifications before saving the file and including it with -sc,--pathToFTPControlFile flag. 
</div>

Here are the contents of an example control.json file configured to do a 'replace via FTP' operation from a CSV file that has a header row containing the column identifiers (API field names) of the columns and dates in ISO8601 format (e.g. 2014-03-25):
```json
{
  "action" : "Replace", 
  "csv" :
    {
      "fixedTimestampFormat" : "ISO8601",
      "separator" : ",",
      "timezone" : "UTC",
      "encoding" : "utf-8",
      "overrides" : {},
      "quote" : "\"",
      "emptyTextIsNull" : true,
      "columns" : null,
      "skip" : 0,
      "floatingTimestampFormat" : "ISO8601",
      "trimWhitespace" : true,
      "trimServerWhitespace" : true
    }
}
```
If the file you are publishing is a TSV file, simply change the 3rd line above from `"csv" :` to `"tsv" :`.

### Step 6: Running a job

Execute the `java -jar  datasync.jar ...` command and logging information will be output to STDOUT. If the job runs successfully a ‘Success’ message will be output to STDOUT and the program will exit with a normal status code (0). If there was a problem running the job a detailed error message will be output to STDERR and the program will exit with an error status code (1). You can capture the exit code to configure error handling logic within your ETL process. 

### Complete example job

```
java -jar datasync.jar -c config.json -t IntegrationJob -m replace -i 7tgi-grrk -f business_licenses_2014-02-10.csv -h true -pf true -sc control.json
```

config.json contents:
```json
{
    "domain": "https://data.cityofchicago.org",
    "username": "john.smith@cityofchicago.org",
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
      "fixedTimestampFormat" : "ISO8601",
      "separator" : ",",
      "timezone" : "UTC",
      "encoding" : "utf-8",
      "overrides" : {},
      "quote" : "\"",
      "emptyTextIsNull" : true,
      "columns" : null,
      "skip" : 0,
      "floatingTimestampFormat" : "ISO8601",
      "trimWhitespace" : true
    }
}
```

**Running a previously saved job file (.sij file)**

Simply run: 

`java -jar datasync.jar <.sij FILE TO RUN>`

For example:

`java -jar datasync.jar /Users/john/Desktop/business_licenses.sij`

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