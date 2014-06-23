package com.socrata.datasync;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

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
