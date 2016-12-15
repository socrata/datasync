package com.socrata.datasync.imports2;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonIgnoreProperties(ignoreUnknown=true)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
public class Summary {
    private int totalFeatureCount;
    private Layer[] layers;

    @JsonProperty("totalFeatureCount")
    public void setTotalFeatureCount(int totalFeatureCount) {
        this.totalFeatureCount = totalFeatureCount;
    }

    @JsonProperty("totalFeatureCount")
    public int getTotalFeatureCount() {
        return this.totalFeatureCount;
    }

    @JsonProperty("layers")
    public void setLayers(Layer[] layers) {
        this.layers = layers;
    }

    @JsonProperty("layers")
    public Layer[] getLayers() {
        return this.layers;
    }
}
