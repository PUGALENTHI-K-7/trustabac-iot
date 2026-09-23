# TrustABAC-IoT — Review 2 Slide Deck
## "Adaptive Trust- and Risk-Aware Smart-Contract Access Control for Resource-Constrained IoT Networks"

**Course**: Final-Year Project (MIC)
**Batch**: 2024–2026
**Date**: September 2026

---

> **HOW TO USE THIS FILE**
> Each `---` separator marks a new slide boundary.
> Slide titles are `## Slide N — Title`.
> Transfer content section-by-section into PowerPoint.
> Presenter attribution: **[PUGAL]** = Pugalenthi K | **[BHARATH]** = RK Bharath

---

## Slide 1 — Title Slide

**Project Title:**
TrustABAC-IoT: Adaptive Trust- and Risk-Aware Smart-Contract
Access Control for Resource-Constrained IoT Networks

**Team:**
- 24MIC0082 — Pugalenthi K
- 24MIC0025 — RK Bharath

**Review**: Review 2 — Modules Explained, Executed & Demonstrated
**Date**: September 2026

---

## Slide 2 — Agenda

1. Problem Statement & Research Gap
2. System Architecture Overview
3. Module 1 — ABAC Gate & Adaptive Authorization Pipeline *(Pugalenthi)*
4. Module 2 — Solidity Smart Contract Decision Engine *(Pugalenthi)*
5. Module 3 — IoT Simulator & Device State Engine *(Bharath)*
6. Module 4 — RabbitMQ Event Pipeline & Monitoring *(Bharath)*
7. Module 5 — Real-Time Dashboard & WebSocket Telemetry *(Bharath)*
8. Module 6 — Offline Analytical Auditing — Spring Batch *(Pugalenthi)*
9. Experimental Results & Key Findings
10. Live Demonstration
11. Conclusion & Individual Contributions

---

## Slide 3 — Problem Statement [PUGAL]

### The IoT Access Control Challenge

**Problem:** Static access control models (RBAC, pure ABAC) are insufficient for dynamic IoT smart environments.

**Why RBAC Fails:**
- Role explosion in multi-tenant short-stay environments
- Cannot reason about time-bounded reservations

**Why Pure ABAC Fails:**
- Binary outcomes only: ALLOW or DENY
- A guest with a valid room key gets ALLOW even while behaving maliciously
- No response to degraded trust or elevated contextual risk

**Research Gap:**
- No fully integrated, empirically evaluated reference implementation combining:
  - Adaptive behavioral trust
  - Contextual risk scoring
  - Blockchain-backed immutable decision authority

---

## Slide 4 — Our Proposed Solution [PUGAL]

### TrustABAC-IoT: A Three-Tier Adaptive Decision Engine

```
ABAC Gate (Attribute Check)
        ↓
Trust Score (Historical Behavior) + Risk Score (Contextual Environment)
        ↓
Smart Contract on Blockchain EVM
        ↓
Tri-State: ALLOW → EXECUTED | RESTRICT → DOWNGRADED | DENY → BLOCKED
```

**Key Innovations:**
- Tri-state enforcement (not just Allow/Deny)
- Tamper-proof decision authority on Ethereum EVM (Ganache testbed)
- Fail-closed safety: blockchain outage → automatic DENY in 0.1 ms

---

## Slide 5 — System Architecture [PUGAL + BHARATH]

### Gateway-Assisted Multi-Tier Architecture

```
[IoT Client / Tenant App]
         │ REST HTTPS
         ▼
[Spring Boot Edge Gateway — Port 8090]
    │
    ├─ ABAC Gate (AbacService)        [PUGAL]
    ├─ Trust Engine (TrustService)    [PUGAL]
    ├─ Risk Engine (RiskService)      [PUGAL]
    ├─ Decision Coordinator → Ganache EVM :8545  [PUGAL]
    ├─ Resource Enforcement           [PUGAL]
    │
    ├─ [BHARATH] RabbitMQ AMQP Publisher
    ├─ [BHARATH] WebSocket STOMP Broker
    └─ Spring Batch Offline Auditor   [PUGAL]
```

**Technology Stack:**
Java 21 · Spring Boot 4.1.1 · Solidity 0.8.20 · Ganache v7 · MySQL 8 · RabbitMQ 3.13

---

