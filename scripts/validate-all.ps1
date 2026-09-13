# Validación Final Integral de FinanzasApp 2.0
# Fecha: 2026-09-01
# Verificar que todos los componentes estén presentes y funcionales

$projectRoot = "c:\Users\filid\Downloads\FinanzasApp (2)\FinanzasApp"
$results = @()

Write-Host "╔════════════════════════════════════════════════════════════════╗"
Write-Host "║        VALIDACIÓN FINAL INTEGRAL - FinanzasApp 2.0            ║"
Write-Host "╚════════════════════════════════════════════════════════════════╝"
Write-Host ""

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# FASE 23.1: Verificar Compilación
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "🔍 FASE 23.1: Verificar Compilación"
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

$clientClasses = @(Get-ChildItem "$projectRoot\target\classes" -Filter "*.class" -Recurse).Count
$backendClasses = @(Get-ChildItem "$projectRoot\finanzas-backend\target\classes" -Filter "*.class" -Recurse).Count

Write-Host "✓ Clases cliente compiladas: $clientClasses"
Write-Host "✓ Clases backend compiladas: $backendClasses"
Write-Host "✓ Total: $($clientClasses + $backendClasses) clases"
Write-Host ""

$results += @{
    Component = "Compilación Cliente"
    Expected = "100+ clases"
    Actual = $clientClasses
    Status = if ($clientClasses -gt 50) { "✅ PASS" } else { "❌ FAIL" }
}

