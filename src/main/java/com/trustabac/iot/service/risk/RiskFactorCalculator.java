package com.trustabac.iot.service.risk;

/**
 * Strategy interface for calculating a single contextual risk factor.
 * Every implementation must return a normalized score strictly between 0.0 and 1.0.
 */
public interface RiskFactorCalculator {

    /**
     * Calculates the normalized risk contribution for the given resolved risk context.
     *
     * @param context the authoritative server-resolved risk context
     * @return normalized risk factor value between 0.0 (minimal risk) and 1.0 (maximum risk)
     */
    double calculate(RiskContext context);

    /**
     * Unique identifier/name of the risk factor.
     */
    String getFactorName();
}
