package com.trustabac.iot.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Immutable off-chain audit record of an authoritative smart-contract authorization evaluation.
 * Persisted strictly after confirmed blockchain execution (or orchestrator decision).
 */
@Entity
@Table(name = "blockchain_authorization_events")
public class BlockchainAuthorizationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_identifier", nullable = false, length = 64)
    private String deviceIdentifier;

    @Column(name = "resource", nullable = false, length = 64)
    private String resource;

    @Column(name = "operation", nullable = false, length = 64)
    private String operation;

    @Column(name = "trust_score", nullable = false)
    private Double trustScore;

    @Column(name = "risk_score", nullable = false)
    private Double riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 32)
    private Decision decision;

    @Column(name = "decision_reason", length = 512)
    private String decisionReason;

    @Column(name = "contract_address", length = 128)
    private String contractAddress;

    @Column(name = "transaction_hash", length = 128)
    private String transactionHash;

    @Column(name = "block_number")
    private Long blockNumber;

    @Column(name = "evaluation_timestamp", nullable = false)
    private LocalDateTime evaluationTimestamp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public BlockchainAuthorizationEvent() {
    }

    public BlockchainAuthorizationEvent(String deviceIdentifier, String resource, String operation,
                                      Double trustScore, Double riskScore, Decision decision,
                                      String decisionReason, String contractAddress, String transactionHash,
                                      Long blockNumber, LocalDateTime evaluationTimestamp) {
        this.deviceIdentifier = deviceIdentifier;
        this.resource = resource;
        this.operation = operation;
        this.trustScore = trustScore;
        this.riskScore = riskScore;
        this.decision = decision;
        this.decisionReason = decisionReason;
        this.contractAddress = contractAddress;
        this.transactionHash = transactionHash;
        this.blockNumber = blockNumber;
        this.evaluationTimestamp = evaluationTimestamp;
        this.createdAt = LocalDateTime.now();
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

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public String getResource() {
        return resource;
    }

    public String getOperation() {
        return operation;
    }

    public Double getTrustScore() {
        return trustScore;
    }

    public Double getRiskScore() {
        return riskScore;
    }

    public Decision getDecision() {
        return decision;
    }

    public String getDecisionReason() {
        return decisionReason;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public LocalDateTime getEvaluationTimestamp() {
        return evaluationTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
