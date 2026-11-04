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
    # Expand-Archive de PowerShell 5.1 rechaza la extension .xlsx; se lee con la API de ZIP de .NET.
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $libro = [IO.Compression.ZipFile]::OpenRead($ExcelPath)
    try {
        $textoExcel = ($libro.Entries | Where-Object { $_.FullName -like "*.xml" } | ForEach-Object {
            $reader = New-Object IO.StreamReader($_.Open()); try { $reader.ReadToEnd() } finally { $reader.Dispose() }
        }) -join ""
    } finally {
        $libro.Dispose()
    }
    if ($textoExcel -match "\[Completar") {
        $problemas.Add("El Excel todavia contiene '[Completar...]' (integrantes / responsables).")
    }

    $commit = (git rev-parse --short HEAD).Trim()
    # El instalador ya va en su propia carpeta ejecutable/: no se duplica dentro del ZIP de codigo.
    git archive --format=zip --prefix=FinanzasApp/ -o (Join-Path $carpeta "FinanzasApp-codigo-fuente.zip") HEAD -- . ":(exclude)ejecutable"
    if ($LASTEXITCODE -ne 0) { throw "git archive fallo." }

    # El instalador va en su propia carpeta para que sea lo primero que se encuentre.
    $carpetaExe = Join-Path $carpeta "ejecutable"
    New-Item -ItemType Directory -Path $carpetaExe -Force | Out-Null
    Copy-Item -LiteralPath $installer -Destination $carpetaExe
    $leemeExe = @"
FinanzasApp - LEEME PRIMERO
===========================

Para probar FinanzasApp con las cuentas de prueba necesita 3 cosas, en este orden:
  A) Docker Desktop instalado y abierto   (solo se instala la primera vez)
  B) El servidor encendido con un comando (cada vez que vaya a usar la aplicacion)
  C) La aplicacion instalada              (solo la primera vez)

Manual completo con imagenes y solucion de problemas: ..\REQUISITOS_E_INSTALACION.docx (carpeta de arriba)


A) DOCKER DESKTOP (solo la primera vez)
---------------------------------------
Requisitos: Windows 10 22H2 u 11 de 64 bits, 8 GB de RAM, 10 GB libres en disco,
virtualizacion activada (Administrador de tareas > Rendimiento > CPU > "Virtualizacion: Habilitado").

1. Abra PowerShell COMO ADMINISTRADOR (Inicio > escriba PowerShell > clic derecho >
   Ejecutar como administrador) y ejecute:

       wsl --install --no-distribution

   Reinicie el computador. Despues, otra vez en PowerShell como administrador:

       wsl --update

2. Descargue Docker Desktop (enlace directo, unos 600 MB):

       https://desktop.docker.com/win/main/amd64/Docker%20Desktop%20Installer.exe

   (Si su PC tiene procesador ARM: https://desktop.docker.com/win/main/arm64/Docker%20Desktop%20Installer.exe
    Pagina oficial por si el enlace cambia: https://www.docker.com/products/docker-desktop/ )

3. Abra "Docker Desktop Installer.exe", deje marcada la opcion
   "Use WSL 2 instead of Hyper-V (recommended)", pulse Ok y al final "Close and restart".

4. Despues del reinicio abra Docker Desktop:
   - Pulse "Accept" en el acuerdo de servicio (gratis para uso personal y educativo).
   - Si pide iniciar sesion o una encuesta, pulse "Skip" (no necesita cuenta).
   - Espere a que abajo a la izquierda diga "Engine running" con un punto verde.

5. Configuracion recomendada (icono de engranaje "Settings" > General):
   - "Use the WSL 2 based engine": marcada.
   - "Start Docker Desktop when you sign in to your computer": marcada (asi abre solo).
   Si cambia algo pulse "Apply & restart".

6. Compruebe que Docker funciona. En PowerShell normal:

       docker --version
       docker run --rm hello-world

   Debe aparecer el texto "Hello from Docker!".


B) ENCENDER EL SERVIDOR (cada vez)
----------------------------------
1. Docker Desktop debe estar abierto y decir "Engine running".
2. Descomprima ..\FinanzasApp-codigo-fuente.zip (carpeta de arriba) en C:\FinanzasApp
   (clic derecho > Extraer todo > escriba C:\FinanzasApp > Extraer).
3. Abra PowerShell normal y ejecute, uno por uno:

       cd C:\FinanzasApp\FinanzasApp
       docker compose up -d --build

   La primera vez tarda entre 5 y 15 minutos. No cierre la ventana.

