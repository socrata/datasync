package com.socrata.datasync.model;

import au.com.bytecode.opencsv.CSVReader;
import com.socrata.api.Soda2Consumer;
import com.socrata.datasync.DatasetUtils;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import java.io.StringReader;

import javax.print.URIException;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import org.apache.http.HttpException;
import java.util.ArrayList;
import java.util.Vector;


/**
 * Created by franklinwilliams on 8/4/14.
 */
public class DatasetModel extends AbstractTableModel {

    //private String[] columnNames;

    private ArrayList<Column> columns;
    int rowsToSample = 100;
    Dataset datasetInfo;
    String domain;

    private Vector data = new Vector();

    public DatasetModel(UserPreferences prefs,
            String fourbyfour) throws LongRunningQueryException, InterruptedException, HttpException, IOException, URISyntaxException{

        this.domain = prefs.getDomain();

        initializeDataset(prefs, fourbyfour);
    }

    //Used to pull the charset for this domain
    public String getDomain(){
        return domain;
    }

    private boolean initializeDataset(UserPreferences prefs, String fourbyfour) throws LongRunningQueryException, InterruptedException, HttpException, IOException, URISyntaxException{
        datasetInfo = DatasetUtils.getDatasetInfo(prefs,fourbyfour);

        columns = (ArrayList) datasetInfo.getColumns();

        String csv = DatasetUtils.getDatasetSample(prefs,fourbyfour,rowsToSample);

        CSVReader reader = new CSVReader(new StringReader(csv));

        String[] lines = reader.readNext();

        while (lines != null){
            insertData(lines);
            lines = reader.readNext();
        }
        return true;
    }

    // Returns the number of columns of type location.  Used in the view to determine whether or not we should suggest
    // creating synthetic columns
    public int getLocationCount(){
        int locationCount = 0;
        for (Column c : columns){
            if (c.getDataTypeName().equals("location"))
                locationCount++;
        }
        return locationCount;
    }

    public Dataset getDatasetInfo(){
        return datasetInfo;
    }

    public Column getColumnByFieldName(String name){
        for (Column c : columns){
            if (c.getFieldName().equalsIgnoreCase(name))
                return c;
        }
        return null;
    }

    public Column getColumnByFriendlyName(String name){
        for (Column c : columns){
            if (c.getName().equalsIgnoreCase(name))
                return c;
        }
        return null;
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
        return ((Vector) data.get(row)).get(col);
    }

    public ArrayList<Column> getColumns(){
        return columns;
    }

    public String getColumnName(int col){
        return columns.get(col).getFieldName();
    }
    public Class getColumnClass(int c){
        return getValueAt(0,c).getClass();
    }

    public void setValueAt(Object value, int row, int col){
        ((Vector) data.get(row)).setElementAt(value, col);
        fireTableCellUpdated(row,col);
    }

    public boolean isCellEditable(int row, int col){
        return false;
    }

    public void insertData(Object[] values){
        data.add(new Vector());
        for(int i =0; i<values.length; i++){
            ((Vector) data.get(data.size()-1)).add(values[i]);
        }
        fireTableDataChanged();
    }
}
