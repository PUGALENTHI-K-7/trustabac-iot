package com.trustabac.iot.batch.dto;

public class BatchAuditStatusResponse {

    private Long batchRunId;
    private Long jobExecutionId;
    private String periodKey;
    private String status;
    private String exitCode;
    private String startTime;
    private String endTime;
    private Long totalRecordsRead;
    private Long totalAnalyticsProduced;

    public BatchAuditStatusResponse() {
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

    public String getExitCode() {
        return exitCode;
    }

    public void setExitCode(String exitCode) {
        this.exitCode = exitCode;
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

    public Long getTotalRecordsRead() {
        return totalRecordsRead;
    }

    public void setTotalRecordsRead(Long totalRecordsRead) {
        this.totalRecordsRead = totalRecordsRead;
    }

    public Long getTotalAnalyticsProduced() {
        return totalAnalyticsProduced;
    }

    public void setTotalAnalyticsProduced(Long totalAnalyticsProduced) {
        this.totalAnalyticsProduced = totalAnalyticsProduced;
    }
}
