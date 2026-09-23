package com.trustabac.iot.service;

import com.trustabac.iot.config.RiskProperties;
import com.trustabac.iot.dto.RiskEvaluationRequest;
import com.trustabac.iot.dto.RiskEvaluationResponse;
import com.trustabac.iot.dto.RiskHistoryResponse;
import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.entity.ResourceSensitivity;
import com.trustabac.iot.entity.RiskEvent;
import com.trustabac.iot.entity.RiskStatus;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.RiskEventRepository;
import com.trustabac.iot.service.risk.BehaviorRiskCalculator;
import com.trustabac.iot.service.risk.FrequencyRiskCalculator;
import com.trustabac.iot.service.risk.LocationRiskCalculator;
import com.trustabac.iot.service.risk.NetworkRiskCalculator;
import com.trustabac.iot.service.risk.ResourceRegistry;
import com.trustabac.iot.service.risk.RiskContext;
import com.trustabac.iot.service.risk.SensitivityRiskCalculator;
import com.trustabac.iot.service.risk.TimeRiskCalculator;
import com.trustabac.iot.service.risk.ViolationRiskCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private RiskEventRepository riskEventRepository;

    private ResourceRegistry resourceRegistry;
    private RiskProperties riskProperties;
    private Clock fixedClock;
    private LocalDateTime fixedTime;

    private RiskService riskService;
    private Device device;

    @BeforeEach
    void setUp() {
        riskProperties = new RiskProperties();
        resourceRegistry = new ResourceRegistry();

        Instant instant = Instant.parse("2026-09-20T14:00:00Z"); // 14:00 (inside operating hours 6-22)
        fixedClock = Clock.fixed(instant, ZoneId.of("UTC"));
        fixedTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));

        TimeRiskCalculator timeCalc = new TimeRiskCalculator(riskProperties);
        LocationRiskCalculator locCalc = new LocationRiskCalculator(riskProperties);
        SensitivityRiskCalculator sensCalc = new SensitivityRiskCalculator(riskProperties);
        FrequencyRiskCalculator freqCalc = new FrequencyRiskCalculator(riskProperties);
        NetworkRiskCalculator netCalc = new NetworkRiskCalculator(riskProperties);
        ViolationRiskCalculator violCalc = new ViolationRiskCalculator(riskProperties);
        BehaviorRiskCalculator behCalc = new BehaviorRiskCalculator(riskProperties);

        riskService = new RiskService(
                deviceRepository,
                riskEventRepository,
                resourceRegistry,
                riskProperties,
                timeCalc,
                locCalc,
                sensCalc,
                freqCalc,
                netCalc,
                violCalc,
                behCalc,
                fixedClock
        );

        device = new Device("DEV-DOOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.REGISTERED, 80.0, true);
        device.setId(1L);
    }

    @Test
    @DisplayName("Scenario 1: Low-risk normal context produces low score and LOW status")
    void testLowRiskEvaluation() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(riskEventRepository.save(any(RiskEvent.class))).thenAnswer(i -> i.getArgument(0));

        RiskContext context = RiskContext.builder()
                .deviceIdentifier("DEV-DOOR-001")
                .userId("Guest-001")
                .resource("SMART_LIGHT")
                .resourceSensitivity(ResourceSensitivity.LOW)
                .operation(Operation.READ)
                .location("Property-001")
                .networkContext("LOCAL_WIFI")
                .evaluationTimestamp(fixedTime)
                .requestCountWindow(1)
                .recentViolationCount(0)
                .behavioralIndicator(BehavioralIndicator.NORMAL)
                .build();

        RiskEvaluationResponse response = riskService.evaluateRisk(context);

        assertNotNull(response);
        assertEquals(0.0, response.riskScore());
        assertEquals(RiskStatus.LOW, response.riskStatus());
        assertEquals(0.0, response.factors().timeRisk());
        assertEquals(0.0, response.factors().locationRisk());
        assertEquals(0.0, response.factors().sensitivityRisk());
        assertEquals(0.0, response.factors().frequencyRisk());
        assertEquals(0.0, response.factors().networkRisk());
        assertEquals(0.0, response.factors().violationRisk());
        assertEquals(0.0, response.factors().behaviorRisk());
        assertEquals(fixedTime, response.evaluationTimestamp());
    }

    @Test
    @DisplayName("Scenario 2: Medium-risk contextual request produces expected intermediate score")
    void testMediumRiskEvaluation() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(riskEventRepository.save(any(RiskEvent.class))).thenAnswer(i -> i.getArgument(0));

        // High sensitivity door lock (0.70 * 20 = 14.0) + remote cellular network (0.60 * 15 = 9.0) = 23.0
        RiskContext context = RiskContext.builder()
                .deviceIdentifier("DEV-DOOR-001")
                .userId("Guest-001")
                .resource("SMART_DOOR_LOCK")
                .resourceSensitivity(ResourceSensitivity.HIGH)
                .operation(Operation.CONTROL)
                .location("Property-001")
                .networkContext("REMOTE_CELLULAR")
                .evaluationTimestamp(fixedTime)
                .requestCountWindow(1)
                .recentViolationCount(0)
                .behavioralIndicator(BehavioralIndicator.NORMAL)
                .build();

        RiskEvaluationResponse response = riskService.evaluateRisk(context);

        assertNotNull(response);
        assertEquals(23.0, response.riskScore());
        assertEquals(RiskStatus.LOW, response.riskStatus()); // <= 30.0 threshold is LOW
    }

    @Test
    @DisplayName("Scenario 3: High-risk request with multiple danger factors produces HIGH status")
    void testHighRiskEvaluation() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(riskEventRepository.save(any(RiskEvent.class))).thenAnswer(i -> i.getArgument(0));

        LocalDateTime nightTime = LocalDateTime.of(2026, 9, 20, 2, 0, 0); // Outside hours (1.0 * 10 = 10)
        // Unexpected location (1.0 * 15 = 15)
        // Critical resource ROUTER (1.0 * 20 = 20)
        // Flooding frequency 30 req (1.0 * 20 = 20)
        // Unknown network (1.0 * 15 = 15)
        // Violations 5 (1.0 * 10 = 10)
        // Abnormal behavior (1.0 * 10 = 10)
        // Total = 100.0

        RiskContext context = RiskContext.builder()
                .deviceIdentifier("DEV-DOOR-001")
                .userId("Attacker-99")
                .resource("ROUTER")
                .resourceSensitivity(ResourceSensitivity.CRITICAL)
                .operation(Operation.WRITE)
                .location("Remote-Hacker-Zone")
                .networkContext("PUBLIC_INTERNET")
                .evaluationTimestamp(nightTime)
                .requestCountWindow(30)
                .recentViolationCount(5)
                .behavioralIndicator(BehavioralIndicator.ABNORMAL)
                .build();

        RiskEvaluationResponse response = riskService.evaluateRisk(context);

        assertNotNull(response);
        assertEquals(100.0, response.riskScore());
        assertEquals(RiskStatus.HIGH, response.riskStatus());
        assertTrue(response.reason().contains("high resource sensitivity"));
        assertTrue(response.reason().contains("elevated request frequency"));
    }

    @Test
    @DisplayName("Scenario 4: ResourceRegistry automatically resolves sensitivity on resolveAndEvaluate")
    void testResolveAndEvaluateServerSensitivity() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(riskEventRepository.save(any(RiskEvent.class))).thenAnswer(i -> i.getArgument(0));

        RiskEvaluationRequest request = new RiskEvaluationRequest(
                "DEV-DOOR-001",
                "Guest-001",
                "SECURITY_CAMERA",
                Operation.READ,
                "Property-001",
                "LOCAL_WIFI"
        );

        RiskEvaluationResponse response = riskService.resolveAndEvaluate(request);

        assertNotNull(response);
        // SECURITY_CAMERA is HIGH sensitivity (0.70 * 20 = 14.0)
        assertEquals(14.0, response.riskScore());
        assertEquals(0.70, response.factors().sensitivityRisk());

        ArgumentCaptor<RiskEvent> captor = ArgumentCaptor.forClass(RiskEvent.class);
        verify(riskEventRepository).save(captor.capture());
        assertEquals("SECURITY_CAMERA", captor.getValue().getResource());
    }

    @Test
    @DisplayName("Scenario 5: Score clamping strictly bounds within [0.0, 100.0]")
    void testScoreClamping() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(riskEventRepository.save(any(RiskEvent.class))).thenAnswer(i -> i.getArgument(0));

        RiskContext contextMax = RiskContext.builder()
                .deviceIdentifier("DEV-DOOR-001")
                .resourceSensitivity(ResourceSensitivity.CRITICAL)
                .location("Remote")
                .networkContext("UNKNOWN")
                .requestCountWindow(100)
                .recentViolationCount(50)
                .behavioralIndicator(BehavioralIndicator.ABNORMAL)
                .evaluationTimestamp(LocalDateTime.of(2026, 9, 20, 3, 0))
                .build();

        RiskEvaluationResponse response = riskService.evaluateRisk(contextMax);
        assertEquals(100.0, response.riskScore());
    }

    @Test
    @DisplayName("Scenario 6: Unknown device throws ResourceNotFoundException (404)")
    void testUnknownDeviceThrowsNotFound() {
        when(deviceRepository.findByDeviceIdentifier("DEV-UNKNOWN")).thenReturn(Optional.empty());

        RiskContext context = RiskContext.builder().deviceIdentifier("DEV-UNKNOWN").build();
        assertThrows(ResourceNotFoundException.class, () -> riskService.evaluateRisk(context));
    }

    @Test
    @DisplayName("Scenario 7: Risk history is retrieved in newest-first order")
    void testGetRiskHistory() {
        RiskEvent event = new RiskEvent(device, "DEV-DOOR-001", 45.0, RiskStatus.MEDIUM,
                "SMART_DOOR_LOCK", Operation.CONTROL, "summary", "reason", fixedTime);
        event.setId(10L);

        when(deviceRepository.existsByDeviceIdentifier("DEV-DOOR-001")).thenReturn(true);
        when(riskEventRepository.findByDeviceIdentifierOrderByEvaluationTimestampDesc("DEV-DOOR-001"))
                .thenReturn(List.of(event));

        List<RiskHistoryResponse> history = riskService.getRiskHistory("DEV-DOOR-001");

        assertEquals(1, history.size());
        assertEquals(10L, history.getFirst().id());
        assertEquals(45.0, history.getFirst().riskScore());
        assertEquals(RiskStatus.MEDIUM, history.getFirst().riskStatus());
    }
}
