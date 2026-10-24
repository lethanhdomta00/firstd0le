@echo off
chcp 65001 >nul
title Botkeep SFTP Uploader - ERP Backend
cd /d "%~dp0tools\sftp-uploader"
node upload.js
pause

