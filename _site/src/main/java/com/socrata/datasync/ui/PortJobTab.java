package com.socrata.datasync.ui;

import com.socrata.datasync.Utils;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.PortMethod;
import com.socrata.datasync.PublishDataset;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.SocrataConnectionInfo;
import com.socrata.datasync.job.PortJob;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Authors: Adrian Laurenzi and Louis Fettet
 * Date: 9/11/13
 */
public class PortJobTab implements JobTab {

    private final int JOB_TEXTFIELD_WIDTH = 370;
    private final int JOB_TEXTFIELD_HEIGHT = 24;
    private final int JOB_FIELD_VGAP = 7;
    private final int SINK_DATASET_ID_TEXTFIELD_WIDTH = 210;
    private final int OPEN_SINK_DATASET_BUTTON_HEIGHT = 22;
    private final String DEFAULT_DESTINATION_SET_ID = "(Generates after running job)";
    private final String JOB_FILE_NAME = "Socrata Port Job";
    private final String JOB_FILE_EXTENSION = "spj";

    private final int HELP_ICON_TOP_PADDING = 10;
    private final String PORT_METHOD_TIP_TEXT = "<html>" +
            "<strong>Copy schema only</strong>: copies only the metadata and columns to a new dataset<br>" +
            "<strong>Copy schema and data</strong>: makes an identical copy to a new dataset<br>" +
            "<strong>Copy data only</strong>: copies only the rows to an existing dataset</html>";
    private final String SOURCE_SITE_TIP_TEXT = "Domain where the source dataset is located.";
    private final String SOURCE_SET_TIP_TEXT = "The xxxx-xxxx ID of the source dataset (e.g. n38h-y5wp)";
    private final String SINK_SITE_TIP_TEXT = "Domain where the destination dataset will be [or is] located.";
    private final String SINK_SET_TIP_TEXT = "<html><body style='width: 400px'>If Port Method is '<strong>copy schema</strong>' or '<strong>copy schema and data</strong>' " +
            "this field will be populated with the xxxx-xxxx ID of the newly created dataset.<br>" +
            "If Port method is '<strong>copy data only</strong>' enter the xxxx-xxxx ID of the existing dataset you wish to copy data to (e.g. n38h-y5wp).</body></html>";
    private final String PUBLISH_METHOD_TIP_TEXT = "The method to use when publishing the data to the destination dataset.";
    private final String PUBLISH_DATASET_TIP_TEXT = "<html><body style='width: 280px'>If <strong>Yes</strong>, publish the newly created destination dataset.<br>" +
            "If <strong>No</strong>, create it as an unpublished working copy.</body></html>";

    private JFrame mainFrame;
    private JPanel jobPanel;

    private String jobFileLocation;
    private JLabel jobTabTitleLabel;

    private JComboBox portMethodComboBox;
    private JTextField sourceSiteDomainTextField;
    private JTextField sourceSetIDTextField;
    private JTextField sinkSiteDomainTextField;
    private JTextField sinkSetIDTextField;

    // Need to expose more of the JComponents locally in order to toggle between PublishMethod and PublishDataset
    private JPanel publishMethodContainerLeft;
    private JComboBox publishMethodComboBox;
    private JPanel publishMethodContainerRight;
    private JPanel publishDatasetContainerLeft;
    private JComboBox publishDatasetComboBox;
    private JPanel publishDatasetContainerRight;


    // build Container with all tab components and load data into form
    public PortJobTab(PortJob job, JFrame containingFrame) {
        mainFrame = containingFrame;

        // build tab panel form
        jobPanel = new JPanel(new GridLayout(0, 2));

        // set FlowLayouts
        FlowLayout flowRight = new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP);

        // Port Method
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Port Method", PORT_METHOD_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel portMethodContainerRight = new JPanel(flowRight);
        portMethodComboBox = new JComboBox();
        for (PortMethod method : PortMethod.values()) {
            portMethodComboBox.addItem(method);
        }
        portMethodComboBox.addItemListener(new PortMethodItemListener());
        portMethodContainerRight.add(portMethodComboBox);
        jobPanel.add(portMethodContainerRight);

