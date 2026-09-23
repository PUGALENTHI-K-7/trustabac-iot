package com.trustabac.iot.batch.dto;

import java.util.List;
import java.util.Map;

public class BatchReportSummaryResponse {

    private Long batchRunId;
    private String periodKey;
    private String periodStart;
    private String periodEnd;
    private String status;
    private Long totalRecordsRead;
    private Long totalAnalyticsProduced;
    private Map<String, Object> authorization;
    private List<Map<String, Object>> devices;
    private Map<String, Object> security;
    private Map<String, Object> reconciliation;

    public BatchReportSummaryResponse() {
    }

    public Long getBatchRunId() {
        return batchRunId;
    }

    public void setBatchRunId(Long batchRunId) {
        this.batchRunId = batchRunId;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public String getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(String periodStart) {
        this.periodStart = periodStart;
    }

    public String getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(String periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Map<String, Object> getAuthorization() {
        return authorization;
    }

    public void setAuthorization(Map<String, Object> authorization) {
        this.authorization = authorization;
    }

    public List<Map<String, Object>> getDevices() {
        return devices;
    }

    public void setDevices(List<Map<String, Object>> devices) {
        this.devices = devices;
    }

    public Map<String, Object> getSecurity() {
        return security;
    }

    public void setSecurity(Map<String, Object> security) {
        this.security = security;
    }

    public Map<String, Object> getReconciliation() {
        return reconciliation;
    }

    public void setReconciliation(Map<String, Object> reconciliation) {
        this.reconciliation = reconciliation;
    }
}
