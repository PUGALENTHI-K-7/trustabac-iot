import json
import solcx
from web3 import Web3
from eth_tester import EthereumTester, PyEVMBackend

def run_solidity_tests():
    print("==================================================")
    print("RUNNING SOLIDITY SMART CONTRACT TESTS (ETH-TESTER)")
    print("==================================================")

    # 1. Compile contract
    solcx.install_solc('0.8.20')
    compiled = solcx.compile_files(
        ['contracts/AdaptiveAccessControl.sol'],
        output_values=['abi', 'bin'],
        solc_version='0.8.20'
    )
    contract_id = 'contracts/AdaptiveAccessControl.sol:AdaptiveAccessControl'
    abi = compiled[contract_id]['abi']
    bytecode = compiled[contract_id]['bin']

    # 2. Setup Ethereum tester provider
    eth_tester = EthereumTester(backend=PyEVMBackend())
    w3 = Web3(Web3.EthereumTesterProvider(eth_tester))
    accounts = w3.eth.accounts
    owner = accounts[0]
    attacker = accounts[1]

    print(f"Connected to Py-EVM Tester. Owner: {owner}, Accounts: {len(accounts)}")

    # 3. Deploy contract
    ContractFactory = w3.eth.contract(abi=abi, bytecode=bytecode)
    tx_hash = ContractFactory.constructor().transact({'from': owner})
    tx_receipt = w3.eth.wait_for_transaction_receipt(tx_hash)
    contract = w3.eth.contract(address=tx_receipt.contractAddress, abi=abi)
    print(f"AdaptiveAccessControl deployed at {contract.address}")

    tests_run = 0
    tests_passed = 0

    def assert_eq(actual, expected, test_name):
        nonlocal tests_run, tests_passed
        tests_run += 1
        if actual == expected:
            tests_passed += 1
            print(f"  [PASS] Test {tests_run}: {test_name} (Got {actual})")
        else:
            print(f"  [FAIL] Test {tests_run}: {test_name} (Expected {expected}, Got {actual})")
            raise AssertionError(f"{test_name} failed: Expected {expected}, Got {actual}")

    # Test 1: Initial owner and thresholds
    assert_eq(contract.functions.owner().call(), owner, "Initial owner is deployer")
    assert_eq(contract.functions.trustHigh().call(), 70, "Initial trustHigh is 70")
    assert_eq(contract.functions.trustMedium().call(), 30, "Initial trustMedium is 30")
    assert_eq(contract.functions.riskLow().call(), 30, "Initial riskLow is 30")
    assert_eq(contract.functions.riskMedium().call(), 70, "Initial riskMedium is 70")

    # Test 2: ABAC False -> DENY (0), reasonCode 1
    dec, reason = contract.functions.calculateDecision(False, True, 2, 4, 85, 10).call()
    assert_eq((dec, reason), (0, 1), "ABAC False -> DENY (reason 1)")

    # Test 3: Booking Inactive -> DENY (0), reasonCode 2
    dec, reason = contract.functions.calculateDecision(True, False, 2, 4, 85, 10).call()
    assert_eq((dec, reason), (0, 2), "Booking Inactive -> DENY (reason 2)")

    # Test 4: Low Trust (< 30) -> DENY (0), reasonCode 3
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 25, 10).call()
    assert_eq((dec, reason), (0, 3), "Low Trust (25) -> DENY (reason 3)")

    # Test 5: High Risk (> 70) -> DENY (0), reasonCode 4
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 85, 75).call()
    assert_eq((dec, reason), (0, 4), "High Risk (75) -> DENY (reason 4)")

    # Test 6: High Trust + Low Risk (80, 20) -> ALLOW (2), reasonCode 0
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 80, 20).call()
    assert_eq((dec, reason), (2, 0), "High Trust (80) + Low Risk (20) -> ALLOW (reason 0)")

    # Test 7: High Trust + Medium Risk (80, 50) -> RESTRICT (1), reasonCode 5
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 80, 50).call()
    assert_eq((dec, reason), (1, 5), "High Trust (80) + Medium Risk (50) -> RESTRICT (reason 5)")

    # Test 8: Medium Trust + Low Risk (50, 20) -> RESTRICT (1), reasonCode 5
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 50, 20).call()
    assert_eq((dec, reason), (1, 5), "Medium Trust (50) + Low Risk (20) -> RESTRICT (reason 5)")

    # Test 9: Medium Trust + Medium Risk (50, 50) -> RESTRICT (1), reasonCode 5
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 50, 50).call()
    assert_eq((dec, reason), (1, 5), "Medium Trust (50) + Medium Risk (50) -> RESTRICT (reason 5)")

    # Boundary Tests:
    # Exact Boundary: Trust = 30, Risk = 30 -> RESTRICT
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 30, 30).call()
    assert_eq((dec, reason), (1, 5), "Boundary Trust=30, Risk=30 -> RESTRICT")

    # Exact Boundary: Trust = 29, Risk = 30 -> DENY (reason 3)
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 29, 30).call()
    assert_eq((dec, reason), (0, 3), "Boundary Trust=29, Risk=30 -> DENY (trust too low)")

    # Exact Boundary: Trust = 70, Risk = 30 -> ALLOW
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 70, 30).call()
    assert_eq((dec, reason), (2, 0), "Boundary Trust=70, Risk=30 -> ALLOW")

    # Exact Boundary: Trust = 70, Risk = 31 -> RESTRICT
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 70, 31).call()
    assert_eq((dec, reason), (1, 5), "Boundary Trust=70, Risk=31 -> RESTRICT")

    # Exact Boundary: Trust = 70, Risk = 70 -> RESTRICT
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 70, 70).call()
    assert_eq((dec, reason), (1, 5), "Boundary Trust=70, Risk=70 -> RESTRICT")

    # Exact Boundary: Trust = 70, Risk = 71 -> DENY (reason 4)
    dec, reason = contract.functions.calculateDecision(True, True, 2, 4, 70, 71).call()
    assert_eq((dec, reason), (0, 4), "Boundary Trust=70, Risk=71 -> DENY (risk too high)")

    # Test State-Changing evaluateAccess Transaction & Event Emission
    req_ref = Web3.keccak(text="REQ-001")
    dev_hash = Web3.keccak(text="DEV-DOOR-001")
    tx = contract.functions.evaluateAccess(
        req_ref, dev_hash, True, True, 2, 4, 85, 15
    ).transact({'from': owner})
    receipt = w3.eth.wait_for_transaction_receipt(tx)
    assert_eq(receipt.status, 1, "evaluateAccess transaction succeeds with status 1")

    # Parse AuthorizationEvaluated Event
    events = contract.events.AuthorizationEvaluated().process_receipt(receipt)
    assert_eq(len(events), 1, "AuthorizationEvaluated event emitted exactly once")
    event_args = events[0]['args']
    assert_eq(event_args['decision'], 2, "Event decision matches ALLOW (2)")
    assert_eq(event_args['trustScore'], 85, "Event trustScore matches 85")
    assert_eq(event_args['riskScore'], 15, "Event riskScore matches 15")
    assert_eq(event_args['resourceSensitivity'], 2, "Event resourceSensitivity matches 2")
    assert_eq(event_args['operation'], 4, "Event operation matches 4")

    # Test Owner Setters
    tx_t = contract.functions.setTrustThresholds(80, 40).transact({'from': owner})
    receipt_t = w3.eth.wait_for_transaction_receipt(tx_t)
    assert_eq(receipt_t.status, 1, "setTrustThresholds succeeds")
    assert_eq(contract.functions.trustHigh().call(), 80, "trustHigh updated to 80")
    assert_eq(contract.functions.trustMedium().call(), 40, "trustMedium updated to 40")

    tx_r = contract.functions.setRiskThresholds(20, 60).transact({'from': owner})
    receipt_r = w3.eth.wait_for_transaction_receipt(tx_r)
    assert_eq(receipt_r.status, 1, "setRiskThresholds succeeds")
    assert_eq(contract.functions.riskLow().call(), 20, "riskLow updated to 20")
    assert_eq(contract.functions.riskMedium().call(), 60, "riskMedium updated to 60")

    # Test Non-Owner Cannot Modify Thresholds (Must Revert)
    reverted = False
    try:
        contract.functions.setTrustThresholds(90, 50).transact({'from': attacker})
    except Exception:
        reverted = True
    assert_eq(reverted, True, "Non-owner call to setTrustThresholds reverts")

    print(f"\n==================================================")
    print(f"ALL {tests_passed}/{tests_run} SOLIDITY SMART CONTRACT TESTS PASSED!")
    print(f"==================================================")

if __name__ == '__main__':
    run_solidity_tests()