        // Source Site
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Source Domain", SOURCE_SITE_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel sourceSiteTextFieldContainer = new JPanel(flowRight);
        sourceSiteDomainTextField = new JTextField();
        sourceSiteDomainTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sourceSiteTextFieldContainer.add(sourceSiteDomainTextField);
        jobPanel.add(sourceSiteTextFieldContainer);

        // Source Set Dataset ID
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Source Dataset ID", SOURCE_SET_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel sourceSetIDTextFieldContainer = new JPanel(flowRight);
        sourceSetIDTextField = new JTextField();
        sourceSetIDTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sourceSetIDTextFieldContainer.add(sourceSetIDTextField);
        jobPanel.add(sourceSetIDTextFieldContainer);

        // Sink Site
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Destination Domain", SINK_SITE_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel sinkSiteTextFieldContainer = new JPanel(flowRight);
        sinkSiteDomainTextField = new JTextField();
        sinkSiteDomainTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sinkSiteTextFieldContainer.add(sinkSiteDomainTextField);
        jobPanel.add(sinkSiteTextFieldContainer);

        // Sink Site Dataset ID
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Destination Dataset ID", SINK_SET_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel destinationSetIDTextFieldContainer = new JPanel(flowRight);
        sinkSetIDTextField = new JTextField(DEFAULT_DESTINATION_SET_ID);
        sinkSetIDTextField.setPreferredSize(new Dimension(
                SINK_DATASET_ID_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sinkSetIDTextField.setEditable(false);
        destinationSetIDTextFieldContainer.add(sinkSetIDTextField);
        JButton openSinkDatasetButton = new JButton("Open Dataset");
        openSinkDatasetButton
                .addActionListener(new OpenDatasetButtonListener());
        openSinkDatasetButton.setPreferredSize(new Dimension(
                openSinkDatasetButton.getPreferredSize().width,
                OPEN_SINK_DATASET_BUTTON_HEIGHT));
        destinationSetIDTextFieldContainer.add(openSinkDatasetButton);
        jobPanel.add(destinationSetIDTextFieldContainer);

        // Publish Method (toggles with Publish Query based on Port Method choice)
        // We will build out the specs of this element without adding it to the jobPanel.
        publishMethodContainerLeft = UIUtility.generateLabelWithHelpBubble(
                "Publish Method", PUBLISH_METHOD_TIP_TEXT, HELP_ICON_TOP_PADDING);
        jobPanel.add(publishMethodContainerLeft);
        publishMethodContainerRight = new JPanel(flowRight);
        publishMethodComboBox = new JComboBox();
        /*for (PublishMethod method : PublishMethod.values()) {
            // TODO: clean this up once publish method changes have been implemented
            if (!method.equals(PublishMethod.append)) {
                publishMethodComboBox.addItem(method);
            }
        }*/
        publishMethodComboBox.addItem(PublishMethod.upsert);
        publishMethodComboBox.addItem(PublishMethod.replace);

        publishMethodComboBox.setEnabled(false);
        publishMethodContainerRight.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        publishMethodContainerRight.add(publishMethodComboBox);

        // Publish Destination Dataset (toggles with Publish Method based on Port Method choice)
        // We will build out the specs of this element without adding it to the jobPanel.
        publishDatasetContainerLeft = UIUtility.generateLabelWithHelpBubble(
                "Publish Destination Dataset?", PUBLISH_DATASET_TIP_TEXT, HELP_ICON_TOP_PADDING);
        publishDatasetContainerRight = new JPanel(flowRight);
        publishDatasetComboBox = new JComboBox();
        for (PublishDataset publish : PublishDataset.values()) {
            publishDatasetComboBox.addItem(publish);
        }
        publishDatasetComboBox.setEnabled(false);
        publishDatasetContainerRight.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        publishDatasetContainerRight.add(publishDatasetComboBox);

        // Load job data into fields
        PortMethod jobPortMethod = job.getPortMethod();
        portMethodComboBox.setSelectedItem(jobPortMethod);
        if (jobPortMethod.equals(PortMethod.copy_schema)
                || jobPortMethod.equals(PortMethod.copy_all)) {
            sinkSetIDTextField.setEditable(false);
            jobPanel.add(publishDatasetContainerLeft);
            jobPanel.add(publishDatasetContainerRight);
            publishDatasetComboBox.setEnabled(true);
        } else {
            sinkSetIDTextField.setEditable(true);
            jobPanel.add(publishMethodContainerLeft);
            jobPanel.add(publishMethodContainerRight);
            publishMethodComboBox.setEnabled(true);
        }
        UserPreferences userPrefs = new UserPreferencesJava();
        SocrataConnectionInfo connectionInfo = userPrefs.getConnectionInfo();
        if (job.getSourceSiteDomain().equals("https://") &&
                !connectionInfo.getUrl().equals("https://")) {
            sourceSiteDomainTextField.setText(connectionInfo.getUrl());
        } else {
            sourceSiteDomainTextField.setText(job.getSourceSiteDomain());
        }
        sourceSetIDTextField.setText(job.getSourceSetID());
        if (job.getSinkSiteDomain().equals("https://") &&
                !connectionInfo.getUrl().equals("https://")) {
            sinkSiteDomainTextField.setText(connectionInfo.getUrl());
        } else {
            sinkSiteDomainTextField.setText(job.getSinkSiteDomain());
        }
        if (job.getSinkSetID().equals("") && !sinkSetIDTextField.isEditable()){
            sinkSetIDTextField.setText(DEFAULT_DESTINATION_SET_ID);
        } else {
            sinkSetIDTextField.setText(job.getSinkSetID());
        }
        PublishMethod jobPublishMethod = job.getPublishMethod();
        publishMethodComboBox.setSelectedItem(jobPublishMethod);
        PublishDataset jobPublishDataset = job.getPublishDataset();
        publishDatasetComboBox.setSelectedItem(jobPublishDataset);

        jobFileLocation = job.getPathToSavedFile();
        jobTabTitleLabel = new JLabel(job.getJobFilename());
    }

    public JPanel getTabPanel() {
        return jobPanel;
    }

    public JobStatus runJobNow() {
        PortJob jobToRun = new PortJob();
        jobToRun.setPortMethod((PortMethod) portMethodComboBox
                .getSelectedItem());
        jobToRun.setSourceSiteDomain(sourceSiteDomainTextField.getText());
        jobToRun.setSourceSetID(sourceSetIDTextField.getText());
        jobToRun.setSinkSiteDomain(sinkSiteDomainTextField.getText());
        if (publishMethodComboBox.isEnabled()) {
            jobToRun.setPublishMethod((PublishMethod) publishMethodComboBox
                    .getSelectedItem());
        }
        if (publishDatasetComboBox.isEnabled()) {
            jobToRun.setPublishDataset((PublishDataset)
                    publishDatasetComboBox.getSelectedItem());
        }
        if (sinkSetIDTextField.isEditable()) {
            jobToRun.setSinkSetID(sinkSetIDTextField.getText());
        }

        JobStatus status = jobToRun.run();
        if (!status.isError()) {
            sinkSetIDTextField.setText(jobToRun.getSinkSetID());
        }
        return status;
    }

    public void saveJob() {
        // Save job data
        PortJob newPortJob = new PortJob();
        newPortJob.setPortMethod((PortMethod) portMethodComboBox
                .getSelectedItem());
        newPortJob.setSourceSiteDomain(sourceSiteDomainTextField.getText());
        newPortJob.setSourceSetID(sourceSetIDTextField.getText());
        newPortJob.setSinkSiteDomain(sinkSiteDomainTextField.getText());
        newPortJob.setSinkSetID(sinkSetIDTextField.getText());
        newPortJob.setPublishMethod((PublishMethod) publishMethodComboBox
                .getSelectedItem());
        newPortJob.setPublishDataset((PublishDataset) publishDatasetComboBox
                .getSelectedItem());
        newPortJob.setPathToSavedFile(jobFileLocation);

        // TODO If an existing file was selected WARN user of overwriting

        // if first time saving this job: Open dialog box to select "Save as..."
        // location
        // otherwise save to existing file
        String selectedJobFileLocation = jobFileLocation;
        if (selectedJobFileLocation.equals("")) {
            JFileChooser savedJobFileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    JOB_FILE_NAME + " (*." + JOB_FILE_EXTENSION + ")",
                    JOB_FILE_EXTENSION);
            savedJobFileChooser.setFileFilter(filter);
            int returnVal = savedJobFileChooser.showSaveDialog(mainFrame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = savedJobFileChooser.getSelectedFile();
                selectedJobFileLocation = file.getAbsolutePath();
                if (!selectedJobFileLocation.endsWith("." + JOB_FILE_EXTENSION)) {
                    selectedJobFileLocation += "." + JOB_FILE_EXTENSION;
                }
                jobFileLocation = selectedJobFileLocation;
                newPortJob.setPathToSavedFile(selectedJobFileLocation);
                jobTabTitleLabel.setText(newPortJob.getJobFilename());
            }
        }

        // actually save the job file (may overwrite)
        try {
            newPortJob.writeToFile(selectedJobFileLocation);

            // Update job tab title label
            jobTabTitleLabel.setText(newPortJob.getJobFilename());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(mainFrame,
                    "Error saving " + selectedJobFileLocation + ": " + e.getMessage());
        }
    }

