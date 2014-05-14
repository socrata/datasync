---
layout: with-sidebar
title: Scheduling a Job
bodyclass: homepage
---

### Using the Windows Task Scheduler

Refer to this guide:
[http://support.socrata.com/entries/24234461-Scheduling-a-DataSync-job-using-Windows-Task-Scheduler](http://support.socrata.com/entries/24234461-Scheduling-a-DataSync-job-using-Windows-Task-Scheduler)


### Using Crontab (Mac or Linux)
Here is a good introduction to using Linux Crontab (Crontab is included with essentially any Linux distro):  
[http://kvz.io/blog/2007/07/29/schedule-tasks-on-linux-using-crontab/](http://kvz.io/blog/2007/07/29/schedule-tasks-on-linux-using-crontab/)

To schedule a DataSync job simply copy the text within the 'Command to execute with scheduler' field (after you save or open a job in DataSync) and use that command in place of the `/bin/execute/this/script.sh` in the above guide. For example, to schedule a job to run daily at midnight your Crontab content might look like:
```
0 0 * * * java -jar datasync.jar /path/to/myjob.sij
```

The above guide also applies to Mac OSX but you can find a more Mac-specific guide here:  
[http://www.maclife.com/article/columns/terminal_101_creating_cron_jobs](http://www.maclife.com/article/columns/terminal_101_creating_cron_jobs)