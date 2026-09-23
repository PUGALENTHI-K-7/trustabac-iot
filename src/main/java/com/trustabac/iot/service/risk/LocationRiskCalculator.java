package com.trustabac.iot.service.risk;

import com.trustabac.iot.config.RiskProperties;
import org.springframework.stereotype.Component;

/**
 * Calculates location-based contextual risk.
 * Evaluates whether the request originates from the expected property perimeter (0.0)
 * or an unexpected/remote location (1.0).
 */
@Component
public class LocationRiskCalculator implements RiskFactorCalculator {

    private final RiskProperties riskProperties;

    public LocationRiskCalculator(RiskProperties riskProperties) {
        this.riskProperties = riskProperties;
    }

    @Override
    public double calculate(RiskContext context) {
        String location = context.location();
        String expected = riskProperties.getContext().getDefaultExpectedLocation();

        if (location == null || location.isBlank()) {
            return 1.0;
        }

        if (location.equalsIgnoreCase(expected)) {
            return 0.0; // Expected property/location
        }

        return 1.0; // Unexpected location
    }

    @Override
    public String getFactorName() {
        return "locationRisk";
    }
}
