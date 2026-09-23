package com.trustabac.iot.messaging.event;

import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;

/**
 * Domain event published when a simulated device operation has been executed, downgraded, or blocked.
 */
public record DeviceOperationEvent(
        String deviceIdentifier,
        String resource,
        String requestedOperation,
        String effectiveOperation,
        Decision decision,
        EnforcementStatus enforcementStatus,
        Double trustScore,
        Double riskScore,
        String abacResult,
        String executionMessage,
        String timestamp,
        String correlationId
) {
}
