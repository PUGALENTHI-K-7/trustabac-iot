package com.trustabac.iot.batch;

import com.trustabac.iot.batch.entity.AuthorizationAnalytics;
import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.repository.AuthorizationAnalyticsRepository;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import com.trustabac.iot.batch.step.AuthorizationAnalyticsTasklet;
import com.trustabac.iot.entity.*;
import com.trustabac.iot.repository.AccessRequestRepository;
import com.trustabac.iot.repository.BlockchainAuthorizationEventRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthorizationAnalyticsTaskletTest {

    @Test
    @DisplayName("Verify AuthorizationAnalyticsTasklet correctly aggregates decisions, proofs, and ABAC results")
    void testAuthorizationAggregation() throws Exception {
        AccessRequestRepository accessRepo = mock(AccessRequestRepository.class);
        BlockchainAuthorizationEventRepository bcRepo = mock(BlockchainAuthorizationEventRepository.class);
        AuthorizationAnalyticsRepository authAnalyticsRepo = mock(AuthorizationAnalyticsRepository.class);
        BatchRunAuditRepository auditRepo = mock(BatchRunAuditRepository.class);

        BatchRunAudit audit = new BatchRunAudit("TEST_PERIOD_001", LocalDateTime.now().minusDays(1), LocalDateTime.now(), "offlineAuditJob", "RUNNING", LocalDateTime.now());
        audit.setId(101L);

        when(auditRepo.findById(101L)).thenReturn(Optional.of(audit));
        when(auditRepo.findByPeriodKey("TEST_PERIOD_001")).thenReturn(Optional.of(audit));
        when(authAnalyticsRepo.findByBatchRunAudit_Id(101L)).thenReturn(Optional.empty());

        // Mock 2 AccessRequests (1 PASS, 1 FAIL on sensitive camera)
        AccessRequest req1 = new AccessRequest("DOOR-SENSOR-001", "guest-1", "GUEST", "ORG", "SMART_DOOR_LOCK", "ACTUATOR", Operation.READ, "PROP", "BOOK-1", "INTERNAL", LocalDateTime.now(), AbacResult.PASS, "OK", "Policy1");
        AccessRequest req2 = new AccessRequest("CAM-001", "guest-1", "GUEST", "ORG", "SECURITY_CAMERA", "SENSOR", Operation.READ, "PROP", "BOOK-1", "INTERNAL", LocalDateTime.now(), AbacResult.FAIL, "Deny", "Policy2");
        when(accessRepo.findByRequestTimestampBetweenOrderByRequestTimestampAsc(any(), any())).thenReturn(List.of(req1, req2));

        // Mock 2 Blockchain Events (1 ALLOW with matched proof, 1 RESTRICT with matched proof)
        BlockchainAuthorizationEvent bc1 = new BlockchainAuthorizationEvent("DOOR-SENSOR-001", "SMART_DOOR_LOCK", "READ", 80.0, 14.0, Decision.ALLOW, "Allow", "0xContract", "0xTx12345", 100L, LocalDateTime.now());
        BlockchainAuthorizationEvent bc2 = new BlockchainAuthorizationEvent("DOOR-SENSOR-001", "SMART_DOOR_LOCK", "CONTROL", 80.0, 35.0, Decision.RESTRICT, "Restrict", "0xContract", "0xTx67890", 101L, LocalDateTime.now());
        when(bcRepo.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(any(), any())).thenReturn(List.of(bc1, bc2));

        AuthorizationAnalyticsTasklet tasklet = new AuthorizationAnalyticsTasklet(accessRepo, bcRepo, authAnalyticsRepo, auditRepo);

        org.springframework.batch.core.job.parameters.JobParameters params = new org.springframework.batch.core.job.parameters.JobParametersBuilder()
                .addString("periodKey", "TEST_PERIOD_001")
                .toJobParameters();
        JobExecution jobExecution = new JobExecution(1L, new JobInstance(1L, "offlineAuditJob"), params);
        jobExecution.getExecutionContext().put("batchRunAuditId", 101L);
        StepExecution stepExecution = new StepExecution("testStep", jobExecution);
        StepContext stepContext = new StepContext(stepExecution);
        ChunkContext chunkContext = new ChunkContext(stepContext);
        StepContribution contribution = new StepContribution(stepExecution);

        RepeatStatus status = tasklet.execute(contribution, chunkContext);
        assertEquals(RepeatStatus.FINISHED, status);

        // Verify analytics was saved
        verify(authAnalyticsRepo, times(1)).save(argThat(analytics -> {
            assertEquals("TEST_PERIOD_001", analytics.getPeriodKey());
            assertEquals(1, analytics.getAllowCount());
            assertEquals(1, analytics.getRestrictCount());
            assertEquals(0, analytics.getDenyCount());
            assertEquals(1, analytics.getExecutedCount());
            assertEquals(1, analytics.getDowngradedCount());
            assertEquals(1, analytics.getAbacPassCount());
            assertEquals(1, analytics.getAbacFailCount());
            assertEquals(1, analytics.getSensitiveResourceDenials());
            assertEquals(2, analytics.getMatchedBlockchainProofs());
            assertEquals(0, analytics.getMissingBlockchainProofs());
            assertEquals(4, analytics.getSourceRecordsReconciled());
            return true;
        }));
    }
}
