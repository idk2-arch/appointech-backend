# E3: registro concurrente de clientes

## Contrato y configuración

POST `/api/auth/registro/cliente` devuelve HTTP 201, `exito=true` y `datos.id` del usuario creado. Se verifican también los campos esperados de respuesta. El registro cifra con BCrypt, sin cambiar su costo, y guarda usuario y cliente en una transacción. La dirección es texto y esta ruta no invoca correo, geocodificación ni otros servicios externos.

Se confirmó el proyecto `appointech-perf`, backend directo en `http://localhost:18080`, conectado a `db-perf:5432/appointech_perf` mediante su red exclusiva. PostgreSQL usa tmpfs. No se usó desarrollo, reiniciaron servicios ni cambiaron recursos o código de negocio.

Se eligió una creación por hilo porque la guía no fija loops: 20 y 40 registros previstos, rampa de 10 s y cohortes nuevas sin colisiones previas. Umbral provisional: 1.500 ms, derivado del umbral existente de escrituras; no es una exigencia específica del profesor. Timeouts: conexión 5.000 ms y respuesta 10.000 ms. Se conserva toda la rampa.

## Incidentes y ejecución

El intento `e3-20261110-182513-2799289312964b6c94aa23380bcc96f5` falló durante la preparación del recolector porque la resolución de Docker devolvía dos rutas. No envió peticiones ni creó filas: no es un fallo del backend. Se conservó y se corrigió la selección de una única ruta de docker.exe. La validación aislada posterior obtuvo tres muestras por servicio y cerró el recolector.

Comando realmente ejecutado una sola vez para la nueva corrida:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e3.ps1 -Usuarios 20 -Rampa 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Preparación: 521 ms, 20 identidades y cero colisiones. El recolector obtuvo muestras antes de lanzar JMeter. JMeter terminó con código 0 y generó JTL, log y dashboard. El ejecutor terminó con código 1 por un defecto del validador: dividía textualmente el encabezado CSV y rechazaba el nombre válido entre comillas `"e3UsuarioId"`. Se corrigió únicamente esa comprobación usando los nombres interpretados por Import-Csv y se validó estáticamente contra el JTL original, sin repetir carga ni alterar evidencias. El código 0 de JMeter no significa que todas las muestras hayan pasado.

La corrida de 40 usuarios quedó pendiente: los doce fallos de transporte activaron la condición de detenerse. No fueron fallos exclusivamente de duración.

## Resultados

| Métrica | 20 usuarios | 40 usuarios |
|---|---:|---:|
| Solicitudes previstas / medidas | 20 / 20 | 40 / pendiente |
| Ventana HTTP | 12,148 s | — |
| Throughput | 1,646 solicitudes/s | — |
| Media | 9.338,25 ms | — |
| Mediana | 10.004,5 ms | — |
| p90 | 10.086 ms | — |
| p95 | 10.086 ms | — |
| p99 | 10.086 ms | — |
| Máximo | 10.086 ms | — |
| success=false | 20 (100 %) | — |
| HTTP 201 / otros códigos HTTP | 8 / 0 | — |
| Fallos de transporte | 12 | — |
| Fallos de contenido detectados | 0 entre las 8 respuestas recibidas | — |
| Duraciones superiores a 1.500 ms | 20 | — |
| HTTP 201 válido pero lento | 8 | — |
| Máximo de hilos activos observado | 20 | — |

El timestamp representa el inicio. Ventana: mínimo timeStamp hasta máximo timeStamp + elapsed, epoch ms 1794353841102–1794353853250. Throughput: 20 / 12,148. Percentiles por rango más cercano, posición ceil(p × N) desde 1: posiciones 18, 19 y 20 para p90, p95 y p99. Mediana: promedio de las dos posiciones centrales.

Las doce muestras de transporte conservan `Non HTTP response code: java.net.SocketTimeoutException` y `Non HTTP response message: Read timed out`. No recibieron respuesta HTTP evaluable. Las ocho HTTP 201 cumplieron contenido y fallaron únicamente duración. Las categorías se solapan: los doce timeouts también superaron 1.500 ms. No se clasifican por contenido los cuerpos no recibidos. Usuarios configurados e hilos activos observados no equivalen a solicitudes simultáneas demostradas.

## Persistencia e integridad

| Comprobación | Antes | Después |
|---|---:|---:|
| Usuarios | 5.051 | 5.071 |
| Clientes | 5.000 | 5.020 |
| Notificaciones | 3.000 | 3.000 |
| Perfiles huérfanos | 0 | 0 |

