[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Directorio)
$ErrorActionPreference='Stop'
$cfg=Get-Content -LiteralPath (Join-Path $Directorio 'ejecucion.json') -Raw|ConvertFrom-Json
$m=@(Import-Csv -LiteralPath (Join-Path $Directorio 'e5.jtl'))
$columnas='timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect,e5Hilo,e5Iteracion,e5TipoFallo,e5ContenidoValido,e5PreInicioEpochMs,e5PreFinEpochMs'.Split(',')
if($m.Count -ne $cfg.solicitudesEsperadas -or -not $m.Count){throw 'Numero de muestras E5 incompleto.'}
if(Compare-Object $columnas @($m[0].PSObject.Properties.Name) -CaseSensitive){throw 'Encabezado JTL E5 inesperado.'}
if(@($m|Where-Object{$_.label -cne 'E5 POST /api/auth/login' -or $_.URL -cne 'http://localhost:18080/api/auth/login' -or $_.e5TipoFallo -notin @('ninguno','HTTP','Transporte','Contenido')}).Count){throw 'Muestra o diagnostico E5 inesperado.'}
$log=Get-Content -LiteralPath (Join-Path $Directorio 'jmeter.log') -Raw
if($log -match '(?m)^\d{4}-\d\d-\d\d \d\d:\d\d:\d\d,\d{3} ERROR '){throw 'JMeter registro ERROR; revisar antes de avanzar.'}
$hilos=@($m|Group-Object e5Hilo)
if($hilos.Count -ne $cfg.usuarios){throw 'Cantidad de identidades/hilos E5 inesperada.'}
foreach($g in $hilos){
    $iteraciones=@($g.Group|ForEach-Object{[int]$_.e5Iteracion}|Sort-Object)
    if($g.Count -ne $cfg.loops -or [int]$g.Name -lt 1 -or [int]$g.Name -gt $cfg.usuarios){throw 'Asignacion E5 por hilo incompleta.'}
    for($i=0;$i -lt $cfg.loops;$i++){if($iteraciones[$i] -ne $i+1){throw 'Iteraciones E5 duplicadas o ausentes.'}}
}
$inicio=($m|ForEach-Object{[long]$_.timeStamp}|Measure-Object -Minimum).Minimum
$fin=($m|ForEach-Object{[long]$_.timeStamp+[long]$_.elapsed}|Measure-Object -Maximum).Maximum
if($fin -le $inicio){throw 'Ventana HTTP E5 invalida.'}
$v=@($m|ForEach-Object{[double]$_.elapsed}|Sort-Object)
$eventos=@(foreach($r in $m){$a=[long]$r.timeStamp;$b=$a+[long]$r.elapsed;if($b -gt $a){[pscustomobject]@{t=$a;delta=1};[pscustomobject]@{t=$b;delta=-1}}})
$actual=0;$solapadas=0
foreach($e in ($eventos|Sort-Object t,delta)){$actual+=$e.delta;$solapadas=[math]::Max($solapadas,$actual)}
$distribucion=@($m|Group-Object{[math]::Floor(([long]$_.timeStamp-$inicio)/1000)}|ForEach-Object{[pscustomobject]@{segundo=[int]$_.Name;muestras=$_.Count}}|Sort-Object segundo)
$distribucion|Export-Csv (Join-Path $Directorio 'inicios.csv') -NoTypeInformation -Encoding UTF8
$porHilo=@(foreach($g in $hilos){$r=@($g.Group|Sort-Object{[long]$_.timeStamp});[pscustomobject]@{hilo=[int]$g.Name;
    muestras=$r.Count;primerInicioEpochMs=[long]$r[0].timeStamp;ultimoFinalEpochMs=($r|ForEach-Object{[long]$_.timeStamp+[long]$_.elapsed}|Measure-Object -Maximum).Maximum}})
