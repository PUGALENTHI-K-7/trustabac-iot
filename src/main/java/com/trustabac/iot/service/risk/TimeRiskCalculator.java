package com.trustabac.iot.service.risk;

import com.trustabac.iot.config.RiskProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Calculates time-based contextual risk.
 * Evaluates whether the access attempt occurs within expected operational hours (0.0)
 * or outside normal hours / nighttime (1.0).
 */
@Component
public class TimeRiskCalculator implements RiskFactorCalculator {

    private final RiskProperties riskProperties;

    public TimeRiskCalculator(RiskProperties riskProperties) {
        this.riskProperties = riskProperties;
    }

    @Override
    public double calculate(RiskContext context) {
        LocalDateTime timestamp = context.evaluationTimestamp();
        if (timestamp == null) {
            return 0.0;
        }

        int hour = timestamp.getHour();
        int start = riskProperties.getContext().getNormalStartHour();
        int end = riskProperties.getContext().getNormalEndHour();

        if (start <= end) {
            if (hour >= start && hour < end) {
                return 0.0; // Normal operating hours
            } else {
                return 1.0; // Outside normal operating hours
            }
        } else {
            // Span across midnight (e.g. 20:00 to 04:00)
            if (hour >= start || hour < end) {
                return 0.0;
            } else {
                return 1.0;
            }
        }
    }

    @Override
    public String getFactorName() {
        return "timeRisk";
    }
}
