# Phase 8C: Empirical Data Quality & Integrity Audit Report

## 1. Frozen Dataset Provenance
- **Primary JSON Dataset**: `experiment_results_phase8b.json`
  - **SHA-256 Checksum**: `3d17535027deaf2498b5def17fc99bdcd2baf54e39d127a72d472170874d7b52`
- **Primary CSV Dataset**: `experiment_results_phase8b.csv`
  - **SHA-256 Checksum**: `6f26deb700fd250630404506f10a6c25cd6fda4820e20c90279b9d84e3e9adb5`
- **Timestamp**: `2026-09-22T09:24:01.745033+00:00`
- **Environment Fingerprint**: `Java / Ganache`

## 2. Structural & Count Reconciliation
| Verification Metric | Expected Value | Observed Value | Status |
| :--- | :--- | :--- | :--- |
| Total Raw Samples | 960 | 960 | PASS |
| Warm-up Operations (Excluded from stats) | 240 | 240 | PASS |
| Primary Measured Samples | 720 | 720 | PASS |
| Scenarios Evaluated | 8 | 8 | PASS |
| Repetitions per Scenario | 3 | 3 | PASS |
| Measured Samples per Repetition | 30 | 30 | PASS |
| Measured Samples per Scenario ($N$) | 90 | 90 | PASS |

## 3. Data Integrity & Domain Invariant Checks
| Invariant Category | Rule Definition | Violations Found | Status |
| :--- | :--- | :--- | :--- |
| Latency Non-Negativity | $Latency_{\text{auth}} \ge 0, Latency_{\text{enforce}} \ge 0$ | 0 | PASS |
| Decision-Enforcement Coupling | ALLOW$\to$EXECUTED, RESTRICT$\to$DOWNGRADED, DENY$\to$BLOCKED | 0 | PASS |
| Trust Score Range | $Trust \in [0.0, 100.0]$ | 0 | PASS |
| Risk Score Range | $Risk \in [0.0, 100.0] \cup \text{None}$ | 0 | PASS |
| Deterministic Gas Pricing | $Gas = 31,863$ for all Ganache EVM evaluations | 0 | PASS |
| Security Expectation Match | $Observed = Expected$ across all scenarios | 0 | PASS |

## 4. Gas Reconciliation Findings
An explicit audit was performed comparing the raw transaction logs against initial draft proposals:
- **Frozen Raw Records**: Exactly **31,863 gas** per successful on-chain invocation (`AdaptiveAccessControl.evaluateAccess`).
- **Measured On-Chain Transactions**: Exactly **421 transactions** across 720 measured requests.
- **Pre-Blockchain Rejections (0 Gas)**: High Risk Gate (90 requests), ABAC Failures (90 requests), Blockchain Outage (90 requests), and 29 denied requests in Mixed Workload.
- **Reconciliation Resolution**: All Phase 8C artifacts, tables, and discussions strictly report the reconciled empirical value of **31,863 gas** per evaluation.
