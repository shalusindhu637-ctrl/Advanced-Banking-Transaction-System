<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.Customer" %>
<%@ page import="com.banking.util.CsrfUtil" %>
<%
    Customer customer = (Customer) session.getAttribute("customer");
    if (customer == null) { response.sendRedirect("login.jsp"); return; }
    String csrf = CsrfUtil.getToken(request);
    Boolean hasPinAttr = (Boolean) request.getAttribute("hasPin");
    boolean hasPin = hasPinAttr != null && hasPinAttr;
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Transaction PIN - SecureBank</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
</head>
<body>
<div class="dashboard-wrapper">
    <aside class="sidebar">
        <div class="sidebar-header"><h3>SecureBank</h3><small>Online Banking</small></div>
        <nav class="sidebar-nav">
            <a href="<%= request.getContextPath() %>/dashboard" class="nav-item">📊 Dashboard</a>
            <a href="<%= request.getContextPath() %>/account.jsp" class="nav-item">👤 Account Details</a>
            <a href="<%= request.getContextPath() %>/deposit" class="nav-item">💰 Deposit</a>
            <a href="<%= request.getContextPath() %>/withdraw" class="nav-item">💸 Withdraw</a>
            <a href="<%= request.getContextPath() %>/balance" class="nav-item">💳 Balance</a>
            <a href="<%= request.getContextPath() %>/set-pin" class="nav-item active">🔐 Transaction PIN</a>
            <a href="<%= request.getContextPath() %>/transactions" class="nav-item">📋 Transaction History</a>
            <a href="<%= request.getContextPath() %>/logout" class="nav-item">🚪 Logout</a>
        </nav>
    </aside>
    <main class="main-content">
        <div class="top-bar"><h1>Transaction PIN</h1></div>
        <% if (request.getAttribute("errorMessage") != null) { %><div class="alert alert-danger"><%= request.getAttribute("errorMessage") %></div><% } %>
        <% if (request.getAttribute("successMessage") != null) { %><div class="alert alert-success"><%= request.getAttribute("successMessage") %></div><% } %>
        <div class="card" style="max-width:520px;">
            <div class="card-header"><h3><%= hasPin ? "Change Transaction PIN" : "Set Transaction PIN" %></h3></div>
            <div class="card-body">
                <p class="text-muted">Use a dedicated 4- or 6-digit PIN to verify balance checks. This is <strong>not</strong> your login password. PINs are verified on the server and stored as a secure salted hash.</p>
                <form method="post" action="<%= request.getContextPath() %>/set-pin" autocomplete="off">
                    <input type="hidden" name="csrfToken" value="<%= csrf %>"/>
                    <input type="hidden" name="action" value="<%= hasPin ? "change" : "set" %>"/>
                    <% if (hasPin) { %>
                    <div class="form-group mb-3">
                        <label for="currentPin">Current PIN</label>
                        <input type="password" inputmode="numeric" pattern="\d{4}|\d{6}" maxlength="6" class="form-control pin-input" id="currentPin" name="currentPin" required autocomplete="off">
                    </div>
                    <% } %>
                    <div class="form-group mb-3">
                        <label for="newPin"><%= hasPin ? "New PIN" : "PIN" %> (4 or 6 digits)</label>
                        <input type="password" inputmode="numeric" pattern="\d{4}|\d{6}" maxlength="6" class="form-control pin-input" id="newPin" name="newPin" required autocomplete="off">
                    </div>
                    <div class="form-group mb-3">
                        <label for="confirmPin">Confirm PIN</label>
                        <input type="password" inputmode="numeric" pattern="\d{4}|\d{6}" maxlength="6" class="form-control pin-input" id="confirmPin" name="confirmPin" required autocomplete="off">
                    </div>
                    <button type="submit" class="btn btn-block"><%= hasPin ? "Change PIN" : "Set PIN" %></button>
                </form>
            </div>
        </div>
    </main>
</div>
<script src="js/app.js"></script>
</body>
</html>