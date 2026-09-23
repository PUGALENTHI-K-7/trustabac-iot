# TrustABAC-IoT — Research Status Report

> **Produced by**: Repository inspection (source code, experiments, results artifacts, thesis chapters)
> **Date**: 2026-09-24
> **Git Baseline**: v1.0.0 (commit `3c10485`)
> **WARNING**: This document reflects the current repository state. Where documentation and source disagree, the source code is treated as authoritative.

---

## 1. Research Problem

IoT deployments in multi-tenant smart environments (e.g., short-term rental properties) require access control that is:
- **Adaptive**: sensitive to real-time context changes
- **Behavioral**: accounting for long-term device/user reliability history
- **Tamper-evident**: providing non-repudiable authorization proof
- **Graduated**: supporting intermediate privilege levels, not merely binary permit/deny

Static rule-based approaches (RBAC, basic ABAC) and purely centralized authorization servers do not jointly satisfy all four requirements. The gap motivates a multi-tier, smart-contract-enforced approach.

---

## 2. Research Objective

To design, implement, and empirically evaluate an adaptive multi-tiered IoT authorization architecture combining:
1. ABAC structural eligibility filtering (Gate 1)
2. Long-term behavioral Trust scoring (Gate 2)
3. Real-time contextual Risk aggregation (Gate 3)
4. Ethereum smart contract as the authoritative final decision engine (Gate 4)

And to produce blockchain-backed, non-repudiable authorization evidence with graduated enforcement: ALLOW / RESTRICT / DENY → EXECUTED / DOWNGRADED / BLOCKED.

---

## 3. Research Questions

From `docs/thesis/03_problem_objectives_rq.md` (inferred from thesis structure):

- **RQ1**: How can ABAC, behavioral trust, and contextual risk be integrated into a coherent IoT authorization pipeline?
- **RQ2**: Can a Solidity smart contract serve as the authoritative decision engine while Java provides coordination and persistence?
- **RQ3**: What are the authorization latency, throughput, and gas cost characteristics of the full pipeline under controlled scenarios?
- **RQ4**: How does the pipeline compare to ABAC-only and centralized Trust-ABAC reference modes on the experimental dataset?
- **RQ5**: Does the fail-closed blockchain unavailability handler maintain security invariants during outages?

---

## 4. Literature Positioning

### Key Related Work Identified in Thesis
| Reference | Approach | Limitation Addressed by TrustABAC-IoT |
| :--- | :--- | :--- |
| Hu et al. (2014), NIST SP 800-162 | Foundational ABAC | No dynamic trust or blockchain enforcement |
| Ouaddah et al. (2016), FairAccess | Bitcoin-UTXO access control | No Turing-complete policy logic; no trust/risk |
| Zhang et al. (2018) | Multi-contract Ethereum ABAC | High gas, no contextual risk, no enforcement |
| Novo (2018) | Gateway-assisted blockchain IoT | Binary permissions, no behavioral trust decay |
| Bernabe et al. (2019), TACIoT | Multidimensional trust + ABAC | Centralized, no blockchain non-repudiation |

### Research Positioning Statement
The TrustABAC-IoT contribution is an **implemented and experimentally evaluated adaptive authorization pipeline** combining:
- ABAC eligibility gate (structural pre-filter)
- Long-term behavioral Trust scoring (historical reliability)
- Current contextual Risk aggregation (7-factor weighted composite)
- Smart-contract final decision enforcement (Solidity on Ganache EVM)
- Graduated enforcement semantics (ALLOW/RESTRICT/DENY → EXECUTED/DOWNGRADED/BLOCKED)
- On-chain transaction evidence (non-repudiable audit trail)
- Off-chain detailed data (MySQL audit log, trust history, risk events)

> [!IMPORTANT]
> **Claims that must NOT be made**:
> - "Blockchain + ABAC + Trust is a novel combination" (TABI/Pathak 2023, Jiang 2023, Wang 2022 predate this)
> - "ABAC alone is only 43.19% secure" (Mode A is an offline counterfactual in a controlled dataset)
> - "TrustABAC improves security by 100%" (Mode B agrees 100% in this dataset — scoped claim)
> - "Deployed on public blockchain" (Ganache local only)
> - "Physical IoT hardware evaluated" (software simulation only)

---

## 5. Architecture Contribution

