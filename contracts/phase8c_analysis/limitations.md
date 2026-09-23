# Methodological Limitations and Threats to Validity

## 1. Internal Validity
- **State Reset Verification**: While state reset routines were executed prior to each repetition, relational database transactions and Spring application context caches could introduce micro-level latency autocorrelation.
- **Simulated Outage Mechanism**: The `BLOCKCHAIN_OUTAGE` scenario was modeled using gateway-level network exception simulation to preserve Ganache container block nonces. While functionally accurate for fail-closed verification, it does not measure TCP connection timeout socket stalls.

## 2. External Validity
- **Software-Simulated IoT Endpoints**: Device actuators were executed in-memory via `DeviceSimulatorService`. Physical microcontroller execution constraints (e.g. ESP32 240MHz clock cycles, Zigbee/Z-Wave wireless propagation delay, flash memory wear) were not present in the testbed.
- **Physical Energy & Battery Consumption**: No physical power meters, oscilloscopes, or current shunts were used. No claims regarding physical IoT energy efficiency, battery lifespan extension, or hardware power savings can or should be derived from this software benchmark.
- **Local EVM vs Public Testnets**: Experiments executed against a local single-node Truffle Ganache EVM (instantaneous block generation upon transaction submission). On public multi-node networks (e.g. Ethereum Sepolia, Arbitrum), block proposal intervals ($12$s) and consensus confirmation delays would govern end-to-end client finality.

## 3. Construct Validity
- **Contextual Risk Formulation**: Composite risk scores were calculated using a weighted additive model across 4 synthetic risk dimensions. Real-world contextual risk may exhibit non-linear interactions, temporal clustering, and multi-sensor correlations not captured in linear formulas.
- **Trust Decay Dynamics**: Trust evaluations used discrete historical baselines (80.0 vs 20.0). Continuous Bayesian reputation decay was not actively stressed during short-duration request bursts.

## 4. Statistical Validity
- **Statistical Unit & Nesting**: The primary dataset contains $N=90$ requests per scenario nested within $R=3$ repetition runs. Because requests within a run share host CPU/RAM conditions, ordinary inferential tests (Welch's t-test, Mann-Whitney U) should be interpreted as exploratory descriptive comparisons rather than independent random samples from an infinite population.
- **Deterministic Seed Usage**: Workloads utilized fixed PRNG seeds (`42` / `1042`) across runs for exact reproducibility. Results characterize system performance under this specific deterministic workload distribution.

## 5. Scope Boundaries & No-Novelty Statement
- **Academic Neutrality**: This research does not claim universal performance superiority or novelty of the abstract concept of combining Blockchain, ABAC, and Trust. The contribution lies strictly in the concrete architectural synthesis, open multi-tiered implementation, and empirical validation under controlled IoT edge conditions.
