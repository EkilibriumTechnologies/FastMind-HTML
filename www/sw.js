const CACHE_NAME = 'fastmind-v8';
const ASSETS = [
  './manifest.json'
  // NOTE: NOT caching index.html - always fetch fresh from network
];

// Install event
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME)
      .then(cache => {
        console.log('SW: Caching assets (excluding index.html)');
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

// Fetch event - Network Only for HTML (NO CACHE) to prevent stale content
self.addEventListener('fetch', event => {
  // IMPORTANT: Let Firebase requests pass through WITHOUT interception
  if (event.request.url.includes('firebase') || 
      event.request.url.includes('googleapis.com') ||
      event.request.url.includes('gstatic.com') ||
      event.request.url.includes('firebaseapp.com') ||
      event.request.url.includes('firestore.googleapis.com') ||
      event.request.url.includes('identitytoolkit.googleapis.com') ||
      event.request.url.includes('securetoken.googleapis.com')) {
    // Let Firebase requests go directly to network, no Service Worker interference
    return;
  }
  
  // For navigation (HTML), ALWAYS fetch from network, NEVER use cache
  if (event.request.mode === 'navigate' || event.request.destination === 'document') {
    event.respondWith(
      fetch(event.request, { cache: 'no-store' })
        .catch(() => {
          console.error('SW: Network fetch failed, returning offline fallback');
          return new Response('Offline', { status: 503, headers: { 'Content-Type': 'text/html' } });
        })
    );
    return;
  }
  
  // For index.html specifically, NEVER cache
  if (event.request.url.includes('index.html')) {
    event.respondWith(
      fetch(event.request, { cache: 'no-store' })
        .catch(() => {
          console.error('SW: Network fetch failed for index.html');
          return new Response('Offline', { status: 503, headers: { 'Content-Type': 'text/html' } });
        })
    );
    return;
  }
  
  // Stale-while-revalidate for other assets (CSS, JS, images, etc.)
  event.respondWith(
    caches.match(event.request).then(cachedResponse => {
        const fetchPromise = fetch(event.request)
          .then(networkResponse => {
              // Clone the response BEFORE using it to cache
              const responseClone = networkResponse.clone();
              
              // Don't cache HTML files or Firebase requests
              if (!networkResponse.url.includes('index.html') && 
                  !networkResponse.url.includes('firebase') &&
                  !networkResponse.url.includes('googleapis.com') &&
                  !networkResponse.url.includes('gstatic.com')) {
                // Use the clone for caching, original for returning
                caches.open(CACHE_NAME).then(cache => {
                  cache.put(event.request, responseClone).catch(err => {
                    console.error('SW: Cache put error:', err);
                  });
                });
              }
              return networkResponse;
          })
          .catch(error => {
              console.error('SW: Fetch error:', error);
              // Return cached response if available, otherwise return error
              return cachedResponse || new Response('Network error', { status: 503 });
          });
        
        // Return cached response immediately if available, otherwise wait for network
        return cachedResponse || fetchPromise;
    })
  );
});
