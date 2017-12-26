package com.socrata.datasync.ui;

import com.socrata.datasync.config.controlfile.SyntheticPointColumn;
import com.socrata.datasync.config.controlfile.GeocodedPointColumn;
import com.socrata.datasync.config.controlfile.ProvidedPointColumn;
import com.socrata.datasync.model.CSVModel;
import com.socrata.datasync.model.ControlFileModel;
import com.socrata.model.importer.Column;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Map;

/**
 * A dialog box which allows the customer to pick from columns in a CSV and then map them into
 * a location column
 */
public class SyntheticPointDialog extends JDialog {
    static class PointSpec {
        enum Active {
            GEOCODED, PROVIDED
        }

        Active active;
        GeocodedPointColumn geocoded;
        ProvidedPointColumn provided;

        PointSpec(SyntheticPointColumn pt) {
            if(pt == null) {
                active = Active.GEOCODED;
                geocoded = new GeocodedPointColumn();
                provided = new ProvidedPointColumn();
            } else if(pt instanceof GeocodedPointColumn) {
                active = Active.GEOCODED;
                geocoded = (GeocodedPointColumn) pt;
                provided = new ProvidedPointColumn();
            } else {
                active = Active.PROVIDED;
                geocoded = new GeocodedPointColumn();
                provided = (ProvidedPointColumn) pt;
            }
        }

        public SyntheticPointColumn getPointColumn() {
            return active == Active.GEOCODED ? geocoded : provided;
        }

        @Override
        public String toString() {
            return "#<" + active + " " + geocoded + " " + provided + ">";
        }
    }
    Map<String, PointSpec> syntheticColumnsCopy;
    Map<String, Integer> columnIndexes;
    ControlFileModel model;
    JComboBox addressCombo;
    JComboBox cityCombo;
    JComboBox stateCombo;
    JComboBox zipCombo;
    JComboBox countryCombo;
    JComboBox latCombo;
    JComboBox lonCombo;
    JButton buttonOK;
    JButton buttonCancel;
    JLabel headerLabel;
    JComboBox activeLocation;
    JTabbedPane tabs;

    private static final int GEOCODED_PANE = 0;
    private static final int PROVIDED_PANE = 1;

    public static JDialog create(ControlFileModel model, JFrame parent, Map<String, SyntheticPointColumn> syntheticColumns) {
        return create(model, parent, syntheticColumns, null);
    }

    public static JDialog create(ControlFileModel model, JFrame parent, Map<String, SyntheticPointColumn> syntheticColumns, String initFieldName) {
        return new SyntheticPointDialog(model, parent, syntheticColumns, initFieldName, "Manage synthetic columns");
    }

