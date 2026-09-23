#Requires -Version 5.1
<#
.SYNOPSIS
    TrustABAC-IoT Demo Shutdown — stop-demo.ps1
.DESCRIPTION
    Safely stops the Spring Boot process (using saved PID) and
    docker compose stops the three infrastructure containers.
    Does NOT remove containers, volumes, or database data.
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Continue'   # Don't abort on individual errors

$ProjectRoot = $PSScriptRoot
Set-Location -LiteralPath $ProjectRoot

$PidFile    = Join-Path $ProjectRoot 'logs\demo-app.pid'
$AppPort    = 8090

function Write-Ok {
    param([string]$Label)
    Write-Host ("  {0,-24} " -f $Label) -NoNewline
    Write-Host "stopped" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Label, [string]$Note)
    Write-Host ("  {0,-24} " -f $Label) -NoNewline
    Write-Host "skipped  ($Note)" -ForegroundColor Magenta
}

Write-Host ''
Write-Host ('=' * 60) -ForegroundColor Cyan
Write-Host '  TrustABAC-IoT DEMO SHUTDOWN' -ForegroundColor Cyan
Write-Host ('=' * 60) -ForegroundColor Cyan
Write-Host ''

# ============================================================
# STOP SPRING BOOT
# ============================================================
Write-Host '>> Stopping Spring Boot application...' -ForegroundColor Yellow

$stopped = $false

# 1. Try saved PID
if (Test-Path -LiteralPath $PidFile) {
    $savedPid = (Get-Content -LiteralPath $PidFile -Raw).Trim()
    if ($savedPid -match '^\d+$') {
        $proc = Get-Process -Id ([int]$savedPid) -ErrorAction SilentlyContinue
        if ($proc) {
            Write-Host "  Stopping saved PID $savedPid ($($proc.ProcessName))..." -ForegroundColor Gray
            Stop-Process -Id ([int]$savedPid) -Force -ErrorAction SilentlyContinue
            Start-Sleep -Seconds 2
            $stopped = $true
        } else {
            Write-Host "  Saved PID $savedPid is no longer running." -ForegroundColor DarkGray
        }
    }
    Remove-Item -LiteralPath $PidFile -Force -ErrorAction SilentlyContinue
}

# 2. Fallback — find any process listening on port 8090
$conn = Get-NetTCPConnection -LocalPort $AppPort -State Listen -ErrorAction SilentlyContinue
if ($conn) {
    foreach ($c in $conn) {
        $pid8090 = $c.OwningProcess
        Write-Host "  Stopping process on port $AppPort (PID $pid8090)..." -ForegroundColor Gray
        Stop-Process -Id $pid8090 -Force -ErrorAction SilentlyContinue
        $stopped = $true
    }
}

# Wait for port to release
$waited = 0
while ($waited -lt 10) {
    $conn2 = Get-NetTCPConnection -LocalPort $AppPort -State Listen -ErrorAction SilentlyContinue
    if (-not $conn2) { break }
    Start-Sleep -Seconds 1; $waited++
}

$portFree = -not (Get-NetTCPConnection -LocalPort $AppPort -State Listen -ErrorAction SilentlyContinue)

if ($stopped -and $portFree) {
    Write-Ok 'Spring Boot'
} elseif (-not $stopped) {
    Write-Warn 'Spring Boot' 'not running'
} else {
    Write-Host "  Spring Boot process stopped but port $AppPort may still be in TIME_WAIT. This is normal." -ForegroundColor Magenta
}

# ============================================================
# STOP DOCKER INFRASTRUCTURE
# ============================================================
Write-Host ''
Write-Host '>> Stopping Docker infrastructure containers...' -ForegroundColor Yellow
Write-Host '   (containers are STOPPED, not removed - data is preserved)' -ForegroundColor DarkGray

if (Get-Command 'docker' -ErrorAction SilentlyContinue) {
    # Stop via docker compose (preserves volumes and data)
    $null = cmd /c "docker compose stop trustabac-mysql trustabac-rabbitmq trustabac-ganache 2>&1"

    # Verify each container stopped
    foreach ($svc in @('trustabac-mysql', 'trustabac-rabbitmq', 'trustabac-ganache')) {
        $state = (cmd /c "docker inspect --format {{.State.Status}} $svc 2>&1").Trim()
        if ($state -eq 'exited') {
            Write-Ok $svc
        } elseif ([string]::IsNullOrEmpty($state) -or $state -match 'No such') {
            Write-Warn $svc 'container not found'
        } else {
            Write-Host ("  {0,-24} " -f $svc) -NoNewline
            Write-Host "state=$state" -ForegroundColor Magenta
        }
    }
} else {
    Write-Warn 'Docker' 'docker command not found'
}

Write-Host ''
Write-Host ('=' * 60) -ForegroundColor Cyan
Write-Host '  Demo environment stopped.' -ForegroundColor Green
Write-Host '  Data and containers are preserved.' -ForegroundColor DarkGray
Write-Host '  Restart with: START_DEMO.bat' -ForegroundColor DarkGray
Write-Host ('=' * 60) -ForegroundColor Cyan
Write-Host ''