## Slide 6 — Module 1: ABAC Gate & Authorization Pipeline [PUGAL]

### Contributor: Pugalenthi K (24MIC0082)

**Gate 1 — AbacService Checks (Pre-EVM):**
1. Is subject registered and active?
2. Does subject have a valid, temporally active booking?
3. Is the requested operation mapped to this resource type?
4. Is the device registered and online?

→ ABAC failure → immediate DENY/BLOCKED → **zero blockchain gas consumed**

**Short-Circuit Benefit:**
- ABAC Failure latency: **34.8 ms** vs **61.2 ms** full on-chain path
- **43% faster** — no trust calc, no risk calc, no EVM call

**Trust Score Formula:**
```
T_new = max(0.0, T_current − Δ_penalty)    [on violation]
T_new = min(100.0, T_current + Δ_recovery) [on recovery]
```

**Risk Score Formula:**
```
R = 0.40·S_resource + 0.25·N_network + 0.15·T_time + 0.20·B_burst
```

---

## Slide 7 — Module 2: Smart Contract Decision Engine [PUGAL]

### Contributor: Pugalenthi K (24MIC0082)
**File:** `contracts/AdaptiveAccessControl.sol` | Deployed: Ganache EVM (Chain ID 1337)

### Decision Matrix (7 Rules — On-Chain)

| ABAC? | Booking? | Trust T | Risk R | Decision | Code |
|:---:|:---:|:---:|:---:|:---:|:---:|
| ✗ | Any | Any | Any | **DENY** | 1 |
| ✓ | ✗ | Any | Any | **DENY** | 2 |
| ✓ | ✓ | T < 30 | Any | **DENY** | 3 |
| ✓ | ✓ | Any | R > 70 | **DENY** | 4 |
| ✓ | ✓ | T ≥ 70 | R ≤ 30 | **ALLOW** | 0 |
| ✓ | ✓ | 30≤T<70 | R ≤ 70 | **RESTRICT** | 5 |
| ✓ | ✓ | T ≥ 70 | 30<R≤70 | **RESTRICT** | 5 |

**Properties:**
- Emits `AuthorizationEvaluated` event on-chain → immutable audit receipt
- Deterministic **31,863 gas** per on-chain transaction
- RPC failure → Fail-Closed DENY in **0.1 ms**

---

## Slide 8 — Module 3: IoT Simulator & Device State Engine [BHARATH]

### Contributor: RK Bharath (24MIC0025)

**10 Simulated Devices (In-Memory, Smart Apartment Scenario):**

| Device | Resource Type | Sensitivity |
|:---|:---|:---:|
| DOOR-SENSOR-001 | SMART_DOOR_LOCK | CRITICAL |
| THERM-001 | SMART_THERMOSTAT | MEDIUM |
| CAM-001 | SECURITY_CAMERA | CRITICAL |
| LIGHT-001 | SMART_LIGHT | LOW |
| TV-001 | SMART_TV | LOW |
| ROUTER-001 | NETWORK_ROUTER | HIGH |

**Tri-State Enforcement Examples:**
- Door Lock: ALLOW→UNLOCKED | RESTRICT→LOCKED+status-only | DENY→no change
- Thermostat: ALLOW→set temp | RESTRICT→clamped 20–24°C | DENY→no change

**Key Endpoints:**
- `POST /api/simulator/scenarios/{SCENARIO}/run`
- `POST /api/simulator/reset`

---

## Slide 9 — Module 4: RabbitMQ Event Pipeline [BHARATH]

### Contributor: RK Bharath (24MIC0025)

**Why an Event Pipeline?**
- Decouples authorization latency from downstream logging and analytics

```
ResourceOperationService
        │ AMQP publish
        ▼
Exchange: trustabac.events (Topic Exchange)
        ├─► trustabac.device.events        [Device state logging]
        ├─► trustabac.trust.events         [Trust updates]
        ├─► trustabac.risk.events          [Risk anomaly records]
        ├─► trustabac.authorization.events [Audit trail]
        └─► trustabac.dlq                  [Dead-letter queue]
```

**Key Design Points:**
- `IdempotencyGuard` → message UUID tracking → at-most-once processing
- Consumers are **strictly observational** — cannot alter in-flight decisions
- RabbitMQ Management UI: `http://localhost:15672`

---

