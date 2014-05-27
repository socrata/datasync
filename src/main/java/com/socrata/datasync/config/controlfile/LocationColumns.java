package com.socrata.datasync.config.controlfile;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class LocationColumns {
    // all of these fields should be set to the column id of the relevant field
    public String address;
    public String city;
    public String state;
    public String zip;
    public String latitude;
    public String longitude;
    
    // Builder methods:

    public LocationColumns address(String a) { address = a; return this; }

    public LocationColumns city(String c) { city = c; return this; }

    public LocationColumns latitude(String l) { latitude = l; return this; }

    public LocationColumns longitude(String l) { longitude = l; return this; }

    public LocationColumns state(String s) { state = s; return this; }

    public LocationColumns zip(String z) { zip = z; return this; }
}
