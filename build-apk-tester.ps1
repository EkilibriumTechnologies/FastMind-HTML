# Script para construir APK/AAB con bypass de tester
# Versión 3.1 - Incluye bypass para demo@fastmind.app

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build APK/AAB FastMind Version 3.1" -ForegroundColor Cyan
Write-Host "Incluye bypass para tester: demo@fastmind.app" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Sincronizar archivos web con Capacitor
Write-Host "Sincronizando archivos web..." -ForegroundColor Yellow
Copy-Item -Path "index.html" -Destination "www\index.html" -Force
if (Test-Path "sw.js") {
    Copy-Item -Path "sw.js" -Destination "www\sw.js" -Force
}
Write-Host "✓ Archivos sincronizados" -ForegroundColor Green
Write-Host ""

# Sincronizar con Capacitor Android
Write-Host "Sincronizando con Capacitor Android..." -ForegroundColor Yellow
npx cap sync android
if ($LASTEXITCODE -ne 0) {
    Write-Host "⚠ Advertencia: npx cap sync puede no estar disponible. Continuando..." -ForegroundColor Yellow
}
Write-Host ""

# Contraseñas guardadas (no se pedirán más)
$keystorePassword = "FastMind2025!"
$keyPassword = "FastMind2025!"

# El alias por defecto es 'fastmind-key' según build.gradle
$keyAlias = "fastmind-key"

Write-Host ""
Write-Host "Configurando variables de entorno..." -ForegroundColor Yellow

# Configurar variables de entorno
$env:KEYSTORE_PASSWORD = $keystorePassword
$env:KEY_ALIAS = $keyAlias
$env:KEY_PASSWORD = $keyPassword

Write-Host "✓ Variables configuradas" -ForegroundColor Green
Write-Host ""

# Preguntar qué construir
Write-Host "¿Qué quieres construir?" -ForegroundColor Cyan
Write-Host "1. APK (para instalación directa - recomendado para tester)"
Write-Host "2. AAB (para Google Play Store)"
Write-Host "3. Ambos"
Write-Host ""
$choice = Read-Host "Selecciona (1/2/3)"

Write-Host ""
Write-Host "Iniciando construcción..." -ForegroundColor Yellow
Write-Host ""

# Cambiar al directorio android
Set-Location android

if ($choice -eq "1" -or $choice -eq "3") {
    Write-Host "Construyendo APK..." -ForegroundColor Cyan
    .\gradlew.bat assembleRelease
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ APK construido exitosamente!" -ForegroundColor Green
        $apkPath = "app\build\outputs\apk\release\app-release.apk"
        if (Test-Path $apkPath) {
            $apkSize = (Get-Item $apkPath).Length / 1MB
            Write-Host "Ubicación: $apkPath" -ForegroundColor Yellow
            Write-Host "Tamaño: $([math]::Round($apkSize, 2)) MB" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "📱 Este APK incluye bypass para: demo@fastmind.app" -ForegroundColor Cyan
        }
    } else {
        Write-Host ""
        Write-Host "✗ Error al construir APK" -ForegroundColor Red
    }
    Write-Host ""
}

if ($choice -eq "2" -or $choice -eq "3") {
    Write-Host "Construyendo AAB..." -ForegroundColor Cyan
    .\gradlew.bat bundleRelease
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ AAB construido exitosamente!" -ForegroundColor Green
        $aabPath = "app\build\outputs\bundle\release\app-release.aab"
        if (Test-Path $aabPath) {
            $aabSize = (Get-Item $aabPath).Length / 1MB
            Write-Host "Ubicación: $aabPath" -ForegroundColor Yellow
            Write-Host "Tamaño: $([math]::Round($aabSize, 2)) MB" -ForegroundColor Yellow
            Write-Host ""
            Write-Host "📱 Este AAB incluye bypass para: demo@fastmind.app" -ForegroundColor Cyan
        }
    } else {
        Write-Host ""
        Write-Host "✗ Error al construir AAB" -ForegroundColor Red
    }
    Write-Host ""
}

# Volver al directorio raíz
Set-Location ..

# Limpiar variables de entorno (seguridad)
Remove-Item Env:KEYSTORE_PASSWORD
Remove-Item Env:KEY_ALIAS
Remove-Item Env:KEY_PASSWORD

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Proceso completado" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "📝 Nota: El tester debe usar el email: demo@fastmind.app" -ForegroundColor Yellow
Write-Host "   para tener acceso ilimitado a todas las funciones premium." -ForegroundColor Yellow
Write-Host ""