Las veinte identidades tienen exactamente un usuario y un cliente, vínculo correcto, rol CLIENTE y campos esperados. Hay cero duplicados y cero usuarios de la cohorte sin perfil. Los ocho datos.id recibidos coinciden con el usuario persistido. Los doce intentos con timeout también persistieron correctamente; no hay un ID recibido que comparar para ellos. El incremento global coincide con la cohorte. Se conservaron los registros.

Pasan persistencia e integridad. No pasan respuesta exitosa recibida para cada solicitud, duración por muestra ni menos del 1 % de muestras fallidas. Entradas únicas sin duplicados no prueban resistencia ante carreras por una misma clave.

## Recursos y protección de datos

El recolector resolvió los contenedores por proyecto y servicio Compose, terminó en finally sin error y no quedaron recolectores activos. Se limpiaron las variables temporales: cero restantes. La revisión posterior de 140 archivos no encontró contraseñas sintéticas, JWT ni cabeceras Authorization con Bearer.

| Servicio | Muestras en ventana HTTP | Máximo CPU muestreado | Máxima memoria en ventana | Límites efectivos |
|---|---:|---:|---:|---|
| backend-perf | 6 | 202,87 % | 448,7 MiB (43,81 %) | 2 CPU; 1 GiB |
| db-perf | 6 | 40,42 % | 153,5 MiB (14,99 %) | Sin cuota CPU explícita; 1 GiB |

Docker expresa CPU con 100 % por CPU lógica. Es un máximo muestreado, no un pico absoluto. Memoria de contenedor no significa heap Java.

Por servicio hubo seis muestras anteriores a la ventana y cuatro posteriores, separadas de las seis medidas. Antes: máximos backend 5,91 % CPU y 445,4 MiB; PostgreSQL 11,02 % y 146,6 MiB. Después: backend 166,83 % y 466,7 MiB; PostgreSQL 5,79 % y 156,4 MiB. Estos valores no se mezclan con los máximos durante HTTP.

Se guardaron 32 filas: 16 capturas de ambos servicios. Intervalo global real: 1,912–2,198 s, media 2,0396 s. Docker stats --no-stream impidió alcanzar la cadencia objetivo aproximada de un segundo. Dentro de la ventana los intervalos fueron 1,998; 2,023; 2,040; 2,052 y 2,016 s. Seis muestras por servicio son escasas para conclusiones sobre recursos. No se atribuyen los timeouts a CPU o PostgreSQL a partir de esta correlación.

## Evidencias y scripts

Carpeta de la corrida: `../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/`.

- [Configuración](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/ejecucion.json), [preparación](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/preparacion.json), [diagnóstico](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/diagnostico.json).
- [JTL](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/e3.jtl), [log](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/jmeter.log), [dashboard](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/reporte/index.html).
- [Antes](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/antes.json), [después](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/despues.json), [reconciliación](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/reconciliacion.csv).
- [Recursos](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/recursos.csv), [límites](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/limites.json).
- [Incidente inicial](../resultados/e3-20261110-182513-2799289312964b6c94aa23380bcc96f5/diagnostico.json), [validación aislada del recolector](../../target/e3-diagnostico-recursos-4a2cea3a11bd467396ba4931a1fac7e4/recursos.csv).

Scripts completos disponibles: [plan](e3-registro.jmx), [ejecutor](ejecutar-e3.ps1), [recolector](recursos-perf.ps1) y [README](README-E3.md). Se revisaron XML, Groovy, sintaxis PowerShell y propiedades. La corrección posterior del validador CSV se verificó sin repetir carga.

Comando para 40, documentado pero NO ejecutado por la condición de parada:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e3.ps1 -Usuarios 40 -Rampa 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

## Limitaciones y estado final

Pocas solicitudes, misma máquina, BCrypt incluido y acceso directo sin nginx. Las corridas estaban previstas secuencialmente, pero solo se ejecutó veinte. No se demuestra capacidad sostenida. PostgreSQL usa tmpfs con fsync y synchronous_commit desactivados: persistencia significa comprobar filas tras las peticiones, no durabilidad ante una caída. Estas métricas no diagnostican la causa de los timeouts.

Perf quedó iniciado, con backend-perf y db-perf saludables. Sin commit, push, cambio de rama, cambios de negocio ni limpieza de datos.

## Reanudación: preparación del generador corregida y nueva corrida de 20

Esta sección añade resultados; conserva íntegros el resultado fallido anterior y su [diagnóstico](DIAGNOSTICO-E3.md). No se ejecutaron 40 usuarios.

### Cambio y validación local

