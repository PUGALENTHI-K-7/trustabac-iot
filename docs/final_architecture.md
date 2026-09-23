# TrustABAC-IoT: Comprehensive Final Architecture Specification

## 1. System Architecture Overview

TrustABAC-IoT implements an adaptive, multi-tiered zero-trust access control architecture designed for smart-hospitality and edge IoT environments. It integrates **Attribute-Based Access Control (ABAC)**, **Behavioral Trust Scoring**, and **Contextual Environmental Risk Analysis** with an **On-Chain Ethereum Smart Contract** for decentralized consensus, policy enforcement, and non-repudiable audit trails.

```
                                 [ Client / Edge Request ]
                                             |
                                             v
               =============================================================
               |             TRUSTABAC-IOT APPLICATION GATEWAY            |
               =============================================================
               |  [ Gate 1: ABAC Attribute Validator ]                     |
               |    - Validates User Identity, Role & Active Reservation   |
               |    - Short-circuits invalid attributes (0 gas)            |
               |                                                           |
               |  [ Gate 2: Behavioral Trust Engine ]                      |
               |    - Computes dynamic trust score T in [0.0, 100.0]        |
               |    - Historical violation decay & success incentives      |
               |                                                           |
               |  [ Gate 3: Contextual Risk Engine ]                       |
               |    - Multi-factor risk aggregation R in [0.0, 100.0]      |
               |    - Pre-blockchain anomaly gate (>70.0 short-circuit)    |
               |                                                           |
               |  [ Gate 4: On-Chain Smart Contract Evaluator ]            |
               |    - Web3j RPC call -> AdaptiveAccessControl.sol (EVM)    |
               |    - Returns: ALLOW / RESTRICT / DENY                     |
               =============================================================
                                             |
                                             v
               =============================================================
               |               RESOURCE OPERATION SERVICE                  |
               =============================================================
               |  - ALLOW    -> EXECUTED   (Full operation on IoT device)   |
               |  - RESTRICT -> DOWNGRADED (Privilege attenuation/clamping)|
               |  - DENY     -> BLOCKED    (Zero state mutation)           |
               =============================================================
                        |                                       |
                        v                                       v
         [ In-Memory IoT Device Simulator ]        [ RabbitMQ Event Bus ]
         - Thermostat setpoints                    - Asynchronous audit pipeline
         - Air conditioner modes                   - STOMP WebSocket telemetry
         - Smart TV & Lighting                     - Spring Batch Offline Analytics
         - Electronic door locks                   - Observational Dashboard
```

---

## 2. Multi-Tiered Evaluation Pipeline & Decision Matrix

1. **Gate 1: ABAC Attribute Verification**:
   - Evaluates subject attributes (e.g. `guest-user-001`), resource identifiers (e.g. `THERMOSTAT-001`), and environmental booking contexts (`BOOKING-PHASE6B-001`).
   - If attributes are invalid or expired, the request is immediately rejected (`DENY` $\to$ `BLOCKED`) without invoking downstream trust, risk, or blockchain layers.
2. **Gate 2: Behavioral Trust Scoring**:
   - Maintains an append-only historical reputation score $T \in [0.0, 100.0]$.
   - Trust scores decay upon confirmed violations (suspicious burst requests, unauthorized resource probing) and recover upon repeated legitimate interactions.
3. **Gate 3: Contextual Environmental Risk Aggregation**:
   - Aggregates multi-dimensional risk factors $R \in [0.0, 100.0]$: Time of access, Geographic location mismatch, Network security level, Request burst frequency, and Historical violation counts.
   - Severe anomalies ($R > 70.0$) are rejected at the gateway level before EVM execution to conserve blockchain gas.
4. **Gate 4: Solidity Smart Contract (`AdaptiveAccessControl.sol`)**:
   - Implements the authoritative policy decision matrix on the EVM:
     - **ALLOW**: $T \ge 70.0 \land R \le 30.0$
     - **RESTRICT**: $(T \ge 70.0 \land 30.0 < R \le 70.0) \lor (30.0 \le T < 70.0 \land R \le 70.0)$
     - **DENY**: $T < 30.0 \lor R > 70.0$

---

## 3. Dynamic Privilege Attenuation (Downgrading)

When the policy evaluates to `RESTRICT`, the `ResourceOperationService` enforces dynamic operation downgrading:
- **Smart Thermostat**: Temperature setpoint adjustments are clamped within safety bounds ($20^\circ\text{C}$--$24^\circ\text{C}$).
- **Smart TV / Media**: Audio volume changes are capped ($\le 30\%$).
- **Smart Lighting**: Brightness levels are capped ($\le 50\%$).
- **Smart Door Lock**: State modifications are downgraded to secondary confirmation requirements.

---

## 4. Architectural Boundaries & Component Roles

| Subsystem | Architectural Role | Authority Level | Boundary & Security Guarantees |
| :--- | :--- | :--- | :--- |
| **Spring Boot Gateway** | Ingress, orchestration & short-circuiting | Gateway Filter | Executes Gate 1--3; short-circuits unauthorized requests; routes to EVM. |
| **Solidity Smart Contract** | Authoritative access decision engine | Authoritative | Immutable on-chain decision matrix; produces cryptographic audit events. |
| **ResourceOperationService**| Physical & simulated actuator enforcement | Enforcer | Sole authorized execution engine for device operations; enforces downgrades. |
| **RabbitMQ Event Bus** | Asynchronous transport & event decoupling | Transport Only | Transports audit envelopes; strictly prohibited from executing authorization logic. |
| **WebSocket / STOMP** | Real-time browser telemetry streaming | Observational | Streams live events to UI; client JavaScript is strictly presentation-only. |
| **Web Dashboard** | Human operator visibility & telemetry UI | Observational | Zero decision authority; cannot mutate trust, risk, or device states directly. |
| **Spring Batch** | Historical auditing & period summaries | Offline / Auditing | Read-only with respect to operational authorization; idempotent periodic processing. |

---

## 5. Fail-Closed Resilience Invariant

In the event of an EVM node disconnection, RPC timeout, or blockchain unavailability:
- The gateway immediately intercepts the failure and enforces a strict **fail-closed security denial** (`DENY` $\to$ `BLOCKED`).
- No cached or stale permissions are permitted for sensitive device modifications.
- Normal blockchain-backed authorization resumes seamlessly upon node reconnection.
