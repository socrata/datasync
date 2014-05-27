package com.socrata.datasync.job;

import com.socrata.datasync.JobStatus;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesFile;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import org.apache.commons.cli.CommandLine;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class LoadPreferencesJob extends Job {
    private UserPreferences userPrefs;
    private CommandLineOptions options = new CommandLineOptions();

    public LoadPreferencesJob() {}
    public LoadPreferencesJob(UserPreferences userPrefs) {
        this.userPrefs = userPrefs;
    }

    public boolean validateArgs(CommandLine cmd){
        if(cmd.getOptionValue(options.CONFIG_FLAG) == null) {
            System.err.println("Missing required argument: " +
                "-c,--"+ options.CONFIG_FLAG +" is required when " + options.JOB_TYPE_FLAG + " is '" + Jobs.LOAD_PREFERENCES_JOB.toString() + "'");
            return false;
        }
        return true;
    }

    public void configure(CommandLine cmd) {
        if (cmd.getOptionValue(options.CONFIG_FLAG) != null) {
            try {
                // load user preferences from given JSON config file
                File configFile = new File(cmd.getOptionValue(options.CONFIG_FLAG));
                ObjectMapper mapper = new ObjectMapper();
                userPrefs = mapper.readValue(configFile, UserPreferencesFile.class);
            } catch (IOException e) {
                System.err.println("Failed to load configuration: " + e.toString());
                System.exit(1);
            }
        }
    }

    public JobStatus run() {
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

        System.out.println("Preferences saved:\n\n" + newUserPrefs.toString());
        return JobStatus.SUCCESS;
    }

    public UserPreferences getUserPrefs() { return userPrefs; }
}
