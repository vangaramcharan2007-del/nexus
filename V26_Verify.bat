@echo off
title NEXUS V26 Master Verification Runner
cd /d %~dp0
echo Executing 12-Factor Automated V26 Verification Suite...
java -cp "bin;lib/*" nexus.V26IntegrationVerificationRunner
pause
