package com.trustabac.iot.batch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Aggregated analytical summary of access control evaluations, policy decisions,
 * enforcement outcomes, and blockchain proofs for a specific analytical period.
 */
@Entity
@Table(name = "authorization_analytics", indexes = {
        @Index(name = "idx_auth_analytics_period", columnList = "period_key")
})
public class AuthorizationAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_run_id", nullable = false)
    private BatchRunAudit batchRunAudit;

    @Column(name = "period_key", nullable = false, length = 100)
    private String periodKey;

    @Column(name = "total_requests", nullable = false)
    private Long totalRequests = 0L;

    @Column(name = "allow_count", nullable = false)
    private Long allowCount = 0L;

    @Column(name = "restrict_count", nullable = false)
    private Long restrictCount = 0L;

    @Column(name = "deny_count", nullable = false)
    private Long denyCount = 0L;

    @Column(name = "executed_count", nullable = false)
    private Long executedCount = 0L;

    @Column(name = "downgraded_count", nullable = false)
    private Long downgradedCount = 0L;

    @Column(name = "blocked_count", nullable = false)
    private Long blockedCount = 0L;

    @Column(name = "abac_pass_count", nullable = false)
    private Long abacPassCount = 0L;

    @Column(name = "abac_fail_count", nullable = false)
    private Long abacFailCount = 0L;

    @Column(name = "sensitive_resource_denials", nullable = false)
    private Long sensitiveResourceDenials = 0L;

    @Column(name = "matched_blockchain_proofs", nullable = false)
    private Long matchedBlockchainProofs = 0L;

    @Column(name = "missing_blockchain_proofs", nullable = false)
    private Long missingBlockchainProofs = 0L;

    @Column(name = "ambiguous_blockchain_proofs", nullable = false)
    private Long ambiguousBlockchainProofs = 0L;

    @Column(name = "source_records_reconciled", nullable = false)
    private Long sourceRecordsReconciled = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public AuthorizationAnalytics() {
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

    public Long getExecutedCount() {
        return executedCount;
    }

    public void setExecutedCount(Long executedCount) {
        this.executedCount = executedCount;
    }

    public Long getDowngradedCount() {
        return downgradedCount;
    }

    public void setDowngradedCount(Long downgradedCount) {
        this.downgradedCount = downgradedCount;
    }

    public Long getBlockedCount() {
        return blockedCount;
    }

    public void setBlockedCount(Long blockedCount) {
        this.blockedCount = blockedCount;
    }

    public Long getAbacPassCount() {
        return abacPassCount;
    }

    public void setAbacPassCount(Long abacPassCount) {
        this.abacPassCount = abacPassCount;
    }

    public Long getAbacFailCount() {
        return abacFailCount;
    }

    public void setAbacFailCount(Long abacFailCount) {
        this.abacFailCount = abacFailCount;
    }

    public Long getSensitiveResourceDenials() {
        return sensitiveResourceDenials;
    }

    public void setSensitiveResourceDenials(Long sensitiveResourceDenials) {
        this.sensitiveResourceDenials = sensitiveResourceDenials;
    }

    public Long getMatchedBlockchainProofs() {
        return matchedBlockchainProofs;
    }

    public void setMatchedBlockchainProofs(Long matchedBlockchainProofs) {
        this.matchedBlockchainProofs = matchedBlockchainProofs;
    }

    public Long getMissingBlockchainProofs() {
        return missingBlockchainProofs;
    }

    public void setMissingBlockchainProofs(Long missingBlockchainProofs) {
        this.missingBlockchainProofs = missingBlockchainProofs;
    }

    public Long getAmbiguousBlockchainProofs() {
        return ambiguousBlockchainProofs;
    }

    public void setAmbiguousBlockchainProofs(Long ambiguousBlockchainProofs) {
        this.ambiguousBlockchainProofs = ambiguousBlockchainProofs;
    }

    public Long getSourceRecordsReconciled() {
        return sourceRecordsReconciled;
    }

    public void setSourceRecordsReconciled(Long sourceRecordsReconciled) {
        this.sourceRecordsReconciled = sourceRecordsReconciled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