    private SyntheticPointDialog(ControlFileModel model, JFrame parent, Map<String, SyntheticPointColumn> syntheticColumns, String initFieldName, String title) {
        super(parent, title);
        this.model = model;
        //Copy the columns so that playing around with them in the dialog doesn't accidentially change the underlying model.
        this.syntheticColumnsCopy = copySyntheticColumns(syntheticColumns);

        columnIndexes = new HashMap<>();
        for (int i = 0; i < model.getColumnCount();i++){
            columnIndexes.put(model.getColumnAtPosition(i), i);
        }

        setLocationRelativeTo(null);
        setModal(true);
        initComponents();
        layoutComponents();
        if (initFieldName != null)
            activeLocation.setSelectedItem(initFieldName);
        updatePane();

        // this has to happen AFTER we've selected our initial pane
        tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(tabs.getSelectedIndex() == GEOCODED_PANE) {
                    ensureActiveIsGeocoded();
                } else {
                    ensureActiveIsProvided();
                }
            }
        });

        addFieldListeners();

        pack();
        setVisible(true);
    }

    //Make a copy of the columns so that we don't change the control file just by launching the dialog.
    private Map<String, PointSpec> copySyntheticColumns(Map<String, SyntheticPointColumn> original){
        Map<String, PointSpec> result = new TreeMap<>();
        for(Map.Entry<String, SyntheticPointColumn> ent : original.entrySet()) {
            result.put(ent.getKey(), new PointSpec(ent.getValue().clone()));
        }
        return result;
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
        countryCombo = getComboBox();
        latCombo = getComboBox();
        lonCombo = getComboBox();

        activeLocation = getLocationsCombobox();
        activeLocation.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    updatePane();
                    updateSyntheticComponentComboboxes();
                }
            }
        });

        tabs = new JTabbedPane();

        updateSyntheticComponentComboboxes();
    }

    private void addFieldListeners() {
        abstract class Listener<T extends SyntheticPointColumn> implements ItemListener {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == ItemEvent.SELECTED) {
                    T col = extractPoint(getActiveLocation());
                    SyntheticComboBoxItem item =  (SyntheticComboBoxItem) e.getItem();
                    setField(col, model.getColumnAtPosition(item.getIndex()));
                }
            }

            abstract T extractPoint(PointSpec spec);
            abstract void setField(T col, String value);
        }

        abstract class GeocodedListener extends Listener<GeocodedPointColumn> {
            GeocodedPointColumn extractPoint(PointSpec spec) {
                return spec.geocoded;
            }
        }

        abstract class ProvidedListener extends Listener<ProvidedPointColumn> {
            ProvidedPointColumn extractPoint(PointSpec spec) {
                return spec.provided;
            }
        }

        addressCombo.addItemListener(new GeocodedListener() {
                void setField(GeocodedPointColumn col, String value) {
                    col.address = value;
                }
            });

        cityCombo.addItemListener(new GeocodedListener() {
                void setField(GeocodedPointColumn col, String value) {
                    col.city = value;
                }
            });

        stateCombo.addItemListener(new GeocodedListener() {
                void setField(GeocodedPointColumn col, String value) {
                    col.state = value;
                }
            });

        zipCombo.addItemListener(new GeocodedListener() {
                void setField(GeocodedPointColumn col, String value) {
                    col.zip = value;
                }
            });

        countryCombo.addItemListener(new GeocodedListener() {
                void setField(GeocodedPointColumn col, String value) {
                    col.country = value;
                }
            });

        latCombo.addItemListener(new ProvidedListener() {
                void setField(ProvidedPointColumn col, String value) {
                    col.latitude = value;
                }
            });

        lonCombo.addItemListener(new ProvidedListener() {
                void setField(ProvidedPointColumn col, String value) {
                    col.longitude = value;
                }
            });
    }

    private void updateSyntheticComponentComboboxes(){
        PointSpec locationColumn = getActiveLocation();
        updateGeocodedComponentComboboxes(locationColumn.geocoded);
        updateProvidedComponentComboboxes(locationColumn.provided);
    }

    private int indexOf(String name) {
        Integer r = columnIndexes.get(name);
        if(r == null) return -1;
        else return r;
    }

    private void updatePane() {
        if(getActiveLocation().active == PointSpec.Active.GEOCODED) {
            tabs.setSelectedIndex(GEOCODED_PANE);
        } else {
            tabs.setSelectedIndex(PROVIDED_PANE);
        }
    }

    private void updateGeocodedComponentComboboxes(GeocodedPointColumn locationColumn) {
        if (locationColumn.address != null)
            addressCombo.setSelectedIndex(indexOf(locationColumn.address));
        else
            addressCombo.setSelectedIndex(-1);

        if (locationColumn.city != null)
            cityCombo.setSelectedIndex(indexOf(locationColumn.city));
        else
            cityCombo.setSelectedIndex(-1);

        if (locationColumn.state != null)
            stateCombo.setSelectedIndex(indexOf(locationColumn.state));
        else
            stateCombo.setSelectedIndex(-1);

        if (locationColumn.zip != null)
            zipCombo.setSelectedIndex(indexOf(locationColumn.zip));
        else
            zipCombo.setSelectedIndex(-1);

        if (locationColumn.country != null)
            countryCombo.setSelectedIndex(indexOf(locationColumn.country));
        else
            countryCombo.setSelectedIndex(-1);
    }

    private void updateProvidedComponentComboboxes(ProvidedPointColumn locationColumn) {
        if (locationColumn.latitude != null)
            latCombo.setSelectedIndex(indexOf(locationColumn.latitude));
        else
            latCombo.setSelectedIndex(-1);

        if (locationColumn.longitude != null)
            lonCombo.setSelectedIndex(indexOf(locationColumn.longitude));
        else
            lonCombo.setSelectedIndex(-1);
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
        panel3.add(panel4, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(50, -1), null, 1, false));
        panel4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        headerLabel.setText("Select the point column to configure");

        panel4.add(headerLabel, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel4.add(activeLocation, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_NORTH, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));

        panel3.add(tabs, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(50, -1), null, 1, false));

        final JPanel panel5 = new JPanel();
        tabs.addTab("Geocoded", panel5);
        panel5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Address");
        panel5.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel5.add(addressCombo, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("City");
        panel5.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel5.add(cityCombo, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("State");
        panel5.add(label3, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel5.add(stateCombo, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Zip");
        panel5.add(label4, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel5.add(zipCombo, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Country");
        panel5.add(label5, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel5.add(countryCombo, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));

        final JPanel panel6 = new JPanel();
        tabs.addTab("Lat/Lon", panel6);
        panel6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label6 = new JLabel();
        label6.setText("Latitude");
        panel6.add(label6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel6.add(latCombo, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Longitude");
        panel6.add(label7, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel6.add(lonCombo, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(220, -1), new Dimension(220, -1), 0, false));

        final com.intellij.uiDesigner.core.Spacer spacer2 = new com.intellij.uiDesigner.core.Spacer();
        contentPane.add(spacer2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, 1, new Dimension(-1, 5), new Dimension(-1, 5), new Dimension(-1, 5), 3, false));
        getContentPane().add(contentPane);
    }

    // Get a combobox whose values are all of the possible fields in the control file that could be mapped to this column
    private JComboBox getComboBox() {
        JComboBox<SyntheticComboBoxItem> columnNamesComboBox = new JComboBox<>();
        for (int i = 0; i < model.getColumnCount();i++){
            String friendlyName = model.getDisplayName(i);
            columnNamesComboBox.addItem(new SyntheticComboBoxItem(friendlyName,i));
        }
        columnNamesComboBox.setSelectedIndex(-1);
        return columnNamesComboBox;
    }

    private JComboBox getLocationsCombobox(){
        ArrayList<Column> columns = model.getDatasetModel().getColumns();
        JComboBox<String> locationComboBox = new JComboBox<String>();

        for (Column column : columns) {
            if (column.getDataTypeName().equals("point")) {
                locationComboBox.addItem(column.getFieldName());
            }
        }
        locationComboBox.setSelectedIndex(0);
        return locationComboBox;
    }

    private String getActiveFieldName() {
        return (String) activeLocation.getSelectedItem();
    }

    private PointSpec getActiveLocation() {
        String fieldName = getActiveFieldName();
        PointSpec locationColumn = syntheticColumnsCopy.get(fieldName);
        if (locationColumn == null) {
            locationColumn = new PointSpec(null);
            syntheticColumnsCopy.put(fieldName,locationColumn);
        }
        return locationColumn;
    }

    private void ensureActiveIsGeocoded() {
        getActiveLocation().active = PointSpec.Active.GEOCODED;
    }

    private void ensureActiveIsProvided() {
        getActiveLocation().active = PointSpec.Active.PROVIDED;
    }

    private void commitUpdates() {
        for (String field : syntheticColumnsCopy.keySet()) {
            model.setSyntheticPoint(field, syntheticColumnsCopy.get(field).getPointColumn());
        }
    }
}
