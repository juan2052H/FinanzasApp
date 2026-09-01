$ErrorActionPreference = "Stop"
trap {
    Write-Error $_
    exit 1
}

$wrapperDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$rootDir = Split-Path -Parent (Split-Path -Parent $wrapperDir)
$propertiesPath = Join-Path $wrapperDir "maven-wrapper.properties"
$properties = @{}

Get-Content $propertiesPath | ForEach-Object {
    if ($_ -match "^\s*([^#][^=]+?)\s*=\s*(.+)\s*$") {
        $properties[$matches[1].Trim()] = $matches[2].Trim()
    }
}

$version = $properties["distributionVersion"]
$url = $properties["distributionUrl"]
$expectedSha512 = $properties["distributionSha512"].ToLowerInvariant()
$distDir = Join-Path $wrapperDir "dists"
$zipPath = Join-Path $distDir "apache-maven-$version-bin.zip"
$mavenHome = Join-Path $distDir "apache-maven-$version"

New-Item -ItemType Directory -Force $distDir | Out-Null

if (-not (Test-Path (Join-Path $mavenHome "bin\mvn.cmd"))) {
    if (-not (Test-Path $zipPath)) {
        Write-Host "Downloading Apache Maven $version..."
        Invoke-WebRequest -Uri $url -OutFile $zipPath
    }

    $actualSha512 = (Get-FileHash -Algorithm SHA512 $zipPath).Hash.ToLowerInvariant()
    if ($actualSha512 -ne $expectedSha512) {
        Remove-Item -LiteralPath $zipPath -Force
        throw "Maven distribution checksum mismatch. Expected $expectedSha512 but got $actualSha512."
    }

    $extractTmp = Join-Path $distDir "extract-$version"
    if (Test-Path $extractTmp) {
        Remove-Item -LiteralPath $extractTmp -Recurse -Force
    }
    Expand-Archive -LiteralPath $zipPath -DestinationPath $extractTmp -Force

    if (Test-Path $mavenHome) {
        Remove-Item -LiteralPath $mavenHome -Recurse -Force
    }
    Move-Item -LiteralPath (Join-Path $extractTmp "apache-maven-$version") -Destination $mavenHome
    Remove-Item -LiteralPath $extractTmp -Recurse -Force
}

Write-Host "Apache Maven $version is ready at $mavenHome"
