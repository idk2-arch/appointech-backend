[CmdletBinding()]
param(
    [string]$JMeterBat,
    [ValidateSet('localhost', '127.0.0.1')][string]$Servidor = 'localhost',
    [ValidateRange(1, 65535)][int]$Puerto = 18080,
    [ValidateSet(20, 40)][int]$Usuarios = 20,
    [ValidateRange(0, 2147483647)][int]$Rampa = 10,
    [ValidateRange(1, 2147483647)][int]$TimeoutConexionMs = 5000,
    [ValidateRange(1, 2147483647)][int]$TimeoutRespuestaMs = 10000
)
$ErrorActionPreference = 'Stop'
if ($Puerto -eq 8080) { throw 'El puerto local 8080 se reserva para desarrollo.' }
if (-not $JMeterBat) {
    $JMeterBat = (Get-Command jmeter.bat -CommandType Application -ErrorAction SilentlyContinue |
        Select-Object -First 1).Source
}
if (-not $JMeterBat -or -not (Test-Path -LiteralPath $JMeterBat -PathType Leaf) -or
    [IO.Path]::GetFileName($JMeterBat) -ne 'jmeter.bat') { throw 'Indique una ruta valida con -JMeterBat.' }
$jmeterJar = Join-Path (Split-Path -Parent (Resolve-Path -LiteralPath $JMeterBat).ProviderPath) 'ApacheJMeter.jar'
if (-not (Test-Path -LiteralPath $jmeterJar -PathType Leaf)) { throw 'No se encuentra ApacheJMeter.jar.' }
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' }
if (-not $java -or -not (Test-Path -LiteralPath $java -PathType Leaf)) {
    $java = (Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1).Source
}
if (-not $java) { throw 'No se encuentra java.exe en JAVA_HOME ni PATH.' }
$null = Get-Command docker -CommandType Application -ErrorAction Stop
$perf = Split-Path -Parent $PSScriptRoot
$plan = Join-Path $PSScriptRoot 'e3-registro.jmx'
$archivoCompose = Join-Path $perf 'docker-compose.perf.yml'
$archivoEnv = Join-Path $perf '.env.perf'
foreach ($ruta in @($plan, $archivoCompose, $archivoEnv)) {
    if (-not (Test-Path -LiteralPath $ruta -PathType Leaf)) { throw "Falta una dependencia de E3 en $ruta." }
}
[xml]$xml = Get-Content -LiteralPath $plan -Raw
if ($xml.DocumentElement.Name -ne 'jmeterTestPlan') { throw 'XML del plan E3 incorrecto.' }
. (Join-Path $PSScriptRoot 'preparar-e2.ps1')
. (Join-Path $PSScriptRoot 'recursos-perf.ps1')
$compose = @('compose','--env-file',$archivoEnv,'-f',$archivoCompose)
$aislamiento = Confirmar-E2Aislamiento $compose $Puerto
$destino = "http://${Servidor}:$Puerto"
$id = [Guid]::NewGuid().ToString('N')
$prefijo = "APPOINTECH_E3_$($id)_"
$resultados = Join-Path $perf ("resultados/e3-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + "-$id")
New-Item -ItemType Directory -Path $resultados | Out-Null
$jtl = Join-Path $resultados 'e3.jtl'
$log = Join-Path $resultados 'jmeter.log'
$html = Join-Path $resultados 'reporte'
$esperadas = [long]$Usuarios
$propiedades = [ordered]@{
    protocolo='http'; host=$Servidor; puerto=$Puerto; usuarios=$Usuarios
    rampa=$Rampa; loops=1; umbralRegistroMs=1500
    timeoutConexionMs=$TimeoutConexionMs; timeoutRespuestaMs=$TimeoutRespuestaMs
    identidadesEnv=$prefijo; planE3=$plan
    preparacionGenerador=(Join-Path $resultados 'preparacion-generador.json')
    diagnosticoGenerador=(Join-Path $resultados 'temporizacion-generador.csv')
}
$guardar = @(
    'timestamp_format=ms', 'default_delimiter=,', 'print_field_names=true',
    'time=true', 'label=true', 'response_code=true', 'response_message=true',
    'thread_name=true', 'successful=true', 'assertion_results_failure_message=true',
    'bytes=true', 'sent_bytes=true', 'thread_counts=true', 'url=true',
    'latency=true', 'idle_time=true', 'connect_time=true',
    'output_format=csv', 'assertion_results=none', 'response_data=false',
    'response_data.on_error=false', 'samplerData=false', 'requestHeaders=false',
    'responseHeaders=false', 'subresults=false', 'encoding=false', 'data_type=false',
    'filename=false', 'hostname=false', 'sample_count=false'
)
$propiedadesJtl = [ordered]@{ sample_variables='e3UsuarioId' }
foreach ($ajuste in $guardar) {
    $partes = $ajuste -split '=', 2
    $propiedadesJtl["jmeter.save.saveservice.$($partes[0])"] = $partes[1]
}
[ordered]@{
    destino=$destino; plan=$plan; jmeter=$jmeterJar; java=$java
    propiedades=$propiedades; jtl=$propiedadesJtl; solicitudesEsperadas=$esperadas
    proyecto='appointech-perf'; base=$aislamiento.Base; red=$aislamiento.Red
    backendId=$aislamiento.BackendId; dbId=$aislamiento.DbId
} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $resultados 'ejecucion.json') -Encoding UTF8
Write-Host "Resultados E3 en $resultados"


