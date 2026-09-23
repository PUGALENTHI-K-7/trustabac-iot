# TrustABAC-IoT: Comprehensive Viva Questions and Answers

This document contains 40+ structured technical questions and precise, evidence-based answers designed for academic defense, viva voce, and technical examination across 20 subject areas (A–T).

---

## Category A: Access Control Fundamentals
### Q1: What is the core limitation of Role-Based Access Control (RBAC) in IoT ecosystems?
**Answer**: RBAC assigns permissions statically based on subject roles. In dynamic IoT environments, access decisions require evaluating contextual attributes (time of day, device location, network transport origin, current sensor status). Attempting to represent dynamic contextual states in RBAC leads to "role explosion" and unmanageable permission matrices.

### Q2: How does Attribute-Based Access Control (ABAC) improve upon RBAC?
**Answer**: ABAC (NIST SP 800-162) evaluates dynamic boolean policies over arbitrary attribute sets: Subject ($S$), Object/Resource ($O$), Action ($A$), and Environment ($E$). This enables fine-grained, policy-driven decisions without creating specialized roles for every context combination.

---

## Category B: IoT & Resource Constraints
### Q3: Why can't IoT microcontrollers directly evaluate smart contracts or run blockchain nodes?
**Answer**: Resource-constrained microcontrollers (e.g., ESP32, STM32) have limited processing power (tens to hundreds of MHz), constrained RAM (tens to hundreds of KB), and strict battery energy budgets. Running an EVM client, parsing large JSON-RPC messages, maintaining peer-to-peer state, or computing cryptographic proofs on-device exceeds their memory and computational capacity.

### Q4: How does the gateway-assisted pattern solve this problem?
**Answer**: The gateway-assisted architecture places a high-capacity edge gateway (implemented in Spring Boot) between constrained IoT devices and the blockchain. The gateway performs heavy attribute collection, trust history querying, contextual risk calculation, and blockchain RPC communication, communicating with physical IoT devices via lightweight protocols while preserving decentralized smart contract decision authority.

---

## Category C: Attribute-Based Access Control (ABAC)
### Q5: What is the purpose of the initial Gate 1 ABAC filter in TrustABAC-IoT?
**Answer**: `AbacService` acts as a pre-blockchain eligibility gate. It verifies fundamental preconditions: whether the device exists, whether the subject role is authorized for the resource type, and whether the subject has an active booking reservation. If Gate 1 fails, the request is immediately rejected at the gateway tier, bypassing downstream trust calculation, risk evaluation, and blockchain RPC calls.

### Q6: What operational benefit does Gate 1 provide in terms of blockchain costs?
**Answer**: In our empirical evaluation of 720 requests, Gate 1 filtering prevented 90 unnecessary blockchain transactions during the `ABAC_FAILURE` scenario alone, resulting in zero gas expenditure for invalid access requests and reducing authorization latency from $61.217$ ms to $34.814$ ms.

---

## Category D: Behavioral Trust Management
### Q7: How is Behavioral Trust defined and calculated in this system?
**Answer**: Behavioral Trust represents the long-term historical reliability of a subject ($0.0 \le T \le 100.0$). It is maintained in MySQL (`trust_history`) and updated via deterministic linear decay and recovery equations when security events occur:
$$T_{new} = \max(0.0, T_{current} - \Delta_{penalty})$$
$$T_{new} = \min(100.0, T_{current} + \Delta_{recovery})$$

### Q8: What happens when a tenant's trust score falls below the medium threshold ($T < 30$)?
**Answer**: The Solidity smart contract enforces a hard-deny condition (`if (trustScore < trustMedium) return (Decision.DENY, 3);`). Even if the tenant has valid credentials and an active reservation, the request is strictly blocked on-chain.

---

