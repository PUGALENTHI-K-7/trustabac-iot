# TrustABAC-IoT — Review 2 Demo Runbook

**Version:** Review 2 | **Date:** September 2026
**Team:** Pugalenthi K (24MIC0082) · RK Bharath (24MIC0025)

---

## PRE-DEMO CHECKLIST (Complete 10 minutes before presentation)

- [ ] Java 21 installed and on PATH (`java -version`)
- [ ] Maven 3.9 installed (`mvn -version`)
- [ ] Docker Desktop running
- [ ] All 3 Docker containers healthy (see Step 1)
- [ ] Spring Boot application compiled (`mvn compile -q`)
- [ ] Browser tab open at `http://localhost:8090/dashboard`
- [ ] `.env` file in project root with correct credentials

---

## STEP 1 — Verify Infrastructure Containers

```powershell
# From project root: J:\PROJECT\TRUST -ABAC\trustabac-iot
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
```

**Expected output — all 3 containers UP:**
```
NAMES                  STATUS          PORTS
trustabac-mysql      Up X minutes    0.0.0.0:3307->3306/tcp
trustabac-rabbitmq     Up X minutes    0.0.0.0:5672->5672/tcp, 0.0.0.0:15672->15672/tcp
trustabac-ganache      Up X minutes    0.0.0.0:8545->8545/tcp
```

**If any container is down:**
```powershell
docker-compose up -d
# Wait 15 seconds, then re-verify
docker ps
```

---

## STEP 2 — Start Spring Boot Gateway

```powershell
# Option A — Use the batch file (recommended for demo)
.\start-app.bat

# Option B — Maven directly
.\mvnw.cmd spring-boot:run
```

**Wait for ready message in logs:**
```
Started TrustAbacIotApplication in X.XXX seconds
```

---

## STEP 3 — Verify Health Endpoint

Open browser: **`http://localhost:8090/api/health`**

**Expected JSON response:**
```json
{
  "status": "UP",
  "components": {
    "db": {"status": "UP"},
    "rabbit": {"status": "UP"}
  }
}
```

---

## STEP 4 — Open Dashboard

Browser: **`http://localhost:8090/dashboard`**

**What to verify:**
- Page loads with 10 device tiles
- STOMP WebSocket connection indicator shows "Connected"
- All devices show initial state (DOOR: LOCKED, devices: ONLINE)
- Trust scores show baseline 80.0

---

## DEMO SEQUENCE

### Demo A — Normal Access (ALLOW → EXECUTED) [BHARATH]

```powershell
# Option 1: REST trigger
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/NORMAL_STAY/run" -Method POST -ContentType "application/json" -Body "{}"

# Option 2: Browser URL (GET alternative if POST fails)
# http://localhost:8090/api/simulator/scenarios/NORMAL_STAY/run
```

**Show on dashboard:**
- DOOR-SENSOR-001 tile updates to UNLOCKED
- Authorization event appears: `ALLOW | EXECUTED | tx: 0x...`
- Trust score remains at 80.0+

**Talking points:**
- "Guest has valid booking, trust=80, risk=low → ALLOW"
- "Smart contract returned ALLOW with reason code 0, 31,863 gas consumed"
- "Door state actuated in simulator"

---

### Demo B — Privilege Attenuation (RESTRICT → DOWNGRADED) [BHARATH]

```powershell
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/RESTRICT_ENFORCEMENT/run" -Method POST -ContentType "application/json" -Body "{}"
```

**Show on dashboard:**
- Event log shows `RESTRICT | DOWNGRADED`
- Door remains LOCKED despite operation request
- Thermostat temperature clamped to 20–24°C safe range
- Reason code 5 displayed

**Talking points:**
- "Tenant on untrusted network → risk elevated → RESTRICT"
- "Smart contract returns RESTRICT, ResourceOperationService clamps the operation"
- "No full denial — user gets read-only telemetry"

---

### Demo C — Security Denial (DENY → BLOCKED) [BHARATH + PUGAL]

#### C1 — Low Trust Attack
```powershell
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/LOW_TRUST_ATTACK/run" -Method POST -ContentType "application/json" -Body "{}"
```
**Dashboard shows:** Trust score < 30, Decision: `DENY | BLOCKED | code:3`

#### C2 — High Risk Attack
```powershell
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/HIGH_RISK_ATTACK/run" -Method POST -ContentType "application/json" -Body "{}"
```
**Dashboard shows:** `DENY | BLOCKED | code:4` — Note: zero blockchain transactions (pre-EVM filter)

#### C3 — ABAC Failure
```powershell
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/UNAUTHORIZED_SENSITIVE_ACCESS/run" -Method POST -ContentType "application/json" -Body "{}"
```
**Dashboard shows:** `DENY | BLOCKED | code:1` — Gate 1 rejection, no trust/risk/EVM called

---

### Demo D — Fail-Closed Blockchain Outage [PUGAL]

```powershell
# Step 1: Pause Ganache
docker pause trustabac-ganache

# Step 2: Submit an operation (show it gets denied instantly)
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/NORMAL_STAY/run" -Method POST -ContentType "application/json" -Body "{}"
```

**Show:** Response arrives in < 0.5 ms with `DENY | BLOCKED`
**Talking point:** "Gateway has fail-closed handler — zero chance of accidental ALLOW during outage"

```powershell
# Step 3: Restore Ganache
docker unpause trustabac-ganache

# Step 4: Demonstrate recovery
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/NORMAL_STAY/run" -Method POST -ContentType "application/json" -Body "{}"
```

**Show:** System immediately returns to `ALLOW | EXECUTED` upon restoration.

---

### Demo E — Spring Batch Offline Audit [PUGAL]

```powershell
# Trigger audit job
Invoke-RestMethod -Uri "http://localhost:8090/api/batch/audit/trigger?periodKey=DEMO_REVIEW2_01" -Method POST

# Fetch latest report
Invoke-RestMethod -Uri "http://localhost:8090/api/batch/audit/reports/latest" -Method GET

# Trigger SAME period again to show idempotency
Invoke-RestMethod -Uri "http://localhost:8090/api/batch/audit/trigger?periodKey=DEMO_REVIEW2_01" -Method POST
```

**Show:** Second trigger returns `alreadyProcessed: true` — no duplicate rows in MySQL.

---

## DEMO RESET

```powershell
# Full system state reset — restores all 10 devices to baseline
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/reset" -Method POST -ContentType "application/json" -Body "{}"
```

**After reset:** All device tiles show default ONLINE state, trust baseline 80.0.

---

## TROUBLESHOOTING

| Problem | Fix |
|:---|:---|
| Spring Boot not starting | Check `.env` file exists; run `docker ps` to verify MySQL/RabbitMQ up |
| Dashboard shows "Disconnected" | Hard-refresh browser (Ctrl+Shift+R); check gateway is running |
| Ganache container not found | Run `docker-compose up -d trustabac-ganache` |
| Scenario 404 | Verify Spring Boot started fully; check logs for "Started in X seconds" |
| Batch job stuck | Check `batch_run_audit` table in MySQL; restart Spring Boot |
