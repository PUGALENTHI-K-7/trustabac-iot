package com.trustabac.iot.controller;

import com.trustabac.iot.dto.TrustEventRequest;
import com.trustabac.iot.dto.TrustHistoryResponse;
import com.trustabac.iot.dto.TrustScoreResponse;
import com.trustabac.iot.dto.TrustUpdateResponse;
import com.trustabac.iot.service.TrustService;
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
 * REST Controller for IoT device behavioral trust score querying, append-only history retrieval,
 * and security/behavioral event processing.
 */
@RestController
@RequestMapping("/api/trust")
public class TrustController {

    private final TrustService trustService;

    public TrustController(TrustService trustService) {
        this.trustService = trustService;
    }

    @GetMapping("/{deviceIdentifier}")
    public ResponseEntity<TrustScoreResponse> getCurrentTrust(@PathVariable String deviceIdentifier) {
        TrustScoreResponse response = trustService.getCurrentTrust(deviceIdentifier);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{deviceIdentifier}/history")
    public ResponseEntity<List<TrustHistoryResponse>> getTrustHistory(@PathVariable String deviceIdentifier) {
        List<TrustHistoryResponse> history = trustService.getTrustHistory(deviceIdentifier);
        return ResponseEntity.ok(history);
    }

    @PostMapping("/{deviceIdentifier}/events")
    public ResponseEntity<TrustUpdateResponse> recordTrustEvent(
            @PathVariable String deviceIdentifier,
            @Valid @RequestBody TrustEventRequest request) {
        TrustUpdateResponse response = trustService.recordTrustEvent(deviceIdentifier, request);
        return ResponseEntity.ok(response);
    }
}
