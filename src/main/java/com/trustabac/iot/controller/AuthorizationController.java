package com.trustabac.iot.controller;

import com.trustabac.iot.dto.BlockchainAuthorizationRequest;
import com.trustabac.iot.dto.BlockchainAuthorizationResponse;
import com.trustabac.iot.service.DecisionCoordinator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for end-to-end adaptive smart-contract authorization evaluation.
 */
@RestController
@RequestMapping("/api/authorization")
public class AuthorizationController {

    private final DecisionCoordinator decisionCoordinator;

    public AuthorizationController(DecisionCoordinator decisionCoordinator) {
        this.decisionCoordinator = decisionCoordinator;
    }

    @PostMapping("/evaluate")
    public ResponseEntity<BlockchainAuthorizationResponse> evaluateAuthorization(
            @Valid @RequestBody BlockchainAuthorizationRequest request) {
        BlockchainAuthorizationResponse response = decisionCoordinator.evaluateAuthorization(request);
        return ResponseEntity.ok(response);
    }
}