El ejecutor serializa las identidades antes de Java. Un setUp secuencial, sin HTTP, lee los cuerpos y expectativas una vez y prepara un ObjectMapper compartido. Ejercita las mismas fuentes Groovy de preprocesador, postprocesador y aserciones con una respuesta ficticia mediante sus elementos JMeter, llenando la caché real antes de la rampa. El camino previo de cada POST solo asigna referencias y el cuerpo ya serializado. No incorpora barreras, pausas ni consultas a la base.

Setup y teardown llaman a SampleResult.setIgnore(): no entran en el JTL global. El diagnóstico numérico por ordinal se acumula en memoria y se escribe una vez al terminar; arranque de hilo se obtiene del log. No se guardan cuerpos ni contraseñas.

Validación independiente contra TCP loopback `127.0.0.1:59282`, sin AppoinTech: 20 respuestas ficticias válidas, separación entre llegadas 9.340 ms y máximo una llegada en cualquier ventana de 100 ms. La distribución por segundo fue 3,2,2,2,2,2,2,2,2,1; no reapareció el grupo inicial de ocho. El servidor se cerró al terminar. Sus latencias no son resultados de AppoinTech. Evidencias: [validación](../resultados/validacion-generador-e3-20261110-224658-d741e001be254fa1951c4dcff02906f7/validacion.json), [llegadas](../resultados/validacion-generador-e3-20261110-224658-d741e001be254fa1951c4dcff02906f7/llegadas.csv).

Comandos realmente ejecutados desde la raíz, una vez cada uno:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\validar-generador-e3.ps1 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e3.ps1 -Usuarios 20 -Rampa 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Se volvieron a comprobar salud y aislamiento de appointech-perf, red exclusiva, JDBC a appointech_perf y puerto publicado 18080. Cero recolectores previos activos. Recursos, BCrypt, umbral 1.500 ms, timeouts 5.000/10.000 ms y negocio sin cambios. Preparación SQL/cohorte: 779 ms y cero colisiones. Preparación local del generador en Java: 609 ms, cero peticiones HTTP.

### Comparación

| Métrica | Original 20 | Corregida 20 |
|---|---:|---:|
| POST medidos | 20 | 20 |
| Separación entre primer y último inicio HTTP | 5,627 s | 9,400 s |
| Máximo de inicios en 100 ms | 8 | 1 |
| Mayor retraso arranque de hilo → POST | 3.823 ms | 114 ms |
| Ventana HTTP completa | 12,148 s | 9,474 s |
| Throughput | 1,646/s | 2,111/s |
| HTTP 201 válidos | 8 | 20 |
| Timeouts | 12 | 0 |
| Otros fallos HTTP / contenido | 0 / 0 entre respuestas recibidas | 0 / 0 |
| success=false | 20 (100 %) | 1 (5 %) |
| Incumplimientos de 1.500 ms | 20 | 1 |
| Media global | 9.338,25 ms | 259,7 ms |
| Mediana global | 10.004,5 ms | 73 ms |
| p90 global | 10.086 ms | 787 ms |
| p95 global | 10.086 ms | 1.275 ms |
| p99 / máximo global | 10.086 / 10.086 ms | 1.676 / 1.676 ms |
| Máximo de hilos activos observado | 20 | 4 |
| Usuarios y perfiles de cohorte persistidos | 20 / 20 | 20 / 20 |

La nueva ventana fue **2026-11-11T03:48:26.694Z–03:48:36.168Z**, equivalente al 10 de noviembre a las 22:48:26.694–22:48:36.168 UTC−05:00. Throughput: 20/9,474. Percentiles por rango más cercano, ceil(p × 20); mediana por promedio central. Se incluyeron todas las muestras, también la rampa.

La comparación global original mezcla respuestas recibidas y tiempos hasta timeout: no son los tiempos finales del servidor de las doce solicitudes sin respuesta. Para las ocho HTTP 201 originales, media 8.257,375 ms, mediana 8.263 ms, p90/p95/p99 9.974 ms y máximo 9.974 ms. En la nueva corrida las veinte se completaron: media 259,7 ms, mediana 73 ms, p90 787 ms, p95 1.275 ms, p99 y máximo 1.676 ms, mínimo 61 ms. No hay tiempos hasta timeout nuevos que analizar.

La única muestra fallida corresponde al ordinal 1: HTTP 201, contenido válido, ID 5072, duración 1.676 ms; falló exclusivamente Duration Assertion. Pasa respuesta/contenido e integridad, pero no pasan duración máxima por muestra ni errores <1 %. JMeter y ejecutor terminaron con código 0: eso no oculta el incumplimiento de duración.

