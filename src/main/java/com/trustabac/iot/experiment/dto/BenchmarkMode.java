package com.trustabac.iot.experiment.dto;

/**
 * Benchmark execution / analytical reference modes for Phase 8A evaluation.
 * MODE_C is the authoritative live production pipeline.
 * MODE_A and MODE_B are offline, non-authoritative analytical reference models
 * computed counterfactually from recorded workload parameters without mutating state.
 */
public enum BenchmarkMode {
    MODE_C_FULL_TRUSTABAC_BLOCKCHAIN,
    MODE_A_ABAC_ONLY_ANALYTICAL_REFERENCE,
    MODE_B_ABAC_TRUST_RISK_ANALYTICAL_REFERENCE
}
