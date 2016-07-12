package com.socrata.datasync.ui;

import com.socrata.datasync.*;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.job.MetadataJob;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Author: Brian Williamson
 * Date: 1/22/2014
 */
public class MetadataJobTab implements JobTab {

    private final int JOB_TEXTFIELD_WIDTH = 370;
    private final int JOB_COMMAND_TEXTFIELD_WIDTH = 211;
    //private final int JOB_FILE_TEXTFIELD_WIDTH = 238;
    private final int JOB_TEXTFIELD_HEIGHT = 26;
    private final int JOB_FIELD_VGAP = 8;
    private final int JOB_TEXTAREA_ROWS = 5;
    private final int JOB_TEXTAREA_COLS = 35;

    private final String DEFAULT_RUN_JOB_COMMAND = "(Generates when job is saved)";

    private final String JOB_FILE_NAME = "Socrata Metadata Job";
    public final static String JOB_FILE_EXTENSION = "smj";

    private final int HELP_ICON_TOP_PADDING = 10;
    
    private final String DATASET_ID_TIP_TEXT = "<html><body style='width: 300px'>The identifier in the form of xxxx-xxxx (e.g. n38h-y5wp) " +
            "of the Socrata dataset where the data will be published</body></html>";  
    private final String TITLE_TIP_TEXT = "Dataset title.  This field is required.";
    private final String DESCRIPTION_TIP_TEXT = "Dataset description.  This is an optional field";
    private final String KEYWORDS_TIP_TEXT = "Dataset Tags/Keywords.  Enter one or more keywords separated by commas.";
    private final String CATEGORY_TIP_TEXT = "Dataset Category.  The value must match one of the available categories for the dataset.";
    private final String LICENSE_TYPE_TIP_TEXT = "Dataset License Type.  This field is optional.";
    private final String DATA_PROVIDED_BY_TIP_TEXT = "Dataset Data Provided By.  This field is optional.";
    private final String SOURCE_LINK_TIP_TEXT = "Dataset Source Link.  This field is optional.";
    private final String CONTACT_INFO_TIP_TEXT = "Dataset Contact Info.  This field is optional.";           
    private final String RUN_COMMAND_TIP_TEXT = "<html><body style='width: 300px'>After saving the job this field will be populated with a command-line command that can be used to run the job." +
            " This command can be input into tools such as the Windows Scheduler or ETL tools to run the job automatically.</body></html>";
    private final String LOAD_METADATA_TIP_TEXT = "<html><body style='width: 300px'>Load existing dataset" +
            " metadata into this form (from dataset with ID given above)</body></html>";

    private JFrame mainFrame;
    private JPanel jobPanel;

    private String jobFileLocation;
    private JLabel jobTabTitleLabel;

    private JTextField datasetIDTextField;
    private JTextField titleTextField;
    private JTextArea descriptionTextArea;
    private JTextField categoryTextField;
    private JTextField keywordsTextField;
    private JComboBox licenseTypeComboBox;
    private JTextField dataProvidedByTextField;
    private JTextField sourceLinkTextField;
    private JTextField contactInfoTextField;
    //private JComboBox publishMethodComboBox;
    private JTextField runCommandTextField;
    
    private MetadataJob metadataJob;

    // build Container with all tab components populated with given job data
    public MetadataJobTab(MetadataJob job, JFrame containingFrame) {
    	this.metadataJob = job;
    	
        mainFrame = containingFrame;

        // build tab panel form
        //jobPanel = new JPanel(new GridLayout(0,2));
        jobPanel = new JPanel(new GridBagLayout());

        // set FlowLayouts
        FlowLayout flowLeft = new FlowLayout(FlowLayout.LEFT, 0, 0);
        FlowLayout flowRight = new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP);
        
        //Dataset ID Text Field
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Dataset ID", DATASET_ID_TIP_TEXT, HELP_ICON_TOP_PADDING), UIUtility.getGridBagLabelConstraints(0,0));
        JPanel datasetIDTextFieldContainer = new JPanel(flowRight);
        datasetIDTextField = new JTextField();
        datasetIDTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        datasetIDTextFieldContainer.add(datasetIDTextField); 
        jobPanel.add(datasetIDTextFieldContainer, UIUtility.getGridBagFieldConstraints(1,0)); 
        
        //Load Metadata Button
        jobPanel.add(new JLabel(""));
        JPanel loadMetadataButtonContainer = new JPanel(flowRight);
        JButton loadMetadataButton = new JButton("Load Current Dataset Metadata");
        loadMetadataButton.addActionListener(new LoadMetadataActionListener());
        loadMetadataButtonContainer.add(loadMetadataButton);
        loadMetadataButtonContainer.add(UIUtility.generateHelpBubble(LOAD_METADATA_TIP_TEXT));
        jobPanel.add(loadMetadataButtonContainer, UIUtility.getGridBagFieldConstraints(1, 1));
        
