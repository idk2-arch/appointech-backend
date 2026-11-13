function Confirmar-E4Entorno($Compose,[int]$Puerto,[string]$Directorio) {
    $a=Confirmar-E2Aislamiento $Compose $Puerto
    $objetos=Invoke-E2Docker inspect $a.BackendId|Out-String|ConvertFrom-Json
    $c=@($objetos)[0];$envMapa=@{}
    foreach($v in $c.Config.Env){$p=$v -split '=',2;$envMapa[$p[0]]=$p[1]}
    foreach($n in @('NOTIFICACIONES_URL','ORS_API_KEY','GROQ_API_KEY')){
        if(-not $envMapa.ContainsKey($n) -or $envMapa[$n]){throw 'Proveedor externo no desactivado explicitamente.'}
    }
    foreach($n in @('SPRING_APPLICATION_JSON','SPRING_CONFIG_LOCATION','SPRING_CONFIG_ADDITIONAL_LOCATION','SPRING_CONFIG_NAME','JDK_JAVA_OPTIONS','_JAVA_OPTIONS')){
        if($envMapa[$n]){throw 'Override de configuracion no permitido en E4.'}
    }
    if($envMapa.JAVA_TOOL_OPTIONS -and $envMapa.JAVA_TOOL_OPTIONS -cne '-Duser.timezone=America/Bogota'){throw 'Opciones Java no verificadas.'}
    if(($envMapa.PAGOS_PASARELA -and $envMapa.PAGOS_PASARELA -ne 'simulada') -or
        ($envMapa.MAPAS_PROVEEDOR -and $envMapa.MAPAS_PROVEEDOR -ne 'haversine')){throw 'Proveedor efectivo inesperado.'}
    if(@($c.Config.Entrypoint+$c.Config.Cmd|Where-Object {$_ -match '^--|^-D' }).Count -or @($c.Mounts).Count){throw 'Argumentos o montajes adicionales no verificados.'}
    Invoke-E2Docker exec $a.BackendId sh -c 'test ! -d /app/config && test ! -f /app/application.properties && test ! -f /app/application-perf.properties'|Out-Null
    $jar=Join-Path (Split-Path -Parent $Directorio) ('e4-inspeccion-'+[Guid]::NewGuid().ToString('N')+'.jar')
    # El jar solo contiene codigo/configuracion empaquetada, nunca variables reales del contenedor.
    Invoke-E2Docker cp "$($a.BackendId):/app/app.jar" $jar|Out-Null
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip=[IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $entrada=$zip.GetEntry('BOOT-INF/classes/application-perf.properties')
        if(-not $entrada){throw 'No existe perfil perf empaquetado.'}
        $r=New-Object IO.StreamReader($entrada.Open())
        try{$texto=$r.ReadToEnd()}finally{$r.Dispose()}
        if($texto -notmatch '(?m)^mapas.proveedor=haversine\s*$' -or $texto -notmatch '(?m)^pagos.pasarela=simulada\s*$' -or
            $texto -notmatch '(?m)^groq.api-key=\s*$'){throw 'Perfil empaquetado no aislado de proveedores.'}
    } finally {$zip.Dispose()}
    [ordered]@{destino="http://localhost:$Puerto";proyecto='appointech-perf';base=$a.Base;red=$a.Red;
        backendId=$a.BackendId;dbId=$a.DbId;perfilEmpaquetadoSha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash;
        correoDesactivado=$true;pasarela='simulada';mapas='haversine';iaDesactivada=$true;solicitudesHTTP=0}|
        ConvertTo-Json|Set-Content (Join-Path $Directorio 'aislamiento.json') -Encoding UTF8
    $a
}

