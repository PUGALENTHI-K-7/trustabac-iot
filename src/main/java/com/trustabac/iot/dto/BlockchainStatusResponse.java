package com.trustabac.iot.dto;

public record BlockchainStatusResponse(
        String rpcUrl,
        boolean rpcReachable,
        Long chainId,
        Long latestBlock,
        String contractAddress,
        boolean contractReachable,
        boolean enabled
) {
}
