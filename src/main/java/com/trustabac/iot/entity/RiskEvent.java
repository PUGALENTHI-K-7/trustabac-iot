package com.trustabac.iot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Append-only immutable audit record for contextual risk evaluations.
 * Stores calculated risk score, status, normalized factor contributions, and contextual reason.
 */
@Entity
@Table(name = "risk_events")
public class RiskEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "device_identifier", nullable = false, length = 100)
    private String deviceIdentifier;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_status", nullable = false, length = 30)
    private RiskStatus riskStatus;

    @Column(name = "resource", nullable = false, length = 100)
    private String resource;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation", nullable = false, length = 30)
    private Operation operation;

    @Column(name = "factor_summary", nullable = false, length = 1000)
    private String factorSummary;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "evaluation_timestamp", nullable = false)
    private LocalDateTime evaluationTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public RiskEvent() {
    }

    public RiskEvent(Device device, String deviceIdentifier, Double riskScore, RiskStatus riskStatus,
                     String resource, Operation operation, String factorSummary, String reason,
                     LocalDateTime evaluationTimestamp) {
        this.device = device;
        this.deviceIdentifier = deviceIdentifier;
        this.riskScore = riskScore;
        this.riskStatus = riskStatus;
        this.resource = resource;
        this.operation = operation;
        this.factorSummary = factorSummary;
        this.reason = reason;
        this.evaluationTimestamp = evaluationTimestamp;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(Double riskScore) {
        this.riskScore = riskScore;
    }

    public RiskStatus getRiskStatus() {
        return riskStatus;
    }

    public void setRiskStatus(RiskStatus riskStatus) {
        this.riskStatus = riskStatus;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public String getFactorSummary() {
        return factorSummary;
    }

    public void setFactorSummary(String factorSummary) {
        this.factorSummary = factorSummary;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getEvaluationTimestamp() {
        return evaluationTimestamp;
    }

    public void setEvaluationTimestamp(LocalDateTime evaluationTimestamp) {
        this.evaluationTimestamp = evaluationTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
