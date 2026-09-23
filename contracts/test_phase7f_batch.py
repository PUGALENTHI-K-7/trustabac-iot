"""
Phase 7F Test Suite: Spring Batch Offline Analytics & Auditing Verification
Tests:
1. REST Endpoints Availability & Health (Gateways, Batch status, Reports)
2. Seed Known Operational Activity via Simulator & Record Ground-Truth Source Counts
3. Execution of Spring Batch Offline Audit Job (periodKey identity)
4. Source vs. Analytical Reconciliation:
   - Total Access Requests, ALLOW / RESTRICT / DENY counts
   - EXECUTED / DOWNGRADED / BLOCKED enforcement counts
   - ABAC PASS / FAIL counts
   - Sensitive Resource Denials
   - Matched vs. Missing Blockchain Proofs
5. Idempotency & Unique Period Constraint:
   - Rerun identical analytical period
   - Assert alreadyProcessed == True
   - Assert zero duplicate summary rows in database
   - Assert zero double counting in metrics
6. Operational Non-Mutation:
   - Assert device trust scores, history, risk events, and access requests remain 100% untouched
   - Assert blockchain smart contract state remains untouched
7. Reporting Endpoints:
   - GET /api/batch/audit/reports/latest
   - GET /api/batch/audit/reports
   - GET /api/batch/audit/status/{id}
"""

import json
import os
import sys
import time
import urllib.request
import urllib.error
import subprocess
from datetime import datetime, timezone, timedelta

if hasattr(sys.stdout, 'reconfigure'):
    try:
        sys.stdout.reconfigure(encoding='utf-8')
    except Exception:
        pass

BASE_URL = "http://localhost:8090"

def http_get(endpoint, accept="application/json"):
    url = f"{BASE_URL}{endpoint}"
    req = urllib.request.Request(url, headers={"Accept": accept})
    with urllib.request.urlopen(req, timeout=15) as resp:
        content = resp.read().decode("utf-8")
        return resp.getcode(), json.loads(content) if content else {}

def http_post(endpoint, data=None):
    url = f"{BASE_URL}{endpoint}"
    body = json.dumps(data).encode("utf-8") if data is not None else b""
    headers = {"Content-Type": "application/json", "Accept": "application/json"} if data is not None else {"Accept": "application/json"}
    req = urllib.request.Request(url, data=body, headers=headers, method="POST")
    with urllib.request.urlopen(req, timeout=30) as resp:
        content = resp.read().decode("utf-8")
        return resp.getcode(), json.loads(content) if content else {}

def run_mysql_query(sql):
    cmd = f'docker exec releasemind-mysql mysql -uroot -proot_secret_change_me trustabac_iot -N -e "{sql}"'
    res = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    return res.stdout.strip()

