# Chapter 8 — Results and Analysis

## 8.1 Overview of Empirical Results
The empirical evaluation of TrustABAC-IoT was conducted using the frozen Phase 8B dataset comprising 720 primary measured requests across 8 operational scenarios. All reported values have been reconciled against the automated Phase 8C statistical analysis engine.

---

## 8.2 Authorization and Enforcement Latency Results

Table 8.1 presents the measured authorization and enforcement latency across all 8 evaluated scenarios ($N = 90$ requests per scenario, pooled across 3 repetitions).

### Table 8.1: Per-Scenario Latency Decomposition and Confidence Intervals (N = 90 per scenario)

| Scenario | Auth Mean $\pm$ SE (ms) | Auth Median (ms) | Auth p95 (ms) | Auth Student's t $CI_{95}$ (ms) | Enforce Mean $\pm$ SE (ms) | Enforce Median (ms) | Enforce p95 (ms) | Enforce $CI_{95}$ (ms) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| `NORMAL_ACCESS` | 61.217 $\pm$ 2.258 | 58.802 | 72.908 | [56.730, 65.704] | 72.020 $\pm$ 2.657 | 69.178 | 85.774 | [66.741, 77.299] |
| `RESTRICT_ACCESS` | 56.005 $\pm$ 0.924 | 54.228 | 68.966 | [54.169, 57.840] | 65.888 $\pm$ 1.087 | 63.798 | 81.137 | [63.729, 68.047] |
| `LOW_TRUST` | 59.435 $\pm$ 3.644 | 55.157 | 76.466 | [52.194, 66.676] | 69.924 $\pm$ 4.287 | 64.891 | 89.959 | [61.405, 78.442] |
| `HIGH_RISK` | 43.953 $\pm$ 0.831 | 42.518 | 54.329 | [42.301, 45.604] | 51.709 $\pm$ 0.978 | 50.022 | 63.916 | [49.766, 53.652] |
| `ABAC_FAILURE` | 34.814 $\pm$ 0.699 | 34.170 | 46.689 | [33.424, 36.204] | 40.958 $\pm$ 0.823 | 40.200 | 54.928 | [39.323, 42.593] |
| `MIXED_SECURITY_WORKLOAD` | 52.478 $\pm$ 2.548 | 50.165 | 71.997 | [47.415, 57.541] | 61.739 $\pm$ 2.998 | 59.017 | 84.702 | [55.782, 67.695] |
| `BLOCKCHAIN_OUTAGE` | 0.100 $\pm$ 0.000 | 0.100 | 0.100 | [0.100, 0.100] | 0.002 $\pm$ 0.000 | 0.001 | 0.003 | [0.002, 0.003] |
| `RECOVERY` | 41.289 $\pm$ 0.844 | 39.011 | 53.785 | [39.611, 42.967] | 48.575 $\pm$ 0.993 | 45.896 | 63.277 | [46.601, 50.549] |

### Key Observations:
1. **Full On-Chain Evaluation Overhead**: For legitimate requests requiring smart contract evaluation (`NORMAL_ACCESS`), mean authorization latency was $61.217 \pm 2.258$ ms ($p95 = 72.908$ ms), with enforcement latency adding $72.020 \pm 2.657$ ms for physical state simulation and database logging.
2. **Pre-Blockchain Gateway Short-Circuiting**: In `ABAC_FAILURE`, where invalid booking attributes were rejected at Gate 1, mean authorization latency dropped to $34.814 \pm 0.699$ ms—a 43.13% reduction compared to normal access—due to the complete bypass of trust calculation, risk evaluation, and EVM transactions.
3. **Fail-Closed Fast-Path Isolation**: Under `BLOCKCHAIN_OUTAGE`, the gateway's fail-closed handler rejected requests in $0.100$ ms, preventing systemic blocking or resource lock-up during infrastructure outages.

---

## 8.3 Sequential Workload Throughput

Table 8.2 summarizes sequential client processing throughput across the 3 experimental runs.

