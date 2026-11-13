[CmdletBinding()]
param([ValidateSet(50,100,150)][int]$Usuarios=50,[string]$JMeterHome=(Join-Path $env:LOCALAPPDATA 'Programs/JMeter'),[string]$CredencialesJson)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'e7-comun.ps1')
$perf=Split-Path $PSScriptRoot
$d=Join-Path $perf (('resultados/e7-'+$Usuarios+'-')+(Get-Date -Format 'yyyyMMdd-HHmmss')+'-'+[Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $d|Out-Null
Write-Host "E7 evidencias: $d"
$etapa='validacion_previa';$fallo=$false;$http=$false;$lanzado=$false;$paquete=$null;$muestreo=$null;$a=$null;$antes=$null;$cfg=$null;$secretos=@();$mutex=$null
$token=$null;$clave=$null;$datos=$null;$problemas=@();$co=@('compose','--env-file',(Join-Path $perf '.env.perf'),'-f',(Join-Path $perf 'docker-compose.perf.yml'))
try{
    $gate=Get-Content (Join-Path $PSScriptRoot 'E7-VALIDACION-ULTIMA.json') -Raw|ConvertFrom-Json
    if(-not $gate.completa -or $gate.HTTP -ne 0 -or -not $gate.sinBackend -or $gate.usuariosNormales -ne $Usuarios){throw 'E7: falta validacion ficticia satisfactoria.'}
    foreach($f in $gate.hashes){if((Get-FileHash (Join-Path $PSScriptRoot $f.archivo)).Hash -cne $f.sha256){throw 'E7: cambio un archivo despues de validar.'}}
    if($Usuarios -eq 100){
        $concurrencia=Get-Content (Join-Path $PSScriptRoot 'E7-CONCURRENCIA-50-VALIDADA.json') -Raw|ConvertFrom-Json
        if(-not $concurrencia.permite100 -or (Get-FileHash -LiteralPath $concurrencia.JTL50).Hash -ine $concurrencia.sha256){throw 'E7: analisis de concurrencia 50 ausente o fuente alterada.'}
        $concurrencia|ConvertTo-Json -Depth 8|Set-Content "$d/puerta-concurrencia-50.json" -Encoding UTF8
    }
    Confirmar-E7Fuentes
    $java=Java-E7
    if(-not(Test-Path (Join-Path $JMeterHome 'bin/ApacheJMeter.jar'))){throw 'E7: JMeter no disponible.'}
    $etapa='procesos_y_exclusion'
    $mutex=New-Object Threading.Mutex($false,'LocalAppoinTechPerfE6Sostenido')
    $procesos=@(Seleccionar-E6ProcesosCarga @(Get-CimInstance Win32_Process))
    ConvertTo-Json -InputObject $procesos -Depth 6|Set-Content "$d/procesos-previos.json" -Encoding UTF8
    if(-not $mutex.WaitOne(0) -or @($procesos|Where-Object bloquea).Count){throw 'E7: carga propia activa.'}
    $etapa='aislamiento_y_limites'
    $a=Confirmar-E2Aislamiento $co 18080
    $estado=@(Estado-E6 $a)
    $ref=Get-Content (Join-Path $PSScriptRoot 'INSPECCION-E6.json') -Raw|ConvertFrom-Json
    foreach($e in $estado){
        $r=@($ref.limites|Where-Object servicio -eq $e.servicio)
        if($r.Count -ne 1 -or -not $e.activo -or $e.salud -ne 'healthy' -or $e.oom){throw 'E7: estado no saludable.'}
        foreach($k in @('memoria','nanoCpus','cuota','periodo','cpuSet')){if($e.$k -cne $r[0].$k){throw 'E7: limite de recursos cambiado.'}}
    }
    $estado|ConvertTo-Json -Depth 5|Set-Content "$d/estado-antes.json" -Encoding UTF8
    @{fechaUTC=[DateTime]::UtcNow.ToString('o');base=$a.Base;red=$a.Red;BackendId=$a.BackendId;DbId=$a.DbId;perfil='perf';puerto=18080;HTTP=0}|
        ConvertTo-Json|Set-Content "$d/aislamiento.json" -Encoding UTF8
    $etapa='identidades_y_reservas'
    $sql=@'
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
SELECT json_build_object(
'administradores',(SELECT json_agg(s) FROM (SELECT id,correo,rol::text,activo FROM usuario WHERE correo='admin@appointech.com' AND rol::text='ADMINISTRADOR' AND activo) s),
'tecnicos',(SELECT json_agg(s ORDER BY ordinal) FROM (SELECT substring(u.correo FROM '[0-9]+')::int AS ordinal,t.id AS tecnicoid,u.id AS usuarioid,u.activo,u.rol::text FROM tecnico t JOIN usuario u ON u.id=t.usuario_id WHERE u.correo ~ '^tecnico([1-9]|[1-4][0-9]|50)@perf[.]test$') s),
'duracion',(SELECT duracion_bloque_minutos FROM configuracion_franjas ORDER BY id DESC LIMIT 1),
'baseFecha',greatest((CURRENT_TIMESTAMP AT TIME ZONE 'America/Bogota')::date+30,(SELECT max(fecha)+1 FROM franja_tecnico),(SELECT max(fecha_hora_fin)::date+1 FROM cita),(SELECT max(fin)::date+1 FROM bloque_indisponibilidad)));
ROLLBACK;
'@
    $datos=Consultar-E2 $co $a $sql|Out-String|ConvertFrom-Json
    if(@($datos.administradores).Count -ne 1 -or @($datos.tecnicos).Count -ne 50 -or $datos.duracion -lt 15 -or $datos.duracion -gt 480){throw 'E7: faltan identidades/configuracion.'}
    $i=0;foreach($t in $datos.tecnicos){$i++;if($t.ordinal -ne $i -or -not $t.activo -or $t.rol -cne 'TECNICO'){throw 'E7: tecnico no sintetico/activo.'}}
    $tech=@($datos.tecnicos|ForEach-Object tecnicoid)
    if(@($tech|Select-Object -Unique).Count -ne 50){throw 'E7: tecnico compartido en fase 50.'}
    $base=[DateTime]::ParseExact($datos.baseFecha,'yyyy-MM-dd',[Globalization.CultureInfo]::InvariantCulture)
    foreach($old in @(Get-ChildItem (Join-Path $perf 'resultados') -Filter configuracion.json -Recurse -File)){
        if($old.Directory.Name -match '^e7-(50|100|150)-'){
            $prev=Get-Content $old.FullName -Raw|ConvertFrom-Json
            if(-not $prev.ficticio -and $prev.baseFecha){$siguiente=[DateTime]::ParseExact($prev.baseFecha,'yyyy-MM-dd',[Globalization.CultureInfo]::InvariantCulture).AddDays(20*([int]$prev.usuarios/50));if($siguiente -gt $base){$base=$siguiente}}
        }
    }
    $baseTexto=$base.ToString('yyyy-MM-dd');$ultimo=$base.AddDays(20*($Usuarios/50)-1).ToString('yyyy-MM-dd')
    $fin=[DateTime]::ParseExact('09:00','HH:mm',[Globalization.CultureInfo]::InvariantCulture).AddMinutes($datos.duracion).ToString('HH:mm')
    $ids=$tech -join ','
    $sql=@"
BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;
WITH r AS (SELECT t AS tid,g::date AS fecha FROM unnest(ARRAY[$ids]::bigint[]) t CROSS JOIN generate_series('$baseTexto'::date,'$ultimo'::date,'1 day') g)
SELECT json_build_object('reservas',(SELECT count(*) FROM r),'franjas',(SELECT count(*) FROM franja_tecnico f JOIN r ON f.tecnico_id=r.tid AND f.fecha=r.fecha),
'citas',(SELECT count(*) FROM cita c JOIN r ON c.tecnico_id=r.tid WHERE c.fecha_hora_inicio<r.fecha+TIME '$fin' AND c.fecha_hora_fin>r.fecha+TIME '08:00'),
'bloques',(SELECT count(*) FROM bloque_indisponibilidad b JOIN r ON b.tecnico_id=r.tid WHERE b.inicio<r.fecha+TIME '$fin' AND b.fin>r.fecha+TIME '08:00'),
'otrasTransaccionesActivas',(SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND pid<>pg_backend_pid() AND xact_start IS NOT NULL AND state<>'idle'));
ROLLBACK;
"@
    $libre=Consultar-E2 $co $a $sql|Out-String|ConvertFrom-Json
    $libre|ConvertTo-Json|Set-Content "$d/reservas-comprobadas.json" -Encoding UTF8
    if($libre.reservas -ne ($Usuarios*20) -or $libre.franjas -ne 0 -or $libre.citas -ne 0 -or $libre.bloques -ne 0 -or $libre.otrasTransaccionesActivas -ne 0){throw 'E7: reservas ocupadas o transaccion concurrente.'}
    $asignados=@(for($h=1;$h -le $Usuarios;$h++){$tech[($h-1)%50]})
    $cfg=[ordered]@{ficticio=$false;usuarios=$Usuarios;ciclos=20;tecnicos=$asignados;administradorId=[long]$datos.administradores[0].id;
        cuentaAdministrativaCompartida=$true;baseFecha=$baseTexto;duracionMinutos=[int]$datos.duracion;destino='http://localhost:18080';
        timeoutConexionMs=5000;timeoutRespuestaMs=10000;salida=$d;docker=(Get-Command docker.exe -CommandType Application|Select-Object -First 1).Source;
        dbId=$a.DbId;dbUsuario=$a.Usuario;rampa=20;validacionFicticia=$gate.evidencias}
    $cfg|ConvertTo-Json -Depth 8|Set-Content "$d/configuracion.json" -Encoding UTF8
    $reservas=@(for($h=1;$h -le $Usuarios;$h++){for($c=1;$c -le 20;$c++){
        [ordered]@{hilo=$h;ciclo=$c;tecnicoId=$asignados[$h-1];fecha=$base.AddDays(20*[math]::Floor(($h-1)/50)+$c-1).ToString('yyyy-MM-dd');
            inicioCreacion='08:00';finCreacion=([DateTime]::ParseExact('08:00','HH:mm',[Globalization.CultureInfo]::InvariantCulture).AddMinutes($datos.duracion).ToString('HH:mm'));inicioEdicion='09:00';finEdicion=$fin}
    }})
    $reservas|ConvertTo-Json -Depth 5|Set-Content "$d/reservas.json" -Encoding UTF8
    $antes=@(Huella-E7 $co $a);$antes|ConvertTo-Json|Set-Content "$d/integridad-antes.json" -Encoding UTF8
    @{completa=$true;HTTP=0;usuarios=$Usuarios;tecnicosDistintos=50;hilosPorTecnico=($Usuarios/50);reservasSinCruces=($Usuarios*20);limitesSinCambios=$true;cuentaAdministrativaCompartida=$true}|
        ConvertTo-Json|Set-Content "$d/preparacion-sin-http.json" -Encoding UTF8
    $etapa='credencial_administrativa'
    if($CredencialesJson){
        $cred=Get-Content -LiteralPath $CredencialesJson -Raw|ConvertFrom-Json
        if($cred.correo -cne $datos.administradores[0].correo){throw 'E7: credencial de otra identidad.'}
        $clave=[string]$cred.contrasena;$cred=$null
    }else{$clave=if($env:ADMIN_CONTRASENA){$env:ADMIN_CONTRASENA}else{$env:APPOINTECH_JMETER_ADMIN_PASSWORD}}
    if([string]::IsNullOrWhiteSpace($clave)){throw 'E7: credencial administrativa no configurada; detener sin HTTP.'}
    $secretos=@($clave)
    $etapa='autenticacion_externa'
    Add-Type -AssemblyName System.Net.Http
    $http=$true;$r=Invoke-E6Http POST '/api/auth/login' '' @{correo=$datos.administradores[0].correo;contrasena=$clave}
    $auth=$r.texto|ConvertFrom-Json
    if($r.codigo -ne 200 -or -not $auth.exito -or -not $auth.datos.token){throw 'E7: autenticacion fallida.'}
    $token=[string]$auth.datos.token;$secretos+=@($token);$r=$null;$auth=$null
    $partes=$token.Split('.');if($partes.Count -ne 3){throw 'E7: token no JWT.'}
    $b64=$partes[1].Replace('-','+').Replace('_','/');$b64=$b64.PadRight($b64.Length+(4-$b64.Length%4)%4,'=')
    $claims=[Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($b64))|ConvertFrom-Json
    if($claims.sub -cne $datos.administradores[0].correo -or $claims.rol -cne 'ADMINISTRADOR' -or $claims.id -ne $cfg.administradorId -or
        [long]$claims.exp -lt ([DateTimeOffset]::UtcNow.ToUnixTimeSeconds()+3700)){throw 'E7: identidad/rol/vigencia JWT no cumplen.'}
    # GET protegido comprueba firma y rol mediante el servidor; la decodificacion anterior no basta.
    $ruta='/api/admin/tecnicos/'+$tech[0]+"/franjas?desde=$baseTexto&hasta=$baseTexto"
    $control=Invoke-E6Http GET $ruta $token $null
    if($control.codigo -ne 200 -or $control.texto.Trim() -ne '[]'){throw 'E7: control de rol/reserva fallo.'}
    @{loginHTTP=200;controlRolHTTP=200;cuentaCompartida=$true;usuarios=$Usuarios;fueraDelJTL=$true}|ConvertTo-Json|Set-Content "$d/autenticacion.json" -Encoding UTF8
    $cfg.token=$token
    $etapa='paquete_privado'
    $paquete=Escribir-E6PaquetePrivado $cfg "$d/errores-seguros.jsonl"
    Comparar-E6Estado $estado @(Estado-E6 (Confirmar-E2Aislamiento $co 18080))
    $etapa='carga_'+$Usuarios
    $argsJava=Argumentos-E7Java $JMeterHome (Join-Path $PSScriptRoot 'e7-crud.jmx') $d $paquete $Usuarios 20 -Dashboard
    try{
        $muestreo=Iniciar-E7Recursos $a $d;Esperar-MuestreoPerf $muestreo
        $lanzado=$true;Invocar-E7Java (Java-E7) $argsJava $d 3600 $secretos
    }finally{
        # El finally exterior conserva captura durante recuperacion.
    }
}catch{
    $fallo=$true;$problemas+=Guardar-E6ErrorSeguro $_ $etapa "$d/errores-seguros.jsonl" $secretos
}finally{

    if($a -and $cfg -and $lanzado){
        try{
            # No se repite ninguna escritura. Se espera el cierre y se observa hasta 60 s si quedan residuos.
            $lecturas=@();$restantes=@();$timer=[Diagnostics.Stopwatch]::StartNew()
            do{
                $sql="BEGIN READ ONLY; SELECT coalesce(json_agg(json_build_object('id',id::text,'tecnicoId',tecnico_id,'fecha',fecha,'horaInicio',hora_inicio,'horaFin',hora_fin,'propietario',creada_por,'creadaEn',creada_en) ORDER BY id),'[]'::json) FROM franja_tecnico WHERE tecnico_id IN ($ids) AND fecha BETWEEN '$baseTexto' AND '$ultimo'; ROLLBACK;"
                $v=Consultar-E2 $co $a $sql|Out-String|ConvertFrom-Json
                $restantes=@(foreach($row in $v){$row});$lecturas+=@{epochMs=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();filas=$restantes.Count}
                if($restantes.Count -eq 0 -and $lecturas.Count -ge 2){break}
                if($timer.Elapsed.TotalSeconds -lt 55){Start-Sleep -Seconds 5}else{break}
            }while($timer.Elapsed.TotalSeconds -lt 60)
            $inciertas=@()
            foreach($file in @(Get-ChildItem $d -Filter 'hilo-*.jsonl')){
                foreach($line in [IO.File]::ReadAllLines($file.FullName)){if($line){$e=$line|ConvertFrom-Json;if($e.evento -eq 'resultado' -and $e.incierta){$inciertas+=@($e)}}}
            }
            @{lecturas=$lecturas;filasCohorte=$restantes;residuos=$restantes.Count;escriturasInciertas=$inciertas;
                incertidumbreNoResuelta=($inciertas.Count -gt 0);reintentosEscritura=0;limpiezaEjecutada=$false;
                nota='Ausencia final no demuestra por si sola rollback de una escritura incierta. No se borran residuos.'}|
                ConvertTo-Json -Depth 10|Set-Content "$d/conciliacion-final.json" -Encoding UTF8
            if($restantes.Count -gt 0 -or $inciertas.Count -gt 0){$fallo=$true}
            $rec=@()
            for($j=1;$j -le 3;$j++){
                Comparar-E6Estado $estado @(Estado-E6 (Confirmar-E2Aislamiento $co 18080))
                $r=Invoke-E6Http GET $ruta $token $null
                $ok=$r.codigo -eq 200 -and $r.texto.Trim() -eq '[]'
                $rec+=@{intento=$j;http=$r.codigo;contenido=$ok;duracionMs=$r.ms}
                if(-not $ok){$fallo=$true}
            }
            $rec|ConvertTo-Json|Set-Content "$d/recuperacion.json" -Encoding UTF8
            Estado-E6 $a|ConvertTo-Json|Set-Content "$d/estado-despues.json" -Encoding UTF8
        }catch{$fallo=$true;$problemas+=Guardar-E6ErrorSeguro $_ 'conciliacion_recuperacion' "$d/errores-seguros.jsonl" $secretos}
    }
    if($a -and $antes){
        try{
            $despues=@(Huella-E7 $co $a);$despues|ConvertTo-Json|Set-Content "$d/integridad-despues.json" -Encoding UTF8
            $igual=($antes|ConvertTo-Json -Compress) -ceq ($despues|ConvertTo-Json -Compress)
            @{iguales=$igual;excluidos=@('log_sistema','flyway_schema_history');tablas=$antes.Count}|ConvertTo-Json|Set-Content "$d/integridad.json" -Encoding UTF8
            if(-not $igual){$fallo=$true}
        }catch{$fallo=$true;$problemas+=Guardar-E6ErrorSeguro $_ 'integridad' "$d/errores-seguros.jsonl" $secretos}
    }
    if($muestreo){try{Detener-E7Recursos $muestreo|ConvertTo-Json|Set-Content "$d/validacion-recursos.json" -Encoding UTF8}catch{$fallo=$true;$problemas+=Guardar-E6ErrorSeguro $_ 'cierre_recursos' "$d/errores-seguros.jsonl" $secretos}}
    if($paquete){try{Remove-Item -LiteralPath $paquete -Force;Remove-Item -LiteralPath (Split-Path $paquete) -Force}catch{$fallo=$true;$problemas+=Guardar-E6ErrorSeguro $_ 'paquete_privado' "$d/errores-seguros.jsonl" $secretos}}
    if($lanzado -and $a){
        try{
            $logs=Invoke-E2Docker logs --since $([DateTime]::UtcNow.AddHours(-2).ToString('o')) $a.BackendId|Out-String
            foreach($s in $secretos){if($s){$logs=$logs.Replace($s,'[REDACTADO]')}}
            $logs=$logs -replace 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+','[JWT_REDACTADO]'
            [IO.File]::WriteAllText("$d/backend.log",$logs,[Text.UTF8Encoding]::new($false))
        }catch{$fallo=$true;$problemas+=Guardar-E6ErrorSeguro $_ 'logs' "$d/errores-seguros.jsonl" $secretos}
    }
    $exposiciones=0
    foreach($file in @(Get-ChildItem $d -Recurse -File|Where-Object Extension -in @('.log','.jtl','.json','.jsonl','.html','.js','.css','.csv','.txt','.md'))){
        $texto=[IO.File]::ReadAllText($file.FullName);$nuevo=$texto
        foreach($s in $secretos){if($s -and $nuevo.Contains($s)){$exposiciones++;$nuevo=$nuevo.Replace($s,'[REDACTADO]')}}
        if($nuevo -match 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+'){$exposiciones++;$nuevo=$nuevo -replace 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+','[JWT_REDACTADO]'}
        if($nuevo -cne $texto){[IO.File]::WriteAllText($file.FullName,$nuevo,[Text.UTF8Encoding]::new($false));$fallo=$true}
    }
    @{exposicionesDetectadasYRedactadas=$exposiciones;artefactosSinTokens=$true}|ConvertTo-Json|Set-Content "$d/privacidad.json" -Encoding UTF8
    @{fallo=$fallo;etapa=$etapa;HTTPRealIntentado=$http;JMeterLanzado=$lanzado;problemas=$problemas;progresionDetenida=$true;
        faseUnica=$Usuarios;cuentaAdministrativaCompartida=$true;paqueteEliminado=($null -eq $paquete -or -not(Test-Path -LiteralPath $paquete))}|
        ConvertTo-Json -Depth 9|Set-Content "$d/diagnostico.json" -Encoding UTF8
    if($mutex){$mutex.Dispose()};$clave=$null;$token=$null;$secretos=@();if($cfg){$cfg.token=$null}
}
if($lanzado -and (Test-Path "$d/motor-final.json")){
    & node.exe (Join-Path $PSScriptRoot 'analizar-e7.js') $d
    if($LASTEXITCODE -ne 0){$fallo=$true}
    if(Test-Path "$d/analisis-e7.json"){
        $analisis=Get-Content "$d/analisis-e7.json" -Raw|ConvertFrom-Json
        if($analisis.aborto -or $analisis.ciclosCompletos -ne ($Usuarios*20) -or $analisis.checkpointsInvalidos -gt 0){$fallo=$true}
    }
}
if($fallo){throw "E7 detenido; no repetir la carga ni progresar. Evidencia: $d"}
Write-Host "E7 $Usuarios finalizado. Detener; no ejecutar otra fase ni E8: $d"
