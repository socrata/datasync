package com.socrata.datasync.imports2;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Blueprint {
    // Success fields
    private String fileId;
    private Summary summary;
    private String[] warnings;

    // Error fields
    private String code;
    private boolean error;
    private String message;

    @JsonProperty("fileId")
    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    @JsonProperty("fileId")
    public String getFileId() {
        return this.fileId;
    }

    @JsonProperty("summary")
    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    @JsonProperty("summary")
    public Summary getSummary() {
        return this.summary;
    }

    @JsonProperty("warnings")
    public void setWarnings(String[] warnings) {
        this.warnings = warnings;
    }

    @JsonProperty("warnings")
    public String[] getWarnings() {
        return this.warnings;
    }

    @JsonProperty("code")
    public void setCode(String code) {
        this.code = code;
    }

    @JsonProperty("code")
    public String getCode() {
        return this.code;
    }

    @JsonProperty("error")
    public void setError(boolean error) {
        this.error = error;
    }

    @JsonProperty("error")
    public boolean getError() {
        return this.error;
    }

    @JsonProperty("message")
    public void setMessage(String message) {
        this.message = message;
    }

    @JsonProperty("message")
    public String getMessage() {
        return this.message;
    }
}