package com.trustabac.iot.batch.dto;

public class BatchRunResponse {

    private Long batchRunId;
    private Long jobExecutionId;
    private String periodKey;
    private String status;
    private String message;
    private boolean alreadyProcessed;

    public BatchRunResponse() {
    }

    public BatchRunResponse(Long batchRunId, Long jobExecutionId, String periodKey,
                            String status, String message, boolean alreadyProcessed) {
        this.batchRunId = batchRunId;
        this.jobExecutionId = jobExecutionId;
        this.periodKey = periodKey;
        this.status = status;
        this.message = message;
        this.alreadyProcessed = alreadyProcessed;
    }

    public Long getBatchRunId() {
        return batchRunId;
    }

    public void setBatchRunId(Long batchRunId) {
        this.batchRunId = batchRunId;
    }

    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isAlreadyProcessed() {
        return alreadyProcessed;
    }

    public void setAlreadyProcessed(boolean alreadyProcessed) {
        this.alreadyProcessed = alreadyProcessed;
    }
}
