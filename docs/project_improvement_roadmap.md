# TrustABAC-IoT — Development Improvement Roadmap

> **Status as of**: v1.0.0 (2026-09-24)
> **Source of this document**: Repository inspection + identified inconsistencies and technical debt
> **Rule**: This roadmap does NOT modify any existing code, configuration, or data. It is a planning artifact only.

---

## Roadmap Overview

```
v1.0.0  ──────── Current Baseline (research prototype complete)
   │
   ├── v1.1.x ── Documentation & Critical Bug Fixes
   │
   ├── v1.2.x ── Security, Robustness, API Hardening
   │
   ├── v1.3.x ── Research Extensions & Scalability
   │
   └── v2.0.0 ── Future Research Release
```

---

## v1.0.0 — Current Baseline

**Status**: ✅ COMPLETE (tagged, pushed, clean working tree)

**Scope**: Full 10-phase research prototype including:
- 4-gate authorization pipeline (ABAC → Trust → Risk → Blockchain)
- `AdaptiveAccessControl.sol` smart contract (Ganache EVM)
- 720-sample controlled experimental dataset (frozen, SHA-256 verified)
- 88 Java source files, 48 test files, 18 Python scripts
- 12 thesis chapters, viva Q&A, demo scripts
- Docker infrastructure (MySQL, RabbitMQ, Ganache)
- Real-time WebSocket dashboard

---

## v1.1.0 — Stabilization & Consistency (Completed)

**Goal**: Remediate all 7 identified inconsistencies and critical/high technical debt items without changing experimental results or research claims.

### Resolved in v1.1.0:

1. **Documentation Corrections**:
   - `docs/submission_checklist.md`: Spring Boot 3.2.3 -> 4.1.1
   - `docs/thesis/06_implementation.md`: Spring Boot 3.2.3 -> 4.1.1, Solidity ^0.8.19 -> ^0.8.20
   - `contracts/phase8c_analysis/thesis_tables.md` Table 1: Spring Boot 3.2.3 -> 4.1.1, Solidity ^0.8.19 -> ^0.8.20
   - `contracts/analyze_phase8c.py`: Spring Boot 3.2.3 -> 4.1.1, Solidity ^0.8.19 -> ^0.8.20
   - `docs/final_project_status.md`: 282 Files Scanned -> 302 Files Scanned (matching `security_scan_final.md`)
   - `docs/Review_2_TrustABAC_IoT_Slides.md`: Updated to Spring Boot 4.1.1 & Solidity 0.8.20

2. **Configuration Alignment**:
   - `src/main/resources/application.properties`: Standardized `server.port=8090` (matching `.env.example`, `START_DEMO.bat`, and `start-demo.ps1`).
   - `docker-compose.yml`: Standardized default host MySQL port to `"${DB_PORT:-3307}:3306"`.
   - `src/main/resources/application.properties`: Added explicit documentation comment on the deliberate 00:00–24:00 time-risk experimental configuration.

3. **Infrastructure & Script Corrections**:
   - `docker-compose.yml`: Removed unused `trustabac-iot` application build service (Docker Compose manages infrastructure only; Spring Boot runs natively via `START_DEMO.bat`).
   - `contracts/*.py` & demo runbooks: Corrected stale container references `releasemind-mysql` -> `trustabac-mysql` and `ganache-trustabac` -> `trustabac-ganache`.

4. **Research Baseline Preserved**:
   - Frozen Phase 8B datasets (`experiment_results_phase8b.json` and `experiment_results_phase8b.csv`) preserved byte-for-byte with exact SHA-256 hashes verified.

---

---

## v1.2.x — Security, Robustness, and API Hardening

**Goal**: Improve operational security, eliminate fragile heuristics, and harden the messaging pipeline.

### v1.2.0 — Booking Active Logic Fix

#### [MODIFY] `service/DecisionCoordinator.java` — `isBookingActive()` method
- **Current**: Determines booking active status by checking if ABAC reason string contains "booking" (string heuristic)
- **Fix**: Introduce an explicit boolean field in `AccessEvaluationResponse` (e.g., `bookingValid`) and set it in `AbacService`
- **Risk**: MEDIUM — requires coordination between AbacService and DecisionCoordinator; affects all authorization flows
- **Affected files**: `DecisionCoordinator.java`, `AbacService.java`, `AccessEvaluationResponse.java`
- **Validation**: `DecisionCoordinatorTest.java`, `AbacServiceTest.java`, integration tests

### v1.2.1 — Persistent Idempotency

