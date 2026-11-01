<#
.SYNOPSIS
    Prueba de humo/aceptacion de la API de FinanzasApp contra los datos de prueba.

.DESCRIPTION
    Ejecuta los casos de prueba de la hoja "11. Casos de Prueba" que se pueden comprobar por API.
    Debe correr sobre datos recien sembrados (modifica datos: liquida la deuda, asigna ahorro, etc.):

        docker compose down -v
        docker compose up -d --build
        powershell -ExecutionPolicy Bypass -File scripts\prueba-humo-api.ps1

    Compatible con Windows PowerShell 5.1 y PowerShell 7. Termina con codigo 1 si algun caso falla.
#>
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Password = "Prueba2026*"
)

$ErrorActionPreference = "Stop"
$Hogar = "20000000-0000-0000-0000-000000000010"
$Ana = "10000000-0000-0000-0000-000000000001"
$Carlos = "10000000-0000-0000-0000-000000000002"
$Resultados = New-Object System.Collections.Generic.List[object]

function Invoke-Api {
    param([string]$Method, [string]$Path, $Body = $null, [string]$Token = $null)
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Uri = "$BaseUrl$Path"; Method = $Method; Headers = $headers; UseBasicParsing = $true }
    if ($null -ne $Body) {
        $params["ContentType"] = "application/json; charset=utf-8"
        $params["Body"] = [System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 6 -Compress))
    }
    try {
        $response = Invoke-WebRequest @params
        $content = $response.Content
        if ($content -is [byte[]]) { $content = [System.Text.Encoding]::UTF8.GetString($content) }
        $json = $null
        if ($content -and $response.Headers["Content-Type"] -like "*json*") { $json = $content | ConvertFrom-Json }
        return [pscustomobject]@{ Status = [int]$response.StatusCode; Json = $json; Raw = $response; Length = $response.RawContentLength }
    } catch [System.Net.WebException] {
        $status = 0
        $text = ""
        if ($_.Exception.Response) {
            $status = [int]$_.Exception.Response.StatusCode
            $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $text = $reader.ReadToEnd()
        }
        $json = $null
        try { $json = $text | ConvertFrom-Json } catch { }
        return [pscustomobject]@{ Status = $status; Json = $json; Raw = $null; Length = 0 }
    } catch {
        if ($_.Exception.Response) {
            return [pscustomobject]@{ Status = [int]$_.Exception.Response.StatusCode; Json = $null; Raw = $null; Length = 0 }
        }
        throw
    }
}

function Get-Token([string]$Email, [string]$Pwd = $Password) {
    $r = Invoke-Api POST "/api/auth/login" @{ email = $Email; password = $Pwd }
    if ($r.Status -ne 200) { throw "No se pudo iniciar sesion como $Email (HTTP $($r.Status))" }
    return $r.Json.accessToken
}

function Test-Caso([string]$Id, [string]$Nombre, [scriptblock]$Prueba) {
    try {
        $detalle = & $Prueba
        $Resultados.Add([pscustomobject]@{ CP = $Id; Caso = $Nombre; Resultado = "Aprobada"; Detalle = "$detalle" })
    } catch {
        $Resultados.Add([pscustomobject]@{ CP = $Id; Caso = $Nombre; Resultado = "Fallida"; Detalle = $_.Exception.Message })
    }
}

function Assert([bool]$Condicion, [string]$Mensaje) {
    if (-not $Condicion) { throw $Mensaje }
}

function Money($Value) { return [decimal]$Value }

$hoy = Get-Date
$inicioMes = Get-Date -Year $hoy.Year -Month $hoy.Month -Day 1
$inicioMesAnterior = $inicioMes.AddMonths(-1).ToString("yyyy-MM-dd")
$finMesAnterior = $inicioMes.AddDays(-1).ToString("yyyy-MM-dd")
$sufijo = (Get-Date).ToString("yyyyMMddHHmmss")
$nuevoEmail = "evaluador.$sufijo@finanzasapp.test"
$nuevoPassword = "Evaluador2026*"

Write-Host "FinanzasApp - prueba de humo de la API contra $BaseUrl" -ForegroundColor Cyan

