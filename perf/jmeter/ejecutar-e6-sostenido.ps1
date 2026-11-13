[CmdletBinding()]
param([ValidateSet('100','200','300')][string]$Fase='100',[string]$JMeterBat,[switch]$DiagnosticoSinHttp)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'errores-e6.ps1')
. (Join-Path $PSScriptRoot 'procesos-e6.ps1')
$perf=Split-Path -Parent $PSScriptRoot
$prefijo=if($DiagnosticoSinHttp){'diagnosticos/e6-sostenido-local-'}else{'resultados/e6-sostenido-'}
$d=Join-Path $perf ($prefijo+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N')+'/fase-'+$Fase)
New-Item -ItemType Directory -Path $d|Out-Null
Write-Host "Evidencias E6: $d"
$a=$null;$datos=$null;$paquete=$null;$muestreo=$null;$codigo=$null;$lanzado=$false;$problemas=@();$fallo=$false
$setupValido=$false;$muestrasMedidas=0
$etapaPreparacion='dependencias'
$preparacion=[Diagnostics.Stopwatch]::StartNew()
try{
    Add-Type -AssemblyName System.Net.Http
    . (Join-Path $PSScriptRoot 'preparar-e2.ps1')
    . (Join-Path $PSScriptRoot 'preparar-e6.ps1')
    . (Join-Path $PSScriptRoot 'recursos-perf.ps1')
    $co=@('compose','--env-file',(Join-Path $perf '.env.perf'),'-f',(Join-Path $perf 'docker-compose.perf.yml'))
    $plan=Join-Path $PSScriptRoot 'e6-sostenido.jmx'
    $soporte=Join-Path $PSScriptRoot 'e6-lecturas.groovy'
    $node=(Get-Command node.exe -CommandType Application -ErrorAction Stop|Select-Object -First 1).Source
    foreach($extra in @('e6-reloj-sostenido.groovy','analizar-e6-sostenido.js')){if(-not(Test-Path (Join-Path $PSScriptRoot $extra))){throw 'E6: dependencia sostenida ausente.'}}
    if(-not $JMeterBat){$JMeterBat=(Get-Command jmeter.bat -CommandType Application -ErrorAction Stop|Select-Object -First 1).Source}
    if([IO.Path]::GetFileName($JMeterBat) -ne 'jmeter.bat'){throw 'E6: indicar jmeter.bat.'}
    $jar=Join-Path (Split-Path -Parent (Resolve-Path -LiteralPath $JMeterBat).ProviderPath) 'ApacheJMeter.jar'
    $java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}
    if(-not $java -or -not(Test-Path -LiteralPath $java)){$java=(Get-Command java.exe -CommandType Application -ErrorAction Stop|Select-Object -First 1).Source}
    foreach($ruta in @($plan,$soporte,$jar,$java,(Join-Path $PSScriptRoot 'clientes.csv'),(Join-Path $PSScriptRoot 'e6-expectativas.sql'))){
        if(-not(Test-Path -LiteralPath $ruta -PathType Leaf)){throw 'E6: dependencia ausente.'}
    }
    [xml]$xml=Get-Content $plan -Raw
    if($xml.DocumentElement.Name -ne 'jmeterTestPlan'){throw 'E6: XML incorrecto.'}
    $etapaPreparacion='procesos'
    $e6Exclusion=New-Object Threading.Mutex($false,'LocalAppoinTechPerfE6Sostenido')

    $p=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process))
    ConvertTo-Json -InputObject @($p) -Depth 5|Set-Content "$d/procesos-detectados.json" -Encoding UTF8
    if(-not $e6Exclusion.WaitOne(0)){throw 'E6: otra corrida propia tiene la exclusion activa.'}
    if(@($p|Where-Object bloquea).Count){throw 'E6: hay JMeter o recolector concurrente.'}
    $etapaPreparacion='aislamiento'
    $a=Confirmar-E2Aislamiento $co 18080
    [ordered]@{fechaUTC=[DateTime]::UtcNow.ToString('o');proyecto='appointech-perf';red=$a.Red;base=$a.Base;BackendId=$a.BackendId;DbId=$a.DbId;perfil='perf';puerto=18080}|
        ConvertTo-Json|Set-Content "$d/aislamiento-confirmado.json" -Encoding UTF8
    $estado=@(Estado-E6 $a);$estado|ConvertTo-Json|Set-Content "$d/estado-antes.json" -Encoding UTF8
    $referencia=Get-Content (Join-Path $PSScriptRoot 'INSPECCION-E6.json') -Raw|ConvertFrom-Json
    foreach($e in $estado){
        $ref=@($referencia.limites|Where-Object servicio -eq $e.servicio)
        if($ref.Count -ne 1){throw 'E6: falta referencia de limites.'}
        foreach($campo in @('memoria','nanoCpus','cuota','periodo','cpuSet')){if($e.$campo -cne $ref[0].$campo){throw 'E6: limites distintos de la inspeccion.'}}
    }
    $etapaPreparacion='datos_e_integridad'
    $huella=@(Huella-E6 $co $a);$huella|ConvertTo-Json|Set-Content "$d/integridad-antes.json" -Encoding UTF8
    $datos=Leer-E6Datos $co $a ([int]$Fase) $d
    $tipos=(Get-Content (Join-Path (Split-Path $perf) 'src/main/java/com/appointech/appointech_backend/domain/models/TipoNotificacion.java') -Raw)
    $tipos=[regex]::Match($tipos,'(?s)enum TipoNotificacion\s*\{([^}]+)\}').Groups[1].Value -split ',' | ForEach-Object {$_.Trim()}
    $datos|Add-Member -NotePropertyName tiposNotificacion -NotePropertyValue @($tipos)
    $datos|Add-Member -NotePropertyName estadosCita -NotePropertyValue @('PENDIENTE','CONFIRMADA','EN_CAMINO','EN_PROCESO','FINALIZADA','CANCELADA')
    if(@($tipos|Where-Object{$_ -notmatch '^[A-Z_]+$'}).Count){throw 'E6: enum no identificable.'}
    $csvPath=Join-Path $PSScriptRoot 'clientes.csv'
    if((Get-Content $csvPath -Encoding UTF8 -TotalCount 1) -cne 'correo,contrasena'){throw 'E6: encabezado de credenciales incorrecto.'}
    $csv=@(Import-Csv $csvPath -Encoding UTF8)
    [ordered]@{fechaUTC=[DateTime]::UtcNow.ToString('o');aislamientoConfirmado=$true;saludConfirmada=$true;limitesIgualesAReferencia=$true;
        cargaPropiaActiva=0;exclusionAdquirida=$true;HTTPHastaEstePunto=0;usuarios=[int]$Fase;cohorteSQL=$datos.identidades.Count}|
        ConvertTo-Json|Set-Content "$d/comprobaciones-previas.json" -Encoding UTF8
    if($DiagnosticoSinHttp){
        $etapaPreparacion='arranque_java_version_sin_jmeter'
        $inicio=New-Object Diagnostics.ProcessStartInfo
        $inicio.FileName=$java;$inicio.Arguments='-version';$inicio.UseShellExecute=$false;$inicio.CreateNoWindow=$true
        $inicio.RedirectStandardOutput=$true;$inicio.RedirectStandardError=$true
        $proceso=[Diagnostics.Process]::Start($inicio)
        try{
            $stdout=$proceso.StandardOutput.ReadToEndAsync();$stderr=$proceso.StandardError.ReadToEndAsync()
            if(-not $proceso.WaitForExit(10000)){throw 'E6: java -version no termino.'}
            if($proceso.ExitCode -ne 0){throw 'E6: java -version devolvio error.'}
            $version=$stdout.GetAwaiter().GetResult()+$stderr.GetAwaiter().GetResult()
            if($version -notmatch '(?i)(java|openjdk).*version'){throw 'E6: no se identifico version Java.'}
        }finally{if(-not $proceso.HasExited){$proceso.Kill();$proceso.WaitForExit()};$proceso.Dispose()}
        [ordered]@{comprobacionesLocales=$true;aislamiento=$true;cohorte=$datos.identidades.Count;integridadTablas=$huella.Count;
            codigoJavaVersion=0;JMeterIniciado=$false;logins=0;GET=0;HTTP=0;recolectorIniciado=$false}|
            ConvertTo-Json|Set-Content "$d/comprobaciones-locales.json" -Encoding UTF8
    }else{
    # Primera identidad: login y prueba de rol/pertenencia; exigir tres lecturas normales antes de preparar el resto.
    $primero=[pscustomobject]@{identidades=@($datos.identidades[0])}
    $etapaPreparacion='autenticacion_externa'
    Preparar-E6Tokens $primero $csv "$d/control-inicial-autenticacion.csv"
    Controles-E6 $co $a $estado $datos "$d/control-normal-previo.csv" $true
    $resto=[pscustomobject]@{identidades=@($datos.identidades|Select-Object -Skip 1)}
    Preparar-E6Tokens $resto $csv "$d/preparacion.csv"
    $publico=Invoke-E6Http GET '/api/especialidades' '' $null
    if($publico.codigo -ne 200){throw 'E6: catalogo no disponible.'}
    $jsonPublico=ConvertFrom-Json -InputObject $publico.texto
    $observado=@($jsonPublico|Sort-Object id)
    if(($observado.id -join ',') -cne (($datos.especialidades|Sort-Object id).id -join ',')){throw 'E6: catalogo distinto de SQL.'}
    $publico=$null
    # Evidencia sin contenido ni tokens: solo ordinal, IDs sinteticos y cantidades.
    $datos.identidades|ForEach-Object{[pscustomobject]@{ordinal=$_.ordinal;usuarioId=$_.usuarioId;clienteId=$_.clienteId;citas=@($_.citas).Count;
        totalNotificaciones=$_.totalNotificaciones;paginaNotificaciones=@($_.notificaciones).Count}}|
        Export-Csv "$d/cohorte.csv" -NoTypeInformation -Encoding UTF8
    [ordered]@{completa=$true;duracionMs=$preparacion.ElapsedMilliseconds;logins=[int]$Fase;
        getPertenencia=2*[int]$Fase;getNormalPrevio=@(Import-Csv "$d/control-normal-previo.csv"|Where-Object httpIntentado -eq 'True').Count;getCatalogo=1;calientaBackend=$true}|
        ConvertTo-Json|Set-Content "$d/preparacion.json" -Encoding UTF8
    # Un unico archivo temporal privado evita el bloque de entorno masivo de Windows.
    $etapaPreparacion='paquete_privado'
    $paquete=Escribir-E6PaquetePrivado $datos "$d/errores-seguros.jsonl"
    $props=[ordered]@{usuarios=[int]$Fase;rampa=20;sostenido=120;host='localhost';puerto=18080;timeoutConexionMs=5000;timeoutRespuestaMs=10000;
        paquete=$paquete;soporte=$soporte;setupSalida=(Join-Path $d 'setup.json');relojSoporte=(Join-Path $PSScriptRoot 'e6-reloj-sostenido.groovy');tiempoSalida=(Join-Path $d 'tiempo-inicio.json');hilosTiempoSalida=(Join-Path $d 'hilos-tiempo.json');'sampleresult.timestamp.start'='true';sample_variables='e6Hilo,e6Iteracion,e6Paso,e6TipoFallo,e6ContenidoValido,e6Umbral,e6CicloAdmitidoEpochMs'}
    $guardado=@('timestamp_format=ms','default_delimiter=,','print_field_names=true','time=true','label=true','response_code=true','response_message=true',
        'thread_name=true','successful=true','assertion_results_failure_message=true','bytes=true','sent_bytes=true','thread_counts=true','url=true',
        'latency=true','idle_time=true','connect_time=true','output_format=csv','assertion_results=none','response_data=false','response_data.on_error=false',
        'samplerData=false','requestHeaders=false','responseHeaders=false','subresults=false','encoding=false','data_type=false','filename=false','hostname=false','sample_count=false')
    foreach($g in $guardado){$kv=$g -split '=',2;$props['jmeter.save.saveservice.'+$kv[0]]=$kv[1]}
    # Guardar configuracion sin ruta de paquete ni contenido sensible.
    [ordered]@{usuarios=[int]$Fase;rampa=20;sostenido=120;getsPorCiclo=5;solicitudesEsperadas='variable';destino='http://localhost:18080';
        mezcla=@{notificaciones=40;historial=40;especialidades=20};umbralesMs=@{notificaciones=800;historial=800;especialidades=300};
        timeoutConexionMs=5000;timeoutRespuestaMs=10000;planSha256=(Get-FileHash $plan).Hash;soporteSha256=(Get-FileHash $soporte).Hash}|
        ConvertTo-Json -Depth 5|Set-Content "$d/ejecucion.json" -Encoding UTF8
    $argsJava=@('-Djava.awt.headless=true','-Djava.net.preferIPv4Stack=true','-Duser.timezone=America/Bogota','-jar',$jar,'-n','-t',$plan,
        '-l',"$d/e6.jtl",'-j',"$d/jmeter.log",'-e','-o',"$d/reporte",'-Lorg.apache.http.wire=OFF','-Lorg.apache.http.headers=OFF','-Lorg.apache.jmeter.protocol.http=OFF')
    foreach($kv in $props.GetEnumerator()){$argsJava+="-J$($kv.Key)=$($kv.Value)"}
    Comparar-E6Estado $estado @(Estado-E6 (Confirmar-E2Aislamiento $co 18080))
    $etapaPreparacion='recolector_y_java'
    try{
        $muestreo=Iniciar-MuestreoPerf $a $d;Esperar-MuestreoPerf $muestreo
        $lanzado=$true;$ErrorActionPreference='Continue';& $java @argsJava;$codigo=$LASTEXITCODE
    }finally{
        $ErrorActionPreference='Stop'
        if($muestreo){Detener-MuestreoPerf $muestreo|ConvertTo-Json|Set-Content "$d/validacion-recursos.json" -Encoding UTF8;$muestreo=$null}
    }
    $etapaPreparacion='preparacion_local'
    if(Test-Path "$d/e6.jtl"){$muestrasMedidas=@(Import-Csv "$d/e6.jtl").Count}
    if(Test-Path "$d/setup.json"){$setup=Get-Content "$d/setup.json" -Raw|ConvertFrom-Json;$setupValido=$setup.estado -ceq 'listo' -and $setup.identidades -eq [int]$Fase}
    if(-not $setupValido -or $muestrasMedidas -eq 0){
        $problemas+=@{etapa='preparacion_local';motivo='setUp fallido/ausente o JTL sin muestras, independientemente del codigo Java'}
        throw 'E6: fallo de preparacion local, no hay medicion valida.'
    }
    }
}catch{
    $original=$_
    $ocultos=@($datos.identidades|ForEach-Object token)+@($csv|ForEach-Object contrasena|Select-Object -Unique)
    $seguro=Guardar-E6ErrorSeguro $original $etapaPreparacion "$d/errores-seguros.jsonl" $ocultos
    $fallo=$true;$problemas+=$seguro
}finally{
    $ErrorActionPreference='Stop'
    # Conservar evidencias de integridad/recuperacion aunque Java o el recolector fallen.
    if($a -and $lanzado){
        try{Controles-E6 $co $a $estado $datos "$d/recuperacion.csv" $false}catch{$fallo=$true;$problemas+=(Guardar-E6ErrorSeguro $_ 'recuperacion' "$d/errores-seguros.jsonl" @($datos.identidades|ForEach-Object token))}
        try{$despues=@(Estado-E6 $a);$despues|ConvertTo-Json|Set-Content "$d/estado-despues.json" -Encoding UTF8;Comparar-E6Estado $estado $despues}
        catch{$fallo=$true;$problemas+=(Guardar-E6ErrorSeguro $_ 'estado' "$d/errores-seguros.jsonl")}
    }
    if($a -and $huella){
        try{
            $post=@(Huella-E6 $co $a);$post|ConvertTo-Json|Set-Content "$d/integridad-despues.json" -Encoding UTF8
            $iguales=($huella|ConvertTo-Json -Compress) -ceq ($post|ConvertTo-Json -Compress)
            @{iguales=$iguales}|ConvertTo-Json|Set-Content "$d/integridad.json" -Encoding UTF8
            if(-not $iguales){throw 'Cambio'}
        }catch{$fallo=$true;$problemas+=(Guardar-E6ErrorSeguro $_ 'integridad' "$d/errores-seguros.jsonl")}
    }
    if($paquete){
        try{
            if(Test-Path -LiteralPath $paquete){Remove-Item -LiteralPath $paquete -Force}
            Remove-Item -LiteralPath (Split-Path -Parent $paquete) -Force
        }catch{$fallo=$true;$problemas+=(Guardar-E6ErrorSeguro $_ 'limpieza_paquete' "$d/errores-seguros.jsonl")}
    }
    [ordered]@{codigoJMeter=$codigo;procesoJavaLanzado=$lanzado;preparacionLocalValida=$setupValido;muestrasMedidas=$muestrasMedidas;fallo=$fallo;problemas=$problemas;
        paqueteEliminado=($null -eq $paquete -or -not(Test-Path -LiteralPath $paquete));repeticiones=0}|
        ConvertTo-Json -Depth 5|Set-Content "$d/diagnostico.json" -Encoding UTF8
    if(-not(Test-Path "$d/preparacion.json")){
        @{completa=$false;duracionMs=$preparacion.ElapsedMilliseconds;verRegistroParcial=$true}|
            ConvertTo-Json|Set-Content "$d/preparacion.json" -Encoding UTF8
    }
}
if($DiagnosticoSinHttp){
    if($fallo){$causa=$problemas[0];throw ('E6: diagnostico detenido en '+$causa.etapa+'; '+$causa.tipo+': '+$causa.mensaje+' ('+$causa.archivo+':'+$causa.linea+')')}
    return
}
try{
    $ex=0;$files=@(Get-ChildItem $d -Recurse -File)
    $secrets=@($datos.identidades|ForEach-Object token)+@($csv|ForEach-Object contrasena|Select-Object -Unique)
    foreach($f in $files){$s=[IO.File]::ReadAllText($f.FullName)
        if($s -match 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|(?i)Authorization\s*:\s*Bearer'){$ex++}
        foreach($secret in $secrets){if($secret -and $s.Contains($secret)){$ex++;break}}
    }
    @{archivos=$files.Count;exposiciones=$ex}|ConvertTo-Json|Set-Content "$d/privacidad.json" -Encoding UTF8
    if($ex){throw 'E6: exposicion de secretos.'}
    if($fallo -or $codigo -ne 0){throw 'E6: ejecucion o preparacion fallida.'}
    & node.exe (Join-Path $PSScriptRoot 'analizar-e6-sostenido.js') $d; if($LASTEXITCODE -ne 0){throw 'E6: analisis sostenido fallido.'}
    $analisis=Get-Content "$d/analisis-sostenido-v2.json" -Raw|ConvertFrom-Json
    if($analisis.fallosContenido -gt 0 -or @(Import-Csv "$d/e6.jtl"|Where-Object responseCode -in @('401','403')).Count){throw 'E6: contrato o autenticacion inesperados.'}
    Write-Host "E6 fase $Fase finalizada; revisar fallos de muestras y recuperacion antes de otra fase."
}catch{
    $original=$_
    $detalle=Guardar-E6ErrorSeguro $original 'validacion_final' "$d/errores-seguros.jsonl" $secrets
    @{detenerProgresion=$true;motivo='Revisar diagnostico, integridad, JTL y recuperacion; no repetir automaticamente.'}|ConvertTo-Json|Set-Content "$d/parada.json" -Encoding UTF8
    $causa=if($problemas.Count){$problemas[0]}else{$detalle}
    throw ('E6 detenido en '+$causa.etapa+'; '+$causa.tipo+': '+$causa.mensaje+' ('+$causa.archivo+':'+$causa.linea+')')
}finally{$datos=$null;$csv=$null;$secrets=$null}

