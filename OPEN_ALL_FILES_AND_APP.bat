@echo off
title NEXUS V10 Plus Academic Review Workspace Launcher
color 0A

echo =====================================================================
echo   SRM B.Tech CSE - NEXUS V10 Review Workspace Launcher
echo   Candidate : Ram Charan Vanga (RA2511027010164)
echo   Target    : Review 1, Review 2, Review 3 and Viva All-In-One Launcher
echo =====================================================================
echo.

echo [1/4] Launching NEXUS V10.0 Core Application...
start "NEXUS V10 Core Platform" cmd /k "cd /d C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition && call Launch_V10_Core.bat"

echo [2/4] Opening Rubric Project Folders in Windows Explorer...
start "" explorer.exe "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus"
start "" explorer.exe "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\database"
start "" explorer.exe "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\common\exceptions"

echo [3/4] Opening Core Architecture and Rubric Source Files...
where code >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo       Detected VS Code - Opening Workspace and Key Source Files...
    start "" code "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\NexusApplication.java" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\database\DatabaseService.java" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\database\IRepository.java" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\common\exceptions\NexusException.java" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\Review2VerificationRunner.java"
) else (
    echo       Opening Source Files in Default Editor...
    start "" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\NexusApplication.java"
    start "" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\database\DatabaseService.java"
    start "" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\database\IRepository.java"
    start "" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\common\exceptions\NexusException.java"
    start "" "C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition\src\nexus\Review2VerificationRunner.java"
)

echo [4/4] Opening Academic Deliverables (Set A PDF and Word Report)...
if exist "C:\Users\vanga\Desktop\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.pdf" (
    start "" "C:\Users\vanga\Desktop\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.pdf"
)
if exist "C:\Users\vanga\Desktop\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.docx" (
    start "" "C:\Users\vanga\Desktop\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.docx"
)
if exist "C:\Users\vanga\Desktop\NEXUS_Presentation_Script.md" (
    start "" "C:\Users\vanga\Desktop\NEXUS_Presentation_Script.md"
)

echo.
echo =====================================================================
echo  [SUCCESS] All application windows, folders, and files opened at once!
echo =====================================================================
ping 127.0.0.1 -n 3 >nul
exit
