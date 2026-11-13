[CmdletBinding()]
param([string]$JMeterHome=(Join-Path $env:LOCALAPPDATA 'Programs/JMeter'))
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'e8-comun.ps1')
$out=Join-Path (Split-Path $PSScriptRoot) ('diagnosticos/e8-ficticio-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $out|Out-Null
$mutex=$null;$muestreo=$null;$etapa='estatica'
try{
    foreach($f in @('e8-comun.ps1','validar-e8.ps1','ejecutar-e8.ps1')){$errors=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $f),[ref]$null,[ref]$errors);if(@($errors).Count){throw ('Sintaxis: '+$f)}}
    & node.exe (Join-Path $PSScriptRoot 'generar-e8.js');if($LASTEXITCODE -ne 0){throw 'Generador E8 fallo'}
    foreach($f in @('e8-resistencia.jmx','e8-ficticio.jmx')){[xml]$x=Get-Content (Join-Path $PSScriptRoot $f) -Raw;if($x.SelectNodes('//HTTPSamplerProxy').Count -ne $(if($f -eq 'e8-ficticio.jmx'){0}else{1})){throw 'XML HTTP inesperado'};if($f -eq 'e8-resistencia.jmx' -and $x.SelectNodes('//HeaderManager').Count -ne 1){throw 'Falta HeaderManager real'}}
    $hashInicial=@(Hashes-E8)
    $java=Java-E7;$classes=Join-Path $out 'clases';New-Item -ItemType Directory $classes|Out-Null
    & $java -cp "$JMeterHome/lib/*;$JMeterHome/lib/ext/*" org.codehaus.groovy.tools.FileSystemCompiler -d $classes (Join-Path $PSScriptRoot 'e8-motor.groovy')
    if($LASTEXITCODE -ne 0){throw 'Compilacion Groovy fallo'}
    $mutex=New-Object Threading.Mutex($false,'LocalAppoinTechPerfE6Sostenido')
    $p=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process));ConvertTo-Json -InputObject $p -Depth 6|Set-Content "$out/procesos-previos.json" -Encoding UTF8
    if(-not $mutex.WaitOne(0) -or @($p|Where-Object bloquea).Count){throw 'Carga propia activa'}
    $etapa='reloj_y_respuestas_ficticias'
    & node.exe (Join-Path $PSScriptRoot 'crear-fixture-e8.js') "$out/paquete-ficticio.json";if($LASTEXITCODE -ne 0){throw 'Fixture fallo'}
    $argsJava=Argumentos-E8 $JMeterHome $out "$out/paquete-ficticio.json" -Ficticio
    try{$muestreo=Iniciar-E7Recursos ([pscustomobject]@{BackendId='ficticio';DbId='ficticio'}) $out -Ficticio;Esperar-MuestreoPerf $muestreo;Invocar-E7Java $java $argsJava $out 120}
    finally{if($muestreo){Start-Sleep -Milliseconds 200;Detener-E7Recursos $muestreo|ConvertTo-Json|Set-Content "$out/validacion-recursos.json" -Encoding UTF8;$muestreo=$null}}
    & node.exe (Join-Path $PSScriptRoot 'analizar-e8.js') $out;if($LASTEXITCODE -ne 0){throw 'Analisis ficticio fallo'}
    $analysis=Get-Content "$out/analisis-e8.json" -Raw|ConvertFrom-Json
    if(-not $analysis.coberturaRecursos.completa -or $analysis.medicion.total.muestras -lt 100 -or $analysis.medicion.total.fallidas -ne 0 -or @($analysis.desviosVentana).Count){throw 'Respuesta/recolector ficticio no cumple'}
    $etapa='negativos_y_limites'
    & node.exe (Join-Path $PSScriptRoot 'probar-analisis-e8.js') $out
    if($LASTEXITCODE -ne 0){throw 'Negativos del analizador fallaron'}
    $abort=Join-Path $out 'aborto-401';New-Item -ItemType Directory -Path $abort|Out-Null
    & node.exe (Join-Path $PSScriptRoot 'crear-fixture-e8.js') "$abort/paquete-ficticio.json"
    $aa=@(Argumentos-E8 $JMeterHome $abort "$abort/paquete-ficticio.json" -Ficticio)+@('-JfalloHTTP=401')
    Invocar-E7Java $java $aa $abort 60
    $ab=Get-Content "$abort/motor-final.json" -Raw|ConvertFrom-Json
    if(-not $ab.monitorCerrado -or $ab.medicionTTL){throw 'Parada 401 ficticia no cerro antes de medicion'}
    $etapa='HTTP_con_servidor_ficticio_local'
    [xml]$final=Get-Content (Join-Path $PSScriptRoot 'e8-resistencia.jmx') -Raw
    if($final.SelectNodes('//HTTPSamplerProxy/following-sibling::hashTree[1]/HeaderManager').Count -ne 1 -or $final.SelectNodes('//HTTPSamplerProxy/following-sibling::hashTree[1]/JSR223PreProcessor').Count -ne 1){throw 'Cabeceras/preprocesador fuera de alcance del GET'}
    $wireD=Join-Path $out 'servidor-local';New-Item -ItemType Directory -Path $wireD|Out-Null
    & node.exe (Join-Path $PSScriptRoot 'crear-fixture-e8.js') "$wireD/paquete-ficticio.json"
    $server=$null
    try{
        $server=Start-Process -FilePath (Get-Command node.exe -CommandType Application|Select-Object -First 1).Source -ArgumentList @(('"'+(Join-Path $PSScriptRoot 'servidor-ficticio-e8.js')+'"'),('"'+$wireD+'"')) -WindowStyle Hidden -PassThru -RedirectStandardOutput "$wireD/servidor-stdout.log" -RedirectStandardError "$wireD/servidor-stderr.log"
        $wait=[Diagnostics.Stopwatch]::StartNew()
        while(-not(Test-Path "$wireD/servidor-listo.json")){if($server.HasExited -or $wait.Elapsed.TotalSeconds -gt 10){throw 'Servidor ficticio no inicio'};Start-Sleep -Milliseconds 100}
        $local=Get-Content "$wireD/servidor-listo.json" -Raw|ConvertFrom-Json
        $wireArgs=@(Argumentos-E8 $JMeterHome $wireD "$wireD/paquete-ficticio.json")+@('-JvalidacionWire=true','-Jhost=127.0.0.1',"-Jpuerto=$($local.puerto)",'-Jrampa=1')
        $wireCapture=$null
        try{$wireCapture=Iniciar-E7Recursos ([pscustomobject]@{BackendId='ficticio';DbId='ficticio'}) $wireD -Ficticio;Esperar-MuestreoPerf $wireCapture;Invocar-E7Java $java $wireArgs $wireD 120}
        finally{if($wireCapture){Start-Sleep -Milliseconds 200;Detener-E7Recursos $wireCapture|ConvertTo-Json|Set-Content "$wireD/validacion-recursos.json" -Encoding UTF8}}
    }finally{if($server){New-Item -ItemType File -Path "$wireD/servidor.fin" -Force|Out-Null;if(-not $server.WaitForExit(5000)){$server.Kill();$server.WaitForExit();throw 'Servidor ficticio no cerro'};$server.Dispose()}}
    $wire=Get-Content "$wireD/servidor-resumen.json" -Raw|ConvertFrom-Json
    if(-not $wire.completa -or -not $wire.cerrado -or $wire.backendRealHTTP -ne 0){throw 'Cabecera HTTP real hacia servidor ficticio fallo'}
    & node.exe (Join-Path $PSScriptRoot 'analizar-e8.js') $wireD;if($LASTEXITCODE -ne 0){throw 'JTL wire no concilia'}
    $wireAnalysis=Get-Content "$wireD/analisis-e8.json" -Raw|ConvertFrom-Json
    if($wireAnalysis.HTTPtotal -ne $wire.HTTP -or $wireAnalysis.medicion.total.fallidas -ne 0 -or @($wireAnalysis.desviosVentana).Count){throw 'Validacion wire no cumple'}
    $p=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process));ConvertTo-Json -InputObject $p -Depth 6|Set-Content "$out/procesos-finales.json" -Encoding UTF8
    if(@($p|Where-Object bloquea).Count){throw 'Procesos propios sin cerrar'}
    foreach($h in @(Hashes-E8)){$inicial=@($hashInicial|Where-Object archivo -eq $h.archivo);if($inicial.Count -ne 1 -or $inicial[0].sha256 -cne $h.sha256){throw 'Fuente modificada durante validacion'}}
    $gate=@{completa=$true;HTTP=$wire.HTTP;HTTPBackend=0;HTTPServidorFicticio=$wire.HTTP;cabeceraEnviadaServidorFicticio=$true;identidadesWire=50;GETPublicoSinAuthorization=$wire.publicasSinAuthorization;sinBackend=$true;evidencias=$out;relojAcelerado=$true;medicionFicticiaSeg=20;noAcredita600sReales=$true;JMeterCerrado=$true;recolectorCerrado=$true;autenticacionRamaRealProbada=$true;parada401Probada=$true;hashes=$hashInicial}
    $gate|ConvertTo-Json -Depth 7|Set-Content "$out/validacion.json" -Encoding UTF8
    $gate|ConvertTo-Json -Depth 7|Set-Content (Join-Path $PSScriptRoot 'E8-VALIDACION-ULTIMA.json') -Encoding UTF8
    Write-Host "E8 ficticio completo: HTTP backend=0; HTTP servidor local=$($wire.HTTP); evidencias: $out"
}catch{
    Guardar-E6ErrorSeguro $_ $etapa "$out/error-seguro.jsonl"|Out-Null
    @{completa=$false;HTTP=0;evidencias=$out;etapa=$etapa}|ConvertTo-Json|Set-Content (Join-Path $PSScriptRoot 'E8-VALIDACION-ULTIMA.json') -Encoding UTF8
    throw "E8 ficticio fallo; no iniciar HTTP. Evidencias: $out"
}finally{if($mutex){$mutex.Dispose()}}
