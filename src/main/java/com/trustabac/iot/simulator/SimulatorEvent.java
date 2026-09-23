package com.trustabac.iot.simulator;

import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;

/**
 * Structured log record for a single simulated device operation event.
 */
public record SimulatorEvent(
        String simulationEventId,
        SimulatorScenarioType scenarioType,
        int stepNumber,
        String timestamp,
        String deviceIdentifier,
        String resource,
        String operation,
        Decision decision,
        EnforcementStatus enforcementStatus,
        String effectiveOperation,
        Double trustScore,
        Double riskScore,
        String abacResult,
        String blockchainTxHash,
        Long blockchainBlockNumber,
        String executionMessage
) {
}