4. Compruebe el estado:

       docker compose ps -a

   Debe decir "(healthy)" en finanzas-postgres y finanzas-backend,
   y "Exited (0)" en db-seed (es normal: ya cargo las cuentas de prueba).

Para apagar el servidor:                 docker compose down
Para borrar todo y recargar los datos:   docker compose down -v   y luego   docker compose up -d


C) INSTALAR Y ABRIR LA APLICACION
---------------------------------
1. Doble clic en FinanzasApp-Setup.exe (esta en esta carpeta) y responda Si.
   Si Windows muestra "Windows protegio su PC": Mas informacion > Ejecutar de todas formas.
2. Abra el acceso directo "FinanzasApp (servidor)" del Escritorio.
   (El otro acceso, "FinanzasApp", es el modo local y NO tiene las cuentas de prueba.)
3. Inicie sesion:

       Usuario:     ana.duena@finanzasapp.test
       Contrasena:  Prueba2026*        (el asterisco final es parte de la contrasena)

   Otras cuentas (misma contrasena): carlos.miembro@finanzasapp.test (miembro),
   laura.lectora@finanzasapp.test (solo lectura).
4. La primera vez aparece "Primeros pasos": deje lo opcional vacio y pulse "Empezar".


PROBLEMAS FRECUENTES
--------------------
- "docker" no se reconoce        -> Cierre y abra PowerShell; si sigue, reinstale Docker Desktop.
- "error during connect"         -> Docker Desktop esta cerrado: abralo y espere "Engine running".
- "WSL needs updating"           -> PowerShell como administrador: wsl --update  y reinicie.
- "port is already allocated"    -> Otro programa usa el puerto 5432 u 8080 (ver Parte 10 del manual).
- "Credenciales invalidas"       -> Revise el asterisco final y que el servidor este encendido.
"@
    Set-Content -LiteralPath (Join-Path $carpetaExe "LEEME_PRIMERO.txt") -Value $leemeExe -Encoding UTF8
    Copy-Item -LiteralPath $ExcelPath -Destination (Join-Path $carpeta "FinanzasApp_Formato_Calidad_Final.xlsx")
    Copy-Item -LiteralPath (Join-Path $root "docs\GUIA_EVALUADOR.md") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root "docs\REQUISITOS_E_INSTALACION.md") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root "docs\REQUISITOS_E_INSTALACION.docx") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root "docs\ARQUITECTURA_Y_AMBIENTE.md") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root "README.md") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root "database\crear_bd.sql") -Destination $carpeta
    Copy-Item -LiteralPath (Join-Path $root ".env.example") -Destination (Join-Path $carpeta "env.example")

    $hash = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash
    $leeme = @"
FinanzasApp - Entrega final
===========================

Repositorio: https://github.com/juan2052H/FinanzasApp  (commit $commit)

Empiece por REQUISITOS_E_INSTALACION (Word o .md): requisitos del PC, como ejecutarlo y cuentas de prueba.
Luego GUIA_EVALUADOR.md: recorrido de verificacion ligado a requisitos y casos de prueba.

Contenido
- REQUISITOS_E_INSTALACION.docx / .md  Requisitos del PC, instalacion, ejecucion y cuentas de prueba.
- GUIA_EVALUADOR.md                     Pasos de instalacion, usuarios de prueba y que verificar.
- FinanzasApp-codigo-fuente.zip         Codigo fuente completo (cliente, backend, Docker, scripts).
- ejecutable\FinanzasApp-Setup.exe      Instalador de Windows (incluye Java 25).
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
    # En Windows PowerShell 5.1, Compress-Archive y ZipFile.CreateFromDirectory guardan rutas con '\',
    # que se extraen mal fuera de Windows: se escribe cada entrada con '/'.
    Add-Type -AssemblyName System.IO.Compression
    $zip = [IO.Compression.ZipFile]::Open($OutputPath, [IO.Compression.ZipArchiveMode]::Create)
    try {
        $base = Split-Path -Parent $carpeta
        foreach ($archivo in Get-ChildItem -LiteralPath $carpeta -Recurse -File) {
            $nombre = $archivo.FullName.Substring($base.Length + 1).Replace("\", "/")
            [IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $archivo.FullName, $nombre, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
        }
    } finally {
        $zip.Dispose()
    }
    Write-Output ("Entrega creada: {0} ({1:N1} MB, commit {2})" -f $OutputPath, ((Get-Item $OutputPath).Length / 1MB), $commit)
}
finally {
    Pop-Location
    if ($work -and (Test-Path -LiteralPath $work)) { Remove-Item -LiteralPath $work -Recurse -Force }
}
