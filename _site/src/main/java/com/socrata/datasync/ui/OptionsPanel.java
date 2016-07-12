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
    JCheckBox setAsideErrors;
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

        SetAsideErrorsListener setAsideErrorsListener = new SetAsideErrorsListener();
        setAsideErrors = generateGenericCheckbox("Set Aside Errors", model.getControlFile().getFileTypeControl().setAsideErrors,setAsideErrorsListener);

        useSocrataGeocoding = generateGenericCheckbox("Use Socrata Geocoding", model.getControlFile().getFileTypeControl().useSocrataGeocoding, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setSocrataGeocoding(useSocrataGeocoding.isSelected());
            }
        });
    }

    public void layoutComponents(){
        this.setLayout(new GridLayout(1,0));
        container.setLayout(new GridLayout(1,3));
        this.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(5,0,1,0),BorderFactory.createTitledBorder("Options")));
        hasHeaderRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        hasHeaderRow.setMaximumSize(checkboxDim);
        container.add(hasHeaderRow);

        trimWhitespace.setAlignmentX(LEFT_ALIGNMENT);

        trimWhitespace.setMaximumSize(checkboxDim);
        container.add(trimWhitespace);

        JPanel errorsPanel = new JPanel();
        errorsPanel.setLayout(new FlowLayout());

        setAsideErrors.setAlignmentX(LEFT_ALIGNMENT);
        setAsideErrors.setMaximumSize(checkboxDim);
        errorsPanel.add(setAsideErrors);

        String helpString = "<HTML>With “Set aside errors” selected, any rows that contain invalid data<br>" +
                            "will be set aside for inspection, while all valid rows will be imported<br>" +
                            "to the dataset. The invalid rows will be available for download on the<br>" +
                            "Job Status page for this job.<HTML>";
        JLabel helpBubble = UIUtility.generateHelpBubble(helpString);
        helpBubble.setAlignmentX(LEFT_ALIGNMENT);
        errorsPanel.add(helpBubble);

        container.add(errorsPanel);
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

    class SetAsideErrorsListener extends FocusAdapter implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            JCheckBox box = (JCheckBox) e.getSource();
            model.setSetAsideErrors(box.isSelected());
        }
    }


}
