package com.trustabac.iot.experiment.dto;

import java.util.Map;

public class ExperimentMetricsResponse {

    private String runId;
    private ScenarioType scenarioType;
    private BenchmarkMode benchmarkMode;
    private long seed;
    private int sampleSize;
    private int warmUpOperations;
    private int measuredOperations;
    private int repetitionNumber;
    private int totalRepetitions;
    private String status;
    private String startTime;
    private String endTime;
    private Long durationMs;
    private Double throughputRps;
    private String environmentInfo;

    // Raw sample records for unaggregated research datasets
    private java.util.List<ExperimentRawSample> rawSamples;

    // Latency Sub-Metrics
    private Map<String, Object> authorizationLatency;
    private Map<String, Object> enforcementLatency;

    // Behavioral Distributions
    private Map<String, Object> decisionDistribution;
    private Map<String, Object> enforcementDistribution;
    private Map<String, Object> abacBehavior;
    private Map<String, Object> trustDynamics;
    private Map<String, Object> riskDynamics;
    private Map<String, Object> blockchainMetrics;
    private Map<String, Object> resilienceMetrics;

    private Map<String, Object> scenarioExpectation;
    private boolean behaviorMatchesExpectation;

    public ExperimentMetricsResponse() {
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public ScenarioType getScenarioType() {
        return scenarioType;
    }

    public void setScenarioType(ScenarioType scenarioType) {
        this.scenarioType = scenarioType;
    }

    public BenchmarkMode getBenchmarkMode() {
        return benchmarkMode;
    }

    public void setBenchmarkMode(BenchmarkMode benchmarkMode) {
        this.benchmarkMode = benchmarkMode;
    }

    public long getSeed() {
        return seed;
    }

    public void setSeed(long seed) {
        this.seed = seed;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public void setSampleSize(int sampleSize) {
        this.sampleSize = sampleSize;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public Double getThroughputRps() {
        return throughputRps;
    }

    public void setThroughputRps(Double throughputRps) {
        this.throughputRps = throughputRps;
    }

    public String getEnvironmentInfo() {
        return environmentInfo;
    }

    public void setEnvironmentInfo(String environmentInfo) {
        this.environmentInfo = environmentInfo;
    }

    public Map<String, Object> getAuthorizationLatency() {
        return authorizationLatency;
    }

    public void setAuthorizationLatency(Map<String, Object> authorizationLatency) {
        this.authorizationLatency = authorizationLatency;
    }

    public Map<String, Object> getEnforcementLatency() {
        return enforcementLatency;
    }

    public void setEnforcementLatency(Map<String, Object> enforcementLatency) {
        this.enforcementLatency = enforcementLatency;
    }

    public Map<String, Object> getDecisionDistribution() {
        return decisionDistribution;
    }

    public void setDecisionDistribution(Map<String, Object> decisionDistribution) {
        this.decisionDistribution = decisionDistribution;
    }

    public Map<String, Object> getEnforcementDistribution() {
        return enforcementDistribution;
    }

    public void setEnforcementDistribution(Map<String, Object> enforcementDistribution) {
        this.enforcementDistribution = enforcementDistribution;
    }

    public Map<String, Object> getAbacBehavior() {
        return abacBehavior;
    }

    public void setAbacBehavior(Map<String, Object> abacBehavior) {
        this.abacBehavior = abacBehavior;
    }

    public Map<String, Object> getTrustDynamics() {
        return trustDynamics;
    }

    public void setTrustDynamics(Map<String, Object> trustDynamics) {
        this.trustDynamics = trustDynamics;
    }

    public Map<String, Object> getRiskDynamics() {
        return riskDynamics;
    }

    public void setRiskDynamics(Map<String, Object> riskDynamics) {
        this.riskDynamics = riskDynamics;
    }

    public Map<String, Object> getBlockchainMetrics() {
        return blockchainMetrics;
    }

    public void setBlockchainMetrics(Map<String, Object> blockchainMetrics) {
        this.blockchainMetrics = blockchainMetrics;
    }

    public Map<String, Object> getResilienceMetrics() {
        return resilienceMetrics;
    }

    public void setResilienceMetrics(Map<String, Object> resilienceMetrics) {
        this.resilienceMetrics = resilienceMetrics;
    }

    public Map<String, Object> getScenarioExpectation() {
        return scenarioExpectation;
    }

    public void setScenarioExpectation(Map<String, Object> scenarioExpectation) {
        this.scenarioExpectation = scenarioExpectation;
    }

    public int getWarmUpOperations() { return warmUpOperations; }
    public void setWarmUpOperations(int warmUpOperations) { this.warmUpOperations = warmUpOperations; }

    public int getMeasuredOperations() { return measuredOperations; }
    public void setMeasuredOperations(int measuredOperations) { this.measuredOperations = measuredOperations; }

    public int getRepetitionNumber() { return repetitionNumber; }
    public void setRepetitionNumber(int repetitionNumber) { this.repetitionNumber = repetitionNumber; }

    public int getTotalRepetitions() { return totalRepetitions; }
    public void setTotalRepetitions(int totalRepetitions) { this.totalRepetitions = totalRepetitions; }

    public java.util.List<ExperimentRawSample> getRawSamples() { return rawSamples; }
    public void setRawSamples(java.util.List<ExperimentRawSample> rawSamples) { this.rawSamples = rawSamples; }

    public boolean isBehaviorMatchesExpectation() {
        return behaviorMatchesExpectation;
    }

    public void setBehaviorMatchesExpectation(boolean behaviorMatchesExpectation) {
        this.behaviorMatchesExpectation = behaviorMatchesExpectation;
    }
}
