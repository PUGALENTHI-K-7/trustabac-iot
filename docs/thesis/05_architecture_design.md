# Chapter 5 — System Architecture and Design

## 5.1 Architectural Overview
The TrustABAC-IoT platform employs a **gateway-assisted, multi-tiered architecture** designed to bridge resource-constrained IoT devices with decentralized Ethereum smart contracts. The architecture is organized into distinct execution paths:

1. **Authoritative Synchronous Path**: Enforces access control policies and executes simulated device state actuations.
2. **Asynchronous Telemetry Path**: Dispatches non-blocking domain events to message brokers and streaming subscribers.
3. **Observational Presentation Path**: Displays real-time system metrics, device states, and security logs without participating in authorization decisions.
4. **Offline Analytical Path**: Periodically aggregates, audits, and reconciles operational history against on-chain transaction proofs.

```
                              [ IoT Client / Tenant App ]
                                           │
                                           ▼ (REST HTTPS / JSON)
 ══════════════════════════════════════════════════════════════════════════════════════════════════
                            SPRING BOOT EDGE GATEWAY (Port 8090)
 ══════════════════════════════════════════════════════════════════════════════════════════════════
                                           │
                                           ▼
                                 [ ResourceOperationController ]
                                           │
                                           ▼
                                 [ ResourceOperationService ]
                                           │
                                           ▼
                                   [ AbacService ] ──(FAIL)──► [ Return DENY / BLOCKED ]
                                           │ (PASS)
                                           ▼
                                   [ TrustService ] ◄──► [ MySQL: trust_history ]
                                           │
                                           ▼
                                    [ RiskService ] ◄──► [ Context & Request Burst Cache ]
                                           │
                                           ▼
                                [ DecisionCoordinator ]
                                           │
                                           ▼ (Web3j JSON-RPC)
 ══════════════════════════════════════════════════════════════════════════════════════════════════
                        BLOCKCHAIN TESTBED (Ganache EVM Port 8545)
 ══════════════════════════════════════════════════════════════════════════════════════════════════
                                           │
                                 [ AdaptiveAccessControl.sol ]
                                 (Owner-only thresholds: 70/30)
                                           │
                                           ▼ (EVM Log Event: AuthorizationEvaluated)
                                [ Transaction Receipt ]
 ══════════════════════════════════════════════════════════════════════════════════════════════════
                                           │
                                           ▼ (ALLOW / RESTRICT / DENY)
                                [ ResourceOperationService ]
                                           │
                      ┌────────────────────┼────────────────────┐
                      ▼                    ▼                    ▼
               [ ALLOW ]             [ RESTRICT ]             [ DENY ]
             Full Actuation         Safe Downgrade         Zero Actuation
             (EXECUTED)             (DOWNGRADED)            (BLOCKED)
                      │                    │                    │
                      └────────────────────┼────────────────────┘
                                           │
                                           ▼
                               [ IoT Simulator Engine ]
                               (In-memory Device Models)
                                           │
                                           ▼
                            [ Asynchronous Event Publisher ]
                                           │
                 ┌─────────────────────────┴─────────────────────────┐
                 ▼ (AMQP 0-9-1)                                      ▼ (STOMP / SockJS)
        [ RabbitMQ Message Broker ]                              [ WebSocket Server ]
        Exchange: trustabac.events                               Topics: /topic/*
                 │                                                   │
                 ▼                                                   ▼
     [ Risk / Audit Consumers ]                              [ Observational Dashboard ]
                 │
                 ▼
        [ MySQL Relational DB ]
        Tables: access_requests,
        trust_history, risk_events,
        authorization_audit
                 │
                 ▼
     [ Spring Batch Offline Engine ]
     (Idempotent Multi-Period Audits)
```

---

## 5.2 Authoritative Synchronous Authorization Pipeline
Every protected IoT resource operation follows a strict, single execution path:

1. **Client Request**: The client issues an HTTP POST request to `/api/resources/operations/execute` containing `userId`, `deviceId`, `resourceType`, `operationType`, and `parameters`.
2. **Gate 1 — ABAC Attribute Evaluation (`AbacService`)**:
   - Queries `DeviceRepository` and `BookingRepository`.
   - Validates that the subject is active, the device is registered, the requested operation is mapped, and the temporal booking is currently valid.
   - If ABAC evaluation fails, execution **terminates immediately** with `DENY` / `BLOCKED`. Downstream trust calculation, risk evaluation, and blockchain transactions are bypassed, conserving computational resources and gas.
