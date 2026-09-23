package com.trustabac.iot.messaging.consumer;

import com.trustabac.iot.dto.TrustEventRequest;
import com.trustabac.iot.dto.TrustUpdateResponse;
import com.trustabac.iot.entity.TrustEventType;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.messaging.event.SecurityAlertEvent;
import com.trustabac.iot.messaging.event.TrustDomainEvent;
import com.trustabac.iot.service.TrustService;
import com.trustabac.iot.websocket.WebSocketEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asynchronous consumer for incoming behavioral trust events and trust audit notifications.
 * Ensures a single logical event triggers at most one authoritative TrustService mutation.
 */
@Component
@ConditionalOnProperty(prefix = "trustabac.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TrustEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TrustEventConsumer.class);

    private final TrustService trustService;
    private final IdempotencyGuard idempotencyGuard;
    private final WebSocketEventPublisher wsPublisher;
    private final AtomicLong receivedCount = new AtomicLong(0);
    private final AtomicLong processedMutations = new AtomicLong(0);

    @Autowired
    public TrustEventConsumer(TrustService trustService,
                              IdempotencyGuard idempotencyGuard,
                              Optional<WebSocketEventPublisher> wsPublisher) {
        this.trustService = trustService;
        this.idempotencyGuard = idempotencyGuard;
        this.wsPublisher = wsPublisher.orElse(null);
    }

    public TrustEventConsumer(TrustService trustService,
                              IdempotencyGuard idempotencyGuard,
                              WebSocketEventPublisher wsPublisher) {
        this(trustService, idempotencyGuard, Optional.ofNullable(wsPublisher));
    }

    public TrustEventConsumer(TrustService trustService, IdempotencyGuard idempotencyGuard) {
        this(trustService, idempotencyGuard, Optional.empty());
    }

    @RabbitListener(queues = "${trustabac.messaging.trust-event-queue:trustabac.trust.events}")
    public void handleTrustEvent(EventEnvelope<TrustDomainEvent> envelope) {
        if (!idempotencyGuard.tryAcquire(envelope.eventId())) {
            log.info("Ignoring duplicate TrustDomainEvent [{}] (corrId: {})", envelope.eventId(), envelope.correlationId());
            return;
        }

        TrustDomainEvent payload = envelope.payload();
        receivedCount.incrementAndGet();

        // If newTrust is not yet established, this is an incoming trigger event requiring authoritative evaluation
        if (payload.newTrust() == null) {
            log.info("Processing inbound async TrustEvent [{}] for device='{}', type='{}' (corrId: {})",
                    envelope.eventId(), payload.deviceIdentifier(), payload.eventType(), envelope.correlationId());

            TrustEventRequest req = new TrustEventRequest();
            req.setEventType(payload.eventType());
            req.setReason(payload.reason() != null ? payload.reason() : "Asynchronous security event: " + payload.eventType());
            req.setSource(payload.source() != null ? payload.source() : "RABBITMQ_TRUST_PIPELINE");

            try {
                TrustUpdateResponse resp = trustService.recordTrustEvent(payload.deviceIdentifier(), req);
                processedMutations.incrementAndGet();
                log.info("Asynchronous TrustService mutation complete for device='{}': {} -> newTrust={}",
                        payload.deviceIdentifier(), payload.eventType(), resp.getNewTrust());

                if (wsPublisher != null) {
                    TrustDomainEvent updatedPayload = new TrustDomainEvent(
                            resp.getDeviceIdentifier(),
                            resp.getEventType(),
                            resp.getOldTrust(),
                            resp.getNewTrust(),
                            resp.getDelta(),
                            resp.getReason(),
                            resp.getSource(),
                            resp.getEventTimestamp() != null ? resp.getEventTimestamp().toString() : LocalDateTime.now().toString(),
                            envelope.correlationId()
                    );
                    EventEnvelope<TrustDomainEvent> updatedEnvelope = new EventEnvelope<>(
                            envelope.eventId(),
                            "TRUST_UPDATE_AUDIT",
                            LocalDateTime.now().toString(),
                            "TRUST_SERVICE",
                            payload.deviceIdentifier(),
                            envelope.propertyId(),
                            envelope.bookingId(),
                            envelope.requestReference(),
                            envelope.correlationId(),
                            updatedPayload,
                            "1.0"
                    );
                    wsPublisher.publishTrustEvent(updatedEnvelope);
                }
            } catch (Exception e) {
                log.warn("Failed to process asynchronous trust event for device='{}': {}",
                        payload.deviceIdentifier(), e.getMessage());
            }
        } else {
            // Already processed by TrustService (audit broadcast) - audit only, no double mutation
            log.info("Received Trust audit notification [{}] for device='{}': old={}, new={}, delta={} (corrId: {})",
                    envelope.eventId(), payload.deviceIdentifier(), payload.oldTrust(), payload.newTrust(), payload.delta(), envelope.correlationId());

            if (wsPublisher != null) {
                wsPublisher.publishTrustEvent(envelope);
            }
        }

        // Security alert dispatch for abnormal / degrading trust events
        if (wsPublisher != null && payload.eventType() != null && isSecurityDegrading(payload.eventType())) {
            SecurityAlertEvent alert = new SecurityAlertEvent(
                    "ALERT-" + UUID.randomUUID().toString().substring(0, 8),
                    payload.eventType().name(),
                    payload.eventType() == TrustEventType.CONFIRMED_MALICIOUS ? "CRITICAL" : "HIGH",
                    payload.deviceIdentifier(),
                    null,
                    null,
                    payload.reason() != null ? payload.reason() : "Trust score degraded due to " + payload.eventType(),
                    "Score reduced; access restrictions applied on subsequent evaluations.",
                    LocalDateTime.now().toString(),
                    envelope.correlationId()
            );
            EventEnvelope<SecurityAlertEvent> alertEnvelope = new EventEnvelope<>(
                    "EVT-ALERT-" + UUID.randomUUID().toString().substring(0, 8),
                    "SECURITY_ALERT",
                    LocalDateTime.now().toString(),
                    "TRUST_PIPELINE",
                    payload.deviceIdentifier(),
                    envelope.propertyId(),
                    envelope.bookingId(),
                    envelope.requestReference(),
                    envelope.correlationId(),
                    alert,
                    "1.0"
            );
            wsPublisher.publishSecurityAlert(alertEnvelope);
        }
    }

    private boolean isSecurityDegrading(TrustEventType type) {
        return type == TrustEventType.SUSPICIOUS_ACTIVITY
                || type == TrustEventType.REQUEST_FLOODING
                || type == TrustEventType.CONFIRMED_MALICIOUS;
    }

    public long getReceivedCount() {
        return receivedCount.get();
    }

    public long getProcessedMutations() {
        return processedMutations.get();
    }

    public void resetMetrics() {
        receivedCount.set(0);
        processedMutations.set(0);
    }
}
