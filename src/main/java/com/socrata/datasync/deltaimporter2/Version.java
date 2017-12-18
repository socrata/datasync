package com.socrata.datasync.deltaimporter2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Version {
    @JsonProperty("max-block-size")
    public int maxBlockSize;
}
