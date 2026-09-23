package com.trustabac.iot.service.risk;

import com.trustabac.iot.config.RiskProperties;
import org.springframework.stereotype.Component;

/**
 * Calculates risk from recent security violations and policy failure events.
 * 0 violations -> 0.0
 * >= high threshold (e.g. 5) -> 1.0
 * Between thresholds -> proportional normalized factor.
 */
@Component
public class ViolationRiskCalculator implements RiskFactorCalculator {

    private final RiskProperties riskProperties;

    public ViolationRiskCalculator(RiskProperties riskProperties) {
        this.riskProperties = riskProperties;
    }

    @Override
    public double calculate(RiskContext context) {
        int violations = context.recentViolationCount();
        if (violations <= 0) {
            return 0.0;
        }

        int high = riskProperties.getContext().getViolationHighThreshold();
        if (violations >= high) {
            return 1.0;
        }

        double score = (double) violations / high;
        return Math.min(1.0, Math.max(0.0, score));
    }

    @Override
    public String getFactorName() {
        return "violationRisk";
    }
}