$relojPrep = [Diagnostics.Stopwatch]::StartNew()
$numeroBase = [DateTime]::UtcNow.ToString('yyyyMMddHHmmssfff')
$identidades = @(for ($i=1; $i -le $Usuarios; $i++) {
    $numero = $i.ToString('00')
    [ordered]@{
        nombre='Cliente E3'; apellido="Perf $numero"
        correo="e3-$id-$numero@perf.test"
        contrasena=('E3!' + [Guid]::NewGuid().ToString('N'))
        telefono=$null; tipoDocumento='CC'; numeroDocumento="$numeroBase$numero"
        direccion="Calle 100 # 10-$numero"
    }
})
$cohorte = @(for ($i=0; $i -lt $Usuarios; $i++) {
    $f = $identidades[$i]
    [pscustomobject]@{ ordinal=$i+1; correo=$f.correo; documento=$f.numeroDocumento;
        nombre=$f.nombre; apellido=$f.apellido; direccion=$f.direccion }
})
$cohorte | Export-Csv (Join-Path $resultados 'cohorte.csv') -NoTypeInformation -Encoding UTF8
$cohorteJson = ConvertTo-Json -InputObject $cohorte -Depth 4 -Compress
if ($cohorteJson.Contains("'")) { throw 'La cohorte contiene caracteres no admitidos en la consulta de verificacion.' }

function Leer-E3Estado {
    $sql = @'
BEGIN READ ONLY;
SELECT json_build_object(
    'usuarios',(SELECT count(*) FROM usuario),
    'clientes',(SELECT count(*) FROM cliente),
    'notificaciones',(SELECT count(*) FROM notificacion),
    'huerfanos',(SELECT count(*) FROM cliente c LEFT JOIN usuario u ON u.id=c.usuario_id WHERE u.id IS NULL)
);
ROLLBACK;
'@
    Consultar-E2 $compose $aislamiento $sql | ConvertFrom-Json
}

function Leer-E3Cohorte {
    $sql = @'
BEGIN READ ONLY;
WITH esperados AS (
    SELECT * FROM json_to_recordset('__COHORTE__'::json)
    AS e(ordinal int, correo text, documento text, nombre text, apellido text, direccion text)
)
SELECT json_build_object(
    'ordinal',e.ordinal, 'usuarios',count(DISTINCT u.id), 'perfiles',count(c.id),
    'usuarioId',min(u.id), 'perfilId',min(c.id), 'vinculoUsuarioId',min(c.usuario_id),
    'camposCorrectos',coalesce(bool_and(u.correo=e.correo AND u.numero_documento=e.documento
        AND u.nombre=e.nombre AND u.apellido=e.apellido AND u.rol='CLIENTE' AND u.activo
        AND u.tipo_documento='CC' AND u.telefono IS NULL AND u.foto_clave IS NULL
        AND u.creado_en IS NOT NULL AND left(u.contrasena,4) IN ('$2a$','$2b$','$2y$')
        AND c.usuario_id=u.id AND c.direccion=e.direccion
        AND c.latitud IS NULL AND c.longitud IS NULL), false)
)
FROM esperados e
LEFT JOIN usuario u ON u.correo=e.correo OR u.numero_documento=e.documento
LEFT JOIN cliente c ON c.usuario_id=u.id
GROUP BY e.ordinal
ORDER BY e.ordinal;
ROLLBACK;
'@.Replace('__COHORTE__',$cohorteJson)
    Consultar-E2 $compose $aislamiento $sql | ForEach-Object { ConvertFrom-Json -InputObject $_ }
}

