# TrustABAC-IoT — Implementation Inventory

> **Inspection Date**: 2026-09-24
> **Source**: Direct repository inspection (code, config, scripts, contracts)
> **Git Baseline**: v1.0.0 (commit `3c10485`)

---

## Core Authorization Pipeline

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **DecisionCoordinator** | `service/DecisionCoordinator.java` | Orchestrates the full 4-gate pipeline: ABAC → Trust lookup → Risk → Blockchain → audit persistence | ✅ COMPLETE | Fail-closed on blockchain unavailability |
| **AbacService** | `service/AbacService.java` | Gate 1: ABAC eligibility evaluation (device, policy, booking, attributes) | ✅ COMPLETE | Returns PASS/FAIL; FAIL short-circuits all downstream gates |
| **TrustService** | `service/TrustService.java` | Gate 2: Trust score retrieval, delta updates, append-only history | ✅ COMPLETE | Read-only in authorization pipeline; TrustService mutates trust via explicit events |
| **RiskService** | `service/RiskService.java` | Gate 3: 7-factor weighted composite risk evaluation + RiskEvent persistence | ✅ COMPLETE | Risk is orthogonal to Trust |
| **BlockchainService** | `service/blockchain/BlockchainService.java` | Gate 4: Web3j RPC to Ganache → AdaptiveAccessControl.evaluateAccess() | ✅ COMPLETE | Returns authoritative ALLOW/RESTRICT/DENY from Solidity |
| **ResourceOperationService** | `service/ResourceOperationService.java` | Enforcement: maps Decision to EnforcementStatus; applies device state changes | ✅ COMPLETE | ALLOW→EXECUTED, RESTRICT→DOWNGRADED, DENY→BLOCKED |
| **PolicyEvaluator** | `service/PolicyEvaluator.java` | Evaluates ABAC policy conditions against attribute dictionary | ✅ COMPLETE | Supports EQUALS, NOT_EQUALS, CONTAINS, EXISTS, NOT_EXISTS, BOOLEAN_TRUE, BOOLEAN_FALSE, IN |
| **BookingService** | `service/BookingService.java` | Validates booking references, active status, temporal bounds, location match | ✅ COMPLETE | Used by AbacService for Gate 1 context |

---

## ABAC

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Policy (entity)** | `entity/Policy.java` | Policy definition: name, targetResource, targetOperation, conditions | ✅ COMPLETE | Stored in MySQL |
| **PolicyCondition (entity)** | `entity/PolicyCondition.java` | Individual condition: attributeKey, operator, value | ✅ COMPLETE | |
| **PolicyOperator (enum)** | `entity/PolicyOperator.java` | EQUALS, NOT_EQUALS, CONTAINS, EXISTS, NOT_EXISTS, BOOLEAN_TRUE, BOOLEAN_FALSE, IN | ✅ COMPLETE | |
| **AbacAttributeKeys** | `entity/AbacAttributeKeys.java` | Constants for attribute dictionary keys | ✅ COMPLETE | |
| **AbacResult (enum)** | `entity/AbacResult.java` | PASS / FAIL | ✅ COMPLETE | |
| **AccessRequest (entity)** | `entity/AccessRequest.java` | Persisted audit log of each ABAC evaluation | ✅ COMPLETE | Created on every request (PASS and FAIL) |
| **PolicyController** | `controller/PolicyController.java` | REST CRUD for policies | ✅ COMPLETE | |
| **PolicyService** | `service/PolicyService.java` | Policy lifecycle management | ✅ COMPLETE | |
| **AccessController** | `controller/AccessController.java` | REST endpoint for direct ABAC evaluation | ✅ COMPLETE | |

---

## Trust

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **TrustService** | `service/TrustService.java` | Trust score retrieval, event recording, bounded deltas, status categorization | ✅ COMPLETE | |
| **TrustProperties** | `config/TrustProperties.java` | Configurable trust parameters (initial=80, deltas, min/max) | ✅ COMPLETE | Bound to `trustabac.trust.*` |
| **TrustHistory (entity)** | `entity/TrustHistory.java` | Append-only audit record of trust events | ✅ COMPLETE | Never deleted or modified |
| **TrustEventType (enum)** | `entity/TrustEventType.java` | NORMAL_SUCCESS, SUSPICIOUS_ACTIVITY, REQUEST_FLOODING, CONFIRMED_MALICIOUS, RECOVERY | ✅ COMPLETE | |
| **TrustController** | `controller/TrustController.java` | REST endpoints for trust score, history, event recording | ✅ COMPLETE | |
| **TrustHistoryRepository** | `repository/TrustHistoryRepository.java` | JPA repository for TrustHistory | ✅ COMPLETE | |

