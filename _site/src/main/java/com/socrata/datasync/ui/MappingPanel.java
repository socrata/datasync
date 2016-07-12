package com.socrata.datasync.ui;

import com.socrata.datasync.model.CSVModel;
import com.socrata.datasync.model.ControlFileModel;
import com.socrata.datasync.model.DatasetModel;
import com.socrata.model.importer.Column;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

/**
 * Provides UI to allow the customer to map a single column in the CSV to a single field name in the dataset
 *
 * Created by franklinwilliams.
 */

public class MappingPanel extends JPanel {

    JPanel CSVValuePreview = new JPanel();
    JLabel CSVTitle = new JLabel();
    JLabel CSVPreview = new JLabel();
    int index;
    JComboBox columnNamesComboBox;
    JLabel arrow = new JLabel("" + '\u2192');
    String lastSelection;
    ControlFileModel model;
    final int IGNORE_INDEX = 0;
    final String ignoreField = "- Ignore this field -";

    public MappingPanel(int index, ControlFileModel model, DatasetModel datasetModel){
        initializeValues(index, model, datasetModel);
        styleComponents();
        layoutComponents();
    }

    public void update(){
        updateCsvPreview();
        updateCombobox();
    }

    private void updateCsvPreview(){
        CSVModel csv = model.getCsvModel();
        CSVTitle.setText(csv.getColumnName(index));
        setPreviewText(csv.getColumnPreview(index, 10));
    }

    private void setPreviewText(String text){
        String preview = "(ex. " + text.substring(0,Math.min(text.length(),50)) + ")";
        CSVPreview.setText(preview);
    }

    private void updateCombobox(){
        String selection = model.getColumnAtPosition(index);

        //Don't do anything if the state for the column hasn't changed (otherwise, you end up with a lot of events unnecessarily firing).
        if (selection == lastSelection)
            return;

        if (model.isIgnored(selection))
            columnNamesComboBox.setSelectedIndex(IGNORE_INDEX);
        else
            columnNamesComboBox.setSelectedItem(selection);
        lastSelection = selection;
    }

    private void initializeValues(int index, ControlFileModel model, DatasetModel datasetModel){
        this.index = index;
        this.model = model;

        columnNamesComboBox = new JComboBox();
        //The ignore field must be the first item in the list.
        columnNamesComboBox.addItem(ignoreField);
        ArrayList<Column> columns = datasetModel.getColumns();
        for (Column column : columns) {
            columnNamesComboBox.addItem(column.getFieldName());
        }
        update();

        columnNamesComboBox.addItemListener(new MappingComboboxListener());
    }

    private void styleComponents(){
        CSVPreview.setForeground(new Color(112,112,112));
        this.setBackground(Color.WHITE);
        this.setBorder(BorderFactory.createMatteBorder(1,0,0,0,Color.gray));
        CSVValuePreview.setOpaque(false);
        CSVValuePreview.setBackground(Color.WHITE);
        CSVTitle.setOpaque(false);
        CSVTitle.setBackground(Color.WHITE);
        CSVPreview.setOpaque(false);
        CSVPreview.setBackground(Color.WHITE);
    }

    private void layoutComponents(){
       // this.setLayout(new BoxLayout(this,BoxLayout.X_AXIS));
        this.setLayout(new BorderLayout());
        this.setMinimumSize(new Dimension(800,48));
        this.setMaximumSize(new Dimension(800,48));

        CSVValuePreview.setLayout(new BoxLayout(CSVValuePreview,BoxLayout.Y_AXIS));

        CSVTitle.setBorder (BorderFactory.createEmptyBorder(5,5,2,0));
        CSVPreview.setBorder (BorderFactory.createEmptyBorder(0,5,10,0));

        CSVValuePreview.add(CSVTitle);
        CSVValuePreview.add(CSVPreview);

        CSVValuePreview.setPreferredSize(new Dimension(300,48));
        CSVValuePreview.setMaximumSize(new Dimension(300, 48));

        columnNamesComboBox.setBorder(BorderFactory.createEmptyBorder(5,5,10,5));
        columnNamesComboBox.setPreferredSize(new Dimension(200,48));
        columnNamesComboBox.setMaximumSize(new Dimension(200, 48));

        this.add(CSVValuePreview,BorderLayout.WEST);
        this.add(arrow, BorderLayout.CENTER);
        this.add(columnNamesComboBox, BorderLayout.EAST);
    }

    //Get a header panel with the same dimensions and layouts as the rows
    public static JPanel getHeaderPanel(){
        JPanel header = new JPanel();
        JLabel csvLabel = new JLabel("<html><b>CSV Field</b></html>");
        JLabel fieldLabel = new JLabel("<html><b>Dataset Field</b></html>");
        header.setBackground(Color.WHITE);
        csvLabel.setOpaque(false);
        fieldLabel.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(5,5,0,0));
        csvLabel.setPreferredSize(new Dimension(300,16));
        csvLabel.setMaximumSize(new Dimension(300, 16));
        header.setLayout(new BorderLayout());
        header.setMinimumSize(new Dimension(800,48));
        header.setMaximumSize(new Dimension(800,48));

        fieldLabel.setBorder(BorderFactory.createEmptyBorder(5,7,0,0));

        fieldLabel.setPreferredSize(new Dimension(200,16));
        fieldLabel.setMaximumSize(new Dimension(200, 16));
        header.add(csvLabel,BorderLayout.WEST);
        //header.add()
        header.add(fieldLabel, BorderLayout.EAST);
        return header;
    }



    private class MappingComboboxListener implements ItemListener
    {
        @Override
        public void itemStateChanged(ItemEvent e) {
            JComboBox box = (JComboBox) e.getSource();
            if (e.getStateChange() == ItemEvent.SELECTED) {
                String selectedItem = (String) box.getSelectedItem();
                //Apparently there is a way to select a null item in Java?
                if (selectedItem != null) {
                    if (selectedItem.equals(ignoreField)) {
                        model.ignoreColumnInCSVAtPosition(index);
                    } else {
                        model.updateColumnAtPosition(selectedItem, index);
                    }
                }
            }
        }
    }


}
