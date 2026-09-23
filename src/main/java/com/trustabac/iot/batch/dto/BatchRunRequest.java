package com.trustabac.iot.batch.dto;

public class BatchRunRequest {

    private String periodKey;
    private String startDate;
    private String endDate;
    private Boolean forceRerun = Boolean.FALSE;

    public BatchRunRequest() {
    }

    public BatchRunRequest(String periodKey, String startDate, String endDate, Boolean forceRerun) {
        this.periodKey = periodKey;
        this.startDate = startDate;
        this.endDate = endDate;
        this.forceRerun = forceRerun != null ? forceRerun : Boolean.FALSE;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public boolean isForceRerun() {
        return Boolean.TRUE.equals(forceRerun);
    }

    public void setForceRerun(Boolean forceRerun) {
        this.forceRerun = forceRerun != null ? forceRerun : Boolean.FALSE;
    }
}
