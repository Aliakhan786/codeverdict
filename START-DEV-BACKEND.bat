@echo off
title CodeVerdict backend (dev)
cd /d "%~dp0"
rem Uses the JDK and Maven that are already on this laptop.
set "JAVA_HOME=%USERPROFILE%\.jdks\openjdk-24.0.1"
set "PATH=%JAVA_HOME%\bin;%USERPROFILE%\tools\apache-maven-3.9.16\bin;%PATH%"
if not exist "%JAVA_HOME%\bin\java.exe" ( echo JDK not found at %JAVA_HOME% & pause & exit /b )
docker compose up -d postgres
cd backend
call mvn -q generate-resources
call mvn spring-boot:run
pause
