# TrustABAC-IoT — Current Project State

> [!NOTE]
> **Source of Truth Rule Applied**: This document was produced by direct inspection of the repository source code, configuration files, experiment scripts, and result artifacts as of 2026-09-24. Where documentation and source code disagree, the discrepancy is explicitly flagged. Nothing was invented or assumed.

---

## 1. Project Identity

| Field | Value |
| :--- | :--- |
| **Project Title** | TrustABAC-IoT: Adaptive Trust- and Risk-Aware Smart-Contract Access Control for Resource-Constrained IoT Networks |
| **Course** | CSI3013 – Blockchain Technologies |
| **Team** | Pugalenthi |
| **GitHub** | https://github.com/PUGALENTHI-K-7/trustabac-iot |
| **Project Root** | `J:\PROJECT\TRUST -ABAC\trustabac-iot` |
| **Demo Scenario** | Smart Short-Term Rental Property (Airbnb-style concept — no real Airbnb integration) |

---

## 2. Git / Version State

| Property | Value |
| :--- | :--- |
| **Branch** | `main` |
| **HEAD Commit** | `3c10485` |
| **Commit Message** | `Initial Review 2 project snapshot` |
| **Tag** | `v1.0.0` (points to commit `3c104859b604cbfa3acc0eef135d7c1672b37f83`) |
| **Remote** | `origin https://github.com/PUGALENTHI-K-7/trustabac-iot.git` |
| **Working Tree** | CLEAN — `nothing to commit, working tree clean` |
| **Branch Sync** | `Your branch is up to date with 'origin/main'.` |

> [!IMPORTANT]
> `v1.0.0` is the only tag. It is a historical baseline milestone. It must NOT be rewritten, amended, or force-pushed over. Future commits belong on `main` as new commits after this baseline.

---

## 3. Executive Summary

TrustABAC-IoT is a fully implemented, experimentally evaluated research prototype implementing a **four-gate adaptive authorization pipeline** for IoT access control. The system combines:

1. ABAC structural eligibility (Gate 1)
2. Behavioral Trust scoring (Gate 2)
3. Contextual Risk aggregation (Gate 3)
4. Ethereum smart contract decision enforcement (Gate 4)

All 10 development phases (Phases 1–10) are marked complete. A controlled experiment campaign collected 720 measured authorization requests across 8 security scenario archetypes. The working tree is clean. There is exactly one commit and one tag in the repository (v1.0.0).

The implementation is **ready for thesis defense and demo** as declared by `docs/final_project_status.md`.

---

## 4. Current Architecture

The actual authorization call-flow verified from source code (`DecisionCoordinator.java`, `AbacService.java`, `ResourceOperationService.java`):

```
[Client / Simulator REST Request]
        |
        v
ResourceOperationController
        |
        v
ResourceOperationService.executeOperation()
        |
        v
DecisionCoordinator.evaluateAuthorization()
        |
        +--> Gate 1: AbacService.evaluateAccess()
        |        |- DeviceRepository (device existence, active, REGISTERED)
        |        |- BookingService (booking validity, location match)
        |        |- PolicyRepository (active policies matching resource/operation)
        |        |- PolicyEvaluator (attribute dictionary evaluation)
        |
        |    [ABAC FAIL → immediate DENY, no trust/risk/blockchain invoked]
        |    [ABAC PASS → continue pipeline]
        |
        +--> Gate 2: Trust lookup (Device.currentTrust from DB, no mutation)
        |
        +--> Gate 3: RiskService.evaluateRisk()
        |        |- 7 RiskFactorCalculators (Time, Location, Sensitivity,
        |           Frequency, Network, Violations, Behavior)
        |        |- Weighted composite score → RiskStatus (LOW/MEDIUM/HIGH)
        |        |- RiskEvent persisted (append-only)
        |
        +--> Gate 4: BlockchainService.evaluateAccessOnChain()
                 |- Web3j → Ganache RPC → AdaptiveAccessControl.evaluateAccess()
                 |- Returns: ALLOW(2) / RESTRICT(1) / DENY(0) + reasonCode
                 |- On-chain: AuthorizationEvaluated event emitted
                 |- Off-chain: BlockchainAuthorizationEvent persisted to MySQL

[Blockchain Unavailable → Fail-Closed DENY enforced]

        v
ResourceOperationService.enforceDecision()
        |- ALLOW   → EnforcementStatus.EXECUTED, full state mutation
        |- RESTRICT→ EnforcementStatus.DOWNGRADED (sensitivity-aware)
        |              HIGH/CRITICAL resource: READ only (no state mutation)
        |              LOW/MEDIUM resource: clamped-parameter execution
        |- DENY    → EnforcementStatus.BLOCKED, effectiveOperation="NONE"

        v
SimulatedDeviceStateStore (in-memory ConcurrentHashMap)
9 pre-seeded device states: DOOR-SENSOR-001, SMART_DOOR_LOCK, GUEST_WIFI,
SMART_TV, AIR_CONDITIONER, SMART_LIGHT, SMART_THERMOSTAT,
SECURITY_CAMERA, ROUTER, OWNER_SETTINGS

        v
IoTEventPublisher → RabbitMQ (trustabac.events exchange)
        |-→ trustabac.device.events (device operations)
        |-→ trustabac.trust.events (trust updates)
        |-→ trustabac.risk.events (risk evaluations)
        |-→ trustabac.authorization.events (authorization decisions)
        |-→ trustabac.dlq (dead-letter queue)

        v
WebSocketEventPublisher → STOMP /ws
        |-→ /topic/* topics for real-time dashboard

        v
DashboardController → /dashboard (Thymeleaf, dashboard.html)
```

