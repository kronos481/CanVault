@echo off
title CANVAULT Android Installation
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0INSTALL_CANVAULT.ps1"
if errorlevel 1 (
  echo.
  echo Installation nicht abgeschlossen. Bitte die Fehlermeldung oben pruefen.
  pause
)
