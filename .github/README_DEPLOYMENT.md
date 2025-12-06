# 🚀 Guía de Deployment

## ⚠️ IMPORTANTE: Configuración de API Keys

**NUNCA subas las API keys al repositorio.** Este repositorio está configurado para ignorar archivos sensibles.

## 📋 Configuración Inicial

### 1. Configurar API Keys

Después de clonar el repositorio:

```bash
# Copia el archivo de ejemplo
cp config.example.js config.js

# Edita config.js y agrega tus API keys
# window.OPENAI_API_KEY = 'tu-key-aqui';
# window.GEMINI_API_KEY = 'tu-key-aqui';
```

### 2. Verificar que config.js esté ignorado

```bash
git check-ignore config.js
# Debe retornar: config.js
```

### 3. Verificar antes de commit

```bash
# Verifica qué archivos se van a subir
git status

# Asegúrate de que config.js NO aparezca en la lista
```

## 🔒 Archivos Protegidos

Los siguientes archivos están en `.gitignore` y NO se subirán a Git:

- `config.js` - API keys
- `*.keystore`, `*.jks` - Keystores de Android
- `.env*` - Variables de entorno
- `node_modules/` - Dependencias

## 📝 Antes de hacer Push

1. ✅ Verifica que `config.js` esté en `.gitignore`
2. ✅ Verifica que no hay keys hardcodeadas en el código
3. ✅ Ejecuta `git status` para revisar qué se va a subir
4. ✅ Nunca hagas commit de `config.js`

## 🆘 Si accidentalmente subiste una key

1. Regenera la key inmediatamente en el servicio correspondiente
2. Usa `git filter-branch` o `git-filter-repo` para remover del historial
3. Considera usar GitHub Secret Scanning para detectar keys expuestas






