# Build APK/AAB Version 3.0 - Instrucciones

## Cambios en la Versión 3.0
- ✅ Integración con Cloudflare Worker para peticiones AI seguras
- ✅ Sistema RAG mejorado con base de conocimiento
- ✅ API keys removidas del frontend (seguridad)
- ✅ Mejor manejo de errores y logging
- ✅ Version Code: 8
- ✅ Version Name: 3.0

## Requisitos Previos

1. **Credenciales del Keystore**: Necesitas las contraseñas del keystore para firmar el APK/AAB
2. **Java JDK**: Asegúrate de tener Java 11+ instalado
3. **Android SDK**: Configurado correctamente

## Pasos para Construir el APK/AAB

### 1. Configurar Variables de Entorno (Windows PowerShell)

```powershell
cd FastMind-HTML\android
$env:KEYSTORE_PASSWORD="tu_keystore_password"
$env:KEY_ALIAS="fastmind-key"
$env:KEY_PASSWORD="tu_key_password"
```

### 2. Construir el APK (para instalación directa)

```powershell
./gradlew assembleRelease
```

El APK se generará en:
`android/app/build/outputs/apk/release/app-release.apk`

### 3. Construir el AAB (para Google Play Store)

```powershell
./gradlew bundleRelease
```

El AAB se generará en:
`android/app/build/outputs/bundle/release/app-release.aab`

## Verificación

Después de construir, verifica que:
- ✅ El archivo se generó correctamente
- ✅ La versión es 3.0 (versionCode 8)
- ✅ El tamaño del archivo es razonable
- ✅ Puedes instalar el APK en un dispositivo de prueba

## Notas Importantes

- **NUNCA** compartas las contraseñas del keystore
- El keystore `fastmind-release-key.keystore` debe estar en `android/app/`
- Si olvidaste las contraseñas, no podrás actualizar la app en Google Play Store
- Para producción, siempre usa `bundleRelease` (AAB) para Google Play Store

## Troubleshooting

### Error: "SigningConfig release is missing required property storePassword"
- **Solución**: Configura las variables de entorno antes de construir

### Error: "Keystore file not found"
- **Solución**: Verifica que `fastmind-release-key.keystore` esté en `android/app/`

### Error: "Invalid keystore format"
- **Solución**: Verifica que el keystore no esté corrupto

## Ubicación de los Archivos Generados

- **APK**: `android/app/build/outputs/apk/release/app-release.apk`
- **AAB**: `android/app/build/outputs/bundle/release/app-release.aab`
- **APK Firmado Anterior**: `android/app/release/app-release.apk` (si existe)

