# Chapter 4 — System Requirements and Threat Model

## 4.1 Functional Requirements

### FR-01: Device and Subject Registration
- The system shall maintain an authenticated registry of IoT devices categorized by resource types (`SMART_DOOR_LOCK`, `SMART_THERMOSTAT`, `SMART_LIGHT`, `SMART_TV`, `SECURITY_CAMERA`, `ROUTER_ADMIN`, `OWNER_SETTINGS`) and sensitivity levels (`CRITICAL`, `HIGH`, `MEDIUM`, `LOW`).
- The system shall register subject profiles (Tenants, Guests, Maintenance Staff, Property Owners) with cryptographic credentials and attribute sets.

### FR-02: Booking-Aware Temporal Authorization
- The system shall validate subject requests against active reservation records (`Booking` entity), enforcing strict check-in and check-out temporal boundaries.

### FR-03: Multi-Tiered Attribute Evaluation (Gate 1 ABAC)
- The gateway shall evaluate request attributes (subject role, assigned property, device ID, operation type, booking validity) prior to invocation of downstream trust, risk, or blockchain services.

### FR-04: Dynamic Behavioral Trust Management
- The system shall compute continuous trust scores ($0.0 \le T \le 100.0$) based on historical interaction compliance, penalizing subjects for security violations and rewarding sustained compliant behavior.

### FR-05: Real-Time Contextual Risk Assessment
- The system shall evaluate stateless contextual risk ($0.0 \le R \le 100.0$) incorporating network origin (Internal WiFi vs. External Cellular/Public), temporal factors (Peak vs. Off-peak), resource sensitivity, and recent request burst frequency.

### FR-06: Authoritative Smart Contract Decision Making
- The Solidity smart contract (`AdaptiveAccessControl.sol`) shall evaluate `(abacPass, bookingActive, sensitivity, operation, trustScore, riskScore)` and output a deterministic tri-state decision (`ALLOW`, `RESTRICT`, `DENY`).

### FR-07: Resource-Level Operation Enforcement
- `ResourceOperationService` shall execute device state mutations according to the decision:
  - `ALLOW` $\longrightarrow$ Full execution (`EXECUTED`).
  - `RESTRICT` $\longrightarrow$ Clamped/downgraded execution (`DOWNGRADED`, e.g., door control clamped to telemetry read, thermostat clamped to eco-temperature range).
  - `DENY` $\longrightarrow$ Complete blocking (`BLOCKED`).

### FR-08: Asynchronous Telemetry and Event Publication
- The gateway shall publish non-blocking AMQP domain events to RabbitMQ topic exchanges and stream real-time telemetry over WebSocket STOMP topics (`/topic/devices`, `/topic/trust`, `/topic/risk`, `/topic/authorization`, `/topic/blockchain`, `/topic/simulator`, `/topic/security`).

### FR-09: Idempotent Offline Analytics and Auditing
- A Spring Batch subsystem shall periodically aggregate historical access logs, reconciling operational records against on-chain audit proofs without modifying runtime state or double-counting idempotency keys.

---

## 4.2 Security Requirements and Non-Functional Properties

### SR-01: Fail-Closed Security Invariant
- If the blockchain node is unreachable, the RPC call times out, or the EVM transaction reverts, the system shall strictly default to a **Fail-Closed `DENY`** outcome, preventing unauthorized device actuation during infrastructure failures.

### SR-02: Pre-Blockchain Short-Circuit Isolation
- Requests failing basic ABAC attribute validation (Gate 1) or exhibiting extreme contextual risk ($> 70.0$) shall be terminated at the gateway tier, preventing unnecessary gas expenditure on the blockchain testbed.

### SR-03: Cryptographic Auditability and Non-Repudiation
- Every authorization evaluated on-chain shall emit an immutable `AuthorizationEvaluated` event containing the transaction hash, block number, subject ID, device ID, evaluated scores, and resulting decision.

### SR-04: Non-Authoritative Telemetry Boundary
- The RabbitMQ event pipeline, WebSocket streaming layers, and Web Monitoring Dashboard shall be strictly observational and incapable of altering authorization outcomes or bypassing policy evaluation.

### SR-05: Zero Hardcoded Secret Invariant
- Source code, properties files, Docker files, and test scripts must be free of embedded private keys, plaintext database passwords, or persistent cryptographic credentials.

---

## 4.3 Threat Model and Attack Vectors

The TrustABAC-IoT threat model identifies key adversaries operating in a shared smart-rental environment:

| Threat Identifier | Adversary Archetype | Threat Vector / Attack Mechanism | System Defense / Mitigating Component |
| :--- | :--- | :--- | :--- |
| **T-01: Unauthorized Subject Access** | Unauthenticated outsider or unregistered guest | Attempting device operations without valid user credentials or without a registered device mapping. | **Gate 1 ABAC Filter**: Rejects unmapped subjects/devices prior to trust or blockchain processing. |
| **T-02: Temporal Boundary Violation** | Expired tenant (Post-checkout) or early arriving guest (Pre-checkin) | Attempting access before reservation check-in timestamp or after checkout expiration. | **Booking-Aware Gate**: Queries database temporal bounds; rejects access with `DENY` / `BLOCKED`. |
| **T-03: Compromised / Malicious Tenant (Low Trust)** | Valid guest exhibiting hostile behavior | Executing physical tamper attempts, unauthorized door unlock bursts, or security violation events. | **Behavioral Trust Engine**: Degrades trust score below threshold ($< 30.0$), triggering on-chain `DENY`. |
| **T-04: Contextual Anomaly / Burst Attack (High Risk)** | Remote adversary using stolen session tokens | Initiating rapid-fire control requests or connecting from suspicious, unverified external cellular networks. | **Contextual Risk Engine**: Computes risk score $> 70.0$; triggers pre-EVM short-circuit denial, conserving gas. |
| **T-05: Privilege Escalation on Sensitive Resources** | Legitimate tenant attempting owner-only controls | Guest user attempting access to security cameras, router administration, or property owner settings. | **Attribute & Sensitivity Matrix**: Rejects unauthorized access levels at ABAC Gate and smart contract. |
| **T-06: Intermediate Risk Operation Exposure** | Legitimate tenant on public network | Controlling high-sensitivity appliances (door locks, heating) over untrusted external networks. | **Operation Downgrading (`RESTRICT`)**: Clamps control operations to safe status reads or eco-bounds. |
| **T-07: Blockchain Infrastructure Outage** | Network partitions, RPC disconnects, miner failure | Blockchain testbed node becomes unreachable during access evaluation. | **Fail-Closed Handler**: `DecisionCoordinator` intercepts exception and enforces immediate `DENY` / `BLOCKED`. |
| **T-08: Double-Authorization / Transaction Replay** | Malicious replay of authorization requests | Resubmitting prior request tokens or injecting duplicate AMQP frames. | **IdempotencyGuard & Unique DB Constraints**: Enforces single transaction per request and prevents duplicate processing. |

### Explicitly Excluded Threat Scope:
- Physical hardware side-channel attacks on physical silicon (DPA/EMA).
- Distributed 51% consensus attacks on public Ethereum mainnets (evaluated on Ganache private EVM).
- Compromise of the Spring Boot gateway operating system root kernel.
