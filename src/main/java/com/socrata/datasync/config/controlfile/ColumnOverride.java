package com.socrata.datasync.config.controlfile;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
public class ColumnOverride {
    public Boolean trimWhitespace;
    public Boolean trimServerWhitespace;
    public Boolean ignoreServerLatLong;
    public Boolean emptyTextIsNull;
    public String[] floatingTimestampFormat;
    public String[] fixedTimestampFormat;
    public String timezone;

    // Builder methods

    public ColumnOverride trimWhitespace (boolean t) { trimWhitespace = t; return this; }

    public ColumnOverride trimServerWhitespace (boolean t) { trimServerWhitespace = t; return this; }

    public ColumnOverride ignoreServerLatLong (boolean i) { ignoreServerLatLong = i; return this; }

    public ColumnOverride emptyTextIsNull (boolean e) { emptyTextIsNull = e; return this; }

    public ColumnOverride floatingTimestampFormat (String[] f) { floatingTimestampFormat = f; return this; }

    public ColumnOverride fixedTimestampFormat (String[] f) { fixedTimestampFormat = f; return this; }

    public ColumnOverride timezone (String t) { timezone = t; return this; }

}
