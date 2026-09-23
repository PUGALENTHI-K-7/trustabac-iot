package com.trustabac.iot.service;

import com.trustabac.iot.config.TrustProperties;
import com.trustabac.iot.dto.TrustEventRequest;
import com.trustabac.iot.dto.TrustHistoryResponse;
import com.trustabac.iot.dto.TrustScoreResponse;
import com.trustabac.iot.dto.TrustUpdateResponse;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.entity.TrustEventType;
import com.trustabac.iot.entity.TrustHistory;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.repository.TrustHistoryRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrustServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private TrustHistoryRepository trustHistoryRepository;

    @Spy
    private TrustProperties trustProperties = new TrustProperties();

    private Clock fixedClock;
    private LocalDateTime fixedDateTime;
    private TrustService trustService;

    private Device device;

    @BeforeEach
    void setUp() {
        Instant fixedInstant = Instant.parse("2026-09-20T18:00:00Z");
        fixedClock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        fixedDateTime = LocalDateTime.ofInstant(fixedInstant, ZoneId.of("UTC"));

        trustService = new TrustService(deviceRepository, trustHistoryRepository, trustProperties, fixedClock);

        device = new Device("DEV-DOOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR,
                RegistrationStatus.REGISTERED, 80.0, true);
        device.setId(1L);
    }

    @Test
    @DisplayName("Scenario 1 & 2: Initial trust retrieval returns configured score and status")
    void testGetCurrentTrust() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));

        TrustScoreResponse response = trustService.getCurrentTrust("DEV-DOOR-001");

        assertNotNull(response);
        assertEquals("DEV-DOOR-001", response.getDeviceIdentifier());
        assertEquals(80.0, response.getCurrentTrust());
        assertEquals("TRUSTED", response.getTrustStatus());
        verify(trustHistoryRepository, never()).save(any(TrustHistory.class));
    }

    @Test
    @DisplayName("Scenario 3: NORMAL_SUCCESS event increases trust score by +1.0 with server clock timestamp")
    void testNormalSuccessIncreasesTrust() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        TrustEventRequest request = new TrustEventRequest(TrustEventType.NORMAL_SUCCESS, "Successful unlock", "DOOR_GATEWAY");
        TrustUpdateResponse response = trustService.recordTrustEvent("DEV-DOOR-001", request);

        assertEquals(80.0, response.getOldTrust());
        assertEquals(81.0, response.getNewTrust());
        assertEquals(1.0, response.getDelta());
        assertEquals(TrustEventType.NORMAL_SUCCESS, response.getEventType());
        assertEquals(81.0, device.getCurrentTrust());
        assertEquals(fixedDateTime, response.getEventTimestamp());

        ArgumentCaptor<TrustHistory> historyCaptor = ArgumentCaptor.forClass(TrustHistory.class);
        verify(trustHistoryRepository).save(historyCaptor.capture());
        TrustHistory savedHistory = historyCaptor.getValue();
        assertEquals(80.0, savedHistory.getOldTrust());
        assertEquals(81.0, savedHistory.getNewTrust());
        assertEquals(1.0, savedHistory.getDelta());
        assertEquals(fixedDateTime, savedHistory.getEventTimestamp());
    }

    @Test
    @DisplayName("Scenario 4: SUSPICIOUS_ACTIVITY event decreases trust score by -10.0")
    void testSuspiciousActivityDecreasesTrust() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        TrustEventRequest request = new TrustEventRequest(TrustEventType.SUSPICIOUS_ACTIVITY, "Anomalous payload pattern", "SENSOR_MONITOR");
        TrustUpdateResponse response = trustService.recordTrustEvent("DEV-DOOR-001", request);

        assertEquals(80.0, response.getOldTrust());
        assertEquals(70.0, response.getNewTrust());
        assertEquals(-10.0, response.getDelta());
        assertEquals(70.0, device.getCurrentTrust());
        assertEquals(fixedDateTime, response.getEventTimestamp());
    }

    @Test
    @DisplayName("Scenario 5: REQUEST_FLOODING event decreases trust score by -25.0")
    void testRequestFloodingDecreasesTrust() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        TrustEventRequest request = new TrustEventRequest(TrustEventType.REQUEST_FLOODING, "Rate limit exceeded (100 req/s)", "RATE_LIMITER");
        TrustUpdateResponse response = trustService.recordTrustEvent("DEV-DOOR-001", request);

        assertEquals(80.0, response.getOldTrust());
        assertEquals(55.0, response.getNewTrust());
        assertEquals(-25.0, response.getDelta());
        assertEquals(55.0, device.getCurrentTrust());
        assertEquals(fixedDateTime, response.getEventTimestamp());
    }

    @Test
    @DisplayName("Scenario 6: CONFIRMED_MALICIOUS event causes strong decrease of -40.0")
    void testConfirmedMaliciousStrongDecrease() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        TrustEventRequest request = new TrustEventRequest(TrustEventType.CONFIRMED_MALICIOUS, "Unauthorized firmware tamper attempt", "SECURITY_AGENT");
        TrustUpdateResponse response = trustService.recordTrustEvent("DEV-DOOR-001", request);

        assertEquals(80.0, response.getOldTrust());
        assertEquals(40.0, response.getNewTrust());
        assertEquals(-40.0, response.getDelta());
        assertEquals(40.0, device.getCurrentTrust());
        assertEquals(fixedDateTime, response.getEventTimestamp());
    }

    @Test
    @DisplayName("Scenario 7: RECOVERY event increases trust gradually by +10.0")
    void testRecoveryGradualIncrease() {
        device.setCurrentTrust(30.0);
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        TrustEventRequest request = new TrustEventRequest(TrustEventType.RECOVERY, "Administrative audit cleared", "ADMIN_CONSOLE");
        TrustUpdateResponse response = trustService.recordTrustEvent("DEV-DOOR-001", request);

        assertEquals(30.0, response.getOldTrust());
        assertEquals(40.0, response.getNewTrust());
        assertEquals(10.0, response.getDelta());
        assertEquals(40.0, device.getCurrentTrust());
        assertEquals(fixedDateTime, response.getEventTimestamp());
    }

    @Test
    @DisplayName("Scenario 8: Trust score is strictly clamped at maximum 100.0")
    void testUpperBoundClamping() {
        device.setCurrentTrust(99.5);
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        TrustEventRequest request = new TrustEventRequest(TrustEventType.NORMAL_SUCCESS, "Normal operation", "GATEWAY");
        TrustUpdateResponse response = trustService.recordTrustEvent("DEV-DOOR-001", request);

        assertEquals(99.5, response.getOldTrust());
        assertEquals(100.0, response.getNewTrust());
        assertEquals(100.0, device.getCurrentTrust());
    }

    @Test
    @DisplayName("Scenario 9: Trust score is strictly clamped at minimum 0.0")
    void testLowerBoundClamping() {
        device.setCurrentTrust(15.0);
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        TrustEventRequest request = new TrustEventRequest(TrustEventType.CONFIRMED_MALICIOUS, "Exploit attempt", "GATEWAY");
        TrustUpdateResponse response = trustService.recordTrustEvent("DEV-DOOR-001", request);

        assertEquals(15.0, response.getOldTrust());
        assertEquals(0.0, response.getNewTrust());
        assertEquals(0.0, device.getCurrentTrust());
    }

    @Test
    @DisplayName("Scenario 10 & 11: TrustHistory completeness and append-only audit trail")
    void testTrustHistoryCompleteness() {
        TrustHistory history = new TrustHistory(device, "DEV-DOOR-001", 80.0, 70.0, -10.0,
                TrustEventType.SUSPICIOUS_ACTIVITY, "Anomalous burst", "SECURITY_PROBE", fixedDateTime);
        history.setId(50L);

        when(deviceRepository.existsByDeviceIdentifier("DEV-DOOR-001")).thenReturn(true);
        when(trustHistoryRepository.findByDeviceIdentifierOrderByCreatedAtDesc("DEV-DOOR-001"))
                .thenReturn(List.of(history));

        List<TrustHistoryResponse> historyList = trustService.getTrustHistory("DEV-DOOR-001");

        assertEquals(1, historyList.size());
        TrustHistoryResponse item = historyList.getFirst();
        assertEquals(50L, item.getId());
        assertEquals("DEV-DOOR-001", item.getDeviceIdentifier());
        assertEquals(80.0, item.getOldTrust());
        assertEquals(70.0, item.getNewTrust());
        assertEquals(-10.0, item.getDelta());
        assertEquals(TrustEventType.SUSPICIOUS_ACTIVITY, item.getEventType());
        assertEquals("Anomalous burst", item.getReason());
        assertEquals("SECURITY_PROBE", item.getSource());
        assertEquals(fixedDateTime, item.getEventTimestamp());
    }

    @Test
    @DisplayName("Scenario 12: Unknown device returns ResourceNotFoundException (404)")
    void testUnknownDeviceReturnsNotFound() {
        when(deviceRepository.findByDeviceIdentifier("DEV-UNKNOWN")).thenReturn(Optional.empty());

        TrustEventRequest request = new TrustEventRequest(TrustEventType.NORMAL_SUCCESS, "Test", "TEST");
        assertThrows(ResourceNotFoundException.class, () -> trustService.recordTrustEvent("DEV-UNKNOWN", request));
        assertThrows(ResourceNotFoundException.class, () -> trustService.getCurrentTrust("DEV-UNKNOWN"));
    }

    @Test
    @DisplayName("Scenario 13: calculateTrustStatus returns expected monitoring categories")
    void testCalculateTrustStatusBands() {
        assertEquals("TRUSTED", trustService.calculateTrustStatus(85.0));
        assertEquals("TRUSTED", trustService.calculateTrustStatus(70.0));
        assertEquals("SUSPICIOUS", trustService.calculateTrustStatus(69.9));
        assertEquals("SUSPICIOUS", trustService.calculateTrustStatus(40.0));
        assertEquals("UNTRUSTED", trustService.calculateTrustStatus(39.9));
        assertEquals("UNTRUSTED", trustService.calculateTrustStatus(0.0));
    }

    @Test
    @DisplayName("Scenario 14: Server clock establishes authoritative timestamp on TrustHistory")
    void testServerClockAuthoritativeTimestamp() {
        when(deviceRepository.findByDeviceIdentifier("DEV-DOOR-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(i -> i.getArgument(0));

        TrustEventRequest request = new TrustEventRequest(TrustEventType.NORMAL_SUCCESS, "Heartbeat", "GATEWAY");
        TrustUpdateResponse response = trustService.recordTrustEvent("DEV-DOOR-001", request);

        assertEquals(fixedDateTime, response.getEventTimestamp());

        ArgumentCaptor<TrustHistory> captor = ArgumentCaptor.forClass(TrustHistory.class);
        verify(trustHistoryRepository).save(captor.capture());
        assertEquals(fixedDateTime, captor.getValue().getEventTimestamp());
    }
}
