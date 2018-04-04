package com.socrata.datasync.deltaimporter2;

import com.socrata.datasync.config.controlfile.ControlFile;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonPropertyOrder(alphabetic=true)
public class CommitMessage<T> {
    public String filename;
    public String relativeTo;
    public List<String> chunks;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL) // this is technically optional
    public Long expectedSize;

    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    public T control;

    // Builder methods
    public CommitMessage<T> filename(String f) { filename = f; return this; }

    public CommitMessage<T> relativeTo(String r) { relativeTo = r; return this; }

    public CommitMessage<T> chunks(List<String> c) { chunks = c; return this; }

    public CommitMessage<T> control(T c) { control = c; return this; }

    public CommitMessage<T> expectedSize(Long s) { expectedSize = s; return this; }
}
