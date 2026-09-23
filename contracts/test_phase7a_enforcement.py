"""
Phase 7A Live Resource Operation Enforcement Test Suite
Verifies:
1. Live REST API: POST /api/resource-operations/execute
2. ALLOW + CONTROL on Smart Door Lock -> EXECUTED (Door unlocks)
3. RESTRICT + CONTROL on Smart Door Lock -> DOWNGRADED (Door remains locked!)
4. RESTRICT + READ on Smart Door Lock -> EXECUTED (Status read permitted)
5. DENY + CONTROL on Smart Door Lock -> BLOCKED (No state mutation)
6. DENY + READ on Smart Door Lock -> BLOCKED (No state mutation)
7. ABAC Gate FAIL (Unregistered Device) -> BLOCKED (Fast fail, 0 gas)
8. ABAC Gate FAIL (Missing Booking) -> BLOCKED (Fast fail, 0 gas)
9. RESTRICT + CONTROL on Thermostat -> DOWNGRADED (Eco temperature clamped)
10. ALLOW + CONTROL on Smart Light -> EXECUTED (Power turned ON)
"""

import json
import time
import subprocess
import urllib.request
import urllib.error
from datetime import datetime, timezone, timedelta

BASE = "http://localhost:8090"

def banner(title):
    print("\n" + "=" * 70)
    print(f"  {title}")
    print("=" * 70)

def set_device_trust(device_id="DOOR-SENSOR-001", trust_val=80.0):
    cmd = f'docker exec releasemind-mysql mysql -uroot -proot_secret_change_me trustabac_iot -e "UPDATE devices SET current_trust = {trust_val} WHERE device_identifier = \'{device_id}\';"'
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

def create_policy_if_missing(name, desc, res, op, existing_names):
    if name in existing_names:
        return
    s, p = post("/api/policies", {
        "name": name,
        "description": desc,
        "targetResource": res,
        "targetOperation": op,
        "active": True,
        "conditions": [
            {"attributeCategory": "SUBJECT", "attributeKey": "subject.role", "operator": "EQUALS", "expectedValue": "GUEST"},
            {"attributeCategory": "SUBJECT", "attributeKey": "subject.organization", "operator": "EQUALS", "expectedValue": "SmartRental"},
            {"attributeCategory": "DEVICE", "attributeKey": "device.registrationStatus", "operator": "EQUALS", "expectedValue": "REGISTERED"},
            {"attributeCategory": "DEVICE", "attributeKey": "device.active", "operator": "EQUALS", "expectedValue": "true"},
            {"attributeCategory": "CONTEXT", "attributeKey": "booking.valid", "operator": "EQUALS", "expectedValue": "true"}
        ]
    })
    print(f"  Policy created: {name} (HTTP {s})")

