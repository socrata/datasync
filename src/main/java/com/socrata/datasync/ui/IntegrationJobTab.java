package com.socrata.datasync.ui;

import com.socrata.datasync.*;
import com.socrata.datasync.job.IntegrationJob;
import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.BalloonTipStyle;
import net.java.balloontip.styles.ToolTipBalloonStyle;
import net.java.balloontip.utils.ToolTipUtils;

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
    private final String HELP_ICON_FILE_PATH = "/help.png";

    private final String FILE_TO_PUBLISH_TIP_TEXT = "CSV or TSV file containing the data to be published";
    private final String HAS_HEADER_ROW_TIP_TEXT = "<html><body style='width: 300px'>Check this box if the first row in the CSV/TSV contains the column names in the dataset.<br>" +
            "If the CSV/TSV does not have a header row the order of rows must exactly match the order in the dataset.</body></html>";
    private final String DATASET_ID_TIP_TEXT = "<html><body style='width: 300px'>The identifier in the form of xxxx-xxxx (i.e. n38h-y5wp) " +
            "of the Socrata dataset where the data will be published</body></html>";
    private final String PUBLISH_METHOD_TIP_TEXT = "<html><body style='width: 400px'>Method used to publish data:<br>" +
            "<strong>upsert</strong>: update any rows that already exist and append any new rows." +
            "<br>NOTE: updating rows requires the dataset to have Row Identifier.<br>" +
            "<strong>append</strong>: adds all rows in the CSV/TSV as new rows.<br>" +
            "<strong>replace</strong>: simply replaces the dataset with the data in the CSV/TSV file to publish." +
            "</body></html>";
    private final String RUN_COMMAND_TIP_TEXT = "<html><body style='width: 300px'>After saving the job this field will be populated with a command-line command that can be used to run the job." +
            " This command can be input into tools such as the Windows Scheduler or ETL tools to run the job automatically.</body></html>";

    private JFrame mainFrame;
    private JPanel jobPanel;

    private String jobFileLocation;
    private JLabel jobTabTitleLabel;

    private JTextField datasetIDTextField;
    private JTextField fileToPublishTextField;
    private JCheckBox fileToPublishHasHeaderCheckBox;
    private JComboBox publishMethodComboBox;
    private JTextField runCommandTextField;

    // build Container with all tab components populated with given job data
    public IntegrationJobTab(IntegrationJob job, JFrame containingFrame) {
        mainFrame = containingFrame;

        // build tab panel form
        jobPanel = new JPanel(new GridLayout(0,2));

        // set FlowLayouts
        FlowLayout flowLeft = new FlowLayout(FlowLayout.LEFT, 0, 0);
        FlowLayout flowRight = new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP);

        // load in help icon for balloontips
        final ImageIcon helpIcon = new ImageIcon(getClass()
                .getResource(HELP_ICON_FILE_PATH));
        //set the style of the balloontips
        BalloonTipStyle style = new ToolTipBalloonStyle(Color.LIGHT_GRAY, Color.BLUE);

        // File to Publish
        JPanel fileToPublishLabelContainer = new JPanel(flowLeft);
        JLabel fileToPublishLabel = new JLabel("File to publish");
        fileToPublishLabelContainer.add(fileToPublishLabel);
        JLabel fileToPublishHelp = new JLabel(helpIcon);
        BalloonTip fileToPublishTip = new BalloonTip(fileToPublishHelp, FILE_TO_PUBLISH_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(fileToPublishTip, 100, 100000);
        fileToPublishLabelContainer.add(fileToPublishHelp);
        jobPanel.add(fileToPublishLabelContainer);
        JPanel fileSelectorContainer = new JPanel(flowRight);
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
        //jobPanel.add(fileToPublishHasHeaderCheckBox);

        JPanel hasHeaderRowLabelContainer = new JPanel(flowLeft);
        hasHeaderRowLabelContainer.add(fileToPublishHasHeaderCheckBox);
        JLabel hasHeaderRowHelp = new JLabel(helpIcon);
        BalloonTip hasHeaderRowTip = new BalloonTip(hasHeaderRowHelp, HAS_HEADER_ROW_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(hasHeaderRowTip, 100, 100000);
        hasHeaderRowLabelContainer.add(hasHeaderRowHelp);
        jobPanel.add(hasHeaderRowLabelContainer);

        JPanel datasetLabelContainer = new JPanel(flowLeft);
        JLabel datasetLabel = new JLabel("Dataset ID ");
        datasetLabelContainer.add(datasetLabel);
        JLabel datasetHelp = new JLabel(helpIcon);
        BalloonTip datasetTip = new BalloonTip(datasetHelp, DATASET_ID_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(datasetTip, 100, 100000);
        datasetLabelContainer.add(datasetHelp);
        jobPanel.add(datasetLabelContainer);
        JPanel datasetIDTextFieldContainer = new JPanel(flowRight);
        datasetIDTextField = new JTextField();
        datasetIDTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        datasetIDTextFieldContainer.add(datasetIDTextField);
        jobPanel.add(datasetIDTextFieldContainer);

        JPanel publishMethodLabelContainer = new JPanel(flowLeft);
        JLabel publishMethodLabel = new JLabel("Publish method ");
        publishMethodLabelContainer.add(publishMethodLabel);
        JLabel publishMethodHelp = new JLabel(helpIcon);
        BalloonTip publishMethodTip = new BalloonTip(publishMethodHelp, PUBLISH_METHOD_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(publishMethodTip, 100, 100000);
        publishMethodLabelContainer.add(publishMethodHelp);
        jobPanel.add(publishMethodLabelContainer);
        JPanel publishMethodTextFieldContainer = new JPanel(flowRight);
        publishMethodComboBox = new JComboBox();
        for(PublishMethod method : PublishMethod.values()) {
            publishMethodComboBox.addItem(method);
        }
        publishMethodTextFieldContainer.add(publishMethodComboBox);
        jobPanel.add(publishMethodTextFieldContainer);

        JPanel runCommandLabelContainer = new JPanel(flowLeft);
        JLabel runCommandLabel = new JLabel("Command to execute with scheduler ");
        runCommandLabelContainer.add(runCommandLabel);
        JLabel runCommandHelp = new JLabel(helpIcon);
        BalloonTip runCommandTip = new BalloonTip(runCommandHelp, RUN_COMMAND_TIP_TEXT, style, false);
        ToolTipUtils.balloonToToolTip(runCommandTip, 100, 100000);
        runCommandLabelContainer.add(runCommandHelp);
        jobPanel.add(runCommandLabelContainer);
        JPanel runCommandTextFieldContainer = new JPanel(flowRight);
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

        // Load job data into fields
        datasetIDTextField.setText(job.getDatasetID());
        fileToPublishTextField.setText(job.getFileToPublish());
        PublishMethod jobPublishMethod = job.getPublishMethod();
        publishMethodComboBox.setSelectedItem(jobPublishMethod);
        fileToPublishHasHeaderCheckBox.setSelected(job.getFileToPublishHasHeaderRow());

        jobFileLocation = job.getPathToSavedFile();
        // if this is an existing job (meaning the job was opened from a file)
        // then populate the scheduler command textfield
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
        jobToRun.setDatasetID(datasetIDTextField.getText());
        jobToRun.setFileToPublish(fileToPublishTextField.getText());
        jobToRun.setPublishMethod(
                (PublishMethod) publishMethodComboBox.getSelectedItem());
        jobToRun.setFileToPublishHasHeaderRow(fileToPublishHasHeaderCheckBox.isSelected());

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
        newIntegrationJob.setFileToPublishHasHeaderRow(fileToPublishHasHeaderCheckBox.isSelected());
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

            String extensionsMsg = "";
            int numExtensions = IntegrationJob.allowedFileToPublishExtensions.size();
            String[] allowedFileExtensions = new String[numExtensions];
            for(int i = 0; i < numExtensions; i++) {
                if(i > 0)
                    extensionsMsg += ", ";
                allowedFileExtensions[i] = IntegrationJob.allowedFileToPublishExtensions.get(i);
                extensionsMsg += "*." + allowedFileExtensions[i];
            }
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    extensionsMsg, allowedFileExtensions);
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
