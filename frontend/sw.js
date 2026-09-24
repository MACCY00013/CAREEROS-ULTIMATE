const CACHE = 'careeros-shell-v2';
const CORE = ['/', '/index.html', '/styles.css', '/app.js', '/manifest.json', '/icon.svg', '/favicon.svg'];
self.addEventListener('install', (event) => event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(CORE))));
self.addEventListener('fetch', (event) => {
	if (event.request.method !== 'GET') return;
	const requestUrl = new URL(event.request.url);
	if (requestUrl.pathname.startsWith('/api/')) {
		event.respondWith(caches.match(event.request).then((cached) => cached || fetch(event.request).then((response) => {
			if (response.ok) caches.open(CACHE).then((cache) => cache.put(event.request, response.clone()));
			return response;
		})));
		return;
	}
	event.respondWith(fetch(event.request).catch(() => caches.match(event.request).then((response) => response || caches.match('/index.html'))));
});
self.addEventListener('sync', (event) => { if (event.tag === 'careeros-outbox') event.waitUntil(self.clients.matchAll().then((clients) => clients.forEach((client) => client.postMessage({ type: 'SYNC_REQUESTED' })))); });