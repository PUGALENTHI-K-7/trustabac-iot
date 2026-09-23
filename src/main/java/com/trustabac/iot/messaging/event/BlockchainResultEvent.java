package com.trustabac.iot.messaging.event;

import com.trustabac.iot.entity.Decision;

/**
 * Domain event capturing on-chain smart-contract evaluation proofs.
 */
public record BlockchainResultEvent(
        String transactionHash,
        Long blockNumber,
        String contractAddress,
        Decision decision,
        String deviceIdentifier,
        String resource,
        String operation,
        String timestamp,
        String correlationId
) {
}
