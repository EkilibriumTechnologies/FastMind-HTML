# Firebase Functions Setup - Secure API Key Management

Este documento explica cómo configurar Firebase Functions para manejar las API keys de forma segura.

## 📋 Prerequisitos

1. Firebase CLI instalado:
   ```bash
   npm install -g firebase-tools
   ```

2. Login en Firebase:
   ```bash
   firebase login
   ```

3. Seleccionar el proyecto:
   ```bash
   firebase use fastmind-c6603
   ```

## 🔧 Instalación

1. **Instalar dependencias de Functions:**
   ```bash
   cd functions
   npm install
   cd ..
   ```

2. **Configurar las API Keys como variables de entorno:**

   **Opción A: Usando .env (Recomendado - método moderno)**
   Crear `functions/.env`:
   ```
   OPENAI_API_KEY=tu-openai-api-key-aqui
   GEMINI_API_KEY=tu-gemini-api-key-aqui
   ```
   Firebase Functions cargará automáticamente las variables desde `.env`

   **Opción B: Usando Firebase Config (Deprecado - funcionará hasta marzo 2026)**
   ```bash
   firebase functions:config:set openai.key="tu-openai-api-key-aqui"
   firebase functions:config:set gemini.key="tu-gemini-api-key-aqui"
   ```
   
   **Opción C: Variables de entorno en Firebase Console (Producción)**
   1. Ve a Firebase Console > Functions > Configuración
   2. Agrega variables de entorno:
      - `OPENAI_API_KEY`
      - `GEMINI_API_KEY`

## 🚀 Despliegue

1. **Desplegar las funciones:**
   ```bash
   firebase deploy --only functions
   ```

2. **Verificar que se desplegó correctamente:**
   ```bash
   firebase functions:log
   ```

3. **Obtener la URL de tu función:**
   Después del deploy, verás algo como:
   ```
   ✔  functions[getApiKeys(us-central1)]: Successful create operation.
   Function URL: https://us-central1-fastmind-c6603.cloudfunctions.net/getApiKeys
   ```

## 🔐 Seguridad

### Restricciones Recomendadas

1. **Restricción por dominio/origen:**
   - Edita `functions/index.js`
   - Descomenta la sección de `allowedOrigins`
   - Agrega tus dominios permitidos

2. **Restricción por autenticación:**
   - Edita `functions/index.js`
   - Descomenta la verificación de `authorization`
   - Solo usuarios autenticados podrán obtener las keys

3. **Restricciones en Firebase Console:**
   - Ve a Firebase Console > Functions
   - Configura reglas de acceso (IAM)

## 📝 Uso en el Código

El código ahora intentará obtener las keys desde la Cloud Function:

```javascript
// Prioridad:
// 1. Firebase Function (producción)
// 2. config.js (desarrollo local)
// 3. localStorage (fallback)
```

## 🧪 Pruebas Locales

1. **Iniciar emulador:**
   ```bash
   firebase emulators:start --only functions
   ```

2. **URL local:**
   ```
   http://localhost:5001/fastmind-c6603/us-central1/getApiKeys
   ```

## 🔄 Actualizar Keys

Para actualizar las keys sin redesplegar:

```bash
firebase functions:config:set openai.key="nueva-key"
firebase functions:config:set gemini.key="nueva-key"
firebase deploy --only functions
```

## 📊 Monitoreo

- **Ver logs:**
  ```bash
  firebase functions:log
  ```

- **Dashboard:**
  https://console.firebase.google.com/project/fastmind-c6603/functions

## ⚠️ Importante

- **NUNCA** subas las keys a Git
- Las keys se almacenan de forma segura en Firebase
- La función devuelve las keys solo a solicitudes autorizadas
- Considera agregar rate limiting para prevenir abuso

