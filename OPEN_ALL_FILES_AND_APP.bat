@echo off
title NEXUS V11 and SRM Evaluation Workspace Launcher
color 0A

echo =====================================================================
echo   SRM B.Tech CSE - NEXUS V11 Evaluation Workspace Launcher
echo   Candidate : Ram Charan Vanga (RA2511027010164)
echo   Rubric    : Review 1, Review 2, Review 3, Report and Viva
echo =====================================================================
echo.

set "PROJ=C:\Users\vanga\Desktop\NEXUS_Java_V7_Startup_Edition"
set "DESK=C:\Users\vanga\Desktop"

echo [1/4] Launching NEXUS V11.0 Real-Time Cinema Application...
start "NEXUS V11 Platform" "%PROJ%\Launch_V11_Realtime.bat"

echo [2/4] Opening All Rubric Architecture Files in VS Code Tabs...
set "CODE_PATH=C:\Users\vanga\AppData\Local\Programs\Microsoft VS Code\bin\code.cmd"
if not exist "%CODE_PATH%" set "CODE_PATH=code"

start "" "%CODE_PATH%" "%PROJ%" ^
    "%PROJ%\src\nexus\Main.java" ^
    "%PROJ%\src\nexus\NexusApplication.java" ^
    "%PROJ%\src\nexus\database\DatabaseService.java" ^
    "%PROJ%\src\nexus\tasks\JdbcTaskRepository.java" ^
    "%PROJ%\src\nexus\finance\JdbcTransactionRepository.java" ^
    "%PROJ%\src\nexus\database\IRepository.java" ^
    "%PROJ%\src\nexus\tasks\ITaskRepository.java" ^
    "%PROJ%\src\nexus\finance\IFinanceEngine.java" ^
    "%PROJ%\src\nexus\media\ICinemaGateway.java" ^
    "%PROJ%\src\nexus\common\exceptions\NexusException.java" ^
    "%PROJ%\src\nexus\common\exceptions\InsufficientFundsException.java" ^
    "%PROJ%\src\nexus\common\exceptions\SeatAlreadyBookedException.java" ^
    "%PROJ%\src\nexus\realtime\BookMyShowScraper.java" ^
    "%PROJ%\src\nexus\Review2VerificationRunner.java" ^
    "%PROJ%\src\nexus\media\LiveDistrictCinemaService.java" ^
    "%PROJ%\src\nexus\media\MediaPanel.java" ^
    "%PROJ%\src\nexus\media\InAppCinemaPlayerDialog.java" ^
    "%PROJ%\src\nexus\analytics\NexusScore.java" ^
    "%DESK%\NEXUS_Presentation_Script.md" ^
    "%DESK%\NEXUS_QnA_CheatSheet.md" ^
    "%DESK%\NEXUS_Master_Documentation.md"

echo [3/4] Opening Rubric Project Folders in Windows Explorer...
start "" explorer.exe "%PROJ%\src\nexus"
start "" explorer.exe "%PROJ%\src\nexus\database"
start "" explorer.exe "%PROJ%\src\nexus\common\exceptions"

echo [4/4] Opening Academic Deliverables (Set A PDF Report)...
if exist "%DESK%\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.pdf" (
    start "" "%DESK%\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.pdf"
)
if exist "%DESK%\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.docx" (
    start "" "%DESK%\SET_A_Data_Visualization_Ram_Charan_Vanga_RA2511027010164.docx"
)

echo.
echo =====================================================================
echo  [SUCCESS] All rubric files opened in VS Code and NEXUS V11 is active!
echo =====================================================================
ping 127.0.0.1 -n 3 >nul
exit
