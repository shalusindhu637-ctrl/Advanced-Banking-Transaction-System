# Banking Transaction System - Complete Setup Guide

## Prerequisites

1. **Java JDK 17** - [Download from Adoptium](https://adoptium.net/)
2. **Apache Tomcat 10.1.x** - [Download from Apache](https://tomcat.apache.org/download-10.cgi)
3. **MySQL 8.x** - [Download from MySQL](https://dev.mysql.com/downloads/mysql/)
4. **MySQL Connector/J 8.x** - [Download from MySQL](https://dev.mysql.com/downloads/connector/j/)

## Setup Instructions (Windows)

### 1. Install MySQL
1. Run MySQL Installer
2. Install MySQL Server
3. Set root password (remember this)
4. Start MySQL service

### 2. Create Database
Open MySQL Command Line Client or MySQL Workbench and run:
`sql
source C:/Banking-Transaction-System/sql/banking_schema.sql
`

Or execute the contents of sql/banking_schema.sql directly.

### 3. Configure Database Credentials
Edit config/db.properties:
`properties
db.url=jdbc:mysql://localhost:3306/banking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.username=root
db.password=YOUR_MYSQL_PASSWORD
`

### 4. Install MySQL Connector/J
1. Download and extract MySQL Connector/J
2. Copy mysql-connector-j-8.x.x.jar to WebContent/WEB-INF/lib/

### 5. Install and Configure Tomcat
1. Extract Apache Tomcat 10.1 to C:\apache-tomcat-10.1.x\
2. Update BUILD_AND_RUN.bat with correct paths if different

### 6. Build and Run
1. Double-click BUILD_AND_RUN.bat or run from command prompt
2. Wait for Tomcat to start
3. Open browser and go to: http://localhost:8080/Banking-Transaction-System/

## Features

- **User Registration** - Create account with personal details
- **Secure Login** - PBKDF2 password hashing
- **Dashboard** - Overview of account with stats and recent transactions
- **Deposit Money** - Add funds to account (max ,000)
- **Withdraw Money** - Withdraw funds with balance check (max ,000)
- **Balance Enquiry** - Check current balance
- **Transaction History** - View all transactions sorted by date
- **Account Details** - View personal and account information
- **Profile** - View profile information
- **Logout** - Secure session termination

## Security Features

- PBKDF2 password hashing with salt
- PreparedStatements to prevent SQL injection
- Session-based authentication
- Authentication filter for protected pages
- Server-side validation
- Database transactions for deposits/withdrawals
- Authorization checks

## Troubleshooting

**Compilation errors:** Ensure MySQL Connector/J and Jakarta Servlet API are in classpath
**Database connection failed:** Check MySQL is running and credentials in config/db.properties
**404 errors:** Ensure app is deployed to correct Tomcat webapps folder
**ClassNotFoundException:** Check all JARs are in WEB-INF/lib

## Default URL
http://localhost:8080/Banking-Transaction-System/
