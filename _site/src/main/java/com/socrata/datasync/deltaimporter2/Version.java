package com.socrata.datasync.deltaimporter2;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class Version {
    @JsonProperty("max-block-size")
    public int maxBlockSize;
}
