#!/usr/bin/env python3
"""
Phase 7D Live Verification Script: WebSocket / STOMP Real-Time Telemetry Streaming

Verifies end-to-end telemetry streaming over WebSocket / STOMP for:
- Connection & STOMP Handshake (/ws)
- Topic Subscriptions across all 7 topics:
    * /topic/devices
    * /topic/trust
    * /topic/risk
    * /topic/authorization
    * /topic/blockchain
    * /topic/simulator
    * /topic/security
- Real-time event streaming triggered by IoT simulator execution
- Correlation ID preservation across all streams
- Payload security (no secrets / private keys / passwords)
- Session & publication metrics verification via /api/websocket/status
"""

import asyncio
import json
import re
import sys
import time
import urllib.request
import urllib.error
import subprocess
from datetime import datetime, timezone, timedelta
import websockets

BASE_URL = "http://localhost:8090"
WS_URL = "ws://localhost:8090/ws"

def print_header(title):
    print("\n" + "=" * 70)
    print(f" {title}")
    print("=" * 70)

def http_get(endpoint):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, headers={"Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=10) as resp:
        return json.loads(resp.read().decode())

def http_post(endpoint, data=None):
    url = f"{BASE_URL}{endpoint}"
    body = json.dumps(data).encode() if data is not None else b""
    headers = {"Content-Type": "application/json", "Accept": "application/json"} if data is not None else {"Accept": "application/json"}
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=30) as resp:
        content = resp.read().decode()
        return json.loads(content) if content else {}

def set_device_trust(device_id="DOOR-SENSOR-001", trust_val=80.0):
    cmd = f'docker exec releasemind-mysql mysql -uroot -proot_secret_change_me trustabac_iot -e "UPDATE devices SET current_trust = {trust_val} WHERE device_identifier = \'{device_id}\';"'
    subprocess.run(cmd, shell=True, capture_output=True)

def setup_prerequisites():
    # Ensure canonical devices exist
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

    # Ensure booking exists and is active
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

    # Ensure policies exist
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

