package com.socrata.datasync.ui;

import com.socrata.datasync.*;
import com.socrata.datasync.job.IntegrationJob;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 9/11/13
 */
public class IntegrationJobTab implements JobTab {

    private final int JOB_TEXTFIELD_WIDTH = 370;
    private final int JOB_COMMAND_TEXTFIELD_WIDTH = 211;
    private final int JOB_FILE_TEXTFIELD_WIDTH = 238;
    private final int JOB_TEXTFIELD_HEIGHT = 26;
    private final int JOB_FIELD_VGAP = 8;

    private final String DEFAULT_RUN_JOB_COMMAND = "(Generates when job is saved)";

    private final String JOB_FILE_NAME = "Socrata Integration Job";
    private final String JOB_FILE_EXTENSION = "sij";

    private JFrame mainFrame;
    private JPanel jobPanel;

    private String jobFileLocation;
    private JLabel jobTabTitleLabel;

    private JTextField datasetIDTextField;
    private JTextField fileToPublishTextField;
    private JComboBox publishMethodComboBox;
    private JTextField runCommandTextField;

    // build Container with all tab components
    public IntegrationJobTab(IntegrationJob job, JFrame containingFrame) {
        mainFrame = containingFrame;

        // build tab panel form
        jobPanel = new JPanel(new GridLayout(0,2));

        jobPanel.add(new JLabel("File to publish"));
        JPanel fileSelectorContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
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

        jobPanel.add(new JLabel("Dataset ID (i.e. n38h-y5wp)"));
        JPanel datasetIDTextFieldContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
        datasetIDTextField = new JTextField();
        datasetIDTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        datasetIDTextFieldContainer.add(datasetIDTextField);
        jobPanel.add(datasetIDTextFieldContainer);

        jobPanel.add(new JLabel("Publish method"));
        JPanel publishMethodTextFieldContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
        publishMethodComboBox = new JComboBox();
        for(PublishMethod method : PublishMethod.values()) {
            publishMethodComboBox.addItem(method);
        }
        publishMethodTextFieldContainer.add(publishMethodComboBox);
        jobPanel.add(publishMethodTextFieldContainer);

        // Load job data into fields
        datasetIDTextField.setText(job.getDatasetID());
        fileToPublishTextField.setText(job.getFileToPublish());
        PublishMethod jobPublishMethod = job.getPublishMethod();
        publishMethodComboBox.setSelectedItem(jobPublishMethod);

        jobPanel.add(new JLabel("Command to execute with scheduler"));
        JPanel runCommandTextFieldContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
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

        jobFileLocation = job.getPathToSavedFile();

        // if this is an existing job (meaning the job was opened from a file) -> populate the scheduler command textfield
        if(!jobFileLocation.equals("")) {
            runCommandTextField.setText(
                    IntegrationUtility.getRunJobCommand(jobFileLocation));
        }

        jobTabTitleLabel = new JLabel(job.getJobFilename());
    }

    public JPanel getTabPanel() {
        return jobPanel;
    }

    public JobStatus runJobNow() {
        IntegrationJob jobToRun = new IntegrationJob();
        jobToRun.setDatasetID(
                datasetIDTextField.getText());
        jobToRun.setFileToPublish(
                fileToPublishTextField.getText());
        jobToRun.setPublishMethod(
                (PublishMethod) publishMethodComboBox.getSelectedItem());

        // TODO include delete file
        //jobToRun.setFileColsToDelete("/home/adrian/delete.csv");

        return jobToRun.run();
    }

    public void saveJob() {
        // Save job data
        IntegrationJob newIntegrationJob = new IntegrationJob();
        newIntegrationJob.setDatasetID(datasetIDTextField.getText());
        newIntegrationJob.setFileToPublish(fileToPublishTextField.getText());
        newIntegrationJob.setPublishMethod(
                (PublishMethod) publishMethodComboBox.getSelectedItem());
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
                String runJobCommand = IntegrationUtility.getRunJobCommand(newIntegrationJob.getPathToSavedFile());
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

            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "*.csv, *.json", "csv", "json");
            fileChooser.setFileFilter(filter);
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
