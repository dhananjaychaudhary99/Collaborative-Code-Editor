@echo off
title Collaborative Code Editor - Cache Purge ^& Reset
color 0C
cls

echo ========================================================
echo   Collaborative Code Editor: 30-Day Cache Purge
echo ========================================================
echo.

:: 1. Running Java processes ko terminate karein taaki file locks release ho jayein
echo [1/3] Terminating active editor and server instances...
taskkill /f /im java.exe >nul 2>&1
if %errorlevel% equ 0 (
    echo       - Active Java processes stopped.
) else (
    echo       - No active Java processes found.
)
echo.

:: 2. Local data cache files ko purge karein
echo [2/3] Purging local session cache from 'data/' folder...
set DATA_DIR=%~dp0data

if exist "%DATA_DIR%\saved_code.txt" (
    del /f /q "%DATA_DIR%\saved_code.txt"
    echo       - Removed saved_code.txt
)
if exist "%DATA_DIR%\saved_meta.properties" (
    del /f /q "%DATA_DIR%\saved_meta.properties"
    echo       - Removed saved_meta.properties
)

:: Agar koi temporary storage file ya crash dump bacha ho
if exist "%DATA_DIR%\*.dat" del /f /q "%DATA_DIR%\*.dat" >nul 2>&1

echo.
echo [3/3] Verifying clean state...
if not exist "%DATA_DIR%\saved_code.txt" (
    color 0A
    echo       [SUCCESS] Local cache successfully purged!
) else (
    echo       [WARNING] Could not delete all files. Please check file permissions.
)

echo.
echo ========================================================
echo   Editor state is fully reset.
echo   Press any key to launch a fresh session with start.bat,
echo   or close this window.
echo ========================================================
pause >nul

if exist "%~dp0start.bat" (
    call "%~dp0start.bat"
)