[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$Directorio)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'e8-comun.ps1')
$root=[IO.Path]::GetFullPath((Join-Path (Split-Path $PSScriptRoot) 'resultados'))+[IO.Path]::DirectorySeparatorChar
$d=[IO.Path]::GetFullPath($Directorio)
if(-not $d.StartsWith($root,[StringComparison]::OrdinalIgnoreCase) -or -not(Test-Path "$d/E8-REAL-INICIADO.json")){throw 'No es corrida E8 propia'}
$out=Join-Path $d 'postflight-recuperacion';New-Item -ItemType Directory -Path $out -ErrorAction Stop|Out-Null
$mutex=$null;$muestreo=$null;$secretos=@();$fallo=$false;$etapa='procesos'
$co=@('compose','--env-file',(Join-Path (Split-Path $PSScriptRoot) '.env.perf'),'-f',(Join-Path (Split-Path $PSScriptRoot) 'docker-compose.perf.yml'))
try{
    $mutex=New-Object Threading.Mutex($false,'LocalAppoinTechPerfE6Sostenido')
    $p=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process));ConvertTo-Json -InputObject $p -Depth 6|Set-Content "$out/procesos-previos.json" -Encoding UTF8
    if(-not $mutex.WaitOne(0) -or @($p|Where-Object bloquea).Count){throw 'Proceso propio activo; no enviar controles'}
    $a=Confirmar-E2Aislamiento $co 18080;$base=Get-Content "$d/estado-antes.json" -Raw|ConvertFrom-Json
    Comparar-E6Estado $base @(Estado-E6 $a)
    $etapa='captura_independiente';$muestreo=Iniciar-E7Recursos $a $out;Esperar-MuestreoPerf $muestreo
    $etapa='autenticacion_control';$datos=Leer-E6Datos $co $a 1 $out;Add-Type -AssemblyName System.Net.Http
    $cred=Import-Csv (Join-Path $PSScriptRoot 'clientes.csv');$secretos=@($cred|ForEach-Object contrasena)
    Preparar-E6Tokens $datos $cred "$out/autenticacion.csv";$secretos+=@($datos.identidades|ForEach-Object token)
    $rec=@();$start=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();$sw=[Diagnostics.Stopwatch]::StartNew()
    foreach($j in 1..3){foreach($k in 0..2){
        if($sw.Elapsed.TotalSeconds -gt 60){throw 'Recuperacion excedio 60s'}
        $ruta=@('/api/notificaciones?page=0&size=20','/api/cliente/citas','/api/especialidades')[$k]
        $r=Invoke-E6Http GET $ruta $(if($k -eq 2){''}else{$datos.identidades[0].token}) $null
        $expected=if($k -eq 0){$datos.identidades[0].notificaciones}elseif($k -eq 1){$datos.identidades[0].citas}else{$datos.especialidades}
        Confirmar-E8Contenido $r $expected $(if($k -eq 0){[long]$datos.identidades[0].totalNotificaciones}else{$null})
        $limit=if($k -eq 2){300}else{800};$ok=$r.ms -le $limit
        $rec+=@{serie=$j;endpoint=$k;codigo=$r.codigo;contenido=$true;duracionMs=$r.ms;umbralMs=$limit;conforme=$ok}
        if(-not $ok){$fallo=$true}
    };if($j -lt 3){Start-Sleep -Seconds 2}}
    $fin=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    Comparar-E6Estado $base @(Estado-E6 (Confirmar-E2Aislamiento $co 18080))
    Estado-E6 $a|ConvertTo-Json|Set-Content "$out/estado.json" -Encoding UTF8
    $antes=Get-Content "$d/integridad-antes.json" -Raw|ConvertFrom-Json;$despues=@(Huella-E7 $co $a)
    $despues|ConvertTo-Json|Set-Content "$out/integridad.json" -Encoding UTF8
    $igual=($antes|ConvertTo-Json -Compress) -ceq ($despues|ConvertTo-Json -Compress)
    if(-not $igual -or $despues.Count -ne 15){throw 'Integridad distinta'}
    @{completa=(-not $fallo);inicioEpochMs=$start;finEpochMs=$fin;controles=$rec;integridad15Tablas=$igual;salud=$true;JMeter=0;nuevaCorridaCarga=$false;loginFueraJTL=1;controlesAutenticacion=2;controlesRecuperacion=9;nota='Control posterior independiente; no sustituye medicion E8 ni extiende artificialmente su ventana'}|
        ConvertTo-Json -Depth 8|Set-Content "$out/recuperacion.json" -Encoding UTF8
    Start-Sleep -Seconds 3
}catch{$fallo=$true;Guardar-E6ErrorSeguro $_ $etapa "$out/error-seguro.jsonl" $secretos|Out-Null}
finally{
    if($muestreo){try{Detener-E7Recursos $muestreo|ConvertTo-Json|Set-Content "$out/validacion-recursos.json" -Encoding UTF8}catch{$fallo=$true;Guardar-E6ErrorSeguro $_ 'cierre_recursos' "$out/error-seguro.jsonl" $secretos|Out-Null}}
    if($mutex){$mutex.Dispose()};$datos=$null;$cred=$null;$secretos=@()
}
if($fallo){throw "E8 control posterior fallo: $out"};Write-Host "Recuperacion posterior completa, sin JMeter ni nueva carga: $out"
