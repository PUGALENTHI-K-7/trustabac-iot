package com.trustabac.iot.dto;

/**
 * Normalized 0.0–1.0 score breakdown for all 7 evaluated contextual risk factors.
 */
public record RiskFactorBreakdown(
        double timeRisk,
        double locationRisk,
        double sensitivityRisk,
        double frequencyRisk,
        double networkRisk,
        double violationRisk,
        double behaviorRisk
) {}
