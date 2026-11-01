[CmdletBinding()]
param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$OutputPath,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

if ([string]::IsNullOrWhiteSpace($JdkHome)) {
    throw "Define JAVA_HOME o proporciona -JdkHome con una JDK 25."
}

$JdkHome = (Resolve-Path -LiteralPath $JdkHome).Path
$jpackage = Join-Path $JdkHome "bin\jpackage.exe"
$jdkRelease = Join-Path $JdkHome "release"
if (-not (Test-Path -LiteralPath $jpackage -PathType Leaf) -or
    -not (Test-Path -LiteralPath $jdkRelease -PathType Leaf)) {
    throw "No se encontro jpackage.exe y el archivo release de JDK en $JdkHome."
}

$releaseMetadata = Get-Content -LiteralPath $jdkRelease -Raw
if ($releaseMetadata -notmatch 'JAVA_VERSION="25\.') {
    throw "Se requiere JDK 25 para compilar y empaquetar el runtime. Version detectada en $jdkRelease."
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path $root "dist\FinanzasApp-Setup.exe"
} elseif (-not [IO.Path]::IsPathRooted($OutputPath)) {
    $OutputPath = Join-Path $root $OutputPath
}
$OutputPath = [IO.Path]::GetFullPath($OutputPath)

if ((Test-Path -LiteralPath $OutputPath) -and -not $Force) {
    throw "El archivo ya existe: $OutputPath. Usa -Force para reemplazarlo."
}

$oldJavaHome = $env:JAVA_HOME
$oldPath = $env:Path
$workPath = Join-Path $env:TEMP ("FinanzasAppPackage-" + [Guid]::NewGuid().ToString("N"))
$maven = Join-Path $root "mvnw.cmd"
$payloadName = "finanzasapp-image.zip"
$installerScript = Join-Path $PSScriptRoot "install-windows-standalone.ps1"
$uninstallerScript = Join-Path $PSScriptRoot "installer\uninstall-finanzasapp.ps1"
$iconPath = Join-Path $PSScriptRoot "installer\FinanzasApp.ico"
$appVersion = "2.0.0"
if (-not (Test-Path -LiteralPath $iconPath -PathType Leaf)) {
    throw "Falta el icono $iconPath. Generalo con: python scripts\installer\generate-icon.py"
}

