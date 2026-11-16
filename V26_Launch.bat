@echo off
title NEXUS Enterprise Platform - V26.0 External Integration Edition
cd /d %~dp0
echo Starting NEXUS V26.0 Enterprise Platform...
java -cp "bin;lib/*" nexus.NexusApplication
pause
