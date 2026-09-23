package com.trustabac.iot.service.risk;

import com.trustabac.iot.config.RiskProperties;
import com.trustabac.iot.entity.ResourceSensitivity;
import org.springframework.stereotype.Component;

/**
 * Calculates resource sensitivity risk.
 * Maps authoritative server-resolved resource sensitivity (LOW=0.0, MEDIUM=0.35, HIGH=0.70, CRITICAL=1.0).
 */
@Component
public class SensitivityRiskCalculator implements RiskFactorCalculator {

    private final RiskProperties riskProperties;

    public SensitivityRiskCalculator(RiskProperties riskProperties) {
        this.riskProperties = riskProperties;
    }

    @Override
    public double calculate(RiskContext context) {
        ResourceSensitivity sensitivity = context.resourceSensitivity();
        if (sensitivity == null) {
            return riskProperties.getContext().getSensitivityMedium();
        }

        return switch (sensitivity) {
            case LOW -> riskProperties.getContext().getSensitivityLow();
            case MEDIUM -> riskProperties.getContext().getSensitivityMedium();
            case HIGH -> riskProperties.getContext().getSensitivityHigh();
            case CRITICAL -> riskProperties.getContext().getSensitivityCritical();
        };
    }

    @Override
    public String getFactorName() {
        return "sensitivityRisk";
    }
}
