package com.trustabac.iot.service.risk;

import com.trustabac.iot.config.RiskProperties;
import com.trustabac.iot.entity.BehavioralIndicator;
import org.springframework.stereotype.Component;

/**
 * Calculates risk from immediate contextual behavioral indicators.
 * NORMAL -> 0.0, SUSPICIOUS -> 0.60, ABNORMAL -> 1.0.
 */
@Component
public class BehaviorRiskCalculator implements RiskFactorCalculator {

    private final RiskProperties riskProperties;

    public BehaviorRiskCalculator(RiskProperties riskProperties) {
        this.riskProperties = riskProperties;
    }

    @Override
    public double calculate(RiskContext context) {
        BehavioralIndicator indicator = context.behavioralIndicator();
        if (indicator == null) {
            return riskProperties.getContext().getBehaviorNormal();
        }

        return switch (indicator) {
            case NORMAL -> riskProperties.getContext().getBehaviorNormal();
            case SUSPICIOUS -> riskProperties.getContext().getBehaviorSuspicious();
            case ABNORMAL -> riskProperties.getContext().getBehaviorAbnormal();
        };
    }

    @Override
    public String getFactorName() {
        return "behaviorRisk";
    }
}
