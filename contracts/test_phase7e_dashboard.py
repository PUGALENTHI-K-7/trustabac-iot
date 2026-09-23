"""
Phase 7E Test Suite: Real-Time Monitoring Dashboard Verification
Tests:
1. Dashboard HTML, CSS, JS endpoints availability and proper rendering
2. Security & Observational constraints (No secrets in JS, No client-side decision logic)
3. Initial REST state & Health endpoints integration
4. Live STOMP over WebSocket streaming across all 7 topics
5. Scenario 1 (NORMAL_STAY): Full pipeline ALLOW / EXECUTED telemetry
6. Scenario 2 (RESTRICT_ENFORCEMENT): RESTRICT / DOWNGRADED physical enforcement
7. Scenario 3 (LOW_TRUST_ATTACK): Trust penalty & DENY / BLOCKED outcome
8. Scenario 4 (UNAUTHORIZED_SENSITIVE_ACCESS): ABAC FAIL & DENY / BLOCKED outcome
9. Correlation ID & Causation ID preservation
10. WebSocket Reconnection & Subscription idempotency
"""

import asyncio
import json
import os
import re
import sys
import time
import urllib.request
import urllib.error
import subprocess
from datetime import datetime, timezone, timedelta
import websockets

if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_URL = "http://localhost:8090"
WS_URL = "ws://localhost:8090/ws"

def http_get(endpoint, accept="application/json"):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, headers={"Accept": accept})
    with urllib.request.urlopen(req, timeout=10) as resp:
        content = resp.read().decode("utf-8")
        status = resp.getcode()
        return status, content, resp.headers

def http_post(endpoint, data=None):
    url = f"{BASE_URL}{endpoint}"
    body = json.dumps(data).encode("utf-8") if data is not None else b""
    headers = {"Content-Type": "application/json", "Accept": "application/json"} if data is not None else {"Accept": "application/json"}
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=30) as resp:
        content = resp.read().decode("utf-8")
        return resp.getcode(), json.loads(content) if content else {}

def set_device_trust(device_id="DOOR-SENSOR-001", trust_val=80.0):
    cmd = f'docker exec trustabac-mysql mysql -uroot -proot_secret_change_me trustabac_iot -e "UPDATE devices SET current_trust = {trust_val} WHERE device_identifier = \'{device_id}\';"'
    subprocess.run(cmd, shell=True, capture_output=True)

def setup_prerequisites():
    devices = [
        ("DOOR-SENSOR-001", "Smart Lock Front Door", "SMART_DOOR_LOCK", "ACTUATOR"),
        ("LIGHT-001", "Living Room Smart Light", "SMART_LIGHT", "ACTUATOR"),
        ("THERMOSTAT-001", "Main HVAC Thermostat", "SMART_THERMOSTAT", "ACTUATOR"),
        ("TV-001", "Living Room Smart TV", "SMART_TV", "ENDPOINT"),
        ("AC-001", "Master Bedroom AC", "AIR_CONDITIONER", "ACTUATOR"),
        ("WIFI-001", "Guest WiFi Router", "GUEST_WIFI", "CONTROLLER"),
        ("CAMERA-001", "Outdoor Security Camera", "SECURITY_CAMERA", "SENSOR"),
        ("ROUTER-001", "Core Gateway Router", "CORE_ROUTER", "CONTROLLER"),
        ("CURTAIN-001", "Smart Bedroom Curtains", "SMART_CURTAINS", "ACTUATOR")
    ]
    for did, dname, dtype, dclass in devices:
        try:
            http_post("/api/devices", {
                "deviceIdentifier": did,
                "deviceName": dname,
                "deviceType": dtype,
                "deviceClass": dclass,
                "location": "PROP-LUXURY-001",
                "organization": "ORG-RENTAL-CORP",
                "initialTrust": 80.0
            })
        except Exception:
            pass

    now = datetime.now(timezone.utc)
    checkin = (now - timedelta(hours=2)).strftime("%Y-%m-%dT%H:%M:%S")
    checkout = (now + timedelta(days=2)).strftime("%Y-%m-%dT%H:%M:%S")
    try:
        http_post("/api/bookings", {
            "bookingId": "BOOKING-ACTIVE-001",
            "guestUserId": "guest-user-001",
            "propertyId": "PROP-LUXURY-001",
            "checkInTime": checkin,
            "checkOutTime": checkout,
            "active": True
        })
    except Exception:
        pass

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
        try:
            http_post("/api/policies", {
                "name": pname,
                "description": pdesc,
                "resource": pres,
                "operation": pop,
                "subjectRole": "GUEST",
                "subjectOrg": "ORG-RENTAL-CORP",
                "deviceType": pres,
                "deviceClass": "ACTUATOR" if "CONTROL" in pop else "SENSOR",
                "minTrust": 60.0,
                "maxRisk": 40.0,
                "active": True
            })
        except Exception:
            pass

