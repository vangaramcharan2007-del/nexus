@echo off
cd /d "%~dp0"
java -cp "bin;lib/flatlaf.jar;lib/sqlite-jdbc.jar" nexus.Main
pause
