# Chapter 12 — Conclusion and Future Work

## 12.1 Conclusion
This thesis presented the design, implementation, and empirical evaluation of **TrustABAC-IoT**, a gateway-assisted adaptive access control platform for IoT ecosystems and smart rental environments. By integrating Attribute-Based Access Control (ABAC), long-term behavioral trust evaluation, real-time contextual risk assessment, and authoritative Ethereum smart contracts, the system achieves dynamic, decentralized access governance tailored for resource-constrained smart devices.

### Summary of Empirical Findings:
1. **Multi-Scenario Policy Correctness (RQ1)**: The system achieved $100.0\%$ behavioral concordance across 720 measured requests, successfully executing valid commands (`ALLOW`), attenuating privileges under moderate risk (`RESTRICT`), and blocking unauthorized or hostile actions (`DENY`).
2. **Predictable Latency Profile (RQ2)**: Measured authorization latency ranged from $0.100$ ms (outage fast-fail) and $34.814 \pm 0.699$ ms (Gate 1 ABAC failure) to $61.217 \pm 2.258$ ms (full on-chain smart contract evaluation). Resource enforcement on simulated device models added $40.958$--$72.020$ ms.
3. **Dynamic Trust and Risk Adaptation (RQ3)**: Behavioral trust degradation ($T < 30.0$) triggered deterministic on-chain denial, while contextual risk spikes ($R > 70.0$) triggered pre-blockchain gateway short-circuiting.
4. **Counterfactual Model Divergence (RQ4)**: Pure ABAC (Mode A) diverged on $56.81\%$ of requests due to its inability to adapt to reputation decay or environmental anomalies. Centralized T-ABAC (Mode B) matched policy decisions ($100.0\%$) but lacked decentralized cryptographic audit proofs.
5. **Fault Tolerance and Security Resilience (RQ5)**: The platform demonstrated $100\%$ fail-closed security during simulated blockchain outages, zero credential leaks across 282 scanned files, and complete anti-double-authorization invariant preservation.
6. **Deterministic Blockchain Footprint (RQ6)**: Every on-chain evaluation consumed a constant $31,863$ gas. Edge gateway filtering eliminated blockchain transactions for 299 unauthorized/anomalous requests, saving substantial gas.

---

## 12.2 Future Work
To extend the scope of this research beyond the local testbed evaluation, several promising directions are identified:

1. **Physical Microcontroller Testbeds**: Deploying the resource enforcement layer onto physical microcontrollers (e.g., ESP32 and STM32 boards running FreeRTOS) communicating via constrained application protocols (CoAP over DTLS and MQTT over TLS).
2. **Direct Hardware Energy Measurements**: Utilizing physical digital power meters and oscilloscopes to measure milliampere-hour battery draw across physical transceivers (Zigbee, BLE, Z-Wave).
3. **Multi-Gateway Consortium Networks**: Implementing distributed multi-gateway architectures utilizing Byzantine Fault Tolerant (BFT) consensus or threshold signatures (BLS) to eliminate the single gateway oracle dependency.
4. **Public Testnet and Layer-2 Rollup Deployment**: Benchmarking transaction finality, gas dynamics, and mempool behavior on Ethereum public testnets (e.g., Sepolia) and Layer-2 optimistic/ZK rollups (e.g., Arbitrum, Optimism).
5. **Hardware Security Module (HSM) Key Management**: Integrating enterprise key vaults and PKCS#11 hardware security modules for secure on-chain signing key management.
6. **Concurrent High-Volume Load Testing**: Evaluating system scaling under heavily saturated multithreaded workloads exceeding thousands of concurrent tenant requests.
