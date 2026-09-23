package com.trustabac.iot.experiment.dto;

import com.trustabac.iot.entity.Decision;
import com.trustabac.iot.entity.EnforcementStatus;

/**
 * Raw measurement record captured for an individual request within an experiment run.
 * Serves as the primary, unaggregated empirical dataset for scientific reporting and charting.
 */
public record ExperimentRawSample(
        String runId,
        int repetitionNumber,
        String scenarioType,
        long seed,
        int requestIndex,
        boolean isWarmUp,
        String requestReference,
        String deviceIdentifier,
        String resource,
        String operation,
        String abacResult,
        Double trustScore,
        Double riskScore,
        Decision decision,
        EnforcementStatus enforcementStatus,
        double authLatencyMs,
        double enforceLatencyMs,
        String blockchainTxHash,
        Long gasUsed,
        Long blockNumber,
        String decisionReason,
        String expectedDecision,
        String expectedEnforcement,
        boolean expectationMatched,
        String correlationId,
        String timestamp
) {}
