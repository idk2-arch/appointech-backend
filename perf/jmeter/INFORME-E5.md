# E5 — Fases de 50, 100, 200 y 300 usuarios

Se ejecutó una sola corrida de cada fase: 50, 100, 200 y 300 usuarios. No se repitió la carga ni se modificaron límites, BCrypt, código de negocio, rampa, loops o timeouts tras observar los resultados. Los apartados originales documentan cada fase en orden; la comparación final de las cuatro y la recuperación de 300 se encuentran al final. Las referencias previas a fases pendientes describen el estado histórico en aquel momento.

## Configuración y aislamiento

Comando ejecutado desde la raíz:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase 50 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

50 usuarios, rampa de 10 segundos, 20 loops: 1.000 POST `/api/auth/login`. Una identidad sintética distinta por hilo, reutilizada en sus 20 iteraciones. Umbral de 2.000 ms; timeout de conexión de 5.000 ms y respuesta de 10.000 ms. Toda la rampa forma parte del análisis.

El destino directo `http://localhost:18080` pertenece a `backend-perf` del proyecto `appointech-perf`, conectado mediante `db-perf:5432` a `appointech_perf`. Se verificaron servicios, red, conexión y base en tmpfs; no se utilizó nginx de desarrollo. Ambos servicios estaban saludables. Límites efectivos: backend 2 CPUs y 1 GiB; PostgreSQL 1 GiB, sin cuota de CPU configurada. No se reiniciaron servicios.

Las comprobaciones iniciales detectaron actividad concurrente y se pospuso el lanzamiento. Antes del único lanzamiento se obtuvieron cuatro observaciones consecutivas sin otro JMeter o docker stats. No se enviaron muestras E5 durante esa espera. La preparación de datos y componentes duró 540 ms y no envió HTTP; las comprobaciones de recuperación posteriores están fuera del JTL.

## Resultados

JMeter y el ejecutor terminaron con código 0; esto no significa que todas las aserciones pasaran.

| Métrica | Resultado |
|---|---:|
| Solicitudes previstas / medidas | 1.000 / 1.000 |
| Ventana HTTP | 40,693 s |
| Throughput | 24,574 solicitudes/s |
| Media / mediana | 1.646,296 / 1.584 ms |
| p90 / p95 / p99 | 2.597 / 2.957 / 3.789 ms |
| Máximo | 5.499 ms |
| HTTP 200 / otros códigos | 1.000 / 0 |
| Fallos de transporte / contenido | 0 / 0 |
| HTTP 200 válido superior a 2.000 ms | 291 |
| Muestras fallidas | 291 (29,1 %) |
| Máximo de hilos activos observado | 50 |
| Máximo de muestras HTTP en curso según JMeter | 50 |

`timeStamp` representa el inicio. Ventana: `max(timeStamp + elapsed) − min(timeStamp)`, de **2026-11-11T05:31:39.809Z a 05:32:20.502Z**. Throughput: 1.000 / 40,693. Percentiles por rango más cercano, posición `ceil(p × N)` desde 1; mediana convencional. No se recortaron muestras.

Las 291 muestras fallidas tienen exclusivamente incumplimiento de duración: no se dedujo la causa solo por recibir HTTP 200. Se verificó contenido válido en las 1.000 respuestas. Las categorías pueden solaparse en general; aquí no hay otros fallos. Se cumple HTTP y contenido, pero **no** duración por muestra ni el criterio de menos del 1 % de muestras fallidas.

### Evolución por inicio de muestra

Intervalos relativos a la primera petición; el último es parcial.

| Intervalo (s) | Muestras | Fallidas | % fallidas | p95 (ms) |
|---|---:|---:|---:|---:|
| [0, 10) | 185 | 67 | 36,22 | 3.789 |
| [10, 20) | 251 | 108 | 43,03 | 3.038 |
| [20, 30) | 286 | 89 | 31,12 | 2.772 |
| [30, 40) | 271 | 27 | 9,96 | 2.209 |
| [40, 40,693] | 7 | 0 | 0 | 310 |

Los primeros POST de los 50 hilos abarcaron 9,795 s. Sus cantidades por segundo relativo fueron **5, 5, 6, 5, 4, 5, 5, 5, 5, 5**. Separación entre primeros POST consecutivos: 185–209 ms (media 199,898 ms); máximo de un primer POST en cualquier ventana de 100 ms. No se observa la agrupación inicial de ocho peticiones. Retraso entre arranque de hilo y primer POST: 1–11 ms, media 2,74 ms; asignación previa máxima 2 ms.

Hilos configurados, hilos activos y solicitudes simultáneas son conceptos distintos. El solapamiento calculado usa intervalos HTTP del cliente; no demuestra ejecución simultánea en el servidor.

