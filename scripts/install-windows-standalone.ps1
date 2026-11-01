[CmdletBinding()]
param(
    [string]$PayloadPath,
    [string]$InstallPath = (Join-Path $env:LOCALAPPDATA "Programs\FinanzasApp"),
    [string]$Version = "2.0.0",
    [switch]$NoLaunch,
    [switch]$NoShortcuts,
    [switch]$Quiet
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms
# $PSScriptRoot puede llegar vacio como valor por defecto de un parametro en Windows PowerShell 5.1.
if ([string]::IsNullOrWhiteSpace($PayloadPath)) {
    $PayloadPath = Join-Path (Split-Path -Parent $MyInvocation.MyCommand.Path) "finanzasapp-image.zip"
}
$UninstallKey = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\FinanzasApp"

function Show-Message([string]$Text, [string]$Icon = "Information") {
    if ($Quiet) { Write-Output $Text; return }
    [System.Windows.Forms.MessageBox]::Show($Text, "FinanzasApp", "OK", $Icon) | Out-Null
}

function New-Shortcut([string]$Path, [string]$Target, [string]$Description, [string]$Arguments = "") {
    $shortcut = (New-Object -ComObject WScript.Shell).CreateShortcut($Path)
    $shortcut.TargetPath = $Target
    $shortcut.Arguments = $Arguments
    $shortcut.WorkingDirectory = $InstallPath
    $shortcut.Description = $Description
    $shortcut.IconLocation = (Join-Path $InstallPath "FinanzasApp.ico") + ",0"
    $shortcut.Save()
}

$workPath = Join-Path $env:TEMP ("FinanzasAppInstall-" + [Guid]::NewGuid().ToString("N"))
$imagePath = Join-Path $workPath "image"

try {
    if (-not (Test-Path -LiteralPath $PayloadPath -PathType Leaf)) {
        throw "No se encontro el paquete de FinanzasApp: $PayloadPath"
    }

    # Una actualizacion no puede reemplazar archivos en uso.
    Get-Process -Name "FinanzasApp", "FinanzasApp-Servidor" -ErrorAction SilentlyContinue |
        Where-Object { $_.Path -and $_.Path.StartsWith($InstallPath, [StringComparison]::OrdinalIgnoreCase) } |
        ForEach-Object { $_.CloseMainWindow() | Out-Null; if (-not $_.WaitForExit(5000)) { $_.Kill() } }

    New-Item -ItemType Directory -Path $imagePath -Force | Out-Null
    Expand-Archive -LiteralPath $PayloadPath -DestinationPath $imagePath -Force
    if (-not (Test-Path -LiteralPath (Join-Path $imagePath "FinanzasApp.exe") -PathType Leaf)) {
        throw "El paquete no contiene FinanzasApp.exe."
    }

    # /MIR elimina archivos de versiones anteriores; los datos del usuario viven en %USERPROFILE%\.finanzasapp.
    New-Item -ItemType Directory -Path $InstallPath -Force | Out-Null
    & robocopy.exe $imagePath $InstallPath /MIR /COPY:DAT /R:2 /W:1 /NFL /NDL /NJH /NJS | Out-Null
    if ($LASTEXITCODE -gt 7) {
        throw "No se pudieron copiar los archivos de FinanzasApp (robocopy: $LASTEXITCODE)."
    }

    $installedExe = Join-Path $InstallPath "FinanzasApp.exe"
    $serverExe = Join-Path $InstallPath "FinanzasApp-Servidor.exe"
    $uninstaller = Join-Path $InstallPath "uninstall-finanzasapp.ps1"
    $uninstallCommand = "powershell.exe -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$uninstaller`""

    if (-not $NoShortcuts) {
        $programsPath = [Environment]::GetFolderPath([Environment+SpecialFolder]::Programs)
        $menuFolder = Join-Path $programsPath "FinanzasApp"
        New-Item -ItemType Directory -Path $menuFolder -Force | Out-Null
        Get-ChildItem -LiteralPath $menuFolder -Filter "*.lnk" | Remove-Item -Force

        New-Shortcut (Join-Path $menuFolder "FinanzasApp.lnk") $installedExe "FinanzasApp en modo local (sin servidor)"
        if (Test-Path -LiteralPath $serverExe -PathType Leaf) {
            New-Shortcut (Join-Path $menuFolder "FinanzasApp (servidor).lnk") $serverExe "FinanzasApp conectada al backend (modo API)"
        }
        New-Shortcut (Join-Path $menuFolder "Desinstalar FinanzasApp.lnk") "powershell.exe" "Desinstalar FinanzasApp" `
            "-NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$uninstaller`""

        $desktopPath = [Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)
        New-Shortcut (Join-Path $desktopPath "FinanzasApp.lnk") $installedExe "FinanzasApp en modo local (sin servidor)"
        if (Test-Path -LiteralPath $serverExe -PathType Leaf) {
            New-Shortcut (Join-Path $desktopPath "FinanzasApp (servidor).lnk") $serverExe "FinanzasApp conectada al backend (modo API)"
        }
    }

    # Registro en "Aplicaciones instaladas" (por usuario, sin permisos de administrador).
    $sizeKb = [int]((Get-ChildItem -LiteralPath $InstallPath -Recurse -File | Measure-Object Length -Sum).Sum / 1KB)
    New-Item -Path $UninstallKey -Force | Out-Null
    $values = @{
        DisplayName = "FinanzasApp"; DisplayVersion = $Version; Publisher = "Equipo FinanzasApp"
        DisplayIcon = (Join-Path $InstallPath "FinanzasApp.ico"); InstallLocation = $InstallPath
        UninstallString = $uninstallCommand; QuietUninstallString = "$uninstallCommand -Quiet"
        InstallDate = (Get-Date).ToString("yyyyMMdd"); URLInfoAbout = "https://github.com/juan2052H/FinanzasApp"
    }
    foreach ($entry in $values.GetEnumerator()) {
        New-ItemProperty -Path $UninstallKey -Name $entry.Key -Value $entry.Value -PropertyType String -Force | Out-Null
    }
    foreach ($flag in "NoModify", "NoRepair") {
        New-ItemProperty -Path $UninstallKey -Name $flag -Value 1 -PropertyType DWord -Force | Out-Null
    }
    New-ItemProperty -Path $UninstallKey -Name "EstimatedSize" -Value $sizeKb -PropertyType DWord -Force | Out-Null

    Show-Message ("FinanzasApp $Version se instalo correctamente.`n`n" +
        "- 'FinanzasApp': modo local, sin servidor.`n" +
        "- 'FinanzasApp (servidor)': se conecta al backend en http://localhost:8080 " +
        "(o en la URL de la variable FINANZAS_API_BASE_URL).`n`n" +
        "Encontraras ambos accesos en el Escritorio y en el menu Inicio.")

    if (-not $NoLaunch) {
        Start-Process -FilePath $installedExe -WorkingDirectory $InstallPath
    }
}
catch {
    Show-Message ("No fue posible instalar FinanzasApp:`n`n" + $_.Exception.Message) "Error"
    exit 1
}
finally {
    if (Test-Path -LiteralPath $workPath) {
        Remove-Item -LiteralPath $workPath -Recurse -Force -ErrorAction SilentlyContinue
    }
}
