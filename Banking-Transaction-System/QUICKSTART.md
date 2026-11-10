# Banking Transaction System - Quick Start Guide

## Prerequisites Check
- [ ] Java JDK installed (check: java -version)
- [ ] Apache Tomcat 10+ downloaded
- [ ] MySQL Server installed and running
- [ ] MySQL Connector/J downloaded

## Quick Setup (Windows)

### 1. Database Setup
Open MySQL Command Line or MySQL Workbench and run:
`sql
CREATE DATABASE banking;
USE banking;
`

Then run the schema from: sql/banking_schema.sql

Or import directly.

### 2. Add Dependencies
Copy mysql-connector-j-*.jar to WebContent/WEB-INF/lib/

### 3. Update DB Credentials
Edit src/com/banking/util/DBConnection.java with your MySQL credentials.

### 4. Compile
`cmd
cd C:\Banking-Transaction-System
javac -encoding UTF-8 -classpath "C:\apache-tomcat-10.x.x\lib\jakarta.servlet-api.jar;WebContent\WEB-INF\lib\mysql-connector-j-*.jar" -sourcepath src -d WebContent\WEB-INF\classes (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { .FullName })
`

### 5. Deploy
Copy entire folder to C:\apache-tomcat-10.x.x\webapps\Banking-Transaction-System\

### 6. Start Tomcat
`cmd
C:\apache-tomcat-10.x.x\bin\startup.bat
`

### 7. Access
Open browser: http://localhost:8080/Banking-Transaction-System/

## Test Flow
1. Register a new user
2. Login with credentials
3. View account details
4. Deposit money
5. Check balance
6. Withdraw money
7. Logout
