# TrustABAC-IoT — Review 2 Demo Commands Reference Card

**Quick reference for live demo execution**
**Team:** 24MIC0082 Pugalenthi · 24MIC0025 Bharath

---

## 1. Infrastructure Commands

```powershell
# Check all containers
docker ps --format "table {{.Names}}\t{{.Status}}"

# Start all services
docker-compose up -d

# Start gateway (from project root)
.\start-app.bat
```

---

## 2. Health & Dashboard URLs

| URL | Purpose |
|:----|:--------|
| `http://localhost:8090/api/health` | System health (UP/DOWN) |
| `http://localhost:8090/dashboard` | Real-time observational dashboard |
| `http://localhost:15672` | RabbitMQ Management (guest/guest) |

---

## 3. Scenario Trigger Commands

```powershell
# NORMAL ACCESS — ALLOW → EXECUTED
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/NORMAL_STAY/run" -Method POST -ContentType "application/json" -Body "{}"

# RESTRICT — RESTRICT → DOWNGRADED
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/RESTRICT_ENFORCEMENT/run" -Method POST -ContentType "application/json" -Body "{}"

# LOW TRUST ATTACK — DENY → BLOCKED (code:3)
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/LOW_TRUST_ATTACK/run" -Method POST -ContentType "application/json" -Body "{}"

# HIGH RISK ATTACK — Pre-EVM DENY → BLOCKED (code:4)
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/HIGH_RISK_ATTACK/run" -Method POST -ContentType "application/json" -Body "{}"

# ABAC FAILURE — Gate 1 DENY → BLOCKED (code:1)
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/UNAUTHORIZED_SENSITIVE_ACCESS/run" -Method POST -ContentType "application/json" -Body "{}"
```

---

## 4. Blockchain Outage Sequence

```powershell
# 1. Pause Ganache (simulate outage)
docker pause ganache-trustabac

# 2. Try access during outage (will get 0.1ms DENY)
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/NORMAL_STAY/run" -Method POST -ContentType "application/json" -Body "{}"

# 3. Restore Ganache
docker unpause ganache-trustabac

# 4. Confirm recovery (will get ALLOW again)
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/scenarios/NORMAL_STAY/run" -Method POST -ContentType "application/json" -Body "{}"
```

---

## 5. Spring Batch Audit Commands

```powershell
# Trigger audit
Invoke-RestMethod -Uri "http://localhost:8090/api/batch/audit/trigger?periodKey=DEMO_REVIEW2_01" -Method POST

# Fetch latest report
Invoke-RestMethod -Uri "http://localhost:8090/api/batch/audit/reports/latest" -Method GET

# Re-trigger same period (shows idempotency: alreadyProcessed: true)
Invoke-RestMethod -Uri "http://localhost:8090/api/batch/audit/trigger?periodKey=DEMO_REVIEW2_01" -Method POST
```

---

## 6. Reset

```powershell
# Reset all device states to baseline
Invoke-RestMethod -Uri "http://localhost:8090/api/simulator/reset" -Method POST -ContentType "application/json" -Body "{}"
```

---

## 7. Expected Decision Reference

| Scenario | Decision | Code | Gas Consumed |
|:---------|:--------:|:----:|:------------:|
| NORMAL_STAY | ALLOW | 0 | 31,863 |
| RESTRICT_ENFORCEMENT | RESTRICT | 5 | 31,863 |
| LOW_TRUST_ATTACK | DENY | 3 | 31,863 |
| HIGH_RISK_ATTACK | DENY | 4 | **0** (pre-EVM) |
| UNAUTHORIZED_SENSITIVE_ACCESS | DENY | 1 | **0** (Gate 1) |
| During outage | DENY | — | **0** (fail-closed) |