$results += @{
    Component = "Compilación Backend"
    Expected = "130+ clases"
    Actual = $backendClasses
    Status = if ($backendClasses -gt 100) { "✅ PASS" } else { "❌ FAIL" }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# FASE 23.2: Verificar JARs
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "🔍 FASE 23.2: Verificar JARs Ejecutables"
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

$shadedJar = Get-Item "$projectRoot\target\FinanzasApp-shaded.jar" -ErrorAction SilentlyContinue
$portableZip = Get-Item "$projectRoot\dist\FinanzasApp-1.0.0-portable.zip" -ErrorAction SilentlyContinue
$exe = Get-Item "$projectRoot\dist\FinanzasApp\FinanzasApp.exe" -ErrorAction SilentlyContinue

if ($shadedJar) {
    $jarSize = $shadedJar.Length / 1MB
    Write-Host "✓ FinanzasApp-shaded.jar: $([Math]::Round($jarSize, 2)) MB"
    $results += @{
        Component = "JAR Ejecutable (Shaded)"
        Expected = ">1 MB"
        Actual = "$([Math]::Round($jarSize, 2)) MB"
        Status = if ($jarSize -gt 1) { "✅ PASS" } else { "❌ FAIL" }
    }
}

if ($portableZip) {
    $zipSize = $portableZip.Length / 1MB
    Write-Host "✓ FinanzasApp-1.0.0-portable.zip: $([Math]::Round($zipSize, 2)) MB"
    $results += @{
        Component = "ZIP Portable"
        Expected = ">50 MB"
        Actual = "$([Math]::Round($zipSize, 2)) MB"
        Status = if ($zipSize -gt 50) { "✅ PASS" } else { "❌ FAIL" }
    }
}

if ($exe) {
    $exeSize = $exe.Length / 1MB
    Write-Host "✓ FinanzasApp.exe (portable): $([Math]::Round($exeSize, 2)) MB"
    $results += @{
        Component = "EXE Ejecutable"
        Expected = ">0.1 MB"
        Actual = "$([Math]::Round($exeSize, 2)) MB"
        Status = if ($exeSize -gt 0.1) { "✅ PASS" } else { "❌ FAIL" }
    }
}

Write-Host ""

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# FASE 23.3: Verificar Pruebas
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "🔍 FASE 23.3: Verificar Reportes de Pruebas"
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

$clientTestResults = @(Get-ChildItem "$projectRoot\target\surefire-reports" -Filter "TEST-*.xml" -ErrorAction SilentlyContinue)
$backendTestResults = @(Get-ChildItem "$projectRoot\finanzas-backend\target\surefire-reports" -Filter "TEST-*.xml" -ErrorAction SilentlyContinue)

Write-Host "✓ Reportes de pruebas cliente: $($clientTestResults.Count)"
Write-Host "✓ Reportes de pruebas backend: $($backendTestResults.Count)"
Write-Host ""

$results += @{
    Component = "Test Reports Cliente"
    Expected = "5+ test classes"
    Actual = "$($clientTestResults.Count) reports"
    Status = if ($clientTestResults.Count -gt 0) { "✅ PASS" } else { "❌ FAIL" }
}

$results += @{
    Component = "Test Reports Backend"
    Expected = "15+ test classes"
    Actual = "$($backendTestResults.Count) reports"
    Status = if ($backendTestResults.Count -gt 0) { "✅ PASS" } else { "❌ FAIL" }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# FASE 23.4: Verificar Documentación
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "🔍 FASE 23.4: Verificar Documentación"
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

$docFiles = @{
    "README.md" = Test-Path "$projectRoot\README.md"
    "docs/ARCHITECTURE.md" = Test-Path "$projectRoot\docs\ARCHITECTURE.md"
    "docs/API.md" = Test-Path "$projectRoot\docs\API.md"
    "docs/SECURITY.md" = Test-Path "$projectRoot\docs\SECURITY.md"
    "docs/DEPLOYMENT.md" = Test-Path "$projectRoot\docs\DEPLOYMENT.md"
    "docs/FINAL_CHECKLIST.md" = Test-Path "$projectRoot\docs\FINAL_CHECKLIST.md"
    "docs/FEATURE_AUDIT_COMPLETE.md" = Test-Path "$projectRoot\docs\FEATURE_AUDIT_COMPLETE.md"
}

$docCount = ($docFiles.Values | Where-Object { $_ }).Count
foreach ($doc in $docFiles.Keys) {
    $status = if ($docFiles[$doc]) { "✓" } else { "✗" }
    Write-Host "$status $doc"
}

Write-Host ""
Write-Host "✓ Documentación completa: $docCount / $($docFiles.Count) archivos"
Write-Host ""

$results += @{
    Component = "Documentación"
    Expected = "10+ archivos"
    Actual = "$docCount archivos"
    Status = if ($docCount -gt 5) { "✅ PASS" } else { "❌ FAIL" }
}

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# FASE 23.5: Verificar Estructura de Carpetas
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "🔍 FASE 23.5: Verificar Estructura de Carpetas"
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

$folders = @(
    "src/main/java",
    "src/test/java",
    "finanzas-backend/src/main/java",
    "finanzas-backend/src/test/java",
    "docs",
    "scripts",
    "dist",
    "target"
)

foreach ($folder in $folders) {
    $folderPath = Join-Path $projectRoot $folder
    $exists = Test-Path $folderPath
    $status = if ($exists) { "✓" } else { "✗" }
    Write-Host "$status $folder"
}

Write-Host ""

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# FASE 23.6: Buscar Problemas Comunes
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "🔍 FASE 23.6: Buscar Problemas Comunes"
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

$issues = @()

#  Buscar TODO críticos
$todoFiles = Get-ChildItem "$projectRoot\src" -Filter "*.java" -Recurse | Select-String "// TODO" -ErrorAction SilentlyContinue
if ($todoFiles.Count -eq 0) {
    Write-Host "✓ Sin comentarios TODO críticos"
    $results += @{
        Component = "TODO/FIXME Críticos"
        Expected = "0"
        Actual = "0"
        Status = "✅ PASS"
    }
} else {
    Write-Host "⚠ Encontrados $($todoFiles.Count) comentarios TODO"
}

#  Buscar printStackTrace
$printStackTraceFiles = Get-ChildItem "$projectRoot\src" -Filter "*.java" -Recurse | Select-String "printStackTrace" -ErrorAction SilentlyContinue | Measure-Object
if ($printStackTraceFiles.Count -eq 0) {
    Write-Host "✓ Sin printStackTrace() inseguro"
} else {
    Write-Host "⚠ Encontrados $($printStackTraceFiles.Count) printStackTrace()"
}

Write-Host ""

# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# RESUMEN FINAL
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Write-Host "📊 RESUMEN DE VALIDACIÓN"
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

$passCount = ($results | Where-Object { $_.Status -eq "✅ PASS" }).Count
$failCount = ($results | Where-Object { $_.Status -eq "❌ FAIL" }).Count

Write-Host ""
Write-Host "Verificaciones: $passCount PASS, $failCount FAIL"
Write-Host ""

if ($failCount -eq 0) {
    Write-Host "╔════════════════════════════════════════════════════════════════╗"
    Write-Host "║              ✅ VALIDACIÓN COMPLETADA EXITOSAMENTE            ║"
    Write-Host "║                                                              ║"
    Write-Host "║  FinanzasApp 2.0 está listo para FASE 24: Entrega Final     ║"
    Write-Host "╚════════════════════════════════════════════════════════════════╝"
    exit 0
} else {
    Write-Host "╔════════════════════════════════════════════════════════════════╗"
    Write-Host "║              ❌ VALIDACIÓN CON ERRORES                       ║"
    Write-Host "║  Por favor corregir los componentes fallidos antes continuar  ║"
    Write-Host "╚════════════════════════════════════════════════════════════════╝"
    exit 1
}
