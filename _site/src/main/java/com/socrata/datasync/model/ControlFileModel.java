package com.socrata.datasync.model;

import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.controlfile.LocationColumn;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.validation.IntegrationJobValidity;
import com.socrata.model.importer.Column;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 * This class maintains a model for the DataSync control file editor.  The model provides
 * a facility through which the UI can map each column in the CSV to a column in the dataset,
 * either by assigning the dataset column name to an the array whose items match the order in which
 * the columns exist in the CSV, or by adding the dataset column name to the ignored columns list.
 *
 * The model takes care of generating columns names from the CSV. All updates are then based on the index of the
 * values in the CSV. Consumers update the model by providing the dataset field name, and the index of the column in the CSV
 * from which the data should be pulled.  For example, if I have a dataset "car_registration" whose fields are
 * "make,model,year" and a CSV whose values are "Dodge,Durango,2000," and the customer wants to map "make" to the first
 * column in the CSV, then they will simply call "updateColumnAtPosition" passing "make" and the index "0".  The model
 * will ensure the control file is updated appropriately.  Note that we are assuming a fixed set of columns throughout.
 * Any changes to the shape of the original columns array should invalidate the rest of the model.
 *
 * The model also provides a facility for validation, leveraging the existing IntegrationJob
 * validation checks.
 *
 * Created by franklinwilliams
 */

public class ControlFileModel extends Observable {

    private ControlFile controlFile;
    private CSVModel csvModel;
    private DatasetModel datasetModel;
    private String path;
    final String fieldSeparator = ",";

    public ControlFileModel (ControlFile file, DatasetModel dataset) throws IOException{
        controlFile = file;
        if (controlFile.getFileTypeControl().hasHeaderRow)
            controlFile.getFileTypeControl().skip = 1;
        csvModel = new CSVModel(file);
        this.datasetModel = dataset;

        // Check to see if the ControlFile already has initialized columns.
        // If it doesn't, then initialize the columns list with dummy names. Note that we use dummy names here
        // instead of names from the CSV since there is no guarentee that the names in the CSV will be valid.  They
        // could be duplicates, as well as empty strings.
        if (!file.getFileTypeControl().hasColumns()){
            initializeColumns();
        }
        // Now attempt to match those in the dataset to those in the CSV
        matchColumns();
    }

    //This will be called anytime that we think the shape of the dataset has changed underneath us.
    private void initializeColumns(){
        controlFile.getFileTypeControl().columns = new String[csvModel.getColumnCount()];
        for (int i = 0; i < getColumnCount(); i++){
            controlFile.getFileTypeControl().columns[i] = ModelUtils.generatePlaceholderName(i);
        }

        // Now ignore all of the columns by default (this is another loop to avoid the callbacks resulting in out of bounds
        // exceptions before the array is fully initialized
        for (int i = 0; i < getColumnCount(); i++){
            ignoreColumnInCSVAtPosition(i);
        }
    }

    public DatasetModel getDatasetModel(){
        return datasetModel;
    }

    public ControlFile getControlFile(){
        return controlFile;
    }

    private void matchColumns(){
        for (int i = 0; i < csvModel.getColumnCount(); i++) {
            String csvHeader = csvModel.getColumnName(i);
            //TODO: I'm running over the dataset column list probably an unnecessary number of times.  Consider fixing later
            Column col = datasetModel.getColumnByFieldName(csvHeader);
            //If we can't match on field name, check for a match on the friendly name
            if (col == null){
                col = datasetModel.getColumnByFriendlyName(csvHeader);
            }
            if (col != null)
                updateColumnAtPosition(col.getFieldName(), i);
        }
    }

    private void removeIgnoredColumn(String columnName){
        String[] ignoredColumns = controlFile.getFileTypeControl().ignoreColumns;
        if (ignoredColumns != null) {
            ArrayList<String> newColumns = new ArrayList<String>();
            for (String c : ignoredColumns) {
                if (!columnName.equals(c))
                    newColumns.add(c);
            }
            controlFile.getFileTypeControl().ignoreColumns(newColumns.toArray(new String[newColumns.size()]));
        }
    }

    public int getColumnCount(){
        return controlFile.getFileTypeControl().columns.length;
    }

    public void ignoreColumnFromDataset(Column column){
        String fieldName = column.getFieldName();
        ArrayList<String> newColumns = new ArrayList<String>(Arrays.asList(controlFile.getFileTypeControl().ignoreColumns));
        newColumns.add(fieldName);
        controlFile.getFileTypeControl().ignoreColumns(newColumns.toArray(new String[newColumns.size()]));
        updateListeners();
    }

    public void ignoreColumnInCSVAtPosition(int index){
        if (index >  getColumnCount())
            throw new IllegalStateException("Cannot update field outside of the CSV");
        String columnName = getColumnAtPosition(index);
        ArrayList<String> newColumns = new ArrayList<String>(Arrays.asList(controlFile.getFileTypeControl().ignoreColumns));
        if (!newColumns.contains(columnName))
            newColumns.add(columnName);
        controlFile.getFileTypeControl().ignoreColumns(newColumns.toArray(new String[newColumns.size()]));
        updateListeners();
    }

