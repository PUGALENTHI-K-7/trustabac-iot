"""
TrustABAC-IoT — Contract Deployment Assurance Script
Checks whether the contract address configured in .env has deployed bytecode on Ganache.
If not deployed (e.g., fresh Ganache chain), deploys AdaptiveAccessControl from accounts[0],
saves phase6b_state.json, and updates BLOCKCHAIN_CONTRACT_ADDRESS in .env.
"""
import json
import os
import re
import sys
from web3 import Web3

GANACHE_URL = os.environ.get("BLOCKCHAIN_RPC_URL", "http://127.0.0.1:8545")
ABI_FILE = "contracts/AdaptiveAccessControl.abi"
BIN_FILE = "contracts/AdaptiveAccessControl.bin"
ENV_FILE = ".env"
STATE_FILE = "contracts/phase6b_state.json"

def read_env_contract_address():
    if not os.path.isfile(ENV_FILE):
        return None
    with open(ENV_FILE, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if line.startswith("#") or not line:
                continue
            if "=" in line:
                k, v = line.split("=", 1)
                if k.strip() == "BLOCKCHAIN_CONTRACT_ADDRESS":
                    return v.strip()
    return None

def update_env_contract_address(new_address):
    if not os.path.isfile(ENV_FILE):
        return
    with open(ENV_FILE, "r", encoding="utf-8") as f:
        content = f.read()
    
    if "BLOCKCHAIN_CONTRACT_ADDRESS=" in content:
        updated = re.sub(
            r"BLOCKCHAIN_CONTRACT_ADDRESS=[^\r\n]*",
            f"BLOCKCHAIN_CONTRACT_ADDRESS={new_address}",
            content
        )
    else:
        updated = content + f"\nBLOCKCHAIN_CONTRACT_ADDRESS={new_address}\n"
        
    with open(ENV_FILE, "w", encoding="utf-8") as f:
        f.write(updated)

def main():
    w3 = Web3(Web3.HTTPProvider(GANACHE_URL))
    if not w3.is_connected():
        print(f"[ERROR] Cannot connect to Ganache at {GANACHE_URL}")
        sys.exit(1)

    accounts = w3.eth.accounts
    if not accounts:
        print("[ERROR] No accounts available on Ganache node.")
        sys.exit(1)

    configured_addr = read_env_contract_address()
    
    # Check if bytecode already exists at the configured address
    if configured_addr and w3.is_address(configured_addr):
        code = w3.eth.get_code(Web3.to_checksum_address(configured_addr))
        if code and len(code) > 0 and code != b'\x00' and code != b'':
            print(f"[OK] Contract bytecode already verified at {configured_addr} (size={len(code)} bytes, latestBlock={w3.eth.block_number})")
            return

    print(f"[INFO] Deploying AdaptiveAccessControl contract to Ganache (current block={w3.eth.block_number})...")
    if not os.path.isfile(ABI_FILE) or not os.path.isfile(BIN_FILE):
        print(f"[ERROR] ABI or BIN file missing in contracts/ ({ABI_FILE}, {BIN_FILE})")
        sys.exit(1)

    with open(ABI_FILE, "r", encoding="utf-8") as f:
        abi = json.load(f)
    with open(BIN_FILE, "r", encoding="utf-8") as f:
        bytecode = f.read().strip()

    owner = accounts[0]
    factory = w3.eth.contract(abi=abi, bytecode=bytecode)
    tx_hash = factory.constructor().transact({"from": owner, "gas": 3_000_000})
    receipt = w3.eth.wait_for_transaction_receipt(tx_hash)

    contract_address = receipt.contractAddress
    deploy_block = receipt.blockNumber
    deploy_tx = receipt.transactionHash.hex()
    deploy_gas = receipt.gasUsed
    deploy_status = receipt.status

    if deploy_status != 1:
        print(f"[ERROR] Contract deployment failed (receipt status={deploy_status})")
        sys.exit(1)

    print(f"[SUCCESS] Contract deployed at address: {contract_address}")
    print(f"  TxHash: {deploy_tx}")
    print(f"  Block : {deploy_block}")
    print(f"  Gas   : {deploy_gas}")

    # Verify deployed contract interface
    contract = w3.eth.contract(address=contract_address, abi=abi)
    t_high = contract.functions.trustHigh().call()
    t_med  = contract.functions.trustMedium().call()
    r_low  = contract.functions.riskLow().call()
    r_med  = contract.functions.riskMedium().call()
    owner_addr = contract.functions.owner().call()

    # Update .env
    update_env_contract_address(contract_address)
    print(f"[UPDATED] .env BLOCKCHAIN_CONTRACT_ADDRESS set to {contract_address}")

    # Update phase6b_state.json
    state_data = {
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
    }
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state_data, f, indent=2)
    print(f"[SAVED] State written to {STATE_FILE}")

if __name__ == "__main__":
    main()
