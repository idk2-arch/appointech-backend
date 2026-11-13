[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$JMeterBat,
    [ValidateSet(10,20)][int]$Usuarios=10,[ValidateSet(5,30)][int]$Rampa=30,[ValidateSet(5)][int]$Loops=5)
$ErrorActionPreference='Stop'
$perf=Split-Path -Parent $PSScriptRoot
$plan=Join-Path $PSScriptRoot 'e4-flujo.jmx'
$soporte=Join-Path $PSScriptRoot 'e4-soporte.groovy'
$credenciales=Join-Path $PSScriptRoot 'clientes.csv'
foreach($f in @($plan,$soporte,$credenciales,$JMeterBat)){if(-not(Test-Path -LiteralPath $f -PathType Leaf)){throw 'Falta una dependencia de E4.'}}
if([IO.Path]::GetFileName($JMeterBat) -ne 'jmeter.bat'){throw 'Indique jmeter.bat.'}
$jar=Join-Path (Split-Path -Parent (Resolve-Path -LiteralPath $JMeterBat).ProviderPath) 'ApacheJMeter.jar'
$java=(Get-Command java.exe -CommandType Application|Select-Object -First 1).Source
if(-not(Test-Path $jar)){throw 'No se encuentra ApacheJMeter.jar.'}
[xml]$xml=Get-Content -LiteralPath $plan -Raw
. (Join-Path $PSScriptRoot 'preparar-e2.ps1')
. (Join-Path $PSScriptRoot 'recursos-perf.ps1')
. (Join-Path $PSScriptRoot 'e4-preparacion.ps1')
$compose=@('compose','--env-file',(Join-Path $perf '.env.perf'),'-f',(Join-Path $perf 'docker-compose.perf.yml'))
$runId=[Guid]::NewGuid().ToString('N');$marcaPrefijo="E4-$runId-";$envPrefijo="APPOINTECH_E4_$($runId)_"
$directorio=Join-Path $perf ('resultados/e4-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+$runId)
New-Item -ItemType Directory -Path $directorio|Out-Null
Write-Host "Resultados E4: $directorio"
$variables=@();$muestreo=$null;$codigo=$null;$errorRecolector=$false;$fase='preparacion';$aislamiento=$null;$esperadosJson=$null
$reloj=[Diagnostics.Stopwatch]::StartNew()
try {
    $aislamiento=Confirmar-E4Entorno $compose 18080 $directorio
    $cuentas=@(Leer-E4Cuentas $compose $aislamiento $Usuarios)
    $especialidad=Leer-E4Especialidad $compose $aislamiento
    $csv=@(Import-Csv -LiteralPath $credenciales)
    $paquetes=@();$esperados=@()
    foreach($c in $cuentas){
        $cred=@($csv|Where-Object correo -ceq $c.correo)
        if($cred.Count -ne 1 -or [string]::IsNullOrWhiteSpace($cred[0].contrasena)){throw 'Credencial sintetica no encontrada o duplicada.'}
        $flujos=@(for($i=1;$i -le $Loops;$i++){
            $marca=$marcaPrefijo+'h'+([int]$c.ordinal).ToString('00')+'-i'+$i
            $esperados+=[ordered]@{marca=$marca;hilo=$c.ordinal;iteracion=$i;usuarioId=$c.usuarioId;clienteId=$c.clienteId;especialidadId=$especialidad.id;costo=$especialidad.costo}
            [ordered]@{marca=$marca;crear=[ordered]@{especialidadId=$especialidad.id;descripcionProblema=$marca;
                direccionServicio='Calle 100 # 10-20 Perf';latitud=4.3365;longitud=-74.3638}}
        })
        $paquetes+=[ordered]@{correo=$c.correo;contrasena=$cred[0].contrasena;especialidadId=$especialidad.id;
            especialidadNombre=$especialidad.nombre;flujos=$flujos}
    }
    if($esperados.Count -ne $Usuarios*$Loops){throw 'Cohorte E4 incompleta.'}
    $esperadosJson=ConvertTo-Json -InputObject @($esperados) -Depth 5 -Compress
    $esperadosJson|Set-Content (Join-Path $directorio 'expectativas.json') -Encoding UTF8
    $previas=@(Leer-E4Cohorte $compose $aislamiento $esperadosJson)
    $previas|ConvertTo-Json -Depth 5|Set-Content (Join-Path $directorio 'cohorte-antes.json') -Encoding UTF8
    if($previas.Count -ne $esperados.Count -or @($previas|Where-Object citas -ne 0).Count){throw 'Colisiones previas en las marcas E4.'}
    $antes=Leer-E4Estado $compose $aislamiento $marcaPrefijo
    $antes|ConvertTo-Json|Set-Content (Join-Path $directorio 'antes.json') -Encoding UTF8
    $especialidad|ConvertTo-Json|Set-Content (Join-Path $directorio 'especialidad.json') -Encoding UTF8
    [ordered]@{cuentas=$cuentas.Count;flujos=$esperados.Count;colisiones=0;duracionMs=$reloj.ElapsedMilliseconds;solicitudesHTTP=0}|
        ConvertTo-Json|Set-Content (Join-Path $directorio 'preparacion.json') -Encoding UTF8
    for($i=0;$i -lt $Usuarios;$i++){
        $nombre=$envPrefijo+($i+1);$valor=$paquetes[$i]|ConvertTo-Json -Depth 8 -Compress
        if($valor.Length -ge 32767){throw 'Paquete de entorno demasiado grande.'}
        $variables+=$nombre;[Environment]::SetEnvironmentVariable($nombre,$valor,'Process')
        if([Environment]::GetEnvironmentVariable($nombre,'Process') -cne $valor){throw 'Paquete de entorno truncado.'}
    }
    $tamano=1;foreach($entrada in [Environment]::GetEnvironmentVariables('Process').GetEnumerator()){$tamano+=$entrada.Key.Length+([string]$entrada.Value).Length+2}
    if($tamano -ge 32760){throw 'Bloque de entorno excede el limite conservador de Windows.'}
    $props=[ordered]@{host='localhost';puerto=18080;usuarios=$Usuarios;rampa=$Rampa;loops=$Loops;
        timeoutConexionMs=5000;timeoutRespuestaMs=10000;identidadesEnv=$envPrefijo;soporte=$soporte;
        preparacionGenerador=(Join-Path $directorio 'preparacion-generador.json');flujosSalida=(Join-Path $directorio 'flujos.json');
        sample_variables='e4Marca,e4Hilo,e4Iteracion,e4CitaId';'sampleresult.timestamp.start'='true'}
    $guardado=@('timestamp_format=ms','default_delimiter=,','print_field_names=true','time=true','label=true',
        'response_code=true','response_message=true','thread_name=true','successful=true','assertion_results_failure_message=true',
        'bytes=true','sent_bytes=true','thread_counts=true','url=true','latency=true','idle_time=true','connect_time=true',
        'output_format=csv','assertion_results=none','response_data=false','response_data.on_error=false','samplerData=false',
        'requestHeaders=false','responseHeaders=false','subresults=false','encoding=false','data_type=false','filename=false','hostname=false','sample_count=false')
    foreach($g in $guardado){$p=$g -split '=',2;$props['jmeter.save.saveservice.'+$p[0]]=$p[1]}
    [ordered]@{destino='http://localhost:18080';usuarios=$Usuarios;rampa=$Rampa;loops=$Loops;flujosEsperados=$esperados.Count;
        muestrasMaximas=5*$esperados.Count;umbrales=[ordered]@{login=2000;consulta=300;crear=1500;cancelar=1500;detalle=800};
        provisionales=@('cancelar','detalle');propiedades=$props;caracteresEntorno=$tamano;planSha256=(Get-FileHash $plan).Hash;soporteSha256=(Get-FileHash $soporte).Hash}|
        ConvertTo-Json -Depth 5|Set-Content (Join-Path $directorio 'ejecucion.json') -Encoding UTF8
    $argsJava=@('-Djava.awt.headless=true','-Djava.net.preferIPv4Stack=true','-Dhttp.nonProxyHosts=localhost|127.*','-jar',$jar,
        '-n','-t',$plan,'-l',(Join-Path $directorio 'e4.jtl'),'-j',(Join-Path $directorio 'jmeter.log'),'-e','-o',(Join-Path $directorio 'reporte'),
        '-Lorg.apache.http.wire=OFF','-Lorg.apache.http.headers=OFF','-Lorg.apache.jmeter.protocol.http=OFF')
    foreach($p in $props.GetEnumerator()){$argsJava+="-J$($p.Key)=$($p.Value)"}
    $muestreo=Iniciar-MuestreoPerf $aislamiento $directorio
    Esperar-MuestreoPerf $muestreo
    $fase='JMeter';$ErrorActionPreference='Continue'
    & $java @argsJava
    $codigo=$LASTEXITCODE
    $ErrorActionPreference='Stop';$fase='reconciliacion'
} catch {
    [ordered]@{fase=$fase;tipo=$_.Exception.GetType().FullName;mensaje='E4 detenido; consultar fase y artefactos, sin reintento automatico'}|
        ConvertTo-Json|Set-Content (Join-Path $directorio 'error.json') -Encoding UTF8
    throw
} finally {
    $ErrorActionPreference='Stop'
    foreach($nombre in $variables){[Environment]::SetEnvironmentVariable($nombre,$null,'Process')}
    if($muestreo){
        try{Detener-MuestreoPerf $muestreo|ConvertTo-Json|Set-Content (Join-Path $directorio 'validacion-recursos.json') -Encoding UTF8}
        catch{$errorRecolector=$true;[ordered]@{csvValido=$false;error='Fallo de cierre o validacion'}|ConvertTo-Json|Set-Content (Join-Path $directorio 'validacion-recursos.json') -Encoding UTF8}
    }
    [ordered]@{codigoJMeter=$codigo;errorRecolector=$errorRecolector;fase=$fase;
        variablesRestantes=@($variables|Where-Object{$null -ne [Environment]::GetEnvironmentVariable($_,'Process')}).Count}|
        ConvertTo-Json|Set-Content (Join-Path $directorio 'diagnostico.json') -Encoding UTF8
    if($aislamiento -and $esperadosJson){
        $consultas=0
        do{
            $filas=@(Leer-E4Cohorte $compose $aislamiento $esperadosJson);$consultas++
            $faltan=@($filas|Where-Object{$_.citas -eq 1 -and $_.estado -eq 'CANCELADA' -and ($_.notificacionRecibida -ne 1 -or $_.notificacionCancelada -ne 1)})
            if(-not $faltan.Count -or $consultas -ge 3){break}
            Start-Sleep -Seconds 1
        }while($true)
        ConvertTo-Json -InputObject @($filas) -Depth 5|Set-Content (Join-Path $directorio 'reconciliacion.json') -Encoding UTF8
        Leer-E4Estado $compose $aislamiento $marcaPrefijo|ConvertTo-Json|Set-Content (Join-Path $directorio 'despues.json') -Encoding UTF8
        [ordered]@{consultasCohorte=$consultas;solicitudesHTTP=0;esperaNotificacionesMaxSegundos=2}|
            ConvertTo-Json|Set-Content (Join-Path $directorio 'reconciliacion-control.json') -Encoding UTF8
    }
}
if($codigo -ne 0 -or $errorRecolector){throw 'Fallo de JMeter o recursos; conservar evidencias y no repetir.'}
$muestras=@(Import-Csv (Join-Path $directorio 'e4.jtl'))
if(-not $muestras.Count -or @($muestras|Where-Object label -notin @('E4 login','E4 consulta','E4 crear','E4 cancelar','E4 detalle')).Count){throw 'JTL con muestras inesperadas.'}
if(-not(Test-Path (Join-Path $directorio 'reporte/index.html')) -or -not(Test-Path (Join-Path $directorio 'flujos.json'))){throw 'Faltan dashboard o diagnostico de flujos.'}
$exposiciones=0;$archivos=@(Get-ChildItem $directorio -Recurse -File)
foreach($f in $archivos){$t=[IO.File]::ReadAllText($f.FullName)
    if($t -match 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|(?i)Authorization\s*:\s*Bearer' -or
        @($paquetes|Where-Object{$t.Contains($_.contrasena)}).Count){$exposiciones++}}
[ordered]@{muestras=$muestras.Count;archivosRevisados=$archivos.Count;exposiciones=$exposiciones}|
    ConvertTo-Json|Set-Content (Join-Path $directorio 'verificacion-artefactos.json') -Encoding UTF8
if($exposiciones){throw 'Se detecto exposicion sensible; no imprimir valores.'}
Write-Host "E4 finalizado. Muestras: $($muestras.Count). Revisar flujos e integridad en $directorio"
