# TrustABAC-IoT — Review 2 Speaker Notes

**Team:** 24MIC0082 Pugalenthi K · 24MIC0025 RK Bharath
**Purpose:** Per-slide talking guide for the live Review 2 presentation

---

## Slide 1 — Title [BOTH]

> "Good morning/afternoon. We are presenting TrustABAC-IoT — an adaptive trust- and risk-aware access control system for IoT environments, backed by a blockchain smart contract.
> I'm Pugalenthi, and my teammate is RK Bharath. Today, we'll walk through the architecture, explain each module with its code, and execute a live demonstration."

---

## Slide 2 — Agenda [PUGAL]

> "Here's our agenda. We'll cover the problem, then go through 6 modules. Pugalenthi covers authorization and blockchain. Bharath covers IoT simulation, event pipeline, and the dashboard. We'll then show empirical results and finish with a live demo."

---

## Slide 3 — Problem Statement [PUGAL]

> "Traditional access control models have critical gaps. RBAC uses static roles — but in a smart apartment, tenants change daily. Pure ABAC checks attributes like booking tokens — but if a guest has a valid token and is behaving maliciously, pure ABAC still says ALLOW.
>
> Our research gap is: there is no fully integrated system combining behavioral trust history, real-time contextual risk, AND blockchain-backed decision authority."

---

## Slide 4 — Solution [PUGAL]

> "Our solution has three tiers. First, ABAC checks the basic attributes. Then we calculate a trust score from the subject's historical behavior, and a risk score from the current environment. These two scores are sent to our smart contract on the Ethereum EVM. The contract outputs one of three decisions: ALLOW, RESTRICT, or DENY.
>
> The key innovation is that RESTRICT is enforced as privilege attenuation — not full denial. A guest on an untrusted network gets read-only access instead of being completely blocked."

---

## Slide 5 — Architecture [PUGAL introduces, BHARATH points to their sections]

> "The architecture is a gateway-assisted design. The Spring Boot edge gateway sits between IoT clients and the blockchain. I implemented the authorization tiers. Bharath implemented the right side — the event pipeline, the WebSocket broker, and the dashboard.
>
> Importantly, the dashboard has zero authorization logic — it is purely observational."

---

## Slide 6 — Module 1: ABAC & Pipeline [PUGAL]

> "Gate 1 is my AbacService. Before we ever touch the blockchain, we check four conditions: is the subject active, do they have a valid booking right now, does the operation map to this resource type, and is the device online?
>
> If any check fails, we return DENY immediately — no trust calculation, no risk score, no blockchain transaction. This saves 43% latency compared to a full on-chain path — 34.8ms vs 61.2ms.
>
> My trust score uses a linear decay formula, and my risk score weights four environmental factors: resource sensitivity, network type, time of day, and request burst frequency."

---

## Slide 7 — Module 2: Smart Contract [PUGAL]

> "This is the heart of the system — AdaptiveAccessControl.sol. It contains 7 deterministic rules on the EVM. I'll walk through them:
>
> ABAC fails → DENY. Booking expired → DENY. Trust below 30 → DENY. Risk above 70 → DENY. Trust above 70 AND risk below 30 → ALLOW. Otherwise → RESTRICT.
>
> Every evaluation emits an AuthorizationEvaluated event on-chain. This is the immutable audit receipt — it cannot be modified after the fact. The gas cost is exactly 31,863 per transaction — completely deterministic."

---

## Slide 8 — Module 3: IoT Simulator [BHARATH]

> "I built the IoT simulator that represents 10 devices in a smart apartment. These are in-memory state models — we're not claiming physical hardware. We have door locks, thermostats, cameras, lights, TVs, and a router.
>
> The critical part is the enforcement. When the smart contract says RESTRICT, my ResourceOperationService doesn't just ignore it — it enforces safe bounds. The thermostat gets clamped to 20–24°C. The door stays locked but returns read-only status. This is privilege attenuation at the device level."

---

## Slide 9 — Module 4: RabbitMQ Pipeline [BHARATH]

> "After every authorization decision, I publish an AMQP event to the RabbitMQ exchange. This decouples the authorization latency from all downstream processing. Logging, analytics, trust updates — none of these block the response to the IoT client.
>
> I have 5 queues — one for device events, one for trust events, one for risk events, one for the authorization audit trail, and a dead-letter queue for unprocessable messages.
>
> My IdempotencyGuard tracks message UUIDs to prevent double-processing. And critically — these consumers cannot change in-flight decisions. They are observational only."

---

## Slide 10 — Module 5: Dashboard [BHARATH]

> "The real-time dashboard connects via STOMP WebSocket. You'll see it live in the demo. There are 10 device tiles that update in real time, and an authorization event feed that shows each decision as it happens — ALLOW, RESTRICT, or DENY — with the trust and risk scores.
>
> This dashboard is purely observational. There are no private keys, no authorization logic. Everything you see is pushed from the backend."

---

## Slide 11 — Module 6: Spring Batch [PUGAL]

> "For offline auditing, I built a Spring Batch job. It reads from the MySQL access_requests table, aggregates ALLOW/RESTRICT/DENY counts, and reconciles against on-chain transaction hashes. This provides a reproducible, multi-party audit trail.
>
> The key property is idempotency — if you run the same audit period twice, the second run returns alreadyProcessed:true with no duplicate rows. We'll demonstrate this live."

---

