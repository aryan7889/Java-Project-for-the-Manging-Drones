@echo off
echo ===================================================
echo   Starting RescueDrone React Frontend (Vite)
echo ===================================================
cd /d "%~dp0\frontend"
npm run dev -- --host
pause
