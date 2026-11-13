@echo off
title CodeVerdict frontend (dev)
cd /d "%~dp0frontend"
if not exist node_modules call npm install
start "" http://localhost:5174
call npm run dev
pause
