# Chapter 2 — Literature Review

## 2.1 Overview of IoT Access Control Literature
Access control in the Internet of Things has evolved across several distinct generations of security paradigms. Early implementations adapted traditional Role-Based Access Control (RBAC) and Capability-Based Access Control (CapBAC) models. However, the unique demands of dynamic multi-tenancy, decentralized infrastructure, and physical device actuation led researchers to investigate Attribute-Based Access Control (ABAC), trust management systems, and blockchain-based smart contract authorization.

This chapter reviews foundational and contemporary research in decentralized IoT access control, evaluates specific architectural trade-offs, and delineates the design space explored by the TrustABAC-IoT platform.

---

## 2.2 Foundational Access Control and Attribute Models

### Hu et al. (2014) — Guide to Attribute Based Access Control (ABAC) Definition and Considerations
- **Citation**: Hu, V. C., Ferraiolo, D., Kuhn, R., Schnitzer, A., Sandlin, K., Miller, R., & Scarfone, K. (2014). *Guide to Attribute Based Access Control (ABAC) Definition and Considerations*. NIST Special Publication 800-162, National Institute of Standards and Technology. DOI: 10.6028/NIST.SP.800-162.
- **Problem Addressed**: Formulating standardized architectural components (Policy Decision Point, Policy Enforcement Point, Policy Information Point, Policy Administration Point) and attribute evaluation rules for enterprise systems.
- **Architecture**: Centralized PDP/PEP architecture evaluating subject, object, action, and environmental attributes.
- **IoT / Edge Setting**: Generalized enterprise architecture; not tailored for constrained IoT edge devices.
- **Relevance & Research Gap**: Establishes the foundational ABAC attribute evaluation logic used in TrustABAC-IoT's initial gating filter (`AbacService`). However, NIST SP 800-162 does not incorporate dynamic subject reputation metrics or decentralized blockchain consensus.

### Dsouza et al. (2014) — Policy-Driven Security Management in Ambient IoT Environments
- **Citation**: Dsouza, C., Ahn, G. J., & Taguinod, M. (2014). Policy-driven security management for fog computing: Making IoT secure and dependable. *IEEE 2nd International Conference on Collaboration and Internet Computing (CIC)*, pp. 284–291.
- **Problem Addressed**: Enforcing contextual policy boundaries across distributed fog computing nodes.
- **Architecture**: Edge-assisted policy caching and localized policy evaluation points.
- **Relevance & Research Gap**: Validates the necessity of edge/gateway assistance for IoT policy evaluation, but relies on centralized trust anchors vulnerable to single-point failure.

---

## 2.3 Blockchain-Based Access Control Systems

### Ouaddah et al. (2016) — FairAccess: A Blockchain-Based Access Control Framework
- **Citation**: Ouaddah, A., Abou Elkalam, A., & Ouahman, A. A. (2016). FairAccess: a new blockchain-based access control framework for the Internet of Things. *Security and Communication Networks*, 9(18), pp. 5943–5964. DOI: 10.1002/sec.1748.
- **Problem Addressed**: Decentralizing access management in IoT using blockchain transaction scripts.
- **Architecture**: Bitcoin-like UTXO scripting representing access tokens transferred between subjects and resource owners.
- **Blockchain Usage**: Custom transaction scripts on a decentralized ledger.
- **IoT / Edge Setting**: Constrained IoT devices parse blockchain transactions.
- **Relevance & Research Gap**: FairAccess introduced decentralized access tokenization. However, Bitcoin-style scripting lacks Turing-complete policy logic, cannot compute multi-dimensional trust/risk algorithms, and imposes excessive transaction overhead on constrained microcontrollers.

### Zhang et al. (2018) — Smart Contract-Based Access Control in IoT
- **Citation**: Zhang, Y., Kasahara, S., Shen, Y., Jiang, X., & Wan, J. (2018). Smart contract-based access control for the Internet of Things. *IEEE Internet of Things Journal*, 6(2), pp. 1594–1605. DOI: 10.1109/JIOT.2018.2847705.
- **Problem Addressed**: Implementing dynamic, distributed access control using Ethereum smart contracts.
- **Architecture**: Multiple smart contracts (Access Control Contract ACC, Register Contract RC, Judge Contract JC) coordinating on-chain authorization.
- **Smart Contract Usage**: Turing-complete Solidity contracts for subject lookup and policy matching.
- **Relevance & Research Gap**: Demonstrates the viability of smart contracts for policy evaluation. However, the multi-contract coordination pattern incurs substantial gas costs and latency per access attempt, and lacks integration with contextual risk calculation and real-time physical device actuation.

### Novo (2018) — Blockchain Meets IoT: Scalable Access Management
- **Citation**: Novo, O. (2018). Blockchain meets IoT: An architecture for scalable access management in IoT. *IEEE Internet of Things Journal*, 5(2), pp. 1184–1195. DOI: 10.1109/JIOT.2018.2812239.
- **Problem Addressed**: Mitigating IoT device computational constraints by offloading blockchain interactions to a management hub.
- **Architecture**: Gateway-assisted architecture where management hubs interface directly with a smart contract on behalf of constrained IoT nodes.
- **IoT / Edge Setting**: CoAP-based IoT nodes communicating with an edge management hub.
- **Relevance & Research Gap**: Confirms the architectural soundness of gateway-assisted blockchain integration, which forms the architectural basis of TrustABAC-IoT's Spring Boot gateway. However, Novo's model employs binary static permissions without behavioral trust history or risk-aware privilege attenuation.

