# TrustABAC-IoT: Final Demonstration Script

## Overview & Demo Parameters
- **Target Duration**: 10–15 Minutes.
- **Audience**: Academic Examiners, Thesis Committee, Industry Evaluators.
- **Demonstration Goal**: Showcase the live, end-to-end multi-tiered access control pipeline, smart contract decision execution, software-level privilege attenuation, real-time WebSocket telemetry, fault-tolerant fail-closed handling, and offline batch analytics.

---

## Demonstration Sequence

### Step 1: Infrastructure Initialization & Health Verification (1.5 min)
1. **Show Docker Containers**:
   ```bash
   docker ps
   ```
   *Verify `trustabac-mysql` (port 3307), `trustabac-rabbitmq` (port 5672/15672), and `trustabac-ganache` (port 8545).*
2. **Launch Spring Boot Gateway**:
   ```bash
   start-app.bat
   ```
3. **Verify Health Endpoint**:
   Open `http://localhost:8090/api/health` in browser $\longrightarrow$ Show `status: UP`.
4. **Open Observational Dashboard**:
   Navigate to `http://localhost:8090/dashboard` $\longrightarrow$ Show active connection to STOMP WebSocket broker and 10 initialized IoT device tiles.

---

### Step 2: Normal Tenant Access & Smart Contract Execution (2.0 min)
1. **Scenario**: Guest user `GUEST-001` with active booking `BK-1001` unlocks the Smart Door Lock (`DOOR-SENSOR-001`).
2. **Trigger Request via REST or Simulator UI**:
   - `POST /api/simulator/scenarios/NORMAL_STAY/run`
3. **Show Dashboard Live Updates**:
   - Device tile updates: `DOOR-SENSOR-001` transitions from `LOCKED` to `UNLOCKED`.
   - Telemetry event log: Displays `ALLOW` decision ($reasonCode = 0$), `EXECUTED` status, and on-chain transaction hash (`0x...`).
4. **Inspect Ganache Block Explorer / Logs**:
   - Point out `AuthorizationEvaluated` event emitted with 31,863 gas.

---

### Step 3: Privilege Attenuation & Operation Downgrading (`RESTRICT`) (2.0 min)
1. **Scenario**: Tenant attempts door lock control over an untrusted external cellular network ($30 \le R \le 70$).
2. **Trigger Request**:
   - `POST /api/simulator/scenarios/RESTRICT_ENFORCEMENT/run`
3. **Show Decision and Enforcement**:
   - Smart contract outputs `RESTRICT` ($reasonCode = 5$).
   - `ResourceOperationService` clamps physical command: Door remains `LOCKED`, while read-only status telemetry is returned (`DOWNGRADED`).
   - Thermostat temperature requested at $16.0^\circ\text{C}$ is clamped to safe eco-bound $24.0^\circ\text{C}$.

---

### Step 4: Security Protections (Low Trust, High Risk, ABAC Gate) (3.0 min)
1. **Demonstrate Low Trust Denial**:
   - Trigger `LOW_TRUST_ATTACK` scenario (Trust score degraded to $20.0 < 30.0$).
   - Smart contract returns `DENY` ($reasonCode = 3$). Enforcement returns `BLOCKED`.
2. **Demonstrate High Risk Pre-EVM Denial**:
   - Trigger `HIGH_RISK_ATTACK` scenario ($R > 70.0$).
   - Gateway short-circuits request before blockchain $\longrightarrow$ zero gas consumed.
3. **Demonstrate ABAC Gate 1 Failure**:
   - Trigger `UNAUTHORIZED_SENSITIVE_ACCESS` scenario (Guest attempting camera or router administration).
   - Gate 1 rejects access immediately $\longrightarrow$ `DENY` / `BLOCKED`.

---

### Step 5: Fail-Closed Blockchain Outage & Recovery (2.5 min)
1. **Simulate Blockchain Failure**:
   - Pause or disconnect Ganache EVM container: `docker pause trustabac-ganache`.
2. **Execute Operation During Outage**:
   - Submit door control request.
   - Show gateway intercepts exception in $0.100$ ms and strictly enforces **Fail-Closed `DENY` / `BLOCKED`**.
3. **Restore Blockchain Node**:
   - `docker unpause trustabac-ganache`.
4. **Demonstrate Recovery**:
   - Submit follow-up request $\longrightarrow$ System resumes normal `ALLOW` / `EXECUTED` operations immediately.

---

### Step 6: Offline Auditing & Spring Batch Reconciliation (1.5 min)
1. **Trigger Batch Audit Job**:
   - `POST /api/batch/audit/trigger?periodKey=DEMO_AUDIT_01`
2. **Inspect Analytical Report**:
   - `GET /api/batch/audit/reports/latest`
3. **Demonstrate Idempotency**:
   - Trigger the identical `periodKey=DEMO_AUDIT_01` again.
   - Show `alreadyProcessed: true` with zero duplicate summary rows in MySQL.

---

### Step 7: Final Demo Reset & Conclusion (1.0 min)
1. **Reset Demo State**:
   - `POST /api/simulator/reset`
2. **Show Clean Baseline**:
   - All 10 devices restored to default online state and $80.0$ baseline trust.
