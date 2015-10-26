package com.socrata.datasync.model;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;


import javax.swing.table.AbstractTableModel;
import au.com.bytecode.opencsv.CSVReader;
import com.socrata.datasync.config.controlfile.ControlFile;

/**
 * The CSV Model is used primarily to drive the previews in the mapping panel, as well as for validation
 * of key failure points (e.g. date formatting).  As discussed in the DatasetModel class, the actual column names
 * are for display purposes only.  The DatasetModel maintains a separate list of invariant columns that it manages
 * independent of the values in the CSV.
 */

public class CSVModel extends AbstractTableModel{

    private String[] columnNames;
    final int rowsToSample = 100;

    private Vector data = new Vector();

    public CSVModel(ControlFile file) throws IOException
    {
        updateTable(file);
    }

    public void updateTable(ControlFile file) throws IOException {
        data.removeAllElements();
        updateColumnNames(file);
        addSamples(file);
    }


    //Return rows added
    private int addSamples(ControlFile controlFile) throws IOException{
        CSVReader reader = getCSVReader(controlFile, controlFile.getFileTypeControl().skip);
        String [] row =  reader.readNext();

        int rowsAdded = 0;
        while (row != null && rowsAdded < rowsToSample){
            // The consumers of this class assume a table with an equal number of columns in every row.
            // If the row is blank, we'll need to get a placeholder with as many columns as the others to allow the
            // control file editor the ability to load.
            if (isBlankRow(row)){
                insertData(getBlankPlaceholderRow(getColumnCount()));
            }
            else {
                insertData(row);
            }
            rowsAdded++;
            row = reader.readNext();
        }

        return rowsAdded;
    }

    private boolean isBlankRow(String[] row){
        return row.length == 1 && row[0].isEmpty();
    }

    // This method will create a dummy row with as many columns as exist in the rest of the dataset.
    // This will allow the control file editor to load, and the customer to skip the first couple of rows
    // by setting the "skip" option under "advanced options"
    private String[] getBlankPlaceholderRow(int columns){
        String[] placeholder = new String[columns];
        return placeholder;
    }

    private CSVReader getCSVReader(ControlFile controlFile, int skip) throws IOException{
        String path = controlFile.getFileTypeControl().filePath;
        String encoding = controlFile.getFileTypeControl().encoding;
        char sep = controlFile.getFileTypeControl().separator.charAt(0);
        char quote = controlFile.getFileTypeControl().quote.charAt(0);
        char escape = '\u0000';

        InputStreamReader inputReader = new InputStreamReader(new FileInputStream(controlFile.getFileTypeControl().filePath), controlFile.getFileTypeControl().encoding);

        if (controlFile.getFileTypeControl().escape != null ){
            if (controlFile.getFileTypeControl().escape.equals(""))
                escape = '\u0000';
            else
                escape = controlFile.getFileTypeControl().escape.charAt(0);
        }
        CSVReader reader = new CSVReader(inputReader,
                sep,
                quote,
                escape,
                skip);

        return reader;
    }

    private void updateColumnNames(ControlFile file) throws IOException {
        boolean hasHeaderRow = file.getFileTypeControl().hasHeaderRow;
        CSVReader headerReader = getCSVReader(file, 0);
        String[] row = headerReader.readNext();

        if (hasHeaderRow) {
            columnNames = row;
        }
        else{
            columnNames = generatePlaceholderNames(row.length);
        }
        fireTableStructureChanged();
    }

    private String[] generatePlaceholderNames(int columnCount){
        String[] placeholders = new String[columnCount];
        for (int i = 0; i < columnCount; i++){
            placeholders[i] = ModelUtils.generatePlaceholderName(i);
        }
        return placeholders;
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    public int getRowSize(int row){
        return ((Vector) data.get(row)).size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        return ((Vector) data.get(row)).get(col);
    }

    public String getColumnName(int col){
        return columnNames[col];
    }
    public Class getColumnClass(int c){
        return getValueAt(0,c).getClass();
    }

    public void setValueAt(Object value, int row, int col){
        ((Vector) data.get(row)).setElementAt(value, col);
        fireTableCellUpdated(row,col);
    }

    public String getColumnPreview(int columnIndex, int itemsToPreview){
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < Math.min(itemsToPreview,getRowCount()); i++){
            Object value = "N/A";
            try
            {
                value = getValueAt(i,columnIndex);
            }
            catch (ArrayIndexOutOfBoundsException e){
                System.out.println("Row contains different number of columns than expected.  Rows with missing data will be displayed as \"N/A\" in the map fields dialog");
            }
            buf.append(value);
            if (i+1 <  Math.min(itemsToPreview,getRowCount()))
                buf.append(", ");
        }
        return buf.toString();
    }

    public boolean isCellEditable(int row, int col){
        return false;
    }

    private void insertData(Object[] values){
        data.add(new Vector());
        for(int i =0; i<values.length; i++){
            ((Vector) data.get(data.size()-1)).add(values[i]);
        }
        fireTableDataChanged();
    }

}