function Leer-E4Cuentas($Compose,$Aislamiento,[int]$Usuarios){
    $sql=@'
BEGIN READ ONLY;
SELECT json_build_object('ordinal',s.i,'correo',u.correo,'usuarioId',u.id,'clienteId',c.id,
 'completo',u.telefono IS NOT NULL AND u.telefono<>'' AND u.tipo_documento IS NOT NULL AND u.numero_documento IS NOT NULL AND c.direccion IS NOT NULL AND c.direccion<>'')
FROM generate_series(1,__N__) s(i) JOIN usuario u ON u.correo='cliente'||s.i||'@perf.test'
JOIN cliente c ON c.usuario_id=u.id WHERE u.activo AND u.rol='CLIENTE' AND u.contrasena IS NOT NULL ORDER BY s.i;
ROLLBACK;
'@.Replace('__N__',[string]$Usuarios)
    $filas=@(Consultar-E2 $Compose $Aislamiento $sql|ForEach-Object{ConvertFrom-Json $_})
    if($filas.Count -ne $Usuarios -or @($filas|Where-Object {-not $_.completo}).Count){throw 'Las cuentas CLIENTE perf no estan completas.'}
    for($i=0;$i -lt $Usuarios;$i++){if($filas[$i].ordinal -ne $i+1){throw 'Asignacion de identidad incompleta.'}}
    $filas
}
function Leer-E4Especialidad($Compose,$Aislamiento){
    $sql=@'
BEGIN READ ONLY;
SELECT json_build_object('id',e.id,'nombre',e.nombre,'costo',e.costo_referencia,'tecnicosElegibles',
 (SELECT count(DISTINCT t.id) FROM tecnico_especialidad te JOIN tecnico t ON t.id=te.tecnico_id JOIN usuario u ON u.id=t.usuario_id WHERE te.especialidad_id=e.id AND u.activo))
FROM especialidad e WHERE e.activo AND e.costo_referencia>0 AND EXISTS
 (SELECT 1 FROM tecnico_especialidad te JOIN tecnico t ON t.id=te.tecnico_id JOIN usuario u ON u.id=t.usuario_id WHERE te.especialidad_id=e.id AND u.activo)
ORDER BY e.id LIMIT 1;
ROLLBACK;
'@
    $filas=@(Consultar-E2 $Compose $Aislamiento $sql|ForEach-Object{ConvertFrom-Json $_})
    if($filas.Count -ne 1 -or $filas[0].tecnicosElegibles -le 0){throw 'No hay especialidad activa con cotizacion y tecnicos elegibles.'}
    $filas[0]
}
function Leer-E4Estado($Compose,$Aislamiento,[string]$Prefijo){
    if($Prefijo -notmatch '^E4-[a-f0-9]{32}-$'){throw 'Prefijo invalido.'}
    $sql=@'
BEGIN READ ONLY;
SELECT json_build_object('usuarios',(SELECT count(*) FROM usuario),'clientes',(SELECT count(*) FROM cliente),
 'citas',(SELECT count(*) FROM cita),'historial',(SELECT count(*) FROM cita_historial_estado),
 'pagos',(SELECT count(*) FROM pago),'notificaciones',(SELECT count(*) FROM notificacion),'logs',(SELECT count(*) FROM log_sistema),
 'huellaUsuarios',(SELECT md5(coalesce(string_agg(row_to_json(u)::text,'|' ORDER BY u.id),'')) FROM usuario u),
 'huellaClientes',(SELECT md5(coalesce(string_agg(row_to_json(c)::text,'|' ORDER BY c.id),'')) FROM cliente c),
 'huellaCitasPrevias',(SELECT md5(coalesce(string_agg(row_to_json(c)::text,'|' ORDER BY c.id),'')) FROM cita c WHERE left(c.descripcion_problema,length('__P__'))<>'__P__'));
ROLLBACK;
'@.Replace('__P__',$Prefijo)
    Consultar-E2 $Compose $Aislamiento $sql|ConvertFrom-Json
}
function Leer-E4Cohorte($Compose,$Aislamiento,[string]$EsperadosJson){
    if($EsperadosJson.Contains("'")){throw 'Expectativas no admitidas.'}
    $sql=@'
BEGIN READ ONLY;
WITH esperados AS (SELECT * FROM json_to_recordset('__E__'::json) AS e(marca text,hilo int,iteracion int,"usuarioId" bigint,"clienteId" bigint,"especialidadId" bigint,costo numeric))
SELECT json_build_object('marca',e.marca,'hilo',e.hilo,'iteracion',e.iteracion,
 'citas',(SELECT count(*) FROM cita c WHERE c.descripcion_problema=e.marca),
 'id',(SELECT min(c.id) FROM cita c WHERE c.descripcion_problema=e.marca),
 'propietarioCorrecto',(SELECT coalesce(bool_and(c.cliente_id=e."clienteId" AND u.id=e."usuarioId"),false) FROM cita c JOIN cliente cl ON cl.id=c.cliente_id JOIN usuario u ON u.id=cl.usuario_id WHERE c.descripcion_problema=e.marca),
 'camposCorrectos',(SELECT coalesce(bool_and(c.especialidad_id=e."especialidadId" AND c.direccion_servicio='Calle 100 # 10-20 Perf' AND c.latitud=4.3365 AND c.longitud=-74.3638 AND c.tecnico_id IS NULL AND c.fecha_hora_inicio IS NULL AND c.fecha_hora_fin IS NULL AND NOT c.agendamiento_manual AND c.motivo_solicitud_especial IS NULL),false) FROM cita c WHERE c.descripcion_problema=e.marca),
 'estado',(SELECT min(c.estado::text) FROM cita c WHERE c.descripcion_problema=e.marca),
 'historial',(SELECT count(*) FROM cita_historial_estado h JOIN cita c ON c.id=h.cita_id WHERE c.descripcion_problema=e.marca),
 'historialCorrecto',(SELECT count(*) FROM cita_historial_estado h JOIN cita c ON c.id=h.cita_id JOIN cliente cl ON cl.id=c.cliente_id JOIN usuario u ON u.id=cl.usuario_id WHERE c.descripcion_problema=e.marca AND h.estado_anterior='PENDIENTE' AND h.estado_nuevo='CANCELADA' AND h.motivo=e.marca AND h.actor=u.correo),
 'pagos',(SELECT count(*) FROM pago p JOIN cita c ON c.id=p.cita_id WHERE c.descripcion_problema=e.marca),
 'pagoCorrecto',(SELECT coalesce(bool_and(p.estado='PENDIENTE' AND p.monto_cotizado=e.costo AND p.monto_final IS NULL AND p.metodo IS NULL AND p.referencia_externa IS NULL AND p.id_transaccion_externa IS NULL AND p.pagado_en IS NULL AND p.intentos_pago=0),false) FROM pago p JOIN cita c ON c.id=p.cita_id WHERE c.descripcion_problema=e.marca),
 'notificacionRecibida',(SELECT count(*) FROM notificacion n JOIN cita c ON c.id=n.cita_id WHERE c.descripcion_problema=e.marca AND n.usuario_id=e."usuarioId" AND n.tipo='SOLICITUD_RECIBIDA' AND NOT n.leida),
 'notificacionCancelada',(SELECT count(*) FROM notificacion n JOIN cita c ON c.id=n.cita_id WHERE c.descripcion_problema=e.marca AND n.usuario_id=e."usuarioId" AND n.tipo='CITA_CANCELADA' AND NOT n.leida),
 'otrasNotificaciones',(SELECT count(*) FROM notificacion n JOIN cita c ON c.id=n.cita_id WHERE c.descripcion_problema=e.marca AND (n.usuario_id<>e."usuarioId" OR n.tipo NOT IN ('SOLICITUD_RECIBIDA','CITA_CANCELADA'))),
 'comprobantes',(SELECT count(*) FROM comprobante_pago cp JOIN pago p ON p.id=cp.pago_id JOIN cita c ON c.id=p.cita_id WHERE c.descripcion_problema=e.marca))
FROM esperados e ORDER BY e.hilo,e.iteracion;
ROLLBACK;
'@.Replace('__E__',$EsperadosJson)
    Consultar-E2 $Compose $Aislamiento $sql|ForEach-Object{ConvertFrom-Json $_}
}
