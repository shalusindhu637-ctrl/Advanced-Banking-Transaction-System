# Banking Transaction System

A complete Java Web Application for managing banking transactions built with JSP, Servlets, JDBC, and MySQL.

## Technologies Used

- **Java** - Core programming language
- **JSP & Servlets** - Web layer (Jakarta EE for Tomcat 10+)
- **JDBC** - Database connectivity
- **MySQL** - Database management
- **Apache Tomcat** - Web server
- **HTML5 & CSS3** - Frontend

## Features

1. **Customer Registration** - Register with full name, username, and password (duplicate username prevention)
2. **Customer Login** - Secure login with session management
3. **Account Details** - View customer ID, name, username, and account balance
4. **Deposit** - Add money to account with validation
5. **Withdrawal** - Withdraw money with balance check and validation
6. **Balance Enquiry** - Check current account balance
7. **Logout** - Secure session invalidation
8. **Password Security** - Passwords are hashed using SHA-256

## Project Structure

`
Banking-Transaction-System/
├── src/
│   └── com/banking/
│       ├── dao/          # Data Access Objects
│       ├── filter/       # Authentication Filter
│       ├── model/        # Model classes
│       ├── servlet/      # Servlets
│       └── util/         # Utility classes (DB connection, Password hashing)
├── WebContent/
│   ├── css/              # Stylesheets
│   ├── WEB-INF/
│   │   ├── lib/          # JAR dependencies
│   │   └── web.xml       # Deployment descriptor
│   ├── login.jsp
│   ├── register.jsp
│   ├── dashboard.jsp
│   ├── account.jsp
│   ├── deposit.jsp
│   ├── withdraw.jsp
│   └── balance.jsp
├── sql/
│   └── banking_schema.sql
└── README.md
`

## Setup Instructions for VS Code on Windows

### Prerequisites

1. **Java JDK 11 or higher** - Install from [Oracle](https://www.oracle.com/java/technologies/downloads/) or use OpenJDK
2. **Apache Tomcat 10+** - Download from [Apache Tomcat](https://tomcat.apache.org/download-10.cgi)
3. **MySQL Server** - Download from [MySQL](https://dev.mysql.com/downloads/mysql/)
4. **MySQL Connector/J** - Download from [MySQL Connector](https://dev.mysql.com/downloads/connector/j/)
5. **VS Code Extensions** (Recommended):
   - Extension Pack for Java
   - Community Server Connectors (for Tomcat)
   - or Smart Tomcat

### Step 1: Setup MySQL Database

1. Open MySQL Command Line Client or MySQL Workbench
2. Run the SQL script:
`sql
source C:/Banking-Transaction-System/sql/banking_schema.sql
`
Or manually execute:
`sql
CREATE DATABASE IF NOT EXISTS banking;
USE banking;
CREATE TABLE IF NOT EXISTS customer (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    account_balance DECIMAL(15,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
`

3. Update database credentials in src/com/banking/util/DBConnection.java if needed:
   - Default: username=oot, password=oot, URL=jdbc:mysql://localhost:3306/banking

### Step 2: Add MySQL JDBC Driver

1. Download mysql-connector-j-x.x.x.jar from MySQL website
2. Copy the JAR file to WebContent/WEB-INF/lib/ directory
3. Also add it to your classpath if needed

### Step 3: Configure in VS Code

1. Open VS Code in the project directory or open the folder C:\Banking-Transaction-System
2. Install required Java extensions
3. Configure Java classpath to include the servlet API and MySQL connector

**Note:** For Tomcat 10+, you need jakarta.servlet-api which is included with Tomcat. Make sure to reference Tomcat's lib folder.

### Step 4: Compile the Project

Using command prompt in the project root:

`cmd
cd C:\Banking-Transaction-System
javac -classpath "C:\path\to\tomcat\lib\jakarta.servlet-api.jar;WebContent\WEB-INF\lib\mysql-connector-j.jar" -sourcepath src -d WebContent\WEB-INF\classes src\com\banking\**\*.java
`

Replace paths with your actual Tomcat and MySQL connector locations.

### Step 5: Deploy to Apache Tomcat

**Option A: Manual Deployment**
1. Copy the entire Banking-Transaction-System folder (or rename to Banking-Transaction-System as WAR context) to TOMCAT_HOME\webapps\
2. Start Tomcat: Run TOMCAT_HOME\bin\startup.bat
3. Access the application at http://localhost:8080/Banking-Transaction-System/

**Option B: Using VS Code with Community Server Connectors**
1. Add Tomcat server in VS Code
2. Right-click project and deploy to server
3. Start the server

### Step 6: Access the Application

1. Open your browser and go to: http://localhost:8080/Banking-Transaction-System/
2. You'll be redirected to the login page
3. Click "Register here" to create a new account
4. After registration, login with your credentials
5. Use the dashboard to perform banking operations

## Security Features

- **Password Hashing**: Passwords are hashed using SHA-256 before storing in database
- **Session Management**: HttpSession used for user authentication
- **Auth Filter**: Prevents unauthorized access to protected pages/servlets
- **SQL Injection Prevention**: Uses PreparedStatement for all database queries
- **Input Validation**: Validates all user inputs on both client and server side
- **Transaction Safety**: Deposits and withdrawals use database transactions with commit/rollback
- **Duplicate Prevention**: Prevents duplicate usernames during registration

## Default Database Configuration

Edit src/com/banking/util/DBConnection.java:
`java
private static final String URL = "jdbc:mysql://localhost:3306/banking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
private static final String USERNAME = "root";
private static final String PASSWORD = "root";
`

Change USERNAME and PASSWORD to match your MySQL installation.

## Troubleshooting

- **ClassNotFoundException: com.mysql.cj.jdbc.Driver** - Ensure MySQL Connector JAR is in WEB-INF/lib
- **Access denied for user** - Check MySQL username/password in DBConnection.java
- **404 Error** - Ensure project is deployed correctly in Tomcat webapps
- **500 Error** - Check Tomcat logs in logs/localhost.*.log for details
- **Connection refused** - Ensure MySQL server is running

## Notes

- This project uses Jakarta EE (jakarta.servlet) for compatibility with Tomcat 10+
- For Tomcat 9 and below, you would need to use javax.servlet packages instead
- Passwords are hashed with SHA-256; for production use, consider BCrypt or stronger algorithms
- Always use HTTPS in production environments
