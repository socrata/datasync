package com.socrata.datasync.config.controlfile;

import com.socrata.datasync.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import com.socrata.datasync.PortMethod;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonPropertyOrder(alphabetic=true)
public class PortControlFile {
    public String destinationDomain;
    public String destinationName;
    public CopyType copyType;
    public Boolean publish;
    public String opaque;

    public static interface CopyType {}

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonPropertyOrder(alphabetic=true)
    public static class CopyData implements CopyType {
        public final String type;
        public String destinationDataset;
        public CopyData(String destinationDataset) {
            this.type = "data";
            this.destinationDataset = destinationDataset;
        }
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonPropertyOrder(alphabetic=true)
    public static class CopySchema implements CopyType {
        public final String type;
        public boolean toNbe;
        public CopySchema(boolean toNbe) {
            this.type = "schema";
            this.toNbe = toNbe;
        }
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonPropertyOrder(alphabetic=true)
    public static class CopyAll implements CopyType {
        public final String type;
        public boolean toNbe;
        public CopyAll(boolean toNbe) {
            this.type = "schema_and_data";
            this.toNbe = toNbe;
        }
    }

    public PortControlFile() {}

    public PortControlFile(String destinationDomain,
                           String destinationName,
                           String destinationDataset,
                           boolean toNbe,
                           PortMethod copyType,
                           Boolean publish)
    {
        this.destinationDomain = destinationDomain;
        this.destinationName = destinationName;
        this.publish = publish;

        switch(copyType) {
        case copy_data:
            this.copyType = new CopyData(destinationDataset);
            break;
        case copy_schema:
            this.copyType = new CopySchema(toNbe);
            break;
        case copy_all:
            this.copyType = new CopyAll(toNbe);
            break;
        }
    }

    public String generateAndAddOpaqueUUID() {
        String uuid = Utils.generateRequestId();
        this.opaque = uuid;
        return uuid;
    }
}
