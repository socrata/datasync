package com.socrata.datasync;

import com.socrata.datasync.job.IntegrationJob;

import java.io.IOException;

public class SimpleIntegrationRunner {
	/**
	 * @author Adrian Laurenzi
	 * 
	 * A command-line interface to DataSync
	 */
	
	public SimpleIntegrationRunner(String jobFileToRun) {
        try {
            IntegrationJob job = new IntegrationJob(jobFileToRun);
            JobStatus status = job.run();
            System.out.println(status.getMessage());
        } catch (IOException e) {
            System.out.println("Error reading " + jobFileToRun + ": " + e.toString());
        }

	}

}
