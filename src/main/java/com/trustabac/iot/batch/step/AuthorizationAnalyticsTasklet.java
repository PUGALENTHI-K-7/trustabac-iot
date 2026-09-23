package com.trustabac.iot.batch.step;

import com.trustabac.iot.batch.entity.AuthorizationAnalytics;
import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.repository.AuthorizationAnalyticsRepository;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import com.trustabac.iot.entity.AbacResult;
import com.trustabac.iot.entity.AccessRequest;
import com.trustabac.iot.entity.BlockchainAuthorizationEvent;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.repository.AccessRequestRepository;
import com.trustabac.iot.repository.BlockchainAuthorizationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spring Batch Tasklet performing read-only aggregation of access requests and blockchain
 * authorization events to produce immutable AuthorizationAnalytics records.
 */
@Component
public class AuthorizationAnalyticsTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationAnalyticsTasklet.class);

    private final AccessRequestRepository accessRequestRepository;
    private final BlockchainAuthorizationEventRepository blockchainRepository;
    private final AuthorizationAnalyticsRepository authorizationAnalyticsRepository;
    private final BatchRunAuditRepository batchRunAuditRepository;

    public AuthorizationAnalyticsTasklet(AccessRequestRepository accessRequestRepository,
                                        BlockchainAuthorizationEventRepository blockchainRepository,
                                        AuthorizationAnalyticsRepository authorizationAnalyticsRepository,
                                        BatchRunAuditRepository batchRunAuditRepository) {
        this.accessRequestRepository = accessRequestRepository;
        this.blockchainRepository = blockchainRepository;
        this.authorizationAnalyticsRepository = authorizationAnalyticsRepository;
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
            log.warn("No BatchRunAudit found for periodKey='{}'. Skipping AuthorizationAnalyticsStep.", periodKey);
            return RepeatStatus.FINISHED;
        }

        // Check if authorization analytics already exists for this run/period
        if (authorizationAnalyticsRepository.findByBatchRunAudit_Id(audit.getId()).isPresent()) {
            log.info("AuthorizationAnalytics already exists for batchRunId={}. Skipping duplicate insert.", audit.getId());
            return RepeatStatus.FINISHED;
        }

        // Read operational records in read-only transaction
        List<AccessRequest> accessRequests = accessRequestRepository.findByRequestTimestampBetweenOrderByRequestTimestampAsc(periodStart, periodEnd);
        List<BlockchainAuthorizationEvent> bcEvents = blockchainRepository.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(periodStart, periodEnd);

        long abacPassCount = 0;
        long abacFailCount = 0;
        long sensitiveDenials = 0;

        for (AccessRequest req : accessRequests) {
            if (req.getAbacResult() == AbacResult.PASS) {
                abacPassCount++;
            } else {
                abacFailCount++;
                if (isSensitiveResource(req.getResource()) || isSensitiveResource(req.getDeviceIdentifier())) {
                    sensitiveDenials++;
                }
            }
        }

        long allowCount = 0;
        long restrictCount = 0;
        long denyCount = 0;
        long matchedProofs = 0;
        long missingProofs = 0;
        long ambiguousProofs = 0;
        Set<String> seenTxHashes = new HashSet<>();

        for (BlockchainAuthorizationEvent ev : bcEvents) {
            if (ev.getDecision() == Decision.ALLOW) {
                allowCount++;
            } else if (ev.getDecision() == Decision.RESTRICT) {
                restrictCount++;
            } else if (ev.getDecision() == Decision.DENY) {
                denyCount++;
                if (isSensitiveResource(ev.getResource()) || isSensitiveResource(ev.getDeviceIdentifier())) {
                    sensitiveDenials++;
                }
            }

            String tx = ev.getTransactionHash();
            if (tx != null && !tx.isBlank() && !tx.equalsIgnoreCase("NONE") && ev.getBlockNumber() != null) {
                if (seenTxHashes.contains(tx)) {
                    ambiguousProofs++;
                } else {
                    seenTxHashes.add(tx);
                    matchedProofs++;
                }
            } else {
                missingProofs++;
            }
        }

        long executedCount = allowCount;
        long downgradedCount = restrictCount;
        long blockedCount = denyCount + abacFailCount;
        long totalRequests = Math.max(accessRequests.size(), bcEvents.size() + abacFailCount);
        long sourceRecordsReconciled = accessRequests.size() + bcEvents.size();

        AuthorizationAnalytics analytics = new AuthorizationAnalytics();
        analytics.setBatchRunAudit(audit);
        analytics.setPeriodKey(periodKey);
        analytics.setTotalRequests(totalRequests);
        analytics.setAllowCount(allowCount);
        analytics.setRestrictCount(restrictCount);
        analytics.setDenyCount(denyCount);
        analytics.setExecutedCount(executedCount);
        analytics.setDowngradedCount(downgradedCount);
        analytics.setBlockedCount(blockedCount);
        analytics.setAbacPassCount(abacPassCount);
        analytics.setAbacFailCount(abacFailCount);
        analytics.setSensitiveResourceDenials(sensitiveDenials);
        analytics.setMatchedBlockchainProofs(matchedProofs);
        analytics.setMissingBlockchainProofs(missingProofs);
        analytics.setAmbiguousBlockchainProofs(ambiguousProofs);
        analytics.setSourceRecordsReconciled(sourceRecordsReconciled);

        authorizationAnalyticsRepository.save(analytics);

        audit.setTotalRecordsRead(audit.getTotalRecordsRead() + sourceRecordsReconciled);
        audit.setTotalAnalyticsProduced(audit.getTotalAnalyticsProduced() + 1);
        batchRunAuditRepository.save(audit);

        log.info("AuthorizationAnalytics completed for period '{}': totalRequests={}, allow={}, restrict={}, deny={}, reconciled={}",
                periodKey, totalRequests, allowCount, restrictCount, denyCount, sourceRecordsReconciled);

        return RepeatStatus.FINISHED;
    }

    private boolean isSensitiveResource(String resource) {
        if (resource == null) return false;
        String r = resource.toUpperCase();
        return r.contains("ADMIN") || r.contains("GATEWAY") || r.contains("ROUTER") ||
               r.contains("CAMERA") || r.contains("CAM-") || r.contains("CURTAIN") || r.contains("OWNER");
    }
}
