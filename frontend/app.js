const state = { skills: [], careers: [], jobs: [], roadmap: [], applications: [], dashboard: {} };
const app = document.querySelector('#app');
const CACHE_KEY = 'careeros-cache-v1';
const OUTBOX_KEY = 'careeros-outbox-v1';

function escapeHtml(value) {
  return String(value).replace(/[&<>'"]/g, (character) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[character]));
}

function persist() { localStorage.setItem(CACHE_KEY, JSON.stringify(state)); }
function cached() { try { Object.assign(state, JSON.parse(localStorage.getItem(CACHE_KEY) || '{}')); } catch { /* Ignore corrupt local cache. */ } }
function outbox() { return JSON.parse(localStorage.getItem(OUTBOX_KEY) || '[]'); }
function queue(path, options) {
  localStorage.setItem(OUTBOX_KEY, JSON.stringify([...outbox(), { path, options }]));
  if ('serviceWorker' in navigator) navigator.serviceWorker.ready.then((registration) => registration.sync?.register('careeros-outbox'));
}

async function flushOutbox() {
  const pending = outbox();
  if (!pending.length) return;
  for (const item of pending) await api(item.path, item.options);
  localStorage.removeItem(OUTBOX_KEY);
}

async function api(path, options = {}) {
  const response = await fetch(`/api/v1${path}`, { headers: { 'Content-Type': 'application/json' }, ...options });
  if (!response.ok) throw new Error((await response.json()).detail || 'Request failed');
  return response.json();
}

function render(view = 'home') {
  document.querySelectorAll('.tab').forEach((tab) => tab.classList.toggle('active', tab.dataset.view === view));
  if (view === 'home') app.innerHTML = `<section class="hero"><div><p class="eyebrow">YOUR OPERATING SYSTEM FOR MOMENTUM</p><h2>A calmer route from learning to landing the role.</h2><p>Track capability, discover aligned paths, and keep moving even when the connection drops.</p></div><div class="metric"><strong>${state.skills.length || '—'}</strong><span>skills in your vault</span></div></section><section class="grid"><article><span class="label">Next move</span><h3>Choose a target role</h3><p>Career paths turn your skills into a practical sequence of next steps.</p><button class="primary" data-go="careers">Explore paths →</button></article><article><span class="label">Offline ready</span><h3>Your work stays yours</h3><p>Core pages are cached locally. Queued changes sync when you reconnect.</p><span class="pill">${outbox().length} changes waiting</span></article></section>`;
  if (view === 'skills') app.innerHTML = `<div class="section-head"><div><p class="eyebrow">SKILL VAULT</p><h2>Capabilities worth carrying forward.</h2></div><form id="skill-form"><input name="name" placeholder="Add a skill" required maxlength="120"><button class="primary">Add</button></form></div><div class="skill-list">${state.skills.map((skill) => `<button class="skill"><span>${escapeHtml(skill.name)}</span><small>${escapeHtml(skill.level)}</small></button>`).join('')}</div>`;
  if (view === 'careers') app.innerHTML = `<div class="section-head"><div><p class="eyebrow">CAREER PATHS</p><h2>Roles shaped around your strengths.</h2></div></div><div class="career-list">${state.careers.map((career) => `<article><span class="label">TARGET ROLE</span><h3>${escapeHtml(career.title)}</h3><div class="chips">${career.skills.map((skill) => `<span>${escapeHtml(skill)}</span>`).join('')}</div></article>`).join('')}</div>`;
  if (view === 'roadmap') app.innerHTML = `<div class="section-head"><div><p class="eyebrow">AI ROADMAP</p><h2>Small milestones, visible progress.</h2></div></div><div class="career-list">${state.roadmap.map((node) => `<article><label><input type="checkbox" data-roadmap="${escapeHtml(node.id)}" ${node.done ? 'checked' : ''}> ${escapeHtml(node.title)}</label><div class="chips">${node.children.map((child) => `<span>${escapeHtml(child)}</span>`).join('')}</div></article>`).join('')}</div>`;
  if (view === 'jobs') app.innerHTML = `<div class="section-head"><div><p class="eyebrow">LIVE JOB FEED</p><h2>Opportunities, without the noise.</h2></div><span class="pill">${state.jobs.length} cached listings</span></div><div class="job-list">${state.jobs.map((job) => `<article><div><span class="label">${escapeHtml(job.source)}</span><h3>${escapeHtml(job.title)}</h3><p>${escapeHtml(job.company)} · ${escapeHtml(job.location)}</p></div><button class="icon-button" aria-label="Bookmark ${escapeHtml(job.title)}">☆</button></article>`).join('')}</div>`;
  if (view === 'tracker') app.innerHTML = `<div class="section-head"><div><p class="eyebrow">APPLICATION TRACKER</p><h2>Keep every opportunity moving.</h2></div><form id="tracker-form"><input name="company" placeholder="Company" required><input name="role" placeholder="Role" required><button class="primary">Add</button></form></div><div class="career-list">${state.applications.map((item) => `<article><span class="label">${escapeHtml(item.status)}</span><h3>${escapeHtml(item.role)}</h3><p>${escapeHtml(item.company)}</p></article>`).join('')}</div>`;
  if (view === 'resume') app.innerHTML = `<div class="ai-panel"><p class="eyebrow">AI & ATS RESUME</p><h2>Check your resume against a role.</h2><form id="ats-form"><textarea name="resume_text" placeholder="Paste your resume text" required></textarea><textarea name="job_description" placeholder="Paste the job description" required></textarea><button class="primary">Run ATS check</button></form><p id="ats-message" class="form-message"></p></div>`;
  if (view === 'ai') app.innerHTML = `<div class="ai-panel"><p class="eyebrow">AI HUB</p><h2>Think with the right model.</h2><p>Prompts stay in your local queue while offline and are sent through the server when an approved provider is configured.</p><form id="ai-form"><select name="provider"><option value="chatgpt">ChatGPT</option><option value="gemini">Gemini</option><option value="superhuman-go">Superhuman GO</option></select><textarea name="prompt" placeholder="What are you working through?" required></textarea><button class="primary">Queue prompt</button></form><p id="ai-message" class="form-message"></p></div>`;
}

async function load() {
  try { [state.skills, state.careers, state.jobs, state.roadmap, state.applications, state.dashboard] = await Promise.all([(api('/skills')).then((x) => x.items), (api('/careers')).then((x) => x.items), (api('/jobs/feed')).then((x) => x.items), (api('/ai/roadmap')).then((x) => x.items), (api('/applications')).then((x) => x.items), api('/dashboard')]); persist(); await flushOutbox(); } catch { cached(); }
  render();
}

document.addEventListener('click', (event) => { const tab = event.target.closest('[data-view]'); const go = event.target.closest('[data-go]'); if (tab) render(tab.dataset.view); if (go) render(go.dataset.go); });
document.addEventListener('change', (event) => { if (event.target.dataset.roadmap) { const node = state.roadmap.find((item) => item.id === event.target.dataset.roadmap); if (node) { node.done = event.target.checked; persist(); } } });
document.addEventListener('submit', async (event) => { event.preventDefault(); const form = event.target; const data = Object.fromEntries(new FormData(form)); try { if (form.id === 'skill-form') { const skill = { name: data.name, level: 'learning' }; state.skills.push(skill); persist(); try { await api('/skills/sync', { method: 'POST', body: JSON.stringify(skill) }); } catch { queue('/skills/sync', { method: 'POST', body: JSON.stringify(skill) }); } await load(); render('skills'); } if (form.id === 'tracker-form') { const item = { id: crypto.randomUUID(), company: data.company, role: data.role, status: 'wishlist' }; state.applications.push(item); persist(); try { await api('/applications/sync', { method: 'POST', body: JSON.stringify(item) }); } catch { queue('/applications/sync', { method: 'POST', body: JSON.stringify(item) }); } render('tracker'); } if (form.id === 'ai-form') { const message = document.querySelector('#ai-message'); try { await api('/ai/hub/process', { method: 'POST', body: JSON.stringify(data) }); message.textContent = 'Prompt queued securely.'; } catch (error) { message.textContent = error.message.includes('Failed to fetch') ? 'Offline. Your prompt is ready to process when you reconnect.' : error.message; } } if (form.id === 'ats-form') { const result = await api('/ai/ats-check', { method: 'POST', body: JSON.stringify(data) }); document.querySelector('#ats-message').textContent = `ATS match: ${result.score}%. ${result.matched_keywords.length} shared keywords found.`; } } catch (error) { const message = form.querySelector('.form-message'); if (message) message.textContent = error.message; } });
window.addEventListener('online', () => { flushOutbox().then(() => render()); });
if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js');
cached();
load();