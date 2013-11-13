package com.socrata.datasync.ui;

import com.socrata.datasync.*;
import com.socrata.datasync.job.PortJob;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.BalloonTipStyle;
import net.java.balloontip.styles.EdgedBalloonStyle;
import net.java.balloontip.utils.ToolTipUtils;

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
    private final int JOB_FIELD_VGAP = 8;
    private final int SINK_DATASET_ID_TEXTFIELD_WIDTH = 210;
    private final int OPEN_SINK_DATASET_BUTTON_HEIGHT = 22;
    private final String DEFAULT_DESTINATION_SET_ID = "(Generates after running job)";
    private final String JOB_FILE_NAME = "Socrata Port Job";
    private final String JOB_FILE_EXTENSION = "spj";
    private final String HELP_ICON_FILE_PATH = "/help.png";

    private final String PORT_METHOD_TIP_TEXT = "<html>" +
            "Copy schema only: copies the source dataset's metadata and columns to new dataset (no rows will be copied).<br>" +
            "Copy schema and data: makes an identical copy of the source dataset as new dataset (including all rows).<br>" +
            "Copy data only: copies only the rows from source dataset to another existing dataset." +
            "</html>";
    private final String SOURCE_SITE_TIP_TEXT = "Domain where the source dataset is located.";
    private final String SOURCE_SET_TIP_TEXT = "The 4-4 ID of the source dataset (i.e. n38h-y5wp)";
    private final String SINK_SITE_TIP_TEXT = "Domain where the destination dataset is located.";
    private final String SINK_SET_TIP_TEXT = "If Port method is 'copy data only' enter the 4-4 ID of the existing dataset (i.e. n38h-y5wp) you wish to copy data to. " +
            "If 'copy schema' or 'copy schema and data' this field will be populated 4-4 ID of the newly created dataset";
    private final String PUBLISH_METHOD_TIP_TEXT = "The method to use when publishing the data to the destination dataset.";
    private final String PUBLISH_DATASET_TIP_TEXT = "If Yes, publish the newly created destination dataset, or if No, create it as an unpublished working copy.";

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
        FlowLayout flowLeft = new FlowLayout(FlowLayout.LEFT, 1, 0);
        FlowLayout flowRight = new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP);

        // load in help icon for balloontips
        final ImageIcon helpIcon = new ImageIcon(getClass().
                getResource(HELP_ICON_FILE_PATH));
        // set the style of the balloontips
        BalloonTipStyle style = new EdgedBalloonStyle(Color.LIGHT_GRAY, Color.BLUE);

        // Port Method
        JPanel portMethodContainerLeft = new JPanel(flowLeft);
        JLabel portMethodLabel = new JLabel("Port Method ");
        portMethodContainerLeft.add(portMethodLabel);
        JLabel portMethodHelp = new JLabel(helpIcon);
        BalloonTip portMethodTip = new BalloonTip(portMethodHelp, PORT_METHOD_TIP_TEXT,
                style, false);
        ToolTipUtils.balloonToToolTip(portMethodTip, 100, 100000);
        portMethodContainerLeft.add(portMethodHelp);
        jobPanel.add(portMethodContainerLeft);
        JPanel portMethodContainerRight = new JPanel(flowRight);
        portMethodComboBox = new JComboBox();
        for (PortMethod method : PortMethod.values()) {
            portMethodComboBox.addItem(method);
        }
        portMethodComboBox.addItemListener(new PortMethodItemListener());
        portMethodContainerRight.add(portMethodComboBox);
        jobPanel.add(portMethodContainerRight);

        // Source Site
        JPanel sourceSiteContainerLeft = new JPanel(flowLeft);
        JLabel sourceSiteLabel = new JLabel("Source Site ");
        sourceSiteContainerLeft.add(sourceSiteLabel);
        JLabel sourceSiteHelp = new JLabel(helpIcon);
        BalloonTip sourceSiteTip = new BalloonTip(sourceSiteHelp, SOURCE_SITE_TIP_TEXT, style, false);
        sourceSiteContainerLeft.add(sourceSiteHelp);
        jobPanel.add(sourceSiteContainerLeft);
        ToolTipUtils.balloonToToolTip(sourceSiteTip, 100, 100000);
        JPanel sourceSiteTextFieldContainer = new JPanel(flowRight);
        sourceSiteDomainTextField = new JTextField();
        sourceSiteDomainTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sourceSiteTextFieldContainer.add(sourceSiteDomainTextField);
        jobPanel.add(sourceSiteTextFieldContainer);

        // Source Set Dataset ID
        JPanel sourceSetContainerLeft = new JPanel(flowLeft);
        JLabel sourceSetLabel = new JLabel("Source Dataset ID ");
        sourceSetContainerLeft.add(sourceSetLabel);
        JLabel sourceSetHelp = new JLabel(helpIcon);
        BalloonTip sourceSetTip = new BalloonTip(sourceSetHelp, SOURCE_SET_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(sourceSetTip, 100, 100000);
        sourceSetContainerLeft.add(sourceSetHelp);
        jobPanel.add(sourceSetContainerLeft);
        JPanel sourceSetIDTextFieldContainer = new JPanel(flowRight);
        sourceSetIDTextField = new JTextField();
        sourceSetIDTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sourceSetIDTextFieldContainer.add(sourceSetIDTextField);
        jobPanel.add(sourceSetIDTextFieldContainer);

        // Sink Site
        JPanel sinkSiteContainerLeft = new JPanel(flowLeft);
        JLabel sinkSiteLabel = new JLabel("Destination Site ");
        sinkSiteContainerLeft.add(sinkSiteLabel);
        JLabel sinkSiteHelp = new JLabel(helpIcon);
        BalloonTip sinkSiteTip = new BalloonTip(sinkSiteHelp, SINK_SITE_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(sinkSiteTip, 100, 100000);
        sinkSiteContainerLeft.add(sinkSiteHelp);
        jobPanel.add(sinkSiteContainerLeft);
        JPanel sinkSiteTextFieldContainer = new JPanel(flowRight);
        sinkSiteDomainTextField = new JTextField();
        sinkSiteDomainTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sinkSiteTextFieldContainer.add(sinkSiteDomainTextField);
        jobPanel.add(sinkSiteTextFieldContainer);

        // Sink Site Dataset ID
        JPanel sinkSetContainerLeft = new JPanel(flowLeft);
        JLabel sinkSetLabel = new JLabel("Destination Dataset ID ");
        sinkSetContainerLeft.add(sinkSetLabel);
        JLabel sinkSetHelp = new JLabel(helpIcon);
        BalloonTip sinkSetTip = new BalloonTip(sinkSetHelp, SINK_SET_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(sinkSetTip, 100, 1000000);
        sinkSetContainerLeft.add(sinkSetHelp);
        jobPanel.add(sinkSetContainerLeft);
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
        publishMethodContainerLeft = new JPanel(flowLeft);
        JLabel publishMethodLabel = new JLabel("Publish Method ");
        publishMethodContainerLeft.add(publishMethodLabel);
        JLabel publishMethodHelp = new JLabel(helpIcon);
        BalloonTip publishMethodTip = new BalloonTip(publishMethodHelp, PUBLISH_METHOD_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(publishMethodTip, 100, 1000000);
        publishMethodContainerLeft.add(publishMethodHelp);
        jobPanel.add(publishMethodContainerLeft);
        publishMethodContainerRight = new JPanel(flowRight);
        publishMethodComboBox = new JComboBox();
        for (PublishMethod method : PublishMethod.values()) {
            // TODO: clean this up once publish method changes have been implemented
            if (!method.equals(PublishMethod.append)) {
                publishMethodComboBox.addItem(method);
            }
        }
        publishMethodComboBox.setEnabled(false);
        publishMethodContainerRight.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        publishMethodContainerRight.add(publishMethodComboBox);

        // Publish Destination Dataset (toggles with Publish Method based on Port Method choice)
        // We will build out the specs of this element without adding it to the jobPanel.
        publishDatasetContainerLeft = new JPanel(flowLeft);
        JLabel publishDatasetLabel = new JLabel("Publish Destination Dataset? ");
        publishDatasetContainerLeft.add(publishDatasetLabel);
        JLabel publishDatasetHelp = new JLabel(helpIcon);
        BalloonTip publishDatasetTip = new BalloonTip(publishDatasetHelp, PUBLISH_DATASET_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(publishDatasetTip, 100, 100000);
        publishDatasetContainerLeft.add(publishDatasetHelp);
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
            jobPanel.add(publishDatasetContainerLeft);
            jobPanel.add(publishDatasetContainerRight);
            publishDatasetComboBox.setEnabled(true);
        } else {
            jobPanel.add(publishMethodContainerLeft);
            jobPanel.add(publishMethodContainerRight);
            publishMethodComboBox.setEnabled(true);
        }
        sourceSiteDomainTextField.setText(job.getSourceSiteDomain());
        sourceSetIDTextField.setText(job.getSourceSetID());
        sinkSiteDomainTextField.setText(job.getSinkSiteDomain());
        sinkSetIDTextField.setText(job.getSinkSetID());
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
        boolean updateJobCommandTextField = false;
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

    public JLabel getJobTabTitleLabel() {
        return jobTabTitleLabel;
    }

    public String getJobFileLocation() {
        return jobFileLocation;
    }

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
                IntegrationUtility.openWebpage(getURIToSinkDataset());
            }
        }
    }
}