3. **Behavioral Trust Aggregation (`TrustService`)**:
   - Retrieves the subject's historical compliance records from MySQL.
   - Computes the normalized trust score ($0.0 \le T \le 100.0$).
4. **Contextual Risk Calculation (`RiskService`)**:
   - Analyzes request context: network location, time of day, resource sensitivity classification (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`), and request burst frequency.
   - Computes the contextual risk score ($0.0 \le R \le 100.0$).
   - If risk exceeds the hard critical threshold ($R > 70.0$), execution short-circuits to `DENY` / `BLOCKED`.
5. **On-Chain Policy Decision (`DecisionCoordinator` & `BlockchainService`)**:
   - Encodes parameters and invokes `evaluateAccess(abacPass, bookingActive, sensitivity, operation, trustScore, riskScore)` on `AdaptiveAccessControl.sol` via Web3j.
   - Ganache executes the deterministic decision logic and emits an `AuthorizationEvaluated` event.
   - Web3j captures the transaction receipt, block number, and return enum (`ALLOW = 2`, `RESTRICT = 1`, `DENY = 0`).
   - If an RPC failure or EVM timeout occurs, `DecisionCoordinator` enforces **Fail-Closed `DENY`**.
6. **Resource-Level Operation Enforcement (`ResourceOperationService`)**:
   - `ALLOW` $\longrightarrow$ Actuates full physical command on the target device model. Returns `EXECUTED`.
   - `RESTRICT` $\longrightarrow$ Evaluates safe parameter bounds (e.g., clamp AC temperature, convert door unlock to telemetry read). Returns `DOWNGRADED`.
   - `DENY` $\longrightarrow$ Discards operation parameters; device state remains untouched. Returns `BLOCKED`.
7. **Audit Record Persistence**: The complete transaction metadata, decision, enforcement status, and on-chain transaction hash are written to the MySQL `access_requests` table.

---

## 5.3 Asynchronous Domain Messaging Tier (RabbitMQ)
To decouple operational logging, security anomaly detection, and analytics from the low-latency synchronous authorization loop, TrustABAC-IoT integrates a RabbitMQ AMQP messaging pipeline:

- **Exchange**: `trustabac.events` (Topic Exchange).
- **Queues & Routing Keys**:
  - `trustabac.device.events` $\longleftarrow$ `device.operation` (Device state transitions).
  - `trustabac.trust.events` $\longleftarrow$ `trust.event` (Trust penalty and reward events).
  - `trustabac.risk.events` $\longleftarrow$ `risk.context` (Contextual anomaly records).
  - `trustabac.authorization.events` $\longleftarrow$ `authorization.result` (On-chain authorization results).
  - `trustabac.dlq` $\longleftarrow$ `dlq.events` (Dead letter queue for unparseable payloads).
- **Idempotency Guarantee**: `IdempotencyGuard` tracks message UUIDs to enforce at-most-once processing semantics across consumers.
- **Architectural Boundary**: RabbitMQ consumers are strictly observational and cannot modify in-flight authorization decisions.

---

## 5.4 Real-Time Streaming and Observational Web Dashboard
- **WebSocket STOMP Broker**: An embedded Spring WebSocket broker broadcasts real-time telemetry over `/ws` to `/topic/*` channels.
- **Observational Web Dashboard**: A single-page application (`/dashboard`) built with HTML5, CSS3, and vanilla JavaScript connects via STOMP/SockJS.
- **Observational Constraint**: The dashboard contains **zero client-side authorization logic** and zero embedded private keys. All metrics and device states reflect backend server authority.

---

## 5.5 Offline Analytical Subsystem (Spring Batch)
- **Role**: Provides periodic, reproducible, multi-party auditing of persisted access history without impacting operational authorization performance.
- **Idempotent Execution**: Chunk-oriented processing (Reader $\to$ Processor $\to$ Writer) keyed on `(periodKey, periodStart, periodEnd)`. Re-running an identical analytical period returns the existing summary without double-counting records.
- **Source Reconciliation**: Reconciles total request counts, `ALLOW`/`RESTRICT`/`DENY` decisions, and matched on-chain transaction proofs against raw MySQL tables.