---

## Risk

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **RiskService** | `service/RiskService.java` | 7-factor weighted composite risk evaluation, status mapping, RiskEvent persistence | ✅ COMPLETE | |
| **RiskProperties** | `config/RiskProperties.java` | Weight configuration (time=10, location=15, sensitivity=20, frequency=20, network=15, violations=10, behavior=10), thresholds | ✅ COMPLETE | ⚠ time risk silently disabled by properties override |
| **TimeRiskCalculator** | `service/risk/TimeRiskCalculator.java` | Off-hours risk factor (0.0–1.0) | ✅ COMPLETE | Currently returns 0.0 (24h window configured) |
| **LocationRiskCalculator** | `service/risk/LocationRiskCalculator.java` | Location mismatch risk factor | ✅ COMPLETE | |
| **SensitivityRiskCalculator** | `service/risk/SensitivityRiskCalculator.java` | Resource sensitivity risk factor (LOW=0, MEDIUM=0.35, HIGH=0.70, CRITICAL=1.0) | ✅ COMPLETE | |
| **FrequencyRiskCalculator** | `service/risk/FrequencyRiskCalculator.java` | Request burst frequency risk factor | ✅ COMPLETE | |
| **NetworkRiskCalculator** | `service/risk/NetworkRiskCalculator.java` | Network trust level (LocalWiFi=0, VPN=0.30, Cellular=0.60, Unknown=1.0) | ✅ COMPLETE | |
| **ViolationRiskCalculator** | `service/risk/ViolationRiskCalculator.java` | Recent violation history risk factor | ✅ COMPLETE | |
| **BehaviorRiskCalculator** | `service/risk/BehaviorRiskCalculator.java` | Behavioral anomaly indicator (NORMAL=0, SUSPICIOUS=0.60, ABNORMAL=1.0) | ✅ COMPLETE | |
| **ResourceRegistry** | `service/risk/ResourceRegistry.java` | Resolves resource sensitivity (authoritative, server-side) | ✅ COMPLETE | |
| **RiskContext** | `service/risk/RiskContext.java` | Immutable context record passed to all risk calculators | ✅ COMPLETE | |
| **RiskEvent (entity)** | `entity/RiskEvent.java` | Append-only risk evaluation audit record | ✅ COMPLETE | |
| **RiskStatus (enum)** | `entity/RiskStatus.java` | LOW / MEDIUM / HIGH | ✅ COMPLETE | |
| **RiskController** | `controller/RiskController.java` | REST endpoints for risk evaluation, history, current risk | ✅ COMPLETE | |

---

## Blockchain / Smart Contract

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **AdaptiveAccessControl.sol** | `contracts/AdaptiveAccessControl.sol` | Authoritative decision engine: DENY/RESTRICT/ALLOW | ✅ COMPLETE | Solidity ^0.8.20 |
| **AdaptiveAccessControl.abi** | `contracts/AdaptiveAccessControl.abi` | Contract ABI for Web3j binding | ✅ COMPLETE | |
| **AdaptiveAccessControl.bin** | `contracts/AdaptiveAccessControl.bin` | Compiled bytecode | ✅ COMPLETE | |
| **AdaptiveAccessControl.java** | `blockchain/contract/AdaptiveAccessControl.java` | Web3j-generated Java wrapper | ✅ COMPLETE | |
| **BlockchainService** | `service/blockchain/BlockchainService.java` | Contract deployment, loading, threshold queries, evaluateAccessOnChain() | ✅ COMPLETE | Fail-closed on exception |
| **BlockchainConfig** | `config/BlockchainConfig.java` | Web3j bean creation, credentials setup (ephemeral key fallback) | ✅ COMPLETE | |
| **BlockchainProperties** | `config/BlockchainProperties.java` | RPC URL, chain ID, contract address, gas config | ✅ COMPLETE | |
| **BlockchainAuthorizationEvent** | `entity/BlockchainAuthorizationEvent.java` | Off-chain audit of each authorization transaction | ✅ COMPLETE | |
| **BlockchainController** | `controller/BlockchainController.java` | REST status, deploy, threshold query endpoints | ✅ COMPLETE | |
| **ensure_contract_deployed.py** | `contracts/ensure_contract_deployed.py` | Python script to deploy/verify contract on startup | ✅ COMPLETE | Called by `start-demo.ps1` |
| **Decision (enum)** | `entity/Decision.java` | DENY(0) / RESTRICT(1) / ALLOW(2) with `fromSolidityCode()` mapping | ✅ COMPLETE | |

