"""
Phase 6B Final Fixed Scenario Runner
Corrects: trust event endpoint (plural /events), extreme risk inputs for DENY.
"""
import json
import time
import urllib.request
import urllib.error
from datetime import datetime, timedelta, timezone

BASE = "http://localhost:8090"

def http(method, path, body=None):
    data = json.dumps(body).encode() if body is not None else None
    headers = {"Content-Type": "application/json"} if data else {}
    req = urllib.request.Request(f"{BASE}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            raw = r.read()
            return r.status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        raw = e.read()
        return e.code, json.loads(raw) if raw else {}

def post(path, body): return http("POST", path, body)
def get(path):        return http("GET",  path)

def banner(msg):
    print("\n" + "="*60)
    print(msg)
    print("="*60)

# ──────────────────────────────────────────────────────────────
# VERIFY EXISTING DATA
# ──────────────────────────────────────────────────────────────
banner("VERIFY EXISTING STATE")
_, devs  = get("/api/devices")
_, pols  = get("/api/policies")
_, books = get("/api/bookings")
_, b_status = get("/api/blockchain/status")
_, b_thresh = get("/api/blockchain/thresholds")

print(f"  Devices       : {len(devs)}")
print(f"  Policies      : {len(pols)}")
print(f"  Bookings      : {len(books)}")
print(f"  Blockchain    : rpcReachable={b_status.get('rpcReachable')} contractReachable={b_status.get('contractReachable')}")
print(f"  Contract Addr : {b_status.get('contractAddress')}")
print(f"  Thresholds    : trustHigh={b_thresh.get('trustHigh')} trustMed={b_thresh.get('trustMedium')} riskLow={b_thresh.get('riskLow')} riskMed={b_thresh.get('riskMedium')}")

for d in devs:
    print(f"  Device: {d.get('deviceIdentifier')} trust={d.get('currentTrust')} active={d.get('active')} status={d.get('registrationStatus')}")
for p in pols:
    print(f"  Policy: {p.get('name')} res={p.get('targetResource')} op={p.get('targetOperation')}")
for b in books:
    print(f"  Booking: {b.get('bookingReference')} from={b.get('validFrom')} until={b.get('validUntil')}")

# ──────────────────────────────────────────────────────────────
# RISK SCORE ANALYSIS
# Risk thresholds from application.properties:
#   riskLow=30, riskMedium=70
# Risk weights: time=10, location=15, sensitivity=20, frequency=20, network=15, violations=10, behavior=10
# Max weighted risk for DENY (>70):
#   Need violation max (recentViolationCount >= 5 -> +10)
#   + frequency max (requestCountWindow >= 20 -> +20)
#   + behavior SUSPICIOUS (+10)
#   + sensitivity HIGH (door-sensor = HIGH -> +20)
#   + network EXTERNAL (+15)
#   = 10+20+10+20+15 = 75 -> DENY
# ──────────────────────────────────────────────────────────────

def run_scenario(label, body, expected):
    banner(f"SCENARIO {label}")
    status, resp = post("/api/authorization/evaluate", body)
    decision = resp.get("finalDecision") or resp.get("decision") or "?"
    tx       = resp.get("blockchainTransactionHash") or resp.get("transactionHash")
    block    = resp.get("blockchainBlockNumber") or resp.get("blockNumber")
    contract = resp.get("contractAddress")
    trust    = resp.get("trustScore")
    risk     = resp.get("riskScore")
    abac_r   = resp.get("abacResult")
    abac_rsn = resp.get("abacReason")
    reason   = resp.get("decisionReason")
    ts       = resp.get("evaluationTimestamp")

    ok = str(decision).upper() == expected.upper()
    mark = "PASS" if ok else "FAIL"
    print(f"  HTTP Status       : {status}")
    print(f"  ABAC Result       : {abac_r} -- {abac_rsn}")
    print(f"  Trust Score       : {trust}")
    print(f"  Risk Score        : {risk}")
    print(f"  Decision          : {decision}  (expected={expected})  [{mark}]")
    print(f"  Reason            : {reason}")
    print(f"  Transaction Hash  : {tx}")
    print(f"  Block Number      : {block}")
    print(f"  Contract Address  : {contract}")
    print(f"  Timestamp         : {ts}")
    return {
        "label": label, "http": status, "decision": str(decision),
        "expected": expected, "match": ok,
        "tx": tx, "block": block, "contract": contract,
        "trust": trust, "risk": risk,
        "abac_result": abac_r, "abac_reason": abac_rsn,
        "reason": reason, "ts": ts
    }

BASE_BODY = {
    "deviceIdentifier": "DOOR-SENSOR-001",
    "userId": "guest-user-001",
    "role": "GUEST",
    "organization": "SmartRental",
    "resource": "door-sensor",
    "operation": "CONTROL",
    "location": "Property-001",
    "bookingId": "BOOKING-PHASE6B-001",
    "networkContext": "INTERNAL",
    "behavioralIndicator": "NORMAL"
}

results = []

# ── SCENARIO A: Trust=HIGH(80), Risk=LOW(<30) -> ALLOW ────────
# Low risk: few requests, no violations, normal, internal network
results.append(run_scenario(
    "A: Trust=HIGH(80) Risk=LOW -> ALLOW",
    {**BASE_BODY,
     "requestReference": "PHASE6B-REQ-A-FINAL",
     "requestCountWindow": 2,
     "recentViolationCount": 0,
     "behavioralIndicator": "NORMAL"},
    "ALLOW"
))
time.sleep(2)

# ── SCENARIO B: Trust=HIGH(80), Risk=MEDIUM(30-70) -> RESTRICT ─
# Medium risk: moderate requests, some violations, normal
results.append(run_scenario(
    "B: Trust=HIGH(80) Risk=MEDIUM -> RESTRICT",
    {**BASE_BODY,
     "requestReference": "PHASE6B-REQ-B-FINAL",
     "requestCountWindow": 12,
     "recentViolationCount": 2,
     "behavioralIndicator": "NORMAL"},
    "RESTRICT"
))
time.sleep(2)

# ── SCENARIO C: Trust=HIGH(80), Risk=HIGH(>70) -> DENY ────────
# Maximum risk: external network, flooding, many violations, suspicious
results.append(run_scenario(
    "C: Trust=HIGH(80) Risk=HIGH -> DENY",
    {**BASE_BODY,
     "requestReference": "PHASE6B-REQ-C-FINAL",
     "requestCountWindow": 25,    # max frequency (+20)
     "recentViolationCount": 6,   # max violations (+10)
     "behavioralIndicator": "SUSPICIOUS",  # suspicious (+10)
     "networkContext": "EXTERNAL"},         # external network (+15)
    "DENY"
))
time.sleep(2)

# ── SCENARIO D: Trust=LOW(<30), Risk=LOW -> DENY ─────────────
# Lower device trust via confirmed malicious events (correct endpoint: /events plural)
banner("PRE-D: Lowering device trust via /api/trust/.../events")
_, t0 = get("/api/trust/DOOR-SENSOR-001")
print(f"  Trust before: {t0.get('currentTrust')}")

for i in range(3):
    s, r = post("/api/trust/DOOR-SENSOR-001/events", {
        "eventType": "CONFIRMED_MALICIOUS",
        "reason": "Phase 6B test: deliberate trust reduction",
        "source": "phase6b_test"
    })
    t_after = r.get("updatedTrustScore") or r.get("newTrustScore") or r.get("currentTrust")
    print(f"  Malicious event {i+1}: HTTP {s} -> trust={t_after} (full: {r})")
    time.sleep(0.5)

_, t_check = get("/api/trust/DOOR-SENSOR-001")
trust_now = t_check.get("currentTrust")
print(f"  Trust after 3x CONFIRMED_MALICIOUS: {trust_now}")

if trust_now is not None and trust_now < 30:
    results.append(run_scenario(
        "D: Trust=LOW(<30) Risk=LOW -> DENY (trust too low)",
        {**BASE_BODY,
         "requestReference": "PHASE6B-REQ-D-FINAL",
         "requestCountWindow": 2,
         "recentViolationCount": 0,
         "behavioralIndicator": "NORMAL"},
        "DENY"
    ))
else:
    print(f"  [NOTE] Trust={trust_now}, not below 30 yet. Running anyway to capture actual behavior.")
    results.append(run_scenario(
        "D: Trust=REDUCED({trust_now}) Risk=LOW -> DENY expected",
        {**BASE_BODY,
         "requestReference": "PHASE6B-REQ-D-FINAL",
         "requestCountWindow": 2,
         "recentViolationCount": 0,
         "behavioralIndicator": "NORMAL"},
        "DENY"
    ))
time.sleep(2)

# ── SCENARIO E: ABAC FAIL (unregistered device) -> DENY ───────
results.append(run_scenario(
    "E: ABAC-FAIL (unregistered device) -> DENY",
    {**BASE_BODY,
     "deviceIdentifier": "UNREGISTERED-DEVICE-999",
     "requestReference": "PHASE6B-REQ-E-FINAL",
     "requestCountWindow": 2,
     "recentViolationCount": 0},
    "DENY"
))
time.sleep(2)

# ── SCENARIO F: No booking -> DENY ────────────────────────────
results.append(run_scenario(
    "F: Booking-INACTIVE (no bookingId) -> DENY",
    {**BASE_BODY,
     "bookingId": None,
     "requestReference": "PHASE6B-REQ-F-FINAL",
     "requestCountWindow": 2,
     "recentViolationCount": 0},
    "DENY"
))

# ──────────────────────────────────────────────────────────────
# SUMMARY
# ──────────────────────────────────────────────────────────────
banner("PHASE 6B FINAL SCENARIO SUMMARY")
for r in results:
    mark = "PASS" if r["match"] else "FAIL"
    print(f"  [{mark}] {r['label'][:50]:50s} decision={r['decision']:10s} tx={str(r['tx'] or 'N/A')[:20]} block={r['block'] or 'N/A'}")

passed = sum(1 for r in results if r["match"])
total  = len(results)
print(f"\n  FINAL: {passed}/{total} scenarios passed")

with open("contracts/phase6b_scenarios.json", "w") as f:
    json.dump(results, f, indent=2, default=str)
print("  Saved: contracts/phase6b_scenarios.json")
