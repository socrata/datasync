package com.socrata.datasync;

/**
 * Author: Adrian Laurenzi
 * Date: 9/18/13
 */
public interface Job {

    public JobStatus run();

    public void writeToFile(String filepath);

    public void setPathToSavedFile(String newPath);

    public String getPathToSavedFile();

    public String getJobFilename();
}
