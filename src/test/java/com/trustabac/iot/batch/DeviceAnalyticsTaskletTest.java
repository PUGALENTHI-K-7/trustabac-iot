package com.trustabac.iot.batch;

import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.entity.DeviceAnalytics;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import com.trustabac.iot.batch.repository.DeviceAnalyticsRepository;
import com.trustabac.iot.batch.step.DeviceAnalyticsTasklet;
import com.trustabac.iot.entity.*;
import com.trustabac.iot.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeviceAnalyticsTaskletTest {

    @Test
    @DisplayName("Verify DeviceAnalyticsTasklet calculates per-device traffic, trust bounds, and risk bounds")
    void testDeviceAggregation() throws Exception {
        DeviceRepository devRepo = mock(DeviceRepository.class);
        AccessRequestRepository accessRepo = mock(AccessRequestRepository.class);
        BlockchainAuthorizationEventRepository bcRepo = mock(BlockchainAuthorizationEventRepository.class);
        TrustHistoryRepository trustRepo = mock(TrustHistoryRepository.class);
        RiskEventRepository riskRepo = mock(RiskEventRepository.class);
        DeviceAnalyticsRepository deviceAnalyticsRepo = mock(DeviceAnalyticsRepository.class);
        BatchRunAuditRepository auditRepo = mock(BatchRunAuditRepository.class);

        BatchRunAudit audit = new BatchRunAudit("TEST_PERIOD_002", LocalDateTime.now().minusDays(1), LocalDateTime.now(), "offlineAuditJob", "RUNNING", LocalDateTime.now());
        audit.setId(102L);

        when(auditRepo.findById(102L)).thenReturn(Optional.of(audit));
        when(deviceAnalyticsRepo.findAllByBatchRunAudit_Id(102L)).thenReturn(Collections.emptyList());

        Device door = new Device("DOOR-SENSOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        when(devRepo.findAll()).thenReturn(List.of(door));
        when(devRepo.findByDeviceIdentifier("DOOR-SENSOR-001")).thenReturn(Optional.of(door));

        // Mock 1 blockchain event for door
        BlockchainAuthorizationEvent bc = new BlockchainAuthorizationEvent("DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", 80.0, 14.0, Decision.ALLOW, "OK", "0xContract", "0xTx", 100L, LocalDateTime.now());
        when(bcRepo.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(any(), any())).thenReturn(List.of(bc));
        when(accessRepo.findByRequestTimestampBetweenOrderByRequestTimestampAsc(any(), any())).thenReturn(Collections.emptyList());

        // Mock 2 trust history records (newTrust: 80.0 and 70.0)
        TrustHistory th1 = new TrustHistory(door, "DOOR-SENSOR-001", 80.0, 80.0, 0.0, TrustEventType.NORMAL_SUCCESS, "OK", "SYS", LocalDateTime.now());
        TrustHistory th2 = new TrustHistory(door, "DOOR-SENSOR-001", 80.0, 70.0, -10.0, TrustEventType.SUSPICIOUS_ACTIVITY, "Context flag", "SYS", LocalDateTime.now());
        when(trustRepo.findByEventTimestampBetweenOrderByEventTimestampAsc(any(), any())).thenReturn(List.of(th1, th2));

        // Mock 1 risk event (14.0)
        RiskEvent re = new RiskEvent(door, "DOOR-SENSOR-001", 14.0, RiskStatus.LOW, "SMART_DOOR_LOCK", Operation.READ, "factors", "reason", LocalDateTime.now());
        when(riskRepo.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(any(), any())).thenReturn(List.of(re));

        DeviceAnalyticsTasklet tasklet = new DeviceAnalyticsTasklet(devRepo, accessRepo, bcRepo, trustRepo, riskRepo, deviceAnalyticsRepo, auditRepo);

        org.springframework.batch.core.job.parameters.JobParameters params = new org.springframework.batch.core.job.parameters.JobParametersBuilder()
                .addString("periodKey", "TEST_PERIOD_002")
                .toJobParameters();
        JobExecution jobExecution = new JobExecution(2L, new JobInstance(2L, "offlineAuditJob"), params);
        jobExecution.getExecutionContext().put("batchRunAuditId", 102L);
        StepExecution stepExecution = new StepExecution("testDevStep", jobExecution);
        StepContext stepContext = new StepContext(stepExecution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        StepContribution contribution = new StepContribution(stepExecution);

        RepeatStatus status = tasklet.execute(contribution, chunkContext);
        assertEquals(RepeatStatus.FINISHED, status);

        verify(deviceAnalyticsRepo, times(1)).saveAll(argThat(list -> {
            List<DeviceAnalytics> devList = (List<DeviceAnalytics>) list;
            assertEquals(1, devList.size());
            DeviceAnalytics d = devList.get(0);
            assertEquals("DOOR-SENSOR-001", d.getDeviceIdentifier());
            assertEquals(1, d.getAllowCount());
            assertEquals(75.0, d.getAvgTrustScore());
            assertEquals(70.0, d.getMinTrustScore());
            assertEquals(80.0, d.getMaxTrustScore());
            assertEquals(14.0, d.getAvgRiskScore());
            return true;
        }));
    }
}