Inicios por segundo desde el primer POST: **3,2,2,2,2,2,2,2,2,1**. Retraso arranque→POST: 114 ms en el primero y 2–5 ms en los restantes; preparación previa de asignación de referencias: 0–1 ms. La tabla por ordinal está en [tabla-temporal.csv](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/tabla-temporal.csv). Máximo de muestras HTTP en curso según JMeter: 4, sin demostrar ejecución simultánea de cuatro peticiones en el servidor. Los primeros cuatro HTTP terminaron juntos a +1,676 s pese a sus inicios escalonados; no se atribuye ese comportamiento a una fase concreta.

### Integridad y recursos de la nueva cohorte

Antes/después: usuarios 5.071→5.091; clientes 5.020→5.040; notificaciones 3.000→3.000; perfiles huérfanos 0→0. Cada ordinal tiene un usuario y un cliente, ID de respuesta igual al usuario persistido, vínculo y campos correctos. Cero duplicados, usuarios sin perfil o campos incorrectos. No hubo solicitudes con timeout. Se conservaron los registros creados.

El JTL contiene exactamente veinte etiquetas POST E3, sin setup, teardown ni solicitudes de preparación; diagnóstico temporal con veinte ordinales únicos coincidentes con el JTL. Dashboard generado. Variables temporales restantes: 0. Recolector sin error y finalizado; sin docker stats activo al comprobar. La revisión del ejecutor cubrió 144 archivos con cero exposiciones; los artefactos de análisis añadidos se revisaron aparte.

| Servicio | Capturas en ventana HTTP | Máximo CPU muestreado | Máxima memoria de contenedor | Límites |
|---|---:|---:|---:|---|
| backend-perf | 5 | 64,55 % | 451,8 MiB (44,12 %) | 2 CPU, 1 GiB |
| db-perf | 5 | 2,67 % | 149,3 MiB (14,58 %) | Sin cuota CPU explícita, 1 GiB |

Por servicio: dos capturas anteriores, cinco durante HTTP y dos posteriores. Antes: máximos backend 0,26 % CPU/450,6 MiB y base 0,04 %/146,5 MiB. Después: backend 2,68 %/451,3 MiB y base 2,02 %/148,9 MiB. Intervalo real entre capturas 2,011–2,067 s, media 2,030375 s. Cinco capturas son escasas; no son picos absolutos. Memoria del contenedor no equivale al heap Java.

Se obtuvieron cpu.stat antes/después. Backend: delta usage_usec 5.357.661, nr_periods 94, nr_throttled 9 y throttled_usec 1.275.128. PostgreSQL: delta usage_usec 266.340, nr_throttled y throttled_usec 0. Estos deltas corresponden al intervalo que rodea Java, incluida preparación local y dashboard: backend epoch 1794368903348–1794368919194, base 1794368903513–1794368919348. No son exclusivos de la ventana HTTP ni atribuibles únicamente a registros, y no se convierten en porcentaje de latencia. No había referencia anterior de la corrida original para compararlos.

Logs históricos nuevos: a **03:48:27.369Z**, pool 30, idle 28, active 2, waiting 0. A **03:48:28.077Z** hay tres avisos de controlador de 800, 800 y 394 ms, con umbral de log 300 ms. No se pueden asignar por ordinal ni confundir con la duración HTTP total. A **03:48:34.639Z**, pool 30, idle 30, active 0, waiting 0. No hay errores de backend, avisos de agotamiento del pool o errores de PostgreSQL en la ventana consultada; PostgreSQL no produjo líneas. No existe serie continua de esperas ni desglose SQL/BCrypt. Perf sigue saludable, sin reinicios ni OOM observados.

### Evidencias y límites de interpretación

Carpeta nueva: `../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/`.

- [JTL](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/e3.jtl), [log](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/jmeter.log), [dashboard](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/reporte/index.html), [análisis](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/analisis.json).
- [Diagnóstico del generador](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/temporizacion-generador.csv), [preparación local](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/preparacion-generador.json), [veredicto](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/veredicto.json).
- [Antes](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/antes.json), [después](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/despues.json), [reconciliación por identidad](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/reconciliacion.csv).
- [Recursos](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/recursos.csv), [límites](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/limites.json), [cpu.stat antes](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/cpu-stat-antes.json), [cpu.stat después](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/cpu-stat-despues.json), [logs correlacionados](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/logs-correlacion.json).

