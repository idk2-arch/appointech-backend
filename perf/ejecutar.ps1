# Ejecuta una prueba k6 (carga o estres) contra el entorno perf, muestreando docker stats y los logs del backend
# mientras corre, y deja en perf\resultados\ el JSON de k6, el resumen legible y el diagnostico de recursos.
#
# Uso:
#   $env:ADMIN_CONTRASENA = '<clave del administrador de la migracion V4>'
#   powershell -File perf\ejecutar.ps1 -Prueba carga
#   powershell -File perf\ejecutar.ps1 -Prueba estres
# Otras variables (USUARIOS, USUARIOS_BASE, ESCALON_SEGUNDOS, MESETA, UMBRAL_*...) se leen del entorno: ver perf\README.md
param(
    [ValidateSet('carga', 'estres')][string]$Prueba = 'carga',
    [string]$BaseUrl = 'http://localhost:18080',
    [int]$IntervaloMuestreoSeg = 2
)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$resultados = Join-Path $PSScriptRoot 'resultados'
$contenedorBackend = 'appointech-perf-backend-perf-1'
$contenedorBase = 'appointech-perf-db-perf-1'

if (-not $env:ADMIN_CONTRASENA) { throw 'Define $env:ADMIN_CONTRASENA (clave del administrador sembrado por V4). Ver perf\README.md' }
if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    $ruta = 'C:\Program Files\k6'
    if (Test-Path (Join-Path $ruta 'k6.exe')) { $env:Path += ";$ruta" } else { throw 'k6 no esta instalado: winget install k6 (y abre una terminal nueva)' }
}
foreach ($c in @($contenedorBackend, $contenedorBase)) {
    if ((docker ps --filter "name=$c" --format '{{.Names}}') -ne $c) { throw "El contenedor $c no esta corriendo: ejecuta primero perf\iniciar.ps1" }
}
New-Item -ItemType Directory -Force -Path $resultados | Out-Null
$marca = Get-Date -Format 'yyyyMMdd-HHmmss'
$csv = Join-Path $resultados "recursos-$Prueba-$marca.csv"
$env:BASE_URL = $BaseUrl
$env:RESULTADOS_DIR = 'perf/resultados'

# Limites reales del backend, para expresar CPU y memoria como % del limite (docker stats muestra CPU sobre 1 nucleo)
$nanoCpus = [double](docker inspect -f '{{.HostConfig.NanoCpus}}' $contenedorBackend)
$cpusLimite = if ($nanoCpus -gt 0) { $nanoCpus / 1e9 } else { 1 }

# Muestreo en segundo plano: una fila por contenedor cada N segundos
"epochMs;contenedor;cpuPct;memPct;memUso" | Set-Content -Path $csv -Encoding ascii
$muestreo = Start-Job -ArgumentList $csv, $IntervaloMuestreoSeg, $contenedorBackend, $contenedorBase -ScriptBlock {
    param($archivo, $intervalo, $a, $b)
    while ($true) {
        $ahora = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        $lineas = docker stats --no-stream --format '{{.Name}};{{.CPUPerc}};{{.MemPerc}};{{.MemUsage}}' $a $b
        foreach ($l in $lineas) { "$ahora;$l" | Add-Content -Path $archivo -Encoding ascii }
        Start-Sleep -Seconds $intervalo
    }
}
$inicio = [DateTimeOffset]::UtcNow
Write-Host "== k6 $Prueba contra $BaseUrl (CPU del backend: $cpusLimite nucleos) =="
Push-Location $raiz
$ErrorActionPreference = 'Continue'
try { k6 run --quiet "perf/k6/$Prueba.js" 2>&1 | ForEach-Object { "$_" }; $codigoK6 = $LASTEXITCODE } finally { Pop-Location; Stop-Job $muestreo; Remove-Job $muestreo -Force }
$fin = [DateTimeOffset]::UtcNow
$ErrorActionPreference = 'Stop'
Write-Host "k6 termino con codigo $codigoK6 (0 = umbrales cumplidos, 99 = algun umbral incumplido)."

# ---------- Diagnostico de recursos ----------
function ConvertirPct([string]$texto) { return [double](($texto -replace '%', '').Trim().Replace(',', '.')) }
$filas = Import-Csv -Path $csv -Delimiter ';' | ForEach-Object {
    [pscustomobject]@{ t = [int64]$_.epochMs; c = $_.contenedor; cpu = (ConvertirPct $_.cpuPct); mem = (ConvertirPct $_.memPct) }
}
$logs = docker logs --timestamps --since $inicio.UtcDateTime.ToString('yyyy-MM-ddTHH:mm:ssZ') $contenedorBackend 2>&1 | ForEach-Object { "$_" }
$pool = foreach ($linea in $logs) {
    # Hikari (DEBUG): "stats (total=10/10, idle=2/10, active=8, waiting=0)" y, al agotarse, "(total=10, active=10, idle=0, waiting=49)"
    $m = [regex]::Match($linea, '^(\S+)\s.*(?:stats \(total=\d+/\d+, idle=\d+/\d+, active=(?<a>\d+), waiting=(?<w>\d+)\)|\(total=\d+, active=(?<a2>\d+), idle=\d+, waiting=(?<w2>\d+)\))')
    if ($m.Success) {
        $ts = $m.Groups[1].Value -replace '(\.\d{3})\d*Z$', '$1Z'
        $activas = if ($m.Groups['a'].Success) { $m.Groups['a'].Value } else { $m.Groups['a2'].Value }
        $esperando = if ($m.Groups['w'].Success) { $m.Groups['w'].Value } else { $m.Groups['w2'].Value }
        [pscustomobject]@{ t = [DateTimeOffset]::Parse($ts).ToUnixTimeMilliseconds(); activas = [int]$activas; esperando = [int]$esperando }
    }
}
$sinConexion = @($logs | Where-Object { $_ -match 'Connection is not available|request timed out' }).Count
$lentos = @($logs | Where-Object { $_ -match '\[LENTO\]' })
$topLentos = $lentos | ForEach-Object { if ($_ -match '\[LENTO\] (\S+)') { $Matches[1] } } | Group-Object | Sort-Object Count -Descending | Select-Object -First 8

