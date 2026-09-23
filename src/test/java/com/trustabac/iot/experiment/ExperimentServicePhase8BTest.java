package com.trustabac.iot.experiment;

import com.trustabac.iot.dto.ResourceOperationResponse;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.experiment.dto.BenchmarkMode;
import com.trustabac.iot.experiment.dto.ExperimentComparisonResponse;
import com.trustabac.iot.experiment.dto.ExperimentMetricsResponse;
import com.trustabac.iot.experiment.dto.ExperimentRawSample;
import com.trustabac.iot.experiment.dto.ExperimentRunRequest;
import com.trustabac.iot.experiment.dto.ExperimentRunResponse;
import com.trustabac.iot.experiment.dto.ScenarioType;
import com.trustabac.iot.experiment.entity.ExperimentMeasurement;
import com.trustabac.iot.experiment.entity.ExperimentRun;
import com.trustabac.iot.experiment.repository.ExperimentMeasurementRepository;
import com.trustabac.iot.experiment.repository.ExperimentRunRepository;
import com.trustabac.iot.experiment.service.ExperimentService;
import com.trustabac.iot.experiment.service.ExperimentStatisticsCalculator;
import com.trustabac.iot.experiment.service.ExperimentWorkloadGenerator;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.service.DecisionCoordinator;
import com.trustabac.iot.service.ResourceOperationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Phase 8B: ExperimentService Warm-Up, Raw Samples, and State Isolation Tests")
class ExperimentServicePhase8BTest {

    private ResourceOperationService opService;
    private DecisionCoordinator coordinator;
    private DeviceRepository deviceRepository;
    private ExperimentRunRepository runRepository;
    private ExperimentMeasurementRepository measurementRepository;
    private ExperimentWorkloadGenerator workloadGenerator;
    private ExperimentStatisticsCalculator statisticsCalculator;
    private ExperimentService experimentService;