El backend ya había procesado registros y **no fue reiniciado**. La mejora puede combinar preparación del generador y calentamiento del backend; no se atribuye exclusivamente al generador. No se demostró que el comportamiento del primer uso del backend esté resuelto. Si vuelven los timeouts, el siguiente paso será instrumentar el backend de forma acotada, sin aumentar timeouts para aprobar. No se añadieron JFR, agentes ni cambios transaccionales en esta fase.

Se mantienen las limitaciones de pocas solicitudes, misma máquina, BCrypt incluido, acceso sin nginx y PostgreSQL en tmpfs con fsync y synchronous_commit desactivados. No se demuestra capacidad sostenida ni durabilidad ante caída. Los cuarenta usuarios permanecen pendientes por instrucción expresa. Perf queda iniciado y saludable.

## Intento único de 40 usuarios con el generador corregido

Se intentó una sola vez el comando autorizado, sin modificar el plan, ejecutor, umbrales, BCrypt, negocio o recursos:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e3.ps1 -Usuarios 40 -Rampa 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Antes se confirmó salud de backend-perf y db-perf, proyecto appointech-perf, red exclusiva y tmpfs, JDBC a appointech_perf y publicación de localhost:18080 por backend-perf. No había recolectores activos. El plan conserva el setUp sin HTTP y la preparación de cuerpos anterior a la rampa; las evidencias existentes de su validación local y corrida corregida de 20 muestran llegadas distribuidas. Eso acredita el mecanismo ya validado, pero no una distribución de 40 todavía no medida.

Se creó la cohorte nueva `b4765c198bd94ae7b3492d3323bbe6a3`: 40 identidades y cero colisiones, preparación SQL/cohorte de 550 ms. La configuración efectiva conserva rampa 10 s, un loop, umbral 1.500 ms y timeouts de conexión/respuesta 5.000/10.000 ms.

**El intento se detuvo en preparación, antes de iniciar Java/JMeter.** Import-Csv intentó abrir recursos.csv mientras otro proceso lo utilizaba: IOException / FileOpenFailure en ejecutar-e3.ps1:194, durante la espera de la primera muestra del recolector. Es una carrera compatible con lectura y escritura concurrentes del CSV en Windows. El trabajo del recolector terminó sin error; falló la lectura del ejecutor. No fue un fallo del backend ni se enviaron solicitudes. Se conservó el diagnóstico y no se repitió automáticamente ni se corrigió el ejecutor en esta ejecución.

### Tabla de las tres configuraciones

| Métrica | 20 original | 20 corregida | 40 corregida, intento de preparación |
|---|---:|---:|---|
| POST medidos | 20 | 20 | 0; JMeter no iniciado |
| HTTP 201 válidos | 8 | 20 | No evaluable |
| Otros códigos HTTP | 0 | 0 | No evaluable |
| Transporte / timeouts | 12 / 12 | 0 / 0 | No evaluable |
| Fallos de contenido | 0 entre las 8 respuestas | 0 | No evaluable |
| success=false | 20 (100 %) | 1 (5 %) | No evaluable |
| Superiores a 1.500 ms | 20 | 1 | No evaluable |
| Ventana HTTP | 12,148 s | 9,474 s | Sin ventana |
| Throughput | 1,646/s | 2,111/s | No calculable |
| Media / mediana | 9.338,25 / 10.004,5 ms | 259,7 / 73 ms | No calculable |
| p90 / p95 / p99 | 10.086 / 10.086 / 10.086 ms | 787 / 1.275 / 1.676 ms | No calculable |
| Máximo | 10.086 ms | 1.676 ms | No calculable |
| Máximo de hilos activos | 20 | 4 | Sin hilos JMeter |
| Separación entre primer y último inicio | 5,627 s | 9,400 s | Sin inicios |
| Máximo de inicios en 100 ms | 8 | 1 | No evaluable |
| Usuarios / perfiles de cohorte persistidos | 20 / 20 | 20 / 20 | 0 / 0 |
| Duración por muestra y errores <1 % | No cumple | **No cumple** | Pendiente de medir |

Los percentiles se calcularon por rango más cercano en las dos corridas reales. Los globales originales incluyen tiempos hasta timeout, no tiempos finales del servidor. Para sus ocho respuestas recibidas, media 8.257,375 ms, mediana 8.263 ms y p90/p95/p99/máximo 9.974 ms; los doce timeouts tuvieron media 10.058,833 ms, mediana y p90/p95/p99/máximo 10.086 ms. La corrida corregida de 20 recibió las veinte respuestas: su única muestra fallida fue HTTP 201 válido de 1.676 ms por Duration Assertion. Sigue visible que incumplió 1.500 ms y el criterio de menos del 1 %, aunque JMeter y ejecutor devolvieran 0.

