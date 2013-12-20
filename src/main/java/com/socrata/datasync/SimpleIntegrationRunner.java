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
                if(status.isError()) {
                    System.err.println(status.getMessage());
                    System.exit(1);
                } else {
                    System.out.println(status.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error running " + jobFileToRun + ": " + e.toString());
                System.exit(1);
            }
        } else {
            // TODO record error in DataSync log
            System.err.println("Error running " + jobFileToRun + ": job file does not exist.");
            System.exit(1);
        }
	}

    public SimpleIntegrationRunner(IntegrationJob job) {
        JobStatus status = job.run();
        if(status.isError()) {
            System.err.println(status.getMessage());
            System.exit(1);
        } else {
            System.out.println(status.getMessage());
        }
    }
}
