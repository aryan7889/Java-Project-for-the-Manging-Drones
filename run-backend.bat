@echo off
echo ===================================================
echo   Starting RescueDrone Java REST Backend Server
echo ===================================================
cd /d "%~dp0"

if not exist "backend\bin" mkdir backend\bin
if not exist "backend\data" mkdir backend\data

echo Compiling Java source files...
dir /s /b backend\src\*.java > sources.txt
javac -d backend\bin @sources.txt
del sources.txt

if %errorlevel% neq 0 (
    echo [ERROR] Java compilation failed!
    pause
    exit /b %errorlevel%
)

echo Starting RescueDrone server on port 8080...
java -Djava.net.preferIPv4Stack=true -cp backend\bin org.rescuedrone.Main 8080
pause
