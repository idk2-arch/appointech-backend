[CmdletBinding()]
param([string]$JMeterHome=(Join-Path $env:LOCALAPPDATA 'Programs/JMeter'),[switch]$ContinuarTrasFalloPlan)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'e8-comun.ps1')
$perf=Split-Path $PSScriptRoot
$d=Join-Path $perf ('resultados/e8-50-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $d|Out-Null
Write-Host "E8 evidencias: $d"
$mutex=$null;$muestreo=$null;$a=$null;$antes=$null;$datos=$null;$paquete=$null;$lanzado=$false;$fallo=$false;$etapa='validacion_ficticia';$secretos=@();$recFin=$null;$integridad=$false;$salud=$false
$co=@('compose','--env-file',(Join-Path $perf '.env.perf'),'-f',(Join-Path $perf 'docker-compose.perf.yml'))
$prep=[Diagnostics.Stopwatch]::StartNew()
try{
    $gate=Get-Content (Join-Path $PSScriptRoot 'E8-VALIDACION-ULTIMA.json') -Raw|ConvertFrom-Json
    if(-not $gate.completa -or $gate.HTTPBackend -ne 0 -or -not $gate.sinBackend -or -not $gate.cabeceraEnviadaServidorFicticio -or $gate.identidadesWire -ne 50){throw 'E8: validacion ficticia local pendiente/fallida'}
    foreach($h in @(Hashes-E8)){$g=@($gate.hashes|Where-Object archivo -eq $h.archivo);if($g.Count -ne 1 -or $g[0].sha256 -cne $h.sha256){throw 'E8: archivo modificado desde validacion'}}
    $gate|ConvertTo-Json -Depth 7|Set-Content "$d/validacion-ficticia-referencia.json" -Encoding UTF8
    $mutex=New-Object Threading.Mutex($false,'LocalAppoinTechPerfE6Sostenido')
    $etapa='procesos';$procesos=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process))
    @{capturaUTC=[DateTime]::UtcNow.ToString('o');procesos=$procesos}|ConvertTo-Json -Depth 7|Set-Content "$d/procesos-previos.json" -Encoding UTF8
    if(-not $mutex.WaitOne(0) -or @($procesos|Where-Object bloquea).Count){throw 'E8: carga propia activa; diagnostico corresponde a este instante'}
    # Una marca persistente impide repetir accidentalmente esta fase real.
    $previas=@(Get-ChildItem (Join-Path $perf 'resultados') -Filter 'E8-REAL-INICIADO.json' -Recurse -File)
    if($previas.Count){
        $anterior=Join-Path $perf 'resultados/e8-50-20261111-213358-1fb139ce0251410aa9d24dd1382a784a'
        if(-not $ContinuarTrasFalloPlan -or $previas.Count -ne 1 -or $previas[0].Directory.FullName -ine $anterior -or (Get-FileHash "$anterior/e8.jtl").Hash -cne '26E9F6E556BF2F7000CAA583E06FB79AF18A7B7A5E4BFF5A2C5FF8E7774540CD'){throw 'E8: corrida previa no es exclusivamente el fallo de plan autorizado; detener'}
        $auditoria=Get-Content "$anterior/cierre-auditoria-final.json" -Raw|ConvertFrom-Json
        if($auditoria.resultadoE8 -cne 'FALLIDO_SIN_MEDICION' -or $auditoria.muestrasMedidas -ne 0){throw 'E8: no se confirma que el intento previo carezca de medicion'}
        @{autorizacion='Continuacion explicita del usuario tras corregir HeaderManager';intentoAnterior=$anterior;clasificacionAnterior='Fallo del plan en calentamiento, excluido de resistencia';unaNuevaCorrida=$true}|ConvertTo-Json|Set-Content "$d/continuacion-autorizada.json" -Encoding UTF8
    }
    $etapa='aislamiento_salud_limites';$a=Confirmar-E2Aislamiento $co 18080
    $a|ConvertTo-Json|Set-Content "$d/aislamiento.json" -Encoding UTF8
    $estado=@(Estado-E6 $a);$ref=Get-Content (Join-Path $PSScriptRoot 'INSPECCION-E6.json') -Raw|ConvertFrom-Json
    Comparar-E6Estado $estado $estado
    foreach($e in $estado){$r=@($ref.limites|Where-Object servicio -eq $e.servicio);if($r.Count -ne 1){throw 'Sin referencia de limites'};foreach($k in @('memoria','nanoCpus','cuota','periodo','cpuSet')){if($e.$k -cne $r[0].$k){throw 'Limites alterados'}}}
    $estado|ConvertTo-Json -Depth 6|Set-Content "$d/estado-antes.json" -Encoding UTF8
    $etapa='integridad_inicial';$antes=@(Huella-E7 $co $a);if($antes.Count -ne 15){throw 'E8: esquema no tiene las 15 tablas esperadas'}
    $antes|ConvertTo-Json|Set-Content "$d/integridad-antes.json" -Encoding UTF8
    $datos=Leer-E6Datos $co $a 50 $d
    foreach($i in $datos.identidades){if($i.correo -cne ('cliente'+$i.ordinal+'@perf.test')){throw 'E8: identidad no reservada'}}
    $enum=Get-Content (Join-Path (Split-Path $perf) 'src/main/java/com/appointech/appointech_backend/domain/models/TipoNotificacion.java') -Raw
    $tipos=@([regex]::Match($enum,'(?s)enum TipoNotificacion\s*\{([^}]+)\}').Groups[1].Value -split ','|ForEach-Object{$_.Trim()})
    $datos|Add-Member -NotePropertyName tiposNotificacion -NotePropertyValue $tipos
    $datos|Add-Member -NotePropertyName estadosCita -NotePropertyValue @('PENDIENTE','CONFIRMADA','EN_CAMINO','EN_PROCESO','FINALIZADA','CANCELADA')
    $etapa='renovar_50_tokens_fuera_JTL';Add-Type -AssemblyName System.Net.Http
    $cred=Import-Csv (Join-Path $PSScriptRoot 'clientes.csv');$secretos=@($cred|ForEach-Object contrasena)
    Preparar-E6Tokens $datos $cred "$d/autenticacion.csv"
    # Otro GET por identidad con contenido completo; fuera del JTL y sin registrar cuerpos.
    $controles=@();foreach($identidad in $datos.identidades){
        $r=Invoke-E6Http GET '/api/notificaciones?page=0&size=20' $identidad.token $null
        Confirmar-E8Contenido $r $identidad.notificaciones ([long]$identidad.totalNotificaciones)
        $controles+=@{ordinal=$identidad.ordinal;HTTP=$r.codigo;contenidoPropio=$true;duracionMs=$r.ms}
    }
    $controles|ConvertTo-Json|Set-Content "$d/controles-identidad-50.json" -Encoding UTF8
    $secretos+=@($datos.identidades|ForEach-Object token);$cred=$null
    $paquete=Escribir-E6PaquetePrivado $datos "$d/errores-seguros.jsonl"
    & node.exe (Join-Path $PSScriptRoot 'inspeccionar-e8.js') --paquete $paquete --etapa Preparacion --exigir-vigencia --salida "$d/vigencia-preparacion" > "$d/vigencia-consola.json"
    if($LASTEXITCODE -ne 0){throw 'E8: vigencia insuficiente'}
    if($prep.Elapsed.TotalSeconds -gt 180){throw 'E8: preparacion excedio 180s; detener sin JMeter real'}
    $etapa='recolector_previo';$muestreo=Iniciar-E7Recursos $a $d;Esperar-MuestreoPerf $muestreo
    Comparar-E6Estado $estado @(Estado-E6 (Confirmar-E2Aislamiento $co 18080))
    $etapa='unica_corrida_real';$argsJava=Argumentos-E8 $JMeterHome $d $paquete
    New-Item -ItemType Directory "$d/fuentes-ejecutadas"|Out-Null
    foreach($h in @(Hashes-E8)){if($h.archivo -notlike '../*'){Copy-Item (Join-Path $PSScriptRoot $h.archivo) (Join-Path "$d/fuentes-ejecutadas" $h.archivo)}}
    @{inicioUTC=[DateTime]::UtcNow.ToString('o');usuarios=50;calentamientoSeg=60;medicionSeg=600;ritmoHTTPs=10;noRepetir=$true}|ConvertTo-Json|Set-Content "$d/E8-REAL-INICIADO.json" -Encoding UTF8
    $lanzado=$true;Invocar-E7Java (Java-E7) $argsJava $d 740 $secretos
}catch{$fallo=$true;Guardar-E6ErrorSeguro $_ $etapa "$d/errores-seguros.jsonl" $secretos|Out-Null}
finally{
    if($lanzado -and $a){
        try{
            $etapa='recuperacion';$rec=@();$sw=[Diagnostics.Stopwatch]::StartNew()
            for($j=1;$j -le 3;$j++){
                foreach($k in 0..2){
                    if($sw.Elapsed.TotalSeconds -ge 60){throw 'Recuperacion excedio 60s'}
                    $ruta=@('/api/notificaciones?page=0&size=20','/api/cliente/citas','/api/especialidades')[$k]
                    $r=Invoke-E6Http GET $ruta $(if($k -eq 2){''}else{$datos.identidades[0].token}) $null
                    $expected=if($k -eq 0){$datos.identidades[0].notificaciones}elseif($k -eq 1){$datos.identidades[0].citas}else{$datos.especialidades}
                    Confirmar-E8Contenido $r $expected $(if($k -eq 0){[long]$datos.identidades[0].totalNotificaciones}else{$null})
                    $umbral=if($k -eq 2){300}else{800};$ok=$r.ms -le $umbral
                    $rec+=@{serie=$j;endpoint=$k;codigo=$r.codigo;contenidoIDs=$true;duracionMs=$r.ms;umbralMs=$umbral;conforme=$ok;epochMs=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()}
                    if(-not $ok){$fallo=$true}
                }
                if($j -lt 3){Start-Sleep -Seconds 2}
            }
            Comparar-E6Estado $estado @(Estado-E6 (Confirmar-E2Aislamiento $co 18080));$salud=$true
            Estado-E6 $a|ConvertTo-Json|Set-Content "$d/estado-despues.json" -Encoding UTF8
            $recFin=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
            @{finEpochMs=$recFin;controles=$rec;duracionTotalMs=$sw.ElapsedMilliseconds}|ConvertTo-Json -Depth 6|Set-Content "$d/recuperacion.json" -Encoding UTF8
            # Permitir un par completo posterior al ultimo control antes de pedir cierre.
            Start-Sleep -Seconds 3
        }catch{$fallo=$true;Guardar-E6ErrorSeguro $_ 'recuperacion' "$d/errores-seguros.jsonl" $secretos|Out-Null}
    }
    if($a -and $antes){try{
        $despues=@(Huella-E7 $co $a);$despues|ConvertTo-Json|Set-Content "$d/integridad-despues.json" -Encoding UTF8
        $integridad=($antes|ConvertTo-Json -Compress) -ceq ($despues|ConvertTo-Json -Compress)
        @{iguales=$integridad;tablas=$antes.Count;excluidos=@('log_sistema','flyway_schema_history')}|ConvertTo-Json|Set-Content "$d/integridad.json" -Encoding UTF8
        if(-not $integridad){$fallo=$true}
    }catch{$fallo=$true;Guardar-E6ErrorSeguro $_ 'integridad' "$d/errores-seguros.jsonl" $secretos|Out-Null}}
    if($muestreo){try{Detener-E7Recursos $muestreo|ConvertTo-Json|Set-Content "$d/validacion-recursos.json" -Encoding UTF8}catch{$fallo=$true;Guardar-E6ErrorSeguro $_ 'cierre_recolector' "$d/errores-seguros.jsonl" $secretos|Out-Null}}
    if($paquete){if(Test-Path -LiteralPath $paquete){Remove-Item -LiteralPath $paquete -Force};$padre=Split-Path $paquete;if(Test-Path -LiteralPath $padre){Remove-Item -LiteralPath $padre -Force}}
    if($lanzado){
        & node.exe (Join-Path $PSScriptRoot 'analizar-e8.js') $d
        if($LASTEXITCODE -ne 0){$fallo=$true}else{
            $r=Get-Content "$d/analisis-e8.json" -Raw|ConvertFrom-Json
            if(-not $r.coberturaRecursos.completa -or $r.erroresMedidosPct -ge 1 -or @($r.desviosVentana).Count){$fallo=$true}
            # Dashboard fuera del periodo y del recolector, sobre copia derivada solo medida.
            $dash=@('-Djava.awt.headless=true','-jar',(Join-Path $JMeterHome 'bin/ApacheJMeter.jar'),'-g',"$d/medicion.jtl",'-o',"$d/dashboard",'-j',"$d/dashboard.log",'-Jsample_variables=e6Hilo,e6Tipo,e6TipoFallo,e6ContenidoValido,e6Umbral,e8Fase,e8Turno,e8Plan,e8RetrasoMs')
            $reporte=Join-Path $d 'reporte-automatico';New-Item -ItemType Directory -Path $reporte|Out-Null
            try{Invocar-E7Java (Java-E7) $dash $reporte 120}catch{$fallo=$true;Guardar-E6ErrorSeguro $_ 'dashboard_sin_carga' "$d/errores-seguros.jsonl"|Out-Null}
        }
    }
    $p=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process));ConvertTo-Json -InputObject $p -Depth 7|Set-Content "$d/procesos-finales.json" -Encoding UTF8
    if(@($p|Where-Object bloquea).Count){$fallo=$true}
    @{completa=(-not $fallo);corridaRealIniciada=$lanzado;integridad15Tablas=$integridad;salud=$salud;evidencias=$d;E9=$false;comando='powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/ejecutar-e8.ps1 -ContinuarTrasFalloPlan';sinReintento=$true}|
        ConvertTo-Json|Set-Content "$d/verificacion-final.json" -Encoding UTF8
    if(Test-Path "$d/INFORME.md"){Add-Content "$d/INFORME.md" "`nVerificacion final: completa=$(-not $fallo); 15 tablas iguales=$integridad; salud=$salud. Autenticacion fuera del JTL. Una sola corrida, sin E9." -Encoding UTF8}
    if($mutex){$mutex.Dispose()};$datos=$null;$secretos=@()
}
Write-Host "E8 finalizado; evidencia: $d"
if($fallo){throw 'E8: fallo o limitacion registrada; no repetir ni avanzar'}