## Slide 10 — Module 5: Real-Time Dashboard & WebSocket Telemetry [BHARATH]

### Contributor: RK Bharath (24MIC0025)

**What it shows:**
- Live device state tiles for 10 IoT devices
- Real-time authorization event feed with decision badges
- Trust/Risk score visualization per request
- Enforcement status: EXECUTED / DOWNGRADED / BLOCKED

**Technology:**
- Spring WebSocket STOMP Broker → `/ws`
- Topics: `/topic/device-updates`, `/topic/authorization-events`
- Frontend: HTML5 + CSS3 + Vanilla JavaScript (SockJS)

**Critical Design Constraint:**
> Dashboard is **OBSERVATIONAL ONLY**.
> Zero authorization logic, zero private keys embedded.
> All values reflect authoritative backend state only.

**Access:** `http://localhost:8090/dashboard`

---

## Slide 11 — Module 6: Spring Batch Offline Auditing [PUGAL]

### Contributor: Pugalenthi K (24MIC0082)

**Purpose:** Periodic, reproducible, idempotent audit of persisted access history

```
MySQL: access_requests
        │
        ▼ Chunk-Oriented Spring Batch
[ItemReader] → [ItemProcessor] → [ItemWriter]
        │
        ▼
batch_run_audit table
(counts ALLOW/RESTRICT/DENY + blockchain hash reconciliation)
```

**Idempotency:**
- Keyed on `(periodKey, periodStart, periodEnd)`
- Re-running same period → `alreadyProcessed: true`, zero duplicates

**Endpoints:**
- `POST /api/batch/audit/trigger?periodKey=AUDIT_01`
- `GET /api/batch/audit/reports/latest`

---

## Slide 12 — Experimental Design [PUGAL]

### Phase 8B Controlled Experiment Setup

**Dataset:** 720 measured requests | 8 scenarios × 30 req/run × 3 repetitions
**Method:** Student's t CI₉₅ = mean ± t(0.975, N−1) × SE

| # | Scenario | Behavior Under Test |
|:--|:---|:---|
| 1 | NORMAL_ACCESS | Valid booking, high trust, low risk → ALLOW |
| 2 | RESTRICT_ACCESS | Moderate conditions → RESTRICT/DOWNGRADE |
| 3 | LOW_TRUST | Trust < 30 → DENY |
| 4 | HIGH_RISK | Risk > 70 → Pre-EVM gateway DENY |
| 5 | ABAC_FAILURE | Invalid booking → Gate 1 DENY |
| 6 | MIXED_SECURITY_WORKLOAD | Stochastic mixed → mixed decisions |
| 7 | BLOCKCHAIN_OUTAGE | Ganache offline → Fail-Closed DENY |
| 8 | RECOVERY | Post-reconnect → ALLOW restored |

**Controls:** State reset verified before every repetition. Anti-double-authorization enforced.

---

## Slide 13 — Results: Authorization Latency [PUGAL]

### Per-Scenario Latency with 95% Confidence Intervals

| Scenario | Auth Mean (ms) | Auth CI₉₅ (ms) | Enforce Mean (ms) |
|:---|:---:|:---:|:---:|
| NORMAL_ACCESS | 61.2 ± 2.3 | [56.7, 65.7] | 72.0 |
| RESTRICT_ACCESS | 56.0 ± 0.9 | [54.2, 57.8] | 65.9 |
| LOW_TRUST | 59.4 ± 3.6 | [52.2, 66.7] | 69.9 |
| HIGH_RISK | 44.0 ± 0.8 | [42.3, 45.6] | 51.7 |
| **ABAC_FAILURE** | **34.8 ± 0.7** | **[33.4, 36.2]** | 40.9 |
| MIXED_WORKLOAD | 52.5 ± 2.5 | [47.4, 57.5] | 61.7 |
| **BLOCKCHAIN_OUTAGE** | **0.100** | **[0.100, 0.100]** | 0.002 |
| RECOVERY | 41.3 ± 0.8 | [39.6, 43.0] | 48.6 |

**Key Insights:**
- ABAC short-circuit = **43% faster** than full on-chain path
- Fail-closed outage response = **0.1 ms** → no blocking or timeout cascade

---

## Slide 14 — Results: Policy Correctness [PUGAL]

### 100% Behavioral Correctness — 720 Measured Requests

