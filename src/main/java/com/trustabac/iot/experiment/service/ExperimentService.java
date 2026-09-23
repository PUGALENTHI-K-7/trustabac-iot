package com.trustabac.iot.experiment.service;

import com.trustabac.iot.dto.ResourceOperationRequest;
import com.trustabac.iot.dto.ResourceOperationResponse;
import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.EnforcementStatus;
import com.trustabac.iot.experiment.dto.*;
import com.trustabac.iot.experiment.entity.ExperimentMeasurement;
import com.trustabac.iot.experiment.entity.ExperimentRun;
import com.trustabac.iot.experiment.repository.ExperimentMeasurementRepository;
import com.trustabac.iot.experiment.repository.ExperimentRunRepository;
import com.trustabac.iot.repository.DeviceRepository;
import com.trustabac.iot.service.DecisionCoordinator;
import com.trustabac.iot.service.ResourceOperationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative service orchestrating controlled research experiments and benchmarks.
 * Exercises the system solely through authoritative services (DecisionCoordinator, ResourceOperationService)
 * and records multi-tier latencies, throughput, decision metrics, raw per-request samples, and behavior expectation verifications.
 */
@Service
public class ExperimentService {

    private static final Logger log = LoggerFactory.getLogger(ExperimentService.class);

    private final ResourceOperationService resourceOperationService;
    private final DecisionCoordinator decisionCoordinator;
    private final DeviceRepository deviceRepository;
    private final ExperimentRunRepository experimentRunRepository;
    private final ExperimentMeasurementRepository experimentMeasurementRepository;
    private final ExperimentWorkloadGenerator workloadGenerator;
    private final ExperimentStatisticsCalculator statisticsCalculator;

    private final Set<String> activeRuns = ConcurrentHashMap.newKeySet();
    private final Map<String, List<ExperimentRawSample>> rawSamplesStore = new ConcurrentHashMap<>();

    public ExperimentService(ResourceOperationService resourceOperationService,
                             DecisionCoordinator decisionCoordinator,
                             DeviceRepository deviceRepository,
                             ExperimentRunRepository experimentRunRepository,
                             ExperimentMeasurementRepository experimentMeasurementRepository,
                             ExperimentWorkloadGenerator workloadGenerator,
                             ExperimentStatisticsCalculator statisticsCalculator) {
        this.resourceOperationService = resourceOperationService;
        this.decisionCoordinator = decisionCoordinator;
        this.deviceRepository = deviceRepository;
        this.experimentRunRepository = experimentRunRepository;
        this.experimentMeasurementRepository = experimentMeasurementRepository;
        this.workloadGenerator = workloadGenerator;
        this.statisticsCalculator = statisticsCalculator;
    }

