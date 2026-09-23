package com.trustabac.iot.messaging.consumer;

import com.trustabac.iot.messaging.event.DeviceOperationEvent;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.websocket.WebSocketEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Asynchronous consumer for simulated device operation telemetry events.
 */
@Component
@ConditionalOnProperty(prefix = "trustabac.messaging", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceOperationEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeviceOperationEventConsumer.class);

    private final IdempotencyGuard idempotencyGuard;
    private final WebSocketEventPublisher wsPublisher;
    private final AtomicLong receivedCount = new AtomicLong(0);

    @Autowired
    public DeviceOperationEventConsumer(IdempotencyGuard idempotencyGuard,
                                        Optional<WebSocketEventPublisher> wsPublisher) {
        this.idempotencyGuard = idempotencyGuard;
        this.wsPublisher = wsPublisher.orElse(null);
    }

    public DeviceOperationEventConsumer(IdempotencyGuard idempotencyGuard,
                                        WebSocketEventPublisher wsPublisher) {
        this(idempotencyGuard, Optional.ofNullable(wsPublisher));
    }

    public DeviceOperationEventConsumer(IdempotencyGuard idempotencyGuard) {
        this(idempotencyGuard, Optional.empty());
    }

    @RabbitListener(queues = "${trustabac.messaging.device-event-queue:trustabac.device.events}")
    public void handleDeviceOperationEvent(EventEnvelope<DeviceOperationEvent> envelope) {
        if (!idempotencyGuard.tryAcquire(envelope.eventId())) {
            log.info("Ignoring duplicate DeviceOperationEvent [{}] (corrId: {})", envelope.eventId(), envelope.correlationId());
            return;
        }

        DeviceOperationEvent op = envelope.payload();
        log.info("Received DeviceOperationEvent [{}] on device='{}', op='{}', outcome='{}', status='{}' (corrId: {})",
                envelope.eventId(), op.deviceIdentifier(), op.requestedOperation(), op.decision(), op.enforcementStatus(), envelope.correlationId());

        receivedCount.incrementAndGet();

        // Observational broadcast to WebSocket subscribers
        if (wsPublisher != null) {
            wsPublisher.publishDeviceOperation(envelope);
        }
    }

    public long getReceivedCount() {
        return receivedCount.get();
    }

    public void resetMetrics() {
        receivedCount.set(0);
    }
}
