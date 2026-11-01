@echo off
setlocal
if defined JAVA_HOME set "PATH=%JAVA_HOME%\bin;%PATH%"
where javac >nul 2>nul
if errorlevel 1 (
    echo Java 25 JDK not found. Set JAVA_HOME or add Java 25 to PATH.
    exit /b 1
)
for /f "tokens=2" %%v in ('javac -version 2^>^&1') do set "JAVA_VERSION=%%v"
for /f "tokens=1 delims=." %%v in ("%JAVA_VERSION%") do set "JAVA_MAJOR=%%v"
if not defined JAVA_MAJOR (
    echo Unable to determine the Java compiler version.
    exit /b 1
)
if not "%JAVA_MAJOR%"=="25" (
    echo Java 25 is required. Found Java %JAVA_VERSION%.
    exit /b 1
)
if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac --release 25 -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo COMPILATION FAILED
    pause
    exit /b 1
) else (
    echo COMPILATION OK - Starting app...
    java -cp out com.finanzas.Main
)
