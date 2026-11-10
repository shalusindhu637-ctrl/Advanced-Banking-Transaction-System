@echo off
setlocal enabledelayedexpansion

echo ========================================
echo Banking Transaction System Setup
echo ========================================
echo.

REM Set paths - UPDATE IF NEEDED
set "TOMCAT_HOME=C:\apache-tomcat-10.1.39"
set "PROJECT_DIR=C:\Banking-Transaction-System"
set "MYSQL_CONNECTOR=mysql-connector-j-8.4.0.jar"

echo [1/6] Checking prerequisites...
if not exist "%TOMCAT_HOME%" (
    echo ERROR: Tomcat not found at %TOMCAT_HOME%
    echo Please update TOMCAT_HOME in this file
    pause
    exit /b 1
)
echo OK: Tomcat found

if not exist "%PROJECT_DIR%\WebContent\WEB-INF\lib\%MYSQL_CONNECTOR%" (
    echo ERROR: MySQL Connector not found: %PROJECT_DIR%\WebContent\WEB-INF\lib\%MYSQL_CONNECTOR%
    echo Please copy mysql-connector-j-8.4.x.jar to WebContent\WEB-INF\lib\
    pause
    exit /b 1
)
echo OK: MySQL Connector found

echo.
echo [2/6] Creating classes directory...
cd "%PROJECT_DIR%"
if not exist "WebContent\WEB-INF\classes" mkdir "WebContent\WEB-INF\classes"
echo OK

echo.
echo [3/6] Compiling Java sources...
javac -encoding UTF-8 -classpath "%TOMCAT_HOME%\lib\jakarta.servlet-api.jar;WebContent\WEB-INF\lib\%MYSQL_CONNECTOR%" -sourcepath src -d WebContent\WEB-INF\classes src\com\banking\model\*.java src\com\banking\util\*.java src\com\banking\dao\*.java src\com\banking\filter\*.java src\com\banking\servlet\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)
echo OK: Compilation successful

echo.
echo [4/6] Deploying to Tomcat...
if exist "%TOMCAT_HOME%\webapps\Banking-Transaction-System" (
    echo Removing old deployment...
    rmdir /s /q "%TOMCAT_HOME%\webapps\Banking-Transaction-System"
)
xcopy /E /I /Y "WebContent" "%TOMCAT_HOME%\webapps\Banking-Transaction-System\"
echo OK: Deployed

echo.
echo [5/6] Starting Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"

echo.
echo [6/6] Done!
echo.
echo Application URL: http://localhost:8080/Banking-Transaction-System/
echo.
echo Press any key to exit...
pause >nul