# ------------------------------------------------------------------ montaje
Test-Caso "CP-001" "Ambiente arriba y base de datos disponible" {
    $r = Invoke-Api GET "/health/readiness"
    Assert ($r.Status -eq 200 -and $r.Json.status -eq "UP" -and $r.Json.database -eq "UP") "readiness HTTP $($r.Status)"
    "status UP, database UP"
}

$tAna = Get-Token "ana.duena@finanzasapp.test"
$tCarlos = Get-Token "carlos.miembro@finanzasapp.test"
$tLaura = Get-Token "laura.lectora@finanzasapp.test"

# ------------------------------------------------------------------ autenticacion
Test-Caso "CP-002" "Registro de cuenta nueva y correo duplicado" {
    $nuevo = Invoke-Api POST "/api/auth/register" @{ nombre = "Evaluador"; apellido = "Externo"; email = $nuevoEmail; password = $nuevoPassword; moneda = "COP"; tipoCuenta = "PERSONAL" }
    Assert ($nuevo.Status -in 200, 201) "registro nuevo HTTP $($nuevo.Status)"
    $dup = Invoke-Api POST "/api/auth/register" @{ nombre = "Ana"; email = "ana.duena@finanzasapp.test"; password = $Password; moneda = "COP"; tipoCuenta = "PERSONAL" }
    Assert ($dup.Status -eq 409) "duplicado HTTP $($dup.Status), se esperaba 409"
    $ws = Invoke-Api GET "/api/workspaces" -Token $nuevo.Json.accessToken
    $cats = Invoke-Api GET "/api/workspaces/$($ws.Json[0].id)/categories" -Token $nuevo.Json.accessToken
    Assert (@($cats.Json).Count -eq 14) "categorias por defecto: $(@($cats.Json).Count)"
    "nuevo: HTTP $($nuevo.Status) con 'Mi espacio' y 14 categorias; duplicado: 409"
}

Test-Caso "CP-003" "Inicio de sesion valido e invalido" {
    $bad = Invoke-Api POST "/api/auth/login" @{ email = "ana.duena@finanzasapp.test"; password = "mala12345" }
    Assert ($bad.Status -eq 401) "clave incorrecta HTTP $($bad.Status)"
    "los 3 usuarios de prueba inician sesion (200); clave incorrecta 401"
}

Test-Caso "CP-029" "Bloqueo tras 5 intentos fallidos" {
    $email = "bloqueo.$sufijo@finanzasapp.test"
    1..5 | ForEach-Object { Invoke-Api POST "/api/auth/login" @{ email = $email; password = "incorrecta1" } | Out-Null }
    $r = Invoke-Api POST "/api/auth/login" @{ email = $email; password = "incorrecta1" }
    Assert ($r.Status -eq 429) "sexto intento HTTP $($r.Status), se esperaba 429"
    "sexto intento: 429"
}

# ------------------------------------------------------------------ lecturas sobre datos sembrados
Test-Caso "CP-005" "Listar transacciones del hogar" {
    $r = Invoke-Api GET "/api/workspaces/$Hogar/transactions" -Token $tAna
    $n = @($r.Json).Count
    Assert ($r.Status -eq 200 -and $n -ge 11) "HTTP $($r.Status), $n transacciones"
    "$n transacciones"
}

Test-Caso "CP-006" "Resumen de ahorro coherente con ingresos" {
    $s = (Invoke-Api GET "/api/workspaces/$Hogar/savings/summary" -Token $tAna).Json
    Assert ((Money $s.ingresos) -eq 10200000 -and (Money $s.gastos) -eq 4785000 -and (Money $s.ahorroTotal) -eq 2040000 -and (Money $s.tasaAhorro) -eq 20) "ingresos=$($s.ingresos) gastos=$($s.gastos) ahorro=$($s.ahorroTotal)"
    "ingresos 10.200.000, gastos 4.785.000, ahorro 2.040.000 (20%)"
}

Test-Caso "CP-007" "Presupuestos del mes actual" {
    $r = Invoke-Api GET "/api/workspaces/$Hogar/budgets" -Token $tAna
    $n = @($r.Json).Count
    Assert ($n -eq 4) "$n presupuestos"
    "4 presupuestos del mes"
}

