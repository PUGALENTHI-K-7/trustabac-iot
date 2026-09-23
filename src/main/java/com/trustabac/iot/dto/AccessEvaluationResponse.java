package com.trustabac.iot.dto;

import com.trustabac.iot.entity.AbacResult;
import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.RiskStatus;

import java.time.LocalDateTime;

/**
 * Response DTO returned by the access evaluation pipeline.
 * In Phase 5, includes ABAC eligibility result (Gate 1), retrieved device trust score (Gate 2),
 * and contextual risk score with explainable breakdown (Gate 3).
 * Note: Does NOT produce final ALLOW / RESTRICT / DENY authorization decisions.
 */
public class AccessEvaluationResponse {

    private Long requestId;
    private AbacResult result;
    private String reason;
    private String evaluatedPolicyName;
    private LocalDateTime evaluationTimestamp;
    private String deviceIdentifier;
    private String userId;
    private String resource;
    private Operation operation;

    // Gate 2: Dynamic Trust
    private Double trustScore;
    private String trustStatus;

    // Gate 3: Contextual Risk
    private Double riskScore;
    private RiskStatus riskStatus;
    private RiskFactorBreakdown riskFactors;
    private String riskReason;

    public AccessEvaluationResponse() {
    }

    public AccessEvaluationResponse(Long requestId, AbacResult result, String reason,
                                  String evaluatedPolicyName, LocalDateTime evaluationTimestamp,
                                  String deviceIdentifier, String userId, String resource,
                                  Operation operation) {
        this(requestId, result, reason, evaluatedPolicyName, evaluationTimestamp,
                deviceIdentifier, userId, resource, operation, null, null, null, null, null, null);
    }

    public AccessEvaluationResponse(Long requestId, AbacResult result, String reason,
                                  String evaluatedPolicyName, LocalDateTime evaluationTimestamp,
                                  String deviceIdentifier, String userId, String resource,
                                  Operation operation, Double trustScore, String trustStatus) {
        this(requestId, result, reason, evaluatedPolicyName, evaluationTimestamp,
                deviceIdentifier, userId, resource, operation, trustScore, trustStatus, null, null, null, null);
    }

    public AccessEvaluationResponse(Long requestId, AbacResult result, String reason,
                                  String evaluatedPolicyName, LocalDateTime evaluationTimestamp,
                                  String deviceIdentifier, String userId, String resource,
                                  Operation operation, Double trustScore, String trustStatus,
                                  Double riskScore, RiskStatus riskStatus, RiskFactorBreakdown riskFactors,
                                  String riskReason) {
        this.requestId = requestId;
        this.result = result;
        this.reason = reason;
        this.evaluatedPolicyName = evaluatedPolicyName;
        this.evaluationTimestamp = evaluationTimestamp;
        this.deviceIdentifier = deviceIdentifier;
        this.userId = userId;
        this.resource = resource;
        this.operation = operation;
        this.trustScore = trustScore;
        this.trustStatus = trustStatus;
        this.riskScore = riskScore;
        this.riskStatus = riskStatus;
        this.riskFactors = riskFactors;
        this.riskReason = riskReason;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public AbacResult getResult() {
        return result;
    }

    public void setResult(AbacResult result) {
        this.result = result;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getEvaluatedPolicyName() {
        return evaluatedPolicyName;
    }

    public void setEvaluatedPolicyName(String evaluatedPolicyName) {
        this.evaluatedPolicyName = evaluatedPolicyName;
    }

    public LocalDateTime getEvaluationTimestamp() {
        return evaluationTimestamp;
    }

    public void setEvaluationTimestamp(LocalDateTime evaluationTimestamp) {
        this.evaluationTimestamp = evaluationTimestamp;
    }

    public String getDeviceIdentifier() {
        return deviceIdentifier;
    }

    public void setDeviceIdentifier(String deviceIdentifier) {
        this.deviceIdentifier = deviceIdentifier;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public Double getTrustScore() {
        return trustScore;
    }

    public void setTrustScore(Double trustScore) {
        this.trustScore = trustScore;
    }

    public String getTrustStatus() {
        return trustStatus;
    }

    public void setTrustStatus(String trustStatus) {
        this.trustStatus = trustStatus;
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

    public RiskFactorBreakdown getRiskFactors() {
        return riskFactors;
    }

    public void setRiskFactors(RiskFactorBreakdown riskFactors) {
        this.riskFactors = riskFactors;
    }

    public String getRiskReason() {
        return riskReason;
    }

    public void setRiskReason(String riskReason) {
        this.riskReason = riskReason;
    }
}
