---
layout: with-sidebar
title: Checking the logs and downloading CSV/TSV "snapshots"
bodyclass: homepage
---

### Contents
- [Via FTP](#ftp-logs)
    - [Connecting to the FTP server](#connecting-ftp)
    - [Checking the logs and downloading CSV "snapshots"](#ftp-check-logs)
- [Via HTTP](#http-logs)

{#ftp-logs}<p>&nbsp;</p>

### Connecting to the logs via FTP

{#connecting-ftp}<p>&nbsp;</p>

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

{#ftp-check-logs}<p>&nbsp;</p>
#### Checking the logs and downloading CSV "snapshots"

Inside each dataset identifier directory there should be the following files/directories:
- active-control.json
- log.txt
- status.txt
- completed (a directory with subdirectories for each day starting on 1/1/2013)

You can download log.txt to see the logging information for the given dataset. Within the ‘completed’ directory you can find CSVs/TSVs and control.json files archived by date (there are nested folders for year, month, and day). After each successful update operation using DataSync replace via FTP, the CSV/TSV and control.json files that were used to perform the update are archived. Archived files will be stored for the most recent 10 successful update operations (or possibly more). Contact Socrata support if you would like additonal information about archiving.



{#http-logs}<p>&nbsp;</p>

### Connecting to the logs via HTTP

Details forthcoming.