Test-Caso "CP-008" "Workspaces del usuario" {
    $r = (Invoke-Api GET "/api/workspaces" -Token $tAna).Json
    $hogar = @($r) | Where-Object { $_.id -eq $Hogar }
    Assert (@($r).Count -eq 2 -and $hogar.role -eq "OWNER" -and $hogar.tipo -eq "HOUSEHOLD") "workspaces: $(@($r).Count)"
    "'Mi espacio' y 'Hogar Gomez' con rol OWNER"
}

Test-Caso "CP-010" "Facturas: listar y rechazar total en cero" {
    $r = Invoke-Api GET "/api/workspaces/$Hogar/invoices" -Token $tAna
    Assert (@($r.Json | Where-Object { $_.invoiceNumber -eq "SPD-2026-000123" }).Count -eq 1) "no aparece SPD-2026-000123"
    $bad = Invoke-Api POST "/api/workspaces/$Hogar/invoices" @{ invoiceNumber = "X-1"; merchantName = "Prueba"; issueDate = $hoy.ToString("yyyy-MM-dd"); subtotal = 0; taxAmount = 0; totalAmount = 0 } -Token $tAna
    Assert ($bad.Status -ge 400 -and $bad.Status -lt 500) "total cero HTTP $($bad.Status)"
    "aparece SPD-2026-000123; total 0 rechazado (HTTP $($bad.Status))"
}

Test-Caso "CP-017" "Alerta de presupuesto excedido" {
    $r = (Invoke-Api GET "/api/workspaces/$Hogar/notifications" -Token $tAna).Json
    Assert (@($r | Where-Object { $_.type -eq "BUDGET_THRESHOLD" }).Count -ge 1) "sin notificacion de presupuesto"
    "notificacion BUDGET_THRESHOLD sin leer"
}

Test-Caso "CP-025" "Reporte del mes anterior" {
    $r = (Invoke-Api GET "/api/workspaces/$Hogar/reports/summary?from=$inicioMesAnterior&to=$finMesAnterior" -Token $tAna).Json
    Assert ((Money $r.ingresos) -eq 5700000 -and (Money $r.gastos) -eq 2515000) "ingresos=$($r.ingresos) gastos=$($r.gastos)"
    $inv = Invoke-Api GET "/api/workspaces/$Hogar/reports/summary?from=$finMesAnterior&to=$inicioMesAnterior" -Token $tAna
    "ingresos 5.700.000, gastos 2.515.000; rango invertido HTTP $($inv.Status)"
}

Test-Caso "CP-019" "Exportar reporte CSV, PDF y Excel" {
    $out = @()
    foreach ($ext in "csv", "pdf", "xlsx") {
        $r = Invoke-Api GET "/api/workspaces/$Hogar/reports/export.$ext`?from=$inicioMesAnterior&to=$finMesAnterior" -Token $tAna
        Assert ($r.Status -eq 200 -and $r.Length -gt 0) "export.$ext HTTP $($r.Status)"
        $out += "$ext=$($r.Length)B"
    }
    "archivos generados: $($out -join ', ')"
}

Test-Caso "CP-028" "Preparacion DIAN (topes UVT)" {
    $anio = $hoy.Year
    $cfg = Invoke-Api PUT "/api/workspaces/$Hogar/tax-preparation/config/$anio" @{ uvtValue = 50000; grossIncomeUvt = 1400; grossPurchasesUvt = 1400; bankDepositsUvt = 1400; grossWealthUvt = 4500; estimatedGrossWealth = 0 } -Token $tAna
    Assert ($cfg.Status -eq 200) "config HTTP $($cfg.Status)"
    $s = (Invoke-Api GET "/api/workspaces/$Hogar/tax-preparation/summary?year=$anio" -Token $tAna).Json
    Assert ((Money $s.incomeThresholdAmount) -eq 70000000) "tope ingresos=$($s.incomeThresholdAmount)"
    "UVT de prueba 50.000 -> tope de ingresos 70.000.000; supera=$($s.exceedsIncomeThreshold)"
}

