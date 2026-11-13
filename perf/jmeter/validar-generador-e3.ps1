[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$JMeterBat)
$ErrorActionPreference = 'Stop'
$plan = Join-Path $PSScriptRoot 'e3-registro.jmx'
$jar = Join-Path (Split-Path -Parent (Resolve-Path -LiteralPath $JMeterBat).ProviderPath) 'ApacheJMeter.jar'
$java = (Get-Command java.exe -CommandType Application | Select-Object -First 1).Source
$id = [Guid]::NewGuid().ToString('N')
$prefijo = "E3_SIMULADO_$($id)_"
$directorio = Join-Path (Split-Path -Parent $PSScriptRoot) ("resultados/validacion-generador-e3-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + "-$id")
New-Item -ItemType Directory -Path $directorio | Out-Null
$variables = @()
$servidor = $null
try {
    $servidor = Start-Job -ArgumentList $directorio -ScriptBlock {
        param($directorio)
        $ErrorActionPreference = 'Stop'
        $listener = New-Object Net.Sockets.TcpListener([Net.IPAddress]::Loopback,0)
        $listener.Start()
        try {
            $listener.LocalEndpoint.Port | Set-Content (Join-Path $directorio 'puerto.txt')
            'ordinal,llegadaEpochMs' | Set-Content (Join-Path $directorio 'llegadas.csv')
            while (-not (Test-Path (Join-Path $directorio 'servidor.fin'))) {
                if (-not $listener.Pending()) { Start-Sleep -Milliseconds 5; continue }
                $cliente = $listener.AcceptTcpClient()
                try {
                    $stream = $cliente.GetStream()
                    $stream.ReadTimeout = 5000
                    $reader = New-Object IO.StreamReader($stream,[Text.Encoding]::UTF8,$false,1024,$true)
                    $primera = $reader.ReadLine()
                    if ($primera -notmatch '^POST /api/auth/registro/cliente HTTP/1\.[01]$') { throw 'Ruta inesperada en simulador.' }
                    $llegada = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
                    $longitud = 0
                    while ($linea = $reader.ReadLine()) {
                        if ($linea -match '^Content-Length:\s*(\d+)$') { $longitud = [int]$matches[1] }
                    }
                    # Los datos ficticios de esta validacion son ASCII, sin credenciales reales.
                    $cuerpo = New-Object char[] $longitud
                    $leidos = 0
                    while ($leidos -lt $longitud) {
                        $n = $reader.Read($cuerpo,$leidos,$longitud-$leidos)
                        if ($n -le 0) { throw 'Cuerpo incompleto en simulador.' }
                        $leidos += $n
                    }
                    $datos = (-join $cuerpo) | ConvertFrom-Json
                    $ordinal = [int]$datos.numeroDocumento
                    "$ordinal,$llegada" | Add-Content (Join-Path $directorio 'llegadas.csv')
                    $respuesta = [ordered]@{exito=$true;datos=[ordered]@{id=$ordinal;nombre=$datos.nombre;
                        apellido=$datos.apellido;correo=$datos.correo;rol='CLIENTE';fotoUrl=$null};
                        mensaje='Cliente registrado correctamente';timestamp='2026-01-01T00:00:00'} | ConvertTo-Json -Depth 5 -Compress
                    $bytes = [Text.Encoding]::UTF8.GetBytes($respuesta)
                    $cabecera = [Text.Encoding]::ASCII.GetBytes("HTTP/1.1 201 Created`r`nContent-Type: application/json`r`nContent-Length: $($bytes.Length)`r`nConnection: close`r`n`r`n")
                    $stream.Write($cabecera,0,$cabecera.Length)
                    $stream.Write($bytes,0,$bytes.Length)
                    $stream.Flush()
                } finally { $cliente.Close() }
            }
        } finally { $listener.Stop() }
    }
    $limite = (Get-Date).AddSeconds(15)
    while (-not (Test-Path (Join-Path $directorio 'puerto.txt'))) {
        if ($servidor.State -eq 'Failed' -or (Get-Date) -gt $limite) { throw 'No arranco el simulador.' }
        Start-Sleep -Milliseconds 50
    }
    $puerto = [int](Get-Content (Join-Path $directorio 'puerto.txt'))
    if ($puerto -in @(8080,18080,55432,5432)) { throw 'Puerto del simulador no permitido.' }
    for ($i=1; $i -le 20; $i++) {
        $nombre = $prefijo+$i
        $variables += $nombre
        $ficticio = [ordered]@{nombre='Simulado';apellido="Hilo$i";correo="ficticio$i@example.invalid";
            contrasena='SoloSimulador123!';telefono=$null;tipoDocumento='CC';numeroDocumento=[string]$i;direccion='Direccion ficticia'}
        [Environment]::SetEnvironmentVariable($nombre,($ficticio | ConvertTo-Json -Compress),'Process')
    }
    $argumentos = @('-Djava.awt.headless=true','-Djava.net.preferIPv4Stack=true','-jar',$jar,'-n','-t',$plan,
        '-l',(Join-Path $directorio 'simulado.jtl'),'-j',(Join-Path $directorio 'jmeter.log'),
        '-Jusuarios=20','-Jrampa=10','-Jhost=127.0.0.1',"-Jpuerto=$puerto","-JidentidadesEnv=$prefijo",
        "-JplanE3=$plan", "-JpreparacionGenerador=$(Join-Path $directorio 'preparacion-generador.json')",
        "-JdiagnosticoGenerador=$(Join-Path $directorio 'temporizacion-generador.csv')",
        '-Jsampleresult.timestamp.start=true','-Jsample_variables=e3UsuarioId',
        '-Jjmeter.save.saveservice.output_format=csv','-Jjmeter.save.saveservice.timestamp_format=ms',
        '-Jjmeter.save.saveservice.response_data=false','-Jjmeter.save.saveservice.response_data.on_error=false',
        '-Jjmeter.save.saveservice.samplerData=false','-Jjmeter.save.saveservice.requestHeaders=false',
        '-Jjmeter.save.saveservice.responseHeaders=false',
        '-Lorg.apache.http.wire=OFF','-Lorg.apache.http.headers=OFF','-Lorg.apache.jmeter.protocol.http=OFF')
    $ErrorActionPreference = 'Continue'
    & $java @argumentos
    $codigo = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    $llegadas = @(Import-Csv (Join-Path $directorio 'llegadas.csv') | Sort-Object {[long]$_.llegadaEpochMs})
    $muestras = @(Import-Csv (Join-Path $directorio 'simulado.jtl'))
    $max100ms = 0
    foreach ($f in $llegadas) {
        $t = [long]$f.llegadaEpochMs
        $n = @($llegadas | Where-Object { [long]$_.llegadaEpochMs -ge $t -and [long]$_.llegadaEpochMs -lt $t+100 }).Count
        $max100ms = [Math]::Max($max100ms,$n)
    }
    $separacion = if ($llegadas.Count) { [long]$llegadas[-1].llegadaEpochMs-[long]$llegadas[0].llegadaEpochMs } else {0}
    $valido = $codigo -eq 0 -and $llegadas.Count -eq 20 -and $muestras.Count -eq 20 -and
        @($llegadas.ordinal | Select-Object -Unique).Count -eq 20 -and
        @($muestras | Where-Object { $_.responseCode -ne '201' -or $_.success -ne 'true' }).Count -eq 0 -and
        $separacion -ge 8000 -and $max100ms -le 2
    [ordered]@{simulador=$true;destino="http://127.0.0.1:$puerto";codigoJMeter=$codigo;
        muestras=$muestras.Count;llegadas=$llegadas.Count;separacionMs=$separacion;maxLlegadas100Ms=$max100ms;
        valido=$valido;criterio='20 respuestas validas; separacion >=8 s; no mas de 2 llegadas por ventana de 100 ms'} |
        ConvertTo-Json | Set-Content (Join-Path $directorio 'validacion.json') -Encoding UTF8
    Write-Host "Validacion local: $valido. Evidencias: $directorio"
    if (-not $valido) { throw 'Distribucion local no validada: no ejecutar contra AppoinTech.' }
} finally {
    foreach ($nombre in $variables) { [Environment]::SetEnvironmentVariable($nombre,$null,'Process') }
    if ($servidor) {
        New-Item -ItemType File -Path (Join-Path $directorio 'servidor.fin') -Force | Out-Null
        if (-not (Wait-Job $servidor -Timeout 7)) { Stop-Job $servidor }
        try { Receive-Job $servidor -ErrorAction Stop | Out-Null } finally { Remove-Job $servidor -Force }
    }
}
