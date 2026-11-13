# JMeter sin interfaz. Resultados nuevos por ejecucion; nunca borra mediciones anteriores.
param(
    [ValidateSet('carga', 'estres')][string]$Prueba = 'estres',
    [string]$AdminContrasena = $env:ADMIN_CONTRASENA,
    [string[]]$Propiedades = @(),
    [int]$UmbralLecturaMs = 800,
    [int]$UmbralLecturaSimpleMs = 300,
    [int]$UmbralLoginMs = 2000,
    [int]$UmbralEscrituraMs = 1500,
    [double]$MaxErrores = 0.01,
    [int]$UsuariosBase = 30,
    [int]$DuracionEscalonSeg = 90,
    [int]$RampaEscalonSeg = 30,
    [int]$CalentamientoSeg = 30,
    [int]$IntervaloMuestreoSeg = 2
)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$marca = Get-Date -Format 'yyyyMMdd-HHmmss'
$resultados = Join-Path $raiz "perf/resultados/realista-$Prueba-$marca"
$backend = 'appointech-perf-backend-perf-1'
$base = 'appointech-perf-db-perf-1'
if ($CalentamientoSeg -lt $RampaEscalonSeg -or $DuracionEscalonSeg -le $CalentamientoSeg) {
    throw 'El sostenido debe empezar despues de completar la rampa y tener duracion positiva.'
}
foreach ($p in ($Propiedades -split ',' | Where-Object { $_ })) {
    if ($p -match '^(usuarios_base|escalon_seg|rampa_escalon|tokens_n|tokens_csv)=') {
        throw 'Para ventanas reproducibles usa UsuariosBase, DuracionEscalonSeg y RampaEscalonSeg; tokens se calculan automaticamente.'
    }
}
if (-not $AdminContrasena) { throw 'Falta la credencial de administrador del entorno de pruebas.' }
$jmeter = Join-Path $env:LOCALAPPDATA 'Programs/JMeter/bin/jmeter.bat'
if (-not (Test-Path $jmeter)) { throw 'JMeter no esta instalado en la ruta esperada.' }
New-Item -ItemType Directory -Path $resultados | Out-Null
$jtl = Join-Path $resultados "$Prueba.jtl"
$csv = Join-Path $resultados 'recursos.csv'
$logJmeter = Join-Path $resultados 'jmeter.log'
'epochMs;contenedor;cpuPct;memPct' | Set-Content $csv -Encoding ascii
$ErrorActionPreference = 'Continue'
$nanoCpus = & docker inspect -f '{{.HostConfig.NanoCpus}}' $backend
if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar el limite de CPU.' }
$limiteCpus = [double]$nanoCpus / 1e9
if ($limiteCpus -le 0) { throw 'El backend debe tener un limite de CPU para comparar.' }
$muestreo = Start-Job -ArgumentList $csv,$IntervaloMuestreoSeg,$backend,$base -ScriptBlock {
    param($archivo,$intervalo,$a,$b)
    while ($true) {
        $ahora = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        docker stats --no-stream --format '{{.Name}};{{.CPUPerc}};{{.MemPerc}}' $a $b |
            ForEach-Object { "$ahora;$_" | Add-Content $archivo -Encoding ascii }
        Start-Sleep -Seconds $intervalo
    }
}
# La credencial no se pasa por -J ni se registra como propiedad de JMeter.
$anterior = $env:APPOINTECH_JMETER_ADMIN_PASSWORD
$env:APPOINTECH_JMETER_ADMIN_PASSWORD = $AdminContrasena
$tokensCsv = (Join-Path $raiz 'perf/jmeter/tokens.csv').Replace('\','/')
$argumentos = @('-n','-t',"perf/jmeter/appointech-$Prueba.jmx",'-l',$jtl,'-j',$logJmeter,
    '-e','-o',(Join-Path $resultados 'reporte'),
    "-Jumbral_lectura=$UmbralLecturaMs","-Jumbral_lectura_simple=$UmbralLecturaSimpleMs",
    "-Jumbral_login=$UmbralLoginMs","-Jumbral_escritura=$UmbralEscrituraMs",
    "-Jusuarios_base=$UsuariosBase","-Jescalon_seg=$DuracionEscalonSeg","-Jrampa_escalon=$RampaEscalonSeg",
    "-Jtokens_n=$($UsuariosBase*8)","-Jtokens_csv=$tokensCsv")
foreach ($p in ($Propiedades -split ',' | Where-Object { $_ })) { $argumentos += "-J$p" }
$inicio = [DateTimeOffset]::UtcNow
Push-Location $raiz
try {
    & $jmeter @argumentos 2>&1 | ForEach-Object { "$_" } |
        Where-Object { $_ -match 'summary =|Err:|Tidying|end of run|ERROR' } | ForEach-Object { Write-Host $_ }
    $codigoJmeter = $LASTEXITCODE
} finally {
    Pop-Location
    Stop-Job $muestreo
    Remove-Job $muestreo -Force
    $env:APPOINTECH_JMETER_ADMIN_PASSWORD = $anterior
}
if ($codigoJmeter -ne 0) { throw "JMeter termino con codigo $codigoJmeter" }
$ErrorActionPreference = 'Stop'
$todas = @(Import-Csv $jtl)
if (-not $todas.Count) { throw 'JMeter no produjo muestras.' }
if (@($todas | Where-Object { $_.label -like 'setUp*' -and $_.success -ne 'true' }).Count) { throw 'Fallo la preparacion del token de admin.' }
if (@(Import-Csv $tokensCsv).Count -ne $UsuariosBase*8) { throw 'Cantidad incorrecta de tokens de clientes preparados.' }
if (Select-String -Path $logJmeter -Pattern '\sERROR\s' -Quiet) { throw 'El plan contiene errores de ejecucion; no se considera una medicion valida.' }
$filas = @($todas | Where-Object { $_.label -notlike 'setUp*' })
function Metricas($grupo) {
    $grupo = @($grupo)
    $orden = @($grupo | ForEach-Object { [int]$_.elapsed } | Sort-Object)
    $fallos = @($grupo | Where-Object { $_.success -ne 'true' }).Count
    return [pscustomobject]@{
        peticiones=$grupo.Count; fallos=$fallos
        p95=$orden[[math]::Ceiling(0.95*$orden.Count)-1]
        p99=$orden[[math]::Ceiling(0.99*$orden.Count)-1]
        erroresPct=[math]::Round(100*$fallos/[math]::Max(1,$grupo.Count),3)
    }
}
$muestras = @(Import-Csv $csv -Delimiter ';' | ForEach-Object {
    [pscustomobject]@{t=[long]$_.epochMs; c=$_.contenedor; cpu=[double](($_.cpuPct -replace '%','').Replace(',','.'))}
})
$ErrorActionPreference = 'Continue'
$logs = @(& docker logs --timestamps --since $inicio.UtcDateTime.ToString('yyyy-MM-ddTHH:mm:ssZ') $backend 2>&1 | ForEach-Object { "$_" })
if ($LASTEXITCODE -ne 0) { throw 'No se pudieron consultar las muestras del pool.' }
$ErrorActionPreference = 'Stop'
$pool = @(foreach ($linea in $logs) {
    if ($linea -match '^(\S+)\s.*waiting=(\d+)') {
        [pscustomobject]@{t=[DateTimeOffset]::Parse(($Matches[1] -replace '(\.\d{3})\d*Z$','$1Z')).ToUnixTimeMilliseconds(); esperando=[int]$Matches[2]}
    }
})
$etapas = @($filas | Group-Object { if ($Prueba -eq 'estres') { ($_.label -split '\|')[0] } else { 'todo' } } |
    Sort-Object { [int]($_.Name -replace '\D','') })
$resumen = @(foreach ($etapa in $etapas) {
    $m = Metricas $etapa.Group
    $desde = ($etapa.Group | ForEach-Object { [long]$_.timeStamp } | Measure-Object -Minimum).Minimum
    $hasta = ($etapa.Group | ForEach-Object { [long]$_.timeStamp + [long]$_.elapsed } | Measure-Object -Maximum).Maximum
    $cpuB = @($muestras | Where-Object { $_.c -eq $backend -and $_.t -ge $desde -and $_.t -le $hasta })
    $cpuD = @($muestras | Where-Object { $_.c -eq $base -and $_.t -ge $desde -and $_.t -le $hasta })
    $espera = @($pool | Where-Object { $_.t -ge $desde -and $_.t -le $hasta })
    $iniciales = @($etapa.Group | Where-Object { $_.label -like '*|login sesion' }).Count
    $ocasionales = @($etapa.Group | Where-Object { $_.label -like '*|login ocasional' }).Count
    [pscustomobject]@{carga=$etapa.Name; peticiones=$m.peticiones; fallos=$m.fallos; p95=$m.p95; p99=$m.p99; erroresPct=$m.erroresPct
        cpuBackendMax=if ($cpuB.Count) { [math]::Round(($cpuB | Measure-Object cpu -Maximum).Maximum/$limiteCpus,1) } else { $null }
        cpuBaseMax=if ($cpuD.Count) { [math]::Round(($cpuD | Measure-Object cpu -Maximum).Maximum,1) } else { $null }
        esperandoMax=if ($espera.Count) { ($espera | Measure-Object esperando -Maximum).Maximum } else { $null }
        loginsIniciales=$iniciales; loginsOcasionales=$ocasionales; loginOcasionalPct=[math]::Round(100*$ocasionales/$m.peticiones,3)}
})
function Umbral($ruta) {
    if ($ruta -eq '/api/auth/login') { return $UmbralLoginMs }
    if ($ruta -eq '/api/especialidades' -or $ruta -like '*/no-leidas/conteo') { return $UmbralLecturaSimpleMs }
    if ($ruta -eq '/api/solicitudes' -or $ruta -eq '/api/citas/confirmar') { return $UmbralEscrituraMs }
    return $UmbralLecturaMs
}
$detalle = @(foreach ($etapa in $etapas) {
    foreach ($g in ($etapa.Group | Group-Object { ([uri]$_.URL).AbsolutePath -replace '/tecnicos/\d+/','/tecnicos/{id}/' })) {
        $m = Metricas $g.Group
        [pscustomobject]@{carga=$etapa.Name; peticion=$g.Name; umbralMs=(Umbral $g.Name); n=$m.peticiones; fallos=$m.fallos; p95=$m.p95; p99=$m.p99; erroresPct=$m.erroresPct}
    }
})
$resumen | Export-Csv (Join-Path $resultados 'resumen.csv') -NoTypeInformation -Encoding UTF8
$detalle | Export-Csv (Join-Path $resultados 'desglose.csv') -NoTypeInformation -Encoding UTF8
@{resumen=$resumen; desglose=$detalle; limiteErroresPct=$MaxErrores*100} | ConvertTo-Json -Depth 6 |
    Set-Content (Join-Path $resultados 'metricas.json') -Encoding UTF8
$md = @('| Carga | p95 ms | p99 ms | Errores % | CPU backend % | CPU base % | Esperando max | Login ocasional % |',
    '|---|---:|---:|---:|---:|---:|---:|---:|')
foreach ($r in $resumen) { $md += "| $($r.carga) | $($r.p95) | $($r.p99) | $($r.erroresPct) | $($r.cpuBackendMax) | $($r.cpuBaseMax) | $($r.esperandoMax) | $($r.loginOcasionalPct) |" }
$md += @('','CPU backend relativa al limite de 2 CPU; base relativa a un nucleo. Espera: maximo simultaneo observado en los logs del pool.', '',
    '| Carga | Peticion | N | Fallos | p95 ms | p99 ms | Errores % | Umbral ms |','|---|---|---:|---:|---:|---:|---:|---:|')
foreach ($r in $detalle) { $md += "| $($r.carga) | $($r.peticion) | $($r.n) | $($r.fallos) | $($r.p95) | $($r.p99) | $($r.erroresPct) | $($r.umbralMs) |" }
$md | Set-Content (Join-Path $resultados 'resumen.md') -Encoding UTF8
$resumen | ConvertTo-Json -Compress | Write-Output
Write-Output "RESULTADOS=$resultados"
if ($Prueba -eq 'estres') {
    & (Join-Path $PSScriptRoot 'analizar-sostenido.ps1') -Todas $todas -Muestras $muestras -Pool $pool -Result $resultados `
        -DuracionSeg $DuracionEscalonSeg -CalentamientoSeg $CalentamientoSeg -Cpus $limiteCpus -MaxErrores $MaxErrores
} elseif (@($resumen | Where-Object { $_.fallos/$_.peticiones -ge $MaxErrores }).Count) {
    throw 'La carga no cumple el limite de errores.'
}
