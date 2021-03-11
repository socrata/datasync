package com.socrata.datasync;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class LicenseType {
    public static final LicenseType no_license = new LicenseType("-- No License --", ""); //this doesn't work, need to figure out what value is needed to remove a license.  Tried "", "''", "null"

    private static final List<LicenseType> values = loadLicenses();
    public static List<LicenseType> values() {
        return values;
    }

    private final String label;
    private final String value;

    private LicenseType(final String label, final String value) {
        this.label = label;
        this.value = value;
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    private static class License {
        @JsonProperty String id;
        @JsonProperty String name;
    }

    private static List<LicenseType> loadLicenses() {
        try(InputStream is = LicenseType.class.getResourceAsStream("licenses.json")) {
            if(is == null) throw new FileNotFoundException("licenses.json");
            ObjectMapper mapper = new ObjectMapper();

            List<LicenseType> resourceLicenses = new ArrayList<>();
            for(License l : mapper.<List<License>>readValue(new InputStreamReader(is, StandardCharsets.UTF_8),
                                                            new TypeReference<List<License>>() {})) {
                resourceLicenses.add(new LicenseType(l.name, l.id));
            }
            resourceLicenses.sort(new Comparator<LicenseType>() {
                    public int compare(LicenseType a, LicenseType b) {
                        return a.getLabel().compareTo(b.getLabel());
                    }
                });
            List<LicenseType> result = new ArrayList<>();
            result.add(no_license);
            result.addAll(resourceLicenses);
            return Collections.unmodifiableList(result);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load licenses", e);
        }
    }


    public String toString() {
        return label;
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public static LicenseType getLicenseTypeForValue(String valueToMatch) {
        for(LicenseType licenseType : LicenseType.values()) {
            if (licenseType.getValue().equals(valueToMatch)) {
                return licenseType;
            }
        }
        return LicenseType.no_license;
    }

     public static LicenseType getLicenseTypeForLabel(String labelToMatch) {
        for(LicenseType licenseType : LicenseType.values()) {
            if (licenseType.getLabel().equals(labelToMatch)) {
                return licenseType;
            }
        }
        return LicenseType.no_license;
     }
}