    public JLabel getJobTabTitleLabel() { return jobTabTitleLabel; }

    public String getJobFileLocation() { return jobFileLocation; }

    /**
     * Returns the URI to the sink dataset based on the form text fields
     *
     * @return URI to sink dataset, or null if URI was malformed
     */
    public URI getURIToSinkDataset() {
        URI sinkDatasetURI = null;
        try {
            sinkDatasetURI = new URI(sinkSiteDomainTextField.getText() + "/d/"
                    + sinkSetIDTextField.getText());

        } catch (URISyntaxException uriE) {
            System.out.println("Could not open sink dataset URL");
        }
        return sinkDatasetURI;
    }

    private class PortMethodItemListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                PortMethod item = (PortMethod) e.getItem();
                switch (item) {
                    case copy_data:
                        sinkSetIDTextField.setText("");
                        sinkSetIDTextField.setEditable(true);
                        jobPanel.remove(publishDatasetContainerLeft);
                        jobPanel.remove(publishDatasetContainerRight);
                        publishDatasetComboBox.setEnabled(false);
                        jobPanel.add(publishMethodContainerLeft);
                        jobPanel.add(publishMethodContainerRight);
                        publishMethodComboBox.setEnabled(true);
                        jobPanel.updateUI();
                        break;
                    case copy_schema:
                    case copy_all:
                        sinkSetIDTextField.setText(DEFAULT_DESTINATION_SET_ID);
                        sinkSetIDTextField.setEditable(false);
                        jobPanel.remove(publishMethodContainerLeft);
                        jobPanel.remove(publishMethodContainerRight);
                        publishMethodComboBox.setEnabled(false);
                        jobPanel.add(publishDatasetContainerLeft);
                        jobPanel.add(publishDatasetContainerRight);
                        publishDatasetComboBox.setEnabled(true);
                        jobPanel.updateUI();
                        break;
                }
            }
        }
    }

    private class OpenDatasetButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (!sinkSetIDTextField.getText()
                    .equals(DEFAULT_DESTINATION_SET_ID)) {
                Utils.openWebpage(getURIToSinkDataset());
            }
        }
    }
}
