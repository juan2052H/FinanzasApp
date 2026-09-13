#  Requires -Version 5.1
#  Script para generar instalador Windows .EXE para FinanzasApp
#  Uso: .\build-windows-installer.ps1

param(
    [string]$JdkHome = "C:\Program Files\Eclipse Adoptium\jdk-21.0.4+7",
    [string]$SourceJar = "target\FinanzasApp-1.0-SNAPSHOT-shaded.jar",
    [string]$OutputDir = "dist",
    [string]$AppVersion = "1.0.0",
    [string]$Vendor = "FinanzasApp",
    [string]$IconPath = "src\main\resources\icon.png"
)

Write-Host "╔════════════════════════════════════════════════════════════════╗"
Write-Host "║     Generando Instalador Windows para FinanzasApp             ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"
Write-Host ""

#  Verificar si jpackage está disponible
$jpackagePath = Join-Path $JdkHome "bin\jpackage.exe"
if (-not (Test-Path $jpackagePath)) {
    Write-Error "jpackage no encontrado en: $jpackagePath"
    Write-Host ""
    Write-Host "Asegúrate de que:"
    Write-Host "1. JDK 16+ está instalado (se requiere jpackage)"
    Write-Host "2. La ruta del JDK es correcta"
    Write-Host "3. Puedes especificar la ruta con -JdkHome"
    Write-Host ""
    Write-Host "Ejemplo:"
    Write-Host "  .\build-windows-installer.ps1 -JdkHome 'C:\Program Files\Java\jdk-21'"
    exit 1
}

Write-Host "✓ jpackage encontrado: $jpackagePath"
Write-Host ""

#  Verificar si el JAR existe
if (-not (Test-Path $SourceJar)) {
    Write-Error "JAR no encontrado: $SourceJar"
    Write-Host "Ejecuta primero: mvnw.cmd clean package -DskipTests"
    exit 1
}

Write-Host "✓ JAR source encontrado: $SourceJar"
$JarSize = (Get-Item $SourceJar).Length / 1MB
Write-Host "  Tamaño: $([Math]::Round($JarSize, 2)) MB"
Write-Host ""

#  Crear directorio de salida
if (-not (Test-Path $OutputDir)) {
    New-Item -ItemType Directory -Path $OutputDir | Out-Null
    Write-Host "✓ Directorio de salida creado: $OutputDir"
} else {
    Write-Host "✓ Directorio de salida existe: $OutputDir"
}
Write-Host ""

#  Preparar icono si existe
$iconArg = ""
if (Test-Path $IconPath) {
    Write-Host "✓ Icono encontrado: $IconPath"
    $iconArg = "--icon `"$IconPath`""
} else {
    Write-Host "⚠ Icono no encontrado: $IconPath (se usará icono por defecto)"
}
Write-Host ""

#  Generar instalador
Write-Host "Generando instalador Windows..."
Write-Host ""

$jpackageArgs = @(
    "--type", "exe",
    "--name", "FinanzasApp",
    "--input", "target",
    "--main-jar", "FinanzasApp-1.0-SNAPSHOT-shaded.jar",
    "--main-class", "com.finanzas.Main",
    "--app-version", $AppVersion,
    "--vendor", $Vendor,
    "--dest", $OutputDir,
    "--win-menu",
    "--win-shortcut",
    "--win-dir-chooser"
) + ($iconArg -split " " | Where-Object { $_ })

Write-Host "Comando:"
Write-Host "  $jpackagePath $($jpackageArgs -join ' ')"
Write-Host ""

#  Ejecutar jpackage
& $jpackagePath $jpackageArgs

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════════════╗"
    Write-Host "║                  ✅ ÉXITO - INSTALADOR GENERADO               ║"
    Write-Host "╚════════════════════════════════════════════════════════════════╝"
    Write-Host ""
    
    $exePath = Join-Path $OutputDir "FinanzasApp-$AppVersion.exe"
    if (Test-Path $exePath) {
        $exeSize = (Get-Item $exePath).Length / 1MB
        Write-Host "Instalador: $exePath"
        Write-Host "Tamaño: $([Math]::Round($exeSize, 2)) MB"
        Write-Host ""
        Write-Host "Para instalar:"
        Write-Host "  $exePath"
    }
} else {
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════════════╗"
    Write-Host "║                  ❌ ERROR AL GENERAR INSTALADOR               ║"
    Write-Host "╚════════════════════════════════════════════════════════════════╝"
    exit 1
}
