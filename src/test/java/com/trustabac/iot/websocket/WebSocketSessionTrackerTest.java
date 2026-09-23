package com.trustabac.iot.websocket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WebSocketSessionTrackerTest {

    private WebSocketSessionTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new WebSocketSessionTracker();
    }

    @Test
    @DisplayName("Verify session connection and disconnection lifecycle")
    void testSessionLifecycle() {
        assertEquals(0, tracker.getActiveSessionCount());
        assertEquals(0, tracker.getTotalConnected());

        Message<byte[]> msg = new GenericMessage<>(new byte[0], Map.of("simpSessionId", "sess-1"));
        SessionConnectedEvent connectEvent = new SessionConnectedEvent(this, msg);
        SessionDisconnectEvent disconnectEvent = new SessionDisconnectEvent(this, msg, "sess-1", CloseStatus.NORMAL);

        tracker.handleSessionConnected(connectEvent);
        assertEquals(1, tracker.getActiveSessionCount());
        assertEquals(1, tracker.getTotalConnected());

        tracker.handleSessionDisconnect(disconnectEvent);
        assertEquals(0, tracker.getActiveSessionCount());
        assertEquals(1, tracker.getTotalConnected());
        assertEquals(1, tracker.getTotalDisconnected());
    }

    @Test
    @DisplayName("Verify message published and failure counters")
    void testMessageCounters() {
        assertEquals(0, tracker.getMessagesPublished());
        assertEquals(0, tracker.getPublishFailures());

        tracker.recordMessagePublished();
        tracker.recordMessagePublished();
        tracker.recordPublishFailure();

        assertEquals(2, tracker.getMessagesPublished());
        assertEquals(1, tracker.getPublishFailures());
    }

    @Test
    @DisplayName("Verify metrics reset functionality")
    void testResetMetrics() {
        tracker.recordMessagePublished();
        tracker.recordPublishFailure();
        assertEquals(1, tracker.getMessagesPublished());
        assertEquals(1, tracker.getPublishFailures());

        tracker.resetMetrics();
        assertEquals(0, tracker.getMessagesPublished());
        assertEquals(0, tracker.getPublishFailures());
        assertEquals(0, tracker.getActiveSessionCount());
    }
}
