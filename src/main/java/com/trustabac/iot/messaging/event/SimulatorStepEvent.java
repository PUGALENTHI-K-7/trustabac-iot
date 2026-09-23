package com.trustabac.iot.messaging.event;

/**
 * Event model representing an individual simulator step for real-time dashboard streaming.
 */
public record SimulatorStepEvent(
        String simulationEventId,
        String scenarioName,
        int stepNumber,
        String deviceIdentifier,
        String resource,
        String operation,
        String decision,
        String enforcementStatus,
        Double trustScore,
        Double riskScore,
        String abacResult,
        String blockchainTxHash,
        Long blockchainBlockNumber,
        String executionMessage,
        String timestamp,
        String correlationId
) {
}
