package com.socrata.datasync.ui;

import com.socrata.datasync.config.controlfile.LocationColumn;
import com.socrata.datasync.model.CSVModel;
import com.socrata.datasync.model.ControlFileModel;
import com.socrata.model.importer.Column;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A dialog box which allows the customer to pick from columns in a CSV and then map them into
 * a location column
 *
 * Created by franklinwilliams
 */
public class SyntheticLocationDialog extends JDialog {
    Map<String,LocationColumn> syntheticColumnsCopy;
    ControlFileModel model;
    JComboBox addressCombo;
    JComboBox cityCombo;
    JComboBox zipCombo;
    JComboBox stateCombo;
    JComboBox latCombo;
    JComboBox lonCombo;
    JButton buttonOK;
    JButton buttonCancel;
    JLabel headerLabel;
    JComboBox activeLocation;

    public SyntheticLocationDialog(ControlFileModel model, JFrame parent, Map<String, LocationColumn> syntheticColumns, String initFieldName, String title) { //}, String message){
        super(parent, title);
        this.model = model;
        //Copy the columns so that playing around with them in the dialog doesn't accidentially change the underlying model.
        this.syntheticColumnsCopy = copySyntheticColumns(syntheticColumns);


        setLocationRelativeTo(null);
        setModal(true);
        initComponents();
        layoutComponents();
        if (initFieldName != null)
            activeLocation.setSelectedItem(initFieldName);

        pack();
        setVisible(true);
    }

    //Make a copy of the columns so that we don't change the control file just by launching the dialog.
    private Map<String, LocationColumn> copySyntheticColumns(Map<String,LocationColumn> original){
        Map<String, LocationColumn> newSet = new HashMap<String, LocationColumn>();
        for (String str : original.keySet()){
            newSet.put(str,original.get(str));
        }
        return newSet;
    }

