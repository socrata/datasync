package com.socrata.datasync.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.socrata.datasync.*;
import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.job.Job;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.job.MetadataJob;
import com.socrata.datasync.job.PortJob;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SimpleIntegrationWizard {
	/**
	 * @author Adrian Laurenzi
	 *
	 * GUI interface to DataSync
	 */
	private static final String TITLE = "Socrata DataSync " + VersionProvider.getThisVersion();
    private static final String LOGO_FILE_PATH = "/datasync_logo.png";
	private static final String LOADING_SPINNER_FILE_PATH = "/loading_spinner.gif";
	private static final int FRAME_WIDTH = 800;
	private static final int FRAME_HEIGHT = 550;
	private static final Dimension JOB_PANEL_DIMENSION = new Dimension(780, 350);
    private static final Dimension BUTTON_PANEL_DIMENSION = new Dimension(780, 40);
    private static final int SSL_PORT_TEXTFIELD_HEIGHT = 26;
	private static final int DEFAULT_TEXTFIELD_COLS = 25;
	private static final Dimension AUTH_DETAILS_DIMENSION = new Dimension(465, 100);
	private static final int PREFERENCES_FRAME_WIDTH = 475;
	private static final int PREFERENCES_FRAME_HEIGHT = 675;

	private static UserPreferencesJava userPrefs;

    // TODO remove these declarations from this file (duplicates...)
	private static final String STANDARD_JOB_FILE_EXTENSION = "sij";
    private static final String PORT_JOB_FILE_EXTENSION = "spj";
    private static final String METADATA_JOB_FILE_EXTENSION = "smj";

    // help icon balloon tip text
    private static final String FILE_CHUNKING_THRESHOLD_TIP_TEXT = "<html><body style='width: 300px'>If using the upsert, append, or " +
            "delete methods (over HTTP) and the CSV/TSV file to be published is larger than this value (in megabytes), " +
            "the file is automatically split up and published in chunks (because it is problematic to publish large files all at once). " +
            "Usually chunking is necessary when a file is larger than about 64 MB.</body></html>";
    private static final String CHUNK_SIZE_THRESHOLD_TIP_TEXT = "<html><body style='width: 300px'>The number of rows to publish in each chunk " +
            "(in cases where filesize exceeds above filesize threshold). Higher values usually means faster upload time but setting the value too " +
            "high could crash the program, depending on your computer's memory limits.</body></html>";
    private static final String DOMAIN_TIP_TEXT = "The domain of the Socrata data site you wish to publish data to (e.g. https://explore.data.gov/)";
    private static final String USERNAME_TIP_TEXT = "Socrata account username (account must have Publisher or Administrator permissions)";
    private static final String PASSWORD_TIP_TEXT = "Socrata account password";
    private static final String APP_TOKEN_TIP_TEXT = "You can create an app token free at http://dev.socrata.com/register";
    private static final String RUN_JOB_NOW_TIP_TEXT = "<html><body style='width: 300px'>" +
            "To view detailed logging information run the job by copying the" +
            " 'Command to execute with scheduler' and running it in your Terminal/Command Prompt (instead of clicking 'Run Job Now' button)</body></html>";

    private static final String QUICK_START_GUIDE = "http://socrata.github.io/datasync/guides/quick-start.html";
    private static final String PORTING_GUIDE = "http://socrata.github.io/datasync/guides/setup-port-job.html";
    private static final String HEADLESS_GUIDE_URL = "http://socrata.github.io/datasync/guides/setup-standard-job-headless.html";
    private static final String CONTROL_GUIDE_URL = "http://socrata.github.io/datasync/resources/control-config.html";
    private static final String SCHEDULING_GUIDE_URL = "http://socrata.github.io/datasync/resources/schedule-job.html";
    private static final String FAQ_URL = "http://socrata.github.io/datasync/resources/faq-common-problems.html";

    private JTextField domainTextField, usernameTextField, apiKeyTextField;
	private JPasswordField passwordField;
    private JTextField filesizeChunkingCutoffTextField, numRowsPerChunkTextField;
	private JTextField logDatasetIDTextField, adminEmailTextField;
	private JTextField outgoingMailServerTextField, smtpPortTextField, sslPortTextField, smtpUsernameTextField;
    private JTextField proxyHostTextField, proxyPortTextField, proxyUsernameTextField, proxyPasswordTextField;
    private JPasswordField smtpPasswordField;
	private JCheckBox useSSLCheckBox;
	private JCheckBox emailUponErrorCheckBox;

	/**
	 * Stores a list of open JobTabs. Each JobTab object contains
     * the UI content of a single job tab. The indices of the
     * tabs within jobTabsPane correspond to the indices of the
     * JobTab objects (holding the UI for each tab) within this
     * list.
     *
	 * *IMPORTANT*: only modify this list in the 'addJobTab' and
     *              'closeJobTab' methods
	 */
	private List<JobTab> jobTabs;

	private JTabbedPane jobTabsPane;
	private JFrame frame;
	private JFrame prefsFrame;
    private JPanel loadingNoticePanel;
    private JButton runJobNowButton;

    /*
	 * Constructs the GUI and displays it on the screen.
	 */
	public SimpleIntegrationWizard() {
		// load user preferences (saved locally)
		userPrefs = new UserPreferencesJava();

		// Build GUI
		frame = new JFrame(TITLE);
		frame.setSize(FRAME_WIDTH, FRAME_HEIGHT);

        jobTabs = new ArrayList<>();
		// save tabs on close
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
		generatePreferencesFrame();

        frame.add(mainPanel);

        frame.pack();
        // centers the window
     	frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		// Alert user if new version is available
		try {
			checkVersion();
		} catch (Exception e) {
			// do nothing upon failure
		}
	}

    private void generatePreferencesFrame() {
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
    }

    /**
     * Queries github for the most recent release. If query is successful
     * and the major version of the user's datasync is outdated, alerts
	 * that a new version is available
	 *
	 * @throws URISyntaxException
	 */
	private void checkVersion() throws URISyntaxException {
		if(VersionProvider.isLatestMajorVersion() == VersionProvider.VersionStatus.NOT_LATEST) {
            String currentVersion = VersionProvider.getLatestVersion();
            if (currentVersion != null) {
                Object[] options = {"Download Now", "No Thanks"};
                int n = JOptionPane.showOptionDialog(frame,
                        "A new version of DataSync is available (version " + currentVersion + ").\n" +
                        "Do you want to download it now?\n",
                        "Alert: New Version Available",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]
                );
                if (n == JOptionPane.YES_OPTION) {
                    URI currentVersionDownloadURI = new URI(VersionProvider.getDownloadUrlForLatestVersion());
                    if (currentVersionDownloadURI != null)
                        Utils.openWebpage(currentVersionDownloadURI);
                }
            }
        }
	}

	private void addJobTab(Job job) throws IllegalArgumentException {
        JobTab newJobTab;
        if(job.getClass().equals(IntegrationJob.class)) {
            newJobTab = new IntegrationJobTab((IntegrationJob) job, frame);
        } else if(job.getClass().equals(PortJob.class)) {
            newJobTab = new PortJobTab((PortJob) job, frame);
        } else if(job.getClass().equals(MetadataJob.class)) {
        	newJobTab = new MetadataJobTab((MetadataJob) job, frame);
        } else {
            throw new IllegalArgumentException("Given job is invalid: unrecognized class '" + job.getClass() + "'");
        }
        JPanel newJobPanel = newJobTab.getTabPanel();

		// Build the tab with close button
	    FlowLayout tabLayout = new FlowLayout(FlowLayout.CENTER, 5, 0);
	    JPanel tabPanel = new JPanel(tabLayout);
	    tabPanel.setOpaque(false);

	    // Create a JButton for the close tab button
	    JButton closeTabButton = new AwesomeButton("cross41");
	    closeTabButton.setBorder(null);
	    closeTabButton.setFocusable(false);

	    tabPanel.add(newJobTab.getJobTabTitleLabel());
	    tabPanel.add(closeTabButton);
	    // Add a thin border to keep the image below the top edge of the tab when the tab is selected
	    tabPanel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));

		// Put tab with close button into tabbed pane
	    //TODO: BW: Possibly implement way to keep other tabs from being scrollable?
	    JScrollPane newJobScrollPanel = new JScrollPane(newJobPanel);
	    newJobScrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        jobTabsPane.addTab(null, newJobScrollPanel);
	    int pos = jobTabsPane.indexOfComponent(newJobScrollPanel);

	    // Now assign the component for the tab
	    jobTabsPane.setTabComponentAt(pos, tabPanel);

	    closeTabButton.addActionListener(new CloseJobFromTabListener());

        jobTabs.add(newJobTab);
	    assert(jobTabsValid());
	}

    private void closeJobTab(int tabIndex) throws IllegalArgumentException {
        if(tabIndex >= jobTabsPane.getTabCount()) {
            throw new IllegalArgumentException("Tab index is invalid");
        }
        jobTabsPane.remove(tabIndex);
        jobTabs.remove(tabIndex);
        assert(jobTabsValid());
    }

	private class RunJobNowListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			saveAuthenticationInfoFromForm();

			// run integration job with data from form
	        int selectedJobTabIndex = jobTabsPane.getSelectedIndex();
            JobTab selectedJobTab = jobTabs.get(selectedJobTabIndex);

            SwingWorker jobWorker = new RunJobWorker(selectedJobTab);
            jobWorker.execute();
		}
	}

    private class RunJobWorker extends SwingWorker<Void, String> {
        private JobTab jobTabToRun;
        private JobStatus jobStatus;

        public RunJobWorker(JobTab jobTabToRun){
            loadingNoticePanel.setVisible(true);
            runJobNowButton.setEnabled(false);
            this.jobTabToRun = jobTabToRun;
        }

        @Override
        protected Void doInBackground() {
            try {
                jobStatus = jobTabToRun.runJobNow();
            } catch (OutOfMemoryError err) {
                jobStatus = JobStatus.PUBLISH_ERROR;
                jobStatus.setMessage("Error: ran out of memory " +
                        "(try decreasing the chunking size and/or threshold by going to Edit -> Preferences)");
            }
            return null;
        }

        //Executed on the Event Dispatch Thread after the doInBackground method is finished
        @Override
        protected void done() {
            loadingNoticePanel.setVisible(false);
            runJobNowButton.setEnabled(true);

            // show popup with returned status
            if(jobStatus.isError()) {
                JOptionPane.showMessageDialog(frame, "Job completed with errors: " + jobStatus.getMessage());
            } else {
                if (jobTabToRun.getClass().equals(PortJobTab.class)) {
                    PortJobTab selectedPortJobTab = (PortJobTab) jobTabToRun;
                    Object[] options = {"Yes", "No"};
                    int n = JOptionPane.showOptionDialog(frame,
                            "Port job completed successfully. Would you like to open the destination dataset?\n",
                            "Port Job Successful",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null, options, options[0]);
                    if (n == JOptionPane.YES_OPTION) {
                        Utils.openWebpage(selectedPortJobTab.getURIToSinkDataset());
                    }
                } else {
                    JOptionPane.showMessageDialog(frame, "Job completed successfully. " + jobStatus.getMessage());
                }
            }
        }
    }

	private class SaveJobListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			saveAuthenticationInfoFromForm();
            int selectedJobTabIndex = jobTabsPane.getSelectedIndex();
            JobTab selectedJobTab = jobTabs.get(selectedJobTabIndex);
            selectedJobTab.saveJob();
		}
	}

	private File openToDirectory = new File(".");
    private class OpenJobListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			JFileChooser savedJobFileChooser = new JFileChooser();
            savedJobFileChooser.setCurrentDirectory(openToDirectory);
            String fileExtensionsAllowed = "*." + STANDARD_JOB_FILE_EXTENSION + ", *." + PORT_JOB_FILE_EXTENSION + ", *." + METADATA_JOB_FILE_EXTENSION;
        	FileNameExtensionFilter filter = new FileNameExtensionFilter(
                    "Socrata Job File (" + fileExtensionsAllowed + ")",
                    STANDARD_JOB_FILE_EXTENSION, PORT_JOB_FILE_EXTENSION, METADATA_JOB_FILE_EXTENSION);
        	savedJobFileChooser.setFileFilter(filter);
        	int returnVal = savedJobFileChooser.showOpenDialog(frame);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File openedFile = savedJobFileChooser.getSelectedFile();
				// Ensure file exists
				if(openedFile.exists()) {
					openToDirectory = savedJobFileChooser.getCurrentDirectory();
					// ensure this job is not already open
					String openedFileLocation = openedFile.getAbsolutePath();
                    int indexOfAlreadyOpenFile = -1;
                    for(int curTabIndex = 0; curTabIndex < jobTabs.size(); curTabIndex++) {
                        String curTabJobFileLocation = jobTabs.get(curTabIndex).getJobFileLocation();
                        if(curTabJobFileLocation.equals(openedFileLocation)) {
                            indexOfAlreadyOpenFile = curTabIndex;
                            break;
                        }
                    }
					if(indexOfAlreadyOpenFile == -1) {
                        try {
                            int i = openedFileLocation.lastIndexOf('.');
                            if (i > 0) {
                                String openedFileExtension = openedFileLocation.substring(i+1);
                                if(openedFileExtension.equals(STANDARD_JOB_FILE_EXTENSION)) {
                                    addJobTab(new IntegrationJob(openedFileLocation));
                                } else if(openedFileExtension.equals(PORT_JOB_FILE_EXTENSION)) {
                                    addJobTab(new PortJob(openedFileLocation));
                                } else if (openedFileExtension.equals(METADATA_JOB_FILE_EXTENSION)) {
                                	addJobTab(new MetadataJob(openedFileLocation));
                                } else {
                                    throw new Exception("unrecognized file extension (" + openedFileExtension + ")");
                                }
                            }
                            // Switch to opened file's tab
                            jobTabsPane.setSelectedIndex(jobTabsPane.getTabCount() - 1);
                        } catch(IntegrationJob.ControlDisagreementException ex) {
                            JOptionPane.showMessageDialog(frame, "Warning: \n" + ex.getMessage() + " found in \n'" +
                                openedFileLocation + "'. \nLoading job, but please confirm the contents of your control file are accurate.");
                            try {
                                addJobTab(new IntegrationJob(openedFileLocation, true));
                                jobTabsPane.setSelectedIndex(jobTabsPane.getTabCount() - 1);
                            } catch(Exception e2) {
                                JOptionPane.showMessageDialog(frame, "Error opening " + openedFileLocation + ": " + e2.toString());
                            }
                        } catch(Exception e2) {
                            JOptionPane.showMessageDialog(frame, "Error opening " + openedFileLocation + ": " + e2.toString());
                        }
	                } else {
	                	// file already open, select that tab
	                	jobTabsPane.setSelectedIndex(indexOfAlreadyOpenFile);
	                }
				}
            }
		}
	}

    private class AuthenticationDetailsFocusListener implements FocusListener {
        @Override
        public void focusGained(FocusEvent e) { }
        @Override
        public void focusLost(FocusEvent e) {
            saveAuthenticationInfoFromForm();
        }
    }

	private class NewStandardJobListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
            addJobTab(getNewIntegrationJob());
            jobTabsPane.setSelectedIndex(jobTabsPane.getTabCount() - 1);
		}
	}

    private IntegrationJob getNewIntegrationJob() {
        IntegrationJob newJob = new IntegrationJob();
        // set publishViaDi2Http to true as default (ONLY for GUI mode)
        newJob.setPublishViaDi2Http(true);
        return newJob;
    }

    private class NewPortJobListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            addJobTab(new PortJob());
            jobTabsPane.setSelectedIndex(jobTabsPane.getTabCount() - 1);
        }
    }

    private class NewMetadataJobListener implements ActionListener {
    	public void actionPerformed(ActionEvent e) {
    		addJobTab(new MetadataJob());
    		jobTabsPane.setSelectedIndex(jobTabsPane.getTabCount() - 1);
    	}
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
		menuBar.add(fileMenu);

        JMenu newJobMenu = new JMenu("New...");
        JMenuItem newStandardJobItem = new JMenuItem("Standard Job");
        newJobMenu.add(newStandardJobItem);
        JMenuItem newPortJobItem = new JMenuItem("Port Job");
        newJobMenu.add(newPortJobItem);
        JMenuItem newMetadataJobItem = new JMenuItem("Metadata Job (beta)");
        newJobMenu.add(newMetadataJobItem);
		fileMenu.add(newJobMenu);

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
      JMenuItem prefsItem = new JMenuItem("Preferences");
      fileMenu.add(prefsItem);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);
        JMenuItem gettingStartedGuideItem = new JMenuItem("Quick start guide");
        JMenuItem portingGuideItem = new JMenuItem("Port job guide");
        JMenuItem headlessDocumentationItem = new JMenuItem("Running in headless mode");
        JMenuItem controlDocumentationItem = new JMenuItem("Control file configuration");
        JMenuItem schedulingItem = new JMenuItem("Scheduling a job");
        JMenuItem faqItem = new JMenuItem("FAQ");
        helpMenu.add(gettingStartedGuideItem);
        helpMenu.add(portingGuideItem);
        helpMenu.add(headlessDocumentationItem);
        helpMenu.add(controlDocumentationItem);
        helpMenu.add(schedulingItem);
        helpMenu.add(faqItem);

        newStandardJobItem.addActionListener(new NewStandardJobListener());
        newPortJobItem.addActionListener(new NewPortJobListener());
        newMetadataJobItem.addActionListener(new NewMetadataJobListener());
		openJobItem.addActionListener(new OpenJobListener());
		saveJobItem.addActionListener(new SaveJobListener());
		runJobItem.addActionListener(new RunJobNowListener());
		prefsItem.addActionListener(new OpenPreferencesListener());
        gettingStartedGuideItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Utils.openWebpage(new URI(QUICK_START_GUIDE));
                } catch (URISyntaxException e1) { e1.printStackTrace(); }
            }
        });
        portingGuideItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Utils.openWebpage(new URI(PORTING_GUIDE));
                } catch (URISyntaxException e1) { e1.printStackTrace(); }
            }
        });
        headlessDocumentationItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Utils.openWebpage(new URI(HEADLESS_GUIDE_URL));
                } catch (URISyntaxException e1) { e1.printStackTrace(); }
            }
        });
        controlDocumentationItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Utils.openWebpage(new URI(CONTROL_GUIDE_URL));
                } catch (URISyntaxException e1) { e1.printStackTrace(); }
            }
        });
        schedulingItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Utils.openWebpage(new URI(SCHEDULING_GUIDE_URL));
                } catch (URISyntaxException e1) { e1.printStackTrace(); }
            }
        });
        faqItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    Utils.openWebpage(new URI(FAQ_URL));
                } catch (URISyntaxException e1) { e1.printStackTrace(); }
            }
        });

		return menuBar;
	}

	private JPanel generateMainPanel() {
		JPanel mainContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
		mainContainer.setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));

		// Build empty job tabbed pane
		JPanel jobTabsContainer = new JPanel(new GridLayout(1, 1));
		jobTabsContainer.setPreferredSize(JOB_PANEL_DIMENSION);
		jobTabsPane = new JTabbedPane();
		jobTabsContainer.add(jobTabsPane);
        jobTabsPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        JPanel jobButtonContainer = new JPanel(new GridLayout(1,2));
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        runJobNowButton = new JButton("Run Job Now");
        runJobNowButton.addActionListener(new RunJobNowListener());
        leftButtonPanel.add(runJobNowButton);
        leftButtonPanel.add(UIUtility.generateHelpBubble(RUN_JOB_NOW_TIP_TEXT));

        generateLoadingNotice();
        leftButtonPanel.add(loadingNoticePanel);

        JPanel rightButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton saveJobButton = new JButton("Save Job");
        saveJobButton.addActionListener(new SaveJobListener());
        rightButtonPanel.add(saveJobButton);
        jobButtonContainer.add(leftButtonPanel);
        jobButtonContainer.add(rightButtonPanel);

        jobButtonContainer.setPreferredSize(BUTTON_PANEL_DIMENSION);
        mainContainer.add(jobTabsContainer);
        mainContainer.add(jobButtonContainer);

        JPanel authenticationDetailsPanel = generateAuthenticationDetailsFormPanel();
		mainContainer.add(authenticationDetailsPanel);

        URL logoImageURL = getClass().getResource(LOGO_FILE_PATH);
        if(logoImageURL != null) {
            JLabel logoLabel = new JLabel(new ImageIcon(logoImageURL));
            Border paddingBorder = BorderFactory.createEmptyBorder(15,15,15,15);
            logoLabel.setBorder(paddingBorder);
            mainContainer.add(logoLabel);
        }

        // TODO populate job tabs w/ previously opened tabs or [if none] a new job tab

        addJobTab(getNewIntegrationJob());
        return mainContainer;
	}

    private void generateLoadingNotice() {
        loadingNoticePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        URL spinnerImageURL = getClass().getResource(LOADING_SPINNER_FILE_PATH);
        if(spinnerImageURL != null) {
            JLabel loadingImageLabel = new JLabel(new ImageIcon(spinnerImageURL));
            loadingNoticePanel.add(loadingImageLabel);
        }
        JLabel loadingTextLabel = new JLabel(" Job is in progress...");
        loadingNoticePanel.add(loadingTextLabel);
        loadingNoticePanel.setVisible(false);
    }

    private JPanel generatePreferencesPanel() {
		JPanel prefsPanel = new JPanel(new GridLayout(0,2));

        // File chunking settings
        JLabel fileChunkingSettingsLabel = new JLabel(" File Chunking Settings");
        Font boldFont = new Font(fileChunkingSettingsLabel.getFont().getFontName(),
                Font.BOLD, fileChunkingSettingsLabel.getFont().getSize());
        fileChunkingSettingsLabel.setFont(boldFont);
        prefsPanel.add(fileChunkingSettingsLabel);
        prefsPanel.add(new JLabel(""));

        prefsPanel.add(
                UIUtility.generateLabelWithHelpBubble(" Chunking filesize threshold", FILE_CHUNKING_THRESHOLD_TIP_TEXT));
        JPanel filesizeChuckingContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filesizeChunkingCutoffTextField = new JTextField();
        filesizeChunkingCutoffTextField.setPreferredSize(new Dimension(
                140, 20));
        filesizeChuckingContainer.add(filesizeChunkingCutoffTextField);
        filesizeChuckingContainer.add(new JLabel(" MB"));
        prefsPanel.add(filesizeChuckingContainer);

        prefsPanel.add(
                UIUtility.generateLabelWithHelpBubble(" Chunk size", CHUNK_SIZE_THRESHOLD_TIP_TEXT));
        JPanel chunkSizeContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        numRowsPerChunkTextField = new JTextField();
        numRowsPerChunkTextField.setPreferredSize(new Dimension(
                140, 20));
        chunkSizeContainer.add(numRowsPerChunkTextField);
        chunkSizeContainer.add(new JLabel(" rows"));
        prefsPanel.add(chunkSizeContainer);

        // Logging and auto-email settings
        JLabel loggingAutoEmailSettingsLabel = new JLabel(" Logging and Auto-Email Settings");
        loggingAutoEmailSettingsLabel.setFont(boldFont);
        prefsPanel.add(loggingAutoEmailSettingsLabel);
        prefsPanel.add(new JLabel(""));

		prefsPanel.add(new JLabel(" Log Dataset ID (e.g., n38h-y5wp)"));
		logDatasetIDTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		prefsPanel.add(logDatasetIDTextField);

		prefsPanel.add(new JLabel(" Admin Email"));
		adminEmailTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		prefsPanel.add(adminEmailTextField);

		emailUponErrorCheckBox = new JCheckBox(" Auto-email admin upon error");
		prefsPanel.add(emailUponErrorCheckBox);
		prefsPanel.add(new JLabel("*must fill in SMTP Settings below"));

        // Auto-email SMTP settings
		JLabel smtpSettingsLabel = new JLabel(" SMTP Settings");
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
				50, SSL_PORT_TEXTFIELD_HEIGHT));
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


        // Proxy settings
        JLabel proxySettingsLabel = new JLabel(" Proxy Settings");
        proxySettingsLabel.setFont(boldFont);
        prefsPanel.add(proxySettingsLabel);
        prefsPanel.add(new JLabel(""));

        prefsPanel.add(new JLabel(" Proxy Host"));
        proxyHostTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
        prefsPanel.add(proxyHostTextField);
        prefsPanel.add(new JLabel(" Proxy Port"));
        proxyPortTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
        prefsPanel.add(proxyPortTextField);
        prefsPanel.add(new JLabel(" Proxy Username"));
        proxyUsernameTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
        prefsPanel.add(proxyUsernameTextField);
        prefsPanel.add(new JLabel(" Proxy Password"));
        proxyPasswordTextField = new JPasswordField(DEFAULT_TEXTFIELD_COLS);
        prefsPanel.add(proxyPasswordTextField);

        JPanel testSMTPSettingsContainer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton testSMTPSettingsButton = new JButton("Test SMTP Settings");
        testSMTPSettingsButton.addActionListener(new TestSMTPSettingsListener());
        testSMTPSettingsContainer.add(testSMTPSettingsButton);
        prefsPanel.add(testSMTPSettingsContainer);

		JPanel prefsButtonContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton cancelPrefsButton = new JButton("Cancel");
		cancelPrefsButton.addActionListener(new CancelPreferencesListener());
		prefsButtonContainer.add(cancelPrefsButton);
		JButton savePrefsButton = new JButton("Save");
		savePrefsButton.addActionListener(new SavePreferencesListener());
		prefsButtonContainer.add(savePrefsButton);
		prefsPanel.add(prefsButtonContainer);

		loadPreferencesIntoForm();

		return prefsPanel;
	}

	private void loadPreferencesIntoForm() {
        filesizeChunkingCutoffTextField.setText(userPrefs.getFilesizeChunkingCutoffMB());
        numRowsPerChunkTextField.setText(userPrefs.getNumRowsPerChunk());

		adminEmailTextField.setText(userPrefs.getAdminEmail());
		logDatasetIDTextField.setText(userPrefs.getLogDatasetID());
		emailUponErrorCheckBox.setSelected(userPrefs.emailUponError());

		outgoingMailServerTextField.setText(userPrefs.getOutgoingMailServer());
		smtpPortTextField.setText(userPrefs.getSmtpPort());
        String sslPort = userPrefs.getSslPort();
		sslPortTextField.setText(sslPort);
		if(sslPort.equals("")) {
			useSSLCheckBox.setSelected(false);
		} else {
			useSSLCheckBox.setSelected(true);
		}
		smtpUsernameTextField.setText(userPrefs.getSmtpUsername());
		smtpPasswordField.setText(userPrefs.getSmtpPassword());

        proxyHostTextField.setText(userPrefs.getProxyHost());
        proxyPortTextField.setText(userPrefs.getProxyPort());
        proxyUsernameTextField.setText(userPrefs.getProxyUsername());
        proxyPasswordTextField.setText(userPrefs.getProxyPassword());

    }

	private void savePreferences() {
        try {
            userPrefs.saveFilesizeChunkingCutoffMB(
                    Integer.parseInt(filesizeChunkingCutoffTextField.getText()));
        } catch(NumberFormatException e) {
            JOptionPane.showMessageDialog(prefsFrame, "Invalid chunking filesize threshold: must be an integer");
        }
        try {
            userPrefs.saveNumRowsPerChunk(
                    Integer.parseInt(numRowsPerChunkTextField.getText()));
        } catch(NumberFormatException e) {
            JOptionPane.showMessageDialog(prefsFrame, "Invalid chunk size: must be an integer");
        }

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

        userPrefs.saveProxyHost(proxyHostTextField.getText());
        userPrefs.saveProxyPort(proxyPortTextField.getText());
        userPrefs.saveProxyUsername(proxyUsernameTextField.getText());
        userPrefs.saveProxyPassword(proxyPasswordTextField.getText());

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

        authenticationDetailsPanel.add(
                UIUtility.generateLabelWithHelpBubble("Domain", DOMAIN_TIP_TEXT));
		domainTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		authenticationDetailsPanel.add(domainTextField);
		authenticationDetailsPanel.add(
                UIUtility.generateLabelWithHelpBubble("Username", USERNAME_TIP_TEXT));
		usernameTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		authenticationDetailsPanel.add(usernameTextField);
		authenticationDetailsPanel.add(
                UIUtility.generateLabelWithHelpBubble("Password", PASSWORD_TIP_TEXT));
		passwordField = new JPasswordField(DEFAULT_TEXTFIELD_COLS);
		authenticationDetailsPanel.add(passwordField);
		authenticationDetailsPanel.add(
                UIUtility.generateLabelWithHelpBubble("App Token", APP_TOKEN_TIP_TEXT));
		apiKeyTextField = new JTextField(DEFAULT_TEXTFIELD_COLS);
		authenticationDetailsPanel.add(apiKeyTextField);
		authenticationDetailsPanel.setPreferredSize(AUTH_DETAILS_DIMENSION);

        AuthenticationDetailsFocusListener focusListener = new AuthenticationDetailsFocusListener();
        domainTextField.addFocusListener(focusListener);
        usernameTextField.addFocusListener(focusListener);
        passwordField.addFocusListener(focusListener);
        apiKeyTextField.addFocusListener(focusListener);

		return authenticationDetailsPanel;
	}

	/**
	 * Ensures consistency of fields within job tabs
	 * @return true if no issues were found, false otherwise
	 */
	private boolean jobTabsValid() {
        return (jobTabsPane.getTabCount() == jobTabs.size());
	}

    /**
     * Saves user authentication data input into form
     */
    private void saveAuthenticationInfoFromForm() {
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