    public boolean isIgnored(String fieldName){
        String[] ignoredColumns = controlFile.getFileTypeControl().ignoreColumns;
        if (ignoredColumns != null) {
            for (String column : ignoredColumns) {
                if (column.equals(fieldName))
                    return true;
            }
        }
        return false;
    }

    public int getIndexOfColumnName(String fieldName){
        String [] columns = controlFile.getFileTypeControl().columns;
        for (int i = 0; i < columns.length; i++){
            if (columns[i].equalsIgnoreCase(fieldName))
                return i;
        }
        return -1;
    }

    public void updateColumnAtPosition(String datasetFieldName, int position){
        if (position >  getColumnCount())
            throw new IllegalStateException("Cannot update field outside of the CSV");
        int index = getIndexOfColumnName(datasetFieldName);
        // The same column cannot be mapped twice.  If the column is already mapped, set the mapped version to be ignored
        if (index != -1 && index != position){
            controlFile.getFileTypeControl().columns[index] = ModelUtils.generatePlaceholderName(index);
            ignoreColumnInCSVAtPosition(index);
        }

        removeIgnoredColumn(getColumnAtPosition(position));
        removeSyntheticColumn(datasetFieldName);
        controlFile.getFileTypeControl().columns[position] = datasetFieldName;

        updateListeners();
    }

    public void removeSyntheticColumn(String fieldName){
        if (controlFile.getFileTypeControl().syntheticLocations != null)
            controlFile.getFileTypeControl().syntheticLocations.remove(fieldName);
        updateListeners();
    }

    public String getColumnAtPosition(int i){
        if (i > getColumnCount())
            throw new IllegalStateException("Cannot update field outside of the CSV");
        return controlFile.getFileTypeControl().columns[i];
    }

    public CSVModel getCsvModel() {
        return csvModel;
    }

    public void setEmptyTextIsNull(boolean isNull){
        controlFile.getFileTypeControl().emptyTextIsNull(isNull);
        updateListeners();
    }

    public void updateListeners(){
        //TODO: Should we really be swallowing this exception from here?  Given the current way it's factored, I think
        // that we will need to...
        try {
            csvModel.updateTable(controlFile);
        }
        catch (IOException e){
            System.out.println(e.getStackTrace());
        }
        setChanged();
        notifyObservers();
    }

    public void setSeparator(String sep){
        controlFile.getFileTypeControl().separator(sep);
        //This is likely to change the number of columns in the dataset.  Reset the columns
        initializeColumns();
        updateListeners();
    }

    public void setRowsToSkip(int rowsToSkip){
        controlFile.getFileTypeControl().skip(rowsToSkip);
        updateListeners();
    }

    public void setEncoding(String encoding){
        controlFile.getFileTypeControl().encoding(encoding);
        updateListeners();
    }

    public void setQuote(String quote){
        controlFile.getFileTypeControl().quote(quote);
        updateListeners();
    }

    public void setType(String type){
        controlFile.action = type;
        updateListeners();
    }

    public void setTrimWhiteSpace(boolean trim){
        controlFile.getFileTypeControl().trimServerWhitespace(trim);
        updateListeners();
    }

    public void setUseSocrataGeocoding(boolean useSocrataGeocoding){
        controlFile.getFileTypeControl().useSocrataGeocoding(useSocrataGeocoding);
        updateListeners();
    }

    public void setEscape(String escape){
        controlFile.getFileTypeControl().escape(escape);
        updateListeners();
    }

    public void setHasHeaderRow(boolean headerRow){
        boolean existingValue = controlFile.getFileTypeControl().hasHeaderRow;
        if (headerRow && !existingValue)
            controlFile.getFileTypeControl().skip(controlFile.getFileTypeControl().skip + 1);
        if (!headerRow && existingValue)
            controlFile.getFileTypeControl().skip(controlFile.getFileTypeControl().skip - 1);
        controlFile.getFileTypeControl().hasHeaderRow(headerRow);
        updateListeners();
    }

    public void setSocrataGeocoding(boolean socrataGeocoding){
       controlFile.getFileTypeControl().useSocrataGeocoding(socrataGeocoding);
        updateListeners();
    }

    // Returns the friendliest possible name - The CSV if it exists, the dummy name if it doesn't
    // Intended only for display.  If you attempt to use this for indexing, you're likely going to break things
    // as everything is done off of the index
    public String getDisplayName(int i){
        if (controlFile.getFileTypeControl().hasHeaderRow)
            return getCsvModel().getColumnName(i);
        else
            return getColumnAtPosition(i);
    }

    public String getStringFromArray(String[] array){
        StringBuffer strbuf = new StringBuffer();
        for (int i = 0; i < array.length; i++){
            strbuf.append(array[i]);
            if (i+1 != array.length)
                strbuf.append(fieldSeparator);
        }
         return strbuf.toString();
    }

    public String getFloatingDateTime(){
        return getStringFromArray(controlFile.getFileTypeControl().floatingTimestampFormat);
    }

    public String getTimezone(){
        return controlFile.getFileTypeControl().timezone;
     }

