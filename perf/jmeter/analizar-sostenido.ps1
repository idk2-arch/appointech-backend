# Analisis sin consultas al backend: mismas ventanas para HTTP, CPU y espera del pool.
param(
    [object[]]$Todas, [object[]]$Muestras, [object[]]$Pool,
    [string]$Result, [int]$DuracionSeg, [int]$CalentamientoSeg,
    [double]$Cpus, [double]$MaxErrores = 0.01
)
$ErrorActionPreference = 'Stop'
$marca = @($Todas | Where-Object label -eq 'setUp|inicio medicion')
if ($marca.Count -ne 1) { throw 'Falta la marca unica de inicio tras preparar todos los tokens.' }
$origen = [long]$marca[0].timeStamp + [long]$marca[0].elapsed
$cargas = @('x4','x6','x8')
function Calcular($filas) {
    $filas = @($filas)
    $orden = @($filas | ForEach-Object { [int]$_.elapsed } | Sort-Object)
    if (-not $orden.Count) { throw 'Una ventana de medicion no tiene peticiones.' }
    $fallos = @($filas | Where-Object success -eq 'false').Count
    [pscustomobject]@{n=$orden.Count; fallos=$fallos; p95=$orden[[math]::Ceiling(.95*$orden.Count)-1];
        p99=$orden[[math]::Ceiling(.99*$orden.Count)-1]; erroresPct=[math]::Round(100*$fallos/$orden.Count,3)}
}
function Limite($ruta) {
    if ($ruta -eq '/api/auth/login') { return 2000 }
    if ($ruta -eq '/api/especialidades' -or $ruta -like '*/no-leidas/conteo') { return 300 }
    if ($ruta -eq '/api/solicitudes' -or $ruta -eq '/api/citas/confirmar') { return 1500 }
    return 800
}
$resumen = @()
$detalle = @()
$ventanas = @()
for ($i=0; $i -lt $cargas.Count; $i++) {
    $carga = $cargas[$i]
    $inicioEtapa = $origen + $i*$DuracionSeg*1000
    $finEtapa = $inicioEtapa + $DuracionSeg*1000
    foreach ($fase in @('arranque','sostenido')) {
        $desde = if ($fase -eq 'arranque') { $inicioEtapa } else { $inicioEtapa+$CalentamientoSeg*1000 }
        $hasta = if ($fase -eq 'arranque') { $inicioEtapa+$CalentamientoSeg*1000 } else { $finEtapa }
        # Clasifica por inicio: incluye respuestas tardias para no censurar la cola de latencia.
        $filas = @($Todas | Where-Object {
            $_.label -like "$carga|*" -and [long]$_.timeStamp -ge $desde -and [long]$_.timeStamp -lt $hasta
        })
        $m = Calcular $filas
        $cpu = @($Muestras | Where-Object { $_.c -eq 'appointech-perf-backend-perf-1' -and $_.t -ge $desde -and $_.t -lt $hasta })
        $cpuDb = @($Muestras | Where-Object { $_.c -eq 'appointech-perf-db-perf-1' -and $_.t -ge $desde -and $_.t -lt $hasta })
        $espera = @($Pool | Where-Object { $_.t -ge $desde -and $_.t -lt $hasta })
        $logins = @($filas | Where-Object { ([uri]$_.URL).AbsolutePath -eq '/api/auth/login' })
        if (@($logins | Where-Object { $_.label -notlike '*|login|POST login' }).Count) { throw 'Hay logins dentro de un escenario de carga que debia reutilizar tokens.' }
        $resumen += [pscustomobject]@{carga=$carga; fase=$fase; n=$m.n; fallos=$m.fallos; p95=$m.p95; p99=$m.p99; erroresPct=$m.erroresPct
            cpuBackendMax=if ($cpu.Count) { [math]::Round(($cpu | Measure-Object cpu -Maximum).Maximum/$Cpus,1) } else { $null }
            cpuBaseMax=if ($cpuDb.Count) { [math]::Round(($cpuDb | Measure-Object cpu -Maximum).Maximum,1) } else { $null }
            esperandoMax=if ($espera.Count) { ($espera | Measure-Object esperando -Maximum).Maximum } else { $null }
            muestrasCpu=$cpu.Count; muestrasPool=$espera.Count; logins=$logins.Count; loginPct=[math]::Round(100*$logins.Count/$m.n,3)}
        $ventanas += [pscustomobject]@{carga=$carga; fase=$fase; desdeEpochMs=$desde; hastaEpochMs=$hasta}
        foreach ($grupo in ($filas | Group-Object { ([uri]$_.URL).AbsolutePath -replace '/tecnicos/\d+/','/tecnicos/{id}/' })) {
            $m = Calcular $grupo.Group
            $detalle += [pscustomobject]@{carga=$carga; fase=$fase; peticion=$grupo.Name; n=$m.n; fallos=$m.fallos;
                p95=$m.p95; p99=$m.p99; erroresPct=$m.erroresPct; umbralMs=(Limite $grupo.Name)}
        }
    }
}
$resumen | Export-Csv (Join-Path $Result 'fases.csv') -NoTypeInformation -Encoding UTF8
$detalle | Export-Csv (Join-Path $Result 'desglose-fases.csv') -NoTypeInformation -Encoding UTF8
$ventanas | Export-Csv (Join-Path $Result 'ventanas.csv') -NoTypeInformation -Encoding UTF8
$Pool | Export-Csv (Join-Path $Result 'pool.csv') -NoTypeInformation -Encoding UTF8
$sostenido = @($resumen | Where-Object fase -eq 'sostenido')
$ultimo = @($sostenido | Where-Object { $_.fallos/$_.n -lt $MaxErrores } | Select-Object -Last 1)
$fuera = @($Todas | Where-Object { $_.label -notlike 'setUp*' }).Count - ($resumen | Measure-Object n -Sum).Sum
@{resumen=$resumen; desglose=$detalle; ventanas=$ventanas; ultimoCumple=if ($ultimo.Count) { $ultimo[0].carga } else { $null };
    peticionesFueraDeVentanas=$fuera; calentamientoSeg=$CalentamientoSeg; duracionSeg=$DuracionSeg} |
    ConvertTo-Json -Depth 6 | Set-Content (Join-Path $Result 'sostenido.json') -Encoding UTF8