## Recursos

| Servicio | Capturas dentro de ventana HTTP | Máximo CPU muestreado | Máxima memoria muestreada |
|---|---:|---:|---:|
| backend-perf | 20 | 220,33 % | 505,5 MiB |
| db-perf | 20 | 9,50 % | 157,5 MiB |

Hubo 24 capturas por servicio: dos previas, veinte dentro de la ventana y dos posteriores. Entre las 24 capturas, los 23 intervalos reales fueron 1.952–2.057 ms, media 2.023,739 ms. La captura efectiva fue aproximadamente cada dos segundos. Los máximos son valores muestreados, no picos absolutos. CPU es el porcentaje informado por Docker (puede superar el 100 % al sumar núcleos); memoria es del contenedor, no heap Java. Estas mediciones no identifican por sí solas la causa de las demoras ni el consumo específico de BCrypt.

## Recuperación y artefactos

Las tres comprobaciones posteriores, fuera del JTL, confirmaron salud, aislamiento y login funcional HTTP 200 con contenido válido: **143, 64 y 59 ms**. Se realizaron a partir de 05:32:23.803Z, 05:32:29.547Z y 05:32:35.206Z respectivamente. No se incluyen en las 1.000 muestras ni en su throughput.

Ambos contenedores permanecieron saludables, sin reinicio ni OOM registrado y con los mismos límites. El recolector terminó y su CSV cerrado pudo validarse. El dashboard se generó; el JTL contiene únicamente el login medido, con 50 hilos y 20 muestras por hilo. La revisión inicial de privacidad examinó 144 archivos y encontró cero exposiciones de contraseñas seleccionadas, tokens JWT o cabeceras Bearer; la verificación final complementaria queda en las evidencias.

El entorno **se recuperó según las tres comprobaciones funcionales y de salud previstas**. Esto no demuestra retorno de todas las métricas a una línea base ni capacidad sostenida.

## Contexto E1 y limitaciones

E1 con 50 usuarios y rampa de 10 s tenía **cinco loops**, frente a veinte aquí: 250 frente a 1.000 solicitudes. Su JTL registró cero fallos, p95 390 ms, p99 489 ms, máximo 498 ms y máximo de 11 hilos activos. E5 observó 50 hilos activos. Además, E1 consumía una fila de credenciales por iteración y E5 conserva una identidad por hilo. El backend estaba previamente utilizado. No es una comparación controlada que permita atribuir el cambio a una única causa.

La ejecución es corta, en la misma máquina, con acceso directo sin nginx y PostgreSQL en tmpfs. No se demuestra capacidad sostenida ni comportamiento de arranque frío. Al cerrar la fase 50 todavía no se habían ejecutado 100, 200 ni 300; la ejecución posterior de 100 se documenta a continuación.

## Evidencias

Carpeta: `../resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/`.

- [JTL](../resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/e5.jtl), [log](../resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/jmeter.log), [dashboard](../resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/reporte/index.html).
- [Análisis ampliado](../resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/analisis-ampliado.json), `analisis.json`, `evolucion-10s.csv`, `inicios.csv`, `primeros-inicios.csv`, `hilos.csv`.
- [Recursos](../resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/recursos.csv), `limites.json`, `validacion-recursos.json`.
- `preflight.json`, `procesos-previos.json`, `preparacion.json`, `ejecucion.json`, `diagnostico.json`, `estado-antes.json`, `estado-despues.json`, `estado-final.json`, `recuperacion.csv`, `privacidad.json`, `verificacion-final.json` en esa misma carpeta.
- [JTL E1 de contexto](../resultados/e1-20261110-173809-f42763b448d64438bf18c9875ce877d0/e1.jtl).

Solo se añadió documentación y análisis local; se conservaron planes, resultados anteriores y código de negocio. No se ejecutaron pruebas Maven: el alcance fue exclusivamente esta fase de rendimiento.

## Fase 100 — ejecución posterior única

Comando ejecutado desde la raíz, una sola vez:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase 100 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Antes de ejecutar se volvió a acreditar `appointech-perf`, backend directo en `localhost:18080`, JDBC hacia `db-perf` / `appointech_perf`, red y tmpfs, ambos servicios saludables y ausencia de JMeter/recolectores concurrentes. Los límites efectivos coincidieron con los de la fase 50: backend 2 CPUs y 1 GiB; PostgreSQL 1 GiB sin cuota CPU. Se comprobaron los hashes originales de plan, soporte Groovy y ejecutor. Las propiedades efectivas confirman rampa 10 s, 20 loops, umbral 2.000 ms y timeouts 5.000/10.000 ms. Solo se cambió N a 100; no hubo reinicio, reconstrucción ni modificación del backend.