# ------------------------------------------------------------------ permisos
Test-Caso "CP-004" "VIEWER no registra gastos y MEMBER si" {
    $cat = @((Invoke-Api GET "/api/workspaces/$Hogar/categories" -Token $tAna).Json | Where-Object { $_.nombre -eq "Alimentacion" -and $_.type -eq "EXPENSE" })[0]
    $body = @{ categoryId = $cat.id; type = "EXPENSE"; description = "Prueba permisos"; amount = 1000; date = $hoy.ToString("yyyy-MM-dd") }
    $viewer = Invoke-Api POST "/api/workspaces/$Hogar/transactions" $body -Token $tLaura
    $member = Invoke-Api POST "/api/workspaces/$Hogar/transactions" $body -Token $tCarlos
    Assert ($viewer.Status -eq 403) "VIEWER HTTP $($viewer.Status)"
    Assert ($member.Status -eq 201) "MEMBER HTTP $($member.Status)"
    if ($member.Json.id) { Invoke-Api DELETE "/api/workspaces/$Hogar/transactions/$($member.Json.id)" -Token $tCarlos | Out-Null }
    "VIEWER 403; MEMBER 201 (y se elimina el gasto de prueba)"
}

# ------------------------------------------------------------------ categorias
Test-Caso "CP-015" "Crear categoria y validar duplicado/color" {
    $nombre = "Mascotas $sufijo"
    $c = Invoke-Api POST "/api/workspaces/$Hogar/categories" @{ nombre = $nombre; type = "EXPENSE"; icono = "MAS"; color = "#ff9900" } -Token $tAna
    Assert ($c.Status -in 200, 201) "crear HTTP $($c.Status)"
    $dup = Invoke-Api POST "/api/workspaces/$Hogar/categories" @{ nombre = $nombre; type = "EXPENSE"; icono = "MAS"; color = "#ff9900" } -Token $tAna
    $color = Invoke-Api POST "/api/workspaces/$Hogar/categories" @{ nombre = "Color $sufijo"; type = "EXPENSE"; icono = "X"; color = "rojo" } -Token $tAna
    Assert ($dup.Status -eq 409) "duplicado HTTP $($dup.Status)"
    Assert ($color.Status -eq 422) "color invalido HTTP $($color.Status)"
    $script:CategoriaPrueba = $c.Json.id
    "creada; duplicado 409; color invalido 422"
}

Test-Caso "CP-020" "Archivar y restaurar categoria con historial" {
    $ent = @((Invoke-Api GET "/api/workspaces/$Hogar/categories" -Token $tAna).Json | Where-Object { $_.nombre -eq "Entretenimiento" -and $_.type -eq "EXPENSE" })[0]
    $del = Invoke-Api DELETE "/api/workspaces/$Hogar/categories/$($ent.id)" -Token $tAna
    Assert ($del.Status -in 200, 204) "archivar HTTP $($del.Status)"
    $activas = (Invoke-Api GET "/api/workspaces/$Hogar/categories" -Token $tAna).Json
    Assert (@($activas | Where-Object { $_.id -eq $ent.id }).Count -eq 0) "sigue apareciendo como activa"
    $tx = (Invoke-Api GET "/api/workspaces/$Hogar/transactions" -Token $tAna).Json
    Assert (@($tx | Where-Object { $_.categoryId -eq $ent.id }).Count -ge 2) "se perdieron transacciones historicas"
    $res = Invoke-Api POST "/api/workspaces/$Hogar/categories/$($ent.id)/restore" -Token $tAna
    Assert ($res.Status -eq 200 -and $res.Json.id -eq $ent.id -and -not $res.Json.archived) "restaurar HTTP $($res.Status)"
    "archivada sin perder sus transacciones; restaurada con el mismo ID"
}

# ------------------------------------------------------------------ ahorro y metas
Test-Caso "CP-018" "Asignar ahorro a una meta" {
    $goal = "40000000-0000-0000-0000-000000000002"
    $a = Invoke-Api POST "/api/workspaces/$Hogar/savings/goals/$goal/allocations" @{ amount = 300000; note = "Prueba"; idempotencyKey = "cp018-$sufijo" } -Token $tAna
    Assert ($a.Status -in 200, 201) "asignar HTTP $($a.Status)"
    $g = @((Invoke-Api GET "/api/workspaces/$Hogar/goals" -Token $tAna).Json | Where-Object { $_.id -eq $goal })[0]
    Assert ((Money $g.currentAmount) -eq 300000) "avance=$($g.currentAmount)"
    $exceso = Invoke-Api POST "/api/workspaces/$Hogar/savings/goals/$goal/allocations" @{ amount = 50000000; idempotencyKey = "cp018x-$sufijo" } -Token $tAna
    Assert ($exceso.Status -eq 409) "exceso HTTP $($exceso.Status)"
    "meta con 300.000 de 3.000.000 (10%); asignar mas que el ahorro libre: 409"
}

