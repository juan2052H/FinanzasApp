[CmdletBinding()]
param(
    [string]$OutputPath,
    [string]$ExcelPath,
    [switch]$AllowIncomplete
)

# Arma dist\FinanzasApp_Entrega_Final.zip con todo lo que pide la entrega:
# codigo fuente (commit actual), instalador, formato Excel, guia del evaluador y documentacion.

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
if ([string]::IsNullOrWhiteSpace($OutputPath)) { $OutputPath = Join-Path $root "dist\FinanzasApp_Entrega_Final.zip" }
if ([string]::IsNullOrWhiteSpace($ExcelPath)) { $ExcelPath = Join-Path $root "calidad_U\FinanzasApp_Formato_Calidad_Final.xlsx" }
$installer = Join-Path $root "dist\FinanzasApp-Setup.exe"
$problemas = New-Object System.Collections.Generic.List[string]

Push-Location $root
try {
    if (git status --porcelain) {
        $problemas.Add("Hay cambios sin commit: el ZIP de codigo usa el ultimo commit (git archive HEAD).")
    }
    if (-not (Test-Path -LiteralPath $installer -PathType Leaf)) {
        throw "Falta $installer. Generelo con: .\scripts\build-windows-standalone.ps1 -JdkHome <JDK 25>"
    }
    try {
        $stream = [IO.File]::Open($ExcelPath, "Open", "Read", "None"); $stream.Close()
    } catch {
        throw "No se puede leer el Excel ($ExcelPath). Cierrelo en Excel y vuelva a intentar."
    }

    $work = Join-Path $env:TEMP ("FinanzasAppEntrega-" + [Guid]::NewGuid().ToString("N"))
    $carpeta = Join-Path $work "FinanzasApp_Entrega_Final"
    New-Item -ItemType Directory -Path $carpeta -Force | Out-Null

    # Si el Excel aun tiene marcadores sin completar, se avisa (y se detiene salvo -AllowIncomplete).
    $excelTmp = Join-Path $work "excel"
    Expand-Archive -LiteralPath $ExcelPath -DestinationPath $excelTmp
    $textoExcel = (Get-ChildItem $excelTmp -Recurse -Filter "*.xml" | Get-Content -Raw) -join ""
    if ($textoExcel -match "\[Completar") {
        $problemas.Add("El Excel todavia contiene '[Completar...]' (integrantes / responsables).")
    }

    $commit = (git rev-parse --short HEAD).Trim()
    git archive --format=zip --prefix=FinanzasApp/ -o (Join-Path $carpeta "FinanzasApp-codigo-fuente.zip") HEAD
    if ($LASTEXITCODE -ne 0) { throw "git archive fallo." }

    Copy-Item -LiteralPath $installer -Destination $carpeta
    Copy-Item -LiteralPath $ExcelPath -Destination (Join-Path $carpeta "FinanzasApp_Formato_Calidad_Final.xlsx")
    Copy-Item -LiteralPath (Join-Path $root "docs\GUIA_EVALUADOR.md") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root "docs\ARQUITECTURA_Y_AMBIENTE.md") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root "README.md") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root "database\crear_bd.sql") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root ".env.example") -Destination (Join-Path $carpeta "env.example")

    $hash = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash
    $leeme = @"
FinanzasApp - Entrega final
===========================

Repositorio: https://github.com/juan2052H/FinanzasApp  (commit $commit)

Empiece por GUIA_EVALUADOR.md: montar el ambiente, usuarios de prueba y recorrido de verificacion.

Contenido
- GUIA_EVALUADOR.md                     Pasos de instalacion, usuarios de prueba y que verificar.
- FinanzasApp-codigo-fuente.zip         Codigo fuente completo (cliente, backend, Docker, scripts).
- FinanzasApp-Setup.exe                 Instalador de Windows (incluye Java 25).
- FinanzasApp_Formato_Calidad_Final.xlsx Proyecto, interesados, requisitos, historias, casos de uso,
                                        mockups, arquitectura, trazabilidad, pruebas y defectos.
- ARQUITECTURA_Y_AMBIENTE.md            Componentes y capas, versiones, dependencias, variables de entorno.
- crear_bd.sql                          Script de la base de datos PostgreSQL 16 (tablas + datos de prueba).
- env.example                           Variables de entorno de ejemplo (copiar como .env).
- README.md                             Instalacion y ejecucion detalladas.

Usuarios de prueba (contrasena Prueba2026*):
  ana.duena@finanzasapp.test (OWNER), carlos.miembro@finanzasapp.test (MEMBER),
  laura.lectora@finanzasapp.test (VIEWER)

SHA-256 FinanzasApp-Setup.exe: $hash
"@
    Set-Content -LiteralPath (Join-Path $carpeta "LEEME.txt") -Value $leeme -Encoding UTF8

    if ($problemas.Count -gt 0) {
        $problemas | ForEach-Object { Write-Warning $_ }
        if (-not $AllowIncomplete) {
            throw "Corrija lo anterior o use -AllowIncomplete para empaquetar de todas formas."
        }
    }

    New-Item -ItemType Directory -Path (Split-Path -Parent $OutputPath) -Force | Out-Null
    if (Test-Path -LiteralPath $OutputPath) { Remove-Item -LiteralPath $OutputPath -Force }
    Compress-Archive -Path $carpeta -DestinationPath $OutputPath -CompressionLevel Optimal
    Write-Output ("Entrega creada: {0} ({1:N1} MB, commit {2})" -f $OutputPath, ((Get-Item $OutputPath).Length / 1MB), $commit)
}
finally {
    Pop-Location
    if ($work -and (Test-Path -LiteralPath $work)) { Remove-Item -LiteralPath $work -Recurse -Force }
}