### Comparación con el mismo método

Se conservaron todas las muestras y la rampa. Ventana desde el primer inicio hasta el último final; throughput = muestras / ventana en segundos. Percentiles por rango más cercano `ceil(p × N)`; mediana convencional. Los 20 timeouts de la fase 100 se incluyen en los estadísticos globales como tiempo hasta agotar la espera, no como tiempo final del servidor.

| Métrica | 50 usuarios | 100 usuarios |
|---|---:|---:|
| Solicitudes previstas / medidas | 1.000 / 1.000 | 2.000 / 2.000 |
| Ventana HTTP (s) | 40,693 | 79,553 |
| Throughput (solicitudes/s) | 24,574 | 25,140 |
| Media (ms) | 1.646,296 | 3.455,968 |
| Mediana (ms) | 1.584 | 3.204 |
| p90 (ms) | 2.597 | 5.935 |
| p95 (ms) | 2.957 | 6.966 |
| p99 (ms) | 3.789 | 9.961 |
| Máximo (ms) | 5.499 | 10.016 |
| HTTP 200 | 1.000 (100 %) | 1.980 (99 %) |
| Otros códigos HTTP | 0 (0 %) | 0 (0 %) |
| Transporte | 0 (0 %) | 20 (1 %) |
| Contenido inválido | 0 (0 %) | 0 (0 %) |
| Duración > 2.000 ms, incluidos timeouts | 291 (29,1 %) | 1.502 (75,1 %) |
| HTTP 200 válido > 2.000 ms | 291 (29,1 %) | 1.482 (74,1 %) |
| Muestras fallidas | 291 (29,1 %) | 1.502 (75,1 %) |
| Máximo de hilos activos | 50 | 100 |
| Máximo de muestras HTTP solapadas | 50 | 100 |

La ventana de 100 va de **2026-11-11T14:08:17.231Z a 14:09:36.784Z** (09:08:17.231–09:09:36.784 UTC−05). Los 20 errores conservaron código `Non HTTP response code: java.net.SocketTimeoutException` y mensaje `Non HTTP response message: Read timed out`. No hay respuestas HTTP de error ni fallos de contenido en las respuestas recibidas; el contenido no pudo validarse en los timeouts. Las categorías se solapan: los 20 errores de transporte también superaron 2.000 ms. Por eso no se suman 1.502 y 20 como muestras diferentes.

JMeter y el ejecutor terminaron con código 0; el recolector no tuvo error y el dashboard se generó. Se verificaron exactamente 100 hilos con 20 iteraciones únicas cada uno, todos bajo la etiqueta del login medido. La fase **no cumple** HTTP 200 en todas las muestras, duración máxima de 2.000 ms ni menos del 1 % de fallidas. Las respuestas recibidas sí cumplen contenido. No se ajustó ningún límite para aprobar.

### Evolución de la fase 100

Intervalos según inicio de muestra relativo al primer POST; se conservó la rampa y el último intervalo es parcial.

| Intervalo (s) | Muestras | Fallidas | % fallidas | p95 (ms) |
|---|---:|---:|---:|---:|
| [0, 10) | 232 | 178 | 76,72 | 8.795 |
| [10, 20) | 218 | 191 | 87,61 | 9.843 |
| [20, 30) | 253 | 203 | 80,24 | 5.922 |
| [30, 40) | 267 | 223 | 83,52 | 5.859 |
| [40, 50) | 277 | 231 | 83,39 | 7.579 |
| [50, 60) | 277 | 204 | 73,65 | 6.543 |
| [60, 70) | 259 | 199 | 76,83 | 5.943 |
| [70, 79,553] | 217 | 73 | 33,64 | 3.261 |

Los 100 primeros POST abarcan **9,901 s**, frente a 9,795 s en fase 50. Primeros inicios por segundo: **11, 10, 10, 10, 10, 10, 9, 11, 10, 9**. Separación consecutiva 90–111 ms, media 100,010 ms; como máximo dos primeros POST en una ventana de 100 ms. Retraso hilo→primer POST 1–15 ms, media 3,17 ms; asignación previa máxima 2 ms. No se aprecia una ráfaga inicial de ocho POST. La mayor incorporación por segundo es coherente con duplicar usuarios manteniendo rampa.

Los máximos de 100 hilos activos y 100 intervalos HTTP solapados son observaciones del cliente, no prueba de 100 ejecuciones simultáneas en el servidor. Los hilos reutilizan su identidad en las veinte iteraciones.

### Recursos, recuperación y estado final

