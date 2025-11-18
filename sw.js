const CACHE_NAME = 'fastmind-v2';
const ASSETS = [
  './',
  './index.html',
  './manifest.json'
];

// Install event
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('SW: Caching assets');
        return cache.addAll(ASSETS);
      })
  );
  self.skipWaiting(); // Force activation
});

// Activate event - Clean old caches
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys => {
      return Promise.all(keys.map(key => {
        if (key !== CACHE_NAME) return caches.delete(key);
      }));
    })
  );
  self.clients.claim();
});

// Fetch event - Network First strategy for HTML (critical for development)
self.addEventListener('fetch', event => {
  if (event.request.mode === 'navigate') {
    event.respondWith(
      fetch(event.request)
        .catch(() => caches.match(event.request))
    );
    return;
  }
  
  // Stale-while-revalidate for other assets
  event.respondWith(
    caches.match(event.request).then(cachedResponse => {
        const fetchPromise = fetch(event.request).then(networkResponse => {
            caches.open(CACHE_NAME).then(cache => cache.put(event.request, networkResponse.clone()));
            return networkResponse;
        });
        return cachedResponse || fetchPromise;
    })
  );
});
