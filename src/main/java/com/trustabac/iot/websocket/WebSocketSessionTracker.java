package com.trustabac.iot.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe, memory-bounded session and metrics tracker for WebSocket / STOMP clients.
 * Tracks connected sessions and transmission counts without retaining message payload history.
 */
@Component
public class WebSocketSessionTracker {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionTracker.class);

    private final Set<String> activeSessionIds = ConcurrentHashMap.newKeySet();
    private final AtomicLong totalConnected = new AtomicLong(0);
    private final AtomicLong totalDisconnected = new AtomicLong(0);
    private final AtomicLong messagesPublished = new AtomicLong(0);
    private final AtomicLong publishFailures = new AtomicLong(0);

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        if (sessionId != null) {
            activeSessionIds.add(sessionId);
            totalConnected.incrementAndGet();
            log.info("WebSocket client connected [session: {}]. Active sessions: {}", sessionId, activeSessionIds.size());
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        if (sessionId != null) {
            activeSessionIds.remove(sessionId);
            totalDisconnected.incrementAndGet();
            log.info("WebSocket client disconnected [session: {}]. Active sessions: {}", sessionId, activeSessionIds.size());
        }
    }

    public void recordMessagePublished() {
        messagesPublished.incrementAndGet();
    }

    public void recordPublishFailure() {
        publishFailures.incrementAndGet();
    }

    public int getActiveSessionCount() {
        return activeSessionIds.size();
    }

    public long getTotalConnected() {
        return totalConnected.get();
    }

    public long getTotalDisconnected() {
        return totalDisconnected.get();
    }

    public long getMessagesPublished() {
        return messagesPublished.get();
    }

    public long getPublishFailures() {
        return publishFailures.get();
    }

    public void resetMetrics() {
        activeSessionIds.clear();
        totalConnected.set(0);
        totalDisconnected.set(0);
        messagesPublished.set(0);
        publishFailures.set(0);
    }
}
