package com.trustabac.iot.messaging.publisher;

import com.trustabac.iot.config.MessagingProperties;
import com.trustabac.iot.messaging.event.AuthorizationResultEvent;
import com.trustabac.iot.messaging.event.BlockchainResultEvent;
import com.trustabac.iot.messaging.event.DeviceOperationEvent;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.messaging.event.RiskDomainEvent;
import com.trustabac.iot.messaging.event.TrustDomainEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Publisher service for dispatching typed IoT telemetry, trust, risk, and authorization events
 * to RabbitMQ topic exchanges.
 */
@Service
public class IoTEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(IoTEventPublisher.class);

    private final AmqpTemplate amqpTemplate;
    private final MessagingProperties properties;

    private final AtomicLong totalPublished = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private volatile String lastPublishedTimestamp;

    @org.springframework.beans.factory.annotation.Autowired
    public IoTEventPublisher(Optional<AmqpTemplate> amqpTemplate, MessagingProperties properties) {
        this.amqpTemplate = amqpTemplate.orElse(null);
        this.properties = properties;
    }

    public IoTEventPublisher(AmqpTemplate amqpTemplate, MessagingProperties properties) {
        this(Optional.ofNullable(amqpTemplate), properties);
    }

    public boolean publishDeviceOperation(EventEnvelope<DeviceOperationEvent> envelope) {
        return sendEvent(properties.getRoutingKeyDeviceOperation(), envelope);
    }

    public boolean publishTrustEvent(EventEnvelope<TrustDomainEvent> envelope) {
        return sendEvent(properties.getRoutingKeyTrustEvent(), envelope);
    }

    public boolean publishRiskEvent(EventEnvelope<RiskDomainEvent> envelope) {
        return sendEvent(properties.getRoutingKeyRiskEvent(), envelope);
    }

    public boolean publishAuthorizationResult(EventEnvelope<AuthorizationResultEvent> envelope) {
        return sendEvent(properties.getRoutingKeyAuthorizationResult(), envelope);
    }

    public boolean publishBlockchainResult(EventEnvelope<BlockchainResultEvent> envelope) {
        return sendEvent(properties.getRoutingKeyAuthorizationResult(), envelope);
    }

    private boolean sendEvent(String routingKey, EventEnvelope<?> envelope) {
        if (amqpTemplate == null || !properties.isEnabled()) {
            log.debug("Messaging disabled or AmqpTemplate unavailable: skipping event dispatch for [{}]", envelope.eventId());
            return false;
        }

        try {
            log.debug("Publishing event [{}] to exchange '{}' with routingKey '{}' (corrId: {})",
                    envelope.eventId(), properties.getExchange(), routingKey, envelope.correlationId());

            amqpTemplate.convertAndSend(properties.getExchange(), routingKey, envelope);
            totalPublished.incrementAndGet();
            lastPublishedTimestamp = LocalDateTime.now().toString();
            return true;
        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.warn("Failed to publish event [{}] to RabbitMQ: {}. Synchronous authorization unaffected.",
                    envelope.eventId(), e.getMessage());
            return false;
        }
    }

    public long getTotalPublished() {
        return totalPublished.get();
    }

    public long getTotalFailed() {
        return totalFailed.get();
    }

    public String getLastPublishedTimestamp() {
        return lastPublishedTimestamp;
    }

    public void resetMetrics() {
        totalPublished.set(0);
        totalFailed.set(0);
        lastPublishedTimestamp = null;
    }
}
