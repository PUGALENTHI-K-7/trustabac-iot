package com.trustabac.iot.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.RegistrationStatus;

import java.time.LocalDateTime;

/**
 * Standard response DTO for IoT device representations.
 */
public record DeviceResponse(
        Long id,
        String deviceIdentifier,
        DeviceType deviceType,
        DeviceClass deviceClass,
        RegistrationStatus registrationStatus,
        Double currentTrust,
        Boolean active,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt
) {
    public static DeviceResponse fromEntity(Device device) {
        return new DeviceResponse(
                device.getId(),
                device.getDeviceIdentifier(),
                device.getDeviceType(),
                device.getDeviceClass(),
                device.getRegistrationStatus(),
                device.getCurrentTrust(),
                device.getActive(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
