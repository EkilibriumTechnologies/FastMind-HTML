const CACHE_NAME = 'fastmind-v13';
const ASSETS = [
  './manifest.json'
  // NOTE: NOT caching index.html - always fetch fresh from network
];

// Install event - Force activate immediately to clear old cache
self.addEventListener('install', event => {
  self.skipWaiting(); // Activate immediately
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
  const requestUrl = event.request.url;
  const requestMethod = event.request.method;
  
  // CRITICAL: Skip ALL POST requests (APIs use POST) - Don't intercept at all
  if (requestMethod !== 'GET') {
    return; // Don't intercept POST/PUT/DELETE/PATCH requests
  }
  
  // CRITICAL: Skip ALL API requests - Let them pass through completely
  if (requestUrl.includes('api.openai.com') || 
      requestUrl.includes('generativelanguage.googleapis.com') ||
      requestUrl.includes('openai.com')) {
    return; // Don't intercept OpenAI requests at all
  }
  
  // Skip Cloudflare Worker requests - let them pass through
  if (requestUrl.includes('workers.dev') || requestUrl.includes('fastmind.consulting')) {
    return; // Don't intercept Worker requests
  }
  
  // IMPORTANT: Let Firebase requests pass through WITHOUT interception
  if (requestUrl.includes('firebase') || 
      requestUrl.includes('googleapis.com') ||
      requestUrl.includes('gstatic.com') ||
      requestUrl.includes('firebaseapp.com') ||
      requestUrl.includes('firestore.googleapis.com') ||
      requestUrl.includes('identitytoolkit.googleapis.com') ||
      requestUrl.includes('securetoken.googleapis.com')) {
    return; // Don't intercept Firebase requests at all
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
  // IMPORTANT: Don't cache POST requests, chrome-extension, or non-GET methods
  if (event.request.method !== 'GET' || 
      event.request.url.startsWith('chrome-extension://') ||
      event.request.url.startsWith('chrome://')) {
    // Let these requests pass through without Service Worker interference
    event.respondWith(fetch(event.request));
    return;
  }
  
  event.respondWith(
    caches.match(event.request).then(cachedResponse => {
        const fetchPromise = fetch(event.request)
          .then(networkResponse => {
              // Only cache GET requests with successful responses
              if (networkResponse.ok && 
                  networkResponse.status === 200 &&
                  !networkResponse.url.includes('index.html') && 
                  !networkResponse.url.includes('firebase') &&
                  !networkResponse.url.includes('googleapis.com') &&
                  !networkResponse.url.includes('gstatic.com') &&
                  !networkResponse.url.startsWith('chrome-extension://') &&
                  !networkResponse.url.startsWith('chrome://')) {
                // Clone the response BEFORE using it to cache
                const responseClone = networkResponse.clone();
                // Use the clone for caching, original for returning
                caches.open(CACHE_NAME).then(cache => {
                  cache.put(event.request, responseClone).catch(err => {
                    // Silently ignore cache errors for unsupported request types
                    if (!err.message.includes('chrome-extension') && 
                        !err.message.includes('POST')) {
                      console.error('SW: Cache put error:', err);
                    }
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
