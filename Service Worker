const CACHE_NAME = 'fastmind-v1';

// Lista de archivos que componen el "app shell" (la app base)
// Estos son los archivos que se guardarán en caché para que la app cargue sin conexión
const APP_SHELL_URLS = [
  '/',
  '/index.html',
  'https://cdn.tailwindcss.com',
  'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;700&display=swap'
  // Los SDK de Firebase se cargarán desde la red, ya que necesitan conexión
];

// Evento 'install': Se dispara cuando el SW se instala por primera vez.
self.addEventListener('install', (event) => {
  console.log('[ServiceWorker] Install');
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('[ServiceWorker] Caching app shell');
      return cache.addAll(APP_SHELL_URLS);
    })
  );
});

// Evento 'activate': Se dispara cuando el SW se activa (después de 'install').
// Se usa para limpiar cachés viejos.
self.addEventListener('activate', (event) => {
  console.log('[ServiceWorker] Activate');
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('[ServiceWorker] Removing old cache', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
  return self.clients.claim();
});

// Evento 'fetch': Se dispara cada vez que la app hace una petición de red
// (ej. cargar una imagen, un script, o una llamada a la API).
self.addEventListener('fetch', (event) => {
  const url = event.request.url;

  // --- Estrategia de Red (Network Only) ---
  // NO guardar en caché las llamadas a las APIs de Google/Firebase.
  // Estas siempre necesitan una conexión a internet.
  if (
    url.includes('generativelanguage.googleapis.com') || // API de Gemini
    url.includes('firebaseapp.com') ||                  // API de Auth de Firebase
    url.includes('googleapis.com/identitytoolkit') ||   // API de Auth de Google
    url.includes('firestore.googleapis.com')            // API de Firestore
  ) {
    // Dejar que la petición vaya directamente a la red
    event.respondWith(fetch(event.request));
    return;
  }

  // --- Estrategia de Caché (Cache, falling back to Network) ---
  // Para todo lo demás (el App Shell: HTML, CSS, fuentes).
  event.respondWith(
    caches.match(event.request).then((response) => {
      // 1. Si está en el caché, devolverlo desde el caché
      if (response) {
        // console.log('[ServiceWorker] Returning from cache:', event.request.url);
        return response;
      }

      // 2. Si no está en el caché, ir a la red
      // console.log('[ServiceWorker] Fetching from network:', event.request.url);
      return fetch(event.request).then((networkResponse) => {
        // 3. Guardar la respuesta de la red en el caché para la próxima vez
        if (networkResponse && networkResponse.status === 200) {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then((cache) => {
            cache.put(event.request, responseToCache);
          });
        }
        return networkResponse;
      });
    }).catch((error) => {
      console.error('[ServiceWorker] Fetch error:', error);
      // Opcional: Podrías devolver una página "offline" personalizada aquí
    })
  );
});