## Category E: Contextual Risk Assessment
### Q9: How does Contextual Risk differ conceptually from Behavioral Trust?
**Answer**: **Trust** is a stateful, historical property of the *subject* accumulated over past interactions. **Risk** is a stateless, instantaneous evaluation of the current *request context* (resource sensitivity, network origin, timing, and burst frequency). A trusted tenant ($T=80$) can generate a high-risk request ($R=75$) if connecting from an unverified public network or triggering high-frequency request bursts.

### Q10: How is the contextual risk score calculated?
**Answer**: `RiskService` computes a weighted linear combination of four normalized dimensions:
$$R = w_s \cdot S_{resource} + w_n \cdot N_{network} + w_t \cdot T_{time} + w_b \cdot B_{burst}$$
Where weights sum to 1.0 ($w_s=0.40, w_n=0.25, w_t=0.15, w_b=0.20$).

---

## Category F: Blockchain & Smart Contracts
### Q11: Why use a smart contract for authorization instead of keeping all logic on the gateway?
**Answer**: Centralized gateways represent single points of trust and single points of compromise. An immutable smart contract deployed on an EVM blockchain ensures that policy decision logic cannot be altered by a rogue gateway administrator, guarantees multi-party consensus, and emits immutable on-chain event receipts (`AuthorizationEvaluated`) for tamper-proof non-repudiation.

### Q12: What is the deterministic gas consumption per on-chain evaluation in TrustABAC-IoT?
**Answer**: On the Ganache EVM testbed, every invocation of `evaluateAccess(...)` consumed exactly **31,863 gas** (0.00063726 ETH at 20 Gwei), confirming constant-time bytecode execution paths.

---

## Category G: Solidity Contract Design
### Q13: Explain the tri-state `Decision` enum in `AdaptiveAccessControl.sol`.
**Answer**: The contract defines `enum Decision { DENY, RESTRICT, ALLOW }`:
- `ALLOW (2)`: Granted when $T \ge 70$ and $R \le 30$ with valid attributes.
- `RESTRICT (1)`: Granted when $30 \le T < 70$ or $30 < R \le 70$, attenuating operation privileges.
- `DENY (0)`: Enforced when attributes fail, $T < 30$, or $R > 70$.

### Q14: Who can update the trust and risk thresholds on the smart contract?
**Answer**: Only the contract deployer (owner) can invoke `setTrustThresholds(...)` and `setRiskThresholds(...)`, protected by the `onlyOwner` modifier. Non-owner calls revert on-chain.

---

## Category H: Web3j Integration
### Q15: How does the Java Spring Boot gateway interact with the Solidity smart contract?
**Answer**: The gateway utilizes **Web3j 4.10.3** over HTTP JSON-RPC (`http://127.0.0.1:8545`). Java wrapper classes generated from the contract ABI encode method calls, sign transactions using credentials, broadcast them to the Ganache node, and extract return values and event logs from transaction receipts.

### Q16: How are private keys managed in the Spring Boot backend?
**Answer**: Following strict security hardening, hardcoded private keys were purged. The application binds to `BLOCKCHAIN_PRIVATE_KEY` via environment variables at startup, or safely falls back to dynamic in-memory ephemeral key derivation (`Keys.createEcKeyPair()`) for testing.

---

## Category I: Smart Rental Demonstration Context
### Q17: Describe the smart rental business scenario evaluated in this project.
**Answer**: A multi-tenant short-term property rental (e.g., Airbnb/vacation rental) where guests are granted time-bounded access to IoT appliances (door locks, thermostats, air conditioners, smart lights, smart TVs) during their active booking reservation (`BK-1001`), while restricted from critical infrastructure (security cameras, router administration, owner settings).

### Q18: What happens during `POST_CHECKOUT` in this context?
**Answer**: When a tenant's reservation end timestamp passes, `AbacService` detects an expired booking. Gate 1 immediately rejects all access attempts with `DENY` / `BLOCKED`, revoking digital key access without requiring physical re-keying of locks.

---

