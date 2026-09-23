package com.trustabac.iot.batch.step;

import com.trustabac.iot.batch.entity.BatchRunAudit;
import com.trustabac.iot.batch.entity.DeviceAnalytics;
import com.trustabac.iot.batch.repository.BatchRunAuditRepository;
import com.trustabac.iot.batch.repository.DeviceAnalyticsRepository;
import com.trustabac.iot.entity.*;
import com.trustabac.iot.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Spring Batch Tasklet aggregating access traffic, decisions, trust bounds, and risk bounds
 * grouped per IoT device.
 */
@Component
public class DeviceAnalyticsTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(DeviceAnalyticsTasklet.class);

    private final DeviceRepository deviceRepository;
    private final AccessRequestRepository accessRequestRepository;
    private final BlockchainAuthorizationEventRepository blockchainRepository;
    private final TrustHistoryRepository trustHistoryRepository;
    private final RiskEventRepository riskEventRepository;
    private final DeviceAnalyticsRepository deviceAnalyticsRepository;
    private final BatchRunAuditRepository batchRunAuditRepository;

    public DeviceAnalyticsTasklet(DeviceRepository deviceRepository,
                                  AccessRequestRepository accessRequestRepository,
                                  BlockchainAuthorizationEventRepository blockchainRepository,
                                  TrustHistoryRepository trustHistoryRepository,
                                  RiskEventRepository riskEventRepository,
                                  DeviceAnalyticsRepository deviceAnalyticsRepository,
                                  BatchRunAuditRepository batchRunAuditRepository) {
        this.deviceRepository = deviceRepository;
        this.accessRequestRepository = accessRequestRepository;
        this.blockchainRepository = blockchainRepository;
        this.trustHistoryRepository = trustHistoryRepository;
        this.riskEventRepository = riskEventRepository;
        this.deviceAnalyticsRepository = deviceAnalyticsRepository;
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
            log.warn("No BatchRunAudit found for periodKey='{}'. Skipping DeviceAnalyticsStep.", periodKey);
            return RepeatStatus.FINISHED;
        }

        // Check if device analytics already exists for this run
        if (!deviceAnalyticsRepository.findAllByBatchRunAudit_Id(audit.getId()).isEmpty()) {
            log.info("DeviceAnalytics already exists for batchRunId={}. Skipping duplicate insert.", audit.getId());
            return RepeatStatus.FINISHED;
        }

        // Collect all distinct device identifiers
        Set<String> deviceIds = new LinkedHashSet<>();
        deviceRepository.findAll().forEach(d -> deviceIds.add(d.getDeviceIdentifier()));

        List<AccessRequest> accessRequests = accessRequestRepository.findByRequestTimestampBetweenOrderByRequestTimestampAsc(periodStart, periodEnd);
        List<BlockchainAuthorizationEvent> bcEvents = blockchainRepository.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(periodStart, periodEnd);
        List<TrustHistory> trustEvents = trustHistoryRepository.findByEventTimestampBetweenOrderByEventTimestampAsc(periodStart, periodEnd);
        List<RiskEvent> riskEvents = riskEventRepository.findByEvaluationTimestampBetweenOrderByEvaluationTimestampAsc(periodStart, periodEnd);

        accessRequests.forEach(r -> deviceIds.add(r.getDeviceIdentifier()));
        bcEvents.forEach(e -> deviceIds.add(e.getDeviceIdentifier()));

        List<DeviceAnalytics> resultList = new ArrayList<>();

        for (String devId : deviceIds) {
            if (devId == null || devId.isBlank()) continue;

            long totalReqs = accessRequests.stream().filter(r -> devId.equalsIgnoreCase(r.getDeviceIdentifier())).count();
            long allows = bcEvents.stream().filter(e -> devId.equalsIgnoreCase(e.getDeviceIdentifier()) && e.getDecision() == Decision.ALLOW).count();
            long restricts = bcEvents.stream().filter(e -> devId.equalsIgnoreCase(e.getDeviceIdentifier()) && e.getDecision() == Decision.RESTRICT).count();
            long denies = bcEvents.stream().filter(e -> devId.equalsIgnoreCase(e.getDeviceIdentifier()) && e.getDecision() == Decision.DENY).count();

            // Trust bounds
            List<Double> trustScores = trustEvents.stream()
                    .filter(t -> devId.equalsIgnoreCase(t.getDeviceIdentifier()))
                    .map(TrustHistory::getNewTrust)
                    .toList();

            Double avgTrust = null;
            Double minTrust = null;
            Double maxTrust = null;
            if (!trustScores.isEmpty()) {
                avgTrust = trustScores.stream().mapToDouble(Double::doubleValue).average().orElse(80.0);
                minTrust = trustScores.stream().mapToDouble(Double::doubleValue).min().orElse(80.0);
                maxTrust = trustScores.stream().mapToDouble(Double::doubleValue).max().orElse(80.0);
            } else {
                Optional<Device> devOpt = deviceRepository.findByDeviceIdentifier(devId);
                if (devOpt.isPresent()) {
                    double current = devOpt.get().getCurrentTrust();
                    avgTrust = current;
                    minTrust = current;
                    maxTrust = current;
                }
            }

            // Risk bounds
            List<Double> riskScores = riskEvents.stream()
                    .filter(r -> devId.equalsIgnoreCase(r.getDeviceIdentifier()))
                    .map(RiskEvent::getRiskScore)
                    .toList();

            Double avgRisk = null;
            Double minRisk = null;
            Double maxRisk = null;
            if (!riskScores.isEmpty()) {
                avgRisk = riskScores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                minRisk = riskScores.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
                maxRisk = riskScores.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
            }

            DeviceAnalytics da = new DeviceAnalytics();
            da.setBatchRunAudit(audit);
            da.setPeriodKey(periodKey);
            da.setDeviceIdentifier(devId);
            da.setTotalRequests(totalReqs);
            da.setAllowCount(allows);
            da.setRestrictCount(restricts);
            da.setDenyCount(denies);
            da.setAvgTrustScore(avgTrust);
            da.setMinTrustScore(minTrust);
            da.setMaxTrustScore(maxTrust);
            da.setAvgRiskScore(avgRisk);
            da.setMinRiskScore(minRisk);
            da.setMaxRiskScore(maxRisk);

            resultList.add(da);
        }

        deviceAnalyticsRepository.saveAll(resultList);

        audit.setTotalAnalyticsProduced(audit.getTotalAnalyticsProduced() + resultList.size());
        batchRunAuditRepository.save(audit);

        log.info("DeviceAnalytics completed for period '{}': aggregated {} devices.", periodKey, resultList.size());

        return RepeatStatus.FINISHED;
    }
}
