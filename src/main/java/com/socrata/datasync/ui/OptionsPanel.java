package com.socrata.datasync.ui;

import com.socrata.datasync.model.ControlFileModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;

/**
 * Created by franklinwilliams on 4/27/15.
 */
public class OptionsPanel extends JPanel {
    ControlFileModel model;
    JCheckBox trimWhitespace;
    JCheckBox hasHeaderRow;
    JCheckBox useSocrataGeocoding;
    JPanel container;

    int maximumHeight = 25;
    int maximumCheckboxWidth = 200;
    Dimension checkboxDim = new Dimension(maximumCheckboxWidth,maximumHeight);

    public OptionsPanel(ControlFileModel model){
        this.model = model;

        initializeComponents();
        layoutComponents();
    }

    public void initializeComponents() {
        container = new JPanel();

        TrimWhitespaceListener trimWhitespaceListener = new TrimWhitespaceListener();
        trimWhitespace = generateGenericCheckbox("Trim whitespace", model.getControlFile().getFileTypeControl().trimWhitespace, trimWhitespaceListener);

        HasHeaderRowListener headerRowListener = new HasHeaderRowListener();
        hasHeaderRow =generateGenericCheckbox("Has header row",model.getControlFile().getFileTypeControl().hasHeaderRow,headerRowListener);

        useSocrataGeocoding = generateGenericCheckbox("Use Socrata Geocoding", model.getControlFile().getFileTypeControl().useSocrataGeocoding, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setSocrataGeocoding(useSocrataGeocoding.isSelected());
            }
        });
    }

    public void layoutComponents(){
        this.setLayout(new GridLayout(1,0));
        container.setLayout(new BoxLayout(container,BoxLayout.X_AXIS));
        this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,0,1,0),BorderFactory.createTitledBorder("Options")));
        hasHeaderRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        hasHeaderRow.setMaximumSize(checkboxDim);
        container.add(hasHeaderRow);

        trimWhitespace.setAlignmentX(LEFT_ALIGNMENT);

        trimWhitespace.setMaximumSize(checkboxDim);
        container.add(trimWhitespace);

        useSocrataGeocoding.setAlignmentX(LEFT_ALIGNMENT);
        useSocrataGeocoding.setMaximumSize(checkboxDim);
        this.add(container);
    }

    //TODO: This already exists in the advanced panel.  Refactor it out into one.
    private JCheckBox generateGenericCheckbox(String label, boolean initialState, ActionListener listener){
        JCheckBox box = new JCheckBox(label);
        box.setBorder(BorderFactory.createEmptyBorder(0,0,5,0));
        box.setSelected(initialState);
        box.addActionListener(listener);
        return box;
    }

    class TrimWhitespaceListener extends FocusAdapter implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox box = (JCheckBox) e.getSource();
            model.setTrimWhiteSpace(box.isSelected());
        }
    }

    class HasHeaderRowListener extends FocusAdapter implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox box = (JCheckBox) e.getSource();
            model.setHasHeaderRow(box.isSelected());
        }
    }



}
