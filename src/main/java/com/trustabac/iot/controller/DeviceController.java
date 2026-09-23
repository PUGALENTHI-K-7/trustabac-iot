package com.trustabac.iot.controller;

import com.trustabac.iot.dto.DeviceCreateRequest;
import com.trustabac.iot.dto.DeviceResponse;
import com.trustabac.iot.dto.DeviceUpdateRequest;
import com.trustabac.iot.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for IoT device registration and lifecycle management.
 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<DeviceResponse> registerDevice(@Valid @RequestBody DeviceCreateRequest request) {
        DeviceResponse response = deviceService.registerDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> getAllDevices() {
        return ResponseEntity.ok(deviceService.getAllDevices());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceResponse> getDeviceById(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.getDeviceById(id));
    }

    @GetMapping("/identifier/{deviceIdentifier}")
    public ResponseEntity<DeviceResponse> getDeviceByIdentifier(@PathVariable String deviceIdentifier) {
        return ResponseEntity.ok(deviceService.getDeviceByIdentifier(deviceIdentifier));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceResponse> updateDevice(@PathVariable Long id,
                                                       @RequestBody DeviceUpdateRequest request) {
        return ResponseEntity.ok(deviceService.updateDevice(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<DeviceResponse> deactivateDevice(@PathVariable Long id) {
        return ResponseEntity.ok(deviceService.deactivateDevice(id));
    }
}