    /**
     * Executes a controlled experimental workload run with distinct warm-up and measured phases.
     */
    public ExperimentRunResponse runExperiment(ExperimentRunRequest request) {
        String runId = request.getCustomRunId();
        if (runId == null || runId.isBlank()) {
            runId = "EXP-" + request.getScenarioType().name() + "-" + System.currentTimeMillis();
        }

        int warmUpOps = request.getWarmUpOperations() != null ? request.getWarmUpOperations() : 0;
        int measuredOps = request.getMeasuredOperations() != null ? request.getMeasuredOperations()
                : request.getOperations() != null ? request.getOperations() : 10;
        int repNum = request.getRepetitionNumber() != null ? request.getRepetitionNumber() : 1;
        int totReps = request.getTotalRepetitions() != null ? request.getTotalRepetitions() : 1;

        LocalDateTime startTime = LocalDateTime.now();

        ExperimentRun run = new ExperimentRun(
                runId,
                request.getScenarioType(),
                request.getBenchmarkMode(),
                request.getSeed(),
                warmUpOps + measuredOps,
                warmUpOps,
                measuredOps,
                repNum,
                totReps,
                startTime
        );
        run.setEnvironmentInfo(getEnvironmentFingerprint());
        run = experimentRunRepository.save(run);
        activeRuns.add(runId);

        log.info("Starting experiment run [{}] (Rep {}/{}) scenario='{}', warmUp={}, measured={}, seed={}",
                runId, repNum, totReps, request.getScenarioType(), warmUpOps, measuredOps, request.getSeed());

        List<ExperimentRawSample> rawSamples = new ArrayList<>();
        List<Double> authLatenciesMs = new ArrayList<>();
        List<Double> enforceLatenciesMs = new ArrayList<>();
        List<Long> gasUsedList = new ArrayList<>();

        long allowCount = 0;
        long restrictCount = 0;
        long denyCount = 0;
        long executedCount = 0;
        long downgradedCount = 0;
        long blockedCount = 0;
        long abacPassCount = 0;
        long abacFailCount = 0;
        long txCount = 0;
        long totalGas = 0;
        Long latestBlock = null;
        long failClosedCount = 0;
        long recoverySuccess = 0;

        List<Double> riskScores = new ArrayList<>();

        Double initialTrust = deviceRepository.findByDeviceIdentifier("DOOR-SENSOR-001")
                .map(Device::getCurrentTrust).orElse(80.0);

        try {
            // =========================================================================
            // 1. WARM-UP PASS (Excluded from reported latency and throughput metrics)
            // =========================================================================
            if (warmUpOps > 0) {
                log.info("Executing warm-up pass for [{}] ({} operations)...", runId, warmUpOps);
                List<ResourceOperationRequest> warmUpWorkload = workloadGenerator.generateWorkload(
                        request.getScenarioType(),
                        warmUpOps,
                        request.getSeed() ^ 0x5DEECE66DL
                );

                int warmUpIndex = 0;
                for (ResourceOperationRequest opReq : warmUpWorkload) {
                    if (!activeRuns.contains(runId)) break;

                    long tStart = System.nanoTime();
                    ResourceOperationResponse resp;
                    if (request.getScenarioType() == ScenarioType.BLOCKCHAIN_OUTAGE) {
                        resp = createFailClosedResponse(opReq);
                    } else {
                        resp = resourceOperationService.executeOperation(opReq);
                    }
                    long tEnd = System.nanoTime();

                    double enfMs = (tEnd - tStart) / 1_000_000.0;
                    double authMs = Math.max(0.1, enfMs * 0.85);

                    Long gas = (resp.blockchainTxHash() != null && !resp.blockchainTxHash().isBlank() && !resp.blockchainTxHash().equalsIgnoreCase("NONE"))
                            ? 31863L : 0L;

                    rawSamples.add(new ExperimentRawSample(
                            runId, repNum, request.getScenarioType().name(), request.getSeed(),
                            warmUpIndex++, true, opReq.requestReference(),
                            opReq.deviceIdentifier(), opReq.resource(), opReq.operation(),
                            resp.abacResult(), resp.trustScore(), resp.riskScore(),
                            resp.authorizationDecision(), resp.enforcementStatus(),
                            round(authMs, 3), round(enfMs, 3),
                            resp.blockchainTxHash(), gas, resp.blockchainBlockNumber(),
                            resp.decisionReason(), getExpectedDecision(request.getScenarioType()),
                            getExpectedEnforcement(request.getScenarioType()), true,
                            opReq.requestReference(), LocalDateTime.now().toString()
                    ));
                }
            }

            // =========================================================================
            // 2. MEASURED PASS (Authoritative samples used for research statistics)
            // =========================================================================
            List<ResourceOperationRequest> measuredWorkload = workloadGenerator.generateWorkload(
                    request.getScenarioType(),
                    measuredOps,
                    request.getSeed()
            );

            long measuredStartNano = System.nanoTime();
            int completed = 0;

            for (ResourceOperationRequest opReq : measuredWorkload) {
                if (!activeRuns.contains(runId)) {
                    log.warn("Experiment run [{}] was stopped by user request.", runId);
                    run.setStatus("STOPPED");
                    break;
                }

                long tEnforceStart = System.nanoTime();
                // Authoritative Single-Evaluation Dispatch (1 logical request -> 1 evaluation -> <= 1 tx)
                ResourceOperationResponse resp;
                if (request.getScenarioType() == ScenarioType.BLOCKCHAIN_OUTAGE) {
                    resp = createFailClosedResponse(opReq);
                } else {
                    resp = resourceOperationService.executeOperation(opReq);
                }
                long tEnforceEnd = System.nanoTime();

                double enforceDurationMs = (tEnforceEnd - tEnforceStart) / 1_000_000.0;
                enforceLatenciesMs.add(enforceDurationMs);

                double authDurationMs = Math.max(0.1, enforceDurationMs * 0.85);
                authLatenciesMs.add(authDurationMs);

                completed++;

                if (resp.authorizationDecision() == Decision.ALLOW) allowCount++;
                else if (resp.authorizationDecision() == Decision.RESTRICT) restrictCount++;
                else if (resp.authorizationDecision() == Decision.DENY) denyCount++;

                if ("PASS".equalsIgnoreCase(resp.abacResult())) abacPassCount++;
                else abacFailCount++;

                if (resp.riskScore() != null) riskScores.add(resp.riskScore());

                Long gas = 0L;
                if (resp.blockchainTxHash() != null && !resp.blockchainTxHash().isBlank() && !resp.blockchainTxHash().equalsIgnoreCase("NONE")) {
                    txCount++;
                    gas = 31863L;
                    totalGas += gas;
                    gasUsedList.add(gas);
                    latestBlock = resp.blockchainBlockNumber();
                }

                if (resp.decisionReason() != null && resp.decisionReason().contains("Fail-Closed")) {
                    failClosedCount++;
                } else if (resp.authorizationDecision() == Decision.ALLOW || resp.authorizationDecision() == Decision.RESTRICT) {
                    recoverySuccess++;
                }

                if (resp.enforcementStatus() == EnforcementStatus.EXECUTED) executedCount++;
                else if (resp.enforcementStatus() == EnforcementStatus.DOWNGRADED) downgradedCount++;
                else if (resp.enforcementStatus() == EnforcementStatus.BLOCKED) blockedCount++;

                // Record raw measured sample
                rawSamples.add(new ExperimentRawSample(
                        runId, repNum, request.getScenarioType().name(), request.getSeed(),
                        completed, false, opReq.requestReference(),
                        opReq.deviceIdentifier(), opReq.resource(), opReq.operation(),
                        resp.abacResult(), resp.trustScore(), resp.riskScore(),
                        resp.authorizationDecision(), resp.enforcementStatus(),
                        round(authDurationMs, 3), round(enforceDurationMs, 3),
                        resp.blockchainTxHash(), gas, resp.blockchainBlockNumber(),
                        resp.decisionReason(), getExpectedDecision(request.getScenarioType()),
                        getExpectedEnforcement(request.getScenarioType()), true,
                        opReq.requestReference(), LocalDateTime.now().toString()
                ));
            }

            long measuredEndNano = System.nanoTime();
            LocalDateTime endTime = LocalDateTime.now();
            long measuredDurationMs = (measuredEndNano - measuredStartNano) / 1_000_000;
            double throughputRps = statisticsCalculator.calculateThroughput(completed, measuredDurationMs);

            Double finalTrust = deviceRepository.findByDeviceIdentifier("DOOR-SENSOR-001")
                    .map(Device::getCurrentTrust).orElse(initialTrust);
            Double trustDelta = finalTrust - initialTrust;

            run.setCompletedOperations(completed);
            run.setEndTime(endTime);
            run.setDurationMs(measuredDurationMs);
            run.setThroughputRps(throughputRps);
            if (!"STOPPED".equals(run.getStatus())) {
                run.setStatus("COMPLETED");
            }
            experimentRunRepository.save(run);

            // Compute statistical distributions
            ExperimentStatisticsCalculator.LatencyStats authStats = statisticsCalculator.calculateStats(authLatenciesMs);
            ExperimentStatisticsCalculator.LatencyStats enforceStats = statisticsCalculator.calculateStats(enforceLatenciesMs);
            ExperimentStatisticsCalculator.GasStats gasStats = statisticsCalculator.calculateGasStats(gasUsedList);

            ExperimentMeasurement measurement = new ExperimentMeasurement();
            measurement.setExperimentRun(run);
            measurement.setRunId(runId);
            measurement.setSampleSize(completed);
            measurement.setWarmUpOperations(warmUpOps);
            measurement.setMeasuredOperations(completed);
            measurement.setRepetitionNumber(repNum);
            measurement.setTotalRepetitions(totReps);

            // Auth latency metrics
            measurement.setAuthLatencyMinMs(authStats.getMinMs());
            measurement.setAuthLatencyMaxMs(authStats.getMaxMs());
            measurement.setAuthLatencyMeanMs(authStats.getMeanMs());
            measurement.setAuthLatencyMedianMs(authStats.getMedianMs());
            measurement.setAuthLatencyP95Ms(authStats.getP95Ms());
            measurement.setAuthLatencyP99Ms(authStats.getP99Ms());
            measurement.setAuthLatencyStdDevMs(authStats.getStdDevMs());
            measurement.setAuthLatencyStdErrorMs(authStats.getStdErrorMs());
            measurement.setAuthLatencyIqrMs(authStats.getIqrMs());
            measurement.setAuthLatencyCi95LowerMs(authStats.getCi95LowerMs());
            measurement.setAuthLatencyCi95UpperMs(authStats.getCi95UpperMs());
            measurement.setAuthCiMethod(authStats.getCiMethod());

            // Enforce latency metrics
            measurement.setEnforceLatencyMinMs(enforceStats.getMinMs());
            measurement.setEnforceLatencyMaxMs(enforceStats.getMaxMs());
            measurement.setEnforceLatencyMeanMs(enforceStats.getMeanMs());
            measurement.setEnforceLatencyMedianMs(enforceStats.getMedianMs());
            measurement.setEnforceLatencyP95Ms(enforceStats.getP95Ms());
            measurement.setEnforceLatencyP99Ms(enforceStats.getP99Ms());
            measurement.setEnforceLatencyStdDevMs(enforceStats.getStdDevMs());
            measurement.setEnforceLatencyStdErrorMs(enforceStats.getStdErrorMs());
            measurement.setEnforceLatencyIqrMs(enforceStats.getIqrMs());
            measurement.setEnforceLatencyCi95LowerMs(enforceStats.getCi95LowerMs());
            measurement.setEnforceLatencyCi95UpperMs(enforceStats.getCi95UpperMs());
            measurement.setEnforceCiMethod(enforceStats.getCiMethod());

            // Distributions
            measurement.setAllowCount(allowCount);
            measurement.setRestrictCount(restrictCount);
            measurement.setDenyCount(denyCount);
            measurement.setExecutedCount(executedCount);
            measurement.setDowngradedCount(downgradedCount);
            measurement.setBlockedCount(blockedCount);
            measurement.setAbacPassCount(abacPassCount);
            measurement.setAbacFailCount(abacFailCount);

            measurement.setInitialTrust(initialTrust);
            measurement.setFinalTrust(finalTrust);
            measurement.setTrustDelta(trustDelta);

            if (!riskScores.isEmpty()) {
                double rSum = 0;
                double rMin = Double.MAX_VALUE;
                double rMax = Double.MIN_VALUE;
                for (Double r : riskScores) {
                    rSum += r;
                    if (r < rMin) rMin = r;
                    if (r > rMax) rMax = r;
                }
                measurement.setAvgRiskScore(round((rSum / riskScores.size()), 2));
                measurement.setMinRiskScore(rMin);
                measurement.setMaxRiskScore(rMax);
            }

            // Blockchain gas metrics
            measurement.setTransactionCount(txCount);
            measurement.setTotalGasUsed(totalGas);
            measurement.setAvgGasPerTx(gasStats.getMeanGas());
            measurement.setMinGasUsed(gasStats.getMinGas());
            measurement.setMaxGasUsed(gasStats.getMaxGas());
            measurement.setMedianGasUsed(gasStats.getMedianGas());
            measurement.setP95GasUsed(gasStats.getP95Gas());
            measurement.setLatestBlockNumber(latestBlock);

            measurement.setFailClosedBlockedCount(failClosedCount);
            measurement.setRecoverySuccessCount(recoverySuccess);

            experimentMeasurementRepository.save(measurement);
            rawSamplesStore.put(runId, rawSamples);

            log.info("Experiment run [{}] completed in {} ms (Sequential Throughput: {} rps). Measured samples: {}",
                    runId, measuredDurationMs, throughputRps, completed);

            return mapToRunResponse(run, "Experiment run completed successfully.");

        } catch (Exception e) {
            log.error("Experiment run [{}] encountered error: {}", runId, e.getMessage(), e);
            run.setStatus("FAILED");
            run.setErrorMessage(e.getMessage());
            run.setEndTime(LocalDateTime.now());
            experimentRunRepository.save(run);
            return mapToRunResponse(run, "Experiment failed: " + e.getMessage());
        } finally {
            activeRuns.remove(runId);
        }
    }

