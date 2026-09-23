@echo off
REM TrustABAC-IoT Gateway Startup Script
REM Reads configuration from environment variables or .env file

if exist .env (
    for /F "usebackq tokens=1,* delims==" %%A in (".env") do (
        if not "%%A"=="" if not "%%A:~0,1%"=="#" set %%A=%%B
    )
)

if "%BLOCKCHAIN_RPC_URL%"=="" set BLOCKCHAIN_RPC_URL=http://127.0.0.1:8545
if "%BLOCKCHAIN_CHAIN_ID%"=="" set BLOCKCHAIN_CHAIN_ID=1337
if "%DB_HOST%"=="" set DB_HOST=localhost
if "%DB_PORT%"=="" set DB_PORT=3307
if "%DB_NAME%"=="" set DB_NAME=trustabac_iot
if "%DB_USERNAME%"=="" set DB_USERNAME=root
if "%SERVER_PORT%"=="" set SERVER_PORT=8090

echo Starting TrustABAC-IoT Gateway on port %SERVER_PORT%...
java -jar target\trustabac-iot-0.0.1-SNAPSHOT.jar --server.port=%SERVER_PORT%
