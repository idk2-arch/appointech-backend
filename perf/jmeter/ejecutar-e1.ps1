[CmdletBinding()]
param(
    [string]$JMeterBat,
    [string]$Credenciales,
    [ValidateSet('http', 'https')][string]$Protocolo = 'http',
    [ValidatePattern('^[a-zA-Z0-9.\-]+$')][string]$Servidor = 'localhost',
    [ValidateRange(1, 65535)][int]$Puerto = 18080,
    [ValidateRange(1, 2147483647)][int]$Usuarios = 20,
    [ValidateRange(0, 2147483647)][int]$Rampa = 10,
    [ValidateRange(1, 2147483647)][int]$Loops = 5,
    [ValidateRange(1, 2147483647)][int]$UmbralLoginMs = 2000,
    [ValidateRange(1, 2147483647)][int]$TimeoutConexionMs = 5000,
    [ValidateRange(1, 2147483647)][int]$TimeoutRespuestaMs = 10000
)
$ErrorActionPreference = 'Stop'

if ($Puerto -eq 8080 -and $Servidor.TrimEnd('.') -in @('localhost', '127.0.0.1')) {
    throw 'Puerto 8080 local reservado para desarrollo. Indique el puerto publicado de backend-perf.'
}
if (-not $JMeterBat) {
    $comando = Get-Command jmeter.bat -CommandType Application -ErrorAction SilentlyContinue |
        Select-Object -First 1
    if (-not $comando) { throw 'jmeter.bat no esta en PATH. Indique -JMeterBat.' }
    $JMeterBat = $comando.Source
}
if (-not (Test-Path -LiteralPath $JMeterBat -PathType Leaf) -or
    [IO.Path]::GetFileName($JMeterBat) -ne 'jmeter.bat') {
    throw 'Indique una ruta valida a jmeter.bat.'
}
$JMeterBat = (Resolve-Path -LiteralPath $JMeterBat).ProviderPath
$jmeterJar = Join-Path (Split-Path -Parent $JMeterBat) 'ApacheJMeter.jar'
if (-not (Test-Path -LiteralPath $jmeterJar -PathType Leaf)) {
    throw 'ApacheJMeter.jar no esta junto a jmeter.bat. Indique una instalacion completa.'
}
$java = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' }
if (-not $java -or -not (Test-Path -LiteralPath $java -PathType Leaf)) {
    $java = (Get-Command java.exe -CommandType Application -ErrorAction SilentlyContinue |
        Select-Object -First 1).Source
}
if (-not $java) { throw 'java.exe no esta disponible en JAVA_HOME ni en PATH.' }
$plan = Join-Path $PSScriptRoot 'e1-login.jmx'
if (-not (Test-Path -LiteralPath $plan -PathType Leaf)) { throw 'No se encuentra e1-login.jmx.' }
[xml]$xml = Get-Content -LiteralPath $plan -Raw
if ($xml.DocumentElement.Name -ne 'jmeterTestPlan') { throw 'El XML no es un plan JMeter.' }

if (-not $Credenciales) { $Credenciales = Join-Path $PSScriptRoot 'clientes.csv' }
if (-not (Test-Path -LiteralPath $Credenciales -PathType Leaf)) { throw 'No se encuentra el CSV de credenciales.' }
$Credenciales = (Resolve-Path -LiteralPath $Credenciales).ProviderPath
if ((Get-Content -LiteralPath $Credenciales -Encoding UTF8 -TotalCount 1) -cne 'correo,contrasena') {
    throw 'La primera linea del CSV debe ser correo,contrasena.'
}
$esperadas = [long]$Usuarios * $Loops
Add-Type -AssemblyName Microsoft.VisualBasic
$lector = [Microsoft.VisualBasic.FileIO.TextFieldParser]::new($Credenciales, [Text.Encoding]::UTF8)
$filas = 0
try {
    $lector.SetDelimiters(',')
    $lector.HasFieldsEnclosedInQuotes = $true
    $lector.TrimWhiteSpace = $false
    $encabezado = $lector.ReadFields()
    if ($null -eq $encabezado -or $encabezado.Count -ne 2 -or
        $encabezado[0] -cne 'correo' -or $encabezado[1] -cne 'contrasena') {
        throw 'El encabezado requerido es correo,contrasena.'
    }
    while (-not $lector.EndOfData) {
        $fila = $lector.ReadFields()
        if ($fila.Count -ne 2 -or [string]::IsNullOrWhiteSpace($fila[0]) -or
            [string]::IsNullOrWhiteSpace($fila[1])) {
            throw "Registro CSV invalido en la fila de datos $($filas + 1)."
        }
        $filas++
    }
} catch [Microsoft.VisualBasic.FileIO.MalformedLineException] {
    throw "CSV mal formado cerca de la fila de datos $($filas + 1)."
} finally {
    $lector.Dispose()
}
if ($filas -lt $esperadas) { throw "CSV insuficiente. Disponibles $filas, requeridas $esperadas." }