    @BeforeEach
    void setUp() {
        opService = mock(ResourceOperationService.class);
        coordinator = mock(DecisionCoordinator.class);
        deviceRepository = mock(DeviceRepository.class);
        runRepository = mock(ExperimentRunRepository.class);
        measurementRepository = mock(ExperimentMeasurementRepository.class);
        workloadGenerator = new ExperimentWorkloadGenerator();
        statisticsCalculator = new ExperimentStatisticsCalculator();

        Device dev = new Device("DOOR-SENSOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        when(deviceRepository.findByDeviceIdentifier("DOOR-SENSOR-001")).thenReturn(Optional.of(dev));
        when(runRepository.save(any(ExperimentRun.class))).thenAnswer(i -> i.getArgument(0));
        when(measurementRepository.save(any(ExperimentMeasurement.class))).thenAnswer(i -> i.getArgument(0));

        ResourceOperationResponse mockResp = new ResourceOperationResponse(
                "OP-TEST", "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ",
                EnforcementStatus.EXECUTED, "READ", "OK", Map.of(),
                Decision.ALLOW, "Allow reason", "PASS", "Policy OK",
                80.0, "TRUSTED", 14.0, "LOW",
                "0xTxHash123", 101L, "0xContract", LocalDateTime.now().toString()
        );
        when(opService.executeOperation(any())).thenReturn(mockResp);

        experimentService = new ExperimentService(
                opService, coordinator, deviceRepository,
                runRepository, measurementRepository,
                workloadGenerator, statisticsCalculator
        );
    }

    @Test
    @DisplayName("Should execute warm-up pass, exclude warm-up from measured sample stats, and record raw samples")
    void testWarmUpAndMeasuredSampleSeparation() {
        ExperimentRunRequest req = new ExperimentRunRequest(
                ScenarioType.NORMAL_ACCESS, BenchmarkMode.MODE_C_FULL_TRUSTABAC_BLOCKCHAIN,
                10, 4, 10, 2, 3, 101L, "EXP-PHASE8B-001"
        );

        ExperimentRunResponse resp = experimentService.runExperiment(req);

        assertNotNull(resp);
        assertEquals("COMPLETED", resp.getStatus());
        assertEquals(10, resp.getCompletedOperations()); // Measured ops only

        // Total operations executed: 4 warm-up + 10 measured = 14 operations
        verify(opService, times(14)).executeOperation(any());

        // Verify measurement persistence
        ArgumentCaptor<ExperimentMeasurement> measCaptor = ArgumentCaptor.forClass(ExperimentMeasurement.class);
        verify(measurementRepository, times(1)).save(measCaptor.capture());
        ExperimentMeasurement savedMeas = measCaptor.getValue();

        assertEquals(10, savedMeas.getSampleSize());
        assertEquals(4, savedMeas.getWarmUpOperations());
        assertEquals(10, savedMeas.getMeasuredOperations());
        assertEquals(2, savedMeas.getRepetitionNumber());
        assertEquals(3, savedMeas.getTotalRepetitions());
        assertNotNull(savedMeas.getAuthLatencyStdDevMs());
        assertNotNull(savedMeas.getAuthLatencyCi95LowerMs());
        assertNotNull(savedMeas.getAuthLatencyCi95UpperMs());
        assertTrue(savedMeas.getAuthCiMethod().contains("STUDENT_T"));

        // Verify raw samples collection: 4 warm-up + 10 measured = 14 samples
        List<ExperimentRawSample> rawSamples = experimentService.getRawSamples("EXP-PHASE8B-001");
        assertNotNull(rawSamples);
        assertEquals(14, rawSamples.size());

        long warmUpSampleCount = rawSamples.stream().filter(ExperimentRawSample::isWarmUp).count();
        long measuredSampleCount = rawSamples.stream().filter(s -> !s.isWarmUp()).count();

        assertEquals(4, warmUpSampleCount);
        assertEquals(10, measuredSampleCount);
    }

    @Test
    @DisplayName("Should enforce anti-double-authorization: 1 logical request invokes executeOperation exactly once")
    void testAntiDoubleAuthorizationInvariant() {
        ExperimentRunRequest req = new ExperimentRunRequest(
                ScenarioType.RESTRICT_ACCESS, BenchmarkMode.MODE_C_FULL_TRUSTABAC_BLOCKCHAIN,
                5, 0, 5, 1, 1, 202L, "EXP-PHASE8B-SINGLE"
        );

        experimentService.runExperiment(req);

        // Exactly 5 invocations for 5 operations (no duplicate authorization calls)
        verify(opService, times(5)).executeOperation(any());
    }

    @Test
    @DisplayName("Should verify Mode A/B offline comparison without mutating state or triggering transactions")
    void testModeAandBOfflineComparison() {
        ExperimentRun run = new ExperimentRun(
                "EXP-PHASE8B-COMP", ScenarioType.NORMAL_ACCESS,
                BenchmarkMode.MODE_C_FULL_TRUSTABAC_BLOCKCHAIN, 42L, 10, LocalDateTime.now()
        );
        when(runRepository.findByRunId("EXP-PHASE8B-COMP")).thenReturn(Optional.of(run));

        ExperimentMeasurement measurement = new ExperimentMeasurement();
        measurement.setRunId("EXP-PHASE8B-COMP");
        measurement.setSampleSize(10);
        measurement.setAllowCount(10);
        measurement.setRestrictCount(0);
        measurement.setDenyCount(0);
        measurement.setAbacPassCount(10);
        measurement.setAbacFailCount(0);
        measurement.setTransactionCount(10);
        measurement.setTotalGasUsed(318630L);
        when(measurementRepository.findByRunId("EXP-PHASE8B-COMP")).thenReturn(Optional.of(measurement));

        Optional<ExperimentComparisonResponse> compOpt = experimentService.getComparison("EXP-PHASE8B-COMP");
        assertTrue(compOpt.isPresent());

        ExperimentComparisonResponse comp = compOpt.get();
        assertEquals("EXP-PHASE8B-COMP", comp.getRunId());
        assertNotNull(comp.getModeAAbacOnlyAnalyticalReference());
        assertNotNull(comp.getModeBAbacTrustRiskAnalyticalReference());
        assertNotNull(comp.getModeCFullTrustabacBlockchain());

        // Zero additional operation executions during analytical comparison
        verify(opService, never()).executeOperation(any());
        verify(deviceRepository, never()).save(any());
    }
}
