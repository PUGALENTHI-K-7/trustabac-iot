#Requires -Version 5.1
<#
.SYNOPSIS
    TrustABAC-IoT Demo Launcher — start-demo.ps1
.DESCRIPTION
    Starts the complete TrustABAC-IoT demo environment:
    Docker infrastructure → Spring Boot JAR → Health/service verification → Browser
    Run via START_DEMO.bat for a double-click experience.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

# ============================================================
# SECTION A — PROJECT ROOT
# ============================================================
$ProjectRoot = $PSScriptRoot
Set-Location -LiteralPath $ProjectRoot

$PidFile     = Join-Path $ProjectRoot 'logs\demo-app.pid'
$LogFile     = Join-Path $ProjectRoot 'logs\demo-app.log'
$ErrLogFile  = Join-Path $ProjectRoot 'logs\demo-app-error.log'
$EnvFile     = Join-Path $ProjectRoot '.env'
$JarPath     = Join-Path $ProjectRoot 'target\trustabac-iot-0.0.1-SNAPSHOT.jar'

$AppBase     = 'http://127.0.0.1:8090'
$HealthUrl   = "$AppBase/api/health"

# ============================================================
# HELPER FUNCTIONS
# ============================================================

function Write-Banner {
    param([string]$Text, [string]$Color = 'Cyan')
    Write-Host "`n$('=' * 60)" -ForegroundColor $Color
    Write-Host "  $Text" -ForegroundColor $Color
    Write-Host "$('=' * 60)" -ForegroundColor $Color
}

function Write-Step {
    param([string]$Text)
    Write-Host "`n>> $Text" -ForegroundColor Yellow
}

function Write-Ok {
    param([string]$Label)
    Write-Host ("  {0,-20} " -f $Label) -NoNewline
    Write-Host "OK" -ForegroundColor Green
}

function Write-Fail {
    param([string]$Label, [string]$Reason = '')
    Write-Host ("  {0,-20} " -f $Label) -NoNewline
    Write-Host "FAIL" -ForegroundColor Red
    if ($Reason) { Write-Host "    Reason: $Reason" -ForegroundColor Red }
}

function Write-Warn {
    param([string]$Label, [string]$Note = '')
    Write-Host ("  {0,-20} " -f $Label) -NoNewline
    Write-Host "WARN" -ForegroundColor Magenta
    if ($Note) { Write-Host "    Note: $Note" -ForegroundColor Magenta }
}

