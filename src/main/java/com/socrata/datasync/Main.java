package com.socrata.datasync;

//import com.socrata.datasync.job.FTPSmartUpdateJob;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesFile;
import com.socrata.datasync.preferences.UserPreferencesJava;
import com.socrata.datasync.ui.SimpleIntegrationWizard;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;

public class Main {
	/**
	 * @author Adrian Laurenzi
	 *
	 * Loads an instance of the SimpleIntegrationWizard in command line 
	 * mode (if arguments are given) or as a GUI (if no arguments are given).
	 */
    private static final String VALID_PUBLISH_METHODS = "upsert, append, or replace";

    public static final Options options = new Options();
    static {
        options.addOption("h", "header", true, "File to publish has header row (true or false)");
        options.addOption("m", "method", true, "Publish method (" + VALID_PUBLISH_METHODS + ")");
        options.addOption("i", "datasetid", true, "Dataset ID to publish to");
        options.addOption("f", "file", true, "CSV or TSV file to publish");
        options.addOption("c", "config", true, ".json file that stores authentication details and/or user preferences (optional)");
        options.addOption("?", "help", false, "Help");
    }

    public static void main(String[] args) throws ParseException {
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
    		    //TODO: Check file extension to determine type of job to run?
			    new SimpleIntegrationRunner(jobFileToRun);
            }
		} else {
            if (!commandArgsValid(cmd)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DataSync", options);
                System.exit(1);
            } else {
                // generate & run a an Integration job from command line args
                com.socrata.datasync.job.IntegrationJob jobToRun = null;
                if (cmd.hasOption('c')) {
                    File configFile = new File(cmd.getOptionValue("c"));
                    try {
                        jobToRun = new com.socrata.datasync.job.IntegrationJob(configFile);
                    } catch (IOException e) {
                        System.err.println("Failed to load " + configFile.getAbsolutePath() + ": " + e.toString());
                        System.exit(1);
                    }
                } else {
                    jobToRun = new com.socrata.datasync.job.IntegrationJob();
                }

                jobToRun.setDatasetID(cmd.getOptionValue("i"));
                jobToRun.setFileToPublish(cmd.getOptionValue("f"));
                jobToRun.setPublishMethod(
                        PublishMethod.valueOf(cmd.getOptionValue("m")));
                if(cmd.getOptionValue("h").equals("true")) {
                    jobToRun.setFileToPublishHasHeaderRow(true);
                } else { // cmd.getOptionValue("h") == "false"
                    jobToRun.setFileToPublishHasHeaderRow(false);
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

                new SimpleIntegrationRunner(jobToRun);
            }
		}
    }

    private static boolean commandArgsValid(CommandLine cmd) {
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
        if(cmd.getOptionValue("m") == null) {
            System.out.println("Missing required argument: -m,--method is required");
            return false;
        }

        boolean publishMethodValid = false;
        final String inputPublishMethod = cmd.getOptionValue("m");
        for(PublishMethod m : PublishMethod.values()) {
            if(inputPublishMethod.equals(m.toString()))
                publishMethodValid = true;
        }
        if(!publishMethodValid) {
            System.err.println("Invalid publish method: " + inputPublishMethod +
                    " (must be " + VALID_PUBLISH_METHODS + ")");
            return false;
        }

        if(!cmd.getOptionValue("h").equals("true") && !cmd.getOptionValue("h").equals("false")) {
            System.err.println("You must specify if file to publish has a header row (true or false)");
            return false;
        }

        return true;
    }

}