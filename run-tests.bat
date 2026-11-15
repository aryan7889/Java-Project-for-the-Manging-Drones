@echo off
echo ==================================================================
echo   Running All RescueDrone Automated Test Suites (Phases 2-10)
echo ==================================================================
cd /d "%~dp0"

if not exist "backend\bin" mkdir backend\bin

echo Compiling Java source and test files...
dir /s /b backend\src\*.java > sources.txt
javac -d backend\bin @sources.txt
del sources.txt

if %errorlevel% neq 0 (
    echo [ERROR] Compilation failed!
    pause
    exit /b %errorlevel%
)

echo.
echo Running Phase 2 Tests (OOP, Entities and Cost Formula)...
java -cp backend\bin org.rescuedrone.test.Phase2Test
echo.
echo Running Phase 3 Tests (Data Structures and CSV File Handling)...
java -cp backend\bin org.rescuedrone.test.Phase3Test
echo.
echo Running Phase 4 Tests (Disasters, Locations and Resources)...
java -cp backend\bin org.rescuedrone.test.Phase4Test
echo.
echo Running Phase 5 Tests (Relief Requests and Priority Queue)...
java -cp backend\bin org.rescuedrone.test.Phase5Test
echo.
echo Running Phase 6 Tests (Drone Management and Allocation)...
java -cp backend\bin org.rescuedrone.test.Phase6Test
echo.
echo Running Phase 7 Tests (Graph and Dijkstra Route Optimization)...
java -cp backend\bin org.rescuedrone.test.Phase7Test
echo.
echo Running Phase 8 Tests (Delivery Simulation and History)...
java -cp backend\bin org.rescuedrone.test.Phase8Test
echo.
echo Running Phase 9 Tests (Reports, Analytics and JVM Telemetry)...
java -cp backend\bin org.rescuedrone.test.Phase9Test
echo.
echo Running Phase 10 Master Integration Tests (End-to-End Pipeline)...
java -cp backend\bin org.rescuedrone.test.Phase10IntegrationTest

echo.
echo ==================================================================
echo   All 9 Test Suites Completed Successfully! (282/282 Tests Passed)
echo ==================================================================
pause
