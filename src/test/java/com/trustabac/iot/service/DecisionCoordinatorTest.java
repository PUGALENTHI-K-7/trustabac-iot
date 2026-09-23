package com.trustabac.iot.service;

import com.trustabac.iot.dto.BlockchainAuthorizationRequest;
import com.trustabac.iot.dto.BlockchainAuthorizationResponse;
import com.trustabac.iot.entity.*;
import com.trustabac.iot.exception.BlockchainUnavailableException;
import com.trustabac.iot.repository.*;
import com.trustabac.iot.service.blockchain.BlockchainService;
import com.trustabac.iot.service.risk.ResourceRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class DecisionCoordinatorTest {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private PolicyRepository policyRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private AccessRequestRepository accessRequestRepository;

    @Autowired
    private TrustHistoryRepository trustHistoryRepository;

    @Autowired
    private RiskEventRepository riskEventRepository;

    @Autowired
    private BlockchainAuthorizationEventRepository authorizationEventRepository;

    @Autowired
    private AbacService abacService;

    @Autowired
    private ResourceRegistry resourceRegistry;

    private BlockchainService mockBlockchainService;
    private DecisionCoordinator coordinator;
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-09-20T10:00:00Z"), ZoneId.of("UTC"));

    @BeforeEach
    void setUp() {
        authorizationEventRepository.deleteAll();
        riskEventRepository.deleteAll();
        trustHistoryRepository.deleteAll();
        accessRequestRepository.deleteAll();
        bookingRepository.deleteAll();
        policyRepository.deleteAll();
        deviceRepository.deleteAll();

        mockBlockchainService = Mockito.mock(BlockchainService.class);
        coordinator = new DecisionCoordinator(
                abacService,
                resourceRegistry,
                mockBlockchainService,
                authorizationEventRepository,
                fixedClock
        );

        // Setup base registered device with 85.0 trust
        Device device = new Device("DEV-DOOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.REGISTERED, 85.0, true);
        deviceRepository.save(device);

        // Setup base guest policy for SMART_DOOR_LOCK CONTROL
        Policy policy = new Policy("Guest Smart Door Lock Access", "Policy description", "SMART_DOOR_LOCK", Operation.CONTROL, true);
        policy.addCondition(new PolicyCondition(AttributeCategory.SUBJECT, "subject.role", PolicyOperator.EQUALS, "GUEST"));
        policy.addCondition(new PolicyCondition(AttributeCategory.DEVICE, "device.active", PolicyOperator.EQUALS, "true"));
        policy.addCondition(new PolicyCondition(AttributeCategory.DEVICE, "device.registrationStatus", PolicyOperator.EQUALS, "REGISTERED"));
        policy.addCondition(new PolicyCondition(AttributeCategory.CONTEXT, "booking.valid", PolicyOperator.EQUALS, "true"));
        policyRepository.save(policy);

        // Setup active booking
        Booking booking = new Booking("BOOK-001", "Property-001", "Guest-001",
                LocalDateTime.of(2026, 9, 20, 0, 0),
                LocalDateTime.of(2026, 9, 25, 23, 59),
                BookingStatus.ACTIVE);
        bookingRepository.save(booking);
    }

    @Test
    @DisplayName("ABAC FAIL should immediately return DENY, bypass Risk, leave Trust untouched, and never call Blockchain")
    void shouldDenyImmediatelyOnAbacFail() {
        BlockchainAuthorizationRequest request = new BlockchainAuthorizationRequest(
                "DEV-DOOR-001", "Maint-001", "MAINTENANCE", "SmartRental",
                "SMART_DOOR_LOCK", "CONTROL", "Property-001", "BOOK-001", "LOCAL_WIFI",
                null, null, null, "REQ-ABAC-FAIL"
        );

        BlockchainAuthorizationResponse response = coordinator.evaluateAuthorization(request);

        assertThat(response.finalDecision()).isEqualTo(Decision.DENY);
        assertThat(response.abacResult()).isEqualTo("FAIL");
        assertThat(response.blockchainTransactionHash()).isNull();
        assertThat(response.trustScore()).isNull();
        assertThat(response.riskScore()).isNull();

        // Trust must remain untouched
        Device dev = deviceRepository.findByDeviceIdentifier("DEV-DOOR-001").orElseThrow();
        assertThat(dev.getCurrentTrust()).isEqualTo(85.0);

        // Zero blockchain audit records
        assertThat(authorizationEventRepository.count()).isEqualTo(0);
        Mockito.verifyNoInteractions(mockBlockchainService);
    }

    @Test
    @DisplayName("ABAC PASS with High Trust and Low Risk should return ALLOW from smart contract")
    void shouldAllowWhenHighTrustAndLowRisk() {
        when(mockBlockchainService.evaluateAccessOnChain(anyString(), anyString(), eq(true), eq(true), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new BlockchainService.BlockchainEvaluationResult(
                        Decision.ALLOW,
                        "Allowed by Smart Contract: High behavioral trust (85) and low contextual risk (15).",
                        "0xabc1234567890",
                        101L,
                        "0xContractAddress123"
                ));

        BlockchainAuthorizationRequest request = new BlockchainAuthorizationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "CONTROL", "Property-001", "BOOK-001", "LOCAL_WIFI",
                null, null, null, "REQ-ALLOW-01"
        );

        BlockchainAuthorizationResponse response = coordinator.evaluateAuthorization(request);

        assertThat(response.abacResult()).isEqualTo("PASS");
        assertThat(response.finalDecision()).isEqualTo(Decision.ALLOW);
        assertThat(response.decisionReason()).contains("Allowed by Smart Contract");
        assertThat(response.blockchainTransactionHash()).isEqualTo("0xabc1234567890");
        assertThat(response.blockchainBlockNumber()).isEqualTo(101L);

        // Verify off-chain audit record persisted
        List<BlockchainAuthorizationEvent> events = authorizationEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDecision()).isEqualTo(Decision.ALLOW);
        assertThat(events.get(0).getTransactionHash()).isEqualTo("0xabc1234567890");
    }

    @Test
    @DisplayName("ABAC PASS with High Trust and Medium Risk should return RESTRICT from smart contract")
    void shouldRestrictWhenHighTrustAndMediumRisk() {
        when(mockBlockchainService.evaluateAccessOnChain(anyString(), anyString(), eq(true), eq(true), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new BlockchainService.BlockchainEvaluationResult(
                        Decision.RESTRICT,
                        "Restricted by Smart Contract: Adaptive authorization enforced for intermediate trust (85) or moderate risk (50).",
                        "0xdef9876543210",
                        102L,
                        "0xContractAddress123"
                ));

        BlockchainAuthorizationRequest request = new BlockchainAuthorizationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "CONTROL", "Property-001", "BOOK-001", "REMOTE_CELLULAR",
                null, null, null, "REQ-RESTRICT-01"
        );

        BlockchainAuthorizationResponse response = coordinator.evaluateAuthorization(request);

        assertThat(response.abacResult()).isEqualTo("PASS");
        assertThat(response.finalDecision()).isEqualTo(Decision.RESTRICT);
        assertThat(response.decisionReason()).contains("Restricted by Smart Contract");
        assertThat(response.blockchainTransactionHash()).isEqualTo("0xdef9876543210");
        assertThat(response.blockchainBlockNumber()).isEqualTo(102L);

        List<BlockchainAuthorizationEvent> events = authorizationEventRepository.findAll();
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getDecision()).isEqualTo(Decision.RESTRICT);
    }

    @Test
    @DisplayName("ABAC PASS with High Risk should return DENY from smart contract")
    void shouldDenyWhenHighRisk() {
        when(mockBlockchainService.evaluateAccessOnChain(anyString(), anyString(), eq(true), eq(true), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(new BlockchainService.BlockchainEvaluationResult(
                        Decision.DENY,
                        "Denied by Smart Contract: Excessive contextual risk (85).",
                        "0x789denyhash123",
                        103L,
                        "0xContractAddress123"
                ));

        BlockchainAuthorizationRequest request = new BlockchainAuthorizationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "CONTROL", "Property-001", "BOOK-001", "UNKNOWN",
                30, 5, "ABNORMAL", "REQ-DENY-RISK"
        );

        BlockchainAuthorizationResponse response = coordinator.evaluateAuthorization(request);

        assertThat(response.abacResult()).isEqualTo("PASS");
        assertThat(response.finalDecision()).isEqualTo(Decision.DENY);
        assertThat(response.decisionReason()).contains("Denied by Smart Contract");
        assertThat(response.blockchainTransactionHash()).isEqualTo("0x789denyhash123");
    }

    @Test
    @DisplayName("Blockchain unavailable should enforce Fail-Closed DENY")
    void shouldFailClosedWhenBlockchainUnavailable() {
        when(mockBlockchainService.evaluateAccessOnChain(anyString(), anyString(), anyBoolean(), anyBoolean(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenThrow(new BlockchainUnavailableException("RPC node connection refused"));

        BlockchainAuthorizationRequest request = new BlockchainAuthorizationRequest(
                "DEV-DOOR-001", "Guest-001", "GUEST", "SmartRental",
                "SMART_DOOR_LOCK", "CONTROL", "Property-001", "BOOK-001", "LOCAL_WIFI",
                null, null, null, "REQ-FAIL-CLOSED"
        );

        BlockchainAuthorizationResponse response = coordinator.evaluateAuthorization(request);

        assertThat(response.abacResult()).isEqualTo("PASS");
        assertThat(response.finalDecision()).isEqualTo(Decision.DENY);
        assertThat(response.decisionReason()).contains("Fail-Closed Security Enforcement");
        assertThat(response.blockchainTransactionHash()).isNull();
    }
}
