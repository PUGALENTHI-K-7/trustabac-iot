# Chapter 3 — Problem Statement, Objectives, and Research Questions

## 3.1 Problem Statement
In smart rental environments and multi-tenant IoT ecosystems, access control systems face a fundamental tension between security expressiveness, operational agility, and decentralized trust:

1. **Static Attribute Limitations**: Standard Attribute-Based Access Control (ABAC) models evaluate static attributes (such as tenant identity, role, and reservation timeframe) but remain blind to dynamic subject behavior. A malicious tenant with a valid reservation token can execute repeated brute-force lock attempts, unauthorized administrative scans, or high-frequency request bursts without policy intervention until manual revocation occurs.
2. **Binary Decision Rigidity**: Traditional authorization systems produce binary decisions (`ALLOW` or `DENY`). When contextual risk is moderate—such as accessing sensitive climate or security parameters over a mobile network—binary denial disrupts legitimate tenant convenience, while unconditional access exposes the infrastructure to risk.
3. **Centralized Authority Vulnerabilities**: Conventional access management relies on centralized servers or cloud endpoints that create single points of failure, single points of compromise, and mutable audit logs prone to insider manipulation or post-incident repudiation.
4. **Computational Constraints of IoT Devices**: Resource-constrained microcontrollers cannot directly maintain blockchain nodes, process cryptographic proof verifications, or evaluate heavy multi-attribute access policies.

## 3.2 Project Objectives
To resolve these challenges, the **TrustABAC-IoT** project defines the following primary engineering and research objectives:

1. **Architectural Design**: Formulate a multi-tiered, gateway-assisted access control architecture where high-capacity edge gateways perform heavy attribute, trust, and risk aggregations while delegating final authorization authority to an immutable smart contract.
2. **Multi-Dimensional Policy Integration**:
   - Integrate deterministic **ABAC rules** as an initial eligibility gate.
   - Maintain dynamic **Behavioral Trust** scores reflecting historical subject compliance and violation history.
   - Evaluate instantaneous **Contextual Risk** scores incorporating resource criticality, network transport, timing, and request burst rates.
3. **Smart Contract Decision Authority**: Implement an authoritative Solidity smart contract (`AdaptiveAccessControl.sol`) on an Ethereum EVM testbed that enforces a tri-state decision matrix (`ALLOW`, `RESTRICT`, `DENY`), logs immutable events, and allows owner-only threshold reconfiguration.
4. **Resource-Level Actuation**: Develop an enforcement layer (`ResourceOperationService`) capable of translating tri-state policy decisions into simulated device actions (`EXECUTED`, `DOWNGRADED`, `BLOCKED`).
5. **Decoupled Asynchronous Telemetry & Analytics**: Construct an event-driven telemetry pipeline using RabbitMQ, WebSocket STOMP streaming, an observational web dashboard, and an idempotent Spring Batch offline auditing engine.
6. **Reproducible Experimental Evaluation**: Build a controlled experimental framework to benchmark system latency across architectural tiers, quantify blockchain gas consumption, evaluate security invariants under failure conditions, and compare counterfactual access control models.

## 3.3 Formulated Research Questions (RQs)
The research investigation is structured around six core Research Questions:

### RQ1: Multi-Scenario Authorization Behavior
*How does the adaptive TrustABAC-IoT authorization pipeline behave across normal access, restricted operations, low trust, high risk, attribute failures, blockchain outages, recovery, and mixed workloads?*
- **Focus**: Evaluating whether observed runtime policy decisions strictly match theoretical security specifications across diverse operating conditions.

### RQ2: Latency Decomposition Across Architectural Tiers
*How do authorization latency, resource enforcement latency, and client end-to-end latency vary across different operational scenarios?*
- **Focus**: Quantifying the exact time spent in gateway attribute gating, trust/risk calculation, on-chain EVM transaction execution, and simulated device state mutation.

### RQ3: Impact of Behavioral Trust and Contextual Risk
*How do behavioral Trust scores and contextual Risk factors dynamically influence policy outcomes and resource actuation?*
- **Focus**: Analyzing how trust decay enforces strict denial and how contextual risk triggers pre-blockchain short-circuiting or privilege attenuation.

### RQ4: Comparative Reference Mode Divergence
*What behavioral differences are observed between pure ABAC (Mode A), centralized T-ABAC (Mode B), and authoritative blockchain-backed execution (Mode C)?*
- **Focus**: Quantifying policy agreement and divergence rates across counterfactual evaluation models.

### RQ5: Security Invariant and Fault Tolerance Preservation
*Does the implementation preserve intended security invariants (fail-closed outage denial, privilege attenuation, Gate 1 short-circuiting, anti-double-authorization) under adversarial and failure conditions?*
- **Focus**: Validating system resilience during simulated node outages, attribute spoofing, and privilege escalation attempts.

### RQ6: Blockchain Execution and Gas Footprint
*What transaction throughput, gas consumption, and cost characteristics are observed in the local Ganache EVM environment?*
- **Focus**: Measuring per-transaction gas consumption, evaluating deterministic execution costs, and analyzing the gas-saving benefits of pre-blockchain gateway filtering.