#### [NEW] `messaging/consumer/PersistentIdempotencyStore.java`
#### [MODIFY] `messaging/consumer/IdempotencyGuard.java`
- **Current**: In-memory `ConcurrentHashMap` for seen message IDs; lost on JVM restart
- **Fix**: Add a database-backed `processed_messages` table with (messageId, processedAt) and a TTL cleanup job
- **Risk**: MEDIUM — requires new JPA entity, migration, and updating all consumers
- **Affected modules**: All 5 RabbitMQ consumers
- **Validation**: `IdempotencyGuardTest.java`, restart resilience tests

### v1.2.2 — Replay Protection

#### [MODIFY] `controller/ResourceOperationController.java`, `AuthorizationController.java`
- **Current**: No timestamp validation on incoming requests; replay attacks possible
- **Fix**: Add a `requestTimestamp` to `BlockchainAuthorizationRequest` and reject requests older than a configurable window (e.g., 60 seconds)
- **Risk**: LOW — defensive addition; requires timestamp on all clients
- **Validation**: New replay protection unit tests

### v1.2.3 — Configurable Enforcement Rules

#### [NEW] `config/EnforcementRulesProperties.java`
#### [MODIFY] `service/ResourceOperationService.java`
- **Current**: Device-type enforcement logic hardcoded in Java switch statements (DOOR, THERMOSTAT, LIGHT, etc.)
- **Fix**: Externalize to `trustabac.enforcement.*` properties (e.g., allowed resource types, clamping bounds per type)
- **Risk**: LOW — refactoring only; should not change behavior
- **Validation**: `ResourceOperationServiceTest.java`

### v1.2.4 — REST API Authentication

#### [MODIFY] `config/SecurityConfig.java`
- **Current**: Stateless REST security; unclear what endpoints are protected for demo vs production
- **Fix**: Add API key or JWT bearer token requirement for sensitive write endpoints (`/api/trust/*/event`, `/api/blockchain/*`, `/api/simulator/*`)
- **Risk**: HIGH — breaking change for all Python integration test scripts that currently call unauthenticated
- **Required**: Update all Python scripts to pass credentials; update `.env.example` with API key config
- **Validation**: All phase 7 Python test suites, smoke test

---

## v1.3.x — Research Extensions & Scalability

**Goal**: Extend the experimental framework, add concurrent load benchmarking, and optionally re-run experiments with corrected time-risk configuration.

### v1.3.0 — Concurrent Load Testing

#### [NEW] `contracts/experiment_phase9a_concurrent.py` (or JMeter plan)
- **Current**: Only sequential benchmarking (1 request at a time per scenario)
- **Fix**: Add concurrent request dispatching (e.g., 10, 25, 50 concurrent threads) to measure saturation throughput
- **Risk**: Zero (new scripts, frozen dataset untouched)
- **Expected output**: Saturated ops/sec, 95th percentile under load, error rate

### v1.3.1 — Time Risk Re-Evaluation

#### [MODIFY] `src/main/resources/application.properties`
#### [NEW] `contracts/experiment_phase9b_timerisk.py`
- **Current**: Time risk disabled (0–24 hour window)
- **Fix**: Enable time risk (6–22 window), re-run a controlled experiment to document the impact
- **Risk**: MEDIUM — new dataset, does not touch frozen Phase 8B dataset
- **Note**: Phase 8B frozen dataset MUST NOT be altered

### v1.3.2 — Additional Security Scenarios

#### [NEW] `contracts/experiment_phase9c_adversarial.py`
- **Scenarios**: Request flooding (rapid burst), location spoofing, expired token replay
- **Goal**: Validate trust degradation and risk escalation under simulated attack conditions

### v1.3.3 — Multi-Device Dataset

- **Current**: Most scenarios use `DOOR-SENSOR-001` as primary device
- **Fix**: Extend to evaluate all 9 simulated device types across authorization scenarios
- **Risk**: Low (experiment extension only)

---

## v2.0.0 — Future Research Release

**Goal**: Address fundamental simulation-vs-reality gap and scale to distributed environments.

### v2.0.0-A — Physical IoT Hardware Integration

| Item | Description |
| :--- | :--- |
| **Target hardware** | ESP32 / Raspberry Pi 4 / Nordic nRF52840 |
| **Integration point** | Replace `SimulatedDeviceStateStore` with real device REST endpoints or MQTT bridge |
| **Enforcement validation** | Physically verify door lock state, thermostat setpoint, light control |
| **New metrics** | Physical actuation latency, hardware bus delay, real-time energy consumption (mJ) |
| **Affected modules** | `ResourceOperationService`, `SimulatedDeviceStateStore`, `SimulatorService` |
| **Risk** | HIGH — fundamental architectural change; requires hardware prototyping |
| **Validation required** | Physical device integration tests, energy measurements |

