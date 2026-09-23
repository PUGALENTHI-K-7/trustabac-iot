package com.trustabac.iot.batch.step;

import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.entity.SecurityAnalytics;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import com.trustabac.iot.batch.repository.SecurityAnalyticsRepository;
import com.trustabac.iot.entity.RiskEvent;
import com.trustabac.iot.entity.RiskStatus;
import com.trustabac.iot.entity.TrustEventType;
import com.trustabac.iot.entity.TrustHistory;
import com.trustabac.iot.repository.RiskEventRepository;
import com.trustabac.iot.repository.TrustHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Batch Tasklet aggregating security incidents, malicious activity classifications,
 * trust mutations, and risk severity distributions.
 */
@Component
public class SecurityAnalyticsTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(SecurityAnalyticsTasklet.class);

    private final TrustHistoryRepository trustHistoryRepository;
    private final RiskEventRepository riskEventRepository;
    private final SecurityAnalyticsRepository securityAnalyticsRepository;
    private final BatchRunAuditRepository batchRunAuditRepository;

    public SecurityAnalyticsTasklet(TrustHistoryRepository trustHistoryRepository,
                                    RiskEventRepository riskEventRepository,
                                    SecurityAnalyticsRepository securityAnalyticsRepository,
                                    BatchRunAuditRepository batchRunAuditRepository) {
        this.trustHistoryRepository = trustHistoryRepository;
        this.riskEventRepository = riskEventRepository;
        this.securityAnalyticsRepository = securityAnalyticsRepository;
        this.batchRunAuditRepository = batchRunAuditRepository;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        var jobParams = chunkContext.getStepContext().getJobParameters();
        String periodKey = (String) jobParams.getOrDefault("periodKey", "ALL_TIME");
        String startStr = (String) jobParams.get("periodStart");
        String endStr = (String) jobParams.get("periodEnd");

        LocalDateTime periodStart = startStr != null ? LocalDateTime.parse(startStr) : LocalDateTime.of(2000, 1, 1, 0, 0);
        LocalDateTime periodEnd = endStr != null ? LocalDateTime.parse(endStr) : LocalDateTime.of(2099, 12, 31, 23, 59);

        Long batchRunAuditId = (Long) chunkContext.getStepContext().getJobExecutionContext().get("batchRunAuditId");
        BatchRunAudit audit = batchRunAuditId != null
                ? batchRunAuditRepository.findById(batchRunAuditId).orElse(null)
                : batchRunAuditRepository.findByPeriodKey(periodKey).orElse(null);

        if (audit == null) {
            log.warn("No BatchRunAudit found for periodKey='{}'. Skipping SecurityAnalyticsStep.", periodKey);
            return RepeatStatus.FINISHED;
        }

        // Check if security analytics already exists
        if (securityAnalyticsRepository.findByBatchRunAudit_Id(audit.getId()).isPresent()) {
            log.info("SecurityAnalytics already exists for batchRunId={}. Skipping duplicate insert.", audit.getId());
            return RepeatStatus.FINISHED;
        }

        List<TrustHistory> trustEvents = trustHistoryRepository.findByEventTimestampBetweenOrderByEventTimestampAsc(periodStart, periodEnd);
        List<RiskEvent> riskEvents = riskEventRepository.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(periodStart, periodEnd);

        long normalSuccess = 0;
        long suspicious = 0;
        long flooding = 0;
        long malicious = 0;
        long recovery = 0;

        for (TrustHistory th : trustEvents) {
            TrustEventType type = th.getEventType();
            if (type == TrustEventType.NORMAL_SUCCESS) normalSuccess++;
            else if (type == TrustEventType.SUSPICIOUS_ACTIVITY) suspicious++;
            else if (type == TrustEventType.REQUEST_FLOODING) flooding++;
            else if (type == TrustEventType.CONFIRMED_MALICIOUS) malicious++;
            else if (type == TrustEventType.RECOVERY) recovery++;
        }

        Double avgDelta = trustEvents.isEmpty() ? 0.0 : trustEvents.stream().mapToDouble(TrustHistory::getDelta).average().orElse(0.0);

        long lowRisk = 0;
        long medRisk = 0;
        long highRisk = 0;

        for (RiskEvent re : riskEvents) {
            RiskStatus st = re.getRiskStatus();
            if (st == RiskStatus.LOW) lowRisk++;
            else if (st == RiskStatus.MEDIUM) medRisk++;
            else if (st == RiskStatus.HIGH) highRisk++;
        }

        SecurityAnalytics sa = new SecurityAnalytics();
        sa.setBatchRunAudit(audit);
        sa.setPeriodKey(periodKey);
        sa.setNormalSuccessCount(normalSuccess);
        sa.setSuspiciousActivityCount(suspicious);
        sa.setRequestFloodingCount(flooding);
        sa.setConfirmedMaliciousCount(malicious);
        sa.setRecoveryCount(recovery);
        sa.setLowRiskCount(lowRisk);
        sa.setMediumRiskCount(medRisk);
        sa.setHighRiskCount(highRisk);
        sa.setAvgTrustDelta(avgDelta);

        securityAnalyticsRepository.save(sa);

        audit.setTotalRecordsRead(audit.getTotalRecordsRead() + trustEvents.size() + riskEvents.size());
        audit.setTotalAnalyticsProduced(audit.getTotalAnalyticsProduced() + 1);
        batchRunAuditRepository.save(audit);

        log.info("SecurityAnalytics completed for period '{}': trustMutations={}, riskAssessments={}",
                periodKey, trustEvents.size(), riskEvents.size());

        return RepeatStatus.FINISHED;
    }
}
