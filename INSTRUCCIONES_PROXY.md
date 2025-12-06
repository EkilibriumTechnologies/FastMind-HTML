# 🚀 Cómo Usar el Proxy Server para Localhost

## Problema
OpenAI API bloquea llamadas desde localhost debido a CORS. Este proxy server soluciona ese problema.

## Solución Rápida

### 1. Abre una terminal en la carpeta del proyecto
```powershell
cd C:\FastMind\FastMind-HTML
```

### 2. Ejecuta el servidor proxy
```powershell
node proxy-server.js
```

Deberías ver:
```
🚀 Proxy server running at http://localhost:3000
📝 OpenAI API key is configured
✅ CORS bypass enabled for localhost

👉 Open your app at: http://localhost:3000
```

### 3. Abre tu app en el navegador
Ve a: **http://localhost:3000**

¡El AI debería funcionar ahora! 🎉

## ¿Cómo Funciona?

- El proxy server escucha en el puerto **3000**
- Cuando tu app (en localhost) llama a OpenAI, el proxy intercepta la llamada
- El proxy hace la llamada a OpenAI desde el servidor (sin CORS)
- El proxy devuelve la respuesta a tu app

## Notas Importantes

✅ **En producción (apps nativas)**: No necesitas el proxy - funcionará directamente  
✅ **El código detecta automáticamente** si estás en localhost y usa el proxy  
✅ **Si estás en producción**, usa la API directamente

## Si el proxy no funciona

1. Verifica que Node.js esté instalado: `node --version`
2. Verifica que el puerto 3000 esté libre
3. Asegúrate de abrir la app en `http://localhost:3000` (no en otro puerto)





