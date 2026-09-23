package com.trustabac.iot.service;

import com.trustabac.iot.dto.DeviceCreateRequest;
import com.trustabac.iot.dto.DeviceResponse;
import com.trustabac.iot.dto.DeviceUpdateRequest;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.exception.DuplicateResourceException;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.DeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing IoT device registration, lifecycle, and retrieval.
 */
@Service
@Transactional
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private final DeviceRepository deviceRepository;
    private final double defaultTrust;

    public DeviceService(DeviceRepository deviceRepository,
                         @Value("${trustabac.device.default-trust:80.0}") double defaultTrust) {
        this.deviceRepository = deviceRepository;
        this.defaultTrust = defaultTrust;
    }

    /**
     * Registers a new device after verifying identifier uniqueness.
     */
    public DeviceResponse registerDevice(DeviceCreateRequest request) {
        log.info("Registering new device with identifier: {}", request.deviceIdentifier());

        if (deviceRepository.existsByDeviceIdentifier(request.deviceIdentifier())) {
            throw new DuplicateResourceException("Device with identifier '" + request.deviceIdentifier() + "' already exists");
        }

        Device device = new Device(
                request.deviceIdentifier().trim(),
                request.deviceType(),
                request.deviceClass(),
                RegistrationStatus.REGISTERED,
                this.defaultTrust,
                true
        );

        Device savedDevice = deviceRepository.save(device);
        log.info("Successfully registered device ID {} with identifier {}", savedDevice.getId(), savedDevice.getDeviceIdentifier());
        return DeviceResponse.fromEntity(savedDevice);
    }

    /**
     * Retrieves a device by its primary key ID.
     */
    @Transactional(readOnly = true)
    public DeviceResponse getDeviceById(Long id) {
        return deviceRepository.findById(id)
                .map(DeviceResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + id));
    }

    /**
     * Retrieves a device by its unique business deviceIdentifier.
     */
    @Transactional(readOnly = true)
    public DeviceResponse getDeviceByIdentifier(String deviceIdentifier) {
        return deviceRepository.findByDeviceIdentifier(deviceIdentifier)
                .map(DeviceResponse::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with identifier: " + deviceIdentifier));
    }

    /**
     * Retrieves all registered devices.
     */
    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(DeviceResponse::fromEntity)
                .toList();
    }

    /**
     * Updates an existing device's metadata or status.
     */
    public DeviceResponse updateDevice(Long id, DeviceUpdateRequest request) {
        log.info("Updating device with ID: {}", id);

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + id));

        if (request.deviceType() != null) {
            device.setDeviceType(request.deviceType());
        }
        if (request.deviceClass() != null) {
            device.setDeviceClass(request.deviceClass());
        }
        if (request.registrationStatus() != null) {
            device.setRegistrationStatus(request.registrationStatus());
        }
        if (request.active() != null) {
            device.setActive(request.active());
        }

        Device updatedDevice = deviceRepository.save(device);
        log.info("Successfully updated device with ID: {}", id);
        return DeviceResponse.fromEntity(updatedDevice);
    }

    /**
     * Soft-deactivates / revokes a device to maintain historical access/trust integrity.
     */
    public DeviceResponse deactivateDevice(Long id) {
        log.info("Soft-deactivating/revoking device with ID: {}", id);

        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with ID: " + id));

        device.setRegistrationStatus(RegistrationStatus.REVOKED);
        device.setActive(false);

        Device revokedDevice = deviceRepository.save(device);
        log.info("Successfully revoked device with ID: {}", id);
        return DeviceResponse.fromEntity(revokedDevice);
    }
}
