package com.trustabac.iot.controller;

import com.trustabac.iot.dto.AccessEvaluationRequest;
import com.trustabac.iot.dto.AccessEvaluationResponse;
import com.trustabac.iot.dto.AccessRequestLogResponse;
import com.trustabac.iot.service.AbacService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller for ABAC access eligibility evaluation and audit log retrieval.
 */
@RestController
@RequestMapping("/api/access")
public class AccessController {

    private final AbacService abacService;

    public AccessController(AbacService abacService) {
        this.abacService = abacService;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<AccessEvaluationResponse> evaluateAccess(
            @Valid @RequestBody AccessEvaluationRequest request) {
        AccessEvaluationResponse response = abacService.evaluateAccess(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests/{id}")
    public ResponseEntity<AccessRequestLogResponse> getAccessRequestLogById(@PathVariable Long id) {
        AccessRequestLogResponse logResponse = abacService.getAccessRequestLogById(id);
        return ResponseEntity.ok(logResponse);
    }

    @GetMapping("/requests")
    public ResponseEntity<List<AccessRequestLogResponse>> getAllAccessRequestLogs() {
        List<AccessRequestLogResponse> logs = abacService.getAllAccessRequestLogs();
        return ResponseEntity.ok(logs);
    }
}
