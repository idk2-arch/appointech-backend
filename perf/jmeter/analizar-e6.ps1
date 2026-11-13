[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Directorio)
$ErrorActionPreference='Stop'
$cfg=Get-Content "$Directorio/ejecucion.json" -Raw|ConvertFrom-Json
$m=@(Import-Csv "$Directorio/e6.jtl")
$columnas='timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect,e6Hilo,e6Iteracion,e6Paso,e6TipoFallo,e6ContenidoValido,e6Umbral'.Split(',')
if(-not $m.Count -or (Compare-Object $columnas @($m[0].PSObject.Properties.Name) -CaseSensitive)){throw 'E6: columnas JTL invalidas.'}
if($m.Count -ne $cfg.solicitudesEsperadas){throw 'E6: muestras incompletas; conservar JTL.'}
$rutas=@{'E6 GET notificaciones'='http://localhost:18080/api/notificaciones?page=0&size=20';'E6 GET historial'='http://localhost:18080/api/cliente/citas';'E6 GET especialidades'='http://localhost:18080/api/especialidades'}
foreach($r in $m){if(-not $rutas.ContainsKey($r.label) -or $r.URL -cne $rutas[$r.label] -or $r.e6TipoFallo -notin @('ninguno','HTTP','Transporte','Contenido')){throw 'E6: muestra ajena al GET esperado.'}}
$hilos=@($m|Group-Object e6Hilo)
if($hilos.Count -ne $cfg.usuarios){throw 'E6: identidades/hilos incompletos.'}
foreach($h in $hilos){
    if([int]$h.Name -lt 1 -or [int]$h.Name -gt $cfg.usuarios -or $h.Count -ne $cfg.loops*5){throw 'E6: asignacion por hilo incorrecta.'}
    $pares=@{}
    foreach($r in $h.Group){
        $key=$r.e6Iteracion+':'+$r.e6Paso
        $paso=[int]$r.e6Paso;$iter=[int]$r.e6Iteracion
        $label=@('E6 GET notificaciones','E6 GET historial','E6 GET especialidades','E6 GET notificaciones','E6 GET historial')
        if($pares.ContainsKey($key) -or $paso -lt 1 -or $paso -gt 5 -or $iter -lt 1 -or $iter -gt $cfg.loops -or $r.label -cne $label[$paso-1]){throw 'E6: mezcla/iteraciones incoherentes.'}
        if([int]$r.e6Umbral -ne $(if($paso -eq 3){300}else{800})){throw 'E6: umbral inesperado.'}
        $pares[$key]=$true
    }
}
if(-not(Test-Path "$Directorio/reporte/index.html")){throw 'E6: falta dashboard.'}
if((Get-Content "$Directorio/jmeter.log" -Raw) -match '(?m)^\d{4}-\d\d-\d\d \d\d:\d\d:\d\d,\d{3} ERROR '){throw 'E6: log contiene ERROR.'}
function Resumen-E6($Filas){
    $v=@($Filas|ForEach-Object{[double]$_.elapsed}|Sort-Object)
    $a=($Filas|ForEach-Object{[long]$_.timeStamp}|Measure-Object -Minimum).Minimum
    $b=($Filas|ForEach-Object{[long]$_.timeStamp+[long]$_.elapsed}|Measure-Object -Maximum).Maximum
    $f=@($Filas|Where-Object success -ne 'true').Count
    [pscustomobject]@{muestras=$v.Count;inicioEpochMs=$a;finEpochMs=$b;ventanaMs=$b-$a;throughput=$v.Count*1000/($b-$a);
        mediaMs=($v|Measure-Object -Average).Average;medianaMs=($v[[int][math]::Floor(($v.Count-1)/2)]+$v[[int][math]::Floor($v.Count/2)])/2;
        p90Ms=$v[[math]::Ceiling(.90*$v.Count)-1];p95Ms=$v[[math]::Ceiling(.95*$v.Count)-1];p99Ms=$v[[math]::Ceiling(.99*$v.Count)-1];maxMs=$v[-1];
        fallidas=$f;porcentajeFallidas=100*$f/$v.Count;fallosHTTP=@($Filas|Where-Object e6TipoFallo -eq 'HTTP').Count;
        fallosTransporte=@($Filas|Where-Object e6TipoFallo -eq 'Transporte').Count;fallosContenido=@($Filas|Where-Object e6TipoFallo -eq 'Contenido').Count;
        sobreUmbral=@($Filas|Where-Object{[long]$_.elapsed -gt [int]$_.e6Umbral}).Count;
        http200ValidoLento=@($Filas|Where-Object{$_.responseCode -eq '200' -and $_.e6ContenidoValido -eq 'true' -and [long]$_.elapsed -gt [int]$_.e6Umbral}).Count;
        codigos=@($Filas|Group-Object responseCode|ForEach-Object{@{codigo=$_.Name;muestras=$_.Count}})}
}
$res=Resumen-E6 $m
$porEndpoint=@(foreach($g in ($m|Group-Object label)){[pscustomobject]@{endpoint=$g.Name;resumen=Resumen-E6 $g.Group}})
$porEndpoint|ConvertTo-Json -Depth 7|Set-Content "$Directorio/por-endpoint.json" -Encoding UTF8
$inicios=@($m|Group-Object{[math]::Floor(([long]$_.timeStamp-$res.inicioEpochMs)/1000)}|ForEach-Object{[pscustomobject]@{segundo=[int]$_.Name;muestras=$_.Count}}|Sort-Object segundo)
$inicios|Export-Csv "$Directorio/inicios.csv" -NoTypeInformation -Encoding UTF8
$evol=@(foreach($g in ($m|Group-Object{[math]::Floor(([long]$_.timeStamp-$res.inicioEpochMs)/10000)}|Sort-Object{[int]$_.Name})){
    @{desdeSegundos=10*[int]$g.Name;resumen=Resumen-E6 $g.Group}
})
$evol|ConvertTo-Json -Depth 6|Set-Content "$Directorio/evolucion-10s.json" -Encoding UTF8
$primeros=@($m|Group-Object e6Hilo|ForEach-Object{[pscustomobject]@{hilo=[int]$_.Name;primerInicioEpochMs=($_.Group|ForEach-Object{[long]$_.timeStamp}|Measure-Object -Minimum).Minimum}}|Sort-Object primerInicioEpochMs)
$primeros|Export-Csv "$Directorio/primeros-inicios.csv" -NoTypeInformation -Encoding UTF8
$primeros|Group-Object{[math]::Floor(($_.primerInicioEpochMs-$res.inicioEpochMs)/1000)}|
    ForEach-Object{[pscustomobject]@{segundo=[int]$_.Name;hilosIniciados=$_.Count}}|Sort-Object segundo|
    Export-Csv "$Directorio/primeros-inicios-por-segundo.csv" -NoTypeInformation -Encoding UTF8
