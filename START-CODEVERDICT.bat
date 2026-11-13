@echo off
title CodeVerdict
cd /d "%~dp0"

findstr /r /c:"^GEMINI_API_KEY=..*" .env >nul
if not errorlevel 1 goto haskey
echo.
echo Get a free key at https://aistudio.google.com/apikey
set /p KEY=Paste your Gemini API key here and press Enter: 
powershell -NoProfile -Command "(Get-Content .env) -replace '^GEMINI_API_KEY=.*','GEMINI_API_KEY=%KEY%' | Set-Content .env"
:haskey

docker info >nul 2>&1
if not errorlevel 1 goto dockerok
echo.
echo Starting Docker... please wait (this can take 1-2 minutes)
start "" "C:\Program Files\Docker\Docker\Docker Desktop.exe"
set /a tries=0
:waitdocker
timeout /t 5 /nobreak >nul
docker info >nul 2>&1
if not errorlevel 1 goto dockerok
set /a tries+=1
if %tries% lss 60 goto waitdocker
echo Docker did not start after 5 minutes. Restart your laptop and try again.
pause
exit /b
:dockerok
echo Docker is ready.

echo.
echo Starting CodeVerdict... (first time can take 5-10 minutes)
docker compose up -d --build
if errorlevel 1 (
  echo.
  echo Something went wrong. Take a screenshot of this window and send it to Claude.
  pause
  exit /b
)
echo.
echo Waiting for the app to be ready...
powershell -NoProfile -Command "for ($i=0; $i -lt 60; $i++) { try { if ((Invoke-WebRequest http://localhost:3001/api/v1/health -UseBasicParsing -TimeoutSec 3).StatusCode -eq 200) { exit 0 } } catch {}; Start-Sleep 3 }; exit 1"
if errorlevel 1 (
  echo The app did not answer. Showing the last log lines:
  docker compose logs --tail 40 backend
  pause
  exit /b
)
start "" http://localhost:3001
echo.
echo ============================================
echo  CodeVerdict is running: http://localhost:3001
echo  Login: demo / demo1234
echo  You can close this window - the app keeps running.
echo  To stop it: double-click STOP-CODEVERDICT.bat
echo ============================================
pause
