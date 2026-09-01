param(
    [string]$AppName = "FinanzasApp"
)

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$BuildDir = Join-Path $Root "build\classes"
$DistDir = Join-Path $Root "dist"
$JarPath = Join-Path $DistDir "$AppName.jar"
$ImageDir = Join-Path $DistDir "image"

New-Item -ItemType Directory -Force -Path $BuildDir | Out-Null
New-Item -ItemType Directory -Force -Path $DistDir | Out-Null

$Sources = Get-ChildItem -Path (Join-Path $Root "src\main\java") -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if ($Sources.Count -eq 0) {
    throw "No se encontraron fuentes Java."
}

javac -encoding UTF-8 -d $BuildDir $Sources
jar --create --file $JarPath --main-class com.finanzas.Main -C $BuildDir .

$JPackage = Get-Command jpackage -ErrorAction SilentlyContinue
if ($null -eq $JPackage) {
    Write-Host "JAR generado en $JarPath"
    Write-Host "jpackage no esta disponible en esta JDK. Instala una JDK con jpackage para crear imagen desktop."
    exit 0
}

if (Test-Path $ImageDir) {
    Remove-Item -LiteralPath $ImageDir -Recurse -Force
}

jpackage `
    --type app-image `
    --name $AppName `
    --input $DistDir `
    --main-jar "$AppName.jar" `
    --main-class com.finanzas.Main `
    --dest $ImageDir

Write-Host "JAR generado en $JarPath"
Write-Host "Imagen desktop generada en $ImageDir"