### Table 8.2: Sequential Request Processing Throughput

| Scenario | Measured N | Mean Client E2E per 30-Req Run (ms) | Mean Throughput (ops/sec) | Observed Run Range (ops/sec) |
| :--- | :---: | :---: | :---: | :---: |
| `NORMAL_ACCESS` | 90 | 3018.56 | 13.98 | [12.38, 15.17] |
| `RESTRICT_ACCESS` | 90 | 2787.75 | 15.21 | [14.18, 15.79] |
| `LOW_TRUST` | 90 | 2810.50 | 14.39 | [13.19, 15.98] |
| `HIGH_RISK` | 90 | 2130.26 | 19.37 | [18.25, 20.68] |
| `ABAC_FAILURE` | 90 | 1881.97 | 24.54 | [22.30, 27.00] |
| `MIXED_SECURITY_WORKLOAD` | 90 | 2493.39 | 16.21 | [15.50, 16.83] |
| `BLOCKCHAIN_OUTAGE` | 90 | 58.62 | 0.00* | [0.00, 0.00] |
| `RECOVERY` | 90 | 2011.17 | 20.66 | [19.06, 22.37] |

*\*Note: Throughput during outage reflects 0.00 permitted operations per second due to 100% fail-closed denial.*

*Important Methodological Clarification*: The throughput values represent **sequential workload execution** where each request is sent after receiving the response to the prior request. They do not represent maximum concurrent throughput capacity under parallel multithreading.

---

## 8.4 Policy Decision and Resource Enforcement Matrix

Table 8.3 details the policy decisions produced by the smart contract and the corresponding device actuations enforced by `ResourceOperationService` on simulated device models.

### Table 8.3: Policy Decision and Resource Enforcement Distributions

| Scenario | N | Decisions (ALLOW / RESTRICT / DENY) | Enforcement (EXECUTED / DOWNGRADED / BLOCKED) | Verification Match Rate |
| :--- | :---: | :---: | :---: | :---: |
| `NORMAL_ACCESS` | 90 | 90 / 0 / 0 | 90 / 0 / 0 | 100.0% PASS |
| `RESTRICT_ACCESS` | 90 | 0 / 90 / 0 | 0 / 90 / 0 | 100.0% PASS |
| `LOW_TRUST` | 90 | 0 / 0 / 90 | 0 / 0 / 90 | 100.0% PASS |
| `HIGH_RISK` | 90 | 0 / 0 / 90 | 0 / 0 / 90 | 100.0% PASS |
| `ABAC_FAILURE` | 90 | 0 / 0 / 90 | 0 / 0 / 90 | 100.0% PASS |
| `MIXED_SECURITY_WORKLOAD` | 90 | 41 / 20 / 29 | 41 / 20 / 29 | 100.0% PASS |
| `BLOCKCHAIN_OUTAGE` | 90 | 0 / 0 / 90 | 0 / 0 / 90 | 100.0% PASS |
| `RECOVERY` | 90 | 90 / 0 / 0 | 90 / 0 / 0 | 100.0% PASS |
| **POOLED TOTAL** | **720** | **221 / 110 / 389** | **221 / 110 / 389** | **100.0% PASS** |

The evaluated dataset achieved **100.0% concordance** between expected scenario behavior and observed system behavior across all 720 measured requests.

---

## 8.5 Reference Mode Agreement and Counterfactual Divergence

Table 8.4 presents the comparative evaluation of Pure ABAC (Mode A) and Centralized T-ABAC (Mode B) against authoritative blockchain execution (Mode C).

### Table 8.4: Reference Mode Agreement Analysis (N = 720)

