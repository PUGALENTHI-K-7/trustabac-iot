package com.trustabac.iot.controller;

import com.trustabac.iot.dto.RiskEvaluationRequest;
import com.trustabac.iot.dto.RiskEvaluationResponse;
import com.trustabac.iot.dto.RiskHistoryResponse;
import com.trustabac.iot.service.RiskService;
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
 * REST Controller for Contextual Risk Engine (Gate 3).
 * Exposes endpoints to evaluate contextual danger and query append-only risk audit history.
 */
@RestController
@RequestMapping("/api/risk")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    /**
     * Evaluates the contextual risk for an access request.
     * Note: Does not mutate long-term device trust and does not produce final access decisions.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<RiskEvaluationResponse> evaluateRisk(@Valid @RequestBody RiskEvaluationRequest request) {
        RiskEvaluationResponse response = riskService.resolveAndEvaluate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the current / latest evaluated risk score for a device.
     */
    @GetMapping("/{deviceIdentifier}")
    public ResponseEntity<RiskEvaluationResponse> getCurrentRisk(@PathVariable String deviceIdentifier) {
        RiskEvaluationResponse response = riskService.getCurrentRisk(deviceIdentifier);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves the complete append-only risk evaluation history for a device (newest first).
     */
    @GetMapping("/{deviceIdentifier}/history")
    public ResponseEntity<List<RiskHistoryResponse>> getRiskHistory(@PathVariable String deviceIdentifier) {
        List<RiskHistoryResponse> history = riskService.getRiskHistory(deviceIdentifier);
        return ResponseEntity.ok(history);
    }
}
