package com.socrata.datasync.deltaimporter2;

import com.socrata.datasync.config.controlfile.ControlFile;
import com.socrata.datasync.config.controlfile.PortControlFile;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class LogItem {

    // NB: when using a mapper to read this class, you must enable
    // DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, if "control" is present and
    // either of the timestamp formats in csvControl or tsvControl are strings, rather than arrays of strings.

    public String type;
    public LogData data;

    public String getJobId() { return data == null ? null : data.jobId; }
    public String getDataset() { return data == null ? null : data.dataset; }
    public String getFilename() { return data == null ? null : data.filename; }
    public String getDuration() { return data == null ? null : data.duration; }
    public String getTimestamp() { return data == null ? null : data.timestarmp; }
    public Integer getInserted() { return data == null ? null : data.inserted; }
    public Integer getUpdated() { return data == null ? null : data.updated; }
    public Integer getDeleted() { return data == null ? null : data.deleted; }
    public Integer getErrors() { return data == null ? null : data.errors; }
    public Map<String,Object> getDetails() { return data == null ? null : data.details; }
    public String getOpaqueUUID() {
        if (data == null) {
            return null;
        } else if(data.control != null) {
            return data.control.opaque;
        } else if(data.portControl != null) {
            return data.portControl.opaque;
        } else {
            return null;
        }
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    static class LogData {

        public String jobId;
        public String dataset;
        public String filename;
        public String duration;
        public String timestarmp;
        public Integer inserted;
        public Integer updated;
        public Integer deleted;
        public Integer errors;
        public ControlFile control;
        public PortControlFile portControl;
        public Map<String,Object> details;

    }
}
