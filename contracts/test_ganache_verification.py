"""
Phase 6 â€” Real Ganache Blockchain Verification
TrustABAC-IoT Smart Contract: AdaptiveAccessControl.sol

This script deploys AdaptiveAccessControl.sol to a REAL Ganache blockchain
running on http://127.0.0.1:8545 and executes the complete verification suite.

Usage:
    python contracts/test_ganache_verification.py

Requirements:
    - Ganache running on http://127.0.0.1:8545 (Docker or standalone)
    - pip packages: web3, py-solc-x
"""

import json
import sys
import time
import solcx
from web3 import Web3


GANACHE_URL = "http://127.0.0.1:8545"
SOL_FILE = "contracts/AdaptiveAccessControl.sol"
ABI_FILE = "contracts/AdaptiveAccessControl.abi"
BIN_FILE = "contracts/AdaptiveAccessControl.bin"
SOLC_VERSION = "0.8.20"

# Ganache well-known test accounts (deterministic mnemonic default)
# We'll use whatever Ganache provides via eth_accounts
OWNER_INDEX = 0
ATTACKER_INDEX = 1


def banner(msg):
    print("\n" + "=" * 60)
    print(msg)
    print("=" * 60)


def connect_to_ganache(url: str, retries: int = 5, delay: float = 2.0) -> Web3:
    """Connect to Ganache RPC endpoint with retries."""
    for attempt in range(1, retries + 1):
        w3 = Web3(Web3.HTTPProvider(url))
        if w3.is_connected():
            print(f"  [OK] Connected to Ganache at {url} (attempt {attempt})")
            return w3
        print(f"  [..] Attempt {attempt}/{retries}: Ganache not ready, retrying in {delay}s...")
        time.sleep(delay)
    raise ConnectionError(f"Could not connect to Ganache at {url} after {retries} attempts.")


def compile_contract(sol_file: str, solc_version: str):
    """Compile Solidity contract using pre-built ABI/BIN if available, else compile."""
    try:
        # Prefer pre-compiled ABI and BIN (from Phase 5)
        with open(ABI_FILE, "r") as f:
            abi = json.load(f)
        with open(BIN_FILE, "r") as f:
            bytecode = f.read().strip()
        print(f"  [OK] Loaded pre-compiled ABI ({ABI_FILE}) and BIN ({BIN_FILE})")
        return abi, bytecode
    except (FileNotFoundError, json.JSONDecodeError):
        print(f"  [..] Pre-compiled files not found, compiling {sol_file} with solc {solc_version}...")
        solcx.install_solc(solc_version)
        compiled = solcx.compile_files(
            [sol_file],
            output_values=["abi", "bin"],
            solc_version=solc_version
        )
        contract_id = f"{sol_file}:AdaptiveAccessControl"
        return compiled[contract_id]["abi"], compiled[contract_id]["bin"]


def deploy_contract(w3: Web3, abi, bytecode: str, owner: str) -> object:
    """Deploy AdaptiveAccessControl to Ganache and return instance."""
    ContractFactory = w3.eth.contract(abi=abi, bytecode=bytecode)
    tx_hash = ContractFactory.constructor().transact({"from": owner, "gas": 3_000_000})
    receipt = w3.eth.wait_for_transaction_receipt(tx_hash)
    if receipt.status != 1:
        raise RuntimeError(f"Deployment transaction failed: {receipt}")
    contract = w3.eth.contract(address=receipt.contractAddress, abi=abi)
    print(f"  [OK] AdaptiveAccessControl deployed at {contract.address}")
    print(f"       Deployment tx hash : {receipt.transactionHash.hex()}")
    print(f"       Block number        : {receipt.blockNumber}")
    print(f"       Gas used            : {receipt.gasUsed}")
    return contract


