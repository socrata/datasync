package com.socrata.datasync;

import com.socrata.datasync.job.Job;
import com.socrata.datasync.job.Jobs;
import com.socrata.datasync.job.LoadPreferencesJob;
import com.socrata.datasync.job.PortJob;
import com.socrata.datasync.preferences.CommandLineOptions;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesFile;
import com.socrata.datasync.preferences.UserPreferencesJava;
import com.socrata.datasync.ui.SimpleIntegrationWizard;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;

public class Main {
	/**
	 * Loads an instance of the SimpleIntegrationWizard in command line 
	 * mode (if arguments are given) or as a GUI (if no arguments are given).
	 */
    public static void main(String[] args) throws ParseException {
        if(args.length == 0) {
            // Open GUI (default)
            new SimpleIntegrationWizard();
        } else if(args.length == 1) {
    		if(args[0].equals("-?") || args[0].equals("--help")) {
                printHelp();
            } else {
                // Run a job file (.sij) in command-line mode
                String jobFileToRun = args[0];
			    new SimpleIntegrationRunner(jobFileToRun);
            }
		} else {
            // generate & run job from command line args
            CommandLineOptions options = new CommandLineOptions();
            CommandLine cmd = options.getCommandLine(args);
            UserPreferences userPrefs = null;
            try {
                userPrefs = loadUserPreferences(cmd);
            } catch (IOException e) {
                System.err.println("Failed to load configuration: " + e.toString());
                System.exit(1);
            }

            String jobTypeFlag = options.JOB_TYPE_FLAG;
            String jobType = cmd.getOptionValue(jobTypeFlag, options.DEFAULT_JOBTYPE);

            Job jobToRun = new com.socrata.datasync.job.IntegrationJob(userPrefs);
            if(jobType.equals(Jobs.PORT_JOB.toString())) {
                jobToRun = new PortJob(userPrefs);
            } else if(jobType.equals(Jobs.LOAD_PREFERENCES_JOB.toString())) {
                jobToRun = new LoadPreferencesJob(userPrefs);
            } else if (!jobType.equals(Jobs.INTEGRATION_JOB.toString())){
                System.err.println("Invalid " + jobTypeFlag + ": " + cmd.getOptionValue(jobTypeFlag) +
                        " (must be " + Arrays.toString(Jobs.values()) + ")");
                System.exit(1);
            }

            if (jobToRun.validateArgs(cmd)) {
                jobToRun.configure(cmd);
                new SimpleIntegrationRunner(jobToRun);
            } else {
                printHelp();
                System.exit(1);
            }
        }
    }

    private static void printHelp() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("DataSync", CommandLineOptions.options);
    }

    // TODO: move the method below to UserPreferences when I get those set of interfaces/classes consolidated.

    /**
     * Returns a UserPreferences object which either loads User Prefs from a JSON file
     * or the Java Preferences class (previously saved from GUI mode input)
     *
     * @param cmd
     * @return UserPreferences object containing global preferences
     * @throws IOException
     */
    private static UserPreferences loadUserPreferences(CommandLine cmd) throws IOException {
        UserPreferences userPrefs;
        if (cmd.getOptionValue("config") != null) {
            // load user preferences from given JSON config file
            File configFile = new File(cmd.getOptionValue("config"));
            ObjectMapper mapper = new ObjectMapper();
            userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
        } else {
            // load user preferences from Java preferences class
            userPrefs = new UserPreferencesJava();
        }
        return userPrefs;
    }
}