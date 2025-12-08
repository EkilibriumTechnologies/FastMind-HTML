# Guía de Actualización de Versión

Este documento explica cómo actualizar la versión de la app en todas las plataformas de forma sincronizada.

## 🚀 Método Rápido (Recomendado)

Usa el script `update-version.ps1` que actualiza automáticamente todas las plataformas:

### Opción 1: Especificar versión manualmente
```powershell
.\update-version.ps1 -NewVersion "1.0.2"
```

### Opción 2: Incrementar automáticamente
```powershell
# Incrementar PATCH (1.0.1 → 1.0.2)
.\update-version.ps1 -IncrementPatch

# Incrementar MINOR (1.0.1 → 1.1.0)
.\update-version.ps1 -IncrementMinor

# Incrementar MAJOR (1.0.1 → 2.0.0)
.\update-version.ps1 -IncrementMajor
```

### Opción 3: Modo interactivo
```powershell
.\update-version.ps1
# Te pedirá la nueva versión
```

## 📋 Qué Actualiza el Script

El script actualiza automáticamente:

1. **Capacitor** (`capacitor.config.json`)
   - Campo: `version`

2. **Android** (`android/app/build.gradle`)
   - `versionName`: Nueva versión especificada
   - `versionCode`: Se incrementa automáticamente (+1)

3. **iOS** (`ios/App/App.xcodeproj/project.pbxproj`)
   - `MARKETING_VERSION`: Nueva versión especificada (Debug y Release)
   - `CURRENT_PROJECT_VERSION`: Se incrementa automáticamente (+1) (Debug y Release)

## ⚙️ Después de Actualizar

Después de ejecutar el script, ejecuta:

```powershell
npx cap sync
```

Esto sincronizará los archivos `capacitor.config.json` en las carpetas de plataforma.

## 📝 Formato de Versión

La versión debe seguir el formato **Semantic Versioning**:
- Formato: `X.Y.Z` (ej: `1.0.1`, `1.2.3`, `2.0.0`)
- **X** = Major (cambios incompatibles)
- **Y** = Minor (nuevas funcionalidades compatibles)
- **Z** = Patch (correcciones de bugs)

## 🔍 Verificar Versiones Actuales

Para verificar las versiones actuales:

```powershell
# Capacitor
Get-Content capacitor.config.json | ConvertFrom-Json | Select-Object version

# Android
Select-String -Path "android\app\build.gradle" -Pattern "versionName|versionCode"

# iOS
Select-String -Path "ios\App\App.xcodeproj\project.pbxproj" -Pattern "MARKETING_VERSION|CURRENT_PROJECT_VERSION"
```

## ⚠️ Importante

- **Siempre** usa el script para actualizar versiones, así todas las plataformas quedan sincronizadas
- El `versionCode` (Android) y `CURRENT_PROJECT_VERSION` (iOS) se incrementan automáticamente
- No edites manualmente los archivos de versión, usa el script

## 📚 Ejemplos de Uso

### Ejemplo 1: Nueva versión patch
```powershell
.\update-version.ps1 -IncrementPatch
# 1.0.1 → 1.0.2
```

### Ejemplo 2: Nueva versión minor
```powershell
.\update-version.ps1 -IncrementMinor
# 1.0.1 → 1.1.0
```

### Ejemplo 3: Versión específica
```powershell
.\update-version.ps1 -NewVersion "1.2.5"
```