try {
    $env:JAVA_HOME = $JdkHome
    $env:Path = (Join-Path $JdkHome "bin") + ";" + $oldPath

    & $maven --batch-mode -f (Join-Path $root "pom.xml") clean package
    if ($LASTEXITCODE -ne 0) {
        throw "La compilacion o las pruebas del cliente fallaron (mvnw: $LASTEXITCODE)."
    }

    $jarPath = Join-Path $root "target\FinanzasApp-shaded.jar"
    if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
        throw "No se encontro el JAR ejecutable: $jarPath"
    }

    New-Item -ItemType Directory -Path $workPath -Force | Out-Null
    $inputPath = Join-Path $workPath "input"
    $imageDestination = Join-Path $workPath "image"
    New-Item -ItemType Directory -Path $inputPath -Force | Out-Null
    Copy-Item -LiteralPath $jarPath -Destination (Join-Path $inputPath "FinanzasApp.jar")

    # Runtime reducido: solo los modulos que usa el cliente (jdeps) + datos regionales es/en
    # (formato de moneda es-CO), charsets y accesibilidad para lectores de pantalla.
    $runtimePath = Join-Path $workPath "runtime"
    & (Join-Path $JdkHome "bin\jlink.exe") `
        --add-modules "java.base,java.desktop,java.logging,java.net.http,jdk.httpserver,jdk.localedata,jdk.charsets,jdk.accessibility" `
        --include-locales "es,en" `
        --strip-debug `
        --no-header-files `
        --no-man-pages `
        --compress "zip-9" `
        --output $runtimePath
    if ($LASTEXITCODE -ne 0) {
        throw "jlink no pudo crear el runtime reducido (codigo: $LASTEXITCODE)."
    }

    # Segundo lanzador que entra directo en modo API (sin variables de entorno).
    # La URL se toma de FINANZAS_API_BASE_URL o, por defecto, http://localhost:8080.
    $serverLauncher = Join-Path $workPath "FinanzasApp-Servidor.properties"
    Set-Content -LiteralPath $serverLauncher -Encoding ASCII -Value @(
        "java-options=-Dfinanzas.api.enabled=true",
        "description=FinanzasApp conectada al servidor (modo API)",
        ("icon=" + $iconPath.Replace("\", "\\"))
    )

    & $jpackage `
        --type app-image `
        --name FinanzasApp `
        --input $inputPath `
        --main-jar "FinanzasApp.jar" `
        --main-class "com.finanzas.Main" `
        --app-version $appVersion `
        --vendor "Equipo FinanzasApp" `
        --description "Finanzas personales y del hogar" `
        --copyright "Equipo FinanzasApp - UNIMINUTO" `
        --icon $iconPath `
        --runtime-image $runtimePath `
        --java-options "-Dfile.encoding=UTF-8" `
        --add-launcher "FinanzasApp-Servidor=$serverLauncher" `
        --dest $imageDestination
    if ($LASTEXITCODE -ne 0) {
        throw "jpackage no pudo crear la aplicacion portable (codigo: $LASTEXITCODE)."
    }

    $imagePath = Join-Path $imageDestination "FinanzasApp"
    $imageExe = Join-Path $imagePath "FinanzasApp.exe"
    $runtimeRelease = Join-Path $imagePath "runtime\release"
    if (-not (Test-Path -LiteralPath $imageExe -PathType Leaf) -or
        -not (Test-Path -LiteralPath $runtimeRelease -PathType Leaf)) {
        throw "La imagen generada no contiene el ejecutable y runtime esperados."
    }
    if ((Get-Content -LiteralPath $runtimeRelease -Raw) -notmatch 'JAVA_VERSION="25\.') {
        throw "La imagen no contiene el runtime Java 25 esperado."
    }

    Copy-Item -LiteralPath $uninstallerScript -Destination (Join-Path $imagePath "uninstall-finanzasapp.ps1")
    Copy-Item -LiteralPath $iconPath -Destination (Join-Path $imagePath "FinanzasApp.ico")

    $payloadPath = Join-Path $workPath $payloadName
    Compress-Archive -Path (Join-Path $imagePath "*") -DestinationPath $payloadPath -CompressionLevel Optimal
    Copy-Item -LiteralPath $installerScript -Destination (Join-Path $workPath "install-windows-standalone.ps1")

    $outputDirectory = Split-Path -Parent $OutputPath
    New-Item -ItemType Directory -Path $outputDirectory -Force | Out-Null
    $sedPath = Join-Path $workPath "FinanzasApp-Setup.sed"
    $sedContent = @"
[Version]
Class=IEXPRESS
SEDVersion=3
[Options]
PackagePurpose=InstallApp
ShowInstallProgramWindow=0
HideExtractAnimation=1
UseLongFileName=1
InsideCompressed=0
CAB_FixedSize=0
CAB_ResvCodeSigning=0
RebootMode=N
InstallPrompt=%InstallPrompt%
DisplayLicense=%DisplayLicense%
FinishMessage=%FinishMessage%
TargetName=%TargetName%
FriendlyName=%FriendlyName%
AppLaunched=%AppLaunched%
PostInstallCmd=<None>
AdminQuietInstCmd=
UserQuietInstCmd=
SourceFiles=SourceFiles
[SourceFiles]
SourceFiles0=$workPath
[SourceFiles0]
%FILE0%=
%FILE1%=
[Strings]
InstallPrompt="Instalar FinanzasApp en este equipo?"
DisplayLicense=
FinishMessage=
TargetName="$OutputPath"
FriendlyName="FinanzasApp"
AppLaunched="powershell.exe -NoLogo -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File install-windows-standalone.ps1 -Version $appVersion"
FILE0="$payloadName"
FILE1="install-windows-standalone.ps1"
"@
    Set-Content -LiteralPath $sedPath -Value $sedContent -Encoding ASCII

    $iexpress = Join-Path $env:SystemRoot "System32\iexpress.exe"
    $iexpressProcess = Start-Process -FilePath $iexpress -ArgumentList @("/N", "/Q", $sedPath) -Wait -PassThru
    if ($iexpressProcess.ExitCode -ne 0 -or -not (Test-Path -LiteralPath $OutputPath -PathType Leaf)) {
        throw "IExpress no pudo crear el instalador (codigo: $($iexpressProcess.ExitCode))."
    }

    Write-Output "Instalador creado: $OutputPath"
    Write-Output ("Tamano: {0:N1} MB" -f ((Get-Item -LiteralPath $OutputPath).Length / 1MB))
    Write-Output "Runtime incluido: Java 25 LTS (reducido con jlink)"
    Write-Output "Lanzadores: FinanzasApp.exe (modo local) y FinanzasApp-Servidor.exe (modo API)"
}
finally {
    $env:JAVA_HOME = $oldJavaHome
    $env:Path = $oldPath
    if (Test-Path -LiteralPath $workPath) {
        Remove-Item -LiteralPath $workPath -Recurse -Force
    }
}
