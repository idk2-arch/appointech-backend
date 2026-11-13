# Entorno de pruebas de rendimiento (perfil `perf`)

Entorno **aislado** de tu base de desarrollo: proyecto Docker propio (`appointech-perf`), PostgreSQL temporal en memoria (tmpfs, puerto 55432, base `appointech_perf`) y backend con el perfil Spring `perf` (puerto 18080).

| Archivo | Para qué |
| --- | --- |
| `docker-compose.perf.yml` | Base temporal + backend perf. CPU/memoria/pool por variables (`PERF_BACKEND_CPUS`, `PERF_BACKEND_MEM`, `PERF_DB_POOL_MAX`, `PERF_UMBRAL_LENTO_MS`). |
| `iniciar.ps1` / `detener.ps1` | Levanta (genera secretos aleatorios en `perf\.env.perf`, ignorado por git) / detiene (sin `-v`). |
| `src/main/resources/application-perf.properties` | Perfil: sin `show-sql`, `mapas.proveedor=haversine`, sin Groq/Google/correo, pool configurable, estado del pool en logs. |
| `src/main/resources/db/perf/R__datos_carga_perf.sql` | Datos de carga (solo con el perfil `perf`; fuera de `db/migration`). |
| `.../infrastructure/perf/` | Medición de tiempos: registra `[LENTO]` en los métodos que superan el umbral (solo perfil `perf`, Spring AOP sin dependencias nuevas). |
| `jmeter/` | **Carga y estrés con JMeter (herramienta principal)**: ver `jmeter/README.md`. |
| `k6/`, `ejecutar.ps1` | Primera versión con k6 (no es el entregable principal). |

## Comandos

```powershell
powershell -File perf\iniciar.ps1 -Cpus 2 -Memoria 1g -PoolMax 10     # primera vez ~3 min (construye la imagen)
# ... pruebas (ver perf\jmeter\README.md) ...
powershell -File perf\detener.ps1
```

Datos sembrados (contraseña `Perf12345` para todos): `cliente1..5000@perf.test`, `tecnico1..50@perf.test` (2 especialidades c/u, radios de 5 a 30 km), franjas de hoy a +13 días, 340 citas, pagos, 100 calificaciones, 40 solicitudes manuales y 3000 notificaciones. El administrador es el de la migración V4.

Servicios externos en perf: sin `NOTIFICACIONES_URL` no se llama al microservicio (no hay correo), sin `GROQ_API_KEY` el pre-diagnóstico queda no disponible, distancias en línea recta (sin OpenRouteService) y Google no se invoca. El almacenamiento de fotos apunta a un puerto cerrado (solo un WARN al arrancar).

Cuidado: cada ejecución de carga crea citas en la base perf; para repetir con datos limpios, `detener.ps1` + `iniciar.ps1`.
