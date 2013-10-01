package com.socrata.datasync;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Authors: Adrian Laurenzi and Louis Fettet
 * Date: 9/11/13
 */
public class PortJobTab implements JobTab {

    private final int JOB_TEXTFIELD_WIDTH = 370;
    private final int JOB_TEXTFIELD_HEIGHT = 26;
    private final int JOB_FIELD_VGAP = 8;
    private final int SINK_DATASET_ID_TEXTFIELD_WIDTH = 210;

    private final String DEFAULT_DESTINATION_SET_ID = "(Generates after running job)";
    private final String JOB_FILE_NAME = "Socrata Port Job";
    private final String JOB_FILE_EXTENSION = "spj";

    private JFrame mainFrame;
    private JPanel jobPanel;

    private String jobFileLocation;
    private JLabel jobTabTitleLabel;

    private JComboBox portMethodComboBox;
    private JCheckBox publishCheck;
    private JTextField sourceSiteDomainTextField;
    private JTextField sourceSetIDTextField;
    private JTextField sinkSiteDomainTextField;
    private JTextField sinkSetIDTextField;
    

    // build Container with all tab components and load data into form
    public PortJobTab(PortJob job, JFrame containingFrame) {
        mainFrame = containingFrame;

        // build tab panel form
        jobPanel = new JPanel(new GridLayout(0,2));
        
        // Port Method and Publish
        jobPanel.add(new JLabel("Port Method"));
        JPanel portpubContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
        portMethodComboBox = new JComboBox();
        for (PortMethod method : PortMethod.values()) {
            portMethodComboBox.addItem(method);
        }
        portMethodComboBox.addItemListener(new PortMethodItemListener());
        portpubContainer.add(portMethodComboBox);
        portpubContainer.add(new JLabel("   Publish?"));
        publishCheck = new JCheckBox();
        portpubContainer.add(publishCheck);
        jobPanel.add(portpubContainer);

        
        // Source Site
        jobPanel.add(new JLabel("Source Site (domain where dataset is located)"));
        JPanel sourceSiteTextFieldContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
        sourceSiteDomainTextField = new JTextField();
        sourceSiteDomainTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sourceSiteTextFieldContainer.add(sourceSiteDomainTextField);
        jobPanel.add(sourceSiteTextFieldContainer);

        // Source Site Dataset ID
        jobPanel.add(new JLabel("Source Dataset ID (i.e. n38h-y5wp)"));
        JPanel sourceSetIDTextFieldContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
        sourceSetIDTextField = new JTextField();
        sourceSetIDTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sourceSetIDTextFieldContainer.add(sourceSetIDTextField);
        jobPanel.add(sourceSetIDTextFieldContainer);

        // Sink Site
        jobPanel.add(new JLabel("Destination Site (domain where you want copy to go)"));
        JPanel sinkSiteTextFieldContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
        sinkSiteDomainTextField = new JTextField();
        sinkSiteDomainTextField.setPreferredSize(new Dimension(
                JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sinkSiteTextFieldContainer.add(sinkSiteDomainTextField);
        jobPanel.add(sinkSiteTextFieldContainer);

        // Sink set dataset ID
        jobPanel.add(new JLabel("Destination Dataset ID"));
        JPanel destinationSetIDTextFieldContainer = new JPanel(
                new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
        sinkSetIDTextField = new JTextField(DEFAULT_DESTINATION_SET_ID);
        sinkSetIDTextField.setPreferredSize(new Dimension(
                SINK_DATASET_ID_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
        sinkSetIDTextField.setEditable(false);
        destinationSetIDTextFieldContainer.add(sinkSetIDTextField);
        JButton openSinkDatasetButton = new JButton("Open Dataset");
        openSinkDatasetButton.addActionListener(new OpenDatasetButtonListener());
        destinationSetIDTextFieldContainer.add(openSinkDatasetButton);
        jobPanel.add(destinationSetIDTextFieldContainer);

        // Load job data into fields
        sourceSiteDomainTextField.setText(job.getSourceSiteDomain());
        sourceSetIDTextField.setText(job.getSourceSetID());
        sinkSiteDomainTextField.setText(job.getSinkSiteDomain());

        PortMethod jobPortMethod = job.getPortMethod();
        int i = 0;
        for(PortMethod method : PortMethod.values()) {
            if(method.equals(jobPortMethod)) {
                portMethodComboBox.setSelectedIndex(i);
                break;
            }
            i++;
        }

        jobFileLocation = job.getPathToSavedFile();

        // if this is an existing job (meaning the job was opened from a file) -> populate the dataset sink ID textfield
        if(!jobFileLocation.equals("")) {
            sinkSetIDTextField.setText(job.getSinkSetID());
        }

        jobTabTitleLabel = new JLabel(job.getJobFilename());
    }

    public JPanel getTabPanel() {
        return jobPanel;
    }

    public JobStatus runJobNow() {
        PortJob jobToRun = new PortJob();
        jobToRun.setPortMethod((PortMethod) portMethodComboBox.getSelectedItem());
        jobToRun.setPublishCheck(publishCheck.isSelected());
        jobToRun.setSourceSiteDomain(sourceSiteDomainTextField.getText());
        jobToRun.setSourceSetID(sourceSetIDTextField.getText());
        jobToRun.setSinkSiteDomain(sinkSiteDomainTextField.getText());

        // TODO include source sink ID
        // jobToRun.setSinkSetID(...);

        JobStatus status = jobToRun.run();
        if(!status.isError()) {
            sinkSetIDTextField.setText(jobToRun.getSinkSetID());
        }
        return status;
    }

    public void saveJob() {
        // Save job data
        PortJob newPortJob = new PortJob();
        newPortJob.setPortMethod(
                (PortMethod) portMethodComboBox.getSelectedItem());
        newPortJob.setPublishCheck(publishCheck.isSelected());
        newPortJob.setSourceSiteDomain(sourceSiteDomainTextField.getText());
        newPortJob.setSourceSetID(sourceSetIDTextField.getText());
        newPortJob.setSinkSiteDomain(sinkSiteDomainTextField.getText());
        newPortJob.setSinkSetID(sinkSetIDTextField.getText());
        

        newPortJob.setPathToSavedFile(jobFileLocation);

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
                newPortJob.setPathToSavedFile(selectedJobFileLocation);
                jobTabTitleLabel.setText(newPortJob.getJobFilename());
            }
        }
        // actually save the job file (may overwrite)
        newPortJob.writeToFile(selectedJobFileLocation);

        // Update job tab title label
        jobTabTitleLabel.setText(newPortJob.getJobFilename());
    }

    public JLabel getJobTabTitleLabel() {
        return jobTabTitleLabel;
    }

    public String getJobFileLocation() {
        return jobFileLocation;
    }

    /**
     * Returns the URI to the sink dataset based on the form text fields
     * @return URI to sink dataset, or null if URI was malformed
     */
    public URI getURIToSinkDataset() {
        URI sinkDatasetURI = null;
        try {
            sinkDatasetURI = new URI(
                    sinkSiteDomainTextField.getText() + "/d/" + sinkSetIDTextField.getText());

        } catch (URISyntaxException uriE) {
            System.out.println("Could not open sink dataset URL");
        }
        return sinkDatasetURI;
    }

    private class PortMethodItemListener implements ItemListener {
    	public void itemStateChanged(ItemEvent e) {
    		if(e.getStateChange() == ItemEvent.SELECTED) {
    			PortMethod item = (PortMethod) e.getItem();
    			switch(item) {
    			case copy_data:
    				sinkSetIDTextField.setText("");
    		        sinkSetIDTextField.setEditable(true);
    		        publishCheck.setSelected(false);
    		        publishCheck.setEnabled(false);
    		        System.out.println(publishCheck.isSelected());
    				break;
    			case copy_schema:
    			case copy_all:
    				sinkSetIDTextField.setText(DEFAULT_DESTINATION_SET_ID);
    		        sinkSetIDTextField.setEditable(false);
    		        publishCheck.setEnabled(true);
    				break;
    			}
    		}
    	}
    }   
     
    private class OpenDatasetButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if(!sinkSetIDTextField.getText().equals(DEFAULT_DESTINATION_SET_ID)) {
                IntegrationUtility.openWebpage(getURIToSinkDataset());
            }
        }
    }
}
