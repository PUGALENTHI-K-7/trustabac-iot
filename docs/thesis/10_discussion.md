# Chapter 10 — Discussion

## 10.1 Architectural Trade-offs in Blockchain-Assisted IoT Access Control

### 1. On-Chain Evaluation vs. Edge Gateway Filtering
The integration of blockchain smart contracts into IoT access control introduces fundamental trade-offs between cryptographic decentralization, execution latency, and financial transaction costs:

- **Full On-Chain Execution**: Direct smart contract invocation guarantees deterministic multi-party consensus and immutable event logging. However, in our local Ganache EVM evaluation, an on-chain evaluation added an average authorization latency of $61.217 \pm 2.258$ ms and consumed $31,863$ gas per request.
- **The Value of Pre-Blockchain Filtering**: By enforcing Gate 1 ABAC validation and high-risk thresholding ($R > 70.0$) at the Spring Boot edge gateway, the architecture avoided $299$ unnecessary blockchain transactions across the 720 evaluated requests. This demonstrates that hybrid architectures—where lightweight edge filters protect the blockchain from malformed requests—provide substantial gas savings without compromising the authoritative decision role of the smart contract.

---

## 10.2 Comparative Analysis of Reference Models (Modes A, B, and C)

The counterfactual analysis presented in Chapter 8 highlights the distinct operational characteristics of each paradigm:

1. **Mode A (Pure ABAC Reference)**:
   - **Behavioral Characteristics**: Relies exclusively on static subject attributes and reservation temporal bounds.
   - **Operational Implications**: While Mode A exhibited high agreement ($100\%$) during normal access and attribute failure scenarios, it diverged on $56.81\%$ of total evaluated requests. Because Mode A lacks awareness of subject reputation decay ($T < 30.0$) or contextual anomalies ($R > 70.0$), it continued granting access to compromised subjects. Furthermore, Mode A cannot enforce intermediate privilege attenuation (`RESTRICT`).
2. **Mode B (Centralized T-ABAC Reference)**:
   - **Behavioral Characteristics**: Evaluates identical ABAC, Trust, and Risk logic within a centralized Java service.
   - **Operational Implications**: Mode B achieved $100.0\%$ policy decision concordance with Mode C, demonstrating that the policy rules are computationally sound. However, Mode B relies entirely on a centralized database, lacking cryptographic non-repudiation and decentralized auditability.
3. **Mode C (TrustABAC-IoT Blockchain)**:
   - **Operational Characteristics**: Combines dynamic behavioral trust, real-time contextual risk, and immutable smart contract enforcement. Provides cryptographic receipts (`AuthorizationEvaluated` events) and verifiable state transitions across all participating stakeholders.

---

## 10.3 The Gateway-as-Oracle Assumption
In the TrustABAC-IoT architecture, the Spring Boot edge gateway serves as the bridge between IoT devices, relational databases, and the Ethereum testbed. This design introduces an important trust assumption:

- **The Oracle Role**: The smart contract relies on the gateway to accurately compute and forward input parameters (`abacPass`, `bookingActive`, `trustScore`, `riskScore`).
- **Mitigating Centralized Vulnerability**: While the gateway acts as an attribute provider, the final policy decision logic and threshold enforcement reside immutably on-chain. An administrator or attacker who modifies gateway code cannot force an `ALLOW` outcome on-chain if the provided trust score is below the contract's immutable threshold ($T < 30$), because the Solidity bytecode enforces the hard-deny condition.

---

## 10.4 Gas Economics and Practical Viability in Smart Environments
In private or consortium blockchain deployments (e.g., enterprise Ethereum or Hyperledger Besu), gas costs represent internal computational accounting rather than real-world fiat currency expenditure. However, when evaluating potential deployment on public Layer-1 or Layer-2 rollups:
- At a nominal gas price of 20 Gwei, a single authorization transaction ($31,863$ gas) incurs an estimated fee of $0.00063726$ ETH.
- On Layer-2 networks (such as Arbitrum or Optimism), transaction fees are several orders of magnitude lower, making deterministic smart contract access control economically viable for high-value IoT environments (e.g., commercial real estate, data centers, and luxury short-term rentals).
