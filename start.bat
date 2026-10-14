@echo off
title Collaborative Code Editor
cd /d "%~dp0"

echo Killing old java processes...
taskkill /f /im java.exe >nul 2>&1
taskkill /f /im javaw.exe >nul 2>&1

if not exist "%~dp0target\collaborative-code-editor-1.0.0.jar" (
    echo [ERROR] JAR file nahi mili! Pehle Maven build karein.
    pause
    exit /b 1
)

echo Starting background server on port 8080...
start "" javaw -jar "%~dp0target\collaborative-code-editor-1.0.0.jar" server 8080

timeout /t 2 /nobreak >nul

echo Starting Collaborative Editor...
start "" javaw -jar "%~dp0target\collaborative-code-editor-1.0.0.jar" client localhost 8080
