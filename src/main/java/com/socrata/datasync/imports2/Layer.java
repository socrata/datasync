package com.socrata.datasync.imports2;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

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
