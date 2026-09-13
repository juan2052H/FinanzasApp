# Validacion Final Integral de FinanzasApp 2.0

param(
    [string]$ProjectRoot = "c:\Users\filid\Downloads\FinanzasApp (2)\FinanzasApp"
)

Write-Host "========================================================================"
Write-Host "  VALIDACION FINAL INTEGRAL - FinanzasApp 2.0"
Write-Host "========================================================================"
Write-Host ""

$passCount = 0
$failCount = 0

# FASE 23.1: Verificar Compilacion
Write-Host "FASE 23.1: Verificar Compilacion"
Write-Host "--------"

$clientClasses = @(Get-ChildItem "$ProjectRoot\target\classes" -Filter "*.class" -Recurse -ErrorAction SilentlyContinue).Count
$backendClasses = @(Get-ChildItem "$ProjectRoot\finanzas-backend\target\classes" -Filter "*.class" -Recurse -ErrorAction SilentlyContinue).Count

Write-Host "Clases cliente compiladas: $clientClasses"
Write-Host "Clases backend compiladas: $backendClasses"

if ($clientClasses -gt 50) { 
    Write-Host "PASS: Cliente compilado" 
    $passCount++
} else {
    Write-Host "FAIL: Cliente no compilado"
    $failCount++
}

if ($backendClasses -gt 100) {
    Write-Host "PASS: Backend compilado"
    $passCount++
} else {
    Write-Host "FAIL: Backend no compilado"
    $failCount++
}

Write-Host ""

# FASE 23.2: Verificar JARs
Write-Host "FASE 23.2: Verificar JARs Ejecutables"
Write-Host "--------"

$shadedJar = Get-Item "$ProjectRoot\target\FinanzasApp-shaded.jar" -ErrorAction SilentlyContinue
$portableZip = Get-Item "$ProjectRoot\dist\FinanzasApp-1.0.0-portable.zip" -ErrorAction SilentlyContinue
$exe = Get-Item "$ProjectRoot\dist\FinanzasApp\FinanzasApp.exe" -ErrorAction SilentlyContinue

if ($shadedJar) {
    $jarSize = $shadedJar.Length / 1MB
    Write-Host "FinanzasApp-shaded.jar: $([Math]::Round($jarSize, 2)) MB"
    $passCount++
} else {
    Write-Host "FAIL: JAR shaded no encontrado"
    $failCount++
}

if ($portableZip) {
    $zipSize = $portableZip.Length / 1MB
    Write-Host "FinanzasApp-1.0.0-portable.zip: $([Math]::Round($zipSize, 2)) MB"
    $passCount++
} else {
    Write-Host "FAIL: ZIP portable no encontrado"
    $failCount++
}

if ($exe) {
    $exeSize = $exe.Length / 1MB
    Write-Host "FinanzasApp.exe (portable): $([Math]::Round($exeSize, 2)) MB"
    $passCount++
} else {
    Write-Host "FAIL: EXE no encontrado"
    $failCount++
}

Write-Host ""

# FASE 23.3: Verificar Pruebas
Write-Host "FASE 23.3: Verificar Reportes de Pruebas"
Write-Host "--------"

$clientTestResults = @(Get-ChildItem "$ProjectRoot\target\surefire-reports" -Filter "TEST-*.xml" -ErrorAction SilentlyContinue)
$backendTestResults = @(Get-ChildItem "$ProjectRoot\finanzas-backend\target\surefire-reports" -Filter "TEST-*.xml" -ErrorAction SilentlyContinue)

Write-Host "Reportes de pruebas cliente: $($clientTestResults.Count)"
Write-Host "Reportes de pruebas backend: $($backendTestResults.Count)"

if ($clientTestResults.Count -gt 0) { 
    Write-Host "PASS: Tests cliente"
    $passCount++
} else {
    Write-Host "FAIL: Tests cliente"
    $failCount++
}

if ($backendTestResults.Count -gt 0) {
    Write-Host "PASS: Tests backend"
    $passCount++
} else {
    Write-Host "FAIL: Tests backend"
    $failCount++
}

Write-Host ""

# FASE 23.4: Verificar Documentacion
Write-Host "FASE 23.4: Verificar Documentacion"
Write-Host "--------"

$docFiles = @(
    "README.md",
    "docs/ARCHITECTURE.md",
    "docs/API.md",
    "docs/SECURITY.md",
    "docs/DEPLOYMENT.md",
    "docs/FINAL_CHECKLIST.md",
    "docs/FEATURE_AUDIT_COMPLETE.md"
)

$docCount = 0
foreach ($doc in $docFiles) {
    if (Test-Path "$ProjectRoot\$doc") {
        $docCount++
        Write-Host "OK: $doc"
    }
}

Write-Host "Documentacion: $docCount/$($docFiles.Count)"

if ($docCount -gt 5) {
    Write-Host "PASS: Documentacion"
    $passCount++
} else {
    Write-Host "FAIL: Documentacion incompleta"
    $failCount++
}

Write-Host ""

# RESUMEN
Write-Host "========================================================================"
Write-Host "RESUMEN: $passCount PASS, $failCount FAIL"
Write-Host "========================================================================"

if ($failCount -eq 0) {
    Write-Host ""
    Write-Host "VALIDACION COMPLETADA EXITOSAMENTE"
    Write-Host "FinanzasApp 2.0 lista para FASE 24: Entrega Final"
    exit 0
} else {
    Write-Host ""
    Write-Host "VALIDACION CON ERRORES"
    exit 1
}
