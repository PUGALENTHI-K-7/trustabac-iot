# TrustABAC-IoT: Final Release & Submission Freeze Audit Report

**Audit Timestamp**: 2026-09-22T20:22:00+05:30  
**Project**: TrustABAC-IoT: Adaptive Trust- and Risk-Aware Smart-Contract Access Control for Resource-Constrained IoT Networks  
**Status**: **SUBMISSION READY**

---

## A. Frozen Dataset Verification
- The empirical Phase 8B research dataset has been preserved with 100% cryptographic integrity:
  - `contracts/experiment_results_phase8b.json`: `3d17535027deaf2498b5def17fc99bdcd2baf54e39d127a72d472170874d7b52` (**EXACT MATCH**)
  - `contracts/experiment_results_phase8b.csv`: `6f26deb700fd250630404506f10a6c25cd6fda4820e20c90279b9d84e3e9adb5` (**EXACT MATCH**)
- Zero regeneration or mutation of raw measurements occurred.

---

## B. Workload Seed Reconciliation
- **Authoritative PRNG Seed Formula**: `seed = r * 1000 + s * 100 + 42` (Base seed `42`, initial seed `1042` for Repetition 1, scenario index `s = 0..7`, repetition `r = 1..3`).
- **Chain ID**: `1337` (Ganache EVM private testnet identifier).
- All thesis chapters, presentations, viva Q&As, and methodologies have been synchronized to distinguish the Ganache EVM Chain ID (`1337`) from the PRNG workload seed sequence (`1042` to `3742` with base seed `42`).

---

## C. Numerical Consistency Results
Exact consistency across raw datasets, statistical tables, figures, thesis chapters, and presentation outlines:
- **Total Measured Requests**: 720 ($N = 90$ per scenario across 8 scenarios and 3 repetitions of 30 requests each).
- **Total Warm-up Requests**: 240 (10 per repetition $\times$ 3 repetitions $\times$ 8 scenarios).
- **Total Raw Requests**: 960 ($240 + 720$).
- **Authorization Latency**: Normal Access ($61.217 \pm 2.258$ ms), Gate 1 ABAC Failure ($34.814 \pm 0.699$ ms), Outage Fast-Fail ($0.100 \pm 0.000$ ms).
- **Decision & Enforcement Totals**: $221$ ALLOW / EXECUTED, $110$ RESTRICT / DOWNGRADED, $389$ DENY / BLOCKED ($100.0\%$ match rate).
- **Mode Comparison**: Mode A agreement = $43.19\%$ ($311/720$), Mode B agreement = $100.0\%$ ($720/720$).
- **Blockchain Execution**: Exactly $31,863$ gas per on-chain transaction ($421$ transactions, $13,414,323$ total gas); $299$ requests filtered pre-EVM with zero gas.

---

## D. Claim & Simulation Language Audit
- **Simulation Language**: All references across documentation and thesis chapters have been verified to state "simulated device operations", "software-level enforcement", and "in-memory device simulation", explicitly avoiding claims of physical microcontroller hardware evaluation.
- **Evidence-Based Framing**: All claims use empirical language ("the measured dataset showed", "the evaluated workloads exhibited", "the results were consistent with") rather than unverified causal assertions ("proves").

---

## E. Literature Reference Audit
- **Verified Citations**: 10 real, peer-reviewed publications (Hu et al. 2014 NIST SP 800-162, Dsouza et al. 2014, Ouaddah et al. 2016, Dorri et al. 2017, Novo 2018, Zhang et al. 2018, Bernabe et al. 2019, Liu et al. 2020, Khurshid et al. 2020, Riaz et al. 2021).
- **Positioning**: Literature review acknowledges prior work combining Blockchain + ABAC + Trust and frames the project contribution around the end-to-end reference implementation, decomposed latency evaluation, and offline reconciliation.

---

## F. Figure and Table Catalog Consistency
- **Figures**: 10 verified charts in `contracts/phase8c_analysis/charts/` matching Figures 1–10 in `docs/final_figure_catalog.md` and thesis text.
- **Tables**: 8 reconciled tables in `contracts/phase8c_analysis/` matching Tables 1–8 in `docs/final_table_catalog.md` and thesis text.

---

## G. Demonstration Script Readiness
- [`docs/final_demo_script.md`](file:///j:/PROJECT/TRUST%20-ABAC/trustabac-iot/docs/final_demo_script.md) provides a clean, 10–15 minute live defense demonstration sequence with zero hardcoded secrets, current contract addresses, and automated post-test baseline restoration.

---

## H. Viva Voce Readiness
- [`docs/viva_questions_and_answers.md`](file:///j:/PROJECT/TRUST%20-ABAC/trustabac-iot/docs/viva_questions_and_answers.md) contains 40+ comprehensive questions and precise, implementation-accurate answers across categories A through T.

---

## I. Clean-Room Reproducibility Results
- Clean startup flow validated against [`docs/local_runbook.md`](file:///j:/PROJECT/TRUST%20-ABAC/trustabac-iot/docs/local_runbook.md):
  - Docker containers: MySQL 8.0 (port 3307), RabbitMQ 3.13 (port 5672/15672), Ganache EVM (port 8545).
  - Smart contract deployed at `0xe78A0F7E598Cc8b0Bb87894B0F60dD2a88d6a8Ab` (Chain ID 1337).
  - Spring Boot gateway runs on port 8090 via environment variable parameters.

---

## J. Security Scan Results
- **Scanner**: [`contracts/security_scan_final.py`](file:///j:/PROJECT/TRUST%20-ABAC/trustabac-iot/contracts/security_scan_final.py)
- **Result**: **0 Potential Secret Findings across 302 scanned files**. Zero embedded private keys or plaintext credentials.

---

## K. Maven Test Results
- **Command**: `./mvnw.cmd clean test`
- **Result**: `BUILD SUCCESS` — **210 / 210 Tests Passed (0 Failures, 0 Errors, 0 Skipped)**.

---

## L. Final Smoke-Test Results
- **Suite**: [`contracts/final_smoke_test.py`](file:///j:/PROJECT/TRUST%20-ABAC/trustabac-iot/contracts/final_smoke_test.py)
- **Result**: **15 / 15 Passed (100%)** — System left in a clean, healthy baseline state.

---

## M. Remaining Issues
- **Zero Unresolved Issues**. All milestones (Phases 1–10) are complete and validated.

---

```
================================================================================
                    PROJECT STATUS: SUBMISSION READY
================================================================================
```
