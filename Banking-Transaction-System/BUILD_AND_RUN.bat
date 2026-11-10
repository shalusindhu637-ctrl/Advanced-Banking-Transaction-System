@echo off
echo ========================================
echo Banking Transaction System - Build Script
echo ========================================
echo.

REM Set paths - UPDATE THESE IF NEEDED
set TOMCAT_HOME=C:\CTomcat
set PROJECT_DIR=C:\Banking-Transaction-System
set JAVA_HOME=C:\Program Files\Java\jdk-17jakarta.servlet-api.ja

echo Checking prerequisites...
if not exist "%TOMCAT_HOME%" (
    echo ERROR: Tomcat not found at %TOMCAT_HOME%
    echo Please update TOMCAT_HOME in this script
    pause
    exit /b 1
)

if not exist "%PROJECT_DIR%\WebContent\WEB-INF\lib\mysql-connector-j-*.jar" (
    echo WARNING: MySQL Connector/J not found in WebContent\WEB-INF\lib\
    echo Please download mysql-connector-j and copy to WebContent\WEB-INF\lib\
)

echo.
echo Compiling Java sources...
cd %PROJECT_DIR%
if not exist WebContent\WEB-INF\classes mkdir WebContent\WEB-INF\classes

javac -encoding UTF-8 -classpath "%TOMCAT_HOME%\lib\servlet-api.jar;WebContent\WEB-INF\lib\mysql-connector-j-*.jar" -sourcepath src -d WebContent\WEB-INF\classes src\com\banking\model\*.java src\com\banking\util\*.java src\com\banking\dao\*.java src\com\banking\filter\*.java src\com\banking\servlet\*.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!
echo.
echo Deploying to Tomcat...
if exist "%TOMCAT_HOME%\webapps\Banking-Transaction-System" rmdir /s /q "%TOMCAT_HOME%\webapps\Banking-Transaction-System"
xcopy /E /I /Y WebContent "%TOMCAT_HOME%\webapps\Banking-Transaction-System"

echo.
echo Deployment complete!
echo.
echo Starting Tomcat...
call "%TOMCAT_HOME%\bin\startup.bat"

echo.
echo Application will be available at: http://localhost:8080/Banking-Transaction-System/
echo.
pause
