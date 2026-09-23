package com.trustabac.iot.controller;

import com.trustabac.iot.dto.PolicyCreateRequest;
import com.trustabac.iot.dto.PolicyResponse;
import com.trustabac.iot.dto.PolicyUpdateRequest;
import com.trustabac.iot.service.PolicyService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for ABAC Policy administration and condition management.
 */
@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;

    public PolicyController(PolicyService policyService) {
        this.policyService = policyService;
    }

    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(@Valid @RequestBody PolicyCreateRequest request) {
        PolicyResponse created = policyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        List<PolicyResponse> policies = policyService.getAllPolicies();
        return ResponseEntity.ok(policies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponse> getPolicyById(@PathVariable Long id) {
        PolicyResponse policy = policyService.getPolicyById(id);
        return ResponseEntity.ok(policy);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PolicyResponse> updatePolicy(
            @PathVariable Long id,
            @Valid @RequestBody PolicyUpdateRequest request) {
        PolicyResponse updated = policyService.updatePolicy(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        policyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