**Authorization layer vs. transport/event layer are clearly separated.** The dashboard observes; it does not authorize.

---

## 5. Technology Stack

Verified from `pom.xml`, `application.properties`, `docker-compose.yml`, and `thesis_tables.md`:

| Component | Declared Version | Source |
| :--- | :--- | :--- |
| **Java** | 21 | `pom.xml` `<java.version>21</java.version>` |
| **Spring Boot** | **4.1.1** | `pom.xml` `<version>4.1.1</version>` |
| **Web3j** | 4.10.3 | `pom.xml` |
| **MySQL Connector** | runtime dependency | `pom.xml` (version managed by Spring Boot parent) |
| **H2** | test scope | `pom.xml` |
| **Spring Batch** | (managed by Spring Boot 4.1.1 parent) | `pom.xml` |
| **MySQL Docker** | 8.0 | `docker-compose.yml` |
| **Ganache Docker** | trufflesuite/ganache:latest (v7.9.2 observed at runtime) | `docker-compose.yml` / `thesis_tables.md` |
| **RabbitMQ Docker** | rabbitmq:3-management (3.13 observed at runtime) | `docker-compose.yml` / `thesis_tables.md` |
| **Solidity** | ^0.8.20 | `AdaptiveAccessControl.sol` line 2 |
| **Chain ID** | 1337 | `application.properties`, `docker-compose.yml` |
| **Ganache Port** | 8545 | `application.properties`, `docker-compose.yml` |
| **App Server Port** | 8080 (application.properties default), 8090 (.env.example, docker-compose) | ⚠ See Inconsistency #1 |
| **MySQL Port (Docker)** | 3308 (docker-compose) vs 3307 (.env.example) | ⚠ See Inconsistency #2 |
| **RabbitMQ Port** | 5672 (AMQP), 15672 (Management) | `application.properties`, `docker-compose.yml` |

> [!WARNING]
> **INCONSISTENCY #1 – Server Port**: `application.properties` sets `server.port=8080`. The `.env.example` and `docker-compose.yml` use port `8090`. The `start-demo.ps1` and `START_DEMO.bat` target `http://127.0.0.1:8090`. The thesis_tables.md reports `Spring Boot 3.2.3` not `4.1.1`. Full details in Section 24.

---

## 6. ABAC

