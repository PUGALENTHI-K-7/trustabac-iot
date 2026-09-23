package com.trustabac.iot.dto;

import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.RegistrationStatus;

public record DeviceUpdateRequest(
                DeviceType deviceType,
                DeviceClass deviceClass,
                RegistrationStatus registrationStatus,
                Boolean active) {
}
