package com.socrata.datasync.job;

import com.socrata.datasync.Utils;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;

public abstract class Job {

    String pathToSavedJobFile = "";

    public abstract String getDefaultJobName();
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

    public String getJobFilename() {
        return StringUtils.isBlank(pathToSavedJobFile) ? getDefaultJobName() : Utils.getFilename(pathToSavedJobFile);
    }


}
