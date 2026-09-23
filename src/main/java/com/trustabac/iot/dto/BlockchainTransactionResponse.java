package com.trustabac.iot.dto;

public record BlockchainTransactionResponse(
        String transactionHash,
        Long blockNumber,
        Long gasUsed,
        String status,
        String contractAddress
) {
}
