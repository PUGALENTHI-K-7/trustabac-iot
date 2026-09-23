# TrustABAC-IoT: Final Presentation Outline

## Slide 1: Title & Overview
- **Title**: TrustABAC-IoT: A Gateway-Assisted Adaptive Access Control Architecture for Internet of Things Using Smart Contracts and Behavioral Trust
- **Subtitle**: Final-Year Capstone Research Project & System Implementation
- **Presenter**: Engineering Research Team
- **Affiliation**: Department of Computer Science & Engineering

---

## Slide 2: Problem Statement & Motivation
- **The IoT Security Challenge**: Massive growth of smart rental environments, dynamic multi-tenancy, and constrained microcontrollers.
- **Limitations of Static Models**:
  - RBAC suffers from role explosion.
  - Pure ABAC evaluates static attributes but ignores dynamic subject behavior and reputation decay.
  - Centralized servers represent single points of failure and mutable audit logs.
- **The Opportunity**: Integrating dynamic behavioral trust, contextual risk, and decentralized smart contracts.

---

## Slide 3: Project Objectives & Research Questions
- **Core Objectives**:
  - Build an end-to-end multi-tiered access control platform.
  - Implement tri-state decision semantics (`ALLOW`, `RESTRICT`, `DENY`).
  - Bridge resource-constrained IoT devices with Solidity smart contracts via a Spring Boot edge gateway.
- **Key Research Questions (RQs 1–6)**: Behavior, latency decomposition, trust/risk influence, reference mode divergence, security invariants, and gas consumption.

---

## Slide 4: Related Literature & Research Positioning
- **Evolution of Access Control**:
  - NIST SP 800-162 (ABAC Foundation).
  - Ouaddah et al. (FairAccess, Bitcoin Scripts).
  - Novo (2018) & Zhang et al. (2018) (Smart Contract Access Control).
  - Bernabe et al. (2019) (Centralized TACIoT).
- **Our Positioning**: An empirical, fully integrated reference implementation evaluating decomposed latency tiers, physical privilege attenuation, and offline reconciliation.

---

## Slide 5: System Architecture Overview
- **Multi-Tiered Design**:
  - **Tier 1 (IoT Clients & Physical Simulators)**: Smart door lock, thermostat, AC, TV, lights.
  - **Tier 2 (Spring Boot Edge Gateway)**: Gate 1 ABAC filter, Trust engine, Risk engine, DecisionCoordinator.
  - **Tier 3 (Blockchain Decision Engine)**: `AdaptiveAccessControl.sol` on Ganache EVM testbed.
  - **Tier 4 (Asynchronous Telemetry & Analytics)**: RabbitMQ, WebSocket STOMP, Observational Dashboard, Spring Batch.

---

## Slide 6: Authoritative Synchronous Pipeline
- **Step-by-Step Authorization Flow**:
  $$\text{Client} \longrightarrow \text{Gateway} \longrightarrow \text{Gate 1 ABAC} \longrightarrow \text{Trust} \longrightarrow \text{Risk} \longrightarrow \text{Web3j RPC} \longrightarrow \text{Solidity EVM} \longrightarrow \text{Enforcement}$$
- **Pre-Blockchain Short-Circuiting**: Gate 1 attribute failure or extreme risk ($R > 70$) terminates immediately at the gateway, avoiding blockchain transaction fees.

---

## Slide 7: Solidity Smart Contract & Decision Matrix
- **`AdaptiveAccessControl.sol`**:
  - **Hard-Deny Conditions**: Attribute failure (code 1), inactive booking (code 2), low trust ($T < 30$, code 3), severe risk ($R > 70$, code 4).
  - **ALLOW (2)**: $T \ge 70$ AND $R \le 30$ ($reasonCode = 0$).
  - **RESTRICT (1)**: $30 \le T < 70$ OR $30 < R \le 70$ ($reasonCode = 5$).
  - **Owner-Only Threshold Updates**: Protected via `onlyOwner` modifier.

---