def run_ganache_tests(w3: Web3, contract, owner: str, attacker: str):
    """Execute the complete verification test suite against real Ganache."""
    tests_run = 0
    tests_passed = 0
    failures = []

    def assert_eq(actual, expected, test_name):
        nonlocal tests_run, tests_passed
        tests_run += 1
        if actual == expected:
            tests_passed += 1
            print(f"  [PASS] Test {tests_run:02d}: {test_name} (Got {actual})")
        else:
            failures.append(test_name)
            print(f"  [FAIL] Test {tests_run:02d}: {test_name} (Expected {expected}, Got {actual})")

    banner("SECTION 1 â€” INITIAL STATE VERIFICATION (View Calls)")

    # Initial owner and thresholds
    assert_eq(contract.functions.owner().call(), owner, "Initial owner is deployer")
    assert_eq(contract.functions.trustHigh().call(), 70, "Initial trustHigh is 70")
    assert_eq(contract.functions.trustMedium().call(), 30, "Initial trustMedium is 30")
    assert_eq(contract.functions.riskLow().call(), 30, "Initial riskLow is 30")
    assert_eq(contract.functions.riskMedium().call(), 70, "Initial riskMedium is 70")

    banner("SECTION 2 â€” DENY CASES (calculateDecision View Calls)")

    # ABAC False â†’ DENY (0), reasonCode 1
    dec, reason = contract.functions.calculateDecision(False, True, 2, 4, 85, 10).call()
    assert_eq((dec, reason), (0, 1), "ABAC False â†’ DENY (reasonCode 1)")

    # Booking Inactive â†’ DENY (0), reasonCode 2
    dec, reason = contract.functions.calculateDecision(True, False, 2, 4, 85, 10).call()
    assert_eq((dec, reason), (0, 2), "Booking Inactive â†’ DENY (reasonCode 2)")

    # Low Trust (<30) â†’ DENY (0), reasonCode 3
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 25, 10).call()
    assert_eq((dec, reason), (0, 3), "Low Trust (25) â†’ DENY (reasonCode 3)")

    # High Risk (>70) â†’ DENY (0), reasonCode 4
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 85, 75).call()
    assert_eq((dec, reason), (0, 4), "High Risk (75) â†’ DENY (reasonCode 4)")

    banner("SECTION 3 â€” ALLOW / RESTRICT CASES (View Calls)")

    # High Trust + Low Risk â†’ ALLOW (2), reasonCode 0
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 80, 20).call()
    assert_eq((dec, reason), (2, 0), "High Trust (80) + Low Risk (20) â†’ ALLOW")

    # High Trust + Medium Risk â†’ RESTRICT (1), reasonCode 5
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 80, 50).call()
    assert_eq((dec, reason), (1, 5), "High Trust (80) + Medium Risk (50) â†’ RESTRICT")

    # Medium Trust + Low Risk â†’ RESTRICT (1), reasonCode 5
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 50, 20).call()
    assert_eq((dec, reason), (1, 5), "Medium Trust (50) + Low Risk (20) â†’ RESTRICT")

    # Medium Trust + Medium Risk â†’ RESTRICT (1), reasonCode 5
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 50, 50).call()
    assert_eq((dec, reason), (1, 5), "Medium Trust (50) + Medium Risk (50) â†’ RESTRICT")

    banner("SECTION 4 â€” BOUNDARY CONDITIONS (View Calls)")

    # Trust = 30, Risk = 30 â†’ RESTRICT
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 30, 30).call()
    assert_eq((dec, reason), (1, 5), "Boundary Trust=30, Risk=30 â†’ RESTRICT")

    # Trust = 29, Risk = 30 â†’ DENY (trust too low)
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 29, 30).call()
    assert_eq((dec, reason), (0, 3), "Boundary Trust=29, Risk=30 â†’ DENY (trust)")

    # Trust = 70, Risk = 30 â†’ ALLOW
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 70, 30).call()
    assert_eq((dec, reason), (2, 0), "Boundary Trust=70, Risk=30 â†’ ALLOW")

    # Trust = 70, Risk = 31 â†’ RESTRICT
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 70, 31).call()
    assert_eq((dec, reason), (1, 5), "Boundary Trust=70, Risk=31 â†’ RESTRICT")

    # Trust = 70, Risk = 70 â†’ RESTRICT
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 70, 70).call()
    assert_eq((dec, reason), (1, 5), "Boundary Trust=70, Risk=70 â†’ RESTRICT")

    # Trust = 70, Risk = 71 â†’ DENY (risk too high)
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 70, 71).call()
    assert_eq((dec, reason), (0, 4), "Boundary Trust=70, Risk=71 â†’ DENY (risk)")

    banner("SECTION 5 â€” STATE-CHANGING TRANSACTION (evaluateAccess + Event)")

    req_ref = Web3.keccak(text="GANACHE-REQ-001")
    dev_hash = Web3.keccak(text="GANACHE-DEV-DOOR-001")
    tx = contract.functions.evaluateAccess(
        req_ref, dev_hash, True, True, 2, 4, 85, 15
    ).transact({"from": owner, "gas": 500_000})
    receipt = w3.eth.wait_for_transaction_receipt(tx)
    print(f"  [TX]  evaluateAccess tx hash  : {receipt.transactionHash.hex()}")
    print(f"        Block number             : {receipt.blockNumber}")
    print(f"        Gas used                 : {receipt.gasUsed}")
    assert_eq(receipt.status, 1, "evaluateAccess transaction status = 1 (success)")

    # Parse AuthorizationEvaluated event
    events = contract.events.AuthorizationEvaluated().process_receipt(receipt)
    assert_eq(len(events), 1, "AuthorizationEvaluated event emitted exactly once")
    ev = events[0]["args"]
    assert_eq(ev["decision"], 2, "Event.decision = ALLOW (2)")
    assert_eq(ev["trustScore"], 85, "Event.trustScore = 85")
    assert_eq(ev["riskScore"], 15, "Event.riskScore = 15")
    assert_eq(ev["resourceSensitivity"], 2, "Event.resourceSensitivity = 2")
    assert_eq(ev["operation"], 4, "Event.operation = 4")

    banner("SECTION 6 â€” OWNER THRESHOLD SETTERS (State Transactions)")

    tx_t = contract.functions.setTrustThresholds(80, 40).transact({"from": owner, "gas": 100_000})
    receipt_t = w3.eth.wait_for_transaction_receipt(tx_t)
    assert_eq(receipt_t.status, 1, "setTrustThresholds transaction succeeds")
    assert_eq(contract.functions.trustHigh().call(), 80, "trustHigh updated to 80")
    assert_eq(contract.functions.trustMedium().call(), 40, "trustMedium updated to 40")

    tx_r = contract.functions.setRiskThresholds(20, 60).transact({"from": owner, "gas": 100_000})
    receipt_r = w3.eth.wait_for_transaction_receipt(tx_r)
    assert_eq(receipt_r.status, 1, "setRiskThresholds transaction succeeds")
    assert_eq(contract.functions.riskLow().call(), 20, "riskLow updated to 20")
    assert_eq(contract.functions.riskMedium().call(), 60, "riskMedium updated to 60")

    banner("SECTION 7 â€” ACCESS CONTROL (Non-Owner Revert Test)")

    # Ganache returns a failed receipt (status=0) on revert rather than raising a Python exception.
    # We detect the revert by checking receipt.status == 0 OR by exception (eth-tester raises).
    reverted = False
    try:
        tx_bad = contract.functions.setTrustThresholds(90, 50).transact({"from": attacker, "gas": 100_000})
        receipt_bad = w3.eth.wait_for_transaction_receipt(tx_bad)
        if receipt_bad.status == 0:
            reverted = True
            print(f"  [OK]  Non-owner call reverted on-chain (receipt.status=0) — Ganache confirmed revert")
            print(f"        Failed tx hash: {receipt_bad.transactionHash.hex()}")
        else:
            print(f"  [WARN] Non-owner call unexpectedly succeeded (status=1)!")
    except Exception as e:
        reverted = True
        print(f"  [OK]  Non-owner call raised exception: {type(e).__name__}: {e}")
    assert_eq(reverted, True, "Non-owner setTrustThresholds correctly reverts (status=0 or exception)")

    banner("SECTION 8 â€” SECOND DENY TRANSACTION ON UPDATED THRESHOLDS")

    # With trustHigh=80 now, a trust of 75 should â†’ RESTRICT (previously ALLOW)
    req_ref2 = Web3.keccak(text="GANACHE-REQ-002")
    dev_hash2 = Web3.keccak(text="GANACHE-DEV-LOCK-002")
    tx2 = contract.functions.evaluateAccess(
        req_ref2, dev_hash2, True, True, 3, 2, 75, 15
    ).transact({"from": owner, "gas": 500_000})
    receipt2 = w3.eth.wait_for_transaction_receipt(tx2)
    print(f"  [TX]  Second evaluateAccess tx : {receipt2.transactionHash.hex()}")
    print(f"        Block number              : {receipt2.blockNumber}")
    assert_eq(receipt2.status, 1, "Second evaluateAccess transaction status = 1")
    events2 = contract.events.AuthorizationEvaluated().process_receipt(receipt2)
    assert_eq(len(events2), 1, "Second AuthorizationEvaluated event emitted")
    ev2 = events2[0]["args"]
    # With updated thresholds (trustHigh=80, trust=75): 75 < 80 -> RESTRICT
    assert_eq(ev2["decision"], 1, "Second event decision = RESTRICT (1) with updated threshold")

    # Restore default contract thresholds for system operations
    contract.functions.setTrustThresholds(70, 30).transact({"from": owner})
    contract.functions.setRiskThresholds(30, 70).transact({"from": owner})

    return tests_run, tests_passed, failures


