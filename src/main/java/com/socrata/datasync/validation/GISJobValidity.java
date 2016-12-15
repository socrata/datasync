package com.socrata.datasync.validation;

import com.socrata.datasync.DatasetUtils;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.Utils;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.job.GISJob;
import com.socrata.datasync.job.JobStatus;
import com.socrata.model.importer.GeoDataset;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class GISJobValidity {
    public static final List<String> allowedGeoFileToPublishExtensions = Arrays.asList("geojson","zip","kml","kmz");

    /**
     * Checks that the command line arguments are sensible
     * NB: it is expected that this is run before 'configure'.
     * @param cmd the commandLine object constructed from the user's options
     * @return true if the commandLine is approved, false otherwise
     */
    public static boolean validateArgs(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        return  validateDatasetIdArg(cmd, options) &&
            validateFileToPublishArg(cmd, options) &&
            validatePublishMethodArg(cmd, options) &&
            validateProxyArgs(cmd, options);
    }

    /**
     * @return an error JobStatus if any input is invalid, otherwise JobStatus.VALID
     */
    public static JobStatus validateJobParams(SocrataConnectionInfo connectionInfo, GISJob job) {
        if (connectionInfo.getUrl().equals("") || connectionInfo.getUrl().equals("https://")) {
            return JobStatus.INVALID_DOMAIN;
        }

        if (!Utils.uidIsValid(job.getDatasetID())) {
            return JobStatus.INVALID_DATASET_ID;
        }

        String fileToPublish = job.getFileToPublish();
        if (fileToPublish.equals("")) {
            return JobStatus.MISSING_FILE_TO_PUBLISH;
        }

        File publishFile = new File(fileToPublish);
        if (!publishFile.exists() || publishFile.isDirectory()) {
            JobStatus errorStatus = JobStatus.FILE_TO_PUBLISH_DOESNT_EXIST;
            errorStatus.setMessage(fileToPublish + ": File to publish does not exist");
            return errorStatus;
        }

        String fileExtension = Utils.getFileExtension(fileToPublish);
        if (!allowedGeoFileToPublishExtensions.contains(fileExtension)) {
            return JobStatus.FILE_TO_PUBLISH_INVALID_FORMAT;
        }

        return JobStatus.VALID;
    }

    public static JobStatus validateDataset(UserPreferences userPrefs, String datasetID) {
        try {
            DatasetUtils.getDatasetInfo(userPrefs, datasetID, GeoDataset.class);
            return JobStatus.VALID;
        } catch (IllegalArgumentException e) {
            // This means getDatasetInfo was unable to parse the response into a GeoDataset object.
            return JobStatus.NOT_A_GEO_DATASET;
        } catch (Exception e) {
            // there’s no way for the client to recover,
            // so a checked exception is not necessary
            throw new RuntimeException(e);
        }
    }

    private static boolean validateFileToPublishArg(CommandLine cmd, CommandLineOptions options) {
        if (cmd.getOptionValue(CommandLineOptions.FILE_TO_PUBLISH_FLAG) != null) {
            return true;
        } else {
            System.err.println("Missing required argument: -f,--" + CommandLineOptions.FILE_TO_PUBLISH_FLAG + " is required");
            return false;
        }
    }

    private static boolean validateDatasetIdArg(CommandLine cmd, CommandLineOptions options) {
        if (cmd.getOptionValue(CommandLineOptions.DATASET_ID_FLAG) != null) {
            return true;
        } else {
            System.err.println("Missing required argument: -i,--" + CommandLineOptions.DATASET_ID_FLAG + " is required");

            return false;
        }
    }

    private static boolean validatePublishMethodArg(CommandLine cmd, CommandLineOptions options) {
        String method = cmd.getOptionValue("m");
        String publishingWithDi2 = cmd.getOptionValue(CommandLineOptions.PUBLISH_VIA_DI2_FLAG);
        String publishingWithFtp = cmd.getOptionValue(CommandLineOptions.PUBLISH_VIA_FTP_FLAG);
        String controlFilePath = cmd.getOptionValue(CommandLineOptions.PATH_TO_CONTROL_FILE_FLAG);

        if (method == null && controlFilePath == null
            && isNullOrFalse(publishingWithFtp) && isNullOrFalse(publishingWithDi2)) {

            System.err.println("Missing required argument: -m,--" +
                    CommandLineOptions.PUBLISH_METHOD_FLAG + " is required");

            return false;
        } else if (method == null && controlFilePath == null
                   && (!isNullOrFalse(publishingWithFtp) || !isNullOrFalse(publishingWithDi2))) {
            // if publishing via ftp or di2/http, we want to err about the control file, not the method arg

            return true;
        } else if (method == null && controlFilePath != null) {
            // have a control file, don't need the method arg (and would ignore it anyway)

            return true;
        } else { // method != null
            boolean publishMethodValid = false;

            for (PublishMethod m : PublishMethod.values()) {
                if (m.name().equalsIgnoreCase(method))
                    publishMethodValid = true;
            }

            if (!publishMethodValid) {
                System.err.println("Invalid argument: -m,--" + CommandLineOptions.PUBLISH_METHOD_FLAG + " must be " +
                                   Arrays.toString(PublishMethod.values()));
                return false;
            }

            return true;
        }
    }

    private static boolean validateProxyArgs(CommandLine cmd, CommandLineOptions options) {
        String username = cmd.getOptionValue(CommandLineOptions.PROXY_USERNAME_FLAG);
        String password = cmd.getOptionValue(CommandLineOptions.PROXY_PASSWORD_FLAG);
        if(username == null && password != null) {
            System.err.println("Missing required argument: -pun,--" + CommandLineOptions.PROXY_USERNAME_FLAG + " is required if" +
                               " supplying proxy credentials with -ppw, --" + CommandLineOptions.PROXY_PASSWORD_FLAG);
            return false;
        } else if(username != null && password == null) {
            System.err.println("Missing required argument: -ppw,--" + CommandLineOptions.PROXY_PASSWORD_FLAG + " is required if" +
                               " supplying proxy credentials with -pun, --" + CommandLineOptions.PROXY_USERNAME_FLAG);
            return false;
        }
        return true;
    }

    private static boolean isNullOrFalse(String s) {
        return s == null || s.equalsIgnoreCase("false");
    }
}
