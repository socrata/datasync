package com.socrata.datasync.config.controlfile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=false)
public class LocationColumn extends SyntheticColumn {
    // all of these fields should be set to the column id of the relevant field
    public String address;
    public String city;
    public String state;
    public String zip;
    public String latitude;
    public String longitude;
    public String type;
    @JsonIgnore
    public static final String[] locationFieldNames = new String[] {"address", "city", "state", "zip", "latitude", "longitude"};

    public Map<String, String> findComponentColumns() {
        Map<String,String> components = new HashMap<>();
        for (String s : locationFieldNames) {
            String fieldname = null;
            switch (s) {
                case "address":   fieldname = address;   break;
                case "city":      fieldname = city;      break;
                case "state":     fieldname = state;     break;
                case "zip":       fieldname = zip;       break;
                case "latitude":  fieldname = latitude;  break;
                case "longitude": fieldname = longitude; break;
            }
            if (fieldname != null && !fieldname.isEmpty())
                components.put(s, fieldname);
        }
        return components;
    }


    // Builder methods:

    public LocationColumn address(String a) { address = a; return this; }

    public LocationColumn city(String c) { city = c; return this; }

    public LocationColumn latitude(String l) { latitude = l; return this; }

    public LocationColumn longitude(String l) { longitude = l; return this; }

    public LocationColumn state(String s) { state = s; return this; }

    public LocationColumn zip(String z) { zip = z; return this; }
}