    public void setFixedDateTime(String fixed){
        String[] newDateTime = fixed.split(fieldSeparator);
        controlFile.getFileTypeControl().fixedTimestampFormat(newDateTime);
        updateListeners();
    }

    public void setFloatingDateTime(String floating){
        String[] newDateTime = floating.split(fieldSeparator);
        controlFile.getFileTypeControl().floatingTimestampFormat(newDateTime);
        updateListeners();
    }

    public void setTimezone(String timezone){
        controlFile.getFileTypeControl().timezone(timezone);
        updateListeners();
    }

    public void setSetAsideErrors(Boolean setAsideErrors){
        controlFile.getFileTypeControl().setAsideErrors(setAsideErrors);
        updateListeners();
    }

    public void setSyntheticLocation(String fieldName, LocationColumn locationField) {
        Map<String, LocationColumn> columnsMap = controlFile.getFileTypeControl().syntheticLocations;
        if (columnsMap != null)
            controlFile.getFileTypeControl().syntheticLocations.put(fieldName, locationField);
        else {
            HashMap<String, LocationColumn> map = new HashMap();
            map.put(fieldName, locationField);
            controlFile.getFileTypeControl().syntheticLocations = map;
        }
        //Reset the location column
        int locationIndex = getIndexOfColumnName(fieldName);
        if (locationIndex != -1)
            ignoreColumnInCSVAtPosition(locationIndex);
        updateListeners();
    }

    //Return the empty set if there are no synthetic locaitons
    public Map<String, LocationColumn> getSyntheticLocations(){
        Map<String, LocationColumn> locations = controlFile.getFileTypeControl().syntheticLocations;
        if (locations == null)
            locations = new HashMap<>();
        return locations;
    }

    public ArrayList<Column> getUnmappedDatasetColumns(){
        ArrayList<Column> unmappedColumns = new ArrayList<Column>();
        for (Column datasetColumn : datasetModel.getColumns()){
            // If the column doesn't exist in the mapped columns list, and it hasn't already been explicitly ignored
            // then we should add it to the list of unmapped columns
            String fieldName = datasetColumn.getFieldName();
            if (getIndexOfColumnName(fieldName) == -1
                    && !isIgnored(fieldName))
                unmappedColumns.add(datasetColumn);
        }
        return unmappedColumns;
    }

    public String getControlFileContents()  {
        ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
        try {
            return mapper.writeValueAsString(controlFile);
        } catch (IOException e) {
            return null;
        }
    }

    //Get the path from which this control file was loaded.
    public String getPath(){
        return path;
    }


    public JobStatus validate(){
        if (!rowsContainSameNumberOfColumns())
            return JobStatus.ROWS_DO_NOT_CONTAIN_SAME_NUMBER_OF_COLUMNS;

            JobStatus status = IntegrationJobValidity.checkControl(controlFile,controlFile.getFileTypeControl(),datasetModel.getDatasetInfo(),new File(controlFile.getFileTypeControl().filePath),datasetModel.getDomain());
        if (status.isError()){
            return status;
        }
        else
            return checkDateTime();
    }

    //Sample the rows in the CSV and attempt to parse the columns that represent dates.
    private JobStatus checkDateTime(){
        for (int i = 0; i < csvModel.getRowCount(); i++){
            for (int j = 0; j < controlFile.getFileTypeControl().columns.length; j++) {
                String columnName = controlFile.getFileTypeControl().columns[j];
                Column c = datasetModel.getColumnByFieldName(columnName);

                if (c != null) {
                    String fieldType = c.getDataTypeName();
                    if (fieldType.equals("calendar_date") ||
                            fieldType.equals("date")) {
                        Object value = csvModel.getValueAt(i, j);
                        if (!canParseDateTime(value, controlFile.getFileTypeControl().floatingTimestampFormat)) {
                            JobStatus status = JobStatus.INVALID_DATETIME;
                            status.setMessage("Cannot parse the datetime value \"" +value.toString()+  "\" in column \"" + columnName+ "\" given the current formatting.  Please check your formatting values under advanced options and try again.");
                            return status;
                        }
                    }
                }
            }
        }
        return JobStatus.VALID;
    }

    //Blanks and nulls are included as parseable
    private boolean canParseDateTime(Object value, String[] dateTimeFormats){
        if (value == null || value.toString().isEmpty())
            return true;

        for (String format : dateTimeFormats) {
            try {
                if (format.startsWith("ISO"))
                    ISODateTimeFormat.dateTime().parseDateTime((String) value);
                else {
                    DateTimeFormatter dateStringFormat = DateTimeFormat.forPattern(format);
                    dateStringFormat.parseDateTime((String) value);
                }
                //If we make it here, then we know that we've been able to parse the value
                return true;
            } catch (Exception e) {
                continue;
            }
        }
        return false;
    }

    public boolean rowsContainSameNumberOfColumns()
    {
        int columnLength = csvModel.getColumnCount();
        for (int i = 0; i < csvModel.getRowCount(); i++){
            if (columnLength != csvModel.getRowSize(i))
                return false;
        }
        return true;
    }

}
