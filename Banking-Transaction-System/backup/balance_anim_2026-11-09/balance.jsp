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
            <a href="<%= request.getContextPath() %>/balance" class="nav-item active">💳 Balance</a>
            <a href="<%= request.getContextPath() %>/set-pin" class="nav-item">🔐 Transaction PIN</a>
            <a href="<%= request.getContextPath() %>/transactions" class="nav-item">📋 Transaction History</a>
            <a href="<%= request.getContextPath() %>/logout" class="nav-item">🚪 Logout</a>
        </nav>
    </aside>
    <main class="main-content">
        <div class="top-bar"><h1>Available Balance</h1></div>
        <div class="card balance-card">
            <div class="card-header"><h3>Check Balance</h3></div>
            <div class="card-body" style="text-align:center; padding:2.5rem;">
                <p class="text-muted mb-2">Available Balance</p>
                <div class="balance-display-row">
                    <span id="balanceValue" class="balance-masked">₹••••••</span>
                    <button type="button" id="toggleEye" class="eye-btn" aria-label="Show or hide balance" title="Reveal/Hide" disabled>👁</button>
                </div>
                <p class="text-muted mt-2 mb-3">Account: <%= customer.getAccountNumber() %></p>
                <div id="balanceStatus" class="balance-status" aria-live="polite"></div>
                <button type="button" id="checkBalanceBtn" class="btn btn-check-balance">Check Balance</button>
                <% if (!hasPin) { %>
                    <p class="mt-3 mb-0"><a class="text-link" href="<%= request.getContextPath() %>/set-pin">Set your transaction PIN</a> to securely view your balance.</p>
                <% } %>
                <p class="mt-3 small text-muted mb-0">Balance is retrieved only after server-side PIN verification. Use HTTPS in production (localhost HTTP is for development only).</p>
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

<script>
(function(){
  const CTX = "<%= request.getContextPath() %>";
  const masked = "₹••••••";
  let revealedValue = null, isRevealed = false;
  const balanceValue = document.getElementById('balanceValue');
  const eye = document.getElementById('toggleEye');
  const status = document.getElementById('balanceStatus');
  const btn = document.getElementById('checkBalanceBtn');
  const modal = document.getElementById('pinModal');
  const pinForm = document.getElementById('pinForm');
  const pinInput = document.getElementById('pinInput');
  const pinError = document.getElementById('pinError');
  const cancel = document.getElementById('cancelPinBtn');
  const verifyBtn = document.getElementById('verifyPinBtn');

  function showModal(){ pinError.textContent=''; pinInput.value=''; modal.classList.add('show'); modal.setAttribute('aria-hidden','false'); pinInput.focus(); }
  function hideModal(){ modal.classList.remove('show'); modal.setAttribute('aria-hidden','true'); }
  function setStatus(msg, ok){ status.textContent=msg||''; status.className='balance-status '+(ok===true?'ok':ok===false?'err':''); }
  function render(){
    if(revealedValue && isRevealed){ balanceValue.textContent=revealedValue; balanceValue.className='balance-visible'; eye.textContent='🙈'; }
    else { balanceValue.textContent=masked; balanceValue.className='balance-masked'; eye.textContent='👁'; }
    eye.disabled = !revealedValue;
  }
  eye.addEventListener('click', function(){ if(!revealedValue) return; isRevealed=!isRevealed; render(); });
  btn.addEventListener('click', function(){ setStatus('',null); showModal(); });
  cancel.addEventListener('click', hideModal);
  modal.addEventListener('click', function(e){ if(e.target===modal) hideModal(); });
  pinForm.addEventListener('submit', async function(e){
    e.preventDefault();
    pinError.textContent='';
    if(!/^\d{4}$|^\d{6}$/.test(pinInput.value)){ pinError.textContent='Enter a 4- or 6-digit PIN.'; return; }
    verifyBtn.disabled=true; btn.disabled=true; setStatus('Verifying…', null);
    try{
      const body = new URLSearchParams(new FormData(pinForm));
      const res = await fetch(CTX+'/balance-api', {method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'}, body: body.toString(), credentials:'same-origin'});
      let data; try{ data = await res.json(); }catch(_){ data={ok:false,message:'Unable to verify PIN.'}; }
      if(res.status===401){ window.location = CTX+'/login.jsp'; return; }
      if(data && data.ok && data.balance){
        revealedValue = data.balance; isRevealed = true; hideModal(); setStatus('Balance verified.', true);
      } else {
        pinError.textContent = (data && data.message) ? data.message : 'Unable to verify PIN.';
        setStatus('', null);
        if(data && data.code==='NOT_SET'){ hideModal(); setStatus('Please set your transaction PIN first.', false); }
      }
    } catch(err){ pinError.textContent='Unable to verify PIN. Please try again.'; setStatus('', null); }
    finally { verifyBtn.disabled=false; btn.disabled=false; render(); pinInput.value=''; }
  });
  render();
})();
</script>
<script src="js/app.js"></script>
</body>
</html>