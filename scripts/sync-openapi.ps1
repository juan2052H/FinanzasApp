param(
    [string]$BackendOpenApi = "finanzas-backend\src\main\resources\openapi\finanzas-api.yaml",
    [string]$ClientOpenApi = "src\main\resources\openapi\finanzas-api.yaml"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $BackendOpenApi)) {
    throw "No existe OpenAPI canonico: $BackendOpenApi"
}

$clientDir = Split-Path -Parent $ClientOpenApi
if ($clientDir -and -not (Test-Path -LiteralPath $clientDir)) {
    New-Item -ItemType Directory -Path $clientDir | Out-Null
}

Copy-Item -LiteralPath $BackendOpenApi -Destination $ClientOpenApi -Force
Write-Host "OpenAPI sincronizado desde $BackendOpenApi hacia $ClientOpenApi"
