# Phase 8A: Research Experimental Evaluation & Benchmarking Framework

## 1. Executive Summary & Experimental Objectives
Phase 8A implements a reproducible, empirical experimental evaluation and benchmarking framework for **TrustABAC-IoT**. The framework enables controlled, scientific workload generation, multi-tier latency measurement, throughput profiling, blockchain transaction and gas cost accounting, scenario behavior verification, and offline counterfactual comparison.

All workloads exercise the system **exclusively through authoritative production APIs and services**:
- Decision Gateways: `DecisionCoordinator` (Gates 1-4)
- Enforcement Pipeline: `ResourceOperationService` (Gate 5)
- REST APIs: `/api/authorization/evaluate`, `/api/resource-operations/execute`, `/api/simulator/scenarios/*`
- REST Experiment Control: `/api/experiments/run`, `/api/experiments/{runId}/metrics`, `/api/experiments/{runId}/comparison`

Zero client-side authorization rules or alternate execution paths were introduced.

---

## 2. Strict Latency Definitions & Metric Separation
To guarantee academic rigor and prevent conflation between network latency and authorization computation, three discrete latency metrics are tracked and reported:

| Metric Name | Scope & Gates Measured | Definition |
|---|---|---|
| **`authorizationLatency`** | Gates 1 to 4 | Time elapsed computing ABAC eligibility + Trust score retrieval + Contextual Risk evaluation + Smart-contract on-chain authorization. |
| **`enforcementLatency`** | Gate 5 | Authoritative resource operation execution time, including hardware command execution, physical suppression, and parameters clamping. |
| **`clientEndToEndLatency`** | Client Round-Trip | Client-observed wall-clock duration from initiating the HTTP request to receiving the full JSON response, including HTTP/JSON transport overhead. |

> **Note on Time Measurement**: In accordance with user specifications, human-readable start/end timestamps use `LocalDateTime` / `Instant`. All elapsed duration and latency measurements use `System.nanoTime()` exclusively for delta calculations. `System.nanoTime()` is never persisted as an absolute timestamp.

---

## 3. Scenario Success Semantics
Verification success is strictly defined as:
$$\text{Expected Scenario Behavior} \equiv \text{Observed System Behavior}$$

A `DENY` or `BLOCKED` outcome is **NOT** an experiment failure when `DENY` / `BLOCKED` is the expected security behavior.

| Scenario Archetype | Controlled Experimental Workload | Expected Decision | Expected Enforcement | Expected ABAC |
|---|---|---|---|---|
| **`NORMAL_ACCESS`** | Authorized guest on registered fleet (`LIGHT-001`, `TV-001`, `AC-001`, `THERMOSTAT-001`, `DOOR-SENSOR-001`) with active booking and normal Wi-Fi context. | `ALLOW` | `EXECUTED` | `PASS` |
| **`RESTRICT_ACCESS`** | Door lock control under moderate risk context (cellular network, frequency=12, violations=2). | `RESTRICT` | `DOWNGRADED` | `PASS` |
| **`LOW_TRUST`** | Device with degraded trust score ($< 30.0$) attempting resource operations. | `DENY` | `BLOCKED` | `PASS` |
| **`HIGH_RISK`** | Extreme risk contextual access (anomalous indicator, public IP, frequency=35, violations=4). | `DENY` | `BLOCKED` | `PASS` |
| **`ABAC_FAILURE`** | Unauthorized resource (`SECURITY_CAMERA` / `CAM-001`) with no guest policy. | `DENY` | `BLOCKED` | `FAIL` |
| **`MIXED_SECURITY_WORKLOAD`** | Deterministic pseudo-random blend across all archetypes. | Mixed | Mixed | Mixed |
| **`BLOCKCHAIN_OUTAGE`** | Simulated RPC outage / circuit-breaker fail-closed state. | `DENY` | `BLOCKED` | `PASS` |
| **`RECOVERY`** | Resumed operations following blockchain recovery and health restoration. | `ALLOW` | `EXECUTED` | `PASS` |

---

## 4. Offline Counterfactual Reference Modes
To benchmark TrustABAC-IoT against classical architectures without creating live security bypasses, offline analytical reference comparisons are computed post-run:

