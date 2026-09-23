package com.trustabac.iot.messaging.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Standard immutable envelope for all domain events across the TrustABAC-IoT messaging pipeline.
 *
 * @param <T> payload type
 */
public record EventEnvelope<T>(
        String eventId,
        String eventType,
        String timestamp,
        String source,
        String deviceIdentifier,
        String propertyId,
        String bookingId,
        String requestReference,
        String correlationId,
        T payload,
        String schemaVersion
) {
    public static <T> EventEnvelope<T> of(String eventType,
                                          String source,
                                          String deviceIdentifier,
                                          String propertyId,
                                          String bookingId,
                                          String requestReference,
                                          String correlationId,
                                          T payload) {
        return new EventEnvelope<>(
                "EVT-" + UUID.randomUUID().toString().substring(0, 8),
                eventType,
                LocalDateTime.now().toString(),
                source != null ? source : "GATEWAY_PIPELINE",
                deviceIdentifier,
                propertyId,
                bookingId,
                requestReference,
                correlationId != null ? correlationId : "CORR-" + UUID.randomUUID().toString().substring(0, 8),
                payload,
                "1.0"
        );
    }
}
