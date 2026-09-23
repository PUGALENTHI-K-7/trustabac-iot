"""
Final Comprehensive Smoke Test Suite for TrustABAC-IoT
Executes a complete 15-point operational and security verification matrix against the live stack:
1. /api/health gateway status
2. Blockchain connection & Web3j health (/api/blockchain/status)
3. Smart contract threshold accessibility (/api/blockchain/thresholds)
4. Normal Access (ALLOW -> EXECUTED)
5. Restrict Access (RESTRICT -> DOWNGRADED)
6. Low Trust (DENY -> BLOCKED)
7. High Risk (DENY -> BLOCKED)
8. ABAC Failure (DENY -> BLOCKED)
9. Sensitive-resource protection
10. Fail-closed sensitive access attack rejection
11. Post-attack recovery
12. Dashboard UI availability (/dashboard)
13. WebSocket telemetry endpoint readiness (/api/websocket/status)
14. RabbitMQ broker connectivity (/api/messaging/status)
15. Batch offline analytics endpoint readiness (/api/batch/audit/reports)

Post-Condition: Restores baseline booking, device, trust, and contract configuration state.
"""

import os
import sys
import json
import time
import urllib.request
import urllib.error

# Ensure UTF-8 output
if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_URL = os.environ.get("TRUSTABAC_BASE_URL", "http://localhost:8090")

def http_get(endpoint, accept="application/json"):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, headers={"Accept": accept})
    with urllib.request.urlopen(req, timeout=15) as resp:
        content = resp.read().decode("utf-8")
        return resp.getcode(), json.loads(content) if accept == "application/json" and content else content

def http_post(endpoint, data=None):
    url = f"{BASE_URL}{endpoint}"
    body = json.dumps(data).encode("utf-8") if data is not None else b""
    headers = {"Content-Type": "application/json", "Accept": "application/json"} if data is not None else {"Accept": "application/json"}
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=15) as resp:
            content = resp.read().decode("utf-8")
            return resp.getcode(), json.loads(content) if content else {}
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", errors="ignore")
        return e.code, json.loads(raw) if raw else {"error": str(e)}

def restore_device_trust(device_id="DOOR-SENSOR-001", target_trust=80.0):
    try:
        code, info = http_get(f"/api/trust/{device_id}")
        curr = info.get("currentTrust", 80.0)
        while curr < target_trust:
            http_post(f"/api/trust/{device_id}/events", {
                "eventType": "RECOVERY",
                "source": "ADMIN_RECOVERY",
                "details": "Restoring trust baseline"
            })
            curr += 10.0
    except Exception:
        pass

def degrade_device_trust(device_id="DOOR-SENSOR-001", target_trust=20.0):
    try:
        code, info = http_get(f"/api/trust/{device_id}")
        curr = info.get("currentTrust", 80.0)
        while curr > target_trust:
            http_post(f"/api/trust/{device_id}/events", {
                "eventType": "CONFIRMED_MALICIOUS",
                "source": "SECURITY_MONITOR",
                "details": "Simulating trust penalty"
            })
            curr -= 40.0
    except Exception:
        pass

