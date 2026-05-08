// Territories service worker
const CACHE_NAME = 'territories-v3';
const ASSETS = [
  './',
  './index.html',
  './styles.css',
  './territories.js',
  './manifest.json',
  './icon-192.png',
  './icon-512.png'
];

self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => cache.addAll(ASSETS))
      .then(() => self.skipWaiting())
  );
});

self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys()
      .then(keys => Promise.all(
        keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))
      ))
      .then(() => self.clients.claim())
  );
});

// Stale-while-revalidate for app shell, cache-first for icons
self.addEventListener('fetch', event => {
  if (event.request.method !== 'GET') return;
  const url = new URL(event.request.url);
  if (url.origin !== location.origin) return;

  event.respondWith(
    caches.match(event.request).then(cached => {
      const networkFetch = fetch(event.request).then(resp => {
        if (resp && resp.status === 200) {
          const respClone = resp.clone();
          caches.open(CACHE_NAME).then(c => c.put(event.request, respClone));
        }
        return resp;
      }).catch(() => cached);
      return cached || networkFetch;
    })
  );
});
