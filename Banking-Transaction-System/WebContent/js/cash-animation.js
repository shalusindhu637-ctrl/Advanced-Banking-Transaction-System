/* SecureBank cash animation controller (visual only). Backend response is source of truth. */
(function(){
  "use strict";
  var prefersReduced = window.matchMedia && window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  var inr = new Intl.NumberFormat('en-IN', { style:'currency', currency:'INR', minimumFractionDigits:2, maximumFractionDigits:2 });

  function formatINR(n){ var v = Number(n); return isFinite(v) ? inr.format(v) : inr.format(0); }
  function parseAmount(v){ var n = Number(String(v==null?'':v).replace(/,/g,'')); return isFinite(n) ? n : NaN; }
  function byId(id){ return document.getElementById(id); }
  function esc(s){ return String(s==null?'':s).replace(/[&<>"']/g,function(c){return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c];}); }

  function animateCounter(el, from, to, ms){
    if(!el) return Promise.resolve();
    from = Number(from)||0; to = Number(to)||0;
    if(prefersReduced || ms<=0){ el.textContent = formatINR(to); return Promise.resolve(); }
    var start = null, diff = to - from;
    return new Promise(function(resolve){
      function step(ts){ if(!start) start=ts; var p=Math.min(1,(ts-start)/ms); var e=1-Math.pow(1-p,3); el.textContent=formatINR(from + diff*e); if(p<1) requestAnimationFrame(step); else { el.textContent=formatINR(to); resolve(); } }
      requestAnimationFrame(step);
    });
  }

  function setStatus(box, html, kind){ if(!box) return; box.innerHTML = html||''; box.className = 'cash-status' + (kind?' '+kind:''); }
  function showFallback(stage, reason){ if(!stage) return; var fb = stage.querySelector('.cash-fallback'); var cv = stage.querySelector('.cash-canvas'); if(cv) cv.style.display='none'; if(fb){ fb.classList.add('show'); if(reason) fb.setAttribute('data-reason', reason); } }

  function showAmountLabel(stage, mode, amount){
    if(!stage) return;
    var el = stage.querySelector('.cash-float-amount');
    if(!el){ el = document.createElement('div'); el.className='cash-float-amount'; var vp = stage.querySelector('.cash-viewport'); if(vp) vp.appendChild(el); else stage.appendChild(el); }
    var sign = mode === 'withdraw' ? '-' : (mode === 'balance' ? '' : '+');
    el.textContent = sign + formatINR(amount);
    el.className = 'cash-float-amount ' + (mode==='withdraw' ? 'neg' : 'pos');
    if(prefersReduced){ el.classList.add('show-static'); return; }
    el.classList.remove('run'); void el.offsetWidth; el.classList.add('run');
  }
  function clearAmountLabel(stage){ if(!stage) return; var el = stage.querySelector('.cash-float-amount'); if(el){ el.classList.remove('run','show-static'); el.textContent=''; } }

  function runCssSequence(stage, mode, amount){
    return new Promise(function(resolve){
      if(!stage || prefersReduced){ resolve(); return; }
      var fb = stage.querySelector('.cash-fallback'); if(fb) fb.classList.remove('show');
      showAmountLabel(stage, mode, amount);
      // Scale fallback visual count by amount (optimized grouping) without per-note objects.
      var n = 3; var a=Number(amount)||0; if(a>50000) n=5; else if(a>10000) n=4; else if(a>2000) n=3; else n=2;
      var notes = stage.querySelectorAll('.cash-fallback .note'); notes.forEach(function(nd,i){ nd.style.display = i < n ? 'flex' : 'none'; });
      stage.classList.remove('anim-deposit','anim-withdraw');
      void stage.offsetWidth;
      stage.classList.add(mode==='withdraw' ? 'anim-withdraw' : 'anim-deposit');
      setTimeout(function(){ stage.classList.remove('anim-deposit','anim-withdraw'); resolve(); }, 1550);
    });
  }

  function initThreeIfAvailable(stage){
    try{
      if(!window.THREE || !stage) return null;
      var canvas = stage.querySelector('canvas.cash-canvas'); if(!canvas) return null;
      var scene = window.SecureBankCashScene && window.SecureBankCashScene.create;
      if(typeof scene !== 'function') return null;
      return scene(canvas, stage);
    }catch(e){ return null; }
  }

  function postForm(url, data){
    return fetch(url, {
      method:'POST', credentials:'same-origin',
      headers:{'Content-Type':'application/x-www-form-urlencoded; charset=UTF-8','X-Requested-With':'XMLHttpRequest','Accept':'application/json'},
      body: new URLSearchParams(data).toString()
    }).then(function(res){
      var ct = res.headers.get('content-type')||'';
      if(ct.indexOf('application/json') !== -1) return res.json().then(function(j){ j._http=res.status; return j; });
      return { ok:false, success:false, _http:res.status, message: res.status===401 ? 'Authentication required. Please sign in again.' : 'Unexpected server response. Please try again.' };
    }).catch(function(){ return { ok:false, success:false, _http:0, message:'Network error. Please check your connection and try again.' }; });
  }

  function setup(opts){
    var form = byId(opts.formId), stage = byId(opts.stageId), status = byId(opts.statusId), balEl = byId(opts.balanceId), amtEl = byId(opts.amountId), csrfEl = byId(opts.csrfId), pinEl = byId(opts.pinId);
    if(!form || !stage) return;
    if(form.dataset && form.dataset.sbBound==='1') return; if(form.dataset) form.dataset.sbBound='1';
    var three = initThreeIfAvailable(stage);
    try{ console.info('[SecureBankCash] init', { mode: opts.mode, three: !!three, reducedMotion: prefersReduced }); }catch(e){}
    if(!three){ showFallback(stage, (window.THREE && !hasWebGL && hasWebGL()) ? 'webgl-unavailable' : 'webgl-unavailable'); }

    var busy = false;
    form.addEventListener('submit', function(ev){
      if(!window.fetch || !window.URLSearchParams) return; // progressive enhancement: normal POST
      ev.preventDefault();
      if(busy) return;
      var amount = parseAmount(amtEl && amtEl.value);
      var prev = parseAmount(balEl && (balEl.getAttribute('data-balance') || balEl.textContent));
      if(!isFinite(amount) || amount <= 0){ setStatus(status, '<div class="tx-error">'+esc('Please enter a valid amount greater than zero.')+'</div>'); return; }
      if(opts.mode==='withdraw'){
        if(!pinEl || !/^\d{4}$|^\d{6}$/.test(String(pinEl.value||''))){ setStatus(status, '<div class="tx-error">'+esc('Please enter your 4- or 6-digit transaction PIN.')+'</div>'); return; }
        if(isFinite(prev) && amount > prev){ /* let server be source of truth too, but avoid obvious bad UX */ }
      }
      busy = true; form.querySelectorAll('button, input').forEach(function(x){ x.disabled=true; });
      try{ console.info('[SecureBankCash] tx request started', { mode: opts.mode, url: opts.url }); }catch(e){}
      setStatus(status, '<span class="processing">Processing securely… please wait.</span>');
      if(three && three.playProcessing) { try{ three.playProcessing(opts.mode); }catch(e){} }
      else { runCssSequence(stage, opts.mode==='withdraw'?'deposit':'deposit'); /* subtle pre-animation */ }

      var payload = { amount: String(amount), csrfToken: csrfEl ? csrfEl.value : '' };
      if(opts.mode==='withdraw' && pinEl) payload.pin = String(pinEl.value||'');

      postForm(opts.url, payload).then(function(res){
        try{ console.info('[SecureBankCash] backend response', { http: res && res._http, ok: !!(res && (res.ok===true || res.success===true)), code: res && res.code }); }catch(e){}
        var ok = !!(res && (res.ok===true || res.success===true));
        if(!ok){
          if(three && three.stop) try{ three.stop(); }catch(e){}
          stage.classList.remove('anim-deposit','anim-withdraw');
          clearAmountLabel(stage);
          var msg = (res && res.message) ? res.message : 'Transaction failed. Please try again.';
          setStatus(status, '<div class="tx-error" role="alert">'+esc(msg)+'</div>');
          if(res && res.csrfToken && csrfEl) csrfEl.value = res.csrfToken;
          if(pinEl) pinEl.value='';
          return;
        }
        // Backend is source of truth: use backend-confirmed amount/final balance.
        var confirmedAmount = Number(res.amount);
        if(!isFinite(confirmedAmount) || confirmedAmount <= 0) confirmedAmount = amount;
        var finalBal = Number(res.balance);
        if(!isFinite(finalBal)) finalBal = prev; // should not happen
        try{ console.info('[SecureBankCash] animation trigger (confirmed)', { mode: opts.mode, amount: confirmedAmount, finalBalance: finalBal, transactionId: res.transactionId }); }catch(e){}
        var seq = playCashTransactionAnimation(opts.mode, confirmedAmount, finalBal, res.transactionId, { stage: stage, three: three });
        Promise.resolve(seq).then(function(){
          return animateCounter(balEl, prev, finalBal, prefersReduced?0:1200);
        }).then(function(){
          if(balEl){ balEl.setAttribute('data-balance', String(finalBal)); balEl.textContent = formatINR(finalBal); }
          if(res.csrfToken && csrfEl) csrfEl.value = res.csrfToken;
          if(pinEl) pinEl.value='';
          var receipt = '<div class="tx-receipt" role="status"><strong>' + esc(opts.mode==='withdraw'?'Withdrawal Successful':'Deposit Successful') + '</strong><br>'
            + 'Amount: ' + esc(formatINR(confirmedAmount)) + '<br>'
            + 'Updated balance: <span class="balance-counter">' + esc(formatINR(finalBal)) + '</span><br>'
            + 'Transaction ID: ' + esc(res.transactionId||'—') + '<br>'
            + 'Date: ' + esc(res.date||'—') + '</div>';
          setStatus(status, receipt);
          try{ console.info('[SecureBankCash] animation completed', { mode: opts.mode, transactionId: res.transactionId }); }catch(e){}
        });
      }).finally(function(){ busy=false; form.querySelectorAll('button, input').forEach(function(x){ x.disabled=false; }); });
    });
  }

  /* Reusable entry point (visual only). Must be called ONLY after backend confirms success.
     transactionType: 'deposit'|'withdraw'|'transfer' (transfer requires existing transfer UI/APIs)
     amount/finalBalance/transactionId must come from backend-confirmed values. */
  function playCashTransactionAnimation(transactionType, amount, finalBalance, transactionId, ctx){
    ctx = ctx || {};
    var mode = transactionType === 'withdraw' ? 'withdraw' : (transactionType === 'transfer' ? 'transfer' : 'deposit');
    var stage = ctx.stage;
    var three = ctx.three;
    if(mode === 'transfer'){
      // No existing transfer backend/UI found in this project; do not fake a transfer.
      return Promise.resolve();
    }
    showAmountLabel(stage, mode, amount);
    if(three && three.playSuccess){ try { return Promise.resolve(three.playSuccess(mode, amount)); } catch(e){} }
    return runCssSequence(stage, mode, amount);
  }

  /* Balance inquiry (visual only): reveals ONLY the backend-confirmed balance after server-side PIN verification.
     IDLE -> FETCHING_BALANCE (caller) -> BACKEND_SUCCESS -> BALANCE_REVEAL -> COMPLETED. */
  function setupBalance(opts){
    var btn = byId(opts.buttonId), form = byId(opts.formId), stage = byId(opts.stageId), status = byId(opts.statusId), balEl = byId(opts.balanceId), eye = byId(opts.eyeId), modal = byId(opts.modalId), pinInput = byId(opts.pinInputId), pinError = byId(opts.pinErrorId), cancel = byId(opts.cancelId), verifyBtn = byId(opts.verifyBtnId), dateEl = byId(opts.dateId);
    if(!btn || !form || !stage) return;
    if(form.dataset && form.dataset.sbBalanceBound==='1') return; if(form.dataset) form.dataset.sbBalanceBound='1';
    var three = initThreeIfAvailable(stage);
    if(!three){ try{ console.info('[SecureBankCash] balance init', { three:false }); }catch(e){} }
    var masked = '₹••••••'; var revealedValue = null; var isRevealed = false; var busy = false;
    function showModal(){ if(pinError) pinError.textContent=''; if(pinInput) pinInput.value=''; if(modal){ modal.classList.add('show'); modal.setAttribute('aria-hidden','false'); } if(pinInput) pinInput.focus(); }
    function hideModal(){ if(modal){ modal.classList.remove('show'); modal.setAttribute('aria-hidden','true'); } }
    function render(){ if(!balEl) return; if(revealedValue && isRevealed){ balEl.textContent=revealedValue; balEl.className='balance-visible'; if(eye) eye.innerHTML='<svg class="eye-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M17.94 17.94A10.94 10.94 0 0 1 12 20C7 20 2.73 16.11 1 12c.74-1.76 1.86-3.33 3.24-4.62"/><path d="M1 1l22 22"/><path d="M9.53 9.53A3 3 0 0 0 12 15a3 3 0 0 0 2.47-1.47"/></svg>'; } else { balEl.textContent=masked; balEl.className='balance-masked'; if(eye) eye.innerHTML='<svg class="eye-ic" viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12z"/><circle cx="12" cy="12" r="3"/></svg>'; } if(eye) eye.disabled = !revealedValue; }
    if(eye) eye.addEventListener('click', function(){ if(!revealedValue) return; isRevealed=!isRevealed; render(); });
    btn.addEventListener('click', function(){ setStatus(status,'',null); if(dateEl) dateEl.textContent=''; clearAmountLabel(stage); showModal(); });
    if(cancel) cancel.addEventListener('click', hideModal);
    if(modal) modal.addEventListener('click', function(e){ if(e.target===modal) hideModal(); });
    form.addEventListener('submit', function(e){
      e.preventDefault(); if(busy) return;
      if(pinError) pinError.textContent='';
      if(pinInput && !/^\d{4}$|^\d{6}$/.test(String(pinInput.value||''))){ if(pinError) pinError.textContent='Enter a 4- or 6-digit PIN.'; return; }
      // Read values BEFORE disabling; disabled controls are omitted from FormData (would drop PIN).
      var pinVal = pinInput ? String(pinInput.value||'') : '';
      var csrfVal = form.querySelector('input[name="csrfToken"]');
      var payload = new URLSearchParams({ csrfToken: csrfVal ? csrfVal.value : '', pin: pinVal }).toString();
      busy=true; [btn,verifyBtn,pinInput].forEach(function(x){ if(x) x.disabled=true; });
      try{ console.info('[SecureBankCash] balance request started'); }catch(e){}
      setStatus(status,'<span class="processing">Verifying account &amp; fetching balance…</span>',null);
      postForm(opts.url, payload).then(function(res){
        try{ console.info('[SecureBankCash] balance backend response', { http: res && res._http, ok: !!(res && (res.ok===true||res.success===true)), code: res && res.code }); }catch(e){}
        var ok = !!(res && (res.ok===true || res.success===true));
        if(res && res._http===401){ window.location = opts.loginUrl || (location.origin + '/login.jsp'); return; }
        if(!ok){
          if(three && three.stop) try{ three.stop(); }catch(e){}
          clearAmountLabel(stage);
          var msg = (res && res.message) ? res.message : 'Unable to retrieve balance.';
          if(pinError) pinError.textContent = msg; setStatus(status, '<div class="tx-error" role="alert">'+esc(msg)+'</div>');
          if(res && res.code==='NOT_SET'){ hideModal(); }
          return;
        }
        // Backend-confirmed only. Parse formatted "₹12345.67" (or plain number) — do NOT fabricate.
        var raw = res.balance; var num = NaN;
        if(typeof raw === 'number') num = raw;
        else if(raw != null){ num = Number(String(raw).replace(/[^0-9.\-]/g,'')); }
        if(!isFinite(num)){ setStatus(status,'<div class="tx-error" role="alert">Unable to read balance from server response.</div>'); return; }
        hideModal();
        setStatus(status,'<span class="processing">Revealing available balance…</span>',null);
        if(three && three.playProcessing) try{ three.playProcessing('balance'); }catch(e){}
        var disp = formatINR(num);
        showAmountLabel(stage, 'balance', num);
        var seq = (three && three.playSuccess) ? three.playSuccess('balance', num) : runCssSequence(stage, 'balance', num);
        Promise.resolve(seq).then(function(){
          if(balEl){ balEl.className='balance-visible'; balEl.setAttribute('aria-label','Available balance'); }
          return animateCounter(balEl, 0, num, prefersReduced?0:1400);
        }).then(function(){
          revealedValue = disp; isRevealed = true; render();
          if(dateEl){ dateEl.textContent = 'As of ' + (res.date || new Date().toLocaleString('en-IN')); }
          setStatus(status,'<div class="tx-receipt" role="status"><strong>Available Balance</strong><br><span class="balance-counter">'+esc(disp)+'</span></div>');
          try{ console.info('[SecureBankCash] balance reveal completed'); }catch(e){}
        });
      }).catch(function(){ if(pinError) pinError.textContent='Unable to retrieve balance. Please try again.'; setStatus(status,'<div class="tx-error" role="alert">Unable to retrieve balance. Please try again.</div>'); })
        .finally(function(){ busy=false; [btn,verifyBtn,pinInput].forEach(function(x){ if(x) x.disabled=false; }); if(pinInput) pinInput.value=''; });
    });
    render();
  }

  window.SecureBankCash = { setup: setup, setupBalance: setupBalance, formatINR: formatINR, playCashTransactionAnimation: playCashTransactionAnimation };
})();