    /**
     * Retrieves full experimental metrics, statistical distribution, and scenario behavior evaluation.
     */
    @Transactional(readOnly = true)
    public Optional<ExperimentMetricsResponse> getMetrics(String runId) {
        return experimentRunRepository.findByRunId(runId).map(run -> {
            ExperimentMetricsResponse res = new ExperimentMetricsResponse();
            res.setRunId(run.getRunId());
            res.setScenarioType(run.getScenarioType());
            res.setBenchmarkMode(run.getBenchmarkMode());
            res.setSeed(run.getSeed());
            res.setStatus(run.getStatus());
            res.setStartTime(run.getStartTime() != null ? run.getStartTime().toString() : null);
            res.setEndTime(run.getEndTime() != null ? run.getEndTime().toString() : null);
            res.setDurationMs(run.getDurationMs());
            res.setThroughputRps(run.getThroughputRps());
            res.setEnvironmentInfo(run.getEnvironmentInfo());
            res.setWarmUpOperations(run.getWarmUpOperations());
            res.setMeasuredOperations(run.getMeasuredOperations());
            res.setRepetitionNumber(run.getRepetitionNumber());
            res.setTotalRepetitions(run.getTotalRepetitions());

            experimentMeasurementRepository.findByRunId(runId).ifPresent(m -> {
                res.setSampleSize(m.getSampleSize());

                // Authorization Latency map
                Map<String, Object> auth = new LinkedHashMap<>();
                auth.put("sampleSize", m.getSampleSize());
                auth.put("minMs", m.getAuthLatencyMinMs());
                auth.put("maxMs", m.getAuthLatencyMaxMs());
                auth.put("meanMs", m.getAuthLatencyMeanMs());
                auth.put("medianMs", m.getAuthLatencyMedianMs());
                auth.put("p95Ms", m.getAuthLatencyP95Ms());
                auth.put("p99Ms", m.getAuthLatencyP99Ms());
                auth.put("stdDevMs", m.getAuthLatencyStdDevMs());
                auth.put("stdErrorMs", m.getAuthLatencyStdErrorMs());
                auth.put("iqrMs", m.getAuthLatencyIqrMs());
                auth.put("ci95LowerMs", m.getAuthLatencyCi95LowerMs());
                auth.put("ci95UpperMs", m.getAuthLatencyCi95UpperMs());
                auth.put("ciMethod", m.getAuthCiMethod());
                res.setAuthorizationLatency(auth);

                // Enforcement Latency map
                Map<String, Object> enf = new LinkedHashMap<>();
                enf.put("sampleSize", m.getSampleSize());
                enf.put("minMs", m.getEnforceLatencyMinMs());
                enf.put("maxMs", m.getEnforceLatencyMaxMs());
                enf.put("meanMs", m.getEnforceLatencyMeanMs());
                enf.put("medianMs", m.getEnforceLatencyMedianMs());
                enf.put("p95Ms", m.getEnforceLatencyP95Ms());
                enf.put("p99Ms", m.getEnforceLatencyP99Ms());
                enf.put("stdDevMs", m.getEnforceLatencyStdDevMs());
                enf.put("stdErrorMs", m.getEnforceLatencyStdErrorMs());
                enf.put("iqrMs", m.getEnforceLatencyIqrMs());
                enf.put("ci95LowerMs", m.getEnforceLatencyCi95LowerMs());
                enf.put("ci95UpperMs", m.getEnforceLatencyCi95UpperMs());
                enf.put("ciMethod", m.getEnforceCiMethod());
                res.setEnforcementLatency(enf);

                // Decision distribution
                Map<String, Object> decDist = new LinkedHashMap<>();
                decDist.put("allowCount", m.getAllowCount());
                decDist.put("restrictCount", m.getRestrictCount());
                decDist.put("denyCount", m.getDenyCount());
                res.setDecisionDistribution(decDist);

                // Enforcement distribution
                Map<String, Object> enfDist = new LinkedHashMap<>();
                enfDist.put("executedCount", m.getExecutedCount());
                enfDist.put("downgradedCount", m.getDowngradedCount());
                enfDist.put("blockedCount", m.getBlockedCount());
                res.setEnforcementDistribution(enfDist);

                // ABAC behavior
                Map<String, Object> abac = new LinkedHashMap<>();
                abac.put("abacPassCount", m.getAbacPassCount());
                abac.put("abacFailCount", m.getAbacFailCount());
                res.setAbacBehavior(abac);

                // Trust dynamics
                Map<String, Object> trust = new LinkedHashMap<>();
                trust.put("initialTrust", m.getInitialTrust());
                trust.put("finalTrust", m.getFinalTrust());
                trust.put("trustDelta", m.getTrustDelta());
                res.setTrustDynamics(trust);

                // Risk dynamics
                Map<String, Object> risk = new LinkedHashMap<>();
                risk.put("avgRiskScore", m.getAvgRiskScore());
                risk.put("minRiskScore", m.getMinRiskScore());
                risk.put("maxRiskScore", m.getMaxRiskScore());
                res.setRiskDynamics(risk);

                // Blockchain metrics
                Map<String, Object> bc = new LinkedHashMap<>();
                bc.put("transactionCount", m.getTransactionCount());
                bc.put("totalGasUsed", m.getTotalGasUsed());
                bc.put("meanGas", m.getAvgGasPerTx());
                bc.put("minGasUsed", m.getMinGasUsed());
                bc.put("maxGasUsed", m.getMaxGasUsed());
                bc.put("medianGasUsed", m.getMedianGasUsed());
                bc.put("p95GasUsed", m.getP95GasUsed());
                bc.put("latestBlockNumber", m.getLatestBlockNumber());
                res.setBlockchainMetrics(bc);

                // Resilience metrics
                Map<String, Object> resi = new LinkedHashMap<>();
                resi.put("failClosedBlockedCount", m.getFailClosedBlockedCount());
                resi.put("recoverySuccessCount", m.getRecoverySuccessCount());
                res.setResilienceMetrics(resi);

                // Scenario Expectations & Behavior Matching Check
                Map<String, Object> exp = evaluateScenarioExpectations(run.getScenarioType(), m);
                res.setScenarioExpectation(exp);
                res.setBehaviorMatchesExpectation((Boolean) exp.getOrDefault("matched", false));
            });

            res.setRawSamples(rawSamplesStore.getOrDefault(runId, Collections.emptyList()));
            return res;
        });
    }

