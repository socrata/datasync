package com.socrata.datasync;

import com.socrata.datasync.job.IntegrationJob;
import com.socrata.datasync.job.Job;
import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.job.PortJob;
import com.socrata.datasync.job.GISJob;
import com.socrata.datasync.job.GISJob.ControlDisagreementException;
import com.socrata.datasync.ui.GISJobTab;
import com.socrata.datasync.job.MetadataJob;
import com.socrata.datasync.ui.MetadataJobTab;
import com.socrata.datasync.ui.PortJobTab;

import java.io.File;
import java.io.IOException;

public class SimpleIntegrationRunner {
	/**
	 * @author Adrian Laurenzi
	 *
	 * A command-line interface to DataSync
	 * @throws ControlDisagreementException 
	 */

    public SimpleIntegrationRunner(String jobFileToRun) throws ControlDisagreementException {
        File jobFile = new File(jobFileToRun);
        if(jobFile.exists()) {
            try {
            	Job job;
            	//TODO BW: Follow how port jobs are run from command line?
            	if (jobFileToRun.endsWith(MetadataJobTab.JOB_FILE_EXTENSION)) {
            		job = new MetadataJob(jobFileToRun);
            	} else if(jobFileToRun.endsWith(GISJobTab.JOB_FILE_EXTENSION)) {
            		job = new GISJob(jobFileToRun);
            	} else if(jobFileToRun.endsWith(PortJobTab.JOB_FILE_EXTENSION)) {
            		job = new PortJob(jobFileToRun);
            	} else {
            		job = new IntegrationJob(jobFileToRun);
            	}
                JobStatus status = job.run();
                if(status.isError()) {
                    System.err.print("Job completed with errors: ");
                    System.err.println(status.getMessage());
                    System.exit(1);
                } else {
                    // job ran successfully!
                    System.out.println("Job completed successfully");
                    if(job.getClass() == PortJob.class) {
                        System.out.println(status.getMessage() + ". " +
                                           "Your newly created dataset is at:\n" +
                                           ((PortJob)job).getSinkSiteDomain() + "/d/" + ((PortJob)job).getSinkSetID());
                    }
                    System.out.println(status.getMessage());
                }
            } catch (IOException | IntegrationJob.ControlDisagreementException e) {
                System.err.println("Error running " + jobFileToRun + ":\n " + e.toString());
                System.exit(1);
            }
        } else {
            // TODO record error in DataSync log?
            System.err.println("Error running " + jobFileToRun + ": job file does not exist.");
            System.exit(1);
        }
	}

    public SimpleIntegrationRunner(Job job) {
        JobStatus status;
        try {
            status = job.run();
            if(status.isError()) {
                System.err.print("Job completed with errors: ");
                System.err.println(status.getMessage());
                System.exit(1);
            } else {
                System.out.println("Job completed successfully");
                if(job.getClass() == PortJob.class) {
                    System.out.println(status.getMessage() + ". " +
                        "Your newly created dataset is at:\n" +
                        ((PortJob)job).getSinkSiteDomain() + "/d/" + ((PortJob)job).getSinkSetID());
                    }
                System.out.println(status.getMessage());
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}

