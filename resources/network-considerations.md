---
layout: with-sidebar
title: Network Considerations
bodyclass: homepage
---

DataSync requires various network permissions depending on the upload method you choose.  When using the latest upload methods (HTTP rather than FTP or SODA2) the standard ports 80 and 443 will be used. Although we strongly recommend using the HTTP methods, if you are using DataSync's FTP methods, ports 22222 and 3131-3141 need to be open, in addition to ports 80 and 443. If you have configured [email notifications]({{ site.baseurl }}/resources/preferences-config.html#error-notification-auto-email-setup), the SMTP and SSL ports need to be open.

NOTE: Because network setups can vary wildly, this does not attempt to be a definitive guide, but does hope to give you some guidance if DataSync doesn't work out of the box because of networking issues.

- [Behind a Firewall?](#behind-a-firewall)
- [Behind a Proxy Server?](#behind-a-proxy-server)
- [Using an Outdated Java?](#using-an-outdated-java)
- [Certificate Validation Issues?](#certificate-validation-issues)
- [Still Stuck?](#still-stuck)


### Behind a Firewall?
Firewalls can block both incoming and outgoing traffic and can be configured to block particular ports, domains, programs and/or types of traffic.

Per port access, as noted above, DataSync requires usage of ports 80 (for http) and 443 (for https).  If you are using FTP methods, port 22222 (for control connection) and ports 3131 to 3141 (for data transferral) are also required.  If you have configured [email notifications]({{ site.baseurl }}/resources/preferences-config.html#error-notification-auto-email-setup), the SMTP and SSL ports need to be open.

Per domain access, if using HTTP or Soda2 methods, DataSync communicates exclusively with the domain you provide in your [configuration]({{ site.baseurl }}/resources/preferences-config.html). If using FTP methods, DataSync will also need to reach Socrata's ftp server. If you are using email notification, DataSync makes requests to the domain for the outgoing mail server. These domains should be white-listed according to your firewall's rules.

Per program restrictions and in particular if using the Windows Firewall, you will need to allow DataSync to communicate through the Windows Firewall. Be aware that you may need to do this for each network (home, work, public) that you use.

Per traffic types, DataSync has different request characteristics depending on which upload method you've chosen.

  * If using the HTTP method, DataSync uses an ssync algorithm to reduce the amount of data that needs to be sent to only the changes made since the last update.  This data is chunked currently to 4MB blocks.
  * If using the Soda2 method, DataSync will attempt to transfer the entire file to publish in one chunk.  This can create a long-lived connection which some firewalls do not allow.  [Chunking can be configured]({{ site.baseurl }}/resources/preferences-config.html#chunking-configuration) using the Soda2 method though.
  * If using the FTP method, DataSync will compress the file to publish and attempt to transfer it in a single chunk. This can create a long-lived connection which some firewalls do not allow.


### Behind a Proxy Server?
Proxy servers intercept network traffic and can be configured in numerous ways to allow/block, inspect and encrypt/decrypt traffic, among other things. As such, everything in the ["Behind a Firewall"](#behind-a-firewall) section may apply. Because DataSync sends ssl requests, the proxy server must be set up to correctly handle encrypted traffic, i.e. that it is a "transparent proxy" - ask your IT deparment to confirm this.

DataSync must be configured to route its requests through the proxy.  At minimum, [this configuration]({{ site.baseurl }}/resources/preferences-config.html) requires the hostname and port and if the proxy server is authenticated, your proxy username and password as well.

**NOTICE:** DataSync has proxy support only for the HTTP methods; FTP and Soda2 methods cannot currently work behind a proxy.

### Using an Outdated Java?
Some networks will not allow Java programs to run if the version is outdated, particularly if the older version presents a security risk. You or your IT department will need to update to the most recent Java.


### Certificate Validation Issues?
If you receive a SunCertPathBuilderException, there are two typical causes:

  1. Java is out-of-date and as a result is failing to validate the SSL certificate. To correct this issue you must update Java JDK or JRE on the machine running DataSync.
  2. Java does not approve of one of the certificates in the chain between your machine and the domain you're trying to upload to.  The solution is to add the necessary certificates into Java's trusted certificate store. The steps to do this are:

  * Get the certificate chain.
    * Find where Java's keytool is located.
      * On Windows, this is likely at "C:\Program Files\Java\jre7\bin")
      * On Mac OS X, this is likely at "/Library/Java/JavaVirtualMachines/jdk1.7.0_45.jdk/Contents/Home/bin/"
    * Run the following, removing the proxy options if you are not behind a proxy server. You can remove the '-rfc' option to get additional information about each certificate in the chain.

           keytool -J-Dhttps.proxyHost=<PROXY_HOST>
                   -J-Dhttps.proxyPort=<PROXY_PORT>
                   -printcert -rfc
                   -sslserver <DOMAIN>:443

  * **Validate any certificates you plan to add with your IT department !!!!**.  It is a security risk to add unknown certificates.
  * Copy the cert you need to add inclusively from -----BEGIN CERTIFICATE----- to -----END CERTIFICATE----- into a file `<FILENAME>`.cer
  * Run the following, using your keystore password if that has been set up or the default password 'changeit' otherwise.

           keytool -import -keystore cacerts -file <FILENAME>.cer




### Still Stuck?
If you are still stuck after reading this page becasue of networking problems, please contact Socrata support and provide the following information:

 * Whether you can browse the internet in a browser.
 * Whether you can browse to the domain you provided in your [configuration]({{ site.baseurl }}/resources/preferences-config.html) in a browser.
 * Whether DataSync can run at all - even if it produces errors.
 * Assuming DataSync can run, a screenshot or text file of the errors that DataSync gives.
 * If possible, a wireshark trace. You will need to download [wireshark](https://www.wireshark.org/) and [capture all network traffic](http://www.howtogeek.com/104278/how-to-use-wireshark-to-capture-filter-and-inspect-packets/) while attempting to run your DataSync job.