| Scenario | N | ALLOW / RESTRICT / DENY | Match |
|:---|:---:|:---:|:---:|
| NORMAL_ACCESS | 90 | 90 / 0 / 0 | ✅ 100% |
| RESTRICT_ACCESS | 90 | 0 / 90 / 0 | ✅ 100% |
| LOW_TRUST | 90 | 0 / 0 / 90 | ✅ 100% |
| HIGH_RISK | 90 | 0 / 0 / 90 | ✅ 100% |
| ABAC_FAILURE | 90 | 0 / 0 / 90 | ✅ 100% |
| MIXED_WORKLOAD | 90 | 41 / 20 / 29 | ✅ 100% |
| BLOCKCHAIN_OUTAGE | 90 | 0 / 0 / 90 | ✅ 100% |
| RECOVERY | 90 | 90 / 0 / 0 | ✅ 100% |
| **POOLED** | **720** | **221 / 110 / 389** | **✅ 100%** |

**Zero policy violations across all 720 requests.**

---

## Slide 15 — Results: Mode Comparison [PUGAL]

### TrustABAC-IoT vs Pure ABAC — Policy Agreement

| Scenario | Pure ABAC | TrustABAC (Mode C) |
|:---|:---:|:---:|
| NORMAL_ACCESS | 100% ✅ | 100% ✅ |
| RESTRICT_ACCESS | **0%** ❌ | 100% ✅ |
| LOW_TRUST | **0%** ❌ | 100% ✅ |
| HIGH_RISK | **0%** ❌ | 100% ✅ |
| BLOCKCHAIN_OUTAGE | **0%** ❌ | 100% ✅ |
| **POOLED (720)** | **43.2%** ❌ | **100%** ✅ |

**Pure ABAC would have incorrectly ALLOWED 56.8% of security-critical requests.**
(Granted ALLOW because a valid booking token existed — ignoring degraded trust and elevated risk)

---

## Slide 16 — Results: Blockchain Gas [PUGAL]

### On-Chain Gas Consumption

| Scenario | On-Chain Txs | Gas/Tx | Total Gas |
|:---|:---:|:---:|:---:|
| NORMAL_ACCESS | 90 | 31,863 | 2,867,670 |
| RESTRICT_ACCESS | 90 | 31,863 | 2,867,670 |
| HIGH_RISK | **0** (pre-EVM filtered) | 0 | 0 |
| ABAC_FAILURE | **0** (Gate 1 filtered) | 0 | 0 |
| BLOCKCHAIN_OUTAGE | **0** (fail-closed) | 0 | 0 |
| **POOLED** | **421** | **31,863** | **13,414,323** |

**Key Findings:**
- Every authorized evaluation = exact **31,863 gas** (deterministic bytecode path)
- 299/720 requests rejected pre-EVM → **zero wasted gas** for denied requests
- Gateway short-circuiting eliminates 41.5% unnecessary EVM invocations

---

## Slide 17 — Security Properties Demonstrated [PUGAL]

### All Security Invariants Verified

| Property | Mechanism | Verified |
|:---|:---|:---:|
| Fail-Closed Outage Safety | DecisionCoordinator exception handler → 0.1 ms DENY | ✅ |
| Anti-Double Authorization | 1 request → 1 blockchain tx | ✅ |
| ABAC Gate Pre-filter | AbacService short-circuit | ✅ 43% latency saving |
| High-Risk Gateway Block | RiskService R > 70 pre-EVM check → 0 gas | ✅ |
| Privilege Attenuation | ResourceOperationService RESTRICT clamping | ✅ |
| Non-Repudiation | `AuthorizationEvaluated` on-chain immutable event | ✅ |
| Idempotent Audit | Spring Batch period-key lock | ✅ |

---

## Slide 18 — Individual Contributions [PUGAL + BHARATH]

### Pugalenthi K (24MIC0082) — Adaptive Authorization & Smart-Contract Enforcement

| Module | File / Component |
|:---|:---|
| ABAC Gate (Gate 1) | `AbacService.java` |
| Trust Score Engine | `TrustService.java` + `trust_history` MySQL table |
| Risk Score Engine | `RiskService.java` (4-factor weighted formula) |
| Smart Contract | `AdaptiveAccessControl.sol` (7-rule EVM matrix) |
| Decision Orchestration | `DecisionCoordinator.java` + Fail-Closed handler |
| Resource Enforcement | `ResourceOperationService.java` (tri-state actuator) |
| Offline Auditing | Spring Batch idempotent reconciliation |
| Experimental Evaluation | Phase 8B design + Phase 8C statistical analysis |