| Servicio | Capturas HTTP: 50 / 100 | Máximo CPU muestreado: 50 / 100 | Máxima memoria MiB: 50 / 100 |
|---|---:|---:|---:|
| backend-perf | 20 / 38 | 220,33 % / 221,59 % | 505,5 / 522,1 |
| db-perf | 20 / 38 | 9,50 % / 37,17 % | 157,5 / 160,1 |

Fase 100: 43 capturas por servicio, de las cuales tres fueron previas, 38 dentro de la ventana HTTP y dos posteriores. Los 42 intervalos reales fueron **2.025–2.596 ms**, media **2.088,119 ms**. Fase 50: 24 capturas por servicio, intervalos 1.952–2.057 ms, media 2.023,739 ms. Son máximos muestreados, no picos absolutos; memoria del contenedor, no heap Java. No se atribuye la latencia a una causa concreta a partir de estos máximos.

Las tres comprobaciones de recuperación de fase 100 quedaron fuera del JTL y dieron salud correcta y login HTTP 200 con contenido válido en **354, 87 y 186 ms**, todas por debajo de 2.000 ms. No se suman al throughput ni a las 2.000 muestras. La fase 50 había registrado controles de 143, 64 y 59 ms. Los estados antes/después y la comprobación final de 100 muestran ambos contenedores activos y saludables, reinicios 0, OOM false, sin cambios de límites. El recolector se cerró y no quedaron procesos de JMeter o docker stats.

El escaneo del ejecutor revisó 145 archivos con cero exposiciones de contraseñas, JWT o Authorization Bearer; la comprobación complementaria incluye los artefactos añadidos. El CSV de credenciales no se copió a resultados y no se guardaron cuerpos de login o tokens.

**El entorno se recuperó funcionalmente según los tres controles y sigue iniciado y saludable.** Las fases **200 y 300 permanecen pendientes**. El aumento de latencias y fallos con throughput parecido describe estas dos corridas, no demuestra por sí solo una causa ni capacidad sostenida. Son ejecuciones secuenciales con backend previamente utilizado, misma máquina, carga cerrada, acceso sin nginx y PostgreSQL en tmpfs. No se aisló el efecto del calentamiento ni de la actividad del anfitrión.

### Evidencias de fase 100

Carpeta: `../resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/`.

- [JTL](../resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/e5.jtl), [log](../resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/jmeter.log), [dashboard](../resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/reporte/index.html).
- [Análisis ampliado](../resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/analisis-ampliado.json), `analisis.json`, `evolucion-10s.csv`, `primeros-inicios.csv`, `inicios.csv`, `hilos.csv`.
- [Recursos](../resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/recursos.csv), `limites.json`, `validacion-recursos.json`.
- [Recuperación](../resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/recuperacion.csv), `estado-antes.json`, `estado-despues.json`, `estado-final.json`, `preflight.json`, `ejecucion.json`, `diagnostico.json`, `privacidad.json`, `verificacion-final.json` en esa carpeta.

La fase 50 conserva sus archivos originales. Solo se actualizó documentación y se añadió análisis local; no se modificó código de negocio ni escenarios anteriores y no se hicieron commit, push o cambios de rama. No se ejecutó Maven, dado que esta tarea autoriza únicamente la fase de rendimiento.

## Fase 200 — ejecución posterior única

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase 200 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

El comando se ejecutó una sola vez desde la raíz. Se verificó previamente aislamiento del proyecto `appointech-perf`, backend directo en `localhost:18080`, conexión a `db-perf` / `appointech_perf`, red exclusiva y tmpfs. Ambos contenedores estaban saludables, sin JMeter ni recolector concurrente. Los hashes del plan, Groovy y ejecutor coincidieron con la revisión original; solo cambió N. Se mantuvieron rampa 10 s, 20 loops por hilo, umbral 2.000 ms, conexión 5.000 ms y respuesta 10.000 ms. Los límites efectivos siguieron siendo backend 2 CPUs/1 GiB y PostgreSQL 1 GiB sin cuota CPU. No se reinició ni reconstruyó nada.

### Comparación de las tres fases

Todas incluyen la rampa, con ventana `max(timeStamp + elapsed) − min(timeStamp)`, throughput `N / segundos`, percentiles de rango más cercano `ceil(p × N)` y mediana convencional. Los tiempos de timeout se incluyen como espera observada por JMeter; no indican cuándo terminó el servidor. No se alteraron los JTL anteriores.