$porHilo|Sort-Object hilo|Export-Csv (Join-Path $Directorio 'hilos.csv') -NoTypeInformation -Encoding UTF8
function Convertir-E5MemoriaMiB([string]$Valor){
    if($Valor -notmatch '^(\d+(?:\.\d+)?)(B|KiB|MiB|GiB|TiB)$'){throw 'Unidad de memoria no reconocida.'}
    $n=[double]::Parse($matches[1],[Globalization.CultureInfo]::InvariantCulture)
    $factor=@{B=1.0/1048576;KiB=1.0/1024;MiB=1.0;GiB=1024.0;TiB=1048576.0}
    $n*$factor[$matches[2]]
}
$rec=@(Import-Csv (Join-Path $Directorio 'recursos.csv'))
$recursos=@(foreach($servicio in @('backend-perf','db-perf')){foreach($fase in @('antes','HTTP','despues')){
    $r=@($rec|Where-Object{$_.servicio -eq $servicio}|Where-Object{
        if($fase -eq 'antes'){[long]$_.epochMs -lt $inicio}elseif($fase -eq 'despues'){[long]$_.epochMs -gt $fin}else{[long]$_.epochMs -ge $inicio -and [long]$_.epochMs -le $fin}})
    [pscustomobject]@{servicio=$servicio;fase=$fase;capturas=$r.Count;
        maxCpuMuestreada=($r|ForEach-Object{[double]::Parse($_.cpuPct,[Globalization.CultureInfo]::InvariantCulture)}|Measure-Object -Maximum).Maximum;
        maxMemoriaMiB=($r|ForEach-Object{Convertir-E5MemoriaMiB $_.memoriaUsada}|Measure-Object -Maximum).Maximum}
}})
$t=@($rec|Where-Object servicio -eq 'backend-perf'|ForEach-Object{[long]$_.epochMs});$intervalos=@()
for($i=1;$i -lt $t.Count;$i++){$intervalos+=($t[$i]-$t[$i-1])}
$resultado=[ordered]@{usuarios=$cfg.usuarios;muestras=$m.Count;inicioEpochMs=$inicio;finEpochMs=$fin;ventanaMs=($fin-$inicio);
    throughput=($m.Count*1000/($fin-$inicio));mediaMs=($v|Measure-Object -Average).Average;
    medianaMs=(($v[[int][math]::Floor(($v.Count-1)/2)]+$v[[int][math]::Floor($v.Count/2)])/2);
    p95Ms=$v[[math]::Ceiling(.95*$v.Count)-1];p99Ms=$v[[math]::Ceiling(.99*$v.Count)-1];maxMs=$v[-1];
    fallidas=@($m|Where-Object success -ne 'true').Count;porcentajeFallidas=(100.0*@($m|Where-Object success -ne 'true').Count/$m.Count);
    fallosHttp=@($m|Where-Object e5TipoFallo -eq 'HTTP').Count;fallosTransporte=@($m|Where-Object e5TipoFallo -eq 'Transporte').Count;
    fallosContenido=@($m|Where-Object e5TipoFallo -eq 'Contenido').Count;sobre2000Ms=@($m|Where-Object{[long]$_.elapsed -gt 2000}).Count;
    http200ValidoLento=@($m|Where-Object{$_.responseCode -eq '200' -and $_.e5ContenidoValido -eq 'true' -and [long]$_.elapsed -gt 2000}).Count;
    maxHilosActivos=($m|Measure-Object allThreads -Maximum).Maximum;maxMuestrasHttpEnCurso=$solapadas;
    separacionPrimerosHilosMs=(($porHilo|Measure-Object primerInicioEpochMs -Maximum).Maximum-($porHilo|Measure-Object primerInicioEpochMs -Minimum).Minimum);
    maxAsignacionMs=($m|ForEach-Object{[long]$_.e5PreFinEpochMs-[long]$_.e5PreInicioEpochMs}|Measure-Object -Maximum).Maximum;
    codigos=@($m|Group-Object responseCode|ForEach-Object{[pscustomobject]@{codigo=$_.Name;muestras=$_.Count}});
    recursos=$recursos;cadenciaMs=($intervalos|Measure-Object -Minimum -Maximum -Average);
    advertencia='Solapamiento de intervalos JMeter, no ejecucion simultanea demostrada en el servidor. Categorias de errores pueden solaparse.'}
$resultado|ConvertTo-Json -Depth 6|Set-Content (Join-Path $Directorio 'analisis.json') -Encoding UTF8
[pscustomobject]$resultado
