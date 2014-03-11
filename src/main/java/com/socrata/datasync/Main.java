package com.socrata.datasync;

import com.socrata.datasync.job.PortJob;
import com.socrata.datasync.preferences.UserPreferences;
import com.socrata.datasync.preferences.UserPreferencesFile;
import com.socrata.datasync.preferences.UserPreferencesJava;
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
        options.addOption("pf", PUBLISH_VIA_FTP_FLAG, true, "Use FTP/SmartUpdate (instead of HTTP) for publishing (true or false) (default: " + DEFAULT_VALUE_publishViaFTP + ") [IntegrationJob]");
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
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DataSync", options);
            } else {
                // Run a job file (.sij) in command-line mode
                String jobFileToRun = args[0];
			    new SimpleIntegrationRunner(jobFileToRun);
            }
		} else {
            if(!commandArgsValid(cmd)) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DataSync", options);
                System.exit(1);
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
                if(jobType.equals(INTEGRATION_JOB)) {
                    runIntegrationJob(cmd, userPrefs);
                } else if(jobType.equals(PORT_JOB)) {
                    runPortJob(cmd, userPrefs);
                } else if(jobType.equals(LOAD_PREFERENCES_JOB)) {
                    loadUserPreferences(userPrefs);
                }
            }
		}
    }

    /**
     * Loads given userPrefs into the saved global DataSync preferences (using
     * Java Preferences class).
     *
     * @param userPrefs object containing user preferences to be loaded
     */
    private static void loadUserPreferences(UserPreferences userPrefs) {
        UserPreferencesJava newUserPrefs = new UserPreferencesJava();
        newUserPrefs.saveDomain(userPrefs.getDomain());
        newUserPrefs.saveUsername(userPrefs.getUsername());
        newUserPrefs.savePassword(userPrefs.getPassword());
        newUserPrefs.saveAPIKey(userPrefs.getAPIKey());
        newUserPrefs.saveAdminEmail(userPrefs.getAdminEmail());
        newUserPrefs.saveEmailUponError(userPrefs.emailUponError());
        newUserPrefs.saveLogDatasetID(userPrefs.getLogDatasetID());
        newUserPrefs.saveOutgoingMailServer(userPrefs.getOutgoingMailServer());
        newUserPrefs.saveSMTPPort(userPrefs.getSmtpPort());
        newUserPrefs.saveSSLPort(userPrefs.getSslPort());
        newUserPrefs.saveSMTPUsername(userPrefs.getSmtpUsername());
        newUserPrefs.saveSMTPPassword(userPrefs.getSmtpPassword());
        newUserPrefs.saveFilesizeChunkingCutoffMB(
                Integer.parseInt(userPrefs.getFilesizeChunkingCutoffMB()));
        newUserPrefs.saveNumRowsPerChunk(
                Integer.parseInt(userPrefs.getNumRowsPerChunk()));
    }

    private static void runIntegrationJob(CommandLine cmd, UserPreferences userPrefs) {
        com.socrata.datasync.job.IntegrationJob jobToRun =
                new com.socrata.datasync.job.IntegrationJob(userPrefs);

        // Set required parameters
        jobToRun.setDatasetID(cmd.getOptionValue(DATASET_ID_FLAG));
        jobToRun.setFileToPublish(cmd.getOptionValue(FILE_TO_PUBLISH_FLAG));
        jobToRun.setPublishMethod(PublishMethod.valueOf(cmd.getOptionValue(PUBLISH_METHOD_FLAG)));
        if(cmd.getOptionValue(HAS_HEADER_ROW_FLAG).equalsIgnoreCase("true")) {
            jobToRun.setFileToPublishHasHeaderRow(true);
        } else { // cmd.getOptionValue(HAS_HEADER_ROW_FLAG) == "false"
            jobToRun.setFileToPublishHasHeaderRow(false);
        }

        // Set optional parameters
        if(cmd.getOptionValue(PATH_TO_FTP_CONTROL_FILE_FLAG) != null) {
            jobToRun.setPathToFTPControlFile(cmd.getOptionValue(PATH_TO_FTP_CONTROL_FILE_FLAG));
        }
        String publishViaFTP = cmd.getOptionValue(PUBLISH_VIA_FTP_FLAG, DEFAULT_VALUE_publishViaFTP);
        if(publishViaFTP.equalsIgnoreCase("true")) {
            jobToRun.setPublishViaFTP(true);
        } else { // cmd.getOptionValue("pf") == "false"
            jobToRun.setPublishViaFTP(false);
        }

        new SimpleIntegrationRunner(jobToRun);
    }

    private static void runPortJob(CommandLine cmd, UserPreferences userPrefs) {
        PortJob jobToRun = new PortJob(userPrefs);
        // TODO FINISH
        jobToRun.setPortMethod(PortMethod.valueOf(cmd.getOptionValue("pm")));
        jobToRun.setSourceSiteDomain(cmd.getOptionValue("pd1"));
        jobToRun.setSourceSetID(cmd.getOptionValue("pi1"));
        jobToRun.setSinkSiteDomain(cmd.getOptionValue("pd2"));

        if(cmd.getOptionValue("pi2") != null)
            jobToRun.setSinkSetID(cmd.getOptionValue("pi2"));
        if(cmd.getOptionValue("ppm") != null)
            jobToRun.setPublishMethod(PublishMethod.valueOf(cmd.getOptionValue("ppm")));
        if(cmd.getOptionValue("pp") != null) {
            if(cmd.getOptionValue("pp").equalsIgnoreCase("true")) {
                jobToRun.setPublishDataset(PublishDataset.publish);
            } else { // cmd.getOptionValue("pp") == "false"
                jobToRun.setPublishDataset(PublishDataset.working_copy);
            }
        }
        if(cmd.getOptionValue("pdt") != null)
            jobToRun.setDestinationDatasetTitle(cmd.getOptionValue("pdt"));
        new SimpleIntegrationRunner(jobToRun);
    }

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

    public static boolean commandArgsValid(CommandLine cmd) {
        if(cmd.getOptionValue(JOB_TYPE_FLAG, INTEGRATION_JOB).equals(INTEGRATION_JOB)) {
            return integrationJobArgsValid(cmd);
        } else if(cmd.getOptionValue(JOB_TYPE_FLAG).equals(PORT_JOB)) {
            return portJobArgsValid(cmd);
        } else if(cmd.getOptionValue(JOB_TYPE_FLAG).equals(LOAD_PREFERENCES_JOB)) {
            return loadPreferencesJobArgsValid(cmd);
        } else {
            System.err.println("Invalid " + JOB_TYPE_FLAG + ": " + cmd.getOptionValue(JOB_TYPE_FLAG) +
                    " (must be " + IntegrationUtility.getArrayAsQuotedList(VALID_JOB_TYPES) + ")");
            return false;
        }
    }

    private static boolean loadPreferencesJobArgsValid(CommandLine cmd) {
        if(cmd.getOptionValue(CONFIG_FLAG) == null) {
            System.err.println("Missing required argument: " +
                    "-c,--"+ CONFIG_FLAG +" is required when " + JOB_TYPE_FLAG + " is '" + LOAD_PREFERENCES_JOB + "'");
            return false;
        }
        return true;
    }

    // TODO finish validation of PortJob params
    private static boolean portJobArgsValid(CommandLine cmd) {
        // Validate required parameters
        if(cmd.getOptionValue("pm") == null) {
            System.err.println("Missing required argument: -pm,--portMethod is required");
            return false;
        }
        if(!portMethodIsValid(cmd)) {
            System.err.println("Invalid argument: -pm,--portMethod must be: " +
                    IntegrationUtility.getValidPortMethods());
            return false;
        }

        if(cmd.getOptionValue("pd1") == null) {
            System.err.println("Missing required argument: -pd1,--sourceDomain is required");
            return false;
        }
        if(cmd.getOptionValue("pi1") == null) {
            System.err.println("Missing required argument: -pi1,--sourceDatasetId is required");
            return false;
        }
        if(cmd.getOptionValue("pd2") == null) {
            System.err.println("Missing required argument: -pd2,--destinationDomain is required");
            return false;
        }

        // TODO Validate optional parameters

        return true;
    }

    private static boolean integrationJobArgsValid(CommandLine cmd) {
        // Validate required parameters
        if(cmd.getOptionValue(DATASET_ID_FLAG) == null) {
            System.err.println("Missing required argument: -i,--" + DATASET_ID_FLAG + " is required");
            return false;
        }
        if(cmd.getOptionValue(FILE_TO_PUBLISH_FLAG) == null) {
            System.err.println("Missing required argument: -f,--" + FILE_TO_PUBLISH_FLAG + " is required");
            return false;
        }
        if(!publishMethodIsValid(cmd)) {
            return false;
        }
        if(cmd.getOptionValue(HAS_HEADER_ROW_FLAG) == null) {
            System.err.println("Missing required argument: -h,--" + HAS_HEADER_ROW_FLAG + " is required");
            return false;
        }
        if(!cmd.getOptionValue(HAS_HEADER_ROW_FLAG).equalsIgnoreCase("true")
                && !cmd.getOptionValue(HAS_HEADER_ROW_FLAG).equalsIgnoreCase("false")) {
            System.err.println("Invalid argument: -h,--" + HAS_HEADER_ROW_FLAG + " must be 'true' or 'false'");
            return false;
        }

        // Validate optional parameters
        if(cmd.getOptionValue(PUBLISH_VIA_FTP_FLAG) != null && !cmd.getOptionValue(PUBLISH_VIA_FTP_FLAG).equalsIgnoreCase("true")
                && !cmd.getOptionValue(PUBLISH_VIA_FTP_FLAG).equalsIgnoreCase("false")) {
            System.err.println("Invalid argument: -pf,--" + PUBLISH_VIA_FTP_FLAG + " must be 'true' or 'false'");
            return false;
        }

        if(cmd.getOptionValue(PATH_TO_FTP_CONTROL_FILE_FLAG) != null
                && cmd.getOptionValue(PATH_TO_FTP_CONTROL_FILE_FLAG).equalsIgnoreCase("false")) {
            System.err.println("Invalid argument: -sc,--" + PATH_TO_FTP_CONTROL_FILE_FLAG + " cannot be supplied " +
                    "unless -pf,--" + PUBLISH_VIA_FTP_FLAG + " is 'true'");
            return false;
        }

        if(cmd.getOptionValue(PATH_TO_FTP_CONTROL_FILE_FLAG) != null) {
            // TODO remove this when flags override other parameters
            if(cmd.getOptionValue(PUBLISH_METHOD_FLAG) != null) {
                System.out.println("WARNING: -m,--" + PUBLISH_METHOD_FLAG + " is being ignored because " +
                        "-sc,--" + PATH_TO_FTP_CONTROL_FILE_FLAG + " is supplied");
            }
            if(cmd.getOptionValue(HAS_HEADER_ROW_FLAG) != null) {
                System.out.println("WARNING: -h,--" + HAS_HEADER_ROW_FLAG + " is being ignored because " +
                        "-sc,--" + PATH_TO_FTP_CONTROL_FILE_FLAG + " is supplied");
            }
        }

        return true;
    }

    private static boolean publishMethodIsValid(CommandLine cmd) {
        if(cmd.getOptionValue("m") == null) {
            System.err.println("Missing required argument: -m,--publishMethod is required");
            return false;
        }
        boolean publishMethodValid = false;
        final String inputPublishMethod = cmd.getOptionValue("m");
        for(PublishMethod m : PublishMethod.values()) {
            if(inputPublishMethod.equals(m.name()))
                publishMethodValid = true;
        }
        if(!publishMethodValid) {
            System.err.println("Invalid argument: -m,--publishMethod must be " +
                    IntegrationUtility.getValidPublishMethods());
            return false;
        }
        return true;
    }

    private static boolean portMethodIsValid(CommandLine cmd) {
        String inputPortMethod = cmd.getOptionValue("pm");
        boolean portMethodValid = false;
        for(PortMethod m : PortMethod.values()) {
            if(inputPortMethod.equals(m.name()))
                portMethodValid = true;
        }
        return portMethodValid;
    }
}