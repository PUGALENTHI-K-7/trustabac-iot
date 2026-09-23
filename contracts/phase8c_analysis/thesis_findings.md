# TrustABAC-IoT: Core Research Findings and Empirical Interpretations

## Finding 1: Multi-Tiered Adaptive Authorization Efficacy
- **Empirical Evidence**: Across 720 measured requests, the architecture achieved a 100.0% expectation match rate across all 8 security scenarios.
- **Measured Result**: `NORMAL_ACCESS` produced 100% `ALLOW` (90/90), `RESTRICT_ACCESS` produced 100% `RESTRICT` (90/90), and adversarial/malfunction scenarios (`LOW_TRUST`, `HIGH_RISK`, `ABAC_FAILURE`, `BLOCKCHAIN_OUTAGE`) produced 100% `DENY` (360/360).
- **Interpretation**: Under the tested local configuration, the multi-tiered architecture reliably maps multi-dimensional attribute, trust, and risk inputs into discrete, deterministic access decisions.
- **Limitation**: Evaluated under controlled synthetic workloads in a single-gateway testbed.

## Finding 2: Dynamic Resource-Level Privilege Attenuation (Downgrading)
- **Empirical Evidence**: In `RESTRICT_ACCESS`, 90 of 90 requests were downgraded from high-privilege state modifications to safe bounded operations.
- **Measured Result**: Thermostat temperature setpoints were clamped within safety bounds ($20^\circ\text{C}$--$24^\circ\text{C}$), media volumes were capped ($\le 30\%$), and door locks required secondary confirmation, taking a mean enforcement latency of $65.888 \pm 0.924$ ms.
- **Interpretation**: The system provides a viable intermediate operational tier between binary permit and deny, preventing total service disruption during moderate contextual risk.
- **Limitation**: Enforcement rules are statically defined in `ResourceOperationService` for the 5 simulated IoT device types.

## Finding 3: Reputation-Driven Behavioral Enforcement
- **Empirical Evidence**: In `LOW_TRUST` ($N=90$, baseline trust = 20.0), 100% of requests were rejected on-chain by the smart contract.
- **Measured Result**: Evaluated mean authorization latency of $59.435 \pm 3.644$ ms and consumed 31,863 gas per transaction on Ganache EVM.
- **Interpretation**: Historical reputation degradation successfully overrides valid booking attributes on-chain, preventing compromised identities from manipulating IoT resources.
- **Limitation**: Trust score updates in this benchmark were configured via controlled experimental baselines rather than a continuous live decay process.

## Finding 4: Pre-Blockchain Contextual Risk Short-Circuiting
- **Empirical Evidence**: In `HIGH_RISK` ($N=90$, composite risk > 70.0), 100% of requests were blocked at the gateway Risk Gate prior to EVM invocation.
- **Measured Result**: Total on-chain transactions = 0, total gas consumed = 0 gas, with a lower mean authorization latency ($43.953 \pm 0.831$ ms) compared to on-chain evaluations ($61.217 \pm 2.258$ ms).
- **Interpretation**: Pre-blockchain risk evaluation successfully conserves blockchain computational bandwidth and gas costs under severe anomaly or burst attack conditions.
- **Limitation**: Relies on gateway trust for pre-EVM short-circuiting; a compromised gateway could theoretically drop legitimate requests.

## Finding 5: Attribute Gate Rejection Isolation (ABAC Short-Circuit)
- **Empirical Evidence**: In `ABAC_FAILURE` ($N=90$), all requests lacking an active booking were rejected at Gate 1.
- **Measured Result**: Mean authorization latency was $34.814 \pm 0.699$ ms (the lowest among non-outage scenarios), consuming 0 on-chain gas.
- **Interpretation**: Gate 1 evaluation acts as an efficient lightweight filter, eliminating unnecessary downstream trust calculation, risk aggregation, and blockchain transactions.
- **Limitation**: Attribute verification latency is dominated by local MySQL relational query times.