### RK Bharath (24MIC0025) — IoT Simulation, Event Pipeline & Monitoring

| Module | File / Component |
|:---|:---|
| IoT Device Engine | 10-device in-memory state engine |
| RabbitMQ Pipeline | Topic exchange, 5 queues, IdempotencyGuard, DLQ |
| Event Consumers | Trust/Risk/Audit consumers |
| WebSocket Streaming | STOMP broker + SockJS frontend |
| Real-Time Dashboard | Device tiles, authorization event log |
| Demo Execution | Live scenario runner, infrastructure health checks |

---

## Slide 19 — Live Demo Sequence [BHARATH]

### 10-Minute Live Demonstration Plan

| Step | Action | Endpoint / Command |
|:--|:---|:---|
| 1 | Verify infrastructure | `docker ps` → MySQL + RabbitMQ + Ganache |
| 2 | Launch gateway | `start-app.bat` |
| 3 | Health check | `GET /api/health` |
| 4 | Open dashboard | `http://localhost:8090/dashboard` |
| 5 | Run NORMAL_ACCESS | `POST /api/simulator/scenarios/NORMAL_STAY/run` |
| 6 | Show ALLOW + tx hash | Dashboard event log |
| 7 | Run RESTRICT scenario | `POST /api/simulator/scenarios/RESTRICT_ENFORCEMENT/run` |
| 8 | Run LOW_TRUST attack | `POST /api/simulator/scenarios/LOW_TRUST_ATTACK/run` |
| 9 | Simulate outage | `docker pause ganache-trustabac` |
| 10 | Show Fail-Closed DENY | Submit request → 0.1 ms DENY |
| 11 | Restore Ganache | `docker unpause ganache-trustabac` |
| 12 | Run Batch Audit | `POST /api/batch/audit/trigger?periodKey=DEMO_01` |
| 13 | Reset | `POST /api/simulator/reset` |

---

## Slide 20 — Scope & Limitations [PUGAL]

### Honest Research Boundaries

| Limitation | Detail |
|:---|:---|
| Local EVM testnet only | Ganache (Chain ID 1337), single-node instant mining |
| Simulated IoT devices | 10 in-memory models; no physical hardware energy measurement |
| Sequential benchmark | Per-request sequential latency; max concurrency not benchmarked |
| Trusted gateway oracle | Gateway computes and forwards attributes to contract |
| Single-site deployment | Smart apartment scenario only |

---

## Slide 21 — Conclusion [PUGAL + BHARATH]

### What TrustABAC-IoT Delivers

✅ 6 integrated subsystems — fully working end-to-end
✅ 720 measured requests — **100% behavioral correctness**
✅ Tri-state enforcement — ALLOW / RESTRICT / DENY with privilege clamping
✅ Fail-closed outage safety — **0.1 ms** rejection under blockchain failure
✅ 43% ABAC short-circuit saving — unnecessary EVM calls eliminated
✅ Deterministic **31,863 gas** — predictable on-chain execution cost
✅ Pure ABAC correctly handles only **43.2%** of security scenarios vs TrustABAC's **100%**

> **Conclusion:** Adding behavioral trust and contextual risk layers to ABAC, backed by
> blockchain-enforced decision authority, measurably improves security correctness
> at a predictable and acceptable latency cost for IoT smart environments.

---

## Slide 22 — Thank You & Q&A

**TrustABAC-IoT**
Adaptive Trust- and Risk-Aware Smart-Contract Access Control
for Resource-Constrained IoT Networks

| Member | Roll No. | Contribution |
|:---|:---:|:---|
| Pugalenthi K | 24MIC0082 | Adaptive Authorization & Smart-Contract Enforcement |
| RK Bharath | 24MIC0025 | IoT Simulation, Event Pipeline & Monitoring |

**Demo:** `http://localhost:8090/dashboard`
**Health:** `http://localhost:8090/api/health`

*All empirical results from frozen Phase 8B dataset — 720 requests, 8 scenarios, 3 repetitions.*
*No data fabricated. Smart-contract thresholds unchanged from implementation.*
