package com.trustabac.iot.messaging.publisher;

import com.trustabac.iot.config.MessagingProperties;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.entity.TrustEventType;
import com.trustabac.iot.messaging.event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IoTEventPublisherTest {

    @Mock
    private AmqpTemplate amqpTemplate;

    private MessagingProperties properties;
    private IoTEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new MessagingProperties();
        properties.setEnabled(true);
        publisher = new IoTEventPublisher(amqpTemplate, properties);
    }

    @Test
    @DisplayName("Test publishing DeviceOperationEvent to topic exchange with correct routing key")
    void testPublishDeviceOperation() {
        DeviceOperationEvent payload = new DeviceOperationEvent(
                "DOOR-SENSOR-001",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "CONTROL",
                Decision.ALLOW,
                EnforcementStatus.EXECUTED,
                85.0,
                20.0,
                "PASS",
                "Success",
                LocalDateTime.now().toString(),
                "CORR-001"
        );
        EventEnvelope<DeviceOperationEvent> env = EventEnvelope.of("DEVICE_OPERATION", "TEST", "DOOR-SENSOR-001", "P-01", "B-01", "REF-01", "CORR-001", payload);

        boolean sent = publisher.publishDeviceOperation(env);

        assertTrue(sent);
        assertEquals(1, publisher.getTotalPublished());
        assertEquals(0, publisher.getTotalFailed());
        verify(amqpTemplate, times(1)).convertAndSend(eq("trustabac.events"), eq("device.operation"), eq(env));
    }

    @Test
    @DisplayName("Test publishing TrustDomainEvent to topic exchange with correct routing key")
    void testPublishTrustEvent() {
        TrustDomainEvent payload = new TrustDomainEvent(
                "DOOR-SENSOR-001",
                TrustEventType.SUSPICIOUS_ACTIVITY,
                80.0,
                70.0,
                -10.0,
                "Suspicious access",
                "IDS",
                LocalDateTime.now().toString(),
                "CORR-002"
        );
        EventEnvelope<TrustDomainEvent> env = EventEnvelope.of("TRUST_EVENT", "TEST", "DOOR-SENSOR-001", "P-01", "B-01", "REF-02", "CORR-002", payload);

        boolean sent = publisher.publishTrustEvent(env);

        assertTrue(sent);
        verify(amqpTemplate, times(1)).convertAndSend(eq("trustabac.events"), eq("trust.event"), eq(env));
    }

    @Test
    @DisplayName("Test publishing RiskDomainEvent to topic exchange with correct routing key")
    void testPublishRiskEvent() {
        RiskDomainEvent payload = new RiskDomainEvent(
                "DOOR-SENSOR-001",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "LOCAL_WIFI",
                2,
                0,
                "NORMAL",
                24.0,
                "LOW",
                "Low risk",
                LocalDateTime.now().toString(),
                "CORR-003"
        );
        EventEnvelope<RiskDomainEvent> env = EventEnvelope.of("RISK_EVENT", "TEST", "DOOR-SENSOR-001", "P-01", "B-01", "REF-03", "CORR-003", payload);

        boolean sent = publisher.publishRiskEvent(env);

        assertTrue(sent);
        verify(amqpTemplate, times(1)).convertAndSend(eq("trustabac.events"), eq("risk.context"), eq(env));
    }

    @Test
    @DisplayName("Test publishing AuthorizationResultEvent to topic exchange with correct routing key")
    void testPublishAuthorizationResult() {
        AuthorizationResultEvent payload = new AuthorizationResultEvent(
                "REF-004",
                "CORR-004",
                "DOOR-SENSOR-001",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "PASS",
                80.0,
                24.0,
                Decision.ALLOW,
                EnforcementStatus.EXECUTED,
                "CONTROL",
                "0x123abc",
                12L,
                LocalDateTime.now().toString()
        );
        EventEnvelope<AuthorizationResultEvent> env = EventEnvelope.of("AUTHORIZATION_RESULT", "TEST", "DOOR-SENSOR-001", "P-01", "B-01", "REF-04", "CORR-004", payload);

        boolean sent = publisher.publishAuthorizationResult(env);

        assertTrue(sent);
        verify(amqpTemplate, times(1)).convertAndSend(eq("trustabac.events"), eq("authorization.result"), eq(env));
    }

    @Test
    @DisplayName("Test publisher fault tolerance: RabbitMQ AmqpException handled gracefully without throwing")
    void testPublisherFaultTolerance() {
        doThrow(new AmqpException("Broker unreachable"))
                .when(amqpTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        DeviceOperationEvent payload = new DeviceOperationEvent(
                "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", "CONTROL",
                Decision.ALLOW, EnforcementStatus.EXECUTED, 80.0, 20.0, "PASS", "OK", LocalDateTime.now().toString(), "CORR-005"
        );
        EventEnvelope<DeviceOperationEvent> env = EventEnvelope.of("DEVICE_OPERATION", "TEST", "DOOR-SENSOR-001", "P-01", "B-01", "REF-05", "CORR-005", payload);

        boolean sent = publisher.publishDeviceOperation(env);

        assertFalse(sent);
        assertEquals(0, publisher.getTotalPublished());
        assertEquals(1, publisher.getTotalFailed());
    }
}
