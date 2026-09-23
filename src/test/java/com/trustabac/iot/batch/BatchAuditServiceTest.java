package com.trustabac.iot.batch;

import com.trustabac.iot.batch.dto.BatchReportSummaryResponse;
import com.trustabac.iot.batch.dto.BatchRunRequest;
import com.trustabac.iot.batch.dto.BatchRunResponse;
import com.trustabac.iot.batch.entity.AuthorizationAnalytics;
import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.repository.AuthorizationAnalyticsRepository;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import com.trustabac.iot.batch.repository.DeviceAnalyticsRepository;
import com.trustabac.iot.batch.repository.SecurityAnalyticsRepository;
import com.trustabac.iot.batch.service.BatchAuditService;
import com.trustabac.iot.repository.AccessRequestRepository;
import com.trustabac.iot.repository.BlockchainAuthorizationEventRepository;
import com.trustabac.iot.repository.RiskEventRepository;
import com.trustabac.iot.repository.TrustHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BatchAuditServiceTest {

    @Test
    @DisplayName("Verify repeated analytical run for identical period returns existing report without double counting")
    void testIdempotentRerun() {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        BatchRunAuditRepository auditRepo = mock(BatchRunAuditRepository.class);
        AuthorizationAnalyticsRepository authRepo = mock(AuthorizationAnalyticsRepository.class);
        DeviceAnalyticsRepository devRepo = mock(DeviceAnalyticsRepository.class);
        SecurityAnalyticsRepository secRepo = mock(SecurityAnalyticsRepository.class);
        AccessRequestRepository accessRepo = mock(AccessRequestRepository.class);
        BlockchainAuthorizationEventRepository bcRepo = mock(BlockchainAuthorizationEventRepository.class);
        TrustHistoryRepository trustRepo = mock(TrustHistoryRepository.class);
        RiskEventRepository riskRepo = mock(RiskEventRepository.class);

        BatchRunAudit existingAudit = new BatchRunAudit("PERIOD_IDEMPOTENT_001", LocalDateTime.now().minusDays(7), LocalDateTime.now(), "offlineAuditJob", "COMPLETED", LocalDateTime.now());
        existingAudit.setId(201L);
        existingAudit.setJobExecutionId(501L);

        when(auditRepo.findByPeriodKey("PERIOD_IDEMPOTENT_001")).thenReturn(Optional.of(existingAudit));

        BatchAuditService service = new BatchAuditService(launcher, job, auditRepo, authRepo, devRepo, secRepo, accessRepo, bcRepo, trustRepo, riskRepo);

        BatchRunRequest req = new BatchRunRequest("PERIOD_IDEMPOTENT_001", null, null, false);
        BatchRunResponse res = service.runAudit(req);

        assertNotNull(res);
        assertTrue(res.isAlreadyProcessed());
        assertEquals("COMPLETED", res.getStatus());
        assertEquals(201L, res.getBatchRunId());
        assertEquals(501L, res.getJobExecutionId());

        // Verify JobLauncher was NOT called (no double execution)
        verifyNoInteractions(launcher);
    }

    @Test
    @DisplayName("Verify new period run launches Spring Batch job and sets status RUNNING")
    void testNewPeriodRun() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        BatchRunAuditRepository auditRepo = mock(BatchRunAuditRepository.class);
        AuthorizationAnalyticsRepository authRepo = mock(AuthorizationAnalyticsRepository.class);
        DeviceAnalyticsRepository devRepo = mock(DeviceAnalyticsRepository.class);
        SecurityAnalyticsRepository secRepo = mock(SecurityAnalyticsRepository.class);
        AccessRequestRepository accessRepo = mock(AccessRequestRepository.class);
        BlockchainAuthorizationEventRepository bcRepo = mock(BlockchainAuthorizationEventRepository.class);
        TrustHistoryRepository trustRepo = mock(TrustHistoryRepository.class);
        RiskEventRepository riskRepo = mock(RiskEventRepository.class);

        when(auditRepo.findByPeriodKey("PERIOD_NEW_001")).thenReturn(Optional.empty());

        JobExecution jobExec = new JobExecution(601L, new JobInstance(601L, "offlineAuditJob"), new JobParameters());
        jobExec.setStatus(BatchStatus.STARTED);
        when(launcher.run(any(), any())).thenReturn(jobExec);

        BatchAuditService service = new BatchAuditService(launcher, job, auditRepo, authRepo, devRepo, secRepo, accessRepo, bcRepo, trustRepo, riskRepo);

        BatchRunRequest req = new BatchRunRequest("PERIOD_NEW_001", "2026-09-01T00:00:00", "2026-09-22T00:00:00", false);
        BatchRunResponse res = service.runAudit(req);

        assertNotNull(res);
        assertFalse(res.isAlreadyProcessed());
        assertEquals("STARTED", res.getStatus());

        verify(launcher, times(1)).run(any(), any());
    }

    @Test
    @DisplayName("Verify report summary computes source reconciliation metrics accurately")
    void testReportSummaryReconciliation() {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        BatchRunAuditRepository auditRepo = mock(BatchRunAuditRepository.class);
        AuthorizationAnalyticsRepository authRepo = mock(AuthorizationAnalyticsRepository.class);
        DeviceAnalyticsRepository devRepo = mock(DeviceAnalyticsRepository.class);
        SecurityAnalyticsRepository secRepo = mock(SecurityAnalyticsRepository.class);
        AccessRequestRepository accessRepo = mock(AccessRequestRepository.class);
        BlockchainAuthorizationEventRepository bcRepo = mock(BlockchainAuthorizationEventRepository.class);
        TrustHistoryRepository trustRepo = mock(TrustHistoryRepository.class);
        RiskEventRepository riskRepo = mock(RiskEventRepository.class);

        BatchRunAudit audit = new BatchRunAudit("PERIOD_RECON_001", LocalDateTime.now().minusDays(1), LocalDateTime.now(), "offlineAuditJob", "COMPLETED", LocalDateTime.now());
        audit.setId(301L);
        when(auditRepo.findByPeriodKey("PERIOD_RECON_001")).thenReturn(Optional.of(audit));

        AuthorizationAnalytics auth = new AuthorizationAnalytics();
        auth.setTotalRequests(10L);
        auth.setAllowCount(8L);
        auth.setRestrictCount(1L);
        auth.setDenyCount(1L);
        auth.setSourceRecordsReconciled(10L);
        when(authRepo.findByBatchRunAudit_Id(301L)).thenReturn(Optional.of(auth));

        when(devRepo.findAllByBatchRunAudit_Id(301L)).thenReturn(Collections.emptyList());
        when(secRepo.findByBatchRunAudit_Id(301L)).thenReturn(Optional.empty());

        when(accessRepo.findByRequestTimestampBetweenOrderByRequestTimestampAsc(any(), any())).thenReturn(Collections.emptyList());
        when(bcRepo.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(any(), any())).thenReturn(Collections.emptyList());
        when(trustRepo.findByEventTimestampBetweenOrderByEventTimestampAsc(any(), any())).thenReturn(Collections.emptyList());
        when(riskRepo.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(any(), any())).thenReturn(Collections.emptyList());

        BatchAuditService service = new BatchAuditService(launcher, job, auditRepo, authRepo, devRepo, secRepo, accessRepo, bcRepo, trustRepo, riskRepo);

        Optional<BatchReportSummaryResponse> reportOpt = service.getReport("PERIOD_RECON_001");
        assertTrue(reportOpt.isPresent());
        BatchReportSummaryResponse report = reportOpt.get();

        assertNotNull(report.getAuthorization());
        assertEquals(10L, report.getAuthorization().get("totalRequests"));
        assertNotNull(report.getReconciliation());
        assertEquals(true, report.getReconciliation().get("reconciledSuccessfully"));
    }
}
