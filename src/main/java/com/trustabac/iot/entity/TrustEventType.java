package com.trustabac.iot.entity;

/**
 * Categorical behavioral and security event types affecting IoT device trust scores.
 */
public enum TrustEventType {
    NORMAL_SUCCESS,
    SUSPICIOUS_ACTIVITY,
    REQUEST_FLOODING,
    CONFIRMED_MALICIOUS,
    RECOVERY
}
