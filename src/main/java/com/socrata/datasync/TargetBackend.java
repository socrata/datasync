package com.socrata.datasync;

public enum TargetBackend {
    default_backend("Default"), nbe("NBE");

    TargetBackend(String humanReadable) {
        this.humanReadable = humanReadable;
    }

    private final String humanReadable;

    public String toString() {
        return humanReadable;
    }
}
