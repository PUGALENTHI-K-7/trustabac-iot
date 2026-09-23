package com.trustabac.iot.messaging.consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyGuardTest {

    private InMemoryIdempotencyGuard guard;

    @BeforeEach
    void setUp() {
        guard = new InMemoryIdempotencyGuard();
    }

    @Test
    @DisplayName("Test tryAcquire returns true for first occurrence and false for duplicates")
    void testTryAcquire() {
        assertTrue(guard.tryAcquire("EVT-100"));
        assertFalse(guard.tryAcquire("EVT-100")); // Duplicate
        assertEquals(1, guard.size());

        assertTrue(guard.tryAcquire("EVT-200"));
        assertEquals(2, guard.size());
    }

    @Test
    @DisplayName("Test release allows re-acquisition of released eventId")
    void testRelease() {
        assertTrue(guard.tryAcquire("EVT-300"));
        guard.release("EVT-300");
        assertTrue(guard.tryAcquire("EVT-300"));
    }

    @Test
    @DisplayName("Test clear resets tracked event IDs")
    void testClear() {
        guard.tryAcquire("EVT-401");
        guard.tryAcquire("EVT-402");
        assertEquals(2, guard.size());

        guard.clear();
        assertEquals(0, guard.size());
        assertTrue(guard.tryAcquire("EVT-401"));
    }

    @Test
    @DisplayName("Test null or blank eventId is handled gracefully without error")
    void testNullOrBlankEventId() {
        assertTrue(guard.tryAcquire(null));
        assertTrue(guard.tryAcquire(""));
        assertTrue(guard.tryAcquire("   "));
    }
}