## Category J: Relational Database & State Persistence
### Q19: What core database tables are maintained in MySQL?
**Answer**: MySQL 8.0 stores: `devices` (IoT hardware catalog), `bookings` (reservation timestamps and user mappings), `access_requests` (full audit log with tx hashes), `trust_history` (penalties/rewards), `risk_events` (contextual anomalies), and `batch_run_audit` (idempotent offline analytics).

### Q20: Why persist access requests in MySQL if they are already recorded on the blockchain?
**Answer**: MySQL provides high-speed relational querying, indexing, and filtering for real-time dashboards and batch reporting, while the blockchain provides decentralized immutability and non-repudiation proofs. The gateway cross-references MySQL records with on-chain transaction hashes.

---

## Category K: RabbitMQ Asynchronous Messaging
### Q21: What is the architectural role of RabbitMQ in TrustABAC-IoT?
**Answer**: RabbitMQ acts as an asynchronous, decoupled event bus. Following synchronous authorization and device actuation, the gateway publishes non-blocking AMQP domain events to the `trustabac.events` topic exchange for asynchronous risk logging, trust penalty tracking, and audit indexing.

### Q22: Can a message delayed or dropped in RabbitMQ affect an in-flight authorization decision?
**Answer**: No. The synchronous authorization path (`ResourceOperationService` $\to$ `DecisionCoordinator` $\to$ Smart Contract) is fully decoupled from RabbitMQ. RabbitMQ failures do not block or alter synchronous access control decisions.

---

## Category L: WebSocket STOMP Streaming
### Q23: How does the observational web dashboard receive real-time updates?
**Answer**: The Spring Boot gateway embeds a WebSocket STOMP broker listening on `/ws`. The dashboard subscribes to 7 topic channels (`/topic/devices`, `/topic/trust`, `/topic/risk`, `/topic/authorization`, `/topic/blockchain`, `/topic/simulator`, `/topic/security`). When operations occur, event payloads are broadcast immediately.

### Q24: What observational constraints are enforced on the web dashboard?
**Answer**: The dashboard is strictly observational. Client-side JavaScript contains **zero authorization evaluation logic**, zero trust calculation routines, and zero embedded private keys or credentials.

---

## Category M: Spring Batch Offline Auditing
### Q25: What is the purpose of the Spring Batch offline subsystem?
**Answer**: Spring Batch performs periodic, multi-party compliance audits. It reads historical operational records, aggregates request volumes, decision distributions, and sensitive resource denials, and reconciles these aggregates against on-chain transaction proofs.

### Q26: How does Spring Batch guarantee idempotency across rerun attempts?
**Answer**: Analytical job instances are keyed by a unique composite parameter `(periodKey, periodStart, periodEnd)` backed by a unique database constraint. Re-running an identical period detects the existing record (`alreadyProcessed = true`) and avoids double-counting metrics.

---

## Category N: Security & Fault Tolerance
### Q27: Explain the Fail-Closed mechanism implemented for blockchain outages.
**Answer**: In `DecisionCoordinator.java`, Web3j JSON-RPC calls are wrapped in an exception handler. If Ganache becomes unreachable or transactions revert, the gateway immediately catches the error and enforces a synthetic `DENY` decision ($reasonCode = 99$), returning `BLOCKED` to the client in $0.100$ ms.

### Q28: How does the system prevent double-authorization attacks?
**Answer**: Every authorization request is assigned a unique UUID. The system enforces an invariant: **1 logical request $\longrightarrow$ exactly 1 authorization evaluation $\longrightarrow$ at most 1 blockchain transaction**.

---

## Category O: Experimental Methodology
### Q29: Detail the experimental parameters of the Phase 8B campaign.
**Answer**: The evaluation executed 8 scenarios with $R = 3$ repetitions. Each repetition comprised 10 unmeasured warm-up requests and 30 measured requests (90 measured requests per scenario; 720 primary measured samples total; 960 raw requests total) using a structured deterministic PRNG seed formula ($seed = r \times 1000 + s \times 100 + 42$, with base seed 42 and initial seed 1042).

