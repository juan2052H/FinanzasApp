[CmdletBinding()]
param(
    [switch]$Quiet,
    [string]$InstallPath
)

# $PSScriptRoot puede llegar vacio como valor por defecto de un parametro en Windows PowerShell 5.1.
if ([string]::IsNullOrWhiteSpace($InstallPath)) {
    $InstallPath = Split-Path -Parent $MyInvocation.MyCommand.Path
}

# Desinstala FinanzasApp para el usuario actual. Conserva los datos en %USERPROFILE%\.finanzasapp.
$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Windows.Forms
$UninstallKey = "HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\FinanzasApp"

# El script vive dentro de la carpeta que va a borrar. PowerShell ya lo leyo completo y no
# bloquea el archivo; solo hay que salir de la carpeta para que no quede como directorio en uso.
# Set-Location no cambia el directorio de trabajo del proceso de Windows: se cambian ambos.
Set-Location -LiteralPath $env:TEMP
[Environment]::CurrentDirectory = $env:TEMP

function Show-Message([string]$Text, [string]$Icon = "Information") {
    if ($Quiet) { Write-Output $Text; return }
    [System.Windows.Forms.MessageBox]::Show($Text, "FinanzasApp", "OK", $Icon) | Out-Null
}

try {
    if (-not $Quiet) {
        $answer = [System.Windows.Forms.MessageBox]::Show(
            "Deseas desinstalar FinanzasApp?`n`nTus datos locales en $env:USERPROFILE\.finanzasapp no se borran.",
            "Desinstalar FinanzasApp", "YesNo", "Question")
        if ($answer -ne "Yes") { exit 0 }
    }

    Get-Process -Name "FinanzasApp", "FinanzasApp-Servidor" -ErrorAction SilentlyContinue |
        Where-Object { $_.Path -and $_.Path.StartsWith($InstallPath, [StringComparison]::OrdinalIgnoreCase) } |
        ForEach-Object { $_.CloseMainWindow() | Out-Null; if (-not $_.WaitForExit(5000)) { $_.Kill() } }

    $programsPath = [Environment]::GetFolderPath([Environment+SpecialFolder]::Programs)
    $desktopPath = [Environment]::GetFolderPath([Environment+SpecialFolder]::Desktop)
    Remove-Item -LiteralPath (Join-Path $programsPath "FinanzasApp") -Recurse -Force -ErrorAction SilentlyContinue
    foreach ($name in "FinanzasApp.lnk", "FinanzasApp (servidor).lnk") {
        Remove-Item -LiteralPath (Join-Path $desktopPath $name) -Force -ErrorAction SilentlyContinue
    }

    if ((Test-Path -LiteralPath (Join-Path $InstallPath "FinanzasApp.exe")) -and $InstallPath.Length -gt 10) {
        Remove-Item -LiteralPath $InstallPath -Recurse -Force
    }
    Remove-Item -Path $UninstallKey -Recurse -Force -ErrorAction SilentlyContinue

    Show-Message "FinanzasApp se desinstalo correctamente."
}
catch {
    Show-Message ("No fue posible desinstalar FinanzasApp:`n`n" + $_.Exception.Message) "Error"
    exit 1
}