| Métrica | 50 usuarios | 100 usuarios | 200 usuarios |
|---|---:|---:|---:|
| Previstas / medidas | 1.000 / 1.000 | 2.000 / 2.000 | 4.000 / 4.000 |
| Ventana HTTP (s) | 40,693 | 79,553 | 158,641 |
| Throughput (solicitudes/s) | 24,574 | 25,140 | 25,214 |
| Media (ms) | 1.646,296 | 3.455,968 | 7.230,833 |
| Mediana (ms) | 1.584 | 3.204 | 8.060,5 |
| p90 (ms) | 2.597 | 5.935 | 10.012 |
| p95 (ms) | 2.957 | 6.966 | 10.015 |
| p99 (ms) | 3.789 | 9.961 | 10.017 |
| Máximo (ms) | 5.499 | 10.016 | 10.889 |
| HTTP 200 | 1.000 (100 %) | 1.980 (99 %) | 2.663 (66,575 %) |
| Otros códigos HTTP | 0 | 0 | 0 |
| Transporte: timeouts | 0 | 20 (1 %) | 1.337 (33,425 %) |
| Contenido inválido recibido | 0 | 0 | 0 |
| Duración > 2.000 ms, incluidos timeouts | 291 (29,1 %) | 1.502 (75,1 %) | 3.785 (94,625 %) |
| HTTP 200 válido > 2.000 ms | 291 (29,1 %) | 1.482 (74,1 %) | 2.448 (61,2 %) |
| Muestras fallidas | 291 (29,1 %) | 1.502 (75,1 %) | 3.785 (94,625 %) |
| Máximo de hilos activos | 50 | 100 | 200 |
| Máximo de muestras HTTP solapadas | 50 | 100 | 200 |

Ventana de fase 200: **2026-11-11T14:18:32.114Z–14:21:10.755Z** (09:18:32.114–09:21:10.755 UTC−05). Los 1.337 errores conservaron código `Non HTTP response code: java.net.SocketTimeoutException` y mensaje `Non HTTP response message: Read timed out`. Todos los timeouts también superaron el umbral. La unión de 2.448 respuestas válidas lentas y 1.337 timeouts suma las 3.785 muestras fallidas; no se suman nuevamente los incumplimientos de duración. Las 215 restantes cumplieron todas las validaciones.

Las 2.663 respuestas recibidas tuvieron HTTP 200 y contenido válido. Los timeouts no permiten validar contenido ni conocer el tiempo final del servidor. La fase no cumple HTTP 200 en todas las muestras, duración máxima de 2.000 ms ni el criterio de menos del 1 % de fallidas. JMeter y el ejecutor terminaron con código 0; esto no convierte la fase en aprobada. No hubo error del recolector y se generó el dashboard. Se verificaron 200 hilos con veinte iteraciones únicas cada uno y únicamente la etiqueta/URL del login medido.

### Evolución temporal de 200

Agrupación por inicio relativo de muestra, incluidos rampa y último intervalo parcial.

| Intervalo (s) | Muestras | Fallidas | % fallidas |
|---|---:|---:|---:|
| [0, 10) | 404 | 334 | 82,67 |
| [10, 20) | 251 | 246 | 98,01 |
| [20, 30) | 280 | 278 | 99,29 |
| [30, 40) | 258 | 257 | 99,61 |
| [40, 50) | 257 | 254 | 98,83 |
| [50, 60) | 224 | 224 | 100 |
| [60, 70) | 269 | 259 | 96,28 |
| [70, 80) | 251 | 251 | 100 |
| [80, 90) | 258 | 253 | 98,06 |
| [90, 100) | 290 | 289 | 99,66 |
| [100, 110) | 202 | 202 | 100 |
| [110, 120) | 210 | 210 | 100 |
| [120, 130) | 229 | 227 | 99,13 |
| [130, 140) | 263 | 245 | 93,16 |
| [140, 150) | 207 | 184 | 88,89 |
| [150, 158,641] | 147 | 72 | 48,98 |

Los primeros POST de los 200 hilos abarcaron **9,923 s**. Inicios por segundo: **21, 20, 20, 20, 20, 20, 20, 20, 20, 19**. Separación consecutiva 26–69 ms, media 49,864 ms; máximo de tres primeros POST en una ventana de 100 ms. Retraso entre arranque de hilo y primer POST: 1–39 ms, media 3,44 ms; asignación previa máxima 8 ms. No se observa la agrupación inicial de ocho peticiones. El ritmo de incorporación aumentó al mantener diez segundos de rampa con más usuarios.

Los 200 intervalos HTTP solapados son muestras en curso según JMeter, no ejecución simultánea demostrada en el servidor ni equivalencia automática con usuarios configurados. La carga es cerrada y cada hilo espera respuesta/timeout antes de continuar.

### Recursos y recuperación de 200

