package com.socrata.datasync.ui;

import com.socrata.datasync.model.ControlFileModel;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

/**
 * UI to host all of the Advanced options in the control file editor.
 *
 */
public class AdvancedOptionsPanel extends JPanel implements Observer {

    private static final Dimension CONTROL_FILE_VIEWER_DIMENSIONS = new Dimension(500, 350);
    ControlFileModel model;

    JTextField encodingField;
    JTextField quoteField;
    JTextField rowsField;
    JTextField sepField;
    JTextField escapeField;
    JCheckBox emptyTextIsNull;
    JCheckBox useSocrataGeocoding;

    JTextField timezoneField;
    JTextField floatingDatetimeField;
    boolean advancedExpanded = false;
    boolean syntheticExpanded = false;
    JButton advancedOptions;
    JButton showSynthetic;
    JButton displayControlFile;

    public AdvancedOptionsPanel(ControlFileModel model){
        this.model = model;
        initializeComponents();
        renderComponents();
        model.addObserver(this);
    }

    public void initializeComponents(){
        EncodingListener encodingListener = new EncodingListener();
        encodingField = generateGenericInput("Encoding", model.getControlFile().getFileTypeControl().encoding, encodingListener,encodingListener);

        QuoteListener quoteListener = new QuoteListener();
        quoteField = generateGenericInput("Quote",model.getControlFile().getFileTypeControl().quote,quoteListener,quoteListener);

        RowListener rowsListener = new RowListener();
        rowsField = generateGenericInput("Rows to skip",model.getControlFile().getFileTypeControl().skip.toString(),rowsListener,rowsListener);

        SeparatorListener separatorListener = new SeparatorListener();
        sepField =generateGenericInput("Separator",model.getControlFile().getFileTypeControl().separator.replace("\t","\\t"),separatorListener,separatorListener);

        EscapeListener escapeListener = new EscapeListener();
        escapeField = generateGenericInput("Escape", model.getControlFile().getFileTypeControl().escape,escapeListener,escapeListener);



        TimeZoneListener timeZoneListener = new TimeZoneListener();
        timezoneField = generateGenericInput("Time zone",model.getTimezone(),timeZoneListener,timeZoneListener);

        FloatingDatetimeListener floatingListener = new FloatingDatetimeListener();
        floatingDatetimeField = new JTextField();
        floatingDatetimeField.setPreferredSize(new Dimension(350,20));
        floatingDatetimeField.addActionListener(floatingListener);
        floatingDatetimeField.addFocusListener(floatingListener);
        floatingDatetimeField.setText(model.getFloatingDateTime());

        emptyTextIsNull = generateGenericCheckbox("Empty text is null",model.getControlFile().getFileTypeControl().emptyTextIsNull,new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setEmptyTextIsNull(emptyTextIsNull.isSelected());
            }
        });

        useSocrataGeocoding = generateGenericCheckbox("Use Socrata Geocoding", model.getControlFile().getFileTypeControl().useSocrataGeocoding, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.setUseSocrataGeocoding(useSocrataGeocoding.isSelected());
            }
        });



        displayControlFile = new JButton();
        displayControlFile.setText("Display Control File");

        displayControlFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showEditControlFileDialog();
            }
        });

        advancedOptions = UIUtility.getButtonAsLink("Advanced Import Options");
        advancedOptions.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (advancedExpanded)
                    renderCollapsed();
                else
                    renderAdvancedExpanded();
            }
        });

        showSynthetic = UIUtility.getButtonAsLink("Manage Synthetic Columns (" + model.getSyntheticLocations().size() +")");
        showSynthetic.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (syntheticExpanded)
                    renderCollapsed();
                else {
                    //If there are no locations to show, take me to the dialog where I can create one
                    if (model.getSyntheticLocations().isEmpty()) {
                        JDialog dialog = new SyntheticLocationDialog(model, (JFrame) ((JDialog) SwingUtilities.getRoot((JButton) e.getSource())).getParent(), model.getSyntheticLocations(), null, "Manage synthetic columns");

                    }
                    else
                        renderSyntheticExpanded();
                }
            }
        });
    }

    private JPanel getBottomPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1,0));

        Dimension panelSize = new Dimension(-1, 24);
        panel.setPreferredSize(panelSize);
        panel.setMaximumSize(panelSize);
        panel.setMinimumSize(panelSize);

        advancedOptions.setAlignmentX(RIGHT_ALIGNMENT);
        advancedOptions.setHorizontalAlignment(SwingConstants.RIGHT);
        if (advancedExpanded)
           advancedOptions.setText("Hide Advanced Options");
        else
            advancedOptions.setText("Advanced Import Options");

        if (model.getDatasetModel().getLocationCount() > 0) {
            if (syntheticExpanded)
                showSynthetic.setText("Hide Synthetic Columns");
            else
                showSynthetic.setText("Manage Synthetic Columns (" + model.getSyntheticLocations().size() + ")");

            showSynthetic.setAlignmentX(LEFT_ALIGNMENT);
            showSynthetic.setHorizontalAlignment(SwingConstants.LEFT);

            panel.add(showSynthetic);
        }
        panel.add(advancedOptions);

        return panel;
    }

    private void renderCollapsed(){
        this.removeAll();
        advancedExpanded = false;
        syntheticExpanded = false;
        setLayout(new BorderLayout());
        add(getBottomPanel(),BorderLayout.SOUTH);
        setBorder(null);
        this.revalidate();

    }

    private void renderSyntheticExpanded(){
        this.removeAll();
        syntheticExpanded = true;
        setLayout(new BorderLayout());
        add(new SyntheticLocationsContainer(model), BorderLayout.CENTER);
        add(getBottomPanel(),BorderLayout.SOUTH);
        this.revalidate();
    }

    private void renderAdvancedExpanded(){
        this.removeAll();
        advancedExpanded = true;
        JPanel container = new JPanel();


        container.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        container.add(panel1, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, 1, null, null, null, 0, false));
        final com.intellij.uiDesigner.core.Spacer spacer1 = new com.intellij.uiDesigner.core.Spacer();
        panel1.add(spacer1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1, true, false));
        panel1.add(panel2, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        container.add(panel3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));

        final JLabel label7 = new JLabel();
        label7.setText("Timestamp Format");
        panel3.add(label7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel3.add(floatingDatetimeField, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));

        panel3.add(labelPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Separator");
        labelPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 1, false));
        final JLabel label2 = new JLabel();
        label2.setText("Quote");
        labelPanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Encoding");
        labelPanel.add(label3, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Rows to skip");
        labelPanel.add(label4, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Escape");
        labelPanel.add(label5, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Timezone");
        labelPanel.add(label6, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_EAST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel fieldPanel = new JPanel();
        fieldPanel.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(fieldPanel, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_CENTER, com.intellij.uiDesigner.core.GridConstraints.FILL_BOTH, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fieldPanel.add(sepField, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        fieldPanel.add(quoteField, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        fieldPanel.add(encodingField, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));         fieldPanel.add(escapeField, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        fieldPanel.add(rowsField, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        fieldPanel.add(escapeField, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));         fieldPanel.add(escapeField, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));
        fieldPanel.add(timezoneField, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(50, -1), null, 0, false));

        panel3.add(emptyTextIsNull, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel3.add(useSocrataGeocoding, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        displayControlFile.setHorizontalAlignment(SwingConstants.LEFT);
        panel3.add(displayControlFile, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1, com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST, com.intellij.uiDesigner.core.GridConstraints.FILL_NONE, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW, com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));



        container.setPreferredSize(new Dimension(-1, 400));
        container.setMinimumSize(new Dimension(-1, 400));
        JScrollPane scroll = new JScrollPane();
        scroll.setPreferredSize(new Dimension(-1, 400));
        scroll.setMaximumSize(new Dimension(800, 400));
        scroll.setViewportView(container);
        //scroll.setBorder(null);
        JPanel titledPanel = new JPanel();
        titledPanel.setLayout(new GridLayout(1,0));
        titledPanel.add(scroll);
        titledPanel.setBorder(BorderFactory.createTitledBorder("Advanced Import options"));
        setBorder(null);
        setLayout(new BorderLayout());
        add(titledPanel, BorderLayout.CENTER);
        add(getBottomPanel(),BorderLayout.SOUTH);
        this.revalidate();
    }

    public void renderComponents(){
        if(advancedExpanded)
            renderAdvancedExpanded();
        else
            renderCollapsed();
    }

    private JCheckBox generateGenericCheckbox(String label, boolean defaultState, ActionListener listener){
        JCheckBox box = new JCheckBox(label);
        box.addActionListener(listener);
        box.setSelected(defaultState);
        return box;
    }

    private JTextField generateGenericInput(String label, String defaultValue, ActionListener listener, FocusListener focusListener){
        JTextField field = new JTextField();
        field.addActionListener(listener);
        field.addFocusListener(focusListener);
        field.setText(defaultValue);
        field.setPreferredSize(new Dimension(50, 20));
        field.setMaximumSize(new Dimension(50,20));

        return field;
    }

    private void showEditControlFileDialog() {
        try {
            ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            String textAreaContent = mapper.writeValueAsString(model.getControlFile());

            JTextArea controlFileContentTextArea = new JTextArea();
            controlFileContentTextArea.setEditable(false);
            controlFileContentTextArea.setText(textAreaContent);
            JScrollPane scrollPane = new JScrollPane(controlFileContentTextArea);
            controlFileContentTextArea.setLineWrap(true);
            controlFileContentTextArea.setWrapStyleWord(true);
            controlFileContentTextArea.setCaretPosition(0);
            scrollPane.setPreferredSize(CONTROL_FILE_VIEWER_DIMENSIONS);

            JOptionPane.showMessageDialog(this,scrollPane,"View Control File",JOptionPane.PLAIN_MESSAGE);
        }
        catch (IOException e){
             JOptionPane.showMessageDialog(this,"Could not load control file.  Please contact support");
        }

    }

    @Override
    public void update(Observable o, Object arg) {
        renderComponents();
    }


    class EncodingListener extends FocusAdapter implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField field = (JTextField) e.getSource();
            update(field);
        }
        @Override
        public void focusLost(FocusEvent e){
            if (!e.isTemporary()) {
                JTextField field = (JTextField) e.getSource();
                update(field);
            }
        }

        public void update(JTextField field){
            model.setEncoding(field.getText());
        }
    }

    class FloatingDatetimeListener extends FocusAdapter implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField field = (JTextField) e.getSource();
            update(field);
        }
        @Override
        public void focusLost(FocusEvent e){
            if (!e.isTemporary()) {
                JTextField field = (JTextField) e.getSource();
                update(field);
            }
        }

        public void update(JTextField field){
            model.setFloatingDateTime(field.getText());
            model.setFixedDateTime(field.getText());
        }
    }


    class TimeZoneListener extends FocusAdapter implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField field = (JTextField) e.getSource();
            update(field);
        }
        @Override
        public void focusLost(FocusEvent e){
            if (!e.isTemporary()) {
                JTextField field = (JTextField) e.getSource();
                update(field);
            }
        }

        public void update(JTextField field){
            model.setTimezone(field.getText());
        }
    }

    class QuoteListener extends FocusAdapter implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField field = (JTextField) e.getSource();
            update(field);
        }
        @Override
        public void focusLost(FocusEvent e){
            if (!e.isTemporary()) {
                JTextField field = (JTextField) e.getSource();
                update(field);
            }
        }

        public void update(JTextField field){
            model.setQuote(field.getText());
        }
    }

    class SeparatorListener extends FocusAdapter implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField field = (JTextField) e.getSource();
            update(field);
        }
        @Override
        public void focusLost(FocusEvent e){
            if (!e.isTemporary()) {
                JTextField field = (JTextField) e.getSource();
                update(field);
            }
        }

        public void update(JTextField field){
            model.setSeparator(field.getText().replace("\\t","\t"));
        }
    }

    class EscapeListener extends FocusAdapter implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField field = (JTextField) e.getSource();
            update(field);
        }
        @Override
        public void focusLost(FocusEvent e){
            if (!e.isTemporary()) {
                JTextField field = (JTextField) e.getSource();
                update(field);
            }
        }

        public void update(JTextField field){
            model.setEscape(field.getText());
        }
    }

    class RowListener extends FocusAdapter implements ActionListener{

        @Override
        public void actionPerformed(ActionEvent e) {
            JTextField field = (JTextField) e.getSource();
            update(field);
        }
        @Override
        public void focusLost(FocusEvent e){
            if (!e.isTemporary()) {
                JTextField field = (JTextField) e.getSource();
                update(field);
            }
        }

        public void update(JTextField field){
            model.setRowsToSkip(Integer.parseInt(field.getText()));
        }
    }


}
