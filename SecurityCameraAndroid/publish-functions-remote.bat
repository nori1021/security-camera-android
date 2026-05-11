@echo off
chcp 65001 >nul
setlocal

REM Personal deploy helper (gitignored template: publish-functions-remote.bat.example).
REM Set FUNCTION_APP_NAME to your Azure Function App before running.

set "FUNCTION_APP_NAME=YOUR_FUNCTION_APP_NAME"
set "PROJECT_DIR=%~dp0..\securitycam-functions"

if "%FUNCTION_APP_NAME%"=="YOUR_FUNCTION_APP_NAME" (
  echo [ERROR] Edit FUNCTION_APP_NAME or copy from publish-functions-remote.bat.example
  pause
  exit /b 1
)

if not exist "%PROJECT_DIR%\package.json" (
  echo [ERROR] Functions project not found: %PROJECT_DIR%
  pause
  exit /b 1
)

cd /d "%PROJECT_DIR%"
if errorlevel 1 (
  echo [ERROR] Cannot cd to: %PROJECT_DIR%
  pause
  exit /b 1
)

echo Publishing %FUNCTION_APP_NAME% with --build remote ...
func azure functionapp publish "%FUNCTION_APP_NAME%" --build remote

if errorlevel 1 (
  echo [FAILED]
  pause
  exit /b 1
)

echo [OK]
pause
exit /b 0