| Servicio | Capturas HTTP | Máximo CPU muestreado | Máxima memoria muestreada |
|---|---:|---:|---:|
| backend-perf | 76 | 228,98 % | 576,1 MiB |
| db-perf | 76 | 11,30 % | 163,3 MiB |

Hubo **82 capturas por servicio**: tres antes, 76 dentro de la ventana y tres después. Los 81 intervalos reales fueron **1.858–2.216 ms**, media **2.082,099 ms**. Los máximos son muestreados, no picos absolutos; memoria del contenedor, no heap Java. CPU es el porcentaje crudo de Docker, agregado entre núcleos. No se concluye la causa de las latencias a partir de estos máximos ni se atribuye automáticamente a BCrypt o PostgreSQL.

Las tres comprobaciones funcionales posteriores, fuera del JTL, confirmaron salud e HTTP 200 con contenido válido en **153, 67 y 61 ms**. Sus inicios de comprobación fueron epochMs 1794406876347, 1794406883035 y 1794406888708; no se incluyen entre las 4.000 muestras ni en el throughput. Ambos contenedores permanecieron activos y saludables, con reinicios 0 y OOM false. No cambiaron límites ni identidades de contenedor. El recolector terminó y no quedaron procesos de JMeter o docker stats. El ejecutor escaneó 145 archivos sin exposición de secretos; la revisión complementaria de los archivos añadidos se conserva por separado.

### Decisión sobre la progresión

**La fase 300 puede intentarse en una ejecución futura autorizada según los criterios operativos definidos**, porque hubo recuperación funcional con tres controles, salud final correcta y ausencia de reinicio, OOM o fallo del ejecutor/recolector. Esto no significa que 200 cumpla los criterios de rendimiento: falló el 94,625 % y hubo 1.337 timeouts. La fase 300 **no se ejecutó** y sigue pendiente; tampoco se avanzó a E6.

El throughput observado cambió poco frente a 100 mientras aumentaron latencias y timeouts. Esta observación no prueba una causa exclusiva ni capacidad sostenida. El backend ya había procesado las fases anteriores; son ejecuciones secuenciales en la misma máquina, con posibles diferencias de calentamiento/actividad del anfitrión, acceso directo sin nginx y PostgreSQL en tmpfs. Los percentiles de espera limitados por timeout no representan los tiempos finales de todas las operaciones del servidor. No se modificaron recursos, BCrypt ni umbrales para mejorar el resultado.

### Evidencias de fase 200

Carpeta: `../resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/`.

- [JTL](../resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/e5.jtl), [log](../resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/jmeter.log), [dashboard](../resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/reporte/index.html).
- [Análisis ampliado](../resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/analisis-ampliado.json), `analisis.json`, `evolucion-10s.csv`, `primeros-inicios.csv`, `inicios.csv`, `hilos.csv`.
- [Recursos](../resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/recursos.csv), `limites.json`, `validacion-recursos.json`.
- [Recuperación](../resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/recuperacion.csv), `estado-antes.json`, `estado-despues.json`, `estado-final.json`, `preflight.json`, `ejecucion.json`, `diagnostico.json`, `privacidad.json`, `verificacion-final.json` en esa carpeta.

Se conservaron íntegros los resultados de 50 y 100. Solo se actualizó documentación y se añadió análisis local y evidencia de esta fase; sin cambios de arquitectura, dominio, DTO, migraciones, frontend ni configuración de negocio. No se ejecutó Maven ni se hicieron commit, push o cambios de rama. Perf queda iniciado y saludable según la comprobación final registrada.

## Fase 300 — cierre de E5

Se ejecutó una sola vez desde la raíz:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase 300 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

La preparación volvió a confirmar ambos servicios saludables y aislados en `appointech-perf`, destino directo `localhost:18080`, JDBC hacia `db-perf` / `appointech_perf`, red exclusiva y tmpfs. No había JMeter ni recolectores concurrentes. Los hashes originales del plan, soporte y ejecutor coincidieron. Se mantuvieron rampa 10 s, 20 loops, umbral 2.000 ms, conexión 5.000 ms, respuesta 10.000 ms, backend 2 CPUs/1 GiB y PostgreSQL 1 GiB sin cuota CPU. Solo cambió N. No hubo reinicios ni cambios de configuración para mejorar resultados.

### Tabla final comparable

Mismos métodos: se incluye toda la rampa; ventana desde mínimo inicio hasta máximo inicio+elapsed; throughput = muestras/ventana; percentiles por rango más cercano `ceil(p×N)` desde 1; mediana convencional. Los timeouts forman parte de estos estadísticos como tiempo hasta que JMeter dejó de esperar, no como tiempo final del servidor.

