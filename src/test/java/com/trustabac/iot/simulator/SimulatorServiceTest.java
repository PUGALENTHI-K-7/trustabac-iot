package com.trustabac.iot.simulator;

import com.trustabac.iot.dto.ResourceOperationRequest;
import com.trustabac.iot.dto.ResourceOperationResponse;
import com.trustabac.iot.dto.TrustEventRequest;
import com.trustabac.iot.dto.TrustUpdateResponse;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.entity.TrustEventType;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.service.ResourceOperationService;
import com.trustabac.iot.service.TrustService;
import com.trustabac.iot.service.enforcement.SimulatedDeviceStateStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SimulatorServiceTest {

    @Mock
    private ResourceOperationService resourceOperationService;

    @Mock
    private TrustService trustService;

    @Mock
    private DeviceRepository deviceRepository;

    private SimulatedDeviceStateStore stateStore;
    private SimulatorConfiguration config;
    private SimulatorService simulatorService;

    @BeforeEach
    void setUp() {
        stateStore = new SimulatedDeviceStateStore();
        config = new SimulatorConfiguration();
        config.setRequestIntervalMs(0); // instant in tests

        simulatorService = new SimulatorService(
                resourceOperationService,
                trustService,
                stateStore,
                config,
                deviceRepository
        );
    }

    private ResourceOperationResponse mockResponse(Decision decision, EnforcementStatus status, String effectiveOp, String msg) {
        return new ResourceOperationResponse(
                "OP-TEST-001",
                "DOOR-SENSOR-001",
                "SMART_DOOR_LOCK",
                "CONTROL",
                status,
                effectiveOp,
                msg,
                Map.of("status", "ONLINE"),
                decision,
                "Policy evaluation complete",
                decision == Decision.ALLOW ? "PASS" : "DENY",
                "ABAC evaluation result",
                85.0,
                "TRUSTED",
                20.0,
                "LOW",
                "0xabc123",
                1234L,
                "0xcontract",
                LocalDateTime.now().toString()
        );
    }

    @Test
    @DisplayName("Test simulator lifecycle: start, stop, getStatus, and reset")
    void testLifecycle() {
        SimulatorStatusResponse r1 = simulatorService.start();
        assertTrue(r1.running());

        SimulatorStatusResponse r2 = simulatorService.stop();
        assertFalse(r2.running());

        SimulatorStatusResponse r3 = simulatorService.reset();
        assertFalse(r3.running());
        assertEquals(0, r3.totalScenariosExecuted());
        assertEquals(0, r3.totalRequestsGenerated());
        assertNull(r3.lastScenarioResult());
    }

    @Test
    @DisplayName("Test scenario catalog contains all 10 scenario types")
    void testAvailableScenarios() {
        List<Map<String, String>> scenarios = simulatorService.getAvailableScenarios();
        assertEquals(10, scenarios.size());
        assertTrue(scenarios.stream().anyMatch(s -> s.get("type").equals("NORMAL_STAY")));
        assertTrue(scenarios.stream().anyMatch(s -> s.get("type").equals("PRE_CHECKIN")));
        assertTrue(scenarios.stream().anyMatch(s -> s.get("type").equals("RECOVERY")));
    }

    @Test
    @DisplayName("Test NORMAL_STAY scenario execution across canonical devices")
    void testNormalStayScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.ALLOW, EnforcementStatus.EXECUTED, "CONTROL", "Operation successful"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.NORMAL_STAY);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(10, result.getTotalRequests());
        assertEquals(10, result.getAllowCount());
        assertEquals(10, result.getExecutedCount());
        verify(resourceOperationService, times(10)).executeOperation(any(ResourceOperationRequest.class));
    }

    @Test
    @DisplayName("Test PRE_CHECKIN scenario execution produces DENY / BLOCKED")
    void testPreCheckinScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.DENY, EnforcementStatus.BLOCKED, "NONE", "Booking inactive"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.PRE_CHECKIN);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRequests());
        assertEquals(3, result.getDenyCount());
        assertEquals(3, result.getBlockedCount());
    }

    @Test
    @DisplayName("Test SUSPICIOUS_ACTIVITY scenario generates security event")
    void testSuspiciousActivityScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.ALLOW, EnforcementStatus.EXECUTED, "READ", "Read permitted"));

        when(trustService.recordTrustEvent(anyString(), any(TrustEventRequest.class)))
                .thenReturn(new TrustUpdateResponse("DOOR-SENSOR-001", 80.0, 70.0, -10.0, TrustEventType.SUSPICIOUS_ACTIVITY, "Suspicious pattern", "SIMULATOR", LocalDateTime.now(), "Trust updated"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.SUSPICIOUS_ACTIVITY);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalRequests());
        verify(trustService, times(1)).recordTrustEvent(eq("DOOR-SENSOR-001"), any(TrustEventRequest.class));
    }

    @Test
    @DisplayName("Test REQUEST_FLOODING scenario generates burst and trust event")
    void testRequestFloodingScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.ALLOW, EnforcementStatus.EXECUTED, "READ", "Read permitted"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.REQUEST_FLOODING);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getTotalRequests() >= 10);
        verify(trustService, atLeastOnce()).recordTrustEvent(eq("DOOR-SENSOR-001"), any(TrustEventRequest.class));
    }

    @Test
    @DisplayName("Test HIGH_RISK_ATTACK scenario executes critical and risk requests")
    void testHighRiskAttackScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.DENY, EnforcementStatus.BLOCKED, "NONE", "Blocked high risk / unauthorized"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.HIGH_RISK_ATTACK);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalRequests());
        assertEquals(2, result.getDenyCount());
        assertEquals(2, result.getBlockedCount());
    }

    @Test
    @DisplayName("Test LOW_TRUST_ATTACK scenario records malicious event and blocks access")
    void testLowTrustAttackScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.DENY, EnforcementStatus.BLOCKED, "NONE", "Blocked by low trust"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.LOW_TRUST_ATTACK);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotalRequests());
        assertEquals(1, result.getDenyCount());
        verify(trustService, times(1)).recordTrustEvent(eq("DOOR-SENSOR-001"), any(TrustEventRequest.class));
    }

    @Test
    @DisplayName("Test UNAUTHORIZED_SENSITIVE_ACCESS scenario attempts access to restricted infrastructure")
    void testUnauthorizedSensitiveAccessScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.DENY, EnforcementStatus.BLOCKED, "NONE", "ABAC rejected sensitive resource"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.UNAUTHORIZED_SENSITIVE_ACCESS);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRequests());
        assertEquals(3, result.getDenyCount());
    }

    @Test
    @DisplayName("Test RESTRICT_ENFORCEMENT scenario captures downgraded and executed requests")
    void testRestrictEnforcementScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.RESTRICT, EnforcementStatus.DOWNGRADED, "READ", "Physical control suppressed"))
                .thenReturn(mockResponse(Decision.RESTRICT, EnforcementStatus.EXECUTED, "READ", "Status read permitted"))
                .thenReturn(mockResponse(Decision.RESTRICT, EnforcementStatus.DOWNGRADED, "CONTROL", "Eco temperature clamped"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.RESTRICT_ENFORCEMENT);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRequests());
        assertEquals(3, result.getRestrictCount());
        assertEquals(1, result.getExecutedCount());
        assertEquals(2, result.getDowngradedCount());
    }

    @Test
    @DisplayName("Test POST_CHECKOUT scenario blocks expired booking attempts")
    void testPostCheckoutScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.DENY, EnforcementStatus.BLOCKED, "NONE", "Booking expired"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.POST_CHECKOUT);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getTotalRequests());
        assertEquals(3, result.getDenyCount());
        assertEquals(3, result.getBlockedCount());
    }

    @Test
    @DisplayName("Test RECOVERY scenario submits recovery event and allows subsequent access")
    void testRecoveryScenario() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.ALLOW, EnforcementStatus.EXECUTED, "CONTROL", "Restored access"));

        SimulatorScenarioResult result = simulatorService.runScenario(SimulatorScenarioType.RECOVERY);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getTotalRequests());
        assertEquals(2, result.getAllowCount());
        verify(trustService, atLeastOnce()).recordTrustEvent(eq("DOOR-SENSOR-001"), any(TrustEventRequest.class));
    }

    @Test
    @DisplayName("Test runAllScenarios executes all 10 scenarios sequentially")
    void testRunAllScenarios() {
        when(resourceOperationService.executeOperation(any(ResourceOperationRequest.class)))
                .thenReturn(mockResponse(Decision.ALLOW, EnforcementStatus.EXECUTED, "READ", "Success"));

        List<SimulatorScenarioResult> allResults = simulatorService.runAllScenarios();

        assertEquals(10, allResults.size());
        assertEquals(10, simulatorService.getStatus().totalScenariosExecuted());
    }
}