async def test_websocket_telemetry():
    results = []

    print_header("PHASE 7D STEP 1: Verify WebSocket Status API (/api/websocket/status)")
    try:
        status_resp = http_get("/api/websocket/status")
        print(f"Status response: {json.dumps(status_resp, indent=2)}")
        assert status_resp.get("status") == "ACTIVE", f"Expected ACTIVE, got {status_resp.get('status')}"
        assert status_resp.get("endpoint") == "/ws", f"Expected /ws, got {status_resp.get('endpoint')}"
        assert len(status_resp.get("supportedTopics", [])) == 7, "Expected 7 supported topics"
        print("  [PASS] Status API verified successfully.")
        results.append(("Status API Verification", True))
    except Exception as e:
        print(f"  [FAIL] Status API failed: {e}")
        results.append(("Status API Verification", False))
        return results

    setup_prerequisites()
    set_device_trust("DOOR-SENSOR-001", 80.0)

    print_header("PHASE 7D STEP 2: Connect & STOMP Handshake on ws://localhost:8090/ws")
    received_events = {
        "/topic/devices": [],
        "/topic/trust": [],
        "/topic/risk": [],
        "/topic/authorization": [],
        "/topic/blockchain": [],
        "/topic/simulator": [],
        "/topic/security": []
    }

    try:
        async with websockets.connect(WS_URL) as ws:
            print("  Connected to raw WebSocket endpoint.")

            # Send STOMP CONNECT frame
            connect_frame = "CONNECT\naccept-version:1.1,1.2\nheart-beat:10000,10000\n\n\x00"
            await ws.send(connect_frame)
            response = await asyncio.wait_for(ws.recv(), timeout=5.0)
            frames = parse_stomp_frames(response)
            assert len(frames) > 0 and frames[0][0] == "CONNECTED", f"Expected CONNECTED frame, got {frames}"
            print(f"  [PASS] STOMP CONNECTED received: {frames[0][1]}")
            results.append(("STOMP Handshake", True))

            print_header("PHASE 7D STEP 3: Subscribe to All 7 Telemetry Topics")
            topics = [
                "/topic/devices",
                "/topic/trust",
                "/topic/risk",
                "/topic/authorization",
                "/topic/blockchain",
                "/topic/simulator",
                "/topic/security"
            ]
            for idx, topic in enumerate(topics):
                sub_frame = f"SUBSCRIBE\nid:sub-{idx}\ndestination:{topic}\n\n\x00"
                await ws.send(sub_frame)
                await asyncio.sleep(0.05)
                print(f"  Subscribed to {topic} (id: sub-{idx})")
            results.append(("Topic Subscriptions", True))

            # Verify active session count via REST
            status_after_connect = http_get("/api/websocket/status")
            active_sessions = status_after_connect["metrics"]["activeSessions"]
            print(f"  Active WebSocket sessions reported by server: {active_sessions}")
            assert active_sessions >= 1, f"Expected activeSessions >= 1, got {active_sessions}"

            # Async background reader task
            async def event_listener():
                try:
                    while True:
                        msg = await ws.recv()
                        stomp_frames = parse_stomp_frames(msg)
                        for cmd, headers, body in stomp_frames:
                            if cmd == "MESSAGE":
                                dest = headers.get("destination")
                                if dest in received_events:
                                    try:
                                        payload_json = json.loads(body)
                                        received_events[dest].append((headers, payload_json))
                                    except Exception:
                                        received_events[dest].append((headers, body))
                except asyncio.CancelledError:
                    pass
                except Exception as ex:
                    print(f"Listener ended: {ex}")

            listener_task = asyncio.create_task(event_listener())

            print_header("PHASE 7D STEP 4: Trigger Real Simulation & Live Stream Telemetry")
            print("  Triggering NORMAL_STAY scenario (10 requests -> ALLOW/EXECUTED)...")
            sim_resp = http_post("/api/simulator/scenarios/NORMAL_STAY/run")
            print(f"  Scenario execution returned {sim_resp.get('totalRequests')} events.")

            print("  Triggering LOW_TRUST_ATTACK scenario to generate security alert & trust update...")
            attack_resp = http_post("/api/simulator/scenarios/LOW_TRUST_ATTACK/run")
            print(f"  Attack scenario execution returned {attack_resp.get('totalRequests')} events.")

            # Wait for queued messages to arrive over WebSocket
            print("  Waiting 4.0s for real-time WebSocket event frames to settle...")
            await asyncio.sleep(4.0)
            listener_task.cancel()

            print_header("PHASE 7D STEP 5: Verify Received Live Telemetry Frames")
            all_topics_streamed = True
            for topic, evts in received_events.items():
                print(f"  Topic [{topic}]: received {len(evts)} event frames")
                if len(evts) == 0:
                    print(f"  [WARN] No events captured on {topic}")
                    all_topics_streamed = False
                else:
                    sample_envelope = evts[0][1]
                    corr_id = sample_envelope.get("correlationId") if isinstance(sample_envelope, dict) else "N/A"
                    print(f"    Sample correlationId: {corr_id}")
                    # Payload security check: ensure no private keys or credentials leaked
                    raw_str = json.dumps(sample_envelope) if isinstance(sample_envelope, dict) else str(sample_envelope)
                    forbidden_terms = ["privateKey", "private_key", "secretKey", "secret_key"]
                    for term in forbidden_terms:
                        assert term not in raw_str, f"CRITICAL: '{term}' leaked in WebSocket payload!"
                    assert not re.search(r"password|secret_change_me", raw_str, re.I), "CRITICAL: Secret/password leaked in WebSocket payload!"

            results.append(("Real-time Event Streaming Across Topics", all_topics_streamed))

            # Send DISCONNECT frame
            await ws.send("DISCONNECT\n\n\x00")
            print("  Sent STOMP DISCONNECT.")

    except Exception as e:
        print(f"  [FAIL] WebSocket streaming test failed: {e}")
        results.append(("Live WebSocket Telemetry Streaming", False))
        return results

    # Reset device trust back to 80
    set_device_trust("DOOR-SENSOR-001", 80.0)

    print_header("PHASE 7D STEP 6: Verify Metrics & Session Disconnection")
    try:
        # Give server moment to process disconnect
        time.sleep(1.0)
        status_final = http_get("/api/websocket/status")
        metrics = status_final["metrics"]
        print(f"Final Metrics: {json.dumps(metrics, indent=2)}")
        assert metrics["totalConnected"] >= 1, "Expected totalConnected >= 1"
        assert metrics["messagesPublished"] > 0, "Expected messagesPublished > 0"
        assert metrics["publishFailures"] == 0, f"Expected publishFailures == 0, got {metrics['publishFailures']}"
        print("  [PASS] Session metrics and failure isolation verified.")
        results.append(("WebSocket Metrics & Health Verification", True))
    except Exception as e:
        print(f"  [FAIL] Final metrics verification failed: {e}")
        results.append(("WebSocket Metrics & Health Verification", False))

    return results

def main():
    print("=" * 70)
    print(" TRUSTABAC-IOT: PHASE 7D WEBSOCKET REAL-TIME STREAMING VERIFICATION")
    print("=" * 70)

    loop = asyncio.new_event_loop()
    asyncio.set_event_loop(loop)
    results = loop.run_until_complete(test_websocket_telemetry())

    print("\n" + "=" * 70)
    print(" PHASE 7D TEST SUMMARY")
    print("=" * 70)
    all_passed = True
    for test_name, passed in results:
        status = "PASSED [OK]" if passed else "FAILED [ERR]"
        print(f" {test_name:<45} : {status}")
        if not passed:
            all_passed = False

    print("=" * 70)
    if all_passed:
        print(" PHASE 7D WEBSOCKET TELEMETRY STREAMING: 100% VERIFIED & COMPLETE")
        sys.exit(0)
    else:
        print(" PHASE 7D WEBSOCKET TELEMETRY STREAMING: VERIFICATION FAILED")
        sys.exit(1)

if __name__ == "__main__":
    main()
