@echo off
if not exist out mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
if errorlevel 1 (
    echo COMPILATION FAILED
    pause
) else (
    echo COMPILATION OK - Starting app...
    java -cp out com.finanzas.Main
)