### Integridad, recursos y estado del intento de 40

Se reconciliaron individualmente los cuarenta ordinales: cada uno tiene cero muestras, cero usuarios y cero perfiles persistidos. No existen campos, rol o vínculos nuevos que validar; no se interpretan los camposCorrectos=false de filas inexistentes como corrupción. Cero duplicados, usuarios de cohorte sin perfil o perfiles huérfanos.

Cantidades antes/después idénticas: usuarios 5.091/5.091; clientes 5.040/5.040; notificaciones 3.000/3.000; huérfanos 0/0. La cohorte prevista no quedó creada.

El ejecutor devolvió 1 y codigoJMeter es null. No existen JTL, log de JMeter ni dashboard; por tanto no se inventan latencias, throughput, resultados funcionales ni máximos de hilos para cuarenta. Variables temporales restantes: 0; recolector terminado, cero docker stats activos al comprobar.

Solo hay dos capturas de preparación por servicio, separadas 2,023 s. Máximos muestreados de preparación: backend 0,59 % CPU y 451,5 MiB; PostgreSQL 0,00 % y 147,4 MiB. No son métricas de carga ni picos absolutos, y memoria de contenedor no es heap Java. Límites efectivos conservados: backend 2 CPU/1 GiB; base sin cuota CPU explícita/1 GiB. Solo se guardó cpu.stat posterior; no se calcula un delta sin referencia anterior.

Se conservan ambos JTL previos y el diagnóstico con las mismas huellas SHA-256; el plan y ejecutor tampoco cambiaron. El informe anterior se conserva y esta sección se añade al final. Perf sigue iniciado: ambos servicios saludables, reinicios 0 y OOMKilled=false. No se avanzó a E4.

### Evidencias y siguiente paso pendiente

Carpeta del intento: `../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/`.

- [Fallo de preparación](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/fallo-preparacion.json), [configuración efectiva](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/ejecucion.json), [preflight y huellas](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/preflight.json).
- [Preparación](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/preparacion.json), [diagnóstico de cierre](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/diagnostico.json), [reconciliación individual](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/reconciliacion.csv).
- [Antes](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/antes.json), [después](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/despues.json), [recursos de preparación](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/recursos.csv), [límites](../resultados/e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3/limites.json).

Para un nuevo intento autorizado, la corrección mínima propuesta es tolerar únicamente el bloqueo transitorio de lectura del CSV durante la espera acotada de disponibilidad del recolector, o abrirlo con compartición de lectura/escritura. No consiste en reintentar carga ni cambiar timeouts HTTP. No se aplicó ni se lanzó un nuevo intento.

El backend estaba previamente utilizado y no se reinició. Estas corridas cortas no acreditan arranque frío ni capacidad sostenida; las diferencias pueden combinar generador y calentamiento. Se mantienen acceso directo sin nginx, misma máquina, BCrypt y PostgreSQL en tmpfs con fsync/synchronous_commit desactivados. Cuarenta queda pendiente de medición real, no aprobado ni fallido por rendimiento.

## Corrección del bloqueo del CSV y corrida medida de 40 usuarios

El intento `e3-20261110-225635-b4765c198bd94ae7b3492d3323bbe6a3` permanece conservado como fallo de preparación: cero peticiones y cero registros creados. La corrida siguiente usa otra carpeta y otra cohorte; no reemplaza ese intento ni las dos mediciones de veinte.

### Cambio concreto y verificación sin backend HTTP

`recursos-perf.ps1` crea ahora **muestreo.listo**, una señal independiente, únicamente después de escribir la primera captura completa de los dos servicios. `Esperar-MuestreoPerf` consulta esa señal y el estado del Job, sin abrir recursos.csv. Si no está disponible en doce segundos o el trabajo termina antes de tiempo, falla explícitamente.

En finally, `Detener-MuestreoPerf` solicita cierre, espera Wait-Job, recibe los errores del trabajo y lo retira. Solo entonces interpreta y valida el CSV: columnas, campos numéricos, unidades y dos servicios únicos por captura. El ejecutor guarda validacion-recursos.json y conserva errorRecolector=true ante cualquier fallo de cierre o validación. No se desactivó la captura ni se ignoraron sus errores; el mismo finally funciona ante errores de JMeter.

