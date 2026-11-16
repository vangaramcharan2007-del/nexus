@echo off
setlocal enabledelayedexpansion
title NEXUS V26.1 Enterprise Platform (Reliability and Security Edition)
cd /d %~dp0

echo ===============================================================================
echo        NEXUS V26.1 - ENTERPRISE PLATFORM LAUNCHER
echo ===============================================================================

if not exist bin\nexus\NexusApplication.class (
    echo [INFO] Binaries not found. Performing full compilation...
    if not exist bin mkdir bin
    dir /s /b src\*.java > sources.txt
    javac -cp "lib/*" -d bin @sources.txt
    set COMPILE_STATUS=%ERRORLEVEL%
    if exist sources.txt del sources.txt
    if !COMPILE_STATUS! NEQ 0 (
        echo [ERROR] Compilation failed with exit code !COMPILE_STATUS!.
        pause
        exit /b !COMPILE_STATUS!
    )
)

echo Starting NEXUS V26.1 Enterprise Platform...
java -cp "bin;lib/*" nexus.NexusApplication
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Application exited with code %ERRORLEVEL%.
    pause
    exit /b %ERRORLEVEL%
)
