@echo off
title ERP Backend Spring Boot
echo ==========================================
echo    Starting ERP Backend (Spring Boot)...
echo ==========================================
cd /d "%~dp0erp-backend"
call .\mvnw.cmd spring-boot:run
pause

