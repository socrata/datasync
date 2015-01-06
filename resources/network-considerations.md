---
layout: with-sidebar
title: Network Considerations
bodyclass: homepage
---

DataSync requires various network permissions depending on the upload method you choose.  Generally, ports 80 and 443 will be used and if you are using DataSync's FTP methods, ports 22222 and 3131-3141 also need to be open. If you have configured [email notifications]({{ site.root }}/resources/preferences-config.html#error-notification-auto-email-setup), the SMTP and SSL ports need to be open.

NOTE: Because network setups can vary wildly, this does not attempt to be a definitive guide, but does hope to give you some guidance if DataSync doesn't work out of the box because of networking issues.

- [Behind a Firewall?](#behind-a-firewall)
- [Behind a Proxy Server?](#behind-a-proxy-server)
- [Using an Outdated Java?](#using-an-outdated-java)
- [Still Stuck?](#still-stuck)


### Behind a Firewall?
Firewalls can block both incoming and outgoing traffic and can be configured to block particular ports, domains, programs and/or types of traffic.

Per port access, as noted above, DataSync requires usage of ports 80 (for http) and 443 (for https).  If you are using FTP methods, port 22222 (for control connection) and ports 3131 to 3141 (for data transferral) are also required.  If you have configured [email notifications]({{ site.root }}/resources/preferences-config.html#error-notification-auto-email-setup), the SMTP and SSL ports need to be open.

Per domain access, if using HTTP or Soda2 methods, DataSync communicates exclusively with the domain you provide in your [configuration]({{ site.root }}/resources/preferences-config.html). If using FTP methods, DataSync will also need to reach 'production.ftp.socrata.net'.  If you are using email notification, DataSync makes requests to the domain for the outgoing mail server. These domains should be white-listed according to your firewall's rules.

Per program restrictions and in particular if using the Windows Firewall, you will need to allow DataSync to communicate through the Windows Firewall. Be aware that you may need to do this for each network (home, work, public) that you use.

Per traffic types, DataSync has different request characteristics depending on which upload method you've chosen.

  * If using the HTTP method, DataSync uses an ssync algorithm to reduce the amount of data that needs to be sent to only the changes made since the last update.  This data is chunked currently to 4MB blocks.
  * If using the Soda2 method, DataSync will attempt to transfer the entire file to publish in one chunk.  This can create a long-lived connection which some firewalls do not allow.  [Chunking can be configured]({{ site.root }}/resources/preferences-config.html#chunking-configuration) using the Soda2 method though.
  * If using the FTP method, DataSync will compress the file to publish and attempt to transfer it in a single chunk. This can create a long-lived connection which some firewalls do not allow.


### Behind a Proxy Server?
Proxy servers intercept network traffic and can be configured in numerous ways to allow/block, inspect and encrypt/decrypt traffic, among other things. As such, everything in the ["Behind a Firewall"](#behind-a-firewall) section may apply. Because DataSync sends ssl requests, the proxy server must be set up to correctly handle encrypted traffic, i.e. that it is a "transparent proxy" - ask your IT deparment to confirm this.  Additionally, DataSync must be configured to route its requests through the proxy.  At minimum, [this configuration]({{ site.root }}/resources/preferences-config.html) requires the hostname and port and if the proxy server is authenticated, your proxy username and password as well.

**NOTICE:** DataSync has proxy support only for the HTTP methods; FTP and Soda2 methods cannot currently work behind a proxy.


### Using an Outdated Java?
Some networks will not allow Java programs to run if the version is outdated, particularly if the older version presents a security risk. You or your IT department will need to update to the most recent Java.


### Still Stuck?
If you are still stuck after reading this page becasue of networking problems, please contact Socrata support and provide the following information:

 * Whether you can browse the internet in a browser.
 * Whether you can browse to the domain you provided in your [configuration]({{ site.root }}/resources/preferences-config.html) in a browser.
 * Whether DataSync can run at all - even if it produces errors.
 * Assuming DataSync can run, a screenshot or text file of the errors that DataSync gives.

