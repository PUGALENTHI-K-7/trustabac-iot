package com.trustabac.iot.experiment.dto;

public class ExperimentRunRequest {

    private ScenarioType scenarioType = ScenarioType.NORMAL_ACCESS;
    private BenchmarkMode benchmarkMode = BenchmarkMode.MODE_C_FULL_TRUSTABAC_BLOCKCHAIN;
    private Integer operations = 10;
    private Integer warmUpOperations = 0;
    private Integer measuredOperations;
    private Integer repetitionNumber = 1;
    private Integer totalRepetitions = 1;
    private Long seed = 42L;
    private String customRunId;

    public ExperimentRunRequest() {
    }

    public ExperimentRunRequest(ScenarioType scenarioType, BenchmarkMode benchmarkMode, Integer operations, Long seed, String customRunId) {
        this(scenarioType, benchmarkMode, operations, 0, operations, 1, 1, seed, customRunId);
    }

    public ExperimentRunRequest(ScenarioType scenarioType, BenchmarkMode benchmarkMode, Integer operations,
                                Integer warmUpOperations, Integer measuredOperations,
                                Integer repetitionNumber, Integer totalRepetitions,
                                Long seed, String customRunId) {
        this.scenarioType = scenarioType != null ? scenarioType : ScenarioType.NORMAL_ACCESS;
        this.benchmarkMode = benchmarkMode != null ? benchmarkMode : BenchmarkMode.MODE_C_FULL_TRUSTABAC_BLOCKCHAIN;
        this.operations = operations != null ? operations : 10;
        this.warmUpOperations = warmUpOperations != null ? warmUpOperations : 0;
        this.measuredOperations = measuredOperations != null ? measuredOperations : this.operations;
        this.repetitionNumber = repetitionNumber != null ? repetitionNumber : 1;
        this.totalRepetitions = totalRepetitions != null ? totalRepetitions : 1;
        this.seed = seed != null ? seed : 42L;
        this.customRunId = customRunId;
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

    public Integer getOperations() {
        return operations;
    }

    public void setOperations(Integer operations) {
        this.operations = operations;
    }

    public Integer getWarmUpOperations() {
        return warmUpOperations;
    }

    public void setWarmUpOperations(Integer warmUpOperations) {
        this.warmUpOperations = warmUpOperations;
    }

    public Integer getMeasuredOperations() {
        return measuredOperations != null ? measuredOperations : operations;
    }

    public void setMeasuredOperations(Integer measuredOperations) {
        this.measuredOperations = measuredOperations;
    }

    public Integer getRepetitionNumber() {
        return repetitionNumber;
    }

    public void setRepetitionNumber(Integer repetitionNumber) {
        this.repetitionNumber = repetitionNumber;
    }

    public Integer getTotalRepetitions() {
        return totalRepetitions;
    }

    public void setTotalRepetitions(Integer totalRepetitions) {
        this.totalRepetitions = totalRepetitions;
    }

    public Long getSeed() {
        return seed;
    }

    public void setSeed(Long seed) {
        this.seed = seed;
    }

    public String getCustomRunId() {
        return customRunId;
    }

    public void setCustomRunId(String customRunId) {
        this.customRunId = customRunId;
    }
}
