param(
    [string]$Base = "http://localhost:8080/api",
    [string]$UrlNotificaciones = "http://localhost:8081"
)

$ErrorActionPreference = "Stop"
$sufijo = Get-Random -Maximum 99999
$resultados = @()

function Titulo($texto) {
    Write-Host ""
    Write-Host "== $texto" -ForegroundColor Cyan
}

function Verificar($condicion, $descripcion) {
    if ($condicion) {
        Write-Host "   OK    $descripcion" -ForegroundColor Green
    } else {
        Write-Host "   FALLA $descripcion" -ForegroundColor Red
    }
    $script:resultados += [pscustomobject]@{ Prueba = $descripcion; Paso = [bool]$condicion }
}

function Llamar($metodo, $ruta, $cuerpo, $token) {
    $parametros = @{
        Method      = $metodo
        Uri         = "$Base$ruta"
        ContentType = "application/json"
    }
    if ($token) { $parametros.Headers = @{ Authorization = "Bearer $token" } }
    if ($cuerpo) { $parametros.Body = ($cuerpo | ConvertTo-Json -Depth 5) }
    $respuesta = Invoke-RestMethod @parametros
    $respuesta
}

function LlamarEsperandoError($metodo, $ruta, $cuerpo, $token) {
    try {
        Llamar $metodo $ruta $cuerpo $token | Out-Null
        return 200
    } catch {
        return [int]$_.Exception.Response.StatusCode
    }
}

Titulo "1. Inicio de sesión del administrador"
$admin = (Llamar "POST" "/auth/login" @{ correo = "admin@appointech.com"; contrasena = "admin12345" }).datos
Verificar ($admin.token.Length -gt 20) "El administrador recibe un token"

Titulo "2. Especialidad Lavadoras"
$especialidades = Llamar "GET" "/especialidades"
$lavadoras = $especialidades | Where-Object { $_.nombre -eq "Lavadoras" } | Select-Object -First 1
Verificar ($null -ne $lavadoras) "Existe la especialidad Lavadoras"

Titulo "3. Técnico con la especialidad"
$tecnico = (Llamar "POST" "/admin/tecnicos" @{
    nombre = "Tecnico"; apellido = "Prueba$sufijo"; correo = "tecnico$sufijo@prueba.com"
    contrasena = "clave12345"; telefono = "3000000000"; tipoDocumento = "CC"
    numeroDocumento = "9$sufijo"; direccionBase = "Fusagasuga"; radioCoberturaKm = 10
    latitudBase = 4.3365; longitudBase = -74.3638
} $admin.token).datos
Verificar ($tecnico.id -gt 0) "Se crea el técnico con coordenadas base"
$ubicacion = (Llamar "PATCH" "/admin/tecnicos/$($tecnico.id)/ubicacion" @{ latitud = 4.3365; longitud = -74.3638 } $admin.token).datos
Verificar ($ubicacion.latitudBase -eq 4.3365) "El administrador actualiza la ubicación del técnico"
Verificar ((LlamarEsperandoError "PATCH" "/admin/tecnicos/$($tecnico.id)/ubicacion" @{ latitud = -74.3638; longitud = 4.3365 } $admin.token) -eq 400) "Coordenadas invertidas se rechazan con 400"
$tecnicoConEspecialidad = Llamar "PATCH" "/tecnicos/$($tecnico.id)/especialidades" @{ especialidadIds = @($lavadoras.id) } $admin.token
Verificar (@($tecnicoConEspecialidad.especialidades).Count -eq 1) "Se le asigna Lavadoras"

Titulo "4. Cliente"
$correoCliente = "cliente$sufijo@prueba.com"
Llamar "POST" "/auth/registro/cliente" @{
    nombre = "Cliente"; apellido = "Prueba$sufijo"; correo = $correoCliente
    contrasena = "clave12345"; telefono = "3111111111"; tipoDocumento = "CC"
    numeroDocumento = "8$sufijo"; direccion = "Fusagasuga"
} | Out-Null
$cliente = (Llamar "POST" "/auth/login" @{ correo = $correoCliente; contrasena = "clave12345" }).datos
Verificar ($cliente.token.Length -gt 20) "El cliente inicia sesión"

$fecha = (Get-Date).AddDays(2).ToString("yyyy-MM-dd")
$solicitudBase = @{
    especialidadId = $lavadoras.id; descripcionProblema = "La lavadora no centrifuga"
    direccionServicio = "Fusagasuga, Cundinamarca"; latitud = 4.3365; longitud = -74.3638
}

