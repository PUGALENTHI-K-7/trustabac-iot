package com.trustabac.iot.service;

import com.trustabac.iot.dto.BlockchainAuthorizationRequest;
import com.trustabac.iot.dto.BlockchainAuthorizationResponse;
import com.trustabac.iot.dto.ResourceOperationRequest;
import com.trustabac.iot.dto.ResourceOperationResponse;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.entity.ResourceSensitivity;
import com.trustabac.iot.service.enforcement.SimulatedDeviceStateStore;
import com.trustabac.iot.service.risk.ResourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceOperationServiceTest {

    @Mock
    private DecisionCoordinator decisionCoordinator;

    private ResourceRegistry resourceRegistry;
    private SimulatedDeviceStateStore stateStore;
    private ResourceOperationService resourceOperationService;

    @BeforeEach
    void setUp() {
        resourceRegistry = new ResourceRegistry();
        stateStore = new SimulatedDeviceStateStore();
        resourceOperationService = new ResourceOperationService(decisionCoordinator, resourceRegistry, stateStore);
    }

    private BlockchainAuthorizationResponse mockAuthResponse(Decision decision, String abacResult, String reason, String txHash) {
        return new BlockchainAuthorizationResponse(
                1L,
                "DOOR-SENSOR-001",
                "guest-user-001",
                "SMART_DOOR_LOCK",
                "CONTROL",
                abacResult,
                "ABAC eval",
                "Test-Policy",
                80.0,
                "TRUSTED",
                20.0,
                "LOW",
                null,
                "Contextual risk LOW",
                decision,
                reason,
                txHash,
                10L,
                "0x1234567890abcdef",
                java.time.LocalDateTime.of(2026, 9, 21, 21, 0, 0)
        );
    }

    @Test
    @DisplayName("TEST 1: ALLOW + CONTROL on Smart Door Lock -> EXECUTED, door state becomes UNLOCKED")
    void testAllowControlDoorLock() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(mockAuthResponse(Decision.ALLOW, "PASS", "Allowed by Smart Contract", "0xabc1"));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "DOOR-SENSOR-001",
                "guest-user-001",
                "GUEST",
                "SmartRental",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-001",
                Map.of("action", "UNLOCK")
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.EXECUTED);
        assertThat(resp.authorizationDecision()).isEqualTo(Decision.ALLOW);
        assertThat(resp.effectiveOperation()).isEqualTo("CONTROL");
        assertThat(resp.deviceState().get("lockState")).isEqualTo("UNLOCKED");
        assertThat(resp.blockchainTxHash()).isEqualTo("0xabc1");
    }

    @Test
    @DisplayName("TEST 2: RESTRICT + CONTROL on Smart Door Lock -> DOWNGRADED, door remains LOCKED")
    void testRestrictControlDoorLock() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(mockAuthResponse(Decision.RESTRICT, "PASS", "Restricted by Smart Contract", "0xabc2"));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "DOOR-SENSOR-001",
                "guest-user-001",
                "GUEST",
                "SmartRental",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-002",
                Map.of("action", "UNLOCK")
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.DOWNGRADED);
        assertThat(resp.authorizationDecision()).isEqualTo(Decision.RESTRICT);
        assertThat(resp.effectiveOperation()).isEqualTo("READ");
        // Critical: Door lock state must NOT change to UNLOCKED
        assertThat(resp.deviceState().get("lockState")).isEqualTo("LOCKED");
        assertThat(resp.executionMessage()).contains("suppressed");
    }

    @Test
    @DisplayName("TEST 3: RESTRICT + READ on Smart Door Lock -> EXECUTED, status read permitted")
    void testRestrictReadDoorLock() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(mockAuthResponse(Decision.RESTRICT, "PASS", "Restricted by Smart Contract", "0xabc3"));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "DOOR-SENSOR-001",
                "guest-user-001",
                "GUEST",
                "SmartRental",
                "SMART_DOOR_LOCK",
                "READ",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-003",
                null
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.EXECUTED);
        assertThat(resp.effectiveOperation()).isEqualTo("READ");
        assertThat(resp.deviceState().get("lockState")).isEqualTo("LOCKED");
    }

    @Test
    @DisplayName("TEST 4: DENY + CONTROL -> BLOCKED, operation blocked")
    void testDenyControl() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(mockAuthResponse(Decision.DENY, "PASS", "Denied by Smart Contract", "0xabc4"));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "DOOR-SENSOR-001",
                "guest-user-001",
                "GUEST",
                "SmartRental",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-004",
                Map.of("action", "UNLOCK")
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.BLOCKED);
        assertThat(resp.effectiveOperation()).isEqualTo("NONE");
        assertThat(resp.deviceState().get("lockState")).isEqualTo("LOCKED");
    }

    @Test
    @DisplayName("TEST 5: DENY + READ -> BLOCKED, operation blocked")
    void testDenyRead() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(mockAuthResponse(Decision.DENY, "PASS", "Denied by Smart Contract", "0xabc5"));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "DOOR-SENSOR-001",
                "guest-user-001",
                "GUEST",
                "SmartRental",
                "SMART_DOOR_LOCK",
                "READ",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-005",
                null
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.BLOCKED);
        assertThat(resp.effectiveOperation()).isEqualTo("NONE");
    }

    @Test
    @DisplayName("TEST 6: ABAC FAIL -> BLOCKED, operation never executes")
    void testAbacFail() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(new BlockchainAuthorizationResponse(
                        null,
                        "UNKNOWN-DEV",
                        "guest-001",
                        "SMART_DOOR_LOCK",
                        "CONTROL",
                        "FAIL",
                        "Device is not registered in gateway inventory",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Decision.DENY,
                        "Access Denied: ABAC eligibility failed",
                        null,
                        null,
                        null,
                        java.time.LocalDateTime.of(2026, 9, 21, 21, 0, 0)
                ));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "UNKNOWN-DEV",
                "guest-001",
                "GUEST",
                "SmartRental",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-006",
                null
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.BLOCKED);
        assertThat(resp.abacResult()).isEqualTo("FAIL");
        assertThat(resp.blockchainTxHash()).isNull();
    }

    @Test
    @DisplayName("TEST 7: Inactive booking -> BLOCKED")
    void testInactiveBooking() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(new BlockchainAuthorizationResponse(
                        null,
                        "DOOR-SENSOR-001",
                        "guest-001",
                        "SMART_DOOR_LOCK",
                        "CONTROL",
                        "FAIL",
                        "Booking is expired or inactive",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Decision.DENY,
                        "Access Denied: ABAC eligibility failed",
                        null,
                        null,
                        null,
                        java.time.LocalDateTime.of(2026, 9, 21, 21, 0, 0)
                ));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "DOOR-SENSOR-001",
                "guest-001",
                "GUEST",
                "SmartRental",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "EXPIRED-BOOKING",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-007",
                null
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.BLOCKED);
        assertThat(resp.abacResult()).isEqualTo("FAIL");
    }

    @Test
    @DisplayName("TEST 8: Blockchain unavailable -> Fail-closed BLOCKED")
    void testBlockchainUnavailable() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(new BlockchainAuthorizationResponse(
                        1L,
                        "DOOR-SENSOR-001",
                        "guest-001",
                        "SMART_DOOR_LOCK",
                        "CONTROL",
                        "PASS",
                        "ABAC pass",
                        "Policy-1",
                        80.0,
                        "TRUSTED",
                        20.0,
                        "LOW",
                        null,
                        "LOW risk",
                        Decision.DENY,
                        "Access Denied: Blockchain authorization engine unavailable (Fail-Closed Security Enforcement).",
                        null,
                        null,
                        null,
                        java.time.LocalDateTime.of(2026, 9, 21, 21, 0, 0)
                ));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "DOOR-SENSOR-001",
                "guest-001",
                "GUEST",
                "SmartRental",
                "SMART_DOOR_LOCK",
                "CONTROL",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-008",
                null
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.BLOCKED);
        assertThat(resp.authorizationDecision()).isEqualTo(Decision.DENY);
        assertThat(resp.decisionReason()).contains("Fail-Closed");
    }

    @Test
    @DisplayName("TEST 9: RESTRICT + CONTROL on Smart Thermostat -> DOWNGRADED with eco-clamped temperature")
    void testRestrictThermostatControl() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(mockAuthResponse(Decision.RESTRICT, "PASS", "Restricted by Smart Contract", "0xabc9"));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "THERMOSTAT-001",
                "guest-user-001",
                "GUEST",
                "SmartRental",
                "SMART_THERMOSTAT",
                "CONTROL",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-009",
                Map.of("targetTempCelsius", 28.0) // requested 28C (excessive heat)
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.DOWNGRADED);
        // Clamped to max eco limit 24.0C
        assertThat(resp.deviceState().get("targetTempCelsius")).isEqualTo(24.0);
        assertThat(resp.deviceState().get("ecoMode")).isEqualTo(true);
    }

    @Test
    @DisplayName("TEST 10: ALLOW + CONTROL on Smart Light -> EXECUTED, power becomes ON")
    void testAllowSmartLightControl() {
        when(decisionCoordinator.evaluateAuthorization(any(BlockchainAuthorizationRequest.class)))
                .thenReturn(mockAuthResponse(Decision.ALLOW, "PASS", "Allowed by Smart Contract", "0xabc10"));

        ResourceOperationRequest req = new ResourceOperationRequest(
                "LIGHT-001",
                "guest-user-001",
                "GUEST",
                "SmartRental",
                "SMART_LIGHT",
                "CONTROL",
                "Property-001",
                "BOOK-001",
                "INTERNAL",
                "NORMAL",
                1,
                0,
                "REQ-010",
                Map.of("power", "ON")
        );

        ResourceOperationResponse resp = resourceOperationService.executeOperation(req);

        assertThat(resp.enforcementStatus()).isEqualTo(EnforcementStatus.EXECUTED);
        assertThat(resp.deviceState().get("power")).isEqualTo("ON");
    }
}
