Socrata Datasync
================

Last updated: April 23, 2014

Authors: [Adrian Laurenzi](http://www.github.com/alaurenz) & [Louis Fettet](http://www.github.com/LouisFettet)

Looking for the latest release? Get it here: https://github.com/socrata/datasync/releases

## General Information
DataSync is an executable Java application which serves as a general solution to automate publishing data on the
Socrata platform. It can be used through a easy-to-use graphical interface or as a command-line tool ('headless mode').
Whether you are a non-technical user, developer, or ETL specialist DataSync makes data publishing simple and reliable.
DataSync takes a CSV or TSV file on a local machine or networked hard drive and publishes it to a Socrata dataset so
that the Socrata dataset stays up-to-date. DataSync jobs can be integrated into an ETL process, scheduled using a tool
such as the Windows Task Scheduler or Cron, or used to perform updates or create new datasets in batches. DataSync
works on any platform that runs Java version 1.7 or higher (i.e. Windows, Mac, and Linux). This simple, yet powerful
publishing tool lets you easily update Socrata datasets programmatically and automatically (scheduled), without
writing a single line of code.

[Comprehensive DataSync Documentation](http://socrata.github.io/datasync/)

The Socrata University Class: [Socrata Introduction to Integration](http://socrata.wistia.com/medias/q4pwut6s56)

### Standard Jobs
Standard jobs can be set up to take a CSV data file from a local machine or networked folder and publish it to a specific dataset. A job can be automated easily using the Windows Task Scheduler or similar tool to run the job at specified intervals (i.e. once per day).
![standard job tab](http://i.imgur.com/byN0ibq.png?1)

### Port Jobs
Port jobs are used for moving data around that is already on the Socrata platform. Users that have publisher rights can make copies of datasets through this tool. Port jobs allow the copying of both dataset schemas (metadata and columns) and data (rows).
![port job tab](http://i.imgur.com/tMz2sQP.png?1)


## Developers
This repository is our development basecamp. If you find a bug or have questions, comments, or suggestions, you can contribute to our [issue tracker](https://github.com/socrata/datasync/issues).

### Apache Maven
DataSync uses Maven for building and package management. For more information: [What is Maven?](http://maven.apache.org/what-is-maven.html)

To build the project run:
```
mvn clean install
```

To compile the project into an executable JAR file (including all dependencies) run:
```
mvn clean compile -Dmaven.test.skip=true assembly:single
```

This puts the JAR file into the "target" directory inside the repo.  So to open DataSync, simply:
```
cd target
java -jar DataSync-1.7.1-jar-with-dependencies.jar
```

### Java SDK

DataSync can be used as a Java SDK, for detailed documentation refer to:
[http://socrata.github.io/datasync/guides/datasync-library-sdk.html](http://socrata.github.io/datasync/guides/datasync-library-sdk.html)