**Source file**: [`AbacService.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/service/AbacService.java)

### ABAC Entities
- **Subject**: userId, role, organization, bookingId
- **Device**: deviceIdentifier, deviceType, deviceClass, registrationStatus, active flag
- **Resource**: resource identifier, resourceType, sensitivity (resolved via `ResourceRegistry`)
- **Operation**: READ, WRITE, UPDATE, DELETE, CONTROL (enum `Operation`)
- **Context**: location, requestTimestamp, networkContext, bookingValid, bookingStatus

### Policy Model
- Policies stored in MySQL `policy` table (entity: `Policy`)
- Each `Policy` has: name, targetResource, targetOperation, list of `PolicyCondition`
- `PolicyCondition`: attributeKey, operator (EQUALS, NOT_EQUALS, CONTAINS, EXISTS, NOT_EXISTS, BOOLEAN_TRUE, BOOLEAN_FALSE, IN), value

### Policy Evaluation Sequence (from source)
1. Device resolution: must exist, be active (`device.active = true`), be `REGISTERED`
2. Booking resolution: optional, checked for validity including location match
3. Attribute dictionary: 5-dimension map built from subject, device, resource, operation, context
4. Active policy fetch: all policies with `active = true`
5. Candidate filtering: policies matching `targetResource` AND `targetOperation`
6. Sequential evaluation: first passing candidate → PASS + policyName
7. All failing → aggregated reason → FAIL

### ABAC FAIL Behavior (verified from `DecisionCoordinator.java`)
- Returns `Decision.DENY` immediately
- NO Trust lookup
- NO Trust mutation
- NO Risk evaluation
- NO BlockchainService invocation
- NO `TrustHistory` record created
- `AccessRequest` record IS persisted with `FAIL` result

---

## 7. Trust

**Source file**: [`TrustService.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/service/TrustService.java), [`TrustProperties.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/config/TrustProperties.java)

### Trust Score Parameters (from `application.properties` + `TrustProperties.java`)
| Parameter | Value |
| :--- | :--- |
| Initial / Default Score | 80.0 |
| Minimum Score | 0.0 |
| Maximum Score | 100.0 |
| NORMAL_SUCCESS delta | +1.0 |
| SUSPICIOUS_ACTIVITY delta | −10.0 |
| REQUEST_FLOODING delta | −25.0 |
| CONFIRMED_MALICIOUS delta | −40.0 |
| RECOVERY delta | +10.0 |

### Trust Behavior (verified from source)
- Trust is stored on `Device.currentTrust` (mutable field)
- Updates are bounded/clamped to [0.0, 100.0]
- Every update creates an **append-only** `TrustHistory` record (never deleted/mutated)
- Event timestamp is strictly from the server-authoritative `Clock` bean
- Trust categories (monitoring-only, not decision logic):
  - `score >= 70.0` → TRUSTED
  - `score >= 40.0` → SUSPICIOUS
  - `score < 40.0` → UNTRUSTED
- **Trust is NOT mutated during the authorization pipeline** (AbacService Gate 2 is read-only)
- Trust updates happen via `TrustService.recordTrustEvent()` (explicit API call or simulator event)
- Trust and Risk are **orthogonal** (high risk does NOT degrade trust)

---

## 8. Risk

**Source file**: [`RiskService.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/service/RiskService.java), [`RiskProperties.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/config/RiskProperties.java)

### Risk Dimensions (7 factors, all verified in source)
| Factor | Weight | Source Class |
| :--- | :--- | :--- |
| Time | 10.0 | `TimeRiskCalculator` |
| Location | 15.0 | `LocationRiskCalculator` |
| Sensitivity | 20.0 | `SensitivityRiskCalculator` |
| Frequency | 20.0 | `FrequencyRiskCalculator` |
| Network | 15.0 | `NetworkRiskCalculator` |
| Violations | 10.0 | `ViolationRiskCalculator` |
| Behavior | 10.0 | `BehaviorRiskCalculator` |
| **Total** | **100.0** | Verified: sums to 100 |

### Risk Composite Formula
```
rawRisk = Σ(factor_i × weight_i)   [each factor normalized 0.0–1.0]
riskScore = clamp(rawRisk, 0.0, 100.0)
```

### Risk Thresholds (from `application.properties`)
| Threshold | Value |
| :--- | :--- |
| Low Threshold | 30.0 |
| Medium Threshold | 70.0 |
| LOW status | score ≤ 30.0 |
| MEDIUM status | 30.0 < score ≤ 70.0 |
| HIGH status | score > 70.0 |

> [!IMPORTANT]
> **DISCREPANCY NOTE**: `application.properties` sets `trustabac.risk.context.normal-start-hour=0` and `trustabac.risk.context.normal-end-hour=24`, which means ALL hours are "normal" (0 risk from time factor). The class default in `RiskProperties.Context` is `normalStartHour=6`, `normalEndHour=22`. The properties file overrides the class defaults. This means `TimeRiskCalculator` always returns 0.0 risk contribution in the current deployed configuration.

### Risk Persistence
- Append-only `RiskEvent` records persisted to MySQL
- Source of truth: Most recent `RiskEvent` per device returned by `getCurrentRisk()`
- Risk does NOT mutate trust

---

## 9. Blockchain / Smart Contract

**Source file**: [`AdaptiveAccessControl.sol`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/contracts/AdaptiveAccessControl.sol)

### Contract Functions
| Function | Type | Description |
| :--- | :--- | :--- |
| `constructor()` | state-changing | Sets owner, initializes thresholds (trustHigh=70, trustMedium=30, riskLow=30, riskMedium=70) |
| `evaluateAccess(...)` | `external` state-changing | Authoritative authorization transaction; emits `AuthorizationEvaluated` event |
| `calculateDecision(...)` | `public view` | Pure logic, no state change; used as fallback if event extraction fails |
| `setTrustThresholds(...)` | `external onlyOwner` | Updates trustHigh and trustMedium |
| `setRiskThresholds(...)` | `external onlyOwner` | Updates riskLow and riskMedium |
| `transferOwnership(...)` | `external onlyOwner` | Transfers ownership |

### Decision Matrix (Solidity, verified from source)
```
1. IF !abacPass          → DENY (reasonCode=1)
2. IF !bookingActive     → DENY (reasonCode=2)
3. IF trustScore < trustMedium(30) → DENY (reasonCode=3)
4. IF riskScore > riskMedium(70)   → DENY (reasonCode=4)
5. IF trustScore >= trustHigh(70) AND riskScore <= riskLow(30) → ALLOW (reasonCode=0)
6. Otherwise             → RESTRICT (reasonCode=5)
```

### Events
- `AuthorizationEvaluated(requestReference, deviceIdentifierHash, decision, trustScore, riskScore, resourceSensitivity, operation, timestamp)`
- `ThresholdsUpdated(trustHigh, trustMedium, riskLow, riskMedium)`
- `OwnershipTransferred(previousOwner, newOwner)`

### Deployment (runtime, from `thesis_tables.md`)
- Deployed address: `0xe78A0F7E598Cc8b0Bb87894B0F60dD2a88d6a8Ab` (Ganache runtime, not tracked in `.gitignore`-exempted files)
- Chain ID: 1337
- Gas per evaluation: exactly **31,863 gas** (constant — verified experimentally across 421 transactions)

> [!IMPORTANT]
> **AUTHORITATIVE DECISION**: The Solidity smart contract `evaluateAccess()` is the sole authoritative final decision engine. Java (`DecisionCoordinator`) passes compact inputs and reads back the on-chain result. Java does NOT independently re-implement the decision logic for the authorization path.

---

## 10. Web3j Integration

**Source file**: [`BlockchainService.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/service/blockchain/BlockchainService.java), [`BlockchainConfig.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/config/BlockchainConfig.java), [`BlockchainProperties.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/config/BlockchainProperties.java)

- Web3j 4.10.3 is the Java Ethereum client library
- `AdaptiveAccessControl.java` is the generated contract wrapper
- `evaluateAccessOnChain()` sends a state-changing transaction (not a view call) to produce on-chain events
- `deviceIdentifierHash` = SHA-3 hash of device identifier string
- `requestReference` = padded/hashed bytes32 from request UUID
- Fail-closed: `BlockchainUnavailableException` → `Decision.DENY` enforced by `DecisionCoordinator`
- RPC URL default: `http://127.0.0.1:8545`
- Gas limit: 3,000,000 | Gas price: 20,000,000,000 wei (20 Gwei)

---

## 11. Device Enforcement

**Source file**: [`ResourceOperationService.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/service/ResourceOperationService.java), [`SimulatedDeviceStateStore.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/service/enforcement/SimulatedDeviceStateStore.java)

### Enforcement Semantics (verified from source)

| Decision | EnforcementStatus | effectiveOperation | State Mutation |
| :--- | :--- | :--- | :--- |
| ALLOW | EXECUTED | requested op | Full state change applied |
| RESTRICT (HIGH/CRITICAL resource) | DOWNGRADED | READ | NO state mutation (read-only) |
| RESTRICT (LOW/MEDIUM resource) | DOWNGRADED | requested op | Clamped/safe execution |
| DENY | BLOCKED | NONE | Zero state mutation |