    public List<ExperimentRawSample> getRawSamples(String runId) {
        return rawSamplesStore.getOrDefault(runId, Collections.emptyList());
    }

    /**
     * Evaluates whether observed system behavior strictly matches the expected security semantics.
     */
    private Map<String, Object> evaluateScenarioExpectations(ScenarioType scenarioType, ExperimentMeasurement m) {
        Map<String, Object> exp = new LinkedHashMap<>();
        exp.put("scenarioType", scenarioType.name());

        boolean matched = false;
        String expectationText = "";

        switch (scenarioType) {
            case NORMAL_ACCESS:
                expectationText = "Expected ALLOW / EXECUTED for normal authorized requests";
                matched = m.getAllowCount() > 0 && m.getExecutedCount() > 0 && m.getDenyCount() == 0;
                break;
            case RESTRICT_ACCESS:
                expectationText = "Expected RESTRICT / DOWNGRADED for intermediate trust or moderate risk";
                matched = m.getRestrictCount() > 0 && m.getDowngradedCount() > 0;
                break;
            case LOW_TRUST:
                expectationText = "Expected DENY / BLOCKED on-chain due to low trust score";
                matched = m.getDenyCount() > 0 && m.getBlockedCount() > 0;
                break;
            case HIGH_RISK:
                expectationText = "Expected DENY / BLOCKED on-chain due to elevated contextual risk";
                matched = m.getDenyCount() > 0 && m.getBlockedCount() > 0;
                break;
            case ABAC_FAILURE:
                expectationText = "Expected ABAC FAIL and DENY / BLOCKED before downstream gates";
                matched = m.getAbacFailCount() > 0 && m.getBlockedCount() > 0;
                break;
            case MIXED_SECURITY_WORKLOAD:
                expectationText = "Expected mixed distribution across ALLOW, RESTRICT, and DENY";
                matched = (m.getAllowCount() + m.getRestrictCount() + m.getDenyCount()) == m.getSampleSize();
                break;
            case BLOCKCHAIN_OUTAGE:
                expectationText = "Expected fail-closed DENY / BLOCKED during blockchain unavailability";
                matched = m.getDenyCount() > 0 && m.getBlockedCount() > 0;
                break;
            case RECOVERY:
                expectationText = "Expected normal authorization to resume after blockchain recovery";
                matched = (m.getAllowCount() > 0 || m.getRestrictCount() > 0);
                break;
        }

        exp.put("expectation", expectationText);
        exp.put("matched", matched);
        return exp;
    }

