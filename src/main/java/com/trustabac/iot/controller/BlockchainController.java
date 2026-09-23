package com.trustabac.iot.controller;

import com.trustabac.iot.dto.BlockchainStatusResponse;
import com.trustabac.iot.dto.ContractThresholdResponse;
import com.trustabac.iot.service.blockchain.BlockchainService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for blockchain status, contract thresholds, and deployment.
 */
@RestController
@RequestMapping("/api/blockchain")
public class BlockchainController {

    private final BlockchainService blockchainService;

    public BlockchainController(BlockchainService blockchainService) {
        this.blockchainService = blockchainService;
    }

    @GetMapping("/status")
    public ResponseEntity<BlockchainStatusResponse> getStatus() {
        return ResponseEntity.ok(blockchainService.getStatus());
    }

    @GetMapping("/thresholds")
    public ResponseEntity<ContractThresholdResponse> getThresholds() {
        return ResponseEntity.ok(blockchainService.getThresholds());
    }

    @PostMapping("/deploy")
    public ResponseEntity<Map<String, String>> deployContract() {
        String address = blockchainService.deployContract();
        return ResponseEntity.ok(Map.of(
                "status", "DEPLOYED",
                "contractAddress", address,
                "message", "AdaptiveAccessControl smart contract successfully deployed."
        ));
    }
}
