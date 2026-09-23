package com.trustabac.iot.dto;

import jakarta.validation.constraints.NotBlank;

public record BlockchainAuthorizationRequest(
        @NotBlank(message = "deviceIdentifier is required")
        String deviceIdentifier,

        @NotBlank(message = "userId is required")
        String userId,

        String role,
        String organization,

        @NotBlank(message = "resource is required")
        String resource,

        @NotBlank(message = "operation is required")
        String operation,

        String location,
        String bookingId,
        String networkContext,

        // Optional simulation inputs for contextual testing
        Integer requestCountWindow,
        Integer recentViolationCount,
        String behavioralIndicator,
        String requestReference
) {
}
