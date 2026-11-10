/* SecureBank Three.js cash scene (visual simulation only; generic note-inspired textures, no official security features). */
(function(){
  "use strict";
  function hasWebGL(){
    try{ var c=document.createElement('canvas'); return !!(window.WebGLRenderingContext && (c.getContext('webgl')||c.getContext('experimental-webgl'))); }catch(e){ return false; }
  }
  function amountInfo(amount){
    var a=Math.max(0, Number(amount)||0);
    // Optimized visual grouping (never 1 object per note/rupee): bundle count scales with amount, capped.
    var bundles = a <= 0 ? 3 : Math.min(12, Math.max(2, Math.ceil(Math.log10(a + 1) * 2.2)));
    var scale = Math.min(1.15, 0.82 + Math.min(a, 100000) / 220000);
    return { bundles: bundles, scale: scale };
  }
  function makeNoteTexture(THREE, amount){
    var c=document.createElement('canvas'); c.width=512; c.height=256; var g=c.getContext('2d');
    var a=Math.max(0, Number(amount)||0);
    // Generic simulated note-inspired texture (no official/security features).
    var grd=g.createLinearGradient(0,0,512,256); grd.addColorStop(0,'#f0c46a'); grd.addColorStop(0.5,'#d4a017'); grd.addColorStop(1,'#b45309');
    g.fillStyle=grd; g.fillRect(0,0,512,256);
    g.strokeStyle='rgba(30,41,59,0.22)'; g.lineWidth=10; g.strokeRect(14,14,484,228);
    g.fillStyle='rgba(255,255,255,0.16)'; g.beginPath(); g.arc(256,128,68,0,Math.PI*2); g.fill();
    g.fillStyle='rgba(30,41,59,0.52)'; g.font='bold 78px sans-serif'; g.textAlign='center'; g.textBaseline='middle'; g.fillText('\u20B9',256,132);
    var amt = a>0 ? ('\u20B9'+Math.round(a).toLocaleString('en-IN')) : '';
    g.fillStyle='rgba(30,41,59,0.42)'; g.font='bold 26px sans-serif'; g.textAlign='right'; g.fillText(amt,488,44);
    g.textAlign='left'; g.fillText(amt,24,228);
    g.fillStyle='rgba(30,41,59,0.30)'; g.font='bold 22px sans-serif'; g.textAlign='center'; g.fillText('SPECIMEN \u2022 SIMULATED',256,36);
    g.fillStyle='rgba(30,41,59,0.22)'; g.font='14px sans-serif'; g.fillText('EDUCATIONAL SIMULATION \u2014 NOT LEGAL TENDER',256,236);
    var t=new THREE.CanvasTexture(c); t.anisotropy=2; t.needsUpdate=true; return t;
  }
  function create(canvas, stage){
    if(!window.THREE || !hasWebGL() || !canvas) return null;
    var THREE=window.THREE;
    var renderer=new THREE.WebGLRenderer({canvas:canvas, antialias:true, alpha:true, powerPreference:'low-power'});
    var w=canvas.clientWidth||300, h=canvas.clientHeight||220; renderer.setSize(w,h,false); renderer.setPixelRatio(Math.min(window.devicePixelRatio||1,2));
    var scene=new THREE.Scene();
    var camera=new THREE.PerspectiveCamera(38, w/h, 0.1, 100); camera.position.set(0,1.6,5.2); camera.lookAt(0,0.7,0);
    scene.add(new THREE.AmbientLight(0xffffff,0.75));
    var dir=new THREE.DirectionalLight(0xffffff,0.7); dir.position.set(3,4,3); scene.add(dir);
    var base=new THREE.Mesh(new THREE.BoxGeometry(2.4,1.4,1.2), new THREE.MeshStandardMaterial({color:0x334155, metalness:0.15, roughness:0.6}));
    base.position.y=0.3; scene.add(base);
    var screen=new THREE.Mesh(new THREE.PlaneGeometry(1.5,0.55), new THREE.MeshStandardMaterial({color:0x0b1220, emissive:0x0f172a, roughness:0.9}));
    screen.position.set(0,0.72,0.61); scene.add(screen);
    var slot=new THREE.Mesh(new THREE.BoxGeometry(1.2,0.08,0.12), new THREE.MeshStandardMaterial({color:0x020617, roughness:1}));
    slot.position.set(0,-0.05,0.62); scene.add(slot);
    var noteGeo=new THREE.PlaneGeometry(0.9,0.45);
    var sharedTex = makeNoteTexture(THREE, 0);
    var sharedMat = new THREE.MeshStandardMaterial({map:sharedTex, side:THREE.DoubleSide, roughness:0.7, metalness:0.0});
    var notes=[]; var group=new THREE.Group(); scene.add(group);
    var activeAmount = 0;
    function clearNotes(){
      while(group.children.length){
        var ch=group.children[0];
        if(ch.material && ch.material.map && ch.material.map !== sharedTex){ try{ ch.material.map.dispose(); }catch(e){} }
        if(ch.material && ch.material !== sharedMat){ try{ ch.material.dispose(); }catch(e){} }
        group.remove(ch);
      }
      notes=[];
    }
    function makeNotes(n, amount){
      clearNotes();
      var info=amountInfo(amount); var count = n || info.bundles; activeAmount = Math.max(0, Number(amount)||0);
      // Per-run texture reflects the exact confirmed amount (visual only; backend remains source of truth).
      var runTex = (activeAmount>0) ? makeNoteTexture(THREE, activeAmount) : sharedTex;
      for(var i=0;i<count;i++){
        var mat = sharedMat.clone(); mat.map = runTex; mat.transparent=true; mat.opacity=1;
        var m2=new THREE.Mesh(noteGeo, mat);
        var col = i - (count-1)/2;
        m2.position.set(col*0.055, 2.25 + i*0.05, 0.82 + (i%3)*0.01);
        m2.rotation.set(-0.2, 0, col*0.018);
        m2.scale.set(info.scale, info.scale, info.scale);
        m2.visible=false; group.add(m2); notes.push({m:m2, baseX:col*0.055});
      }
    }
    makeNotes(amountInfo(0).bundles, 0);
    var raf=null, playing=false, startTs=0, mode='deposit';
    function resize(){ var w2=canvas.clientWidth||300,h2=canvas.clientHeight||220; if(w2<10||h2<10) return; renderer.setSize(w2,h2,false); camera.aspect=w2/h2; camera.updateProjectionMatrix(); }
    var ro = (window.ResizeObserver)? new ResizeObserver(resize):null; if(ro) ro.observe(canvas);
    function render(ts){
      if(!playing) return; if(!startTs) startTs=ts; var elapsed=ts-startTs;
      var stagger = notes.length > 8 ? 140 : 200;
      notes.forEach(function(o,idx){
        var local=(elapsed - idx*stagger)/1050; if(local<0){ o.m.visible=false; return; } if(local>1){ o.m.visible=false; return; }
        o.m.visible=true; var e=1-Math.pow(1-Math.min(1,local),3);
        if(mode==='withdraw'){ o.m.position.set(o.baseX, -0.05 + e*1.85, 0.66 + e*0.95); o.m.rotation.x = -0.1 + e*0.28; o.m.rotation.z = (idx%2?1:-1)*e*0.16; }
        else { o.m.position.set(o.baseX, 1.95 - e*2.0, 0.95 - e*0.32); o.m.rotation.x = -0.2 + e*0.16; o.m.rotation.z = (idx%2?1:-1)*(0.06 - e*0.06); if(local>0.86) o.m.visible = local < 0.985; }
        o.m.material.opacity = local>0.9 ? Math.max(0,1-(local-0.9)/0.1) : 1; o.m.material.transparent=true;
      });
      renderer.render(scene,camera); raf=requestAnimationFrame(render);
      if(elapsed > (notes.length-1)*stagger + 1200){ playing=false; }
    }
    function play(m, amount){ mode=m||'deposit'; makeNotes(null, amount); startTs=0; playing=true; if(raf) cancelAnimationFrame(raf); raf=requestAnimationFrame(render); var dur=(notes.length-1)*(notes.length>8?140:200)+1200; return new Promise(function(res){ setTimeout(res,dur); }); }
    return {
      playProcessing:function(m){ resize(); renderer.render(scene,camera); },
      playSuccess:function(m, amount){ resize(); return play(m, amount); },
      stop:function(){ playing=false; if(raf) cancelAnimationFrame(raf); notes.forEach(function(o){o.m.visible=false;}); renderer.render(scene,camera); },
      dispose:function(){ playing=false; if(raf) cancelAnimationFrame(raf); if(ro) ro.disconnect(); clearNotes(); noteGeo.dispose(); sharedMat.map&&sharedMat.map.dispose(); sharedMat.dispose(); renderer.dispose(); }
    };
  }
  window.SecureBankCashScene = { create:create };
})();
