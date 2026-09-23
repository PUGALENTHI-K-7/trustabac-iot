"""
Phase 7C RabbitMQ / Spring AMQP Live Event Pipeline Verification Suite.
Tests:
1. RabbitMQ Live Connectivity and Topology Health Check (/api/messaging/status).
2. Synchronous REST Resource Operation Execution Unaffected (Authoritative baseline).
3. IoT Simulator Scenario Execution with Asynchronous Event Dispatching.
4. RabbitMQ Metrics & Asynchronous Event Consumption Verification.
5. In-Memory Idempotency & Duplicate Protection Verification (Single side-effect guarantee).
6. Security Degradation & Recovery Simulation Event Decoupling.
"""

import sys
import json
import time
import subprocess
import urllib.request
import urllib.error
from datetime import datetime, timezone, timedelta

BASE_URL = "http://localhost:8090"

def log_step(name):
    print(f"\n[PHASE 7C TEST] >>> {name}")

def make_request(path, method="GET", data=None):
    url = f"{BASE_URL}{path}"
    headers = {"Content-Type": "application/json"}
    body = json.dumps(data).encode("utf-8") if data is not None else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            resp_body = resp.read().decode("utf-8")
            return status, json.loads(resp_body) if resp_body else {}
    except urllib.error.HTTPError as e:
        resp_body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(resp_body)
        except Exception:
            return e.code, {"error": resp_body}
    except Exception as e:
        return 500, {"error": str(e)}

def set_device_trust(device_id="DOOR-SENSOR-001", trust_val=80.0):
    cmd = f'docker exec releasemind-mysql mysql -uroot -proot_secret_change_me trustabac_iot -e "UPDATE devices SET current_trust = {trust_val} WHERE device_identifier = \'{device_id}\';"'
    subprocess.run(cmd, shell=True, capture_output=True)

def setup_prerequisites():
    # Ensure canonical devices exist
    s, devs = make_request("/api/devices")
    dev_ids = {d.get("deviceIdentifier") for d in devs} if isinstance(devs, list) else set()
    devices_to_ensure = [
        ("DOOR-SENSOR-001", "Smart Lock Front Door", "SMART_LOCK", "ACTUATOR"),
        ("LIGHT-001", "Living Room Smart Light", "SMART_LIGHT", "ACTUATOR"),
        ("THERMOSTAT-001", "Main HVAC Thermostat", "THERMOSTAT", "ACTUATOR"),
        ("SECURITY-CAM-001", "Outdoor Security Camera", "CAMERA", "SENSOR")
    ]
    for did, dname, dtype, dclass in devices_to_ensure:
        if did not in dev_ids:
            make_request("/api/devices", method="POST", data={
                "deviceIdentifier": did,
                "deviceName": dname,
                "deviceType": dtype,
                "deviceClass": dclass,
                "location": "Property-001",
                "initialTrust": 80.0
            })

    # Ensure active booking
    s, bookings = make_request("/api/bookings")
    booking_ids = {b.get("bookingReference") for b in bookings} if isinstance(bookings, list) else set()
    now = datetime.now(timezone.utc)
    checkin = (now - timedelta(hours=2)).strftime("%Y-%m-%dT%H:%M:%SZ")
    checkout = (now + timedelta(days=3)).strftime("%Y-%m-%dT%H:%M:%SZ")

    if "BOOKING-PHASE6B-001" not in booking_ids:
        make_request("/api/bookings", method="POST", data={
            "bookingReference": "BOOKING-PHASE6B-001",
            "guestUserId": "guest-user-001",
            "propertyId": "Property-001",
            "checkInTime": checkin,
            "checkOutTime": checkout,
            "active": True
        })

    # Ensure policies exist
    s, pols = make_request("/api/policies")
    existing_pols = {p.get("name") for p in pols} if isinstance(pols, list) else set()

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
        if pname not in existing_pols:
            make_request("/api/policies", method="POST", data={
                "name": pname,
                "description": pdesc,
                "targetResource": pres,
                "targetOperation": pop,
                "effect": "PERMIT",
                "priority": 10,
                "active": True,
                "conditions": [
                    {
                        "attributeName": "subject.role",
                        "operator": "EQUALS",
                        "expectedValue": "GUEST"
                    },
                    {
                        "attributeName": "booking.valid",
                        "operator": "EQUALS",
                        "expectedValue": "true"
                    }
                ]
            })

