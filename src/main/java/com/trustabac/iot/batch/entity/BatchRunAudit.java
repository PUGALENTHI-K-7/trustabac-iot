package com.trustabac.iot.batch.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Persistent audit record representing a Spring Batch execution run for offline analytics.
 * Enforces period identity and tracks job lifecycle metadata.
 */
@Entity
@Table(name = "batch_run_audits", indexes = {
        @Index(name = "idx_batch_period_key", columnList = "period_key"),
        @Index(name = "idx_batch_status", columnList = "status")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_batch_period_key", columnNames = {"period_key"})
})
public class BatchRunAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_key", nullable = false, length = 100)
    private String periodKey;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @Column(name = "job_execution_id")
    private Long jobExecutionId;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "exit_code", length = 50)
    private String exitCode;

    @Column(name = "exit_message", length = 1000)
    private String exitMessage;

    @Column(name = "total_records_read")
    private Long totalRecordsRead = 0L;

    @Column(name = "total_analytics_produced")
    private Long totalAnalyticsProduced = 0L;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BatchRunAudit() {
    }

    public BatchRunAudit(String periodKey, LocalDateTime periodStart, LocalDateTime periodEnd,
                         String jobName, String status, LocalDateTime startTime) {
        this.periodKey = periodKey;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.jobName = jobName;
        this.status = status;
        this.startTime = startTime;
        this.createdAt = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.startTime == null) {
            this.startTime = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Long getJobExecutionId() {
        return jobExecutionId;
    }

    public void setJobExecutionId(Long jobExecutionId) {
        this.jobExecutionId = jobExecutionId;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
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

    public String getExitMessage() {
        return exitMessage;
    }

    public void setExitMessage(String exitMessage) {
        this.exitMessage = exitMessage;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
