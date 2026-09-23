package com.trustabac.iot.batch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Aggregated analytical summary of access attempts, trust score bounds, and risk score bounds
 * broken down per device for a specific analytical period.
 */
@Entity
@Table(name = "device_analytics", indexes = {
        @Index(name = "idx_device_analytics_period", columnList = "period_key"),
        @Index(name = "idx_device_analytics_dev", columnList = "device_identifier")
})
public class DeviceAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_run_id", nullable = false)
    private BatchRunAudit batchRunAudit;

    @Column(name = "period_key", nullable = false, length = 100)
    private String periodKey;

    @Column(name = "device_identifier", nullable = false, length = 100)
    private String deviceIdentifier;

    @Column(name = "total_requests", nullable = false)
    private Long totalRequests = 0L;

    @Column(name = "allow_count", nullable = false)
    private Long allowCount = 0L;

    @Column(name = "restrict_count", nullable = false)
    private Long restrictCount = 0L;

    @Column(name = "deny_count", nullable = false)
    private Long denyCount = 0L;

    @Column(name = "avg_trust_score")
    private Double avgTrustScore;

    @Column(name = "min_trust_score")
    private Double minTrustScore;

    @Column(name = "max_trust_score")
    private Double maxTrustScore;

    @Column(name = "avg_risk_score")
    private Double avgRiskScore;

    @Column(name = "min_risk_score")
    private Double minRiskScore;

    @Column(name = "max_risk_score")
    private Double maxRiskScore;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public DeviceAnalytics() {
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BatchRunAudit getBatchRunAudit() {
        return batchRunAudit;
    }

    public void setBatchRunAudit(BatchRunAudit batchRunAudit) {
        this.batchRunAudit = batchRunAudit;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public Long getTotalRequests() {
        return totalRequests;
    }

    public void setTotalRequests(Long totalRequests) {
        this.totalRequests = totalRequests;
    }

    public Long getAllowCount() {
        return allowCount;
    }

    public void setAllowCount(Long allowCount) {
        this.allowCount = allowCount;
    }

    public Long getRestrictCount() {
        return restrictCount;
    }

    public void setRestrictCount(Long restrictCount) {
        this.restrictCount = restrictCount;
    }

    public Long getDenyCount() {
        return denyCount;
    }

    public void setDenyCount(Long denyCount) {
        this.denyCount = denyCount;
    }

    public Double getAvgTrustScore() {
        return avgTrustScore;
    }

    public void setAvgTrustScore(Double avgTrustScore) {
        this.avgTrustScore = avgTrustScore;
    }

    public Double getMinTrustScore() {
        return minTrustScore;
    }

    public void setMinTrustScore(Double minTrustScore) {
        this.minTrustScore = minTrustScore;
    }

    public Double getMaxTrustScore() {
        return maxTrustScore;
    }

    public void setMaxTrustScore(Double maxTrustScore) {
        this.maxTrustScore = maxTrustScore;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
