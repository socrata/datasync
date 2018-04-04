package com.socrata.datasync.config.controlfile;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.HashMap;
import java.util.Map;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=false)
public class GeocodedPointColumn extends SyntheticPointColumn implements Cloneable {
    // all of these fields should be set to the column id of the relevant field
    public String address;
    public String city;
    public String state;
    public String zip;
    public String country;
    public String type = "geocoded";

    @JsonIgnore
    public static final String[] locationFieldNames = new String[] {"address", "city", "state", "zip", "country"};

    public Map<String, String> findComponentColumns() {
        Map<String,String> components = new HashMap<>();
        for (String s : locationFieldNames) {
            String fieldname = null;
            switch (s) {
                case "address":   fieldname = address;   break;
                case "city":      fieldname = city;      break;
                case "state":     fieldname = state;     break;
                case "zip":       fieldname = zip;       break;
                case "country":   fieldname = country;   break;
            }
            if (fieldname != null && !fieldname.isEmpty())
                components.put(s, fieldname);
        }
        return components;
    }


    // Builder methods:

    public GeocodedPointColumn address(String a) { address = a; return this; }

    public GeocodedPointColumn city(String c) { city = c; return this; }

    public GeocodedPointColumn state(String s) { state = s; return this; }

    public GeocodedPointColumn zip(String z) { zip = z; return this; }

    public GeocodedPointColumn country(String c) { country = c; return this; }

    public GeocodedPointColumn clone() {
        return (GeocodedPointColumn) super.clone();
    }
}
