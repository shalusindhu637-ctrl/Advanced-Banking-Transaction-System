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
    <title>Balance - SecureBank</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="css/style.css">
    <link rel="stylesheet" href="css/cash-animation.css">
</head>
<body>
<div class="dashboard-wrapper">
    <aside class="sidebar">
        <div class="sidebar-header"><h3>SecureBank</h3><small>Online Banking</small></div>
        <nav class="sidebar-nav" aria-label="Main navigation">
            <a href="<%= request.getContextPath() %>/dashboard" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="3" width="8" height="8" rx="1.5"/><rect x="13" y="3" width="8" height="5" rx="1.5"/><rect x="13" y="10" width="8" height="11" rx="1.5"/><rect x="3" y="13" width="8" height="8" rx="1.5"/></svg><span>Dashboard</span></a>
            <a href="<%= request.getContextPath() %>/account.jsp" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="4"/><path d="M4 20a8 8 0 0 1 16 0"/></svg><span>Account Details</span></a>
            <a href="<%= request.getContextPath() %>/deposit" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12"/><path d="m7 10 5 5 5-5"/><path d="M4 21h16"/></svg><span>Deposit</span></a>
            <a href="<%= request.getContextPath() %>/withdraw" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 21V9"/><path d="m7 14 5-5 5 5"/><path d="M4 3h16"/></svg><span>Withdraw</span></a>
            <a href="<%= request.getContextPath() %>/balance" class="nav-item active"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 10h18"/></svg><span>Balance</span></a>
            <a href="<%= request.getContextPath() %>/set-pin" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 1 1 8 0v3"/></svg><span>Transaction PIN</span></a>
            <a href="<%= request.getContextPath() %>/transactions" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h10"/></svg><span>Transaction History</span></a>
            <a href="<%= request.getContextPath() %>/logout" class="nav-item"><svg class="nav-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><path d="m16 17 5-5-5-5"/><path d="M21 12H9"/></svg><span>Logout</span></a>
        </nav>
    </aside>
    <main class="main-content">
        <div class="top-bar"><h1>Balance Enquiry</h1></div>
        <div class="card balance-card" style="max-width: 680px;">
            <div class="card-header"><h3>ATM Balance Inquiry (Simulated)</h3></div>
            <div class="card-body" style="padding:1.5rem 1.75rem 2rem;">
                <p class="text-muted mb-2" style="text-align:center;">Available Balance</p>
                <div class="balance-display-row" style="justify-content:center;">
                    <span id="balanceValue" class="balance-masked" aria-live="polite">&#8377;&#8226;&#8226;&#8226;&#8226;&#8226;&#8226;</span>
                    <button type="button" id="toggleEye" class="eye-btn" aria-label="Show or hide balance" title="Reveal/Hide" disabled>
                        <svg class="eye-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>
                    </button>
                </div>
                <p id="balanceAsOf" class="text-muted mt-1 mb-2" style="text-align:center; min-height:1.25rem; font-size:.92rem;"></p>
                <p class="text-muted mb-3" style="text-align:center;">Account: <%= customer.getAccountNumber() %></p>

                <div id="balanceStage" class="cash-stage" aria-live="polite">
                    <div class="cash-stage-header"><h4 class="cash-stage-title">ATM Balance Check</h4><span class="text-muted" style="font-size:.85rem;">Simulated</span></div>
                    <div class="cash-viewport">
                        <canvas id="balanceCanvas" class="cash-canvas" aria-hidden="true"></canvas>
                        <div class="cash-fallback" aria-hidden="true">
                            <div class="cash-atm"><div class="cash-slot"></div></div>
                        </div>
                    </div>
                    <div id="balanceStatus" class="cash-status" style="text-align:center;"></div>
                </div>

                <div style="text-align:center; margin-top:1rem;">
                    <button type="button" id="checkBalanceBtn" class="btn btn-check-balance">Check Balance</button>
                    <a href="<%= request.getContextPath() %>/dashboard" class="btn btn-secondary" style="margin-left:.35rem;">Back to Dashboard</a>
                </div>
                <% if (!hasPin) { %>
                    <p class="mt-3 mb-0" style="text-align:center;"><a class="text-link" href="<%= request.getContextPath() %>/set-pin">Set your transaction PIN</a> to securely view your balance.</p>
                <% } %>
                <p class="mt-3 small text-muted mb-0" style="text-align:center;">Educational simulation. Balance is retrieved only after server-side PIN verification (authenticated session). No balance/database modification is performed. Use HTTPS in production (localhost HTTP is for development only).</p>
            </div>
        </div>
    </main>
</div>

<div id="pinModal" class="pin-modal" aria-hidden="true">
    <div class="pin-modal-dialog" role="dialog" aria-modal="true" aria-labelledby="pinModalTitle">
        <h4 id="pinModalTitle">Verify Transaction PIN</h4>
        <p class="text-muted">Enter your 4- or 6-digit transaction PIN to view your balance.</p>
        <form id="pinForm" autocomplete="off">
            <input type="hidden" name="csrfToken" value="<%= csrf %>"/>
            <input type="password" inputmode="numeric" pattern="\d{4}|\d{6}" maxlength="6" class="form-control pin-input mb-2" id="pinInput" name="pin" placeholder="Enter PIN" required autocomplete="off">
            <div id="pinError" class="pin-error" aria-live="polite"></div>
            <div class="d-flex gap-2 mt-3">
                <button type="submit" id="verifyPinBtn" class="btn btn-block">Verify</button>
                <button type="button" id="cancelPinBtn" class="btn btn-secondary">Cancel</button>
            </div>
        </form>
    </div>
</div>

<script src="https://cdnjs.cloudflare.com/ajax/libs/three.js/r128/three.min.js" defer></script>
<script src="js/three-cash-scene.js" defer></script>
<script src="js/cash-animation.js" defer></script>
<script src="js/app.js" defer></script>
<script>
window.addEventListener('DOMContentLoaded', function(){
  if (window.SecureBankCash && SecureBankCash.setupBalance) {
    SecureBankCash.setupBalance({
      buttonId:'checkBalanceBtn', formId:'pinForm', stageId:'balanceStage', statusId:'balanceStatus',
      balanceId:'balanceValue', eyeId:'toggleEye', modalId:'pinModal', pinInputId:'pinInput',
      pinErrorId:'pinError', cancelId:'cancelPinBtn', verifyBtnId:'verifyPinBtn', dateId:'balanceAsOf',
      url:'<%= request.getContextPath() %>/balance-api',
      loginUrl:'<%= request.getContextPath() %>/login.jsp'
    });
  }
});
</script>
</body>
</html>