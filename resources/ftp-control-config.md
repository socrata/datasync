---
layout: with-sidebar
title: FTP / Control File Configuration
bodyclass: homepage
---

### Contents
- Setting up FTP Control file
    - Header row / column list
    - Date/time formatting
    -[Location column and geocoding configuration](#location_col_geocoding)
    - Other options
- Checking the logs and downloading CSV "snapshots" 

<div class="well">
<strong>NOTICE: this guide only pertains to using the 'replace via FTP' method available in DataSync version 1.0</strong>
</div>

### Setting up FTP Control file

The control file is a JSON-formatted file that is used to configure a Standard DataSync job that uses the 'replace via FTP' method. Control files are specific to the dataset you are updating.

An example of a typical control file:
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

This guide will describe how to use the different options within the control file.


#### Header row/column list

The `columns` and `skip` options enable configuration of how the columns within the CSV/TSV aligns with those of the dataset.

`columns`: List of column names in the following format `["col_id1","col_id2",..]` (double quotes are optional). If it’s `null` then the first line of the CSV/TSV after any skipped records is used. If specified, it must be an array of strings, and must not contain nulls.   
**IMPORTANT NOTE:** the column names, whether provided in “columns” or in the first row of the CSV/TSV, must be column identifiers (API field names), not the display name of the columns. 

`skip`: Specifies the number of rows to skip before reaching the header. 

**Common combinations of `columns` and `skip`:**

If the first line of the CSV/TSV is the list of column identifiers:
```
"columns": null,
"skip": 0,
```

If the first line of the CSV is the columns incorrectly formatted, for example with human-readable names instead of column identifiers, for example:  
```
"columns": ["first_name","last_name","age"], 
"skip": 1,
```

If the first line of the CSV/TSV is data (there is no header row), for example you would use:  
```
"columns": ["first_name","last_name","age"], 
"skip": 0,
```

### Date/time formatting

#### Timestamp Format Options

The `floatingTimestampFormat` and `fixedTimestampFormat` options specify how date/time data is formatted in the CSV/TSV file. `floatingTimestampFormat` applies to ("Date & Time" datatype columns) and `fixedTimestampFormat` functions in the same way but applies to Fixed Timestamps ("Date & Time (with timezone)" datatype columns). If the format does not specify a time zone, the zone named by the `timezone` option.

Both `floatingTimestampFormat` and `fixedTimestampFormat` accept a string (e.g. "ISO8601") or a JSON-formatted list of formats including "ISO8601" and any date/time "Joda time" format-string. Joda time syntax is documented in detail here: [http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html](http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html)

Example syntax to accept three of the most common date/time formats:
```
"fixedTimestampFormat" : ["ISO8601","MM/dd/yy","MM/dd/yyyy","dd-MMM-yyyy"],
"floatingTimestampFormat" : ["ISO8601","MM/dd/yy","MM/dd/yyyy","dd-MMM-yyyy"],
```

This would accept any of the following example date/time data values: "2014-04-22", "2014-04-22T05:44:38", "04/22/2014", "4/22/2014", "4/22/14", and "22-Apr-2014".

If you want to allow a date with or without a time value (e.g. both "2014-04-22" and "2014-04-22 9:30:00"), you would use:
```
"fixedTimestampFormat" : ["yyyy-MM-dd", "yyyy-MM-dd hh:mm:ss"],
"floatingTimestampFormat" : ["yyyy-MM-dd", "yyyy-MM-dd hh:mm:ss"],
```

#### Timezone option 
`timezone` specifies the timezones for FixedTimestamps ("Date & Time (with timezone)" columns). This only has an effect if the timestamp format does not specify a time zone.

You can set this to one of the following:
1. "UTC"  
2. An offset (ex "-0800")  
3. A timezone name (e.g. "US/Pacific").  The list of accepted names is in timezones.txt in the root directory of the FTP server (instructions for logging into the FTP server is in the section below "Checking the logs and downloading CSV 'snapshots'").

<a name="location_col_geocoding"></a>
### Location column and geocoding configuration 

The the `syntheticLocations` option allows configuring a Location datatype column to "pull" or populate from address, city, state, zipcode or latitude/longitude data within existing columns of the CSV/TSV. 

For example: 
```
 "syntheticLocations" : {
   "location_col_id" : {
     "address" : "address_col_id",
     "city" : "city_col_id",
     "state" : "state_col_id",
     "zip" : "zipcode_col_id",
     "latitude" : "lat_col_id",
     "longitude" : "lng_col_id"
   }
 }
```

All of the following are optional "address", "city", "state", "zip", "latitude", and "longitude" are optional. 
Those that are are not provided are not filled in on the generated location.  The values are field names of columns
that must exist in the CSV.

<div class="well">
If you are using Socrata's geocoding you must set the `useSocrataGeocoding` option to `true`. If you are providing 
latitude and longitude data directly (rather than using gecoding) you should set the `useSocrataGeocoding` option to `false`.
</div>

### Other options

TEMP DOCUMENTATION:
[https://docs.google.com/a/socrata.com/document/d/1ddB0pvxEo6pylLtECW2XE9mYYzaW8hA7qlzPgSOQ0wg/edit#heading=h.m3u3dqp3qac3](https://docs.google.com/a/socrata.com/document/d/1ddB0pvxEo6pylLtECW2XE9mYYzaW8hA7qlzPgSOQ0wg/edit#heading=h.m3u3dqp3qac3)

Comming soon!

| Option name   | Explanation                    | Example value |
| ------------- | ------------------------------ | ------------- |
|               |                                |               |



### Checking the logs and downloading CSV "snapshots" 

#### Connecting to the FTP server
You can use [Filezilla](https://filezilla-project.org/) or any other FTP client that supports FTPS to connect to the FTP server.

In Filezilla go to `File -> Site Manager`

Set up a new connection with the following details: 

**Host:** production.ftp.socrata.net  
**Port:** 22222  
**Protocol:** FTP  
**Encryption:** Require explicitly FTP over TLS  
**User:** `<Your Socrata username>`  
**Password:** `<Your Socrata password>`  

Ensure the transfer mode is 'Passive' by going to:
`Transfer Settings -> Transfer mode : Passive.`

Save the connection and press "Connect"

If you only have permission to one domain, you will be dropped into the directory for that domain. You should see the directories named with the dataset ID (e.g. b2fd-cjk2) of any dataset you have updated using DataSync replace via FTP. If you have permission to multiple domains, you will see them as subdirectories. 

#### Checking the logs and downloading CSV "snapshots"

Inside each dataset identifier directory there should be the following files/directories:
- active-control.json
- log.txt
- status.txt
- completed (a directory with subdirectories for each day starting on 1/1/2013)

You can download log.txt to see the logging information for the given dataset. Within the ‘completed’ directory you can find CSVs/TSVs and control.json files archived by date (there are nested folders for year, month, and day). After each successful update operation using DataSync replace via FTP, the CSV/TSV and control.json files that were used to perform the update are archived. Archived files will be stored for the most recent 10 successful update operations (or possibly more). Contact Socrata support if you would like additonal information about archiving.

