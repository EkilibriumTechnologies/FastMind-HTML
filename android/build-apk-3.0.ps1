# Script para construir APK/AAB versión 3.0
# Este script te pedirá las contraseñas de forma segura

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Build APK/AAB FastMind Version 3.0" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Solicitar contraseñas de forma segura
$secureKeystorePassword = Read-Host "Ingresa la contraseña del KEYSTORE" -AsSecureString
$secureKeyPassword = Read-Host "Ingresa la contraseña de la KEY" -AsSecureString

# Convertir a texto plano (solo para esta sesión)
$BSTR1 = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKeystorePassword)
$keystorePassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR1)

$BSTR2 = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKeyPassword)
$keyPassword = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR2)

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
Write-Host "1. APK (para instalación directa)"
Write-Host "2. AAB (para Google Play Store)"
Write-Host "3. Ambos"
Write-Host ""
$choice = Read-Host "Selecciona (1/2/3)"

Write-Host ""
Write-Host "Iniciando construcción..." -ForegroundColor Yellow
Write-Host ""

if ($choice -eq "1" -or $choice -eq "3") {
    Write-Host "Construyendo APK..." -ForegroundColor Cyan
    ./gradlew assembleRelease
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ APK construido exitosamente!" -ForegroundColor Green
        Write-Host "Ubicación: app\build\outputs\apk\release\app-release.apk" -ForegroundColor Yellow
    } else {
        Write-Host ""
        Write-Host "✗ Error al construir APK" -ForegroundColor Red
    }
    Write-Host ""
}

if ($choice -eq "2" -or $choice -eq "3") {
    Write-Host "Construyendo AAB..." -ForegroundColor Cyan
    ./gradlew bundleRelease
    if ($LASTEXITCODE -eq 0) {
        Write-Host ""
        Write-Host "✓ AAB construido exitosamente!" -ForegroundColor Green
        Write-Host "Ubicación: app\build\outputs\bundle\release\app-release.aab" -ForegroundColor Yellow
    } else {
        Write-Host ""
        Write-Host "✗ Error al construir AAB" -ForegroundColor Red
    }
    Write-Host ""
}

# Limpiar variables de entorno (seguridad)
Remove-Item Env:KEYSTORE_PASSWORD
Remove-Item Env:KEY_ALIAS
Remove-Item Env:KEY_PASSWORD

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Proceso completado" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

