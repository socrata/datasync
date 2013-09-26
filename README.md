Last updated: September 20, 2013

Author: Adrian Laurenzi

General information
--------------------------------------
DataSync is a lightweight desktop client that makes automated publishing of data to 
Socrata datasets simple. The simple, clean, graphical interface (GUI) can be used to 
configure various jobs to publish your data. Each job can be set up to take a CSV data 
file from a local machine or networked folder and publish it to a specific dataset. A job 
can be automated easily using the Windows Task Scheduler or similar tool to run the job 
at specified intervals (i.e. once per day). DataSync will run on any platform with Java 
installed so it will work on Windows, Mac, and Linux.

Here is a guide on how to use DataSync:
http://support.socrata.com/entries/24241271-Setting-up-a-basic-DataSync-job

Using Maven
--------------------------------------

DataSync uses Maven for package management. For more information on Maven go here:
http://maven.apache.org/what-is-maven.html

To compile the project run:
```
mvn clean install
```

To compile the project into an executable JAR file (including all dependencies) run:
```
mvn clean compile assembly:single
```

This puts the JAR file into the "target" directory inside the repo.  So to open DataSync, simply:
```
cd target && java -jar DataSync-0.2-SNAPSHOT-jar-with-dependencies.jar
```
