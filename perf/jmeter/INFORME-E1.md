# E1 · Login concurrente de AppoinTech

## Objetivo y configuración

El 10 de noviembre de 2026 se midió el login por correo y contraseña con `POST /api/auth/login`. Se ejecutaron dos corridas con 20 y 50 usuarios configurados, rampa de 10 s, 5 iteraciones por hilo y umbral de 2.000 ms por muestra. Se analizaron todas las muestras del JTL, incluida la rampa.

## Entorno

El destino registrado en ambos JTL fue `http://localhost:18080/api/auth/login`, publicado directamente por `backend-perf` del proyecto `appointech-perf`. La comprobación previa confirmó el perfil `perf`, la red exclusiva de rendimiento y JDBC hacia `db-perf:5432/appointech_perf`. PostgreSQL perf almacena los datos en `tmpfs`, separado de la base de desarrollo. [Compose perf](../docker-compose.perf.yml)

## Resultados

| Métrica | 20 usuarios | 50 usuarios |
|---|---:|---:|
| Solicitudes | 100 | 250 |
| Ventana medida | 9,235 s | 9,472 s |
| Throughput | 10,83 req/s | 26,39 req/s |
| Media | 101,31 ms | 148,64 ms |
| Mediana | 64,5 ms | 98,5 ms |
| p90 | 182 ms | 299 ms |
| p95 | 196 ms | 390 ms |
| p99 | 763 ms | 489 ms |
| Máximo | 763 ms | 498 ms |
| HTTP 200 | 100 | 250 |
| Muestras fallidas | 0 (0 %) | 0 (0 %) |
| Fallos HTTP / transporte / contenido | 0 / 0 / 0 | 0 / 0 / 0 |
| Muestras por encima de 2.000 ms | 0 | 0 |
| Máximo de hilos activos registrado | 6 | 11 |

La ventana va del inicio de la primera muestra al fin de la última. El throughput divide el total de muestras por esa ventana. Los percentiles se calcularon por rango más cercano, en la posición `ceil(p × N)` de la serie ordenada desde 1. En la corrida de 20 usuarios, las posiciones 99 y 100 contienen ambas 763 ms.

Los usuarios configurados son los hilos programados. Los máximos de 6 y 11 corresponden a los hilos activos registrados en las muestras del JTL. Las solicitudes simultáneas en vuelo son una medida distinta.

## Aceptación e integridad

Ambas corridas completaron todas las solicitudes previstas. Todas devolvieron HTTP 200 y superaron las aserciones de JSON válido, `exito` booleano `true` y extracción de un `datos.token` no vacío. Ambas cumplieron el límite de 2.000 ms por muestra y menos del 1 % de fallos.

| Tabla | Filas antes y después en ambas corridas | Huella antes/después |
|---|---:|---|
| `usuario` | 5.051 | Idéntica |
| `cliente` | 5.000 | Idéntica |
| `tecnico` | 50 | Idéntica |

Las huellas agregadas coinciden al comparar el estado antes y después de cada corrida. La comparación no detecta escrituras transitorias revertidas. Se registraron siete comprobaciones previas de disponibilidad en la primera corrida y una en la segunda, fuera de los JTL medidos.

## Evidencias

- **20 usuarios** · [JTL](../resultados/e1-20261110-173448-cdb7e37a9d934eee960c165c926a1b8f/e1.jtl) · [Propiedades](../resultados/e1-20261110-173448-cdb7e37a9d934eee960c165c926a1b8f/ejecucion.json) · [Log](../resultados/e1-20261110-173448-cdb7e37a9d934eee960c165c926a1b8f/jmeter.log) · [Dashboard](../resultados/e1-20261110-173448-cdb7e37a9d934eee960c165c926a1b8f/reporte/index.html)
- **50 usuarios** · [JTL](../resultados/e1-20261110-173809-f42763b448d64438bf18c9875ce877d0/e1.jtl) · [Propiedades](../resultados/e1-20261110-173809-f42763b448d64438bf18c9875ce877d0/ejecucion.json) · [Log](../resultados/e1-20261110-173809-f42763b448d64438bf18c9875ce877d0/jmeter.log) · [Dashboard](../resultados/e1-20261110-173809-f42763b448d64438bf18c9875ce877d0/reporte/index.html)
- **Integridad y comprobaciones** · [20 usuarios](../resultados/e1-verificacion-20261110-173435-030/) · [50 usuarios](../resultados/e1-verificacion-20261110-173807-996/)

## Limitaciones

Las ejecuciones son cortas y la de 50 usuarios se hizo después de la de 20 con el backend ya utilizado, por lo que el calentamiento difiere. E1 usa acceso directo al backend, sin nginx, y una base perf en `tmpfs`. El ejecutor carece de métricas de recursos. Los resultados describen estas corridas y no permiten estimar capacidad sostenida ni atribuir los tiempos a saturación de recursos.
