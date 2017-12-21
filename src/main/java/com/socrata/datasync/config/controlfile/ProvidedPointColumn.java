package com.socrata.datasync.config.controlfile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=false)
public class ProvidedPointColumn extends SyntheticPointColumn {
    // all of these fields should be set to the column id of the relevant field
    public String latitude;
    public String longitude;
    public String type = "point";

    @JsonIgnore
    public static final String[] locationFieldNames = new String[] {"latitude", "longitude"};

    public Map<String, String> findComponentColumns() {
        Map<String,String> components = new HashMap<>();
        for (String s : locationFieldNames) {
            String fieldname = null;
            switch (s) {
                case "latitude":  fieldname = latitude;  break;
                case "longitude": fieldname = longitude; break;
            }
            if (fieldname != null && !fieldname.isEmpty())
                components.put(s, fieldname);
        }
        return components;
    }


    // Builder methods:

    public ProvidedPointColumn latitude(String l) { latitude = l; return this; }

    public ProvidedPointColumn longitude(String l) { longitude = l; return this; }
}
