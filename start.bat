@echo off
chcp 65001 >nul
cd /d "%~dp0"
title Bank Account System

echo ============================================================
echo   Bank Account System - Local Run
echo ============================================================
echo.

where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found in PATH. Please install JDK 17+.
    pause & exit /b 1
)

where mvn >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven not found in PATH. Please install Maven 3.8+.
    pause & exit /b 1
)

echo [1/2] Checking MySQL (localhost:3306)...
if not exist "%TEMP%\mysql_check.txt" echo select 1; | mysql -u root -h localhost -P 3306 > "%TEMP%\mysql_check.txt" 2>&1
type "%TEMP%\mysql_check.txt" | findstr "ERROR" >nul && (
    echo   [WARN] MySQL not reachable. Start it first or set DB_PASSWORD env.
) || echo   MySQL OK

echo [2/2] Starting Spring Boot...
echo.
echo   Access: http://localhost:8080/login.html
echo   Docs:   http://localhost:8080/doc.html
echo.

call mvn spring-boot:run
pause
