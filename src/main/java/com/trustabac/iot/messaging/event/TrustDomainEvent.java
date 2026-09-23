package com.trustabac.iot.messaging.event;

import com.trustabac.iot.entity.TrustEventType;

/**
 * Domain event representing a trust update or security event affecting an IoT device.
 */
public record TrustDomainEvent(
        String deviceIdentifier,
        TrustEventType eventType,
        Double oldTrust,
        Double newTrust,
        Double delta,
        String reason,
        String source,
        String timestamp,
        String correlationId
) {
}
