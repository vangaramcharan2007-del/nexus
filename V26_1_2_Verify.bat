@echo off
setlocal enabledelayedexpansion
title NEXUS V26.1.2 Review 3 Master Verification
cd /d %~dp0

echo ===============================================================================
echo        NEXUS V26.1.2 - CLEAN BUILD AND RELEASE VERIFICATION RUNNER
echo ===============================================================================

echo [1/3] Wiping build directory for pristine compilation...
if exist bin rmdir /s /q bin
mkdir bin

echo [2/3] Discovering and compiling all NEXUS V26.1.2 source files...
dir /s /b src\*.java > sources.txt
javac -cp "lib/*" -d bin @sources.txt
set COMPILE_STATUS=%ERRORLEVEL%
if exist sources.txt del sources.txt

if %COMPILE_STATUS% NEQ 0 (
    echo [ERROR] Clean compilation failed with exit code %COMPILE_STATUS%.
    exit /b %COMPILE_STATUS%
)

echo [3/3] Executing 17-Factor Release Candidate Verification Suite...
java -cp "bin;lib/*" nexus.V26_1_2_IntegrationVerificationRunner
set TEST_STATUS=%ERRORLEVEL%

if %TEST_STATUS% NEQ 0 (
    echo [ERROR] Verification suite failed with exit code %TEST_STATUS%.
    exit /b %TEST_STATUS%
)

echo ===============================================================================
echo [SUCCESS] All 17 V26.1.2 release candidate criteria verified successfully!
echo ===============================================================================
exit /b 0