$antes = Leer-E3Estado
$previas = @(Leer-E3Cohorte)
$antes | ConvertTo-Json | Set-Content (Join-Path $resultados 'antes.json') -Encoding UTF8
$previas | Export-Csv (Join-Path $resultados 'cohorte-antes.csv') -NoTypeInformation -Encoding UTF8
if ($previas.Count -ne $Usuarios -or @($previas | Where-Object { $_.usuarios -ne 0 -or $_.perfiles -ne 0 }).Count) {
    throw 'La comprobacion previa detecto colisiones o una cohorte incompleta.'
}
[ordered]@{usuarios=$Usuarios; colisiones=0; duracionMs=$relojPrep.ElapsedMilliseconds} |
    ConvertTo-Json | Set-Content (Join-Path $resultados 'preparacion.json') -Encoding UTF8

$argumentos = @('-Djava.awt.headless=true','-Djava.net.preferIPv4Stack=true','-Dhttp.nonProxyHosts=localhost|127.*',
    '-jar',$jmeterJar,'-n','-t',$plan,'-l',$jtl,'-j',$log,'-e','-o',$html,
    '-Lorg.apache.http.wire=OFF','-Lorg.apache.http.headers=OFF','-Lorg.apache.jmeter.protocol.http=OFF')
foreach ($tabla in @($propiedades, $propiedadesJtl)) {
    foreach ($entrada in $tabla.GetEnumerator()) { $argumentos += "-J$($entrada.Key)=$($entrada.Value)" }
}

