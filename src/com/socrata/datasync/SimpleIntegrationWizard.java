package com.socrata.datasync;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.socrata.api.HttpLowLevel;
import com.socrata.api.Soda2Consumer;
import com.socrata.builders.SoqlQueryBuilder;
import com.socrata.exceptions.LongRunningQueryException;
import com.socrata.exceptions.SodaError;
import com.socrata.model.soql.SoqlQuery;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jndi.toolkit.url.Uri;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

public class SimpleIntegrationWizard {
	/**
	 * @author Adrian Laurenzi
	 * 
	 * GUI interface to DataSync
	 */
	
	private final String VERSION = "0.1";
	private final String METADATA_URL = "https://adrian.demo.socrata.com";
	private final String METADATA_DATASET_ID = "7w7i-q9n6";
	
	private final String TITLE = "Socrata DataSync " + VERSION;
	private final String LOGO_FILE_PATH = "/datasync_logo.png";
	private final int FRAME_WIDTH = 800;
	private final int FRAME_HEIGHT = 360;
	private final Dimension JOB_PANEL_DIMENSION = new Dimension(780, 220);
	private final int JOB_TEXTFIELD_WIDTH = 370;
	private final int JOB_COMMAND_TEXTFIELD_WIDTH = 211;
	private final int JOB_FILE_TEXTFIELD_WIDTH = 238;
	private final int JOB_TEXTFIELD_HEIGHT = 26;
	private final int JOB_FIELD_VGAP = 8;
	private final int DEFAULT_TEXTFIELD_COLS = 25;
	private final Dimension AUTH_DETAILS_DIMENSION = new Dimension(465, 100);
	private final int PREFERENCES_FRAME_WIDTH = 475;
	private final int PREFERENCES_FRAME_HEIGHT = 335;
	
	private static UserPreferences userPrefs;
	
	private final String JOB_FILE_NAME = "Socrata Integration Job";
	private final String JOB_FILE_EXTENSION = "sij";
	private final String DEFAULT_RUN_JOB_COMMAND = "(Will be generated upon saving this job)";
	
	private JTextField domainTextField, usernameTextField, apiKeyTextField;
	private JPasswordField passwordField;
	private JTextField logDatasetIDTextField, adminEmailTextField;
	private JTextField outgoingMailServerTextField, smtpPortTextField, sslPortTextField, smtpUsernameTextField;
	private JPasswordField smtpPasswordField;
	private JCheckBox useSSLCheckBox;
	private JCheckBox emailUponErrorCheckBox;
	
	/**
	 * Stores a list of datasetID text fields where the index of a text 
	 * field corresponds to the index of the tabbed pane it exists in.
	 * NOTE: only add to this list in the 'addJobTab' method
	 */
	private List<JTextField> datasetIDTextFields;
	
	/**
	 * Stores a list of 'file to publish' text fields where the index of a  
	 * text field corresponds to the index of the tabbed pane it exists in.
	 * NOTE: only add to this list in the 'addJobTab' method
	 */
	private List<JTextField> fileToPublishTextFields;
	
	/**
	 * Stores a list of job files (*.sij) where the index of a file 
	 * corresponds to the index of the tabbed pane it exists in.
	 * NOTE: only add to this list in the 'addJobTab' method
	 */
	private List<String> jobFileLocations;
	
	/**
	 * Stores a list of 'publish method' comboboxes where the index of a  
	 * text field corresponds to the index of the tabbed pane it exists in.
	 * NOTE: only add to this list in the 'addJobTab' method
	 */
	private List<JComboBox> publishMethodComboBoxes;
	
	/**
	 * Stores a list of job tab title labels where the index of a  
	 * label corresponds to the index of the tabbed pane it labels.
	 * NOTE: only add to this list in the 'addJobTab' method
	 */
	private List<JLabel> jobTabTitleLabels;
	
	/**
	 * Stores a list of job command text fields where the index of a  
	 * field corresponds to the index of the tabbed pane it exists in.
	 * NOTE: only add to this list in the 'addJobTab' method
	 */
	private List<JTextField> jobTabCommandTextFields;
	