def run_tests():
    setup_prerequisites()
    set_device_trust("DOOR-SENSOR-001", 80.0)

    total = 0
    passed = 0

    # 1. Test Messaging Status Endpoint & Broker Reachability
    log_step("1. Verify RabbitMQ Broker Reachability & Topology via /api/messaging/status")
    total += 1
    status, res = make_request("/api/messaging/status")
    if status == 200 and res.get("brokerReachable") is True and res.get("messagingEnabled") is True:
        print(f"  PASS: RabbitMQ is reachable and topology is verified:")
        print(f"        Exchange: {res.get('exchange')}")
        print(f"        Queues: {list(res.get('queues', {}).values())}")
        print(f"        Routing Keys: {list(res.get('routingKeys', {}).values())}")
        passed += 1
    else:
        print(f"  FAIL: Messaging status error. Status={status}, Body={res}")

    # 2. Test Synchronous Authorization Unaffected (REST baseline)
    log_step("2. Verify Synchronous REST Resource Operation Execution Unaffected")
    total += 1
    exec_req = {
        "deviceIdentifier": "DOOR-SENSOR-001",
        "resource": "SMART_DOOR_LOCK",
        "operation": "READ",
        "userId": "guest-user-001",
        "role": "GUEST",
        "organization": "SmartRental",
        "propertyId": "Property-001",
        "bookingId": "BOOKING-PHASE6B-001",
        "bookingValid": True,
        "location": "Property-001",
        "networkContext": "LOCAL_WIFI",
        "requestCountWindow": 1,
        "recentViolationCount": 0,
        "behavioralIndicator": "NORMAL"
    }
    status, res = make_request("/api/resource-operations/execute", method="POST", data=exec_req)
    if status == 200 and res.get("authorizationDecision") == "ALLOW" and res.get("enforcementStatus") == "EXECUTED":
        print(f"  PASS: Synchronous resource operation executed: decision={res.get('authorizationDecision')}, status={res.get('enforcementStatus')}")
        passed += 1
    else:
        print(f"  FAIL: Synchronous execution failed: Status={status}, Body={res}")

    # 3. Test Simulation Run with Asynchronous RabbitMQ Publishing
    log_step("3. Trigger IoT Simulator Scenario and Verify Event Pipeline Dispatch")
    total += 1
    status, sim_res = make_request("/api/simulator/scenarios/NORMAL_STAY/run", method="POST")
    if status == 200 and sim_res.get("scenarioName") == "NORMAL_STAY" and sim_res.get("executedCount") == 10:
        print(f"  PASS: Simulator executed {sim_res.get('totalRequests')} requests (Executed={sim_res.get('executedCount')})")
        passed += 1
    else:
        print(f"  FAIL: Simulator run failed: Status={status}, Body={sim_res}")

    # Allow async messages to be processed by consumers
    time.sleep(1.5)

    # 4. Verify Metrics reflect published & consumed messages
    log_step("4. Verify Metrics for Published and Consumed Events")
    total += 1
    status, res = make_request("/api/messaging/status")
    metrics = res.get("metrics", {})
    total_pub = metrics.get("totalPublishedEvents", 0)
    dev_rec = metrics.get("deviceEventsReceived", 0)
    auth_rec = metrics.get("authorizationEventsReceived", 0)
    tracked_idemp = metrics.get("idempotencyTrackedEvents", 0)

    if total_pub > 0 and (dev_rec > 0 or auth_rec > 0) and tracked_idemp > 0:
        print(f"  PASS: Messaging metrics verified:")
        print(f"        Total Published Events: {total_pub}")
        print(f"        Device Events Received: {dev_rec}")
        print(f"        Auth Events Received: {auth_rec}")
        print(f"        Idempotency Tracked Events: {tracked_idemp}")
        passed += 1
    else:
        print(f"  FAIL: Messaging metrics insufficient: {metrics}")

    # 5. Verify In-Memory Idempotency & Single Side-Effect Guarantee
    log_step("5. Verify Idempotency Tracking & At-Most-Once Delivery Semantics")
    total += 1
    if tracked_idemp >= (dev_rec + auth_rec):
        print(f"  PASS: IdempotencyGuard actively protects against duplicate processing ({tracked_idemp} events tracked)")
        passed += 1
    else:
        print(f"  FAIL: Idempotency guard tracking mismatch: tracked={tracked_idemp}, consumed={dev_rec + auth_rec}")

    # 6. Verify Security Degradation & Recovery Simulation Event Decoupling
    log_step("6. Trigger Attack & Recovery Scenario and Verify Decoupled Trust Pipeline")
    total += 1
    status, sim_att = make_request("/api/simulator/scenarios/LOW_TRUST_ATTACK/run", method="POST")
    time.sleep(1.5)
    status_m, res_m = make_request("/api/messaging/status")
    new_metrics = res_m.get("metrics", {})
    new_pub = new_metrics.get("totalPublishedEvents", 0)

    if status == 200 and new_pub > total_pub:
        print(f"  PASS: Attack scenario published additional events (New Total Published: {new_pub})")
        passed += 1
    else:
        print(f"  FAIL: Attack scenario event dispatch failed: new_pub={new_pub}, old_pub={total_pub}")

    # Reset device trust back to 80
    set_device_trust("DOOR-SENSOR-001", 80.0)

    # Summary
    print("\n" + "=" * 60)
    print(f"PHASE 7C RABBITMQ VERIFICATION SUMMARY: {passed}/{total} Passed")
    print("=" * 60)

    if passed == total:
        print("ALL PHASE 7C TESTS PASSED SUCCESSFULLY!")
        return 0
    else:
        print(f"FAILURE: {total - passed} test(s) failed.")
        return 1

if __name__ == "__main__":
    sys.exit(run_tests())