## Slide 8: Physical Resource-Level Enforcement
- **Translating Decisions to Physical Actions**:
  - **`ALLOW` $\longrightarrow$ `EXECUTED`**: Full physical actuation (Door unlocks, thermostat sets target temp).
  - **`RESTRICT` $\longrightarrow$ `DOWNGRADED`**: Privilege attenuation (Door remains locked but read status permitted; thermostat temperature clamped to $20.0^\circ\text{C}$--$24.0^\circ\text{C}$).
  - **`DENY` $\longrightarrow$ `BLOCKED`**: Zero device mutation.

---

## Slide 9: Experimental Methodology
- **Rigorous Evaluation Protocol**:
  - 8 Scenario archetypes across operational and adversarial conditions.
  - $R = 3$ repetitions with structured PRNG seed formula ($seed = r \times 1000 + s \times 100 + 42$, Base seed 42).
  - 10 unmeasured warm-up requests + 30 measured requests per repetition.
  - Total primary sample: **720 measured requests** (960 total raw requests).
  - Pre-repetition REST state resets for baseline consistency.

---

## Slide 10: Empirical Results: Latency Decomposition
- **Key Measured Results ($N = 90$ per scenario)**:
  - `NORMAL_ACCESS`: Auth Latency = $61.217 \pm 2.258$ ms ($CI_{95} = [56.730, 65.704]$ ms); Enforce = $72.020 \pm 2.657$ ms.
  - `ABAC_FAILURE`: Auth Latency = $34.814 \pm 0.699$ ms ($43.13\%$ faster due to pre-EVM short-circuiting).
  - `BLOCKCHAIN_OUTAGE`: Fast-fail rejection in $0.100$ ms.
- **Throughput**: Sequential client throughput of $13.98$--$24.54$ ops/sec.

---

## Slide 11: Counterfactual Analysis (Modes A, B, and C)
- **Mode A (Pure ABAC)**: Diverged on **56.81%** of requests (311/720 agreement). Failed to block compromised low-trust users or high-risk environments.
- **Mode B (Centralized T-ABAC)**: Matched policy decisions **100.0%** (720/720 agreement), confirming algorithmic equivalence, but lacked decentralized cryptographic audit proofs.
- **Mode C (TrustABAC-IoT)**: Combined full adaptive policy gating with immutable on-chain event receipts.

---

## Slide 12: Security Evaluation & Fault Tolerance
- **Security Invariant Verification**:
  - **100% Fail-Closed Outage Isolation**: Zero operations permitted during blockchain disconnection.
  - **100% Anti-Double-Authorization**: Exactly 1 evaluation and $\le 1$ blockchain tx per request.
  - **100% Gate 1 Short-Circuiting**: Expired bookings and unregistered devices rejected with zero gas spent.
- **Secret Sanitization**: 282 files scanned $\longrightarrow$ **0 secret findings**.

---

## Slide 13: Blockchain Gas Consumption & Economics
- **Deterministic Execution**:
  - Every on-chain evaluation consumed exactly **31,863 gas** ($0.00063726$ ETH @ 20 Gwei).
  - 421 total on-chain transactions across 720 requests ($13,414,323$ total gas).
  - **299 requests filtered pre-EVM** $\longrightarrow$ Substantial gas savings for rejected access attempts.

---

## Slide 14: System Limitations
- Software-simulated IoT device state models (no physical MCU deployment).
- Absence of physical hardware battery energy measurements.
- Local Ganache private EVM testbed with instant mining (public network scalability not claimed).
- Single edge gateway oracle boundary.
- Sequential benchmarking workload (not maximum concurrent capacity).

---

## Slide 15: Conclusion, Future Work & Q&A
- **Conclusion**: Demonstrated a fully integrated, empirically validated adaptive access control platform combining ABAC, trust, risk, and smart contracts.
- **Future Directions**: Physical ESP32 microcontrollers, hardware power meters, multi-gateway BFT consensus, Layer-2 rollup deployment.
- **Questions & Discussion**: Open for defense committee examination.
