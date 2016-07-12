package com.socrata.datasync.deltaimporter2;

import com.socrata.datasync.config.controlfile.ControlFile;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.List;

@JsonPropertyOrder(alphabetic=true)
public class CommitMessage {
    public String filename;
    public String relativeTo;
    public List<String> chunks;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL) // this is technically optional
    public Long expectedSize;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    public ControlFile control;

    // Builder methods
    public CommitMessage filename(String f) { filename = f; return this; }

    public CommitMessage relativeTo(String r) { relativeTo = r; return this; }

    public CommitMessage chunks(List<String> c) { chunks = c; return this; }

    public CommitMessage control(ControlFile c) { control = c; return this; }

    public CommitMessage expectedSize(Long s) { expectedSize = s; return this; }
}
