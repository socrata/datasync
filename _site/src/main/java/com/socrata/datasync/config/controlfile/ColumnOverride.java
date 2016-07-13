package com.socrata.datasync.config.controlfile;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=false)
public class ColumnOverride {
    public Boolean trimWhitespace;
    public Boolean trimServerWhitespace;
    public Boolean useSocrataGeocoding;
    public Boolean emptyTextIsNull;
    public String[] timestampFormat;
    public String timezone;

    // Builder methods

    public ColumnOverride trimWhitespace (boolean t) { trimWhitespace = t; return this; }

    public ColumnOverride trimServerWhitespace (boolean t) { trimServerWhitespace = t; return this; }

    public ColumnOverride useSocrataGeocoding (boolean u) { useSocrataGeocoding = u; return this; }

    public ColumnOverride emptyTextIsNull (boolean e) { emptyTextIsNull = e; return this; }

    public ColumnOverride timestampFormat (String[] f) { timestampFormat = f; return this; }

    public ColumnOverride timezone (String t) { timezone = t; return this; }

}
