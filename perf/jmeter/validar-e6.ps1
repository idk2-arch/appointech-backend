[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$JMeterHome)
$ErrorActionPreference='Stop'
$raiz=Split-Path (Split-Path $PSScriptRoot)
$salida=Join-Path $raiz 'target/e6-clases'
New-Item -ItemType Directory -Path $salida -Force|Out-Null
$archivos=@('ejecutar-e6.ps1','preparar-e6.ps1','analizar-e6.ps1','validar-e6.ps1','validar-bucles-e6.ps1')
foreach($archivo in $archivos){
    $errores=$null
    [void][Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $archivo),[ref]$null,[ref]$errores)
    if(@($errores).Count){throw "Sintaxis PowerShell incorrecta en $archivo"}
}
[xml]$xml=Get-Content (Join-Path $PSScriptRoot 'e6-lecturas.jmx') -Raw
function Comprobar-Arbol($Nodo){
    $elementos=@($Nodo.ChildNodes|Where-Object NodeType -eq Element)
    if($elementos.Count%2){throw 'Pares JMeter incompletos.'}
    for($i=0;$i -lt $elementos.Count;$i+=2){
        if($elementos[$i+1].Name -ne 'hashTree'){throw 'Falta hashTree asociado.'}
        Comprobar-Arbol $elementos[$i+1]
    }
}
Comprobar-Arbol $xml.DocumentElement.hashTree
$samplers=$xml.SelectNodes('//HTTPSamplerProxy')
if($samplers.Count -ne 1 -or $samplers[0].SelectSingleNode("stringProp[@name='HTTPSampler.method']").InnerText -cne 'GET'){throw 'E6 requiere un unico GET dinamico.'}
$grupo=$xml.SelectSingleNode('//ThreadGroup')
if($grupo.SelectSingleNode("elementProp[@name='ThreadGroup.main_controller']/stringProp[@name='LoopController.loops']").InnerText -cne '${__P(loops,10)}'){
    throw 'Cantidad de ciclos inesperada.'
}
if($xml.SelectSingleNode("//LoopController/stringProp[@name='LoopController.loops']").InnerText -cne '5'){throw 'Mezcla requiere cinco GET por ciclo.'}
if($xml.SelectSingleNode("//LoopController/boolProp[@name='LoopController.continue_forever']").InnerText -cne 'true'){
    throw 'El bucle interior debe reiniciarse en cada ciclo exterior.'
}
if($grupo.SelectSingleNode("elementProp[@name='ThreadGroup.main_controller']/boolProp[@name='LoopController.continue_forever']").InnerText -cne 'false'){
    throw 'El controlador principal debe finalizar tras los ciclos configurados.'
}
foreach($kv in @(@{name='HTTPSampler.connect_timeout';valor='${__P(timeoutConexionMs,5000)}'},@{name='HTTPSampler.response_timeout';valor='${__P(timeoutRespuestaMs,10000)}'})){
    if($samplers[0].SelectSingleNode("stringProp[@name='$($kv.name)']").InnerText -cne $kv.valor){throw 'Timeout inesperado.'}
}
foreach($p in @(@{name='ThreadGroup.num_threads';valor='${__P(usuarios,100)}'},@{name='ThreadGroup.ramp_time';valor='${__P(rampa,20)}'})){
    if($grupo.SelectSingleNode("stringProp[@name='$($p.name)']").InnerText -cne $p.valor){throw 'Propiedad de grupo inesperada.'}
}
if($xml.SelectNodes('//ResultCollector').Count -or $xml.SelectNodes('//ConstantTimer').Count){throw 'Listeners/timers inesperados.'}
$script=Get-Content (Join-Path $PSScriptRoot 'e6-lecturas.groovy') -Raw
foreach($texto in @("SampleResult.setIgnore()","Parameters=='preparar'","Parameters=='cerrar'","[0,1,2,0,1]","props.get('e6Datos')","headers.removeHeaderNamed('Authorization')")){
    if(-not $script.Contains($texto)){throw 'Falta patron de aislamiento o mezcla.'}
}
$ejecutor=Get-Content (Join-Path $PSScriptRoot 'ejecutar-e6.ps1') -Raw
foreach($prop in @('usuarios','rampa','loops','host','puerto','timeoutConexionMs','timeoutRespuestaMs','paquete','soporte')){
    if($ejecutor -notmatch ('\b'+$prop+'=')){throw 'Propiedad del plan ausente en el ejecutor.'}
}
$java=if($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}
if(-not $java -or -not(Test-Path $java)){$java=(Get-Command java.exe -CommandType Application -ErrorAction Stop|Select-Object -First 1).Source}
if(-not(Test-Path (Join-Path $JMeterHome 'bin/ApacheJMeter.jar'))){throw 'Falta instalacion JMeter.'}
& $java -cp "$JMeterHome/lib/*;$JMeterHome/lib/ext/*" org.codehaus.groovy.tools.FileSystemCompiler -d $salida (Join-Path $PSScriptRoot 'e6-lecturas.groovy')
if($LASTEXITCODE -ne 0){throw 'Compilacion Groovy fallida.'}
$hashes=@(Get-ChildItem $PSScriptRoot -File|Where-Object Name -match '(?i)(^e6-|e6\.ps1$|^README-E6|^INSPECCION-E6)'|ForEach-Object{
    [pscustomobject]@{archivo=$_.Name;sha256=(Get-FileHash $_.FullName).Hash}
})
[ordered]@{fechaUTC=[datetime]::UtcNow.ToString('o');powershell=$PSVersionTable.PSVersion.ToString();erroresSintaxis=0;xmlValido=$true;
    paresHashTreeValidos=$true;samplersHTTP=$samplers.Count;metodo='GET';groovyCompilado=$true;codigoCompilador=0;
    propiedadesVerificadas=@('usuarios','rampa','loops','host','puerto','timeoutConexionMs','timeoutRespuestaMs','paquete','soporte');
    JMeterEjecutado=$false;GroovyEjecutado=$false;solicitudesHTTP=0;cargaEjecutada=$false;hashes=$hashes;
    pendiente='Autenticacion, ACL/limpieza real, mezcla medida, contenido, recursos, integridad y recuperacion en ejecucion autorizada'}|
    ConvertTo-Json -Depth 5|Set-Content (Join-Path $PSScriptRoot 'REVISION-E6.json') -Encoding UTF8
Write-Host 'XML y PowerShell validos; Groovy compilado sin ejecutarlo. Cero solicitudes HTTP.'