Titulo "5. Solicitud de servicio (SCRUM-10)"
$solicitud = Llamar "POST" "/solicitudes" $solicitudBase $cliente.token
Verificar ($solicitud.estado -eq "PENDIENTE") "La solicitud queda PENDIENTE (id $($solicitud.id))"
Verificar ((LlamarEsperandoError "POST" "/solicitudes" $solicitudBase $admin.token) -eq 403) "El administrador no puede crear solicitudes (403)"
Verificar ((LlamarEsperandoError "POST" "/solicitudes" $solicitudBase $null) -in 401, 403) "Sin token se rechaza"

Titulo "6. Técnicos y franjas (SCRUM-11, 12 y 13)"
$tecnicos = @(Llamar "GET" "/disponibilidad/tecnicos?especialidadId=$($lavadoras.id)" $null $cliente.token)
Write-Host "   (técnico creado: id $($tecnico.id); el filtro devolvió ids: $(($tecnicos | ForEach-Object { $_.id }) -join ', '))"
Verificar (@($tecnicos | Where-Object { [long]$_.id -eq [long]$tecnico.id }).Count -eq 1) "El técnico aparece en el filtro por especialidad"
$ruta = "/disponibilidad/franjas?especialidadId=$($lavadoras.id)&fecha=$fecha&latitud=4.3365&longitud=-74.3638"
$franjas = @(Llamar "GET" $ruta $null $cliente.token)
Verificar ($franjas.Count -gt 0) "Se ofrecen $($franjas.Count) franjas para $fecha"

Titulo "7. Confirmar cita (SCRUM-14)"
$franja = $franjas[0]
$confirmacion = @{ solicitudId = $solicitud.id; tecnicoId = $franja.tecnicoId; inicio = $franja.inicio; fin = $franja.fin }
$cita = Llamar "POST" "/citas/confirmar" $confirmacion $cliente.token
Verificar ($cita.estado -eq "CONFIRMADA") "La cita queda CONFIRMADA"
$franjasDespues = @(Llamar "GET" $ruta $null $cliente.token)
Verificar (@($franjasDespues | Where-Object { $_.tecnicoId -eq $franja.tecnicoId -and $_.inicio -eq $franja.inicio }).Count -eq 0) "La franja confirmada ya no se ofrece"

Titulo "8. Franja ocupada (409)"
$segundaSolicitud = Llamar "POST" "/solicitudes" $solicitudBase $cliente.token
$conflicto = @{ solicitudId = $segundaSolicitud.id; tecnicoId = $franja.tecnicoId; inicio = $franja.inicio; fin = $franja.fin }
Verificar ((LlamarEsperandoError "POST" "/citas/confirmar" $conflicto $cliente.token) -eq 409) "Confirmar la misma franja devuelve 409"
Verificar ((LlamarEsperandoError "POST" "/citas/confirmar" $confirmacion $cliente.token) -eq 409) "Confirmar dos veces la misma solicitud devuelve 409"

Titulo "9. Logs en la base de datos"
$logs = @(Llamar "GET" "/logs?limite=20" $null $admin.token)
Verificar ($logs.Count -gt 0) "El administrador consulta $($logs.Count) logs"
Verificar (@($logs | Where-Object { $_.tipo -eq "AUDITORIA" }).Count -gt 0) "Hay un log de AUDITORIA por la cita confirmada"
Verificar (@($logs | Where-Object { $_.nivel -eq "WARN" }).Count -gt 0) "Hay un log WARN por la franja ocupada"
Verificar ((LlamarEsperandoError "GET" "/logs" $null $cliente.token) -eq 403) "El cliente no puede ver los logs (403)"

Titulo "10. Microservicio de notificaciones con JWT"
$salud = Invoke-RestMethod -Uri "$UrlNotificaciones/api/notificaciones/salud"
Verificar ($salud.estado -eq "UP") "El microservicio responde"
try {
    Invoke-RestMethod -Method POST -Uri "$UrlNotificaciones/api/notificaciones/cita-confirmada" -ContentType "application/json" -Body "{}" | Out-Null
    $codigoSinToken = 200
} catch {
    $codigoSinToken = [int]$_.Exception.Response.StatusCode
}
Verificar ($codigoSinToken -in 401, 403) "Sin JWT el microservicio rechaza la petición"

Write-Host ""
$fallas = @($resultados | Where-Object { -not $_.Paso })
if ($fallas.Count -eq 0) {
    Write-Host "TODAS LAS PRUEBAS PASARON ($($resultados.Count))" -ForegroundColor Green
} else {
    Write-Host "$($fallas.Count) de $($resultados.Count) pruebas fallaron:" -ForegroundColor Red
    $fallas | ForEach-Object { Write-Host " - $($_.Prueba)" -ForegroundColor Red }
}
