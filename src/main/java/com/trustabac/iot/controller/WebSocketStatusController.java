package com.trustabac.iot.controller;

import com.trustabac.iot.config.WebSocketProperties;
import com.trustabac.iot.websocket.WebSocketEventPublisher;
import com.trustabac.iot.websocket.WebSocketSessionTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller exposing health, configuration topology, and metrics for the WebSocket / STOMP streaming layer.
 */
@RestController
@RequestMapping("/api/websocket")
public class WebSocketStatusController {

    private final WebSocketProperties properties;
    private final WebSocketSessionTracker sessionTracker;

    @Autowired
    public WebSocketStatusController(WebSocketProperties properties,
                                     Optional<WebSocketSessionTracker> sessionTracker) {
        this.properties = properties;
        this.sessionTracker = sessionTracker.orElse(null);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getWebSocketStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", properties.isEnabled() ? "ACTIVE" : "DISABLED");
        status.put("enabled", properties.isEnabled());
        status.put("endpoint", properties.getEndpoint());
        status.put("appPrefix", properties.getAppPrefix());
        status.put("topicPrefix", properties.getTopicPrefix());
        status.put("allowedOrigins", properties.getAllowedOrigins());

        Map<String, String> topics = new LinkedHashMap<>();
        topics.put("devices", WebSocketEventPublisher.TOPIC_DEVICES);
        topics.put("trust", WebSocketEventPublisher.TOPIC_TRUST);
        topics.put("risk", WebSocketEventPublisher.TOPIC_RISK);
        topics.put("authorization", WebSocketEventPublisher.TOPIC_AUTHORIZATION);
        topics.put("blockchain", WebSocketEventPublisher.TOPIC_BLOCKCHAIN);
        topics.put("simulator", WebSocketEventPublisher.TOPIC_SIMULATOR);
        topics.put("security", WebSocketEventPublisher.TOPIC_SECURITY);
        status.put("topics", topics);
        status.put("supportedTopics", new java.util.ArrayList<>(topics.values()));

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("activeSessions", sessionTracker != null ? sessionTracker.getActiveSessionCount() : 0);
        metrics.put("totalConnected", sessionTracker != null ? sessionTracker.getTotalConnected() : 0);
        metrics.put("totalDisconnected", sessionTracker != null ? sessionTracker.getTotalDisconnected() : 0);
        metrics.put("messagesPublished", sessionTracker != null ? sessionTracker.getMessagesPublished() : 0);
        metrics.put("publishFailures", sessionTracker != null ? sessionTracker.getPublishFailures() : 0);
        status.put("metrics", metrics);

        status.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(status);
    }
}
