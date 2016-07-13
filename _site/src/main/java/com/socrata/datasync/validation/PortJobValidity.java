package com.socrata.datasync.validation;

import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.Utils;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.job.PortJob;
import org.apache.commons.cli.CommandLine;

import java.util.Arrays;

public class PortJobValidity {

    public static boolean validateArgs(CommandLine cmd) {
        // TODO Validate optional parameters
        return validatePortMethodArg(cmd) &&
                validateSourceDomainArg(cmd) &&
                validateSourceIdArg(cmd) &&
                validateDestinationDomainArg(cmd);
    }

    public static JobStatus validateJobParams(SocrataConnectionInfo connectionInfo, PortJob job) {
        if (connectionInfo.getUrl().equals("") || connectionInfo.getUrl().equals("https://"))
            return JobStatus.INVALID_DOMAIN;

        if (!Utils.uidIsValid(job.getSourceSetID()))
            return JobStatus.INVALID_DATASET_ID;

        if (job.getPortMethod().equals(PortMethod.copy_data) && !Utils.uidIsValid(job.getSinkSetID()))
            return JobStatus.INVALID_DATASET_ID;

        if (job.getSourceSiteDomain().equals("") || job.getSourceSiteDomain().equals("https://"))
            return JobStatus.INVALID_DOMAIN;

        if (job.getSinkSiteDomain().equals("") || job.getSinkSiteDomain().equals("https://"))
            return JobStatus.INVALID_DOMAIN;

        boolean okayMethod = false;
        for (PortMethod method : PortMethod.values()) {
            if (method.equals(job.getPortMethod()))
                okayMethod = true;
        }
        if (!okayMethod)
            return JobStatus.INVALID_PORT_METHOD;

        if (!job.getPublishMethod().equals(PublishMethod.upsert)
                && !job.getPublishMethod().equals(PublishMethod.replace))
            return JobStatus.INVALID_PUBLISH_METHOD;

        return JobStatus.SUCCESS;
    }

    private static boolean validateDestinationDomainArg(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        if(cmd.getOptionValue(options.DESTINATION_DOMAIN_FLAG) == null) {
            System.err.println("Missing required argument: -pd2,--" + options.DESTINATION_DOMAIN_FLAG + " is required");
            return false;
        }
        return true;
    }

    private static boolean validateSourceIdArg(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        if(cmd.getOptionValue(options.SOURCE_DATASET_ID_FLAG) == null) {
            System.err.println("Missing required argument: -pi1,--" + options.SOURCE_DATASET_ID_FLAG + " is required");
            return false;
        }
        return true;
    }

    private static boolean validateSourceDomainArg(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        if(cmd.getOptionValue(options.SOURCE_DOMAIN_FLAG) == null) {
            System.err.println("Missing required argument: -pd1,--" + options.SOURCE_DOMAIN_FLAG + " is required");
            return false;
        }
        return true;
    }

    private static boolean validatePortMethodArg(CommandLine cmd) {
        CommandLineOptions options = new CommandLineOptions();
        String portMethod = cmd.getOptionValue(options.PORT_METHOD_FLAG);
        if(portMethod == null) {
            System.err.println("Missing required argument: -pm,--" + options.PORT_METHOD_FLAG + " is required");
            return false;
        }

        boolean portMethodValid = false;
        for(PortMethod m : PortMethod.values()) {
            if(portMethod.equals(m.name()))
                portMethodValid = true;
        }
        if(!portMethodValid) {
            System.err.println("Invalid argument: -pm,--" + options.PORT_METHOD_FLAG + " must be: " +
                    Arrays.toString(PortMethod.values()));
            return false;
        }
        return true;
    }
}
