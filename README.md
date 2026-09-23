# TRUSTABAC-IoT: ADAPTIVE TRUST- AND RISK-AWARE SMART-CONTRACT ACCESS CONTROL FOR RESOURCE-CONSTRAINED IOT NETWORKS

## 1. Project Overview

**TrustABAC-IoT** is a research prototype implementing an adaptive, multi-tiered authorization framework designed for resource-constrained IoT environments. The architecture bridges attribute-based access control (ABAC), dynamic behavioral trust evaluation, contextual risk assessment, and decentralized policy enforcement via smart contracts.

The primary demonstration scenario models a **Smart Short-Term Rental Property** (e.g., Property-001 with Guest-001), where temporary guests access authorized amenities (Smart Door Lock, Guest Wi-Fi Network, Air Conditioner, Smart Lights, Smart Thermostat) while strictly isolating sensitive administrative interfaces and hardware controls (Security Cameras, Gateway Routers, Owner Settings).

---

## 2. High-Level Architecture & Access Evaluation Pipeline

```text
  +--------------------------------------------------------------+
  |                        Access Request                        |
  |  (Subject, Device Identifier, Resource, Operation, Context)  |
  +------------------------------+-------------------------------+
                                 |
                                 v
  +--------------------------------------------------------------+
  |              Gate 1: ABAC Eligibility Engine                 |
  |   Evaluates Subject, Device, Resource, Operation, Context    |
  |      Deterministic predicates & Normalized Policy Rules      |
  +------------------------------+-------------------------------+
                                 |
                     +-----------+-----------+
                     |                       |
                  [ PASS ]                [ FAIL ] -> [ ABAC FAIL ]
                     |                                (Device Trust UNCHANGED)
                     |                                (No Risk Evaluation)
                     |                                (No Smart Contract Execution)
                     |                                (Immediate DENY Verdict)
                     v
  +--------------------------------------------------------------+
  |               Gate 2: Dynamic Trust Engine                   |
  |      Retrieves Long-Term Behavioral Reliability (Score/Band) |
  |      (Independent of Risk; Trust is NOT mutated by Access)   |
  +------------------------------+-------------------------------+
                                 |
                                 v
  +--------------------------------------------------------------+
  |               Gate 3: Contextual Risk Engine                 |
  |      Resolves Authoritative Context & 7 Modular Factors      |
  |      Calculates Weighted Composite Risk Score & Status Band  |
  |      Persists Append-Only RiskEvent (Explainable Reason)     |
  +------------------------------+-------------------------------+
                                 |
                                 v
  +--------------------------------------------------------------+
  |    Gate 4: Adaptive Smart-Contract Authorization Engine      |
  |  - Authoritative Solidity Execution: AdaptiveAccessControl   |
  |  - Synthesizes ABAC + Trust + Risk + Sensitivity + Operation |
  |  - Produces Authoritative Verdict: ALLOW / RESTRICT / DENY   |
  |  - Records On-Chain Proof (TxHash, BlockNumber, GasUsed)     |
  |  - Persists Off-Chain Audit: blockchain_authorization_events |
  +--------------------------------------------------------------+
```

