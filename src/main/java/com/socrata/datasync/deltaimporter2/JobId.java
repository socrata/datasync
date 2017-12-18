package com.socrata.datasync.deltaimporter2;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class JobId {
    public String jobId;
}