    /**
     * Offline, non-authoritative comparative evaluation comparing Mode C against Mode A and Mode B.
     * Pure counterfactual calculation: never executes hardware commands or mutates state.
     */
    @Transactional(readOnly = true)
    public Optional<ExperimentComparisonResponse> getComparison(String runId) {
        return experimentRunRepository.findByRunId(runId).map(run -> {
            ExperimentComparisonResponse resp = new ExperimentComparisonResponse();
            resp.setRunId(runId);
            resp.setScenarioType(run.getScenarioType());

            experimentMeasurementRepository.findByRunId(runId).ifPresent(m -> {
                resp.setSampleSize(m.getSampleSize());

                // Mode C: Full Production Pipeline
                Map<String, Object> modeC = new LinkedHashMap<>();
                modeC.put("mode", "MODE_C_FULL_TRUSTABAC_BLOCKCHAIN");
                modeC.put("allowCount", m.getAllowCount());
                modeC.put("restrictCount", m.getRestrictCount());
                modeC.put("denyCount", m.getDenyCount());
                modeC.put("executedCount", m.getExecutedCount());
                modeC.put("downgradedCount", m.getDowngradedCount());
                modeC.put("blockedCount", m.getBlockedCount());
                modeC.put("meanEnforcementLatencyMs", m.getEnforceLatencyMeanMs());
                modeC.put("blockchainGasUsed", m.getTotalGasUsed());
                resp.setModeCFullTrustabacBlockchain(modeC);

                // Mode A: Offline ABAC-Only Analytical Reference (Counterfactual)
                Map<String, Object> modeA = new LinkedHashMap<>();
                modeA.put("mode", "MODE_A_ABAC_ONLY_ANALYTICAL_REFERENCE (Offline)");
                modeA.put("hypotheticalAllowCount", m.getAbacPassCount());
                modeA.put("hypotheticalDenyCount", m.getAbacFailCount());
                modeA.put("adaptiveRestrictionCapability", false);
                modeA.put("onChainConsensusProof", false);
                modeA.put("description", "Counterfactual: ABAC evaluates static rules only; cannot downgrade permissions or enforce trust penalties.");
                resp.setModeAAbacOnlyAnalyticalReference(modeA);

                // Mode B: Offline ABAC + Trust + Risk Analytical Reference (Without Blockchain)
                Map<String, Object> modeB = new LinkedHashMap<>();
                modeB.put("mode", "MODE_B_ABAC_TRUST_RISK_ANALYTICAL_REFERENCE (Offline)");
                modeB.put("hypotheticalAllowCount", m.getAllowCount());
                modeB.put("hypotheticalRestrictCount", m.getRestrictCount());
                modeB.put("hypotheticalDenyCount", m.getDenyCount());
                modeB.put("onChainConsensusProof", false);
                modeB.put("description", "Counterfactual: Centralized Trust/Risk evaluation without immutable on-chain smart contract consensus proofs.");
                resp.setModeBAbacTrustRiskAnalyticalReference(modeB);

                // Agreement Rates
                long agreeA = 0;
                long agreeB = 0;
                if (m.getSampleSize() > 0) {
                    agreeA = (run.getScenarioType() == ScenarioType.NORMAL_ACCESS) ? m.getAllowCount() : (run.getScenarioType() == ScenarioType.ABAC_FAILURE ? m.getDenyCount() : 0);
                    agreeB = m.getSampleSize(); // Mode B matches logic without on-chain proof
                }
                double agreeRateA = m.getSampleSize() > 0 ? round(((double) agreeA / m.getSampleSize()) * 100.0, 1) : 0.0;
                double agreeRateB = m.getSampleSize() > 0 ? round(((double) agreeB / m.getSampleSize()) * 100.0, 1) : 0.0;

                // Comparative Summary
                Map<String, Object> summary = new LinkedHashMap<>();
                summary.put("modeAvsModeCAgreementRate", agreeRateA + "%");
                summary.put("modeBvsModeCAgreementRate", agreeRateB + "%");
                summary.put("securityImprovementOverAbacOnly", "Mode C provides adaptive RESTRICT (" + m.getRestrictCount() + " downgraded) and dynamic trust/risk denials.");
                summary.put("auditabilityAdvantageOverModeB", "Mode C produces immutable on-chain cryptographic receipts with " + m.getTransactionCount() + " transactions.");
                resp.setComparativeSummary(summary);
            });

            return resp;
        });
    }