### Core Semantic Separation: ABAC vs Trust vs Risk vs Smart Contract
- **ABAC (Structural & Contextual Eligibility)**: Evaluates whether a subject and device meet structural, role-based, resource-specific, and temporal criteria (e.g., valid booking window, registered device, guest role). Outputs strictly `PASS` or `FAIL`. ABAC PASS does **not** grant final immediate authorization. ABAC FAIL does **not** degrade device trust, **bypasses** risk evaluation and smart contract, and produces immediate `DENY`.
- **Trust (Long-Term Behavioral Reliability)**: Measures the accumulated historical reliability and operational integrity of an IoT device based on observed runtime events (e.g., normal operation vs suspicious activity or flooding). Maintained on `Device.currentTrust` and audited in append-only `TrustHistory`.
- **Risk (Current Contextual Danger & Threat Score)**: Assesses real-time environmental context, request velocity, network anomalies, resource sensitivity, and immediate threat posture for a specific access request.
- **Independence of Signals**:
  - `Risk != 100 - Trust`. Trust and Risk are orthogonal signals (e.g., a highly trusted device `Trust=85` accessing a critical router over an untrusted cellular network produces high risk `Risk=79`, without degrading the device's trust).
  - High Risk does **not** automatically mutate device trust.
- **Adaptive Decision & Smart Contract (Phase 6)**: The smart contract (`AdaptiveAccessControl.sol`) is the **authoritative decision engine**. It consumes compact normalized inputs and executes the multi-dimensional decision matrix to emit on-chain events and return `ALLOW`, `RESTRICT`, or `DENY`.

---

## 3. Technology Stack

- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 4.1.1
- **Build Tool**: Apache Maven (via Maven Wrapper `mvnw.cmd` / `mvnw`)
- **Web & REST**: Spring Web MVC, Jakarta Validation
- **Security**: Spring Security (Stateless REST security filter chain)
- **Data Persistence**: Spring Data JPA, Hibernate, MySQL Connector (`mysql-connector-j`), H2 (test-isolated)
- **Concurrency**: JPA Optimistic Locking (`@Version` on `Device`)
- **Blockchain**: Solidity 0.8.20, Ganache / EVM JSON-RPC, Web3j 4.10.3
- **Time/Clock**: Authoritative Java `Clock` bean for deterministic testability
- **Messaging**: Spring AMQP / RabbitMQ (AMQP 0-9-1)
- **Telemetry Streaming**: Spring WebSocket, STOMP Messaging
- **Offline Analytics**: Spring Batch 5 with MySQL job repository

---

## 4. Project Phases & Milestone Summary

- **Phase 1**: Baseline Architecture & REST API Foundation (Complete)
- **Phase 2**: MySQL Relational Persistence & Device Registry (Complete)
- **Phase 3**: Attribute-Based Access Control (ABAC) Gate 1 Engine (Complete)
- **Phase 4**: Behavioral Trust Scoring & History Gate 2 Engine (Complete)
- **Phase 5**: Contextual Risk Aggregation Gate 3 Engine (Complete)
- **Phase 6**: Solidity Smart Contract (`AdaptiveAccessControl.sol`) & EVM Gate 4 Integration (Complete)
- **Phase 7A**: Sensitivity-Aware IoT Resource Operation Enforcement (Complete)
- **Phase 7B**: IoT Traffic & Scenario Simulator Engine (Complete)
- **Phase 7C**: RabbitMQ Asynchronous Event Pipeline (Complete)
- **Phase 7D**: WebSocket / STOMP Real-Time Telemetry Streaming (Complete)
- **Phase 7E**: Observational Real-Time Web Dashboard (Complete)
- **Phase 7F**: Spring Batch Offline Auditing & Reconciliation Analytics (Complete)
- **Phase 8A**: Experimental Benchmarking & Workload Generation Framework (Complete)
- **Phase 8B**: Controlled Multi-Repetition Experimental Campaign ($N=720$, 31,863 Gas) (Complete)
- **Phase 8C**: Empirical Research Analysis, Mode Comparison & Thesis Tables (Complete)
- **Phase 9**: Final System Hardening, Security Sanitization & Clean Reproducibility (Complete)
- **Phase 10**: Final Thesis, Defense Presentation, Demo Script & Viva Package (Complete)

---

## 5. Comprehensive Documentation & Runbooks

- **[Final Thesis Chapters (1–12)](docs/thesis/)**: Full thesis manuscript covering introduction, literature review, architecture, implementation, methodology, empirical results, and security evaluation.
- **[Final Demonstration Script](docs/final_demo_script.md)**: 10–15 minute live defense demonstration sequence.
- **[Comprehensive Viva Q&A](docs/viva_questions_and_answers.md)**: 40+ technical questions and evidence-based answers across 20 subject areas (A–T).
- **[Final Presentation Outline](docs/final_presentation_outline.md)**: 15-slide defense presentation blueprint.
- **[Figure Catalog](docs/final_figure_catalog.md)** & **[Table Catalog](docs/final_table_catalog.md)**: Catalogs of all 10 empirical figures and 8 thesis tables.
- **[Literature Reference Audit](docs/reference_audit.md)**: Verified bibliographic audit of authentic peer-reviewed literature.
- **[Submission Checklist](docs/submission_checklist.md)** & **[Final Project Status](docs/final_project_status.md)**: Full milestone records and deliverable sign-offs.
- **[Local Reproducibility Runbook](docs/local_runbook.md)**: Complete step-by-step guide to deploying the full stack from a clean environment.
- **[Final Architecture Specification](docs/final_architecture.md)**: Detailed multi-tiered pipeline, component boundaries, and security guarantees.
- **[Security Hardening Report](docs/security_hardening_report.md)**: Audit of sanitized credentials, fail-closed mechanics, and verified properties.

---

## 6. Build, Test, and Execution

### Run Full Regression Suite
```bash
./mvnw.cmd clean test
python contracts/test_adaptive_access_control.py
python contracts/test_ganache_verification.py
python contracts/test_phase7a_enforcement.py
python contracts/test_phase7b_simulator.py
python contracts/test_phase7c_rabbitmq.py
python contracts/test_phase7d_websocket.py
python contracts/test_phase7e_dashboard.py
python contracts/test_phase7f_batch.py
python contracts/validate_phase8c.py
python contracts/final_smoke_test.py
python contracts/security_scan_final.py
```

### Package Executable JAR
```bash
./mvnw.cmd clean package -DskipTests
```

### Launch Application Server
```bash
java -jar target/trustabac-iot-0.0.1-SNAPSHOT.jar --server.port=8090
```
