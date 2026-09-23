package com.trustabac.iot.messaging.consumer;

/**
 * Strategy interface providing duplicate protection / at-most-once business effect
 * for incoming asynchronous domain events.
 */
public interface IdempotencyGuard {

    /**
     * Attempts to acquire execution rights for the given event identifier.
     *
     * @param eventId the unique event identifier
     * @return true if the event has not been processed yet and acquisition succeeded; false if duplicate
     */
    boolean tryAcquire(String eventId);

    /**
     * Releases or clears the processed state for the given event identifier.
     *
     * @param eventId the unique event identifier
     */
    void release(String eventId);

    /**
     * Clears all recorded event identifiers (for test isolation / reset).
     */
    void clear();

    /**
     * Returns the count of unique event identifiers tracked.
     */
    int size();
}
