'use strict';
const $ = id => document.getElementById(id);
let token = sessionStorage.getItem('familyQuestToken') || '';
let ready = false, busy = false;
function notice(text) { $('notice').textContent = text || ''; }
function loginView() { token = ''; sessionStorage.removeItem('familyQuestToken'); ready = false; $('login').hidden = false; $('dashboard').hidden = true; }
async function api(path, data) {
  const headers = {Authorization: 'Bearer ' + token};
  const init = {headers, cache:'no-store'};
  if (data !== undefined) { init.method='POST'; headers['Content-Type']='application/json'; init.body=JSON.stringify(data); }
  const res = await fetch('/api/' + path, init);
  const body = await res.json();
  if (!res.ok) { if (res.status===401 && path!=='login') loginView(); throw new Error(body.error || '無法完成操作'); }
  return body;
}
function render(s, form=false) {
  $('wordlist-title').textContent=s.wordlistTitle;
  $('login').hidden=true; $('dashboard').hidden=false;
  const secs=Math.max(0,s.remainingSeconds);
  $('remaining').textContent=String(Math.floor(secs/60)).padStart(2,'0')+':'+String(secs%60).padStart(2,'0');
  $('bank').textContent=String(s.bankMinutes);
  $('progress').textContent=`每天 ${s.startTime} 後開放。已答對 ${s.progress} / ${s.questions} 題，每回合可存入 ${s.minutes} 分鐘。`;
  $('gate').textContent=!s.enabled?'練習模式':s.serviceConnected?'限制啟用中':'⚠ 限制服務未連線';
  if (!form) return;
  $('level').replaceChildren();
  for(const level of s.levels) { const option=document.createElement('option'); option.value=level.id; option.textContent=`${level.name} · ${level.count} 題`; $('level').append(option); }
  $('questions').value=s.questions; $('minutes').value=s.minutes; $('level').value=s.level; $('enabled').checked=s.enabled;
  $('start-time').value=s.startTime;
  $('apps').replaceChildren(); $('launch-app').replaceChildren();
  for(const app of s.apps) {
    const label=document.createElement('label'); label.className='check';
    const box=document.createElement('input'); box.type='checkbox'; box.value=app.package; box.checked=app.restricted;
    const name=document.createElement('span'); name.textContent=app.label; label.append(box,name); $('apps').append(label);
    const option=document.createElement('option'); option.value=app.package; option.textContent=app.label; $('launch-app').append(option);
  }
  ready=true;
}
async function act(fn) {
  if(busy) return; busy=true; notice('');
  document.querySelectorAll('button').forEach(b=>b.disabled=true);
  try { await fn(); } catch(e) { notice(e.message || '連線失敗，請確認手機與電視在相同區網。'); }
  finally { busy=false; document.querySelectorAll('button').forEach(b=>b.disabled=false); }
}
$('sign-in').onclick=()=>act(async()=>{const res=await api('login',{pin:$('pin').value}); token=res.token; $('pin').value=''; sessionStorage.setItem('familyQuestToken',token); render(await api('state'),true);});
$('pin').onkeydown=e=>{if(e.key==='Enter') $('sign-in').click();};
$('save').onclick=()=>act(async()=>{const s=await api('config',{questions:Number($('questions').value),minutes:Number($('minutes').value),level:$('level').value,startTime:$('start-time').value,enabled:$('enabled').checked,apps:Array.from($('apps').querySelectorAll('input:checked'),e=>e.value)}); render(s,true); notice('規則已儲存。');});
for(const [id,path,data] of [['start','start',{}],['stop','stop',{}],['home','home',{}],['up','volume',{direction:1}],['down','volume',{direction:-1}]]) $(id).onclick=()=>act(async()=>render(await api(path,data)));
$('give-bonus').onclick=()=>act(async()=>render(await api('bonus',{minutes:Number($('bonus').value)})));
$('launch').onclick=()=>act(async()=>render(await api('launch',{package:$('launch-app').value})));
$('logout').onclick=()=>act(async()=>{await api('logout',{});loginView();});
async function refresh(){if(!token||busy||document.hidden)return;try{render(await api('state'),!ready);}catch(e){notice(e.message);}}
if(token) refresh();
setInterval(refresh,5000);