### Q30: Why separate warm-up requests from measured requests?
**Answer**: Warm-up requests eliminate initial transient cold-start noise caused by JVM bytecode JIT compilation, database connection pool establishment, and EVM gas estimation caches, ensuring steady-state statistical measurements.

---

## Category P: Statistical Analysis
### Q31: Why use Student's t distribution instead of $1.96 \times SE$ for confidence intervals?
**Answer**: For sample sizes of $N = 90$ per scenario across 3 repetitions, the Student's t critical value ($t_{0.975, 89} = 1.98698$) provides mathematically rigorous interval estimation that accounts for degrees of freedom in finite samples.

### Q32: What were the measured authorization latencies for `NORMAL_ACCESS` vs. `ABAC_FAILURE`?
**Answer**:
- `NORMAL_ACCESS`: Mean $61.217 \pm 2.258$ ms ($p95 = 72.908$ ms, $CI_{95} = [56.730, 65.704]$ ms).
- `ABAC_FAILURE`: Mean $34.814 \pm 0.699$ ms ($p95 = 46.689$ ms, $CI_{95} = [33.424, 36.204]$ ms).

---

## Category Q: Limitations
### Q33: What is the primary limitation regarding IoT hardware evaluation in this project?
**Answer**: IoT devices were evaluated as in-memory software state models within `DeviceSimulatorService` rather than deployed on physical microcontrollers (e.g., ESP32, STM32). Consequently, physical transceiver energy consumption and hardware watchdog constraints were not directly measured.

### Q34: What is the primary limitation regarding the blockchain testbed?
**Answer**: The blockchain tier was evaluated on a local Ganache EVM testbed (Chain ID 1337) with instant mining rather than a decentralized public network (e.g., Ethereum mainnet) with variable gossip delays and mempool congestion.

---

## Category R: Literature Comparison
### Q35: How does TrustABAC-IoT compare with Zhang et al. (2018)?
**Answer**: Zhang et al. utilized multiple coordinating smart contracts (ACC, RC, JC) directly on-chain, incurring high gas costs and multi-transaction latency. TrustABAC-IoT uses a gateway-assisted single-contract model with lightweight pre-blockchain filtering, reducing gas consumption.

### Q36: How does TrustABAC-IoT compare with Bernabe et al. (2019) TACIoT?
**Answer**: TACIoT demonstrated multi-dimensional trust-aware ABAC on centralized servers. TrustABAC-IoT extends this concept by incorporating smart contract consensus and cryptographic on-chain non-repudiation.

---

## Category S: Design Decisions & Justifications
### Q37: Why was Java Spring Boot selected for the edge gateway?
**Answer**: Spring Boot provides mature enterprise-grade concurrency, robust JPA database integration, production AMQP (RabbitMQ) clients, STOMP WebSocket support, and native Web3j integration, making it ideal for high-throughput edge gateways.

### Q38: Why was Solidity selected for the smart contract implementation?
**Answer**: Solidity is the industry-standard language for EVM-compatible blockchains, supported by extensive tooling, deterministic execution semantics, and broad compatibility with Layer-1 and Layer-2 rollups.

---

## Category T: Future Work
### Q39: How can the single-gateway oracle trust boundary be eliminated in future work?
**Answer**: By deploying a multi-gateway consortium network utilizing Byzantine Fault Tolerant (BFT) consensus or threshold signature schemes (e.g., BLS multi-signatures) where multiple independent gateways must co-sign attribute payloads before on-chain submission.

### Q40: What steps are required to deploy TrustABAC-IoT onto physical IoT hardware?
**Answer**: Implementing lightweight CoAP/MQTT client firmware on ESP32 microcontrollers with TLS/DTLS mutual authentication, interfacing with the Spring Boot gateway REST API for physical actuator triggering.
