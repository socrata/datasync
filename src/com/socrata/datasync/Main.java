package com.socrata.datasync;

public class Main {
	/**
	 * @author Adrian Laurenzi
	 *
	 * Loads an instance of the SimpleIntegrationWizard in command line 
	 * mode or as a GUI.
	 */
    public static void main(String[] args)
    {
    	if(args.length > 0) {
    		// Run in command line mode (usually for scheduler calls)
    		String jobFileToRun = args[0];
			new SimpleIntegrationRunner(jobFileToRun);
		} else {
			// Open GUI (default)
			new SimpleIntegrationWizard();
		}
    }

}