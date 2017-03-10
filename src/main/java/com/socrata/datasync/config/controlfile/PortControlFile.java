package com.socrata.datasync.config.controlfile;

import com.socrata.datasync.Utils;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonPropertyOrder(alphabetic=true)
public class PortControlFile {
    public String destinationDomain;
    public String destinationName;
    public Boolean toNbe;
    public Boolean schemaOnly;
    public Boolean publish;
    public String opaque;

    public PortControlFile() {}

    public PortControlFile(String destinationDomain,
                           String destinationName,
                           Boolean toNbe,
                           Boolean schemaOnly,
                           Boolean publish)
    {
        this.destinationDomain = destinationDomain;
        this.destinationName = destinationName;
        this.toNbe = toNbe;
        this.schemaOnly = schemaOnly;
        this.publish = publish;
    }

    public String generateAndAddOpaqueUUID() {
        String uuid = Utils.generateRequestId();
        this.opaque = uuid;
        return uuid;
    }
}
