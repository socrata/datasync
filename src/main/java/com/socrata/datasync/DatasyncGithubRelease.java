package com.socrata.datasync;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class DatasyncGithubRelease {

    public String url;

    @JsonProperty("html_url")
    public String htmlUrl;

    @JsonProperty("tag_name")
    public String tagName;

    public String name;

}
