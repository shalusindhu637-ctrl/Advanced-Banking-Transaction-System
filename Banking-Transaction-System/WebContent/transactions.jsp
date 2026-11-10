<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.Customer" %>
<%@ page import="com.banking.model.Transaction" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.DecimalFormat" %>
<%
    Customer customer = (Customer) session.getAttribute("customer");
    if (customer == null) {
        response.sendRedirect("login.jsp");
        return;
    }
    List<Transaction> transactions = (List<Transaction>) request.getAttribute("transactions");
    DecimalFormat df = new DecimalFormat("#,##0.00");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Transaction History - SecureBank</title>
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
                <a href="<%= request.getContextPath() %>/account.jsp" class="nav-item">👤 Account Details</a>
                <a href="<%= request.getContextPath() %>/deposit" class="nav-item">💰 Deposit</a>
                <a href="<%= request.getContextPath() %>/withdraw" class="nav-item">💸 Withdraw</a>
                <a href="<%= request.getContextPath() %>/balance" class="nav-item">💳 Balance Enquiry</a>
                <a href="<%= request.getContextPath() %>/transactions" class="nav-item active">📋 Transaction History</a>
                <a href="<%= request.getContextPath() %>/profile" class="nav-item">⚙️ Profile</a>
                <a href="<%= request.getContextPath() %>/logout" class="nav-item">🚪 Logout</a>
            </nav>
        </aside>
        
        <main class="main-content">
            <div class="top-bar">
                <h1>Transaction History</h1>
            </div>
            
            <div class="card">
                <div class="card-header">
                    <h3>All Transactions</h3>
                </div>
                <div class="card-body">
                    <% if (transactions == null || transactions.isEmpty()) { %>
                        <p class="text-center" style="color: var(--text-light); padding: 2rem;">No transactions found</p>
                    <% } else { %>
                        <div class="table-responsive">
                            <table class="table">
                                <thead>
                                    <tr>
                                        <th>Transaction ID</th>
                                        <th>Date & Time</th>
                                        <th>Type</th>
                                        <th>Amount</th>
                                        <th>Balance After</th>
                                        <th>Description</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% for (Transaction t : transactions) { %>
                                        <tr>
                                            <td>#<%= t.getId() %></td>
                                            <td><%= t.getCreatedAt() %></td>
                                            <td>
                                                <% if ("DEPOSIT".equals(t.getTransactionType())) { %>
                                                    <span class="badge badge-success">DEPOSIT</span>
                                                <% } else { %>
                                                    <span class="badge badge-warning">WITHDRAWAL</span>
                                                <% } %>
                                            </td>
                                            <td>$<%= df.format(t.getAmount()) %></td>
                                            <td>$<%= df.format(t.getBalanceAfter()) %></td>
                                            <td><%= t.getDescription() %></td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    <% } %>
                </div>
            </div>
        </main>
    </div>
    <script src="js/app.js"></script>
</body>
</html>
