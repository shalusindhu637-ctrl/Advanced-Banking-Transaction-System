<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ page import="com.banking.model.Customer" %>
<%@ page import="com.banking.util.CsrfUtil" %>
<%@ page import="java.util.Locale" %>
<%
    Customer customer = (Customer) session.getAttribute("customer");
    if (customer == null) { response.sendRedirect("login.jsp"); return; }
    String csrf = CsrfUtil.getToken(request);
    double bal = customer.getBalance();
    String balInr = String.format(Locale.forLanguageTag("en-IN"), "%,.2f", bal);
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Withdraw Money - SecureBank</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/cash-animation.css">
</head>
<body>
    <div class="dashboard-wrapper">
        <aside class="sidebar">
            <div class="sidebar-header"><h3>SecureBank</h3><small>Online Banking</small></div>
            <nav class="sidebar-nav" aria-label="Main navigation">
                <a href="dashboard" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="8" height="8" rx="1.5"/><rect x="13" y="3" width="8" height="5" rx="1.5"/><rect x="13" y="10" width="8" height="11" rx="1.5"/><rect x="3" y="13" width="8" height="8" rx="1.5"/></svg><span>Dashboard</span></a>
                <a href="account.jsp" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 20a8 8 0 0 1 16 0"/></svg><span>Account Details</span></a>
                <a href="deposit" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M4 21h16"/></svg><span>Deposit</span></a>
                <a href="withdraw" class="nav-item active"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 21V9"/><path d="m7 14 5-5 5 5"/><path d="M4 3h16"/></svg><span>Withdraw</span></a>
                <a href="balance" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18"/></svg><span>Balance</span></a>
                <a href="transactions" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h10"/></svg><span>Transaction History</span></a>
                <a href="profile" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1 1 0 0 0 .2 1.1l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1 1 0 0 0-1.1-.2 1 1 0 0 0-.6.9V20a2 2 0 0 1-4 0v-.1a1 1 0 0 0-.7-.9 1 1 0 0 0-1.1.2l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1 1 0 0 0 .2-1.1 1 1 0 0 0-.9-.6H4a2 2 0 0 1 0-4h.1a1 1 0 0 0 .9-.7 1 1 0 0 0-.2-1.1l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1 1 0 0 0 1.1.2h.1a1 1 0 0 0 .6-.9V4a2 2 0 0 1 4 0v.1a1 1 0 0 0 .6.9 1 1 0 0 0 1.1-.2l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1 1 0 0 0-.2 1.1v.1a1 1 0 0 0 .9.6H20a2 2 0 0 1 0 4h-.1a1 1 0 0 0-.9.6z"/></svg><span>Profile</span></a>
                <a href="logout" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/></svg><span>Logout</span></a>
            </nav>
        </aside>

        <main class="main-content">
            <div class="top-bar"><h1>Withdraw Money</h1></div>

            <div class="card" style="max-width: 640px;">
                <div class="card-header"><h3>3D ATM Withdrawal (Simulated)</h3></div>
                <div class="card-body">
                    <% if (request.getAttribute("errorMessage") != null) { %>
                        <div class="alert alert-danger" role="alert"><%= request.getAttribute("errorMessage") %></div>
                    <% } %>
                    <% if (request.getAttribute("successMessage") != null) { %>
                        <div class="alert alert-success" role="status"><%= request.getAttribute("successMessage") %></div>
                    <% } %>

                    <p class="mb-2"><strong>Current Balance:</strong> <span id="currentBalance" class="balance-counter" data-balance="<%= String.format(Locale.US, "%.2f", bal) %>">&#8377;<%= balInr %></span></p>
                    <p class="text-muted" style="font-size:.92rem;">Educational simulation only. The server verifies your transaction PIN, validates amount and available balance, and updates the balance only after a successful backend transaction. No cash is dispensed on failure.</p>

                    <form id="withdrawForm" action="withdraw" method="post" novalidate>
                        <input type="hidden" id="csrfToken" name="csrfToken" value="<%= csrf %>">
                        <div class="form-group">
                            <label for="amount">Withdrawal Amount (&#8377;)</label>
                            <input type="number" class="form-control" id="amount" name="amount" step="0.01" min="0.01" max="50000" placeholder="Enter amount" required inputmode="decimal">
                            <small style="color: var(--text-light);">Maximum: &#8377;50,000 per transaction</small>
                        </div>
                        <div class="form-group">
                            <label for="pin">Transaction PIN</label>
                            <input type="password" class="form-control pin-input" id="pin" name="pin" inputmode="numeric" pattern="\d{4}|\d{6}" maxlength="6" autocomplete="off" placeholder="4 or 6 digits" required>
                            <small style="color: var(--text-light);">Server-side verification required. Set/change PIN under <a href="set-pin">Transaction PIN</a>.</small>
                        </div>
                        <button type="submit" class="btn btn-warning btn-block">Withdraw Money</button>
                    </form>

                    <div id="withdrawStage" class="cash-stage" aria-live="polite">
                        <div class="cash-stage-header"><h4 class="cash-stage-title">ATM Cash Dispenser</h4><span class="text-muted" style="font-size:.85rem;">Simulated</span></div>
                        <div class="cash-viewport">
                            <canvas id="withdrawCanvas" class="cash-canvas" aria-hidden="true"></canvas>
                            <div class="cash-fallback" aria-hidden="true">
                                <div class="cash-atm"><div class="cash-slot"></div><div class="note n1" style="top:18px"></div><div class="note n2" style="top:26px"></div><div class="note n3" style="top:34px"></div></div>
                            </div>
                        </div>
                        <div id="withdrawStatus" class="cash-status"></div>
                    </div>
                </div>
            </div>
        </main>
    </div>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js" defer></script>
    <script src="js/three-cash-scene.js" defer></script>
    <script src="js/cash-animation.js" defer></script>
    <script src="js/app.js" defer></script>
    <script>
    window.addEventListener('DOMContentLoaded', function(){
      if (window.SecureBankCash && SecureBankCash.setup) {
        SecureBankCash.setup({ mode:'withdraw', formId:'withdrawForm', stageId:'withdrawStage', statusId:'withdrawStatus', balanceId:'currentBalance', amountId:'amount', csrfId:'csrfToken', pinId:'pin', url:'<%= request.getContextPath() %>/withdraw' });
      }
    });
    </script>
</body>
</html>
