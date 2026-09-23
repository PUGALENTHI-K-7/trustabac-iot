# Chapter 7 — Experimental Methodology

## 7.1 Evaluation Framework and Objectives
The experimental evaluation of TrustABAC-IoT was designed to provide reproducible, objective empirical measurements of the multi-tiered access control platform. The evaluation aims to:
1. Validate policy decision correctness and enforcement fidelity across diverse operating conditions.
2. Measure latency decomposition across architectural tiers (Pre-blockchain filter, trust/risk calculation, EVM smart contract evaluation, and physical resource actuation).
3. Quantify sequential request processing throughput under controlled conditions.
4. Measure blockchain gas consumption and transaction overhead in a local Ganache EVM environment.
5. Evaluate system fault tolerance and fail-closed security invariants during simulated blockchain outages.
6. Compare counterfactual access control models (Pure ABAC vs. Centralized T-ABAC vs. TrustABAC-IoT).

---

## 7.2 Experimental Campaign Structure
The empirical dataset was generated using the automated benchmark harness (`contracts/experiment_phase8b.py`) under a strictly controlled, deterministic testing protocol:

- **Evaluated Scenarios**: 8 controlled operational and adversarial archetypes.
- **Experimental Repetitions**: $R = 3$ independent repetitions per scenario.
- **Warm-up Phase**: $W = 10$ unmeasured warm-up requests per repetition to prime JVM JIT compilation, database connection pools, and EVM gas caches ($10 \times 3 \times 8 = 240$ warm-up requests total).
- **Primary Measured Sample**: $M = 30$ measured requests per repetition ($30 \times 3 = 90$ measured requests per scenario; $90 \times 8 = 720$ primary measured samples total).
- **Total Workload Executed**: $240 + 720 = 960$ total raw requests.
- **Deterministic Workload Seed**: Structured PRNG seed formula `seed = r * 1000 + s * 100 + 42` (Base seed `42`, initial seed `1042` for Repetition 1) to guarantee identical request attribute sequences across comparative repetitions.
- **State Reset Protocol**: Automated REST baseline reset executed before every repetition to restore device parameters, trust baselines ($T = 80.0$), and active booking states.

---

## 7.3 Evaluated Scenario Archetypes

| Scenario Identifier | Workload Description | Evaluated Invariants | Expected Decision & Enforcement |
| :--- | :--- | :--- | :--- |
| **1. `NORMAL_ACCESS`** | Legitimate tenant controlling appliances during valid booking with high trust ($T=80$) and low risk ($R=4.2$). | Baseline operational performance, optimal access path. | `ALLOW` $\longrightarrow$ `EXECUTED` |
| **2. `RESTRICT_ACCESS`** | Legitimate tenant operating high-sensitivity appliances over external cellular network ($30 \le R \le 70$). | Dynamic privilege attenuation and parameter clamping. | `RESTRICT` $\longrightarrow$ `DOWNGRADED` |
| **3. `LOW_TRUST`** | Tenant with degraded historical compliance score ($T=20.0 < 30.0$) attempting device operations. | On-chain behavioral trust penalty enforcement. | `DENY` $\longrightarrow$ `BLOCKED` |
| **4. `HIGH_RISK`** | Tenant exhibiting severe contextual anomalies or burst frequency attacks ($R > 70.0$). | Pre-blockchain gateway short-circuiting and gas conservation. | `DENY` $\longrightarrow$ `BLOCKED` |
| **5. `ABAC_FAILURE`** | Unauthorized guest attempting access without a valid booking reservation. | Gate 1 ABAC attribute filtering and zero gas expenditure. | `DENY` $\longrightarrow$ `BLOCKED` |
| **6. `MIXED_SECURITY_WORKLOAD`** | Stochastic blend across normal, restricted, low-trust, and high-risk requests. | Multi-pattern resilience under dynamic multi-user traffic. | Dynamic Distribution |
| **7. `BLOCKCHAIN_OUTAGE`** | Access attempts occurring during simulated Ganache RPC disconnection. | Fail-closed security invariant enforcement. | `DENY` $\longrightarrow$ `BLOCKED` |
| **8. `RECOVERY`** | Access attempts immediately following blockchain RPC restoration. | Seamless operational recovery and normal access resumption. | `ALLOW` $\longrightarrow$ `EXECUTED` |

---

## 7.4 Latency and Performance Metrics Decomposition

To avoid confounding blockchain transaction delays with gateway computation or physical device actuation, the experimental framework measures three decomposed latency tiers:

1. **Authorization Latency ($L_{auth}$)**: Time elapsed from gateway request receipt through ABAC gating, trust calculation, risk evaluation, and EVM smart contract execution until the final decision enum is produced.
2. **Resource Enforcement Latency ($L_{enforce}$)**: Time required by `ResourceOperationService` to evaluate parameter clamping rules, mutate simulated IoT device hardware state models, and persist audit logs in MySQL.
3. **Client End-to-End Latency ($L_{client}$)**: Total wall-clock time perceived by the external client for a sequential batch of 30 operations, including network serialization and HTTP round-trip transport.

### Statistical Confidence Intervals:
For each scenario ($N = 90$ samples across 3 repetitions), sample mean ($\bar{x}$), standard error ($SE = \frac{s}{\sqrt{N}}$), median, 95th percentile ($p95$), and Student's t 95% Confidence Intervals are computed:
$$CI_{95} = \bar{x} \pm t(0.975, N-1) \times SE$$
Where $t(0.975, 89) = 1.98698$.

---

## 7.5 Counterfactual Reference Models
To evaluate the specific behavioral divergence introduced by each architectural component, three reference modes are evaluated against identical request attribute streams:

- **Mode A (Pure ABAC Reference)**: Centralized attribute evaluation evaluating subject role and booking validity only. Ignores behavioral trust decay, contextual risk spikes, and blockchain availability.
- **Mode B (Centralized T-ABAC Reference)**: Evaluates ABAC, Trust, and Risk in a centralized software service without blockchain consensus or on-chain smart contract transactions.
- **Mode C (TrustABAC-IoT Blockchain)**: Authoritative multi-tiered architecture executing on-chain Solidity smart contract evaluations on Ganache EVM.