$eventos=@(foreach($r in $m){$a=[long]$r.timeStamp;$b=$a+[long]$r.elapsed;if($b -gt $a){[pscustomobject]@{t=$a;delta=1};[pscustomobject]@{t=$b;delta=-1}}})
$actual=0;$max=0;foreach($e in ($eventos|Sort-Object t,delta)){$actual+=$e.delta;$max=[math]::Max($max,$actual)}
$rec=@(Import-Csv "$Directorio/recursos.csv")
function Memoria-E6MiB([string]$Texto){
    if($Texto -notmatch '^(\d+(?:\.\d+)?)(B|KiB|MiB|GiB|TiB)$'){throw 'E6: unidad de memoria inesperada.'}
    $numero=[double]::Parse($matches[1],[Globalization.CultureInfo]::InvariantCulture)
    $factor=@{B=1.0/1048576;KiB=1.0/1024;MiB=1.0;GiB=1024.0;TiB=1048576.0}
    $numero*$factor[$matches[2]]
}
$recResumen=@(foreach($serv in @('backend-perf','db-perf')){
    $r=@($rec|Where-Object{$_.servicio -eq $serv -and [long]$_.epochMs -ge $res.inicioEpochMs -and [long]$_.epochMs -le $res.finEpochMs})
    [pscustomobject]@{servicio=$serv;capturasHTTP=$r.Count;maxCPUMuestreada=($r|ForEach-Object{[double]::Parse($_.cpuPct,[Globalization.CultureInfo]::InvariantCulture)}|Measure-Object -Maximum).Maximum;
        maxMemoriaMuestreadaMiB=($r|ForEach-Object{Memoria-E6MiB $_.memoriaUsada}|Measure-Object -Maximum).Maximum;
        memoriaObservada=@($r|Select-Object epochMs,memoriaUsada,memoriaLimite,memPct)}
})
$t=@($rec|Where-Object servicio -eq 'backend-perf'|ForEach-Object{[long]$_.epochMs});$gaps=@();for($i=1;$i -lt $t.Count;$i++){$gaps+=($t[$i]-$t[$i-1])}
$salida=[ordered]@{resumen=$res;fallosContenido=$res.fallosContenido;maxHilosActivos=($m|Measure-Object allThreads -Maximum).Maximum;maxHttpSolapadas=$max;
    separacionPrimerosHilosMs=$primeros[-1].primerInicioEpochMs-$primeros[0].primerInicioEpochMs;recursos=$recResumen;cadenciaMs=($gaps|Measure-Object -Minimum -Maximum -Average);
    advertencia='Categorias pueden solaparse; solapamiento JMeter no demuestra ejecucion simultanea en servidor. Memoria es del contenedor, no heap.'}
$salida|ConvertTo-Json -Depth 8|Set-Content "$Directorio/analisis.json" -Encoding UTF8
[pscustomobject]$salida