### Liu et al. (2020) — Blockchain-Based Decentralized Dynamic Access Control
- **Citation**: Liu, H., Han, D., & Li, D. (2020). Fabric-IoT: A blockchain-based access control system in IoT. *IEEE Access*, 8, pp. 18207–18218. DOI: 10.1109/ACCESS.2020.2968492.
- **Problem Addressed**: Hyperledger Fabric-based permissioned access control with dynamic attribute evaluation.
- **Architecture**: Chaincode-based Policy Decision Point utilizing endorsement peers.
- **Relevance & Research Gap**: Demonstrates permissioned consortium access control. However, permissioned chaincode execution requires heavy server infrastructure and does not provide an integrated model for IoT reputation trust decay and recovery.

---

## 2.4 Trust Management and Dynamic Risk in IoT

### Bernabe et al. (2019) — TACIoT: Multidimensional Trust-Aware Access Control
- **Citation**: Bernabe, J. B., Ramos, J. L. H., & Gomez, A. F. (2019). TACIoT: Multidimensional trust-aware access control framework for the Internet of Things. *IEEE Access*, 7, pp. 31847–31864. DOI: 10.1109/ACCESS.2019.2902994.
- **Problem Addressed**: Combining multi-dimensional trust (reputation, QoS, security metrics) with ABAC in IoT networks.
- **Architecture**: Centralized Trust and Policy Server computing continuous trust vectors.
- **Trust Usage**: Dynamic trust evaluation penalizing malicious nodes over time.
- **Relevance & Research Gap**: TACIoT provides strong conceptual motivation for integrating trust scores into ABAC policies. However, its evaluation relies on centralized servers lacking cryptographic non-repudiation and smart contract enforcement.

### Khurshid et al. (2020) — Trust-Based Collaborative Access Control for IoT
- **Citation**: Khurshid, A., Al-Dhaqm, M., & Choo, K. K. R. (2020). Trust-based collaborative access control for Internet of Things. *Sensors (MDPI)*, 20(18), 5184. DOI: 10.3390/s20185184.
- **Problem Addressed**: Dynamic trust calculation based on direct interactions and recommendations in peer-to-peer IoT networks.
- **Relevance & Research Gap**: Demonstrates collaborative trust aggregation, but highlights vulnerability to collusion and recommendation tampering in the absence of immutable on-chain audit trails.

### Riaz et al. (2021) — Attribute-Based Access Control and Trust Management Survey
- **Citation**: Riaz, M. N., Raza, M. A., & Anis, A. (2021). Attribute-based access control and trust management in Cloud-IoT: A survey. *IEEE Communications Surveys & Tutorials*, 23(4), pp. 2480–2512.
- **Problem Addressed**: Comprehensive synthesis of literature combining ABAC, trust management, and blockchain in Cloud-IoT.
- **Findings**: The survey explicitly notes that while conceptual architectures combining Blockchain, ABAC, and Trust exist, few implementations provide reproducible experimental datasets dissecting latency overhead, resource enforcement semantics, and offline reconciliation.

---

## 2.5 Literature Synthesis and TrustABAC-IoT Positioning

| Study | Core Mechanism | Gateway Involved? | Trust Model | Contextual Risk? | Decision Semantics | Blockchain Testbed | Empirical Latency Breakdown? |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **Hu et al. (2014)** | Pure ABAC | No | None | Static | Binary (`ALLOW`/`DENY`) | None | No |
| **Ouaddah et al. (2016)** | Bitcoin Scripts | No | None | No | Binary Token | Bitcoin Testnet | Transaction only |
| **Novo (2018)** | Smart Contract Hub | Yes | None | No | Binary | Ethereum Private | End-to-End only |
| **Zhang et al. (2018)** | Multi-Contract ACC | No | None | No | Binary | Ethereum Private | Contract gas only |
| **Bernabe et al. (2019)** | Centralized T-ABAC | Yes | Multi-dim | Yes | Binary | None | Centralized only |
| **Liu et al. (2020)** | Chaincode ABAC | Yes | Static | No | Binary | Hyperledger Fabric | Peer latency |
| **TrustABAC-IoT (This Work)** | **Gateway T-ABAC + Smart Contract** | **Yes (Spring Boot)** | **Long-term History** | **Real-time Stateless** | **Tri-state (`ALLOW`/`RESTRICT`/`DENY`)** | **Ganache EVM (Chain ID 1337)** | **Decomposed (Auth, Enforce, E2E)** |

### Identified Research Gap:
Prior literature has established the theoretical foundations of Blockchain-based ABAC and Trust-aware access control. The primary contribution of this thesis is **not** to claim novelty in combining these concepts, but to provide a **rigorously engineered, end-to-end reference implementation and controlled empirical evaluation** that:
1. Decomposes latency across architectural tiers (Pre-blockchain ABAC gate, contextual risk evaluation, on-chain EVM evaluation, and simulated resource actuation).
2. Implements real resource-level privilege attenuation (`RESTRICT` $\to$ `DOWNGRADED`) for simulated IoT devices.
3. Evaluates fail-closed security invariants under simulated blockchain outages.
4. Provides an idempotent offline analytical auditing subsystem (Spring Batch) for multi-party reconciliation.
