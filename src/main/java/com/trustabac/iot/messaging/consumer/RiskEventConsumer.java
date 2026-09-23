package com.trustabac.iot.messaging.consumer;

import com.trustabac.iot.dto.RiskEvaluationRequest;
import com.trustabac.iot.dto.RiskEvaluationResponse;
import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.messaging.event.RiskDomainEvent;
import com.trustabac.iot.messaging.event.SecurityAlertEvent;
import com.trustabac.iot.service.RiskService;
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
 * Asynchronous consumer for incoming contextual risk events and risk audit notifications.
 * Ensures a single logical event triggers at most one authoritative RiskService evaluation.
 */
@Component
@ConditionalOnProperty(prefix = "trustabac.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RiskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(RiskEventConsumer.class);

    private final RiskService riskService;
    private final IdempotencyGuard idempotencyGuard;
    private final WebSocketEventPublisher wsPublisher;
    private final AtomicLong receivedCount = new AtomicLong(0);
    private final AtomicLong processedEvaluations = new AtomicLong(0);

    @Autowired
    public RiskEventConsumer(RiskService riskService,
                             IdempotencyGuard idempotencyGuard,
                             Optional<WebSocketEventPublisher> wsPublisher) {
        this.riskService = riskService;
        this.idempotencyGuard = idempotencyGuard;
        this.wsPublisher = wsPublisher.orElse(null);
    }

    public RiskEventConsumer(RiskService riskService,
                             IdempotencyGuard idempotencyGuard,
                             WebSocketEventPublisher wsPublisher) {
        this(riskService, idempotencyGuard, Optional.ofNullable(wsPublisher));
    }

    public RiskEventConsumer(RiskService riskService, IdempotencyGuard idempotencyGuard) {
        this(riskService, idempotencyGuard, Optional.empty());
    }

    @RabbitListener(queues = "${trustabac.messaging.risk-event-queue:trustabac.risk.events}")
    public void handleRiskEvent(EventEnvelope<RiskDomainEvent> envelope) {
        if (!idempotencyGuard.tryAcquire(envelope.eventId())) {
            log.info("Ignoring duplicate RiskDomainEvent [{}] (corrId: {})", envelope.eventId(), envelope.correlationId());
            return;
        }

        RiskDomainEvent payload = envelope.payload();
        receivedCount.incrementAndGet();

        // If calculatedRiskScore is not yet established, evaluate via authoritative RiskService
        if (payload.calculatedRiskScore() == null) {
            log.info("Processing inbound async RiskContext [{}] for device='{}', res='{}', op='{}' (corrId: {})",
                    envelope.eventId(), payload.deviceIdentifier(), payload.resource(), payload.operation(), envelope.correlationId());

            Operation op = Operation.READ;
            if (payload.operation() != null) {
                try {
                    op = Operation.valueOf(payload.operation().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            BehavioralIndicator indicator = BehavioralIndicator.NORMAL;
            if (payload.behavioralIndicator() != null) {
                try {
                    indicator = BehavioralIndicator.valueOf(payload.behavioralIndicator().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                }
            }

            RiskEvaluationRequest req = new RiskEvaluationRequest(
                    payload.deviceIdentifier(),
                    "SYSTEM_ASYNC",
                    payload.resource(),
                    op,
                    payload.location(),
                    payload.networkContext(),
                    payload.requestCountWindow(),
                    payload.recentViolationCount(),
                    indicator
            );

            try {
                RiskEvaluationResponse resp = riskService.resolveAndEvaluate(req);
                processedEvaluations.incrementAndGet();
                log.info("Asynchronous RiskService evaluation complete for device='{}': score={}, status={}",
                        payload.deviceIdentifier(), resp.riskScore(), resp.riskStatus());

                if (wsPublisher != null) {
                    RiskDomainEvent evaluatedPayload = new RiskDomainEvent(
                            resp.deviceIdentifier(),
                            payload.resource(),
                            payload.operation(),
                            payload.location(),
                            payload.networkContext(),
                            payload.requestCountWindow(),
                            payload.recentViolationCount(),
                            indicator.name(),
                            resp.riskScore(),
                            resp.riskStatus() != null ? resp.riskStatus().name() : "LOW",
                            resp.reason(),
                            resp.evaluationTimestamp() != null ? resp.evaluationTimestamp().toString() : LocalDateTime.now().toString(),
                            envelope.correlationId()
                    );
                    EventEnvelope<RiskDomainEvent> evaluatedEnvelope = new EventEnvelope<>(
                            envelope.eventId(),
                            "RISK_EVALUATION_AUDIT",
                            LocalDateTime.now().toString(),
                            "RISK_ENGINE",
                            payload.deviceIdentifier(),
                            envelope.propertyId(),
                            envelope.bookingId(),
                            envelope.requestReference(),
                            envelope.correlationId(),
                            evaluatedPayload,
                            "1.0"
                    );
                    wsPublisher.publishRiskEvent(evaluatedEnvelope);
                }
            } catch (Exception e) {
                log.warn("Failed to evaluate asynchronous risk context for device='{}': {}",
                        payload.deviceIdentifier(), e.getMessage());
            }
        } else {
            // Already evaluated by RiskService (audit broadcast) - audit only
            log.info("Received Risk audit notification [{}] for device='{}': score={}, status='{}' (corrId: {})",
                    envelope.eventId(), payload.deviceIdentifier(), payload.calculatedRiskScore(), payload.riskStatus(), envelope.correlationId());

            if (wsPublisher != null) {
                wsPublisher.publishRiskEvent(envelope);
            }
        }

        // Security alert dispatch on high contextual risk
        Double riskScore = payload.calculatedRiskScore();
        if (wsPublisher != null && ((riskScore != null && riskScore >= 70.0) || "HIGH".equalsIgnoreCase(payload.riskStatus()))) {
            SecurityAlertEvent alert = new SecurityAlertEvent(
                    "ALERT-" + UUID.randomUUID().toString().substring(0, 8),
                    "HIGH_RISK_CONTEXT",
                    "HIGH",
                    payload.deviceIdentifier(),
                    null,
                    payload.resource(),
                    payload.riskReason() != null ? payload.riskReason() : "Contextual risk elevated to " + riskScore,
                    "Access requests subjected to restrictive enforcement or denial.",
                    LocalDateTime.now().toString(),
                    envelope.correlationId()
            );
            EventEnvelope<SecurityAlertEvent> alertEnvelope = new EventEnvelope<>(
                    "EVT-ALERT-" + UUID.randomUUID().toString().substring(0, 8),
                    "SECURITY_ALERT",
                    LocalDateTime.now().toString(),
                    "RISK_ENGINE",
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

    public long getReceivedCount() {
        return receivedCount.get();
    }

    public long getProcessedEvaluations() {
        return processedEvaluations.get();
    }

    public void resetMetrics() {
        receivedCount.set(0);
        processedEvaluations.set(0);
    }
}
