[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$JMeterHome,[ValidateSet(50,100,150)][int]$Usuarios=50)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'e7-comun.ps1')
$perf=Split-Path $PSScriptRoot
$out=Join-Path $perf ('diagnosticos/e7-ficticio-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $out|Out-Null
$muestreo=$null;$etapa='estatica'
try{
    Confirmar-E7Fuentes
    foreach($f in @('e7-comun.ps1','recursos-e7.ps1','validar-e7.ps1','ejecutar-e7.ps1')){
        $errors=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $f),[ref]$null,[ref]$errors)
        if(@($errors).Count){throw ('E7: sintaxis PowerShell '+$f)}
    }
    foreach($f in @('generar-e7.js','analizar-e7.js')){& node.exe --check (Join-Path $PSScriptRoot $f);if($LASTEXITCODE -ne 0){throw 'E7: sintaxis JavaScript.'}}
    foreach($f in @('e7-crud.jmx','e7-ficticio.jmx')){
        [xml]$x=Get-Content (Join-Path $PSScriptRoot $f) -Raw
        if($x.DocumentElement.Name -ne 'jmeterTestPlan'){throw 'E7: XML no valido.'}
        if($x.SelectNodes('//HTTPSamplerProxy').Count -ne $(if($f -eq 'e7-crud.jmx'){1}else{0})){throw 'E7: cantidad de samplers HTTP.'}
        if($x.SelectSingleNode("//LoopController[@testname='E7 veinte ciclos']/stringProp[@name='LoopController.loops']").InnerText -ne '20' -or
           $x.SelectSingleNode("//LoopController[@testname='E7 seis pasos']/stringProp[@name='LoopController.loops']").InnerText -ne '6'){throw 'E7: ciclos/pasos incorrectos.'}
        if($x.SelectNodes('//DurationAssertion').Count){throw 'E7: umbral CRUD no acordado.'}
        if($f -eq 'e7-crud.jmx'){
            foreach($p in @(@{n='HTTPSampler.domain';v='localhost'},@{n='HTTPSampler.port';v='18080'},@{n='HTTPSampler.connect_timeout';v='5000'},@{n='HTTPSampler.response_timeout';v='10000'})){
                if($x.SelectSingleNode("//HTTPSamplerProxy/stringProp[@name='$($p.n)']").InnerText -cne $p.v){throw 'E7: destino o timeout alterado.'}
            }
        }
    }
    $java=Java-E7
    $clases=Join-Path $out 'clases';New-Item -ItemType Directory -Path $clases|Out-Null
    & $java -cp "$JMeterHome/lib/*;$JMeterHome/lib/ext/*" org.codehaus.groovy.tools.FileSystemCompiler -d $clases (Join-Path $PSScriptRoot 'e7-motor.groovy')
    if($LASTEXITCODE -ne 0){throw 'E7: compilacion Groovy fallida.'}
    $etapa='procesos'
    $mutex=New-Object Threading.Mutex($false,'LocalAppoinTechPerfE6Sostenido')
    $previos=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process))
    ConvertTo-Json -InputObject $previos -Depth 6|Set-Content "$out/procesos-previos.json" -Encoding UTF8
    if(-not $mutex.WaitOne(0) -or @($previos|Where-Object bloquea).Count){throw 'E7: proceso propio activo.'}
    $etapa='ficticio'
    $casos=@(@{nombre=('normal-'+$Usuarios);usuarios=$Usuarios;falloPaso=0;falloSinId=$false},
        @{nombre='timeout-post';usuarios=1;falloPaso=1;falloSinId=$false},
        @{nombre='timeout-put';usuarios=1;falloPaso=3;falloSinId=$false},
        @{nombre='timeout-delete';usuarios=1;falloPaso=5;falloSinId=$false},
        @{nombre='post-sin-id';usuarios=1;falloPaso=0;falloSinId=$true})
    foreach($caso in $casos){
        $d=Join-Path $out $caso.nombre;New-Item -ItemType Directory -Path $d|Out-Null
        $cfg=[ordered]@{ficticio=$true;usuarios=$caso.usuarios;ciclos=20;tecnicos=@(1..$caso.usuarios|ForEach-Object{100+11*(1+(($_-1)%50))});
            administradorId=77;baseFecha='2031-02-01';duracionMinutos=120;destino='http://localhost:18080';
            timeoutConexionMs=5000;timeoutRespuestaMs=10000;salida=$d;falloPaso=$caso.falloPaso;falloSinId=$caso.falloSinId}
        $cfg|ConvertTo-Json -Depth 6|Set-Content "$d/configuracion.json" -Encoding UTF8
        if(@($cfg.tecnicos|Select-Object -Unique).Count -ne [math]::Min($caso.usuarios,50)){throw 'E7: distribucion ficticia incorrecta.'}
        $argsJava=Argumentos-E7Java $JMeterHome (Join-Path $PSScriptRoot 'e7-ficticio.jmx') $d "$d/configuracion.json" $caso.usuarios 1 -Dashboard:($caso.nombre -like 'normal-*')
        try{
            $muestreo=Iniciar-E7Recursos ([pscustomobject]@{BackendId='ficticio';DbId='ficticio'}) $d -Ficticio
            Esperar-MuestreoPerf $muestreo
            Invocar-E7Java $java $argsJava $d 180
        }finally{
            $ErrorActionPreference='Stop'
            if($muestreo){Detener-E7Recursos $muestreo|ConvertTo-Json|Set-Content "$d/validacion-recursos.json" -Encoding UTF8;$muestreo=$null}
        }
        & node.exe (Join-Path $PSScriptRoot 'analizar-e7.js') $d
        if($LASTEXITCODE -ne 0){throw 'E7: caso ficticio no cumple.'}
    }
    $etapa='cierre'
    $restantes=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process))
    ConvertTo-Json -InputObject $restantes -Depth 6|Set-Content "$out/procesos-finales.json" -Encoding UTF8
    if(@($restantes|Where-Object bloquea).Count){throw 'E7: quedan procesos propios.'}
    $hashes=@(foreach($f in @('generar-e7.js','e7-crud.jmx','e7-ficticio.jmx','e7-motor.groovy','analizar-e7.js','e7-comun.ps1','recursos-e7.ps1','ejecutar-e7.ps1','validar-e7.ps1')){
        @{archivo=$f;sha256=(Get-FileHash (Join-Path $PSScriptRoot $f)).Hash}
    })
    $v=[ordered]@{completa=$true;fechaUTC=[DateTime]::UtcNow.ToString('o');HTTP=0;sinBackend=$true;usuariosNormales=$Usuarios;ciclos=20;pasos=6;
        tecnicosDistintos=50;hilosPorTecnico=($Usuarios/50);reservasPorHiloDistintas=$true;extraccionIdRealFicticio=$true;ordenValidado=$true;incertidumbrePOSTPUTDELETE=$true;
        rechazoDeIdAusente=$true;recolectorValidadoYCerrado=$true;JMeterCerrado=$true;evidencias=$out;hashes=$hashes}
    $v|ConvertTo-Json -Depth 7|Set-Content "$out/validacion.json" -Encoding UTF8
    $v|ConvertTo-Json -Depth 7|Set-Content (Join-Path $PSScriptRoot 'E7-VALIDACION-ULTIMA.json') -Encoding UTF8
    Write-Host "E7 ficticio completo sin HTTP: $out"
}catch{
    $ErrorActionPreference='Stop'
    @{completa=$false;HTTP=0;etapa=$etapa;evidencias=$out;prohibirCargaReal=$true}|ConvertTo-Json|
        Set-Content (Join-Path $PSScriptRoot 'E7-VALIDACION-ULTIMA.json') -Encoding UTF8
    Guardar-E6ErrorSeguro $_ $etapa "$out/error-seguro.jsonl"|Out-Null
    throw 'E7: validacion fallida; detener sin HTTP real. Revisar evidencia.'
}finally{if($mutex){$mutex.Dispose()}}