### Simulated Device States (in-memory, pre-seeded)
| State Key | Device |
| :--- | :--- |
| `DOOR-SENSOR-001` / `SMART_DOOR_LOCK` | Smart Door Lock (lockState, batteryLevel, tamperAlarm) |
| `GUEST_WIFI` | Wi-Fi Controller (ssid, enabled, bandwidthTier, clientCount) |
| `SMART_TV` | Smart TV (power, input, volume) |
| `AIR_CONDITIONER` | AC (power, mode, targetTempCelsius, fanSpeed) |
| `SMART_LIGHT` | Lights (power, brightnessPct, colorTemp) |
| `SMART_THERMOSTAT` | Thermostat (currentTempCelsius, targetTempCelsius, ecoMode) |
| `SECURITY_CAMERA` | Security Camera (status, recording, privacyMode) |
| `ROUTER` | Router (status, firewall, adminAccess) |
| `OWNER_SETTINGS` | Owner Settings (propertyStatus, billingCycle) |

> [!IMPORTANT]
> All device states are **software-simulated** (in-memory ConcurrentHashMap). No physical hardware, microcontrollers, or physical actuators are present. The `SimulatedDeviceStateStore` Javadoc itself states: "without claiming physical hardware control."

---

## 12. IoT Simulator

**Source files**: `SimulatorService.java` (in `src/main/java/com/trustabac/iot/simulator/`), `test_phase7b_simulator.py`

The IoT simulator is a Spring Boot service that drives synthetic authorization scenarios for testing and demonstration. From `experiment_phase8b.py` and `docs/final_project_status.md`:

### 8 Scenario Archetypes
1. `NORMAL_ACCESS` — High trust (80), low risk, valid booking → expected ALLOW
2. `RESTRICT_ACCESS` — High trust (80), moderate risk (cellular/off-peak ~36.3) → expected RESTRICT
3. `LOW_TRUST` — Degraded trust (20), low risk → expected on-chain DENY
4. `HIGH_RISK` — High trust (80), risk >70 → expected gateway DENY (pre-blockchain)
5. `ABAC_FAILURE` — Missing/expired booking → expected Gate 1 DENY
6. `MIXED_SECURITY_WORKLOAD` — Stochastic blend of all above
7. `BLOCKCHAIN_OUTAGE` — Simulated EVM unavailability → fail-closed DENY
8. `RECOVERY` — Post-outage reconnection → expected ALLOW

### Experimental Configuration (from `experiment_phase8b.py`)
| Parameter | Value |
| :--- | :--- |
| Warm-up operations per scenario/repetition | 10 |
| Measured operations per scenario/repetition | 30 |
| Repetitions per scenario | 3 |
| Total scenarios | 8 |
| Total warm-up samples | 240 (10 × 8 × 3) |
| Total measured samples | 720 (30 × 8 × 3) |
| Seed formula | `seed = r * 1000 + s * 100 + 42` (starting at 1042 for r=1, s=0) |

---

## 13. RabbitMQ

**Source**: `RabbitMqConfig.java`, `application.properties`, `IoTEventPublisher.java`, various consumer classes

### Exchange and Queues (verified from `application.properties`)
| Name | Value |
| :--- | :--- |
| Exchange | `trustabac.events` |
| Device event queue | `trustabac.device.events` (routing key: `device.operation`) |
| Trust event queue | `trustabac.trust.events` (routing key: `trust.event`) |
| Risk event queue | `trustabac.risk.events` (routing key: `risk.context`) |
| Authorization queue | `trustabac.authorization.events` (routing key: `authorization.result`) |
| Dead-letter queue | `trustabac.dlq` (routing key: `dlq.events`) |

### Idempotency
- `IdempotencyGuard` component exists in `src/.../messaging/consumer/`
- Idempotency is **in-memory** (not persisted to database)
- Implication: Idempotency is lost on application restart
- This is a documented limitation (see Section 22)

### Retry Configuration (from `application.properties`)
- Retry enabled, max 3 attempts, initial interval 1000ms, multiplier 2.0x
- Rejected messages do NOT requeue (`default-requeue-rejected=false`)

> [!NOTE]
> RabbitMQ is a **transport/event pipeline** only. It does not make authorization decisions. All authorization decisions are completed before events are published to RabbitMQ.

---

## 14. WebSocket

**Source**: `WebSocketConfig.java`, `WebSocketProperties.java`, `WebSocketEventPublisher.java`

| Property | Value |
| :--- | :--- |
| WebSocket Endpoint | `/ws` |
| STOMP Topic Prefix | `/topic` |
| App Prefix | `/app` |
| Allowed Origins | `*` (from `application.properties`) |

### STOMP Topics
From `docs/submission_checklist.md` and source: 7 broadcast channels on `/ws`. Topics include real-time authorization decisions, trust updates, risk updates, device state changes, and security alerts.

---

## 15. Dashboard

