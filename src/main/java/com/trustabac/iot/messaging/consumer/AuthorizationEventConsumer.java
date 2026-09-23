package com.trustabac.iot.messaging.consumer;

import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.messaging.event.AuthorizationResultEvent;
import com.trustabac.iot.messaging.event.BlockchainResultEvent;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.messaging.event.SecurityAlertEvent;
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
 * Asynchronous consumer for authorization outcome events and blockchain proofs.
 */
@Component
@ConditionalOnProperty(prefix = "trustabac.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AuthorizationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationEventConsumer.class);

    private final IdempotencyGuard idempotencyGuard;
    private final WebSocketEventPublisher wsPublisher;
    private final AtomicLong receivedCount = new AtomicLong(0);
    private final AtomicLong blockchainEventCount = new AtomicLong(0);

    @Autowired
    public AuthorizationEventConsumer(IdempotencyGuard idempotencyGuard,
                                      Optional<WebSocketEventPublisher> wsPublisher) {
        this.idempotencyGuard = idempotencyGuard;
        this.wsPublisher = wsPublisher.orElse(null);
    }

    public AuthorizationEventConsumer(IdempotencyGuard idempotencyGuard,
                                      WebSocketEventPublisher wsPublisher) {
        this(idempotencyGuard, Optional.ofNullable(wsPublisher));
    }

    public AuthorizationEventConsumer(IdempotencyGuard idempotencyGuard) {
        this(idempotencyGuard, Optional.empty());
    }

    @RabbitListener(queues = "${trustabac.messaging.authorization-event-queue:trustabac.authorization.events}")
    public void handleAuthorizationResultEvent(EventEnvelope<AuthorizationResultEvent> envelope) {
        if (!idempotencyGuard.tryAcquire(envelope.eventId())) {
            log.info("Ignoring duplicate AuthorizationResultEvent [{}] (corrId: {})", envelope.eventId(), envelope.correlationId());
            return;
        }

        AuthorizationResultEvent auth = envelope.payload();
        log.info("Received AuthorizationResultEvent [{}] on device='{}', op='{}', decision='{}', status='{}', tx='{}' (corrId: {})",
                envelope.eventId(), auth.deviceIdentifier(), auth.operation(), auth.decision(), auth.enforcementStatus(), auth.blockchainTxHash(), envelope.correlationId());

        receivedCount.incrementAndGet();

        if (wsPublisher != null) {
            // 1. Broadcast to /topic/authorization
            wsPublisher.publishAuthorizationResult(envelope);

            // 2. Broadcast security alert if authorization was DENIED or BLOCKED
            if (auth.decision() == Decision.DENY || auth.enforcementStatus() == EnforcementStatus.BLOCKED) {
                SecurityAlertEvent alert = new SecurityAlertEvent(
                        "ALERT-" + UUID.randomUUID().toString().substring(0, 8),
                        "ACCESS_DENIED_ALERT",
                        "MEDIUM",
                        auth.deviceIdentifier(),
                        envelope.source(),
                        auth.resource(),
                        "Access denied by security policies: " + auth.abacResult(),
                        "Operation execution suppressed.",
                        LocalDateTime.now().toString(),
                        envelope.correlationId()
                );
                EventEnvelope<SecurityAlertEvent> alertEnvelope = new EventEnvelope<>(
                        "EVT-ALERT-" + UUID.randomUUID().toString().substring(0, 8),
                        "SECURITY_ALERT",
                        LocalDateTime.now().toString(),
                        "AUTHORIZATION_ENGINE",
                        auth.deviceIdentifier(),
                        envelope.propertyId(),
                        envelope.bookingId(),
                        envelope.requestReference(),
                        envelope.correlationId(),
                        alert,
                        "1.0"
                );
                wsPublisher.publishSecurityAlert(alertEnvelope);
            }

            // 3. Broadcast to /topic/blockchain if blockchain transaction hash is present
            if (auth.blockchainTxHash() != null && !auth.blockchainTxHash().isBlank()) {
                BlockchainResultEvent bcPayload = new BlockchainResultEvent(
                        auth.blockchainTxHash(),
                        auth.blockchainBlockNumber(),
                        "0xe78A0F7E598Cc8b0Bb87894B0F60dD2a88d6a8Ab",
                        auth.decision(),
                        auth.deviceIdentifier(),
                        auth.resource(),
                        auth.operation(),
                        auth.timestamp(),
                        envelope.correlationId()
                );
                EventEnvelope<BlockchainResultEvent> bcEnvelope = new EventEnvelope<>(
                        "EVT-BC-" + UUID.randomUUID().toString().substring(0, 8),
                        "BLOCKCHAIN_CONFIRMATION",
                        LocalDateTime.now().toString(),
                        "BLOCKCHAIN_ENGINE",
                        auth.deviceIdentifier(),
                        envelope.propertyId(),
                        envelope.bookingId(),
                        envelope.requestReference(),
                        envelope.correlationId(),
                        bcPayload,
                        "1.0"
                );
                wsPublisher.publishBlockchainResult(bcEnvelope);
            }
        }
    }

    public void handleBlockchainResultEvent(EventEnvelope<BlockchainResultEvent> envelope) {
        if (!idempotencyGuard.tryAcquire(envelope.eventId())) {
            log.info("Ignoring duplicate BlockchainResultEvent [{}] (corrId: {})", envelope.eventId(), envelope.correlationId());
            return;
        }

        blockchainEventCount.incrementAndGet();
        if (wsPublisher != null) {
            wsPublisher.publishBlockchainResult(envelope);
        }
    }

    public long getReceivedCount() {
        return receivedCount.get();
    }

    public long getBlockchainEventCount() {
        return blockchainEventCount.get();
    }

    public void resetMetrics() {
        receivedCount.set(0);
        blockchainEventCount.set(0);
    }
}
