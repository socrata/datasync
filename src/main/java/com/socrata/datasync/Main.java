package com.socrata.datasync;

import com.socrata.datasync.job.Job;
import com.socrata.datasync.job.LoadPreferencesJob;
import com.socrata.datasync.job.PortJob;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesFile;
import com.socrata.datasync.preferences.UserPreferencesJava;
import com.socrata.datasync.ui.SimpleIntegrationWizard;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.codehaus.jackson.map.ObjectMapper;

public class Main {
	/**
	 * @author Adrian Laurenzi
	 *
	 * Loads an instance of the SimpleIntegrationWizard in command line 
	 * mode (if arguments are given) or as a GUI (if no arguments are given).
	 */
    private static final String INTEGRATION_JOB = "IntegrationJob";
    private static final String PORT_JOB = "PortJob";
    private static final String LOAD_PREFERENCES_JOB = "LoadPreferences";
    private static final String[] VALID_JOB_TYPES = {INTEGRATION_JOB, PORT_JOB, LOAD_PREFERENCES_JOB};

    private static final String JOB_TYPE_FLAG = "jobType";
    private static final String CONFIG_FLAG = "config";

    private static final String DATASET_ID_FLAG = "datasetID";
    private static final String FILE_TO_PUBLISH_FLAG = "fileToPublish";
    private static final String PUBLISH_METHOD_FLAG = "publishMethod";
    private static final String HAS_HEADER_ROW_FLAG = "fileToPublishHasHeaderRow";
    private static final String PUBLISH_VIA_FTP_FLAG = "publishViaFTP";
    private static final String PATH_TO_FTP_CONTROL_FILE_FLAG = "pathToFTPControlFile";

    private static final String DEFAULT_VALUE_jobType = INTEGRATION_JOB;
    private static final String DEFAULT_VALUE_publishViaFTP = "false";

    private static final String DEFAULT_VALUE_portPublishMethod = PublishMethod.upsert.toString();
    private static final String DEFAULT_VALUE_publishDestinationDataset = "false";

    public static final Options options = new Options();
    static {
        options.addOption("t", JOB_TYPE_FLAG, true, "Type of job to run: " + IntegrationUtility.getArrayAsQuotedList(VALID_JOB_TYPES) + " (default: " + DEFAULT_VALUE_jobType + ")");
        options.addOption("c", CONFIG_FLAG, true, ".json file that stores global preferences (authentication details, etc) (optional)");

        // IntegrationJob params
        options.addOption("i", DATASET_ID_FLAG, true, "Dataset ID to publish to [IntegrationJob]");
        options.addOption("f", FILE_TO_PUBLISH_FLAG, true, "CSV or TSV file to publish [IntegrationJob]");
        options.addOption("m", PUBLISH_METHOD_FLAG, true, "Publish method (" + IntegrationUtility.getValidPublishMethods() + ") [IntegrationJob]");
        options.addOption("h", HAS_HEADER_ROW_FLAG, true, "File to publish has header row (true or false) [IntegrationJob]");
        options.addOption("pf", PUBLISH_VIA_FTP_FLAG, true, "Use FTP (instead of HTTP) for publishing (true or false) (default: " + DEFAULT_VALUE_publishViaFTP + ") [IntegrationJob]");
        options.addOption("sc", PATH_TO_FTP_CONTROL_FILE_FLAG, true, "FTP control.json file, if set overrides job parameters (optional) [IntegrationJob]");

        // TODO fill in flags with FINAL VARS like above
        // PortJob params
        options.addOption("pm", "portMethod", true, "Port method (" + IntegrationUtility.getValidPortMethods() + ") [PortJob]");
        options.addOption("pd1", "sourceDomain", true, "Source Domain [PortJob]");
        options.addOption("pi1", "sourceDatasetId", true, "Source Dataset ID [PortJob]");
        options.addOption("pd2", "destinationDomain", true, " Destination Domain [PortJob]");
        options.addOption("pi2", "destinationDatasetId", true, "Destination Dataset ID (only use when sourceDomain is 'copy_data') [PortJob]");
        options.addOption("ppm", "portPublishMethod", true, "Data Porting Publish Method (upsert or replace) (default: " + DEFAULT_VALUE_portPublishMethod + ") [PortJob]");
        options.addOption("pp", "publishDestinationDataset", true, "Publish Destination Dataset (true or false) (default: " + DEFAULT_VALUE_publishDestinationDataset + ") [PortJob]");
        options.addOption("pdt", "destinationDatasetTitle", true, "Destination Dataset Title (optional) [PortJob]");

        options.addOption("?", "help", false, "Help");
    }

    public static void main(String[] args) throws ParseException {
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);

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
            UserPreferences userPrefs = null;
            try {
                userPrefs = loadUserPreferences(cmd);
            } catch (IOException e) {
                System.err.println("Failed to load configuration: " + e.toString());
                System.exit(1);
            }

            String jobType = cmd.getOptionValue(JOB_TYPE_FLAG, DEFAULT_VALUE_jobType);

            Job jobToRun = new com.socrata.datasync.job.IntegrationJob(userPrefs);
            if(jobType.equals(PORT_JOB)) {
                jobToRun = new PortJob(userPrefs);
            } else if(jobType.equals(LOAD_PREFERENCES_JOB)) {
                jobToRun = new LoadPreferencesJob(userPrefs);
            } else if (!jobType.equals(INTEGRATION_JOB)){
                System.err.println("Invalid " + JOB_TYPE_FLAG + ": " + cmd.getOptionValue(JOB_TYPE_FLAG) +
                        " (must be " + IntegrationUtility.getArrayAsQuotedList(VALID_JOB_TYPES) + ")");
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
        formatter.printHelp("DataSync", options);
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
        if (cmd.getOptionValue(CONFIG_FLAG) != null) {
            // load user preferences from given JSON config file
            File configFile = new File(cmd.getOptionValue(CONFIG_FLAG));
            ObjectMapper mapper = new ObjectMapper();
            userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
        } else {
            // load user preferences from Java preferences class
            userPrefs = new UserPreferencesJava();
        }
        return userPrefs;
    }
}