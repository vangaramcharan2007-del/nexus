@echo off
setlocal enabledelayedexpansion

title NEXUS V26.1.2 Review 3 - Full Evaluation Workspace Launcher
color 0B
cd /d "%~dp0"

echo ===============================================================================
echo     SRM B.Tech CSE - NEXUS V26.1.2 REVIEW 3 MASTER EVALUATION WORKSPACE
echo       Candidate : Ram Charan Vanga (Reg No: RA2511027010164)
echo       Target    : Review 3 (Software Application Development - 20 Marks)
echo ===============================================================================
echo.

set "PROJ=%~dp0"
if "%PROJ:~-1%"=="\" set "PROJ=%PROJ:~0,-1%"

echo [1/4] Launching NEXUS V26.1.2 Enterprise Platform (19 Production Tabs)...
start "NEXUS Platform" javaw -XX:+UseG1GC -Xms256m -Xmx1024m -cp "bin;lib/*" -Dfile.encoding=UTF-8 nexus.Main

echo.
echo [2/4] Opening All Review 3 Architecture and Implementation Files in VS Code...
set "CODE_EXE=C:\Users\vanga\AppData\Local\Programs\Microsoft VS Code\Code.exe"
if not exist "%CODE_EXE%" set "CODE_EXE=Code.exe"

start "" "%CODE_EXE%" "%PROJ%" ^
    "%PROJ%\NEXUS_REVIEW3_FINAL_RELEASE_REPORT.md" ^
    "%PROJ%\src\nexus\NexusApplication.java" ^
    "%PROJ%\src\nexus\Review3_LiveDemonstration.java" ^
    "%PROJ%\src\nexus\V26_1_2_IntegrationVerificationRunner.java" ^
    "%PROJ%\src\nexus\database\DatabaseService.java" ^
    "%PROJ%\src\nexus\common\EventBus.java" ^
    "%PROJ%\src\nexus\finance\FinanceService.java" ^
    "%PROJ%\src\nexus\market\MarketService.java" ^
    "%PROJ%\src\nexus\rules\RuleEngineService.java" ^
    "%PROJ%\src\nexus\vault\VaultService.java" ^
    "%PROJ%\src\nexus\tenant\TenantService.java" ^
    "%PROJ%\src\nexus\integrations\notifications\NtfyNotificationService.java" ^
    "%PROJ%\src\nexus\integrations\analytics\MetabaseAnalyticsService.java" ^
    "%PROJ%\src\nexus\media\CinemaBooking.java" ^
    "%PROJ%\src\nexus\media\LiveDistrictCinemaService.java" ^
    "%PROJ%\src\nexus\Main.java"

echo.
echo [3/4] Opening Architecture and Subsystem Directories in Windows Explorer...
start "" explorer.exe "%PROJ%\src\nexus"
start "" explorer.exe "%PROJ%\src\nexus\database"
start "" explorer.exe "%PROJ%\src\nexus\finance"
start "" explorer.exe "%PROJ%\src\nexus\integrations"

echo.
echo [4/4] Locating Academic Deliverables and Verification Proofs...
if exist "%PROJ%\NEXUS_REVIEW3_FINAL_RELEASE_REPORT.md" (
    echo   [OK] Found Master Review 3 Report: %PROJ%\NEXUS_REVIEW3_FINAL_RELEASE_REPORT.md
)

echo.
echo ===============================================================================
echo   [SUCCESS] All Review 3 Architecture Files Loaded in VS Code Tabs!
echo   NEXUS V26.1.2 Enterprise Platform is Active.
echo ===============================================================================
timeout /t 3 >nul