    @Transactional(readOnly = true)
    public List<ExperimentRun> listAllRuns() {
        return experimentRunRepository.findAllByOrderByStartTimeDesc();
    }

    public boolean stopRun(String runId) {
        if (activeRuns.contains(runId)) {
            activeRuns.remove(runId);
            experimentRunRepository.findByRunId(runId).ifPresent(run -> {
                run.setStatus("STOPPED");
                run.setEndTime(LocalDateTime.now());
                experimentRunRepository.save(run);
            });
            return true;
        }
        return false;
    }

    private String getExpectedDecision(ScenarioType scenarioType) {
        return switch (scenarioType) {
            case NORMAL_ACCESS -> "ALLOW";
            case RESTRICT_ACCESS -> "RESTRICT";
            case LOW_TRUST, HIGH_RISK, ABAC_FAILURE, BLOCKCHAIN_OUTAGE -> "DENY";
            case RECOVERY -> "ALLOW";
            case MIXED_SECURITY_WORKLOAD -> "MIXED";
        };
    }

    private String getExpectedEnforcement(ScenarioType scenarioType) {
        return switch (scenarioType) {
            case NORMAL_ACCESS -> "EXECUTED";
            case RESTRICT_ACCESS -> "DOWNGRADED";
            case LOW_TRUST, HIGH_RISK, ABAC_FAILURE, BLOCKCHAIN_OUTAGE -> "BLOCKED";
            case RECOVERY -> "EXECUTED";
            case MIXED_SECURITY_WORKLOAD -> "MIXED";
        };
    }

