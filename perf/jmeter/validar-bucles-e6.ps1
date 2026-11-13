[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$JMeterHome,[string]$Plan,
    [ValidateSet('original','corregido')][string]$Etiqueta='corregido',[switch]$SoloObservar)
$ErrorActionPreference='Stop'
if(-not $Plan){$Plan=Join-Path $PSScriptRoot 'e6-lecturas.jmx'}
$out=Join-Path (Split-Path $PSScriptRoot) ('diagnosticos/e6-bucles-'+$Etiqueta+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $out|Out-Null
Copy-Item -LiteralPath $Plan -Destination (Join-Path $out 'plan-fuente-no-ejecutado.jmx')
[xml]$xml=Get-Content -LiteralPath $Plan -Raw
$tree=$xml.DocumentElement.hashTree.hashTree
$grupo=$xml.SelectSingleNode('//ThreadGroup')
$grupoTree=$grupo.NextSibling
while($grupoTree.NodeType -ne [Xml.XmlNodeType]::Element){$grupoTree=$grupoTree.NextSibling}
foreach($n in @($tree.ChildNodes)){if($n -ne $grupo -and $n -ne $grupoTree){[void]$tree.RemoveChild($n)}}
$controllersAntes=@($xml.SelectNodes('//ThreadGroup/elementProp|//LoopController')|ForEach-Object OuterXml)
$samplers=@($xml.SelectNodes('//HTTPSamplerProxy'))
foreach($http in $samplers){
    $sub=$http.NextSibling;while($sub.NodeType -ne [Xml.XmlNodeType]::Element){$sub=$sub.NextSibling}
    $jsr=$xml.CreateElement('JSR223Sampler');$jsr.SetAttribute('guiclass','TestBeanGUI');$jsr.SetAttribute('testclass','JSR223Sampler');$jsr.SetAttribute('testname','FICTICIO E6')
    $texto=@'
int c=(vars.get('localCuenta')?:'0').toInteger()
String tipo=['notificaciones','historial','especialidades','notificaciones','historial'][c%5]
vars.put('localCuenta',(c+1).toString())
vars.put('localCiclo',(c.intdiv(5)+1).toString())
vars.put('localPaso',(c%5+1).toString())
vars.put('localHilo',(ctx.getThreadNum()+1).toString())
vars.put('localBuclesControlador',ctx.getThreadGroup().getSamplerController().getLoops().toString())
vars.put('localPropiedadLoops',props.getProperty('loops'))
SampleResult.setSampleLabel('FICTICIO '+tipo)
SampleResult.setResponseCode('200');SampleResult.setResponseMessage('FICTICIO SIN HTTP')
SampleResult.setSuccessful(true)
'@
    foreach($kv in @(@{n='scriptLanguage';v='groovy'},@{n='cacheKey';v='true'},@{n='script';v=$texto})){
        $prop=$xml.CreateElement('stringProp');$prop.SetAttribute('name',$kv.n);$prop.InnerText=$kv.v;[void]$jsr.AppendChild($prop)
    }
    [void]$http.ParentNode.ReplaceChild($jsr,$http)
    [void]$sub.ParentNode.ReplaceChild($xml.CreateElement('hashTree'),$sub)
}
if($xml.SelectNodes('//HTTPSamplerProxy|//SetupThreadGroup|//PostThreadGroup|//JSR223PreProcessor|//JSR223PostProcessor').Count){throw 'Copia local contiene elementos ajenos a validacion ficticia.'}
$controllersDespues=@($xml.SelectNodes('//ThreadGroup/elementProp|//LoopController')|ForEach-Object OuterXml)
if(Compare-Object $controllersAntes $controllersDespues -CaseSensitive){throw 'Se alteraron los controladores al construir la copia.'}
$copia=Join-Path $out 'plan-ficticio.jmx';$xml.Save($copia)
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}
if(-not $java -or -not(Test-Path $java)){$java=(Get-Command java.exe -CommandType Application|Select-Object -First 1).Source}
$jar=Join-Path $JMeterHome 'bin/ApacheJMeter.jar'
$resultados=@()
foreach($ciclos in @(3,10)){
    $jtl=Join-Path $out ("ficticio-$ciclos.jtl");$log=Join-Path $out ("ficticio-$ciclos.log")
    # Rampa 0 solo en la copia local; el plan de backend conserva 20 s.
    $argsJava=@('-Djava.awt.headless=true','-jar',$jar,'-n','-t',$copia,'-l',$jtl,'-j',$log,
        '-Jusuarios=2',"-Jloops=$ciclos",'-Jrampa=0','-Jsampleresult.timestamp.start=true',
        '-Jsample_variables=localHilo,localCiclo,localPaso,localBuclesControlador,localPropiedadLoops','-Jjmeter.save.saveservice.output_format=csv',
        '-Jjmeter.save.saveservice.print_field_names=true','-Jjmeter.save.saveservice.response_data=false',
        '-Jjmeter.save.saveservice.response_data.on_error=false','-Jjmeter.save.saveservice.samplerData=false',
        '-Jjmeter.save.saveservice.requestHeaders=false','-Jjmeter.save.saveservice.responseHeaders=false')
    $ErrorActionPreference='Continue';& $java @argsJava;$codigo=$LASTEXITCODE;$ErrorActionPreference='Stop'
    if($codigo -ne 0){throw 'JMeter ficticio devolvio error.'}
    $m=@(Import-Csv $jtl)
    $counts=@{};foreach($g in ($m|Group-Object label)){$counts[$g.Name]=$g.Count}
    $valida=($m.Count -eq 2*$ciclos*5 -and @($m|Where-Object success -ne 'true').Count -eq 0 -and
        $counts['FICTICIO notificaciones'] -eq 4*$ciclos -and $counts['FICTICIO historial'] -eq 4*$ciclos -and $counts['FICTICIO especialidades'] -eq 2*$ciclos)
    foreach($h in ($m|Group-Object localHilo)){
        $pares=@($h.Group|ForEach-Object{$_.localCiclo+':'+$_.localPaso}|Select-Object -Unique)
        if($pares.Count -ne 5*$ciclos -or $h.Count -ne 5*$ciclos){$valida=$false}
    }
    $resultados+=[pscustomobject]@{hilos=2;ciclos=$ciclos;esperadas=2*$ciclos*5;muestras=$m.Count;
        notificaciones=$counts['FICTICIO notificaciones'];historial=$counts['FICTICIO historial'];especialidades=$counts['FICTICIO especialidades'];
        muestrasPorHilo=@($m|Group-Object localHilo|ForEach-Object Count);ciclosObservados=@($m.localCiclo|Select-Object -Unique);valida=$valida;codigoJMeter=$codigo}
    $resultados[-1]|Add-Member -NotePropertyName buclesControladorRuntime -NotePropertyValue @($m.localBuclesControlador|Select-Object -Unique)
    $resultados[-1]|Add-Member -NotePropertyName propiedadLoopsRuntime -NotePropertyValue @($m.localPropiedadLoops|Select-Object -Unique)
}
[ordered]@{fechaUTC=[datetime]::UtcNow.ToString('o');etiqueta=$Etiqueta;planSha256=(Get-FileHash $Plan).Hash;
    controladoresSinCambiosEnCopia=$true;HTTP=0;destinoBackendUsado=$false;sinCredenciales=$true;rampaLocal=0;rampaBackend=20;
    casos=$resultados;todosCorrectos=(@($resultados|Where-Object valida -ne $true).Count -eq 0)}|
    ConvertTo-Json -Depth 7|Set-Content (Join-Path $out 'validacion-bucles.json') -Encoding UTF8
$resultados|Format-Table hilos,ciclos,esperadas,muestras,notificaciones,historial,especialidades,valida
Write-Host "Evidencia ficticia: $out"
if(-not $SoloObservar -and @($resultados|Where-Object valida -ne $true).Count){throw 'Validacion de bucles fallida; no ejecutar backend.'}
