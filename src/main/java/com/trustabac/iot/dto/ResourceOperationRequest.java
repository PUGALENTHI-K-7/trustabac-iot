package com.trustabac.iot.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * DTO for executing a protected resource operation against an IoT device.
 * Client cannot directly set trust, risk, sensitivity, or final decision.
 */
public record ResourceOperationRequest(
        @NotBlank(message = "Device identifier is required")
        String deviceIdentifier,

        @NotBlank(message = "User ID is required")
        String userId,

        @NotBlank(message = "Role is required")
        String role,

        String organization,

        @NotBlank(message = "Resource name is required")
        String resource,

        @NotBlank(message = "Operation is required")
        String operation,

        String location,

        String bookingId,

        String networkContext,

        String behavioralIndicator,

        Integer requestCountWindow,

        Integer recentViolationCount,

        String requestReference,

        Map<String, Object> commandPayload
) {
}
