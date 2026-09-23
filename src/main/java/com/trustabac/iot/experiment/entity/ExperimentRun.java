package com.trustabac.iot.experiment.entity;

import com.trustabac.iot.experiment.dto.BenchmarkMode;
import com.trustabac.iot.experiment.dto.ScenarioType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity tracking an individual execution of an experimental workload run.
 */
@Entity
@Table(name = "experiment_runs", indexes = {
        @Index(name = "idx_run_id", columnList = "run_id", unique = true),
        @Index(name = "idx_scenario_type", columnList = "scenario_type")
})
public class ExperimentRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "run_id", unique = true, nullable = false, length = 100)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(name = "scenario_type", nullable = false, length = 50)
    private ScenarioType scenarioType;

    @Enumerated(EnumType.STRING)
    @Column(name = "benchmark_mode", nullable = false, length = 50)
    private BenchmarkMode benchmarkMode = BenchmarkMode.MODE_C_FULL_TRUSTABAC_BLOCKCHAIN;

    @Column(name = "random_seed", nullable = false)
    private long seed;

    @Column(name = "requested_operations", nullable = false)
    private int requestedOperations;

    @Column(name = "warm_up_operations", nullable = false)
    private int warmUpOperations = 0;

    @Column(name = "measured_operations", nullable = false)
    private int measuredOperations = 0;

    @Column(name = "repetition_number", nullable = false)
    private int repetitionNumber = 1;

    @Column(name = "total_repetitions", nullable = false)
    private int totalRepetitions = 1;

    @Column(name = "completed_operations", nullable = false)
    private int completedOperations;

    @Column(name = "status", nullable = false, length = 30)
    private String status; // RUNNING, COMPLETED, FAILED, STOPPED

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "throughput_rps")
    private Double throughputRps;

    @Column(name = "environment_info", length = 255)
    private String environmentInfo;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public ExperimentRun() {
    }

    public ExperimentRun(String runId, ScenarioType scenarioType, BenchmarkMode benchmarkMode, long seed, int requestedOperations, LocalDateTime startTime) {
        this(runId, scenarioType, benchmarkMode, seed, requestedOperations, 0, requestedOperations, 1, 1, startTime);
    }

    public ExperimentRun(String runId, ScenarioType scenarioType, BenchmarkMode benchmarkMode, long seed,
                         int requestedOperations, int warmUpOperations, int measuredOperations,
                         int repetitionNumber, int totalRepetitions, LocalDateTime startTime) {
        this.runId = runId;
        this.scenarioType = scenarioType;
        this.benchmarkMode = benchmarkMode;
        this.seed = seed;
        this.requestedOperations = requestedOperations;
        this.warmUpOperations = warmUpOperations;
        this.measuredOperations = measuredOperations;
        this.repetitionNumber = repetitionNumber;
        this.totalRepetitions = totalRepetitions;
        this.completedOperations = 0;
        this.status = "RUNNING";
        this.startTime = startTime;
        this.environmentInfo = "Java " + System.getProperty("java.version") + " / " + System.getProperty("os.name") + " / Ganache (1337)";
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

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
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

    public int getWarmUpOperations() {
        return warmUpOperations;
    }

    public void setWarmUpOperations(int warmUpOperations) {
        this.warmUpOperations = warmUpOperations;
    }

    public int getMeasuredOperations() {
        return measuredOperations;
    }

    public void setMeasuredOperations(int measuredOperations) {
        this.measuredOperations = measuredOperations;
    }

    public int getRepetitionNumber() {
        return repetitionNumber;
    }

    public void setRepetitionNumber(int repetitionNumber) {
        this.repetitionNumber = repetitionNumber;
    }

    public int getTotalRepetitions() {
        return totalRepetitions;
    }

    public void setTotalRepetitions(int totalRepetitions) {
        this.totalRepetitions = totalRepetitions;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
