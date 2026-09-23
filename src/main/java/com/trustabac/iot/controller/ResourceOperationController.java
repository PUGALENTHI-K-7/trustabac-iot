package com.trustabac.iot.controller;

import com.trustabac.iot.dto.ResourceOperationRequest;
import com.trustabac.iot.dto.ResourceOperationResponse;
import com.trustabac.iot.service.ResourceOperationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for executing and enforcing protected IoT resource operations.
 */
@RestController
@RequestMapping("/api/resource-operations")
public class ResourceOperationController {

    private final ResourceOperationService resourceOperationService;

    public ResourceOperationController(ResourceOperationService resourceOperationService) {
        this.resourceOperationService = resourceOperationService;
    }

    @PostMapping("/execute")
    public ResponseEntity<ResourceOperationResponse> executeOperation(@Valid @RequestBody ResourceOperationRequest request) {
        ResourceOperationResponse response = resourceOperationService.executeOperation(request);
        return ResponseEntity.ok(response);
    }
}
