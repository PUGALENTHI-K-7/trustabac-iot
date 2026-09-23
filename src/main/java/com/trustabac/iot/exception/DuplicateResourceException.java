package com.trustabac.iot.exception;

/**
 * Exception thrown when an entity creation/update violates uniqueness constraints.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