| Métrica | 50 | 100 | 200 | 300 |
|---|---:|---:|---:|---:|
| Logins previstos / muestras medidas | 1.000 / 1.000 | 2.000 / 2.000 | 4.000 / 4.000 | 6.000 / 6.000 |
| Ventana (s) | 40,693 | 79,553 | 158,641 | 210,158 |
| Throughput total (muestras/s) | 24,574 | 25,140 | 25,214 | 28,550 |
| Media (ms) | 1.646,296 | 3.455,968 | 7.230,833 | 9.621,398 |
| Mediana (ms) | 1.584 | 3.204 | 8.060,5 | 10.009 |
| p90 (ms) | 2.597 | 5.935 | 10.012 | 10.016 |
| p95 (ms) | 2.957 | 6.966 | 10.015 | 10.017 |
| p99 (ms) | 3.789 | 9.961 | 10.017 | 10.022 |
| Máximo (ms) | 5.499 | 10.016 | 10.889 | 10.317 |
| HTTP 200 | 1.000 | 1.980 | 2.663 | 642 |
| Otros códigos HTTP | 0 | 0 | 0 | 0 |
| Timeouts de transporte | 0 | 20 | 1.337 | 5.358 |
| Contenido inválido recibido | 0 | 0 | 0 | 0 |
| HTTP 200 válido > 2.000 ms | 291 | 1.482 | 2.448 | 578 |
| Duración > 2.000 ms, incluidos timeouts | 291 | 1.502 | 3.785 | 5.936 |
| Muestras fallidas únicas | 291 | 1.502 | 3.785 | 5.936 |
| Porcentaje fallido | 29,1 % | 75,1 % | 94,625 % | 98,933 % |
| Máximo hilos activos / intervalos HTTP solapados | 50 / 50 | 100 / 100 | 200 / 200 | 300 / 300 |

Ventana de 300: **2026-11-11T14:30:24.965Z–14:33:55.123Z**, equivalente a 09:30:24.965–09:33:55.123 UTC−05. Todas las 642 respuestas recibidas fueron HTTP 200 con contenido válido (10,7 % de las muestras). Hubo 5.358 errores `Non HTTP response code: java.net.SocketTimeoutException`, mensaje `Non HTTP response message: Read timed out` (89,3 %). El contenido de esos timeouts no pudo verificarse.

Las 578 respuestas válidas lentas (9,633 %) más 5.358 timeouts suman **5.936 muestras fallidas únicas (98,933 %)**. Los timeouts también superaron 2.000 ms: no se duplican al contar fallos. Solo 64 muestras cumplieron todos los criterios (1,067 %). No hubo otros errores HTTP ni contenido inválido recibido. No se cumplen HTTP 200 para todas, duración máxima ni menos del 1 % fallido. JMeter y el ejecutor devolvieron 0; esto no significa aprobación. Se generó dashboard y se validaron 300 hilos, veinte iteraciones únicas cada uno, y únicamente POST de login medidos.

### Evolución de la fase 300

Intervalos por inicio relativo; se conserva toda la rampa. No hubo nuevos inicios entre 210 y 210,158 s, aunque la última muestra terminó a 210,158 s.

| Intervalo (s) | Muestras | Fallidas |
|---|---:|---:|
| [0, 10) | 502 | 439 |
| [10, 20) | 319 | 318 |
| [20, 30) | 316 | 316 |
| [30, 40) | 308 | 308 |
| [40, 50) | 300 | 300 |
| [50, 60) | 300 | 300 |
| [60, 70) | 300 | 300 |
| [70, 80) | 300 | 300 |
| [80, 90) | 300 | 300 |
| [90, 100) | 301 | 301 |
| [100, 110) | 299 | 299 |
| [110, 120) | 299 | 299 |
| [120, 130) | 299 | 299 |
| [130, 140) | 300 | 300 |
| [140, 150) | 300 | 300 |
| [150, 160) | 299 | 299 |
| [160, 170) | 293 | 293 |
| [170, 180) | 275 | 275 |
| [180, 190) | 238 | 238 |
| [190, 200) | 148 | 148 |
| [200, 210) | 4 | 4 |
| [210, 210,158] | 0 | 0 |

Los primeros POST de los 300 hilos abarcaron **9,853 s**, con cantidades por segundo **31, 31, 30, 30, 30, 30, 31, 30, 30, 27**. Separación consecutiva 19–58 ms, media 32,953 ms; máximo cuatro primeros POST en una ventana de 100 ms. Retraso hilo→primer POST 0–19 ms, media 2,733 ms; asignación máxima 7 ms. No aparece la agrupación inicial de ocho solicitudes. Los 300 intervalos HTTP solapados son muestras en curso según JMeter, no ejecución simultánea demostrada en el servidor.