The defensible architectural contribution is the **integration and evaluation** of:
- A clean 4-gate sequential pipeline with explicit short-circuit semantics at each gate
- Separation of Trust (long-term, history-based) from Risk (short-term, context-based)
- A smart contract that is the **sole authoritative decision engine** (not a duplicate of Java logic)
- Sensitivity-aware graduated enforcement (RESTRICT behavior differs by resource sensitivity)
- Fail-closed outage resilience with empirical evidence
- Off-chain/on-chain data partitioning: detailed records in MySQL, non-repudiable evidence on-chain

---

## 6. Experimental Methodology

### Design (verified from `experiment_phase8b.py`, `docs/thesis/07_experimental_methodology.md`)

| Parameter | Value |
| :--- | :--- |
| Total scenarios | 8 |
| Repetitions per scenario | 3 |
| Warm-up ops per repetition | 10 (excluded from statistics) |
| Measured ops per repetition | 30 |
| Measured ops per scenario | 90 |
| Total measured ops | 720 |
| Total warm-up ops | 240 |
| Total raw samples | 960 |
| Seed formula | `seed = r * 1000 + s * 100 + 42` |

### Statistical Methods (verified from `experiment_phase8b.py`)
- Mean, Standard Deviation, Standard Error
- Student's t 95% Confidence Interval (manual t-table lookup)
- Median, p75, p25, IQR, p95
- Per-repetition breakdown (R1, R2, R3)

### Reference Modes
| Mode | Description | What it lacks vs. Mode C |
| :--- | :--- | :--- |
| **Mode A** | Pure ABAC (offline counterfactual) | Trust, Risk, blockchain, graduated enforcement |
| **Mode B** | Centralized T-ABAC (offline counterfactual) | Blockchain, on-chain evidence |
| **Mode C** | Full TrustABAC-IoT pipeline (actual system) | N/A — authoritative |

---

## 7. Dataset

### Frozen Dataset (cryptographically verified)
| File | SHA-256 | Size |
| :--- | :--- | :--- |
| `experiment_results_phase8b.json` | `3d17535027deaf2498b5def17fc99bdcd2baf54e39d127a72d472170874d7b52` | 1,038,736 bytes |
| `experiment_results_phase8b.csv` | `6f26deb700fd250630404506f10a6c25cd6fda4820e20c90279b9d84e3e9adb5` | 370,687 bytes |

The dataset is immutable. The `validate_phase8c.py` script enforces SHA-256 hash integrity as the first two of 45 validation checks.

---

## 8. Metrics

Metrics collected per request sample:
- `authLatencyMs` — Time for full Gates 1–4 evaluation
- `enforceLatencyMs` — Time for Gates 1–5 (authorization + enforcement)
- `clientE2EMs` — Total round-trip duration from client perspective
- `throughputRps` — Sequential requests per second per run
- `decision` — ALLOW / RESTRICT / DENY
- `enforcementStatus` — EXECUTED / DOWNGRADED / BLOCKED
- `gasUsed` — Gas consumed per on-chain transaction (0 if no tx)
- `trustScore` — Device trust at time of request
- `riskScore` — Composite risk score at time of request
- `abacResult` — PASS / FAIL
- `modeADecision` — Offline counterfactual ABAC-only decision
- `modeBDecision` — Offline counterfactual T-ABAC decision
- `txHash` — On-chain transaction hash (if applicable)
- `isWarmUp` — Boolean warm-up flag

---

## 9. Baselines

| Baseline | Role |
| :--- | :--- |
| Mode A (ABAC-only) | Counterfactual: what would happen with only structural attribute eligibility, no Trust or Risk |
| Mode B (Centralized T-ABAC) | Counterfactual: what would happen with Trust+ABAC but no blockchain enforcement |
| BLOCKCHAIN_OUTAGE scenario | Availability baseline: behavior under infrastructure failure |
| RECOVERY scenario | Resilience baseline: post-outage behavior |
| ABAC_FAILURE scenario | Short-circuit baseline: Gate 1 efficiency |

---

## 10. Results Summary

### Decision Distribution (720 measured samples)
| Decision | Count | Percentage |
| :--- | :--- | :--- |
| ALLOW | 221 | 30.69% |
| RESTRICT | 110 | 15.28% |
| DENY | 389 | 54.03% |