- **`MODE_A_ABAC_ONLY_ANALYTICAL_REFERENCE`**: Pure analytical calculation of what an ABAC-only architecture would decide. Never executes commands, never mutates trust/risk/blockchain.
- **`MODE_B_ABAC_TRUST_RISK_ANALYTICAL_REFERENCE`**: Pure analytical calculation of gateway-only ABAC+Trust+Risk without smart-contract verification.
- **`MODE_C_FULL_TRUSTABAC_BLOCKCHAIN`**: The live, authoritative, production TrustABAC-IoT execution path.

---

## 5. Measured Experimental Benchmark Results

### Execution Environment & Metadata
- **Environment**: Local Development / Ganache EVM Testbed (Chain ID 1337)
- **Database**: MySQL 8.0 Container (Port 3307)
- **Message Broker**: RabbitMQ 3.13 Container (Port 5672)
- **Smart Contract**: `AdaptiveAccessControl.sol` at `0xCfEB869F69431e42cdB54A4F4f105C19C080A601`
- **Total Scenarios Evaluated**: 8
- **Total Operations Evaluated**: 64
- **Verification Result**: **52 / 52 Checks Passed (100.0% Success Rate)**

### Statistical Latency & Throughput Summary Table
| Scenario Type | Ops | Seed | Mean Auth Latency (ms) | Median Auth (ms) | p95 Auth (ms) | Mean Enforce Latency (ms) | Throughput (req/s) | Result Semantics |
|---|---|---|---|---|---|---|---|---|
| **`NORMAL_ACCESS`** | 10 | 42 | 60.17 | 56.40 | 78.43 | 70.79 | 13.74 | MATCHED (ALLOW / EXECUTED) |
| **`RESTRICT_ACCESS`** | 8 | 101 | 65.08 | 63.85 | 74.31 | 76.57 | 12.64 | MATCHED (RESTRICT / DOWNGRADED) |
| **`LOW_TRUST`** | 6 | 202 | 51.54 | 49.94 | 57.13 | 60.63 | 15.75 | MATCHED (DENY / BLOCKED) |
| **`HIGH_RISK`** | 6 | 303 | 54.79 | 56.27 | 58.60 | 64.45 | 14.60 | MATCHED (DENY / BLOCKED) |
| **`ABAC_FAILURE`** | 8 | 404 | 30.45 | 30.02 | 35.32 | 35.82 | 25.56 | MATCHED (ABAC FAIL / BLOCKED) |
| **`MIXED_SECURITY_WORKLOAD`** | 15 | 505 | 49.89 | 50.63 | 64.20 | 58.69 | 16.67 | MATCHED (Distribution Verified) |
| **`BLOCKCHAIN_OUTAGE`** | 5 | 606 | 51.20 | 46.59 | 60.97 | 60.24 | 15.63 | MATCHED (Fail-Closed DENY) |
| **`RECOVERY`** | 6 | 707 | 51.41 | 51.22 | 55.52 | 60.48 | 15.63 | MATCHED (Resumed ALLOW) |

### Key Observations:
1. **ABAC Short-Circuit Efficiency**: `ABAC_FAILURE` recorded the lowest mean authorization latency (**30.45 ms**) because ineligible requests fail fast at Gate 1 without invoking downstream Trust, Risk, or on-chain smart contract transactions.
2. **On-Chain Evaluation Cost**: Successful evaluations requiring smart-contract verification consume a predictable **31,863 gas** per transaction on Ganache.
3. **Enforcement Separation**: Hardware operation dispatch and safe parameter clamping adds an average overhead of **~9.5 ms** over raw authorization latency.

---

## 6. Artifact Exports
The evaluation framework exported standard machine-readable results:
- **JSON Format**: `contracts/experiment_results_phase8a.json`
- **CSV Format**: `contracts/experiment_results_phase8a.csv`

---

## 7. Simulation vs. Physical IoT Hardware Distinction
All tests in this suite were conducted using simulated software device endpoints adhering to standard IoT communication protocols. Measured absolute latencies reflect local development infrastructure and are intended for comparative analysis across architectural archetypes rather than universal IoT hardware performance claims.
