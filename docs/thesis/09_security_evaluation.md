# Chapter 9 — Security Evaluation

## 9.1 Security Evaluation Framework
The security posture of the TrustABAC-IoT platform was evaluated through empirical attack scenarios, failure injection tests, and automated static/runtime security audits. The evaluation focuses on validating core security invariants and resilience properties against the threat vectors established in Chapter 4.

---

## 9.2 Invariant Verification Across Threat Scenarios

### Table 9.1: Empirical Threat Mitigation and Security Invariant Matrix

| Threat Identifier | Evaluated Scenario | Expected Behavior | Observed Outcome | Security Verdict |
| :--- | :--- | :--- | :--- | :---: |
| **T-01: Unauthorized Subject Access** | `ABAC_FAILURE` | Reject unmapped / unregistered subjects at Gate 1. Zero state mutation. | 90 / 90 requests rejected at Gate 1. Zero blockchain tx. | **PASS (100%)** |
| **T-02: Temporal Boundary Violation** | `PRE_CHECKIN` / `POST_CHECKOUT` | Block device operations outside reservation timestamps. | 100% of expired / pre-checkin requests blocked. | **PASS (100%)** |
| **T-03: Compromised / Low-Trust Subject** | `LOW_TRUST` | Subject with degraded reputation ($T=20.0 < 30.0$) denied on-chain. | 90 / 90 requests evaluated on-chain $\to$ `DENY` / `BLOCKED`. | **PASS (100%)** |
| **T-04: Contextual Anomaly / Burst Flooding** | `HIGH_RISK` | Extreme risk ($R > 70.0$) terminated pre-blockchain at gateway. | 90 / 90 requests rejected pre-EVM. Zero gas spent. | **PASS (100%)** |
| **T-05: Privilege Escalation on Sensitive Items** | `UNAUTHORIZED_SENSITIVE_ACCESS` | Block guest attempts on camera, router admin, owner settings. | 100% of unauthorized sensitive resource requests blocked. | **PASS (100%)** |
| **T-06: Intermediate Risk Operation Exposure** | `RESTRICT_ACCESS` | Moderate risk triggers operation attenuation and parameter clamping. | 90 / 90 requests downgraded to status reads or eco-bounds. | **PASS (100%)** |
| **T-07: Blockchain Infrastructure Outage** | `BLOCKCHAIN_OUTAGE` | Unreachable EVM RPC triggers immediate fail-closed denial. | 90 / 90 requests rejected in $0.100$ ms. Zero device leaks. | **PASS (100%)** |
| **T-08: Transaction Replay / Double-Auth** | Multiple Re-runs | Enforce single evaluation per logical request and idempotent audit. | Re-runs processed without duplicate rows or tx replay. | **PASS (100%)** |

---

## 9.3 Detailed Analysis of Key Security Mechanisms

### 1. Fail-Closed Resilience During Blockchain Outages
A critical vulnerability in blockchain-assisted IoT systems is fail-open behavior during network partitions or validator outages. In TrustABAC-IoT, `DecisionCoordinator` wraps all Web3j RPC communications in an explicit exception handler. When a JSON-RPC timeout, transport error, or revert is detected, the gateway immediately generates a synthetic **Fail-Closed `DENY`** response:
- **Observed Fast-Fail Latency**: $0.100 \pm 0.000$ ms.
- **Actuation Outcome**: $0.00$ operations permitted (`BLOCKED`).
- **Post-Recovery Behavior**: Following RPC reconnection (`RECOVERY` scenario), normal access resumes seamlessly ($61.217$ ms auth latency, $100\%$ `ALLOW`).

### 2. Multi-Tiered Privilege Attenuation (`RESTRICT`)
Unlike binary authorization models that force a trade-off between complete denial and full access, TrustABAC-IoT enforces fine-grained privilege attenuation:
- **Smart Door Lock**: Under `RESTRICT` ($30 \le R \le 70$), door unlocking commands are suppressed (`lockState` remains `LOCKED`), while read-only status queries (battery level, lock state) are permitted.
- **Smart Thermostat**: Under `RESTRICT`, tenant temperature setpoints are clamped to a safe ecological operating window ($20.0^\circ\text{C} \le T_{target} \le 24.0^\circ\text{C}$), preventing excessive power draw.

### 3. Anti-Double-Authorization and Idempotency Guarantees
The architecture enforces strict transactional invariants:
- **1 Logical Request $\longrightarrow$ 1 Authorization Evaluation $\longrightarrow$ At Most 1 Blockchain Transaction**.
- Validated across all 720 measured requests: zero instances of duplicate blockchain transactions or dangling authorizations occurred.
- In the offline analytical tier (Spring Batch), unique composite database keys `(periodKey, periodStart, periodEnd)` prevent duplicate summary insertion during repeated analytical runs.

---

## 9.4 Secret Sanitization and Static Security Audit
To prevent credential leaks in open-source and multi-party deployments, an automated repository-wide security scan was executed across 282 codebase files (`contracts/security_scan_final.py`):
- **Embedded Private Keys**: 0 detected (Hardcoded keys purged; dynamic ephemeral key derivation used).
- **Plaintext Passwords**: 0 detected (Replaced with `.env` parameter bindings).
- **Observational Client JavaScript**: 0 embedded secrets and 0 client-side decision logic routines.
