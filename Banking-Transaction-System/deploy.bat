@echo off
echo ========================================
echo Banking Transaction System - Quick Deploy
echo ========================================
echo.

REM Update these paths as needed
set TOMCAT=C:\CTomcat
set PROJECT=C:\Banking-Transaction-System
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot
set PATH=%JAVA_HOME%\bin;%PATH%

echo Checking MySQL Connector...
dir "%PROJECT%\WebContent\WEB-INF\lib\*.jar" 2>nul
echo.

REM Find MySQL Connector JAR
for %%f in ("%PROJECT%\WebContent\WEB-INF\lib\mysql-connector-j*.jar") do set MYSQL_JAR=%%f
if not defined MYSQL_JAR (
    echo ERROR: MySQL Connector/J not found in WebContent\WEB-INF\lib\
    pause
    exit /b 1
)
echo Using MySQL Connector: %MYSQL_JAR%
echo.

echo Compiling...
cd /d "%PROJECT%"
if not exist WebContent\WEB-INF\classes mkdir WebContent\WEB-INF\classes
javac -encoding UTF-8 -classpath "%TOMCAT%\lib\servlet-api.jar;%MYSQL_JAR%" -sourcepath src -d WebContent\WEB-INF\classes src\com\banking\model\*.java src\com\banking\util\*.java src\com\banking\dao\*.java src\com\banking\filter\*.java src\com\banking\servlet\*.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)
echo Compilation done.

echo.
echo Deploying...
if exist "%TOMCAT%\webapps\Banking-Transaction-System" rmdir /s /q "%TOMCAT%\webapps\Banking-Transaction-System"
xcopy /E /I /Y "%PROJECT%\WebContent" "%TOMCAT%\webapps\Banking-Transaction-System\"
if %errorlevel% neq 0 (
    echo Deployment copy failed!
    pause
    exit /b 1
)
echo Deployment copy done.

echo.
echo To start Tomcat (if not running):
echo   %TOMCAT%\bin\startup.bat
echo.
echo Done! Open http://localhost:8080/Banking-Transaction-System/
pause
