package com.socrata.datasync;

import com.socrata.datasync.job.PortJob;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesFile;
import com.socrata.datasync.ui.SimpleIntegrationWizard;
import org.codehaus.jackson.map.ObjectMapper;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import com.socrata.datasync.ui.SimpleIntegrationWizard;

public class Main {
	/**
	 * @author Adrian Laurenzi
	 *
	 * Loads an instance of the SimpleIntegrationWizard in command line 
	 * mode (if arguments are given) or as a GUI (if no arguments are given).
	 */
    private static final String VALID_PUBLISH_METHODS = "upsert, append, replace, delete, copy_data, copy_schema, copy_all";

    public static final Options options = new Options();
    static {
        options.addOption("h", "header", true, "File to publish has header row (true or false)");
        options.addOption("m", "method", true, "Publish method (" + VALID_PUBLISH_METHODS + ")");
        options.addOption("i", "datasetid", true, "Dataset ID to publish to");
        options.addOption("f", "file", true, "CSV or TSV file to publish");
        options.addOption("c", "config", true, ".json file that stores authentication details and/or user preferences (optional)");
        options.addOption("sc", "control", true, "SmartUpdate control.json file (optional)");

        options.addOption("pd1", "portsourcedomain", true, "Source Domain (for port jobs only)");
        options.addOption("pi1", "portsourcedatasetid", true, "Source Dataset ID (for port jobs only)");
        options.addOption("pd2", "portdestdomain", true, " Destination Domain (for Port Jobs only)");
        options.addOption("pi2", "portdestdatasetid", true, "Destination Dataset ID (for port jobs only)");
        options.addOption("pp", "portpublishdest", true, "Publish Destination Dataset (true or false) (for port jobs only)");
        options.addOption("pm", "portpublishmethod", true, "Data Porting Publish Method (upsert or replace) (for port jobs only)");

        options.addOption("?", "help", false, "Help");
    }

