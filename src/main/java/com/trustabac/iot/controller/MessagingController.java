package com.trustabac.iot.controller;

import com.trustabac.iot.config.MessagingProperties;
import com.trustabac.iot.messaging.consumer.AuthorizationEventConsumer;
import com.trustabac.iot.messaging.consumer.DeadLetterQueueConsumer;
import com.trustabac.iot.messaging.consumer.DeviceOperationEventConsumer;
import com.trustabac.iot.messaging.consumer.IdempotencyGuard;
import com.trustabac.iot.messaging.consumer.RiskEventConsumer;
import com.trustabac.iot.messaging.consumer.TrustEventConsumer;
import com.trustabac.iot.messaging.publisher.IoTEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Controller exposing health, configuration topology, and metrics for the RabbitMQ event pipeline.
 */
@RestController
@RequestMapping("/api/messaging")
public class MessagingController {

    private static final Logger log = LoggerFactory.getLogger(MessagingController.class);

    private final MessagingProperties properties;
    private final IoTEventPublisher eventPublisher;
    private final DeviceOperationEventConsumer deviceConsumer;
    private final TrustEventConsumer trustConsumer;
    private final RiskEventConsumer riskConsumer;
    private final AuthorizationEventConsumer authConsumer;
    private final DeadLetterQueueConsumer dlqConsumer;
    private final IdempotencyGuard idempotencyGuard;
    private final ConnectionFactory connectionFactory;

    @org.springframework.beans.factory.annotation.Autowired
    public MessagingController(MessagingProperties properties,
                               Optional<IoTEventPublisher> eventPublisher,
                               Optional<DeviceOperationEventConsumer> deviceConsumer,
                               Optional<TrustEventConsumer> trustConsumer,
                               Optional<RiskEventConsumer> riskConsumer,
                               Optional<AuthorizationEventConsumer> authConsumer,
                               Optional<DeadLetterQueueConsumer> dlqConsumer,
                               Optional<IdempotencyGuard> idempotencyGuard,
                               Optional<ConnectionFactory> connectionFactory) {
        this.properties = properties;
        this.eventPublisher = eventPublisher.orElse(null);
        this.deviceConsumer = deviceConsumer.orElse(null);
        this.trustConsumer = trustConsumer.orElse(null);
        this.riskConsumer = riskConsumer.orElse(null);
        this.authConsumer = authConsumer.orElse(null);
        this.dlqConsumer = dlqConsumer.orElse(null);
        this.idempotencyGuard = idempotencyGuard.orElse(null);
        this.connectionFactory = connectionFactory.orElse(null);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMessagingStatus() {
        boolean brokerReachable = false;
        if (connectionFactory != null) {
            try (var conn = connectionFactory.createConnection()) {
                brokerReachable = conn.isOpen();
            } catch (Exception e) {
                log.debug("RabbitMQ ping check error: {}", e.getMessage());
            }
        }

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("brokerReachable", brokerReachable);
        status.put("messagingEnabled", properties.isEnabled());
        status.put("exchange", properties.getExchange());

        Map<String, String> queues = new LinkedHashMap<>();
        queues.put("deviceEvents", properties.getDeviceEventQueue());
        queues.put("trustEvents", properties.getTrustEventQueue());
        queues.put("riskEvents", properties.getRiskEventQueue());
        queues.put("authorizationEvents", properties.getAuthorizationEventQueue());
        queues.put("deadLetterQueue", properties.getDlqQueue());
        status.put("queues", queues);

        Map<String, String> routingKeys = new LinkedHashMap<>();
        routingKeys.put("deviceOperation", properties.getRoutingKeyDeviceOperation());
        routingKeys.put("trustEvent", properties.getRoutingKeyTrustEvent());
        routingKeys.put("riskEvent", properties.getRoutingKeyRiskEvent());
        routingKeys.put("authorizationResult", properties.getRoutingKeyAuthorizationResult());
        routingKeys.put("dlq", properties.getRoutingKeyDlq());
        status.put("routingKeys", routingKeys);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("totalPublishedEvents", eventPublisher != null ? eventPublisher.getTotalPublished() : 0);
        metrics.put("totalFailedPublishAttempts", eventPublisher != null ? eventPublisher.getTotalFailed() : 0);
        metrics.put("deviceEventsReceived", deviceConsumer != null ? deviceConsumer.getReceivedCount() : 0);
        metrics.put("trustEventsReceived", trustConsumer != null ? trustConsumer.getReceivedCount() : 0);
        metrics.put("trustMutationsProcessed", trustConsumer != null ? trustConsumer.getProcessedMutations() : 0);
        metrics.put("riskEventsReceived", riskConsumer != null ? riskConsumer.getReceivedCount() : 0);
        metrics.put("riskEvaluationsProcessed", riskConsumer != null ? riskConsumer.getProcessedEvaluations() : 0);
        metrics.put("authorizationEventsReceived", authConsumer != null ? authConsumer.getReceivedCount() : 0);
        metrics.put("dlqMessagesReceived", dlqConsumer != null ? dlqConsumer.getDlqMessageCount() : 0);
        metrics.put("idempotencyTrackedEvents", idempotencyGuard != null ? idempotencyGuard.size() : 0);
        status.put("metrics", metrics);

        status.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(status);
    }
}
