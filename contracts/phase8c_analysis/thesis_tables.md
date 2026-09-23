# TrustABAC-IoT: Comprehensive Empirical Thesis Tables

## Table 1: Experimental Environment and Testbed Specification

| Subsystem | Component | Implementation Specification | Configuration Parameters |
| :--- | :--- | :--- | :--- |
| **Host Platform** | Operating System | Windows 11 Enterprise (Build 10.0) | Multi-core x86_64, 16 Logical Processors |
| **Application Runtime** | Java Virtual Machine | OpenJDK 21.0.8 LTS (Eclipse Adoptium) | Spring Boot 3.2.3, Spring Security 6.2 |
| **Relational Database** | MySQL Database Server | MySQL Community 8.0.45 (Docker) | InnoDB Engine, Port 3307, SSL Disabled |
| **Message Broker** | RabbitMQ Messaging | RabbitMQ 3.13-management (Docker) | AMQP 0-9-1, Port 5672, Telemetry STOMP |
| **Blockchain Testbed** | Ethereum Ganache EVM | Truffle Ganache v7.9.2 (Docker) | Chain ID 1337, Port 8545, Gas Limit 6,721,975 |
| **Smart Contract** | `AdaptiveAccessControl` | Solidity ^0.8.19 (Web3j integration) | Deployed at `0xe78A0F7E598Cc8b0Bb87894B0F60dD2a88d6a8Ab` |
| **Device Simulator** | `DeviceSimulatorService` | In-memory concurrent state emulator | 5 Devices (Thermostat, AC, TV, Light, Door Lock) |

## Table 2: Controlled Security Scenario Definitions and Evaluation Workloads

| Scenario Identifier | Workload Description | Booking State | Baseline Trust | Contextual Risk Factors | Expected Policy Outcome |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `NORMAL_ACCESS` | Legitimate guest device operation | Valid, Active | High (80.0) | Internal WiFi (0.0), Normal Freq | `ALLOW` $\to$ `EXECUTED` |
| `RESTRICT_ACCESS` | Access under moderate risk | Valid, Active | High (80.0) | Cellular / Off-peak (36.3) | `RESTRICT` $\to$ `DOWNGRADED` |
| `LOW_TRUST` | Device operation under degraded user history | Valid, Active | Degraded (20.0) | Low contextual risk (14.0) | `DENY` $\to$ `BLOCKED` |
| `HIGH_RISK` | Severe anomaly / burst frequency attack | Valid, Active | High (80.0) | Location mismatch, Burst (>70.0) | `DENY` $\to$ `BLOCKED` |
| `ABAC_FAILURE` | Operation without valid reservation | Expired/Missing | High (80.0) | Standard Context | `DENY` $\to$ `BLOCKED` |
| `MIXED_SECURITY_WORKLOAD` | Stochastic multi-pattern blend | Mixed | Variable | Stochastic context distribution | Dynamic Distribution |
| `BLOCKCHAIN_OUTAGE` | Access during EVM testbed outage | Valid, Active | High (80.0) | Node unreachable / fail-closed | `DENY` $\to$ `BLOCKED` |
| `RECOVERY` | Restored access post-outage | Valid, Active | High (80.0) | Node operational / reconnected | `ALLOW` $\to$ `EXECUTED` |

## Table 3: Per-Scenario Latency Statistics (N = 90 measured requests per scenario)

| Scenario | Auth Mean $\pm$ SE (ms) | Auth Median (ms) | Auth p95 (ms) | Auth Student's t $CI_{95}$ (ms) | Enforce Mean $\pm$ SE (ms) | Enforce Median (ms) | Enforce p95 (ms) | Enforce $CI_{95}$ (ms) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `NORMAL_ACCESS` | 61.217 $\pm$ 2.258 | 58.802 | 72.908 | [56.730, 65.704] | 72.020 $\pm$ 2.657 | 69.178 | 85.774 | [66.741, 77.299] |
| `RESTRICT_ACCESS` | 56.005 $\pm$ 0.924 | 54.228 | 68.966 | [54.169, 57.840] | 65.888 $\pm$ 1.087 | 63.798 | 81.137 | [63.729, 68.047] |
| `LOW_TRUST` | 59.435 $\pm$ 3.644 | 55.157 | 76.466 | [52.194, 66.676] | 69.924 $\pm$ 4.287 | 64.891 | 89.959 | [61.405, 78.442] |
| `HIGH_RISK` | 43.953 $\pm$ 0.831 | 42.518 | 54.329 | [42.301, 45.604] | 51.709 $\pm$ 0.978 | 50.022 | 63.916 | [49.766, 53.652] |
| `ABAC_FAILURE` | 34.814 $\pm$ 0.699 | 34.170 | 46.689 | [33.424, 36.204] | 40.958 $\pm$ 0.823 | 40.200 | 54.928 | [39.323, 42.593] |
| `MIXED_SECURITY_WORKLOAD` | 52.478 $\pm$ 2.548 | 50.165 | 71.997 | [47.415, 57.541] | 61.739 $\pm$ 2.998 | 59.017 | 84.702 | [55.782, 67.695] |
| `BLOCKCHAIN_OUTAGE` | 0.100 $\pm$ 0.000 | 0.100 | 0.100 | [0.100, 0.100] | 0.002 $\pm$ 0.000 | 0.001 | 0.003 | [0.002, 0.003] |
| `RECOVERY` | 41.289 $\pm$ 0.844 | 39.011 | 53.785 | [39.611, 42.967] | 48.575 $\pm$ 0.993 | 45.896 | 63.277 | [46.601, 50.549] |

