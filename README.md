Last updated: June 19, 2013

Author: Adrian Laurenzi

How to set up project from scratch
--------------------------------------

1. Open Eclipse
2. File -> New -> Java Project
3. Uncheck "Use default location" and set the Location to the root of the repo
4. Make sure the JRE is JavaSE-1.6
5. Enter a project name and click Finish.
6. Right-click on the project in the explorer on the left -> Properties
7. Java Build Path -> Libraries Tab -> Add External JARs and add both mail.jar and soda-api-java-0.9.3-standalone.jar in the ~/datasync/lib/ directory


How to export the application as an executable JAR
--------------------------------------

1. Go to Run -> Run Configurations...
2. Be sure the DataSync project is selected
3. Enter "DataSync-Main" as the name of the configuration
3. Press "Search" next to the "Main class:" field and select "Main-com.socrata.datasync"
4. Click Apply and then Run to ensure the application runs correctly
5. File -> Export -> Java -> Runnable JAR file, hit next 
6. Select "DataSync-Main-..." for the Launch configuration
7. Be sure "Extract required libraries into generated JAR" is selected under Library Handeling
8. Click finish
9. Finally, you must manually add the socrata_logo.png file to the root directory of the .jar archive
 