### Authorization Latency (key scenarios)
| Scenario | Auth Mean (ms) | Notes |
| :--- | :--- | :--- |
| NORMAL_ACCESS | 61.217 | Full 4-gate pipeline, on-chain tx |
| ABAC_FAILURE | 34.814 | Gate 1 short-circuit, 0 gas |
| HIGH_RISK | 43.953 | Pre-blockchain short-circuit, 0 gas |
| BLOCKCHAIN_OUTAGE | 0.100 | Fail-closed handler, 0 gas |

### Blockchain Gas
| Metric | Value |
| :--- | :--- |
| On-chain transactions | 421 / 720 |
| Gas per transaction | 31,863 (constant, σ=0) |
| Total gas consumed | 13,414,323 |
| Nominal cost per tx (20 Gwei) | 0.00063726 ETH |

### Mode Comparison (dataset-scoped)
| Mode | Agreements | Rate |
| :--- | :--- | :--- |
| Mode A (ABAC-only) | 311/720 | 43.19% |
| Mode B (Centralized T-ABAC) | 720/720 | 100.00% |

---

## 11. Security Validation

### 6 Security Invariants Verified (from `thesis_tables.md` Table 9)
| Invariant | Result |
| :--- | :--- |
| Attribute Gate Isolation (ABAC_FAILURE → 0 gas) | PASS |
| Reputation Enforcement (LOW_TRUST → on-chain DENY) | PASS |
| Contextual Anomaly Block (HIGH_RISK → 0 gas, pre-blockchain DENY) | PASS |
| Adaptive Downgrading (RESTRICT → 100% DOWNGRADED) | PASS |
| Fail-Closed Availability (OUTAGE → 100% BLOCKED) | PASS |
| Anti-Double-Authorization (1 request → ≤ 1 tx, exactly) | PASS |

### Phase-level test suites (historical, at v1.0.0)
| Suite | Pass Rate |
| :--- | :--- |
| Maven unit/integration | 210/210 |
| Py-EVM contract | 33/33 |
| Ganache verification | 36/36 |
| Phase 7A Enforcement | 9/9 |
| Phase 7B Simulator | 14/14 |
| Phase 7C RabbitMQ | 6/6 |
| Phase 7D WebSocket | 5/5 |
| Phase 7E Dashboard | 10/10 |
| Phase 8C Validation | 45/45 |
| Final Smoke Test | 15/15 |
| Secret Scan | 302 files, 0 findings |

---

## 12. Limitations

### Scope-of-Claims Limitations (research integrity)
1. **Software-simulated IoT** — No physical hardware, no MCU, no physical bus delays
2. **Local Ganache EVM** — No distributed consensus, no mempool, no gas fee volatility
3. **Sequential benchmarking** — Reported throughput (~14–24 ops/sec) is sequential, not concurrent saturation
4. **Controlled synthetic workloads** — 720 samples over 8 deterministic scenarios; not long-running real-world populations
5. **Single gateway** — No multi-gateway consensus or decentralized oracle
6. **Time risk currently disabled** — application.properties overrides normal hours to 0–24 (always normal)
7. **Prototype thresholds** — Trust deltas and risk weights are starting-point parameters, not scientifically validated universal constants
8. **In-memory idempotency** — No replay protection across JVM restarts

---

## 13. Current Evidence

The following claims are **directly supported by repository evidence**:

| Claim | Evidence |
| :--- | :--- |
| 4-gate authorization pipeline is implemented | Source: `DecisionCoordinator.java`, `AbacService.java`, `RiskService.java`, `BlockchainService.java` |
| Smart contract is authoritative final decision engine | Source: `AdaptiveAccessControl.sol`, `BlockchainService.java` |
| ABAC FAIL short-circuits trust/risk/blockchain | Source: `DecisionCoordinator.java` lines 82-108 |
| Trust and Risk are orthogonal signals | Source: `README.md` Architecture section; AbacService Gate 2 is read-only |
| 720 measured samples, 421 on-chain transactions, 31,863 gas each | Source: `validate_phase8c.py` lines 97-125, frozen JSON/CSV |
| SHA-256 dataset hashes are embedded in validation script | Source: `validate_phase8c.py` lines 36-37 |
| Fail-closed DENY on blockchain unavailability | Source: `BlockchainService.java` + `DecisionCoordinator.java` lines 167-172 |
| All simulated device state is in-memory | Source: `SimulatedDeviceStateStore.java` - ConcurrentHashMap |
| Working tree clean, v1.0.0 tag present | Source: `git status`, `git tag` commands executed 2026-09-24 |

