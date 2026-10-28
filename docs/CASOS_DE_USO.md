# Casos de uso y trazabilidad con Jira

| Historia de usuario | Caso de uso | Endpoint | Rol | Código |
|---|---|---|---|---|
| [SCRUM-10](https://rossas2111.atlassian.net/browse/SCRUM-10) RF02 Crear solicitud de servicio | Crear solicitud de servicio | `POST /api/solicitudes` | CLIENTE | [CrearSolicitudServicioUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/CrearSolicitudServicioUseCaseImpl.java) |
| [SCRUM-11](https://rossas2111.atlassian.net/browse/SCRUM-11) RF27 Filtrar técnicos por especialidad | Filtrar técnicos por especialidad | `GET /api/disponibilidad/tecnicos` | CLIENTE, ADMINISTRADOR | [FiltrarTecnicosPorEspecialidadUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/FiltrarTecnicosPorEspecialidadUseCaseImpl.java) |
| [SCRUM-12](https://rossas2111.atlassian.net/browse/SCRUM-12) RF19 Calcular franjas horarias | Calcular franjas disponibles | `GET /api/disponibilidad/franjas` | CLIENTE | [CalcularFranjasDisponiblesUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/CalcularFranjasDisponiblesUseCaseImpl.java) |
| [SCRUM-13](https://rossas2111.atlassian.net/browse/SCRUM-13) RF03 Mostrar franjas sugeridas | Entregar franjas al cliente | `GET /api/disponibilidad/franjas` | CLIENTE | mismo caso de uso que SCRUM-12 |
| [SCRUM-14](https://rossas2111.atlassian.net/browse/SCRUM-14) RF04 Confirmar cita | Confirmar cita en una franja | `POST /api/citas/confirmar` | CLIENTE | [ConfirmarCitaUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ConfirmarCitaUseCaseImpl.java) |
| [SCRUM-17](https://rossas2111.atlassian.net/browse/SCRUM-17) RF13 Bloques de indisponibilidad | Crear, listar y eliminar bloques | `POST`, `GET` y `DELETE /api/tecnico/bloques` | TECNICO | [CrearBloqueUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/CrearBloqueUseCaseImpl.java), [ListarBloquesUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ListarBloquesUseCaseImpl.java), [EliminarBloqueUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/EliminarBloqueUseCaseImpl.java) |
| [SCRUM-15](https://rossas2111.atlassian.net/browse/SCRUM-15) RF10 Ruta optimizada del día | Consultar citas del día y calcular la ruta | `GET /api/tecnico/citas` y `GET /api/tecnico/ruta` | TECNICO | [ConsultarCitasDelTecnicoUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ConsultarCitasDelTecnicoUseCaseImpl.java), [CalcularRutaDelDiaUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/CalcularRutaDelDiaUseCaseImpl.java) |
| [SCRUM-16](https://rossas2111.atlassian.net/browse/SCRUM-16) RF20 Recalcular ruta ante cambios | La ruta se calcula en cada petición, sin guardarla | `GET /api/tecnico/ruta` | TECNICO | mismo caso de uso que SCRUM-15 |

## Casos de uso técnicos (no son historias de Jira)

| Caso de uso | Endpoint o disparador | Código |
|---|---|---|
| Consultar logs del sistema | `GET /api/logs` (ADMINISTRADOR) | [ConsultarLogsUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ConsultarLogsUseCaseImpl.java) |
| Listar citas de todos los técnicos (respuesta `{contenido:[{id, fecha, horaInicio, horaFin, estado, nombreCliente, nombreTecnico, servicio, direccion, tecnicoFotoUrl}], pagina, tamanio, totalElementos, totalPaginas}`; filtros `tecnicoId`, `estado`, `fechaDesde`, `fechaHasta`; paginado con `pagina` y `tamanio`, máx. 50) | `GET /api/admin/citas` (ADMINISTRADOR) | [ConsultarCitasAdminUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ConsultarCitasAdminUseCaseImpl.java) |
| Limpiar logs antiguos | Job programado cada día a las 3:00 | [LimpiarLogsAntiguosUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/LimpiarLogsAntiguosUseCaseImpl.java) |

Cada clase `UseCaseImpl` lleva en su Javadoc el enlace a su historia de Jira. Las historias de Jira no se modificaron.

Nota de trazabilidad: la descripción de SCRUM-14 en Jira habla de ruta optimizada, aunque su título es "Confirmar cita". Se deja documentado sin cambiar Jira.

## Agenda del técnico (Sprint 3): reglas y pruebas con curl

Todos los endpoints exigen el rol TECNICO. El técnico se identifica por el correo del JWT, nunca por un id en la URL o el body. Las respuestas exitosas son JSON directo. Los errores de negocio usan el formato `{ timestamp, status, error }` de `AgendamientoExceptionHandler`. Los errores de validación del body (por ejemplo, falta `inicio`) usan ProblemDetail de `GlobalExceptionHandler`.

Reglas:

- Un bloque no puede terminar antes de empezar, ni empezar en el pasado (400).
- Un bloque no puede cruzar una cita CONFIRMADA, EN_CAMINO o EN_PROCESO, ni otro bloque propio (409).
- Borrar un bloque que no existe o es de otro técnico responde 404.
- La ruta ordena las paradas por hora (las citas tienen hora fija) y calcula cada tramo por carretera con `CalculadoraDistanciaPort` (ORS, con respaldo Haversine). No se guarda nada: cada petición recalcula la ruta, así que refleja citas nuevas, canceladas y bloques (SCRUM-16).
- Sin ubicación base del técnico, `GET /ruta` responde 409. Si una cita del día no tiene coordenadas válidas, responde 409 con el id de esa cita.
- `alcanzaATiempo` es falso si `fin de la cita anterior + viaje + ruta.margen-minutos` supera el inicio de la cita. En la primera parada es siempre verdadero, y `horaEstimadaLlegada` es la hora de salida de la base para llegar justo.

```bash
# TOKEN_TECNICO = token del login de un usuario con rol TECNICO
BASE=http://localhost:8080/api

# Crear bloque (201)
curl -X POST $BASE/tecnico/bloques -H "Authorization: Bearer $TOKEN_TECNICO" -H "Content-Type: application/json" \
  -d '{"inicio":"2030-01-11T09:00:00","fin":"2030-01-11T11:00:00","motivo":"Cita médica"}'

# Listar bloques (por defecto desde hoy); desde y hasta son opcionales
curl "$BASE/tecnico/bloques?desde=2030-01-01&hasta=2030-01-31" -H "Authorization: Bearer $TOKEN_TECNICO"

# Eliminar bloque propio (204); ajeno o inexistente (404)
curl -i -X DELETE $BASE/tecnico/bloques/1 -H "Authorization: Bearer $TOKEN_TECNICO"

# Bloque que cruza una cita confirmada (409)
curl -i -X POST $BASE/tecnico/bloques -H "Authorization: Bearer $TOKEN_TECNICO" -H "Content-Type: application/json" \
  -d '{"inicio":"2030-01-11T10:00:00","fin":"2030-01-11T12:00:00"}'

# Mis citas activas del día
curl "$BASE/tecnico/citas?fecha=2030-01-11" -H "Authorization: Bearer $TOKEN_TECNICO"

# Ruta del día
curl "$BASE/tecnico/ruta?fecha=2030-01-11" -H "Authorization: Bearer $TOKEN_TECNICO"
```

Propiedades nuevas (todas con valor por defecto): `ruta.margen-minutos` (`RUTA_MARGEN_MINUTOS`, 0), `ruta.tolerancia-retraso-minutos` (`RUTA_TOLERANCIA_MINUTOS`, 5), `mapas.haversine.velocidad-kmh` (`HAVERSINE_VELOCIDAD_KMH`, 40, solo para estimar minutos del respaldo) y `app.zona-horaria` (`APP_ZONA_HORARIA`, America/Bogota).

## Fotos de perfil

| Caso de uso | Endpoint | Rol | Código |
|---|---|---|---|
| Guardar o reemplazar la foto propia (`{"imagenBase64": "data:image/jpeg;base64,..."}` → `200 {"fotoUrl": "/api/fotos/<clave>"}`) | `PUT /api/perfil/foto` | cualquier usuario autenticado | [ActualizarFotoPerfilUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ActualizarFotoPerfilUseCaseImpl.java) |
| Borrar la foto propia (`204`) | `DELETE /api/perfil/foto` | cualquier usuario autenticado | [ActualizarFotoPerfilUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ActualizarFotoPerfilUseCaseImpl.java) |
| Guardar o reemplazar la foto de otro usuario | `PUT /api/admin/usuarios/{id}/foto` | ADMINISTRADOR | [ActualizarFotoUsuarioUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ActualizarFotoUsuarioUseCaseImpl.java) |
| Ver una foto (bytes con `Content-Type` y `Cache-Control`; la clave es un UUID, `404` si no existe) | `GET /api/fotos/{clave}` | público, sin token (solo este GET) | [ObtenerFotoUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ObtenerFotoUseCaseImpl.java) |

Reglas de la imagen ([ValidadorImagenFoto](../src/main/java/com/appointech/appointech_backend/domain/models/ValidadorImagenFoto.java)): base64 estricto, máximo 1 MB ya decodificado y solo JPEG, PNG o WebP comprobados con los bytes de cabecera (se rechazan SVG, GIF y cualquier otro). Errores: `400` (formato ProblemDetail) y `503` si el bucket no responde. Al reemplazar o borrar se elimina el objeto anterior. `fotoUrl` (null si no hay foto) se agrega al final de las respuestas de usuario; dentro de una cita el campo se llama `tecnicoFotoUrl`. Respuestas con foto: `UsuarioResponse` (perfil y registros), `AuthResponse` (login), `UsuarioAdminResponse`, `TecnicoResponse`, `TecnicoDisponibleResponse`, `ClienteAdminResponse`, `CitaResponse` y `CitaDetalleResponse` (foto del técnico, en el campo `tecnicoFotoUrl`, para el cliente y el administrador), `CitaAdminItemResponse` (`tecnicoFotoUrl`, foto del técnico de cada fila de `GET /api/admin/citas`; null si no hay técnico o foto) y `ClienteResumenResponse` (foto del cliente, para el técnico).

## Fotos del electrodoméstico de la cita

| Caso de uso | Endpoint | Quién | Código |
|---|---|---|---|
| Adjuntar una foto (`{"imagenBase64": "data:image/jpeg;base64,..."}` → `201 {"id": n, "url": "/api/citas/{citaId}/fotos/{id}"}`); máximo 3 por cita, solo con la cita PENDIENTE o CONFIRMADA (`409` si se excede el límite o el estado no lo permite) | `POST /api/cliente/citas/{citaId}/fotos` | solo el cliente dueño de la cita | [AgregarFotoCitaUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/AgregarFotoCitaUseCaseImpl.java) |
| Quitar una foto (`204`; borra también el objeto del bucket), mismas reglas | `DELETE /api/cliente/citas/{citaId}/fotos/{fotoId}` | solo el cliente dueño de la cita | [EliminarFotoCitaUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/EliminarFotoCitaUseCaseImpl.java) |
| Ver una foto (bytes con `Content-Type` y `Cache-Control: private`); `403` para cualquier otro, `404` si la cita o la foto no existen | `GET /api/citas/{citaId}/fotos/{fotoId}` | **requiere JWT**; cliente dueño, técnico asignado a la cita y administrador (la pertenencia la decide el caso de uso en `VerificadorAccesoCita`, no el rol) | [ObtenerFotoCitaUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ObtenerFotoCitaUseCaseImpl.java) |

La imagen se valida igual que la foto de perfil ([ValidadorImagenFoto](../src/main/java/com/appointech/appointech_backend/domain/models/ValidadorImagenFoto.java): base64 estricto, máximo 1 MB, JPEG, PNG o WebP por bytes de cabecera, sin SVG). El objeto se guarda en el mismo bucket (`ALMACENAMIENTO_*`) con la clave `citas/{citaId}/{uuid}`, que la ruta pública `/api/fotos/{clave}` no puede servir; en la tabla `cita_foto` (migración `V13`) solo quedan los metadatos. Si el bucket falla: `503` y un `WARN` de tipo `INTEGRACION`. `fotosElectrodomestico` (`[{"id": n, "url": "..."}]`, lista vacía si no hay fotos) se agrega al final de `CitaDetalleResponse` (detalle del cliente y del administrador) y de `CitaTecnicoResponse` (agenda del técnico, `GET /api/tecnico/citas`).

## Perfil propio

| Caso de uso | Endpoint | Quién | Implementación |
|---|---|---|---|
| Ver mis datos: `id, nombre, apellido, correo, telefono, tipoDocumento, numeroDocumento, rol, direccion, radioCoberturaKm, especialidades, fotoUrl` (`direccion` es la del cliente o la base del técnico; radio y especialidades solo para técnico, de solo lectura) | `GET /api/perfil` | cualquier usuario autenticado | [ConsultarPerfilPropioUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ConsultarPerfilPropioUseCaseImpl.java) |
| Editar `nombre`, `apellido`, `telefono` y `direccion` (opcional; se ignora para el administrador); devuelve el mismo cuerpo del GET | `PUT /api/perfil` | cualquier usuario autenticado | [ActualizarPerfilPropioUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ActualizarPerfilPropioUseCaseImpl.java) |

El usuario sale siempre del JWT. Correo, documento, rol, estado, especialidades y radio no se pueden cambiar aquí: si vienen en el cuerpo se ignoran. Al cambiar la dirección no se recalculan las coordenadas (no existe un servicio de geocodificación en el proyecto).
