package com.trustabac.iot.messaging.consumer;

import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.messaging.event.DeviceOperationEvent;
import com.trustabac.iot.messaging.event.EventEnvelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeviceOperationEventConsumerTest {

    private IdempotencyGuard idempotencyGuard;
    private DeviceOperationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        idempotencyGuard = new InMemoryIdempotencyGuard();
        consumer = new DeviceOperationEventConsumer(idempotencyGuard);
    }

    @Test
    @DisplayName("Test DeviceOperationEventConsumer receives event and ignores duplicate")
    void testReceiveAndIdempotency() {
        DeviceOperationEvent payload = new DeviceOperationEvent(
                "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", "CONTROL",
                Decision.ALLOW, EnforcementStatus.EXECUTED, 85.0, 20.0, "PASS", "OK", LocalDateTime.now().toString(), "CORR-D-01"
        );
        EventEnvelope<DeviceOperationEvent> env = new EventEnvelope<>(
                "EVT-DEV-001", "DEVICE_OPERATION", LocalDateTime.now().toString(),
                "SIMULATOR", "DOOR-SENSOR-001", "P-01", "B-01", "REF-D-01", "CORR-D-01", payload, "1.0"
        );

        consumer.handleDeviceOperationEvent(env);
        // Duplicate delivery
        consumer.handleDeviceOperationEvent(env);

        assertEquals(1, consumer.getReceivedCount());
    }
}
