@echo off
title ERP Frontend Dev Server
echo ==========================================
echo    Starting ERP Frontend (Vite Dev)...
echo ==========================================
cd /d "%~dp0erp-frontend"
npm run dev
pause

