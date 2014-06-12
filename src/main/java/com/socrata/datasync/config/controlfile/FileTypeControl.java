package com.socrata.datasync.config.controlfile;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Map;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonPropertyOrder(alphabetic=true)
public class FileTypeControl {
    public String encoding;
    public String separator;
    public String quote;
    public String escape;
    public String[] columns;
    public Integer skip;
    public Boolean trimWhitespace;
    public Boolean trimServerWhitespace;
    public Boolean ignoreServerLatLong;
    public Boolean emptyTextIsNull;
    public String[] floatingTimestampFormat;
    public String[] fixedTimestampFormat;
    public String timezone;
    public Map<String, ColumnOverride> overrides;
    public Map<String, LocationColumns> syntheticLocations;
    public Boolean useSocrataGeocoding;

    public FileTypeControl() {}

    public boolean hasColumns(){
        return (columns != null && columns.length > 0);
    }


    // Builder methods:

    public FileTypeControl encoding(String e) { encoding = e; return this; }

    public FileTypeControl separator(String s) { separator = s; return this; }

    public FileTypeControl quote(String q) { quote = q; return this; }

    public FileTypeControl escape(String e) { escape = e; return this; }

    public FileTypeControl columns(String[] c) { columns = c; return this; }

    public FileTypeControl skip(int s) { skip = s; return this; }

    public FileTypeControl trimWhitespace(boolean t) { trimWhitespace = t; return this; }

    public FileTypeControl trimServerWhitespace(boolean t) { trimServerWhitespace = t; return this; }

    public FileTypeControl ignoreServerLatLong(boolean i) { ignoreServerLatLong = i; return this; }

    public FileTypeControl emptyTextIsNull(boolean e) { emptyTextIsNull = e; return this; }

    public FileTypeControl floatingTimestampFormat(String[] f) { floatingTimestampFormat = f; return this; }

    public FileTypeControl fixedTimestampFormat(String[] f) { fixedTimestampFormat = f; return this; }

    public FileTypeControl timezone(String t) { timezone = t; return this; }

    public FileTypeControl overrides(Map<String, ColumnOverride> o) { overrides = o; return this; }

    public FileTypeControl syntheticLocations(Map<String, LocationColumns> s) { syntheticLocations = s; return this; }

    public FileTypeControl useSocrataGeocoding(boolean u) { useSocrataGeocoding = u; return this; }
}


