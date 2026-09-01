@echo off
setlocal

set "BASE_DIR=%~dp0"
set "MAVEN_HOME=%BASE_DIR%.mvn\wrapper\dists\apache-maven-3.9.16"

if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%BASE_DIR%.mvn\wrapper\maven-wrapper.ps1"
  if errorlevel 1 exit /b %errorlevel%
)

call "%MAVEN_HOME%\bin\mvn.cmd" %*
exit /b %errorlevel%
