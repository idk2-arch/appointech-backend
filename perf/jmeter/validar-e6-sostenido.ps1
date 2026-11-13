[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$JMeterHome)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'errores-e6.ps1')
. (Join-Path $PSScriptRoot 'procesos-e6.ps1')
$out=Join-Path (Split-Path $PSScriptRoot) ('diagnosticos/e6-sostenido-ficticio-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $out|Out-Null
$etapa='sintaxis'
. (Join-Path $PSScriptRoot 'recursos-perf.ps1')
$muestreo=$null
try{
    foreach($file in @('ejecutar-e6-sostenido.ps1','validar-e6-sostenido.ps1')){
        $err=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $file),[ref]$null,[ref]$err)
        if(@($err).Count){throw 'E6: sintaxis PowerShell invalida.'}
    }
    & node.exe --check (Join-Path $PSScriptRoot 'analizar-e6-sostenido.js');if($LASTEXITCODE -ne 0){throw 'E6: sintaxis Node invalida.'}
    & node.exe (Join-Path $PSScriptRoot 'analizar-e6-sostenido.js') --selftest
    if($LASTEXITCODE -ne 0){throw 'E6: regla de limites invalida.'}
    $etapa='procesos_previos'
    $e6Exclusion=New-Object Threading.Mutex($false,'LocalAppoinTechPerfE6Sostenido')

    $previos=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process))
    ConvertTo-Json -InputObject @($previos) -Depth 5|Set-Content "$out/procesos-previos.json" -Encoding UTF8
    if(-not $e6Exclusion.WaitOne(0)){throw 'E6: otra corrida propia tiene la exclusion activa.'}
    if(@($previos|Where-Object bloquea).Count){throw 'E6: proceso de carga previo activo.'}
    $java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}
    if(-not $java -or -not(Test-Path $java)){$java=(Get-Command java.exe -CommandType Application|Select-Object -First 1).Source}
    $reloj=Join-Path $PSScriptRoot 'e6-reloj-sostenido.groovy'
    $clases=Join-Path $out 'clases';New-Item -ItemType Directory $clases|Out-Null
    & $java -cp "$JMeterHome/lib/*;$JMeterHome/lib/ext/*" org.codehaus.groovy.tools.FileSystemCompiler -d $clases $reloj
    if($LASTEXITCODE -ne 0){throw 'E6: compilacion reloj fallida.'}
    $etapa='copia_ficticia'
    [xml]$x=Get-Content (Join-Path $PSScriptRoot 'e6-sostenido.jmx') -Raw
    $controladores=@($x.SelectNodes('//ThreadGroup/elementProp|//LoopController|//WhileController')|ForEach-Object OuterXml)
    # Solo se retiran la preparacion y cierre REST del paquete; el reloj y los bucles se conservan.
    foreach($jsr in @($x.SelectNodes('//SetupThreadGroup/following-sibling::hashTree[1]/JSR223Sampler|//PostThreadGroup/following-sibling::hashTree[1]/JSR223Sampler'))){
        $filename=$jsr.SelectSingleNode("stringProp[@name='filename']").InnerText
        if($filename -ceq '${__P(soporte)}'){$sub=$jsr.NextSibling;[void]$jsr.ParentNode.RemoveChild($sub);[void]$jsr.ParentNode.RemoveChild($jsr)}
    }
    foreach($http in @($x.SelectNodes('//HTTPSamplerProxy'))){
        $sub=$http.NextSibling;$jsr=$x.CreateElement('JSR223Sampler');$jsr.SetAttribute('testclass','JSR223Sampler');$jsr.SetAttribute('guiclass','TestBeanGUI');$jsr.SetAttribute('testname','FICTICIO sostenido')
        $script=@'
int count=(vars.get('e6Cuenta')?:'0').toInteger()
String label=['notificaciones','historial','especialidades','notificaciones','historial'][count%5]
vars.put('e6Cuenta',(count+1).toString());vars.put('e6Hilo',(ctx.getThreadNum()+1).toString())
vars.put('e6Iteracion',(count.intdiv(5)+1).toString());vars.put('e6Paso',(count%5+1).toString())
vars.put('e6TipoFallo','ninguno');vars.put('e6ContenidoValido','true');vars.put('e6Umbral',label=='especialidades'?'300':'800')
Thread.sleep(40)
SampleResult.setSampleLabel('FICTICIO E6 GET '+label)
SampleResult.setResponseCode('200');SampleResult.setResponseMessage('FICTICIO SIN HTTP');SampleResult.setSuccessful(true)
'@
        foreach($kv in @(@{n='scriptLanguage';v='groovy'},@{n='cacheKey';v='true'},@{n='script';v=$script})){$p=$x.CreateElement('stringProp');$p.SetAttribute('name',$kv.n);$p.InnerText=$kv.v;[void]$jsr.AppendChild($p)}
        [void]$http.ParentNode.ReplaceChild($jsr,$http);[void]$sub.ParentNode.ReplaceChild($x.CreateElement('hashTree'),$sub)
    }
    if($x.SelectNodes('//HTTPSamplerProxy|//JSR223PreProcessor|//JSR223PostProcessor').Count){throw 'E6: copia local no aislada.'}
    if(Compare-Object $controladores @($x.SelectNodes('//ThreadGroup/elementProp|//LoopController|//WhileController')|ForEach-Object OuterXml) -CaseSensitive){throw 'E6: se alteraron controladores.'}
    $fake=Join-Path $out 'solo-ficticio.jmx';$x.Save($fake)
    @{usuarios=2;rampa=20;sostenido=120;ficticio=$true}|ConvertTo-Json|Set-Content "$out/ejecucion.json" -Encoding UTF8
    $argsJava=@('-Djava.awt.headless=true','-jar',(Join-Path $JMeterHome 'bin/ApacheJMeter.jar'),'-n','-t',$fake,'-l',"$out/e6.jtl",'-j',"$out/jmeter.log",'-e','-o',"$out/reporte",
        '-Jusuarios=2','-Jrampa=20','-Jsostenido=120',"-JrelojSoporte=$reloj","-JtiempoSalida=$out/tiempo-inicio.json","-JhilosTiempoSalida=$out/hilos-tiempo.json",'-Jsampleresult.timestamp.start=true',
        '-Jsample_variables=e6Hilo,e6Iteracion,e6Paso,e6TipoFallo,e6ContenidoValido,e6Umbral,e6CicloAdmitidoEpochMs')
    $guardado=@('timestamp_format=ms','default_delimiter=,','print_field_names=true','time=true','label=true','response_code=true','response_message=true','thread_name=true','successful=true','assertion_results_failure_message=true',
        'bytes=true','sent_bytes=true','thread_counts=true','url=true','latency=true','idle_time=true','connect_time=true','output_format=csv','assertion_results=none','response_data=false','response_data.on_error=false','samplerData=false','requestHeaders=false','responseHeaders=false','subresults=false','encoding=false','data_type=false','filename=false','hostname=false','sample_count=false')
    foreach($g in $guardado){$argsJava+="-Jjmeter.save.saveservice.$g"}
    $etapa='ejecucion_ficticia'
    try{
        $muestreo=Iniciar-MuestreoPerf ([pscustomobject]@{BackendId='ficticio-backend';DbId='ficticio-db'}) $out -Ficticio
        Esperar-MuestreoPerf $muestreo
        $ErrorActionPreference='Continue';& $java @argsJava;$code=$LASTEXITCODE;$ErrorActionPreference='Stop'
    }finally{
        $ErrorActionPreference='Stop'
        if($muestreo){Detener-MuestreoPerf $muestreo|ConvertTo-Json|Set-Content "$out/validacion-recursos.json"; $muestreo=$null}
    }
    if($code -ne 0){throw 'E6: JMeter ficticio fallo.'}
    $etapa='analisis_ficticio'
    & node.exe (Join-Path $PSScriptRoot 'analizar-e6-sostenido.js') $out --ficticio
    if($LASTEXITCODE -ne 0){throw 'E6: analisis ficticio fallo.'}
    $info=Get-Content "$out/analisis-ficticio.json" -Raw|ConvertFrom-Json
    if(@($info.hilos|Where-Object ciclos -le 1).Count){throw 'E6: el ciclo no se repitio.'}
    if(@($info.periodos|Where-Object{$_.periodo -eq 'sostenido' -and $_.muestras -gt 0}).Count -ne 1){throw 'E6: falta sostenido ficticio.'}
    $activos=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process))
    ConvertTo-Json -InputObject @($activos) -Depth 5|Set-Content "$out/procesos-finales.json"
    if(@($activos|Where-Object bloquea).Count){throw 'E6: quedan procesos de carga/recoleccion.'}
    [ordered]@{completa=$true;HTTP=0;sinBackend=$true;controladoresIdenticos=$true;rampaLocal=20;sostenidoLocal=120;
        duracionBackend=@{rampa=20;sostenido=120};muestras=$info.muestras;ciclosPorHilo=@($info.hilos|Select-Object hilo,ciclos);
        paradaPorPlazoValidada=$true;clasificacionLimitesValidada=$true;procesosRestantes=0;recolectoresIniciados=1;recolectorCerrado=$true}|
        ConvertTo-Json -Depth 6|Set-Content "$out/validacion-previa.json" -Encoding UTF8
    Write-Host "Validacion sostenida sin HTTP completa: $out"
}catch{
    $seguro=Guardar-E6ErrorSeguro $_ $etapa "$out/error-seguro.jsonl"
    throw ($seguro.etapa+': '+$seguro.mensaje+' ('+$seguro.archivo+':'+$seguro.linea+')')
}