        //Title Text Field
        jobPanel.add(UIUtility.generateLabelWithHelpBubble("Title", TITLE_TIP_TEXT, HELP_ICON_TOP_PADDING), 
        		UIUtility.getGridBagLabelConstraints(0,2));
        JPanel titleTextFieldContainer = new JPanel(flowRight);
        titleTextField = new JTextField();
        titleTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        titleTextFieldContainer.add(titleTextField);
        jobPanel.add(titleTextFieldContainer, UIUtility.getGridBagFieldConstraints(1,2)); 
        
        //Description Text Area
        jobPanel.add(UIUtility.generateLabelWithHelpBubble("Description", DESCRIPTION_TIP_TEXT, HELP_ICON_TOP_PADDING), 
        		UIUtility.getGridBagLabelConstraints(0, 3));
        JPanel descriptionTextFieldContainer = new JPanel();
        descriptionTextArea = new JTextArea(5, 33);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionTextArea);
        descriptionTextFieldContainer.add(descriptionScrollPane);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 1;
        constraints.gridy = 3; 
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.ipadx = -10; //Not sure why, but text area was getting extra margin so left edge didn't line up with other fields        
        jobPanel.add(descriptionTextFieldContainer, constraints);
        
        //Category Text Field
        jobPanel.add(UIUtility.generateLabelWithHelpBubble("Category", CATEGORY_TIP_TEXT, HELP_ICON_TOP_PADDING),
        		UIUtility.getGridBagLabelConstraints(0, 4));
        JPanel categoryTextFieldContainer = new JPanel(flowRight);
        categoryTextField = new JTextField();
        categoryTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        categoryTextFieldContainer.add(categoryTextField);
        jobPanel.add(categoryTextFieldContainer, UIUtility.getGridBagFieldConstraints(1, 4));  
        
        //Tags/Keywords Text Field
        jobPanel.add(UIUtility.generateLabelWithHelpBubble("Tags/Keywords", KEYWORDS_TIP_TEXT, HELP_ICON_TOP_PADDING),
        		UIUtility.getGridBagLabelConstraints(0, 5)); 
        JPanel keywordsTextFieldContainer = new JPanel(flowRight);
        keywordsTextField = new JTextField();
        keywordsTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        keywordsTextFieldContainer.add(keywordsTextField);
        jobPanel.add(keywordsTextFieldContainer, UIUtility.getGridBagFieldConstraints(1, 5));         
        
        //License Type Combo Box
        jobPanel.add(UIUtility.generateLabelWithHelpBubble("License Type", LICENSE_TYPE_TIP_TEXT, HELP_ICON_TOP_PADDING),
        		UIUtility.getGridBagLabelConstraints(0, 6));
        JPanel licenseTypeContainer = new JPanel(flowRight);
        licenseTypeComboBox = new JComboBox();
        for(LicenseType licenseType : LicenseType.values()) {
        	licenseTypeComboBox.addItem(licenseType);
        }
        licenseTypeContainer.add(licenseTypeComboBox);
        jobPanel.add(licenseTypeContainer, UIUtility.getGridBagFieldConstraints(1, 6));     
        
        //Data Provided By Text Field
        jobPanel.add(UIUtility.generateLabelWithHelpBubble("Data Provided By", DATA_PROVIDED_BY_TIP_TEXT, HELP_ICON_TOP_PADDING),
        		UIUtility.getGridBagLabelConstraints(0, 7)); 
        JPanel dataProvidedByTextFieldContainer = new JPanel(flowRight);
        dataProvidedByTextField = new JTextField();
        dataProvidedByTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        dataProvidedByTextFieldContainer.add(dataProvidedByTextField);
        jobPanel.add(dataProvidedByTextFieldContainer, UIUtility.getGridBagFieldConstraints(1, 7));  
        
        //Source Link Text Field
        jobPanel.add(UIUtility.generateLabelWithHelpBubble("Source Link", SOURCE_LINK_TIP_TEXT, HELP_ICON_TOP_PADDING),
        		UIUtility.getGridBagLabelConstraints(0, 8)); 
        JPanel sourceLinkTextFieldContainer = new JPanel(flowRight);
        sourceLinkTextField = new JTextField();
        sourceLinkTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sourceLinkTextFieldContainer.add(sourceLinkTextField);
        jobPanel.add(sourceLinkTextFieldContainer, UIUtility.getGridBagFieldConstraints(1, 8)); 
        
        //Contact Info Text Field
        jobPanel.add(UIUtility.generateLabelWithHelpBubble("Contact Info", CONTACT_INFO_TIP_TEXT, HELP_ICON_TOP_PADDING),
        		UIUtility.getGridBagLabelConstraints(0, 9)); 
        JPanel contactInfoTextFieldContainer = new JPanel(flowRight);
        contactInfoTextField = new JTextField();
        contactInfoTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        contactInfoTextFieldContainer.add(contactInfoTextField);
        jobPanel.add(contactInfoTextFieldContainer, UIUtility.getGridBagFieldConstraints(1, 9));          
        
        //Command to execute
        jobPanel.add(UIUtility.generateLabelWithHelpBubble(
                "Command to execute with scheduler", RUN_COMMAND_TIP_TEXT, HELP_ICON_TOP_PADDING),
                UIUtility.getGridBagLabelConstraints(0, 10));
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
        jobPanel.add(runCommandTextFieldContainer, UIUtility.getGridBagFieldConstraints(1, 10));

        // Load job data into fields
        populateFieldsFromJobData();

        jobTabTitleLabel = new JLabel(job.getJobFilename());
       
    }

    public JPanel getTabPanel() {
        return jobPanel;
    }

    public JobStatus runJobNow() {
    	populateJobFromFields();
        return metadataJob.run();
    }

    public void saveJob() {
    	populateJobFromFields();

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
                metadataJob.setPathToSavedFile(selectedJobFileLocation);
                jobTabTitleLabel.setText(metadataJob.getJobFilename());
                updateJobCommandTextField = true;
            }
        }
        // actually save the job file (may overwrite)
        try {
            metadataJob.writeToFile(selectedJobFileLocation);

            // Update job tab title label
            jobTabTitleLabel.setText(metadataJob.getJobFilename());

            // Update the textfield with new command
            if(updateJobCommandTextField) {
                String runJobCommand = Utils.getRunJobCommand(
                        metadataJob.getPathToSavedFile());
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
    
    private class LoadMetadataActionListener implements ActionListener {
    	public void actionPerformed(ActionEvent event) {
    		int confirmResult = JOptionPane.showConfirmDialog(mainFrame, "This will reset the values in all metadata fields with the saved metadata values.  Any unsaved changes will be lost.  Do you want to continue?", "Reset Metadata Fields", JOptionPane.OK_CANCEL_OPTION);
    		if (confirmResult == JOptionPane.OK_OPTION) {
    			if (metadataJob == null) {
    				metadataJob = new MetadataJob();
    			}
    			
    			metadataJob.setDatasetID(datasetIDTextField.getText());    			
    			String resultingErrors = metadataJob.loadCurrentMetadata();
    			if (!StringUtils.isBlank(resultingErrors)) {
    				JOptionPane.showMessageDialog(mainFrame, "Error loading metadata: " + resultingErrors);
    			}
    			populateFieldsFromJobData();
    		}    		
    	}
    }
    
    private void populateFieldsFromJobData() {
        datasetIDTextField.setText(metadataJob.getDatasetID());
        titleTextField.setText(metadataJob.getTitle());
        descriptionTextArea.setText(metadataJob.getDescription());
        categoryTextField.setText(metadataJob.getCategory());
        keywordsTextField.setText(StringUtils.join(metadataJob.getKeywords(), ","));
        licenseTypeComboBox.setSelectedItem(LicenseType.getLicenseTypeForValue(metadataJob.getLicenseTypeId()));
//        for(LicenseType licenseType : LicenseType.values()) {
//        	if (licenseType.getLabel().equals(metadataJob.getLicenseName())) {
//        		licenseTypeComboBox.setSelectedItem(licenseType);
//        	}
//        }        
        dataProvidedByTextField.setText(metadataJob.getDataProvidedBy());
        sourceLinkTextField.setText(metadataJob.getSourceLink());
        contactInfoTextField.setText(metadataJob.getContactInfo());

        jobFileLocation = metadataJob.getPathToSavedFile();
        // if this is an existing job (meaning the job was opened from a file)
        // then populate the scheduler command textfield
        if(!StringUtils.isBlank(jobFileLocation)) {
            runCommandTextField.setText(Utils.getRunJobCommand(jobFileLocation));
        }
    }
    
    private void populateJobFromFields() {
    	if (metadataJob == null) {
    		metadataJob = new MetadataJob();
    	}
    	metadataJob.setDatasetID(datasetIDTextField.getText());
    	metadataJob.setTitle(titleTextField.getText());
    	metadataJob.setDescription(descriptionTextArea.getText());
    	metadataJob.setCategory(categoryTextField.getText());
    	if (!StringUtils.isBlank(keywordsTextField.getText())) {
    		metadataJob.setKeywords(Arrays.asList(keywordsTextField.getText().split("\\s*,\\s*")));
    	}
    	else {
    		metadataJob.setKeywords(null);
    	}
    	LicenseType selectedLicenseType = (LicenseType) licenseTypeComboBox.getSelectedItem();
    	if (selectedLicenseType != null) {
    		metadataJob.setLicenseType(selectedLicenseType);
    	}
    	metadataJob.setDataProvidedBy(dataProvidedByTextField.getText());
    	metadataJob.setSourceLink(sourceLinkTextField.getText());
    	metadataJob.setContactInfo(contactInfoTextField.getText());
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

}