---

## Web3j Integration

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Web3j 4.10.3** | `pom.xml` dependency | Java Ethereum client library | ✅ COMPLETE | `org.web3j:core:4.10.3` |
| **Credentials bean** | `BlockchainConfig.java` | Private key loading / ephemeral EC keypair generation | ✅ COMPLETE | Dynamic ephemeral key if env var absent |
| **ContractGasProvider** | `BlockchainConfig.java` | Gas limit 3,000,000 / Gas price 20,000,000,000 | ✅ COMPLETE | |

---

## Enforcement

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **ResourceOperationService** | `service/ResourceOperationService.java` | End-to-end operation pipeline: authorize → enforce → state mutation | ✅ COMPLETE | |
| **SimulatedDeviceStateStore** | `service/enforcement/SimulatedDeviceStateStore.java` | Thread-safe in-memory device state (ConcurrentHashMap, 9 pre-seeded devices) | ✅ COMPLETE | Software simulation only |
| **EnforcementStatus (enum)** | `entity/EnforcementStatus.java` | EXECUTED / DOWNGRADED / BLOCKED | ✅ COMPLETE | |
| **Operation (enum)** | `entity/Operation.java` | READ / WRITE / UPDATE / DELETE / CONTROL | ✅ COMPLETE | |
| **ResourceOperationController** | `controller/ResourceOperationController.java` | REST endpoint for device operations | ✅ COMPLETE | |

---

## IoT Simulator

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **SimulatorService** | `simulator/SimulatorService.java` (inferred) | In-memory IoT traffic scenario generator | ✅ COMPLETE | 10 scenario archetypes, batch runner |
| **SimulatorController** | `controller/` (inferred) | REST API for simulator start/stop/reset/scenario execution | ✅ COMPLETE | `/api/simulator/reset` called in experiment scripts |
| **experiment_phase8b.py** | `contracts/experiment_phase8b.py` | Controlled experimental campaign (8 scenarios, 3 repetitions, 30 ops each) | ✅ COMPLETE | Frozen results stored |
| **experiment_phase8a.py** | `contracts/experiment_phase8a.py` | Phase 8A baseline workload framework | ✅ COMPLETE | |
| **phase6b_scenarios.py** | `contracts/phase6b_scenarios.py` | Phase 6B scenario definitions | ✅ COMPLETE | |

---

## RabbitMQ

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **RabbitMqConfig** | `config/RabbitMqConfig.java` | Exchange, queue, binding declarations | ✅ COMPLETE | |
| **MessagingProperties** | `config/MessagingProperties.java` | Exchange name, queue names, routing keys | ✅ COMPLETE | |
| **IoTEventPublisher** | `messaging/publisher/IoTEventPublisher.java` | Publishes events to RabbitMQ exchange | ✅ COMPLETE | |
| **AuthorizationEventConsumer** | `messaging/consumer/AuthorizationEventConsumerTest.java` implies consumer class | Consumes authorization events | ✅ COMPLETE | |
| **TrustEventConsumer** | `messaging/consumer/` | Consumes trust events | ✅ COMPLETE | |
| **RiskEventConsumer** | `messaging/consumer/` | Consumes risk events | ✅ COMPLETE | |
| **DeviceOperationEventConsumer** | `messaging/consumer/` | Consumes device operation events | ✅ COMPLETE | |
| **DeadLetterQueueConsumer** | `messaging/consumer/DeadLetterQueueConsumerTest.java` implies class | Handles DLQ messages | ✅ COMPLETE | |
| **IdempotencyGuard** | `messaging/consumer/IdempotencyGuard.java` | In-memory deduplication guard | ✅ COMPLETE | ⚠ Non-durable (in-memory only) |
| **MessagingController** | `controller/MessagingController.java` | REST endpoint for messaging status | ✅ COMPLETE | |

---

