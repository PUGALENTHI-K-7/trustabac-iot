package com.trustabac.iot.websocket;

import com.trustabac.iot.config.WebSocketProperties;
import com.trustabac.iot.messaging.event.AuthorizationResultEvent;
import com.trustabac.iot.messaging.event.BlockchainResultEvent;
import com.trustabac.iot.messaging.event.DeviceOperationEvent;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.messaging.event.RiskDomainEvent;
import com.trustabac.iot.messaging.event.SecurityAlertEvent;
import com.trustabac.iot.messaging.event.SimulatorStepEvent;
import com.trustabac.iot.messaging.event.TrustDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Observational, client-safe WebSocket event publisher.
 * Broadcasts typed events to STOMP topic destinations without mutating domain state.
 */
@Component
public class WebSocketEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventPublisher.class);

    public static final String TOPIC_DEVICES = "/topic/devices";
    public static final String TOPIC_TRUST = "/topic/trust";
    public static final String TOPIC_RISK = "/topic/risk";
    public static final String TOPIC_AUTHORIZATION = "/topic/authorization";
    public static final String TOPIC_BLOCKCHAIN = "/topic/blockchain";
    public static final String TOPIC_SIMULATOR = "/topic/simulator";
    public static final String TOPIC_SECURITY = "/topic/security";

    private final SimpMessagingTemplate messagingTemplate;
    private final WebSocketProperties properties;
    private final WebSocketSessionTracker sessionTracker;

    @Autowired
    public WebSocketEventPublisher(Optional<SimpMessagingTemplate> messagingTemplate,
                                   WebSocketProperties properties,
                                   WebSocketSessionTracker sessionTracker) {
        this.messagingTemplate = messagingTemplate.orElse(null);
        this.properties = properties;
        this.sessionTracker = sessionTracker;
    }

    public WebSocketEventPublisher(SimpMessagingTemplate messagingTemplate,
                                   WebSocketProperties properties,
                                   WebSocketSessionTracker sessionTracker) {
        this(Optional.ofNullable(messagingTemplate), properties, sessionTracker);
    }

    public boolean publishDeviceOperation(EventEnvelope<DeviceOperationEvent> envelope) {
        return sendToTopic(TOPIC_DEVICES, envelope);
    }

    public boolean publishTrustEvent(EventEnvelope<TrustDomainEvent> envelope) {
        return sendToTopic(TOPIC_TRUST, envelope);
    }

    public boolean publishRiskEvent(EventEnvelope<RiskDomainEvent> envelope) {
        return sendToTopic(TOPIC_RISK, envelope);
    }

    public boolean publishAuthorizationResult(EventEnvelope<AuthorizationResultEvent> envelope) {
        return sendToTopic(TOPIC_AUTHORIZATION, envelope);
    }

    public boolean publishBlockchainResult(EventEnvelope<BlockchainResultEvent> envelope) {
        return sendToTopic(TOPIC_BLOCKCHAIN, envelope);
    }

    public boolean publishSimulatorStep(EventEnvelope<SimulatorStepEvent> envelope) {
        return sendToTopic(TOPIC_SIMULATOR, envelope);
    }

    public boolean publishSecurityAlert(EventEnvelope<SecurityAlertEvent> envelope) {
        return sendToTopic(TOPIC_SECURITY, envelope);
    }

    private boolean sendToTopic(String destination, EventEnvelope<?> envelope) {
        if (!properties.isEnabled() || messagingTemplate == null) {
            log.debug("WebSocket streaming disabled or SimpMessagingTemplate unavailable; skipping [{}] to {}",
                    envelope.eventId(), destination);
            return false;
        }

        try {
            log.debug("Broadcasting WebSocket event [{}] (type: {}) to destination '{}' (corrId: {})",
                    envelope.eventId(), envelope.eventType(), destination, envelope.correlationId());

            messagingTemplate.convertAndSend(destination, envelope);
            sessionTracker.recordMessagePublished();
            return true;
        } catch (Exception e) {
            sessionTracker.recordPublishFailure();
            log.warn("Failed to broadcast WebSocket message [{}] to destination '{}': {}. Core processing unaffected.",
                    envelope.eventId(), destination, e.getMessage());
            return false;
        }
    }
}
