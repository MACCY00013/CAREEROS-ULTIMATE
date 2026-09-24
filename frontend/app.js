const state = { skills: [], careers: [], jobs: [] };
const app = document.querySelector('#app');

async function api(path, options = {}) {
  const response = await fetch(`/api/v1${path}`, { headers: { 'Content-Type': 'application/json' }, ...options });
  if (!response.ok) throw new Error((await response.json()).detail || 'Request failed');
  return response.json();
}

function render(view = 'home') {
  document.querySelectorAll('.tab').forEach((tab) => tab.classList.toggle('active', tab.dataset.view === view));
  if (view === 'home') app.innerHTML = `<section class="hero"><div><p class="eyebrow">YOUR OPERATING SYSTEM FOR MOMENTUM</p><h2>A calmer route from learning to landing the role.</h2><p>Track capability, discover aligned paths, and keep moving even when the connection drops.</p></div><div class="metric"><strong>${state.skills.length || '—'}</strong><span>skills in your vault</span></div></section><section class="grid"><article><span class="label">Next move</span><h3>Choose a target role</h3><p>Career paths turn your skills into a practical sequence of next steps.</p><button class="primary" data-go="careers">Explore paths →</button></article><article><span class="label">Offline ready</span><h3>Your work stays yours</h3><p>Core pages are cached locally. Queued changes sync when you reconnect.</p><span class="pill">Service worker enabled</span></article></section>`;
  if (view === 'skills') app.innerHTML = `<div class="section-head"><div><p class="eyebrow">SKILL VAULT</p><h2>Capabilities worth carrying forward.</h2></div><form id="skill-form"><input name="name" placeholder="Add a skill" required maxlength="120"><button class="primary">Add</button></form></div><div class="skill-list">${state.skills.map((skill) => `<button class="skill"><span>${skill.name}</span><small>${skill.level}</small></button>`).join('')}</div>`;
  if (view === 'careers') app.innerHTML = `<div class="section-head"><div><p class="eyebrow">CAREER PATHS</p><h2>Roles shaped around your strengths.</h2></div></div><div class="career-list">${state.careers.map((career) => `<article><span class="label">TARGET ROLE</span><h3>${career.title}</h3><div class="chips">${career.skills.map((skill) => `<span>${skill}</span>`).join('')}</div></article>`).join('')}</div>`;
  if (view === 'jobs') app.innerHTML = `<div class="section-head"><div><p class="eyebrow">LIVE JOB FEED</p><h2>Opportunities, without the noise.</h2></div><span class="pill">${state.jobs.length} cached listings</span></div><div class="job-list">${state.jobs.map((job) => `<article><div><span class="label">${job.source}</span><h3>${job.title}</h3><p>${job.company} · ${job.location}</p></div><button class="icon-button" aria-label="Bookmark ${job.title}">☆</button></article>`).join('')}</div>`;
  if (view === 'ai') app.innerHTML = `<div class="ai-panel"><p class="eyebrow">AI HUB</p><h2>Think with the right model.</h2><p>Prompts stay in your local queue while offline and are sent through the server when an approved provider is configured.</p><form id="ai-form"><select name="provider"><option value="chatgpt">ChatGPT</option><option value="gemini">Gemini</option><option value="superhuman-go">Superhuman GO</option></select><textarea name="prompt" placeholder="What are you working through?" required></textarea><button class="primary">Queue prompt</button></form><p id="ai-message" class="form-message"></p></div>`;
}

async function load() {
  try { [state.skills, state.careers, state.jobs] = await Promise.all([(api('/skills')).then((x) => x.items), (api('/careers')).then((x) => x.items), (api('/jobs/feed')).then((x) => x.items)]); } catch { app.innerHTML = '<div class="empty"><h2>You are offline.</h2><p>Reconnect once to load your workspace, then the core experience will remain available here.</p></div>'; }
  render();
}

document.addEventListener('click', (event) => { const tab = event.target.closest('[data-view]'); const go = event.target.closest('[data-go]'); if (tab) render(tab.dataset.view); if (go) render(go.dataset.go); });
document.addEventListener('submit', async (event) => { event.preventDefault(); if (event.target.id === 'skill-form') { await api('/skills/sync', { method: 'POST', body: JSON.stringify({ name: new FormData(event.target).get('name') }) }); await load(); render('skills'); } if (event.target.id === 'ai-form') { const data = Object.fromEntries(new FormData(event.target)); const message = document.querySelector('#ai-message'); try { await api('/ai/hub/process', { method: 'POST', body: JSON.stringify(data) }); message.textContent = 'Prompt queued securely.'; } catch (error) { message.textContent = error.message; } } });
if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js');
load();