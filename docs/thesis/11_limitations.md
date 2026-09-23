# Chapter 11 — Limitations

## 11.1 Methodological and Testbed Limitations
To maintain rigorous scientific integrity and avoid overstated claims, this chapter explicitly documents the architectural, experimental, and operational limitations of the TrustABAC-IoT evaluation.

---

## 11.2 Key System Limitations

### 1. In-Memory Software Simulation of IoT Devices
- **Limitation**: The evaluated IoT devices (smart locks, thermostats, air conditioners, smart TVs, lights) were simulated as in-memory Java state models within `DeviceSimulatorService` rather than deployed on physical microcontrollers (e.g., ESP32, STM32, or Raspberry Pi).
- **Implication**: Physical hardware constraints—such as flash memory wear, hardware watchdog timers, low-power sleep modes, and microcontroller boot cycles—were not directly evaluated.

### 2. Absence of Physical Energy Consumption Measurements
- **Limitation**: Due to the software-simulated testbed environment, direct hardware power draw (milliampere-hours / Joules) across constrained wireless transceivers (Zigbee, BLE, Z-Wave, LoRaWAN) was not measured.
- **Implication**: Claims regarding IoT device battery lifespan extension or physical energy savings are not made.

### 3. Local Ganache EVM Testbed Evaluation
- **Limitation**: The blockchain tier was evaluated on Truffle Ganache v7.9.2 (Chain ID 1337) operating as a single-node local EVM with instant block mining.
- **Implication**: Distributed consensus latency, multi-validator network gossip delays, transaction mempool congestion, and variable gas price spikes inherent in public Ethereum networks or decentralized Layer-2 rollups were not simulated.

### 4. Single Gateway and Oracle Trust Boundary
- **Limitation**: The implementation employs a single Spring Boot edge gateway instance acting as the sole intermediary between IoT devices, relational storage, and the blockchain.
- **Implication**: The architecture assumes gateway integrity for attribute collection. Multi-gateway consensus, threshold signature schemes, and decentralized oracle networks (e.g., Chainlink) were not implemented.

### 5. Sequential Workload Benchmarking
- **Limitation**: The benchmarking framework executed requests sequentially (issuing the next request only upon receipt of the prior response) to isolate per-request latency decomposition without queue contention.
- **Implication**: Reported throughput figures ($13.98$--$24.54$ ops/sec) represent sequential client round-trip performance, not the maximum saturated concurrency capacity of the multi-threaded Spring Boot gateway.

### 6. Sample Size and Deterministic Workload Scope
- **Limitation**: The experimental dataset was collected across $R = 3$ repetitions with 30 measured requests per run (90 requests per scenario; 720 measured requests total) using a deterministic PRNG seed formula ($seed = r \times 1000 + s \times 100 + 42$, starting at $1042$).
- **Implication**: While adequate for Student's t 95% confidence interval estimation under controlled conditions, broader stochastic user population distributions over multi-month timeframes remain unmeasured.
