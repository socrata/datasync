package com.socrata.datasync.ui;

import com.socrata.datasync.*;
import com.socrata.datasync.job.IntegrationJob;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.*;
import java.util.List;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 9/11/13
 */
public class IntegrationJobTab implements JobTab {

    private static final int JOB_TEXTFIELD_WIDTH = 370;
    private static final int JOB_COMMAND_TEXTFIELD_WIDTH = 211;
    private static final int JOB_FILE_TEXTFIELD_WIDTH = 238;
    private static final int JOB_TEXTFIELD_HEIGHT = 26;
    private static final int JOB_FIELD_VGAP = 8;
    private static final FlowLayout FLOW_LEFT = new FlowLayout(FlowLayout.LEFT, 0, 0);
    private static final FlowLayout FLOW_RIGHT = new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP);

    private static final String DEFAULT_RUN_JOB_COMMAND = "(Generates when job is saved)";

    private static final String JOB_FILE_NAME = "Socrata Integration Job";
    private static final String JOB_FILE_EXTENSION = "sij";

    private static final int HELP_ICON_TOP_PADDING = 12;
    private static final String FILE_TO_PUBLISH_TIP_TEXT = "CSV or TSV file containing the data to be published";
    private static final String HAS_HEADER_ROW_TIP_TEXT = "<html><body style='width: 300px'>Check this box if the first row in the CSV/TSV contains the column names in the dataset.<br>" +
            "If the CSV/TSV does not have a header row the order of rows must exactly match the order in the dataset.</body></html>";
    private static final String DATASET_ID_TIP_TEXT = "<html><body style='width: 300px'>The identifier in the form of xxxx-xxxx (e.g. n38h-y5wp) " +
            "of the Socrata dataset where the data will be published</body></html>";
    private static final String PUBLISH_METHOD_TIP_TEXT = "<html><body style='width: 400px'>Method used to publish data:<br>" +
            "<strong>upsert</strong>: update rows that already exist and append any new rows.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; " +
            "NOTE: updating rows requires the dataset to have Row Identifier<br>" +
            "<strong>append</strong>: adds all rows in the CSV/TSV as new rows.<br>" +
            "<strong>replace</strong>: simply replaces the dataset with the data in the CSV/TSV file to publish.<br>" +
            "<strong>delete</strong>: delete all rows matching Row Identifiers given in CSV/TSV file.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; " +
            "NOTE: requires dataset to have Row Identifier." +
            "</body></html>";
    private static final String PUBLISH_VIA_FTP_SMARTUPDATE_ROW_TIP_TEXT = "TODO...";
    private static final String FTP_CONTROL_FILE_TIP_TEXT = "TODO...";
    private static final String RUN_COMMAND_TIP_TEXT = "<html><body style='width: 300px'>After saving the job this field will be populated with a command-line command that can be used to run the job." +
            " This command can be input into tools such as the Windows Scheduler or ETL tools to run the job automatically.</body></html>";

    private JFrame mainFrame;
    private JPanel jobPanel;

    private String jobFileLocation;
    private JLabel jobTabTitleLabel;

    private JTextField datasetIDTextField;
    private JTextField fileToPublishTextField;
    private JCheckBox fileToPublishHasHeaderCheckBox;
    private JComboBox publishMethodComboBox;
    private JCheckBox publishViaFTPSmartUpdateCheckBox;
    private JTextField ftpControlFileTextField;
    private JPanel ftpControlFileLabelContainer;
    private JPanel ftpControlFileSelectorContainer;
    private JTextField runCommandTextField;

    // build Container with all tab components populated with given job data
    public IntegrationJobTab(IntegrationJob job, JFrame containingFrame) {
        mainFrame = containingFrame;

        // build tab panel form
        jobPanel = new JPanel(new GridLayout(0,2));

        addFileToPublishFieldToJobPanel();
        addDatasetIdFieldToJobPanel();
        addPublishMethodFieldToJobPanel();
        addFtpControlFileFieldToJobPanel();
        addRunCommandFieldToJobPanel();

        loadJobDataIntoUIFields(job);
    }

    private void addFtpControlFileFieldToJobPanel() {
        ftpControlFileLabelContainer = UIUtility.generateLabelWithHelpBubble(
                "FTP SmartUpdate control file", FTP_CONTROL_FILE_TIP_TEXT, HELP_ICON_TOP_PADDING);
        jobPanel.add(ftpControlFileLabelContainer);

        ftpControlFileSelectorContainer = new JPanel(FLOW_RIGHT);
        ftpControlFileTextField = new JTextField();
        ftpControlFileTextField.setPreferredSize(new Dimension(
                JOB_FILE_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        ftpControlFileSelectorContainer.add(ftpControlFileTextField);
        JFileChooser ftpControlFileChooser = new JFileChooser();
        JButton openButton = new JButton("Select a file...");
        FtpControlFileSelectorListener chooserListener = new FtpControlFileSelectorListener(
                ftpControlFileChooser, ftpControlFileTextField);
        openButton.addActionListener(chooserListener);
        ftpControlFileSelectorContainer.add(openButton);
        jobPanel.add(ftpControlFileSelectorContainer);
    }

    private void addRunCommandFieldToJobPanel() {
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Command to execute with scheduler", RUN_COMMAND_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel runCommandTextFieldContainer = new JPanel(FLOW_RIGHT);
        runCommandTextField = new JTextField(DEFAULT_RUN_JOB_COMMAND);
        runCommandTextField.setPreferredSize(new Dimension(
                JOB_COMMAND_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        runCommandTextField.setEditable(false);
        runCommandTextField.addMouseListener(new JobCommandTextFieldListener());
        runCommandTextFieldContainer.add(runCommandTextField);
        JButton copyJobCommandButton = new JButton("Copy to clipboard");
        copyJobCommandButton.addActionListener(new CopyJobCommandListener());
        runCommandTextFieldContainer.add(copyJobCommandButton);
        jobPanel.add(runCommandTextFieldContainer);
    }

    private void addPublishMethodFieldToJobPanel() {
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Publish method", PUBLISH_METHOD_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel publishMethodTextFieldContainer = new JPanel(FLOW_RIGHT);
        publishMethodComboBox = new JComboBox();
        for(PublishMethod method : PublishMethod.values()) {
            publishMethodComboBox.addItem(method);
        }
        publishMethodComboBox.addActionListener(new PublishMethodComboBoxListener());

        publishMethodTextFieldContainer.add(publishMethodComboBox);

        publishViaFTPSmartUpdateCheckBox = new JCheckBox("Publish via FTP \"SmartUpdate\"");
        publishViaFTPSmartUpdateCheckBox.addActionListener(
                new PublishViaFTPSmartUpdateCheckBoxListener());
        JPanel publishViaFTPSmartUpdateLabelContainer = new JPanel(FLOW_LEFT);
        publishViaFTPSmartUpdateLabelContainer.add(publishViaFTPSmartUpdateCheckBox);
        publishViaFTPSmartUpdateLabelContainer.add(
                UIUtility.generateHelpBubble(PUBLISH_VIA_FTP_SMARTUPDATE_ROW_TIP_TEXT));
        publishMethodTextFieldContainer.add(publishViaFTPSmartUpdateLabelContainer);

        jobPanel.add(publishMethodTextFieldContainer);
    }

    private void addDatasetIdFieldToJobPanel() {
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Dataset ID", DATASET_ID_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel datasetIDTextFieldContainer = new JPanel(FLOW_RIGHT);
        datasetIDTextField = new JTextField();
        datasetIDTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        datasetIDTextFieldContainer.add(datasetIDTextField);
        jobPanel.add(datasetIDTextFieldContainer);
    }

    private void addFileToPublishFieldToJobPanel() {
        jobPanel.add(
                UIUtility.generateLabelWithHelpBubble("File to publish", FILE_TO_PUBLISH_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel fileSelectorContainer = new JPanel(FLOW_RIGHT);
        fileToPublishTextField = new JTextField();
        fileToPublishTextField.setPreferredSize(new Dimension(
                JOB_FILE_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        fileSelectorContainer.add(fileToPublishTextField);
        JFileChooser fileToPublishChooser = new JFileChooser();
        JButton openButton = new JButton("Select a file...");
        FileToPublishSelectorListener chooserListener = new FileToPublishSelectorListener(
                fileToPublishChooser, fileToPublishTextField);
        openButton.addActionListener(chooserListener);
        fileSelectorContainer.add(openButton);
        jobPanel.add(fileSelectorContainer);

        jobPanel.add(new JLabel(""));
        fileToPublishHasHeaderCheckBox = new JCheckBox("File to publish contains a header row");

        JPanel hasHeaderRowLabelContainer = new JPanel(FLOW_LEFT);
        hasHeaderRowLabelContainer.add(fileToPublishHasHeaderCheckBox);
        hasHeaderRowLabelContainer.add(
                UIUtility.generateHelpBubble(HAS_HEADER_ROW_TIP_TEXT));
        jobPanel.add(hasHeaderRowLabelContainer);
    }

    private void loadJobDataIntoUIFields(IntegrationJob job) {
        datasetIDTextField.setText(job.getDatasetID());
        fileToPublishTextField.setText(job.getFileToPublish());
        PublishMethod jobPublishMethod = job.getPublishMethod();
        publishMethodComboBox.setSelectedItem(jobPublishMethod);
        fileToPublishHasHeaderCheckBox.setSelected(job.getFileToPublishHasHeaderRow());

        updatePublishViaFTPSmartUpdateUIFields(job.getPublishMethod(),
                job.getPublishViaFTP());
        ftpControlFileTextField.setText(job.getPathToFTPControlFile());

        jobFileLocation = job.getPathToSavedFile();
        // if this is an existing job (meaning the job was opened from a file)
        // then populate the scheduler command textfield
        if(!jobFileLocation.equals("")) {
            runCommandTextField.setText(
                    IntegrationUtility.getRunJobCommand(jobFileLocation));
        }

        jobTabTitleLabel = new JLabel(job.getJobFilename());
    }

    private void updatePublishViaFTPSmartUpdateUIFields(PublishMethod publishMethod,
                                                        boolean publishViaFTP) {
        publishViaFTPSmartUpdateCheckBox.setSelected(publishViaFTP);
        publishViaFTPSmartUpdateCheckBox.setVisible(
                publishMethod.equals(PublishMethod.replace));
        if(publishViaFTP) {
            ftpControlFileLabelContainer.setVisible(true);
            ftpControlFileSelectorContainer.setVisible(true);
        } else {
            ftpControlFileLabelContainer.setVisible(false);
            ftpControlFileSelectorContainer.setVisible(false);
        }
        jobPanel.updateUI();
    }

    public JPanel getTabPanel() {
        return jobPanel;
    }

    public JobStatus runJobNow() {
        IntegrationJob jobToRun = new IntegrationJob();
        jobToRun.setDatasetID(datasetIDTextField.getText());
        jobToRun.setFileToPublish(fileToPublishTextField.getText());
        jobToRun.setPublishMethod(
                (PublishMethod) publishMethodComboBox.getSelectedItem());
        jobToRun.setFileToPublishHasHeaderRow(fileToPublishHasHeaderCheckBox.isSelected());
        jobToRun.setPublishViaFTP(publishViaFTPSmartUpdateCheckBox.isSelected());
        jobToRun.setPathToFTPControlFile(ftpControlFileTextField.getText());
        return jobToRun.run();
    }

    public void saveJob() {
        // Save job data
        IntegrationJob newIntegrationJob = new IntegrationJob();
        newIntegrationJob.setDatasetID(datasetIDTextField.getText());
        newIntegrationJob.setFileToPublish(fileToPublishTextField.getText());
        newIntegrationJob.setPublishMethod(
                (PublishMethod) publishMethodComboBox.getSelectedItem());
        newIntegrationJob.setFileToPublishHasHeaderRow(fileToPublishHasHeaderCheckBox.isSelected());
        newIntegrationJob.setPublishViaFTP(publishViaFTPSmartUpdateCheckBox.isSelected());
        newIntegrationJob.setPathToFTPControlFile(ftpControlFileTextField.getText());
        newIntegrationJob.setPathToSavedFile(jobFileLocation);

        // TODO If an existing file was selected WARN user of overwriting

        // if first time saving this job: Open dialog box to select "Save as..." location
        // otherwise save to existing file
        boolean updateJobCommandTextField = false;
        String selectedJobFileLocation = jobFileLocation;
        if(selectedJobFileLocation.equals("")) {
            JFileChooser savedJobFileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    JOB_FILE_NAME + " (*." + JOB_FILE_EXTENSION + ")", JOB_FILE_EXTENSION);
            savedJobFileChooser.setFileFilter(filter);
            int returnVal = savedJobFileChooser.showSaveDialog(mainFrame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = savedJobFileChooser.getSelectedFile();
                selectedJobFileLocation = file.getAbsolutePath();
                if(!selectedJobFileLocation.endsWith("." + JOB_FILE_EXTENSION)) {
                    selectedJobFileLocation += "." + JOB_FILE_EXTENSION;
                }
                jobFileLocation = selectedJobFileLocation;
                newIntegrationJob.setPathToSavedFile(selectedJobFileLocation);
                jobTabTitleLabel.setText(newIntegrationJob.getJobFilename());
                updateJobCommandTextField = true;
            }
        }
        // actually save the job file (may overwrite)
        try {
            newIntegrationJob.writeToFile(selectedJobFileLocation);

            // Update job tab title label
            jobTabTitleLabel.setText(newIntegrationJob.getJobFilename());

            // Update the textfield with new command
            if(updateJobCommandTextField) {
                String runJobCommand = IntegrationUtility.getRunJobCommand(
                        newIntegrationJob.getPathToSavedFile());
                runCommandTextField.setText(runJobCommand);
            }
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

    private class FileToPublishSelectorListener implements ActionListener {
        JFileChooser fileChooser;
        JTextField filePathTextField;

        public FileToPublishSelectorListener(JFileChooser chooser, JTextField textField) {
            fileChooser = chooser;
            filePathTextField = textField;
            fileChooser.setFileFilter(
                    getFileChooserFilter(IntegrationJob.allowedFileToPublishExtensions));
        }

        public void actionPerformed(ActionEvent e) {
            int returnVal = fileChooser.showOpenDialog(mainFrame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                filePathTextField.setText(file.getAbsolutePath());
            } else {
                // Open command cancelled by user: do nothing
            }
        }
    }

    private class FtpControlFileSelectorListener implements ActionListener {
        JFileChooser fileChooser;
        JTextField filePathTextField;

        public FtpControlFileSelectorListener(JFileChooser chooser, JTextField textField) {
            fileChooser = chooser;
            filePathTextField = textField;
            fileChooser.setFileFilter(
                    getFileChooserFilter(IntegrationJob.allowedFtpControlFileExtensions));
        }

        public void actionPerformed(ActionEvent e) {
            int returnVal = fileChooser.showOpenDialog(mainFrame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                filePathTextField.setText(file.getAbsolutePath());
            } else {
                // Open command cancelled by user: do nothing
            }
        }
    }

    private class PublishMethodComboBoxListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            PublishMethod selectedPublishMethod =
                    (PublishMethod) publishMethodComboBox.getSelectedItem();
            boolean setPublishViaFTPAsChecked = 
                    selectedPublishMethod.equals(PublishMethod.replace);
            updatePublishViaFTPSmartUpdateUIFields(selectedPublishMethod, setPublishViaFTPAsChecked);
        }
    }

    private class PublishViaFTPSmartUpdateCheckBoxListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updatePublishViaFTPSmartUpdateUIFields(
                    (PublishMethod) publishMethodComboBox.getSelectedItem(),
                    publishViaFTPSmartUpdateCheckBox.isSelected());
        }
    }

    private FileNameExtensionFilter getFileChooserFilter(List<String> allowedExtensions) {
        String extensionsMsg = "";
        int numExtensions = allowedExtensions.size();
        String[] allowedFileExtensions = new String[numExtensions];
        for(int i = 0; i < numExtensions; i++) {
            if(i > 0)
                extensionsMsg += ", ";
            allowedFileExtensions[i] = allowedExtensions.get(i);
            extensionsMsg += "*." + allowedFileExtensions[i];
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                extensionsMsg, allowedFileExtensions);
        return filter;
    }

    private class JobCommandTextFieldListener implements MouseListener {
        @Override
        public void mouseClicked(MouseEvent e) {
            JTextField jobCommandTextField = (JTextField) e.getSource();
            jobCommandTextField.selectAll();
        }
        @Override
        public void mouseExited(MouseEvent e) { }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) { }
        @Override
        public void mouseReleased(MouseEvent e) { }
    }

    private class CopyJobCommandListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String runJobCommand = runCommandTextField.getText();
            StringSelection stringSelection = new StringSelection(runJobCommand);
            Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
            clpbrd.setContents(stringSelection, null);
        }
    }

}
