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
    List<Transaction> recentTransactions = (List<Transaction>) request.getAttribute("recentTransactions");
    Double totalDeposit = (Double) request.getAttribute("totalDeposit");
    Double totalWithdrawal = (Double) request.getAttribute("totalWithdrawal");
    if (totalDeposit == null) totalDeposit = 0.0;
    if (totalWithdrawal == null) totalWithdrawal = 0.0;
    DecimalFormat df = new DecimalFormat("#,##0.00");
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard - SecureBank</title>
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
                <a href="<%= request.getContextPath() %>/dashboard" class="nav-item active">📊 Dashboard</a>
                <a href="account.jsp" class="nav-item">👤 Account Details</a>
                <a href="deposit" class="nav-item">💰 Deposit</a>
                <a href="withdraw" class="nav-item">💸 Withdraw</a>
                <a href="balance" class="nav-item">💳 Balance Enquiry</a>
                <a href="<%= request.getContextPath() %>/set-pin" class="nav-item">🔐 Transaction PIN</a>
                <a href="<%= request.getContextPath() %>/transactions" class="nav-item">📋 Transaction History</a>
                <a href="profile" class="nav-item">⚙️ Profile</a>
                <a href="<%= request.getContextPath() %>/logout" class="nav-item">🚪 Logout</a>
            </nav>
        </aside>
        
        <main class="main-content">
            <div class="top-bar">
                <h1>Dashboard</h1>
                <div class="user-menu">
                    <span>Welcome, <strong><%= customer.getName() %></strong></span>
                </div>
            </div>
            
            <div class="stats-grid">
                <div class="stat-card">
                    <h4>Available Balance</h4>
                    <div class="stat-value balance-masked" style="font-size: 1.8rem;">₹••••••</div>
                    <div style="margin-top:.5rem;"><a class="text-link" href="<%= request.getContextPath() %>/balance">Check Balance (PIN required)</a></div>
                </div>
                <div class="stat-card success">
                    <h4>Total Deposits</h4>
                    <div class="stat-value">&#8377;<%= df.format(totalDeposit) %></div>
                </div>
                <div class="stat-card warning">
                    <h4>Total Withdrawals</h4>
                    <div class="stat-value">&#8377;<%= df.format(totalWithdrawal) %></div>
                </div>
                <div class="stat-card">
                    <h4>Account Number</h4>
                    <div class="stat-value" style="font-size: 1.2rem;"><%= customer.getAccountNumber() %></div>
                </div>
            </div>
            
            <div class="card">
                <div class="card-header">
                    <h3>Quick Actions</h3>
                </div>
                <div class="card-body">
                    <div class="quick-actions">
                        <a href="<%= request.getContextPath() %>/deposit" class="quick-action-btn">
                            <div class="icon">💰</div>
                            <div>Deposit Money</div>
                        </a>
                        <a href="<%= request.getContextPath() %>/withdraw" class="quick-action-btn">
                            <div class="icon">💸</div>
                            <div>Withdraw Money</div>
                        </a>
                        <a href="<%= request.getContextPath() %>/balance" class="quick-action-btn">
                            <div class="icon">💳</div>
                            <div>Check Balance</div>
                        </a>
                        <a href="<%= request.getContextPath() %>/transactions" class="quick-action-btn">
                            <div class="icon">📋</div>
                            <div>Transaction History</div>
                        </a>
                    </div>
                </div>
            </div>
            
            <div class="card">
                <div class="card-header">
                    <h3>Recent Transactions</h3>
                    <a href="<%= request.getContextPath() %>/transactions" class="text-link">View All</a>
                </div>
                <div class="card-body">
                    <% if (recentTransactions == null || recentTransactions.isEmpty()) { %>
                        <p class="text-center" style="color: var(--text-light);">No transactions yet</p>
                    <% } else { %>
                        <div class="table-responsive"><table class="table">
                            <thead>
                                <tr>
                                    <th>Date</th>
                                    <th>Type</th>
                                    <th>Amount</th>
                                    <th>Balance After</th>
                                    <th>Description</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (Transaction t : recentTransactions) { %>
                                    <tr>
                                        <td><%= t.getCreatedAt() %></td>
                                        <td>
                                            <% if ("DEPOSIT".equals(t.getTransactionType())) { %>
                                                <span class="badge badge-success">DEPOSIT</span>
                                            <% } else { %>
                                                <span class="badge badge-warning">WITHDRAWAL</span>
                                            <% } %>
                                        </td>
                                        <td>&#8377;<%= df.format(t.getAmount()) %></td>
                                        <td>&#8377;<%= df.format(t.getBalanceAfter()) %></td>
                                        <td><%= t.getDescription() %></td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    <% } %>
                </div>
            </div>
        </main>
    </div>
    <script src="js/app.js"></script>
</body>
</html>
