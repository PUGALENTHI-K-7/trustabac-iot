package com.trustabac.iot.dto;

import com.trustabac.iot.entity.RiskStatus;

import java.time.LocalDateTime;

/**
 * Result DTO returned after evaluating contextual risk for an access request.
 * Contains the composite score, status band, explainable factor breakdown, and reason.
 */
public record RiskEvaluationResponse(
        String deviceIdentifier,
        Double riskScore,
        RiskStatus riskStatus,
        RiskFactorBreakdown factors,
        String reason,
        LocalDateTime evaluationTimestamp
) {}