## Finding 6: Fail-Closed Blockchain Outage Resilience
- **Empirical Evidence**: In `BLOCKCHAIN_OUTAGE` ($N=90$), 100% of requests were blocked (`DENY` $\to$ `BLOCKED`).
- **Measured Result**: Mean authorization response latency was $0.100 \pm 0.000$ ms, with 0 device state mutations.
- **Interpretation**: The architecture strictly maintains a fail-closed security invariant during node unavailability, preventing unauthorized physical access during infrastructure partitioning.
- **Limitation**: Fail-closed semantics trade off availability for security; legitimate users cannot operate devices during a total blockchain outage.

## Finding 7: Post-Outage Seamless Authorization Recovery
- **Empirical Evidence**: In `RECOVERY` ($N=90$), immediately following blockchain node reconnection, 100% of requests achieved `ALLOW` $\to$ `EXECUTED`.
- **Measured Result**: Mean authorization latency was $41.289 \pm 0.844$ ms and on-chain transactions resumed normally (90 transactions, 31,863 gas each).
- **Interpretation**: The Web3j connection provider and smart-contract evaluation pipeline recover gracefully without requiring gateway restarts or state re-initialization.
- **Limitation**: Recovery was evaluated on a local Ganache instance where RPC reconnection latency is minimal.

## Finding 8: Latency and Throughput Characteristics
- **Empirical Evidence**: On-chain evaluated scenarios (`NORMAL_ACCESS`, `RESTRICT_ACCESS`, `LOW_TRUST`) exhibited mean authorization latencies of $56.0$--$61.2$ ms and sequential throughputs of $13.98$--$15.21$ req/s.
- **Measured Result**: Pre-EVM short-circuited scenarios (`ABAC_FAILURE`, `HIGH_RISK`) achieved higher sequential throughput ($19.37$--$24.54$ req/s) and lower latency ($34.8$--$43.9$ ms).
- **Interpretation**: On-chain cryptographic evaluation introduces a measurable but bounded latency overhead (~$18$--$26$ ms) on local EVM, which is mitigated for unauthorized requests by gateway-level short-circuit gates.
- **Limitation**: Measurements reflect single-threaded sequential client dispatch on a local development workstation.

## Finding 9: Deterministic Smart Contract Gas Consumption
- **Empirical Evidence**: Across all 421 successful on-chain transactions in the dataset, gas usage was constant at exactly **31,863 gas** per evaluation.
- **Measured Result**: Standard deviation of gas consumed was $0.0$ gas ($p50 = p90 = p95 = 31,863$ gas), representing a nominal cost of $0.00063726$ ETH per authorization at 20 Gwei gas price.
- **Interpretation**: The Solidity decision matrix executes with fixed-cost computational predictability, avoiding dynamic memory expansion or iterative loop vulnerabilities.
- **Limitation**: Gas prices and execution fees reflect local Ganache defaults; base fees on public L1/L2 networks vary dynamically with network congestion.

## Finding 10: Counterfactual Reference Mode Divergence
- **Empirical Evidence**: Mode A (Pure ABAC) diverged from the authoritative pipeline on **56.81%** of requests (409/720 disagreements, 311/720 agreements), while Mode B (Centralized T-ABAC) agreed on 100% of policy decisions (720/720).
- **Measured Result**: Mode A failed to restrict degraded-trust users (90 requests), failed to attenuate privileges under moderate risk (90 requests), failed to block high-risk anomalies (90 requests), failed to reflect fail-closed outage isolation (90 requests), and diverged on 49 requests in mixed load.
- **Interpretation**: The integration of dynamic behavioral trust and contextual risk provides essential granularity beyond static attribute models, while on-chain smart contract execution (Mode C) provides cryptographic non-repudiation that Mode B lacks.
- **Limitation**: Mode A and Mode B evaluations represent offline counterfactual calculations rather than separate deployed production backends.
