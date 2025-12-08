# 🚀 Solución para que funcione en Localhost

## El Problema
OpenAI bloquea llamadas desde `http://127.0.0.1:5500` (localhost) debido a CORS.

## La Solución
Un proxy server local que:
- ✅ Escucha en `http://localhost:3000`
- ✅ Hace las llamadas a OpenAI desde el servidor (sin CORS)
- ✅ Sirve tu app completa

## Pasos para Usar

### Paso 1: Abre una terminal PowerShell
```powershell
cd C:\FastMind\FastMind-HTML
```

### Paso 2: Verifica que todo esté listo
```powershell
.\test-proxy.ps1
```

### Paso 3: Inicia el proxy server
```powershell
node proxy-server.js
```

Deberías ver:
```
==================================================
🚀 Proxy server running at http://localhost:3000
📝 OpenAI API key is configured
✅ CORS bypass enabled for localhost
📁 Serving files from: C:\FastMind\FastMind-HTML
==================================================

👉 Open your app at: http://localhost:3000

💡 Press Ctrl+C to stop the server
```

### Paso 4: Abre tu navegador
Ve a: **http://localhost:3000**

## ⚠️ IMPORTANTE

1. **NO uses** `http://127.0.0.1:5500` - ese es tu servidor Live Server
2. **USA** `http://localhost:3000` - ese es el proxy server
3. El proxy debe estar **corriendo** para que funcione

## Si algo no funciona

### Error: "Port 3000 is already in use"
- Cierra cualquier otra aplicación que use el puerto 3000
- O cambia el puerto en `proxy-server.js` (línea 11)

### Error: "Cannot find module"
- Verifica que Node.js esté instalado: `node --version`
- Si no está instalado: https://nodejs.org/

### La página carga pero el AI no funciona
1. Abre la consola del navegador (F12)
2. Verifica que veas: `Trying OpenAI: gpt-4o-mini`
3. Si ves errores de CORS, asegúrate de estar en `localhost:3000` (no 127.0.0.1:5500)

## Notas

- ✅ En **apps nativas** (Android/iOS) NO necesitas el proxy - funciona directo
- ✅ El código detecta automáticamente si estás en localhost
- ✅ En producción (HTTPS) también funciona directo sin proxy






