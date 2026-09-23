package com.trustabac.iot.experiment.dto;

public class ExperimentRunResponse {

    private Long id;
    private String runId;
    private ScenarioType scenarioType;
    private BenchmarkMode benchmarkMode;
    private long seed;
    private int requestedOperations;
    private int completedOperations;
    private String status;
    private String startTime;
    private String endTime;
    private Long durationMs;
    private Double throughputRps;
    private String message;

    public ExperimentRunResponse() {
    }

    public ExperimentRunResponse(Long id, String runId, ScenarioType scenarioType, BenchmarkMode benchmarkMode, long seed, int requestedOperations, int completedOperations, String status, String startTime, String endTime, Long durationMs, Double throughputRps, String message) {
        this.id = id;
        this.runId = runId;
        this.scenarioType = scenarioType;
        this.benchmarkMode = benchmarkMode;
        this.seed = seed;
        this.requestedOperations = requestedOperations;
        this.completedOperations = completedOperations;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.durationMs = durationMs;
        this.throughputRps = throughputRps;
        this.message = message;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public int getRequestedOperations() {
        return requestedOperations;
    }

    public void setRequestedOperations(int requestedOperations) {
        this.requestedOperations = requestedOperations;
    }

    public int getCompletedOperations() {
        return completedOperations;
    }

    public void setCompletedOperations(int completedOperations) {
        this.completedOperations = completedOperations;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
