# Casos de uso y trazabilidad con Jira

| Historia de usuario | Caso de uso | Endpoint | Rol | Código |
|---|---|---|---|---|
| [SCRUM-10](https://rossas2111.atlassian.net/browse/SCRUM-10) RF02 Crear solicitud de servicio | Crear solicitud de servicio | `POST /api/solicitudes` | CLIENTE | [CrearSolicitudServicioUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/CrearSolicitudServicioUseCaseImpl.java) |
| [SCRUM-11](https://rossas2111.atlassian.net/browse/SCRUM-11) RF27 Filtrar técnicos por especialidad | Filtrar técnicos por especialidad | `GET /api/disponibilidad/tecnicos` | CLIENTE, ADMINISTRADOR | [FiltrarTecnicosPorEspecialidadUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/FiltrarTecnicosPorEspecialidadUseCaseImpl.java) |
| [SCRUM-12](https://rossas2111.atlassian.net/browse/SCRUM-12) RF19 Calcular franjas horarias | Calcular franjas disponibles | `GET /api/disponibilidad/franjas` | CLIENTE | [CalcularFranjasDisponiblesUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/CalcularFranjasDisponiblesUseCaseImpl.java) |
| [SCRUM-13](https://rossas2111.atlassian.net/browse/SCRUM-13) RF03 Mostrar franjas sugeridas | Entregar franjas al cliente | `GET /api/disponibilidad/franjas` | CLIENTE | mismo caso de uso que SCRUM-12 |
| [SCRUM-14](https://rossas2111.atlassian.net/browse/SCRUM-14) RF04 Confirmar cita | Confirmar cita en una franja | `POST /api/citas/confirmar` | CLIENTE | [ConfirmarCitaUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ConfirmarCitaUseCaseImpl.java) |

## Casos de uso técnicos (no son historias de Jira)

| Caso de uso | Endpoint o disparador | Código |
|---|---|---|
| Consultar logs del sistema | `GET /api/logs` (ADMINISTRADOR) | [ConsultarLogsUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/ConsultarLogsUseCaseImpl.java) |
| Limpiar logs antiguos | Job programado cada día a las 3:00 | [LimpiarLogsAntiguosUseCaseImpl](../src/main/java/com/appointech/appointech_backend/application/usecases/LimpiarLogsAntiguosUseCaseImpl.java) |

Cada clase `UseCaseImpl` lleva en su Javadoc el enlace a su historia de Jira. Las historias de Jira no se modificaron.

Nota de trazabilidad: la descripción de SCRUM-14 en Jira habla de ruta optimizada, aunque su título es "Confirmar cita". Se deja documentado sin cambiar Jira.