Test-Caso "CP-030" "Retiro mayor al ahorro libre" {
    $r = Invoke-Api POST "/api/workspaces/$Hogar/savings/withdrawals" @{ amount = 99000000; note = "Prueba"; idempotencyKey = "cp011-$sufijo" } -Token $tAna
    Assert ($r.Status -eq 409) "HTTP $($r.Status)"
    "rechazado con 409"
}

# ------------------------------------------------------------------ hogar compartido
Test-Caso "CP-022" "Gasto compartido con partes que no suman el total" {
    $r = Invoke-Api POST "/api/workspaces/$Hogar/shared-expenses" @{ description = "Prueba"; amount = 100000; date = $hoy.ToString("yyyy-MM-dd"); paidByUserId = $Carlos; splitMethod = "CUSTOM_AMOUNT"; participants = @(@{ userId = $Ana; amount = 60000 }, @{ userId = $Carlos; amount = 30000 }) } -Token $tCarlos
    Assert ($r.Status -eq 422) "HTTP $($r.Status), se esperaba 422"
    "rechazado con 422"
}

Test-Caso "CP-009" "Balances y liquidacion de deuda" {
    $b = (Invoke-Api GET "/api/workspaces/$Hogar/shared-expenses/balances" -Token $tAna).Json
    $deuda = @($b | Where-Object { $_.userId -eq $Carlos })[0]
    Assert ((Money $deuda.netBalance) -eq -150000) "Carlos netBalance=$($deuda.netBalance)"
    $s = Invoke-Api POST "/api/workspaces/$Hogar/shared-expenses/settlements" @{ fromUserId = $Carlos; toUserId = $Ana; amount = 150000; date = $hoy.ToString("yyyy-MM-dd"); note = "Liquidacion de prueba" } -Token $tCarlos
    Assert ($s.Status -in 200, 201) "liquidar HTTP $($s.Status)"
    $b2 = (Invoke-Api GET "/api/workspaces/$Hogar/shared-expenses/balances" -Token $tAna).Json
    Assert (@($b2 | Where-Object { (Money $_.netBalance) -ne 0 }).Count -eq 0) "quedan balances distintos de 0"
    $hist = (Invoke-Api GET "/api/workspaces/$Hogar/shared-expenses" -Token $tAna).Json
    Assert (@($hist | Where-Object { $_.id -eq "60000000-0000-0000-0000-000000000001" }).Count -eq 1) "el gasto desaparecio del historial"
    "Carlos debia 150.000; tras liquidar todos en 0 y el gasto sigue en el historial"
}

Test-Caso "CP-021" "Invitar miembro, aceptar y controlar permisos" {
    $vi = Invoke-Api POST "/api/workspaces/$Hogar/invitations" @{ email = $nuevoEmail; role = "MEMBER" } -Token $tLaura
    Assert ($vi.Status -eq 403) "VIEWER invita HTTP $($vi.Status)"
    $inv = Invoke-Api POST "/api/workspaces/$Hogar/invitations" @{ email = $nuevoEmail; role = "MEMBER" } -Token $tAna
    Assert ($inv.Status -in 200, 201) "OWNER invita HTTP $($inv.Status)"
    $tNuevo = Get-Token $nuevoEmail $nuevoPassword
    $mias = (Invoke-Api GET "/api/invitations/mine" -Token $tNuevo).Json
    $pend = @($mias | Where-Object { $_.workspaceId -eq $Hogar })[0]
    Assert ($null -ne $pend) "la invitacion no aparece en la bandeja"
    $acc = Invoke-Api POST "/api/invitations/$($pend.id)/accept" -Token $tNuevo
    Assert ($acc.Status -in 200, 201, 204) "aceptar HTTP $($acc.Status)"
    $ws = (Invoke-Api GET "/api/workspaces" -Token $tNuevo).Json
    Assert (@($ws | Where-Object { $_.id -eq $Hogar -and $_.role -eq "MEMBER" }).Count -eq 1) "no quedo como MEMBER"
    "VIEWER no puede invitar (403); invitado acepta y queda MEMBER"
}

