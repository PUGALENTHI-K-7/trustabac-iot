"""
Phase 6B Setup Script
Deploys AdaptiveAccessControl to the running Ganache and prints all
environment variables needed to start the Spring Boot application.
"""
import json
import sys
from web3 import Web3

GANACHE_URL = "http://127.0.0.1:8545"
ABI_FILE = "contracts/AdaptiveAccessControl.abi"
BIN_FILE = "contracts/AdaptiveAccessControl.bin"

# Ganache private keys from docker logs (deterministic per session)
# We query them live from the Ganache node instead
# The private key for account[0] is embedded in the Ganache logs
# but we need to use the one printed in docker logs.
# For Ganache, we use eth_accounts and send from account[0] directly (unlocked).

def main():
    w3 = Web3(Web3.HTTPProvider(GANACHE_URL))
    if not w3.is_connected():
        print("[FATAL] Cannot connect to Ganache at", GANACHE_URL)
        sys.exit(1)

    accounts = w3.eth.accounts
    owner = accounts[0]

    print("=== GANACHE RPC VERIFICATION ===")
    print(f"  client_version : {w3.client_version}")
    print(f"  chain_id       : {w3.eth.chain_id}")
    print(f"  net_version    : {w3.net.version}")
    print(f"  block_number   : {w3.eth.block_number}")
    print(f"  eth_accounts[0]: {accounts[0]}")
    print(f"  eth_accounts[1]: {accounts[1]}")
    print(f"  balance[0]     : {w3.from_wei(w3.eth.get_balance(accounts[0]), 'ether')} ETH")

    print("\n=== LOADING PRE-COMPILED CONTRACT ===")
    with open(ABI_FILE) as f:
        abi = json.load(f)
    with open(BIN_FILE) as f:
        bytecode = f.read().strip()
    print(f"  ABI loaded from  : {ABI_FILE}")
    print(f"  BIN loaded from  : {BIN_FILE}")

    print("\n=== DEPLOYING CONTRACT TO GANACHE ===")
    factory = w3.eth.contract(abi=abi, bytecode=bytecode)
    tx_hash = factory.constructor().transact({"from": owner, "gas": 3_000_000})
    receipt = w3.eth.wait_for_transaction_receipt(tx_hash)

    contract_address = receipt.contractAddress
    deploy_block = receipt.blockNumber
    deploy_tx = receipt.transactionHash.hex()
    deploy_gas = receipt.gasUsed
    deploy_status = receipt.status

    print(f"  Contract Address  : {contract_address}")
    print(f"  Deployment TxHash : {deploy_tx}")
    print(f"  Deployment Block  : {deploy_block}")
    print(f"  Receipt Status    : {deploy_status} ({'SUCCESS' if deploy_status == 1 else 'FAILED'})")
    print(f"  Gas Used          : {deploy_gas}")

    # Verify initial thresholds
    contract = w3.eth.contract(address=contract_address, abi=abi)
    t_high = contract.functions.trustHigh().call()
    t_med  = contract.functions.trustMedium().call()
    r_low  = contract.functions.riskLow().call()
    r_med  = contract.functions.riskMedium().call()
    owner_addr = contract.functions.owner().call()

    print(f"\n=== CONTRACT INITIAL STATE ===")
    print(f"  owner()       : {owner_addr}")
    print(f"  trustHigh()   : {t_high}")
    print(f"  trustMedium() : {t_med}")
    print(f"  riskLow()     : {r_low}")
    print(f"  riskMedium()  : {r_med}")

    # The private key for account[0] from Ganache (from docker logs, fresh session)
    # Ganache with default mnemonic gives the same keys each run if mnemonic is same,
    # Private key for account[0] can be set via BLOCKCHAIN_PRIVATE_KEY environment variable.

    print(f"\n=== SPRING BOOT ENV VARS ===")
    print(f"  BLOCKCHAIN_RPC_URL=http://127.0.0.1:8545")
    print(f"  BLOCKCHAIN_CHAIN_ID=1337")
    print(f"  BLOCKCHAIN_CONTRACT_ADDRESS={contract_address}")
    print(f"  DB_HOST=localhost")
    print(f"  DB_PORT=3307")
    print(f"  DB_NAME=trustabac_iot")
    print(f"  DB_USERNAME=root")
    print(f"  DB_PASSWORD=<configured_database_password>")
    print(f"\n  NOTE: Set BLOCKCHAIN_PRIVATE_KEY in environment or .env file.")
    print(f"  Current block_number after deploy: {w3.eth.block_number}")

    # Write to a properties file for use by the verification script
    with open("contracts/phase6b_state.json", "w") as f:
        json.dump({
            "ganache_url": GANACHE_URL,
            "chain_id": w3.eth.chain_id,
            "net_version": w3.net.version,
            "client_version": w3.client_version,
            "block_number_after_deploy": w3.eth.block_number,
            "accounts": list(accounts[:5]),
            "contract_address": contract_address,
            "deploy_tx_hash": deploy_tx,
            "deploy_block": deploy_block,
            "deploy_gas": deploy_gas,
            "deploy_status": deploy_status,
            "initial_trust_high": t_high,
            "initial_trust_medium": t_med,
            "initial_risk_low": r_low,
            "initial_risk_medium": r_med,
            "owner": owner_addr,
        }, f, indent=2)
    print(f"\n  State saved to: contracts/phase6b_state.json")

if __name__ == "__main__":
    main()
