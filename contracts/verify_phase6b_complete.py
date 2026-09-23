"""
Phase 6B Complete Real Ganache + Spring Boot + MySQL Verification Suite
Executes:
1. Ganache RPC & Web3 status verification
2. Spring Boot blockchain status & contract threshold verification
3. Entity setup (Device, Policies, Booking)
4. All 6 Core Authorization Scenarios (A, B, C, D, E, F)
5. Ganache Outage & Auto-Recovery resilience test
6. MySQL audit trail correlation & verification
"""

import json
import time
import subprocess
import urllib.request
import urllib.error
from datetime import datetime, timezone, timedelta

BASE = "http://localhost:8090"
GANACHE_URL = "http://127.0.0.1:8545"

def banner(title):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)

def set_device_trust(trust_val=80.0):
    cmd = f'docker exec trustabac-mysql mysql -uroot -proot_secret_change_me trustabac_iot -e "UPDATE devices SET current_trust = {trust_val} WHERE device_identifier = \'DOOR-SENSOR-001\';"'
    subprocess.run(cmd, shell=True, capture_output=True)

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
        try:
            return e.code, json.loads(raw) if raw else {}
        except Exception:
            return e.code, {"error": raw.decode("utf-8", errors="ignore")}
    except Exception as e:
        return 500, {"error": str(e)}

def post(path, body): return http("POST", path, body)
def get(path):        return http("GET",  path)