def main():
    banner("PHASE 6 â€” REAL GANACHE BLOCKCHAIN VERIFICATION")
    print("TrustABAC-IoT: AdaptiveAccessControl.sol")
    print(f"Ganache URL: {GANACHE_URL}")

    # 1. Connect
    banner("STEP 1 â€” CONNECT TO GANACHE")
    try:
        w3 = connect_to_ganache(GANACHE_URL)
    except ConnectionError as e:
        print(f"\n[FATAL] {e}")
        print("Ensure Ganache is running: docker run -d -p 8545:8545 trufflesuite/ganache --chain.chainId 1337")
        sys.exit(1)

    accounts = w3.eth.accounts
    print(f"  Chain ID       : {w3.eth.chain_id}")
    print(f"  Block number   : {w3.eth.block_number}")
    print(f"  Accounts       : {len(accounts)}")
    owner = accounts[OWNER_INDEX]
    attacker = accounts[ATTACKER_INDEX]
    print(f"  Owner          : {owner}")
    print(f"  Attacker       : {attacker}")
    balance = w3.eth.get_balance(owner)
    print(f"  Owner balance  : {w3.from_wei(balance, 'ether')} ETH")

    # 2. Compile
    banner("STEP 2 â€” COMPILE / LOAD CONTRACT")
    abi, bytecode = compile_contract(SOL_FILE, SOLC_VERSION)

    # 3. Deploy
    banner("STEP 3 â€” DEPLOY CONTRACT TO GANACHE")
    contract = deploy_contract(w3, abi, bytecode, owner)

    # 4. Run full test suite
    banner("STEP 4 â€” EXECUTING FULL VERIFICATION TEST SUITE")
    tests_run, tests_passed, failures = run_ganache_tests(w3, contract, owner, attacker)

    # 5. Final summary
    banner("PHASE 6 REAL GANACHE VERIFICATION SUMMARY")
    print(f"  Tests Run     : {tests_run}")
    print(f"  Tests Passed  : {tests_passed}")
    print(f"  Tests Failed  : {tests_run - tests_passed}")
    print(f"  Contract Addr : {contract.address}")
    print(f"  Chain ID      : {w3.eth.chain_id}")
    print(f"  Final Block   : {w3.eth.block_number}")

    if failures:
        print(f"\n  [FAIL] FAILED TESTS:")
        for f in failures:
            print(f"    - {f}")
        print(f"\n[RESULT] PHASE 6 VERIFICATION FAILED â€” {len(failures)} test(s) failed.")
        sys.exit(1)
    else:
        print(f"\n  [RESULT] ALL {tests_passed}/{tests_run} TESTS PASSED ON REAL GANACHE BLOCKCHAIN!")
        print(f"  PHASE 6 â€” VERIFIED COMPLETE âœ“")


if __name__ == "__main__":
    main()
