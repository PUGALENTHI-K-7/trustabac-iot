package com.trustabac.iot.dto;

import com.trustabac.iot.entity.Decision;

import java.time.LocalDateTime;

public record BlockchainAuthorizationResponse(
        Long requestId,
        String deviceIdentifier,
        String userId,
        String resource,
        String operation,

        // ABAC Gate Signal
        String abacResult,
        String abacReason,
        String evaluatedPolicyName,

        // Trust Gate Signal
        Double trustScore,
        String trustStatus,

        // Risk Gate Signal
        Double riskScore,
        String riskStatus,
        RiskFactorBreakdown riskFactors,
        String riskReason,

        // Final Smart-Contract Gate Decision
        Decision finalDecision,
        String decisionReason,

        // Blockchain Proof Metadata
        String blockchainTransactionHash,
        Long blockchainBlockNumber,
        String contractAddress,
        LocalDateTime evaluationTimestamp
) {
}
