@echo off
setlocal
title NEXUS Enterprise Platform - Review 3 Master Edition
color 0B

echo ===============================================================================
echo     NEXUS PLATFORM - V25.0 FINAL MASTER EDITION (REVIEW 3 EVALUATION)
echo     Course    : Advanced Object Oriented Programming (21CSC206P) - Slot D
echo     Department: Data Science and Business Systems (DSBS), SRMIST Kattankulathur
echo     Team (AR2): Bala (RA2511027010120) ^| Vaibhav (RA2511027010133) ^| Ram Charan (RA2511027010164)
echo     Guide     : Dr. S. Venkatesh
echo     Evaluation: Review 3 (Software Application Development - 20 Marks)
echo ===============================================================================
echo.

cd /d "C:\Users\vanga\Documents\nexus-temp"

echo [*] Team Members : Bala, Vaibhav, Ram Charan (Equal 33.33%% Parity)
echo [*] Subsystems   : 17 Enterprise Modules (Cinema, Finance, Pass, Test Harness)
echo [*] Environment  : Java JDK 21 ^| FlatDarkLaf 3.5 ^| SQLite 3.45 WAL
echo.
echo [*] Launching NEXUS V25.0 Desktop GUI...

start "NEXUS Platform" javaw -XX:+UseG1GC -Xms256m -Xmx1024m -cp "bin;lib/*" nexus.Main

echo.
echo ===============================================================================
echo  [SUCCESS] Application started in dedicated window.
echo ===============================================================================
ping 127.0.0.1 -n 3 >nul
exit