## Table 4: Sequential Request Processing Throughput

| Scenario | Pooled Measured N | Mean Client E2E per Run (ms) | Throughput Mean (ops/sec) | Observed Run Range (ops/sec) |
| :--- | :--- | :--- | :--- | :--- |
| `NORMAL_ACCESS` | 90 | 3018.56 | 13.98 | [12.38, 15.17] |
| `RESTRICT_ACCESS` | 90 | 2787.75 | 15.21 | [14.18, 15.79] |
| `LOW_TRUST` | 90 | 2810.50 | 14.39 | [13.19, 15.98] |
| `HIGH_RISK` | 90 | 2130.26 | 19.37 | [18.25, 20.68] |
| `ABAC_FAILURE` | 90 | 1881.97 | 24.54 | [22.30, 27.00] |
| `MIXED_SECURITY_WORKLOAD` | 90 | 2493.39 | 16.21 | [15.50, 16.83] |
| `BLOCKCHAIN_OUTAGE` | 90 | 58.62 | 0.00 | [0.00, 0.00] |
| `RECOVERY` | 90 | 2011.17 | 20.66 | [19.06, 22.37] |

## Table 5: Policy Decision and Resource Enforcement Distributions

| Scenario | Measured N | Decisions (ALLOW / RESTRICT / DENY) | Enforcement (EXECUTED / DOWNGRADED / BLOCKED) | Verification Match Rate |
| :--- | :--- | :--- | :--- | :--- |
| `NORMAL_ACCESS` | 90 | 90 / 0 / 0 | 90 / 0 / 0 | 100.0% PASS |
| `RESTRICT_ACCESS` | 90 | 0 / 90 / 0 | 0 / 90 / 0 | 100.0% PASS |
| `LOW_TRUST` | 90 | 0 / 0 / 90 | 0 / 0 / 90 | 100.0% PASS |
| `HIGH_RISK` | 90 | 0 / 0 / 90 | 0 / 0 / 90 | 100.0% PASS |
| `ABAC_FAILURE` | 90 | 0 / 0 / 90 | 0 / 0 / 90 | 100.0% PASS |
| `MIXED_SECURITY_WORKLOAD` | 90 | 41 / 20 / 29 | 41 / 20 / 29 | 100.0% PASS |
| `BLOCKCHAIN_OUTAGE` | 90 | 0 / 0 / 90 | 0 / 0 / 90 | 100.0% PASS |
| `RECOVERY` | 90 | 90 / 0 / 0 | 90 / 0 / 0 | 100.0% PASS |

## Table 6: Observed Trust and Contextual Risk Distributions

| Scenario | Mean Trust Score | Trust Range | Mean Contextual Risk | Risk Range | Key Context Drivers |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `NORMAL_ACCESS` | 80.0 | [80.0, 80.0] | 4.2 | [0.0, 14.0] | Trusted internal network, normal burst frequency |
| `RESTRICT_ACCESS` | 80.0 | [80.0, 80.0] | 36.3 | [35.0, 42.0] | Cellular connection, off-peak timing factor |
| `LOW_TRUST` | 20.0 | [20.0, 20.0] | 14.0 | [14.0, 14.0] | Prior violation penalty history |
| `HIGH_RISK` | 80.0 | [80.0, 80.0] | >70.0 | [72.0, 88.0] | Mismatched geo-location, burst request anomalies |
| `ABAC_FAILURE` | 80.0 | [80.0, 80.0] | N/A | N/A | Missing/expired reservation attribute (Pre-Risk Gate) |
| `MIXED_SECURITY_WORKLOAD` | 80.0 | [20.0, 80.0] | 21.2 | [0.0, 85.0] | Stochastic mixture across normal, restricted, and malicious |
| `BLOCKCHAIN_OUTAGE` | 80.0 | [80.0, 80.0] | 14.0 | [14.0, 14.0] | Node disconnection (Fail-Closed handler) |
| `RECOVERY` | 80.0 | [80.0, 80.0] | 14.0 | [14.0, 14.0] | Reconnected node state |

## Table 7: Reference Mode Agreement and Counterfactual Divergence Analysis

