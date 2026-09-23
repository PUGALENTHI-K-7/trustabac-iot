package com.trustabac.iot.batch.listener;

import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Job execution listener managing BatchRunAudit lifecycle and idempotency recording.
 */
@Component
public class BatchAuditJobExecutionListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchAuditJobExecutionListener.class);

    private final BatchRunAuditRepository batchRunAuditRepository;

    public BatchAuditJobExecutionListener(BatchRunAuditRepository batchRunAuditRepository) {
        this.batchRunAuditRepository = batchRunAuditRepository;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        var params = jobExecution.getJobParameters();
        String periodKey = params.getString("periodKey", "ALL_TIME");
        String startStr = params.getString("periodStart");
        String endStr = params.getString("periodEnd");

        LocalDateTime periodStart = startStr != null ? LocalDateTime.parse(startStr) : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime periodEnd = endStr != null ? LocalDateTime.parse(endStr) : LocalDateTime.of(2099, 12, 31, 23, 59);

        // Check if audit record already exists for this period
        BatchRunAudit audit = batchRunAuditRepository.findByPeriodKey(periodKey).orElse(null);

        if (audit == null) {
            audit = new BatchRunAudit();
            audit.setPeriodKey(periodKey);
            audit.setPeriodStart(periodStart);
            audit.setPeriodEnd(periodEnd);
            audit.setJobName(jobExecution.getJobInstance().getJobName());
            audit.setJobExecutionId(jobExecution.getId());
            audit.setStatus("RUNNING");
            audit.setStartTime(LocalDateTime.now());
            audit = batchRunAuditRepository.save(audit);
            log.info("Initialized new BatchRunAudit [ID: {}] for periodKey='{}'", audit.getId(), periodKey);
        } else {
            audit.setJobExecutionId(jobExecution.getId());
            audit.setStatus("RUNNING");
            audit = batchRunAuditRepository.save(audit);
            log.info("Reusing existing BatchRunAudit [ID: {}] for periodKey='{}'", audit.getId(), periodKey);
        }

        jobExecution.getExecutionContext().put("batchRunAuditId", audit.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Long auditId = (Long) jobExecution.getExecutionContext().get("batchRunAuditId");
        if (auditId != null) {
            batchRunAuditRepository.findById(auditId).ifPresent(audit -> {
                audit.setEndTime(LocalDateTime.now());
                audit.setExitCode(jobExecution.getExitStatus().getExitCode());
                audit.setExitMessage(jobExecution.getExitStatus().getExitDescription());
                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    audit.setStatus("COMPLETED");
                } else {
                    audit.setStatus("FAILED");
                }
                batchRunAuditRepository.save(audit);
                log.info("Finalized BatchRunAudit [ID: {}] with status='{}', totalRecordsRead={}, analyticsProduced={}",
                        audit.getId(), audit.getStatus(), audit.getTotalRecordsRead(), audit.getTotalAnalyticsProduced());
            });
        }
    }
}