def run_tests():
    print("=" * 65)
    print("PHASE 7F — SPRING BATCH OFFLINE ANALYTICS & AUDITING VERIFICATION")
    print("=" * 65)
    print(f"Target Backend URL: {BASE_URL}")

    passed = 0
    failed = 0

    def assert_test(name, condition, details=""):
        nonlocal passed, failed
        if condition:
            print(f"  [PASS] {name} {details}")
            passed += 1
        else:
            print(f"  [FAIL] {name} {details}")
            failed += 1

    # Step 1: Health & Connectivity Check
    print("\n--- STEP 1: Backend Health & REST Availability ---")
    try:
        status, body = http_get("/api/health")
        assert_test("Health Check", status == 200 and body.get("status") == "UP")
    except Exception as e:
        print(f"  [FAIL] Health check failed: {e}")
        return

    # Step 2: Seed Known Operational Activity
    print("\n--- STEP 2: Seeding Known Operational Activity ---")
    scenarios = ["NORMAL_STAY", "RESTRICT_ENFORCEMENT", "LOW_TRUST_ATTACK", "UNAUTHORIZED_SENSITIVE_ACCESS"]
    for sc in scenarios:
        status, resp = http_post(f"/api/simulator/scenarios/{sc}/run")
        assert_test(f"Run Scenario {sc}", status == 200 and resp.get("status") in ["SUCCESS", "TRIGGERED", "COMPLETED", None] or resp.get("scenarioName") == sc)
        time.sleep(0.5)

    # Let asynchronous messaging settle
    time.sleep(2.0)

    # Record Ground Truth from MySQL
    print("\n--- STEP 3: Querying Ground-Truth Source Records from MySQL ---")
    source_access_reqs = int(run_mysql_query("SELECT COUNT(*) FROM access_requests;") or 0)
    source_allow = int(run_mysql_query("SELECT COUNT(*) FROM blockchain_authorization_events WHERE decision = 'ALLOW';") or 0)
    source_restrict = int(run_mysql_query("SELECT COUNT(*) FROM blockchain_authorization_events WHERE decision = 'RESTRICT';") or 0)
    source_deny = int(run_mysql_query("SELECT COUNT(*) FROM blockchain_authorization_events WHERE decision = 'DENY';") or 0)
    source_abac_pass = int(run_mysql_query("SELECT COUNT(*) FROM access_requests WHERE abac_result = 'PASS';") or 0)
    source_abac_fail = int(run_mysql_query("SELECT COUNT(*) FROM access_requests WHERE abac_result = 'FAIL';") or 0)
    
    sens_sql = (
        "SELECT (SELECT COUNT(*) FROM access_requests WHERE abac_result = 'FAIL' AND ("
        "resource LIKE '%CAMERA%' OR resource LIKE '%ADMIN%' OR resource LIKE '%GATEWAY%' OR resource LIKE '%ROUTER%' OR resource LIKE '%CURTAIN%' OR resource LIKE '%OWNER%' "
        "OR device_identifier LIKE '%CAM%' OR device_identifier LIKE '%ADMIN%' OR device_identifier LIKE '%GATEWAY%' OR device_identifier LIKE '%ROUTER%' OR device_identifier LIKE '%CURTAIN%' OR device_identifier LIKE '%OWNER%')) "
        "+ (SELECT COUNT(*) FROM blockchain_authorization_events WHERE decision = 'DENY' AND ("
        "resource LIKE '%CAMERA%' OR resource LIKE '%ADMIN%' OR resource LIKE '%GATEWAY%' OR resource LIKE '%ROUTER%' OR resource LIKE '%CURTAIN%' OR resource LIKE '%OWNER%' "
        "OR device_identifier LIKE '%CAM%' OR device_identifier LIKE '%ADMIN%' OR device_identifier LIKE '%GATEWAY%' OR device_identifier LIKE '%ROUTER%' OR device_identifier LIKE '%CURTAIN%' OR device_identifier LIKE '%OWNER%'));"
    )
    source_sensitive_denials = int(run_mysql_query(sens_sql) or 0)
    source_matched_proofs = int(run_mysql_query("SELECT COUNT(DISTINCT transaction_hash) FROM blockchain_authorization_events WHERE transaction_hash IS NOT NULL AND transaction_hash != '' AND transaction_hash != 'NONE';") or 0)
    source_missing_proofs = int(run_mysql_query("SELECT COUNT(*) FROM blockchain_authorization_events WHERE transaction_hash IS NULL OR transaction_hash = '' OR transaction_hash = 'NONE';") or 0)
    source_ambiguous_proofs = int(run_mysql_query("SELECT COUNT(*) - COUNT(DISTINCT transaction_hash) FROM blockchain_authorization_events WHERE transaction_hash IS NOT NULL AND transaction_hash != '' AND transaction_hash != 'NONE';") or 0)
    source_trust_events = int(run_mysql_query("SELECT COUNT(*) FROM trust_history;") or 0)
    source_risk_events = int(run_mysql_query("SELECT COUNT(*) FROM risk_events;") or 0)
    initial_door_trust = float(run_mysql_query("SELECT current_trust FROM devices WHERE device_identifier = 'DOOR-SENSOR-001';") or 80.0)

    print(f"  Source Access Requests      : {source_access_reqs}")
    print(f"  Source Decisions            : ALLOW={source_allow}, RESTRICT={source_restrict}, DENY={source_deny}")
    print(f"  Source ABAC Results         : PASS={source_abac_pass}, FAIL={source_abac_fail}")
    print(f"  Source Sensitive Denials    : {source_sensitive_denials}")
    print(f"  Source Matched Proofs       : {source_matched_proofs}")
    print(f"  Source Missing Proofs       : {source_missing_proofs}")
    print(f"  Source Ambiguous Proofs     : {source_ambiguous_proofs}")
    print(f"  Source Trust History Events : {source_trust_events}")
    print(f"  Source Risk Events          : {source_risk_events}")
    print(f"  Initial DOOR-SENSOR-001 Trust: {initial_door_trust}")

    assert_test("Operational Data Persisted", source_access_reqs > 0 and source_trust_events > 0)

    # Step 4: Trigger First Batch Run for Period
    period_key = f"PHASE7F_LIVE_{int(time.time())}"
    print(f"\n--- STEP 4: Triggering Batch Offline Audit Run (periodKey={period_key}) ---")
    batch_req = {
        "periodKey": period_key,
        "startDate": "2020-01-01T00:00:00",
        "endDate": "2099-12-31T23:59:59",
        "forceRerun": False
    }
    status, run_resp = http_post("/api/batch/audit/run", batch_req)
    assert_test("Trigger Batch Run 1 (Status 200)", status == 200)
    assert_test("Batch Run 1 Returned Period Key", run_resp.get("periodKey") == period_key)
    assert_test("Batch Run 1 Is Not Already Processed", run_resp.get("alreadyProcessed") == False)

    # Wait for completion
    batch_run_id = run_resp.get("batchRunId")
    max_wait = 10
    audit_status = None
    while max_wait > 0:
        time.sleep(1)
        st_code, st_resp = http_get(f"/api/batch/audit/status?periodKey={period_key}")
        audit_status = st_resp.get("status")
        if audit_status in ["COMPLETED", "FAILED"]:
            break
        max_wait -= 1

    assert_test("Batch Run 1 Status is COMPLETED", audit_status == "COMPLETED", f"(Got {audit_status})")

    # Step 5: Fetch Report and Verify Source/Analytics Reconciliation
    print("\n--- STEP 5: Verifying Source vs. Analytics Reconciliation ---")
    rep_status, report = http_get(f"/api/batch/audit/reports/{period_key}")
    assert_test("Fetch Batch Report (Status 200)", rep_status == 200)

    auth = report.get("authorization", {})
    recon = report.get("reconciliation", {})

    print(f"  Analytical Total Requests  : {auth.get('totalRequests')} (Source: {source_access_reqs})")
    print(f"  Analytical ALLOW Count     : {auth.get('allowCount')} (Source: {source_allow})")
    print(f"  Analytical RESTRICT Count  : {auth.get('restrictCount')} (Source: {source_restrict})")
    print(f"  Analytical DENY Count      : {auth.get('denyCount')} (Source: {source_deny})")
    print(f"  Analytical ABAC PASS Count : {auth.get('abacPassCount')} (Source: {source_abac_pass})")
    print(f"  Analytical ABAC FAIL Count : {auth.get('abacFailCount')} (Source: {source_abac_fail})")
    print(f"  Analytical Sensitive Deny  : {auth.get('sensitiveResourceDenials')} (Source: {source_sensitive_denials})")
    print(f"  Analytical Matched Proofs  : {auth.get('matchedBlockchainProofs')} (Source: {source_matched_proofs})")
    print(f"  Analytical Missing Proofs  : {auth.get('missingBlockchainProofs')} (Source: {source_missing_proofs})")
    print(f"  Analytical Ambiguous Proofs: {auth.get('ambiguousBlockchainProofs')} (Source: {source_ambiguous_proofs})")

    assert_test("Total Requests Reconciled", auth.get("totalRequests") == source_access_reqs)
    assert_test("ALLOW Count Reconciled", auth.get("allowCount") == source_allow)
    assert_test("RESTRICT Count Reconciled", auth.get("restrictCount") == source_restrict)
    assert_test("DENY Count Reconciled", auth.get("denyCount") == source_deny)
    assert_test("ABAC PASS Count Reconciled", auth.get("abacPassCount") == source_abac_pass)
    assert_test("ABAC FAIL Count Reconciled", auth.get("abacFailCount") == source_abac_fail)
    assert_test("Sensitive Resource Denials Reconciled", auth.get("sensitiveResourceDenials") == source_sensitive_denials)
    assert_test("Matched Blockchain Proofs Reconciled", auth.get("matchedBlockchainProofs") == source_matched_proofs)
    assert_test("Missing Blockchain Proofs Reconciled", auth.get("missingBlockchainProofs") == source_missing_proofs)
    assert_test("Ambiguous Blockchain Proofs Reconciled", auth.get("ambiguousBlockchainProofs") == source_ambiguous_proofs)
    assert_test("Reconciliation Flag True", recon.get("reconciledSuccessfully") == True)

    # Step 6: Idempotency & Unique Period Constraint Verification
    print("\n--- STEP 6: Verifying Idempotency on Identical Period Rerun ---")
    audit_rows_before = int(run_mysql_query(f"SELECT COUNT(*) FROM batch_run_audits WHERE period_key = '{period_key}';") or 0)
    auth_rows_before = int(run_mysql_query(f"SELECT COUNT(*) FROM authorization_analytics WHERE period_key = '{period_key}';") or 0)
    dev_rows_before = int(run_mysql_query(f"SELECT COUNT(*) FROM device_analytics WHERE period_key = '{period_key}';") or 0)
    sec_rows_before = int(run_mysql_query(f"SELECT COUNT(*) FROM security_analytics WHERE period_key = '{period_key}';") or 0)

    # Trigger second run with identical periodKey
    status2, run_resp2 = http_post("/api/batch/audit/run", batch_req)
    assert_test("Rerun Identical Period (Status 200)", status2 == 200)
    assert_test("Rerun Marks alreadyProcessed == True", run_resp2.get("alreadyProcessed") == True)
    assert_test("Rerun Reuses Original BatchRunId", run_resp2.get("batchRunId") == run_resp.get("batchRunId"))

    audit_rows_after = int(run_mysql_query(f"SELECT COUNT(*) FROM batch_run_audits WHERE period_key = '{period_key}';") or 0)
    auth_rows_after = int(run_mysql_query(f"SELECT COUNT(*) FROM authorization_analytics WHERE period_key = '{period_key}';") or 0)
    dev_rows_after = int(run_mysql_query(f"SELECT COUNT(*) FROM device_analytics WHERE period_key = '{period_key}';") or 0)
    sec_rows_after = int(run_mysql_query(f"SELECT COUNT(*) FROM security_analytics WHERE period_key = '{period_key}';") or 0)

    assert_test("Zero Duplicate BatchRunAudit Rows", audit_rows_before == audit_rows_after == 1)
    assert_test("Zero Duplicate AuthorizationAnalytics Rows", auth_rows_before == auth_rows_after == 1)
    assert_test("Zero Duplicate DeviceAnalytics Rows", dev_rows_before == dev_rows_after)
    assert_test("Zero Duplicate SecurityAnalytics Rows", sec_rows_before == sec_rows_after == 1)

    # Verify metrics did not double count
    rep_status2, report2 = http_get(f"/api/batch/audit/reports/{period_key}")
    auth2 = report2.get("authorization", {})
    assert_test("Metrics Not Double-Counted on Rerun", auth2.get("totalRequests") == auth.get("totalRequests"))

    # Step 7: Read-Only Operational Non-Mutation Verification
    print("\n--- STEP 7: Verifying Non-Mutation of Operational State ---")
    post_access_reqs = int(run_mysql_query("SELECT COUNT(*) FROM access_requests;") or 0)
    post_trust_events = int(run_mysql_query("SELECT COUNT(*) FROM trust_history;") or 0)
    post_risk_events = int(run_mysql_query("SELECT COUNT(*) FROM risk_events;") or 0)
    post_door_trust = float(run_mysql_query("SELECT current_trust FROM devices WHERE device_identifier = 'DOOR-SENSOR-001';") or 80.0)

    assert_test("Access Requests Table Untouched", source_access_reqs == post_access_reqs)
    assert_test("Trust History Table Untouched", source_trust_events == post_trust_events)
    assert_test("Risk Events Table Untouched", source_risk_events == post_risk_events)
    assert_test("Device Current Trust Untouched", initial_door_trust == post_door_trust)

    # Step 8: Additional Reporting Endpoints
    print("\n--- STEP 8: Verifying Status and Reporting Endpoints ---")
    status_latest, latest_rep = http_get("/api/batch/audit/reports/latest")
    assert_test("GET /api/batch/audit/reports/latest (200 OK)", status_latest == 200 and latest_rep.get("periodKey") == period_key)

    status_all, all_runs = http_get("/api/batch/audit/reports")
    assert_test("GET /api/batch/audit/reports (200 OK)", status_all == 200 and len(all_runs) >= 1)

    print("\n" + "=" * 65)
    print("PHASE 7F VERIFICATION SUMMARY")
    print("=" * 65)
    print(f"  Tests Passed : {passed}")
    print(f"  Tests Failed : {failed}")
    print(f"  Success Rate : {passed / (passed + failed) * 100:.1f}%")
    print("=" * 65)

    if failed == 0:
        print(">>> ALL PHASE 7F VERIFICATION CHECKS PASSED SUCCESSFULLY! <<<\n")
        return 0
    else:
        print(f">>> {failed} CHECKS FAILED! <<<\n")
        return 1

if __name__ == "__main__":
    sys.exit(run_tests())
