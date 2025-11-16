const CACHE_NAME = 'fastmind-v4'; // Versión incrementada
const NOTIFICATION_TAG = 'fastmind-timer';
const GOAL_NOTIFICATION_TAG = 'fastmind-goal'; // *** NEW: ID para notificación de meta ***

// Lista de archivos actualizada para incluir todos los assets
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
  if (
    url.includes('generativelanguage.googleapis.com') || 
    url.includes('firebaseapp.com') ||                   
    url.includes('googleapis.com/identitytoolkit') ||   
    url.includes('firestore.googleapis.com')             
  ) {
    event.respondWith(fetch(event.request));
    return;
  }

  // Estrategia de Caché (Cache, falling back to Network)
  event.respondWith(
    caches.match(event.request).then((response) => {
      if (response) {
        return response; // Desde el caché
      }
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
 * Escucha los mensajes de la app
 * *** ¡NUEVO: Añadido GOAL_REACHED! ***
 */
self.addEventListener('message', (event) => {
  if (event.data === 'START_TIMER') {
    console.log('[ServiceWorker] Received START_TIMER command');
    showTimerNotification();
  } else if (event.data === 'STOP_TIMER') {
    console.log('[ServiceWorker] Received STOP_TIMER command');
    closeTimerNotification();
    closeGoalNotification(); // También cierra la de meta si está abierta
  } else if (event.data === 'GOAL_REACHED') { // *** NUEVO ***
    console.log('[ServiceWorker] Received GOAL_REACHED command');
    showGoalNotification();
  }
});

/**
 * Muestra la notificación persistente de AYUNO
 */
function showTimerNotification() {
  const title = 'FastMind: Fast in Progress';
  const options = {
    body: 'Your fast is currently running. Tap to open the app.',
    icon: 'https://placehold.co/192x192/F8FAFC/0F172A?text=🧠&font=noto', 
    tag: NOTIFICATION_TAG,    
    renotify: false,          
    silent: true              
  };
  
  self.waitUntil(self.registration.showNotification(title, options));
}

/**
 * Cierra la notificación de AYUNO
 */
function closeTimerNotification() {
  self.registration.getNotifications({ tag: NOTIFICATION_TAG }).then(notifications => {
    notifications.forEach(notification => {
      notification.close(); 
    });
  });
}

/**
 * *** NUEVO: Muestra la notificación de META CUMPLIDA ***
 */
function showGoalNotification() {
  const title = '🎉 Goal Reached!';
  const options = {
    body: "You've completed your fasting goal. Great job!",
    icon: 'https://placehold.co/192x192/F8FAFC/0F172A?text=🎉&font=noto', 
    tag: GOAL_NOTIFICATION_TAG,    
    renotify: true, // Queremos que esta sí notifique
    silent: false
  };
  
  self.waitUntil(self.registration.showNotification(title, options));
}

/**
 * *** NUEVO: Cierra la notificación de META CUMPLIDA ***
 */
function closeGoalNotification() {
    self.registration.getNotifications({ tag: GOAL_NOTIFICATION_TAG }).then(notifications => {
        notifications.forEach(notification => {
            notification.close();
        });
    });
}


/**
 * Se dispara cuando el usuario TOCA la notificación
 */
self.addEventListener('notificationclick', (event) => {
  console.log('[ServiceWorker] Notification click received.');
  
  // Cierra la notificación que fue tocada
  event.notification.close();
  
  // Abre la PWA
  event.waitUntil(
    clients.openWindow('./') // Abre la página principal de la app
  );
});
