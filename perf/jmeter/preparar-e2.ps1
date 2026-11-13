# Cargado por ejecutar-e2.ps1. La salida de preparacion queda en memoria.
function Invoke-E2Docker {
    $salida = & docker @args 2>$null
    if ($LASTEXITCODE -ne 0) { throw 'Docker no pudo verificar o consultar el entorno perf.' }
    $salida
}

function Confirmar-E2Aislamiento($Compose, [int]$Puerto) {
    $backendId = @(Invoke-E2Docker @Compose ps -q backend-perf)
    $baseId = @(Invoke-E2Docker @Compose ps -q db-perf)
    if ($backendId.Count -ne 1 -or $baseId.Count -ne 1) { throw 'Se necesita una instancia activa por servicio perf.' }
    $backend, $base = (Invoke-E2Docker inspect $backendId[0] $baseId[0] | Out-String | ConvertFrom-Json)
    $red = 'appointech-perf_perf-net'
    foreach ($par in @(@{c=$backend; servicio='backend-perf'}, @{c=$base; servicio='db-perf'})) {
        $c = $par.c
        $redes = @($c.NetworkSettings.Networks.PSObject.Properties.Name)
        if ($c.Config.Labels.'com.docker.compose.project' -ne 'appointech-perf' -or
            $c.Config.Labels.'com.docker.compose.service' -ne $par.servicio -or
            -not $c.State.Running -or $c.State.Health.Status -ne 'healthy' -or
            $redes.Count -ne 1 -or $redes[0] -ne $red) {
            throw 'Los servicios deben estar saludables y en la red exclusiva de appointech-perf.'
        }
    }
    $infoRed = Invoke-E2Docker network inspect $red | Out-String | ConvertFrom-Json
    $miembros = @($infoRed.Containers.PSObject.Properties.Name)
    if ($miembros.Count -ne 2 -or $miembros -notcontains $backend.Id -or $miembros -notcontains $base.Id -or
        $base.NetworkSettings.Networks.$red.Aliases -notcontains 'db-perf') { throw 'La red o el alias de db-perf no coinciden.' }
    if ($backend.Config.Env -notcontains 'SPRING_PROFILES_ACTIVE=perf' -or
        $backend.Config.Env -notcontains 'DB_URL=jdbc:postgresql://db-perf:5432/appointech_perf' -or
        $base.Config.Env -notcontains 'POSTGRES_DB=appointech_perf' -or
        @($backend.Config.Env | Where-Object { $_ -match '^(SPRING_DATASOURCE_URL|SPRING_APPLICATION_JSON)=' }).Count) {
        throw 'El perfil o el JDBC efectivo no se pueden confirmar como perf.'
    }
    if (-not @($base.Mounts | Where-Object { $_.Type -eq 'tmpfs' -and $_.Destination -eq '/var/lib/postgresql/data' }).Count -and
        $base.HostConfig.Tmpfs.PSObject.Properties.Name -notcontains '/var/lib/postgresql/data') { throw 'Falta tmpfs de db-perf.' }
    $bindings = @($backend.NetworkSettings.Ports.'8080/tcp' | Where-Object { $_.HostPort -eq "$Puerto" })
    if (-not @($bindings | Where-Object HostIp -in @('0.0.0.0','127.0.0.1')).Count) {
        throw 'El puerto local solicitado no esta publicado por backend-perf en IPv4.'
    }
    $usuario = @($base.Config.Env | Where-Object { $_ -like 'POSTGRES_USER=*' })
    if ($usuario.Count -ne 1 -or $usuario[0] -notmatch '^POSTGRES_USER=([a-zA-Z0-9_]+)$') { throw 'Usuario de db-perf no identificable.' }
    [pscustomobject]@{ Usuario = $Matches[1]; Base = 'appointech_perf'; Red = $red; BackendId = $backend.Id; DbId = $base.Id }
}

function Consultar-E2($Compose, $Aislamiento, [string]$Sql) {
    $encodingAnterior = [Console]::OutputEncoding
    try {
        [Console]::OutputEncoding = [Text.Encoding]::UTF8
        $salida = $Sql | & docker @Compose exec -T -e PGCLIENTENCODING=UTF8 db-perf psql -X -q -U $Aislamiento.Usuario -d $Aislamiento.Base -v ON_ERROR_STOP=1 -At 2>$null
        if ($LASTEXITCODE -ne 0) { throw 'Fallo la consulta de solo lectura en db-perf.' }
    } finally {
        [Console]::OutputEncoding = $encodingAnterior
    }
    $salida
}