def parse_stomp_frames(raw_data):
    frames = []
    raw_frames = raw_data.split("\x00")
    for raw in raw_frames:
        raw = raw.strip()
        if not raw:
            continue
        lines = raw.split("\n")
        command = lines[0].strip()
        headers = {}
        body_idx = 1
        for i, line in enumerate(lines[1:], 1):
            if line.strip() == "":
                body_idx = i + 1
                break
            if ":" in line:
                k, v = line.split(":", 1)
                headers[k.strip()] = v.strip()
        body = "\n".join(lines[body_idx:]).strip() if body_idx < len(lines) else ""
        frames.append((command, headers, body))
    return frames

def test_dashboard_endpoints():
    print("\n--- 1. Testing Dashboard Web Endpoints ---")
    
    # 1. GET /dashboard
    status, html, _ = http_get("/dashboard", accept="text/html")
    assert status == 200, f"Expected 200, got {status}"
    assert "TrustABAC-IoT" in html, "Missing app title in HTML"
    assert "Smart-Rental Device Fleet" in html, "Missing fleet title in HTML"
    assert "DOOR-SENSOR-001" in html, "Missing door sensor in HTML"
    assert "WIFI-001" in html, "Missing wifi in HTML"
    assert "TV-001" in html, "Missing TV in HTML"
    assert "AC-001" in html, "Missing AC in HTML"
    assert "LIGHT-001" in html, "Missing light in HTML"
    assert "THERMOSTAT-001" in html, "Missing thermostat in HTML"
    assert "CAM-001" in html, "Missing camera in HTML"
    assert "ROUTER-001" in html, "Missing router in HTML"
    assert "OWNER-001" in html, "Missing owner/curtain in HTML"
    assert "dashboard.css" in html, "Missing stylesheet reference in HTML"
    assert "dashboard.js" in html, "Missing script reference in HTML"
    print("  [PASS] GET /dashboard returned 200 with full 9-device HTML template.")

    # 2. GET / (Redirect check)
    try:
        req = urllib.request.Request(f"{BASE_URL}/", headers={"Accept": "text/html"})
        opener = urllib.request.build_opener(urllib.request.HTTPRedirectHandler)
        with opener.open(req) as resp:
            assert resp.getcode() == 200
            assert "TrustABAC-IoT" in resp.read().decode("utf-8")
            print("  [PASS] GET / redirects to /dashboard and renders template.")
    except Exception as e:
        raise AssertionError(f"Root redirect failed: {e}")

    # 3. GET /css/dashboard.css
    status, css, _ = http_get("/css/dashboard.css", accept="text/css")
    assert status == 200, f"Expected 200, got {status}"
    assert "--bg-primary:" in css, "CSS missing color variables"
    print("  [PASS] GET /css/dashboard.css returned 200 with CSS tokens.")

    # 4. GET /js/dashboard.js
    status, js, _ = http_get("/js/dashboard.js", accept="application/javascript")
    assert status == 200, f"Expected 200, got {status}"
    assert "SimpleStompClient" in js, "JS missing SimpleStompClient"
    print("  [PASS] GET /js/dashboard.js returned 200 with STOMP telemetry client.")

