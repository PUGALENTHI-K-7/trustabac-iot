"""
Phase 6B Outage + Recovery - Automated (non-interactive)
Controls Docker container programmatically via subprocess.
"""
import json
import subprocess
import time
import urllib.request
import urllib.error

BASE = "http://localhost:8090"

def http_post(path, body):
    data = json.dumps(body).encode()
    req = urllib.request.Request(f"{BASE}{path}", data=data,
                                  headers={"Content-Type": "application/json"}, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())
    except Exception as e:
        return 0, {"error": str(e)}

def http_get(path):
    req = urllib.request.Request(f"{BASE}{path}", method="GET")
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read())
    except Exception as e:
        return 0, {"error": str(e)}

def docker(cmd):
    result = subprocess.run(f"docker {cmd}", shell=True, capture_output=True, text=True)
    return result.returncode, result.stdout.strip(), result.stderr.strip()

def banner(msg):
    print("\n" + "="*60)
    print(msg)
    print("="*60)

SENSITIVE_REQUEST = {
    "deviceIdentifier": "DOOR-SENSOR-001",
    "userId": "guest-user-001",
    "role": "GUEST",
    "organization": "SmartRental",
    "resource": "door-sensor",
    "operation": "CONTROL",
    "location": "Property-001",
    "bookingId": "BOOKING-PHASE6B-001",
    "networkContext": "INTERNAL",
    "requestCountWindow": 2,
    "recentViolationCount": 0,
    "behavioralIndicator": "NORMAL"
}

# ── RESTORE TRUST FIRST ──────────────────────────────────────
banner("PRE-OUTAGE SETUP: Restore trust to HIGH")
for i in range(3):
    s, r = http_post("/api/trust/DOOR-SENSOR-001/events", {
        "eventType": "RECOVERY",
        "reason": "Outage test: trust restore",
        "source": "phase6b_outage"
    })
    print(f"  RECOVERY {i+1}: HTTP {s} -> {r.get('newTrust') or r.get('message','?')}")
    time.sleep(0.3)

_, trust_check = http_get("/api/trust/DOOR-SENSOR-001")
print(f"  Trust now: {trust_check.get('currentTrust')}")

# ── PRE-OUTAGE: Confirm ALLOW works ──────────────────────────
banner("STEP 1 - PRE-OUTAGE: Verify normal ALLOW")
s1, r1 = http_post("/api/authorization/evaluate", {**SENSITIVE_REQUEST, "requestReference": "OUTAGE-PRE"})
pre_decision = r1.get("finalDecision")
pre_tx = r1.get("blockchainTransactionHash")
pre_block = r1.get("blockchainBlockNumber")
print(f"  HTTP {s1}: decision={pre_decision} tx={pre_tx} block={pre_block}")
print(f"  PRE-OUTAGE: {'PASS' if pre_decision in ('ALLOW','RESTRICT') else 'WARN: ' + str(pre_decision)}")

# ── STOP GANACHE ─────────────────────────────────────────────
banner("STEP 2 - STOP GANACHE CONTAINER")
rc, out, err = docker("stop ganache-trustabac")
print(f"  docker stop rc={rc} out={out}")
print("  Waiting 5s for Ganache to fully stop...")
time.sleep(5)

# Verify RPC is down
_, status_down = http_get("/api/blockchain/status")
print(f"  Blockchain status (should be down): rpcReachable={status_down.get('rpcReachable')}")

# ── DURING OUTAGE: Sensitive request must DENY (fail-closed) ─
banner("STEP 3 - DURING OUTAGE: Sensitive request (must DENY fail-closed)")
s3, r3 = http_post("/api/authorization/evaluate", {**SENSITIVE_REQUEST, "requestReference": "OUTAGE-DURING"})
during_decision = r3.get("finalDecision")
during_tx       = r3.get("blockchainTransactionHash")
during_reason   = r3.get("decisionReason")

print(f"  HTTP {s3}: decision={during_decision}")
print(f"  Transaction Hash  : {during_tx}")
print(f"  Reason            : {during_reason}")

fail_closed = (during_decision == "DENY" and during_tx is None)
print(f"\n  FAIL-CLOSED: {'PASS - DENY with no tx hash' if fail_closed else 'FAIL - Expected DENY, got: ' + str(during_decision)}")

# ── RESTART GANACHE ──────────────────────────────────────────
banner("STEP 4 - RESTART GANACHE CONTAINER")
rc2, out2, err2 = docker("start ganache-trustabac")
print(f"  docker start rc={rc2} out={out2}")
print("  Waiting 8s for Ganache to be ready...")
time.sleep(8)

# Check recovery
_, status_up = http_get("/api/blockchain/status")
print(f"  Blockchain status after restart: rpcReachable={status_up.get('rpcReachable')} contractReachable={status_up.get('contractReachable')} block={status_up.get('latestBlock')}")

# ── POST-RECOVERY: New authorization must produce real tx ────
banner("STEP 5 - POST-RECOVERY: Verify authorization works again")
s5, r5 = http_post("/api/authorization/evaluate", {**SENSITIVE_REQUEST, "requestReference": "OUTAGE-RECOVERY"})
rec_decision = r5.get("finalDecision")
rec_tx       = r5.get("blockchainTransactionHash")
rec_block    = r5.get("blockchainBlockNumber")
rec_reason   = r5.get("decisionReason")

print(f"  HTTP {s5}: decision={rec_decision} tx={rec_tx} block={rec_block}")
print(f"  Reason: {rec_reason}")

recovered = (rec_decision in ("ALLOW","RESTRICT","DENY") and rec_tx is not None)
print(f"\n  RECOVERY: {'PASS - New real tx hash on chain' if recovered else 'FAIL - No tx after recovery'}")

# ── SUMMARY ──────────────────────────────────────────────────
banner("OUTAGE + RECOVERY SUMMARY")
print(f"  Pre-outage      : decision={pre_decision} tx={pre_tx} block={pre_block}")
print(f"  During outage   : decision={during_decision} tx={during_tx} fail_closed={'YES' if fail_closed else 'NO'}")
print(f"  Recovery reason : {during_reason}")
print(f"  Post-recovery   : decision={rec_decision} tx={rec_tx} block={rec_block}")
print(f"\n  OUTAGE FAIL-CLOSED : {'PASS' if fail_closed else 'FAIL'}")
print(f"  RECOVERY           : {'PASS' if recovered else 'FAIL'}")

result = {
    "pre_outage": {"decision": pre_decision, "tx": pre_tx, "block": str(pre_block)},
    "during_outage": {
        "decision": during_decision, "tx": during_tx, "reason": during_reason,
        "fail_closed": fail_closed, "rpc_reachable": status_down.get("rpcReachable")
    },
    "post_recovery": {
        "decision": rec_decision, "tx": rec_tx, "block": str(rec_block),
        "rpc_reachable": status_up.get("rpcReachable"), "recovered": recovered
    }
}
with open("contracts/phase6b_outage.json", "w") as f:
    json.dump(result, f, indent=2, default=str)
print("  Saved: contracts/phase6b_outage.json")
