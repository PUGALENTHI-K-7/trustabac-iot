package com.trustabac.iot.websocket;

import com.trustabac.iot.config.WebSocketProperties;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketEventPublisherTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private WebSocketProperties properties;
    private WebSocketSessionTracker sessionTracker;
    private WebSocketEventPublisher publisher;

    @BeforeEach
    void setUp() {
        properties = new WebSocketProperties();
        properties.setEnabled(true);
        sessionTracker = new WebSocketSessionTracker();
        publisher = new WebSocketEventPublisher(messagingTemplate, properties, sessionTracker);
    }

    @Test
    @DisplayName("Verify publishDeviceOperation broadcasts to /topic/devices")
    void testPublishDeviceOperation() {
        DeviceOperationEvent payload = new DeviceOperationEvent(
                "DOOR-01", "SMART_DOOR_LOCK", "READ", "READ",
                Decision.ALLOW, EnforcementStatus.EXECUTED, 80.0, 15.0, "PASS", "Success",
                LocalDateTime.now().toString(), "CORR-01"
        );
        EventEnvelope<DeviceOperationEvent> env = EventEnvelope.of("DEVICE_OP", "TEST", "DOOR-01", "P-1", "B-1", "REF-1", "CORR-01", payload);

        boolean sent = publisher.publishDeviceOperation(env);
        assertTrue(sent);
        verify(messagingTemplate, times(1)).convertAndSend(eq(WebSocketEventPublisher.TOPIC_DEVICES), eq(env));
        assertEquals(1, sessionTracker.getMessagesPublished());
    }

    @Test
    @DisplayName("Verify publishTrustEvent broadcasts to /topic/trust")
    void testPublishTrustEvent() {
        TrustDomainEvent payload = new TrustDomainEvent(
                "DOOR-01", TrustEventType.NORMAL_SUCCESS, 75.0, 80.0, 5.0, "Routine", "TEST",
                LocalDateTime.now().toString(), "CORR-02"
        );
        EventEnvelope<TrustDomainEvent> env = EventEnvelope.of("TRUST_EVT", "TEST", "DOOR-01", "P-1", "B-1", "REF-2", "CORR-02", payload);

        boolean sent = publisher.publishTrustEvent(env);
        assertTrue(sent);
        verify(messagingTemplate, times(1)).convertAndSend(eq(WebSocketEventPublisher.TOPIC_TRUST), eq(env));
        assertEquals(1, sessionTracker.getMessagesPublished());
    }

    @Test
    @DisplayName("Verify publishRiskEvent broadcasts to /topic/risk")
    void testPublishRiskEvent() {
        RiskDomainEvent payload = new RiskDomainEvent(
                "DOOR-01", "SMART_DOOR_LOCK", "READ", "L-1", "LOCAL_WIFI", 1, 0,
                "NORMAL", 15.0, "LOW_RISK", "Normal context",
                LocalDateTime.now().toString(), "CORR-03"
        );
        EventEnvelope<RiskDomainEvent> env = EventEnvelope.of("RISK_EVT", "TEST", "DOOR-01", "P-1", "B-1", "REF-3", "CORR-03", payload);

        boolean sent = publisher.publishRiskEvent(env);
        assertTrue(sent);
        verify(messagingTemplate, times(1)).convertAndSend(eq(WebSocketEventPublisher.TOPIC_RISK), eq(env));
        assertEquals(1, sessionTracker.getMessagesPublished());
    }

    @Test
    @DisplayName("Verify publishAuthorizationResult broadcasts to /topic/authorization")
    void testPublishAuthorizationResult() {
        AuthorizationResultEvent payload = new AuthorizationResultEvent(
                "REF-4", "CORR-04", "DOOR-01", "SMART_DOOR_LOCK", "READ",
                "PASS", 85.0, 10.0, Decision.ALLOW, EnforcementStatus.EXECUTED, "READ",
                "0xabc", 123L, LocalDateTime.now().toString()
        );
        EventEnvelope<AuthorizationResultEvent> env = EventEnvelope.of("AUTH_EVT", "TEST", "DOOR-01", "P-1", "B-1", "REF-4", "CORR-04", payload);

        boolean sent = publisher.publishAuthorizationResult(env);
        assertTrue(sent);
        verify(messagingTemplate, times(1)).convertAndSend(eq(WebSocketEventPublisher.TOPIC_AUTHORIZATION), eq(env));
        assertEquals(1, sessionTracker.getMessagesPublished());
    }

    @Test
    @DisplayName("Verify publishBlockchainResult broadcasts to /topic/blockchain")
    void testPublishBlockchainResult() {
        BlockchainResultEvent payload = new BlockchainResultEvent(
                "0x123abc", 42L, "0xcontract", Decision.ALLOW, "DOOR-01", "SMART_DOOR_LOCK", "CONTROL",
                LocalDateTime.now().toString(), "CORR-05"
        );
        EventEnvelope<BlockchainResultEvent> env = EventEnvelope.of("BC_EVT", "TEST", "DOOR-01", "P-1", "B-1", "REF-5", "CORR-05", payload);

        boolean sent = publisher.publishBlockchainResult(env);
        assertTrue(sent);
        verify(messagingTemplate, times(1)).convertAndSend(eq(WebSocketEventPublisher.TOPIC_BLOCKCHAIN), eq(env));
        assertEquals(1, sessionTracker.getMessagesPublished());
    }

    @Test
    @DisplayName("Verify publishSimulatorStep broadcasts to /topic/simulator")
    void testPublishSimulatorStep() {
        SimulatorStepEvent payload = new SimulatorStepEvent(
                "EVT-SIM-01", "NORMAL_STAY", 1, "DOOR-01", "SMART_DOOR_LOCK", "READ",
                "ALLOW", "EXECUTED", 80.0, 10.0, "PASS", null, null, "OK",
                LocalDateTime.now().toString(), "CORR-06"
        );
        EventEnvelope<SimulatorStepEvent> env = EventEnvelope.of("SIM_STEP", "TEST", "DOOR-01", "P-1", "B-1", "REF-6", "CORR-06", payload);

        boolean sent = publisher.publishSimulatorStep(env);
        assertTrue(sent);
        verify(messagingTemplate, times(1)).convertAndSend(eq(WebSocketEventPublisher.TOPIC_SIMULATOR), eq(env));
        assertEquals(1, sessionTracker.getMessagesPublished());
    }

    @Test
    @DisplayName("Verify publishSecurityAlert broadcasts to /topic/security")
    void testPublishSecurityAlert() {
        SecurityAlertEvent payload = new SecurityAlertEvent(
                "ALERT-01", "LOW_TRUST_ALERT", "CRITICAL",
                "DOOR-01", "GUEST-01", "SMART_DOOR_LOCK",
                "Trust score dropped below critical threshold",
                "Lockout triggered",
                LocalDateTime.now().toString(), "CORR-07"
        );
        EventEnvelope<SecurityAlertEvent> env = EventEnvelope.of("SEC_ALERT", "TEST", "DOOR-01", "P-1", "B-1", "REF-7", "CORR-07", payload);

        boolean sent = publisher.publishSecurityAlert(env);
        assertTrue(sent);
        verify(messagingTemplate, times(1)).convertAndSend(eq(WebSocketEventPublisher.TOPIC_SECURITY), eq(env));
        assertEquals(1, sessionTracker.getMessagesPublished());
    }

    @Test
    @DisplayName("Verify publisher gracefully handles exception and increments failure counter without throwing")
    void testPublishFailureIsolation() {
        doThrow(new RuntimeException("Simulated broker disconnect"))
                .when(messagingTemplate).convertAndSend(anyString(), any(Object.class));

        SimulatorStepEvent payload = new SimulatorStepEvent(
                "EVT-SIM-02", "NORMAL_STAY", 1, "DOOR-01", "SMART_DOOR_LOCK", "READ",
                "ALLOW", "EXECUTED", 80.0, 10.0, "PASS", null, null, "OK",
                LocalDateTime.now().toString(), "CORR-08"
        );
        EventEnvelope<SimulatorStepEvent> env = EventEnvelope.of("SIM_STEP", "TEST", "DOOR-01", "P-1", "B-1", "REF-8", "CORR-08", payload);

        boolean sent = assertDoesNotThrow(() -> publisher.publishSimulatorStep(env));
        assertFalse(sent);
        assertEquals(0, sessionTracker.getMessagesPublished());
        assertEquals(1, sessionTracker.getPublishFailures());
    }

    @Test
    @DisplayName("Verify publisher skips sending when disabled in properties")
    void testDisabledPublisher() {
        properties.setEnabled(false);

        SimulatorStepEvent payload = new SimulatorStepEvent(
                "EVT-SIM-03", "NORMAL_STAY", 1, "DOOR-01", "SMART_DOOR_LOCK", "READ",
                "ALLOW", "EXECUTED", 80.0, 10.0, "PASS", null, null, "OK",
                LocalDateTime.now().toString(), "CORR-09"
        );
        EventEnvelope<SimulatorStepEvent> env = EventEnvelope.of("SIM_STEP", "TEST", "DOOR-01", "P-1", "B-1", "REF-9", "CORR-09", payload);

        boolean sent = publisher.publishSimulatorStep(env);
        assertFalse(sent);
        verifyNoInteractions(messagingTemplate);
    }
}
