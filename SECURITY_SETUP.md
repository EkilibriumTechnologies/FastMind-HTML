# 🔐 Configuración de Seguridad - API Key de Gemini

## ⚠️ PROBLEMA DETECTADO

Tu API key estaba expuesta públicamente en el código. Esto es un **riesgo crítico de seguridad**.

## ✅ SOLUCIONES RECOMENDADAS

### Opción 1: Restricciones en Google Cloud Console (RECOMENDADO para apps estáticas)

1. **Ve a Google Cloud Console:**
   - https://console.cloud.google.com/apis/credentials
   - Encuentra tu API key

2. **Configura RESTRICCIONES de HTTP referrer:**
   - Haz clic en "Editar" en tu API key
   - En "Restricciones de aplicación":
     - Selecciona "Referencias HTTP (sitios web)"
     - Agrega tus dominios:
       - `https://tudominio.com/*`
       - `https://*.tudominio.com/*`
       - Para desarrollo local: `http://localhost:*` (solo desarrollo)
   - En "Restricciones de API":
     - Selecciona "Restringir clave"
     - Solo habilita "Generative Language API"

3. **Guarda los cambios**

4. **Regenera la API key** (opcional pero recomendado):
   - Haz clic en "Regenerar clave"
   - Copia la nueva clave

### Opción 2: Usar config.js (para desarrollo)

1. Copia el archivo de ejemplo:
   ```bash
   cp config.example.js config.js
   ```

2. Edita `config.js` y agrega tu API key:
   ```javascript
   window.GEMINI_API_KEY = 'TU_API_KEY_AQUI';
   ```

3. Agrega en `index.html` (antes de otros scripts):
   ```html
   <script src="./config.js"></script>
   ```

4. **IMPORTANTE:** Asegúrate de que `config.js` esté en `.gitignore`:
   ```
   config.js
   ```

### Opción 3: Backend Proxy (MEJOR para producción)

Para una solución completamente segura, crea un backend que:
- Maneje la API key en el servidor
- Haga las requests a Gemini desde el backend
- Exponga solo un endpoint seguro a tu frontend

## 🚨 ACCIONES INMEDIATAS REQUERIDAS

1. ✅ **YA HECHO:** API key removida del código fuente

2. ⚠️ **HAZLO AHORA:**
   - Ve a Google Cloud Console
   - Configura restricciones de HTTP referrer
   - Regenera la API key (recomendado)
   - Configura límites de cuota diaria

3. 📝 **Configuración de la nueva API key:**
   
   **Método temporal (para testing):**
   ```javascript
   // Abre la consola del navegador (F12) y ejecuta:
   localStorage.setItem('GEMINI_API_KEY', 'TU_NUEVA_API_KEY');
   ```
   
   O agrega en `index.html` temporalmente:
   ```html
   <script>
       window.GEMINI_API_KEY = 'TU_NUEVA_API_KEY'; // Solo para testing
   </script>
   ```

## 📊 Configurar Límites de Cuota

1. Ve a: https://console.cloud.google.com/apis/api/generativelanguage.googleapis.com/quotas
2. Configura límites diarios para prevenir costos inesperados
3. Configura alertas de uso

## 🔍 Verificar Seguridad

Después de configurar:
1. Verifica que la API key funcione solo desde tus dominios
2. Intenta usar la API desde otro dominio (debe fallar)
3. Revisa los logs de uso en Google Cloud Console

## ⚠️ NUNCA

- ❌ Subas API keys a Git
- ❌ Compartas API keys públicamente
- ❌ Uses la misma API key en múltiples proyectos sin restricciones
- ❌ Olvides configurar límites de cuota