**Source**: [`DashboardController.java`](file:///J:/PROJECT/TRUST%20-ABAC/trustabac-iot/src/main/java/com/trustabac/iot/controller/DashboardController.java), `dashboard.html` (Thymeleaf), `dashboard.css`, `dashboard.js`

- URL: `/dashboard` (served by Spring Boot / Thymeleaf)
- Infrastructure status is **backend-driven** (health APIs, not hardcoded)
- The dashboard is **observational only** — it does not make authorization decisions
- Live updates via STOMP WebSocket subscriptions in `dashboard.js`
- Dashboard assets: `dashboard.css` (19,680 bytes), `dashboard.js` (35,390 bytes), `dashboard.html` (26,284 bytes)
- Zero embedded secrets and zero client-side authorization logic (confirmed by security scan)

---

## 16. Spring Batch

**Source**: `BatchConfig.java`, `BatchAuditService.java`, `AuthorizationAnalyticsTasklet.java`, `DeviceAnalyticsTasklet.java`, `SecurityAnalyticsTasklet.java`

### Jobs and Tasklets
- 3 analytics tasklets: Authorization, Device, Security
- Input: Existing MySQL records (access requests, trust history, risk events, blockchain events)
- Output: Analytics tables (`authorization_analytics`, `device_analytics`, `security_analytics`) + `batch_run_audit`

### Configuration
- `spring.batch.job.enabled=false` — batch jobs do NOT run automatically on startup
- `trustabac.batch.enabled=true` — batch is activated via API
- Jobs triggered via `BatchAuditController` REST endpoint

> [!NOTE]
> Spring Batch analytics are **read-only with respect to authorization**. They aggregate existing data and do not make or modify authorization decisions.

---

## 17. Experimental Framework

**Source**: `contracts/experiment_phase8b.py`, `contracts/experiment_phase8a.py`, `contracts/analyze_phase8c.py`

- **Phase 8A**: Baseline workload generator and latency decomposition framework
- **Phase 8B**: Full controlled experimental campaign (see Section 18)
- **Phase 8C**: Statistical analysis, chart generation, thesis table production

### Experiment Design (verified from `experiment_phase8b.py`)
- Base URL: `http://localhost:8090` (via `TRUSTABAC_BASE_URL` env var)
- 3 statistical modes:
  - **Mode A**: ABAC-only offline counterfactual (no trust/risk/blockchain)
  - **Mode B**: Centralized T-ABAC offline counterfactual (trust+ABAC, no blockchain)
  - **Mode C**: Full authoritative TrustABAC-IoT pipeline (actual system under test)
- Seed formula confirmed: `seed = r * 1000 + s * 100 + 42`
- Warm-up samples excluded from all latency/throughput statistics

---

## 18. Experimental Results

**Source**: `contracts/experiment_results_phase8b.json`, `contracts/experiment_results_phase8b.csv`, `contracts/phase8c_analysis/thesis_tables.md`

All values verified from `validate_phase8c.py` (ground truth) and `thesis_tables.md`:

### Dataset Counts (verified from `validate_phase8c.py` line 97-99)
| Metric | Value |
| :--- | :--- |
| Total raw samples (incl. warm-up) | 960 |
| Warm-up samples | 240 |
| Measured samples | 720 |
| Scenarios | 8 |
| Repetitions | 3 |
| Measured per scenario | 90 |

### Decision Distribution (verified from `validate_phase8c.py` lines 107-109)
| Decision | Count |
| :--- | :--- |
| ALLOW | 221 |
| RESTRICT | 110 |
| DENY | 389 |
| **Total** | **720** |

### Enforcement Distribution (mirrors decisions 1:1)
| Status | Count |
| :--- | :--- |
| EXECUTED | 221 |
| DOWNGRADED | 110 |
| BLOCKED | 389 |

### Latency Results (from `thesis_tables.md` Table 3)
| Scenario | Auth Mean (ms) | Enforce Mean (ms) |
| :--- | :--- | :--- |
| NORMAL_ACCESS | 61.217 ± 2.258 | 72.020 ± 2.657 |
| RESTRICT_ACCESS | 56.005 ± 0.924 | 65.888 ± 1.087 |
| LOW_TRUST | 59.435 ± 3.644 | 69.924 ± 4.287 |
| HIGH_RISK | 43.953 ± 0.831 | 51.709 ± 0.978 |
| ABAC_FAILURE | **34.814 ± 0.699** | 40.958 ± 0.823 |
| MIXED | 52.478 ± 2.548 | 61.739 ± 2.998 |
| BLOCKCHAIN_OUTAGE | **0.100 ± 0.000** | 0.002 ± 0.000 |
| RECOVERY | 41.289 ± 0.844 | 48.575 ± 0.993 |

### Blockchain Gas (verified from `validate_phase8c.py` lines 123-125)
| Metric | Value |
| :--- | :--- |
| On-chain transactions | 421 |
| Gas per transaction | 31,863 (constant, σ=0) |
| Total gas consumed | 13,414,323 |

### Mode Comparison (verified from `validate_phase8c.py` lines 154-155)
| Mode | Agreements | Rate | Scope |
| :--- | :--- | :--- | :--- |
| Mode A (ABAC-only) | 311/720 | 43.19% | Dataset-specific |
| Mode B (Centralized T-ABAC) | 720/720 | 100.00% | Dataset-specific |

> [!IMPORTANT]
> **Mode A (43.19%) and Mode B (100.00%) are dataset-scoped offline counterfactual calculations**. These must NOT be stated as "ABAC is only 43.19% secure" or "TrustABAC improves security by 100%." They represent agreement rates against the authoritative Mode C pipeline in this specific controlled dataset.

### Frozen Dataset Hashes (verified from `validate_phase8c.py`)
| File | SHA-256 |
| :--- | :--- |
| `experiment_results_phase8b.json` | `3d17535027deaf2498b5def17fc99bdcd2baf54e39d127a72d472170874d7b52` |
| `experiment_results_phase8b.csv` | `6f26deb700fd250630404506f10a6c25cd6fda4820e20c90279b9d84e3e9adb5` |

---

## 19. Security Validation

Historical pass counts from `docs/final_project_status.md`:

| Suite | Result |
| :--- | :--- |
| Maven unit/integration tests | 210 / 210 |
| Py-EVM (test_adaptive_access_control.py) | 33 / 33 |
| Ganache verification | 36 / 36 |
| Phase 7A Enforcement | 9 / 9 |
| Phase 7B Simulator | 14 / 14 |
| Phase 7C RabbitMQ | 6 / 6 |
| Phase 7D WebSocket | 5 / 5 |
| Phase 7E Dashboard | 10 / 10 |
| Phase 8C Validator | 45 / 45 |
| Final smoke test | 15 / 15 |
| Secret scan | 302 files, 0 findings |

> [!CAUTION]
> **CANNOT BE VERIFIED IN ISOLATION**: These test results were produced at the time of the v1.0.0 snapshot. Current working tree is clean. Maven tests and Python integration suites have NOT been re-executed during this analysis run (per the no-modification rule). They are reported as historical documented evidence only.

> [!WARNING]
> **DISCREPANCY**: `docs/final_project_status.md` states "282 Files Scanned" while `contracts/security_scan_final.md` states "302 Files Scanned." These numbers disagree. The `security_scan_final.md` is the direct output artifact and is likely more authoritative. The difference may reflect a documentation update lag.

---

## 20. Demo Workflow

**Source**: `docs/final_demo_script.md`, `docs/review2_demo_runbook.md`, `start-demo.ps1`, `START_DEMO.bat`

### Core Demo Scenarios
| Scenario | Trust | Risk | ABAC | Decision | Enforcement | Effective Op |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| NORMAL | 80 | ~14 | PASS | ALLOW | EXECUTED | CONTROL |
| RESTRICT | 80 | ~31 | PASS | RESTRICT | DOWNGRADED | READ |
| LOW TRUST | 15–20 | ~14 | PASS | DENY | BLOCKED | NONE |

### Startup Sequence (from `start-demo.ps1`)
1. Load `.env` file
2. Start Docker services (MySQL, RabbitMQ, Ganache) via `docker-compose up -d`
3. Wait for MySQL health check
4. Run `ensure_contract_deployed.py` (Python) to deploy/verify smart contract
5. Start Spring Boot JAR (`target/trustabac-iot-0.0.1-SNAPSHOT.jar`) as background process on port 8090
6. Poll `/api/health` until healthy
7. Open browser to `http://127.0.0.1:8090/dashboard`

### Entry Points
- `START_DEMO.bat` — double-click launcher (calls `start-demo.ps1`)
- `STOP_DEMO.bat` → `stop-demo.ps1` — graceful shutdown

---

## 21. Current Documentation

| Document | Location | Status |
| :--- | :--- | :--- |
| README.md | root | Present, accurate, comprehensive |
| DEMO_README.md | root | Present |
| HELP.md | root | Present |
| Thesis Ch 1–12 | `docs/thesis/` | Complete (12 chapters) |
| Final Demo Script | `docs/final_demo_script.md` | Present |
| Viva Q&A | `docs/viva_questions_and_answers.md` | 40+ questions |
| Final Architecture | `docs/final_architecture.md` | Present |
| Submission Checklist | `docs/submission_checklist.md` | All items checked |
| Final Project Status | `docs/final_project_status.md` | READY FOR THESIS/DEMO/VIVA |
| Reference Audit | `docs/reference_audit.md` | 10 academic citations |
| Security Hardening | `docs/security_hardening_report.md` | Present |
| Phase 8C Analysis | `contracts/phase8c_analysis/` | 12 files + 10 charts |

---

## 22. Current Limitations

All limitations verified from source/experiment inspection:

1. **Software-simulated IoT devices** — All devices are in-memory Java state (`SimulatedDeviceStateStore`, `ConcurrentHashMap`). No physical MCU, no hardware bus, no real actuation.
2. **Local Ganache EVM only** — Chain ID 1337, single-node, instant block mining. No distributed consensus latency, no mempool, no public testnet/mainnet.
3. **Trust/Risk thresholds are prototype values** — Weights (time=10, location=15, sensitivity=20, etc.) and trust deltas (normal=+1, malicious=-40) are experimentally configured starting points, not scientifically validated universal constants.
4. **In-memory idempotency** — `IdempotencyGuard` uses in-memory state. Idempotency tracking is lost on application restart, providing no replay protection across JVM restarts.
5. **Sequential benchmarking only** — Throughput figures reflect single-threaded sequential dispatch. Concurrent request capacity under saturated load is not evaluated.
6. **Controlled synthetic workloads** — 720 samples across 8 deterministic scenarios. Real-world adversarial distributions and long-running behavioral decay are not represented.
7. **No physical energy measurements** — Power consumption of IoT transceivers (BLE, Zigbee, LoRaWAN) was not measured.
8. **Single gateway instance** — No multi-gateway consensus, no threshold signatures, no decentralized oracle.
9. **Time risk factor currently inactive** — `application.properties` sets `normal-start-hour=0` and `normal-end-hour=24`, making the time risk contribution 0.0 in all scenarios.
10. **No authentication layer** — Spring Security is configured but the current mode appears to be stateless REST with permissive access for demo purposes (requires verification at run-time).

---

## 23. Technical Debt

Ranked by severity based on repository inspection:

### CRITICAL
| Item | Location | Description |
| :--- | :--- | :--- |
| Port inconsistency | `application.properties` vs `docker-compose.yml` / `.env.example` | `server.port=8080` in properties but all demo scripts target port `8090`. Application runs on whichever port is specified at launch. |
| Time risk factor misconfiguration | `application.properties` lines 41-42 | `normal-start-hour=0`, `normal-end-hour=24` silently disables time risk contribution for all requests. |

### HIGH
| Item | Location | Description |
| :--- | :--- | :--- |
| In-memory idempotency | `IdempotencyGuard.java` | Not durable across restarts; no replay protection |
| Spring Boot version doc mismatch | `docs/submission_checklist.md`, `docs/thesis/06_implementation.md` | States "Spring Boot 3.2.3" but `pom.xml` shows `4.1.1` |
| Security scan file count mismatch | `docs/final_project_status.md` (282) vs `security_scan_final.md` (302) | 20-file count discrepancy in documentation |
| No persistent secret/session management | `SecurityConfig.java` | Stateless REST security; no token refresh or session management for production use |

### MEDIUM
| Item | Location | Description |
| :--- | :--- | :--- |
| Hardcoded device identifiers in simulator | `experiment_phase8b.py` line 90 | Container name `releasemind-mysql` hardcoded (should be `trustabac-mysql`) |
| Hardcoded trust manipulation in experiment | `experiment_phase8b.py` lines 90-104 | Direct MySQL `docker exec` command for trust manipulation |
| `bookingActive` detection heuristic | `DecisionCoordinator.java` lines 198-203 | Booking active status derived by checking if reason string contains "booking" — fragile heuristic |
| `ResourceSensitivity` defaults | `ResourceOperationService.java` | Falls back to `MEDIUM` sensitivity when resource is unrecognized |
| No Dockerfile in repo | `docker-compose.yml` references `Dockerfile` | `trustabac-iot` service builds from Dockerfile but `Dockerfile` is not present in root — Docker container build would fail |

### LOW
| Item | Location | Description |
| :--- | :--- | :--- |
| `target/` directory not ignored from non-Maven IDE builds | `.gitignore` | `target/` is gitignored but `app-8090.log` and `app-8090-error.log` in root are log files (logs/ is ignored, root-level *.log not explicitly) |
| Multiple overlapping launcher scripts | `start-app.bat`, `start-daemon.cmd`, `start-demo.ps1`, `START_DEMO.bat` | 4 different entry points with overlapping scope |
| Spring Batch Phase 7F mislabeled | `docs/final_project_status.md` line 37 | States "36 / 36 Batch PASS" but Phase numbering says 7F, count differs from other suite sizes |
| `phase6b_state.json` gitignored | `.gitignore` line 138 | Runtime state file ignored but `phase6b_state.json` still present in `contracts/` directory |

---

## 24. Known Inconsistencies

All identified contradictions between repository artifacts:

### Inconsistency #1 — Spring Boot Version
- **`pom.xml`**: Spring Boot `4.1.1`
- **`docs/submission_checklist.md` line 8**: "Spring Boot 3.2.3"
- **`docs/thesis/06_implementation.md` line 7**: "Spring Boot 3.2.3"
- **`thesis_tables.md` Table 1**: "Spring Boot 3.2.3, Spring Security 6.2"
- **`README.md` line 74**: "Spring Boot 4.1.1"
- **Authoritative source**: `pom.xml` (actual build file). Documentation uses the older version number.
- **Action required**: Update thesis documentation to reflect 4.1.1.

### Inconsistency #2 — Server Port
- **`application.properties`**: `server.port=8080`
- **`.env.example`**: `SERVER_PORT=8090`
- **`docker-compose.yml`**: `SERVER_PORT:-8090`, exposes `8090:8090`
- **`start-demo.ps1`**: targets `http://127.0.0.1:8090`
- **Authoritative for local-native run**: Spring Boot uses `application.properties` default (8080) unless overridden by `--server.port=8090` argument or `SERVER_PORT` env var.
- **Authoritative for Docker run**: Environment variable 8090 overrides.

### Inconsistency #3 — MySQL Port
- **`docker-compose.yml`**: `"${DB_PORT:-3308}:3306"` — host port 3308
- **`.env.example`**: `DB_PORT=3307` — suggests 3307
- **Authoritative**: `docker-compose.yml` default (3308) unless `.env` overrides with 3307.

### Inconsistency #4 — Security Scan File Count
- **`docs/final_project_status.md`**: "282 Files Scanned"
- **`contracts/security_scan_final.md`**: "Total Files Scanned: 302"
- **Authoritative**: `security_scan_final.md` (direct script output artifact, dated 2026-09-22).

### Inconsistency #5 — Solidity Version in Thesis
- **`AdaptiveAccessControl.sol` line 2**: `pragma solidity ^0.8.20;`
- **`docs/submission_checklist.md`**: No explicit version reference
- **`thesis_tables.md` Table 1**: "Solidity ^0.8.19"
- **Authoritative**: The `.sol` source file (`^0.8.20`).

### Inconsistency #6 — Dockerfile Missing
- **`docker-compose.yml`**: References `Dockerfile` in root context
- **Repository root**: No `Dockerfile` present
- **Implication**: `docker-compose up --build` for the `trustabac-iot` service would fail. The current Docker setup is infrastructure-only (MySQL, Ganache, RabbitMQ); the Spring Boot app is launched as a local JAR.

### Inconsistency #7 — Container Name in Experiment Script
- **`experiment_phase8b.py` line 90**: `docker exec releasemind-mysql` (wrong container name)
- **`docker-compose.yml`**: Container name is `trustabac-mysql`
- **Implication**: The trust manipulation via docker exec in the experiment script would fail in the current Docker environment.

---

## 25. Recommended Improvements

Listed by category and version target:

| Version | Priority | Improvement | Description |
| :--- | :--- | :--- | :--- |
| v1.1.x | CRITICAL | Fix server port consistency | Align `application.properties` to 8090 or document the env-var override clearly |
| v1.1.x | CRITICAL | Fix time risk misconfiguration | Set `normal-start-hour=6`, `normal-end-hour=22` in properties |
| v1.1.x | HIGH | Fix Spring Boot version in docs | Update thesis/checklist to 4.1.1 |
| v1.1.x | HIGH | Fix container name in experiment script | `releasemind-mysql` → `trustabac-mysql` |
| v1.1.x | HIGH | Add Dockerfile or remove from docker-compose | Either provide a Dockerfile or switch to external JAR launch |
| v1.1.x | MEDIUM | Fix bookingActive heuristic | Replace string-check in `DecisionCoordinator` with explicit boolean from ABAC result |
| v1.2.x | HIGH | Persistent idempotency | Replace in-memory `IdempotencyGuard` with database-backed idempotency table |
| v1.2.x | MEDIUM | Add replay protection | Include request nonce or timestamp validation against recent-request window |
| v1.2.x | MEDIUM | Solidity version documentation | Update thesis_tables.md to reference ^0.8.20 |
| v1.3.x | MEDIUM | Concurrent load testing | Add JMeter/Gatling harness for saturated concurrency benchmarks |
| v1.3.x | MEDIUM | Configurable enforcement rules | Move device-type enforcement logic from hardcoded Java to configuration |
| v2.0.0 | HIGH | Physical device simulation | ESP32 or Raspberry Pi integration for physical enforcement validation |
| v2.0.0 | HIGH | Public testnet evaluation | Sepolia or Polygon Mumbai gas/latency benchmarking |
| v2.0.0 | MEDIUM | Multi-gateway architecture | Distributed gateway consensus for attribute collection |

---

## 26. Reproducibility

| Check | Status |
| :--- | :--- |
| `.env.example` present and tracked | ✅ Confirmed |
| `.env` gitignored | ✅ Confirmed |
| Private key patterns ignored | ✅ `*.pem`, `*.key`, `*.p12`, `*.jks` all in `.gitignore` |
| `contracts/phase6b_state.json` gitignored | ✅ Confirmed (but file still exists in working tree) |
| Maven wrapper present | ✅ `mvnw`, `mvnw.cmd` both present |
| Docker compose present | ✅ `docker-compose.yml` |
| Contract deployment script present | ✅ `contracts/ensure_contract_deployed.py` |
| Frozen dataset files present | ✅ `experiment_results_phase8b.json`, `.csv` |
| All 10 charts present | ✅ `contracts/phase8b_charts/` (10 PNG files) |
| Dockerfile present | ❌ Missing (Inconsistency #6) |
| `target/*.jar` present | Gitignored — must be built with `mvnw clean package` |

### Clean Reproducibility Steps
```
1. Copy .env.example → .env and fill credentials
2. docker-compose up -d (starts MySQL, Ganache, RabbitMQ)
3. Wait for MySQL health check
4. python contracts/ensure_contract_deployed.py
5. ./mvnw.cmd clean package -DskipTests
6. java -jar target/trustabac-iot-0.0.1-SNAPSHOT.jar --server.port=8090
7. Open http://localhost:8090/dashboard
```

---

## 28. v1.1.0 Stabilization Status

As part of the v1.1.0 stabilization milestone, the 7 verified repository inconsistencies have been systematically resolved:
1. **Spring Boot Version**: Standardized all documentation and thesis tables to `4.1.1` (matching `pom.xml`).
2. **Application Port**: Standardized `application.properties` default to `server.port=8090` (aligning with `start-demo.ps1`, `START_DEMO.bat`, and `.env.example`).
3. **MySQL Host Port**: Standardized `docker-compose.yml` to `"${DB_PORT:-3307}:3306"` (aligning with `.env.example`).
4. **Security Scan Count**: Corrected documentation to 302 files scanned (matching `security_scan_final.md`).
5. **Solidity Version**: Standardized thesis documentation to `^0.8.20` (matching `AdaptiveAccessControl.sol`).
6. **Docker Compose Architecture**: Removed broken `trustabac-iot` container build from `docker-compose.yml`; Docker Compose is explicitly dedicated to infrastructure services (MySQL, Ganache, RabbitMQ), while Spring Boot is launched natively via `START_DEMO.bat` / `start-demo.ps1`.
7. **Container Names**: Corrected stale container references (`releasemind-mysql` -> `trustabac-mysql`, `ganache-trustabac` -> `trustabac-ganache`) across all test/experiment scripts and runbooks.

**Frozen Research Datasets**: Preserved byte-for-byte with unchanged SHA-256 hashes (`experiment_results_phase8b.json` and `experiment_results_phase8b.csv`).
**Time-Risk Configuration**: Deliberately maintained at 00:00–24:00 (zero time-risk contribution) for consistency with the frozen Phase 8B experimental campaign, and explicitly documented in `application.properties`.

---

## 29. Final Current-State Assessment

| Dimension | Assessment |
| :--- | :--- |
| **Implementation completeness** | All 10 phases implemented. 88 Java source files. |
| **Smart contract** | Solidity ^0.8.20, correctly authored, ABI/BIN committed. |
| **Experiments** | 720 measured samples, frozen, hash-verified. |
| **Test coverage** | 210 Maven tests + 10 Python integration suites (historical). |
| **Git state** | Maintained clean history; v1.0.0 preserved; v1.1.0 stabilization applied. |
| **Security hygiene** | 302 files scanned, 0 secret findings. |
| **Documentation** | 12 thesis chapters, viva Q&A, demo scripts, submission checklist (all version-synchronized). |
| **Inconsistencies** | 7 verified inconsistencies identified and resolved in v1.1.0. |
| **Research positioning** | Accurately scoped as prototype with controlled evaluation. |
| **Demo readiness** | Ready for native-launch demo via `START_DEMO.bat` / `start-demo.ps1`. |

**PROJECT STATUS (v1.1.0): STABILIZED RESEARCH PROTOTYPE — CONSISTENT CONFIGURATION, CLEAN REPRODUCIBILITY, PRESERVED RESEARCH BASELINE.**
