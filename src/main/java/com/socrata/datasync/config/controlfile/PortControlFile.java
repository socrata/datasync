package com.socrata.datasync.config.controlfile;

import java.io.IOException;

import com.socrata.datasync.Utils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;

import com.socrata.datasync.PortMethod;

@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonPropertyOrder(alphabetic=true)
public class PortControlFile {
    public String sourceDomain;
    public String sourceDataset;
    public String destinationName;
    public CopyType copyType;
    public Boolean publish;
    public String opaque;

    public static class CopyTypeDeserializer extends StdDeserializer<CopyType> {
        public CopyTypeDeserializer() {
            super(CopyType.class);
        }

        public CopyType deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
            JsonNode node = jp.getCodec().readTree(jp);
            String type = node.get("type").asText();
            switch(type) {
            case "data": return new CopyData();
            case "schema": return new CopySchema();
            case "schema_and_data": return new CopyAll();
            default: return null;
            }
        }
    }

    @JsonDeserialize(using = CopyTypeDeserializer.class)
    public static interface CopyType {}

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonPropertyOrder(alphabetic=true)
    public static class CopyData implements CopyType {
        public final String type;
        public CopyData() {
            this.type = "data";
        }
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonPropertyOrder(alphabetic=true)
    public static class CopySchema implements CopyType {
        public final String type;
        public CopySchema() {
            this.type = "schema";
        }
    }

    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @JsonPropertyOrder(alphabetic=true)
    public static class CopyAll implements CopyType {
        public final String type;
        public CopyAll() {
            this.type = "schema_and_data";
        }
    }

    public PortControlFile() {}

    public PortControlFile(String sourceDomain,
                           String sourceDataset,
                           String destinationName,
                           PortMethod copyType,
                           Boolean publish)
    {
        this.sourceDomain = sourceDomain;
        this.sourceDataset = sourceDataset;
        this.destinationName = destinationName;
        this.publish = publish;

        switch(copyType) {
        case copy_data:
            this.copyType = new CopyData();
            break;
        case copy_schema:
            this.copyType = new CopySchema();
            break;
        case copy_all:
            this.copyType = new CopyAll();
            break;
        }
    }

    public String generateAndAddOpaqueUUID() {
        String uuid = Utils.generateRequestId();
        this.opaque = uuid;
        return uuid;
    }
}
