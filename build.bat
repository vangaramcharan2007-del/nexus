@echo off
setlocal
cd /d "%~dp0"

echo ===================================================
echo   Building NEXUS Platform (Production Core Edition)
echo ===================================================

if not exist bin mkdir bin

set "TEMP_SOURCES=%TEMP%\nexus_sources_%RANDOM%.txt"
dir /s /b src\*.java > "%TEMP_SOURCES%"

echo Compiling Java sources with JDK 21...
javac -encoding UTF-8 -cp "lib\flatlaf.jar;lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar;lib\jsoup.jar" -d bin @"%TEMP_SOURCES%"
set "ERR=%ERRORLEVEL%"
del "%TEMP_SOURCES%" >nul 2>nul

if %ERR% neq 0 (
    echo.
    echo [ERROR] Build failed with exit code %ERR%.
    exit /b %ERR%
)

echo.
echo [SUCCESS] NEXUS compiled cleanly into bin\
exit /b 0
