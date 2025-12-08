# Script para actualizar la versión de la app en todas las plataformas
# Uso: .\update-version.ps1 -NewVersion "1.0.2"
# O simplemente: .\update-version.ps1 (te pedirá la versión)

param(
    [Parameter(Mandatory=$false)]
    [string]$NewVersion = "",
    
    [Parameter(Mandatory=$false)]
    [switch]$IncrementPatch = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$IncrementMinor = $false,
    
    [Parameter(Mandatory=$false)]
    [switch]$IncrementMajor = $false
)

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Actualizador de Versión FastMind" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Función para leer versión actual de capacitor.config.json
function Get-CurrentVersion {
    $configPath = "capacitor.config.json"
    if (Test-Path $configPath) {
        $config = Get-Content $configPath | ConvertFrom-Json
        if ($config.version) {
            return $config.version
        }
    }
    return $null
}

# Función para incrementar versión
function Increment-Version {
    param([string]$Version, [string]$Type)
    
    $parts = $Version -split '\.'
    $major = [int]$parts[0]
    $minor = [int]$parts[1]
    $patch = [int]$parts[2]
    
    switch ($Type) {
        "major" { 
            $major++
            $minor = 0
            $patch = 0
        }
        "minor" { 
            $minor++
            $patch = 0
        }
        "patch" { 
            $patch++
        }
    }
    
    return "$major.$minor.$patch"
}

# Determinar la nueva versión
$currentVersion = Get-CurrentVersion

if ($NewVersion -eq "") {
    if ($IncrementPatch) {
        if ($currentVersion) {
            $NewVersion = Increment-Version -Version $currentVersion -Type "patch"
            Write-Host "Incrementando versión PATCH..." -ForegroundColor Yellow
        } else {
            Write-Host "No se pudo determinar la versión actual. Usa -NewVersion para especificar." -ForegroundColor Red
            exit 1
        }
    }
    elseif ($IncrementMinor) {
        if ($currentVersion) {
            $NewVersion = Increment-Version -Version $currentVersion -Type "minor"
            Write-Host "Incrementando versión MINOR..." -ForegroundColor Yellow
        } else {
            Write-Host "No se pudo determinar la versión actual. Usa -NewVersion para especificar." -ForegroundColor Red
            exit 1
        }
    }
    elseif ($IncrementMajor) {
        if ($currentVersion) {
            $NewVersion = Increment-Version -Version $currentVersion -Type "major"
            Write-Host "Incrementando versión MAJOR..." -ForegroundColor Yellow
        } else {
            Write-Host "No se pudo determinar la versión actual. Usa -NewVersion para especificar." -ForegroundColor Red
            exit 1
        }
    }
    else {
        if ($currentVersion) {
            Write-Host "Versión actual: $currentVersion" -ForegroundColor Cyan
            Write-Host ""
            $NewVersion = Read-Host "Ingresa la nueva versión (ej: 1.0.2)"
        } else {
            $NewVersion = Read-Host "Ingresa la versión (ej: 1.0.1)"
        }
    }
}

