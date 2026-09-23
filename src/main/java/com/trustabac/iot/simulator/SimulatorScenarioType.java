package com.trustabac.iot.simulator;

/**
 * Enumeration of supported smart-rental simulation scenario archetypes.
 */
public enum SimulatorScenarioType {
    NORMAL_STAY("Simulates standard guest arrival, amenity control, thermostat adjustments, and departure during an active booking."),
    PRE_CHECKIN("Simulates guest access attempts prior to authorized check-in window (expected rejection at ABAC/booking gate)."),
    SUSPICIOUS_ACTIVITY("Simulates unexpected network context and anomalous behavioral indicators, logging explicit trust security events."),
    REQUEST_FLOODING("Simulates high-frequency burst traffic against devices, triggering frequency risk evaluation and trust penalty."),
    HIGH_RISK_ATTACK("Simulates anomalous access under elevated sensitivity, abnormal indicators, and external network conditions."),
    LOW_TRUST_ATTACK("Simulates legitimate access attempts on a device whose behavioral trust has been degraded via confirmed malicious activity."),
    UNAUTHORIZED_SENSITIVE_ACCESS("Simulates guest attempts to access restricted resources (security camera, router admin, owner settings)."),
    RESTRICT_ENFORCEMENT("Simulates moderate risk conditions producing RESTRICT verdicts with physical control suppression and status read permission."),
    POST_CHECKOUT("Simulates access attempts following checkout / booking expiration (expected rejection at booking gate)."),
    RECOVERY("Simulates explicit device trust recovery events, returning the device to trusted status and restoring normal access.");

    private final String description;

    SimulatorScenarioType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
