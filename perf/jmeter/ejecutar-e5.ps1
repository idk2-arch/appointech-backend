[CmdletBinding()]
param([string]$JMeterBat,[string]$Credenciales,
    [ValidateSet('Todas','50','100','200','300')][string]$Fase='Todas')
$ErrorActionPreference='Stop'
$perf=Split-Path -Parent $PSScriptRoot
$plan=Join-Path $PSScriptRoot 'e5-login.jmx'
$soporte=Join-Path $PSScriptRoot 'e5-login.groovy'
$analizador=Join-Path $PSScriptRoot 'analizar-e5.ps1'
if(-not $JMeterBat){$JMeterBat=(Get-Command jmeter.bat -CommandType Application -ErrorAction SilentlyContinue|Select-Object -First 1).Source}
if(-not $Credenciales){$Credenciales=Join-Path $PSScriptRoot 'clientes.csv'}
foreach($archivo in @($JMeterBat,$Credenciales,$plan,$soporte,$analizador)){
    if(-not $archivo -or -not(Test-Path -LiteralPath $archivo -PathType Leaf)){throw 'Falta dependencia E5: JMeter, CSV, plan o soporte.'}
}
if([IO.Path]::GetFileName($JMeterBat) -ne 'jmeter.bat'){throw 'Indique jmeter.bat.'}
$Credenciales=(Resolve-Path -LiteralPath $Credenciales).ProviderPath
$jar=Join-Path (Split-Path -Parent (Resolve-Path -LiteralPath $JMeterBat).ProviderPath) 'ApacheJMeter.jar'
if(-not(Test-Path -LiteralPath $jar)){throw 'No existe ApacheJMeter.jar junto a jmeter.bat.'}
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}
if(-not $java -or -not(Test-Path -LiteralPath $java)){$java=(Get-Command java.exe -CommandType Application -ErrorAction Stop|Select-Object -First 1).Source}
[xml]$xml=Get-Content -LiteralPath $plan -Raw
if($xml.DocumentElement.Name -ne 'jmeterTestPlan'){throw 'XML E5 invalido.'}
. (Join-Path $PSScriptRoot 'preparar-e2.ps1')
. (Join-Path $PSScriptRoot 'recursos-perf.ps1')
$compose=@('compose','--env-file',(Join-Path $perf '.env.perf'),'-f',(Join-Path $perf 'docker-compose.perf.yml'))
[int[]]$fases=if($Fase -eq 'Todas'){@(50,100,200,300)}else{@([int]$Fase)}
$sesion=Join-Path $perf ('resultados/e5-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $sesion|Out-Null
Write-Host "Evidencias E5: $sesion"
$baseEstado=$null;$resumen=@();$faseActual='preparacion';$directorio=$null

function Estado-E5($Aislamiento){
    $objetos=Invoke-E2Docker inspect $Aislamiento.BackendId $Aislamiento.DbId|Out-String|ConvertFrom-Json
    foreach($c in $objetos){[pscustomobject]@{id=$c.Id;servicio=$c.Config.Labels.'com.docker.compose.service';
        reinicios=$c.RestartCount;inicio=$c.State.StartedAt;oom=$c.State.OOMKilled;activo=$c.State.Running;salud=$c.State.Health.Status;
        memoria=$c.HostConfig.Memory;nanoCpus=$c.HostConfig.NanoCpus;cuota=$c.HostConfig.CpuQuota;periodo=$c.HostConfig.CpuPeriod;cpuSet=$c.HostConfig.CpusetCpus}}
}
function Comparar-E5Estado($Anterior,$Actual){
    if(@($Actual).Count -ne 2){throw 'E5: faltan contenedores perf.'}
    foreach($c in $Actual){
        $a=@($Anterior|Where-Object servicio -eq $c.servicio)
        if($a.Count -ne 1 -or $a[0].id -ne $c.id -or $a[0].inicio -ne $c.inicio -or $a[0].reinicios -ne $c.reinicios -or $c.oom){throw 'E5: reinicio, recreacion u OOM; detener siguientes fases.'}
        if($a[0].memoria -ne $c.memoria -or $a[0].nanoCpus -ne $c.nanoCpus -or $a[0].cuota -ne $c.cuota -or $a[0].periodo -ne $c.periodo -or $a[0].cpuSet -ne $c.cpuSet){throw 'E5: cambiaron limites efectivos; detener comparacion.'}
    }
}
function Comprobar-E5Cuentas($Aislamiento,[int]$Usuarios){
    $sql=@'
BEGIN READ ONLY;
SELECT json_build_object('seleccionadas',count(DISTINCT u.id),
 'control',(SELECT count(*) FROM usuario u2 JOIN cliente c2 ON c2.usuario_id=u2.id WHERE u2.correo='cliente5000@perf.test' AND u2.activo AND u2.rol='CLIENTE' AND u2.contrasena IS NOT NULL))
FROM usuario u JOIN cliente c ON c.usuario_id=u.id
WHERE u.correo IN(SELECT 'cliente'||i||'@perf.test' FROM generate_series(1,__N__)i)
AND u.activo AND u.rol='CLIENTE' AND u.contrasena IS NOT NULL;
ROLLBACK;
'@.Replace('__N__',[string]$Usuarios)
    $r=Consultar-E2 $compose $Aislamiento $sql|ConvertFrom-Json
    if($r.seleccionadas -ne $Usuarios -or $r.control -ne 1){throw 'E5: cuentas sinteticas incompletas en db-perf.'}
    $r
}
function Recuperar-E5($Aislamiento,$Anterior,$Credencial,[string]$Carpeta){
    Add-Type -AssemblyName System.Net.Http
    $filas=New-Object 'System.Collections.Generic.List[object]'
    $reloj=[Diagnostics.Stopwatch]::StartNew();$consecutivas=0;$intento=0
    try{
        while($reloj.Elapsed.TotalSeconds -lt 60){
            $intento++;$inicio=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();$estado=@(Estado-E5 $Aislamiento)
            Comparar-E5Estado $Anterior $estado
            $sano=@($estado|Where-Object{-not $_.activo -or $_.salud -ne 'healthy'}).Count -eq 0
            $codigo=$null;$funcional=$false;$tipo='Salud';$duracion=$null
            if($sano){
                # Volver a acreditar destino/aislamiento ANTES de cada HTTP de control.
                $verificado=Confirmar-E2Aislamiento $compose 18080
                if($verificado.BackendId -ne $Aislamiento.BackendId -or $verificado.DbId -ne $Aislamiento.DbId){throw 'E5: destino de control cambio.'}
                $handler=New-Object Net.Http.HttpClientHandler
                $handler.AllowAutoRedirect=$false;$handler.UseProxy=$false
                $cliente=New-Object Net.Http.HttpClient($handler)
                $cliente.Timeout=[TimeSpan]::FromSeconds([math]::Max(1,[math]::Min(10,60-$reloj.Elapsed.TotalSeconds)))
                $contenido=$null;$respuesta=$null;$json=$null;$texto=$null;$body=$null
                $r=[Diagnostics.Stopwatch]::StartNew()
                try{
                    $body=@{correo=$Credencial.correo;contrasena=$Credencial.contrasena}|ConvertTo-Json -Compress
                    $contenido=New-Object Net.Http.StringContent($body,[Text.Encoding]::UTF8,'application/json')
                    $respuesta=$cliente.PostAsync('http://localhost:18080/api/auth/login',$contenido).GetAwaiter().GetResult()
                    $codigo=[int]$respuesta.StatusCode;$tipo='HTTP'
                    if($codigo -eq 200){
                        $texto=$respuesta.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                        $tipo='Contenido'
                        try{$json=$texto|ConvertFrom-Json
                            $funcional=($json.exito -is [bool]) -and $json.exito -and ($json.datos.token -is [string]) -and -not [string]::IsNullOrWhiteSpace($json.datos.token)
                        }catch{$funcional=$false}
                        if($funcional){$tipo='ninguno'}
                    }
                }catch{$tipo='Transporte'}finally{
                    $duracion=$r.ElapsedMilliseconds
                    if($respuesta){$respuesta.Dispose()};if($contenido){$contenido.Dispose()};$cliente.Dispose();$handler.Dispose()
                    $json=$null;$texto=$null;$body=$null
                }
            }
            $consecutivas=if($sano -and $funcional){$consecutivas+1}else{0}
            $filas.Add([pscustomobject]@{intento=$intento;inicioEpochMs=$inicio;finEpochMs=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();
                salud=$sano;httpIntentado=$sano;http=$codigo;funcional=$funcional;tipo=$tipo;duracionMs=$duracion;
                sobre2000Ms=($null -ne $duracion -and $duracion -gt 2000);consecutivas=$consecutivas})
            if($consecutivas -ge 3){return}
            $espera=[math]::Min(5000,[math]::Max(0,60000-$reloj.ElapsedMilliseconds))
            if($espera -gt 0){Start-Sleep -Milliseconds ([int]$espera)}
        }
        throw 'E5: no se confirmo recuperacion funcional/salud persistente en 60 segundos.'
    }finally{
        $filas|Export-Csv (Join-Path $Carpeta 'recuperacion.csv') -NoTypeInformation -Encoding UTF8
    }
}

try{
    try{$csv=@(Import-Csv -LiteralPath $Credenciales -Encoding UTF8)}catch{throw 'E5: CSV no valido; no imprimir su contenido.'}
    if(-not $csv.Count){throw 'E5: CSV sin cuentas.'}
    $columnasCsv=@($csv[0].PSObject.Properties.Name)
    if($columnasCsv.Count -ne 2 -or $columnasCsv[0] -cne 'correo' -or $columnasCsv[1] -cne 'contrasena'){throw 'E5: encabezado CSV debe ser correo,contrasena.'}
    $cuentas=@{}
    foreach($c in $csv){
        if($c.correo -cnotmatch '^cliente[1-9][0-9]*@perf\.test$' -or [string]::IsNullOrWhiteSpace($c.contrasena) -or $cuentas.ContainsKey($c.correo)){throw 'E5: CSV no sintetico, duplicado o incompleto.'}
        $cuentas[$c.correo]=$c
    }
    $maxUsuarios=[int](($fases|Measure-Object -Maximum).Maximum)
    foreach($n in 1..$maxUsuarios){if(-not $cuentas.ContainsKey("cliente$n@perf.test")){throw 'E5: faltan cuentas para la fase solicitada.'}}
    $control=$cuentas['cliente5000@perf.test'];if(-not $control){throw 'E5: falta cliente5000 de control fuera de la carga.'}
    [ordered]@{fases=$fases;rampa=10;loops=20;duracionAdicional=$false;umbralLoginMs=2000;
        timeoutConexionMs=5000;timeoutRespuestaMs=10000;destino='http://localhost:18080';recuperacionMaxSegundos=60;controlesConsecutivos=3}|
        ConvertTo-Json|Set-Content (Join-Path $sesion 'sesion.json') -Encoding UTF8
    foreach($usuarios in $fases){
        $faseActual=[string]$usuarios
        $directorio=Join-Path $sesion ('fase-'+$usuarios)
        New-Item -ItemType Directory -Path $directorio|Out-Null
        $aislamiento=Confirmar-E2Aislamiento $compose 18080
        $antes=@(Estado-E5 $aislamiento)
        if(-not $baseEstado){$baseEstado=$antes}
        Comparar-E5Estado $baseEstado $antes
        $antes|ConvertTo-Json|Set-Content (Join-Path $directorio 'estado-antes.json') -Encoding UTF8
        Comprobar-E5Cuentas $aislamiento $usuarios|ConvertTo-Json|Set-Content (Join-Path $directorio 'cuentas.json') -Encoding UTF8
        $props=[ordered]@{usuarios=$usuarios;rampa=10;loops=20;host='localhost';puerto=18080;timeoutConexionMs=5000;timeoutRespuestaMs=10000;
            credenciales=$Credenciales;soporte=$soporte;preparacionSalida=(Join-Path $directorio 'preparacion.json');
            'sampleresult.timestamp.start'='true';sample_variables='e5Hilo,e5Iteracion,e5TipoFallo,e5ContenidoValido,e5PreInicioEpochMs,e5PreFinEpochMs'}
        $guardado=@('timestamp_format=ms','default_delimiter=,','print_field_names=true','time=true','label=true','response_code=true','response_message=true',
            'thread_name=true','successful=true','assertion_results_failure_message=true','bytes=true','sent_bytes=true','thread_counts=true','url=true',
            'latency=true','idle_time=true','connect_time=true','output_format=csv','assertion_results=none','response_data=false','response_data.on_error=false',
            'samplerData=false','requestHeaders=false','responseHeaders=false','subresults=false','encoding=false','data_type=false','filename=false','hostname=false','sample_count=false')
        foreach($g in $guardado){$p=$g -split '=',2;$props['jmeter.save.saveservice.'+$p[0]]=$p[1]}
        [ordered]@{usuarios=$usuarios;loops=20;rampa=10;solicitudesEsperadas=$usuarios*20;umbralLoginMs=2000;propiedades=$props;
            planSha256=(Get-FileHash -LiteralPath $plan).Hash;soporteSha256=(Get-FileHash -LiteralPath $soporte).Hash}|
            ConvertTo-Json -Depth 5|Set-Content (Join-Path $directorio 'ejecucion.json') -Encoding UTF8
        $argsJava=@('-Djava.awt.headless=true','-Djava.net.preferIPv4Stack=true','-Duser.timezone=America/Bogota','-jar',$jar,'-n','-t',$plan,
            '-l',(Join-Path $directorio 'e5.jtl'),'-j',(Join-Path $directorio 'jmeter.log'),'-e','-o',(Join-Path $directorio 'reporte'),
            '-Lorg.apache.http.wire=OFF','-Lorg.apache.http.headers=OFF','-Lorg.apache.jmeter.protocol.http=OFF')
        foreach($p in $props.GetEnumerator()){$argsJava+="-J$($p.Key)=$($p.Value)"}
        $muestreo=$null;$codigo=$null;$errorRecursos=$false
        try{
            $muestreo=Iniciar-MuestreoPerf $aislamiento $directorio
            Esperar-MuestreoPerf $muestreo
            $ErrorActionPreference='Continue';& $java @argsJava;$codigo=$LASTEXITCODE
        }finally{
            $ErrorActionPreference='Stop'
            if($muestreo){try{Detener-MuestreoPerf $muestreo|ConvertTo-Json|Set-Content (Join-Path $directorio 'validacion-recursos.json') -Encoding UTF8}
                catch{$errorRecursos=$true;[ordered]@{csvValido=$false;tipo=$_.Exception.GetType().FullName;motivo='Fallo de captura, cierre o validacion del recolector'}|
                    ConvertTo-Json|Set-Content (Join-Path $directorio 'validacion-recursos.json') -Encoding UTF8}}
            [ordered]@{codigoJMeter=$codigo;errorRecursos=$errorRecursos}|ConvertTo-Json|Set-Content (Join-Path $directorio 'diagnostico.json') -Encoding UTF8
        }
        # La recuperacion queda fuera de -l y se intenta tambien si JMeter devolvio error.
        $despues=@(Estado-E5 $aislamiento)
        $despues|ConvertTo-Json|Set-Content (Join-Path $directorio 'estado-despues.json') -Encoding UTF8
        Comparar-E5Estado $antes $despues
        Recuperar-E5 $aislamiento $antes $control $directorio
        if($codigo -ne 0 -or $errorRecursos){throw 'E5: fallo del ejecutor/JMeter/recolector; no avanzar.'}
        if(-not(Test-Path (Join-Path $directorio 'reporte/index.html')) -or -not(Test-Path (Join-Path $directorio 'preparacion.json'))){throw 'E5: falta dashboard o evidencia de preparacion.'}
        $estadisticas=& $analizador -Directorio $directorio
        $exposiciones=0;$archivos=@(Get-ChildItem -LiteralPath $directorio -Recurse -File)
        $secretos=@(foreach($n in 1..$usuarios){$cuentas["cliente$n@perf.test"].contrasena})+@($control.contrasena)
        foreach($f in $archivos){$texto=[IO.File]::ReadAllText($f.FullName)
            if($texto -match 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|(?i)Authorization\s*:\s*Bearer' -or @($secretos|Where-Object{$texto.Contains($_)}).Count){$exposiciones++}}
        [ordered]@{archivosRevisados=$archivos.Count;exposiciones=$exposiciones}|ConvertTo-Json|Set-Content (Join-Path $directorio 'privacidad.json') -Encoding UTF8
        if($exposiciones){throw 'E5: exposicion sensible detectada; no imprimir valores ni avanzar.'}
        $m=@(Import-Csv (Join-Path $directorio 'e5.jtl'))
        if($estadisticas.fallosContenido -gt 0 -or @($m|Where-Object responseCode -in @('401','403')).Count){throw 'E5: contrato o credenciales inesperados; revisar antes de siguientes fases.'}
        $resumen+=[pscustomobject]@{usuarios=$usuarios;muestras=$estadisticas.muestras;fallidas=$estadisticas.fallidas;sobre2000Ms=$estadisticas.sobre2000Ms;recuperada=$true;directorio=$directorio}
        $resumen|ConvertTo-Json|Set-Content (Join-Path $sesion 'resumen.json') -Encoding UTF8
        Write-Host "E5 fase $usuarios finalizada. Muestras $($estadisticas.muestras), fallidas $($estadisticas.fallidas), sobre 2000 ms $($estadisticas.sobre2000Ms)."
    }
}catch{
    $motivo=if($_.Exception.Message -like 'E5:*'){$_.Exception.Message}else{'Fallo de preparacion, captura o analisis; revisar fase, tipo y artefactos sin imprimir datos sensibles.'}
    [ordered]@{fase=$faseActual;tipo=$_.Exception.GetType().FullName;motivo=$motivo;
        siguientesDetenidas=$true;repeticiones=0}|ConvertTo-Json|Set-Content (Join-Path $sesion 'parada.json') -Encoding UTF8
    throw 'E5 detenido; conservar evidencias y revisar parada.json. No se repite carga automaticamente.'
}
