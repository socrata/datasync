package com.socrata.datasync.config;

import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.job.Jobs;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.util.Arrays;

public class CommandLineOptions {

    public static final String JOB_TYPE_FLAG = "jobType";
    public static final String CONFIG_FLAG = "config";
    public static final String USER_AGENT_FLAG = "userAgent";

    public static final String DATASET_ID_FLAG = "datasetID";
    public static final String FILE_TO_PUBLISH_FLAG = "fileToPublish";
    public static final String PUBLISH_METHOD_FLAG = "publishMethod";
    public static final String HAS_HEADER_ROW_FLAG = "fileToPublishHasHeaderRow";
    public static final String PUBLISH_VIA_FTP_FLAG = "publishViaFTP";
    public static final String PUBLISH_VIA_DI2_FLAG = "publishViaHttp";
    // TODO: remove at some point, ftp_control is deprecated in favor of control
    public static final String PATH_TO_FTP_CONTROL_FILE_FLAG = "pathToFTPControlFile";
    public static final String PATH_TO_CONTROL_FILE_FLAG = "pathToControlFile";
    public static final String PROXY_USERNAME_FLAG = "proxyUsername";
    public static final String PROXY_PASSWORD_FLAG = "proxyPassword";

    public static final String PORT_METHOD_FLAG = "portMethod";
    public static final String SOURCE_DOMAIN_FLAG = "sourceDomain";
    public static final String SOURCE_DATASET_ID_FLAG = "sourceDatasetId";
    public static final String DESTINATION_DOMAIN_FLAG = "destinationDomain";
    public static final String DESTINATION_DATASET_ID_FLAG = "destinationDatasetId";
    public static final String PORT_PUBLISH_METHOD = "portPublishMethod";
    public static final String PUBLISH_DESTINATION_DATASET_FLAG = "publishDestinationDataset";
    public static final String DESTINATION_DATASET_TITLE_FLAG = "destinationDatasetTitle";

    public static final String DEFAULT_JOBTYPE = Jobs.INTEGRATION_JOB.toString();
    public static final String DEFAULT_PUBLISH_VIA_FTP = "false";
    public static final String DEFAULT_PUBLISH_VIA_DI2 = "false";
    public static final String DEFAULT_PORT_PUBLISH_METHOD = PublishMethod.upsert.toString();
    public static final String DEFAULT_PUBLISH_DESTINATION_DATASET = "false";

    private static CommandLineParser parser = new PosixParser();

    public static final Options options = new Options();
    static {
        options.addOption("t", JOB_TYPE_FLAG, true, "Type of job to run: " + Arrays.toString(Jobs.values()) + " (default: " + DEFAULT_JOBTYPE  + ")");
        options.addOption("c", CONFIG_FLAG, true, ".json file that stores global preferences (authentication details, etc) (optional)");
        options.addOption("a", USER_AGENT_FLAG, true, "User-Agent string passed when making HTTPS calls (optional)");

        // IntegrationJob params
        options.addOption("i", DATASET_ID_FLAG, true, "Dataset ID to publish to [IntegrationJob]");
        options.addOption("f", FILE_TO_PUBLISH_FLAG, true, "CSV or TSV file to publish [IntegrationJob]");
        options.addOption("m", PUBLISH_METHOD_FLAG, true, "Publish method (" + Arrays.toString(PublishMethod.values()) + ") [IntegrationJob]");
        options.addOption("h", HAS_HEADER_ROW_FLAG, true, "File to publish has header row (true or false) [IntegrationJob]");
        options.addOption("pf", PUBLISH_VIA_FTP_FLAG, true, "Use FTP (instead of HTTP) for publishing (true or false) (default: " + DEFAULT_PUBLISH_VIA_FTP + ") [IntegrationJob]");
        options.addOption("ph", PUBLISH_VIA_DI2_FLAG, true, "Use HTTP for publishing (true or false) (default: false) [IntegrationJob]");
        options.addOption("sc", PATH_TO_FTP_CONTROL_FILE_FLAG, true, "deprecated! please use -cf, --pathToControlFile instead");
        options.addOption("cf", PATH_TO_CONTROL_FILE_FLAG, true, "control.json file, needed for publishing via FTP or HTTP;" +
                "if set overrides job parameters [IntegrationJob]");
        options.addOption("pun", PROXY_USERNAME_FLAG, true, "The username to supply to connect to the proxy server [IntegrationJob]");
        options.addOption("ppw", PROXY_PASSWORD_FLAG, true, "The password to supply to connect to the proxy server [IntegrationJob]");

        // PortJob params
        options.addOption("pm", PORT_METHOD_FLAG, true, "Port method (" + Arrays.toString(PortMethod.values()) + ") [PortJob]");
        options.addOption("pd1", SOURCE_DOMAIN_FLAG, true, "Source Domain [PortJob]");
        options.addOption("pi1", SOURCE_DATASET_ID_FLAG, true, "Source Dataset ID [PortJob]");
        options.addOption("pd2", DESTINATION_DOMAIN_FLAG, true, " Destination Domain [PortJob]");
        options.addOption("pi2", DESTINATION_DATASET_ID_FLAG, true, "Destination Dataset ID (only use when sourceDomain is 'copy_data') [PortJob]");
        options.addOption("ppm", PORT_PUBLISH_METHOD, true, "Data Porting Publish Method (upsert or replace) (default: " + DEFAULT_PORT_PUBLISH_METHOD + ") [PortJob]");
        options.addOption("pp",  PUBLISH_DESTINATION_DATASET_FLAG, true, "Publish Destination Dataset (true or false) (default: " + DEFAULT_PUBLISH_DESTINATION_DATASET + ") [PortJob]");
        options.addOption("pdt", DESTINATION_DATASET_TITLE_FLAG, true, "Destination Dataset Title (optional) [PortJob]");

        options.addOption("?", "help", false, "Help");
        options.addOption("v", "version", false, "DataSync version");
    }

    public static CommandLine getCommandLine(String[] args) throws ParseException, ParseException {
        return parser.parse(options, args);
    }
}
