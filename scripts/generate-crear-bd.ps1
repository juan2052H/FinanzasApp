param(
    [string]$MigrationsDir = (Join-Path $PSScriptRoot "..\finanzas-backend\src\main\resources\db\migration"),
    [string]$SeedFile = (Join-Path $PSScriptRoot "..\database\datos_prueba.sql"),
    [string]$Output = (Join-Path $PSScriptRoot "..\database\crear_bd.sql")
)

$ErrorActionPreference = "Stop"

$migrations = Get-ChildItem $MigrationsDir -Filter "V*__*.sql" |
    Sort-Object { [int]($_.Name -replace '^V(\d+)__.*$', '$1') }
$lastVersion = [int]($migrations[-1].Name -replace '^V(\d+)__.*$', '$1')

$builder = New-Object System.Text.StringBuilder
[void]$builder.AppendLine("-- =====================================================================")
[void]$builder.AppendLine("-- FinanzasApp - crear_bd.sql (PostgreSQL 16)")
[void]$builder.AppendLine("-- Generado por scripts/generate-crear-bd.ps1 a partir de las migraciones")
[void]$builder.AppendLine("-- Flyway V1..V$lastVersion y database/datos_prueba.sql. No editar a mano.")
[void]$builder.AppendLine("--")
[void]$builder.AppendLine("-- Uso (base vacia ya creada):")
[void]$builder.AppendLine("--   psql -h localhost -p 5432 -U finanzas -d finanzas -v ON_ERROR_STOP=1 -f database/crear_bd.sql")
[void]$builder.AppendLine("-- Si despues se arranca el backend sobre esta base, definir:")
[void]$builder.AppendLine("--   SPRING_FLYWAY_BASELINE_ON_MIGRATE=true")
[void]$builder.AppendLine("--   SPRING_FLYWAY_BASELINE_VERSION=$lastVersion")
[void]$builder.AppendLine("-- =====================================================================")
[void]$builder.AppendLine()

foreach ($migration in $migrations) {
    [void]$builder.AppendLine("-- ---------------------------------------------------------------------")
    [void]$builder.AppendLine("-- $($migration.Name)")
    [void]$builder.AppendLine("-- ---------------------------------------------------------------------")
    [void]$builder.AppendLine((Get-Content $migration.FullName -Raw).TrimEnd())
    [void]$builder.AppendLine()
}

[void]$builder.AppendLine("-- =====================================================================")
[void]$builder.AppendLine("-- Datos de prueba")
[void]$builder.AppendLine("-- =====================================================================")
[void]$builder.AppendLine((Get-Content $SeedFile -Raw).TrimEnd())

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText((Resolve-Path (Split-Path $Output)).Path + "\" + (Split-Path $Output -Leaf), $builder.ToString().Replace("`r`n", "`n"), $utf8NoBom)
Write-Host "Generado $Output (migraciones V1..V$lastVersion + datos de prueba)"
