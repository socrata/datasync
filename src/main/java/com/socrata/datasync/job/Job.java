package com.socrata.datasync.job;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class Job {

    String pathToSavedJobFile = "";
    String defaultJobName = "Unsaved Job";
    static AtomicInteger jobCounter = new AtomicInteger(0);
    int jobNum = jobCounter.getAndIncrement();

   public abstract boolean validateArgs(CommandLine cmd);
    public abstract void configure(CommandLine cmd);
    public abstract JobStatus run() throws IOException;

    public void writeToFile(String filepath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(filepath), this);
    }

    public void setPathToSavedFile(String newPath) {
        pathToSavedJobFile = newPath;
    }

    public String getPathToSavedFile() {
        return pathToSavedJobFile;
    }

    // TODO: why are we creating the file, just to throw it away?
    public String getJobFilename() {
        if(StringUtils.isBlank(pathToSavedJobFile)) {
            return defaultJobName + " (" + jobNum + ")";
        }
        return new File(pathToSavedJobFile).getName();
    }
}
