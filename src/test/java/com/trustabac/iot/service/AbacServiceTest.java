package com.trustabac.iot.service;

import com.trustabac.iot.dto.AccessEvaluationRequest;
import com.trustabac.iot.dto.AccessEvaluationResponse;
import com.trustabac.iot.dto.RiskEvaluationResponse;
import com.trustabac.iot.dto.RiskFactorBreakdown;
import com.trustabac.iot.entity.AbacAttributeKeys;
import com.trustabac.iot.entity.AbacResult;
import com.trustabac.iot.entity.AccessRequest;
import com.trustabac.iot.entity.AttributeCategory;
import com.trustabac.iot.entity.BehavioralIndicator;
import com.trustabac.iot.entity.Booking;
import com.trustabac.iot.entity.BookingStatus;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.Policy;
import com.trustabac.iot.entity.PolicyCondition;
import com.trustabac.iot.entity.PolicyOperator;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.entity.ResourceSensitivity;
import com.trustabac.iot.entity.RiskStatus;
import com.trustabac.iot.repository.AccessRequestRepository;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.PolicyRepository;
import com.trustabac.iot.service.risk.ResourceRegistry;
import com.trustabac.iot.service.risk.RiskContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbacServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private BookingService bookingService;

    @Mock
    private AccessRequestRepository accessRequestRepository;

    @Spy
    private ResourceRegistry resourceRegistry = new ResourceRegistry();

    @Mock
    private RiskService riskService;

    @Spy
    private PolicyEvaluator policyEvaluator = new PolicyEvaluator();

    private AbacService abacService;

    private Device activeDoorLock;
    private Policy guestDoorPolicy;
    private Policy guestWifiPolicy;
    private Booking validBooking;
    private LocalDateTime baseTime;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        Instant instant = Instant.parse("2026-09-20T14:00:00Z");
        fixedClock = Clock.fixed(instant, ZoneId.of("UTC"));
        baseTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));

        abacService = new AbacService(
                deviceRepository,
                policyRepository,
                bookingService,
                policyEvaluator,
                accessRequestRepository,
                resourceRegistry,
                riskService,
                fixedClock
        );

        activeDoorLock = new Device("DEV-DOOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.REGISTERED, 80.0, true);

        validBooking = new Booking("BOOK-001", "Property-001", "Guest-001",
                baseTime.minusDays(1), baseTime.plusDays(2), BookingStatus.CONFIRMED);

        guestDoorPolicy = new Policy("Guest Smart Door Lock Access", "Allows guest to control door lock during valid booking",
                "SMART_DOOR_LOCK", Operation.CONTROL, true);
        guestDoorPolicy.addCondition(new PolicyCondition(AttributeCategory.SUBJECT, AbacAttributeKeys.SUBJECT_ROLE, PolicyOperator.EQUALS, "GUEST"));
        guestDoorPolicy.addCondition(new PolicyCondition(AttributeCategory.DEVICE, AbacAttributeKeys.DEVICE_ACTIVE, PolicyOperator.EQUALS, "true"));
        guestDoorPolicy.addCondition(new PolicyCondition(AttributeCategory.DEVICE, AbacAttributeKeys.DEVICE_REGISTRATION_STATUS, PolicyOperator.EQUALS, "REGISTERED"));
        guestDoorPolicy.addCondition(new PolicyCondition(AttributeCategory.CONTEXT, AbacAttributeKeys.BOOKING_VALID, PolicyOperator.EQUALS, "true"));

        guestWifiPolicy = new Policy("Guest Wi-Fi Access", "Allows guest to read Wi-Fi network during valid booking",
                "GUEST_WIFI", Operation.READ, true);
        guestWifiPolicy.addCondition(new PolicyCondition(AttributeCategory.SUBJECT, AbacAttributeKeys.SUBJECT_ROLE, PolicyOperator.EQUALS, "GUEST"));
        guestWifiPolicy.addCondition(new PolicyCondition(AttributeCategory.CONTEXT, AbacAttributeKeys.BOOKING_VALID, PolicyOperator.EQUALS, "true"));

        when(accessRequestRepository.save(any(AccessRequest.class))).thenAnswer(invocation -> {
            AccessRequest req = invocation.getArgument(0);
            req.setId(100L);
            return req;
        });
    }

    private void mockRiskEvaluation(double score, RiskStatus status) {
        when(riskService.evaluateRisk(any(RiskContext.class))).thenReturn(new RiskEvaluationResponse(
                "DEV-DOOR-001",
                score,
                status,
                new RiskFactorBreakdown(0.0, 0.0, 0.70, 0.0, 0.0, 0.0, 0.0),
                "Contextual risk evaluation complete.",
                baseTime
        ));
    }

    @Test
    @DisplayName("Scenario 1: Valid guest door-control request during valid booking -> PASS with Trust & Risk")
    void testValidGuestDoorControlPass() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));
        mockRiskEvaluation(14.0, RiskStatus.LOW);

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.PASS, response.getResult());
        assertTrue(response.getReason().contains("All required ABAC conditions satisfied"));
        assertEquals("Guest Smart Door Lock Access", response.getEvaluatedPolicyName());
        assertEquals(80.0, response.getTrustScore());
        assertEquals("TRUSTED", response.getTrustStatus());
        assertEquals(14.0, response.getRiskScore());
        assertEquals(RiskStatus.LOW, response.getRiskStatus());
    }

    @Test
    @DisplayName("Scenario 2: Revoked device -> FAIL, no Risk evaluation")
    void testRevokedDeviceFail() {
        Device revokedDevice = new Device("DEV-DOOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.REVOKED, 80.0, true);
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(revokedDevice));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("REVOKED"));
        assertNull(response.getTrustScore());
        assertNull(response.getRiskScore());
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 3: Suspended device -> FAIL")
    void testSuspendedDeviceFail() {
        Device suspendedDevice = new Device("DEV-DOOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.SUSPENDED, 80.0, true);
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(suspendedDevice));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("SUSPENDED"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 4: Inactive device -> FAIL")
    void testInactiveDeviceFail() {
        Device inactiveDevice = new Device("DEV-DOOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.REGISTERED, 80.0, false);
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(inactiveDevice));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("inactive"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 5: Expired booking -> FAIL")
    void testExpiredBookingFail() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-EXPIRED")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(false);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-EXPIRED", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("booking.valid"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 6: Booking not yet started -> FAIL")
    void testBookingNotYetStartedFail() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-FUTURE")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(false);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-FUTURE", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("booking.valid"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 7: Guest requesting forbidden security camera -> FAIL")
    void testGuestSecurityCameraFail() {
        Device cameraDevice = new Device("DEV-CAM-001", DeviceType.SECURITY_CAMERA, DeviceClass.SENSOR,
                RegistrationStatus.REGISTERED, 80.0, true);
        when(deviceRepository.findByDeviceIdentifier("DEV-CAM-001")).thenReturn(Optional.of(cameraDevice));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy, guestWifiPolicy));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-CAM-001", "Guest-001", "GUEST", "SmartRental",
                "SECURITY_CAMERA", "SECURITY_CAMERA", null,
                Operation.READ, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("No active policy applies to resource 'SECURITY_CAMERA'"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 8: Guest requesting router administration -> FAIL")
    void testGuestRouterAdminFail() {
        Device routerDevice = new Device("DEV-ROUTER-001", DeviceType.ROUTER, DeviceClass.GATEWAY,
                RegistrationStatus.REGISTERED, 80.0, true);
        when(deviceRepository.findByDeviceIdentifier("DEV-ROUTER-001")).thenReturn(Optional.of(routerDevice));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy, guestWifiPolicy));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-ROUTER-001", "Guest-001", "GUEST", "SmartRental",
                "ROUTER", "ROUTER", null,
                Operation.WRITE, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("No active policy applies to resource 'ROUTER'"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 9: Valid guest Wi-Fi request during valid booking -> PASS")
    void testValidGuestWifiPass() {
        Device wifiDevice = new Device("DEV-WIFI-001", DeviceType.GUEST_WIFI, DeviceClass.GATEWAY,
                RegistrationStatus.REGISTERED, 80.0, true);
        when(deviceRepository.findByDeviceIdentifier("DEV-WIFI-001")).thenReturn(Optional.of(wifiDevice));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy, guestWifiPolicy));
        mockRiskEvaluation(7.0, RiskStatus.LOW);

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-WIFI-001", "Guest-001", "GUEST", "SmartRental",
                "GUEST_WIFI", "GUEST_WIFI", null,
                Operation.READ, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.PASS, response.getResult());
        assertEquals("Guest Wi-Fi Access", response.getEvaluatedPolicyName());
    }

    @Test
    @DisplayName("Scenario 10: Wrong subject role (e.g. VISITOR instead of GUEST) -> FAIL")
    void testWrongRoleFail() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Visitor-001", "VISITOR", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("subject.role"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 11: Disallowed operation on resource -> FAIL")
    void testDisallowedOperationFail() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));

        // Policy only allows CONTROL, request asks for DELETE
        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.DELETE, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("No active policy applies"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 12: Unknown device not registered in gateway -> FAIL")
    void testUnknownDeviceFail() {
        when(deviceRepository.findByDeviceIdentifier("DEV-UNKNOWN-999")).thenReturn(Optional.empty());

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-UNKNOWN-999", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertTrue(response.getReason().contains("not registered"));
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 13: Multiple matching candidate policies evaluates deterministically")
    void testMultipleMatchingPoliciesDeterministic() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        mockRiskEvaluation(14.0, RiskStatus.LOW);

        // Policy 1: Requires ADMIN role -> will fail for GUEST
        Policy adminDoorPolicy = new Policy("Admin Door Control", "Allows admin to control door",
                "SMART_DOOR_LOCK", Operation.CONTROL, true);
        adminDoorPolicy.addCondition(new PolicyCondition(AttributeCategory.SUBJECT, AbacAttributeKeys.SUBJECT_ROLE, PolicyOperator.EQUALS, "ADMIN"));

        // Policy 2: Requires GUEST role -> will pass for GUEST
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(adminDoorPolicy, guestDoorPolicy));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.PASS, response.getResult());
        assertEquals("Guest Smart Door Lock Access", response.getEvaluatedPolicyName());
    }

    @Test
    @DisplayName("Scenario 14: Persists audit log record with evaluated attributes and explanation")
    void testAuditLogPersistence() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));
        mockRiskEvaluation(14.0, RiskStatus.LOW);

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        ArgumentCaptor<AccessRequest> captor = ArgumentCaptor.forClass(AccessRequest.class);
        verify(accessRequestRepository).save(captor.capture());

        AccessRequest savedReq = captor.getValue();
        assertEquals("DEV-DOOR-001", savedReq.getDeviceIdentifier());
        assertEquals("Guest-001", savedReq.getUserId());
        assertEquals(AbacResult.PASS, savedReq.getAbacResult());
        assertNotNull(savedReq.getAbacReason());
        assertEquals(100L, response.getRequestId());
    }

    @Test
    @DisplayName("Scenario 15: ABAC FAIL leaves device trust unchanged and does not evaluate Risk")
    void testAbacFailLeavesTrustUnchanged() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));

        // Wrong role (VISITOR) -> ABAC FAIL
        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Visitor-001", "VISITOR", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.FAIL, response.getResult());
        assertEquals(80.0, activeDoorLock.getCurrentTrust());
        assertNull(response.getTrustScore());
        assertNull(response.getRiskScore());
        verify(riskService, never()).evaluateRisk(any());
    }

    @Test
    @DisplayName("Scenario 16: ABAC PASS includes current trust score, trust status, and risk evaluation")
    void testAbacPassIncludesTrustAndRisk() {
        activeDoorLock.setCurrentTrust(75.0);
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));
        mockRiskEvaluation(14.0, RiskStatus.LOW);

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "LOCAL_WIFI", baseTime
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.PASS, response.getResult());
        assertEquals(75.0, response.getTrustScore());
        assertEquals("TRUSTED", response.getTrustStatus());
        assertEquals(14.0, response.getRiskScore());
        assertEquals(RiskStatus.LOW, response.getRiskStatus());
        assertEquals(75.0, activeDoorLock.getCurrentTrust());
    }

    @Test
    @DisplayName("Scenario 17: High contextual risk does NOT mutate device Trust")
    void testHighRiskDoesNotMutateTrust() {
        activeDoorLock.setCurrentTrust(85.0);
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(activeDoorLock));
        when(bookingService.findByBookingReference("BOOK-001")).thenReturn(Optional.of(validBooking));
        when(bookingService.isBookingValid(any(Booking.class), any(LocalDateTime.class), anyString())).thenReturn(true);
        when(policyRepository.findByActiveTrue()).thenReturn(List.of(guestDoorPolicy));

        // High contextual risk evaluation (e.g. 85.0 HIGH)
        when(riskService.evaluateRisk(any(RiskContext.class))).thenReturn(new RiskEvaluationResponse(
                "DEV-DOOR-001",
                85.0,
                RiskStatus.HIGH,
                new RiskFactorBreakdown(1.0, 1.0, 1.0, 0.8, 0.6, 0.0, 0.6),
                "Contextual risk elevated due to high sensitivity and anomalous factors.",
                baseTime
        ));

        AccessEvaluationRequest request = new AccessEvaluationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "SMART_DOOR_LOCK", null,
                Operation.CONTROL, "Property-001", "BOOK-001", "REMOTE_CELLULAR", baseTime,
                15, 0, BehavioralIndicator.SUSPICIOUS
        );

        AccessEvaluationResponse response = abacService.evaluateAccess(request);

        assertEquals(AbacResult.PASS, response.getResult());
        assertEquals(85.0, response.getTrustScore());
        assertEquals(85.0, response.getRiskScore());
        assertEquals(RiskStatus.HIGH, response.getRiskStatus());

        // CRITICAL: Device trust remains 85.0 (unmutated by high contextual risk)
        assertEquals(85.0, activeDoorLock.getCurrentTrust());
    }
}
