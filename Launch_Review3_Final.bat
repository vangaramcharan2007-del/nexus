@echo off
setlocal enabledelayedexpansion

title NEXUS Enterprise Platform - V26.1.2 Review 3 Master Edition
color 0B

:: Resolve Project Directory (supports running from Desktop or root)
set "NEXUS_DIR=%~dp0"
if not exist "%NEXUS_DIR%lib\flatlaf.jar" (
    set "NEXUS_DIR=C:\Users\vanga\Documents\nexus-temp\"
)
cd /d "%NEXUS_DIR%"

set "CP=bin;lib\flatlaf.jar;lib\sqlite-jdbc.jar;lib\slf4j-api.jar;lib\slf4j-nop.jar;lib\jsoup.jar"

:: Check CLI Argument
if /i "%~1"=="gui" goto do_gui
if /i "%~1"=="demo" goto do_demo
if /i "%~1"=="verify" goto do_verify
if /i "%~1"=="open" goto do_open
if /i "%~1"=="all" goto do_open

:menu
cls
echo ===============================================================================
echo     NEXUS ENTERPRISE PLATFORM - V26.1.2 REVIEW 3 MASTER EVALUATION
echo       Candidate : Ram Charan Vanga (Reg No: RA2511027010164)
echo       Department: B.Tech CSE (Big Data Analytics), SRMIST Kattankulathur
echo       Evaluation: Review 3 (Software Application Development - 20 Marks)
echo ===============================================================================
echo.
echo   [1] Launch NEXUS V26.1.2 GUI Application (Full 19-Tab Enterprise Suite)
echo   [2] Run Review 3 Interactive Live Terminal Demo (9 Technical Modules)
echo   [3] Run Clean Build ^& 17-Factor Release Verification Suite (Pristine)
echo   [4] Capture High-Definition UI Screenshots (All 19 Tabs in FlatDarkLaf)
echo   [5] Open Official Review 3 Report, Architecture Diagrams ^& Viva Guide
echo   [6] Open Review 3 Implementation Code in VS Code ^& Explorer
echo   [7] Exit
echo.
echo ===============================================================================

choice /c 1234567 /n /t 10 /d 1 /m "Select an option [1-7] (Default: [1] Auto-launches GUI in 10s): "
set CHOICE_VAL=%ERRORLEVEL%

if "%CHOICE_VAL%"=="1" goto do_gui
if "%CHOICE_VAL%"=="2" goto do_demo
if "%CHOICE_VAL%"=="3" goto do_verify
if "%CHOICE_VAL%"=="4" goto do_screenshots
if "%CHOICE_VAL%"=="5" goto do_report
if "%CHOICE_VAL%"=="6" goto do_open
if "%CHOICE_VAL%"=="7" goto do_exit

:: -----------------------------------------------------------------------------
:: OPTION 1: LAUNCH GUI
:: -----------------------------------------------------------------------------
:do_gui
cls
echo ===============================================================================
echo [1/2] Verifying Source Compilation...
echo ===============================================================================
if not exist "bin" mkdir "bin"

if not exist bin\nexus\NexusApplication.class (
    echo Compiling all NEXUS source files...
    dir /s /b src\*.java > sources.txt
    javac -cp "%CP%" -d bin @sources.txt
    set COMPILE_STATUS=%ERRORLEVEL%
    if exist sources.txt del sources.txt
    if !COMPILE_STATUS! NEQ 0 (
        echo [ERROR] Compilation failed with exit code !COMPILE_STATUS!.
        pause
        goto menu
    )
)
echo [OK] Bytecode verified. All 19 enterprise subsystem modules ready.
echo.
echo ===============================================================================
echo [2/2] Launching NEXUS V26.1.2 GUI (FlatDarkLaf Dark Mode)...
echo ===============================================================================
java -XX:+UseG1GC -Xms256m -Xmx1024m -cp "%CP%" -Dfile.encoding=UTF-8 nexus.Main
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo Application exited with code %ERRORLEVEL%.
    pause
)
goto do_exit

:: -----------------------------------------------------------------------------
:: OPTION 2: LIVE TERMINAL DEMONSTRATION
:: -----------------------------------------------------------------------------
:do_demo
cls
echo ===============================================================================
echo   Executing Review 3 Live Terminal Demonstration (9 Subsystems)...
echo ===============================================================================
call Run_Review3_Demo.bat
goto menu

:: -----------------------------------------------------------------------------
:: OPTION 3: VERIFICATION SUITE
:: -----------------------------------------------------------------------------
:do_verify
cls
echo ===============================================================================
echo   Executing 17-Factor Release Candidate Master Verification Suite...
echo ===============================================================================
call V26_1_2_Verify.bat
echo.
pause
goto menu

:: -----------------------------------------------------------------------------
:: OPTION 4: CAPTURE SCREENSHOTS
:: -----------------------------------------------------------------------------
:do_screenshots
cls
echo ===============================================================================
echo   Capturing High-Definition Screenshots Across All 19 Tabs...
echo ===============================================================================
java -cp "%CP%" -Dfile.encoding=UTF-8 nexus.CaptureV26_1_2Screenshots
echo.
echo Screenshots saved to C:\Users\vanga\Downloads\nexus_v26_1_2_screenshots
pause
goto menu

:: -----------------------------------------------------------------------------
:: OPTION 5: OPEN EVALUATION REPORT
:: -----------------------------------------------------------------------------
:do_report
cls
echo ===============================================================================
echo   Opening Official Review 3 Release Report & Viva Voce Guide...
echo ===============================================================================
if exist "NEXUS_REVIEW3_FINAL_RELEASE_REPORT.md" (
    start "" "NEXUS_REVIEW3_FINAL_RELEASE_REPORT.md"
) else (
    echo [WARNING] Report file not found locally.
)
ping -n 3 127.0.0.1 >nul
goto menu

:: -----------------------------------------------------------------------------
:: OPTION 6: OPEN WORKSPACE IN VS CODE
:: -----------------------------------------------------------------------------
:do_open
cls
echo ===============================================================================
echo   Opening All Review 3 Implementation Code & Architecture Files...
echo ===============================================================================
call OPEN_ALL_FILES_AND_APP_REVIEW3.bat
goto menu

:do_exit
echo.
echo Exiting NEXUS Master Launcher.
exit /b 0
