const CACHE_NAME = 'fastmind-v3'; // Versión incrementada
const NOTIFICATION_TAG = 'fastmind-timer'; // ID único para la notificación

// Lista de archivos actualizada
const APP_SHELL_URLS = [
  './',
  './index.html',
  './manifest.json',
  'https://cdn.tailwindcss.com',
  'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap',
  'https://cdnjs.cloudflare.com/ajax/libs/tone/14.7.77/Tone.js',
  'https://www.gstatic.com/firebasejs/11.6.1/firebase-app.js',
  'https://www.gstatic.com/firebasejs/11.6.1/firebase-auth.js',
  'https://www.gstatic.com/firebasejs/11.6.1/firebase-firestore.js'
];

// Evento 'install': Se dispara cuando el SW se instala
self.addEventListener('install', (event) => {
  console.log('[ServiceWorker] Install');
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('[ServiceWorker] Caching app shell');
      return cache.addAll(APP_SHELL_URLS);
    })
  );
});

// Evento 'activate': Se dispara cuando el SW se activa.
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

// Evento 'fetch': Se dispara cada vez que la app hace una petición
self.addEventListener('fetch', (event) => {
  const url = event.request.url;

  // Estrategia de Red (Network Only)
  // NO guardar en caché las llamadas a las APIs de Google/Firebase.
  if (
    url.includes('generativelanguage.googleapis.com') || // API de Gemini
    url.includes('firebaseapp.com') ||                   // API de Auth de Firebase
    url.includes('googleapis.com/identitytoolkit') ||   // API de Auth de Google
    url.includes('firestore.googleapis.com')             // API de Firestore
  ) {
    event.respondWith(fetch(event.request));
    return;
  }

  // Estrategia de Caché (Cache, falling back to Network)
  // Para todo lo demás (el App Shell: HTML, CSS, fuentes).
  event.respondWith(
    caches.match(event.request).then((response) => {
      if (response) {
        return response; // Desde el caché
      }
      // Desde la red, y luego lo guardamos en caché
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
// --- LÓGICA DE NOTIFICACIONES ---
// ---

/**
 * Escucha los mensajes ("START_TIMER" o "STOP_TIMER") de la app
 */
self.addEventListener('message', (event) => {
  if (event.data === 'START_TRAMER') {
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
    icon: 'https://placehold.co/192x192/F8FAFC/0F172A?text=🧠&font=noto', // Icono actualizado
    tag: NOTIFICATION_TAG,    // ID para que podamos cerrarla
    renotify: false,          // No vibrar si ya existe
    silent: true              // No hacer sonido
  };
  
  // self.registration.showNotification es la función
  // CORRECCIÓN: 'event.waitUntil' no está definido aquí, usamos 'self.waitUntil'
  self.waitUntil(self.registration.showNotification(title, options));
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
    clients.openWindow('./') // Abre la página principal de la app
  );
});
