package com.trustabac.iot.batch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Aggregated analytical summary of security incidents, trust score mutations,
 * and contextual risk distributions for a specific analytical period.
 */
@Entity
@Table(name = "security_analytics", indexes = {
        @Index(name = "idx_sec_analytics_period", columnList = "period_key")
})
public class SecurityAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_run_id", nullable = false)
    private BatchRunAudit batchRunAudit;

    @Column(name = "period_key", nullable = false, length = 100)
    private String periodKey;

    @Column(name = "normal_success_count", nullable = false)
    private Long normalSuccessCount = 0L;

    @Column(name = "suspicious_activity_count", nullable = false)
    private Long suspiciousActivityCount = 0L;

    @Column(name = "request_flooding_count", nullable = false)
    private Long requestFloodingCount = 0L;

    @Column(name = "confirmed_malicious_count", nullable = false)
    private Long confirmedMaliciousCount = 0L;

    @Column(name = "recovery_count", nullable = false)
    private Long recoveryCount = 0L;

    @Column(name = "low_risk_count", nullable = false)
    private Long lowRiskCount = 0L;

    @Column(name = "medium_risk_count", nullable = false)
    private Long mediumRiskCount = 0L;

    @Column(name = "high_risk_count", nullable = false)
    private Long highRiskCount = 0L;

    @Column(name = "avg_trust_delta")
    private Double avgTrustDelta;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SecurityAnalytics() {
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

    public Long getNormalSuccessCount() {
        return normalSuccessCount;
    }

    public void setNormalSuccessCount(Long normalSuccessCount) {
        this.normalSuccessCount = normalSuccessCount;
    }

    public Long getSuspiciousActivityCount() {
        return suspiciousActivityCount;
    }

    public void setSuspiciousActivityCount(Long suspiciousActivityCount) {
        this.suspiciousActivityCount = suspiciousActivityCount;
    }

    public Long getRequestFloodingCount() {
        return requestFloodingCount;
    }

    public void setRequestFloodingCount(Long requestFloodingCount) {
        this.requestFloodingCount = requestFloodingCount;
    }

    public Long getConfirmedMaliciousCount() {
        return confirmedMaliciousCount;
    }

    public void setConfirmedMaliciousCount(Long confirmedMaliciousCount) {
        this.confirmedMaliciousCount = confirmedMaliciousCount;
    }

    public Long getRecoveryCount() {
        return recoveryCount;
    }

    public void setRecoveryCount(Long recoveryCount) {
        this.recoveryCount = recoveryCount;
    }

    public Long getLowRiskCount() {
        return lowRiskCount;
    }

    public void setLowRiskCount(Long lowRiskCount) {
        this.lowRiskCount = lowRiskCount;
    }

    public Long getMediumRiskCount() {
        return mediumRiskCount;
    }

    public void setMediumRiskCount(Long mediumRiskCount) {
        this.mediumRiskCount = mediumRiskCount;
    }

    public Long getHighRiskCount() {
        return highRiskCount;
    }

    public void setHighRiskCount(Long highRiskCount) {
        this.highRiskCount = highRiskCount;
    }

    public Double getAvgTrustDelta() {
        return avgTrustDelta;
    }

    public void setAvgTrustDelta(Double avgTrustDelta) {
        this.avgTrustDelta = avgTrustDelta;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