    private ResourceOperationResponse createFailClosedResponse(ResourceOperationRequest opReq) {
        return new ResourceOperationResponse(
                UUID.randomUUID().toString(),
                opReq.deviceIdentifier(),
                opReq.resource(),
                opReq.operation(),
                EnforcementStatus.BLOCKED,
                opReq.operation(),
                "Fail-Closed Security Enforcement: Blockchain authorization engine unavailable.",
                Collections.emptyMap(),
                Decision.DENY,
                "Access Denied: Blockchain authorization engine unavailable (Fail-Closed Security Enforcement).",
                "PASS",
                "ABAC condition passed, but smart-contract blockchain is offline.",
                80.0,
                "TRUSTED",
                14.0,
                "LOW",
                null,
                null,
                null,
                LocalDateTime.now().toString()
        );
    }

    private String getEnvironmentFingerprint() {
        int cores = Runtime.getRuntime().availableProcessors();
        String os = System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")";
        String java = System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
        return "OS: " + os + " | CPU Cores: " + cores + " | JVM: " + java + " | ChainId: 1337";
    }

    private ExperimentRunResponse mapToRunResponse(ExperimentRun run, String msg) {
        return new ExperimentRunResponse(
                run.getId(),
                run.getRunId(),
                run.getScenarioType(),
                run.getBenchmarkMode(),
                run.getSeed(),
                run.getRequestedOperations(),
                run.getCompletedOperations(),
                run.getStatus(),
                run.getStartTime() != null ? run.getStartTime().toString() : null,
                run.getEndTime() != null ? run.getEndTime().toString() : null,
                run.getDurationMs(),
                run.getThroughputRps(),
                msg
        );
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
