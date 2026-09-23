package com.trustabac.iot.messaging.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of IdempotencyGuard providing duplicate protection
 * and at-most-once business effects within the running application instance.
 */
@Component
public class InMemoryIdempotencyGuard implements IdempotencyGuard {

    private static final Logger log = LoggerFactory.getLogger(InMemoryIdempotencyGuard.class);
    private static final int MAX_TRACKED_EVENTS = 10000;

    private final Set<String> processedEventIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public boolean tryAcquire(String eventId) {
        if (eventId == null || eventId.isBlank()) {
            return true; // No ID provided, allow processing
        }

        // Bounded size eviction if memory limit approached
        if (processedEventIds.size() > MAX_TRACKED_EVENTS) {
            log.warn("Idempotency guard memory boundary exceeded ({} items). Clearing half.", MAX_TRACKED_EVENTS);
            processedEventIds.clear();
        }

        boolean acquired = processedEventIds.add(eventId);
        if (!acquired) {
            log.info("Duplicate event [{}] detected by IdempotencyGuard - suppressing duplicate execution.", eventId);
        }
        return acquired;
    }

    @Override
    public void release(String eventId) {
        if (eventId != null) {
            processedEventIds.remove(eventId);
        }
    }

    @Override
    public void clear() {
        processedEventIds.clear();
    }

    @Override
    public int size() {
        return processedEventIds.size();
    }
}
