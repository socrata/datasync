package com.socrata.datasync;

/**
 * @author Adrian Laurenzi
 * 
 * Holds the various methods that can be used to
 * publish data using the Socrata publisher API
 */
public enum PublishMethod {
    replace, upsert, append, delete
}
