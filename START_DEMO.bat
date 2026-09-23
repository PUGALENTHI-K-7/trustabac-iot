@echo off
setlocal EnableDelayedExpansion

:: TrustABAC-IoT — START_DEMO.bat
:: Double-click this file in Windows Explorer to start the demo.

:: Resolve directory robustly (handles spaces in path)
set "SCRIPT_DIR=%~dp0"
:: Remove trailing backslash
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

title TrustABAC-IoT Demo Launcher

echo.
echo ============================================================
echo   TrustABAC-IoT Demo Launcher
echo ============================================================
echo   Project root: %SCRIPT_DIR%
echo   Starting start-demo.ps1 via PowerShell...
echo ============================================================
echo.

:: Check that PowerShell is available
where powershell.exe >nul 2>&1
if errorlevel 1 (
    echo ERROR: powershell.exe not found on PATH.
    echo Please install Windows PowerShell 5.1 or later.
    pause
    exit /b 1
)

:: Check the PowerShell script exists
if not exist "%SCRIPT_DIR%\start-demo.ps1" (
    echo ERROR: start-demo.ps1 not found in:
    echo   %SCRIPT_DIR%
    pause
    exit /b 1
)

:: Launch PowerShell with execution policy bypass so no signature is required.
:: -NoProfile avoids loading user profiles that might interfere.
:: -File with quoted path handles spaces correctly.
powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass ^
    -File "%SCRIPT_DIR%\start-demo.ps1"

set "PS_EXIT=%errorlevel%"

if %PS_EXIT% NEQ 0 (
    echo.
    echo ============================================================
    echo   ERROR: Demo startup failed (exit code %PS_EXIT%)
    echo   Check the output above for details.
    echo ============================================================
    echo.
    pause
    exit /b %PS_EXIT%
)

:: Keep terminal open so faculty can see the demo summary
echo.
echo Demo is running. Close this window when done, or press a key.
pause >nul

endlocal
