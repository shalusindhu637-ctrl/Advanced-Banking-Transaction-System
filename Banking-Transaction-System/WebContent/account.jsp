<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.Customer" %>
<%
    Customer customer = (Customer) session.getAttribute("customer");
    if (customer == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Account Details - SecureBank</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
    <div class="dashboard-wrapper">
        <aside class="sidebar">
            <div class="sidebar-header">
                <h3>SecureBank</h3>
                <small>Online Banking</small>
            </div>
            <nav class="sidebar-nav">
                <a href="<%= request.getContextPath() %>/dashboard" class="nav-item">📊 Dashboard</a>
                <a href="<%= request.getContextPath() %>/account.jsp" class="nav-item active">👤 Account Details</a>
                <a href="<%= request.getContextPath() %>/deposit" class="nav-item">💰 Deposit</a>
                <a href="<%= request.getContextPath() %>/withdraw" class="nav-item">💸 Withdraw</a>
                <a href="<%= request.getContextPath() %>/balance" class="nav-item">💳 Balance Enquiry</a>
                <a href="<%= request.getContextPath() %>/transactions" class="nav-item">📋 Transaction History</a>
                <a href="<%= request.getContextPath() %>/profile" class="nav-item">⚙️ Profile</a>
                <a href="<%= request.getContextPath() %>/logout" class="nav-item">🚪 Logout</a>
            </nav>
        </aside>
        
        <main class="main-content">
            <div class="top-bar">
                <h1>Account Details</h1>
            </div>
            
            <div class="card" style="max-width: 600px;">
                <div class="card-header">
                    <h3>Personal Information</h3>
                </div>
                <div class="card-body">
                    <div class="info-row">
                        <span class="info-label">Account Number</span>
                        <span class="info-value"><%= customer.getAccountNumber() %></span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Full Name</span>
                        <span class="info-value"><%= customer.getName() %></span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Email Address</span>
                        <span class="info-value"><%= customer.getEmail() %></span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Phone Number</span>
                        <span class="info-value"><%= customer.getPhone() %></span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Address</span>
                        <span class="info-value"><%= customer.getAddress() %></span>
                    </div>
                    <div class="info-row">
                        <span class="info-label">Available Balance</span>
                        <span class="info-value"><a class="text-link" href="<%= request.getContextPath() %>/balance">Check balance (PIN required)</a></span>
                    </div>
                </div>
            </div>
        </main>
    </div>
    <script src="js/app.js"></script>
</body>
</html>
