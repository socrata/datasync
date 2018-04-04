package com.socrata.datasync.publishers;

import java.util.Map;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StatusResponse {
    public String type;
    public Map<String, Object> data;
    public String english;
}
