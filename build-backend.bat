@echo off
chcp 65001 >nul
echo ========================================================
echo        ERP SYSTEM - BUILD SPRING BOOT BACKEND JAR
echo ========================================================
echo.
cd /d "%~dp0erp-backend"

echo [1/2] Dang dong goi Maven (Clean & Package)...
call mvnw.cmd clean package -DskipTests

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================================
    echo  [THANH CONG] File .jar da duoc tao thanh cong tai:
    echo  target\erp-backend-0.0.1-SNAPSHOT.jar
    echo ========================================================
    echo.
    echo Dang mo thu muc chua file .jar de ban keo tha len Botkeep...
    explorer "%~dp0erp-backend\target"
) else (
    echo.
    echo [THAT BAI] Co loi xay ra trong qua trinh build.
)
pause