---

## 14. Claims That Must NOT Be Made

| Prohibited Claim | Reason |
| :--- | :--- |
| "ABAC alone is only 43.19% secure" | Mode A is an offline counterfactual in a controlled dataset; not a security measurement |
| "TrustABAC improves security by 100%" | Mode B 100% agreement is dataset-scoped; not a universal security improvement claim |
| "This system integrates with Airbnb" | The smart rental is a demonstration scenario only; no Airbnb API |
| "Deployed on a decentralized blockchain" | Ganache is a local controlled single-node EVM |
| "Physical IoT devices are controlled" | All devices are in-memory Java simulation |
| "Trust and Risk are novel independently" | Extensive prior work on both concepts separately |
| "The combination of Blockchain + ABAC + Trust is novel" | TABI (2023), Jiang (2023), Wang (2022) all combine these concepts |
| "Results generalize to real-world IoT deployments" | Controlled, synthetic, single-gateway, sequential workload only |

---

## 15. Remaining Research Gaps

Based on limitations identified in source and thesis Chapter 11:

| Gap | Description |
| :--- | :--- |
| Physical hardware evaluation | Testing on ESP32, Raspberry Pi, or similar MCU with real actuators |
| Public testnet/L2 evaluation | Gas costs and latency on Sepolia, Polygon, or Arbitrum |
| Distributed gateway consensus | Multi-gateway attribute collection without single point of trust |
| Concurrent load testing | Maximum throughput under saturated parallel request dispatch |
| Long-running behavioral evolution | Trust decay and recovery over weeks/months of real interactions |
| Adversarial robustness | Active attack simulation (replay attacks, Sybil, flooding) |
| Calibrated threshold validation | Scientific methodology for trust delta and risk weight selection |
| Energy measurement | Physical mJ/transaction measurements on IoT hardware |

---

## 16. Final Thesis & Implementation Consistency Updates (v1.1.0)

The following documentation and configuration alignments have been completed for v1.1.0:
1. **Spring Boot Version**: Standardized to `4.1.1` across thesis chapters, checklist, and slide decks.
2. **Solidity Version**: Standardized to `^0.8.20` in thesis tables and implementation chapters.
3. **Security Scan Count**: Synchronized documentation to 302 files scanned (0 findings).
4. **Application Port**: Standardized `application.properties` default to `server.port=8090`.
5. **Time Risk Configuration**: Explicitly documented that normal hours (00:00–24:00) produce zero time-risk contribution to isolate factors during the frozen Phase 8B campaign.
6. **Docker Compose**: Dedicated to infrastructure (MySQL on host port 3307, Ganache on 8545, RabbitMQ on 5672/15672), with Spring Boot launched natively via `START_DEMO.bat`.
7. **Container Names**: Corrected across all test and demonstration scripts to `trustabac-mysql` and `trustabac-ganache`.

---

## 17. Development Roadmap Summary

### v1.0.0 (Baseline Milestone — Complete)
Full prototype with 10 phases implemented, controlled experiment (720 samples), thesis documentation, demo scripts, v1.0.0 tag preserved.

### v1.1.0 (Stabilization Milestone — Completed)
- Documentation consistency fixes (Spring Boot 4.1.1, Solidity ^0.8.20, Security scan 302 files)
- Application and MySQL port standardization (8090, 3307)
- Infrastructure Docker Compose cleanup
- Experiment and test script container name corrections
- Frozen research dataset and methodology strictly preserved

### v1.2.x (Near-term — Security & Robustness)
- Persistent database-backed idempotency (replace in-memory `IdempotencyGuard`)
- Replay protection (nonce or timestamp window)
- Configurable enforcement rules (externalize from hardcoded Java switch)
- Authentication/authorization layer for the REST API
- Booking active boolean representation in ABAC response

### v1.3.x (Research Extensions)
- Multi-repetition concurrent benchmarking
- Additional experimental scenarios (Sybil attack, flooding, replay)
- Time-risk re-evaluation under separate, dedicated experiment campaign
- Configurable trust decay models (exponential decay, time-weighted)

### v2.0.0 (Future Research)
- Physical IoT hardware integration (ESP32/RPi)
- Public testnet or L2 deployment and evaluation
- Multi-gateway consensus mechanism
- Formal security proofs or model checking of decision matrix

---

*End of Research Status Report*
