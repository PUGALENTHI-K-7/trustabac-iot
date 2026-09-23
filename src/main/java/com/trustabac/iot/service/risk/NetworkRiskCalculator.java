package com.trustabac.iot.service.risk;

import com.trustabac.iot.config.RiskProperties;
import org.springframework.stereotype.Component;

/**
 * Calculates network context risk.
 * Maps network connection origins:
 * LOCAL_WIFI (0.0), VPN (0.30), REMOTE_CELLULAR (0.60), UNKNOWN (1.0).
 */
@Component
public class NetworkRiskCalculator implements RiskFactorCalculator {

    private final RiskProperties riskProperties;

    public NetworkRiskCalculator(RiskProperties riskProperties) {
        this.riskProperties = riskProperties;
    }

    @Override
    public double calculate(RiskContext context) {
        String network = context.networkContext();
        if (network == null || network.isBlank()) {
            return riskProperties.getContext().getNetworkUnknown();
        }

        String normalized = network.trim().toUpperCase();
        return switch (normalized) {
            case "LOCAL_WIFI", "INTERNAL", "LOCAL" -> riskProperties.getContext().getNetworkLocalWifi();
            case "VPN" -> riskProperties.getContext().getNetworkVpn();
            case "REMOTE_CELLULAR" -> riskProperties.getContext().getNetworkRemoteCellular();
            default -> riskProperties.getContext().getNetworkUnknown();
        };
    }

    @Override
    public String getFactorName() {
        return "networkRisk";
    }
}
