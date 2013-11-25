Socrata Datasync
================

Last updated: November 14, 2013

Authors: [Adrian Laurenzi](http://www.github.com/alaurenz) & [Louis Fettet](http://www.github.com/LouisFettet)

Looking for the latest release? Get it here: https://github.com/socrata/datasync/releases

## General Information
DataSync is a lightweight desktop client that makes automated publishing of data to Socrata datasets easy. The simple, clean, graphical interface (GUI) can be used to configure various jobs to publish your data. DataSync runs on any platform with Java (1.6) installed, so it will work on Windows, Mac, and Linux.

Guide on how to use Datasync: [Setting Up a Basic DataSync Job](http://support.socrata.com/entries/24241271-Setting-up-a-basic-DataSync-job)

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
mvn clean compile assembly:single
```

Did it work? If not, you can always skip the tests:
```
mvn clean compile -Dmaven.test.skip=true assembly:single
```

This puts the JAR file into the "target" directory inside the repo.  So to open DataSync, simply:
```
cd target
java -jar DataSync-0.2-SNAPSHOT-jar-with-dependencies.jar
```
