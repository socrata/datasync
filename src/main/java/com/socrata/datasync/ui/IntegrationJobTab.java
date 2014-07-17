package com.socrata.datasync.ui;

import com.socrata.api.SodaDdl;
import com.socrata.datasync.*;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.job.JobStatus;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Dataset;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 9/11/13
 */
public class IntegrationJobTab implements JobTab {

    private static final int DATASET_ID_TEXTFIELD_WIDTH = 160;
    private static final int JOB_COMMAND_TEXTFIELD_WIDTH = 212;
    private static final int JOB_FILE_TEXTFIELD_WIDTH = 263;
    private static final int JOB_TEXTFIELD_HEIGHT = 26;
    private static final int JOB_FIELD_VGAP = 5;
    private static final int CONTROL_FILE_TEXTFIELD_WIDTH = 102;
    private static final Dimension CONTROL_FILE_EDITOR_DIMENSIONS = new Dimension(500, 350);
    private static final FlowLayout FLOW_LEFT = new FlowLayout(FlowLayout.LEFT, 0, 0);
    private static final FlowLayout FLOW_RIGHT = new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP);

    private static final String DEFAULT_RUN_JOB_COMMAND = "(Generates when job is saved)";
    private static final String BROWSE_BUTTON_TEXT = "Browse...";
    private static final String EMPTY_TEXTAREA_CONTENT = "";

    private static final String JOB_FILE_NAME = "Socrata Integration Job";
    private static final String JOB_FILE_EXTENSION = "sij";

    private static final int HELP_ICON_TOP_PADDING = 12;
    private static final String FILE_TO_PUBLISH_TIP_TEXT = "CSV or TSV file containing the data to be published";
    private static final String HAS_HEADER_ROW_TIP_TEXT = "<html><body style='width: 300px'>Check this box if the first row in the CSV/TSV " +
            "contains the column identifiers (API field names) in the dataset.<br>" +
            "If the CSV/TSV does not have a header row the order of rows must exactly match the order in the dataset.</body></html>";
    private static final String DATASET_ID_TIP_TEXT = "<html><body style='width: 300px'>The identifier in the form of xxxx-xxxx (e.g. n38h-y5wp) " +
            "of the Socrata dataset where the data will be published</body></html>";
    private static final String PUBLISH_METHOD_TIP_TEXT = "<html><body style='width: 400px'>Method used to publish data:<br>" +
            "<strong>replace</strong>: simply replaces the dataset with the data in the CSV/TSV file to publish.<br>" +
            "<strong>upsert</strong>: update rows that already exist and append any new rows.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; " +
            "NOTE: updating rows requires the dataset to have Row Identifier<br>" +
            "<strong>append</strong>: adds all rows in the CSV/TSV as new rows.<br>" +
            "<strong>delete</strong>: delete all rows matching Row Identifiers given in CSV/TSV file. " +
            "CSV/TSV should only contain a single column listing the Row Identifiers to delete.<br>" +
            "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp; " +
            "NOTE: requires dataset to have Row Identifier." +
            "</body></html>";
    private static final String PUBLISH_VIA_FTP_ROW_TIP_TEXT = "<html><body style='width: 400px'>'Replace via HTTP' is the preferred " +
            "and most efficient publishing method. Sends CSV/TSV file over HTTP, automatically detects " +
            "changes since last update, and only updates new/changed rows.<br>" +
            "<strong>NOTE</strong>: If you choose FTP, your firewall may need to be configured to allow FTP traffic through ports " +
            "22222 (for the control connection) and all ports within the range of 3131 to 3141 (for data connection)</body></html>";
    private static final String CONTROL_FILE_TIP_TEXT = "<html><body style='width: 300px'>" +
            "Establishes import configuration such as date formatting and Location column being populated" +
            " from existing columns (for more information refer to Help -> control file configuration)</body></html>";
    private static final String GET_COLUMN_IDS_TIP_TEXT = "<html><body style='width: 400px'>" +
            "Displays a comma-separated list of the column identifiers (API field names) for the" +
            " dataset with the given ID (should be used as the header row of the CSV/TSV)" +
            "</body></html>";
    private static final String RUN_COMMAND_TIP_TEXT = "<html><body style='width: 300px'>After saving the job this field will be populated with a command-line command that can be used to run the job." +
            " This command can be input into tools such as the Windows Task Scheduler or ETL tools to run the job automatically.</body></html>";
    public static final String CONTAINS_A_HEADER_ROW_CHECKBOX_TEXT = "File to publish contains a header row";
    public static final String PUBLISH_VIA_SODA_RADIO_TEXT = "SODA2";
    public static final String PUBLISH_VIA_FTP_RADIO_TEXT = "FTP";
    public static final String PUBLISH_VIA_HTTP_RADIO_TEXT = "HTTP";
    public static final String COPY_TO_CLIPBOARD_BUTTON_TEXT = "Copy to clipboard";
    public static final String GENERATE_EDIT_CONTROL_FILE_BUTTON_TEXT = "Generate/Edit";
    public static final String GET_COLUMN_IDS_BUTTON_TEXT = "Get Column ID List";
    public static final int COPY_TO_CLIPBOARD_OPTION_INDEX = 1;

    private JFrame mainFrame;
    private JPanel jobPanel;

    private String jobFileLocation;
    private JLabel jobTabTitleLabel;

    private JTextField datasetIDTextField;
    private JTextField fileToPublishTextField;
    private JCheckBox fileToPublishHasHeaderCheckBox;
    private JComboBox publishMethodComboBox;
    private ButtonGroup publishMethodRadioButtonGroup;
    private JRadioButton soda2Button;
    private JRadioButton ftpButton;
    private JRadioButton httpButton;
    //private JCheckBox publishViaFTPCheckBox;
    private JPanel publishViaFTPLabelContainer;
    private JTextField controlFileTextField;
    private JButton browseForControlFileButton;
    private JPanel controlFileLabelContainer;
    private JPanel controlFileSelectorContainer;
    private JButton generateEditControlFileButton;
    private JTextArea controlFileContentTextArea;
    private JTextField runCommandTextField;

    // build Container with all tab components populated with given job data
    public IntegrationJobTab(IntegrationJob job, JFrame containingFrame) {
        mainFrame = containingFrame;

        // build tab panel form
        jobPanel = new JPanel(new GridLayout(0,2));

        addFileToPublishFieldToJobPanel();
        addDatasetIdFieldToJobPanel();
        addPublishMethodFieldToJobPanel();
        controlFileContentTextArea = new JTextArea(EMPTY_TEXTAREA_CONTENT);
        addControlFileFieldToJobPanel();
        addRunCommandFieldToJobPanel();

        loadJobDataIntoUIFields(job);
    }

    private void addControlFileFieldToJobPanel() {
        controlFileLabelContainer = UIUtility.generateLabelWithHelpBubble(
                "Control file", CONTROL_FILE_TIP_TEXT, HELP_ICON_TOP_PADDING);
        jobPanel.add(controlFileLabelContainer);

        controlFileSelectorContainer = new JPanel(FLOW_RIGHT);
        generateEditControlFileButton = new JButton(GENERATE_EDIT_CONTROL_FILE_BUTTON_TEXT);
        generateEditControlFileButton.addActionListener(new EditGenerateControlFileListener());
        controlFileSelectorContainer.add(generateEditControlFileButton);
        JLabel orLabel = new JLabel(" -or- ");
        controlFileSelectorContainer.add(orLabel);

        controlFileTextField = new JTextField();
        controlFileTextField.setPreferredSize(new Dimension(
                CONTROL_FILE_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        controlFileSelectorContainer.add(controlFileTextField);
        JFileChooser controlFileChooser = new JFileChooser();
        browseForControlFileButton = new JButton(BROWSE_BUTTON_TEXT);
        FtpControlFileSelectorListener chooserListener = new FtpControlFileSelectorListener(
                controlFileChooser, controlFileTextField);
        browseForControlFileButton.addActionListener(chooserListener);
        controlFileSelectorContainer.add(browseForControlFileButton);
        jobPanel.add(controlFileSelectorContainer);
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
        JButton copyJobCommandButton = new JButton(COPY_TO_CLIPBOARD_BUTTON_TEXT);
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

        //Create the radio buttons
        soda2Button = new JRadioButton(PUBLISH_VIA_SODA_RADIO_TEXT);
        ftpButton = new JRadioButton(PUBLISH_VIA_FTP_RADIO_TEXT);
        httpButton = new JRadioButton(PUBLISH_VIA_HTTP_RADIO_TEXT);
        httpButton.setSelected(true);

        //Should refactor the name to be radio button
        PublishViaReplaceListener listener = new PublishViaReplaceListener();

        soda2Button.addActionListener(listener);
        ftpButton.addActionListener(listener);
        httpButton.addActionListener(listener);

        publishMethodRadioButtonGroup = new ButtonGroup();
        publishMethodRadioButtonGroup.add(soda2Button);
        publishMethodRadioButtonGroup.add(ftpButton);
        publishMethodRadioButtonGroup.add(httpButton);

        publishViaFTPLabelContainer = new JPanel(FLOW_LEFT);
        publishViaFTPLabelContainer.add(soda2Button);
        publishViaFTPLabelContainer.add(ftpButton);
        publishViaFTPLabelContainer.add(httpButton);
        publishViaFTPLabelContainer.add(
                UIUtility.generateHelpBubble(PUBLISH_VIA_FTP_ROW_TIP_TEXT));
        publishMethodTextFieldContainer.add(publishViaFTPLabelContainer);

        jobPanel.add(publishMethodTextFieldContainer);
    }

    private void addDatasetIdFieldToJobPanel() {
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Dataset ID", DATASET_ID_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel datasetIDTextFieldContainer = new JPanel(FLOW_RIGHT);
        datasetIDTextField = new JTextField();
        datasetIDTextField.setPreferredSize(new Dimension(
                DATASET_ID_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        datasetIDTextFieldContainer.add(datasetIDTextField);

        JButton getColumnIdsButton = new JButton(GET_COLUMN_IDS_BUTTON_TEXT);
        getColumnIdsButton.addActionListener(new GetColumnIdsButtonListener());
        datasetIDTextFieldContainer.add(getColumnIdsButton);
        datasetIDTextFieldContainer.add(UIUtility.generateHelpBubble(GET_COLUMN_IDS_TIP_TEXT));

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
        JButton openButton = new JButton(BROWSE_BUTTON_TEXT);
        FileToPublishSelectorListener chooserListener = new FileToPublishSelectorListener(
                fileToPublishChooser, fileToPublishTextField);
        openButton.addActionListener(chooserListener);
        fileSelectorContainer.add(openButton);
        jobPanel.add(fileSelectorContainer);

        jobPanel.add(new JLabel(""));
        fileToPublishHasHeaderCheckBox = new JCheckBox(CONTAINS_A_HEADER_ROW_CHECKBOX_TEXT);

        JPanel hasHeaderRowLabelContainer = new JPanel(FLOW_LEFT);
        hasHeaderRowLabelContainer.add(fileToPublishHasHeaderCheckBox);
        hasHeaderRowLabelContainer.add(
                UIUtility.generateHelpBubble(HAS_HEADER_ROW_TIP_TEXT));
        jobPanel.add(hasHeaderRowLabelContainer);
    }

    private void setReplaceRadioButtons(IntegrationJob job) {
        if (!job.getPublishViaDi2Http() && !job.getPublishViaFTP())
            soda2Button.setSelected(true);
        else{
            ftpButton.setSelected(job.getPublishViaFTP());
            httpButton.setSelected(job.getPublishViaDi2Http());
        }
    }

    private void loadJobDataIntoUIFields(IntegrationJob job) {
        datasetIDTextField.setText(job.getDatasetID());
        fileToPublishTextField.setText(job.getFileToPublish());
        PublishMethod jobPublishMethod = job.getPublishMethod();
        publishMethodComboBox.setSelectedItem(jobPublishMethod);
        fileToPublishHasHeaderCheckBox.setSelected(job.getFileToPublishHasHeaderRow());

        updatePublishViaReplaceUIFields(job.getPublishMethod(),
                job.getPublishViaFTP() || job.getPublishViaDi2Http());

        //Set the defaults on the button correctly.
        setReplaceRadioButtons(job);


        updateControlFileInputs(job.getPathToControlFile(), job.getControlFileContent());

        jobFileLocation = job.getPathToSavedFile();
        // if this is an existing job (meaning the job was opened from a file)
        // then populate the scheduler command textfield
        if(!jobFileLocation.equals("")) {
            runCommandTextField.setText(
                    Utils.getRunJobCommand(jobFileLocation));
        }

        jobTabTitleLabel = new JLabel(job.getJobFilename());
    }

    private void updateControlFileInputs(final String pathToControlFile, final String controlFileContent) {
        if((pathToControlFile == null || pathToControlFile.equals(""))
                && (controlFileContent == null || controlFileContent.equals(""))) {
            setFtpControlFileSelectorEnabled(true);
            setEditGenerateFtpControlFileButtonEnabled(true);
        } else if(pathToControlFile != null && !pathToControlFile.equals("")) {
            controlFileTextField.setText(pathToControlFile);
            setFtpControlFileSelectorEnabled(true);
            setEditGenerateFtpControlFileButtonEnabled(false);
        } else {
            controlFileContentTextArea.setText(controlFileContent);
            setFtpControlFileSelectorEnabled(false);
            setEditGenerateFtpControlFileButtonEnabled(true);
        }
    }

    private void setFtpControlFileSelectorEnabled(boolean setSelectFtpControlFileEnabled) {
        controlFileTextField.setEnabled(setSelectFtpControlFileEnabled);
        browseForControlFileButton.setEnabled(setSelectFtpControlFileEnabled);
    }

    private void setEditGenerateFtpControlFileButtonEnabled(boolean setEditGenerateFtpControlFileEnabled) {
        generateEditControlFileButton.setEnabled(setEditGenerateFtpControlFileEnabled);
    }

    private void updatePublishViaReplaceUIFields(PublishMethod publishMethod,
                                                        boolean showFileInfo) {
        publishViaFTPLabelContainer.setVisible(
                publishMethod.equals(PublishMethod.replace));
        if(showFileInfo) {
            controlFileLabelContainer.setVisible(true);
            controlFileSelectorContainer.setVisible(true);
        } else {
            controlFileLabelContainer.setVisible(false);
            controlFileSelectorContainer.setVisible(false);
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
        jobToRun.setPublishViaFTP(ftpButton.isSelected());
        jobToRun.setPublishViaDi2Http(httpButton.isSelected());
        jobToRun.setPathToControlFile(controlFileTextField.getText());
        jobToRun.setControlFileContent(controlFileContentTextArea.getText());
        try {
            return jobToRun.run();
        } catch (IOException e) {
            JobStatus status = JobStatus.PUBLISH_ERROR;
            status.setMessage(e.getMessage());
            return status;
        }
    }

    public void saveJob() {
        // Save job data
        IntegrationJob newIntegrationJob = new IntegrationJob();
        newIntegrationJob.setDatasetID(datasetIDTextField.getText());
        newIntegrationJob.setFileToPublish(fileToPublishTextField.getText());
        newIntegrationJob.setPublishMethod(
                (PublishMethod) publishMethodComboBox.getSelectedItem());
        newIntegrationJob.setFileToPublishHasHeaderRow(fileToPublishHasHeaderCheckBox.isSelected());
        newIntegrationJob.setPublishViaFTP(ftpButton.isSelected());
        newIntegrationJob.setPublishViaDi2Http(httpButton.isSelected());
        newIntegrationJob.setPathToControlFile(controlFileTextField.getText());
        newIntegrationJob.setControlFileContent(controlFileContentTextArea.getText());
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

        saveJobAsFile(newIntegrationJob, updateJobCommandTextField, selectedJobFileLocation);
    }

    private void saveJobAsFile(IntegrationJob newIntegrationJob, boolean updateJobCommandTextField, String selectedJobFileLocation) {
        try {
            newIntegrationJob.writeToFile(selectedJobFileLocation);

            // Update job tab title label
            jobTabTitleLabel.setText(newIntegrationJob.getJobFilename());

            // Update the textfield with new command
            if(updateJobCommandTextField) {
                String runJobCommand = Utils.getRunJobCommand(
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
                    UIUtility.getFileChooserFilter(IntegrationJob.allowedFileToPublishExtensions));
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
                    UIUtility.getFileChooserFilter(IntegrationJob.allowedControlFileExtensions));
        }

        public void actionPerformed(ActionEvent e) {
            int returnVal = fileChooser.showOpenDialog(mainFrame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                filePathTextField.setText(file.getAbsolutePath());
                setFtpControlFileSelectorEnabled(true);
                setEditGenerateFtpControlFileButtonEnabled(false);
            } else {
                // Open command cancelled by user: do nothing
            }
        }
    }

    private class PublishMethodComboBoxListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            PublishMethod selectedPublishMethod =
                    (PublishMethod) publishMethodComboBox.getSelectedItem();
            if(selectedPublishMethod.equals(PublishMethod.replace)) {
                httpButton.setSelected(true);
            } else {
                publishMethodRadioButtonGroup.clearSelection();
            }
            boolean needControlFile =
                    (selectedPublishMethod.equals(PublishMethod.replace) && (ftpButton.isSelected() || httpButton.isSelected()));
            updatePublishViaReplaceUIFields(selectedPublishMethod, needControlFile);
        }
    }

    private class PublishViaReplaceListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            updatePublishViaReplaceUIFields(
                    (PublishMethod) publishMethodComboBox.getSelectedItem(),
                    controlFileNeeded());
        }

        private boolean controlFileNeeded() {
            return httpButton.isSelected() || ftpButton.isSelected();
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
            UIUtility.copyToClipboard(runJobCommand);
        }
    }

    private class EditGenerateControlFileListener implements ActionListener {
        public void actionPerformed(ActionEvent evnt) {
            String generateControlFileErrorMessage = null;
            String previousControlFileContent = controlFileContentTextArea.getText();
            String controlFileContent = previousControlFileContent;

            if(previousControlFileContent.equals(EMPTY_TEXTAREA_CONTENT)) {
                if(!fileToPublishIsSelected()) {
                    generateControlFileErrorMessage = "Error generating control file: " +
                            "you must select a File to Publish";
                } else if(!datasetIdValid()) {
                    generateControlFileErrorMessage = "Error generating control file: " +
                            "you must enter valid Dataset ID";
                } else {
                    try {
                        controlFileContent = generateControlFileContent(
                                getSodaDdl(),
                                fileToPublishTextField.getText(),
                                (PublishMethod) publishMethodComboBox.getSelectedItem(),
                                datasetIDTextField.getText(),
                                fileToPublishHasHeaderCheckBox.isSelected());
                    } catch (Exception e) {
                        e.printStackTrace();
                        generateControlFileErrorMessage = "Error generating control file: " + e.getMessage();
                    }
                }
            }

            if(generateControlFileErrorMessage == null) {
                int clickedOkOrCancel = showEditControlFileDialog(controlFileContent);
                if(clickedOkOrCancel == JOptionPane.OK_OPTION) {
                    setFtpControlFileSelectorEnabled(false);
                    setEditGenerateFtpControlFileButtonEnabled(true);
                } else if (clickedOkOrCancel == JOptionPane.CANCEL_OPTION) {
                    // Set control file content to its previous content
                    controlFileContentTextArea.setText(previousControlFileContent);
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, generateControlFileErrorMessage);
            }
        }

        private boolean fileToPublishIsSelected() {
            String fileToPublish = fileToPublishTextField.getText();
            return !fileToPublish.equals("");
        }

        /**
         * Generates default content of control.json based on given job parameters
         *
         * @param ddl Soda 2 ddl object
         * @param publishMethod to use to publish (upsert, append, replace, or delete)
         *               NOTE: this option will be overriden if userPrefs has pathToFTPControlFile or pathToControlFile set
         * @param datasetId id of the Socrata dataset to publish to
         * @param fileToPublish filename of file to publish (.tsv or .csv file)
         * @param containsHeaderRow if true assume the first row in CSV/TSV file is a list of the dataset columns,
         *                          otherwise upload all rows as new rows (column order must exactly match that of
         *                          Socrata dataset)
         * @return content of control.json based on given job parameters
         * @throws com.socrata.exceptions.SodaError
         * @throws InterruptedException
         */
        private String generateControlFileContent(SodaDdl ddl, String fileToPublish, PublishMethod publishMethod,
                                                  String datasetId, boolean containsHeaderRow) throws SodaError, InterruptedException, IOException {
            Dataset datasetInfo = (Dataset) ddl.loadDatasetInfo(datasetId);
            boolean useGeocoding = Utils.datasetHasLocationColumn(datasetInfo);

            // In FTP Dropbox v2 there is only Append (append == upsert)
            //Consider factoring the ftp and http button check into a new method
            if ((ftpButton.isSelected()||httpButton.isSelected()) && publishMethod.equals(PublishMethod.upsert)) {
                publishMethod = PublishMethod.append;
            }

            String[] columns = null;
            if (!containsHeaderRow)
                columns = Utils.getDatasetFieldNames(datasetInfo);

            ControlFile control = ControlFile.generateControlFile(fileToPublish, publishMethod, columns, useGeocoding);
            ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            return mapper.writeValueAsString(control);
        }

        /**
         * Display dialog with scrollable text area allowing editing
         * of given control file content
         *
         * @param textAreaContent
         * @return generated scrollable text area
         */
        private int showEditControlFileDialog(String textAreaContent) {
            controlFileContentTextArea.setText(textAreaContent);
            JScrollPane scrollPane = new JScrollPane(controlFileContentTextArea);
            controlFileContentTextArea.setLineWrap(true);
            controlFileContentTextArea.setWrapStyleWord(true);
            controlFileContentTextArea.setCaretPosition(0);
            scrollPane.setPreferredSize(CONTROL_FILE_EDITOR_DIMENSIONS);
            return JOptionPane.showConfirmDialog(mainFrame,
                    scrollPane,
                    "Edit Control File Content",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);
        }
    }

    private class GetColumnIdsButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent evnt) {
            String errorMessage = null;
            String datasetFieldNames = null;
            if(datasetIdValid()) {
                try {
                    datasetFieldNames = Utils.getDatasetFieldNamesString(
                            getSodaDdl(), datasetIDTextField.getText());
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage = "Error getting column IDs for dataset with ID" +
                            " '" + datasetIDTextField.getText() + "': " + e.getMessage();
                }
            } else {
                errorMessage = "You must enter a valid Dataset ID";
            }

            if(errorMessage == null) {
                int selectedOption = showGetColumnIdsDialog(datasetFieldNames);
                if(selectedOption == COPY_TO_CLIPBOARD_OPTION_INDEX) {
                    UIUtility.copyToClipboard(datasetFieldNames);
                }
            } else {
                JOptionPane.showMessageDialog(mainFrame, errorMessage);
            }
        }

        private int showGetColumnIdsDialog(String textAreaContent) {
            JTextArea columnIdTextArea = new JTextArea(textAreaContent);
            JScrollPane scrollPane = new JScrollPane(columnIdTextArea);
            columnIdTextArea.setLineWrap(true);
            columnIdTextArea.setWrapStyleWord(true);
            columnIdTextArea.setCaretPosition(0);
            columnIdTextArea.setEditable(false);
            scrollPane.setPreferredSize(CONTROL_FILE_EDITOR_DIMENSIONS);
            String[] selectionValues = {"Close", COPY_TO_CLIPBOARD_BUTTON_TEXT};
            return JOptionPane.showOptionDialog(mainFrame,
                    scrollPane,
                    "Column ID List",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    selectionValues,
                    selectionValues[COPY_TO_CLIPBOARD_OPTION_INDEX]);
        }
    }

    private boolean datasetIdValid() {
        String datasetId = datasetIDTextField.getText();
        return Utils.uidIsValid(datasetId);
    }

    private SodaDdl getSodaDdl() {
        UserPreferences userPrefs = new UserPreferencesJava();
        return SodaDdl.newDdl(
                userPrefs.getDomain(),
                userPrefs.getUsername(),
                userPrefs.getPassword(),
                userPrefs.getAPIKey());
    }
}
