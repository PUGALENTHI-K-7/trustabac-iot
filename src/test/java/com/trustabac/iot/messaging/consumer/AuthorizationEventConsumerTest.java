package com.trustabac.iot.messaging.consumer;

import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.messaging.event.AuthorizationResultEvent;
import com.trustabac.iot.messaging.event.BlockchainResultEvent;
import com.trustabac.iot.messaging.event.EventEnvelope;
import com.trustabac.iot.websocket.WebSocketEventPublisher;
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
class AuthorizationEventConsumerTest {

    private IdempotencyGuard idempotencyGuard;

    @Mock
    private WebSocketEventPublisher wsPublisher;

    private AuthorizationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        idempotencyGuard = new InMemoryIdempotencyGuard();
        consumer = new AuthorizationEventConsumer(idempotencyGuard, wsPublisher);
    }

    @Test
    @DisplayName("Test AuthorizationEventConsumer processes authorization event and triggers security alert on DENY")
    void testAuthorizationEventDenyTriggersAlert() {
        AuthorizationResultEvent payload = new AuthorizationResultEvent(
                "REF-AUTH-01", "CORR-A-01", "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL",
                "DENIED_BY_TRUST", 40.0, 75.0, Decision.DENY, EnforcementStatus.BLOCKED, "NONE",
                null, null, LocalDateTime.now().toString()
        );
        EventEnvelope<AuthorizationResultEvent> env = EventEnvelope.of(
                "AUTHORIZATION_RESULT", "TEST", "DOOR-SENSOR-001", "P-01", "B-01", "REF-AUTH-01", "CORR-A-01", payload
        );

        consumer.handleAuthorizationResultEvent(env);
        // Duplicate delivery should be ignored
        consumer.handleAuthorizationResultEvent(env);

        assertEquals(1, consumer.getReceivedCount());
        verify(wsPublisher, times(1)).publishAuthorizationResult(eq(env));
        verify(wsPublisher, times(1)).publishSecurityAlert(any());
    }

    @Test
    @DisplayName("Test AuthorizationEventConsumer processes blockchain event and forwards to WebSocket")
    void testBlockchainEventForwarded() {
        BlockchainResultEvent payload = new BlockchainResultEvent(
                "0xhash123", 100L, "0xcontractAddress", Decision.ALLOW,
                "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL",
                LocalDateTime.now().toString(), "CORR-BC-01"
        );
        EventEnvelope<BlockchainResultEvent> env = EventEnvelope.of(
                "BLOCKCHAIN_RESULT", "TEST", "DOOR-SENSOR-001", "P-01", "B-01", "REF-BC-01", "CORR-BC-01", payload
        );

        consumer.handleBlockchainResultEvent(env);
        consumer.handleBlockchainResultEvent(env);

        assertEquals(1, consumer.getBlockchainEventCount());
        verify(wsPublisher, times(1)).publishBlockchainResult(eq(env));
    }
}
