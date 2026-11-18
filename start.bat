@echo off
setlocal
cd /d "%~dp0"

if not exist "bin\nexus\Main.class" (
    echo bin\ directory not found or not compiled. Running build first...
    call build.bat
    if errorlevel 1 exit /b 1
)

echo Starting NEXUS Platform...
java -cp "bin;lib\flatlaf.jar;lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar;lib\jsoup.jar" nexus.Main
