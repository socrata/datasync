package com.socrata.datasync.model;

import au.com.bytecode.opencsv.CSVReader;
import com.socrata.api.Soda2Consumer;
import com.socrata.datasync.DatasetUtils;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Column;
import com.socrata.model.importer.Dataset;
import com.socrata.model.soql.SoqlQuery;
import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Consumer;
import com.socrata.api.Soda2Producer;
import com.socrata.api.SodaDdl;
import com.socrata.builders.SoqlQueryBuilder;
import java.io.StringReader;
import com.sun.jersey.api.client.ClientResponse;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.io.StringReader;
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

    public DatasetModel(
            String domain,
            String username,
            String password,
            String appToken,
            String fourbyfour) throws LongRunningQueryException, InterruptedException, SodaError, IOException{

        this.domain = domain;
        SodaDdl ddl = SodaDdl.newDdl(domain,username,password,appToken);
        Soda2Consumer consumer = Soda2Consumer.newConsumer(domain,
                username,
                password,
                appToken);

        initializeDataset(ddl,consumer, fourbyfour);
    }

    //Used to pull the charset for this domain
    public String getDomain(){
        return domain;
    }

    private boolean initializeDataset(SodaDdl ddl, Soda2Consumer consumer, String fourbyfour) throws LongRunningQueryException, InterruptedException, SodaError, IOException{
        datasetInfo = (Dataset) ddl.loadDatasetInfo(fourbyfour);

        columns = (ArrayList) datasetInfo.getColumns();

        SoqlQueryBuilder builder = new SoqlQueryBuilder(SoqlQuery.SELECT_ALL).setLimit(rowsToSample);
        ClientResponse response = consumer.query(fourbyfour, HttpLowLevel.CSV_TYPE,builder.build());

        String csv = response.getEntity(String.class);
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
