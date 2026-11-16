@echo off
setlocal enabledelayedexpansion
title NEXUS V26.1.1 Master Reliability and Integration Verification
cd /d %~dp0

echo ===============================================================================
echo        NEXUS V26.1.1 - CLEAN BUILD AND VERIFICATION RUNNER
echo ===============================================================================

if not exist bin mkdir bin

echo [1/2] Discovering and compiling all NEXUS V26.1.1 source files...
dir /s /b src\*.java > sources.txt
javac -cp "lib/*" -d bin @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%
if exist sources.txt del sources.txt

if %COMPILE_STATUS% NEQ 0 (
    echo [ERROR] Compilation failed with exit code %COMPILE_STATUS%.
    exit /b %COMPILE_STATUS%
)

echo [2/2] Executing 16-Factor Verification Suite...
java -cp "bin;lib/*" nexus.V26_1_1_IntegrationVerificationRunner
set TEST_STATUS=%ERRORLEVEL%

if %TEST_STATUS% NEQ 0 (
    echo [ERROR] Verification suite failed with exit code %TEST_STATUS%.
    exit /b %TEST_STATUS%
)

echo ===============================================================================
echo [SUCCESS] All 16 V26.1.1 reliability and integration assertions verified!
echo ===============================================================================
exit /b 0