# Ventanas: en el estres, una por escalon (del JSON de k6); en la carga, toda la prueba
$ventanas = @()
$jsonK6 = Join-Path $resultados "$Prueba-ultimo.json"
if ($Prueba -eq 'estres' -and (Test-Path $jsonK6)) {
    $k = Get-Content $jsonK6 -Raw | ConvertFrom-Json
    foreach ($e in $k.escalones) {
        $ventanas += [pscustomobject]@{ nombre = $e.nombre; vus = $e.vus; desde = [int64]$k.inicioEpochMs + 1000 * $e.desdeSeg; hasta = [int64]$k.inicioEpochMs + 1000 * $e.hastaSeg }
    }
} else {
    $ventanas += [pscustomobject]@{ nombre = 'toda la prueba'; vus = '-'; desde = $inicio.ToUnixTimeMilliseconds(); hasta = $fin.ToUnixTimeMilliseconds() }
}

$reporte = New-Object System.Collections.Generic.List[string]
$reporte.Add("# Diagnostico de recursos ($Prueba, $marca)")
$reporte.Add('')
$reporte.Add("Limites del backend: $cpusLimite nucleo(s). 'CPU backend' = % del limite (100 % = todos los nucleos asignados); la base no tiene limite de CPU: su % es sobre 1 nucleo.")
$reporte.Add('')
$reporte.Add('| ventana | usuarios | CPU backend max | memoria backend max | CPU base max | conexiones activas max / esperando max (pool) | recurso agotado |')
$reporte.Add('| --- | --- | --- | --- | --- | --- | --- |')
foreach ($v in $ventanas) {
    $b = $filas | Where-Object { $_.c -eq $contenedorBackend -and $_.t -ge $v.desde -and $_.t -le $v.hasta }
    $d = $filas | Where-Object { $_.c -eq $contenedorBase -and $_.t -ge $v.desde -and $_.t -le $v.hasta }
    $p = $pool | Where-Object { $_.t -ge $v.desde -and $_.t -le $v.hasta }
    $cpuB = if ($b) { [math]::Min(100, [math]::Round((($b | Measure-Object cpu -Maximum).Maximum) / $cpusLimite, 0)) } else { $null }
    $memB = if ($b) { [math]::Round(($b | Measure-Object mem -Maximum).Maximum, 0) } else { $null }
    $cpuD = if ($d) { [math]::Round(($d | Measure-Object cpu -Maximum).Maximum, 0) } else { $null }
    $activas = if ($p) { ($p | Measure-Object activas -Maximum).Maximum } else { $null }
    $esperando = if ($p) { ($p | Measure-Object esperando -Maximum).Maximum } else { $null }
    $agotados = @()
    if ($null -ne $cpuB -and $cpuB -ge 90) { $agotados += 'CPU del backend' }
    if ($null -ne $memB -and $memB -ge 90) { $agotados += 'memoria del backend' }
    if ($null -ne $esperando -and $esperando -gt 0) { $agotados += 'pool de conexiones' }
    if ($null -ne $cpuD -and $cpuD -ge 90) { $agotados += 'CPU de la base' }
    $texto = if ($agotados) { $agotados -join ', ' } else { 'ninguno al limite' }
    $reporte.Add("| $($v.nombre) | $($v.vus) | $cpuB % | $memB % | $cpuD % | $activas / $esperando | $texto |")
}
$reporte.Add('')
$reporte.Add("- Errores de conexion al pool en logs (Connection is not available / request timed out): $sinConexion")
$reporte.Add("- Metodos marcados [LENTO] en logs: $($lentos.Count)")
foreach ($g in $topLentos) { $reporte.Add("  - $($g.Name): $($g.Count) veces") }
$reporte.Add('- El pool solo deja rastro en los logs cuando se acerca a su limite o cada 30 s (DEBUG): una celda vacia significa sin muestra, no cero.')
$salida = Join-Path $resultados "recursos-$Prueba-$marca.md"
$reporte | Set-Content -Path $salida -Encoding utf8
Copy-Item $salida (Join-Path $resultados "recursos-$Prueba-ultimo.md") -Force
$reporte | ForEach-Object { Write-Host $_ }
Write-Host "Archivos en perf\resultados\: $Prueba-ultimo.json, $Prueba-ultimo.md, recursos-$Prueba-ultimo.md y el CSV de docker stats."
exit 0