### Recursos y recuperación de 300

| Servicio | Capturas durante HTTP | Máximo CPU muestreado | Máxima memoria muestreada |
|---|---:|---:|---:|
| backend-perf | 102 | 278,05 % | 634,3 MiB |
| db-perf | 102 | 52,47 % | 165,8 MiB |

Se capturaron 108 instantes por servicio: tres previos, 102 dentro de la ventana y tres posteriores. Los 107 intervalos reales fueron **1.683–2.946 ms**, media **2.072,626 ms**. Son máximos muestreados, no picos absolutos; memoria de contenedor, no heap Java. Se conserva CPU cruda de Docker sin convertirla en una prueba de causa o de incumplimiento de cuota. En las tres capturas posteriores el backend aún alcanzó 198,2 % de CPU muestreada; esas capturas no demuestran la finalización de todo trabajo pendiente.

**La recuperación no fue inmediata.** Hubo cuatro controles externos al JTL, no tres intentos todos satisfactorios:

| Intento | Inicio epochMs | Resultado | Duración HTTP (ms) | Consecutivos válidos |
|---|---:|---|---:|---:|
| 1 | 1794407641247 | Transporte; sin respuesta HTTP | 10.567 | 0 |
| 2 | 1794407657847 | HTTP 200, contenido válido | 99 | 1 |
| 3 | 1794407664089 | HTTP 200, contenido válido | 111 | 2 |
| 4 | 1794407671107 | HTTP 200, contenido válido | 66 | 3 |

Los servicios estaban saludables en cada control. El primero registró fallo de transporte; este mecanismo no conserva la excepción original del control, por lo que no se le asigna una clase concreta. Después hubo tres controles consecutivos satisfactorios dentro del plazo previsto. Estas cuatro solicitudes no se suman al JTL ni a su throughput. La recuperación cumple el criterio funcional definido, pero el incidente inicial se conserva y no se oculta como recuperación inmediata.

Los estados antes/después y la comprobación final mostraron ambos contenedores activos y saludables, reinicios 0, OOM false, mismas identidades y límites. El recolector cerró sin error; no quedaron JMeter ni docker stats activos. El ejecutor revisó 145 archivos sin exposición de secretos; la comprobación complementaria se conserva en `verificacion-final.json`.

### Conclusión de las cuatro fases

El throughput total de muestras apenas cambió entre 50, 100 y 200 (**24,574 → 25,140 → 25,214/s**) mientras aumentaron latencias y timeouts. En 300 subió a 28,550/s, pero **incluye 5.358 timeouts**: no representa una mejora equivalente de autenticaciones exitosas. HTTP 200 recibidos divididos por cada ventana son aproximadamente **24,574; 24,889; 16,786 y 3,055/s**, respectivamente. En carga cerrada, al agotarse la espera el hilo puede emitir su siguiente iteración aunque no se conozca el final del trabajo anterior del servidor; no se interpreta el ritmo de timeouts como capacidad útil.

La mediana terminó cerca del timeout de 10 s y los percentiles superiores están dominados por esperas agotadas. La meseta observada y el deterioro son evidencias de estas corridas, pero no identifican una causa exclusiva sin mediciones por etapa. No se declara un número universal de usuarios soportados ni capacidad sostenida. Las cuatro fases incluyen backend previamente utilizado, ejecución secuencial, misma máquina, acceso directo sin nginx, PostgreSQL en tmpfs y recursos específicos; no son pruebas de arranque frío. Ninguna fase cumplió menos del 1 % de fallidas.

**E5 tiene sus cuatro fases ejecutadas una vez. El entorno logró la recuperación funcional prevista, con un primer control fallido en 300, y queda iniciado y saludable.** No se ejecutó E6 ni otro escenario. No se repitió carga, elevó timeout/umbral, redujo BCrypt o modificó código de negocio/recursos. Solo se actualizaron documentación y análisis; se preservaron íntegros los artefactos previos.

### Evidencias de fase 300

Carpeta: `../resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/`.

- [JTL](../resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/e5.jtl), [log](../resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/jmeter.log), [dashboard](../resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/reporte/index.html).
- [Análisis ampliado](../resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/analisis-ampliado.json), `analisis.json`, `evolucion-10s.csv`, `primeros-inicios.csv`, `inicios.csv`, `hilos.csv`.
- [Recursos](../resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/recursos.csv), `limites.json`, `validacion-recursos.json`.
- [Recuperación](../resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/recuperacion.csv), `estado-antes.json`, `estado-despues.json`, `estado-final.json`, `preflight.json`, `ejecucion.json`, `diagnostico.json`, `privacidad.json`, `verificacion-final.json` en esa carpeta.