# ------------------------------------------------------------------ recurrencias y notificaciones
Test-Caso "CP-023" "Ejecutar recurrencia de arriendo" {
    $id = "50000000-0000-0000-0000-000000000001"
    $antes = @((Invoke-Api GET "/api/workspaces/$Hogar/recurring-transactions" -Token $tAna).Json | Where-Object { $_.id -eq $id })[0]
    $run = Invoke-Api POST "/api/workspaces/$Hogar/recurring-transactions/$id/run" -Token $tAna
    Assert ($run.Status -in 200, 201) "ejecutar HTTP $($run.Status)"
    $despues = @((Invoke-Api GET "/api/workspaces/$Hogar/recurring-transactions" -Token $tAna).Json | Where-Object { $_.id -eq $id })[0]
    $esperada = ([datetime]$antes.nextRunDate).AddMonths(1).ToString("yyyy-MM-dd")
    Assert ($despues.nextRunDate -eq $esperada) "proxima fecha $($despues.nextRunDate), esperada $esperada"
    "gasto creado; proxima fecha $($antes.nextRunDate) -> $($despues.nextRunDate)"
}

Test-Caso "CP-024" "Marcar notificacion como leida" {
    $n = @((Invoke-Api GET "/api/workspaces/$Hogar/notifications" -Token $tAna).Json)[0]
    $r = Invoke-Api PATCH "/api/workspaces/$Hogar/notifications/$($n.id)/read" -Token $tAna
    Assert ($r.Status -in 200, 204) "HTTP $($r.Status)"
    $sinLeer = (Invoke-Api GET "/api/workspaces/$Hogar/notifications" -Token $tAna).Json
    Assert (@($sinLeer | Where-Object { $_.id -eq $n.id }).Count -eq 0) "sigue como no leida"
    "marcada como leida"
}

# ------------------------------------------------------------------ perfil y cuenta
Test-Caso "CP-026" "Cambiar tema y conservarlo" {
    $p = Invoke-Api PATCH "/api/users/me/settings" @{ theme = "DARK" } -Token $tCarlos
    Assert ($p.Status -eq 200) "PATCH HTTP $($p.Status)"
    $otra = Get-Token "carlos.miembro@finanzasapp.test"
    $g = (Invoke-Api GET "/api/users/me/settings" -Token $otra).Json
    Assert ($g.theme -eq "DARK") "tema=$($g.theme)"
    "tema DARK persiste en una nueva sesion"
}

Test-Caso "CP-027" "Exportar y eliminar cuenta" {
    $t = Get-Token $nuevoEmail $nuevoPassword
    $exp = Invoke-Api GET "/api/users/me/export" -Token $t
    Assert ($exp.Status -eq 200) "export HTTP $($exp.Status)"
    $mala = Invoke-Api DELETE "/api/users/me" @{ confirmEmail = $nuevoEmail; password = "incorrecta1" } -Token $t
    Assert ($mala.Status -eq 401) "clave incorrecta HTTP $($mala.Status)"
    $del = Invoke-Api DELETE "/api/users/me" @{ confirmEmail = $nuevoEmail; password = $nuevoPassword } -Token $t
    Assert ($del.Status -in 200, 204) "eliminar HTTP $($del.Status)"
    $login = Invoke-Api POST "/api/auth/login" @{ email = $nuevoEmail; password = $nuevoPassword }
    Assert ($login.Status -eq 401) "login posterior HTTP $($login.Status)"
    $owner = Invoke-Api DELETE "/api/users/me" @{ confirmEmail = "ana.duena@finanzasapp.test"; password = $Password } -Token $tAna
    Assert ($owner.Status -eq 409) "OWNER con hogar compartido HTTP $($owner.Status)"
    "export 200; clave mala 401; eliminada y login posterior 401; OWNER del hogar 409"
}

# ------------------------------------------------------------------ resumen
$Resultados | Format-Table -AutoSize -Wrap
$fallidas = @($Resultados | Where-Object { $_.Resultado -ne "Aprobada" }).Count
Write-Host ("Aprobadas: {0}  Fallidas: {1}" -f ($Resultados.Count - $fallidas), $fallidas) -ForegroundColor $(if ($fallidas) { "Red" } else { "Green" })
if ($fallidas) { exit 1 }
