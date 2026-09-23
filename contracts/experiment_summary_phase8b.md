# Phase 8B: Controlled Experimental Campaign & Statistical Summary Report

## 1. Experimental Methodology & Metadata
- **Total Scenarios**: 8
- **Repetitions per Scenario**: 3
- **Warm-up Requests per Run**: 10 (strictly excluded from reported latency/throughput statistics)
- **Measured Requests per Run**: 30
- **Total Measured Empirical Samples**: 720
- **Statistical Confidence Method**: Student's t-distribution $CI_{95} = \bar{x} \pm t_{0.975, N-1} \cdot SE$
- **Execution Date**: 2026-09-22 09:24:03 UTC

## 2. Pooled Statistical Results by Scenario

| Scenario | Pooled N | Mean Auth (ms) | 95% CI Auth (ms) | Mean Enforce (ms) | 95% CI Enforce (ms) | Throughput (req/s) | Decisions (A/R/D) | Result Semantics |
|---|---|---|---|---|---|---|---|---|
| **`NORMAL_ACCESS`** | 90 | 61.217 | [56.745, 65.689] | 72.02 | [66.759, 77.281] | 13.98 | 90/0/0 | MATCHED [OK] |
| **`RESTRICT_ACCESS`** | 90 | 56.005 | [54.176, 57.834] | 65.888 | [63.736, 68.039] | 15.21 | 0/90/0 | MATCHED [OK] |
| **`LOW_TRUST`** | 90 | 59.435 | [52.22, 66.65] | 69.924 | [61.435, 78.412] | 14.39 | 0/0/90 | MATCHED [OK] |
| **`HIGH_RISK`** | 90 | 43.953 | [42.307, 45.599] | 51.709 | [49.773, 53.645] | 19.37 | 0/0/90 | MATCHED [OK] |
| **`ABAC_FAILURE`** | 90 | 34.814 | [33.429, 36.199] | 40.958 | [39.329, 42.587] | 24.54 | 0/0/90 | MATCHED [OK] |
| **`MIXED_SECURITY_WORKLOAD`** | 90 | 52.478 | [47.433, 57.523] | 61.739 | [55.803, 67.675] | 16.21 | 41/20/29 | MATCHED [OK] |
| **`BLOCKCHAIN_OUTAGE`** | 90 | 0.1 | [0.1, 0.1] | 0.002 | [0.002, 0.003] | 0.0 | 0/0/90 | MATCHED [OK] |
| **`RECOVERY`** | 90 | 41.289 | [39.617, 42.961] | 48.575 | [46.608, 50.542] | 20.66 | 90/0/0 | MATCHED [OK] |

## 3. Offline Reference Mode Agreement Summary
- **Mode A (ABAC-Only Analytical Reference)**: Static rule evaluation without dynamic trust degradation or adaptive downgrading.
- **Mode B (Centralized T-ABAC Analytical Reference)**: Gateway evaluation without on-chain consensus receipts.
- **Mode C (Authoritative TrustABAC-IoT)**: Real on-chain multi-tier adaptive execution pipeline.

## 4. Limitations & Research Statements
- Experiments were performed on local development infrastructure with Ganache EVM testbed.
- IoT hardware devices were simulated endpoints; no physical battery/energy measurements were recorded.
- Absolute latency figures reflect testbed host performance and demonstrate relative architectural separation.