$marca = Get-Date -Format 'yyyyMMdd-HHmmss'
$id = [Guid]::NewGuid().ToString('N')
$resultados = Join-Path (Split-Path -Parent $PSScriptRoot) "resultados/e1-$marca-$id"
New-Item -ItemType Directory -Path $resultados | Out-Null
$jtl = Join-Path $resultados 'e1.jtl'
$log = Join-Path $resultados 'jmeter.log'
$html = Join-Path $resultados 'reporte'
$propiedades = [ordered]@{
    protocolo = $Protocolo
    host = $Servidor
    puerto = $Puerto
    usuarios = $Usuarios
    rampa = $Rampa
    loops = $Loops
    umbralLoginMs = $UmbralLoginMs
    timeoutConexionMs = $TimeoutConexionMs
    timeoutRespuestaMs = $TimeoutRespuestaMs
    credenciales = $Credenciales.Replace('\', '/')
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
$propiedadesJtl = [ordered]@{ sample_variables = '' }
foreach ($ajuste in $guardar) {
    $partes = $ajuste -split '=', 2
    $propiedadesJtl["jmeter.save.saveservice.$($partes[0])"] = $partes[1]
}
[ordered]@{
    destino = "${Protocolo}://${Servidor}:$Puerto"
    plan = $plan
    jmeter = $jmeterJar
    java = $java
    propiedades = $propiedades
    jtl = $propiedadesJtl
    filasCsv = $filas
    solicitudesEsperadas = $esperadas
} | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $resultados 'ejecucion.json') -Encoding UTF8

$argumentos = @('-Djava.awt.headless=true', '-jar', $jmeterJar,
    '-n', '-t', $plan, '-l', $jtl, '-j', $log, '-e', '-o', $html,
    '-Lorg.apache.jmeter.extractor.json.jsonpath=OFF',
    '-Lorg.apache.http.wire=OFF', '-Lorg.apache.http.headers=OFF')
foreach ($tabla in @($propiedades, $propiedadesJtl)) {
    foreach ($entrada in $tabla.GetEnumerator()) {
        $argumentos += "-J$($entrada.Key)=$($entrada.Value)"
    }
}
Write-Host "Destino ${Protocolo}://${Servidor}:$Puerto. Solicitudes esperadas $esperadas."
Write-Host "Resultados en $resultados"
$ErrorActionPreference = 'Continue'
& $java @argumentos
$codigo = $LASTEXITCODE
$ErrorActionPreference = 'Stop'
if ($codigo -ne 0) { throw "JMeter termino con codigo $codigo. Revise el log en $resultados." }
if (-not (Test-Path -LiteralPath $jtl -PathType Leaf)) { throw "JMeter no produjo el JTL en $resultados." }
if (-not (Test-Path -LiteralPath (Join-Path $html 'index.html') -PathType Leaf)) {
    throw "JMeter no produjo el dashboard HTML en $resultados."
}
$columnasRequeridas = @(
    'timeStamp', 'elapsed', 'label', 'responseCode', 'responseMessage', 'threadName',
    'success', 'failureMessage', 'bytes', 'sentBytes', 'grpThreads', 'allThreads',
    'URL', 'Latency', 'IdleTime', 'Connect'
)
$encabezadoJtl = Get-Content -LiteralPath $jtl -TotalCount 1
if (-not $encabezadoJtl -or
    (Compare-Object $columnasRequeridas ($encabezadoJtl.Split(',')) -CaseSensitive)) {
    throw "El JTL tiene columnas distintas de las previstas. Revise $resultados."
}
$muestras = @(Import-Csv -LiteralPath $jtl)
if ($muestras.Count -ne $esperadas) {
    throw "Ejecucion incompleta. Se guardaron $($muestras.Count) de $esperadas muestras. Revise el JTL y el log."
}
$fallos = @($muestras | Where-Object success -eq 'false').Count
Write-Host "Muestras $($muestras.Count). Fallidas $fallos. Dashboard en $html."