# Validar formato de versión (X.Y.Z)
if ($NewVersion -notmatch '^\d+\.\d+\.\d+$') {
    Write-Host "Error: La versión debe tener el formato X.Y.Z (ej: 1.0.2)" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Actualizando a versión: $NewVersion" -ForegroundColor Green
Write-Host ""

# 1. Actualizar capacitor.config.json
Write-Host "1. Actualizando capacitor.config.json..." -ForegroundColor Yellow
$configPath = "capacitor.config.json"
if (Test-Path $configPath) {
    $config = Get-Content $configPath | ConvertFrom-Json
    $config.version = $NewVersion
    $config | ConvertTo-Json -Depth 10 | Set-Content $configPath
    Write-Host "   ✓ capacitor.config.json actualizado" -ForegroundColor Green
} else {
    Write-Host "   ✗ capacitor.config.json no encontrado" -ForegroundColor Red
}

# 2. Actualizar Android build.gradle
Write-Host "2. Actualizando Android build.gradle..." -ForegroundColor Yellow
$gradlePath = "android\app\build.gradle"
if (Test-Path $gradlePath) {
    $gradleContent = Get-Content $gradlePath -Raw
    
    # Leer versionCode actual
    if ($gradleContent -match 'versionCode\s+(\d+)') {
        $currentVersionCode = [int]$matches[1]
        $newVersionCode = $currentVersionCode + 1
        Write-Host "   versionCode: $currentVersionCode → $newVersionCode" -ForegroundColor Cyan
    } else {
        Write-Host "   ⚠ No se encontró versionCode, usando 1" -ForegroundColor Yellow
        $newVersionCode = 1
    }
    
    # Actualizar versionName
    $gradleContent = $gradleContent -replace 'versionName\s+"[^"]+"', "versionName `"$NewVersion`""
    
    # Actualizar versionCode
    $gradleContent = $gradleContent -replace 'versionCode\s+\d+', "versionCode $newVersionCode"
    
    Set-Content -Path $gradlePath -Value $gradleContent -NoNewline
    Write-Host "   ✓ Android build.gradle actualizado" -ForegroundColor Green
    Write-Host "     - versionName: $NewVersion" -ForegroundColor Gray
    Write-Host "     - versionCode: $newVersionCode" -ForegroundColor Gray
} else {
    Write-Host "   ✗ build.gradle no encontrado" -ForegroundColor Red
}

# 3. Actualizar iOS project.pbxproj
Write-Host "3. Actualizando iOS project.pbxproj..." -ForegroundColor Yellow
$pbxprojPath = "ios\App\App.xcodeproj\project.pbxproj"
if (Test-Path $pbxprojPath) {
    $pbxprojContent = Get-Content $pbxprojPath -Raw
    
    # Leer CURRENT_PROJECT_VERSION actual (debe ser el mismo en Debug y Release)
    if ($pbxprojContent -match 'CURRENT_PROJECT_VERSION\s*=\s*(\d+);') {
        $currentBuildNumber = [int]$matches[1]
        $newBuildNumber = $currentBuildNumber + 1
        Write-Host "   CURRENT_PROJECT_VERSION: $currentBuildNumber → $newBuildNumber" -ForegroundColor Cyan
    } else {
        Write-Host "   ⚠ No se encontró CURRENT_PROJECT_VERSION, usando 1" -ForegroundColor Yellow
        $newBuildNumber = 1
    }
    
    # Actualizar MARKETING_VERSION (en Debug y Release)
    $pbxprojContent = $pbxprojContent -replace 'MARKETING_VERSION\s*=\s*[^;]+;', "MARKETING_VERSION = $NewVersion;"
    
    # Actualizar CURRENT_PROJECT_VERSION (en Debug y Release)
    $pbxprojContent = $pbxprojContent -replace 'CURRENT_PROJECT_VERSION\s*=\s*\d+;', "CURRENT_PROJECT_VERSION = $newBuildNumber;"
    
    Set-Content -Path $pbxprojPath -Value $pbxprojContent -NoNewline
    Write-Host "   ✓ iOS project.pbxproj actualizado" -ForegroundColor Green
    Write-Host "     - MARKETING_VERSION: $NewVersion (Debug y Release)" -ForegroundColor Gray
    Write-Host "     - CURRENT_PROJECT_VERSION: $newBuildNumber (Debug y Release)" -ForegroundColor Gray
} else {
    Write-Host "   ✗ project.pbxproj no encontrado" -ForegroundColor Red
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "✓ Versión actualizada exitosamente" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Resumen de cambios:" -ForegroundColor Yellow
Write-Host "  Capacitor:    $NewVersion" -ForegroundColor White
Write-Host "  Android:      $NewVersion (versionCode: $newVersionCode)" -ForegroundColor White
Write-Host "  iOS:          $NewVersion (build: $newBuildNumber)" -ForegroundColor White
Write-Host ""
Write-Host "Próximos pasos:" -ForegroundColor Yellow
Write-Host "  1. Ejecuta: npx cap sync (para sincronizar archivos)" -ForegroundColor Cyan
Write-Host "  2. Construye tu app con la nueva versión" -ForegroundColor Cyan
Write-Host ""