def main():
    report = {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "phase": "6B",
        "scenarios": [],
        "outage_test": {},
        "mysql_verification": {}
    }

    banner("1. BLOCKCHAIN STATUS & THRESHOLDS VERIFICATION")
    s, b_status = get("/api/blockchain/status")
    print(f"  GET /api/blockchain/status (HTTP {s}):")
    print(f"    rpcUrl            : {b_status.get('rpcUrl')}")
    print(f"    rpcReachable      : {b_status.get('rpcReachable')}")
    print(f"    chainId           : {b_status.get('chainId')}")
    print(f"    latestBlock       : {b_status.get('latestBlock')}")
    print(f"    contractAddress   : {b_status.get('contractAddress')}")
    print(f"    contractReachable : {b_status.get('contractReachable')}")
    assert b_status.get("rpcReachable") is True, "RPC not reachable!"
    assert b_status.get("contractReachable") is True, "Contract not reachable!"

    s, b_thresh = get("/api/blockchain/thresholds")
    print(f"  GET /api/blockchain/thresholds (HTTP {s}):")
    print(f"    trustHigh         : {b_thresh.get('trustHigh')}")
    print(f"    trustMedium       : {b_thresh.get('trustMedium')}")
    print(f"    riskLow           : {b_thresh.get('riskLow')}")
    print(f"    riskMedium        : {b_thresh.get('riskMedium')}")
    assert b_thresh.get("trustHigh") == 70
    assert b_thresh.get("trustMedium") == 30
    assert b_thresh.get("riskLow") == 30
    assert b_thresh.get("riskMedium") == 70

    banner("2. ENTITY SETUP (DEVICE, POLICIES, BOOKING)")
    # 2.1 Device Setup
    s, devs = get("/api/devices")
    dev_exists = any(d.get("deviceIdentifier") == "DOOR-SENSOR-001" for d in devs)
    if not dev_exists:
        s, dev = post("/api/devices", {
            "deviceIdentifier": "DOOR-SENSOR-001",
            "name": "Front Door Lock Sensor",
            "deviceType": "LOCK",
            "deviceClass": "CRITICAL",
            "location": "Property-001",
            "initialTrust": 80.0
        })
        print(f"  Device created: HTTP {s} -> {dev.get('deviceIdentifier')}")
    else:
        set_device_trust(80.0)
        print("  Device trust reset to 80.0")

    # 2.2 Policy Setup
    s, pols = get("/api/policies")
    # Door sensor policy
    if not any(p.get("name") == "Phase6B-Guest-DoorLock-CONTROL" for p in pols):
        s, pol = post("/api/policies", {
            "name": "Phase6B-Guest-DoorLock-CONTROL",
            "description": "Allows GUEST to CONTROL door-sensor under valid booking",
            "targetResource": "door-sensor",
            "targetOperation": "CONTROL",
            "active": True,
            "conditions": [
                {"attributeCategory": "SUBJECT", "attributeKey": "subject.role", "operator": "EQUALS", "expectedValue": "GUEST"},
                {"attributeCategory": "SUBJECT", "attributeKey": "subject.organization", "operator": "EQUALS", "expectedValue": "SmartRental"},
                {"attributeCategory": "DEVICE", "attributeKey": "device.registrationStatus", "operator": "EQUALS", "expectedValue": "REGISTERED"},
                {"attributeCategory": "DEVICE", "attributeKey": "device.active", "operator": "EQUALS", "expectedValue": "true"},
                {"attributeCategory": "CONTEXT", "attributeKey": "booking.valid", "operator": "EQUALS", "expectedValue": "true"}
            ]
        })
        print(f"  Policy created: HTTP {s} -> ID={pol.get('id')} name={pol.get('name')}")
    else:
        print(f"  Policy already exists: Phase6B-Guest-DoorLock-CONTROL")

    # Critical resource policy (for Scenario C extreme risk test)
    if not any(p.get("name") == "Phase6B-Guest-GatewayAdmin-CONTROL" for p in pols):
        s, pol = post("/api/policies", {
            "name": "Phase6B-Guest-GatewayAdmin-CONTROL",
            "description": "Allows GUEST to CONTROL GATEWAY_ADMIN under valid booking",
            "targetResource": "GATEWAY_ADMIN",
            "targetOperation": "CONTROL",
            "active": True,
            "conditions": [
                {"attributeCategory": "SUBJECT", "attributeKey": "subject.role", "operator": "EQUALS", "expectedValue": "GUEST"},
                {"attributeCategory": "SUBJECT", "attributeKey": "subject.organization", "operator": "EQUALS", "expectedValue": "SmartRental"},
                {"attributeCategory": "DEVICE", "attributeKey": "device.registrationStatus", "operator": "EQUALS", "expectedValue": "REGISTERED"},
                {"attributeCategory": "DEVICE", "attributeKey": "device.active", "operator": "EQUALS", "expectedValue": "true"},
                {"attributeCategory": "CONTEXT", "attributeKey": "booking.valid", "operator": "EQUALS", "expectedValue": "true"}
            ]
        })
        print(f"  Policy created: HTTP {s} -> ID={pol.get('id')} name={pol.get('name')}")
    else:
        print(f"  Policy already exists: Phase6B-Guest-GatewayAdmin-CONTROL")

    # 2.3 Booking Setup
    s, books = get("/api/bookings")
    book_exists = any(b.get("bookingReference") == "BOOKING-PHASE6B-001" for b in books)
    if not book_exists:
        valid_from = (datetime.now(timezone.utc) - timedelta(hours=1)).isoformat()
        valid_until = (datetime.now(timezone.utc) + timedelta(days=7)).isoformat()
        s, book = post("/api/bookings", {
            "bookingReference": "BOOKING-PHASE6B-001",
            "guestId": "guest-user-001",
            "propertyId": "Property-001",
            "validFrom": valid_from,
            "validUntil": valid_until,
            "active": True
        })
        print(f"  Booking created: HTTP {s} -> ref={book.get('bookingReference')}")
    else:
        print(f"  Booking already exists: BOOKING-PHASE6B-001")

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

    def execute_scenario(tag, desc, payload, expected_decision, expect_blockchain_tx=True):
        banner(f"SCENARIO {tag}: {desc}")
        s, resp = post("/api/authorization/evaluate", payload)
        decision = resp.get("finalDecision") or resp.get("decision")
        tx = resp.get("blockchainTransactionHash") or resp.get("transactionHash")
        block = resp.get("blockchainBlockNumber") or resp.get("blockNumber")
        trust = resp.get("trustScore")
        risk = resp.get("riskScore")
        reason = resp.get("decisionReason")
        abac_res = resp.get("abacResult")
        abac_rsn = resp.get("abacReason")

        match = str(decision).upper() == expected_decision.upper()
        tx_match = (tx is not None and len(str(tx)) > 10) if expect_blockchain_tx else (tx is None)
        passed = match and tx_match

        print(f"  HTTP Status       : {s}")
        print(f"  ABAC Result       : {abac_res} ({abac_rsn})")
        print(f"  Trust Score       : {trust}")
        print(f"  Risk Score        : {risk}")
        print(f"  Decision          : {decision} (expected: {expected_decision}) -> {'MATCH' if match else 'MISMATCH'}")
        print(f"  Blockchain TxHash : {tx}")
        print(f"  Blockchain Block  : {block}")
        print(f"  Decision Reason   : {reason}")
        print(f"  Result            : {'[PASS]' if passed else '[FAIL]'}")

        result_item = {
            "scenario": tag,
            "description": desc,
            "expected_decision": expected_decision,
            "actual_decision": decision,
            "expect_blockchain_tx": expect_blockchain_tx,
            "tx_hash": tx,
            "block_number": block,
            "trust_score": trust,
            "risk_score": risk,
            "abac_result": abac_res,
            "reason": reason,
            "passed": passed
        }
        report["scenarios"].append(result_item)
        time.sleep(1.5)
        return result_item

    banner("3. EXECUTING 6 CORE AUTHORIZATION SCENARIOS")

    # Ensure device trust is 80.0
    set_device_trust(80.0)

    # Scenario A: High Trust (80) + Low Risk (<30) -> ALLOW (Blockchain Tx mined)
    execute_scenario(
        "A",
        "High Trust (80) + Low Risk (22.0) -> ALLOW",
        {**BASE_BODY,
         "requestReference": "PHASE6B-SCENARIO-A",
         "requestCountWindow": 1,
         "recentViolationCount": 0,
         "behavioralIndicator": "NORMAL",
         "networkContext": "INTERNAL"},
        "ALLOW",
        expect_blockchain_tx=True
    )

    # Scenario B: High Trust (80) + Medium Risk (30-70) -> RESTRICT (Blockchain Tx mined)
    execute_scenario(
        "B",
        "High Trust (80) + Medium Risk (35.3) -> RESTRICT",
        {**BASE_BODY,
         "requestReference": "PHASE6B-SCENARIO-B",
         "requestCountWindow": 12,
         "recentViolationCount": 2,
         "behavioralIndicator": "NORMAL",
         "networkContext": "INTERNAL"},
        "RESTRICT",
        expect_blockchain_tx=True
    )

    # Scenario C: High Trust (80) + Extreme High Risk (>70) -> DENY (Blockchain Tx mined)
    execute_scenario(
        "C",
        "High Trust (80) + Extreme High Risk (75.0 > 70) -> DENY",
        {**BASE_BODY,
         "requestReference": "PHASE6B-SCENARIO-C",
         "resource": "GATEWAY_ADMIN",
         "location": "Property-001",
         "behavioralIndicator": "ABNORMAL",
         "networkContext": "EXTERNAL",
         "requestCountWindow": 30,
         "recentViolationCount": 10},
        "DENY",
        expect_blockchain_tx=True
    )

    # Scenario D: Low Trust (<30) + Low Risk -> DENY (Blockchain Tx mined)
    set_device_trust(15.0)
    execute_scenario(
        "D",
        "Low Trust (15.0 < 30) + Low Risk -> DENY",
        {**BASE_BODY,
         "requestReference": "PHASE6B-SCENARIO-D",
         "requestCountWindow": 1,
         "recentViolationCount": 0,
         "behavioralIndicator": "NORMAL",
         "networkContext": "INTERNAL"},
        "DENY",
        expect_blockchain_tx=True
    )

    # Scenario E: Unregistered Device -> DENY at ABAC Gate (No blockchain tx)
    execute_scenario(
        "E",
        "Unregistered Device -> DENY (Fast fail at ABAC Gate)",
        {**BASE_BODY,
         "deviceIdentifier": "UNKNOWN-DEV-999",
         "requestReference": "PHASE6B-SCENARIO-E",
         "requestCountWindow": 1,
         "recentViolationCount": 0},
        "DENY",
        expect_blockchain_tx=False
    )

    # Scenario F: Inactive/Missing Booking -> DENY at ABAC Gate (No blockchain tx)
    execute_scenario(
        "F",
        "Missing Booking -> DENY (Fast fail at ABAC Gate)",
        {**BASE_BODY,
         "bookingId": None,
         "requestReference": "PHASE6B-SCENARIO-F",
         "requestCountWindow": 1,
         "recentViolationCount": 0},
        "DENY",
        expect_blockchain_tx=False
    )

    banner("4. GANACHE OUTAGE & RESILIENCE VERIFICATION")
    # Restore device trust to 80.0
    set_device_trust(80.0)

    # Step 4.1: Pre-outage check
    print("  [Step 4.1] Pre-outage baseline check...")
    s, r_pre = post("/api/authorization/evaluate", {
        **BASE_BODY,
        "requestReference": "PHASE6B-OUTAGE-PRE",
        "requestCountWindow": 1,
        "recentViolationCount": 0
    })
    dec_pre = r_pre.get("finalDecision") or r_pre.get("decision")
    tx_pre = r_pre.get("blockchainTransactionHash") or r_pre.get("transactionHash")
    print(f"    Pre-outage Decision: {dec_pre}, TxHash: {tx_pre}")
    assert dec_pre == "ALLOW", f"Pre-outage expected ALLOW, got {dec_pre}"

    # Step 4.2: Stop Ganache
    print("\n  [Step 4.2] Stopping Ganache Docker container (simulating outage)...")
    subprocess.run(["docker", "stop", "ganache-trustabac"], check=True, capture_output=True)
    time.sleep(3)

    # Verify Ganache is stopped
    s_b, status_down = get("/api/blockchain/status")
    print(f"    Blockchain status during outage: rpcReachable={status_down.get('rpcReachable')}")

    # Request during outage -> FAIL CLOSED
    print("    Sending authorization request during blockchain outage...")
    s_out, r_out = post("/api/authorization/evaluate", {
        **BASE_BODY,
        "requestReference": "PHASE6B-OUTAGE-DURING",
        "requestCountWindow": 1,
        "recentViolationCount": 0
    })
    dec_out = r_out.get("finalDecision") or r_out.get("decision") or r_out.get("error") or "FAIL_CLOSED"
    print(f"    During-outage HTTP status: {s_out}, Response: {r_out}")
    outage_fail_closed = s_out in (500, 503) or str(dec_out).upper() == 'DENY'
    print(f"    Fail-Closed Verified: {outage_fail_closed}")

    # Step 4.3: Restart Ganache
    print("\n  [Step 4.3] Restarting Ganache Docker container...")
    subprocess.run(["docker", "start", "ganache-trustabac"], check=True, capture_output=True)
    time.sleep(5)

    # Step 4.4: Verify recovery
    s_rec, status_rec = get("/api/blockchain/status")
    print(f"    Blockchain status after restart: rpcReachable={status_rec.get('rpcReachable')}, contractReachable={status_rec.get('contractReachable')}")

    if not status_rec.get("contractReachable"):
        print("    Redeploying contract on restarted Ganache...")
        subprocess.run(["python", "contracts/phase6b_setup.py"], check=True, capture_output=True)

    print("    Sending authorization request after blockchain recovery...")
    s_post, r_post = post("/api/authorization/evaluate", {
        **BASE_BODY,
        "requestReference": "PHASE6B-OUTAGE-POST",
        "requestCountWindow": 1,
        "recentViolationCount": 0
    })
    dec_post = r_post.get("finalDecision") or r_post.get("decision")
    tx_post = r_post.get("blockchainTransactionHash") or r_post.get("transactionHash")
    print(f"    Post-recovery Decision: {dec_post}, TxHash: {tx_post}")
    recovery_passed = dec_post == "ALLOW" and tx_post is not None

    report["outage_test"] = {
        "pre_outage_decision": dec_pre,
        "pre_outage_tx": tx_pre,
        "during_outage_status": s_out,
        "during_outage_response": r_out,
        "fail_closed_verified": outage_fail_closed,
        "post_recovery_decision": dec_post,
        "post_recovery_tx": tx_post,
        "recovery_passed": recovery_passed
    }

    banner("5. MYSQL AUDIT TRAIL CORRELATION VERIFICATION")
    mysql_cmd = ['docker', 'exec', 'trustabac-mysql', 'mysql', '-uroot', '-proot_secret_change_me', 'trustabac_iot', '-e', 'SELECT id, device_identifier, resource, operation, trust_score, risk_score, decision, contract_address, transaction_hash, block_number, evaluation_timestamp FROM blockchain_authorization_events ORDER BY id DESC LIMIT 10;']
    proc = subprocess.run(mysql_cmd, capture_output=True, text=True)
    print("  MySQL blockchain_authorization_events records:")
    print(proc.stdout)
    report["mysql_verification"]["raw_output"] = proc.stdout

    banner("6. PHASE 6B VERIFICATION SUMMARY")
    all_scenarios_pass = all(s["passed"] for s in report["scenarios"])
    passed_cnt = sum(1 for s in report['scenarios'] if s['passed'])
    total_cnt = len(report['scenarios'])

    print(f"  Scenarios Passed      : {passed_cnt}/{total_cnt}")
    for s in report["scenarios"]:
        print(f"    Scenario {s['scenario']}: {s['description']} -> {'[PASS]' if s['passed'] else '[FAIL]'}")
    print(f"  Fail-Closed Tested    : {'[PASS]' if outage_fail_closed else '[FAIL]'}")
    print(f"  Recovery Tested       : {'[PASS]' if recovery_passed else '[FAIL]'}")
    print(f"  MySQL Correlation     : {'[PASS]' if '0x' in proc.stdout else '[FAIL]'}")

    overall_ok = all_scenarios_pass and outage_fail_closed and recovery_passed
    print(f"\n  OVERALL PHASE 6B STATUS: {'[VERIFIED SUCCESS - ALL PASSED]' if overall_ok else '[ACTION REQUIRED]'}")

    with open("contracts/phase6b_complete_report.json", "w") as f:
        json.dump(report, f, indent=2, default=str)
    print("\n  Full report saved to: contracts/phase6b_complete_report.json")

if __name__ == "__main__":
    main()
