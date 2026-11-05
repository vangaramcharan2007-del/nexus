@echo off
title NEXUS V10 & Academic Review Workspace Launcher
color 0A

echo =====================================================================
echo   SRM B.Tech CSE - NEXUS V10 & Academic Review Workspace Launcher
echo   Candidate : Ram Charan Vanga (RA2511027010164)
echo   Target    : Review 1, Review 2, Review 3 & Viva All-In-One Launcher
echo =====================================================================
echo.

echo [1/4] Launching NEXUS V10.0 Core Application...
start "NEXUS V10 Core Platform" cmd /c "cd /d %~dp0 && Launch_V10_Core.bat"

echo [2/4] Opening Rubric Project Folders in Windows Explorer...
start "" explorer.exe "%~dp0src\nexus"
start "" explorer.exe "%~dp0src\nexus\database"
start "" explorer.exe "%~dp0src\nexus\common\exceptions"

echo [3/4] Opening Core Architecture & Rubric Source Files...
where code >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       Detected VS Code - Opening Project Workspace & Key Rubric Source Files...
    start "" code "%~dp0." "%~dp0src\nexus\NexusApplication.java" "%~dp0src\nexus\database\DatabaseService.java" "%~dp0src\nexus\database\IRepository.java" "%~dp0src\nexus\common\exceptions\NexusException.java" "%~dp0src\nexus\Review2VerificationRunner.java"
) else (
    echo       Opening Source Files in Default Editor...
    start "" "%~dp0src\nexus\NexusApplication.java"
    start "" "%~dp0src\nexus\database\DatabaseService.java"
    start "" "%~dp0src\nexus\database\IRepository.java"
    start "" "%~dp0src\nexus\common\exceptions\NexusException.java"
    start "" "%~dp0src\nexus\Review2VerificationRunner.java"
)

echo [4/4] Opening Academic Deliverables (Set A PDF & Word Report)...
if exist "%USERPROFILE%\Desktop\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.pdf" (
    start "" "%USERPROFILE%\Desktop\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.pdf"
)
if exist "%USERPROFILE%\Desktop\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.docx" (
    start "" "%USERPROFILE%\Desktop\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.docx"
)
if exist "%USERPROFILE%\Desktop\NEXUS_Presentation_Script.md" (
    start "" "%USERPROFILE%\Desktop\NEXUS_Presentation_Script.md"
)

echo.
echo =====================================================================
echo  [SUCCESS] All application windows, folders, and files opened at once!
echo =====================================================================
timeout /t 3 >nul
exit