    private void initComponents() {

        headerLabel = new JLabel();
        headerLabel.setText("Select the fields that make up the column");
        buttonOK = new JButton();
        buttonOK.setText("OK");

        buttonCancel = new JButton();
        buttonCancel.setText("Cancel");

        buttonCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        });

        buttonOK.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                commitUpdates();
                setVisible(false);
                dispose();
            }
        });

        addressCombo = getComboBox();
        cityCombo = getComboBox();
        stateCombo = getComboBox();
        zipCombo = getComboBox();
        latCombo = getComboBox();
        lonCombo = getComboBox();

        activeLocation = getLocationsCombobox();
        activeLocation.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    updateSyntheticComponentComboboxes();
                }
            }
        });

        updateSyntheticComponentComboboxes();
    }


    private void updateSyntheticComponentComboboxes(){
        LocationColumn locationColumn = getActiveLocation();

        if (locationColumn.address != null)
            addressCombo.setSelectedItem(locationColumn.address);
            addressCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                LocationColumn col = getActiveLocation();
                SyntheticComboBoxItem item =  (SyntheticComboBoxItem) e.getItem();
                col.address = model.getColumnAtPosition(item.getIndex());
            }
        });

        if (locationColumn.city != null)
            cityCombo.setSelectedItem(locationColumn.city);
            cityCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                LocationColumn col = getActiveLocation();
                SyntheticComboBoxItem item =  (SyntheticComboBoxItem) e.getItem();
                col.city = model.getColumnAtPosition(item.getIndex());
            }
        });


        if (locationColumn.state != null)
            stateCombo.setSelectedItem(locationColumn.state);
            stateCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                LocationColumn col = getActiveLocation();
                SyntheticComboBoxItem item =  (SyntheticComboBoxItem) e.getItem();
                col.state = model.getColumnAtPosition(item.getIndex());
            }
        });


        if (locationColumn.zip != null)
            zipCombo.setSelectedItem(locationColumn.zip);
            zipCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                LocationColumn col = getActiveLocation();
                SyntheticComboBoxItem item =  (SyntheticComboBoxItem) e.getItem();
                col.zip = model.getColumnAtPosition(item.getIndex());
            }
        });

        if (locationColumn.latitude != null)
            latCombo.setSelectedItem(locationColumn.latitude);
            latCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                LocationColumn col = getActiveLocation();
                SyntheticComboBoxItem item =  (SyntheticComboBoxItem) e.getItem();
                col.latitude = model.getColumnAtPosition(item.getIndex());
            }
        });

        if (locationColumn.longitude != null)
            lonCombo.setSelectedItem(locationColumn.longitude);
            lonCombo.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                LocationColumn col = getActiveLocation();
                SyntheticComboBoxItem item =  (SyntheticComboBoxItem) e.getItem();
                col.longitude = model.getColumnAtPosition(item.getIndex());
            }
        });
    }

    private void layoutComponents() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(10, 10, 10, 10), -1, -1));
        contentPane.setMaximumSize(new Dimension(315, 291));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel1.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        panel2.add(buttonOK, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        panel2.add(buttonCancel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel3.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.add(addressCombo, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        panel4.add(cityCombo, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        panel4.add(stateCombo, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        panel4.add(zipCombo, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        panel4.add(latCombo, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        panel4.add(lonCombo, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(50, -1), null, 1, false));
        final JLabel label1 = new JLabel();
        label1.setText("Address");
        panel5.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("City");
        panel5.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("State");
        panel5.add(label3, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Zip");
        panel5.add(label4, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Latitude");
        panel5.add(label5, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Longitude");
        panel5.add(label6, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));

        headerLabel.setText("Select the location column to configure");

        panel6.add(headerLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel6.add(activeLocation, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        contentPane.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, 1, new Dimension(-1, 5), new Dimension(-1, 5), new Dimension(-1, 5), 3, false));
        getContentPane().add(contentPane);
    }

    // Get a combobox whose values are all of the possible fields in the control file that could be mapped to this column
    private JComboBox getComboBox() {
        JComboBox columnNamesComboBox = new JComboBox();
        for (int i = 0; i < model.getColumnCount();i++){
            String friendlyName = model.getDisplayName(i);
            columnNamesComboBox.addItem(new SyntheticComboBoxItem(friendlyName,i));
        }
        columnNamesComboBox.setSelectedIndex(-1);
        return columnNamesComboBox;
    }

    private JComboBox getLocationsCombobox(){
        ArrayList<Column> columns = model.getDatasetModel().getColumns();
        JComboBox locationComboBox = new JComboBox();

        for (Column column : columns) {
            if (column.getDataTypeName().equals("location")) {
                locationComboBox.addItem(column.getFieldName()); //new ColumnItem(column,null)); //column.getName());// = new JComboBox(datasetModel.getColumns());
            }
        }
        locationComboBox.setSelectedIndex(0);
        return locationComboBox;
    }

    private LocationColumn getActiveLocation() {
        String fieldName = (String) activeLocation.getSelectedItem();
        LocationColumn locationColumn = syntheticColumnsCopy.get(fieldName);
        if (locationColumn == null) {
            locationColumn = new LocationColumn();
            syntheticColumnsCopy.put(fieldName,locationColumn);
        }
        return locationColumn;
    }

    private void commitUpdates() {
        for (String field : syntheticColumnsCopy.keySet()) {
            model.setSyntheticLocation(field,syntheticColumnsCopy.get(field));
        }

        //When lat and lon are set, there is no need to geocode.  Turn it off
        if (latitudeOrLongitudeAreSet(syntheticColumnsCopy))
            model.setSocrataGeocoding(false);
    }

    private boolean latitudeOrLongitudeAreSet(Map<String,LocationColumn> syntheticColumns){
        for (String key : syntheticColumns.keySet()){
            LocationColumn column = syntheticColumns.get(key);
            if (column.latitude != null || column.longitude != null)
                return true;
        }
        return false;
    }
}

// All of the updates will be done based on the index of the columns array in the model.
// This will provide a facility for displaying a friendly name, while still updating the internal
// name by index, whatever it happens to be at the time.

class SyntheticComboBoxItem{
    String displayName;
    int index;

    public SyntheticComboBoxItem(String displayName, int index){
        this.displayName = displayName;
        this.index = index;
    }

    public String getDisplayName(){
        return displayName;
    }

    public int getIndex(){
        return index;
    }

    @Override
    public String toString(){
        return getDisplayName();
    }
}