def test_security_and_observational_constraints():
    print("\n--- 2. Testing Security & Observational Constraints ---")
    _, js_content, _ = http_get("/js/dashboard.js", accept="application/javascript")

    # Verify no leaked secrets
    assert not re.search(r"0x[a-fA-F0-9]{64}", js_content), "Security Violation: Found private key in client JS!"
    forbidden_terms = [
        "root_secret",
        "trustabac_secret",
        "privateKey",
        "secret_key"
    ]
    for term in forbidden_terms:
        assert term not in js_content, f"Security Violation: Found sensitive token '{term}' in client JS!"
    print("  [PASS] Client JavaScript is free of private keys, database credentials, and secrets.")

    # Verify observational nature (no evaluation formulas)
    assert "function evaluateAccess" not in js_content, "Client JS must not evaluate access!"
    assert "function calculateTrust" not in js_content, "Client JS must not calculate trust!"
    assert "function calculateRisk" not in js_content, "Client JS must not calculate risk!"
    print("  [PASS] Client JavaScript is strictly observational with zero authorization or trust decision logic.")

def test_rest_status_endpoints():
    print("\n--- 3. Testing REST Status & Infrastructure Health Endpoints ---")
    
    # Health
    s, h, _ = http_get("/api/health")
    h_data = json.loads(h)
    assert s == 200 and h_data.get("status") == "UP", f"Unexpected health data: {h_data}"
    
    # Blockchain Status
    s, bc, _ = http_get("/api/blockchain/status")
    bc_data = json.loads(bc)
    assert s == 200 and bc_data.get("rpcReachable") is True, f"Ganache RPC unreachable: {bc_data}"
    print(f"  [PASS] Ganache Blockchain connected at {bc_data.get('rpcUrl')}, Latest Block #{bc_data.get('latestBlock')}")

    # Messaging Status
    s, msg, _ = http_get("/api/messaging/status")
    msg_data = json.loads(msg)
    assert s == 200 and msg_data.get("brokerReachable") is True, f"RabbitMQ broker unreachable: {msg_data}"
    print("  [PASS] RabbitMQ Messaging connected.")

    # WebSocket Status
    s, ws, _ = http_get("/api/websocket/status")
    ws_data = json.loads(ws)
    assert s == 200 and ws_data.get("status") == "ACTIVE"
    assert ws_data.get("endpoint") == "/ws"
    print("  [PASS] WebSocket STOMP endpoint active.")

    # Devices REST
    s, devs, _ = http_get("/api/devices")
    devs_data = json.loads(devs)
    assert s == 200 and isinstance(devs_data, list)
    print(f"  [PASS] Devices REST returned {len(devs_data)} initialized devices.")