Verificación aislada en PowerShell **5.1.26100.9711**, sin HTTP ni JMeter: sintaxis de ambos scripts sin errores; arranque/cierre normal y cierre ante excepción simulada con cuatro filas válidas (dos capturas por servicio) cada uno; trabajos retirados. Se comprobó también detección de terminación prematura al arrancar y al cerrar. La función de espera no contiene lectura del CSV. [Resumen de validación](../resultados/validacion-recolector-e3-20261110-230353-aa3e1afc37b54a62aaebcf7d26fecd7a/resumen.json).

Comandos realmente ejecutados:

```powershell
powershell.exe -NoProfile -File target/probar-recolector-e3.ps1
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e3.ps1 -Usuarios 40 -Rampa 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Antes de la corrida se confirmó nuevamente appointech-perf, ambos servicios saludables, red exclusiva, db-perf en tmpfs, JDBC a appointech_perf y localhost:18080 publicado por backend-perf. No había recolectores previos activos. La cohorte nueva tuvo cuarenta identidades y **cero colisiones**. Preparación de cohorte/SQL: 1.147 ms; preparación local del generador: 1.713 ms y cero HTTP. Se mantuvieron una creación por hilo, rampa 10 s, timeouts 5.000/10.000 ms, umbral 1.500 ms, BCrypt y límites de recursos.

### Comparación de las tres corridas medidas

| Métrica | 20 original | 20 corregida | 40 corregida |
|---|---:|---:|---:|
| Solicitudes | 20 | 20 | 40 |
| HTTP 201 válidos | 8 | 20 | 40 |
| Otros códigos HTTP | 0 | 0 | 0 |
| Timeouts / transporte | 12 / 12 | 0 / 0 | 0 / 0 |
| Fallos de contenido detectados | 0 entre 8 respuestas | 0 | 0 |
| success=false | 20 (100 %) | 1 (5 %) | **8 (20 %)** |
| Superiores a 1.500 ms | 20 | 1 | **8** |
| Ventana HTTP | 12,148 s | 9,474 s | 9,625 s |
| Throughput | 1,646/s | 2,111/s | 4,156/s |
| Media | 9.338,25 ms | 259,7 ms | 632,55 ms |
| Mediana | 10.004,5 ms | 73 ms | 109,5 ms |
| p90 | 10.086 ms | 787 ms | 2.155 ms |
| p95 | 10.086 ms | 1.275 ms | 2.666 ms |
| p99 | 10.086 ms | 1.676 ms | 2.950 ms |
| Máximo | 10.086 ms | 1.676 ms | 2.950 ms |
| Máximo de hilos activos observado | 20 | 4 | 13 |
| Separación primer–último inicio HTTP | 5,627 s | 9,400 s | 9,521 s |
| Máximo de inicios en 100 ms | 8 | 1 | 2 |
| Usuarios y perfiles persistidos | 20 / 20 | 20 / 20 | 40 / 40 |
| Duración por muestra y errores <1 % | **No cumple** | **No cumple** | **No cumple** |

Percentiles por rango más cercano, posición ceil(p × N) desde 1. Para cuarenta, p90/p95/p99 son posiciones 36/38/40. Mediana: promedio de posiciones centrales. Se conservan todas las muestras, incluida la rampa. La ventana nueva es **2026-11-11T04:06:41.535Z–04:06:51.160Z**, equivalente al 10 de noviembre a las 23:06:41.535–23:06:51.160 UTC−05:00; throughput = 40/9,625.

Los globales originales mezclan respuestas completadas y tiempos hasta timeout, no tiempos finales de las veinte operaciones del servidor. En aquella corrida, las ocho respuestas completadas tuvieron media 8.257,375 ms, mediana 8.263 ms y p90/p95/p99/máximo 9.974 ms. En las corridas corregidas todas las respuestas se recibieron: no hay tiempos hasta timeout que analizar.

La nueva corrida tuvo código JMeter **0** y código del ejecutor **0**, pero ocho fallos reales de Duration Assertion. Son los ordinales **1–8**, duraciones 2.950, 2.911, 2.666, 2.404, 2.155, 1.906, 1.657 y 1.792 ms. Todos fueron HTTP 201 y contenido válido. No hay solapamientos con fallos HTTP, transporte o contenido porque estas categorías tuvieron cero casos. La aceptación funcional y de integridad pasa; duración máxima y menos del 1 % no pasan. No se ajustaron límites para cambiar ese resultado.

### Llegadas e integridad

Inicios por segundo desde el primer POST: **5,4,4,4,4,4,4,4,4,3**. No reapareció el grupo inicial de ocho. Los dos primeros inicios estuvieron separados 39 ms: no se afirma una separación exacta de 250 ms. El retraso arranque de hilo→POST fue de 234 ms en el primero y 1–15 ms en los demás; la asignación preparada duró 0–2 ms. La [tabla temporal](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/tabla-temporal.csv) contiene los cuarenta ordinales. Máximo calculado de **muestras HTTP en curso según JMeter: 13**; no demuestra ejecución simultánea de trece peticiones en el servidor.

Se reconciliaron una por una las **40 identidades**: exactamente un usuario y un cliente por ordinal, vínculo correcto, rol CLIENTE, campos esperados y datos.id igual al usuario persistido en los cuarenta casos. Cero duplicados, cero usuarios sin perfil, cero campos incorrectos y cero huérfanos. No hubo solicitudes con timeout. Se conservaron los registros.

Cantidades antes/después: usuarios **5.091→5.131**; clientes **5.040→5.080**; notificaciones **3.000→3.000**; huérfanos **0→0**. El incremento coincide exactamente con la nueva cohorte. Comprobar entradas únicas no prueba carreras por una misma clave.

### Recursos y logs disponibles

| Servicio | Capturas durante HTTP | Máximo CPU muestreado | Máxima memoria de contenedor durante HTTP | Límites efectivos |
|---|---:|---:|---:|---|
| backend-perf | 4 | 203,75 % | 452,9 MiB | 2 CPU, 1 GiB |
| db-perf | 4 | 14,09 % | 153,2 MiB | Sin cuota CPU explícita, 1 GiB |

El CSV final validado contiene **24 filas, doce capturas por servicio**: cinco anteriores, cuatro durante HTTP y tres posteriores. Intervalos reales globales 1,991–2,134 s, media 2,046545 s. Antes: máximos backend 7,99 %/451,6 MiB y base 6,08 %/147,2 MiB. Después: backend 59,28 %/452,5 MiB y base 3,11 %/153,4 MiB. No se mezclan con los valores de la ventana HTTP. Docker expresa 100 % por CPU lógica. Cuatro capturas son escasas: sus máximos no son picos absolutos, y memoria de contenedor no es heap Java.

Deltas de cpu.stat en el intervalo que rodea Java, incluidos preparación y dashboard: backend usage_usec +10.797.180, nr_periods +151, nr_throttled +34, throttled_usec +15.602.928; PostgreSQL usage_usec +769.651 y throttling 0. Los intervalos fueron epoch ms 1794369992973–1794370014622 para backend y 1794369993255–1794370014885 para base. Son deltas reales con referencia anterior, pero no exclusivos del tramo HTTP ni de BCrypt; no se convierten en fracción de latencia ni prueban por sí solos su causa.

Los logs conservados muestran doce avisos del controlador de 494–1.761 ms con umbral de log 300 ms. No se pueden asociar por ordinal ni equiparar a elapsed HTTP. Las instantáneas de Hikari anteriores y posteriores tenían total 30, active 0, waiting 0; no hay una serie continua de espera durante toda la carga. No se encontraron errores de backend, agotamiento registrado del pool o errores de PostgreSQL en la ventana consultada; PostgreSQL no produjo líneas. No se atribuyen automáticamente las demoras a CPU, SQL o BCrypt.

### Evidencias y estado final

Carpeta nueva: `../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/`.

- [JTL](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/e3.jtl), [log](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/jmeter.log), [dashboard](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/reporte/index.html), [análisis](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/analisis.json).
- [Reconciliación individual](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/reconciliacion.csv), [antes](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/antes.json), [después](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/despues.json), [veredicto](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/veredicto.json).
- [Validación del CSV cerrado](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/validacion-recursos.json), [recursos](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/recursos.csv), [cpu.stat antes](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/cpu-stat-antes.json), [cpu.stat después](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/cpu-stat-despues.json), [logs correlacionados](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/logs-correlacion.json).

El JTL contiene exclusivamente cuarenta POST medidos, sin setup/teardown. Dashboard generado; cero variables temporales restantes y CSV válido después del cierre. El ejecutor revisó 146 archivos y no detectó exposiciones. La comprobación final volvió a encontrar cero docker stats activos. Perf permanece iniciado y saludable, sin reinicios ni OOM observados. No se repitió carga ni se avanzó a E4.

El backend estaba previamente utilizado y no se reinició. Las diferencias entre corridas pueden combinar generador, calentamiento y otras condiciones no aisladas: no demuestran comportamiento de arranque frío ni capacidad sostenida. Se mantienen las limitaciones de pocas solicitudes, misma máquina, BCrypt incluido, acceso directo sin nginx y PostgreSQL en tmpfs con fsync/synchronous_commit desactivados. La persistencia comprobada es existencia de filas tras las peticiones, no durabilidad ante caída.
