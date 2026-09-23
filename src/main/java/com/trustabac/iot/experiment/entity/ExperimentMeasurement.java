package com.trustabac.iot.experiment.entity;

import jakarta.persistence.*;

/**
 * Entity persisting measured experimental statistics and behavior outcomes for an experiment run.
 */
@Entity
@Table(name = "experiment_measurements", indexes = {
        @Index(name = "idx_meas_run_id", columnList = "run_id")
})
public class ExperimentMeasurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "experiment_run_id", nullable = false)
    private ExperimentRun experimentRun;

    @Column(name = "run_id", nullable = false, length = 100)
    private String runId;

    // Sample size
    @Column(name = "sample_size", nullable = false)
    private int sampleSize;

    // Authorization Latency (ms) - Pure ABAC + Trust + Risk + Smart Contract
    @Column(name = "auth_latency_min_ms")
    private Double authLatencyMinMs;

    @Column(name = "auth_latency_max_ms")
    private Double authLatencyMaxMs;

    @Column(name = "auth_latency_mean_ms")
    private Double authLatencyMeanMs;

    @Column(name = "auth_latency_median_ms")
    private Double authLatencyMedianMs;

    @Column(name = "auth_latency_p95_ms")
    private Double authLatencyP95Ms;

    @Column(name = "auth_latency_p99_ms")
    private Double authLatencyP99Ms;

    @Column(name = "auth_latency_std_dev_ms")
    private Double authLatencyStdDevMs;

    @Column(name = "auth_latency_std_error_ms")
    private Double authLatencyStdErrorMs;

    @Column(name = "auth_latency_iqr_ms")
    private Double authLatencyIqrMs;

    @Column(name = "auth_latency_ci95_lower_ms")
    private Double authLatencyCi95LowerMs;

    @Column(name = "auth_latency_ci95_upper_ms")
    private Double authLatencyCi95UpperMs;

    @Column(name = "auth_ci_method", length = 100)
    private String authCiMethod;

    // Enforcement Latency (ms) - ResourceOperationService Authoritative Pipeline
    @Column(name = "enforce_latency_min_ms")
    private Double enforceLatencyMinMs;

    @Column(name = "enforce_latency_max_ms")
    private Double enforceLatencyMaxMs;

    @Column(name = "enforce_latency_mean_ms")
    private Double enforceLatencyMeanMs;

    @Column(name = "enforce_latency_median_ms")
    private Double enforceLatencyMedianMs;

    @Column(name = "enforce_latency_p95_ms")
    private Double enforceLatencyP95Ms;

    @Column(name = "enforce_latency_p99_ms")
    private Double enforceLatencyP99Ms;

    @Column(name = "enforce_latency_std_dev_ms")
    private Double enforceLatencyStdDevMs;

    @Column(name = "enforce_latency_std_error_ms")
    private Double enforceLatencyStdErrorMs;

    @Column(name = "enforce_latency_iqr_ms")
    private Double enforceLatencyIqrMs;

    @Column(name = "enforce_latency_ci95_lower_ms")
    private Double enforceLatencyCi95LowerMs;

    @Column(name = "enforce_latency_ci95_upper_ms")
    private Double enforceLatencyCi95UpperMs;

    @Column(name = "enforce_ci_method", length = 100)
    private String enforceCiMethod;

    // Repetition and Sample Counts
    @Column(name = "warm_up_operations", nullable = false)
    private int warmUpOperations = 0;

    @Column(name = "measured_operations", nullable = false)
    private int measuredOperations = 0;

    @Column(name = "repetition_number", nullable = false)
    private int repetitionNumber = 1;

    @Column(name = "total_repetitions", nullable = false)
    private int totalRepetitions = 1;

    // Decision Breakdown
    @Column(name = "allow_count", nullable = false)
    private long allowCount;

    @Column(name = "restrict_count", nullable = false)
    private long restrictCount;

    @Column(name = "deny_count", nullable = false)
    private long denyCount;

    // Enforcement Breakdown
    @Column(name = "executed_count", nullable = false)
    private long executedCount;

    @Column(name = "downgraded_count", nullable = false)
    private long downgradedCount;

    @Column(name = "blocked_count", nullable = false)
    private long blockedCount;

    // ABAC Breakdown
    @Column(name = "abac_pass_count", nullable = false)
    private long abacPassCount;

    @Column(name = "abac_fail_count", nullable = false)
    private long abacFailCount;

    // Trust & Risk Dynamics
    @Column(name = "initial_trust")
    private Double initialTrust;

    @Column(name = "final_trust")
    private Double finalTrust;

    @Column(name = "trust_delta")
    private Double trustDelta;

    @Column(name = "avg_risk_score")
    private Double avgRiskScore;

    @Column(name = "min_risk_score")
    private Double minRiskScore;

    @Column(name = "max_risk_score")
    private Double maxRiskScore;

    // Blockchain Metrics
    @Column(name = "transaction_count", nullable = false)
    private long transactionCount;

    @Column(name = "total_gas_used", nullable = false)
    private long totalGasUsed;

    @Column(name = "avg_gas_per_tx")
    private Double avgGasPerTx;

    @Column(name = "min_gas_used")
    private Long minGasUsed;

    @Column(name = "max_gas_used")
    private Long maxGasUsed;

    @Column(name = "median_gas_used")
    private Double medianGasUsed;

    @Column(name = "p95_gas_used")
    private Double p95GasUsed;

    @Column(name = "latest_block_number")
    private Long latestBlockNumber;

    // Failure / Outage & Recovery counts
    @Column(name = "fail_closed_blocked_count", nullable = false)
    private long failClosedBlockedCount;

    @Column(name = "recovery_success_count", nullable = false)
    private long recoverySuccessCount;

    public ExperimentMeasurement() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ExperimentRun getExperimentRun() {
        return experimentRun;
    }

    public void setExperimentRun(ExperimentRun experimentRun) {
        this.experimentRun = experimentRun;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public Double getAuthLatencyMinMs() {
        return authLatencyMinMs;
    }

    public void setAuthLatencyMinMs(Double authLatencyMinMs) {
        this.authLatencyMinMs = authLatencyMinMs;
    }

    public Double getAuthLatencyMaxMs() {
        return authLatencyMaxMs;
    }

    public void setAuthLatencyMaxMs(Double authLatencyMaxMs) {
        this.authLatencyMaxMs = authLatencyMaxMs;
    }

    public Double getAuthLatencyMeanMs() {
        return authLatencyMeanMs;
    }

    public void setAuthLatencyMeanMs(Double authLatencyMeanMs) {
        this.authLatencyMeanMs = authLatencyMeanMs;
    }

    public Double getAuthLatencyMedianMs() {
        return authLatencyMedianMs;
    }

    public void setAuthLatencyMedianMs(Double authLatencyMedianMs) {
        this.authLatencyMedianMs = authLatencyMedianMs;
    }

    public Double getAuthLatencyP95Ms() {
        return authLatencyP95Ms;
    }

    public void setAuthLatencyP95Ms(Double authLatencyP95Ms) {
        this.authLatencyP95Ms = authLatencyP95Ms;
    }

    public Double getAuthLatencyP99Ms() {
        return authLatencyP99Ms;
    }

    public void setAuthLatencyP99Ms(Double authLatencyP99Ms) {
        this.authLatencyP99Ms = authLatencyP99Ms;
    }

    public Double getEnforceLatencyMinMs() {
        return enforceLatencyMinMs;
    }

    public void setEnforceLatencyMinMs(Double enforceLatencyMinMs) {
        this.enforceLatencyMinMs = enforceLatencyMinMs;
    }

    public Double getEnforceLatencyMaxMs() {
        return enforceLatencyMaxMs;
    }

    public void setEnforceLatencyMaxMs(Double enforceLatencyMaxMs) {
        this.enforceLatencyMaxMs = enforceLatencyMaxMs;
    }

    public Double getEnforceLatencyMeanMs() {
        return enforceLatencyMeanMs;
    }

    public void setEnforceLatencyMeanMs(Double enforceLatencyMeanMs) {
        this.enforceLatencyMeanMs = enforceLatencyMeanMs;
    }

    public Double getEnforceLatencyMedianMs() {
        return enforceLatencyMedianMs;
    }

    public void setEnforceLatencyMedianMs(Double enforceLatencyMedianMs) {
        this.enforceLatencyMedianMs = enforceLatencyMedianMs;
    }

    public Double getEnforceLatencyP95Ms() {
        return enforceLatencyP95Ms;
    }

    public void setEnforceLatencyP95Ms(Double enforceLatencyP95Ms) {
        this.enforceLatencyP95Ms = enforceLatencyP95Ms;
    }

    public Double getEnforceLatencyP99Ms() {
        return enforceLatencyP99Ms;
    }

    public void setEnforceLatencyP99Ms(Double enforceLatencyP99Ms) {
        this.enforceLatencyP99Ms = enforceLatencyP99Ms;
    }

    public long getAllowCount() {
        return allowCount;
    }

    public void setAllowCount(long allowCount) {
        this.allowCount = allowCount;
    }

    public long getRestrictCount() {
        return restrictCount;
    }

    public void setRestrictCount(long restrictCount) {
        this.restrictCount = restrictCount;
    }

    public long getDenyCount() {
        return denyCount;
    }

    public void setDenyCount(long denyCount) {
        this.denyCount = denyCount;
    }

    public long getExecutedCount() {
        return executedCount;
    }

    public void setExecutedCount(long executedCount) {
        this.executedCount = executedCount;
    }

    public long getDowngradedCount() {
        return downgradedCount;
    }

    public void setDowngradedCount(long downgradedCount) {
        this.downgradedCount = downgradedCount;
    }

    public long getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(long blockedCount) {
        this.blockedCount = blockedCount;
    }

    public long getAbacPassCount() {
        return abacPassCount;
    }

    public void setAbacPassCount(long abacPassCount) {
        this.abacPassCount = abacPassCount;
    }

    public long getAbacFailCount() {
        return abacFailCount;
    }

    public void setAbacFailCount(long abacFailCount) {
        this.abacFailCount = abacFailCount;
    }

    public Double getInitialTrust() {
        return initialTrust;
    }

    public void setInitialTrust(Double initialTrust) {
        this.initialTrust = initialTrust;
    }

    public Double getFinalTrust() {
        return finalTrust;
    }

    public void setFinalTrust(Double finalTrust) {
        this.finalTrust = finalTrust;
    }

    public Double getTrustDelta() {
        return trustDelta;
    }

    public void setTrustDelta(Double trustDelta) {
        this.trustDelta = trustDelta;
    }

    public Double getAvgRiskScore() {
        return avgRiskScore;
    }

    public void setAvgRiskScore(Double avgRiskScore) {
        this.avgRiskScore = avgRiskScore;
    }

    public Double getMinRiskScore() {
        return minRiskScore;
    }

    public void setMinRiskScore(Double minRiskScore) {
        this.minRiskScore = minRiskScore;
    }

    public Double getMaxRiskScore() {
        return maxRiskScore;
    }

    public void setMaxRiskScore(Double maxRiskScore) {
        this.maxRiskScore = maxRiskScore;
    }

    public long getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(long transactionCount) {
        this.transactionCount = transactionCount;
    }

    public long getTotalGasUsed() {
        return totalGasUsed;
    }

    public void setTotalGasUsed(long totalGasUsed) {
        this.totalGasUsed = totalGasUsed;
    }

    public Double getAvgGasPerTx() {
        return avgGasPerTx;
    }

    public void setAvgGasPerTx(Double avgGasPerTx) {
        this.avgGasPerTx = avgGasPerTx;
    }

    public Long getLatestBlockNumber() {
        return latestBlockNumber;
    }

    public void setLatestBlockNumber(Long latestBlockNumber) {
        this.latestBlockNumber = latestBlockNumber;
    }

    public long getFailClosedBlockedCount() {
        return failClosedBlockedCount;
    }

    public void setFailClosedBlockedCount(long failClosedBlockedCount) {
        this.failClosedBlockedCount = failClosedBlockedCount;
    }

    public long getRecoverySuccessCount() {
        return recoverySuccessCount;
    }

    public void setRecoverySuccessCount(long recoverySuccessCount) {
        this.recoverySuccessCount = recoverySuccessCount;
    }

    public Double getAuthLatencyStdDevMs() { return authLatencyStdDevMs; }
    public void setAuthLatencyStdDevMs(Double authLatencyStdDevMs) { this.authLatencyStdDevMs = authLatencyStdDevMs; }

    public Double getAuthLatencyStdErrorMs() { return authLatencyStdErrorMs; }
    public void setAuthLatencyStdErrorMs(Double authLatencyStdErrorMs) { this.authLatencyStdErrorMs = authLatencyStdErrorMs; }

    public Double getAuthLatencyIqrMs() { return authLatencyIqrMs; }
    public void setAuthLatencyIqrMs(Double authLatencyIqrMs) { this.authLatencyIqrMs = authLatencyIqrMs; }

    public Double getAuthLatencyCi95LowerMs() { return authLatencyCi95LowerMs; }
    public void setAuthLatencyCi95LowerMs(Double authLatencyCi95LowerMs) { this.authLatencyCi95LowerMs = authLatencyCi95LowerMs; }

    public Double getAuthLatencyCi95UpperMs() { return authLatencyCi95UpperMs; }
    public void setAuthLatencyCi95UpperMs(Double authLatencyCi95UpperMs) { this.authLatencyCi95UpperMs = authLatencyCi95UpperMs; }

    public String getAuthCiMethod() { return authCiMethod; }
    public void setAuthCiMethod(String authCiMethod) { this.authCiMethod = authCiMethod; }

    public Double getEnforceLatencyStdDevMs() { return enforceLatencyStdDevMs; }
    public void setEnforceLatencyStdDevMs(Double enforceLatencyStdDevMs) { this.enforceLatencyStdDevMs = enforceLatencyStdDevMs; }

    public Double getEnforceLatencyStdErrorMs() { return enforceLatencyStdErrorMs; }
    public void setEnforceLatencyStdErrorMs(Double enforceLatencyStdErrorMs) { this.enforceLatencyStdErrorMs = enforceLatencyStdErrorMs; }

    public Double getEnforceLatencyIqrMs() { return enforceLatencyIqrMs; }
    public void setEnforceLatencyIqrMs(Double enforceLatencyIqrMs) { this.enforceLatencyIqrMs = enforceLatencyIqrMs; }

    public Double getEnforceLatencyCi95LowerMs() { return enforceLatencyCi95LowerMs; }
    public void setEnforceLatencyCi95LowerMs(Double enforceLatencyCi95LowerMs) { this.enforceLatencyCi95LowerMs = enforceLatencyCi95LowerMs; }

    public Double getEnforceLatencyCi95UpperMs() { return enforceLatencyCi95UpperMs; }
    public void setEnforceLatencyCi95UpperMs(Double enforceLatencyCi95UpperMs) { this.enforceLatencyCi95UpperMs = enforceLatencyCi95UpperMs; }

    public String getEnforceCiMethod() { return enforceCiMethod; }
    public void setEnforceCiMethod(String enforceCiMethod) { this.enforceCiMethod = enforceCiMethod; }

    public int getWarmUpOperations() { return warmUpOperations; }
    public void setWarmUpOperations(int warmUpOperations) { this.warmUpOperations = warmUpOperations; }

    public int getMeasuredOperations() { return measuredOperations; }
    public void setMeasuredOperations(int measuredOperations) { this.measuredOperations = measuredOperations; }

    public int getRepetitionNumber() { return repetitionNumber; }
    public void setRepetitionNumber(int repetitionNumber) { this.repetitionNumber = repetitionNumber; }

    public int getTotalRepetitions() { return totalRepetitions; }
    public void setTotalRepetitions(int totalRepetitions) { this.totalRepetitions = totalRepetitions; }

    public Long getMinGasUsed() { return minGasUsed; }
    public void setMinGasUsed(Long minGasUsed) { this.minGasUsed = minGasUsed; }

    public Long getMaxGasUsed() { return maxGasUsed; }
    public void setMaxGasUsed(Long maxGasUsed) { this.maxGasUsed = maxGasUsed; }

    public Double getMedianGasUsed() { return medianGasUsed; }
    public void setMedianGasUsed(Double medianGasUsed) { this.medianGasUsed = medianGasUsed; }

    public Double getP95GasUsed() { return p95GasUsed; }
    public void setP95GasUsed(Double p95GasUsed) { this.p95GasUsed = p95GasUsed; }
}
