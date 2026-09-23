package com.trustabac.iot.experiment;

import com.trustabac.iot.dto.ResourceOperationResponse;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.experiment.dto.BenchmarkMode;
import com.trustabac.iot.experiment.dto.ExperimentRunRequest;
import com.trustabac.iot.experiment.dto.ExperimentRunResponse;
import com.trustabac.iot.experiment.dto.ScenarioType;
import com.trustabac.iot.experiment.entity.ExperimentRun;
import com.trustabac.iot.experiment.repository.ExperimentMeasurementRepository;
import com.trustabac.iot.experiment.repository.ExperimentRunRepository;
import com.trustabac.iot.experiment.service.ExperimentService;
import com.trustabac.iot.experiment.service.ExperimentStatisticsCalculator;
import com.trustabac.iot.experiment.service.ExperimentWorkloadGenerator;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.service.DecisionCoordinator;
import com.trustabac.iot.service.ResourceOperationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExperimentServiceTest {

    @Test
    @DisplayName("Verify full experiment execution, measurement persistence, and expectation check")
    void testRunExperiment() {
        ResourceOperationService opService = mock(ResourceOperationService.class);
        DecisionCoordinator coord = mock(DecisionCoordinator.class);
        DeviceRepository devRepo = mock(DeviceRepository.class);
        ExperimentRunRepository runRepo = mock(ExperimentRunRepository.class);
        ExperimentMeasurementRepository measRepo = mock(ExperimentMeasurementRepository.class);
        ExperimentWorkloadGenerator generator = new ExperimentWorkloadGenerator();
        ExperimentStatisticsCalculator statsCalc = new ExperimentStatisticsCalculator();

        Device dev = new Device("DOOR-SENSOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        when(devRepo.findByDeviceIdentifier("DOOR-SENSOR-001")).thenReturn(Optional.of(dev));
        when(runRepo.save(any(ExperimentRun.class))).thenAnswer(i -> i.getArgument(0));

        ResourceOperationResponse mockOpResp = new ResourceOperationResponse(
                "OP-123", "DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ",
                EnforcementStatus.EXECUTED, "READ", "OK", Map.of(),
                Decision.ALLOW, "Allow", "PASS", "OK",
                80.0, "TRUSTED", 14.0, "LOW",
                "0xTx12345", 100L, "0xContract", LocalDateTime.now().toString()
        );
        when(opService.executeOperation(any())).thenReturn(mockOpResp);

        ExperimentService service = new ExperimentService(opService, coord, devRepo, runRepo, measRepo, generator, statsCalc);

        ExperimentRunRequest req = new ExperimentRunRequest(ScenarioType.NORMAL_ACCESS, BenchmarkMode.MODE_C_FULL_TRUSTABAC_BLOCKCHAIN, 5, 42L, "EXP-TEST-001");
        ExperimentRunResponse resp = service.runExperiment(req);

        assertNotNull(resp);
        assertEquals("EXP-TEST-001", resp.getRunId());
        assertEquals("COMPLETED", resp.getStatus());
        assertEquals(5, resp.getCompletedOperations());

        verify(opService, times(5)).executeOperation(any());
        verify(measRepo, times(1)).save(any());
    }
}