$md = @('# Regimen sostenido de JMeter','',
    "Tokens generados antes de medir. Rampa y arranque excluidos: $CalentamientoSeg s; sostenido: $($DuracionSeg-$CalentamientoSeg) s por escalon.",
    'Mismos limites: backend 2 CPU/1 GB, base 1 GB, pool 30. CPU backend relativa a 2 CPU; espera = maximo simultaneo observado en logs Hikari.',
    '', '| Carga | N | p95 ms | p99 ms | Errores % | CPU max backend % | Esperando max | Login % |',
    '|---|---:|---:|---:|---:|---:|---:|---:|')
foreach ($r in $sostenido) { $md += "| $($r.carga) | $($r.n) | $($r.p95) | $($r.p99) | $($r.erroresPct) | $($r.cpuBackendMax) | $($r.esperandoMax) | $($r.loginPct) |" }
$md += @('', '## Desglose sostenido por peticion', '', '| Peticion | Umbral ms | 4x N / fallos / p95 / p99 | 6x N / fallos / p95 / p99 | 8x N / fallos / p95 / p99 |',
    '|---|---:|---:|---:|---:|')
foreach ($ruta in @($detalle | Where-Object fase -eq 'sostenido' | Select-Object -ExpandProperty peticion -Unique | Sort-Object)) {
    $celdas = foreach ($carga in $cargas) {
        $r = @($detalle | Where-Object { $_.fase -eq 'sostenido' -and $_.carga -eq $carga -and $_.peticion -eq $ruta })
        if ($r.Count) { "$($r[0].n) / $($r[0].fallos) / $($r[0].p95) / $($r[0].p99)" } else { '0 / 0 / - / -' }
    }
    $md += "| $ruta | $(Limite $ruta) | $($celdas -join ' | ') |"
}
$md += @('', '## Arranque (reportado aparte)', '', '| Carga | N | p95 ms | p99 ms | Errores % | CPU backend % | Esperando max |','|---|---:|---:|---:|---:|---:|---:|')
foreach ($r in @($resumen | Where-Object fase -eq 'arranque')) { $md += "| $($r.carga) | $($r.n) | $($r.p95) | $($r.p99) | $($r.erroresPct) | $($r.cpuBackendMax) | $($r.esperandoMax) |" }
$md += @('', "Clasificacion por instante de inicio, incluyendo respuestas tardias. Peticiones iniciadas fuera de las ventanas planificadas: $fuera. Ultimo escalon que cumple errores < 1%: $(if ($ultimo.Count) { $ultimo[0].carga } else { 'ninguno' }).")
$md | Set-Content (Join-Path $Result 'sostenido.md') -Encoding UTF8
$sostenido | ConvertTo-Json -Compress | Write-Output
Write-Output "REPORTE_SOSTENIDO=$Result"
if (@($sostenido | Where-Object { $_.fallos/$_.n -ge $MaxErrores }).Count) { throw 'Al menos un escalon sostenido no cumple errores < 1 %.' }
