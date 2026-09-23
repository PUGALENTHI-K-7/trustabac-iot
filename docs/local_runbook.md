# TrustABAC-IoT: Local Reproducibility & Deployment Runbook

This runbook documents the clean-room deployment procedure for **TrustABAC-IoT**. Following these instructions allows an external reviewer to set up, deploy, and verify the entire system from a clean environment without proprietary dependencies or hardcoded machine paths.

---

## 1. Prerequisites & System Requirements

Ensure the following tools are installed on the host system:
- **Java**: OpenJDK 21 LTS (`java -version`)
- **Maven**: Apache Maven 3.9+ or included Maven Wrapper (`./mvnw.cmd` / `./mvnw`)
- **Python**: Python 3.10+ with `web3`, `requests`, `websockets`, `matplotlib` (`pip install -r requirements.txt` or equivalent)
- **Docker & Docker Compose**: Docker 24.0+ with Compose V2 (`docker compose version`)
- **Solidity Compiler (optional)**: Pre-compiled ABI and Bytecode are provided in `contracts/AdaptiveAccessControl.abi` and `contracts/AdaptiveAccessControl.bin`.

---

## 2. Environment Configuration

1. Copy `.env.example` to create your local `.env`:
   ```bash
   cp .env.example .env
   ```
2. Configure credentials in `.env` (or pass as environment variables):
   ```ini
   SERVER_PORT=8090
   DB_HOST=localhost
   DB_PORT=3307
   DB_NAME=trustabac_iot
   DB_USERNAME=root
   DB_PASSWORD=your_secure_db_password
   DB_ROOT_PASSWORD=your_secure_root_password
   RABBITMQ_HOST=localhost
   RABBITMQ_PORT=5672
   RABBITMQ_USERNAME=trustabac
   RABBITMQ_PASSWORD=your_secure_rabbitmq_password
   BLOCKCHAIN_RPC_URL=http://127.0.0.1:8545
   BLOCKCHAIN_CHAIN_ID=1337
   BLOCKCHAIN_CONTRACT_ADDRESS=
   BLOCKCHAIN_PRIVATE_KEY=
   ```

---

## 3. Infrastructure Service Startup (Docker)

Launch the containerized infrastructure components:

```bash
# 1. Start MySQL 8.0
docker run -d --name trustabac-mysql -p 3307:3306 -e MYSQL_ROOT_PASSWORD=your_secure_root_password -e MYSQL_DATABASE=trustabac_iot mysql:8.0

# 2. Start Ganache EVM Blockchain
docker run -d --name trustabac-ganache -p 8545:8545 trufflesuite/ganache:latest --wallet.deterministic --chain.chainId 1337 --chain.networkId 1337 --server.host 0.0.0.0 --server.port 8545

# 3. Start RabbitMQ Message Broker
docker run -d --name trustabac-rabbitmq -p 5672:5672 -p 15672:15672 -e RABBITMQ_DEFAULT_USER=trustabac -e RABBITMQ_DEFAULT_PASS=your_secure_rabbitmq_password rabbitmq:3-management
```

Verify container health:
```bash
docker ps
```

---

## 4. Smart Contract Deployment

Deploy the `AdaptiveAccessControl.sol` smart contract to the running Ganache EVM instance:

```bash
python contracts/test_ganache_verification.py
```

- Note the deployed contract address output (e.g. `0xe78A0F7E598Cc8b0Bb87894B0F60dD2a88d6a8Ab`).
- Set `BLOCKCHAIN_CONTRACT_ADDRESS` in your `.env` file or environment.

---

## 5. Gateway Application Build & Startup

1. **Build the executable JAR**:
   ```bash
   ./mvnw.cmd clean package -DskipTests
   ```
2. **Launch the TrustABAC-IoT Gateway**:
   ```bash
   java -Dserver.port=8090 \
        -Dspring.datasource.url="jdbc:mysql://localhost:3307/trustabac_iot?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" \
        -Dspring.datasource.username=root \
        -Dspring.datasource.password=your_secure_root_password \
        -Dspring.rabbitmq.host=localhost \
        -Dspring.rabbitmq.port=5672 \
        -Dspring.rabbitmq.username=trustabac \
        -Dspring.rabbitmq.password=your_secure_rabbitmq_password \
        -Dtrustabac.blockchain.rpc-url=http://127.0.0.1:8545 \
        -Dtrustabac.blockchain.chain-id=1337 \
        -Dtrustabac.blockchain.contract-address=0xe78A0F7E598Cc8b0Bb87894B0F60dD2a88d6a8Ab \
        -jar target/trustabac-iot-0.0.1-SNAPSHOT.jar
   ```

---

## 6. Health & Smoke Test Verification

1. **Verify Gateway Health**:
   ```bash
   curl http://localhost:8090/api/health
   ```
   *Expected Response*: `{"status":"UP", ...}`

2. **Execute Automated Final Smoke Test Suite**:
   ```bash
   python contracts/final_smoke_test.py
   ```
   *Validates all 15 operational checks (ABAC, Trust, Risk, Solidity, Downgrading, Fail-Closed, STOMP, Batch).*

3. **Access Observational Web Dashboard**:
   Open a browser to: `http://localhost:8090/dashboard`

---

## 7. Clean Demo Environment Reset

To reset the device fleet and baseline trust/booking state for a clean demonstration:
```bash
curl -X POST http://localhost:8090/api/simulator/reset
```

---

## 8. Graceful Teardown

```bash
# Stop application process (Ctrl+C in terminal)

# Stop and remove infrastructure containers
docker stop trustabac-mysql trustabac-ganache trustabac-rabbitmq
docker rm trustabac-mysql trustabac-ganache trustabac-rabbitmq
```
