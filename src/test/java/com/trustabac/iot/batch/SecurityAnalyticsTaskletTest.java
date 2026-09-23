package com.trustabac.iot.batch;

import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.entity.SecurityAnalytics;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import com.trustabac.iot.batch.repository.SecurityAnalyticsRepository;
import com.trustabac.iot.batch.step.SecurityAnalyticsTasklet;
import com.trustabac.iot.entity.*;
import com.trustabac.iot.repository.RiskEventRepository;
import com.trustabac.iot.repository.TrustHistoryRepository;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecurityAnalyticsTaskletTest {

    @Test
    @DisplayName("Verify SecurityAnalyticsTasklet aggregates threat event breakdown and risk status distribution")
    void testSecurityAggregation() throws Exception {
        TrustHistoryRepository trustRepo = mock(TrustHistoryRepository.class);
        RiskEventRepository riskRepo = mock(RiskEventRepository.class);
        SecurityAnalyticsRepository secAnalyticsRepo = mock(SecurityAnalyticsRepository.class);
        BatchRunAuditRepository auditRepo = mock(BatchRunAuditRepository.class);

        BatchRunAudit audit = new BatchRunAudit("TEST_PERIOD_003", LocalDateTime.now().minusDays(1), LocalDateTime.now(), "offlineAuditJob", "RUNNING", LocalDateTime.now());
        audit.setId(103L);

        when(auditRepo.findById(103L)).thenReturn(Optional.of(audit));
        when(secAnalyticsRepo.findByBatchRunAudit_Id(103L)).thenReturn(Optional.empty());

        Device dev = new Device("DOOR-SENSOR-001", DeviceType.SMART_DOOR_LOCK, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        TrustHistory th1 = new TrustHistory(dev, "DOOR-SENSOR-001", 80.0, 70.0, -10.0, TrustEventType.SUSPICIOUS_ACTIVITY, "Context flag", "SYS", LocalDateTime.now());
        TrustHistory th2 = new TrustHistory(dev, "DOOR-SENSOR-001", 70.0, 30.0, -40.0, TrustEventType.CONFIRMED_MALICIOUS, "Malicious breach", "SIM", LocalDateTime.now());
        TrustHistory th3 = new TrustHistory(dev, "DOOR-SENSOR-001", 30.0, 40.0, 10.0, TrustEventType.RECOVERY, "Restored", "ADMIN", LocalDateTime.now());
        when(trustRepo.findByEventTimestampBetweenOrderByEventTimestampAsc(any(), any())).thenReturn(List.of(th1, th2, th3));

        RiskEvent re1 = new RiskEvent(dev, "DOOR-SENSOR-001", 14.0, RiskStatus.LOW, "SMART_DOOR_LOCK", Operation.READ, "factors", "reason", LocalDateTime.now());
        RiskEvent re2 = new RiskEvent(dev, "DOOR-SENSOR-001", 75.0, RiskStatus.HIGH, "SMART_DOOR_LOCK", Operation.CONTROL, "factors", "high threat", LocalDateTime.now());
        when(riskRepo.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(any(), any())).thenReturn(List.of(re1, re2));

        SecurityAnalyticsTasklet tasklet = new SecurityAnalyticsTasklet(trustRepo, riskRepo, secAnalyticsRepo, auditRepo);

        org.springframework.batch.core.job.parameters.JobParameters params = new org.springframework.batch.core.job.parameters.JobParametersBuilder()
                .addString("periodKey", "TEST_PERIOD_003")
                .toJobParameters();
        JobExecution jobExecution = new JobExecution(3L, new JobInstance(3L, "offlineAuditJob"), params);
        jobExecution.getExecutionContext().put("batchRunAuditId", 103L);
        StepExecution stepExecution = new StepExecution("testSecStep", jobExecution);
        StepContext stepContext = new StepContext(stepExecution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        StepContribution contribution = new StepContribution(stepExecution);

        RepeatStatus status = tasklet.execute(contribution, chunkContext);
        assertEquals(RepeatStatus.FINISHED, status);

        verify(secAnalyticsRepo, times(1)).save(argThat(sec -> {
            assertEquals("TEST_PERIOD_003", sec.getPeriodKey());
            assertEquals(1, sec.getSuspiciousActivityCount());
            assertEquals(1, sec.getConfirmedMaliciousCount());
            assertEquals(1, sec.getRecoveryCount());
            assertEquals(1, sec.getLowRiskCount());
            assertEquals(1, sec.getHighRiskCount());
            assertEquals(0, sec.getMediumRiskCount());
            return true;
        }));
    }
}