	private JTabbedPane jobTabsPane;
	private JFrame frame;
	private JFrame prefsFrame;
	
	/*
	 * Constructs the GUI and displays it on the screen.
	 */
	public SimpleIntegrationWizard() {
		// load user preferences (saved locally)
		userPrefs = new UserPreferences();
		
		// Build GUI
		frame = new JFrame(TITLE);
		frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);
		
		// save tabs on close
		//OLD: frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
            	saveAuthenticationInfoFromForm();
            	// TODO save open tabs to userPrefs
                System.exit(0);
            }
        });
		
		JMenuBar menuBar = generateMenuBar();
		frame.setJMenuBar(menuBar);
		
		JPanel mainPanel = generateMainPanel();
		loadAuthenticationInfoIntoForm();
		
		// Create preferences popup window
		prefsFrame = new JFrame("Preferences");
		prefsFrame.setSize(PREFERENCES_FRAME_WIDTH, PREFERENCES_FRAME_HEIGHT);
		prefsFrame.setVisible(false);
		JPanel preferencesPanel = generatePreferencesPanel();
		prefsFrame.add(preferencesPanel);
		prefsFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		prefsFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
            	prefsFrame.setVisible(false);
            }
        });
		
		/* NOT USED, but may be useful in the future...
		// Create "cards" to enable switching screens
		screensPanel = new JPanel();
		screens = new CardLayout();
		screensPanel.setLayout(screens);
        screensPanel.add(mainPanel, MAIN_SCREEN_ID);
        screensPanel.add(preferencesPanel, PREFERENCES_SCREEN_ID);
        screens.show(screensPanel, "main");
        frame.add(screensPanel);*/
		
        frame.add(mainPanel);
		
        frame.pack();
        // centers the window
     	frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		
		// Alert user if new version is available
		try {
			checkVersion();
		} catch (Exception e) {
			// do nothing
		}
	}
	
	/** Queries special Socrata dataset with DataSync metadata to 
	 *  determine if this is the most recent version. Alert user
	 *  if a new version is available
	 *   
	 * @throws LongRunningQueryException 
	 * @throws SodaError 
	 * @throws MalformedURLException 
	 * @throws URISyntaxException 
	 * @throws JSONException 
	 */
	private void checkVersion() throws LongRunningQueryException, SodaError, JSONException, URISyntaxException {
		final Soda2Consumer consumer = Soda2Consumer.newConsumer(METADATA_URL);
		
		ClientResponse response = consumer.query(METADATA_DATASET_ID, HttpLowLevel.JSON_TYPE, SoqlQuery.SELECT_ALL);
		String payload = response.getEntity(String.class);
    	
    	JSONArray responseJSON = null;
	    responseJSON = new JSONArray(payload);
	    JSONObject allMetadata = responseJSON.getJSONObject(0);
	    String currentVersion = allMetadata.get("current_version").toString();
	    
	    if(!currentVersion.equals(VERSION)) {
	    	Object[] options = {"Download Now", "No Thanks"};
            int n = JOptionPane.showOptionDialog(frame,
            				"A new version of DataSync is available (version " 
        	    	    	+ currentVersion + ").\nDo you want to download it now?\n",
        	    	    	"Alert: New Version Available",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]);
            if (n == JOptionPane.YES_OPTION) {
            	URI currentVersionDownloadURI = new URI(
    	    			allMetadata.get("current_version_download_url").toString());
            	IntegrationUtility.openWebpage(currentVersionDownloadURI);
            }
	    }
	}
	
	private void addJobTab(IntegrationJob job) {
		JPanel newJobPanel = new JPanel(new GridLayout(0,2));
		
		newJobPanel.add(new JLabel("Dataset ID (i.e. n38h-y5wp)"));
		JPanel datasetIDTextFieldContainer = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
		JTextField newDatasetIDTextField = new JTextField();
		newDatasetIDTextField.setPreferredSize(new Dimension(
				JOB_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
		datasetIDTextFieldContainer.add(newDatasetIDTextField);
		newJobPanel.add(datasetIDTextFieldContainer);
		
		newJobPanel.add(new JLabel("File to publish"));
		JPanel fileSelectorContainer = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
		JTextField newfileToPublishTextField = new JTextField();
		newfileToPublishTextField.setPreferredSize(new Dimension(
				JOB_FILE_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
		fileSelectorContainer.add(newfileToPublishTextField);
		
		JFileChooser fileToPublishChooser = new JFileChooser();
        JButton openButton = new JButton("Select a file...");
        FileToPublishSelectorListener chooserListener = new FileToPublishSelectorListener(
        		fileToPublishChooser, newfileToPublishTextField);
        openButton.addActionListener(chooserListener);
        fileSelectorContainer.add(openButton);
        newJobPanel.add(fileSelectorContainer);
		
		newJobPanel.add(new JLabel("Publish method"));
		JPanel publishMethodTextFieldContainer = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
		JComboBox newPublishMethodComboBox = new JComboBox();
		for(PublishMethod method : PublishMethod.values()) {
			newPublishMethodComboBox.addItem(method);
		}
		publishMethodTextFieldContainer.add(newPublishMethodComboBox);
		newJobPanel.add(publishMethodTextFieldContainer);
		
		// Load job data into fields
		newDatasetIDTextField.setText(job.getDatasetID());
		newfileToPublishTextField.setText(job.getFileToPublish());
		PublishMethod jobPublishMethod = job.getPublishMethod();
		int i = 0;
		for(PublishMethod method : PublishMethod.values()) {
			if(method.equals(jobPublishMethod)) {
				newPublishMethodComboBox.setSelectedIndex(i);
				break;
			}
			i++;
		}
		
		newJobPanel.add(new JLabel("Command to execute with scheduler"));
		JPanel runCommandTextFieldContainer = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 0, JOB_FIELD_VGAP));
		JTextField newRunCommandTextField = new JTextField(DEFAULT_RUN_JOB_COMMAND);
		newRunCommandTextField.setPreferredSize(new Dimension(
				JOB_COMMAND_TEXTFIELD_WIDTH, JOB_TEXTFIELD_HEIGHT));
		newRunCommandTextField.setEditable(false);
		newRunCommandTextField.addMouseListener(new JobCommandTextFieldListener());
		runCommandTextFieldContainer.add(newRunCommandTextField);
		JButton copyJobCommandButton = new JButton("Copy to clipboard");
		copyJobCommandButton.addActionListener(new CopyJobCommandListener());
		runCommandTextFieldContainer.add(copyJobCommandButton);
		newJobPanel.add(runCommandTextFieldContainer);
		
		// Create job action buttons
		JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton runJobNowButton = new JButton("Run Job Now");
		runJobNowButton.addActionListener(new RunJobNowListener());
		leftButtonPanel.add(runJobNowButton);
		JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton saveJobButton = new JButton("Save Job");
		saveJobButton.addActionListener(new SaveJobListener());
		rightButtonPanel.add(saveJobButton);
		newJobPanel.add(leftButtonPanel);
		newJobPanel.add(rightButtonPanel);		
		
		// Build the tab with close button
	    FlowLayout tabLayout = new FlowLayout(FlowLayout.CENTER, 5, 0);
	    JPanel tabPanel = new JPanel(tabLayout);
	    tabPanel.setOpaque(false);
	 
	    // Add a JLabel with title and the left-side tab icon
	    JLabel tabTitleLabel = new JLabel(job.getJobFilename());
	    
	    // Create a JButton for the close tab button
	    JButton closeTabButton = new JButton("[X]");
	    
	    // TODO make close button an icon rather than [X]
	    //tabTitleLabel.setIcon(icon);
	    //closeTabButton.setOpaque(false);
	    // Configure icon and rollover icon for button
	    //closeTabButton.setRolloverIcon(CLOSE_TAB_ICON);
	    //closeTabButton.setRolloverEnabled(true);
	    //closeTabButton.setIcon(RGBGrayFilter.getDisabledIcon(closeTabButton, CLOSE_TAB_ICON));
	    
	    closeTabButton.setBorder(null);
	    closeTabButton.setFocusable(false);
	    tabPanel.add(tabTitleLabel);
	    tabPanel.add(closeTabButton);
	    // Add a thin border to keep the image below the top edge of the tab
	    // when the tab is selected
	    tabPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        
	    // Add all components to their respective reference lists to ensure
        // this tabbed pane index corresponds to that of each input element 
        // within the tabbed pane.
        datasetIDTextFields.add(newDatasetIDTextField);
        fileToPublishTextFields.add(newfileToPublishTextField);
        publishMethodComboBoxes.add(newPublishMethodComboBox);
        jobFileLocations.add(job.getPathToSavedFile());
        jobTabTitleLabels.add(tabTitleLabel);
        jobTabCommandTextFields.add(newRunCommandTextField);
	    
		// Put tab with close button into tabbed pane 
        jobTabsPane.addTab(null, newJobPanel);
	    int pos = jobTabsPane.indexOfComponent(newJobPanel);
	    
	    // Now assign the component for the tab
	    jobTabsPane.setTabComponentAt(pos, tabPanel);
	    
	    closeTabButton.addActionListener(new CloseJobFromTabListener());
	    
	    assert(jobTabsValid());
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
			int returnVal = fileChooser.showOpenDialog(frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                filePathTextField.setText(file.getAbsolutePath());
            } else {
            	// Open command cancelled by user: do nothing
            }
		}
	}

	private class RunJobNowListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			saveAuthenticationInfoFromForm();
			
			// create an run integration job with data from form
	        IntegrationJob jobToRun = new IntegrationJob();
	        int selectedJobTabIndex = jobTabsPane.getSelectedIndex();
	        jobToRun.setDatasetID(
	        		datasetIDTextFields.get(selectedJobTabIndex).getText());
	        jobToRun.setFileToPublish(
	        		fileToPublishTextFields.get(selectedJobTabIndex).getText());
	        jobToRun.setPublishMethod(
	        		(PublishMethod) publishMethodComboBoxes.get(selectedJobTabIndex).getSelectedItem());
	        //jobToRun.setFileColsToDelete("/home/adrian/delete.csv");
	        JobStatus status = jobToRun.run();
	        
	        // show popup with returned status
	        JOptionPane.showMessageDialog(frame, status.getMessage());
		}
	}
	
	private class CopyJobCommandListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			int selectedJobTabIndex = jobTabsPane.getSelectedIndex();
			String runJobCommand = jobTabCommandTextFields.get(selectedJobTabIndex).getText();
			StringSelection stringSelection = new StringSelection(runJobCommand);
			Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
			clpbrd.setContents(stringSelection, null);
		}
	}
	
	private class SaveJobListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// save authentication data (userPrefs)
			saveAuthenticationInfoFromForm();
			
			// Save job data
			IntegrationJob newIntegrationJob = new IntegrationJob();
			int selectedJobTabIndex = jobTabsPane.getSelectedIndex();
			newIntegrationJob.setDatasetID(
					datasetIDTextFields.get(selectedJobTabIndex).getText());
			newIntegrationJob.setFileToPublish(
					fileToPublishTextFields.get(selectedJobTabIndex).getText());
	        newIntegrationJob.setPublishMethod(
	        		(PublishMethod) publishMethodComboBoxes.get(selectedJobTabIndex).getSelectedItem());
	        newIntegrationJob.setPathToSavedFile(
	        		jobFileLocations.get(selectedJobTabIndex));
	        
	        // TODO If an existing file was selected WARN user of overwriting
	        
	        // if first time saving this job: Open dialog box to select "Save as..." location
	        // otherwise save to existing file
	        boolean updateJobCommandTextField = false;
	        String selectedJobFileLocation = jobFileLocations.get(selectedJobTabIndex);
	        if(selectedJobFileLocation.equals("")) {
	        	JFileChooser savedJobFileChooser = new JFileChooser();
	        	FileNameExtensionFilter filter = new FileNameExtensionFilter(
	        			JOB_FILE_NAME + " (*." + JOB_FILE_EXTENSION + ")", JOB_FILE_EXTENSION);
	        	savedJobFileChooser.setFileFilter(filter);
	        	int returnVal = savedJobFileChooser.showSaveDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
	                File file = savedJobFileChooser.getSelectedFile();
	                selectedJobFileLocation = file.getAbsolutePath();
	                if(!selectedJobFileLocation.endsWith("." + JOB_FILE_EXTENSION)) {
	                	selectedJobFileLocation += "." + JOB_FILE_EXTENSION;
	                }
	                jobFileLocations.set(jobTabsPane.getSelectedIndex(), selectedJobFileLocation);
	                newIntegrationJob.setPathToSavedFile(selectedJobFileLocation);
	                jobTabTitleLabels.get(jobTabsPane.getSelectedIndex()).setText(
	                		newIntegrationJob.getJobFilename());
	                updateJobCommandTextField = true;
	            }
	        }
	        // actually save the job file (may overwrite)
	        newIntegrationJob.writeToFile(selectedJobFileLocation);
	        
	        // Update the textfield with new command
	        if(updateJobCommandTextField) {
	        	String runJobCommand = getRunJobCommand(newIntegrationJob.getPathToSavedFile());
	        	jobTabCommandTextFields.get(jobTabsPane.getSelectedIndex()).setText(runJobCommand);
	        }
		}
	}
	
	private class OpenJobButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JFileChooser savedJobFileChooser = new JFileChooser();
        	FileNameExtensionFilter filter = new FileNameExtensionFilter(
	                "Socrata Job File (*." + JOB_FILE_EXTENSION + ")", JOB_FILE_EXTENSION);
        	savedJobFileChooser.setFileFilter(filter);
        	int returnVal = savedJobFileChooser.showOpenDialog(frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File openedFile = savedJobFileChooser.getSelectedFile();
				// Ensure file exists
				if(openedFile.exists()) {	
					// ensure this job is not already open
					String openedFileLocation = openedFile.getAbsolutePath();
	                int indexOfAlreadyOpenFile = jobFileLocations.indexOf(openedFileLocation);
					if(indexOfAlreadyOpenFile == -1) {
						addJobTab(new IntegrationJob(openedFileLocation));
	                	jobTabsPane.setSelectedIndex(jobTabsPane.getTabCount() - 1);
	                	// Update the textfield with command
	        	        String runJobCommand = getRunJobCommand(openedFileLocation);
	        	        jobTabCommandTextFields.get(jobTabsPane.getSelectedIndex()).setText(runJobCommand);
	                } else {
	                	// file already open, select that tab
	                	jobTabsPane.setSelectedIndex(indexOfAlreadyOpenFile);
	                }
				}
            }
		}
	}
	
	private class NewJobListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			addJobTab(new IntegrationJob());
			jobTabsPane.setSelectedIndex(jobTabsPane.getTabCount() - 1);
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
	
	/**
	 * Listen for action to close currently selected tab
	 */
	private class CloseJobFromTabListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JComponent source = (JComponent) e.getSource();
		    Container tabComponent = source.getParent();
		    int jobTabIndex = jobTabsPane.indexOfTabComponent(tabComponent);
		    closeJobTab(jobTabIndex);
		}
	}
	
	private JMenuBar generateMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		//fileMenu.setMnemonic(KeyEvent.VK_A);
		//fileMenu.getAccessibleContext().setAccessibleDescription(
		//        "The only menu in this program that has menu items");
		menuBar.add(fileMenu);
		
		JMenuItem newJobItem = new JMenuItem("New Job");
		newJobItem.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		fileMenu.add(newJobItem);
		
		JMenuItem openJobItem = new JMenuItem("Open Job");
		openJobItem.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		fileMenu.add(openJobItem);
		
		JMenuItem saveJobItem = new JMenuItem("Save Job");
		saveJobItem.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		fileMenu.add(saveJobItem);
		
		JMenuItem runJobItem = new JMenuItem("Run Job Now");
		runJobItem.setAccelerator(KeyStroke.getKeyStroke(
			KeyEvent.VK_R, ActionEvent.CTRL_MASK));
		fileMenu.add(runJobItem);
		
		JMenu editMenu = new JMenu("Edit");
		menuBar.add(editMenu);
		
		JMenuItem prefsItem = new JMenuItem("Preferences");
		editMenu.add(prefsItem);

		newJobItem.addActionListener(new NewJobListener());
		openJobItem.addActionListener(new OpenJobButtonListener());
		saveJobItem.addActionListener(new SaveJobListener());
		runJobItem.addActionListener(new RunJobNowListener());
		prefsItem.addActionListener(new OpenPreferencesListener());
		
		return menuBar;
	}
	
	private JPanel generateMainPanel() {
		JPanel mainContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
		mainContainer.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
		
		// Initialise reference lists of input elements within job tab pane
		datasetIDTextFields = new ArrayList<JTextField>();
		fileToPublishTextFields = new ArrayList<JTextField>();
		publishMethodComboBoxes = new ArrayList<JComboBox>();
		jobFileLocations = new ArrayList<String>();
		jobTabTitleLabels = new ArrayList<JLabel>();
		jobTabCommandTextFields = new ArrayList<JTextField>();
		
		// Build empty job tabbed pane
		JPanel jobTabsContainer = new JPanel(new GridLayout(1, 1));
		jobTabsContainer.setPreferredSize(JOB_PANEL_DIMENSION);
		jobTabsPane = new JTabbedPane();
		jobTabsContainer.add(jobTabsPane);
        mainContainer.add(jobTabsContainer);
        jobTabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);        
        
        // TODO populate job tabs w/ previously opened tabs or [if none] a new job tab
        addJobTab(new IntegrationJob());
        
		JPanel authenticationDetailsPanel = generateAuthenticationDetailsFormPanel();
		mainContainer.add(authenticationDetailsPanel);
		
		URL imageURL = getClass().getResource(LOGO_FILE_PATH);
		if(imageURL != null) {
			// Add Socrata logo (only works when running from .jar file)
			JLabel logoLabel = new JLabel(
					new ImageIcon(getClass().getResource(LOGO_FILE_PATH)));
			Border paddingBorder = BorderFactory.createEmptyBorder(15,15,15,15);
			logoLabel.setBorder(paddingBorder);
			mainContainer.add(logoLabel);
		}
		
		return mainContainer;
	}
	
	private JPanel generatePreferencesPanel() {
		JPanel prefsPanel = new JPanel(new GridLayout(0,2));
		
		prefsPanel.add(new JLabel(" Log Dataset ID (i.e. n38h-y5wp)"));
		logDatasetIDTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		prefsPanel.add(logDatasetIDTextField);
		
		prefsPanel.add(new JLabel(" Admin Email"));
		adminEmailTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		prefsPanel.add(adminEmailTextField);
		
		emailUponErrorCheckBox = new JCheckBox(" Auto-email admin upon error");
		prefsPanel.add(emailUponErrorCheckBox);
		prefsPanel.add(new JLabel("*must fill in SMTP Settings below"));
		
		JLabel smtpSettingsLabel = new JLabel(" SMTP Settings");
		Font boldFont = new Font(smtpSettingsLabel.getFont().getFontName(), 
				Font.BOLD, smtpSettingsLabel.getFont().getSize());
		smtpSettingsLabel.setFont(boldFont);
		prefsPanel.add(smtpSettingsLabel);
		prefsPanel.add(new JLabel(""));
		prefsPanel.add(new JLabel(" Outgoing Mail Server"));
		outgoingMailServerTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		prefsPanel.add(outgoingMailServerTextField);
		prefsPanel.add(new JLabel(" SMTP Port"));
		smtpPortTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		prefsPanel.add(smtpPortTextField);
		final JPanel sslPortContainer = new JPanel(
				new FlowLayout(FlowLayout.LEFT, 0, 0));
		sslPortContainer.setVisible(false);
		useSSLCheckBox = new JCheckBox("Use SSL");
		prefsPanel.add(useSSLCheckBox);
		sslPortContainer.add(new JLabel(" SSL Port  "));
		sslPortTextField = new JTextField();
		sslPortTextField.setPreferredSize(new Dimension(
				50, JOB_TEXTFIELD_HEIGHT));
		sslPortContainer.add(sslPortTextField);
		useSSLCheckBox.addItemListener(new ItemListener() {
		    public void itemStateChanged(ItemEvent e) {
		    	if(useSSLCheckBox.isSelected()) {
		    		sslPortContainer.setVisible(true);
		    	} else {
		    		sslPortContainer.setVisible(false);
		    	}
		    }
		});
		prefsPanel.add(sslPortContainer);
		prefsPanel.add(new JLabel(" SMTP Username"));
		smtpUsernameTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		prefsPanel.add(smtpUsernameTextField);
		prefsPanel.add(new JLabel(" SMTP Password"));
		smtpPasswordField = new JPasswordField(DEFAULT_TEXTFIELD_COLS);
		prefsPanel.add(smtpPasswordField);
		
		JPanel prefsButtonContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton cancelPrefsButton = new JButton("Cancel");
		cancelPrefsButton.addActionListener(new CancelPreferencesListener());
		prefsButtonContainer.add(cancelPrefsButton);
		JButton savePrefsButton = new JButton("Save");
		savePrefsButton.addActionListener(new SavePreferencesListener());
		prefsButtonContainer.add(savePrefsButton);
		prefsPanel.add(prefsButtonContainer);
		
		JPanel testSMTPSettingsContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton testSMTPSettingsButton = new JButton("Test SMTP Settings");
		testSMTPSettingsButton.addActionListener(new TestSMTPSettingsListener());
		testSMTPSettingsContainer.add(testSMTPSettingsButton);
		prefsPanel.add(testSMTPSettingsContainer);
		
		loadPreferencesIntoForm();
		
		return prefsPanel;
	}
	
	private void loadPreferencesIntoForm() {
		adminEmailTextField.setText(userPrefs.getAdminEmail());
		logDatasetIDTextField.setText(userPrefs.getLogDatasetID());
		emailUponErrorCheckBox.setSelected(userPrefs.emailUponError());
		
		outgoingMailServerTextField.setText(userPrefs.getOutgoingMailServer());
		smtpPortTextField.setText(userPrefs.getSMTPPort());
		String sslPort = userPrefs.getSSLPort();
		sslPortTextField.setText(sslPort);
		if(sslPort.equals("")) {
			useSSLCheckBox.setSelected(false);
		} else {
			useSSLCheckBox.setSelected(true);
		}
		smtpUsernameTextField.setText(userPrefs.getSMTPUsername());
		smtpPasswordField.setText(userPrefs.getSMTPPassword());
	}
	
	private void savePreferences() {
		userPrefs.saveAdminEmail(adminEmailTextField.getText());
		userPrefs.saveLogDatasetID(logDatasetIDTextField.getText());
		userPrefs.saveEmailUponError(emailUponErrorCheckBox.isSelected());
		
		userPrefs.saveOutgoingMailServer(outgoingMailServerTextField.getText());
		userPrefs.saveSMTPPort(smtpPortTextField.getText());
		if(useSSLCheckBox.isSelected()) {
			userPrefs.saveSSLPort(sslPortTextField.getText());
		} else {
			userPrefs.saveSSLPort("");
		}
		userPrefs.saveSMTPUsername(smtpUsernameTextField.getText());
		String smtpPassword = new String(smtpPasswordField.getPassword());
    	userPrefs.saveSMTPPassword(smtpPassword);
	}
	
	private class SavePreferencesListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// TODO validation of email and log dataset ID
			savePreferences();
			prefsFrame.setVisible(false);
		}
	}
	
	private class CancelPreferencesListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			loadPreferencesIntoForm();
			prefsFrame.setVisible(false);
		}
	}
	
	private class TestSMTPSettingsListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			savePreferences();
			String adminEmail = userPrefs.getAdminEmail();
			String message;
			try {
				SMTPMailer.send(adminEmail, "Socrata DataSync Test Email", 
						"This email confirms that your SMTP Settings are valid.");
				message = "Sent test email to " + adminEmail + ". Please ensure the "
						+ "email was delievered successfully (it may take a few minutes).";
			} catch (Exception emailE) {
				message = "Error sending email to " + adminEmail + ":\n" + emailE.getMessage();
			}
			JOptionPane.showMessageDialog(prefsFrame, message);
		}
	}
	
	private class OpenPreferencesListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			// centers the window
			prefsFrame.setLocationRelativeTo(null);
			prefsFrame.setVisible(true);
		}
	}
	
	private JPanel generateAuthenticationDetailsFormPanel() {
		JPanel authenticationDetailsPanel = new JPanel(new GridLayout(0,2));
		authenticationDetailsPanel.add(new JLabel("Domain"));
		domainTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		authenticationDetailsPanel.add(domainTextField);
		authenticationDetailsPanel.add(new JLabel("Username"));
		usernameTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		authenticationDetailsPanel.add(usernameTextField);
		authenticationDetailsPanel.add(new JLabel("Password"));
		passwordField = new JPasswordField(DEFAULT_TEXTFIELD_COLS);
		authenticationDetailsPanel.add(passwordField);
		authenticationDetailsPanel.add(new JLabel("App token"));
		apiKeyTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		authenticationDetailsPanel.add(apiKeyTextField);
		authenticationDetailsPanel.setPreferredSize(AUTH_DETAILS_DIMENSION);
		return authenticationDetailsPanel;
	}
	
	private String getRunJobCommand(String pathToSaveJobFile) {
		String jarPath = Main.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		try {
			jarPath = URLDecoder.decode(jarPath, "UTF-8");
			// Needed correct issue with windows where path includes a leading slash
			if(jarPath.contains(":") && (jarPath.startsWith("/") || jarPath.startsWith("\\"))) {
				jarPath = jarPath.substring(1, jarPath.length());
			}
			return "java -jar " + jarPath + " " + pathToSaveJobFile;
		} catch (UnsupportedEncodingException unsupportedEncoding) {
			return "Error getting path to this executeable: " + unsupportedEncoding.getMessage();
		}
	}
	
	private void closeJobTab(int tabIndex) throws IllegalArgumentException {
		if(tabIndex >= jobTabsPane.getTabCount()) {
			throw new IllegalArgumentException("Tab index is invalid");
		}
		// remove fields from reference lists
		datasetIDTextFields.remove(tabIndex);
		fileToPublishTextFields.remove(tabIndex);
        publishMethodComboBoxes.remove(tabIndex);
        jobFileLocations.remove(tabIndex);
        jobTabTitleLabels.remove(tabIndex);
		jobTabsPane.remove(tabIndex);
		jobTabCommandTextFields.remove(tabIndex);
        
		assert(jobTabsValid());
	}
    
	/**
	 * Ensures consistency of fields within job tabs
	 * @return true if no issues were found, false otherwise
	 */
	private boolean jobTabsValid() {
		int numTabs = jobTabsPane.getTabCount();
		if(datasetIDTextFields.size() != numTabs)
			return false;
        if(fileToPublishTextFields.size() != numTabs)
        	return false;
        if(datasetIDTextFields.size() != numTabs)
        	return false;
        if(jobFileLocations.size() != numTabs)
    		return false;
        if(jobTabTitleLabels.size() != numTabs)
    		return false;
        if(jobTabCommandTextFields.size() != numTabs)
        	return false;
        return true;
	}
	
    /**
     * Saves user authentication data input into form
     */
    private void saveAuthenticationInfoFromForm() {
    	// TODO make this more secure...
    	userPrefs.saveDomain(domainTextField.getText()); 
    	userPrefs.saveUsername(usernameTextField.getText());
		String password = new String(passwordField.getPassword());
    	userPrefs.savePassword(password);
    	userPrefs.saveAPIKey(apiKeyTextField.getText());
    }
    
    /**
     * Loads user authentication data from userPrefs
     */
    private void loadAuthenticationInfoIntoForm() {
    	domainTextField.setText(userPrefs.getDomain());
		usernameTextField.setText(userPrefs.getUsername());
		passwordField.setText(userPrefs.getPassword());
		apiKeyTextField.setText(userPrefs.getAPIKey());
    }
    
}
