@echo off
rem Double-click to open the Shot Table Editor in your browser. Close this window to stop it.
cd /d "%~dp0"
python server.py
if errorlevel 1 pause
