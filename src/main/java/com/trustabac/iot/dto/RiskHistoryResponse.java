package com.trustabac.iot.dto;

import com.trustabac.iot.entity.Operation;
import com.trustabac.iot.entity.RiskStatus;

import java.time.LocalDateTime;

/**
 * Response DTO representing an append-only contextual risk audit record.
 */
public record RiskHistoryResponse(
        Long id,
        String deviceIdentifier,
        Double riskScore,
        RiskStatus riskStatus,
        String resource,
        Operation operation,
        String factorSummary,
        String reason,
        LocalDateTime evaluationTimestamp,
        LocalDateTime createdAt
) {}
