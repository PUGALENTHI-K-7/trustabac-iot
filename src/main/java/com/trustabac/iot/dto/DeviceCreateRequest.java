package com.trustabac.iot.dto;

import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for registering a new IoT device.
 */
public record DeviceCreateRequest(
        @NotBlank(message = "Device identifier is required")
        @Size(min = 3, max = 100, message = "Device identifier must be between 3 and 100 characters")
        String deviceIdentifier,

        @NotNull(message = "Device type is required")
        DeviceType deviceType,

        @NotNull(message = "Device class is required")
        DeviceClass deviceClass
) {}
