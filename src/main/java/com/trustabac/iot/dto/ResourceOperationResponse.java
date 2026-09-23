package com.trustabac.iot.dto;

import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;
import java.util.Map;

/**
 * Detailed outcome response for a protected resource operation enforcement attempt.
 */
public record ResourceOperationResponse(
        String operationId,
        String deviceIdentifier,
        String resource,
        String requestedOperation,
        EnforcementStatus enforcementStatus,
        String effectiveOperation,
        String executionMessage,
        Map<String, Object> deviceState,
        Decision authorizationDecision,
        String decisionReason,
        String abacResult,
        String abacReason,
        Double trustScore,
        String trustStatus,
        Double riskScore,
        String riskStatus,
        String blockchainTxHash,
        Long blockchainBlockNumber,
        String contractAddress,
        String timestamp
) {
}
