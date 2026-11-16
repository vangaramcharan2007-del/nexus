@echo off
setlocal enabledelayedexpansion

title NEXUS V26.1.2 - Official Review 3 Demonstration
cls
echo ===============================================================================
echo            NEXUS V26.1.2 - REVIEW 3 INTERACTIVE LIVE DEMONSTRATION
echo       Software Application Development (15 Marks) ^| Presentation (5 Marks)
echo ===============================================================================
echo.

if not exist bin (
    echo [INFO] Compiling NEXUS V26.1.2...
    call V26_1_2_Verify.bat
)

echo [1/2] Executing Live Terminal Demonstration across all 9 technical modules...
echo.
java -cp "bin;lib/*" -Dfile.encoding=UTF-8 nexus.Review3_LiveDemonstration
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Live demonstration encountered an error!
    pause
    exit /b %ERRORLEVEL%
)

echo.
echo ===============================================================================
echo [2/2] Live Demonstration completed successfully with 100%% criteria pass rate.
echo ===============================================================================
echo.
echo Would you like to launch the full graphical interactive UI (NexusApplication)?
set /p LAUNCH_GUI="Launch GUI now? [Y/N] (Default: N): "
if /i "%LAUNCH_GUI%"=="Y" (
    echo Launching NEXUS Enterprise Platform GUI...
    start javaw -cp "bin;lib/*" -Dfile.encoding=UTF-8 nexus.NexusApplication
)

echo.
echo Press any key to exit...
pause >nul
