@echo off
setlocal EnableDelayedExpansion

cd /d "%~dp0"

:: Load environment variables from .env
if exist .env (
    for /f "usebackq tokens=1,* eol=# delims==" %%A in (".env") do (
        set "KEY=%%A"
        set "VAL=%%B"
        if not "!KEY!"=="" (
            set "!KEY!=!VAL!"
        )
    )
)

if "%SERVER_PORT%"=="" set SERVER_PORT=8090

:: Start Java directly with redirection
java -jar "target\trustabac-iot-0.0.1-SNAPSHOT.jar" --server.port=%SERVER_PORT% > "logs\demo-app.log" 2> "logs\demo-app-error.log"

endlocal
