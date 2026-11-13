# Funciones sin efectos al cargarse. Todas las operaciones requieren aislamiento acreditado.
. (Join-Path $PSScriptRoot 'errores-e6.ps1')
function Leer-E6Datos($Compose,$Aislamiento,[int]$Usuarios,[string]$Carpeta) {
    $sql=(Get-Content (Join-Path $PSScriptRoot 'e6-expectativas.sql') -Raw).Replace('__N__',[string]$Usuarios)
    $filas=@(Consultar-E2 $Compose $Aislamiento $sql | ForEach-Object {ConvertFrom-Json $_})
    if($filas.Count -ne $Usuarios){throw 'E6: faltan clientes sinteticos con citas y notificaciones.'}
    $ids=@{}; $i=0
    foreach($f in $filas){
        $i++
        if($f.ordinal -ne $i -or $f.rol -cne 'CLIENTE' -or $f.correo -cnotmatch '^cliente[1-9][0-9]*@perf\.test$' -or
            $ids.ContainsKey([string]$f.usuarioId) -or @($f.citas).Count -lt 1 -or @($f.notificaciones).Count -lt 1 -or
            @($f.notificaciones).Count -ne [math]::Min(20,[long]$f.totalNotificaciones)){throw 'E6: cohorte vacia, duplicada o inconsistente.'}
        $ids[[string]$f.usuarioId]=$true
    }
    $sql=@'
BEGIN READ ONLY;
SELECT coalesce(json_agg(json_build_object('id',id,'nombre',nombre,'descripcion',descripcion) ORDER BY id),'[]'::json) FROM especialidad WHERE activo;
ROLLBACK;
'@
    $catalogo=Consultar-E2 $Compose $Aislamiento $sql | Out-String | ConvertFrom-Json
    $especialidades=@(foreach($e in $catalogo){$e})
    if(-not $especialidades.Count){throw 'E6: catalogo activo vacio.'}
    [pscustomobject]@{identidades=$filas;especialidades=$especialidades}
}
function Huella-E6($Compose,$Aislamiento){
    $partes=@(foreach($tabla in @('usuario','cliente','tecnico','especialidad','cita','cita_historial_estado','pago','notificacion')){
        "SELECT '$tabla' AS tabla,count(*) AS cantidad,md5(coalesce(string_agg(md5(row_to_json(t)::text),'' ORDER BY id),'')) AS huella FROM $tabla t"
    })
    $sql="BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ READ ONLY;`n"+
        'SELECT json_agg(s ORDER BY tabla) FROM ('+($partes -join ' UNION ALL ')+")s;`nROLLBACK;"
    $filas=Consultar-E2 $Compose $Aislamiento $sql | Out-String | ConvertFrom-Json
    foreach($fila in $filas){$fila}
}
function Estado-E6($Aislamiento){
    foreach($c in (Invoke-E2Docker inspect $Aislamiento.BackendId $Aislamiento.DbId | Out-String | ConvertFrom-Json)){
        [pscustomobject]@{id=$c.Id;servicio=$c.Config.Labels.'com.docker.compose.service';inicio=$c.State.StartedAt;
            reinicios=$c.RestartCount;oom=$c.State.OOMKilled;salud=$c.State.Health.Status;activo=$c.State.Running;
            memoria=$c.HostConfig.Memory;nanoCpus=$c.HostConfig.NanoCpus;cuota=$c.HostConfig.CpuQuota;periodo=$c.HostConfig.CpuPeriod;cpuSet=$c.HostConfig.CpusetCpus}
    }
}
function Comparar-E6Estado($Antes,$Despues){
    if(@($Despues).Count -ne 2){throw 'E6: faltan servicios.'}
    foreach($d in $Despues){
        $a=@($Antes|Where-Object servicio -eq $d.servicio)
        if($a.Count -ne 1 -or $d.oom -or -not $d.activo -or $d.salud -ne 'healthy'){throw 'E6: OOM o perdida de salud.'}
        foreach($campo in @('id','inicio','reinicios','memoria','nanoCpus','cuota','periodo','cpuSet')){
            if($a[0].$campo -cne $d.$campo){throw 'E6: reinicio, recreacion o cambio de limites.'}
        }
    }
}
function Invoke-E6Http([string]$Metodo,[string]$Ruta,[string]$Token,$Body,[int]$TimeoutMs=10000){
    # No registrar cuerpos, cabeceras, excepciones que incluyan datos ni valores de tokens.
    $handler=New-Object Net.Http.HttpClientHandler
    $handler.UseProxy=$false;$handler.AllowAutoRedirect=$false
    $http=New-Object Net.Http.HttpClient($handler);$http.Timeout=[TimeSpan]::FromMilliseconds($TimeoutMs)
    $pet=New-Object Net.Http.HttpRequestMessage([Net.Http.HttpMethod]::new($Metodo),('http://localhost:18080'+$Ruta))
    $resp=$null;$reloj=[Diagnostics.Stopwatch]::StartNew()
    try{
        if($Token){$pet.Headers.Authorization=[Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer',$Token)}
        if($null -ne $Body){$pet.Content=[Net.Http.StringContent]::new(($Body|ConvertTo-Json -Compress),[Text.Encoding]::UTF8,'application/json')}
        $resp=$http.SendAsync($pet).GetAwaiter().GetResult()
        $texto=$resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $total=$null;if($resp.Headers.Contains('X-Total-Count')){$total=@($resp.Headers.GetValues('X-Total-Count')) -join ','}
        [pscustomobject]@{codigo=[int]$resp.StatusCode;texto=$texto;total=$total;ms=$reloj.ElapsedMilliseconds}
    }catch{throw}
    finally{if($resp){$resp.Dispose()};$pet.Dispose();$http.Dispose();$handler.Dispose()}
}
function Confirmar-E6Respuesta($R,$Esperado,[Nullable[long]]$Total){
    if($R.codigo -ne 200 -or -not $R.texto.TrimStart().StartsWith('[')){throw 'E6: HTTP o arreglo inesperado en preparacion.'}
    $json=ConvertFrom-Json -InputObject $R.texto
    $a=@(foreach($elemento in $json){$elemento})
    if($a.Count -ne @($Esperado).Count -or $a.Count -eq 0){throw 'E6: respuesta vacia o cantidad inesperada.'}
    if((($a|ForEach-Object id) -join ',') -cne (($Esperado|ForEach-Object id) -join ',')){throw 'E6: IDs o pertenencia inesperados.'}
    if($null -ne $Total -and $R.total -cne [string]$Total){throw 'E6: total paginado incorrecto.'}
}
function Preparar-E6Tokens($Datos,$Csv,[string]$Registro){
    $cred=@{}
    foreach($c in $Csv){if($cred.ContainsKey($c.correo)){throw 'E6: credenciales duplicadas.'};$cred[$c.correo]=$c.contrasena}
    'ordinal,operacion,duracionMs,resultado' | Set-Content $Registro -Encoding UTF8
    foreach($f in $Datos.identidades){
        if(-not $cred.ContainsKey($f.correo) -or [string]::IsNullOrWhiteSpace($cred[$f.correo])){throw 'E6: falta credencial sintetica seleccionada.'}
        $op='login';$sw=[Diagnostics.Stopwatch]::StartNew()
        try{
            $r=Invoke-E6Http POST '/api/auth/login' '' @{correo=$f.correo;contrasena=$cred[$f.correo]}
            $j=ConvertFrom-Json -InputObject $r.texto
            if($r.codigo -ne 200 -or $j.exito -isnot [bool] -or -not $j.exito -or $j.datos.token -isnot [string] -or [string]::IsNullOrWhiteSpace($j.datos.token)){throw 'Contrato'}
            $token=$j.datos.token
            "$($f.ordinal),login,$($sw.ElapsedMilliseconds),ok"|Add-Content $Registro -Encoding UTF8
            $op='identidad_notificaciones';$sw.Restart()
            $r=Invoke-E6Http GET '/api/notificaciones?page=0&size=20' $token $null
            Confirmar-E6Respuesta $r $f.notificaciones $f.totalNotificaciones
            "$($f.ordinal),$op,$($sw.ElapsedMilliseconds),ok"|Add-Content $Registro -Encoding UTF8
            $op='rol_cliente_historial';$sw.Restart()
            $r=Invoke-E6Http GET '/api/cliente/citas' $token $null
            Confirmar-E6Respuesta $r $f.citas $null
            "$($f.ordinal),$op,$($sw.ElapsedMilliseconds),ok"|Add-Content $Registro -Encoding UTF8
            # El servidor valida firma/rol y devuelve IDs propios cotejados con SQL, no solo JWT decodificado.
            $f|Add-Member -NotePropertyName token -NotePropertyValue $token
        }catch{
            $original=$_
            "$($f.ordinal),$op,$($sw.ElapsedMilliseconds),fallo"|Add-Content $Registro -Encoding UTF8
            Guardar-E6ErrorSeguro $original ('preparacion_'+$op) ($Registro+'.errores.jsonl') @($token,$cred[$f.correo])|Out-Null
            throw
        }finally{$j=$null;$r=$null;$token=$null}
    }
}
function Controles-E6($Compose,$Aislamiento,$Antes,$Datos,[string]$Archivo,[bool]$ExigirNormal){
    'intento,inicioEpochMs,salud,httpIntentado,http,tipo,resultado,duracionMs,consecutivos'|Set-Content $Archivo -Encoding UTF8
    $sw=[Diagnostics.Stopwatch]::StartNew();$ok=0;$i=0
    while($sw.ElapsedMilliseconds -lt 60000){
        $i++;$t=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds();$ms=$null;$resultado='fallo'
        $sano=$false;$httpIntentado=$false;$codigo=$null;$tipo='Salud';$controlReloj=$null
        try{
            $a=Confirmar-E2Aislamiento $Compose 18080
            if($a.BackendId -ne $Aislamiento.BackendId -or $a.DbId -ne $Aislamiento.DbId){throw 'E6: cambio de destino.'}
            Comparar-E6Estado $Antes @(Estado-E6 $a)
            $sano=$true;$httpIntentado=$true;$tipo='Transporte';$controlReloj=[Diagnostics.Stopwatch]::StartNew()
            $f=$Datos.identidades[0]
            $r=Invoke-E6Http GET '/api/notificaciones?page=0&size=20' $f.token $null ([int][math]::Min(10000,[math]::Max(1,60000-$sw.ElapsedMilliseconds)))
            $ms=$r.ms;$codigo=$r.codigo;$tipo=if($codigo -eq 200){'Contenido'}else{'HTTP'}
            Confirmar-E6Respuesta $r $f.notificaciones $f.totalNotificaciones
            if($ExigirNormal -and $ms -gt 800){$tipo='Duracion';throw 'Lento'}
            $resultado='ok';$tipo='ninguno';$ok++
        }catch{
            $original=$_
            $ok=0;if($httpIntentado -and $null -eq $ms -and $controlReloj){$ms=$controlReloj.ElapsedMilliseconds}
            Guardar-E6ErrorSeguro $original ('control_'+$tipo) ($Archivo+'.errores.jsonl') @($f.token)|Out-Null
        }
        "$i,$t,$sano,$httpIntentado,$codigo,$tipo,$resultado,$ms,$ok"|Add-Content $Archivo -Encoding UTF8
        if($ok -ge 3){return}
        if($sw.ElapsedMilliseconds -lt 60000){Start-Sleep -Milliseconds ([int][math]::Min(5000,60000-$sw.ElapsedMilliseconds))}
    }
    throw 'E6: no se confirmaron tres controles consecutivos; detener progresion.'
}
function Escribir-E6PaquetePrivado {
    [CmdletBinding()]
    param($Datos,[string]$ArchivoErrores)
    $carpeta=Join-Path ([IO.Path]::GetTempPath()) ('appointech-e6-'+[Guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $carpeta|Out-Null
    try{
        $acl=New-Object Security.AccessControl.DirectorySecurity
        $acl.SetAccessRuleProtection($true,$false)
        $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User
        $acl.SetOwner($sid)
        $regla=New-Object Security.AccessControl.FileSystemAccessRule($sid,'FullControl','ContainerInherit,ObjectInherit','None','Allow')
        $acl.AddAccessRule($regla);Set-Acl -LiteralPath $carpeta -AclObject $acl
        $archivo=Join-Path $carpeta 'paquete.json'
        [IO.File]::WriteAllText($archivo,($Datos|ConvertTo-Json -Depth 16 -Compress),[Text.UTF8Encoding]::new($false))
        $archivo
    }catch{
        $original=$_
        # Conservar el ErrorRecord de creacion, aunque la limpieza tambien falle.
        try{
            $archivoFallido=Join-Path $carpeta 'paquete.json'
            if(Test-Path -LiteralPath $archivoFallido){Remove-Item -LiteralPath $archivoFallido -Force}
            if(Test-Path -LiteralPath $carpeta){Remove-Item -LiteralPath $carpeta -Force}
        }catch{
            if($ArchivoErrores){Guardar-E6ErrorSeguro $_ 'limpieza_paquete_tras_error' $ArchivoErrores @($Datos.identidades|ForEach-Object token)|Out-Null}
        }
        $PSCmdlet.ThrowTerminatingError($original)
    }
}
