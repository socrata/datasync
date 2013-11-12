package com.socrata.datasync;

import com.socrata.datasync.job.IntegrationJob;

import java.io.File;
import java.io.IOException;

public class SimpleIntegrationRunner {
	/**
	 * @author Adrian Laurenzi
	 * 
	 * A command-line interface to DataSync
	 */

    public SimpleIntegrationRunner(String jobFileToRun) {
        File jobFile = new File(jobFileToRun);
        if(jobFile.exists()) {
            try {
                IntegrationJob job = new IntegrationJob(jobFileToRun);
                JobStatus status = job.run();
                System.out.println(status.getMessage());
            } catch (IOException e) {
                System.out.println("Error running " + jobFileToRun + ": " + e.toString());
            }
        } else {
            // TODO record error in DataSync log
            System.out.println("Error running " + jobFileToRun + ": job file does not exist.");
        }
	}

    public SimpleIntegrationRunner(IntegrationJob job) {
        JobStatus status = job.run();
        System.out.println(status.getMessage());
    }
}
