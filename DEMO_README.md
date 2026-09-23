# TrustABAC-IoT — Demo Quick Start Guide

**Project**: TrustABAC-IoT — Adaptive Trust- and Risk-Aware Smart-Contract Access Control for Resource-Constrained IoT Networks
**Team**: 24MIC0082 Pugalenthi K · 24MIC0025 RK Bharath

---

## Start

Double-click:

```
START_DEMO.bat
```

The launcher will automatically:

1. Verify Docker, Java, and the application JAR are available
2. Load environment variables from `.env`
3. Start MySQL, RabbitMQ, and Ganache in Docker
4. Kill any stale process on port 8090
5. Start the Spring Boot application from `target\trustabac-iot-0.0.1-SNAPSHOT.jar`
6. Wait until the application is healthy (up to 120 seconds)
7. Verify all backend subsystems (MySQL, RabbitMQ, Ganache, Smart Contract, WebSocket)
8. Check demo baseline state (DOOR-SENSOR-001 locked, active booking present)
9. Open the browser automatically at the dashboard

Wait until the terminal shows:

```
TRUSTABAC-IoT DEMO READY
```

Then the primary demo interface is ready:

```
http://127.0.0.1:8090/dashboard
```

> **Faculty demonstration**: Use the browser dashboard as the primary interface.
> PowerShell commands are only needed for the backup/developer scenario demonstration.

---

## Stop

Double-click:

```
STOP_DEMO.bat
```

This safely stops:
- The Spring Boot process (using saved PID)
- MySQL, RabbitMQ, and Ganache Docker containers

**Data is preserved** — containers and database volumes are NOT removed.

---

## Restart

Simply double-click `START_DEMO.bat` again.  
The launcher is idempotent: if infrastructure containers are already running it will continue without re-creating them.

---

## Important URLs

| URL | Purpose |
|:---|:---|
| `http://127.0.0.1:8090/dashboard` | **Primary demo interface** |
| `http://127.0.0.1:8090/api/health` | Application health |
| `http://127.0.0.1:8090/api/blockchain/status` | Ganache RPC + smart contract status |
| `http://127.0.0.1:8090/api/messaging/status` | RabbitMQ pipeline status |
| `http://127.0.0.1:8090/api/websocket/status` | WebSocket broker status |
| `http://127.0.0.1:8090/api/devices` | Registered IoT devices |
| `http://127.0.0.1:8090/api/bookings` | Active bookings |
| `http://127.0.0.1:8090/api/simulator/status` | IoT simulator state |
| `http://127.0.0.1:15672` | RabbitMQ Management UI (guest / guest) |

---

## Demo Scenario Commands (PowerShell)

Run these from a PowerShell window opened in the project folder:

```powershell
# Normal access — ALLOW → EXECUTED
Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/simulator/scenarios/NORMAL_STAY/run" -Method POST -ContentType "application/json" -Body "{}"

# RESTRICT enforcement — RESTRICT → DOWNGRADED
Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/simulator/scenarios/RESTRICT_ENFORCEMENT/run" -Method POST -ContentType "application/json" -Body "{}"

# Low trust attack — DENY → BLOCKED
Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/simulator/scenarios/LOW_TRUST_ATTACK/run" -Method POST -ContentType "application/json" -Body "{}"

# High risk attack — pre-EVM DENY → BLOCKED (0 gas)
Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/simulator/scenarios/HIGH_RISK_ATTACK/run" -Method POST -ContentType "application/json" -Body "{}"

# Blockchain outage — Fail-Closed DENY
docker pause trustabac-ganache
Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/simulator/scenarios/NORMAL_STAY/run" -Method POST -ContentType "application/json" -Body "{}"
docker unpause trustabac-ganache

# Batch audit
Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/batch/audit/trigger?periodKey=DEMO_01" -Method POST
Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/batch/audit/reports/latest" -Method GET

# Reset simulator state
Invoke-RestMethod -Uri "http://127.0.0.1:8090/api/simulator/reset" -Method POST -ContentType "application/json" -Body "{}"
```

---

## Log Files

| File | Contents |
|:---|:---|
| `logs\demo-app.log` | Spring Boot stdout |
| `logs\demo-app-error.log` | Spring Boot stderr |
| `logs\demo-app.pid` | Saved application PID (used by STOP_DEMO.bat) |

---

## Troubleshooting

### Port 8090 already occupied

The launcher automatically detects and stops the process on port 8090 before starting Spring Boot.
If it cannot stop the process (e.g., the process requires administrator rights), you will see:

```
Could not stop PID XXXX
Please stop the process manually and rerun.
```

Manually run:
```powershell
Stop-Process -Id XXXX -Force
```
Then restart `START_DEMO.bat`.

---

### Docker not running

You will see:
```
ERROR: Docker daemon is not running. Please start Docker Desktop.
```

**Fix**: Start Docker Desktop from the Windows Start menu and wait until the Docker icon in the system tray is solid (not animating), then double-click `START_DEMO.bat` again.

---

### MySQL not ready

The launcher polls MySQL for up to 60 seconds. If it times out you will see:
```
MySQL: still initializing (proceeding)
```

If the Spring Boot startup then fails with a database connection error, wait 15 seconds and run `START_DEMO.bat` again. MySQL sometimes takes longer on first boot when initializing the schema.

---

### RabbitMQ not ready

If the messaging status check shows `brokerReachable=false`, the RabbitMQ container may still be initializing.
Wait 15 seconds and restart.

---

### Ganache unavailable

If you see `Ganache RPC: FAIL`, verify:
```powershell
docker ps --filter name=trustabac-ganache
```
If not running:
```powershell
docker compose up -d ganache
```

---

### Smart contract address mismatch

You may see:
```
Smart Contract    WARN
  Contract at 0x... may not be deployed on current Ganache instance.
```

This means the `BLOCKCHAIN_CONTRACT_ADDRESS` in `.env` does not match the contract deployed on the current Ganache instance.

**Cause**: Ganache uses a deterministic wallet. If Ganache has been restarted with the same seed, the contract address should match. If Ganache was restarted with a different configuration, the deployment address changes.

**Fix**: Check `.env` `BLOCKCHAIN_CONTRACT_ADDRESS` and compare with the deployed address in the Spring Boot startup logs (`logs\demo-app.log`), or re-deploy the contract via:
```
POST http://127.0.0.1:8090/api/blockchain/deploy
```
> **Note**: Re-deploying creates a new contract address. Update `BLOCKCHAIN_CONTRACT_ADDRESS` in `.env` if you re-deploy.

---

### Application startup failure

If Spring Boot fails to start, the launcher will show the last startup error lines and the log paths.

Check:
```
logs\demo-app.log
logs\demo-app-error.log
```

Common causes:
- MySQL not accepting connections yet (retry after 30 seconds)
- `.env` missing or has incorrect credentials
- JAR not built — run `mvnw.cmd package -DskipTests`
