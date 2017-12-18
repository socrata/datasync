package com.socrata.datasync.imports2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Layer {
    private String name;
    private String referenceSystem;
    private String replacingUid;

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("name")
    public String getName() {
        return this.name;
    }

    @JsonProperty("referenceSystem")
    public void setReferenceSystem(String referenceSystem) {
        this.referenceSystem = referenceSystem;
    }

    @JsonProperty("referenceSystem")
    public String getReferenceSystem() {
        return this.referenceSystem;
    }

    @JsonProperty("replacingUid")
    public void setReplacingUid(String replacingUid) {
        this.replacingUid = replacingUid;
    }

    @JsonProperty("replacingUid")
    public String getReplacingUid() {
        return this.replacingUid;
    }
}