| Scenario | Sample N | Mode A Agreement (%) | Mode B Agreement (%) | Primary Source of Counterfactual Divergence |
| :--- | :--- | :--- | :--- | :--- |
| `NORMAL_ACCESS` | 90 | 100.00% (90/90) | 100.00% (90/90) | Full policy concordance under valid attributes and high trust. |
| `RESTRICT_ACCESS` | 90 | 0.00% (0/90) | 100.00% (90/90) | Mode A lacks dynamic contextual risk & operation downgrading. |
| `LOW_TRUST` | 90 | 0.00% (0/90) | 100.00% (90/90) | Mode A ignores degraded historical reputation (<30). |
| `HIGH_RISK` | 90 | 0.00% (0/90) | 100.00% (90/90) | Mode A ignores contextual environment risk elevation (>70). |
| `ABAC_FAILURE` | 90 | 100.00% (90/90) | 100.00% (90/90) | Concordance on invalid booking / expired attribute Gate 1 rejection. |
| `MIXED_SECURITY_WORKLOAD` | 90 | 45.56% (41/90) | 100.00% (90/90) | Mode A diverges on degraded trust and high-risk stochastic requests. |
| `BLOCKCHAIN_OUTAGE` | 90 | 0.00% (0/90) | 100.00% (90/90) | Mode A analytical model does not incorporate blockchain availability. |
| `RECOVERY` | 90 | 100.00% (90/90) | 100.00% (90/90) | Full concordance restored following node reconnection. |
| `POOLED_TOTAL` | 720 | 43.19% (311/720) | 100.00% (720/720) | Overall counterfactual policy divergence across 720 requests. |

## Table 8: Blockchain Transaction and Gas Consumption Metrics (Ganache EVM)

| Scenario | Measured Requests | On-Chain Transactions | Gas per Transaction | Total Gas Consumed | Mean Transaction Cost (ETH @ 20 Gwei) |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `NORMAL_ACCESS` | 90 | 90 | 31863 gas | 2867670 gas | 0.00063726 ETH |
| `RESTRICT_ACCESS` | 90 | 90 | 31863 gas | 2867670 gas | 0.00063726 ETH |
| `LOW_TRUST` | 90 | 90 | 31863 gas | 2867670 gas | 0.00063726 ETH |
| `HIGH_RISK` | 90 | 0 | 0 gas | 0 gas | 0.00000000 ETH |
| `ABAC_FAILURE` | 90 | 0 | 0 gas | 0 gas | 0.00000000 ETH |
| `BLOCKCHAIN_OUTAGE` | 90 | 0 | 0 gas | 0 gas | 0.00000000 ETH |
| `RECOVERY` | 90 | 90 | 31863 gas | 2867670 gas | 0.00063726 ETH |
| `MIXED_SECURITY_WORKLOAD` | 90 | 61 | 31863 gas | 1943643 gas | 0.00063726 ETH |
| `POOLED_TOTAL` | 720 | 421 | 31863 gas | 13414323 gas | 0.00063726 ETH |

## Table 9: Security Invariant and Behavioral Verification Summary

| Invariant Description | Tested Conditions | Expected Semantics | Observed Semantics | Verification Result |
| :--- | :--- | :--- | :--- | :--- |
| **Attribute Gate Isolation** | Invalid/expired booking (`ABAC_FAILURE`) | Immediate Gate 1 DENY, 0 gas | 100% DENY (90/90), 0 tx | PASS [OK] |
| **Reputation Enforcement** | Degraded trust history (`LOW_TRUST`) | On-chain smart contract DENY | 100% DENY (90/90), 31,863 gas | PASS [OK] |
| **Contextual Anomaly Block** | Severe risk spike >70 (`HIGH_RISK`) | Pre-blockchain Risk Gate DENY | 100% DENY (90/90), 0 gas | PASS [OK] |
| **Adaptive Downgrading** | Moderate contextual risk (`RESTRICT_ACCESS`) | Smart contract RESTRICT $\to$ Downgrade | 100% DOWNGRADED (90/90) | PASS [OK] |
| **Fail-Closed Availability** | Unreachable EVM (`BLOCKCHAIN_OUTAGE`) | Gateway fail-closed DENY | 100% BLOCKED (90/90), 0 tx | PASS [OK] |
| **Anti-Double-Authorization** | All 720 measured requests | 1 request $\to$ 1 eval $\le$ 1 tx | Exactly 1:1 correlation verified | PASS [OK] |

## Table 10: Summary of Validity Threats and Mitigation Strategies

| Validity Category | Specific Threat / Experimental Limitation | Potential Impact | Methodological Mitigation Implemented |
| :--- | :--- | :--- | :--- |
| **Internal Validity** | State drift between repetition runs | Contaminated baseline across runs | Explicit state reset (trust, simulator, active bookings) before every repetition |
| **Internal Validity** | Nonce / block resets during container restarts | Interrupted EVM synchronization | Simulated outage injected via fail-closed handler without resetting live Ganache container |
| **External Validity** | Software-simulated IoT devices | Lacks physical bus/network delays | Explicitly documented as simulated software endpoints; no MCU/hardware claims made |
| **External Validity** | Local single-node Ganache EVM | Sub-second mining vs public testnets | Documented as EVM execution baseline; public testnet block times (12s) not evaluated |
| **Construct Validity** | Synthetic context and risk models | Risk scores derived from rules | Calibrated multi-factor risk weights (Network, Frequency, Geo-location, Violation history) |
| **Statistical Validity** | Nested request samples ($N=90$) across $R=3$ repetitions | Observations not fully independent | Showed both repetition-level (R1/R2/R3) and pooled statistics; used Student's t distribution |
