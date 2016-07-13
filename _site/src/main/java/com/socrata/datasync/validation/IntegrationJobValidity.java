package com.socrata.datasync.validation;

import com.socrata.api.SodaImporter;
import com.socrata.datasync.DatasetUtils;
import com.socrata.datasync.HttpUtility;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.Utils;
import com.socrata.datasync.config.CommandLineOptions;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.controlfile.FileTypeControl;
import com.socrata.datasync.config.controlfile.LocationColumn;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.job.JobStatus;
import com.socrata.model.importer.Dataset;
import org.apache.commons.cli.CommandLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.codehaus.jackson.map.ObjectMapper;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class IntegrationJobValidity {

    public static final List<String> allowedFileToPublishExtensions = Arrays.asList("csv", "tsv");
    public static final List<String> allowedControlFileExtensions = Arrays.asList("json");
    public static final String supportedTimeFormat = "ISO8601";
    public static final String jodaLink = "http://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html";
    public static final String charsetsPath = "/datasync/charsets.json";

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
                validateHeaderRowArg(cmd, options) &&
                validatePublishViaFtpArg(cmd, options) &&
                validatePublishViaDi2HttpArg(cmd, options) &&
                validatePathToControlFileArg(cmd, options) &&
                validateProxyArgs(cmd, options);
    }

    /**
     * @return an error JobStatus if any input is invalid, otherwise JobStatus.VALID
     */
    public static JobStatus validateJobParams(SocrataConnectionInfo connectionInfo, IntegrationJob job) {
        if(connectionInfo.getUrl().equals("") || connectionInfo.getUrl().equals("https://"))
            return JobStatus.INVALID_DOMAIN;

        if(!Utils.uidIsValid(job.getDatasetID()))
            return JobStatus.INVALID_DATASET_ID;

        String fileToPublish = job.getFileToPublish();
        if(fileToPublish.equals(""))
            return JobStatus.MISSING_FILE_TO_PUBLISH;

        File publishFile = new File(fileToPublish);
        if(!publishFile.exists() || publishFile.isDirectory()) {
            JobStatus errorStatus = JobStatus.FILE_TO_PUBLISH_DOESNT_EXIST;
            errorStatus.setMessage(fileToPublish + ": File to publish does not exist");
            return errorStatus;
        }

        String fileExtension = Utils.getFileExtension(fileToPublish);
        if(!allowedFileToPublishExtensions.contains(fileExtension))
            return JobStatus.FILE_TO_PUBLISH_INVALID_FORMAT;

        final SodaImporter importer = SodaImporter.newImporter(connectionInfo.getUrl(), connectionInfo.getUser(), connectionInfo.getPassword(), connectionInfo.getToken());
        Dataset schema;
        try {
            schema = (Dataset) importer.loadDatasetInfo(job.getDatasetID());

            if(job.getPublishViaDi2Http() || job.getPublishViaFTP()) {

                ControlFile control = job.getControlFile();
                FileTypeControl fileControl = null;
                switch (fileExtension) {
                    case "csv": fileControl = control.getCsvFtc(); break;
                    case "tsv": fileControl = control.getTsvFtc(); break;
                }
                String action = control.action;
                //See if it was passed on the command line
                if (action == null)
                    action = job.getPublishMethod().name();
                JobStatus actionOkay = checkAction(action, job, schema);
                if (actionOkay.isError())
                    return actionOkay;

                JobStatus controlOkay = checkControl(control,fileControl,schema,publishFile,connectionInfo.getUrl());
                if (controlOkay.isError())
                    return controlOkay;

            }
        } catch (Exception e) {
            // Not going to fail jobs on the validation check
        }
        return JobStatus.VALID;
    }

    public static JobStatus checkControl(ControlFile control,FileTypeControl fileControl, Dataset schema, File publishFile, String urlBase){

        String fileExtension = Utils.getFileExtension(publishFile.getAbsolutePath());


        if (fileControl == null && !control.action.equalsIgnoreCase(PublishMethod.delete.name())) {
            JobStatus noFileTypeContent = JobStatus.PUBLISH_ERROR;
            noFileTypeContent.setMessage("The control file for '" + publishFile.getName() +
                    "' requires that the '" + fileExtension + "' option be filled in");
            return noFileTypeContent;
        }

        try {
            String[] headers = getHeaders(fileControl, publishFile);
            if (headers == null) {
                JobStatus noHeaders = JobStatus.PUBLISH_ERROR;
                noHeaders.setMessage("Headers must be specified in one of " + publishFile.getName() + " or the control file using 'columns'");
                return noHeaders;
            }

            PublishMethod method = PublishMethod.valueOf(control.action.toLowerCase());
            JobStatus csvDatasetAgreement = checkColumnAgreement(fileControl, method, schema, headers ,publishFile.getName());
            if (csvDatasetAgreement.isError())
                return csvDatasetAgreement;

            JobStatus controlHeaderAgreement = checkControlAgreement(fileControl, schema, headers, publishFile.getName());
            if (controlHeaderAgreement.isError())
                return controlHeaderAgreement;

            JobStatus controlSensibility = validateControlFile(fileControl, urlBase);
            if (controlSensibility.isError())
                return controlSensibility;
        }
        catch (IOException e){
            JobStatus cannotConnect = JobStatus.PUBLISH_ERROR;
            cannotConnect.setMessage("Cannot determine the headers for dataset: " + schema.getId());
            return cannotConnect;
        }
        return JobStatus.VALID;

    }

    private static boolean validatePathToControlFileArg(CommandLine cmd, CommandLineOptions options) {
        String controlFilePath = cmd.getOptionValue(options.PATH_TO_CONTROL_FILE_FLAG);
        if (controlFilePath == null) controlFilePath = cmd.getOptionValue(options.PATH_TO_FTP_CONTROL_FILE_FLAG);

        if(controlFilePath != null) {
            String publishingWithFtp = cmd.getOptionValue(options.PUBLISH_VIA_FTP_FLAG);
            String publishingWithDi2 = cmd.getOptionValue(options.PUBLISH_VIA_DI2_FLAG);
            if (isNullOrFalse(publishingWithFtp) && isNullOrFalse(publishingWithDi2)) {
                System.err.println("Invalid argument: Neither -sc,--" + options.PATH_TO_FTP_CONTROL_FILE_FLAG +
                        " -cf,--" + options.PATH_TO_CONTROL_FILE_FLAG + " can be supplied " +
                        "unless -pf,--" + options.PUBLISH_VIA_FTP_FLAG + " is 'true' or " +
                        "unless -ph,--" + options.PUBLISH_VIA_DI2_FLAG + " is 'true'");
                return false;
            }
        }

        if(controlFilePath != null) {
            if(cmd.getOptionValue(options.HAS_HEADER_ROW_FLAG) != null) {
                System.out.println("WARNING: -h,--" + options.HAS_HEADER_ROW_FLAG + " is being ignored because " +
                        "-sc,--" + options.PATH_TO_FTP_CONTROL_FILE_FLAG +  " or " +
                        "-cf,--" + options.PATH_TO_CONTROL_FILE_FLAG +  " was supplied");
            }
        }
        return true;
    }

    private static boolean validatePublishViaFtpArg(CommandLine cmd, CommandLineOptions options) {
        String publishingWithFtp = cmd.getOptionValue(options.PUBLISH_VIA_FTP_FLAG);
        if (publishingWithFtp == null)
            return true;

        if (!publishingWithFtp.equalsIgnoreCase("true") && !publishingWithFtp.equalsIgnoreCase("false")) {
            System.err.println("Invalid argument: -pf,--" + options.PUBLISH_VIA_FTP_FLAG + " must be 'true' or 'false'");
            return false;
        }
        if (publishingWithFtp.equalsIgnoreCase("true")) {
            String publishingWithDi2 = cmd.getOptionValue(options.PUBLISH_VIA_DI2_FLAG);
            if (publishingWithDi2 != null && publishingWithDi2.equalsIgnoreCase("true")) {
                System.err.println("Only one of -pf,--" + options.PUBLISH_VIA_DI2_FLAG + " and " +
                        "-ph,--" + options.PUBLISH_VIA_DI2_FLAG + " may be set to 'true'");
                return false;
            }
            String controlFilePath = cmd.getOptionValue(options.PATH_TO_CONTROL_FILE_FLAG);
            if (controlFilePath == null) controlFilePath = cmd.getOptionValue(options.PATH_TO_FTP_CONTROL_FILE_FLAG);
            if (controlFilePath == null) {
                System.err.println("A control file must be specified when " +
                        "-pf,--" + options.PUBLISH_VIA_FTP_FLAG + " is set to 'true'");
                return false;
            }
        }
        return true;
    }

    private static boolean validatePublishViaDi2HttpArg(CommandLine cmd, CommandLineOptions options) {
        String publishingWithDi2 = cmd.getOptionValue(options.PUBLISH_VIA_DI2_FLAG);
        if (publishingWithDi2 == null)
            return true;

        if(!publishingWithDi2.equalsIgnoreCase("true") && !publishingWithDi2.equalsIgnoreCase("false")) {
            System.err.println("Invalid argument: -pf,--" + options.PUBLISH_VIA_DI2_FLAG + " must be 'true' or 'false'");
            return false;
        }
        if (publishingWithDi2.equalsIgnoreCase("true")) {
            String publishingWithFtp = cmd.getOptionValue(options.PUBLISH_VIA_FTP_FLAG);
            if (publishingWithFtp != null && publishingWithFtp.equalsIgnoreCase("true")) {
                System.err.println("Only one of -pf,--" + options.PUBLISH_VIA_DI2_FLAG + " and " +
                        "-ph,--" + options.PUBLISH_VIA_DI2_FLAG + " may be set to 'true'");
                return false;
            }
            String controlFilePath = cmd.getOptionValue(options.PATH_TO_CONTROL_FILE_FLAG);
            if (controlFilePath == null) controlFilePath = cmd.getOptionValue(options.PATH_TO_FTP_CONTROL_FILE_FLAG);
            if (controlFilePath == null) {
                System.err.println("A control file must be specified when " +
                        "-ph,--" + options.PUBLISH_VIA_DI2_FLAG + " is set to 'true'");
                return false;
            }
        }
        return true;
    }

    private static boolean validateHeaderRowArg(CommandLine cmd, CommandLineOptions options) {
        String haveHeader = cmd.getOptionValue(options.HAS_HEADER_ROW_FLAG);
        String publishingWithDi2 = cmd.getOptionValue(options.PUBLISH_VIA_DI2_FLAG);
        String publishingWithFtp = cmd.getOptionValue(options.PUBLISH_VIA_FTP_FLAG);
        String controlFilePath = cmd.getOptionValue(options.PATH_TO_CONTROL_FILE_FLAG);
        if(haveHeader == null) {
            if (controlFilePath == null) {
                if (isNullOrFalse(publishingWithFtp) && isNullOrFalse(publishingWithDi2)) {
                    System.err.println("Missing required argument: -h,--" + options.HAS_HEADER_ROW_FLAG + " is required");
                    return false;
                } else {
                    // if publishing via ftp or di2/http, we want to err about the control file, not the header arg
                    return true;
                }
            } else { // have a control file, don't need the header arg (and would ignore it anyway)
                return true;
            }
        } else {  // have non-null header arg
            if (!cmd.getOptionValue(options.HAS_HEADER_ROW_FLAG).equalsIgnoreCase("true")
                    && !cmd.getOptionValue(options.HAS_HEADER_ROW_FLAG).equalsIgnoreCase("false")) {
                System.err.println("Invalid argument: -h,--" + options.HAS_HEADER_ROW_FLAG + " must be 'true' or 'false'");
                return false;
            }
            return true;
        }
    }

    private static boolean validateFileToPublishArg(CommandLine cmd, CommandLineOptions options) {
        if(cmd.getOptionValue(options.FILE_TO_PUBLISH_FLAG) == null) {
            System.err.println("Missing required argument: -f,--" + options.FILE_TO_PUBLISH_FLAG + " is required");
            return false;
        }
        return true;
    }

    private static boolean validateDatasetIdArg(CommandLine cmd, CommandLineOptions options) {
        if(cmd.getOptionValue(options.DATASET_ID_FLAG) == null) {
            System.err.println("Missing required argument: -i,--" + options.DATASET_ID_FLAG + " is required");
            return false;
        }
        return true;
    }

    private static boolean validatePublishMethodArg(CommandLine cmd, CommandLineOptions options) {
        String method = cmd.getOptionValue("m");
        String publishingWithDi2 = cmd.getOptionValue(options.PUBLISH_VIA_DI2_FLAG);
        String publishingWithFtp = cmd.getOptionValue(options.PUBLISH_VIA_FTP_FLAG);
        String controlFilePath = cmd.getOptionValue(options.PATH_TO_CONTROL_FILE_FLAG);
        if(method == null) {
            if (controlFilePath == null) {
                if (isNullOrFalse(publishingWithFtp) && isNullOrFalse(publishingWithDi2)) {
                    System.err.println("Missing required argument: -m,--" + options.PUBLISH_METHOD_FLAG + " is required");
                    return false;
                } else {
                    // if publishing via ftp or di2/http, we want to err about the control file, not the method arg
                    return true;
                }
            } else { // have a control file, don't need the method arg (and would ignore it anyway)
                return true;
            }
        } else {  // non-null method given
            boolean publishMethodValid = false;
            for (PublishMethod m : PublishMethod.values()) {
                if (m.name().equalsIgnoreCase(method))
                    publishMethodValid = true;
            }
            if (!publishMethodValid) {
                System.err.println("Invalid argument: -m,--" + options.PUBLISH_METHOD_FLAG + " must be " +
                        Arrays.toString(PublishMethod.values()));
                return false;
            }
            return true;
        }
    }

    private static boolean validateProxyArgs(CommandLine cmd, CommandLineOptions options) {
        String username = cmd.getOptionValue(options.PROXY_USERNAME_FLAG);
        String password = cmd.getOptionValue(options.PROXY_PASSWORD_FLAG);
        if(username == null && password != null) {
            System.err.println("Missing required argument: -pun,--" + options.PROXY_USERNAME_FLAG + " is required if" +
                    " supplying proxy credentials with -ppw, --" + options.PROXY_PASSWORD_FLAG);
            return false;
        } else if(username != null && password == null) {
            System.err.println("Missing required argument: -ppw,--" + options.PROXY_PASSWORD_FLAG + " is required if" +
                    " supplying proxy credentials with -pun, --" + options.PROXY_USERNAME_FLAG);
            return false;
        }
        return true;
    }

    private static String[] getHeaders(FileTypeControl fileControl, File csvOrTsvFile) throws IOException {
        String[] columns = fileControl.columns;
        if (columns == null) {
            int skip = fileControl.skip == null ? 0 : fileControl.skip;
            columns = Utils.pullHeadersFromFile(csvOrTsvFile, fileControl, skip);
        }

        if (fileControl.hasIgnoredColumns()) {
            ArrayList<String> ignoredColumns = new ArrayList<>(Arrays.asList(fileControl.ignoreColumns));
            if (columns.length < ignoredColumns.size()) {
                throw new ArrayIndexOutOfBoundsException("You cannot ignore more columns than are present in your file");
            }
            String[] headers = new String[columns.length - ignoredColumns.size()];
            int i = 0;
            for (String header: columns) {
                if (!ignoredColumns.contains(header)) {
                    headers[i] = header;
                    i++;
                }
            }
            return headers;
        } else {
            return columns;
        }
    }

    private static JobStatus validateControlFile(FileTypeControl fileControl, String urlBase) {

        if (fileControl == null) return JobStatus.VALID;

        JobStatus goodTimestampFormats = checkTimeFormattingValidity(fileControl);
        if (goodTimestampFormats.isError())
            return goodTimestampFormats;

        JobStatus goodEncoding = checkEncodingValidity(fileControl, urlBase);
        if (goodEncoding.isError())
            return goodEncoding;

        return JobStatus.VALID;
    }

    private static JobStatus checkAction(String action, IntegrationJob job, Dataset schema) {
        StringBuilder methods = new StringBuilder();
        boolean okAction = false;
        for (PublishMethod m : PublishMethod.values()) {
            methods.append("\t" + m.name() + "\n");
            if (m.name().equalsIgnoreCase(action))
                okAction = true;
        }
        if (!okAction) {
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Unknown Publish Method: " +
                    "The control file must specify the publishing method via the 'action' option as one of: \n" +
                    methods.toString());
            return status;
        }
        if (!PublishMethod.replace.name().equalsIgnoreCase(action) && job.getPublishViaFTP()) {
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("FTP does not currently support upsert, append or delete");
            return status;
        }
        PublishMethod publishMethod = job.getPublishMethod();
        if (publishMethod != null && !action.equalsIgnoreCase(publishMethod.name())) {
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Conflicting Publish Methods: " +
                    "The publish method selected was '" + publishMethod.name() +
                    "', but the 'action' option in the control file specifies the publish method as '" + action + ".");
            return status;
        }
        String rowIdentifier = DatasetUtils.getRowIdentifierName(schema);
        if (rowIdentifier == null && PublishMethod.delete.name().equalsIgnoreCase(action)) {
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage("Dataset Requirement Unfulfilled: " +
                    "To delete from a dataset, a row identifier must be set. Dataset '" + schema.getId() +
                    "' does not have a row identifier set");
            return status;
        }
        return JobStatus.VALID;
    }

    private static JobStatus checkTimeFormattingValidity(FileTypeControl fileControl) {
        if (!fileControl.hasTimestampFormatting())
            return JobStatus.VALID;

        Set<String> timeFormats = fileControl.lookupTimestampFormatting();
        DateTimeFormatter formatter;
        for (String format : timeFormats) {
            if (format.equalsIgnoreCase(supportedTimeFormat))
                continue;
            try { formatter = DateTimeFormat.forPattern(format); }
            catch (IllegalArgumentException e) {
                JobStatus status = JobStatus.PUBLISH_ERROR;
                status.setMessage("Unsupported Date Time Format: The time format '" + format +
                        "' specified in the control file is not a valid pattern." +
                        "\nPlease consult " + jodaLink + " for more information");
                return status;
            }
        }

        return JobStatus.VALID;
    }

    private static JobStatus checkEncodingValidity(FileTypeControl fileControl, String urlBase) {
        if (!fileControl.hasEncoding())
            return JobStatus.VALID;

        String encoding = fileControl.encoding;
        if (encoding.equalsIgnoreCase("ISO-8859-1") ||
                encoding.equalsIgnoreCase("UTF-8"))
            return JobStatus.VALID;

        HttpUtility http = new HttpUtility();
        String charsetUri = urlBase + charsetsPath;
        ObjectMapper mapper = new ObjectMapper();
        try(CloseableHttpResponse response = http.get(new URI(charsetUri), ContentType.APPLICATION_JSON.getMimeType())) {
            String[] charsets = mapper.readValue(response.getEntity().getContent(), String[].class);
            boolean encodingFound = false;
            for (String charset : charsets) {
                if (encoding.equalsIgnoreCase(charset)) {
                    encodingFound = true;
                    break;
                }
            }
            if (!encodingFound) {
                JobStatus status = JobStatus.PUBLISH_ERROR;
                status.setMessage("Unsupported Encoding: The encoding '" + encoding + "' in the control file is not supported." +
                        "\nPlease consult " + charsetUri + " for a listing of supported encodings");
                return status;
            }
        } catch (Exception e) {
            // no reason to fail jobs because of encoding check
        }
        return JobStatus.VALID;
    }

    /**
     * This check compares the information found in the control file to the headers and dataset schema.
     * We check 3 things:
     *   1) that the headers don't already contain any synthetic locations
     *   2) that the components of each synthetic location are present in the headers
     *   3) that the datatypes used in a synthetic location column are supported types
     * @param schema the soda-java Dataset that provides dataset info
     * @param headers the columns that will be uploaded; taken from either the file or the control
     *                file and minus the ignoredColumns
     * @param csvFilename the file name, for printing purposes.
     * @return
     */
    private static JobStatus checkControlAgreement(FileTypeControl fileControl, Dataset schema, String[] headers, String csvFilename) {

        if (schema == null || fileControl == null || !fileControl.hasSyntheticLocations()) return JobStatus.VALID;

        Map<String, LocationColumn> syntheticLocations = fileControl.syntheticLocations;
        for (String field : syntheticLocations.keySet()) {     // O(N) N = 1,2 (small, probably)
            LocationColumn location = syntheticLocations.get(field);
            String[] locationComponents = new String[]{location.address, location.city,
                    location.state, location.zip, location.latitude, location.longitude};
            boolean locationInFile = false;
            boolean[] componentsInFile = new boolean[]{location.address == null, location.city == null,
                    location.state == null, location.zip == null, location.latitude == null, location.longitude == null};
            for (String header : headers) {     // O(M)  M = a couple dozen?
                if (field.equalsIgnoreCase(header)) {
                    locationInFile = true;
                    break;
                }
                for (int j = 0; j < componentsInFile.length; j++) {   // O(1)  constant at 6
                    if (!componentsInFile[j])
                        componentsInFile[j] = header.equalsIgnoreCase(locationComponents[j]);
                }
            }
            if (locationInFile) {
                JobStatus status = JobStatus.PUBLISH_ERROR;
                status.setMessage("Ambiguous Column Name: Synthetic location '" + field + "' specified in the control file may conflict with '" +
                        field + "' provided in '" + csvFilename + "'." +
                        "\nPlease ensure '" + field + "' is not currently mapped to fields in the CSV.");
                return status;
            }

            //The documentation says that this should work, so I'm commenting it out

//            for (int i = 0; i < componentsInFile.length; i++) {
//                if (!componentsInFile[i]) {
//                    JobStatus status = JobStatus.PUBLISH_ERROR;
//                    status.setMessage("Synthetic Location Not Found: The synthetic location column '" + field +
//                            "' references a component '" + locationComponents[i] + "' which is not present in '" +
//                            csvFilename + "'." +
//                            "\nPlease check your control file to ensure that the column name is spelled correctly, " +
//                            "and that '" + locationComponents[i] + "' is not included in the 'ignoreColumns' array.");
//                    return status;
//                }
//            }
            JobStatus typesSupported = locationTypesSupported(field, location, DatasetUtils.getDatasetTypeMapping(schema));
            if (typesSupported.isError())
                return typesSupported;
        }

        return JobStatus.VALID;
    }

    /**
     * checks that the datatypes associated with each synthetic location component column are supported
     * @param location a LocationColumn
     * @param typeMapping a map from dataset column field names to their type
     * @return true if each component column has a supported type; false if any have an unsupported type
     */
    private static JobStatus locationTypesSupported(String locationName, LocationColumn location, Map<String,String> typeMapping) {
        Map<String,String> componentColumns = location.findComponentColumns();
        for (String component : componentColumns.keySet()){
            String fieldname = componentColumns.get(component);
            String[] supportedTypes;
            if (component.equalsIgnoreCase("zip") || component.equalsIgnoreCase("latitude") || component.equalsIgnoreCase("longitude")) {
                supportedTypes = new String[]{"text", "number"};
            } else {
                supportedTypes = new String[]{"text"};
            }
            String unsupportedType = getUnsupportedType(fieldname, supportedTypes, typeMapping);
            if (unsupportedType != null) {
                JobStatus status = JobStatus.PUBLISH_ERROR;
                StringBuilder message = new StringBuilder("Unsupported Datatype: " + Utils.capitalizeFirstLetter(component) + " component  '" +
                        locationName + "' is of type '" + unsupportedType + "'; " + component + " components require ");
                if (supportedTypes.length == 1) message.append(" a ");
                message.append("'" + supportedTypes[0] + "'");
                for (int i=1; i<supportedTypes.length; i++)
                    message.append(" or '" + supportedTypes[i] + "'");
                message.append(" column");
                if (supportedTypes.length > 1) message.append('s');
                message.append(".\nPlease specify a column that matches a supported type.");
                status.setMessage(message.toString());
                return status;
            }
        }
        return  JobStatus.VALID;
    }

    /**
     * returns null if the component type is okay, otherwise the bad type.
     */
    private static String getUnsupportedType(String component, String[] supportedTypes, Map<String, String> typeMapping) {
        String componentType = typeMapping.get(component);
        if (component == null || componentType == null)
            return null;

        for (String okayType: supportedTypes) {
            if (okayType.equalsIgnoreCase(componentType))
                return null;
        }
        return componentType;
    }

    /**
     * This check compares the information found in the headers to the columns found in the dataset.
     * We check 3 things:
     *   1) that the headers have a row identifier column if the dataset does
     *   2) that the headers don't contain columns not in the dataset
     *   3) that the headers aren't missing columns in the dataset IF there is no row identifier
     * @param schema the soda-java Dataset that provides dataset info
     * @param headers the columns that will be uploaded; taken from either the file or the control
     *                file and minus the ignoredColumns
     * @param csvFilename the file name, for printing purposes.
     * @return
     */
    private static JobStatus checkColumnAgreement(FileTypeControl fileControl, PublishMethod method, Dataset schema, String[] headers, String csvFilename) {

        String[] ignoredColumns = fileControl.ignoreColumns;

        if (schema == null) return JobStatus.VALID;

        String rowIdentifier = DatasetUtils.getRowIdentifierName(schema);
        Set<String> columnNames = DatasetUtils.getFieldNamesSet(schema);

        boolean headerHasRowId = false;
        for (String field : headers) {
            field = field.toLowerCase();
            if (field.equalsIgnoreCase(rowIdentifier)) headerHasRowId = true;

            if (!columnNames.contains(field)) {
                JobStatus status = JobStatus.PUBLISH_ERROR;
                status.setMessage("Extra Columns Specified: File '" + csvFilename + "' contains column '" + field +
                        "', but dataset '" + schema.getId() + "' does not." +
                        "\nPlease check that your headers are using the field name rather than the human readable name." +
                        "\nConsider using 'ignoreColumns' in the control file if '" + field +
                        "' should not be included in the dataset");
                return status;
            } else {
                columnNames.remove(field);
            }
        }
        for (String ignored : ignoredColumns) {
            if (columnNames.contains(ignored))
                columnNames.remove(ignored);
        }


        Map<String,LocationColumn> syntheticColumnsMap =  fileControl.syntheticLocations;

        if (syntheticColumnsMap != null) {
            Set<String> syntheticColumns = syntheticColumnsMap.keySet();
            for (String synthetic : syntheticColumns) {
                if (columnNames.contains(synthetic))
                    columnNames.remove(synthetic);
            }
        }
        if (columnNames.size() > 0 && method.equals(PublishMethod.replace)) {
            if (rowIdentifier == null) {
                JobStatus status = JobStatus.MISSING_COLUMNS;
                StringBuilder message = new StringBuilder("Missing Fields: Dataset " + schema.getId() +
                        " contains the following field(s) that are not mapped: ");//available in '" + csvFilename + "': ");
                boolean writtenFirstValue = false;
                for (String colName : columnNames) {
                    if (writtenFirstValue)
                        message.append(", ").append(colName);
                    else{
                        message.append(colName);
                        writtenFirstValue = true;
                    }
                }
                status.setMessage(message.toString());
                return status;
            } else if (!headerHasRowId) {
                JobStatus status = JobStatus.PUBLISH_ERROR;
                status.setMessage("Missing Row Identifier: Dataset '" + schema.getId() + "' contains a row identifier in column '" + rowIdentifier +
                        "'. This column must be present in '" + csvFilename + "'.");
                return status;
            }
        }
        return JobStatus.VALID;
    }

    private static boolean isNullOrFalse(String s) {
        return s == null || s.equalsIgnoreCase("false");
    }

}
