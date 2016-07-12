package com.socrata.datasync.ui;

import com.socrata.datasync.*;
import com.socrata.datasync.DatasetUtils;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.Utils;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.model.ControlFileModel;
import com.socrata.datasync.model.DatasetModel;
import com.socrata.datasync.validation.IntegrationJobValidity;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Dataset;
import org.apache.http.HttpException;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.dnd.InvalidDnDOperationException;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;

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

    private JFrame mainFrame;
    private JPanel jobPanel;

    private String jobFileLocation;
    //Rest of the code assumes that this is never null. Adding to avoid null pointer exception when job initialization fails.
    private JLabel jobTabTitleLabel = new JLabel("Untitled");

    private JTextField datasetIDTextField;
    private JTextField fileToPublishTextField;
    private JComboBox publishMethodComboBox;
    private ButtonGroup publishMethodRadioButtonGroup;
    private JRadioButton soda2Button;
    private JRadioButton ftpButton;
    private JRadioButton httpButton;
    private JPanel publishViaFTPLabelContainer;
    private JButton browseForControlFileButton;
    private JPanel controlFileLabelContainer;
    private JPanel controlFileSelectorContainer;


    private JButton generateEditControlFileButton;
    private ControlFileModel controlFileModel;
    private DatasetModel datasetModel;
    private JTextField runCommandTextField;

    private boolean usingControlFile;

    // build Container with all tab components populated with given job data
    public IntegrationJobTab(IntegrationJob job, JFrame containingFrame) {
        mainFrame = containingFrame;

        // build tab panel form
        jobPanel = new JPanel(new GridLayout(5,2));
        addFileToPublishFieldToJobPanel();
        addDatasetIdFieldToJobPanel();
        addPublishMethodFieldToJobPanel();
  //      controlFileContentTextArea = new JTextArea(EMPTY_TEXTAREA_CONTENT);
        addControlFileFieldToJobPanel();
        addRunCommandFieldToJobPanel();


        loadJobDataIntoUIFields(job);

        if(job.getPublishMethod() == null)
            publishMethodComboBox.setSelectedItem(PublishMethod.replace);

    }


    private void addControlFileFieldToJobPanel() {
        controlFileLabelContainer = UIUtility.generateLabelWithHelpBubble(
                "Step 4 - Tell us how to import your file", CONTROL_FILE_TIP_TEXT, HELP_ICON_TOP_PADDING);
        jobPanel.add(controlFileLabelContainer);

        controlFileSelectorContainer = new JPanel(FLOW_RIGHT);
        generateEditControlFileButton = new JButton("Map fields");
        generateEditControlFileButton.addActionListener(new EditControlFileListener());
        controlFileSelectorContainer.add(generateEditControlFileButton);
        jobPanel.add(controlFileSelectorContainer);
    }

    private void addRunCommandFieldToJobPanel() {
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Step 5 - Copy command for later (optional)", RUN_COMMAND_TIP_TEXT, HELP_ICON_TOP_PADDING));
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
                "Step 3 - Select update method", PUBLISH_METHOD_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel publishMethodTextFieldContainer = new JPanel(FLOW_RIGHT);
        publishMethodComboBox = new JComboBox(PublishMethod.values());
        publishMethodComboBox.addActionListener(new PublishMethodComboBoxListener());

        publishMethodTextFieldContainer.add(publishMethodComboBox);

        //Create the radio buttons
        //TODO: For test purposes only.  Remove these eventually
        soda2Button = new JRadioButton(PUBLISH_VIA_SODA_RADIO_TEXT);
        ftpButton = new JRadioButton(PUBLISH_VIA_FTP_RADIO_TEXT);
        httpButton = new JRadioButton(PUBLISH_VIA_HTTP_RADIO_TEXT);
        httpButton.setSelected(true);

        publishViaFTPLabelContainer = new JPanel(FLOW_LEFT);
        jobPanel.add(publishMethodTextFieldContainer);
    }

    private void addDatasetIdFieldToJobPanel() {
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Step 2 - Enter Dataset ID to update", DATASET_ID_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel datasetIDTextFieldContainer = new JPanel(FLOW_RIGHT);
        datasetIDTextField = new JTextField();
        datasetIDTextField.setPreferredSize(new Dimension(
                DATASET_ID_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        RegenerateControlFileListener regenerateListener = new RegenerateControlFileListener();
        datasetIDTextField.addActionListener(regenerateListener);
        datasetIDTextField.addFocusListener(regenerateListener);
        datasetIDTextFieldContainer.add(datasetIDTextField);
        jobPanel.add(datasetIDTextFieldContainer);
    }

    private void addFileToPublishFieldToJobPanel() {
        jobPanel.add(
                UIUtility.generateLabelWithHelpBubble("Step 1 - Select file to publish", FILE_TO_PUBLISH_TIP_TEXT, HELP_ICON_TOP_PADDING));
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
        RegenerateControlFileListener regenerateListener = new RegenerateControlFileListener();
        fileToPublishTextField.addActionListener(regenerateListener);
        fileToPublishTextField.addFocusListener(regenerateListener);
        jobPanel.add(fileSelectorContainer);
    }

    private void setReplaceRadioButtons(IntegrationJob job) {
        if (!job.getPublishViaDi2Http() && !job.getPublishViaFTP())
            soda2Button.setSelected(true);
        else{
            ftpButton.setSelected(job.getPublishViaFTP());
            httpButton.setSelected(job.getPublishViaDi2Http());
        }
    }

    private void loadJobDataIntoUIFields(IntegrationJob job)  {
        try {
            if (job.getControlFileContent() != null) {
                ObjectMapper mapper = new ObjectMapper().enable(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
                ControlFile controlFile = mapper.readValue(job.getControlFileContent(), ControlFile.class);
                //Ideally this could be saved with the control file or factored out.  However, we're stuck with this redundant call
                // because of DI2's strict enforcement of control files and the current factoring of what CSVTableModel knows about
                controlFile.getFileTypeControl().filePath(job.getFileToPublish());
                //TODO: This is not being saved due to the fact that this value is set on the control file and not the job.  Pick one or the other.
                controlFile.getFileTypeControl().hasHeaderRow(job.getFileToPublishHasHeaderRow());
                updateControlFileModel(controlFile,job.getDatasetID());
            }
            datasetIDTextField.setText(job.getDatasetID());
            fileToPublishTextField.setText(job.getFileToPublish());
            PublishMethod jobPublishMethod = job.getPublishMethod();
            publishMethodComboBox.setSelectedItem(jobPublishMethod);

            updatePublishViaReplaceUIFields(job.getPublishViaFTP() || job.getPublishViaDi2Http());

            //Set the defaults on the button correctly.
            setReplaceRadioButtons(job);

            //TODO: If there is a way to save the pointer to the file, but not the file then we'll need to add a way to load it here
            if (job.getPathToControlFile() != null) {
                System.out.println("SKipping");
            }

            jobFileLocation = job.getPathToSavedFile();
            // if this is an existing job (meaning the job was opened from a file)
            // then populate the scheduler command textfield
            if (!jobFileLocation.equals("")) {
                runCommandTextField.setText(
                        Utils.getRunJobCommand(jobFileLocation));
            }

            jobTabTitleLabel = new JLabel(job.getJobFilename());
        }
        catch (Exception e){
            JOptionPane.showMessageDialog(mainFrame, "Error: " + e.getMessage());
        }
    }


    private void updateControlFileModel(ControlFile controlFile, String fourbyfour) throws LongRunningQueryException, InterruptedException, HttpException, IOException, URISyntaxException{

        UserPreferences userPrefs = new UserPreferencesJava();

        datasetModel = new DatasetModel(userPrefs, fourbyfour);

        controlFileModel = new ControlFileModel(controlFile,datasetModel);

    }

    private void updatePublishViaReplaceUIFields(boolean showFileInfo) {
        publishViaFTPLabelContainer.setVisible(true);
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
        if (controlFileModel == null || controlFileModel.validate().isError()) {
            JobStatus noControlFile = JobStatus.INVALID_PUBLISH_METHOD;
            noControlFile.setMessage("We aren't quite ready to upload.  Click the \"Map Fields\" button to set the mappings for your CSV");
            return noControlFile;
        }
        IntegrationJob jobToRun = new IntegrationJob();
        jobToRun.setDatasetID(datasetIDTextField.getText());
        jobToRun.setFileToPublish(fileToPublishTextField.getText());
        jobToRun.setPublishMethod(
                (PublishMethod) publishMethodComboBox.getSelectedItem());
        jobToRun.setFileToPublishHasHeaderRow(controlFileModel.getControlFile().getFileTypeControl().hasHeaderRow);
        jobToRun.setPublishViaFTP(ftpButton.isSelected());
        jobToRun.setPublishViaDi2Http(httpButton.isSelected());
        if (usingControlFile) {
            jobToRun.setPathToControlFile(controlFileModel.getPath());
        } else {
            jobToRun.setControlFileContent(controlFileModel.getControlFileContents());

        }
        jobToRun.setUserAgentClient();
        return jobToRun.run();
    }

    public void saveJob() {
        // Save job data
        IntegrationJob newIntegrationJob = new IntegrationJob();
        newIntegrationJob.setDatasetID(datasetIDTextField.getText());
        newIntegrationJob.setFileToPublish(fileToPublishTextField.getText());
        newIntegrationJob.setPublishMethod(
                (PublishMethod) publishMethodComboBox.getSelectedItem());
        newIntegrationJob.setFileToPublishHasHeaderRow(controlFileModel.getControlFile().getFileTypeControl().hasHeaderRow);
        newIntegrationJob.setPublishViaFTP(ftpButton.isSelected());
        newIntegrationJob.setPublishViaDi2Http(httpButton.isSelected());
        newIntegrationJob.setPathToControlFile(controlFileModel.getPath());
        newIntegrationJob.setControlFileContent(controlFileModel.getControlFileContents());
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
            jobTabTitleLabel.setText(Utils.getFilename(selectedJobFileLocation));

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
                    UIUtility.getFileChooserFilter(IntegrationJobValidity.allowedFileToPublishExtensions));
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

    private class RegenerateControlFileListener extends FocusAdapter implements ActionListener {
        @Override
        public void focusLost(FocusEvent e){
            if (!e.isTemporary()) {
                setRegenerateControlFile();
            }
        }

        public void actionPerformed(ActionEvent e) {
            setRegenerateControlFile();
        }

        //Set the right variables so that the control file is regenerated when going to the mapping dialog
        //TODO: Consider paying attention to these at run job time as well
        private void setRegenerateControlFile(){
            controlFileModel = null;
            datasetModel = null;
        }
    }


    private class PublishMethodComboBoxListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            PublishMethod selectedPublishMethod =
                (PublishMethod) publishMethodComboBox.getSelectedItem();
            ftpButton.setVisible(PublishMethod.replace.equals(selectedPublishMethod));
            httpButton.setSelected(true);
            //Should not be null
            if (controlFileModel != null)
                controlFileModel.setType(Utils.capitalizeFirstLetter(selectedPublishMethod.name()));
            updatePublishViaReplaceUIFields(controlFileNeeded());

        }
    }

    private boolean controlFileNeeded() {
        return httpButton.isSelected() || ftpButton.isSelected();
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

    private class EditControlFileListener implements ActionListener {
        public void actionPerformed(ActionEvent evnt) {
            String generateControlFileErrorMessage;
                if(!datasetIdValid()) {
                    generateControlFileErrorMessage = "Error generating control file: " +
                            "you must enter valid Dataset ID";
                    JOptionPane.showMessageDialog(mainFrame, generateControlFileErrorMessage);
                } else {
                    try {
                        if (controlFileModel == null) {
                            ControlFile controlFile = generateControlFile(
                                    new UserPreferencesJava(),
                                    fileToPublishTextField.getText(),
                                    (PublishMethod) publishMethodComboBox.getSelectedItem(),
                                    datasetIDTextField.getText(),
                                    true);

                            updateControlFileModel(controlFile,datasetIDTextField.getText());
                        }

                        ControlFileEditDialog editorFrame = new ControlFileEditDialog(controlFileModel,mainFrame);

                    } catch (Exception e) {
                        e.printStackTrace();
                        generateControlFileErrorMessage = "Error generating control file: " + e.getMessage();
                        JOptionPane.showMessageDialog(mainFrame, generateControlFileErrorMessage);
                    }
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
        private String generateControlFileContent(UserPreferences prefs, String fileToPublish, PublishMethod publishMethod,
                                                  String datasetId, boolean containsHeaderRow) throws HttpException, URISyntaxException, InterruptedException, IOException {
            ControlFile control = generateControlFile(prefs,fileToPublish,publishMethod,datasetId,containsHeaderRow);
            ObjectMapper mapper = new ObjectMapper().configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            return mapper.writeValueAsString(control);
        }

        private ControlFile generateControlFile(UserPreferences prefs, String fileToPublish, PublishMethod publishMethod,
                                                String datasetId, boolean containsHeaderRow) throws HttpException, URISyntaxException, InterruptedException, IOException {

            Dataset datasetInfo = DatasetUtils.getDatasetInfo(prefs, datasetId);
            boolean useGeocoding = DatasetUtils.hasLocationColumn(datasetInfo);

            String[] columns = null;
            if (!containsHeaderRow) {
                if (PublishMethod.delete.equals(publishMethod))
                    columns = new String[]{DatasetUtils.getRowIdentifierName(datasetInfo)};
                else
                    columns = DatasetUtils.getFieldNamesArray(datasetInfo);
            }

            return ControlFile.generateControlFile(fileToPublish, publishMethod, columns, useGeocoding, containsHeaderRow);
        }

    }

    private boolean datasetIdValid() {
        String datasetId = datasetIDTextField.getText();
        return Utils.uidIsValid(datasetId);
    }
}
