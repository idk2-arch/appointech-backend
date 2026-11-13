[CmdletBinding()]
param(
    [string]$JMeterBat,
    [ValidateSet('localhost', '127.0.0.1')][string]$Servidor = 'localhost',
    [ValidateRange(1, 65535)][int]$Puerto = 18080,
    [ValidateRange(1, 80)][int]$Usuarios = 30,
    [ValidateRange(0, 2147483647)][int]$Rampa = 15,
    [ValidateRange(1, 2147483647)][int]$Loops = 10,
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
$raiz = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$perf = Split-Path -Parent $PSScriptRoot
$plan = Join-Path $PSScriptRoot 'e2-notificaciones.jmx'
$credenciales = Join-Path $PSScriptRoot 'clientes.csv'
$helper = Join-Path $PSScriptRoot 'preparar-e2.ps1'
$archivoCompose = Join-Path $perf 'docker-compose.perf.yml'
$archivoEnv = Join-Path $perf '.env.perf'
foreach ($ruta in @($plan, $credenciales, $helper, $archivoCompose, $archivoEnv)) {
    if (-not (Test-Path -LiteralPath $ruta -PathType Leaf)) { throw "Falta una dependencia de E2 en $ruta." }
}
[xml]$xml = Get-Content -LiteralPath $plan -Raw
if ($xml.DocumentElement.Name -ne 'jmeterTestPlan') { throw 'XML del plan E2 incorrecto.' }
if ((Get-Content -LiteralPath $credenciales -Encoding UTF8 -TotalCount 1) -cne 'correo,contrasena') {
    throw 'Encabezado CSV incorrecto.'
}
$csv = @(Import-Csv -LiteralPath $credenciales -Encoding UTF8)
. $helper

$enumPath = Join-Path $raiz 'src/main/java/com/appointech/appointech_backend/domain/models/TipoNotificacion.java'
$enum = [regex]::Match((Get-Content -LiteralPath $enumPath -Raw), '(?s)enum TipoNotificacion\s*\{([^}]+)\}')
$tipos = @($enum.Groups[1].Value -split ',' | ForEach-Object { $_.Trim() })
if (-not $enum.Success -or @($tipos | Where-Object { $_ -notmatch '^[A-Z][A-Z_]+$' }).Count) {
    throw 'No se pudo leer el enum TipoNotificacion del dominio.'
}
$compose = @('compose','--env-file',$archivoEnv,'-f',$archivoCompose)
$aislamiento = Confirmar-E2Aislamiento $compose $Puerto
$destino = "http://${Servidor}:$Puerto"
$id = [Guid]::NewGuid().ToString('N')
$prefijo = "APPOINTECH_E2_$($id)_"
$resultados = Join-Path $perf ("resultados/e2-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + "-$id")
New-Item -ItemType Directory -Path $resultados | Out-Null
$jtl = Join-Path $resultados 'e2.jtl'
$log = Join-Path $resultados 'jmeter.log'
$html = Join-Path $resultados 'reporte'
$esperadas = [long]$Usuarios * $Loops
$propiedades = [ordered]@{
    protocolo='http'; host=$Servidor; puerto=$Puerto; usuarios=$Usuarios
    rampa=$Rampa; loops=$Loops; umbralLecturaMs=800
    timeoutConexionMs=$TimeoutConexionMs; timeoutRespuestaMs=$TimeoutRespuestaMs
    identidadesEnv=$prefijo
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
$propiedadesJtl = [ordered]@{ sample_variables='' }
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
Write-Host "Resultados E2 en $resultados"

$reloj = [Diagnostics.Stopwatch]::StartNew()
$preparado = $false
$registro = Join-Path $resultados 'preparacion.csv'
try {
    $filas = @(Leer-E2Expectativas $compose $aislamiento $Usuarios)
    $identidades = @(Invoke-E2Preparacion $filas $csv "http://127.0.0.1:$Puerto" $TimeoutRespuestaMs $registro $tipos)
    if ($identidades.Count -ne $Usuarios) { throw 'Preparacion de identidades incompleta.' }
    $preparado = $true
} finally {
    $duracionPreparacion = $reloj.ElapsedMilliseconds
    $eventos = @(if (Test-Path -LiteralPath $registro) { Import-Csv -LiteralPath $registro })
    [ordered]@{
        duracionMs=$duracionPreparacion; usuarios=$Usuarios; completo=$preparado
        logins=@($eventos | Where-Object operacion -eq 'login').Count
        verificaciones=@($eventos | Where-Object operacion -eq 'verificar_identidad').Count
        fallos=@($eventos | Where-Object resultado -eq 'fallo').Count
    } |
        ConvertTo-Json | Set-Content -LiteralPath (Join-Path $resultados 'preparacion.json') -Encoding UTF8
}

$argumentos = @('-Djava.awt.headless=true','-Djava.net.preferIPv4Stack=true','-Dhttp.nonProxyHosts=localhost|127.*',
    '-jar',$jmeterJar,'-n','-t',$plan,'-l',$jtl,'-j',$log,'-e','-o',$html,
    '-Lorg.apache.http.wire=OFF','-Lorg.apache.http.headers=OFF','-Lorg.apache.jmeter.protocol.http=OFF')
foreach ($tabla in @($propiedades, $propiedadesJtl)) {
    foreach ($entrada in $tabla.GetEnumerator()) { $argumentos += "-J$($entrada.Key)=$($entrada.Value)" }
}
$variables = New-Object 'System.Collections.Generic.List[string]'
$longitudes = New-Object 'System.Collections.Generic.List[int]'
$codigo = $null
try {
    for ($i=0; $i -lt $identidades.Count; $i++) {
        $nombre = $prefijo + ($i + 1)
        $valor = $identidades[$i] | ConvertTo-Json -Depth 8 -Compress
        if ($valor.Length -ge 32767) { throw 'La identidad preparada excede el limite de una variable de entorno de Windows.' }
        $variables.Add($nombre)
        $longitudes.Add($valor.Length)
        [Environment]::SetEnvironmentVariable($nombre, $valor, 'Process')
        if ([Environment]::GetEnvironmentVariable($nombre, 'Process') -cne $valor) {
            throw 'No se pudo conservar integro el paquete de identidad en el entorno.'
        }
    }
    $ErrorActionPreference = 'Continue'
    & $java @argumentos
    $codigo = $LASTEXITCODE
} finally {
    foreach ($nombre in $variables) { [Environment]::SetEnvironmentVariable($nombre, $null, 'Process') }
    $ErrorActionPreference = 'Stop'
    $tamano = $longitudes | Measure-Object -Maximum -Sum
    [ordered]@{
        paquetes=$variables.Count
        longitudMaxima=$tamano.Maximum
        longitudTotalValores=$tamano.Sum
        variablesRestantes=@($variables | Where-Object { $null -ne [Environment]::GetEnvironmentVariable($_, 'Process') }).Count
        codigoJMeter=$codigo
    } | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $resultados 'diagnostico-entorno.json') -Encoding UTF8
}
if ($codigo -ne 0) { throw "JMeter termino con codigo $codigo. Artefactos en $resultados." }
if (-not (Test-Path -LiteralPath $jtl -PathType Leaf) -or
    -not (Test-Path -LiteralPath (Join-Path $html 'index.html') -PathType Leaf)) {
    throw "Falta el JTL o el dashboard en $resultados."
}
$columnas = 'timeStamp,elapsed,label,responseCode,responseMessage,threadName,success,failureMessage,bytes,sentBytes,grpThreads,allThreads,URL,Latency,IdleTime,Connect'.Split(',')
$encabezado = Get-Content -LiteralPath $jtl -TotalCount 1
if (-not $encabezado -or (Compare-Object $columnas ($encabezado.Split(',')) -CaseSensitive)) { throw 'Encabezado JTL inesperado.' }
$muestras = @(Import-Csv -LiteralPath $jtl)
$url = "$destino/api/notificaciones?page=0&size=20"
if (@($muestras | Where-Object { $_.label -cne 'E2 GET /api/notificaciones' -or $_.URL -cne $url }).Count) {
    throw 'El JTL contiene muestras ajenas al GET E2.'
}
if ($muestras.Count -ne $esperadas) { throw "Se guardaron $($muestras.Count) de $esperadas muestras." }
Write-Host "Muestras $($muestras.Count). Fallidas $(@($muestras | Where-Object success -eq 'false').Count). Dashboard en $html."