| Scenario | Sample N | Mode A Agreement (%) | Mode B Agreement (%) | Primary Source of Counterfactual Divergence |
| :--- | :---: | :---: | :---: | :--- |
| `NORMAL_ACCESS` | 90 | 100.00% (90/90) | 100.00% (90/90) | Full concordance under valid attributes and high trust. |
| `RESTRICT_ACCESS` | 90 | 0.00% (0/90) | 100.00% (90/90) | Mode A lacks dynamic contextual risk & operation downgrading. |
| `LOW_TRUST` | 90 | 0.00% (0/90) | 100.00% (90/90) | Mode A ignores degraded historical reputation ($T < 30$). |
| `HIGH_RISK` | 90 | 0.00% (0/90) | 100.00% (90/90) | Mode A ignores contextual environment risk elevation ($R > 70$). |
| `ABAC_FAILURE` | 90 | 100.00% (90/90) | 100.00% (90/90) | Concordance on invalid booking Gate 1 rejection. |
| `MIXED_SECURITY_WORKLOAD` | 90 | 45.56% (41/90) | 100.00% (90/90) | Mode A diverges on degraded trust and high-risk stochastic requests. |
| `BLOCKCHAIN_OUTAGE` | 90 | 0.00% (0/90) | 100.00% (90/90) | Mode A analytical model does not incorporate blockchain availability. |
| `RECOVERY` | 90 | 100.00% (90/90) | 100.00% (90/90) | Full concordance restored following node reconnection. |
| **POOLED TOTAL** | **720** | **43.19% (311/720)** | **100.00% (720/720)** | **Overall counterfactual policy divergence across 720 requests.** |

### Analysis of Divergence:
- **Mode A (Pure ABAC)** diverged on **56.81%** of requests (409/720 disagreements). In `RESTRICT_ACCESS`, `LOW_TRUST`, and `HIGH_RISK`, Mode A granted unconditional `ALLOW` because the tenant possessed a valid reservation, failing to prevent operations under degraded trust or severe environmental risk.
- **Mode B (Centralized T-ABAC)** achieved **100.0% policy decision agreement** (720/720), proving that the centralized logic correctly mirrors the smart contract's decision matrix. However, Mode B lacks on-chain cryptographic receipts and multi-party non-repudiation proofs.

---

## 8.6 Blockchain Gas Consumption and Execution Overhead

Table 8.5 details the on-chain transactions and gas consumption observed on the Ganache EVM testbed.

### Table 8.5: Blockchain Transaction and Gas Metrics (Ganache EVM)

| Scenario | Measured Requests | On-Chain Transactions | Gas per Transaction | Total Gas Consumed | Nominal Cost (ETH @ 20 Gwei) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `NORMAL_ACCESS` | 90 | 90 | 31,863 gas | 2,867,670 gas | 0.00063726 ETH |
| `RESTRICT_ACCESS` | 90 | 90 | 31,863 gas | 2,867,670 gas | 0.00063726 ETH |
| `LOW_TRUST` | 90 | 90 | 31,863 gas | 2,867,670 gas | 0.00063726 ETH |
| `HIGH_RISK` | 90 | 0 (Pre-EVM Filter) | 0 gas | 0 gas | 0.00000000 ETH |
| `ABAC_FAILURE` | 90 | 0 (Gate 1 Filter) | 0 gas | 0 gas | 0.00000000 ETH |
| `MIXED_SECURITY_WORKLOAD` | 90 | 61 | 31,863 gas | 1,943,643 gas | 0.00063726 ETH |
| `BLOCKCHAIN_OUTAGE` | 90 | 0 (Fail-Closed) | 0 gas | 0 gas | 0.00000000 ETH |
| `RECOVERY` | 90 | 90 | 31,863 gas | 2,867,670 gas | 0.00063726 ETH |
| **POOLED TOTAL** | **720** | **421** | **31,863 gas** | **13,414,323 gas** | **0.26828646 ETH** |

### Key Findings:
- Every on-chain evaluation consumed an exact, deterministic **31,863 gas**, reflecting predictable EVM bytecode execution paths.
- Out of 720 measured requests, **299 requests were rejected pre-EVM** at the gateway (Gate 1 ABAC failure, high contextual risk, or outage fast-fail), resulting in **zero on-chain gas expenditure** for rejected transactions.
