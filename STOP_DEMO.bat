@echo off
setlocal EnableDelayedExpansion

:: TrustABAC-IoT — STOP_DEMO.bat
:: Double-click this file in Windows Explorer to stop the demo.

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

title TrustABAC-IoT Demo Shutdown

echo.
echo ============================================================
echo   TrustABAC-IoT Demo Shutdown
echo ============================================================
echo   Project root: %SCRIPT_DIR%
echo ============================================================
echo.

where powershell.exe >nul 2>&1
if errorlevel 1 (
    echo ERROR: powershell.exe not found on PATH.
    pause
    exit /b 1
)

if not exist "%SCRIPT_DIR%\stop-demo.ps1" (
    echo ERROR: stop-demo.ps1 not found in:
    echo   %SCRIPT_DIR%
    pause
    exit /b 1
)

powershell.exe -NoProfile -NonInteractive -ExecutionPolicy Bypass ^
    -File "%SCRIPT_DIR%\stop-demo.ps1"

set "PS_EXIT=%errorlevel%"

echo.
if %PS_EXIT% EQU 0 (
    echo   Shutdown complete.
) else (
    echo   Shutdown finished with warnings (exit code %PS_EXIT%).
    echo   Check output above.
)
echo.
pause

endlocal
