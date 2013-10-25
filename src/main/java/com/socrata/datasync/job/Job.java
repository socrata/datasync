package com.socrata.datasync.job;

import com.socrata.datasync.JobStatus;

import java.io.IOException;

/**
 * Author: Adrian Laurenzi
 * Date: 9/18/13
 */
public interface Job {

    public JobStatus run();

    public void writeToFile(String filepath) throws IOException;

    public void setPathToSavedFile(String newPath);

    public String getPathToSavedFile();

    public String getJobFilename();
}
