package com.trustabac.iot.messaging.event;

/**
 * Event model representing a security-relevant alert or abnormality for real-time monitoring.
 */
public record SecurityAlertEvent(
        String alertId,
        String alertType,
        String severity,
        String deviceIdentifier,
        String userId,
        String resource,
        String reason,
        String mitigationAction,
        String timestamp,
        String correlationId
) {
}
