# E2 · Consulta masiva de notificaciones

## Configuración y destino

El 10 de noviembre de 2026 se ejecutaron dos corridas de `GET /api/notificaciones?page=0&size=20` con 30 y 80 usuarios, rampa de 15 s y 10 iteraciones por hilo. Se verificó el destino `http://localhost:18080` en `backend-perf` del proyecto aislado `appointech-perf`, con JDBC hacia `db-perf:5432/appointech_perf`.

## Temporización

Las propiedades registradas, el XML y el log coinciden en una rampa de 15 s. La configuración local de JMeter establece `sampleresult.timestamp.start=true`. No se encontró una redefinición en las propiedades de usuario ni en las de la corrida. Por ello, la ventana es `max(timeStamp + elapsed) - min(timeStamp)`. Se conservaron todas las muestras y el cálculo anterior de 12,685 s para 30 usuarios se mantiene.

En la corrida de 30, el log registra el inicio del hilo 1 a las 18:11:20,978 y el del hilo 30 a las 18:11:35,458, separados por 14,480 s. Sus primeros GET empezaron a las 18:11:22,933 y 18:11:35,461, separados por 12,528 s. El primer hilo pasó 1,955 s entre su arranque y el primer GET, frente a 3 ms en el último. Los primeros cuatro hilos tienen el mismo timestamp de primera muestra. El preprocesador Groovy se ejecuta antes de cada primera petición y prepara la identidad del hilo. La inicialización o compilación de scripts puede contribuir a ese retraso, pero el log no mide ese trabajo por separado. Los logins y GET de preparación habían terminado antes de iniciar JMeter. [Tiempos por hilo](../resultados/e2-temporizacion-30/hilos.csv)

## Resultados comparativos

| Métrica | 30 usuarios | 80 usuarios |
|---|---:|---:|
| GET medidos | 300 | 800 |
| Ventana medida | 12,685 s | 13,059 s |
| Throughput | 23,65 req/s | 61,26 req/s |
| Media | 32,67 ms | 28,94 ms |
| Mediana | 22 ms | 8 ms |
| p90 | 70 ms | 97 ms |
| p95 | 82 ms | 111 ms |
| p99 | 194 ms | 201 ms |
| Máximo | 203 ms | 284 ms |
| HTTP 200 | 300 | 800 |
| Muestras fallidas | 0 (0 %) | 0 (0 %) |
| Fallos de transporte / HTTP / contenido / duración | 0 / 0 / 0 / 0 | 0 / 0 / 0 / 0 |
| HTTP 200 válidas por encima de 800 ms | 0 | 0 |
| Máximo de hilos activos observado | 7 | 19 |

El throughput divide las muestras por la ventana medida. Los percentiles usan rango más cercano, posición `ceil(p × N)` desde 1. Ambos JTL contienen solo el GET medido, con 10 muestras por hilo. El plan asigna una identidad preparada por índice de hilo y la conserva en sus variables. Todas las respuestas pasaron las aserciones de IDs, orden, campos y total para esa identidad. Ambas corridas cumplieron el contrato, el máximo de 800 ms por muestra y menos del 1 % de fallos. Las categorías de fallo podrían solaparse en una muestra.

| Inicio de muestra desde el primer GET | Muestras / media / p95 con 30 | Muestras / media / p95 con 80 |
|---|---:|---:|
| 0 a menos de 5 s | 140 / 36,43 ms / 86 ms | 370 / 55,33 ms / 180 ms |
| 5 a menos de 10 s | 96 / 36,47 ms / 109 ms | 264 / 5,07 ms / 8 ms |
| Desde 10 s hasta el fin | 64 / 18,73 ms / 41 ms | 166 / 8,07 ms / 13 ms |

Con 30 usuarios, el p95 más alto se observó en el segundo intervalo. Con 80, los valores más altos se concentraron en el primero. Los intervalos tienen distintos números de muestras.

## Preparación, integridad y artefactos

| Preparación fuera del JTL | 30 usuarios | 80 usuarios |
|---|---:|---:|
| Duración | 9,320 s | 7,637 s |
| Logins / GET de comprobación | 30 / 30 | 80 / 80 |
| Fallos de preparación | 0 | 0 |
| Variables temporales restantes | 0 | 0 |

Antes y después de cada corrida, la base contenía 3.000 notificaciones, 1.000 leídas y 2.000 sin leer. La huella agregada fue idéntica en las cuatro comprobaciones. Esto compara el estado final de la tabla, incluidas las marcas de lectura. La revisión de los artefactos nuevos no encontró coincidencias con contraseñas sintéticas, patrones JWT ni cabeceras Bearer. JMeter terminó con código 0 en ambas corridas y se generaron los dashboards.

Para la corrida de 80 se verificó antes de medir que Java podía heredar 80 variables ficticias de 4.096 caracteres cada una. Los paquetes reales sumaron 84.285 caracteres en sus valores y el mayor tuvo 1.054. Se verificaron las 16 columnas del JTL y se revisaron 135 archivos nuevos en el escaneo de credenciales. El ejecutor de 80 terminó con código 0.

- **30 usuarios** · [JTL](../resultados/e2-20261110-181106-d14cb52a786b4f92bcdea378fd108bb2/e2.jtl) · [Log](../resultados/e2-20261110-181106-d14cb52a786b4f92bcdea378fd108bb2/jmeter.log) · [Dashboard](../resultados/e2-20261110-181106-d14cb52a786b4f92bcdea378fd108bb2/reporte/index.html) · [Preparación](../resultados/e2-20261110-181106-d14cb52a786b4f92bcdea378fd108bb2/preparacion.json) · [Integridad](../resultados/e2-integridad-20261110-181103-954/)
- **80 usuarios** · [JTL](../resultados/e2-20261110-181450-fd5a622d79614b1fac519c9715ce7a1e/e2.jtl) · [Log](../resultados/e2-20261110-181450-fd5a622d79614b1fac519c9715ce7a1e/jmeter.log) · [Dashboard](../resultados/e2-20261110-181450-fd5a622d79614b1fac519c9715ce7a1e/reporte/index.html) · [Preparación](../resultados/e2-20261110-181450-fd5a622d79614b1fac519c9715ce7a1e/preparacion.json) · [Entorno](../resultados/e2-20261110-181450-fd5a622d79614b1fac519c9715ce7a1e/diagnostico-entorno.json) · [Integridad](../resultados/e2-integridad-20261110-181448-855/)

## Limitaciones

Cada consulta devuelve tres notificaciones por usuario. La preparación consulta previamente los mismos datos y puede calentar las cachés. Las corridas son cortas y secuenciales. Los usuarios configurados representan hilos programados y los máximos observados del JTL cuentan hilos activos en las muestras. Las peticiones simultáneas en vuelo son una medida distinta. El acceso fue directo, sin nginx, y PostgreSQL perf usa tmpfs. Estas corridas carecen de métricas de recursos y no demuestran capacidad sostenida.

Perf quedó iniciado con `backend-perf` y `db-perf` saludables.