## Slide 12 — Experimental Design [PUGAL]

> "For our empirical evaluation, we ran Phase 8B: 8 scenarios, 30 requests per run, 3 repetitions each. That's 720 measured requests in total. We verified state reset before every repetition, and we enforced that one request produces exactly one blockchain transaction — no double-authorizations.
>
> Our confidence intervals use Student's t-distribution, not just the 1.96 approximation, because N=90 per scenario is moderately sized."

---

## Slide 13 — Latency Results [PUGAL]

> "The latency table shows two important findings. First, the ABAC short-circuit: when a request fails Gate 1, latency drops to 34.8ms versus 61.2ms for normal access — that's because we skip trust, risk, and the entire EVM call.
>
> Second, fail-closed outage: when Ganache is disconnected, the gateway rejects requests in 0.1 milliseconds — the exception handler fires immediately. There's no timeout cascade."

---

## Slide 14 — Policy Correctness [PUGAL]

> "The most important result: 100% behavioral correctness across all 720 measured requests. Every scenario produced exactly the decisions we expected — ALLOW in normal access, RESTRICT in moderate risk, DENY under attack or outage.
>
> This is not a coincidence — it validates that the smart contract logic correctly implements our design."

---

## Slide 15 — Mode Comparison [PUGAL]

> "We compared against a pure ABAC baseline — Mode A. The result is stark: pure ABAC only gets 43.2% agreement with our authoritative blockchain decisions across 720 requests.
>
> Why? Because pure ABAC doesn't know about degraded trust or elevated environmental risk. A guest with a valid booking gets ALLOW from pure ABAC — even when trust is 20 and the request is coming from a suspicious foreign network. Our system correctly DENYs those."

---

## Slide 16 — Gas Results [PUGAL]

> "Every on-chain evaluation consumed exactly 31,863 gas — perfectly deterministic. No EVM path variation.
>
> Even more importantly: 299 out of 720 requests were rejected pre-EVM by the gateway. Those 299 consumed zero gas. The short-circuit gates save 41.5% of potential blockchain invocations."

---

## Slide 17 — Security Invariants [PUGAL]

> "We verified seven security invariants. Fail-closed is confirmed: blockchain down → DENY in 0.1ms. Anti-double-authorization is confirmed: one request always produces at most one blockchain transaction. Privilege attenuation: RESTRICT correctly clamps device operations. Non-repudiation: every authorization has an immutable on-chain receipt."

---

## Slide 18 — Contributions [PUGAL introduces, BHARATH reads their own]

> Pugalenthi: "My contributions cover the entire authorization pipeline — from AbacService through TrustService, RiskService, the smart contract, DecisionCoordinator, ResourceOperationService, and the offline batch auditor. I also designed and analyzed the Phase 8B experiment."
>
> Bharath: "My contributions are the IoT simulation engine with 10 device types, the full RabbitMQ event pipeline with idempotency guard, the WebSocket STOMP broker, and the real-time dashboard. I'll also be running the live demo."

---

## Slide 19 — Demo Preview [BHARATH]

> "Here's our demo plan. We'll start infrastructure, open the dashboard, then run each scenario live — ALLOW, RESTRICT, DENY by low trust, DENY by high risk, DENY by ABAC failure, blockchain outage, recovery, and finally batch audit with idempotency. The whole demo runs about 10 minutes."

---

## Slide 20 — Limitations [PUGAL]

> "We want to be transparent about scope. We evaluated on a local Ganache testnet — we make no claims about Ethereum mainnet scalability. Our IoT devices are simulated — no physical hardware energy measurements. Our benchmark is sequential — concurrent throughput is not claimed. These are honest scope limits, not design failures."

---

## Slide 21 — Conclusion [PUGAL]

> "To conclude: TrustABAC-IoT is a complete, working, empirically validated platform. 6 integrated subsystems, 720 measured requests, 100% behavioral correctness. We proved that adding behavioral trust and contextual risk to ABAC — backed by blockchain authority — correctly handles 56.8% more security-critical scenarios than pure ABAC alone.
>
> The fail-closed mechanism, privilege attenuation, and gas-efficient gateway filtering together make this a robust and honest contribution to IoT access control research."

---

## Slide 22 — Q&A [BOTH]

**Anticipated questions and brief answers:**

1. *"Why use blockchain instead of just a database?"*
   → "A database can be modified by an administrator. The smart contract is immutable — the decision rules cannot be changed after deployment without explicit owner re-deployment. This gives cryptographic non-repudiation."

2. *"What happens if the gateway itself is compromised?"*
   → "We acknowledge the gateway oracle trust assumption as a stated scope limitation. The gateway computes attributes — we do not claim to solve oracle security."

3. *"Why not test on a real Ethereum network?"*
   → "Public network evaluation was out of scope — gas fees, variable block times, and network latency would confound our controlled latency measurements. We clearly state this in our limitations."

4. *"What does RESTRICT actually do to the device?"*
   → "It depends on the device type. For a door lock, it prevents unlocking but returns read-only status. For a thermostat, it clamps the temperature to a safe 20–24°C range. The ResourceOperationService has type-specific attenuation logic."

5. *"Are the 720 requests truly independent?"*
   → "They are independent within statistical analysis terms — each request has independent trust/risk inputs. They share the same device models within a repetition, but state is reset between repetitions. We clearly distinguish request count, repetition count, and scenario count."
