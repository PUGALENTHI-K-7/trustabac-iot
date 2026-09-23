package com.trustabac.iot.service;

import com.trustabac.iot.dto.DeviceCreateRequest;
import com.trustabac.iot.dto.DeviceResponse;
import com.trustabac.iot.dto.DeviceUpdateRequest;
import com.trustabac.iot.entity.Device;
import com.trustabac.iot.entity.DeviceClass;
import com.trustabac.iot.entity.DeviceType;
import com.trustabac.iot.entity.RegistrationStatus;
import com.trustabac.iot.exception.DuplicateResourceException;
import com.trustabac.iot.exception.ResourceNotFoundException;
import com.trustabac.iot.repository.DeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceService = new DeviceService(deviceRepository, 80.0);
    }

    @Test
    @DisplayName("registerDevice should successfully save and return DeviceResponse")
    void testRegisterDeviceSuccess() {
        DeviceCreateRequest request = new DeviceCreateRequest(
                "IOT-DOOR-001",
                DeviceType.SMART_DOOR_LOCK,
                DeviceClass.ACTUATOR
        );

        when(deviceRepository.existsByDeviceIdentifier("IOT-DOOR-001")).thenReturn(false);

        Device savedDevice = new Device(
                "IOT-DOOR-001",
                DeviceType.SMART_DOOR_LOCK,
                DeviceClass.ACTUATOR,
                RegistrationStatus.REGISTERED,
                80.0,
                true
        );
        savedDevice.setId(1L);
        savedDevice.setCreatedAt(LocalDateTime.now());
        savedDevice.setUpdatedAt(LocalDateTime.now());

        when(deviceRepository.save(any(Device.class))).thenReturn(savedDevice);

        DeviceResponse response = deviceService.registerDevice(request);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("IOT-DOOR-001", response.deviceIdentifier());
        assertEquals(DeviceType.SMART_DOOR_LOCK, response.deviceType());
        assertEquals(DeviceClass.ACTUATOR, response.deviceClass());
        assertEquals(RegistrationStatus.REGISTERED, response.registrationStatus());
        assertEquals(80.0, response.currentTrust());
        assertTrue(response.active());

        verify(deviceRepository).existsByDeviceIdentifier("IOT-DOOR-001");
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    @DisplayName("registerDevice with duplicate identifier should throw DuplicateResourceException")
    void testRegisterDuplicateDeviceThrowsConflict() {
        DeviceCreateRequest request = new DeviceCreateRequest(
                "IOT-DOOR-001",
                DeviceType.SMART_DOOR_LOCK,
                DeviceClass.ACTUATOR
        );

        when(deviceRepository.existsByDeviceIdentifier("IOT-DOOR-001")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> deviceService.registerDevice(request));

        verify(deviceRepository).existsByDeviceIdentifier("IOT-DOOR-001");
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    @DisplayName("getDeviceById should return DeviceResponse when device exists")
    void testGetDeviceByIdSuccess() {
        Device device = new Device("IOT-TV-001", DeviceType.SMART_TV, DeviceClass.ENDPOINT,
                RegistrationStatus.REGISTERED, 80.0, true);
        device.setId(10L);
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());

        when(deviceRepository.findById(10L)).thenReturn(Optional.of(device));

        DeviceResponse response = deviceService.getDeviceById(10L);

        assertEquals(10L, response.id());
        assertEquals("IOT-TV-001", response.deviceIdentifier());
    }

    @Test
    @DisplayName("getDeviceById should throw ResourceNotFoundException when device does not exist")
    void testGetDeviceByIdNotFound() {
        when(deviceRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> deviceService.getDeviceById(999L));
    }

    @Test
    @DisplayName("getDeviceByIdentifier should return DeviceResponse when device exists")
    void testGetDeviceByIdentifierSuccess() {
        Device device = new Device("IOT-WIFI-001", DeviceType.GUEST_WIFI, DeviceClass.CONTROLLER,
                RegistrationStatus.REGISTERED, 80.0, true);
        device.setId(5L);
        device.setCreatedAt(LocalDateTime.now());
        device.setUpdatedAt(LocalDateTime.now());

        when(deviceRepository.findByDeviceIdentifier("IOT-WIFI-001")).thenReturn(Optional.of(device));

        DeviceResponse response = deviceService.getDeviceByIdentifier("IOT-WIFI-001");

        assertEquals(5L, response.id());
        assertEquals("IOT-WIFI-001", response.deviceIdentifier());
    }

    @Test
    @DisplayName("getAllDevices should return list of all devices")
    void testGetAllDevices() {
        Device d1 = new Device("DEV-1", DeviceType.SMART_LIGHT, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        d1.setId(1L);
        Device d2 = new Device("DEV-2", DeviceType.SMART_THERMOSTAT, DeviceClass.CONTROLLER, RegistrationStatus.REGISTERED, 80.0, true);
        d2.setId(2L);

        when(deviceRepository.findAll()).thenReturn(List.of(d1, d2));

        List<DeviceResponse> responses = deviceService.getAllDevices();

        assertEquals(2, responses.size());
        assertEquals("DEV-1", responses.get(0).deviceIdentifier());
        assertEquals("DEV-2", responses.get(1).deviceIdentifier());
    }

    @Test
    @DisplayName("updateDevice should update metadata and return updated response")
    void testUpdateDevice() {
        Device device = new Device("DEV-1", DeviceType.SMART_LIGHT, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        device.setId(1L);

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceUpdateRequest updateRequest = new DeviceUpdateRequest(
                DeviceType.AIR_CONDITIONER,
                DeviceClass.ENDPOINT,
                RegistrationStatus.SUSPENDED,
                false
        );

        DeviceResponse updated = deviceService.updateDevice(1L, updateRequest);

        assertEquals(DeviceType.AIR_CONDITIONER, updated.deviceType());
        assertEquals(DeviceClass.ENDPOINT, updated.deviceClass());
        assertEquals(RegistrationStatus.SUSPENDED, updated.registrationStatus());
        assertFalse(updated.active());
    }

    @Test
    @DisplayName("deactivateDevice should soft-revoke device without deleting record")
    void testDeactivateDevice() {
        Device device = new Device("DEV-1", DeviceType.SMART_LIGHT, DeviceClass.ACTUATOR, RegistrationStatus.REGISTERED, 80.0, true);
        device.setId(1L);

        when(deviceRepository.findById(1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DeviceResponse revoked = deviceService.deactivateDevice(1L);

        assertEquals(RegistrationStatus.REVOKED, revoked.registrationStatus());
        assertFalse(revoked.active());
        verify(deviceRepository, never()).delete(any(Device.class));
        verify(deviceRepository, never()).deleteById(any());
    }
}
