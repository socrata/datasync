package com.socrata.datasync.deltaimporter2;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Map;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class LogItem {

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
    public Map<String,String> getDetails() { return data == null ? null : data.details; }

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
        public Map<String,String> details;

    }
}
