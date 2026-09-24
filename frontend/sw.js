const CACHE = 'careeros-shell-v1';
const CORE = ['/', '/index.html', '/styles.css', '/app.js', '/manifest.json', '/icon.svg'];
self.addEventListener('install', (event) => event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(CORE))));
self.addEventListener('fetch', (event) => event.respondWith(fetch(event.request).catch(() => caches.match(event.request).then((response) => response || caches.match('/index.html')))));