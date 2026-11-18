@echo off
setlocal
cd /d "%~dp0"

if not exist "bin\nexus\test\NexusProductionTestSuite.class" (
    echo Building sources before test execution...
    call build.bat
    if errorlevel 1 exit /b 1
)

echo Running NEXUS Automated Test Suite...
java -cp "bin;lib\flatlaf.jar;lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar;lib\jsoup.jar" nexus.test.NexusProductionTestSuite