## WebSocket

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **WebSocketConfig** | `config/WebSocketConfig.java` | STOMP endpoint (`/ws`), broker configuration | ✅ COMPLETE | |
| **WebSocketProperties** | `config/WebSocketProperties.java` | Endpoint path, topic prefix, origins | ✅ COMPLETE | |
| **WebSocketEventPublisher** | `websocket/WebSocketEventPublisher.java` | Broadcasts events to STOMP topics | ✅ COMPLETE | |
| **WebSocketSessionTracker** | `websocket/WebSocketSessionTracker.java` | Tracks active STOMP sessions | ✅ COMPLETE | |
| **WebSocketStatusController** | `controller/WebSocketStatusController.java` | REST endpoint for WebSocket status | ✅ COMPLETE | |

---

## Dashboard

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **DashboardController** | `controller/DashboardController.java` | Serves `/dashboard` Thymeleaf view | ✅ COMPLETE | |
| **dashboard.html** | `resources/templates/dashboard.html` | Main dashboard page (26,284 bytes) | ✅ COMPLETE | Thymeleaf template |
| **dashboard.css** | `resources/static/css/dashboard.css` | Dashboard styling (19,680 bytes) | ✅ COMPLETE | |
| **dashboard.js** | `resources/static/js/dashboard.js` | Real-time STOMP subscriptions, UI updates (35,390 bytes) | ✅ COMPLETE | |

---

## Spring Batch Analytics

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **BatchConfig** | `batch/config/BatchConfig.java` | Job and step definitions | ✅ COMPLETE | |
| **BatchProperties** | `batch/config/BatchProperties.java` | Configurable batch parameters | ✅ COMPLETE | |
| **AuthorizationAnalyticsTasklet** | `batch/step/AuthorizationAnalyticsTasklet.java` | Reads access requests, computes analytics | ✅ COMPLETE | |
| **DeviceAnalyticsTasklet** | `batch/step/DeviceAnalyticsTasklet.java` | Reads device events, computes device analytics | ✅ COMPLETE | |
| **SecurityAnalyticsTasklet** | `batch/step/SecurityAnalyticsTasklet.java` | Reads security events, computes security analytics | ✅ COMPLETE | |
| **BatchAuditService** | `batch/service/BatchAuditService.java` | Coordinates batch job execution | ✅ COMPLETE | |
| **BatchAuditJobExecutionListener** | `batch/listener/BatchAuditJobExecutionListener.java` | Job lifecycle logging | ✅ COMPLETE | |
| **AuthorizationAnalytics** | `batch/entity/AuthorizationAnalytics.java` | Analytics entity for authorization data | ✅ COMPLETE | |
| **DeviceAnalytics** | `batch/entity/DeviceAnalytics.java` | Analytics entity for device data | ✅ COMPLETE | |
| **SecurityAnalytics** | `batch/entity/SecurityAnalytics.java` | Analytics entity for security data | ✅ COMPLETE | |
| **BatchRunAudit** | `batch/entity/BatchRunAudit.java` | Tracks each batch job run for idempotency | ✅ COMPLETE | |
| **BatchAuditController** | `batch/controller/BatchAuditController.java` | REST endpoints to trigger and query batch jobs | ✅ COMPLETE | |

---

