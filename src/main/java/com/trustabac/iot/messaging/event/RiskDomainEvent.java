package com.trustabac.iot.messaging.event;

/**
 * Domain event representing a contextual risk evaluation result.
 */
public record RiskDomainEvent(
        String deviceIdentifier,
        String resource,
        String operation,
        String location,
        String networkContext,
        Integer requestCountWindow,
        Integer recentViolationCount,
        String behavioralIndicator,
        Double calculatedRiskScore,
        String riskStatus,
        String riskReason,
        String timestamp,
        String correlationId
) {
}
