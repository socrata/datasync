package com.socrata.datasync.ui;

import com.socrata.datasync.*;
import com.socrata.datasync.DatasetUtils;
import com.socrata.datasync.PublishMethod;
import com.socrata.datasync.Utils;
import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.socrata.datasync.job.GISJob;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.model.ControlFileModel;
import com.socrata.datasync.model.DatasetModel;
import com.socrata.datasync.validation.GISJobValidity;
import com.socrata.datasync.validation.IntegrationJobValidity;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.importer.Dataset;
import org.apache.http.HttpException;
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
public class GISJobTab implements JobTab {

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

    private static final String JOB_FILE_NAME = "Socrata GIS Job";
    public static final String JOB_FILE_EXTENSION = "gij";

    private static final int HELP_ICON_TOP_PADDING = 12;
    private static final String FILE_TO_PUBLISH_TIP_TEXT = "GeoJSON, kml/kmz, or zipped shape file containing the data to be published";
    private static final String DATASET_ID_TIP_TEXT = "<html><body style='width: 300px'>The identifier in the form of xxxx-xxxx (e.g. n38h-y5wp) " +
        "of the Socrata dataset where the data will be published</body></html>";
    private static final String PUBLISH_METHOD_TIP_TEXT = "<html><body style='width: 400px'>Method used to publish data:<br>" +
        "<strong>replace</strong>: the only allowable method for geospatial files.<br>";
    private static final String CONTROL_FILE_TIP_TEXT = "<html><body style='width: 300px'>" +
        "Establishes import configuration such as date formatting and Location column being populated" +
        " from existing columns (for more information refer to Help -> control file configuration)</body></html>";
    private static final String RUN_COMMAND_TIP_TEXT = "<html><body style='width: 300px'>After saving the job this field will be populated with a command-line command that can be used to run the job." +
        " This command can be input into tools such as the Windows Task Scheduler or ETL tools to run the job automatically.</body></html>";
    public static final String COPY_TO_CLIPBOARD_BUTTON_TEXT = "Copy to clipboard";

    private String jobFileLocation;
    private boolean usingControlFile;

    //Rest of the code assumes that this is never null. Adding to avoid null pointer exception when job initialization fails.
    private JLabel jobTabTitleLabel = new JLabel("Untitled GIS Job");

    private JTextField datasetIDTextField;
    private JTextField fileToPublishTextField;
    private JComboBox<String> publishMethodComboBox;
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
    private JFrame mainFrame;
    private JPanel jobPanel;

    // build Container with all tab components populated with given job data
    public GISJobTab(GISJob job, JFrame containingFrame) {
        mainFrame = containingFrame;

        // build tab panel form
        jobPanel = new JPanel(new GridLayout(5,2));
        addFileToPublishFieldToJobPanel();
        addDatasetIdFieldToJobPanel();
        addPublishMethodFieldToJobPanel();
        addRunCommandFieldToJobPanel();

        loadJobDataIntoUIFields(job);
    }

