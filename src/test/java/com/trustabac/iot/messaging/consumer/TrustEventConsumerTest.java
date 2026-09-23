package com.trustabac.iot.messaging.consumer;

import com.trustabac.iot.dto.TrustEventRequest;
import com.trustabac.iot.dto.TrustUpdateResponse;
import com.trustabac.iot.entity.TrustEventType;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.messaging.event.TrustDomainEvent;
import com.trustabac.iot.service.TrustService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrustEventConsumerTest {

    @Mock
    private TrustService trustService;

    private IdempotencyGuard idempotencyGuard;
    private TrustEventConsumer consumer;

    @BeforeEach
    void setUp() {
        idempotencyGuard = new InMemoryIdempotencyGuard();
        consumer = new TrustEventConsumer(trustService, idempotencyGuard);
    }

    @Test
    @DisplayName("Test inbound trigger TrustEvent invokes authoritative TrustService exactly once")
    void testInboundTriggerEventInvokesTrustService() {
        when(trustService.recordTrustEvent(eq("DOOR-SENSOR-001"), any(TrustEventRequest.class)))
                .thenReturn(new TrustUpdateResponse("DOOR-SENSOR-001", 80.0, 70.0, -10.0,
                        TrustEventType.SUSPICIOUS_ACTIVITY, "Suspicious pattern", "EXTERNAL_IDS", LocalDateTime.now(), "Updated"));

        TrustDomainEvent payload = new TrustDomainEvent(
                "DOOR-SENSOR-001",
                TrustEventType.SUSPICIOUS_ACTIVITY,
                null, // null indicates inbound trigger
                null,
                null,
                "Suspicious pattern",
                "EXTERNAL_IDS",
                LocalDateTime.now().toString(),
                "CORR-T-001"
        );
        EventEnvelope<TrustDomainEvent> envelope = new EventEnvelope<>(
                "EVT-TRUST-001", "TRUST_EVENT", LocalDateTime.now().toString(),
                "EXTERNAL_IDS", "DOOR-SENSOR-001", "P-01", "B-01", "REF-T-001", "CORR-T-001", payload, "1.0"
        );

        consumer.handleTrustEvent(envelope);

        assertEquals(1, consumer.getReceivedCount());
        assertEquals(1, consumer.getProcessedMutations());
        verify(trustService, times(1)).recordTrustEvent(eq("DOOR-SENSOR-001"), any(TrustEventRequest.class));
    }

    @Test
    @DisplayName("Test duplicate TrustEvent is suppressed by IdempotencyGuard (no double-mutation)")
    void testDuplicateTrustEventSuppressed() {
        when(trustService.recordTrustEvent(eq("DOOR-SENSOR-001"), any(TrustEventRequest.class)))
                .thenReturn(new TrustUpdateResponse("DOOR-SENSOR-001", 80.0, 70.0, -10.0,
                        TrustEventType.SUSPICIOUS_ACTIVITY, "Suspicious pattern", "EXTERNAL_IDS", LocalDateTime.now(), "Updated"));

        TrustDomainEvent payload = new TrustDomainEvent(
                "DOOR-SENSOR-001",
                TrustEventType.SUSPICIOUS_ACTIVITY,
                null,
                null,
                null,
                "Suspicious pattern",
                "EXTERNAL_IDS",
                LocalDateTime.now().toString(),
                "CORR-T-002"
        );
        EventEnvelope<TrustDomainEvent> envelope = new EventEnvelope<>(
                "EVT-TRUST-DUP-001", "TRUST_EVENT", LocalDateTime.now().toString(),
                "EXTERNAL_IDS", "DOOR-SENSOR-001", "P-01", "B-01", "REF-T-002", "CORR-T-002", payload, "1.0"
        );

        // First delivery
        consumer.handleTrustEvent(envelope);
        // Duplicate delivery with same eventId
        consumer.handleTrustEvent(envelope);

        // TrustService must only be invoked ONCE
        assertEquals(1, consumer.getReceivedCount());
        assertEquals(1, consumer.getProcessedMutations());
        verify(trustService, times(1)).recordTrustEvent(eq("DOOR-SENSOR-001"), any(TrustEventRequest.class));
    }

    @Test
    @DisplayName("Test audit broadcast TrustEvent (newTrust != null) does not re-invoke TrustService")
    void testAuditBroadcastDoesNotReInvokeTrustService() {
        TrustDomainEvent auditPayload = new TrustDomainEvent(
                "DOOR-SENSOR-001",
                TrustEventType.RECOVERY,
                40.0,
                50.0,
                10.0,
                "Admin recovery",
                "SIMULATOR",
                LocalDateTime.now().toString(),
                "CORR-T-003"
        );
        EventEnvelope<TrustDomainEvent> envelope = new EventEnvelope<>(
                "EVT-TRUST-AUDIT-001", "TRUST_UPDATE_AUDIT", LocalDateTime.now().toString(),
                "SIMULATOR", "DOOR-SENSOR-001", "P-01", "B-01", "REF-T-003", "CORR-T-003", auditPayload, "1.0"
        );

        consumer.handleTrustEvent(envelope);

        assertEquals(1, consumer.getReceivedCount());
        assertEquals(0, consumer.getProcessedMutations());
        verify(trustService, never()).recordTrustEvent(anyString(), any(TrustEventRequest.class));
    }
}
