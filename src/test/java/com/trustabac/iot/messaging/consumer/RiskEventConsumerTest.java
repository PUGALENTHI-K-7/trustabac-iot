package com.trustabac.iot.messaging.consumer;

import com.trustabac.iot.dto.RiskEvaluationRequest;
import com.trustabac.iot.dto.RiskEvaluationResponse;
import com.trustabac.iot.dto.RiskFactorBreakdown;
import com.trustabac.iot.entity.RiskStatus;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.messaging.event.RiskDomainEvent;
import com.trustabac.iot.service.RiskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RiskEventConsumerTest {

    @Mock
    private RiskService riskService;

    private IdempotencyGuard idempotencyGuard;
    private RiskEventConsumer consumer;

    @BeforeEach
    void setUp() {
        idempotencyGuard = new InMemoryIdempotencyGuard();
        consumer = new RiskEventConsumer(riskService, idempotencyGuard);
    }

    @Test
    @DisplayName("Test inbound trigger RiskEvent invokes authoritative RiskService exactly once")
    void testInboundTriggerEventInvokesRiskService() {
        RiskFactorBreakdown factors = new RiskFactorBreakdown(0.0, 0.0, 0.35, 0.0, 1.0, 0.0, 0.0);
        RiskEvaluationResponse mockResp = new RiskEvaluationResponse("DOOR-SENSOR-001", 24.0, RiskStatus.LOW, factors, "Low risk", LocalDateTime.now());
        when(riskService.resolveAndEvaluate(any(RiskEvaluationRequest.class))).thenReturn(mockResp);

        RiskDomainEvent payload = new RiskDomainEvent(
                "DOOR-SENSOR-001",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "LOCAL_WIFI",
                2,
                0,
                "NORMAL",
                null, // null indicates inbound trigger
                null,
                null,
                LocalDateTime.now().toString(),
                "CORR-R-001"
        );
        EventEnvelope<RiskDomainEvent> envelope = new EventEnvelope<>(
                "EVT-RISK-001", "RISK_EVENT", LocalDateTime.now().toString(),
                "CONTEXT_SENSOR", "DOOR-SENSOR-001", "P-01", "B-01", "REF-R-001", "CORR-R-001", payload, "1.0"
        );

        consumer.handleRiskEvent(envelope);

        assertEquals(1, consumer.getReceivedCount());
        assertEquals(1, consumer.getProcessedEvaluations());
        verify(riskService, times(1)).resolveAndEvaluate(any(RiskEvaluationRequest.class));
    }

    @Test
    @DisplayName("Test duplicate RiskEvent is suppressed by IdempotencyGuard (no double-evaluation)")
    void testDuplicateRiskEventSuppressed() {
        RiskFactorBreakdown factors = new RiskFactorBreakdown(0.0, 0.0, 0.35, 0.0, 1.0, 0.0, 0.0);
        RiskEvaluationResponse mockResp = new RiskEvaluationResponse("DOOR-SENSOR-001", 24.0, RiskStatus.LOW, factors, "Low risk", LocalDateTime.now());
        when(riskService.resolveAndEvaluate(any(RiskEvaluationRequest.class))).thenReturn(mockResp);

        RiskDomainEvent payload = new RiskDomainEvent(
                "DOOR-SENSOR-001",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "LOCAL_WIFI",
                2,
                0,
                "NORMAL",
                null,
                null,
                null,
                LocalDateTime.now().toString(),
                "CORR-R-002"
        );
        EventEnvelope<RiskDomainEvent> envelope = new EventEnvelope<>(
                "EVT-RISK-DUP-001", "RISK_EVENT", LocalDateTime.now().toString(),
                "CONTEXT_SENSOR", "DOOR-SENSOR-001", "P-01", "B-01", "REF-R-002", "CORR-R-002", payload, "1.0"
        );

        consumer.handleRiskEvent(envelope);
        consumer.handleRiskEvent(envelope);

        assertEquals(1, consumer.getReceivedCount());
        assertEquals(1, consumer.getProcessedEvaluations());
        verify(riskService, times(1)).resolveAndEvaluate(any(RiskEvaluationRequest.class));
    }

    @Test
    @DisplayName("Test audit broadcast RiskEvent (calculatedRiskScore != null) does not re-invoke RiskService")
    void testAuditBroadcastDoesNotReInvokeRiskService() {
        RiskDomainEvent auditPayload = new RiskDomainEvent(
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
                "Standard access",
                LocalDateTime.now().toString(),
                "CORR-R-003"
        );
        EventEnvelope<RiskDomainEvent> envelope = new EventEnvelope<>(
                "EVT-RISK-AUDIT-001", "RISK_AUDIT", LocalDateTime.now().toString(),
                "CONTEXTUAL_RISK_ENGINE", "DOOR-SENSOR-001", "P-01", "B-01", "REF-R-003", "CORR-R-003", auditPayload, "1.0"
        );

        consumer.handleRiskEvent(envelope);

        assertEquals(1, consumer.getReceivedCount());
        assertEquals(0, consumer.getProcessedEvaluations());
        verify(riskService, never()).resolveAndEvaluate(any(RiskEvaluationRequest.class));
    }
}
