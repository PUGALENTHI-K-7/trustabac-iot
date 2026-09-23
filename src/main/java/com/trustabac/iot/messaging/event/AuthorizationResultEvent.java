package com.trustabac.iot.messaging.event;

import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;

/**
 * Domain event capturing the complete end-to-end authorization outcome.
 */
public record AuthorizationResultEvent(
        String requestReference,
        String correlationId,
        String deviceIdentifier,
        String resource,
        String operation,
        String abacResult,
        Double trustScore,
        Double riskScore,
        Decision decision,
        EnforcementStatus enforcementStatus,
        String effectiveOperation,
        String blockchainTxHash,
        Long blockchainBlockNumber,
        String timestamp
) {
}