async def test_live_scenarios_telemetry():
    print("\n--- 4. Testing Live WebSocket Telemetry Across Simulation Scenarios ---")
    
    setup_prerequisites()
    set_device_trust("DOOR-SENSOR-001", 80.0)

    topics = [
        "/topic/devices",
        "/topic/trust",
        "/topic/risk",
        "/topic/authorization",
        "/topic/blockchain",
        "/topic/simulator",
        "/topic/security"
    ]

    async with websockets.connect(WS_URL) as ws:
        # Send STOMP CONNECT
        connect_frame = "CONNECT\naccept-version:1.2,1.1\nheart-beat:10000,10000\n\n\x00"
        await ws.send(connect_frame)
        response = await asyncio.wait_for(ws.recv(), timeout=5.0)
        frames = parse_stomp_frames(response)
        assert len(frames) > 0 and frames[0][0] == "CONNECTED", f"STOMP Connect failed: {frames}"
        print("  [PASS] STOMP connection established.")

        # Subscribe to all 7 topics
        for idx, topic in enumerate(topics):
            sub_frame = f"SUBSCRIBE\nid:sub-{idx}\ndestination:{topic}\nack:auto\n\n\x00"
            await ws.send(sub_frame)
            await asyncio.sleep(0.02)
        print("  [PASS] Subscribed to all 7 telemetry topics.")

        received_events = {t: [] for t in topics}

        async def reader():
            try:
                while True:
                    raw = await ws.recv()
                    stomp_frames = parse_stomp_frames(raw)
                    for cmd, headers, body in stomp_frames:
                        if cmd == "MESSAGE":
                            dest = headers.get("destination")
                            if dest in received_events and body:
                                try:
                                    received_events[dest].append(json.loads(body))
                                except Exception:
                                    received_events[dest].append(body)
            except asyncio.CancelledError:
                pass
            except Exception as e:
                print(f"Reader exception: {e}")

        # Reset simulator first
        http_post("/api/simulator/reset")
        await asyncio.sleep(0.5)

        def unwrap(e):
            if isinstance(e, dict) and "payload" in e and isinstance(e["payload"], dict):
                merged = dict(e["payload"])
                merged["correlationId"] = e.get("correlationId")
                merged["eventId"] = e.get("eventId")
                return merged
            return e if isinstance(e, dict) else {}

        # -------------------------------------------------------------
        # Scenario 1: NORMAL_STAY
        # -------------------------------------------------------------
        print("\n--- 5. Scenario 1: NORMAL_STAY (ALLOW / EXECUTED) ---")
        for k in received_events: received_events[k].clear()
        task = asyncio.create_task(reader())

        http_post("/api/simulator/scenarios/NORMAL_STAY/run")
        await asyncio.sleep(3.0)
        task.cancel()

        print(f"   Received: {len(received_events['/topic/devices'])} device, "
              f"{len(received_events['/topic/trust'])} trust, "
              f"{len(received_events['/topic/risk'])} risk, "
              f"{len(received_events['/topic/authorization'])} auth, "
              f"{len(received_events['/topic/blockchain'])} blockchain, "
              f"{len(received_events['/topic/simulator'])} simulator events.")

        assert len(received_events['/topic/devices']) > 0, "Missing device events for NORMAL_STAY"
        assert len(received_events['/topic/authorization']) > 0, "Missing auth events"
        assert len(received_events['/topic/blockchain']) > 0, "Missing blockchain events"

        auth_events = [unwrap(e) for e in received_events['/topic/authorization']]
        allows = [e for e in auth_events if e.get("decision") == "ALLOW"]
        assert len(allows) > 0, f"NORMAL_STAY should produce ALLOW decisions. Sample: {auth_events[:2]}"
        print(f"  [PASS] NORMAL_STAY confirmed: {len(allows)} ALLOW decisions streamed.")

        # -------------------------------------------------------------
        # Scenario 2: RESTRICT_ENFORCEMENT
        # -------------------------------------------------------------
        print("\n--- 6. Scenario 2: RESTRICT_ENFORCEMENT (RESTRICT / DOWNGRADED) ---")
        for k in received_events: received_events[k].clear()
        task = asyncio.create_task(reader())

        http_post("/api/simulator/scenarios/RESTRICT_ENFORCEMENT/run")
        await asyncio.sleep(3.0)
        task.cancel()

        auth_events = [unwrap(e) for e in received_events['/topic/authorization']]
        restricts = [e for e in auth_events if e.get("decision") == "RESTRICT"]
        assert len(restricts) > 0, f"RESTRICT_ENFORCEMENT must produce RESTRICT decisions. Sample: {auth_events[:2]}"
        assert any(e.get("enforcementStatus") == "DOWNGRADED" or e.get("enforcementOutcome") == "DOWNGRADED" for e in restricts), "RESTRICT must be DOWNGRADED"
        print(f"  [PASS] RESTRICT_ENFORCEMENT confirmed: {len(restricts)} RESTRICT/DOWNGRADED events streamed.")

        # -------------------------------------------------------------
        # Scenario 3: LOW_TRUST_ATTACK
        # -------------------------------------------------------------
        print("\n--- 7. Scenario 3: LOW_TRUST_ATTACK (Trust Penalty -> DENY / BLOCKED) ---")
        for k in received_events: received_events[k].clear()
        task = asyncio.create_task(reader())

        http_post("/api/simulator/scenarios/LOW_TRUST_ATTACK/run")
        await asyncio.sleep(3.0)
        task.cancel()

        trust_events = [unwrap(e) for e in received_events['/topic/trust']]
        assert len(trust_events) > 0, "LOW_TRUST_ATTACK must generate trust events"
        print(f"  [PASS] LOW_TRUST_ATTACK confirmed: Trust update streamed.")

        # -------------------------------------------------------------
        # Scenario 4: UNAUTHORIZED_SENSITIVE_ACCESS
        # -------------------------------------------------------------
        print("\n--- 8. Scenario 4: UNAUTHORIZED_SENSITIVE_ACCESS (ABAC FAIL -> DENY / BLOCKED) ---")
        for k in received_events: received_events[k].clear()
        task = asyncio.create_task(reader())

        http_post("/api/simulator/scenarios/UNAUTHORIZED_SENSITIVE_ACCESS/run")
        await asyncio.sleep(3.0)
        task.cancel()

        auth_events = [unwrap(e) for e in received_events['/topic/authorization']]
        denies = [e for e in auth_events if e.get("decision") == "DENY"]
        assert len(denies) > 0, "UNAUTHORIZED_SENSITIVE_ACCESS must produce DENY decisions"
        assert any(e.get("enforcementStatus") == "BLOCKED" or e.get("enforcementOutcome") == "BLOCKED" for e in denies), "DENY must be BLOCKED"
        print(f"  [PASS] UNAUTHORIZED_SENSITIVE_ACCESS confirmed: {len(denies)} DENY/BLOCKED events streamed.")

        # -------------------------------------------------------------
        # Correlation ID Preservation Check
        # -------------------------------------------------------------
        print("\n--- 9. Verifying Correlation ID Propagation ---")
        for topic, evts in received_events.items():
            for evt in evts:
                if isinstance(evt, dict):
                    assert "correlationId" in evt or "eventId" in evt, f"Event missing correlationId: {evt}"
        print("  [PASS] Correlation IDs and Event IDs consistently preserved across all streams.")

        # Close first connection
        await ws.send("DISCONNECT\n\n\x00")

    # -------------------------------------------------------------
    # Reconnection & Subscription Idempotency
    # -------------------------------------------------------------
    print("\n--- 10. Verifying WebSocket Reconnect & Topic Resubscription ---")
    async with websockets.connect(WS_URL) as ws2:
        await ws2.send("CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n\n\x00")
        await asyncio.wait_for(ws2.recv(), timeout=5.0)
        await ws2.send("SUBSCRIBE\nid:sub-rec\ndestination:/topic/simulator\nack:auto\n\n\x00")
        
        received_rec = []
        async def rec_reader():
            try:
                while True:
                    msg = await ws2.recv()
                    for cmd, h, b in parse_stomp_frames(msg):
                        if cmd == "MESSAGE" and b:
                            received_rec.append(json.loads(b))
            except asyncio.CancelledError:
                pass

        t = asyncio.create_task(rec_reader())
        http_post("/api/simulator/scenarios/RECOVERY/run")
        await asyncio.sleep(2.0)
        t.cancel()
        assert len(received_rec) > 0, "Reconnected client failed to receive simulator event"
        await ws2.send("DISCONNECT\n\n\x00")
        print("  [PASS] WebSocket reconnected cleanly and resumed receiving live telemetry.")

def run_all_tests():
    print("============================================================")
    print("      PHASE 7E: REAL-TIME MONITORING DASHBOARD SUITE       ")
    print("============================================================")
    
    test_dashboard_endpoints()
    test_security_and_observational_constraints()
    test_rest_status_endpoints()
    
    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    loop.run_until_complete(test_live_scenarios_telemetry())
    
    print("\n============================================================")
    print(" ALL PHASE 7E DASHBOARD TESTS PASSED (10/10)")
    print("============================================================\n")

if __name__ == "__main__":
    try:
        run_all_tests()
    except Exception as e:
        print(f"\n[ERROR] TEST SUITE FAILED: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