    // TODO remove EXCEPTIONS in method signatures
    public static void main(String[] args) throws ParseException, IOException {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

        if(args.length == 0) {
            // Open GUI (default)
            new SimpleIntegrationWizard();
        } else if(args.length == 1) {
    		// Run a specific job file in command line mode (usually for scheduler calls)
            if(args[0].equals("-?") || args[0].equals("--help")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DataSync", options);
            } else {
    		    String jobFileToRun = args[0];
			    new SimpleIntegrationRunner(jobFileToRun);
            }
		} else {
            if (!commandArgsValid(cmd)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DataSync", options);
                System.exit(1);
            } else {
                // generate & run job from command line args
                UserPreferences userPrefs = null;
                if (cmd.hasOption('c')) {
                    File configFile = new File(cmd.getOptionValue("c"));
                    // load user preferences from given JSON config file
                    ObjectMapper mapper = new ObjectMapper();
                    try {
                        userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
                    } catch (IOException e) {
                        System.err.println("Failed to load " + configFile.getAbsolutePath() + ": " + e.toString());
                        System.exit(1);
                    }
                }

                if(cmd.getOptionValue("m").startsWith("copy_")) {
                    // running a PortJob
                    PortJob jobToRun;
                    if (cmd.hasOption('c')) {
                        jobToRun = new PortJob(userPrefs);
                    } else {
                        jobToRun = new PortJob();
                    }

                    jobToRun.setPortMethod(PortMethod.valueOf(cmd.getOptionValue("m")));
                    jobToRun.setSourceSiteDomain(cmd.getOptionValue("pd1"));
                    jobToRun.setSourceSetID(cmd.getOptionValue("pi1"));
                    jobToRun.setSinkSiteDomain(cmd.getOptionValue("pd2"));

                    if(cmd.getOptionValue("pi2") != null)
                        jobToRun.setSinkSetID(cmd.getOptionValue("pi2"));
                    if(cmd.getOptionValue("pp") != null)
                        jobToRun.setPublishDataset(PublishDataset.valueOf(cmd.getOptionValue("pp")));
                    if(cmd.getOptionValue("pm") != null)
                        jobToRun.setPublishMethod(PublishMethod.valueOf(cmd.getOptionValue("pm")));

                    new SimpleIntegrationRunner(jobToRun);

                } else {
                    // running a normal IntegrationJob
                    com.socrata.datasync.job.IntegrationJob jobToRun;
                    if (cmd.hasOption('c')) {
                        jobToRun = new com.socrata.datasync.job.IntegrationJob(userPrefs);
                    } else {
                        jobToRun = new com.socrata.datasync.job.IntegrationJob();
                    }
                    jobToRun.setDatasetID(cmd.getOptionValue("i"));
                    jobToRun.setFileToPublish(cmd.getOptionValue("f"));
                    jobToRun.setPublishMethod(
                            PublishMethod.valueOf(cmd.getOptionValue("m")));
                    if(cmd.getOptionValue("h").equalsIgnoreCase("true")) {
                        jobToRun.setFileToPublishHasHeaderRow(true);
                    } else { // cmd.getOptionValue("h") == "false"
                        jobToRun.setFileToPublishHasHeaderRow(false);
                    }
                    if(cmd.getOptionValue("sc") != null) {
                        jobToRun.setPathToFTPControlFile(cmd.getOptionValue("sc"));
                    }
                    new SimpleIntegrationRunner(jobToRun);
                }

                // DEPRECIATED...now you can only establish auth credentials from config file
                // Set authentication credentials if they were supplied,
                // otherwise get them from previously saved UserPreferences
                /*UserPreferencesJava userPrefs = new UserPreferencesJava();
                final String domain = cmd.getOptionValue("d", userPrefs.getDomain());
                final String username = cmd.getOptionValue("u", userPrefs.getUsername());
                final String appToken = cmd.getOptionValue("a", userPrefs.getAPIKey());
                // get the password
                String password;
                if (cmd.hasOption('P')) {
                    System.out.print("Password:");
                    char[] pwdChar = System.console().readPassword();
                    password = new String(pwdChar);
                } else {
                    password = cmd.getOptionValue("p", userPrefs.getPassword());
                }
                userPrefs.saveDomain(domain);
                userPrefs.saveUsername(username);
                userPrefs.saveAPIKey(appToken);
                userPrefs.savePassword(password);*/
            }
		}
    }

    private static boolean commandArgsValid(CommandLine cmd) {
        if(cmd.getOptionValue("m") == null) {
            System.out.println("Missing required argument: -m,--method is required");
            return false;
        }
        boolean publishMethodValid = false;
        final String inputPublishMethod = cmd.getOptionValue("m");
        for(PublishMethod m : PublishMethod.values()) {
            if(inputPublishMethod.equals(m.name()))
                publishMethodValid = true;
        }
        for(PortMethod m : PortMethod.values()) {
            if(inputPublishMethod.equals(m.name()))
                publishMethodValid = true;
        }
        if(!publishMethodValid) {
            System.err.println("Invalid publish method: " + inputPublishMethod +
                    " (must be " + VALID_PUBLISH_METHODS + ")");
            return false;
        }

        if(cmd.getOptionValue("m").startsWith("copy_")) {
            // TODO validate PortJob params
            // ...
        } else {
            // validate normal IntegrationJob params
            if(cmd.getOptionValue("f") == null) {
                System.out.println("Missing required argument: -f,--file is required");
                return false;
            }
            if(cmd.getOptionValue("h") == null) {
                System.out.println("Missing required argument: -h,--header is required");
                return false;
            }
            if(cmd.getOptionValue("i") == null) {
                System.out.println("Missing required argument: -i,--datasetid is required");
                return false;
            }

            if(!cmd.getOptionValue("h").equalsIgnoreCase("true")
                    && !cmd.getOptionValue("h").equalsIgnoreCase("false")) {
                System.err.println("You must specify if file to publish has a header row (true or false)");
                return false;
            }
        }

        return true;
    }

}