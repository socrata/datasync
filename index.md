---
layout: with-sidebar
title: Getting Started with Socrata DataSync
bodyclass: homepage
---

DataSync is an executable Java application which serves as a general solution to automate publishing data on the Socrata platform. It can be used through a easy-to-use graphical interface or as a command-line tool ('headless mode'). Whether you are a non-technical user, developer, or ETL specialist DataSync makes data publishing simple and reliable. DataSync takes a CSV or TSV file on a local machine or networked hard drive and publishes it to a Socrata dataset so that the Socrata dataset stays up-to-date. DataSync jobs can be integrated into an ETL process, scheduled using a tool such as the Windows Task Scheduler or Cron, or used to perform updates or create new datasets in batches. DataSync works on any platform that runs Java version 1.7 or higher (i.e. Windows, Mac, and Linux). This simple, yet powerful publishing tool lets you easily update Socrata datasets programmatically and automatically (scheduled), without writing a single line of code.

<!--
insert screenshot
-->

## Download DataSync

Download the latest release of the DataSync .jar file from here (requires Java 1.7 or higher):
[https://github.com/socrata/datasync/releases](https://github.com/socrata/datasync/releases)

Save the JAR file you downloaded in a permanent location on your computer or server. Double-click the JAR file you just downloaded to open the program in GUI mode.

**NOTE:** DataSync is under active development and new versions are released regularly. You will be notified when you open DataSync when new versions are available. If you have feature suggestions or you find bugs in DataSync please submit them to the [GitHub issue tracker](https://github.com/socrata/datasync/issues)

DataSync source code is available [on GitHub](https://github.com/socrata/datasync)

## Prerequisites

- A computer/server running Java 1.7 (or higher). If you do not have the the JRE or JDK version 1.7 you can [download it here](http://www.oracle.com/technetwork/java/javase/downloads/jre7-downloads-1880261.html).
- A Socrata account with a Publisher role or Owner rights to at least one dataset.
- An App Token. If you have not yet created one read [how to obtain an App token](http://beta.dev.socrata.com/docs/app-tokens.html)
- **IMPORTANT:** To take advantage of the most up-to-date and efficient publishing method (replace via FTP), if you are running DataSync behind a firewall it must be configured to allow FTP traffic through ports 22222 (for the control connection) and all ports within the range of 3131 to 3141 (for data connection). 


[Continue to guide for setting up a standard job]({{ site.root }}/guides/setup-standard-job.html)

