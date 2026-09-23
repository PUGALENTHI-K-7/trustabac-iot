"""
Phase 7B Live IoT Traffic & Scenario Simulator Test Suite
Verifies:
1. GET /api/simulator/status & GET /api/simulator/scenarios
2. POST /api/simulator/start & POST /api/simulator/reset
3. NORMAL_STAY scenario (10 requests, canonical devices, valid active booking -> ALLOW/EXECUTED)
4. PRE_CHECKIN scenario (3 requests, unactivated booking -> DENY/BLOCKED)
5. SUSPICIOUS_ACTIVITY scenario (contextual flags + explicit trust security event)
6. REQUEST_FLOODING scenario (burst frequency + explicit flooding trust event)
7. HIGH_RISK_ATTACK scenario (router admin / extreme risk context -> DENY/BLOCKED)
8. LOW_TRUST_ATTACK scenario (malicious trust degradation -> smart contract DENY/BLOCKED)
9. UNAUTHORIZED_SENSITIVE_ACCESS scenario (camera / router / owner -> ABAC FAIL/BLOCKED)
10. RESTRICT_ENFORCEMENT scenario (high sensitivity door control downgraded; status read executed)
11. POST_CHECKOUT scenario (expired booking -> DENY/BLOCKED)
12. RECOVERY scenario (explicit administrative recovery event -> restored valid access)
13. POST /api/simulator/run-all (executes all 10 scenarios sequentially)
"""

import json
import time
import subprocess
import urllib.request
import urllib.error
from datetime import datetime, timezone, timedelta

BASE = "http://localhost:8090"

def banner(title):
    print("\n" + "=" * 75)
    print(f"  {title}")
    print("=" * 75)

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

def post(path, body=None): return http("POST", path, body)
def get(path):             return http("GET",  path)

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

def setup_prerequisites():
    set_device_trust("DOOR-SENSOR-001", 80.0)

    # 1. Ensure devices exist
    s, devs = get("/api/devices")
    dev_ids = {d.get("deviceIdentifier") for d in devs}

    fleet = [
        ("DOOR-SENSOR-001", "Smart Door Lock", "SMART_DOOR_LOCK", "ACTUATOR"),
        ("WIFI-001", "Guest Wi-Fi Controller", "GUEST_WIFI", "CONTROLLER"),
        ("TV-001", "Living Room Smart TV", "SMART_TV", "ENDPOINT"),
        ("AC-001", "Bedroom Air Conditioner", "AIR_CONDITIONER", "ACTUATOR"),
        ("LIGHT-001", "Living Room Smart Light", "SMART_LIGHT", "ACTUATOR"),
        ("THERMOSTAT-001", "Smart Thermostat", "SMART_THERMOSTAT", "ACTUATOR"),
        ("CAM-001", "Security Camera", "SECURITY_CAMERA", "SENSOR"),
        ("ROUTER-001", "Gateway Router", "ROUTER", "GATEWAY"),
        ("OWNER-001", "Owner Settings", "OWNER_SETTINGS", "CONTROLLER")
    ]

    for dev_id, name, dtype, dclass in fleet:
        if dev_id not in dev_ids:
            post("/api/devices", {
                "deviceIdentifier": dev_id,
                "name": name,
                "deviceType": dtype,
                "deviceClass": dclass,
                "location": "Property-001",
                "initialTrust": 80.0
            })

    # 2. Ensure active booking
    s, bookings = get("/api/bookings")
    booking_ids = {b.get("bookingReference") for b in bookings}

    now = datetime.now(timezone.utc)
    checkin = (now - timedelta(hours=2)).strftime("%Y-%m-%dT%H:%M:%SZ")
    checkout = (now + timedelta(days=3)).strftime("%Y-%m-%dT%H:%M:%SZ")

    if "BOOKING-PHASE6B-001" not in booking_ids:
        post("/api/bookings", {
            "bookingReference": "BOOKING-PHASE6B-001",
            "guestUserId": "guest-user-001",
            "propertyId": "Property-001",
            "checkInTime": checkin,
            "checkOutTime": checkout,
            "active": True
        })

    # 3. Ensure policies exist for guest
    s, pols = get("/api/policies")
    existing_pols = {p.get("name") for p in pols}

    policies = [
        ("Phase6B-Guest-DoorLock-CONTROL", "Guest Door Lock Control", "SMART_DOOR_LOCK", "CONTROL"),
        ("Phase6B-Guest-DoorLock-READ", "Guest Door Lock Read", "SMART_DOOR_LOCK", "READ"),
        ("Phase7-Guest-Light-CONTROL", "Guest Light Control", "SMART_LIGHT", "CONTROL"),
        ("Phase7-Guest-TV-CONTROL", "Guest TV Control", "SMART_TV", "CONTROL"),
        ("Phase7-Guest-AC-CONTROL", "Guest AC Control", "AIR_CONDITIONER", "CONTROL"),
        ("Phase7-Guest-Thermostat-READ", "Guest Thermostat Read", "SMART_THERMOSTAT", "READ"),
        ("Phase7-Guest-Thermostat-CONTROL", "Guest Thermostat Control", "SMART_THERMOSTAT", "CONTROL"),
        ("Phase7-Guest-WiFi-CONTROL", "Guest WiFi Control", "GUEST_WIFI", "CONTROL")
    ]

    for pname, pdesc, pres, pop in policies:
        create_policy_if_missing(pname, pdesc, pres, pop, existing_pols)