function Leer-E2Expectativas($Compose, $Aislamiento, [int]$Usuarios) {
    $sql = @"
BEGIN READ ONLY;
SELECT json_build_object(
    'ordinal', s.i, 'correo', u.correo, 'rol', u.rol,
    'total', count(n.id), 'sinLeer', count(n.id) FILTER (WHERE NOT n.leida),
    'notificaciones', coalesce(json_agg(json_build_object(
        'id', n.id, 'tipo', n.tipo, 'titulo', n.titulo, 'mensaje', n.mensaje,
        'citaId', n.cita_id, 'leida', n.leida,
        'creadaEn', to_char(n.creada_en, 'YYYY-MM-DD"T"HH24:MI:SS.US')
    ) ORDER BY n.creada_en DESC, n.id DESC) FILTER (WHERE n.id IS NOT NULL), '[]'::json)
)
FROM generate_series(1,$Usuarios) s(i)
JOIN usuario u ON u.correo='cliente' || s.i || '@perf.test' AND u.activo AND u.rol='CLIENTE'
JOIN cliente c ON c.usuario_id=u.id
LEFT JOIN notificacion n ON n.usuario_id=u.id
GROUP BY s.i,u.correo,u.rol
ORDER BY s.i;
ROLLBACK;
"@
    $filas = @(Consultar-E2 $Compose $Aislamiento $sql | ForEach-Object { ConvertFrom-Json -InputObject $_ })
    if ($filas.Count -ne $Usuarios) { throw 'Cohorte incompleta. Se requieren los primeros N clientes activos con perfil.' }
    for ($i=0; $i -lt $Usuarios; $i++) {
        $f = $filas[$i]
        if ($f.ordinal -ne ($i + 1) -or $f.total -ne 3 -or $f.sinLeer -ne 2 -or @($f.notificaciones).Count -ne 3) {
            throw "Datos perf inesperados para el cliente sintetico numero $($i + 1). Se esperan tres notificaciones y dos sin leer."
        }
    }
    $filas
}

function Invoke-E2Preparacion($Filas, $Csv, [string]$Destino, [int]$TimeoutMs, [string]$Registro, [string[]]$Tipos) {
    $creds = @{}
    foreach ($fila in $Csv) {
        if ($creds.ContainsKey($fila.correo)) { throw 'El CSV contiene identidades duplicadas.' }
        $creds[$fila.correo] = $fila.contrasena
    }
    foreach ($f in $Filas) {
        if (-not $creds.ContainsKey($f.correo) -or [string]::IsNullOrWhiteSpace($creds[$f.correo])) {
            throw "Falta la credencial sintetica numero $($f.ordinal)."
        }
    }
    'ordinal,operacion,duracionMs,resultado' | Set-Content -LiteralPath $Registro -Encoding UTF8
    Add-Type -AssemblyName System.Net.Http
    $handler = [Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $handler.UseProxy = $false
    $http = [Net.Http.HttpClient]::new($handler)
    try {
        $http.Timeout = [TimeSpan]::FromMilliseconds($TimeoutMs)
        foreach ($f in $Filas) {
            $reloj = [Diagnostics.Stopwatch]::StartNew()
            $resultado = 'fallo'
            $respuesta = $null
            $cuerpo = [Net.Http.StringContent]::new(
                (@{correo=$f.correo;contrasena=$creds[$f.correo]} | ConvertTo-Json -Compress),
                [Text.Encoding]::UTF8, 'application/json')
            try {
                $respuesta = $http.PostAsync("$Destino/api/auth/login", $cuerpo).GetAwaiter().GetResult()
                if ([int]$respuesta.StatusCode -ne 200) { throw 'Estado HTTP inesperado' }
                $login = ConvertFrom-Json -InputObject ($respuesta.Content.ReadAsStringAsync().GetAwaiter().GetResult())
                if ($login.exito -isnot [bool] -or -not $login.exito -or
                    $login.datos.token -isnot [string] -or [string]::IsNullOrWhiteSpace($login.datos.token)) {
                    throw 'Contrato de login invalido'
                }
                $token = $login.datos.token
                $resultado = 'ok'
            } catch {
                throw "Fallo de autenticacion para la identidad sintetica numero $($f.ordinal). La carga no se inicia."
            } finally {
                $cuerpo.Dispose()
                if ($respuesta) { $respuesta.Dispose() }
                "$($f.ordinal),login,$($reloj.ElapsedMilliseconds),$resultado" | Add-Content -LiteralPath $Registro -Encoding UTF8
            }

            # El servidor verifica el JWT. Los IDs acreditan la identidad contra la base perf.
            $reloj.Restart()
            $resultado = 'fallo'
            $peticion = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::Get, "$Destino/api/notificaciones?page=0&size=20")
            $respuesta = $null
            try {
                $peticion.Headers.Authorization = [Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $token)
                $respuesta = $http.SendAsync($peticion).GetAwaiter().GetResult()
                if ([int]$respuesta.StatusCode -ne 200) { throw 'Estado HTTP inesperado' }
                $json = $respuesta.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                if (-not $json.TrimStart().StartsWith('[')) { throw 'Se esperaba un arreglo' }
                $notificaciones = ConvertFrom-Json -InputObject $json
                $ids = ($notificaciones | ForEach-Object id) -join ','
                $esperados = ($f.notificaciones | ForEach-Object id) -join ','
                if ($notificaciones.Count -ne 3 -or $ids -cne $esperados -or
                    (@($respuesta.Headers.GetValues('X-Total-Count')) -join ',') -cne '3') { throw 'Pertenencia o pagina inesperada' }
                $resultado = 'ok'
            } catch {
                throw "No se pudo acreditar el token contra los datos de la identidad sintetica numero $($f.ordinal)."
            } finally {
                $peticion.Dispose()
                if ($respuesta) { $respuesta.Dispose() }
                "$($f.ordinal),verificar_identidad,$($reloj.ElapsedMilliseconds),$resultado" | Add-Content -LiteralPath $Registro -Encoding UTF8
            }
            [pscustomobject]@{ correo=$f.correo; rol=$f.rol; token=$token; total=$f.total; sinLeer=$f.sinLeer;
                notificaciones=$f.notificaciones; tipos=$Tipos }
        }
    } finally {
        $http.Dispose()
    }
}
