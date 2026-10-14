@echo off
title Collaborative Code Editor - Stopper
echo Stopping all Collaborative Code Editor instances...

:: 1. Kill java process running the JAR / editor
taskkill /f /im java.exe >nul 2>&1

:: 2. Close launched CMD windows for Server and Peers
taskkill /f /fi "WINDOWTITLE eq Collab-Server" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq Peer-1" >nul 2>&1
taskkill /f /fi "WINDOWTITLE eq Peer-2" >nul 2>&1

echo All editor windows, peers, and background server processes stopped cleanly.
timeout /t 2 /nobreak >nul
