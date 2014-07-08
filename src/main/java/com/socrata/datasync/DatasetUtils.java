package com.socrata.datasync;

import com.socrata.api.SodaDdl;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DatasetUtils {

    private static final String LOCATION_DATATYPE_NAME = "location";

    /**
     * Retruns the field name of the row identifier, if there is one, else null
     */
    public static String getRowIdentifierName(Dataset schema) {
        Column rowIdentifierColumn = schema.lookupRowIdentifierColumn();
        if (rowIdentifierColumn == null) {
            return null;
        } else {
            return rowIdentifierColumn.getFieldName();
        }
    }

    /**
     * Returns list of dataset field names in the form: "col1","col2",...
     *
     * @param ddl
     * @param datasetId
     * @return list of field names or null if there
     */
    public static String getFieldNamesString(SodaDdl ddl, String datasetId) throws SodaError, InterruptedException {
        Dataset datasetInfo = (Dataset) ddl.loadDatasetInfo(datasetId);
        return getFieldNamesString(datasetInfo);
    }

    /**
     * Returns list of dataset field names in the form: "col1","col2",...
     *
     * @param datasetInfo
     * @return list of field names or null if there are none
     */
    public static String getFieldNamesString(Dataset datasetInfo) {
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
    public static String[] getFieldNamesArray(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        String[] columnsArray = new String[columns.size()];
        for(int i = 0; i < columns.size(); i++) {
            columnsArray[i] = columns.get(i).getFieldName();
        }
        return columnsArray;
    }

    /**
     * Returns the set of dataset field names.
     *
     * @param datasetInfo
     * @return array of field names or null if there are none
     */
    public static Set<String> getFieldNamesSet(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        Set<String> fields = new HashSet<>();
        for(int i = 0; i < columns.size(); i++) {
            fields.add(columns.get(i).getFieldName());
        }
        return fields;
    }

    /**
     * Returns a mapping of dataset field names to their type.
     *
     * @param datasetInfo
     * @return a map of dataset field names to datatypes
     */
    public static Map<String,String> getDatasetTypeMapping(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        Map<String,String> mapping = new HashMap<>();
        for(Column c : columns) {
            mapping.put(c.getFieldName(), c.getDataTypeName());
        }
        return mapping;
    }

    /**
     * @return true if given dataset has one or more Location columns, false otherwise
     */
    public static boolean hasLocationColumn(Dataset datasetInfo) {
        List<Column> columns = datasetInfo.getColumns();
        for(int i = 0; i < columns.size(); i++) {
            if(columns.get(i).getDataTypeName().equals(LOCATION_DATATYPE_NAME))
                return true;
        }
        return false;
    }
}
