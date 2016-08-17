---
layout: with-sidebar
title: Scheduling a Job
bodyclass: homepage
---

This guide covers how to schedule a Socrata DataSync job to run automatically at some specified interval (i.e. once per day) using either the Windows Task Scheduler or Crontab.

###**Using the Windows Task Scheduler**

#### Step 1: Save Socrata .sij file

![Save Job](/datasync/images/save_job.png)

After you save or open a job with DataSync the text field called "Command to execute with scheduler" is automatically populated with the command to run the given job. Simply click the "Copy to clipboard" button to copy the command to the clipboard. You can also click on the text field (which will automatically highlight the entire command) and then press Ctrl+C to copy the command to the clipboard. Additionally, you can save the Socrata Integration Job (.sij) file to a directory of your choice and call that from within Task Scheduler. 


#### Step 2: Create a new task using the Windows Task Scheduler

The Windows Task Scheduler typically comes installed on any Windows platform. On most systems it will be located in the Control Panel in the Administrative Tools section or under System Tools. Follow one of the following guides to create a task using the scheduler:

 - Windows 7: [http://windows.microsoft.com/en-us/windows7/schedule-a-task](http://windows.microsoft.com/en-us/windows7/schedule-a-task)
 - Windows XP: [http://support.microsoft.com/kb/308569](http://support.microsoft.com/kb/308569)

The task you want to perform is “Start a program”. Instead of clicking the “Browse” button to find a program, paste (Ctrl+V) the command you copied from DataSync directly into the field beside the “Browse” button. You will probably get a message asking you if you want to run the program “java” with the following arguments. Simply click “Yes”.

You may want to test run the task by finding the task you just created in the task library and right-clicking it and selecting "Run". Make sure the dataset was updated as you expect. **Remember** each job runs at its own frequency, meaning that there needs to be a business conversation with the data owners about how frequent each dataset should be updated.

![Task Scheduler](/datasync/images/task_scheduler.png)




### **Using Crontab (Mac or Linux)**
A good introduction to using Crontab (Crontab is included with essentially any Linux distro) can be found here:
[http://kvz.io/blog/2007/07/29/schedule-tasks-on-linux-using-crontab/](http://kvz.io/blog/2007/07/29/schedule-tasks-on-linux-using-crontab/)

To schedule a DataSync job simply copy the text within the "Command to execute with scheduler" field (after you save or open a job in DataSync) and use that command in place of the `/bin/execute/this/script.sh` in the above guide. For example, to schedule a job to run daily at midnight your Crontab content might look like:

    0 0 * * * java -jar <DATASYNC_JAR> /path/to/myjob.sij


A more Mac-specific guide here:
[http://www.maclife.com/article/columns/terminal_101_creating_cron_jobs](http://www.maclife.com/article/columns/terminal_101_creating_cron_jobs)
