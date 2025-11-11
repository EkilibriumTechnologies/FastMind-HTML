const CACHE_NAME = 'fastmind-v2'; // Incremented cache version
const NOTIFICATION_TAG = 'fastmind-timer'; // A unique ID for our notification

// Lista de archivos que componen el "app shell" (la app base)
const APP_SHELL_URLS = [
  '/',
  '/index.html',
  'https://cdn.tailwindcss.com',
  'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;700&display=swap'
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
self.addEventListener('fetch', (event) => {
  const url = event.request.url;

  // --- Estrategia de Red (Network Only) ---
  // NO guardar en caché las llamadas a las APIs de Google/Firebase.
  if (
    url.includes('generativelanguage.googleapis.com') || // API de Gemini
    url.includes('firebaseapp.com') ||                  // API de Auth de Firebase
    url.includes('googleapis.com/identitytoolkit') ||   // API de Auth de Google
    url.includes('firestore.googleapis.com')            // API de Firestore
  ) {
    event.respondWith(fetch(event.request));
    return;
  }

  // --- Estrategia de Caché (Cache, falling back to Network) ---
  // Para todo lo demás (el App Shell: HTML, CSS, fuentes).
  event.respondWith(
    caches.match(event.request).then((response) => {
      if (response) {
        return response; // From cache
      }
      // From network, then cache it
      return fetch(event.request).then((networkResponse) => {
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
    })
  );
});

// ---
// --- INICIO: NUEVA LÓGICA DE NOTIFICACIONES ---
// ---

/**
 * Escucha los mensajes ("START_TIMER" o "STOP_TIMER") de la app
 */
self.addEventListener('message', (event) => {
  if (event.data === 'START_TIMER') {
    console.log('[ServiceWorker] Received START_TIMER command');
    showTimerNotification();
  } else if (event.data === 'STOP_TIMER') {
    console.log('[ServiceWorker] Received STOP_TIMER command');
    closeTimerNotification();
  }
});

/**
 * Muestra la notificación persistente
 */
function showTimerNotification() {
  const title = 'FastMind: Fast in Progress';
  const options = {
    body: 'Your fast is currently running. Tap to open the app.',
    icon: 'https://placehold.co/192x192/111827/FFFFFF?text=🧠&font=noto', // Icono
    tag: NOTIFICATION_TAG,    // Un ID para que podamos cerrarla después
    renotify: false,          // No vibrar si ya existe
    silent: true              // No hacer sonido
  };
  
  // self.registration.showNotification es la función mágica
  event.waitUntil(self.registration.showNotification(title, options));
}

/**
 * Cierra la notificación
 */
function closeTimerNotification() {
  // Busca todas las notificaciones con nuestro ID
  self.registration.getNotifications({ tag: NOTIFICATION_TAG }).then(notifications => {
    notifications.forEach(notification => {
      notification.close(); // Cierra cada una
    });
  });
}

/**
 * Se dispara cuando el usuario TOCA la notificación
 */
self.addEventListener('notificationclick', (event) => {
  console.log('[ServiceWorker] Notification click received.');
  
  // Cierra la notificación
  event.notification.close();
  
  // Abre la PWA
  event.waitUntil(
    clients.openWindow('/') // Abre la página principal de la app
  );
});