### v2.0.0-B — Public Testnet / L2 Evaluation

| Item | Description |
| :--- | :--- |
| **Target networks** | Ethereum Sepolia testnet, Polygon Mumbai, or Arbitrum Goerli |
| **Goal** | Measure real-world gas costs, block confirmation times (12s average on Ethereum L1), fee volatility |
| **Integration point** | `BlockchainProperties` RPC URL and chain ID configuration |
| **Affected modules** | `BlockchainService`, `BlockchainConfig` |
| **Risk** | LOW for testnet (no real ETH); MEDIUM for mainnet (gas fees, key management) |
| **Note** | Private key management must be strengthened (HSM or hardware wallet) for public networks |
| **Validation required** | Contract deployment on testnet, gas benchmark comparison vs Ganache |

### v2.0.0-C — Multi-Gateway Architecture

| Item | Description |
| :--- | :--- |
| **Goal** | Eliminate single-gateway trust assumption identified in thesis limitations |
| **Approach** | Multiple Spring Boot gateways with shared MySQL or distributed coordination |
| **Advanced option** | Decentralized oracle (e.g., Chainlink) for off-chain attribute collection |
| **Risk** | HIGH — fundamental architecture change |
| **Validation** | Consensus correctness tests, Byzantine fault tolerance tests |

### v2.0.0-D — Formal Security Verification

| Item | Description |
| :--- | :--- |
| **Goal** | Formal proof or model checking of `AdaptiveAccessControl.sol` decision matrix |
| **Tools** | Certora Prover, Slither, Mythril, or Isabelle/HOL |
| **Properties to verify** | Fail-safe denial on invalid inputs, monotonicity of decision w.r.t. trust/risk, no integer overflow |
| **Risk** | Low (analysis only, no code changes required) |

---

## Validation Requirements Per Version

| Version | Test Type | Required Before Release |
| :--- | :--- | :--- |
| v1.1.x | Maven tests, smoke test, manual doc review | `./mvnw test` + `python contracts/final_smoke_test.py` |
| v1.2.x | Maven tests, ALL Python integration suites, smoke test | All 10 Python suites passing |
| v1.3.x | Maven tests, new experiment scripts, validate frozen dataset unchanged | SHA-256 hash check on Phase 8B files |
| v2.0.0 | All of the above + hardware/network-specific validation | Physical device tests + testnet transaction logs |

---

## Documentation Requirements Per Version

| Version | Documentation Updates Required |
| :--- | :--- |
| v1.1.x | Update thesis, checklist, README with correct versions and port; add inconsistency notes |
| v1.2.x | Update architecture docs, API docs, Python script READMEs |
| v1.3.x | New experiment appendix/chapter; updated limitations section |
| v2.0.0 | Major thesis revision or new publication; hardware setup guide; deployment guide |

---

## Priority Summary

| Priority | Item | Version | Effort |
| :--- | :--- | :--- | :--- |
| 🔴 CRITICAL | Fix Spring Boot version in all docs | v1.1.0 | 30 min |
| 🔴 CRITICAL | Fix server port documentation/config | v1.1.1 | 1 hour |
| 🔴 CRITICAL | Fix container name in experiment script | v1.1.2 | 5 min |
| 🟠 HIGH | Add Dockerfile or remove from docker-compose | v1.1.2 | 2 hours |
| 🟠 HIGH | Fix Solidity version in thesis_tables | v1.1.0 | 15 min |
| 🟠 HIGH | Fix security scan file count discrepancy | v1.1.0 | 15 min |
| 🟠 HIGH | Fix bookingActive heuristic | v1.2.0 | 3 hours |
| 🟠 HIGH | Persistent idempotency | v1.2.1 | 1 day |
| 🟡 MEDIUM | Document time risk window as known config | v1.1.1 | 30 min |
| 🟡 MEDIUM | Configurable enforcement rules | v1.2.3 | 4 hours |
| 🟡 MEDIUM | Concurrent load testing | v1.3.0 | 1 day |
| 🟢 LOW | REST API authentication | v1.2.4 | 2 days |
| 🟢 LOW | Physical hardware integration | v2.0.0 | Weeks |
| 🟢 LOW | Public testnet evaluation | v2.0.0 | Days |
