package com.socrata.datasync;

/**
 * @author Adrian Laurenzi
 * 
 * Holds the various methods that can be used to
 * publish data using the Socrata publisher API
 */
public enum PublishMethod {
	upsert, append, replace, delete
}
