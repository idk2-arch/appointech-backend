# Logs en la base de datos, tipos de logs, jobs y cron

## Logs en la base de datos

La migración `V7__tabla_log_sistema.sql` crea la tabla `log_sistema` (nivel, tipo, origen, usuario, mensaje, fecha). Es una tabla nueva: no modifica las existentes. Se escribe con `RegistroLogPort`, en una transacción propia, así el log se guarda aunque la operación falle y se revierta.

Qué se registra hoy:
- Solicitud de servicio creada (NEGOCIO, INFO).
- Cita confirmada (AUDITORIA, INFO).
- Franja ocupada, solicitud o cliente no encontrado (NEGOCIO).
- Fallos al contactar el microservicio de notificaciones (INTEGRACION, WARN).
- Limpieza de logs (SISTEMA, INFO).

Consulta, solo para ADMINISTRADOR:

```
GET /api/logs?nivel=WARN&tipo=NEGOCIO&limite=50
```

o directamente en la base:

```sql
SELECT creado_en, nivel, tipo, origen, usuario, mensaje FROM log_sistema ORDER BY creado_en DESC LIMIT 20;
```

## Tipos de logs

Por **nivel de severidad** (de menor a mayor):

| Nivel | Para qué |
|---|---|
| DEBUG | Detalle para desarrolladores al depurar |
| INFO | Hechos normales: algo se hizo bien |
| WARN | Algo raro que no detiene el sistema |
| ERROR | Una operación falló |

Los frameworks añaden TRACE (más detalle que DEBUG) y FATAL (el sistema cae).

Por **categoría**:

| Categoría | Qué guarda |
|---|---|
| AUDITORIA | Quién hizo qué y cuándo (confirmar una cita) |
| NEGOCIO | Eventos y reglas del negocio (franja ocupada) |
| SEGURIDAD | Logins, accesos denegados, tokens inválidos |
| ERROR_APLICACION | Excepciones inesperadas |
| INTEGRACION | Llamadas a otros servicios y APIs |
| SISTEMA | Arranque, tareas programadas, mantenimiento |

Otros tipos habituales: logs de acceso (cada petición HTTP), de rendimiento (tiempos de respuesta) y de transacciones (cambios en la base de datos).

## Qué son los jobs

Un **job** es una tarea que corre sola, sin que un usuario la pida: limpiar datos viejos, enviar recordatorios, generar reportes. Se dispara por un horario o un evento.

En este proyecto hay dos formas de programarlo:

| Nivel | Cómo | Dónde |
|---|---|---|
| Aplicación | `@Scheduled` de Spring | `LimpiezaLogsJob`, cada día a las 3:00 |
| Servidor | cron del sistema o CronJob de Kubernetes | `k8s/07-cronjob-limpieza-logs.yaml` |

El job de aplicación se configura con `logs.limpieza-cron` y `logs.retencion-dias` (por defecto 30 días). Con varias réplicas del backend cada una lo ejecuta, y no pasa nada porque borrar dos veces es inofensivo.

## Cron a nivel del servidor

Cron es el programador de tareas de Linux. Cada línea de `crontab -e` tiene cinco campos:

```
minuto hora día-del-mes mes día-de-la-semana comando
```

Ejemplos:

| Expresión | Significado |
|---|---|
| `0 3 * * *` | Todos los días a las 3:00 |
| `*/15 * * * *` | Cada 15 minutos |
| `0 8 * * 1-5` | De lunes a viernes a las 8:00 |

Limpieza de logs desde el servidor, sin pasar por la aplicación:

```
0 3 * * * docker exec appointech-db psql -U appointech -d appointech -c "DELETE FROM log_sistema WHERE creado_en < NOW() - INTERVAL '30 days';"
```

Spring usa seis campos, con segundos al inicio: `0 0 3 * * *`.
