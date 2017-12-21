package com.socrata.datasync.config.controlfile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = GeocodedPointColumn.class, name = "geocoded"),
            @JsonSubTypes.Type(value = ProvidedPointColumn.class, name = "point")
    })
public abstract class SyntheticPointColumn extends SyntheticColumn {
}
