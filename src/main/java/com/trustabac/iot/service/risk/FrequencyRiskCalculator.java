package com.trustabac.iot.service.risk;

import com.trustabac.iot.config.RiskProperties;
import org.springframework.stereotype.Component;

/**
 * Calculates request frequency risk in a rolling time window.
 * Evaluates request burst velocity:
 * <= low threshold (e.g. 5 req) -> 0.0 (normal)
 * >= high threshold (e.g. 20 req) -> 1.0 (flooding / burst velocity)
 * Between thresholds -> proportional normalized factor.
 */
@Component
public class FrequencyRiskCalculator implements RiskFactorCalculator {

    private final RiskProperties riskProperties;

    public FrequencyRiskCalculator(RiskProperties riskProperties) {
        this.riskProperties = riskProperties;
    }

    @Override
    public double calculate(RiskContext context) {
        int count = context.requestCountWindow();
        int low = riskProperties.getContext().getFrequencyLowThreshold();
        int high = riskProperties.getContext().getFrequencyHighThreshold();

        if (count <= low) {
            return 0.0;
        }
        if (count >= high) {
            return 1.0;
        }

        double score = (double) (count - low) / (high - low);
        return Math.min(1.0, Math.max(0.0, score));
    }

    @Override
    public String getFactorName() {
        return "frequencyRisk";
    }
}