$variables = New-Object 'System.Collections.Generic.List[string]'
$codigo = $null
$muestreo = $null
$errorRecolector = $false
function Guardar-E3CpuStat([string]$Fase) {
    $filas = foreach ($servicio in @('backend-perf','db-perf')) {
        $contenedor = if ($servicio -eq 'backend-perf') { $aislamiento.BackendId } else { $aislamiento.DbId }
        $inicio = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        try {
            $salida = @(Invoke-E2Docker exec $contenedor cat /sys/fs/cgroup/cpu.stat)
            $valores = [ordered]@{}
            foreach ($linea in $salida) {
                if ($linea -match '^(\w+)\s+(\d+)$') { $valores[$matches[1]] = [long]$matches[2] }
            }
            [ordered]@{servicio=$servicio; inicioEpochMs=$inicio; finEpochMs=[DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds(); contadores=$valores}
        } catch { [ordered]@{servicio=$servicio; inicioEpochMs=$inicio; disponible=$false} }
    }
    ConvertTo-Json -InputObject @($filas) -Depth 5 |
        Set-Content (Join-Path $resultados "cpu-stat-$Fase.json") -Encoding UTF8
}
try {
    for ($i=0; $i -lt $Usuarios; $i++) {
        $nombre = $prefijo + ($i + 1)
        $valor = $identidades[$i] | ConvertTo-Json -Depth 4 -Compress
        if ($valor.Length -ge 32767) { throw 'Paquete de identidad demasiado grande.' }
        $variables.Add($nombre)
        [Environment]::SetEnvironmentVariable($nombre, $valor, 'Process')
        if ([Environment]::GetEnvironmentVariable($nombre, 'Process') -cne $valor) { throw 'Paquete de identidad truncado.' }
    }
    $muestreo = Iniciar-MuestreoPerf $aislamiento $resultados
    Esperar-MuestreoPerf $muestreo
    Guardar-E3CpuStat 'antes'
    $ErrorActionPreference = 'Continue'
    & $java @argumentos
    $codigo = $LASTEXITCODE
} finally {
    $ErrorActionPreference = 'Stop'
    Guardar-E3CpuStat 'despues'
    foreach ($nombre in $variables) { [Environment]::SetEnvironmentVariable($nombre, $null, 'Process') }
    if ($muestreo) {
        try {
            Detener-MuestreoPerf $muestreo | ConvertTo-Json |
                Set-Content (Join-Path $resultados 'validacion-recursos.json') -Encoding UTF8
        } catch {
            $errorRecolector=$true
            [ordered]@{csvValido=$false; error='Fallo de cierre o validacion del recolector';
                tipo=$_.Exception.GetType().FullName} | ConvertTo-Json |
                Set-Content (Join-Path $resultados 'validacion-recursos.json') -Encoding UTF8
        }
    }
    [ordered]@{codigoJMeter=$codigo; errorRecolector=$errorRecolector;
        variablesRestantes=@($variables | Where-Object { $null -ne [Environment]::GetEnvironmentVariable($_, 'Process') }).Count} |
        ConvertTo-Json | Set-Content (Join-Path $resultados 'diagnostico.json') -Encoding UTF8
    $despues = Leer-E3Estado
    $despues | ConvertTo-Json | Set-Content (Join-Path $resultados 'despues.json') -Encoding UTF8
    $persistidas = @(Leer-E3Cohorte)
    $muestras = @(if (Test-Path -LiteralPath $jtl) { Import-Csv -LiteralPath $jtl })
    $reconciliacion = @(foreach ($f in $persistidas) {
        $m = @($muestras | Where-Object { $_.threadName -like "* 1-$($f.ordinal)" })
        $registro = $cohorte[$f.ordinal-1]
        $http = if ($m.Count -eq 1) { $m[0].responseCode } else { 'SIN_MUESTRA_UNICA' }
        $idRespuesta = if ($m.Count -eq 1) { $m[0].e3UsuarioId }
        [pscustomobject]@{ ordinal=$f.ordinal; correo=$registro.correo; documento=$registro.documento; http=$http;
            muestras=$m.Count; usuarioIdRespuesta=$idRespuesta; usuarioId=$f.usuarioId; perfilId=$f.perfilId;
            vinculoUsuarioId=$f.vinculoUsuarioId; usuarios=$f.usuarios; perfiles=$f.perfiles;
            camposCorrectos=$f.camposCorrectos; sinPerfil=($f.usuarios -gt 0 -and $f.perfiles -eq 0);
            duplicados=($f.usuarios -gt 1 -or $f.perfiles -gt 1) }
    })
    $reconciliacion | Export-Csv (Join-Path $resultados 'reconciliacion.csv') -NoTypeInformation -Encoding UTF8
}
if ($codigo -ne 0 -or $errorRecolector) { throw "E3 termino con error del ejecutor o del recolector. Evidencias en $resultados." }
if (-not (Test-Path -LiteralPath (Join-Path $html 'index.html') -PathType Leaf)) { throw 'Falta dashboard HTML.' }
$columnas = 'timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect,e3UsuarioId'.Split(',')
# Import-Csv interpreta las comillas que JMeter añade al nombre de sample_variables.
if (-not $muestras.Count -or
    (Compare-Object $columnas @($muestras[0].PSObject.Properties.Name) -CaseSensitive)) { throw 'Encabezado JTL inesperado.' }
if ($muestras.Count -ne $Usuarios -or @($muestras | Where-Object {
    $_.label -cne 'E3 POST /api/auth/registro/cliente' -or $_.URL -cne "$destino/api/auth/registro/cliente"
}).Count) { throw 'Cantidad o tipo de muestras inesperados en E3.' }
$integridad = @($reconciliacion | Where-Object {
    $_.muestras -ne 1 -or $_.http -ne '201' -or $_.usuarios -ne 1 -or $_.perfiles -ne 1 -or
    -not $_.camposCorrectos -or $_.usuarioIdRespuesta -ne [string]$_.usuarioId -or $_.vinculoUsuarioId -ne $_.usuarioId
}).Count -eq 0 -and $reconciliacion.Count -eq $Usuarios -and
    ($despues.usuarios-$antes.usuarios) -eq $Usuarios -and ($despues.clientes-$antes.clientes) -eq $Usuarios -and
    $despues.notificaciones -eq $antes.notificaciones -and $despues.huerfanos -eq 0
$otrosFallos = @($muestras | Where-Object {
    $_.responseCode -ne '201' -or [string]::IsNullOrWhiteSpace($_.e3UsuarioId) -or
    ($_.success -ne 'true' -and [int]$_.elapsed -le 1500)
}).Count
$duracion = @($muestras | Where-Object { [int]$_.elapsed -gt 1500 }).Count
$artefactos = @(Get-ChildItem -LiteralPath $resultados -Recurse -File)
$exposiciones = 0
foreach ($archivo in $artefactos) {
    $contenido = [IO.File]::ReadAllText($archivo.FullName)
    if ($contenido -match 'eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+|(?i)Authorization\s*:\s*Bearer' -or
        @($identidades | Where-Object { $contenido.Contains($_.contrasena) }).Count) { $exposiciones++ }
}
[ordered]@{muestras=$muestras.Count; integridadCorrecta=$integridad; otrosFallos=$otrosFallos; sobreUmbral=$duracion;
    archivosRevisados=$artefactos.Count; exposiciones=$exposiciones} |
    ConvertTo-Json | Set-Content (Join-Path $resultados 'veredicto.json') -Encoding UTF8
Write-Host "E3 $Usuarios usuarios. Muestras $($muestras.Count). Integridad $integridad. Fallos de duracion $duracion."
if (-not $integridad -or $otrosFallos -or $exposiciones) { throw 'E3 presenta fallos funcionales, de transporte, integridad o proteccion de datos.' }