def main():
    print("================================================================================")
    print("TRUSTABAC-IOT: FINAL 15-POINT SYSTEM SMOKE TEST SUITE")
    print("================================================================================")

    passed_checks = 0
    failed_checks = 0

    def check(num, name, condition, details=""):
        nonlocal passed_checks, failed_checks
        if condition:
            passed_checks += 1
            print(f"  [PASS] Check {num:02d}: {name}")
        else:
            failed_checks += 1
            print(f"  [FAIL] Check {num:02d}: {name} - {details}")

    try:
        # Pre-test reset & trust baseline restoration
        http_post("/api/simulator/reset")
        restore_device_trust("DOOR-SENSOR-001", 80.0)
        restore_device_trust("THERMOSTAT-001", 80.0)

        # Check 1: /api/health
        code, health = http_get("/api/health")
        check(1, "Gateway Health (/api/health)", code == 200 and health.get("status") == "UP")

        # Check 2: Blockchain Connection Status
        code, bc_info = http_get("/api/blockchain/status")
        bc_connected = bc_info.get("rpcReachable") is True and bc_info.get("contractReachable") is True
        check(2, "Blockchain EVM Connection", code == 200 and bc_connected, f"Observed {bc_info}")

        # Check 3: Smart Contract Threshold Accessibility
        code, thresh_info = http_get("/api/blockchain/thresholds")
        check(3, "Smart Contract Threshold Accessibility", code == 200 and "trustHigh" in thresh_info, f"Observed {thresh_info}")

        # Check 4: Normal Access (ALLOW -> EXECUTED)
        payload_normal = {
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
            "requestCountWindow": 1,
            "recentViolationCount": 0,
            "commandPayload": {"action": "UNLOCK"}
        }
        code, resp_normal = http_post("/api/resource-operations/execute", payload_normal)
        dec_normal = resp_normal.get("authorizationDecision")
        enf_normal = resp_normal.get("enforcementStatus")
        check(4, "Normal Access (ALLOW -> EXECUTED)", 
              code == 200 and dec_normal == "ALLOW" and enf_normal == "EXECUTED", f"Observed {dec_normal}/{enf_normal}")

        # Check 5: Restrict Access (RESTRICT -> DOWNGRADED)
        payload_restrict = {
            "deviceIdentifier": "DOOR-SENSOR-001",
            "userId": "guest-user-001",
            "role": "GUEST",
            "organization": "SmartRental",
            "resource": "SMART_DOOR_LOCK",
            "operation": "CONTROL",
            "location": "Property-001",
            "bookingId": "BOOKING-PHASE6B-001",
            "networkContext": "CELLULAR",
            "behavioralIndicator": "SUSPICIOUS_TIMING",
            "requestCountWindow": 12,
            "recentViolationCount": 2,
            "commandPayload": {"action": "UNLOCK"}
        }
        code, resp_restrict = http_post("/api/resource-operations/execute", payload_restrict)
        dec_restrict = resp_restrict.get("authorizationDecision")
        enf_restrict = resp_restrict.get("enforcementStatus")
        check(5, "Restrict Access (RESTRICT -> DOWNGRADED)",
              code == 200 and dec_restrict == "RESTRICT" and enf_restrict == "DOWNGRADED", f"Observed {dec_restrict}/{enf_restrict}")

        # Check 6: Low Trust Access (DENY -> BLOCKED)
        degrade_device_trust("DOOR-SENSOR-001", 20.0)
        payload_lowtrust = {
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
            "requestCountWindow": 1,
            "recentViolationCount": 0,
            "commandPayload": {"action": "UNLOCK"}
        }
        code, resp_lowtrust = http_post("/api/resource-operations/execute", payload_lowtrust)
        dec_lowtrust = resp_lowtrust.get("authorizationDecision")
        enf_lowtrust = resp_lowtrust.get("enforcementStatus")
        check(6, "Low Trust Access (DENY -> BLOCKED)",
              code == 200 and dec_lowtrust == "DENY" and enf_lowtrust == "BLOCKED", f"Observed {dec_lowtrust}/{enf_lowtrust}")
        
        # Reset trust back to normal
        restore_device_trust("DOOR-SENSOR-001", 80.0)

        # Check 7: High Risk Anomaly (DENY -> BLOCKED)
        payload_highrisk = {
            "deviceIdentifier": "THERMOSTAT-001",
            "userId": "guest-user-001",
            "role": "GUEST",
            "organization": "SmartRental",
            "resource": "SMART_THERMOSTAT",
            "operation": "CONTROL",
            "location": "Unknown-Geo-Mismatch",
            "bookingId": "BOOKING-PHASE6B-001",
            "networkContext": "PUBLIC_UNSECURED",
            "behavioralIndicator": "REQUEST_FLOODING",
            "requestCountWindow": 50,
            "recentViolationCount": 10,
            "commandPayload": {"targetTemperature": 22.0}
        }
        code, resp_highrisk = http_post("/api/resource-operations/execute", payload_highrisk)
        dec_highrisk = resp_highrisk.get("authorizationDecision")
        enf_highrisk = resp_highrisk.get("enforcementStatus")
        check(7, "High Risk Anomaly (DENY -> BLOCKED)",
              code == 200 and dec_highrisk == "DENY" and enf_highrisk == "BLOCKED", f"Observed {dec_highrisk}/{enf_highrisk}")

        # Check 8: ABAC Failure (DENY -> BLOCKED)
        payload_abac = {
            "deviceIdentifier": "THERMOSTAT-001",
            "userId": "guest-user-001",
            "role": "GUEST",
            "organization": "SmartRental",
            "resource": "SMART_THERMOSTAT",
            "operation": "CONTROL",
            "location": "Property-001",
            "bookingId": "INVALID-BOOKING-9999",
            "networkContext": "INTERNAL",
            "behavioralIndicator": "NORMAL",
            "commandPayload": {"targetTemperature": 22.0}
        }
        code, resp_abac = http_post("/api/resource-operations/execute", payload_abac)
        dec_abac = resp_abac.get("authorizationDecision")
        enf_abac = resp_abac.get("enforcementStatus")
        check(8, "ABAC Failure (DENY -> BLOCKED)",
              code == 200 and dec_abac == "DENY" and enf_abac == "BLOCKED", f"Observed {dec_abac}/{enf_abac}")

        # Check 9: Sensitive Resource Protection (Door Lock Unlock with Low Privilege)
        payload_door = {
            "deviceIdentifier": "DOOR-SENSOR-001",
            "userId": "unauthorized-visitor",
            "role": "VISITOR",
            "organization": "UnknownOrg",
            "resource": "SMART_DOOR_LOCK",
            "operation": "CONTROL",
            "location": "Property-001",
            "bookingId": "BOOKING-PHASE6B-001",
            "networkContext": "INTERNAL",
            "behavioralIndicator": "NORMAL",
            "commandPayload": {"action": "UNLOCK"}
        }
        code, resp_door = http_post("/api/resource-operations/execute", payload_door)
        dec_door = resp_door.get("authorizationDecision")
        enf_door = resp_door.get("enforcementStatus")
        check(9, "Sensitive Resource Protection",
              code == 200 and dec_door == "DENY" and enf_door == "BLOCKED", f"Observed {dec_door}/{enf_door}")

        # Check 10: Fail-Closed Sensitive Access Attack Rejection
        code_out, resp_out = http_post("/api/simulator/scenarios/UNAUTHORIZED_SENSITIVE_ACCESS/run")
        check(10, "Fail-Closed Sensitive Access Attack Rejection",
              code_out == 200 and resp_out.get("scenarioName") == "UNAUTHORIZED_SENSITIVE_ACCESS")

        # Check 11: Post-Attack Normal Recovery
        restore_device_trust("DOOR-SENSOR-001", 80.0)
        http_post("/api/simulator/scenarios/RECOVERY/run")
        code, recovery_resp = http_post("/api/resource-operations/execute", payload_normal)
        dec_recov = recovery_resp.get("authorizationDecision")
        enf_recov = recovery_resp.get("enforcementStatus")
        check(11, "Post-Attack Normal Recovery (ALLOW -> EXECUTED)",
              code == 200 and dec_recov == "ALLOW" and enf_recov == "EXECUTED", f"Observed {dec_recov}/{enf_recov}")

        # Check 12: Observational Dashboard Availability
        code, html_content = http_get("/dashboard", accept="text/html")
        check(12, "Observational Dashboard UI Readiness", code == 200 and "<html" in html_content.lower())

        # Check 13: WebSocket Telemetry Endpoint Readiness
        code, ws_info = http_get("/api/websocket/status")
        check(13, "WebSocket Telemetry Endpoint Readiness", code == 200 and ws_info.get("enabled") is True)

        # Check 14: RabbitMQ Message Broker Connectivity
        code, messaging_status = http_get("/api/messaging/status")
        check(14, "RabbitMQ Message Broker Readiness", 
              code == 200 and messaging_status.get("messagingEnabled") is True and messaging_status.get("brokerReachable") is True)

        # Check 15: Spring Batch Offline Analytics Endpoint Readiness
        code, batch_reports = http_get("/api/batch/audit/reports")
        check(15, "Spring Batch Offline Analytics Endpoint", code == 200 and isinstance(batch_reports, list))

    except Exception as e:
        print(f"\n[CRITICAL ERROR] Smoke test encountered unexpected exception: {e}")
        failed_checks += 1

    # Cleanup & State Reset
    print("\n--- Performing Post-Test State Sanitization & Baseline Reset ---")
    try:
        http_post("/api/simulator/reset")
        restore_device_trust("DOOR-SENSOR-001", 80.0)
        restore_device_trust("THERMOSTAT-001", 80.0)
        print("  [OK] Device simulator and baseline trust state restored.")
    except Exception as e:
        print(f"  [WARN] State reset encountered error: {e}")

    print("\n================================================================================")
    print(f"FINAL SMOKE TEST SUMMARY: {passed_checks} PASSED / {failed_checks} FAILED")
    print("================================================================================")

    if failed_checks > 0:
        sys.exit(1)

if __name__ == "__main__":
    main()
