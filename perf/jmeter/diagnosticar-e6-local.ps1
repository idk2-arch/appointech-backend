[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'errores-e6.ps1')
. (Join-Path $PSScriptRoot 'procesos-e6.ps1')
$d=Join-Path (Split-Path $PSScriptRoot) ('diagnosticos/e6-pruebas-locales-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory $d|Out-Null
$etapa='sintaxis'
try{
    $files=@('ejecutar-e6.ps1','preparar-e6.ps1','errores-e6.ps1','procesos-e6.ps1','diagnosticar-e6-local.ps1')
    foreach($f in $files){$errores=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $f),[ref]$null,[ref]$errores);if(@($errores).Count){throw "Error de sintaxis en $f"}}
    $etapa='parametros_sin_ejecutar_cuerpo'
    $ast=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot 'ejecutar-e6.ps1'),[ref]$null,[ref]$null)
    $prueba=[scriptblock]::Create($ast.ParamBlock.Extent.Text+"`n[pscustomobject]@{fase=`$Fase;diagnostico=[bool]`$DiagnosticoSinHttp}")
    $r=& $prueba -Fase 100 -JMeterBat 'C:\Ruta ficticia con espacios\jmeter.bat' -DiagnosticoSinHttp
    if($r.fase -ne '100' -or -not $r.diagnostico){throw 'Parametros validos no conservados.'}
    $negativo=$false
    try{& $prueba -Fase 999|Out-Null}catch{
        $negativo=$true;Guardar-E6ErrorSeguro $_ 'parametro_invalido_esperado' "$d/errores-esperados.jsonl"|Out-Null
    }
    if(-not $negativo){throw 'Se acepto fase invalida.'}
    $etapa='detector_procesos_ficticios'
    $casos=@(
        @{nombre='JMeter jar con espacios';exe='java.exe';cmd='java.exe -jar "C:\Ruta con espacios\ApacheJMeter.jar" -n';esperado=$true},
        @{nombre='JMeter jar relativo';exe='java.exe';cmd='java.exe -jar ApacheJMeter.jar';esperado=$true},
        @{nombre='JMeter NewDriver';exe='java.exe';cmd='java.exe -cp "C:\JMeter\lib\*" org.apache.jmeter.NewDriver -n';esperado=$true},
        @{nombre='Groovy sin motor';exe='java.exe';cmd='java.exe -cp "C:\JMeter\lib\*" groovy.ui.GroovyMain validar.groovy';esperado=$false},
        @{nombre='Docker stats';exe='docker.exe';cmd='docker.exe stats --no-stream';esperado=$true},
        @{nombre='Recolector real';exe='powershell.exe';cmd='powershell.exe -File "C:\Ruta con espacios\recursos-perf.ps1"';esperado=$true},
        @{nombre='Mencion sin ejecutar recolector';exe='powershell.exe';cmd='powershell.exe -File otro.ps1 -Mensaje recursos-perf.ps1';esperado=$false},
        @{nombre='Java version';exe='java.exe';cmd='java.exe -version';esperado=$false}
    )
    $procesos=@(foreach($c in $casos){
        $p=[pscustomobject]@{Name=$c.exe;CommandLine=$c.cmd;ProcessId=123}
        $actual=@(Seleccionar-E6ProcesosCarga @($p)).Count -gt 0
        $anterior=($c.exe -match '^java(w)?\.exe$' -and $c.cmd -match 'ApacheJMeter|jmeter') -or
            ($c.exe -eq 'docker.exe' -and $c.cmd -match '\sstats\s') -or
            ($c.exe -eq 'powershell.exe' -and $c.cmd -match '\s-File\s+[^\r\n]*recursos-perf\.ps1')
        if($actual -ne $c.esperado){throw ('Clasificacion incorrecta: '+$c.nombre)}
        [pscustomobject]@{caso=$c.nombre;esperado=$c.esperado;corregido=$actual;anterior=$anterior}
    })
    $etapa='error_record_seguro'
    $secreto='FICTICIO_PRIVADO'
    try{throw [InvalidOperationException]::new(('Detalle util: archivo ausente; password='+$secreto+'; {"correo":"ficticio@perf.test","contrasena":"'+$secreto+'"}'),[IO.IOException]::new('Detalle interno: archivo ausente'))}
    catch{
        $original=$_
        $seguro=Guardar-E6ErrorSeguro $original 'error_ficticio_esperado' "$d/errores-esperados.jsonl" @($secreto)
        $j=$seguro|ConvertTo-Json -Depth 6 -Compress
        if($j.Contains($secreto) -or $j.Contains('ficticio@perf.test') -or $j -match 'TargetObject|PositionMessage|ScriptStackTrace'){throw 'Fallo de censura.'}
        if($seguro.tipo -ne $original.Exception.GetType().FullName -or $seguro.linea -le 0 -or $seguro.excepciones.Count -ne 2 -or $seguro.mensaje -notmatch 'Detalle util: archivo ausente'){throw 'Se perdio informacion util del ErrorRecord.'}
    }
    $etapa='arranque_powershell_inocuo'
    $info=New-Object Diagnostics.ProcessStartInfo
    $info.FileName=(Get-Command powershell.exe -CommandType Application|Select-Object -First 1).Source
    $info.Arguments='-NoProfile -NonInteractive -Command "exit 0"';$info.UseShellExecute=$false;$info.CreateNoWindow=$true
    $p=[Diagnostics.Process]::Start($info)
    try{if(-not $p.WaitForExit(10000) -or $p.ExitCode -ne 0){throw 'No termino PowerShell local.'}}
    finally{if(-not $p.HasExited){$p.Kill();$p.WaitForExit()};$p.Dispose()}
    [ordered]@{fechaUTC=[datetime]::UtcNow.ToString('o');powershell=$PSVersionTable.PSVersion.ToString();sintaxisValida=$true;
        parametrosValidos=$true;parametroInvalidoRechazado=$true;erroresConservadosYCensurados=$true;casosProcesos=$procesos;
        arranquePowerShell=0;HTTP=0;logins=0;GET=0;JMeterIniciado=$false}|
        ConvertTo-Json -Depth 6|Set-Content "$d/validacion.json" -Encoding UTF8
    Write-Host "Validaciones locales completas, sin HTTP: $d"
}catch{
    $seguro=Guardar-E6ErrorSeguro $_ $etapa "$d/error-inesperado.jsonl" @($secreto)
    throw ($seguro.etapa+': '+$seguro.mensaje+' ('+$seguro.archivo+':'+$seguro.linea+')')
}