def main():
    banner("PHASE 7A LIVE RESOURCE OPERATION ENFORCEMENT SUITE")

    # 1. Device Setup
    set_device_trust("DOOR-SENSOR-001", 80.0)

    s, devs = get("/api/devices")
    dev_ids = {d.get("deviceIdentifier") for d in devs}

    if "LIGHT-001" not in dev_ids:
        post("/api/devices", {
            "deviceIdentifier": "LIGHT-001",
            "name": "Living Room Smart Light",
            "deviceType": "SMART_LIGHT",
            "deviceClass": "ACTUATOR",
            "location": "Property-001",
            "initialTrust": 80.0
        })
    if "THERMOSTAT-001" not in dev_ids:
        post("/api/devices", {
            "deviceIdentifier": "THERMOSTAT-001",
            "name": "Smart Thermostat",
            "deviceType": "SMART_THERMOSTAT",
            "deviceClass": "ACTUATOR",
            "location": "Property-001",
            "initialTrust": 80.0
        })

    # 2. Policy Setup
    s, pols = get("/api/policies")
    pol_names = {p.get("name") for p in pols}

    create_policy_if_missing("Phase7A-DoorLock-CONTROL", "Allows GUEST to CONTROL SMART_DOOR_LOCK", "SMART_DOOR_LOCK", "CONTROL", pol_names)
    create_policy_if_missing("Phase7A-DoorLock-READ", "Allows GUEST to READ SMART_DOOR_LOCK", "SMART_DOOR_LOCK", "READ", pol_names)
    create_policy_if_missing("Phase7A-Light-CONTROL", "Allows GUEST to CONTROL SMART_LIGHT", "SMART_LIGHT", "CONTROL", pol_names)
    create_policy_if_missing("Phase7A-Light-READ", "Allows GUEST to READ SMART_LIGHT", "SMART_LIGHT", "READ", pol_names)
    create_policy_if_missing("Phase7A-Thermostat-CONTROL", "Allows GUEST to CONTROL SMART_THERMOSTAT", "SMART_THERMOSTAT", "CONTROL", pol_names)
    create_policy_if_missing("Phase7A-Thermostat-READ", "Allows GUEST to READ SMART_THERMOSTAT", "SMART_THERMOSTAT", "READ", pol_names)

    BASE_BODY = {
        "deviceIdentifier": "DOOR-SENSOR-001",
        "userId": "guest-user-001",
        "role": "GUEST",
        "organization": "SmartRental",
        "resource": "SMART_DOOR_LOCK",
        "operation": "CONTROL",
        "location": "Property-001",
        "bookingId": "BOOKING-PHASE6B-001",
        "networkContext": "INTERNAL",
        "behavioralIndicator": "NORMAL",
        "commandPayload": {"action": "UNLOCK"}
    }

    test_results = []

    def run_test(test_id, name, payload, expected_status, expected_decision, check_fn=None):
        banner(f"{test_id}: {name}")
        s, resp = post("/api/resource-operations/execute", payload)
        status = resp.get("enforcementStatus")
        decision = resp.get("authorizationDecision")
        eff_op = resp.get("effectiveOperation")
        msg = resp.get("executionMessage")
        dev_state = resp.get("deviceState") or {}
        tx = resp.get("blockchainTxHash")
        block = resp.get("blockchainBlockNumber")

        status_match = status == expected_status
        dec_match = decision == expected_decision
        custom_ok = check_fn(resp) if check_fn else True
        passed = status_match and dec_match and custom_ok

        print(f"  HTTP Status        : {s}")
        print(f"  Enforcement Status : {status} (expected: {expected_status}) -> {'MATCH' if status_match else 'MISMATCH'}")
        print(f"  Auth Decision      : {decision} (expected: {expected_decision}) -> {'MATCH' if dec_match else 'MISMATCH'}")
        print(f"  Effective Operation: {eff_op}")
        print(f"  Execution Message  : {msg}")
        print(f"  Device State       : {dev_state}")
        print(f"  Blockchain TxHash  : {tx}")
        print(f"  Blockchain Block   : {block}")
        print(f"  Test Verdict       : {'[PASS]' if passed else '[FAIL]'}")

        test_results.append({
            "id": test_id,
            "name": name,
            "expected_status": expected_status,
            "actual_status": status,
            "expected_decision": expected_decision,
            "actual_decision": decision,
            "passed": passed,
            "tx_hash": tx,
            "block": block
        })
        time.sleep(1.5)
        return passed

    # TEST 1: ALLOW + CONTROL on Smart Door Lock -> EXECUTED (Door unlocks)
    set_device_trust("DOOR-SENSOR-001", 80.0)
    run_test(
        "TEST 1",
        "ALLOW + CONTROL on Smart Door Lock -> EXECUTED (Door unlocks)",
        {**BASE_BODY,
         "requestReference": "P7A-TEST-1",
         "requestCountWindow": 1,
         "recentViolationCount": 0},
        "EXECUTED",
        "ALLOW",
        check_fn=lambda r: r.get("deviceState", {}).get("lockState") == "UNLOCKED"
    )

    # TEST 2: RESTRICT + CONTROL on Smart Door Lock -> DOWNGRADED (Door remains locked!)
    set_device_trust("DOOR-SENSOR-001", 80.0)
    run_test(
        "TEST 2",
        "RESTRICT + CONTROL on Smart Door Lock -> DOWNGRADED (Door remains LOCKED)",
        {**BASE_BODY,
         "requestReference": "P7A-TEST-2",
         "requestCountWindow": 12,
         "recentViolationCount": 2},
        "DOWNGRADED",
        "RESTRICT",
        check_fn=lambda r: r.get("effectiveOperation") == "READ"
    )

    # TEST 3: RESTRICT + READ on Smart Door Lock -> EXECUTED (Status read permitted)
    run_test(
        "TEST 3",
        "RESTRICT + READ on Smart Door Lock -> EXECUTED (Status read permitted)",
        {**BASE_BODY,
         "operation": "READ",
         "requestReference": "P7A-TEST-3",
         "requestCountWindow": 12,
         "recentViolationCount": 2,
         "commandPayload": None},
        "EXECUTED",
        "RESTRICT",
        check_fn=lambda r: r.get("effectiveOperation") == "READ"
    )

    # TEST 4: DENY + CONTROL on Smart Door Lock (Low Trust) -> BLOCKED
    set_device_trust("DOOR-SENSOR-001", 15.0)
    run_test(
        "TEST 4",
        "DENY + CONTROL on Smart Door Lock (Low Trust 15.0) -> BLOCKED",
        {**BASE_BODY,
         "requestReference": "P7A-TEST-4",
         "requestCountWindow": 1,
         "recentViolationCount": 0},
        "BLOCKED",
        "DENY",
        check_fn=lambda r: r.get("effectiveOperation") == "NONE" and r.get("blockchainTxHash") is not None
    )

    # TEST 5: DENY + READ on Smart Door Lock (Low Trust) -> BLOCKED
    run_test(
        "TEST 5",
        "DENY + READ on Smart Door Lock (Low Trust 15.0) -> BLOCKED",
        {**BASE_BODY,
         "operation": "READ",
         "requestReference": "P7A-TEST-5",
         "requestCountWindow": 1,
         "recentViolationCount": 0,
         "commandPayload": None},
        "BLOCKED",
        "DENY",
        check_fn=lambda r: r.get("effectiveOperation") == "NONE" and r.get("blockchainTxHash") is not None
    )

    # TEST 6: ABAC FAIL (Unregistered Device) -> BLOCKED (Fast fail, 0 gas)
    run_test(
        "TEST 6",
        "ABAC Gate FAIL (Unregistered Device) -> BLOCKED",
        {**BASE_BODY,
         "deviceIdentifier": "UNREGISTERED-999",
         "requestReference": "P7A-TEST-6"},
        "BLOCKED",
        "DENY",
        check_fn=lambda r: r.get("abacResult") == "FAIL" and r.get("blockchainTxHash") is None
    )

    # TEST 7: ABAC Gate FAIL (Missing Booking) -> BLOCKED
    run_test(
        "TEST 7",
        "ABAC Gate FAIL (Missing Booking) -> BLOCKED",
        {**BASE_BODY,
         "bookingId": None,
         "requestReference": "P7A-TEST-7"},
        "BLOCKED",
        "DENY",
        check_fn=lambda r: r.get("abacResult") == "FAIL" and r.get("blockchainTxHash") is None
    )

    # TEST 8: RESTRICT + CONTROL on Thermostat -> DOWNGRADED (Eco-clamped temperature)
    # Give moderate violations and frequency to land risk in [30, 70] (MEDIUM risk)
    set_device_trust("THERMOSTAT-001", 80.0)
    run_test(
        "TEST 8",
        "RESTRICT + CONTROL on Smart Thermostat -> DOWNGRADED (Eco-clamp temp to 24.0C)",
        {**BASE_BODY,
         "deviceIdentifier": "THERMOSTAT-001",
         "resource": "SMART_THERMOSTAT",
         "operation": "CONTROL",
         "requestReference": "P7A-TEST-8",
         "requestCountWindow": 16,
         "recentViolationCount": 4,
         "commandPayload": {"targetTempCelsius": 29.0}},
        "DOWNGRADED",
        "RESTRICT",
        check_fn=lambda r: r.get("deviceState", {}).get("targetTempCelsius") == 24.0 and r.get("deviceState", {}).get("ecoMode") is True
    )

    # TEST 9: ALLOW + CONTROL on Smart Light -> EXECUTED (Power turned ON)
    set_device_trust("LIGHT-001", 80.0)
    run_test(
        "TEST 9",
        "ALLOW + CONTROL on Smart Light -> EXECUTED (Power turned ON)",
        {**BASE_BODY,
         "deviceIdentifier": "LIGHT-001",
         "resource": "SMART_LIGHT",
         "operation": "CONTROL",
         "requestReference": "P7A-TEST-9",
         "requestCountWindow": 1,
         "recentViolationCount": 0,
         "commandPayload": {"power": "ON"}},
        "EXECUTED",
        "ALLOW",
        check_fn=lambda r: r.get("deviceState", {}).get("power") == "ON"
    )

    # Summary
    banner("PHASE 7A LIVE ENFORCEMENT TEST SUMMARY")
    passed_count = sum(1 for t in test_results if t["passed"])
    total_count = len(test_results)
    for t in test_results:
        print(f"  [{'PASS' if t['passed'] else 'FAIL'}] {t['id']}: {t['name']}")
    print(f"\n  OVERALL: {passed_count}/{total_count} live enforcement tests passed")

    with open("contracts/phase7a_enforcement_report.json", "w") as f:
        json.dump(test_results, f, indent=2)

if __name__ == "__main__":
    main()
