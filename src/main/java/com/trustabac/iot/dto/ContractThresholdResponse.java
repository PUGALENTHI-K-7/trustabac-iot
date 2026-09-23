package com.trustabac.iot.dto;

public record ContractThresholdResponse(
        Integer trustHigh,
        Integer trustMedium,
        Integer riskLow,
        Integer riskMedium,
        String contractAddress
) {
}
