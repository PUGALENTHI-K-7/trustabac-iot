# TrustABAC-IoT: Security Hardening & Audit Report

## 1. Executive Summary

This security report documents the hardening measures, credential cleanup, authorization path audits, fail-closed verification, and operational boundaries implemented in **TrustABAC-IoT (Phase 9)**.

---

## 2. Credential & Secret Sanitization

A repository-wide audit was conducted across all source code, configuration files, deployment scripts, test harnesses, and documentation:
1. **Zero Hardcoded Private Keys**: All default 64-character hexadecimal private keys (`0x4f3edf...`) have been purged from `application.properties`, test properties, batch scripts, and test asserts. Ephemeral key pairs are dynamically generated in-memory via `org.web3j.crypto.Keys.createEcKeyPair()` when no environment key is supplied.
2. **Environment-Variable Decoupling**: All database credentials, message broker accounts, and blockchain RPC parameters are bound to standardized environment variables (`DB_PASSWORD`, `RABBITMQ_PASSWORD`, `BLOCKCHAIN_PRIVATE_KEY`, etc.).
3. **Template & Git Hygiene**: `.env.example` provides non-secret placeholder definitions. `.gitignore` strictly excludes `.env`, `*.env`, `*.log`, `logs/`, and temporary runtime files.

---

## 3. Authorization Path Verification

An end-to-end trace confirmed that **100% of protected IoT device operations** route through the authoritative multi-tiered pipeline:

$$\text{Client} \longrightarrow \text{Gateway} \longrightarrow \text{ABAC (Gate 1)} \longrightarrow \text{Trust (Gate 2)} \longrightarrow \text{Risk (Gate 3)} \longrightarrow \text{Solidity EVM (Gate 4)} \longrightarrow \text{ResourceOperationService} \longrightarrow \text{Actuator}$$

- **Zero Bypass Endpoints**: No REST controller, service method, or messaging consumer bypasses the `DecisionCoordinator`.
- **Enforcement Integrity**: The `ResourceOperationService` remains the sole authorized executor of device operations, translating `ALLOW` to full execution, `RESTRICT` to bounded operation downgrading, and `DENY` to zero-mutation blocks.

---

## 4. Subsystem Boundary Audits

1. **Web Dashboard Boundary**:
   - The web dashboard (`/dashboard`) is strictly observational.
   - Client-side JavaScript is presentation logic only; it contains zero decision formulas, zero trust/risk logic, and zero embedded secrets.
2. **RabbitMQ Messaging Boundary**:
   - The message bus operates strictly as an asynchronous transport layer for audit records and STOMP telemetry.
   - Consumers do not perform authorization evaluations.
3. **Spring Batch Analytics Boundary**:
   - Batch jobs read historical operational records and write aggregate summaries.
   - Batch processing is read-only with respect to operational devices, bookings, and live authorization states, guaranteeing that offline auditing never mutates real-time security state.

---

## 5. Verified Security Properties & Invariants

| Security Property | Tested Condition | Verified Operational Behavior | Result |
| :--- | :--- | :--- | :--- |
| **Fail-Closed Availability** | Blockchain RPC node unreachable | Immediate gateway fail-closed denial ($0.1$ ms), 0 device mutations | VERIFIED |
| **Attribute Isolation** | Missing or expired booking reservation | Gate 1 ABAC short-circuit rejection ($34.8$ ms), 0 blockchain gas | VERIFIED |
| **Reputation Enforcement**| Degraded behavioral trust score ($<30.0$) | Authoritative smart contract rejection ($59.4$ ms), 31,863 gas | VERIFIED |
| **Contextual Risk Shield** | Severe anomaly / burst frequency ($>70.0$) | Gateway pre-EVM Risk Gate denial ($43.9$ ms), 0 blockchain gas | VERIFIED |
| **Privilege Attenuation** | Moderate environmental risk ($30.0 < R \le 70.0$) | Smart contract RESTRICT $\to$ Parameter clamping / Downgrading | VERIFIED |
| **Anti-Double-Auth** | 720 measured requests in Phase 8B | Exactly 1 evaluation and $\le 1$ on-chain transaction per request | VERIFIED |

---

## 6. Assumptions & Operational Limitations

1. **Local EVM Testing Environment**: Blockchain validation and gas measurements ($31,863$ gas) were conducted on local Ganache EVM. On public multi-node networks, block mining latency ($12$s) and gas fee volatility apply.
2. **Software-Simulated Hardware**: Device actuators were simulated in-memory via `DeviceSimulatorService`. Physical microcontroller execution constraints and wireless bus delays were not evaluated.
3. **Gateway Trust Assumption**: The architecture assumes the Spring Boot gateway instance executes within a trusted perimeter; pre-blockchain short-circuiting relies on gateway integrity.
