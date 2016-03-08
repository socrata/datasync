package com.socrata.datasync.config.controlfile;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.stream.Location;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=false)
@JsonPropertyOrder(alphabetic=true)
public class FileTypeControl {

    @JsonIgnore
    public String filePath;
    public String encoding;
    public String separator;
    @JsonIgnore
    public boolean hasHeaderRow;
    public String quote;
    public String escape;
    public String[] columns;
    public String[] ignoreColumns;
    public Integer skip;
    public Boolean trimWhitespace;
    public Boolean trimServerWhitespace;
    public Boolean emptyTextIsNull;
    public String[] floatingTimestampFormat;
    public String[] fixedTimestampFormat;
    public String timezone;
    public String dropUninterpretableRows;
    public Map<String, ColumnOverride> overrides;
    public Map<String, LocationColumn> analyticLocations;
    public Map<String, LocationColumn> syntheticLocations;
    public Map<String, LocationColumn> syntheticPoints;
    public Boolean useSocrataGeocoding;
    public String action;
    public Boolean columnStatistics;
    public Boolean setAsideErrors;

    public FileTypeControl() {}

    public boolean hasEncoding(){ return (encoding != null && !encoding.isEmpty()); }

    public boolean hasColumns(){
        return (columns != null && columns.length > 0);
    }

    public boolean hasIgnoredColumns(){
        return (ignoreColumns != null && ignoreColumns.length > 0);
    }

    public boolean hasSyntheticLocations(){
        return (syntheticLocations != null && syntheticLocations.size() > 0);
    }

    public boolean hasTimestampFormatting() {
        boolean haveDatasetTimestampFormatting =
                ((floatingTimestampFormat != null && floatingTimestampFormat.length > 0) ||
                (fixedTimestampFormat != null && fixedTimestampFormat.length > 0));

        boolean haveColumnTimestampFormatting = false;
        if (overrides != null) {
            for (ColumnOverride override : overrides.values()) {
                if (override.timestampFormat != null && override.timestampFormat.length > 0) {
                    haveColumnTimestampFormatting = true;
                    break;
                }
            }
        }
        return haveDatasetTimestampFormatting || haveColumnTimestampFormatting;
    }

    public boolean hasOverrides() { return (overrides != null && overrides.size() > 0); }

    public Set<String> lookupTimestampFormatting() {
        Set<String> formats = new HashSet<>();

        if (floatingTimestampFormat != null)
            for (String format : floatingTimestampFormat)
                formats.add(format);
        if (fixedTimestampFormat != null)
            for (String format : fixedTimestampFormat)
                formats.add(format);
        if (overrides != null) {
            for (ColumnOverride override : overrides.values()) {
                if (override.timestampFormat != null) {
                    for (String format : override.timestampFormat)
                        formats.add(format);
                }
            }
        }
        return formats;
    }

    // Builder methods:

    public FileTypeControl encoding(String e) { encoding = e; return this; }

    public FileTypeControl separator(String s) { separator = s; return this; }

    public FileTypeControl quote(String q) { quote = q; return this; }

    public FileTypeControl escape(String e) { escape = e; return this; }

    public FileTypeControl columns(String[] c) { columns = c; return this; }

    public FileTypeControl ignoreColumns(String[] c) { ignoreColumns = c; return this; }

    public FileTypeControl skip(int s) { skip = s; return this; }

    public FileTypeControl trimWhitespace(boolean t) { trimWhitespace = t; return this; }


    public FileTypeControl filePath(String path) { filePath = path; return this; }

    public FileTypeControl trimServerWhitespace(boolean t) { trimServerWhitespace = t; return this; }

    public FileTypeControl emptyTextIsNull(boolean e) { emptyTextIsNull = e; return this; }

    public FileTypeControl floatingTimestampFormat(String[] f) { floatingTimestampFormat = f; return this; }

    public FileTypeControl fixedTimestampFormat(String[] f) { fixedTimestampFormat = f; return this; }

    public FileTypeControl timezone(String t) { timezone = t; return this; }

    public FileTypeControl dropUninterpretableRows(String d) { dropUninterpretableRows = d; return this; }

    public FileTypeControl overrides(Map<String, ColumnOverride> o) { overrides = o; return this; }

    public FileTypeControl syntheticLocations(Map<String, LocationColumn> s) { syntheticLocations = s; return this; }

    public FileTypeControl analyticLocations(Map<String, LocationColumn> s) { analyticLocations = s; return this; }

    public FileTypeControl syntheticPoints(Map<String, LocationColumn> s) { syntheticPoints = s; return this; }

    public FileTypeControl useSocrataGeocoding(boolean u) { useSocrataGeocoding = u; return this; }

    public FileTypeControl hasHeaderRow(boolean h) { hasHeaderRow = h; return this;}

    public FileTypeControl action(String a) { action = a; return this;}

    public FileTypeControl columnStatistics(boolean u) { columnStatistics = u; return this; }

    public FileTypeControl setAsideErrors(boolean u) { setAsideErrors = u; return this; }

}
