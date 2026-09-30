@echo off
cd /d "%~dp0"
java -cp "bin;lib/flatlaf.jar" nexus.Main
pause
