package com.trustabac.iot.controller;

import com.trustabac.iot.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller providing health check endpoints for the TrustABAC-IoT gateway.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(new HealthResponse("UP"));
    }
}
