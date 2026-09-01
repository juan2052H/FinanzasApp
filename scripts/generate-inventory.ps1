param(
    [string]$OutputPath = "docs/baseline/inventory.md"
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path ".").Path

function Get-RelativePath([string]$Path) {
    $resolved = (Resolve-Path $Path).Path
    return $resolved.Substring($root.Length).TrimStart([System.IO.Path]::DirectorySeparatorChar, [System.IO.Path]::AltDirectorySeparatorChar)
}

function Get-XmlText([xml]$Xml, [string]$LocalName) {
    $node = $Xml.Project.ChildNodes | Where-Object { $_.LocalName -eq $LocalName } | Select-Object -First 1
    if ($node) { return $node.InnerText.Trim() }
    return ""
}

function Add-Section([System.Collections.Generic.List[string]]$Lines, [string]$Title) {
    $Lines.Add("")
    $Lines.Add("## $Title")
    $Lines.Add("")
}

$lines = [System.Collections.Generic.List[string]]::new()
$lines.Add("# Inventario inicial")
$lines.Add("")
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss zzz"
$lines.Add("Generado: $timestamp")
$lines.Add("")
$lines.Add("Raiz: $root")

Add-Section $lines "Modulos Maven"
$pomFiles = Get-ChildItem -Path . -Filter pom.xml -Recurse | Sort-Object FullName
foreach ($pom in $pomFiles) {
    [xml]$xml = Get-Content -Raw $pom.FullName
    $artifactId = Get-XmlText $xml "artifactId"
    $packaging = Get-XmlText $xml "packaging"
    if ([string]::IsNullOrWhiteSpace($packaging)) { $packaging = "jar" }
    $javaVersion = ""
    $properties = $xml.Project.ChildNodes | Where-Object { $_.LocalName -eq "properties" } | Select-Object -First 1
    if ($properties) {
        foreach ($child in $properties.ChildNodes) {
            if ($child.LocalName -in @("java.version", "maven.compiler.source", "maven.compiler.release")) {
                $javaVersion = "$($child.LocalName)=$($child.InnerText.Trim())"
                break
            }
        }
    }
    $lines.Add("- " + (Get-RelativePath $pom.FullName) + ": artifactId=$artifactId; packaging=$packaging; $javaVersion")
}

Add-Section $lines "Clases de aplicacion"
$javaFiles = Get-ChildItem -Path src, finanzas-backend/src -Filter *.java -Recurse -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -notmatch "\\src\\test\\" } |
    Sort-Object FullName
foreach ($file in $javaFiles) {
    $content = Get-Content -Raw $file.FullName
    $package = if ($content -match "(?m)^\s*package\s+([^;]+);") { $matches[1] } else { "(default)" }
    $types = [regex]::Matches($content, "(?m)^\s*(?:public\s+)?(?:abstract\s+|final\s+|sealed\s+|non-sealed\s+)?(class|interface|enum|record)\s+([A-Za-z0-9_]+)") |
        ForEach-Object { "$($_.Groups[1].Value) $($_.Groups[2].Value)" }
    if (-not $types) { $types = @("(sin tipo publico detectado)") }
    $lines.Add("- " + (Get-RelativePath $file.FullName) + ": $package - $($types -join ", ")")
}

Add-Section $lines "Endpoints REST detectados"
$controllerFiles = Get-ChildItem -Path finanzas-backend/src/main/java -Filter *Controller.java -Recurse -ErrorAction SilentlyContinue | Sort-Object FullName
foreach ($file in $controllerFiles) {
    $content = Get-Content -Raw $file.FullName
    $className = if ($content -match "(?m)class\s+([A-Za-z0-9_]+)") { $matches[1] } else { $file.BaseName }
    $basePattern = '@RequestMapping\((?:value\s*=\s*)?"([^"]+)"'
    $base = if ($content -match $basePattern) { $matches[1] } else { "" }
    $lines.Add("- " + (Get-RelativePath $file.FullName) + " ($className), base $base")
    $mappingPattern = '@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|RequestMapping)\(([^)]*)\)'
    $matches = [regex]::Matches($content, $mappingPattern)
    foreach ($m in $matches) {
        $method = $m.Groups[1].Value.Replace("Mapping", "").ToUpperInvariant()
        if ($method -eq "REQUEST") { $method = "ANY" }
        $raw = $m.Groups[2].Value
        $quotedPath = [regex]::Match($raw, '"([^"]*)"')
        $path = if ($quotedPath.Success) { $quotedPath.Groups[1].Value } else { "" }
        $lines.Add("  - $method $base$path")
    }
}

Add-Section $lines "Tablas, indices y migraciones"
$migrationFiles = Get-ChildItem -Path . -Filter "V*.sql" -Recurse | Sort-Object FullName
foreach ($file in $migrationFiles) {
    $content = Get-Content -Raw $file.FullName
    $objects = [System.Collections.Generic.List[string]]::new()
    foreach ($m in [regex]::Matches($content, '(?im)^\s*CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([a-zA-Z0-9_.]+)')) {
        $objects.Add("table $($m.Groups[1].Value)")
    }
    foreach ($m in [regex]::Matches($content, '(?im)^\s*CREATE\s+(?:UNIQUE\s+)?INDEX\s+(?:IF\s+NOT\s+EXISTS\s+)?([a-zA-Z0-9_.]+)')) {
        $objects.Add("index $($m.Groups[1].Value)")
    }
    foreach ($m in [regex]::Matches($content, '(?im)^\s*ALTER\s+TABLE\s+([a-zA-Z0-9_.]+)')) {
        $objects.Add("alter $($m.Groups[1].Value)")
    }
    if ($objects.Count -eq 0) { $objects.Add("sin objetos detectados por regex") }
    $lines.Add("- " + (Get-RelativePath $file.FullName) + ": $($objects -join "; ")")
}

Add-Section $lines "Pruebas"
$testFiles = Get-ChildItem -Path src, finanzas-backend/src -Filter *Test.java -Recurse -ErrorAction SilentlyContinue | Sort-Object FullName
foreach ($file in $testFiles) {
    $content = Get-Content -Raw $file.FullName
    $className = if ($content -match "(?m)class\s+([A-Za-z0-9_]+)") { $matches[1] } else { $file.BaseName }
    $testCount = ([regex]::Matches($content, "@Test")).Count
    $lines.Add("- " + (Get-RelativePath $file.FullName) + ": $className ($testCount tests anotados)")
}

Add-Section $lines "Contratos OpenAPI"
$openApiFiles = Get-ChildItem -Path . -Include *.yaml,*.yml -Recurse |
    Where-Object { $_.FullName -match "openapi" } |
    Sort-Object FullName
foreach ($file in $openApiFiles) {
    $lines.Add("- " + (Get-RelativePath $file.FullName))
}

$outputFullPath = Join-Path $root $OutputPath
New-Item -ItemType Directory -Force (Split-Path -Parent $outputFullPath) | Out-Null
$lines | Set-Content -Path $outputFullPath -Encoding UTF8
Write-Host "Inventory written to $OutputPath"
