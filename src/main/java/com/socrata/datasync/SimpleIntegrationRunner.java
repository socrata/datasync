package com.socrata.datasync;

import com.socrata.datasync.job.IntegrationJob;

public class SimpleIntegrationRunner {
	/**
	 * @author Adrian Laurenzi
	 * 
	 * A command-line interface to DataSync
	 */
	
	public SimpleIntegrationRunner(String jobFileToRun) {
		IntegrationJob job = new IntegrationJob(jobFileToRun);
		JobStatus status = job.run();
		System.out.println(status.getMessage());
	}

}