function Invoke-SafeGet {
    param([string]$Url, [int]$TimeoutSec = 10)
    try {
        $resp = Invoke-WebRequest -Uri $Url -UseBasicParsing `
                    -TimeoutSec $TimeoutSec -ErrorAction Stop
        return $resp
    } catch {
        return $null
    }
}

function Get-JsonField {
    param([string]$Json, [string]$Field)
    try {
        $obj = $Json | ConvertFrom-Json -ErrorAction Stop
        return $obj.$Field
    } catch {
        return $null
    }
}

function Test-PortFree {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    return ($null -eq $conn)
}

function Get-PidOnPort {
    param([int]$Port)
    $conn = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
    if ($conn) { return $conn[0].OwningProcess }
    return $null
}

function Stop-PidOnPort {
    param([int]$Port)
    $pid8090 = Get-PidOnPort -Port $Port
    if ($null -ne $pid8090) {
        Write-Host "  Port $Port occupied by PID $pid8090. Stopping process..." -ForegroundColor Magenta
        try {
            Stop-Process -Id $pid8090 -Force -ErrorAction Stop
        } catch {
            Write-Host "  Could not stop PID $pid8090 - $($_.Exception.Message)" -ForegroundColor Red
            Write-Host "  Please stop the process manually and rerun." -ForegroundColor Red
            exit 1
        }
        # Wait up to 10 s for port to free
        $waited = 0
        while (-not (Test-PortFree -Port $Port) -and $waited -lt 10) {
            Start-Sleep -Seconds 1; $waited++
        }
        if (-not (Test-PortFree -Port $Port)) {
            Write-Host "  Port $Port still occupied after stopping PID $pid8090." -ForegroundColor Red
            exit 1
        }
        Write-Host "  Port $Port is now free." -ForegroundColor Green
    }
}

# ============================================================
# SECTION B — BASIC CHECKS
# ============================================================
Write-Banner 'TrustABAC-IoT DEMO LAUNCHER'
Write-Step 'Checking prerequisites...'

if (-not (Get-Command 'docker' -ErrorAction SilentlyContinue)) {
    Write-Host 'ERROR: docker command not found. Please install Docker Desktop and ensure it is on PATH.' -ForegroundColor Red
    Read-Host 'Press Enter to exit'
    exit 1
}
Write-Ok 'Docker binary'

if (-not (Get-Command 'java' -ErrorAction SilentlyContinue)) {
    Write-Host 'ERROR: java command not found. Please install Java 21 and add it to PATH.' -ForegroundColor Red
    Read-Host 'Press Enter to exit'
    exit 1
}
$javaVerRaw = ''
try { $javaVerRaw = (java -version 2>&1) -join ' ' } catch {}
Write-Ok "Java binary"

if (-not (Test-Path -LiteralPath $JarPath)) {
    Write-Host "ERROR: JAR not found at: $JarPath" -ForegroundColor Red
    Write-Host 'Run: mvnw.cmd package -DskipTests' -ForegroundColor Yellow
    Read-Host 'Press Enter to exit'
    exit 1
}
Write-Ok 'Application JAR found'

if (-not (Test-Path -LiteralPath $EnvFile)) {
    Write-Host "ERROR: .env file not found at: $EnvFile" -ForegroundColor Red
    Write-Host 'Copy .env.example to .env and configure values.' -ForegroundColor Yellow
    Read-Host 'Press Enter to exit'
    exit 1
}
Write-Ok '.env file found'

# Verify Docker daemon is responsive
$dockerInfo = cmd /c "docker info 2>&1"
if ($LASTEXITCODE -ne 0) {
    Write-Host 'ERROR: Docker daemon is not running. Please start Docker Desktop.' -ForegroundColor Red
    Read-Host 'Press Enter to exit'
    exit 1
}
Write-Ok 'Docker daemon running'

# ============================================================
# SECTION C — LOAD .ENV
# ============================================================
Write-Step 'Loading environment variables from .env...'

$secretKeys = @('DB_PASSWORD','DB_ROOT_PASSWORD','BLOCKCHAIN_PRIVATE_KEY','RABBITMQ_PASSWORD')

foreach ($line in (Get-Content -LiteralPath $EnvFile)) {
    $trimmed = $line.Trim()
    # Skip blanks and comments
    if ([string]::IsNullOrEmpty($trimmed) -or $trimmed.StartsWith('#')) { continue }

    # Split only at first '='
    $eqIdx = $trimmed.IndexOf('=')
    if ($eqIdx -le 0) { continue }

    $varName  = $trimmed.Substring(0, $eqIdx).Trim()
    $varValue = $trimmed.Substring($eqIdx + 1).Trim()

    # Set in current process scope so child java process inherits
    [System.Environment]::SetEnvironmentVariable($varName, $varValue, 'Process')
}

Write-Host '  Environment loaded' -ForegroundColor Green

# Capture variables we need for verification (non-secret)
$EnvDbHost      = [System.Environment]::GetEnvironmentVariable('DB_HOST')
$EnvDbPort      = [System.Environment]::GetEnvironmentVariable('DB_PORT')
$EnvDbName      = [System.Environment]::GetEnvironmentVariable('DB_NAME')
$EnvDbUser      = [System.Environment]::GetEnvironmentVariable('DB_USERNAME')
$EnvRmqHost     = [System.Environment]::GetEnvironmentVariable('RABBITMQ_HOST')
$EnvRmqPort     = [System.Environment]::GetEnvironmentVariable('RABBITMQ_PORT')
$EnvChainUrl    = [System.Environment]::GetEnvironmentVariable('BLOCKCHAIN_RPC_URL')
$EnvContractAddr= [System.Environment]::GetEnvironmentVariable('BLOCKCHAIN_CONTRACT_ADDRESS')
$EnvServerPort  = [System.Environment]::GetEnvironmentVariable('SERVER_PORT')
if (-not $EnvServerPort) { $EnvServerPort = '8090' }

Write-Host "  DB        : $EnvDbUser@$EnvDbHost`:$EnvDbPort/$EnvDbName" -ForegroundColor DarkGray
Write-Host "  RabbitMQ  : $EnvRmqHost`:$EnvRmqPort" -ForegroundColor DarkGray
Write-Host "  Blockchain: $EnvChainUrl" -ForegroundColor DarkGray
Write-Host "  Contract  : $EnvContractAddr" -ForegroundColor DarkGray

# ============================================================
# SECTION D — START DOCKER INFRASTRUCTURE
# ============================================================
Write-Step 'Starting Docker infrastructure (mysql, rabbitmq, ganache)...'

# Check whether trustabac-iot (app) container exists to warn user but not start it
$appContainer = cmd /c "docker ps -a --filter name=trustabac-iot --format {{.Names}} 2>&1"
if ($appContainer -match 'trustabac-iot') {
    Write-Host '  NOTE: trustabac-iot Docker app container exists but will NOT be started.' -ForegroundColor DarkGray
    Write-Host '        Spring Boot runs directly from JAR on this host.' -ForegroundColor DarkGray
}

# Start only the three infrastructure services
# Use cmd.exe to avoid PowerShell 5.1 NativeCommandError on docker's stderr warnings
$dockerUpOutput = cmd /c "docker compose up -d trustabac-mysql trustabac-rabbitmq trustabac-ganache 2>&1"
$dockerUpExit   = $LASTEXITCODE
if ($dockerUpExit -ne 0) {
    Write-Host "ERROR: docker compose up failed (exit $dockerUpExit)." -ForegroundColor Red
    Write-Host $dockerUpOutput -ForegroundColor Red
    Read-Host 'Press Enter to exit'
    exit 1
}

# Wait for containers to be healthy / running (up to 60s)
Write-Host '  Waiting for containers to become ready...' -ForegroundColor Gray
$containers = @('trustabac-mysql', 'trustabac-rabbitmq', 'trustabac-ganache')
$maxWait = 60
$interval = 3

foreach ($cname in $containers) {
    $elapsed = 0
    Write-Host "  Waiting for $cname ..." -NoNewline -ForegroundColor Gray
    while ($elapsed -lt $maxWait) {
        $state = cmd /c "docker inspect --format {{.State.Status}} $cname 2>&1"
        $state = $state.Trim()
        if ($state -eq 'running') {
            Write-Host " running" -ForegroundColor Green
            break
        }
        Start-Sleep -Seconds $interval
        $elapsed += $interval
        Write-Host '.' -NoNewline -ForegroundColor Gray
    }
    if ($elapsed -ge $maxWait) {
        Write-Host " TIMEOUT" -ForegroundColor Red
        Write-Host "  Container $cname did not start in time. Check: docker logs $cname" -ForegroundColor Red
    }
}

# Extra wait for MySQL to finish initialization (mysqladmin ping)
Write-Host '  Waiting for MySQL to accept connections...' -NoNewline -ForegroundColor Gray
$mysqlReady = $false
$dbRootPw = [System.Environment]::GetEnvironmentVariable('DB_ROOT_PASSWORD')
for ($i = 0; $i -lt 20; $i++) {
    $pingOut = cmd /c "docker exec trustabac-mysql mysqladmin ping -h localhost -u root -p$dbRootPw --silent 2>&1"
    if ($LASTEXITCODE -eq 0) { $mysqlReady = $true; break }
    Start-Sleep -Seconds 3
    Write-Host '.' -NoNewline -ForegroundColor Gray
}
if ($mysqlReady) {
    Write-Host ' ready' -ForegroundColor Green
} else {
    Write-Host ' still initializing (proceeding)' -ForegroundColor Magenta
}

# Ensure smart contract is deployed on Ganache
if (Test-Path -LiteralPath "$ProjectRoot\contracts\ensure_contract_deployed.py") {
    Write-Host '  Verifying smart contract deployment on Ganache...' -ForegroundColor Gray
    $depOut = cmd /c "python `"$ProjectRoot\contracts\ensure_contract_deployed.py`" 2>&1"
    Write-Host "  $depOut" -ForegroundColor DarkGray
    # Reload .env so $EnvContractAddr and process environment reflect the active contract address
    if (Test-Path -LiteralPath $EnvFile) {
        foreach ($line in (Get-Content -LiteralPath $EnvFile)) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
            $eqIdx = $trimmed.IndexOf('=')
            if ($eqIdx -gt 0) {
                $k = $trimmed.Substring(0, $eqIdx).Trim()
                $v = $trimmed.Substring($eqIdx + 1).Trim()
                if ($k -eq 'BLOCKCHAIN_CONTRACT_ADDRESS') {
                    $EnvContractAddr = $v
                }
                [System.Environment]::SetEnvironmentVariable($k, $v, 'Process')
            }
        }
    }
}

# ============================================================
# SECTION E — CLEAR PORT 8090
# ============================================================
Write-Step "Checking port $EnvServerPort for stale processes..."
Stop-PidOnPort -Port ([int]$EnvServerPort)
Write-Host "  Port $EnvServerPort is free." -ForegroundColor Green

# ============================================================
# SECTION F — START SPRING BOOT
# ============================================================
Write-Step 'Starting Spring Boot application...'

# Ensure logs directory exists
$LogDir = Join-Path $ProjectRoot 'logs'
if (-not (Test-Path -LiteralPath $LogDir)) {
    New-Item -ItemType Directory -Path $LogDir | Out-Null
}

# Check if already running (idempotent)
if (Test-Path -LiteralPath $PidFile) {
    $savedPid = Get-Content -LiteralPath $PidFile -Raw
    $savedPid = $savedPid.Trim()
    $proc = Get-Process -Id $savedPid -ErrorAction SilentlyContinue
    if ($proc -and -not (Test-PortFree -Port ([int]$EnvServerPort))) {
        Write-Host "  Spring Boot already running (PID $savedPid). Skipping start." -ForegroundColor Green
        $AppPid = [int]$savedPid
        # Jump to health check
        goto_health = $true
    }
}

$goto_health = $false

if (-not $goto_health) {
    # Initialize/clear log files
    Set-Content -LiteralPath $LogFile -Value '' -Encoding UTF8
    Set-Content -LiteralPath $ErrLogFile -Value '' -Encoding UTF8

    # Extract all configuration keys from .env as JVM system properties (-Dkey=value)
    $jvmArgs = @()
    if (Test-Path -LiteralPath $EnvFile) {
        foreach ($line in (Get-Content -LiteralPath $EnvFile)) {
            $trimmed = $line.Trim()
            if (-not $trimmed -or $trimmed.StartsWith('#')) { continue }
            $eqIdx = $trimmed.IndexOf('=')
            if ($eqIdx -gt 0) {
                $k = $trimmed.Substring(0, $eqIdx).Trim()
                $v = $trimmed.Substring($eqIdx + 1).Trim()
                $jvmArgs += "-D$k=$v"
            }
        }
    }

    $jvmArgStr = $jvmArgs -join ' '
    $cmd = "cmd.exe /c `"`"java.exe`" $jvmArgStr -jar `"$JarPath`" --server.port=$EnvServerPort > `"$LogFile`" 2> `"$ErrLogFile`"`""

    # Launch Java as a completely detached background process via WMI (independent of shell/job lifecycle)
    $wmiRes = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
        CommandLine      = $cmd
        CurrentDirectory = $ProjectRoot
    }

    $cmdPid = $wmiRes.ProcessId
    Write-Host "  Spring Boot starting via WMI daemon (CmdPID $cmdPid)..." -ForegroundColor Cyan
    Write-Host "  stdout log : $LogFile" -ForegroundColor DarkGray
    Write-Host "  stderr log : $ErrLogFile" -ForegroundColor DarkGray
}

# ============================================================
# SECTION G — WAIT FOR APPLICATION HEALTH
# ============================================================
Write-Step 'Waiting for Spring Boot health endpoint...'

$healthTimeout  = 120   # seconds
$healthInterval = 3
$elapsed        = 0
$healthy        = $false

Write-Host '  Polling http://127.0.0.1:8090/api/health' -ForegroundColor Gray
Write-Host '  ' -NoNewline

while ($elapsed -lt $healthTimeout) {
    $resp = Invoke-SafeGet -Url $HealthUrl -TimeoutSec 5
    if ($resp -and $resp.StatusCode -eq 200) {
        $healthy = $true
        break
    }

    Write-Host '.' -NoNewline -ForegroundColor Gray
    Start-Sleep -Seconds $healthInterval
    $elapsed += $healthInterval
}
Write-Host ''

if (-not $healthy) {
    Write-Host "`n  Spring Boot did NOT become healthy within $healthTimeout seconds." -ForegroundColor Red
    Write-Host "  Check logs for errors: $LogFile" -ForegroundColor Red
    Write-Host "  Check errors:          $ErrLogFile" -ForegroundColor Red

    if (Test-Path -LiteralPath $ErrLogFile) {
        Write-Host "`n  --- Last startup error lines ---" -ForegroundColor Yellow
        $errLines = Get-Content -LiteralPath $ErrLogFile -Tail 25 -ErrorAction SilentlyContinue
        if ($errLines) {
            foreach ($l in $errLines) { Write-Host "  $l" -ForegroundColor Red }
        }
        Write-Host '  --------------------------------' -ForegroundColor Yellow
    }

    Read-Host 'Press Enter to exit'
    exit 1
}

# Record the exact PID listening on port 8090
$listenConn = Get-NetTCPConnection -LocalPort ([int]$EnvServerPort) -State Listen -ErrorAction SilentlyContinue
if ($listenConn) {
    $AppPid = $listenConn[0].OwningProcess
    Set-Content -LiteralPath $PidFile -Value $AppPid
}
Write-Host "  Spring Boot: HEALTHY (PID $AppPid)" -ForegroundColor Green

# ============================================================
# SECTION H + I — VERIFY ALL SERVICES
# ============================================================
Write-Step 'Verifying all subsystems...'

$allOk = $true

# 1. Backend /api/health
$r = Invoke-SafeGet -Url "$AppBase/api/health"
if ($r -and $r.StatusCode -eq 200) {
    Write-Ok 'Backend       '
} else {
    Write-Fail 'Backend       ' 'Health endpoint returned non-200'
    $allOk = $false
}

# 2. Blockchain & Smart Contract /api/blockchain/status
$r = Invoke-SafeGet -Url "$AppBase/api/blockchain/status"
if ($r -and $r.StatusCode -eq 200) {
    $json = $r.Content
    $rpcOk      = Get-JsonField -Json $json -Field 'rpcReachable'
    $contractOk = Get-JsonField -Json $json -Field 'contractReachable'
    $chainId    = Get-JsonField -Json $json -Field 'chainId'

    if ($rpcOk -eq $true) {
        Write-Ok "Ganache RPC   (chain=$chainId)"
    } else {
        Write-Fail 'Ganache RPC   ' 'RPC not reachable'
        $allOk = $false
    }

    if ($contractOk -eq $true) {
        Write-Ok "Smart Contract ($EnvContractAddr)"
    } else {
        Write-Fail 'Smart Contract' "Contract not reachable at $EnvContractAddr"
        $allOk = $false
    }
} else {
    Write-Fail 'Blockchain    ' '/api/blockchain/status unreachable'
    $allOk = $false
}

# 3. RabbitMQ /api/messaging/status
$r = Invoke-SafeGet -Url "$AppBase/api/messaging/status"
if ($r -and $r.StatusCode -eq 200) {
    $brokerOk = Get-JsonField -Json $r.Content -Field 'brokerReachable'
    if ($brokerOk -eq $true) {
        Write-Ok 'RabbitMQ      '
    } else {
        Write-Fail 'RabbitMQ      ' 'brokerReachable=false'
        $allOk = $false
    }
} else {
    Write-Fail 'RabbitMQ      ' '/api/messaging/status unreachable'
    $allOk = $false
}

# 4. WebSocket /api/websocket/status
$r = Invoke-SafeGet -Url "$AppBase/api/websocket/status"
if ($r -and $r.StatusCode -eq 200) {
    $wsStatus = Get-JsonField -Json $r.Content -Field 'status'
    if ($wsStatus -eq 'ACTIVE') {
        Write-Ok 'WebSocket     '
    } else {
        Write-Warn 'WebSocket     ' "status=$wsStatus (not ACTIVE)"
    }
} else {
    Write-Fail 'WebSocket     ' '/api/websocket/status unreachable'
    $allOk = $false
}

# 5. Devices /api/devices
$r = Invoke-SafeGet -Url "$AppBase/api/devices"
if ($r -and $r.StatusCode -eq 200) {
    try {
        $devCount = ($r.Content | ConvertFrom-Json).Count
        Write-Ok "Devices       ($devCount registered)"
    } catch {
        Write-Ok 'Devices       '
    }
} else {
    Write-Fail 'Devices       ' '/api/devices unreachable'
    $allOk = $false
}

# 6. Bookings /api/bookings
$r = Invoke-SafeGet -Url "$AppBase/api/bookings"
if ($r -and $r.StatusCode -eq 200) {
    try {
        $bkCount = ($r.Content | ConvertFrom-Json).Count
        Write-Ok "Bookings      ($bkCount found)"
    } catch {
        Write-Ok 'Bookings      '
    }
} else {
    Write-Fail 'Bookings      ' '/api/bookings unreachable'
    $allOk = $false
}

# 7. Simulator status /api/simulator/status
$r = Invoke-SafeGet -Url "$AppBase/api/simulator/status"
if ($r -and $r.StatusCode -eq 200) {
    try {
        $simObj = $r.Content | ConvertFrom-Json
        $simState = if ($simObj.running) { 'RUNNING' } else { 'IDLE' }
        $simMode  = if ($simObj.mode) { $simObj.mode } else { 'MANUAL' }
        Write-Ok "Simulator     (state=$simState, mode=$simMode)"
    } catch {
        Write-Ok 'Simulator     '
    }
} else {
    Write-Warn 'Simulator     ' '/api/simulator/status unreachable (non-critical)'
}

# ============================================================
# SECTION J — DEMO BASELINE CHECK
# ============================================================
Write-Step 'Checking demo baseline state...'

# Check DOOR-SENSOR-001
$r = Invoke-SafeGet -Url "$AppBase/api/devices/identifier/DOOR-SENSOR-001"
$doorFound = $false
if ($r -and $r.StatusCode -eq 200) {
    try {
        $door = $r.Content | ConvertFrom-Json
        if ($door -and $door.deviceIdentifier -eq 'DOOR-SENSOR-001') {
            $doorFound = $true
            $isActive  = $door.active
            $trust     = $door.currentTrust
            if ($isActive) {
                Write-Ok "Door device   (active=true, trust=$trust)"
            } else {
                Write-Warn 'Door device   ' 'DOOR-SENSOR-001 active=false - check device registration'
            }
        } else {
            Write-Warn 'Door device   ' 'DOOR-SENSOR-001 not found in device list'
        }
    } catch {
        Write-Warn 'Door baseline ' "Parse error: $($_.Exception.Message)"
    }
}

# Check for active bookings (BookingResponse.bookingStatus == 'ACTIVE')
$r = Invoke-SafeGet -Url "$AppBase/api/bookings"
$activeBooking = $false
if ($r -and $r.StatusCode -eq 200) {
    try {
        $bookings = @($r.Content | ConvertFrom-Json)
        # BookingResponse fields: id, bookingReference, propertyId, guestUserId,
        #                         validFrom, validUntil, bookingStatus, createdAt, updatedAt
        $active = $bookings | Where-Object { $_.bookingStatus -eq 'ACTIVE' }
        if ($active -and @($active).Count -gt 0) {
            $activeBooking = $true
            Write-Ok "Active booking ($(@($active).Count) ACTIVE found)"
        } else {
            Write-Warn 'Active booking' 'No ACTIVE bookings - demo scenarios requiring booking may fail'
        }
    } catch {
        Write-Warn 'Active booking' "Parse error: $($_.Exception.Message)"
    }
}

# Dashboard HTTP 200 check
$r = Invoke-SafeGet -Url "$AppBase/dashboard"
if ($r -and $r.StatusCode -eq 200) {
    Write-Ok 'Dashboard page'
} else {
    Write-Fail 'Dashboard page' "$AppBase/dashboard returned non-200"
    $allOk = $false
}

# ============================================================
# SECTION K — OPEN BROWSER
# ============================================================
Write-Step 'Opening browser...'
Start-Sleep -Seconds 1
Start-Process "$AppBase/dashboard"
Write-Host "  Dashboard opened: $AppBase/dashboard" -ForegroundColor Cyan

# ============================================================
# SECTION L — FINAL SUMMARY
# ============================================================
$readyLine = if ($allOk) { 'DEMO READY' } else { 'DEMO READY WITH WARNINGS — check items above' }

Write-Host ''
Write-Host ('=' * 60) -ForegroundColor Cyan
Write-Host "  TRUSTABAC-IoT $readyLine" -ForegroundColor $(if ($allOk) { 'Green' } else { 'Yellow' })
Write-Host ('=' * 60) -ForegroundColor Cyan
Write-Host ''
Write-Host "  Dashboard  : $AppBase/dashboard" -ForegroundColor White
Write-Host "  Health     : $AppBase/api/health" -ForegroundColor White
Write-Host "  App PID    : $AppPid" -ForegroundColor White
Write-Host ''
Write-Host '  Service Status:' -ForegroundColor Gray
Write-Host "    Backend            $(if ($allOk) { '[OK]' } else { '[see above]' })" -ForegroundColor Cyan
Write-Host "    Logs               : $LogFile" -ForegroundColor DarkGray
Write-Host ''
Write-Host '  Demo Flow:' -ForegroundColor Gray
Write-Host '    1.  Open dashboard at http://127.0.0.1:8090/dashboard' -ForegroundColor White
Write-Host '    2.  Select Smart Door Lock (DOOR-SENSOR-001)' -ForegroundColor White
Write-Host '    3.  POST /api/simulator/scenarios/NORMAL_STAY/run' -ForegroundColor White
Write-Host '    4.  Show ABAC gate pass -> Trust + Risk scores -> Smart Contract' -ForegroundColor White
Write-Host '    5.  Show ALLOW -> EXECUTED -> on-chain tx hash' -ForegroundColor White
Write-Host '    6.  Run RESTRICT_ENFORCEMENT -> Show DOWNGRADED' -ForegroundColor White
Write-Host '    7.  Run LOW_TRUST_ATTACK / HIGH_RISK_ATTACK -> Show DENIED' -ForegroundColor White
Write-Host '    8.  Pause Ganache -> Show Fail-Closed DENY in 0.1ms' -ForegroundColor White
Write-Host '    9.  Unpause Ganache -> Show recovery' -ForegroundColor White
Write-Host '    10. POST /api/batch/audit/trigger -> Show idempotent audit' -ForegroundColor White
Write-Host ''
Write-Host ('=' * 60) -ForegroundColor Cyan
Write-Host ''
Write-Host "  To stop: double-click STOP_DEMO.bat" -ForegroundColor DarkGray
Write-Host ''
