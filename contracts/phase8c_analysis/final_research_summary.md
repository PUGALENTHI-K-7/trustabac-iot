# TrustABAC-IoT: Final Research Summary & Empirical Evaluation

## 1. Research Questions & Evaluated Answers

### RQ1: Multi-Scenario Authorization Behavior
**Question**: How does the adaptive TrustABAC-IoT authorization pipeline behave across normal, restricted, low-trust, high-risk, ABAC-failure, outage, recovery, and mixed workloads?
- **Answer**: The pipeline demonstrated 100.0% behavioral concordance with expected policy specifications across all 720 measured requests. It granted full access (`ALLOW` $\to$ `EXECUTED`) for legitimate users, attenuated access (`RESTRICT` $\to$ `DOWNGRADED`) under moderate risk, and blocked requests (`DENY` $\to$ `BLOCKED`) under degraded trust, severe risk anomalies, attribute failures, or blockchain outages.

### RQ2: Latency Decomposition Across Architectural Tiers
**Question**: How do authorization, enforcement, and client end-to-end latency vary across scenarios?
- **Answer**: Measured authorization latency ranged from $0.100 \pm 0.000$ ms (outage fast-fail) and $34.814 \pm 0.699$ ms (Gate 1 ABAC failure) to $61.217 \pm 2.258$ ms (on-chain normal access). Resource enforcement latency added $40.958$--$72.020$ ms for physical simulation updates, resulting in total sequential client E2E durations of $1.88$--$3.02$ seconds per 30-operation batch ($13.98$--$24.54$ ops/sec throughput).

### RQ3: Impact of Behavioral Trust and Contextual Risk
**Question**: How do Trust and contextual Risk affect authorization outcomes?
- **Answer**: Behavioral trust degradation (<30.0) triggered deterministic on-chain denial, overriding valid attributes. Contextual risk elevation (>70.0) triggered pre-blockchain gateway short-circuit denial, conserving EVM gas. Moderate risk ($30.0 \le Risk \le 70.0$) dynamically triggered smart-contract privilege attenuation (`RESTRICT`).

### RQ4: Comparative Reference Mode Divergence
**Question**: What behavioral differences are observed between pure ABAC (Mode A), centralized T-ABAC (Mode B), and authoritative blockchain TrustABAC-IoT (Mode C)?
- **Answer**: Pure ABAC diverged on 56.81% of requests (409/720 disagreements, 311/720 agreements) because it cannot adapt to behavioral trust decay, environmental risk spikes, or fail-closed outage isolation. Centralized T-ABAC achieved identical policy decisions (100% agreement) but lacked cryptographic immutability and multi-party non-repudiation receipts provided by Mode C's smart contracts.

### RQ5: Security Invariant Preservation
**Question**: Does the implementation preserve intended security invariants under adversarial and failure conditions?
- **Answer**: Yes. Verified 100% fail-closed isolation during simulated outages, 100% privilege attenuation for restricted operations, 100% Gate 1 short-circuiting on attribute failure, and 100% anti-double-authorization (exactly 1 evaluation and $\le 1$ blockchain transaction per request).

### RQ6: Blockchain Execution & Gas Footprint
**Question**: What blockchain transaction and gas characteristics were observed in the local Ganache EVM environment?
- **Answer**: On-chain smart contract evaluations consumed a constant, deterministic **31,863 gas** ($0.00063726$ ETH @ 20 Gwei). Out of 720 measured requests, 421 required on-chain transactions, while 299 unauthorized/anomalous requests were rejected pre-EVM with zero gas consumption.

## 2. Research Conclusion
The empirical findings confirm that multi-tiered adaptive access control combining ABAC, reputation trust, and contextual risk is technically feasible and operationally robust in a gateway-assisted IoT architecture. The separation of lightweight pre-blockchain filters from deterministic on-chain smart contracts provides predictable security enforcement and bounded latency overhead under local EVM evaluation conditions.