## Experiments

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **experiment_phase8a.py** | `contracts/experiment_phase8a.py` | Phase 8A: Baseline workload generator, latency decomposition | ✅ COMPLETE | |
| **experiment_phase8b.py** | `contracts/experiment_phase8b.py` | Phase 8B: 8 scenarios × 3 reps × 30 measured ops | ✅ COMPLETE | |
| **analyze_phase8c.py** | `contracts/analyze_phase8c.py` | Phase 8C: Statistical analysis, charts, thesis tables | ✅ COMPLETE | 72,817 bytes |
| **validate_phase8c.py** | `contracts/validate_phase8c.py` | Automated 45-check validator of frozen results | ✅ COMPLETE | |
| **experiment_results_phase8b.json** | `contracts/experiment_results_phase8b.json` | Frozen raw dataset (JSON, 1,038,736 bytes) | ✅ FROZEN | SHA-256 verified |
| **experiment_results_phase8b.csv** | `contracts/experiment_results_phase8b.csv` | Frozen raw dataset (CSV, 370,687 bytes) | ✅ FROZEN | SHA-256 verified |
| **phase8b_charts/** | `contracts/phase8b_charts/` | 10 high-resolution research charts (PNG, 200 DPI) | ✅ COMPLETE | |
| **phase8c_analysis/** | `contracts/phase8c_analysis/` | Statistical CSVs, markdown reports, 10 thesis figures | ✅ COMPLETE | |
| **ExperimentService** | `experiment/ExperimentService.java` (inferred from test) | Java experiment service | ✅ COMPLETE | |
| **ExperimentStatisticsCalculator** | `experiment/ExperimentStatisticsCalculator.java` (inferred) | Statistics engine (mean, CI, IQR) | ✅ COMPLETE | |

---

## Testing

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **Maven Unit Tests** | `src/test/java/` (48 test files) | Unit and integration tests for all service/controller/config layers | ✅ COMPLETE | 210/210 pass (historical) |
| **test_adaptive_access_control.py** | `contracts/` | Py-EVM contract logic tests | ✅ COMPLETE | 33/33 pass (historical) |
| **test_ganache_verification.py** | `contracts/` | Ganache deployment and interaction tests | ✅ COMPLETE | 36/36 pass (historical) |
| **test_phase7a_enforcement.py** | `contracts/` | Enforcement behavior tests | ✅ COMPLETE | 9/9 pass |
| **test_phase7b_simulator.py** | `contracts/` | Simulator behavior tests | ✅ COMPLETE | 14/14 pass |
| **test_phase7c_rabbitmq.py** | `contracts/` | RabbitMQ pipeline tests | ✅ COMPLETE | 6/6 pass |
| **test_phase7d_websocket.py** | `contracts/` | WebSocket behavior tests | ✅ COMPLETE | 5/5 pass |
| **test_phase7e_dashboard.py** | `contracts/` | Dashboard functional tests | ✅ COMPLETE | 10/10 pass |
| **test_phase7f_batch.py** | `contracts/` | Batch analytics tests | ✅ COMPLETE | Results in final_project_status.md |
| **validate_phase8c.py** | `contracts/` | 45-check research validator | ✅ COMPLETE | 45/45 pass |
| **final_smoke_test.py** | `contracts/final_smoke_test.py` | 15-check comprehensive system smoke test | ✅ COMPLETE | 15/15 pass |
| **security_scan_final.py** | `contracts/security_scan_final.py` | 302-file secret scanner | ✅ COMPLETE | 0 findings |

---

## Startup / Infrastructure

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **START_DEMO.bat** | root | Double-click demo launcher (calls start-demo.ps1) | ✅ COMPLETE | |
| **start-demo.ps1** | root | Full startup orchestration: Docker → contract → JAR → health → browser | ✅ COMPLETE | 634 lines |
| **STOP_DEMO.bat** | root | Demo shutdown launcher | ✅ COMPLETE | |
| **stop-demo.ps1** | root | Graceful shutdown of JAR and Docker services | ✅ COMPLETE | |
| **start-app.bat** | root | Alternative native app starter | ✅ COMPLETE | |
| **start-daemon.cmd** | root | Daemon mode launcher | ✅ COMPLETE | |
| **docker-compose.yml** | root | MySQL 8.0 + Ganache + RabbitMQ infrastructure | ✅ COMPLETE | ⚠ References Dockerfile that doesn't exist for Spring Boot service |
| **ensure_contract_deployed.py** | `contracts/` | Checks/deploys smart contract on Ganache at startup | ✅ COMPLETE | |
| **.env.example** | root | Environment template (tracked, secrets-free) | ✅ COMPLETE | |
| **Dockerfile** | root | ❌ MISSING | ❌ MISSING | docker-compose service `trustabac-iot` cannot build without it |

---

## Documentation

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **README.md** | root | Project overview, architecture, tech stack, build instructions | ✅ COMPLETE | Synchronized with 8090/3307 |
| **DEMO_README.md** | root | Demo-specific quick-start instructions | ✅ COMPLETE | |
| **HELP.md** | root | Development reference guide | ✅ COMPLETE | |
| **Thesis Ch 1–12** | `docs/thesis/` | Full 12-chapter thesis manuscript | ✅ COMPLETE | Version synchronized to Spring Boot 4.1.1 & Solidity ^0.8.20 |
| **docs/final_architecture.md** | `docs/` | Detailed architecture specification | ✅ COMPLETE | |
| **docs/final_demo_script.md** | `docs/` | 10–15 min demo sequence | ✅ COMPLETE | Container names updated to trustabac-* |
| **docs/viva_questions_and_answers.md** | `docs/` | 40+ technical Q&A | ✅ COMPLETE | |
| **docs/final_presentation_outline.md** | `docs/` | 15-slide defense blueprint | ✅ COMPLETE | |
| **docs/final_figure_catalog.md** | `docs/` | Catalog of all 10 research figures | ✅ COMPLETE | |
| **docs/final_table_catalog.md** | `docs/` | Catalog of all 8 thesis tables | ✅ COMPLETE | |
| **docs/reference_audit.md** | `docs/` | Verified bibliographic citations | ✅ COMPLETE | |
| **docs/security_hardening_report.md** | `docs/` | Security audit findings and hardening steps | ✅ COMPLETE | |
| **docs/submission_checklist.md** | `docs/` | Full deliverable sign-off checklist | ✅ COMPLETE | Updated to Spring Boot 4.1.1 |
| **docs/final_project_status.md** | `docs/` | Final phase-by-phase status report | ✅ COMPLETE | Updated to 302 files scanned |
| **docs/local_runbook.md** | `docs/` | Clean-room reproducibility guide | ✅ COMPLETE | |
| **docs/review2_demo_runbook.md** | `docs/` | Review 2 demo step guide | ✅ COMPLETE | Container names updated to trustabac-* |
| **docs/viva_questions_and_answers.md** | `docs/` | 40+ viva Q&A preparation | ✅ COMPLETE | |
| **Review_2_TrustABAC_IoT_Slides.md** | `docs/` | Slide-by-slide presentation content | ✅ COMPLETE | Updated to Spring Boot 4.1.1 & Solidity 0.8.20 |
| **walkthrough_phase8a.md** | `contracts/` | Phase 8A walkthrough | ✅ COMPLETE | |
| **experiment_summary_phase8b.md** | `contracts/` | Phase 8B results summary | ✅ COMPLETE | |

---

## Configuration

| Component | Location | Responsibility | Status | Notes |
| :--- | :--- | :--- | :--- | :--- |
| **application.properties** | `resources/` | All Spring Boot and domain configuration | ✅ COMPLETE | server.port=8090; time risk documented |
| **TrustProperties** | `config/TrustProperties.java` | Type-safe trust configuration bean | ✅ COMPLETE | |
| **RiskProperties** | `config/RiskProperties.java` | Type-safe risk configuration bean | ✅ COMPLETE | |
| **BlockchainProperties** | `config/BlockchainProperties.java` | Type-safe blockchain configuration bean | ✅ COMPLETE | |
| **MessagingProperties** | `config/MessagingProperties.java` | Type-safe RabbitMQ configuration bean | ✅ COMPLETE | |
| **WebSocketProperties** | `config/WebSocketProperties.java` | Type-safe WebSocket configuration bean | ✅ COMPLETE | |
| **BatchProperties** | `batch/config/BatchProperties.java` | Type-safe batch configuration bean | ✅ COMPLETE | |
| **SecurityConfig** | `config/SecurityConfig.java` | Spring Security filter chain configuration | ✅ COMPLETE | Stateless REST security |
| **AppConfig** | `config/AppConfig.java` | Application-wide beans (Clock, etc.) | ✅ COMPLETE | |
| **.env.example** | root | Environment variable template | ✅ COMPLETE | |
| **.gitignore** | root | Ignore rules for secrets, build artifacts, IDE files | ✅ COMPLETE | |
| **.gitattributes** | root | Git line-ending attributes | ✅ COMPLETE | |

---

## Summary Counts

| Category | Count |
| :--- | :--- |
| Java source files | 88 |
| Java test files | 48 |
| Python scripts | 18 |
| Markdown documentation files | 28+ |
| Solidity contract files | 1 |
| Contract ABI/BIN | 2 |
| Frozen dataset files | 2 |
| Research charts (phase8b_charts) | 10 |
| Thesis figures (phase8c_analysis/charts) | 10 |
| Thesis chapters | 12 |
| Docker services | 3 (MySQL, Ganache, RabbitMQ) |
| Simulated device types | 9 key types |
| Experiment scenarios | 8 |
| Total measured requests | 720 |
| Maven tests | 210 (historical) |
| Total Python integration checks | ~179 (historical) |
