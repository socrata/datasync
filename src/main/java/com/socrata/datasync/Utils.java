package com.socrata.datasync;

import com.socrata.api.SodaDdl;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {

    private static final String LOCATION_DATATYPE_NAME = "location";

    /**
     * Get file extension from the given path to a file
     * @param file filename
     * @return
     */
    public static String getFileExtension(String file) {
        String extension = "";
        int i = file.lastIndexOf('.');
        if (i > 0)
            extension = file.substring(i+1).toLowerCase();
        return extension;
    }

    /**
     * Returns a random 32 character request id
     */
    public static String generateRequestId() {
        String uuid = UUID.randomUUID().toString();
        String requestId = uuid.replace("-", "");
        return requestId;
    }

    /**
     * @param uid to validate
     * @return true if given uid is a valid Socrata uid (e.g. abcd-1234)
     */
    public static boolean uidIsValid(String uid) {
        Matcher uidMatcher = Pattern.compile("[a-z0-9]{4}-[a-z0-9]{4}").matcher(uid);
        return uidMatcher.matches();
    }

    /**
     * Returns list of dataset field names in the form: "col1","col2",...
     *
     * @param ddl
     * @param datasetId
     * @return list of field names or null if there
     */
    public static String getDatasetFieldNamesString(SodaDdl ddl, String datasetId) throws SodaError, InterruptedException {
        Dataset datasetInfo = (Dataset) ddl.loadDatasetInfo(datasetId);
        return getDatasetFieldNamesString(datasetInfo);
    }

    /**
     * Returns list of dataset field names in the form: "col1","col2",...
     *
     * @param datasetInfo
     * @return list of field names or null if there are none
     */
    public static String getDatasetFieldNamesString(Dataset datasetInfo) {
        String columnsValue = "";
        List<Column> columns = datasetInfo.getColumns();
        for(int i = 0; i < columns.size(); i++) {
            if(i > 0)
                columnsValue += ",";
            columnsValue += "\"" + columns.get(i).getFieldName() + "\"";
        }
        return columnsValue;
    }

    /**
     * Returns an array of dataset field names.
     *
     * @param datasetInfo
     * @return array of field names or null if there are none
     */
    public static String[] getDatasetFieldNames(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        String[] columnsArray = new String[columns.size()];
        for(int i = 0; i < columns.size(); i++) {
            columnsArray[i] = columns.get(i).getFieldName();
        }
        return columnsArray;
    }


    /**
     * @return true if given dataset has one or more Location columns, false otherwise
     */
    public static boolean datasetHasLocationColumn(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        for(int i = 0; i < columns.size(); i++) {
            if(columns.get(i).getDataTypeName().equals(LOCATION_DATATYPE_NAME))
                return true;
        }
        return false;
    }

    /**
     * Open given uri in local web browser
     * @param uri to open in browser
     */
    public static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                System.out.println("Error: cannot open web page");
            }
        }
    }

    /**
     * @param pathToSaveJobFile path to a saved job file
     * @return command with absolute paths to execute job file at given path
     */
    public static String getRunJobCommand(String pathToSaveJobFile) {
        String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            jarPath = URLDecoder.decode(jarPath, "UTF-8");
            // Needed correct issue with windows where path includes a leading slash
            if(jarPath.contains(":") && (jarPath.startsWith("/") || jarPath.startsWith("\\"))) {
                jarPath = jarPath.substring(1, jarPath.length());
            }
            //TODO: This may change based on how we implement running metadata jobs from the command line.
            return "java -jar " + jarPath + " " + pathToSaveJobFile;
        } catch (UnsupportedEncodingException unsupportedEncoding) {
            return "Error getting path to this executeable: " + unsupportedEncoding.getMessage();
        }
    }

}
