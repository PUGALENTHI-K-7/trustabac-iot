# Chapter 1 — Introduction

## 1.1 The IoT Access Control Challenge
The rapid proliferation of Internet of Things (IoT) devices in smart environments—such as smart residential apartments, short-term rental properties, and intelligent buildings—has introduced significant security and access control challenges. Unlike conventional IT infrastructures characterized by high computational capacity, centralized perimeter defenses, and static user directories, modern IoT ecosystems exhibit distinct operational constraints:

1. **Extreme Resource Constraints**: Microcontrollers and peripheral sensors possess limited processing capability, constrained RAM, and minimal storage, precluding the execution of heavy cryptographic routines or complex policy evaluation engines on-device.
2. **Dynamic Multi-Tenancy**: In short-term rental environments, subject identities and tenant authorizations change frequently, requiring rapid policy reassignment and strict temporal bounding (e.g., check-in and check-out intervals).
3. **Vulnerability to Physical and Environmental Compromise**: IoT devices deployed in shared or semi-public spaces are susceptible to tampering, contextual anomalies, and intermittent network connectivity.

## 1.2 Limitations of Traditional Access Control Models
Traditional access control paradigms fall short in dynamic IoT environments:
- **Role-Based Access Control (RBAC)** assigns permissions statically based on predefined roles. RBAC suffers from "role explosion" when applied to dynamic environments where access depends on contextual variables such as time, location, network subnet, and device state.
- **Pure Attribute-Based Access Control (ABAC)** evaluates rich contextual and environmental attributes (NIST SP 800-162) to determine access eligibility. However, standard ABAC is inherently static regarding subject behavior: if an entity possesses valid attributes (e.g., an active booking token), access is granted unconditionally, even if the entity exhibits anomalous request rates, degraded historical reliability, or suspicious contextual telemetry.
- **Binary Decision Limitations**: Standard authorization engines return binary outcomes (`ALLOW` or `DENY`). In IoT scenarios, binary decisions fail to handle intermediate risk states—such as a legitimate tenant attempting device configuration changes over an insecure public network—where access attenuation (`RESTRICT` / parameter clamping) is preferable to abrupt denial.

## 1.3 Adaptive Authorization: Integrating Behavioral Trust and Contextual Risk
To address these limitations, adaptive access control models augment attribute evaluation with two orthogonal dynamic dimensions:
1. **Behavioral Trust (Historical Subject Reliability)**: A quantitative, long-term metric computed from an entity's historical compliance and operational violations (e.g., tamper attempts, unauthorized resource probes, frequency flooding).
2. **Contextual Risk (Instantaneous Request Danger)**: A real-time, stateless evaluation of environmental risk factors surrounding an individual request (e.g., sensitive resource classification, network transport origin, off-peak timing, burst request volume).

By decoupling long-term behavioral trust from instantaneous contextual risk, access decisions adapt dynamically:
- High trust + Low risk $\longrightarrow$ Unrestricted access (`ALLOW`).
- Moderate trust or Moderate risk $\longrightarrow$ Attenuated / privilege-clamped access (`RESTRICT`).
- Low trust, Severe risk, or Attribute mismatch $\longrightarrow$ Strict denial (`DENY`).

## 1.4 The Role of Blockchain and Smart Contracts
Centralized authorization servers represent single points of failure (SPOF) and single points of trust, susceptible to insider manipulation and repudiation of access logs. Incorporating blockchain technology and deterministic Ethereum Virtual Machine (EVM) smart contracts offers:
- **Authoritative, Tamper-Proof Decision Logic**: Smart contracts enforce immutable policy evaluation rules across all participating stakeholders without reliance on a single administrator.
- **Cryptographic Auditability and Non-Repudiation**: Every evaluated authorization generates an immutable, timestamped on-chain event receipt containing cryptographic transaction proofs.
- **Gateway-Assisted Architecture**: By placing a high-capacity Spring Boot edge gateway between resource-constrained IoT devices and the blockchain, heavy cryptographic operations, ABAC attribute evaluation, and trust/risk aggregations are offloaded from IoT microcontrollers while preserving on-chain decision authority.

## 1.5 Research Problem and Objectives
While conceptual models combining access control, trust, and blockchain exist in the literature, there remains a critical gap in fully integrated, empirically validated reference implementations that evaluate multi-tiered gateway-assisted architectures under controlled experimental conditions.

### Primary Objectives:
1. **Design and Implement** an end-to-end adaptive access control platform (**TrustABAC-IoT**) integrating Spring Boot, ABAC policy gating, historical trust evaluation, contextual risk calculation, and an authoritative Solidity smart contract deployed on a local Ganache EVM testbed.
2. **Develop Resource-Level Enforcement Mechanisms** that actuate simulated device operations according to tri-state decision semantics (`ALLOW` $\to$ `EXECUTED`, `RESTRICT` $\to$ `DOWNGRADED`, `DENY` $\to$ `BLOCKED`).
3. **Construct an Empirical Benchmarking Framework** to measure authorization latency, enforcement overhead, client end-to-end duration, sequential throughput, and blockchain gas consumption across controlled operational and adversarial scenarios.
4. **Conduct Rigorous Statistical Evaluation** using frozen experimental datasets, multi-repetition confidence intervals, and counterfactual reference mode comparisons.

## 1.6 Research Questions (RQs)
- **RQ1**: How does the adaptive authorization pipeline behave across normal, restricted, low-trust, high-risk, attribute-failure, outage, recovery, and mixed workloads?
- **RQ2**: How do authorization, enforcement, and client end-to-end latency vary across the architectural tiers?
- **RQ3**: How do behavioral Trust and contextual Risk scores influence policy decisions and resource actuation?
- **RQ4**: What behavioral differences are observed between pure ABAC (Mode A), centralized T-ABAC (Mode B), and authoritative blockchain-backed execution (Mode C)?
- **RQ5**: Does the implementation preserve security invariants (fail-closed outage rejection, privilege attenuation, Gate 1 short-circuiting) under adversarial workloads?
- **RQ6**: What blockchain transaction characteristics and gas costs are observed under local EVM evaluation?

## 1.7 Thesis Contributions and Scope Limitations
### Key Contributions:
- A complete, working, gateway-assisted IoT authorization architecture integrating Spring Boot, MySQL, RabbitMQ, WebSocket telemetry, and Solidity smart contracts.
- An empirical experimental evaluation comprising 720 measured requests across 8 controlled scenarios with Student's t 95% confidence intervals.
- An offline analytical auditing subsystem built on Spring Batch for idempotent historical reconciliation.

### Explicit Scope Limitations:
- The experimental testbed was evaluated on a local Ganache EVM testnet (Chain ID 1337) with single-node instant mining; public network scalability is not claimed.
- IoT devices were simulated using high-fidelity in-memory state models rather than physical microcontrollers; hardware energy consumption was not measured.
- The platform relies on a trusted edge gateway oracle to compute and forward attributes to the smart contract.
