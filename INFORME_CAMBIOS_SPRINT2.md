# Informe de cambios - AppoinTech Sprint 2 (backend integrado)

Base: appointech-backend-main. Regla: solo agregar código.

## Archivos existentes modificados (solo líneas agregadas, 0 eliminadas)

1. domain/ports/out/TecnicoRepositoryPort.java: + buscarActivosPorEspecialidad, + buscarPorIdBloqueando
2. infrastructure/repositories/TecnicoJpaRepository.java: + 2 consultas (técnicos activos por especialidad y bloqueo pesimista del técnico) e imports
3. infrastructure/adapters/TecnicoRepositoryAdapter.java: + implementación de los 2 métodos

## Archivos nuevos

Dominio: Cita, EstadoCita, FranjaDisponible, ConfiguracionFranjas, BloqueIndisponibilidad; excepciones ClienteNoEncontrada, SolicitudNoEncontrada, FranjaNoDisponible, FranjaInvalida; puertos in (4 casos de uso) y out (Cita, BloqueIndisponibilidad, ConfiguracionFranjas, CalculadorDistancia).
Aplicación: CrearSolicitudServicio, FiltrarTecnicosPorEspecialidad, CalcularFranjasDisponibles, ConfirmarCita (UseCaseImpl).
Infraestructura: entidades (Cita, BloqueIndisponibilidad, ConfiguracionFranjas, EstadoCita), repositorios JPA, adaptadores, HaversineDistanciaAdapter, DTO, controladores y AgendamientoExceptionHandler.
Despliegue: Dockerfile y docker-compose.yml (solo el backend; usa la base de datos existente), .env.example. Se agregó .env al .gitignore.
Pruebas: AgendamientoUseCasesTest (6 casos).

## Otros archivos existentes

- .gitignore: + línea `.env`

## No se modificó

SecurityConfig (la seguridad se aplica con @PreAuthorize, ya habilitado), GlobalExceptionHandler, JWT, migraciones Flyway, application.properties, base de datos, Jira.

## Endpoints

| Historia | Método y ruta | Rol |
|---|---|---|
| SCRUM-10 | POST /api/solicitudes | CLIENTE |
| SCRUM-11 | GET /api/disponibilidad/tecnicos?especialidadId | CLIENTE, ADMINISTRADOR |
| SCRUM-12/13 | GET /api/disponibilidad/franjas?especialidadId&fecha&latitud&longitud | CLIENTE |
| SCRUM-14 | POST /api/citas/confirmar | CLIENTE |

Errores: 400 datos o franja inválida, 403 rol, 404 solicitud o cliente, 409 franja ocupada. Formato { timestamp, status, error }.

## Decisiones

- El cliente sale del token (correo), no del body. Una solicitud solo la confirma su dueño.
- Una franja por horario: se ofrece el técnico más cercano.
- La confirmación valida de nuevo disponibilidad, bloquea la fila del técnico (anti-concurrencia) y verifica que la franja coincida con configuracion_franjas.
- Los técnicos se registran sin latitud/longitud base, así que hoy no se les aplica el filtro de radio (distancia 0). Se resuelve geocodificando direccion_base (paso de Google).

## Pendientes

- Compilar con Maven y probar con PostgreSQL (no se pudo en el entorno de generación).
- Postman, geocodificación y Google Routes.

## Ampliación: infraestructura y operación

Nuevos archivos:
- Logs en base de datos: migración V7 (tabla nueva log_sistema), RegistroLogPort, entidad, repositorio, adaptador, LogController (GET /api/logs, ADMINISTRADOR) y los casos de uso ConsultarLogs y LimpiarLogsAntiguos.
- Jobs: LimpiezaLogsJob (@Scheduled) y ProgramacionTareasConfig; CronJob de Kubernetes.
- Microservicio con JWT: carpeta microservicio-notificaciones y NotificadorCitaHttpAdapter (activo con NOTIFICACIONES_URL).
- Docker: balanceador nginx, puertos internos y externos, docker-compose.sonarqube.yml.
- Kubernetes: carpeta k8s (12 recursos).
- CI/CD: .github/workflows/ci.yml; SonarQube: sonar-project.properties.
- Documentación: docs/CASOS_DE_USO.md (con enlaces a Jira), DESPLIEGUE.md, LOGS_Y_JOBS.md, MICROSERVICIOS_JWT.md.

Modificados (solo agregando): CrearSolicitudServicioUseCaseImpl, ConfirmarCitaUseCaseImpl y AgendamientoExceptionHandler, que son archivos propios del Sprint 2 (ahora registran logs y notifican).

Cambio de regla: se agrega una tabla a la base de datos con la migración V7. No se modifica ninguna tabla existente.