    private void addRunCommandFieldToJobPanel() {
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                         "Step 4 - Copy command for later (optional)", RUN_COMMAND_TIP_TEXT, HELP_ICON_TOP_PADDING));
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

    /* GIS Only has a replace method */
    private void addPublishMethodFieldToJobPanel() {
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                         "Step 3 - Select update method", PUBLISH_METHOD_TIP_TEXT, HELP_ICON_TOP_PADDING));
        JPanel publishMethodTextFieldContainer = new JPanel(FLOW_RIGHT);
        String[] onlyOption = {"replace"};
        JComboBox<String> publishMethodComboBox = new JComboBox<>(onlyOption);
        publishMethodComboBox.addActionListener(new PublishMethodComboBoxListener());

        publishMethodTextFieldContainer.add(publishMethodComboBox);

        //Create the radio buttons
        //TODO: For test purposes only.  Remove these eventually
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

    private void loadJobDataIntoUIFields(GISJob job)  {
        try {
            datasetIDTextField.setText(job.getDatasetID());
            fileToPublishTextField.setText(job.getFileToPublish());

            jobFileLocation = job.getPathToSavedFile();
            // if this is an existing job (meaning the job was opened from a file)
            // then populate the scheduler command textfield
            if (!jobFileLocation.equals("")) {
                runCommandTextField.setText(Utils.getRunJobCommand(jobFileLocation));
            }

            jobTabTitleLabel = new JLabel(job.getJobFilename());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "Error: " + e.getMessage());
        }
    }

    private void updateControlFileModel(ControlFile controlFile, String fourbyfour)
        throws LongRunningQueryException, InterruptedException, HttpException, IOException, URISyntaxException {

        UserPreferences userPrefs = new UserPreferencesJava();

        datasetModel = new DatasetModel(userPrefs, fourbyfour);

        controlFileModel = new ControlFileModel(controlFile, datasetModel);
    }

    private void updatePublishViaReplaceUIFields(boolean showFileInfo) {
        publishViaFTPLabelContainer.setVisible(true);

        if (showFileInfo) {
            controlFileLabelContainer.setVisible(true);
            controlFileSelectorContainer.setVisible(true);
        } else {
            controlFileSelectorContainer.setVisible(false);
        }
        jobPanel.updateUI();
    }

    public JPanel getTabPanel() {
        return jobPanel;
    }

    public JobStatus runJobNow() {

        GISJob jobToRun = new GISJob();
        jobToRun.setDatasetID(datasetIDTextField.getText());
        jobToRun.setFileToPublish(fileToPublishTextField.getText());
        jobToRun.setPublishMethod(PublishMethod.replace);

        jobToRun.setUserAgentClient();
        //MAGIC
        return jobToRun.run();
    }

    public void saveJob() {
        // Save job data
        GISJob newGISJob = new GISJob();
        newGISJob.setDatasetID(datasetIDTextField.getText());
        newGISJob.setFileToPublish(fileToPublishTextField.getText());
        newGISJob.setPublishMethod(PublishMethod.replace);
        newGISJob.setPathToSavedFile(jobFileLocation);

        // TODO If an existing file was selected WARN user of overwriting
        // if first time saving this job: Open dialog box to select "Save as..." location
        // otherwise save to existing file
        boolean updateJobCommandTextField = false;
        String selectedJobFileLocation = jobFileLocation;
        if (selectedJobFileLocation.equals("")) {
            JFileChooser savedJobFileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                JOB_FILE_NAME + " (*." + JOB_FILE_EXTENSION + ")", JOB_FILE_EXTENSION);
            savedJobFileChooser.setFileFilter(filter);
            if (savedJobFileChooser.showSaveDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File file = savedJobFileChooser.getSelectedFile();
                selectedJobFileLocation = file.getAbsolutePath();

                if (!selectedJobFileLocation.endsWith("." + JOB_FILE_EXTENSION)) {
                    selectedJobFileLocation += "." + JOB_FILE_EXTENSION;
                }

                jobFileLocation = selectedJobFileLocation;
                newGISJob.setPathToSavedFile(selectedJobFileLocation);
                jobTabTitleLabel.setText(newGISJob.getJobFilename());
                updateJobCommandTextField = true;
            }
        }
        saveJobAsFile(newGISJob, updateJobCommandTextField, selectedJobFileLocation);
    }

    private void saveJobAsFile(GISJob newGISJob,
                               boolean updateJobCommandTextField,
                               String selectedJobFileLocation) {
        try {
            newGISJob.writeToFile(selectedJobFileLocation);

            // Update job tab title label
            jobTabTitleLabel.setText(Utils.getFilename(selectedJobFileLocation));

            // Update the textfield with new command
            if(updateJobCommandTextField) {
                String runJobCommand = Utils.getRunJobCommand(
                    newGISJob.getPathToSavedFile());
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
                UIUtility.getFileChooserFilter(GISJobValidity.allowedGeoFileToPublishExtensions));
        }

        public void actionPerformed(ActionEvent e) {
            if (fileChooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                filePathTextField.setText(file.getAbsolutePath());
            }

            // If open command was cancelled by user: do nothing
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
            PublishMethod selectedPublishMethod = PublishMethod.replace;
            ftpButton.setVisible(PublishMethod.replace.equals(selectedPublishMethod));
            httpButton.setSelected(true);
            //Should not be null
            if (controlFileModel != null) {
                controlFileModel.setType(Utils.capitalizeFirstLetter(selectedPublishMethod.name()));
            }

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
            final String generateControlFileErrorMessage;
            if (!datasetIdValid()) {
                generateControlFileErrorMessage = "Error generating control file: " +
                    "you must enter valid Dataset ID";
                JOptionPane.showMessageDialog(mainFrame, generateControlFileErrorMessage);
            } else {
                try {
                    if (controlFileModel == null) {
                        ControlFile controlFile = generateControlFile(
                            new UserPreferencesJava(),
                            fileToPublishTextField.getText(),
                            PublishMethod.replace,
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
            return !"".equals(fileToPublish);
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

        private ControlFile generateControlFile(UserPreferences prefs,
                                                String fileToPublish,
                                                PublishMethod publishMethod,
                                                String datasetId,
                                                boolean containsHeaderRow)
            throws HttpException, URISyntaxException, InterruptedException, IOException {

            Dataset datasetInfo = DatasetUtils.getDatasetInfo(prefs, datasetId);
            boolean useGeocoding = DatasetUtils.hasLocationColumn(datasetInfo);

            String[] columns = null;
            if (!containsHeaderRow) {
                if (PublishMethod.delete.equals(publishMethod)) {
                    columns = new String[]{DatasetUtils.getRowIdentifierName(datasetInfo)};
                } else {
                    columns = DatasetUtils.getFieldNamesArray(datasetInfo);
                }
            }

            return ControlFile.generateControlFile(fileToPublish, publishMethod, columns, useGeocoding, containsHeaderRow);
        }

    }

    private boolean datasetIdValid() {
        String datasetId = datasetIDTextField.getText();
        return Utils.uidIsValid(datasetId);
    }
}
