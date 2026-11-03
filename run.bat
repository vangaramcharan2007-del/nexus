@echo off
echo ===================================
echo   Starting NEXUS V8 Application...
echo ===================================

java -cp "bin;lib/flatlaf.jar;lib/sqlite-jdbc.jar;lib/slf4j-api.jar;lib/slf4j-nop.jar;lib/jsoup.jar" nexus.Main

pause
