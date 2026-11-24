@echo off
cd /d "%~dp0"
where mvn >nul 2>nul
if errorlevel 1 (
  call mvnw.cmd javafx:run
) else (
  mvn javafx:run
)