def main():
    banner("PHASE 7B LIVE IOT SIMULATOR TEST SUITE")
    setup_prerequisites()

    passed = 0
    total = 0

    # 1. Check Simulator Status & Scenarios catalog
    total += 1
    s, status = get("/api/simulator/status")
    print(f"\n[Test 1] GET /api/simulator/status: HTTP {s}, running={status.get('running')}")
    if s == 200 and "deviceStates" in status:
        print("  -> PASS: Simulator status retrieved successfully.")
        passed += 1
    else:
        print(f"  -> FAIL: {status}")

    total += 1
    s, scenarios = get("/api/simulator/scenarios")
    print(f"\n[Test 2] GET /api/simulator/scenarios: HTTP {s}, found {len(scenarios)} scenarios")
    if s == 200 and len(scenarios) == 10:
        print("  -> PASS: All 10 scenario archetypes available in catalog.")
        passed += 1
    else:
        print(f"  -> FAIL: {scenarios}")

    # 2. Simulator Start & Reset
    total += 1
    s, r_start = post("/api/simulator/start")
    s2, r_reset = post("/api/simulator/reset")
    print(f"\n[Test 3] POST /api/simulator/start & reset: start={r_start.get('running')}, reset={r_reset.get('running')}")
    if s == 200 and s2 == 200 and not r_reset.get("running"):
        print("  -> PASS: Lifecycle start and reset functional.")
        passed += 1
    else:
        print(f"  -> FAIL: start={r_start}, reset={r_reset}")

    # 3. NORMAL_STAY Scenario
    total += 1
    set_device_trust("DOOR-SENSOR-001", 80.0)
    print("\n[Test 4] Running scenario: NORMAL_STAY...")
    s, res = post("/api/simulator/scenarios/NORMAL_STAY/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, allow={res.get('allowCount')}, exec={res.get('executedCount')}")
    if s == 200 and res.get("totalRequests") == 10 and res.get("allowCount") == 10 and res.get("executedCount") == 10:
        print("  -> PASS: NORMAL_STAY generated 10 requests, 10 ALLOW, 10 EXECUTED.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 4. PRE_CHECKIN Scenario
    total += 1
    print("\n[Test 5] Running scenario: PRE_CHECKIN...")
    s, res = post("/api/simulator/scenarios/PRE_CHECKIN/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, deny={res.get('denyCount')}, blocked={res.get('blockedCount')}")
    if s == 200 and res.get("totalRequests") == 3 and res.get("denyCount") == 3 and res.get("blockedCount") == 3:
        print("  -> PASS: PRE_CHECKIN correctly rejected access prior to booking activation (DENY/BLOCKED).")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 5. SUSPICIOUS_ACTIVITY Scenario
    total += 1
    print("\n[Test 6] Running scenario: SUSPICIOUS_ACTIVITY...")
    s, res = post("/api/simulator/scenarios/SUSPICIOUS_ACTIVITY/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, success={res.get('success')}")
    if s == 200 and res.get("success") and res.get("totalRequests") == 2:
        print("  -> PASS: SUSPICIOUS_ACTIVITY processed contextual risk and explicit trust penalty.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 6. REQUEST_FLOODING Scenario
    total += 1
    print("\n[Test 7] Running scenario: REQUEST_FLOODING...")
    s, res = post("/api/simulator/scenarios/REQUEST_FLOODING/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, success={res.get('success')}")
    if s == 200 and res.get("success") and res.get("totalRequests") >= 10:
        print("  -> PASS: REQUEST_FLOODING generated high-frequency burst and trust event.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 7. HIGH_RISK_ATTACK Scenario
    total += 1
    print("\n[Test 8] Running scenario: HIGH_RISK_ATTACK...")
    s, res = post("/api/simulator/scenarios/HIGH_RISK_ATTACK/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, deny={res.get('denyCount')}, blocked={res.get('blockedCount')}")
    if s == 200 and res.get("denyCount") >= 1 and res.get("blockedCount") >= 1:
        print("  -> PASS: HIGH_RISK_ATTACK blocked critical and extreme risk access attempts.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 8. LOW_TRUST_ATTACK Scenario
    total += 1
    print("\n[Test 9] Running scenario: LOW_TRUST_ATTACK...")
    s, res = post("/api/simulator/scenarios/LOW_TRUST_ATTACK/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, deny={res.get('denyCount')}, blocked={res.get('blockedCount')}")
    if s == 200 and res.get("denyCount") == 1 and res.get("blockedCount") == 1:
        print("  -> PASS: LOW_TRUST_ATTACK degraded device trust and blocked access on-chain.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 9. UNAUTHORIZED_SENSITIVE_ACCESS Scenario
    total += 1
    print("\n[Test 10] Running scenario: UNAUTHORIZED_SENSITIVE_ACCESS...")
    s, res = post("/api/simulator/scenarios/UNAUTHORIZED_SENSITIVE_ACCESS/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, deny={res.get('denyCount')}, blocked={res.get('blockedCount')}")
    if s == 200 and res.get("totalRequests") == 3 and res.get("denyCount") == 3 and res.get("blockedCount") == 3:
        print("  -> PASS: UNAUTHORIZED_SENSITIVE_ACCESS rejected guest attempts on camera, router, owner settings.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 10. RESTRICT_ENFORCEMENT Scenario
    total += 1
    set_device_trust("DOOR-SENSOR-001", 80.0)
    print("\n[Test 11] Running scenario: RESTRICT_ENFORCEMENT...")
    s, res = post("/api/simulator/scenarios/RESTRICT_ENFORCEMENT/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, restrict={res.get('restrictCount')}, downgraded={res.get('downgradedCount')}")
    if s == 200 and res.get("restrictCount") >= 2 and res.get("downgradedCount") >= 1:
        print("  -> PASS: RESTRICT_ENFORCEMENT suppressed physical door control and permitted status read.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 11. POST_CHECKOUT Scenario
    total += 1
    print("\n[Test 12] Running scenario: POST_CHECKOUT...")
    s, res = post("/api/simulator/scenarios/POST_CHECKOUT/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, deny={res.get('denyCount')}, blocked={res.get('blockedCount')}")
    if s == 200 and res.get("totalRequests") == 3 and res.get("denyCount") == 3 and res.get("blockedCount") == 3:
        print("  -> PASS: POST_CHECKOUT blocked guest access after expired booking.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 12. RECOVERY Scenario
    total += 1
    print("\n[Test 13] Running scenario: RECOVERY...")
    s, res = post("/api/simulator/scenarios/RECOVERY/run")
    print(f"  Result: HTTP {s}, reqs={res.get('totalRequests')}, allow={res.get('allowCount')}, exec={res.get('executedCount')}")
    if s == 200 and res.get("allowCount") >= 1 and res.get("executedCount") >= 1:
        print("  -> PASS: RECOVERY submitted explicit recovery event and restored valid access.")
        passed += 1
    else:
        print(f"  -> FAIL: {res}")

    # 13. POST /api/simulator/run-all
    total += 1
    set_device_trust("DOOR-SENSOR-001", 80.0)
    print("\n[Test 14] Running POST /api/simulator/run-all...")
    s, all_res = post("/api/simulator/run-all")
    print(f"  Result: HTTP {s}, executed {len(all_res)} scenarios")
    if s == 200 and len(all_res) == 10 and all(r.get("success") for r in all_res):
        print("  -> PASS: run-all executed all 10 scenarios sequentially with 100% success.")
        passed += 1
    else:
        print(f"  -> FAIL: {all_res}")

    banner("PHASE 7B SIMULATOR TEST RESULTS")
    print(f"  Passed: {passed}/{total} ({(passed/total)*100:.1f}%)")
    if passed == total:
        print("  Status: ALL PHASE 7B TESTS PASSED!")
    else:
        print("  Status: SOME TESTS FAILED.")

    return passed == total

if __name__ == "__main__":
    success = main()
    exit(0 if success else 1)
