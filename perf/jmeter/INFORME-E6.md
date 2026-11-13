# E6 — Intentos de fase 100, corrección del setUp y ejecución parcial

**Estado más reciente:** se creó una variante sostenida independiente, pero su validación previa se detuvo por proceso activo, sin iniciar JMeter ni enviar HTTP. La fase sostenida de 100 no se ejecutó. La última sección documenta ese bloqueo; las estadísticas reales anteriores siguen correspondiendo exclusivamente a la corrida parcial de 500 GET.

Este informe conserva los intentos anteriores y añade al final la ejecución parcial posterior: se midieron 500 de 5.000 GET previstos. **Todavía no hay una fase E6 válida de 100 usuarios conforme al diseño.** Los apartados iniciales describen los intentos históricos y no se reinterpretan como carga medida.

Actualización más reciente: la causa del ciclo único quedó confirmada, se corrigió una propiedad del controlador interior y se validó con samplers ficticios. No se ejecutó HTTP al backend ni otra corrida real tras esa corrección. Véase la sección final de este informe.

El diagnóstico posterior, sin carga ni HTTP, está en [DIAGNOSTICO-PREPARACION-E6.md](DIAGNOSTICO-PREPARACION-E6.md): se conservó el ErrorRecord y se observó un bloqueo actual por Docker stats. Ese hallazgo no se atribuye retroactivamente al segundo intento.

## Resultado y alcance

Se ejecutó el comando solicitado **una sola vez**. La preparación HTTP externa terminó correctamente, pero el setUp local de JMeter abortó por un defecto del generador antes de iniciar el grupo medido. **Hubo cero GET medidos. No existe una medición válida de rendimiento E6 de 100 usuarios.** No se repitió la carga ni se ejecutaron 200 o 300.

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e6.ps1 -Fase 100 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Carpeta de evidencias: `../resultados/e6-20261111-102351-fd51ff1966ee4b2e9b4b210902bc9d3b/fase-100/`.

Configuración conservada: 100 usuarios, rampa 20 s, diez ciclos de cinco GET por hilo, mezcla 40/40/20, conexión 5.000 ms y respuesta 10.000 ms. Umbrales: notificaciones e historial 800 ms; especialidades 300 ms. No se modificó backend, BCrypt, límites, timeouts ni umbrales.

## Aislamiento y preparación

Se confirmó el proyecto `appointech-perf`, los servicios saludables `backend-perf` y `db-perf`, red exclusiva, publicación de `localhost:18080` por backend-perf y JDBC hacia `db-perf:5432/appointech_perf`, con PostgreSQL en tmpfs. No se usó nginx de desarrollo ni se reiniciaron servicios.

Límites sin cambios: backend 2 CPUs y 1 GiB; PostgreSQL 1 GiB sin cuota CPU. No había JMeter ni docker stats concurrentes. La lectura SQL confirmó las 100 identidades CLIENTE con perfil, citas y notificaciones.

### Paginación revisada

Diez cuentas de la cohorte tienen **23 notificaciones**. Sus expectativas contienen **solo 20 elementos** para `page=0&size=20`, ordenados por `creadaEn DESC,id DESC`, y un total separado igual a 23. El SQL usa `LIMIT 20` para la página y una consulta `count(*)` independiente para el total. La aserción Groovy compara el tamaño contra la página esperada y `X-Total-Count` contra el total completo. No se encontró confusión entre tamaño de página y total; no se cambió esta configuración.

Una validación local con JSON ficticio de veinte IDs y total 23 pasó antes de lanzar. La preparación HTTP de las 100 cuentas también acreditó sus IDs ordenados y totales, incluidas las diez cuentas con 23 registros. Esa comprobación no sustituye las aserciones completas que debían ejecutarse durante la carga.

### Solicitudes externas al JTL

La preparación duró **25.314 ms** y completó:

| Operación | Solicitudes |
|---|---:|
| Logins nuevos | 100 POST |
| Comprobaciones de pertenencia: notificaciones e historial | 200 GET |
| Controles normales previos | 3 GET |
| Catálogo público | 1 GET |
| Total de preparación | 304 |

Los tres controles normales fueron HTTP 200 y contenido esperado en **10, 13 y 8 ms**, por debajo de 800 ms. Los registros de preparación terminaron con resultado `ok`. Los tokens se obtuvieron fuera de JMeter y se acreditaron mediante rutas CLIENTE e IDs propios contrastados con SQL. No se incluyeron logins ni GET de preparación en el JTL. Estas solicitudes pudieron calentar el backend.

## Fallo del generador

El log registra el arranque del único hilo de setUp a **2026-11-11 10:24:22,758 UTC−05**. A las 10:24:23,870 el preprocesador invocado para precalentar scripts lanzó `Falta identidad preparada E6`; a las 10:24:23,898 el setUp terminó con `Preparacion local E6 fallida`. El grupo HTTP medido no arrancó.

La condición relevante de `e6-lecturas.groovy` es:

```groovy
if (!root || (!warm && props.get('e6Listo') != true)
          || thread >= root.path('identidades').size()) {
    // detener
}
```

**Causa reproducida localmente:** `root` es un objeto Jackson `JsonNode`. Su coerción booleana en este contexto hace que `!root` sea verdadero aunque no sea nulo y contenga identidades. La reproducción con JSON ficticio, contexto de preparación verdadero e índice válido devolvió `guardiaActualRechaza=true`; sustituyendo únicamente la prueba por `root == null`, devolvió `guardiaNullRechaza=false`. No se usaron tokens, HTTP ni un motor JMeter en esta reproducción. La evidencia está en `diagnostico-local.json`; el programa local se conserva en `target/e6-diagnostico-local.groovy`.

Es un fallo de preparación local del generador, **no un fallo demostrado de consultas REST del backend**. La compilación estática previa había pasado, pero no detecta esta semántica en ejecución.

JMeter devolvió **0** a pesar del aborto. El ejecutor terminó con **1** al validar los artefactos. El JTL solo contiene el encabezado. La generación del dashboard lanzó una `NullPointerException` por ausencia de muestras; es un fallo secundario. No hay dashboard HTML válido.

El `diagnostico.json` original se conserva: `cargaLanzada=true` significa que se inició el proceso Java, no que se enviaran GET. Su `fallo=false` se escribió antes de que fallara el análisis final. `parada.json`, el log, el JTL vacío y `verificacion-final.json` documentan el resultado real del intento.

**Corrección propuesta, no aplicada ni ejecutada en esta tarea:** usar comprobación explícita de nulidad para `root`, revisar otras coerciones de nodos Jackson y validar localmente el setUp con un paquete ficticio, antes de autorizar otro intento. No se cambió el plan después del fallo para repetirlo automáticamente.

## Resultados medidos y criterios

| Endpoint | Previstas | Medidas | Ventana, throughput y latencias |
|---|---:|---:|---|
| Notificaciones | 2.000 | 0 | No disponibles |
| Historial | 2.000 | 0 | No disponibles |
| Especialidades | 1.000 | 0 | No disponibles |
| Conjunto | 5.000 | 0 | No disponibles |

No se pueden calcular media, mediana, p90, p95, p99, máximo, ventana HTTP ni throughput con un JTL sin muestras. Tampoco corresponde informar 0 % de errores o afirmar cumplimiento de los umbrales. No hay respuestas medidas para clasificar HTTP, transporte, contenido o duración. El único fallo observable del intento es de preparación/ejecutor y dashboard secundario, fuera del tráfico medido.

La mezcla 40/40/20 está configurada, pero **no quedó ejercitada ni demostrada por muestras**. La pertenencia y orden se comprobaron durante la preparación externa; las aserciones completas del grupo medido no llegaron a ejecutarse. El JTL no contiene solicitudes ajenas porque no contiene ninguna muestra. No hay máximos observados de hilos del grupo medido ni de solapamiento HTTP que reportar; el único hilo que arrancó fue el de setUp. No se equipara el número 100 configurado con concurrencia efectiva.

| Criterio | Resultado |
|---|---|
| Aislamiento, salud, límites y datos previos | Confirmados |
| Paginación 20 elementos / total 23 | Verificada en SQL, comprobación local y preparación HTTP |
| Tokens nuevos y preparación fuera del JTL | Confirmados |
| 5.000 GET, mezcla y contenido durante carga | No ejecutados |
| Duración por muestra y menos del 1 % fallido | No evaluables |
| Ejecutor y dashboard válidos | No cumplido |
| Integridad antes/después | Sin cambios detectados |
| Recuperación posterior | Tres controles satisfactorios |
| Secretos y limpieza | Sin exposiciones detectadas; paquete retirado |

## Integridad

Cantidades y huellas agregadas coinciden antes/después en las ocho tablas, incluido el estado `leida` de notificaciones:

| Tabla | Antes | Después | Huella |
|---|---:|---:|---|
| usuario | 5.131 | 5.131 | Igual |
| cliente | 5.080 | 5.080 | Igual |
| tecnico | 50 | 50 | Igual |
| especialidad | 6 | 6 | Igual |
| cita | 490 | 490 | Igual |
| cita_historial_estado | 150 | 150 | Igual |
| pago | 490 | 490 | Igual |
| notificacion | 3.300 | 3.300 | Igual |

Las huellas incluyen las filas completas ordenadas por ID; son una detección agregada de cambios, no una auditoría causal. Aquí cubren preparación y controles; no demuestran comportamiento inmutable bajo carga porque no hubo carga medida.

## Recursos y estado final

El recolector produjo un CSV válido, cerró y terminó. Hubo **cuatro capturas por servicio**, ocho filas totales. Los tres intervalos reales fueron **2.028–2.064 ms**, media **2.046,333 ms**.

| Servicio | Capturas | Máximo CPU muestreado | Máxima memoria muestreada |
|---|---:|---:|---:|
| backend-perf | 4 | 0,18 % | 604,1 MiB |
| db-perf | 4 | 2,48 % | 150,2 MiB |

Estas capturas corresponden al intento local de arranque, **no a una ventana HTTP de rendimiento**. Son demasiado escasas para conclusiones de capacidad. No son picos absolutos y memoria de contenedor no equivale a heap Java. No se atribuye el fallo a CPU, memoria o PostgreSQL.

Los tres controles posteriores, fuera del JTL, fueron HTTP 200 con contenido esperado en **11, 26 y 7 ms**. Ambos contenedores siguen activos y saludables, con reinicios 0, OOM false, mismas identidades y límites. La comprobación final no detectó JMeter ni docker stats activos. Se confirmó que el archivo temporal con tokens y su directorio privado se eliminaron. El escaneo de artefactos no detectó contraseñas CSV, JWT o Authorization Bearer.

## Evidencias y siguiente paso

En la carpeta del intento:

- [JTL solo con encabezado](../resultados/e6-20261111-102351-fd51ff1966ee4b2e9b4b210902bc9d3b/fase-100/e6.jtl).
- [Log de JMeter](../resultados/e6-20261111-102351-fd51ff1966ee4b2e9b4b210902bc9d3b/fase-100/jmeter.log), `diagnostico.json`, `parada.json`, `diagnostico-local.json`.
- [Preparación](../resultados/e6-20261111-102351-fd51ff1966ee4b2e9b4b210902bc9d3b/fase-100/preparacion.json), `preparacion.csv`, `control-inicial-autenticacion.csv`, `control-normal-previo.csv`, `cohorte.csv` y `preflight.json`.
- [Integridad](../resultados/e6-20261111-102351-fd51ff1966ee4b2e9b4b210902bc9d3b/fase-100/integridad.json), `integridad-antes.json` y `integridad-despues.json`.
- [Recursos](../resultados/e6-20261111-102351-fd51ff1966ee4b2e9b4b210902bc9d3b/fase-100/recursos.csv), `limites.json`, `validacion-recursos.json`.
- [Recuperación](../resultados/e6-20261111-102351-fd51ff1966ee4b2e9b4b210902bc9d3b/fase-100/recuperacion.csv), `estado-antes.json`, `estado-despues.json`, `privacidad.json`, `verificacion-final.json`.

**Progresión detenida:** corregir y revisar el generador antes de otro intento de 100. Las fases 200 y 300 quedan pendientes y no deben ejecutarse con este defecto. Se conserva el intento completo; no se contabiliza como fallo de rendimiento del backend.

Perf queda iniciado y saludable según la comprobación final. E1–E5, el plan E6 ejecutado y sus umbrales se conservan. No hubo commit, push, cambio de rama ni modificación de negocio, DTO, migraciones, frontend o recursos. No se ejecutó Maven: el alcance fue esta única invocación de rendimiento y el diagnóstico local posterior.

## Corrección del setUp y segunda invocación autorizada

### Cambio exacto

Se sustituyó la condición `!root` de `e6-lecturas.groovy` por comprobaciones explícitas: instancia de `JsonNode`, no nulo/missing, objeto raíz, campo `identidades` de tipo arreglo no vacío, índice válido e identidad de tipo objeto. El setUp valida una sola vez el paquete completo: cantidad de identidades, ordinales, IDs de usuario/cliente positivos y únicos, correo sintético, rol CLIENTE, token textual no vacío, arrays poblados de citas/notificaciones y campos esperados de DTO. Se mantiene separada la página de notificaciones de su total: `size == min(20,total)`. También comprueba catálogo y enums presentes como arrays no vacíos.

Se revisaron las demás condiciones E6: la prueba del arreglo de respuesta ahora usa `actual == null || !actual.isArray()` explícitamente. Las restantes negaciones son de resultados booleanos (`isArray`, `isObject`, `isTextual`, `any`, etc.) o cadenas, no una coerción de `JsonNode`. No se flexibilizaron pertenencia, orden, paginación, contenido ni umbrales.

El setUp escribe un marcador no sensible `setup.json` con estado `listo` solo al terminar todas sus comprobaciones e inicialización; ante error escribe `fallido` y retira la bandera de disponibilidad. El ejecutor exige marcador válido para N y al menos una muestra JTL, independientemente del código de salida Java. Si falta cualquiera, registra explícitamente etapa `preparacion_local`. La privacidad se verifica aun cuando la ejecución haya fallado, antes de invocar el analizador.

Se mantienen 100 usuarios, 20 s de rampa, diez ciclos de cinco GET, mezcla 40/40/20, umbrales 800/800/300 ms y timeouts 5.000/10.000 ms. No cambió JMX, SQL de expectativas, backend, BCrypt ni recursos.

### Validación local sin HTTP

El nuevo auxiliar `validar-setup-e6.groovy` invoca el **mismo archivo de setUp mediante JSR223Sampler.sample**, con contexto local, un motor simulado que solo registra solicitudes de parada y JSON ficticio. No arranca un motor de carga ni ejecuta HTTP. El caso válido incluye total de 23 notificaciones y página de veinte, además de una cita y catálogo ficticios completos. Se ejercitan también los pre/postprocesadores usados por el precalentamiento.

| Caso | Resultado observado |
|---|---|
| Raíz válida con identidad completa | Aceptado |
| Raíz ausente: archivo no existente | Rechazado |
| Raíz JSON null | Rechazado |
| Raíz de tipo incorrecto | Rechazado |
| Arreglo de identidades vacío | Rechazado |
| Identidad incompleta: falta token | Rechazado |

Solo pasó el caso válido; los errores de los casos negativos son esperados. Cero solicitudes HTTP. Sintaxis PowerShell 5.1, XML, propiedades y compilación Groovy pasaron antes de la invocación real. La evidencia local está en `target/e6-setup-validacion/validacion-setup.json` y copiada en la carpeta del segundo intento como `validacion-setup-local.json`.

### Segunda invocación: detenida antes de Java

Se ejecutó **una sola vez** el mismo comando autorizado, generando:

`../resultados/e6-20261111-103815-12ea678c8fa4455f985e3a9828a6e741/fase-100/`.

La verificación previa acreditó aislamiento `appointech-perf`, salud de ambos servicios, los mismos límites y 100 clientes elegibles, sin procesos de carga. Sin embargo, la invocación se detuvo durante la preparación externa a los **444 ms**, antes de escribir `estado-antes.json`, obtener tokens o iniciar Java/JMeter. El código del ejecutor fue **1** y `codigoJMeter=null`. No se creó JTL ni dashboard, no arrancó el recolector y no se enviaron solicitudes HTTP de preparación ni GET medidos en esta nueva invocación. No se volvió a ejecutar el comando.

El diagnóstico original conserva tipo `System.Management.Automation.RuntimeException`, pero el manejador existente descartó el mensaje y la línea. **No hay evidencia suficiente para identificar cuál de las comprobaciones anteriores a `estado-antes.json` falló** (dependencias/XML, procesos o aislamiento). Las comprobaciones posteriores de solo lectura encontraron dependencias/XML disponibles, aislamiento y salud correctos y cero procesos concurrentes; eso no reconstruye el estado exacto del fallo. No se atribuye el bloqueo a un proceso concurrente ni al backend sin evidencia.

Después de conservar el fallo se añadió diagnóstico seguro por etapa, línea y mensajes propios `E6:` al ejecutor, evitando imprimir excepciones de proveedor potencialmente sensibles. Esta mejora posterior **no se ejecutó contra perf**; su sintaxis se verificó localmente. No recupera retroactivamente el mensaje perdido del segundo intento.

### Resultados e integridad disponibles

| Métrica | Primer intento | Segunda invocación |
|---|---:|---:|
| GET previstos | 5.000 | 5.000 |
| GET medidos | 0 | 0 |
| Java/JMeter iniciado | Sí, solo setUp | No |
| Código JMeter | 0 | No ejecutado |
| Código ejecutor | 1 | 1 |
| HTTP de preparación | 100 POST + 204 GET | 0 |
| GET de recuperación | 3 | 0 |
| Dashboard válido | No | No |
| Latencias, throughput, errores HTTP medidos | No evaluables | No evaluables |

Las autenticaciones y GET del primer intento siguen excluidos de cualquier carga medida. En el segundo no hay muestras para comprobar mezcla, pertenencia, paginación bajo carga, percentiles, evolución temporal, duración o concurrencia. Tampoco hay recursos capturados de una nueva ventana HTTP. No se presentan ceros como tasas de éxito ni se inventan estadísticas.

No se capturaron snapshots de integridad antes/después de la segunda invocación: se detuvo antes de esa etapa. Sí se conserva la igualdad de las ocho tablas del primer intento. En la segunda no se ejecutó ningún HTTP ni operación de escritura; no se sustituye la ausencia de snapshots por una medición ficticia. La validación local usó únicamente archivos ficticios.

El primer intento se conservó íntegro y se verificó por SHA-256 contra el manifiesto creado antes de corregir. Los servicios permanecen activos y saludables, reinicios 0, OOM false y sin procesos JMeter/docker stats en la comprobación posterior de solo lectura. No fue necesario solicitar nuevos controles HTTP de recuperación porque la segunda invocación no llegó a enviar tráfico. No se generó un paquete con tokens ni un recolector en esta invocación; su escaneo de artefactos no detectó secretos.

### Evidencias de la segunda invocación y estado de progresión

- [Diagnóstico original](../resultados/e6-20261111-103815-12ea678c8fa4455f985e3a9828a6e741/fase-100/diagnostico.json), [preparación incompleta](../resultados/e6-20261111-103815-12ea678c8fa4455f985e3a9828a6e741/fase-100/preparacion.json) y `parada.json`.
- [Validación local del setUp](../resultados/e6-20261111-103815-12ea678c8fa4455f985e3a9828a6e741/fase-100/validacion-setup-local.json) y `preflight.json`.
- [Estado final](../resultados/e6-20261111-103815-12ea678c8fa4455f985e3a9828a6e741/fase-100/estado-final.json), `diagnostico-posterior.json` y `privacidad.json`.
- Código completo corregido: `e6-lecturas.groovy`, `ejecutar-e6.ps1`; validación reproducible sin HTTP: `validar-setup-e6.groovy`. El ZIP original E6 corresponde a la versión inicial, no a esta corrección.

**Progresión detenida; 100 sigue sin medición válida y 200/300 quedan pendientes.** El defecto de coerción Jackson quedó corregido y validado localmente; el segundo bloqueo no tiene causa exacta conservada. Otro intento requiere autorización explícita, no una repetición automática. Perf queda iniciado y saludable según la comprobación posterior. No se cambiaron negocio, recursos, BCrypt, umbrales, DTO, migraciones o frontend, ni se hicieron commit, push o cambios de rama.

## Resolución del PID y única corrida posterior — 500 GET parciales

### Identificación del proceso y resolución

A **2026-11-11T16:17:05.4106553Z**, el PID **40980 ya no existía**. No se pudieron recuperar ejecutable, hora de inicio, padre, comando, contenedor observado, archivo de salida ni pertenencia. No se identifica como recolector propio, de otra prueba o actividad del usuario; no se atribuye al intento histórico cuyo error se perdió.

La consulta simultánea no encontró JMeter ni recolectores activos. Por tanto, no había un bloqueo actual que resolver terminando procesos. **No se mató ningún proceso ni se cambió el detector.** La evidencia está en [identificacion.json](../diagnosticos/e6-proceso-40980-20261111-111704/identificacion.json).

El comando `-DiagnosticoSinHttp` pasó con código 0 en `../diagnosticos/e6-local-20261111-111738-a000f30e8a4a4fad927c3391874fc80e/fase-100/`: aislamiento y salud confirmados, 100 identidades, ocho tablas leídas, integridad sin cambios y Java `-version` con código 0. Cero HTTP y sin iniciar recolector/JMeter. No se deduce de ello el origen del PID desaparecido.

### Única invocación real y etapa exacta de detención

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e6.ps1 -Fase 100 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Se ejecutó una sola vez, sin cambiar recursos, mezcla, umbrales, rampa o ciclos configurados. Evidencias:

`../resultados/e6-20261111-111809-30e6b81ce7d04e96972e9095c17e87db/fase-100/`.

La preparación terminó y `setup.json` acredita `estado=listo`, 100 identidades y cero HTTP local del setUp. JMeter terminó con código 0 y produjo dashboard. **Solo produjo 500 de los 5.000 GET previstos**. El ejecutor terminó con **código 1** en `validacion_final`, con ErrorRecord conservado: `E6: muestras incompletas; conservar JTL.`, `analizar-e6.ps1:8`. No se repitió la carga ni se ejecutaron 200/300.

Cada uno de los 100 hilos emitió cinco muestras, con pasos 1–5 y únicamente `e6Iteracion=1`. El log confirma propiedad `loops=10` y la configuración efectiva conserva diez ciclos con bucle interior de cinco GET. **Los diez ciclos no quedaron ejercitados:** hay un defecto de ejecución del diseño de bucles que debe revisarse antes de otro intento. El recuento observado demuestra el problema, pero no se afirma una causa interna exacta de JMeter sin validar el ciclo de los controladores. No se cambió el plan después de observarlo ni se debilitó la validación para aceptar 500 como 5.000.

El `diagnostico.json` original de esta corrida muestra setUp válido y 500 muestras, con `fallo=false` porque se escribió antes del análisis final. El fallo posterior está en `errores-seguros.jsonl` y `parada.json`; no se interpreta aquella bandera como fase aprobada.

### Resultados reales de la ejecución parcial

El análisis independiente `analisis-parcial.json` no altera el JTL ni el rechazo del analizador principal. Incluye todas las muestras y la rampa. Ventana desde mínimo `timeStamp` (inicio) hasta máximo `timeStamp+elapsed`; percentiles por rango más cercano `ceil(p×N)`, mediana convencional. Throughput por endpoint calculado sobre su propia ventana, no sumable directamente al global.

| Endpoint | Previstas / medidas | Ventana (s) | Throughput (/s) | Media (ms) | Mediana | p90 | p95 | p99 | Máximo |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Notificaciones | 2.000 / 200 | 19,766 | 10,118 | 7,340 | 7 | 11 | 14 | 19 | 69 |
| Historial | 2.000 / 200 | 19,682 | 10,162 | 8,545 | 7 | 14 | 16 | 30 | 71 |
| Especialidades | 1.000 / 100 | 19,657 | 5,087 | 4,060 | 4 | 6 | 6 | 9 | 19 |
| Conjunto | 5.000 / 500 | 19,771 | 25,290 | 7,166 | 6 | 12 | 15 | 23 | 71 |

Ventana global: **2026-11-11T16:18:46.278Z–16:19:06.049Z**. Throughput global = 500/19,771 s. No se utiliza la duración de consola, que incluye trabajo local antes de la primera petición.

| Endpoint | HTTP 200 | Otros HTTP | Transporte | Contenido | Sobre umbral | Fallidas únicas |
|---|---:|---:|---:|---:|---:|---:|
| Notificaciones | 200 | 0 | 0 | 0 | 0 | 0 |
| Historial | 200 | 0 | 0 | 0 | 0 | 0 |
| Especialidades | 100 | 0 | 0 | 0 | 0 | 0 |
| Total | 500 | 0 | 0 | 0 | 0 | 0 (0 %) |

La mezcla observada sí fue **40/40/20**, pero solo para un ciclo por hilo. Las 500 muestras indican `e6ContenidoValido=true`, con pertenencia, IDs, orden, tipos, paginación y campos contrastados en memoria con SQL. Solo aparecen los GET medidos, sin login ni preparación. Las diez cuentas con total de 23 notificaciones conservaron página esperada de veinte y encabezado total 23. No se guardaron cuerpos para revalidación posterior: la evidencia es la aserción ejecutada y sus diagnósticos no sensibles.

### Evolución y concurrencia observada

| Intervalo de inicio relativo | Muestras | Fallidas | Media (ms) | p95 (ms) |
|---|---:|---:|---:|---:|
| [0, 10) s | 255 | 0 | 7,937 | 16 |
| [10, 19,771] s | 245 | 0 | 6,363 | 14 |

Los primeros POST no aplican en E6: los primeros **GET** de los 100 hilos abarcaron **19,747 s**. Distribución por segundo: seis en el primero, cinco en cada segundo 1–18 y cuatro en el último. Se observaron **máximo un hilo activo y una muestra HTTP en curso según JMeter**. No se demuestra sobrecarga ni concurrencia elevada y no se equiparan los 100 usuarios configurados a solicitudes simultáneas. El solapamiento de intervalos cliente no acredita ejecución simultánea en el servidor.

### Preparación, integridad, recursos y recuperación

Preparación externa nueva: **27.844 ms**, 100 logins, 200 GET de pertenencia, tres controles normales en 11/24/10 ms y un GET de catálogo: **304 solicitudes fuera del JTL**. La preparación pudo calentar las cachés. Las 100 autenticaciones y 204 GET del primer intento siguen siendo otra preparación histórica y no se suman a esta carga.

Las cantidades y huellas completas permanecieron iguales en las ocho tablas:

| Tabla | Antes / después |
|---|---:|
| usuario | 5.131 / 5.131 |
| cliente | 5.080 / 5.080 |
| tecnico | 50 / 50 |
| especialidad | 6 / 6 |
| cita | 490 / 490 |
| cita_historial_estado | 150 / 150 |
| pago | 490 / 490 |
| notificacion | 3.300 / 3.300 |

Esto incluye el estado `leida`; la igualdad es evidencia agregada, no una auditoría causal.

| Servicio | Capturas HTTP / totales | Máximo CPU muestreado durante HTTP | Máxima memoria durante HTTP |
|---|---:|---:|---:|
| backend-perf | 9 / 15 | 37,22 % | 606,6 MiB |
| db-perf | 9 / 15 | 7,18 % | 150,4 MiB |

Cadencia: catorce intervalos reales de **1.960–2.073 ms**, media **2.012,286 ms**. El recolector cerró y su CSV de treinta filas se validó. Son máximos muestreados, no picos absolutos; memoria de contenedor, no heap Java. No se atribuye el defecto de ciclos a CPU, memoria o PostgreSQL.

Los tres controles posteriores, fuera del JTL, devolvieron HTTP 200 y contenido esperado en **14, 24 y 195 ms**. Ambos contenedores siguen activos y saludables, sin reinicio ni OOM y con los límites originales: backend 2 CPUs/1 GiB, base 1 GiB sin cuota CPU. El paquete privado y su directorio se eliminaron. El escaneo no detectó credenciales, JWT o Authorization Bearer.

Una observación posterior al cierre contó un proceso clasificado por el detector; no conservó identidad. Al intentar identificarlo en una lectura posterior, ya no se encontraron procesos clasificados. Se conservan ambas observaciones en `verificacion-final.json` y `procesos-observacion-final.json`; no se afirma su autoría, interferencia durante la corrida o relación con el PID 40980. No se terminó ningún proceso. La última observación no detectó JMeter/recolectores.

### Evaluación y evidencias

Las muestras recibidas cumplen HTTP, contenido y duración, pero **la fase falla por recuento/ciclos incompletos** y no cubre la sobrecarga prevista. Se conserva el dashboard de 500 muestras como evidencia parcial, no como aprobación. Antes de otra ejecución debe validarse sin HTTP que cada hilo recorra diez ciclos y cincuenta GET. Las fases 200/300 no se ejecutaron; no se avanzó a E6 adicional ni a otro escenario.

- [JTL parcial](../resultados/e6-20261111-111809-30e6b81ce7d04e96972e9095c17e87db/fase-100/e6.jtl), [log](../resultados/e6-20261111-111809-30e6b81ce7d04e96972e9095c17e87db/fase-100/jmeter.log), [dashboard parcial](../resultados/e6-20261111-111809-30e6b81ce7d04e96972e9095c17e87db/fase-100/reporte/index.html).
- [Análisis parcial](../resultados/e6-20261111-111809-30e6b81ce7d04e96972e9095c17e87db/fase-100/analisis-parcial.json), `primeros-inicios-parcial.csv`. Programa local: `target/analizar-e6-parcial.ps1`, sin HTTP ni JMeter.
- [Recursos](../resultados/e6-20261111-111809-30e6b81ce7d04e96972e9095c17e87db/fase-100/recursos.csv), `validacion-recursos.json`, `limites.json`, `integridad-antes.json`, `integridad-despues.json`, `integridad.json`.
- [Recuperación](../resultados/e6-20261111-111809-30e6b81ce7d04e96972e9095c17e87db/fase-100/recuperacion.csv), `preparacion.json`, `cohorte.csv`, `setup.json`, `diagnostico.json`, `errores-seguros.jsonl`, `parada.json`, `estado-final.json`, `verificacion-final.json`.
- `identificacion-proceso-40980.json` y `diagnostico-previo-sin-http.json` en la misma carpeta.

Los dos intentos anteriores se conservaron íntegros y se verificaron por hashes. No se modificaron plan, ciclos configurados, aserciones, umbrales, código de negocio, BCrypt, recursos ni servicios durante esta tarea. Solo se añadieron análisis y documentación. Perf queda iniciado y saludable según el estado final acreditado. No se declara capacidad sostenida, arranque frío ni número de usuarios soportados.

## Corrección posterior del bucle — validación exclusivamente ficticia

Esta sección corresponde a una tarea posterior. **No modifica ni reemplaza los resultados reales de 500 GET ni ejecuta nuevas peticiones a AppoinTech.** Tampoco se consultó db-perf ni se capturaron recursos de servicios en esta tarea.

### Causa exacta confirmada

El plan tenía dos controladores: principal con `${__P(loops,10)}` y `continue_forever=false`, e interior con cinco repeticiones y **`continue_forever=false`**. El log real confirma `usuarios=100`, `rampa=20`, `loops=10`. No hay errores ni una parada del motor antes de finalizar normalmente los 100 hilos; el fallo del ejecutor vino después por recuento incompleto.

Para aislar la causa se clonó el plan y se conservó la misma estructura de controladores, sustituyendo únicamente el sampler HTTP por uno JSR223 local ficticio y retirando la preparación/validación REST. La copia no tiene HTTPSampler ni credenciales. Con dos hilos, pedir tres o diez ciclos seguía produciendo diez muestras: cinco por hilo. La prueba registró además que el controlador principal cargaba **3 y 10**, respectivamente, y que la propiedad JMeter tenía los mismos valores. Por tanto, `-Jloops` no se perdió.

Se inspeccionó el bytecode de las clases instaladas de **JMeter 5.6.3**:

- `LoopController.next()` y `nextIsNull()`: cuando termina el bucle y `continue_forever=false`, ejecutan `setDone(true)`.
- `GenericController.currentReturnedNull()`: si el controlador hijo está `done`, llama a `removeCurrentElement()`.
- Con `continue_forever=true`, el bucle interior restablece su contador al terminar y devuelve el control al padre, sin retirarse de sus hijos.

Así, después de las primeras cinco peticiones el controlador interior desaparecía de la lista del padre. Los ciclos exteriores siguientes no encontraban ese sampler. Esto explica el único ciclo efectivo sin atribuirlo a backend, tokens, duración, un parámetro perdido o una orden de parada de los scripts.

Evidencia primaria local: [bytecode de controladores](../diagnosticos/e6-bucles-corregido-20261111-150837-0af222db68ef4d2eafdf7d77a2142b58/controladores-jmeter-5.6.3-bytecode.txt), métodos `LoopController.next` (línea 126), `nextIsNull` (212) y `GenericController.currentReturnedNull` (588). La inspección no inició un motor ni HTTP.

### Corrección mínima

En `e6-lecturas.jmx`, únicamente para el `LoopController` interior:

```diff
- <boolProp name="LoopController.continue_forever">false</boolProp>
+ <boolProp name="LoopController.continue_forever">true</boolProp>
```

Se mantiene `loops=5` interior. El principal sigue con diez ciclos y `continue_forever=false`; no se convierte el plan en infinito. Una comparación XML normalizada confirma que esa es la única diferencia del plan. Rutas, preparación separada, aserciones, umbrales 800/800/300 ms, rampa de 20 s, timeouts y recursos permanecen iguales. El validador estático ahora exige ambos flags correctos para prevenir la regresión.

### Salida de validación sin HTTP

Nuevo `validar-bucles-e6.ps1`: genera copias temporales bajo `perf/diagnosticos/e6-bucles-*`, verifica que los controladores no se alteran durante la conversión, que no quedan HTTPSamplers y que solo se ejecutan contadores ficticios. La rampa de las pruebas locales se sobrescribe a cero para agilizar el recuento; la propiedad y defecto de rampa del plan de backend siguen siendo 20 s. Las métricas de duración ficticias no se usan como rendimiento del backend.

| Plan | Hilos | Ciclos pedidos / cargados | Esperadas | Muestras | Notificaciones | Historial | Especialidades | Por hilo |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| Original | 2 | 3 / 3 | 30 | 10 | 4 | 4 | 2 | 5 |
| Original | 2 | 10 / 10 | 100 | 10 | 4 | 4 | 2 | 5 |
| Corregido | 2 | 3 / 3 | 30 | 30 | 12 | 12 | 6 | 15 |
| Corregido | 2 | 10 / 10 | 100 | 100 | 40 | 40 | 20 | 50 |

Ambos casos corregidos terminaron con código 0, sin muestras fallidas y con pares ciclo/paso únicos y cantidades correctas por hilo. Los casos originales documentan el defecto y no se presentan como pruebas aprobadas. **HTTP enviados al backend: 0.**

Comando de la validación corregida:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\validar-bucles-e6.ps1 -JMeterHome "$env:LOCALAPPDATA\Programs\JMeter" -Etiqueta corregido
```

- [Reproducción original instrumentada](../diagnosticos/e6-bucles-original-20261111-150539-1c50a166088a419cbb5f776d38b3c420/validacion-bucles.json).
- [Validación corregida](../diagnosticos/e6-bucles-corregido-20261111-150837-0af222db68ef4d2eafdf7d77a2142b58/validacion-bucles.json).
- En cada carpeta: `plan-fuente-no-ejecutado.jmx`, `plan-ficticio.jmx`, `ficticio-3.jtl`, `ficticio-10.jtl` y logs exclusivamente locales. Los JTL reales no se tocaron.
- [Causa, comparación XML y modelo](../diagnosticos/e6-bucles-corregido-20261111-150837-0af222db68ef4d2eafdf7d77a2142b58/causa-y-modelo.json).

La revisión adicional del XML, sintaxis Windows PowerShell 5.1 y compilación del Groovy de backend pasó sin ejecutar ese Groovy contra servicios. No se reejecutó el ejecutor REST ni se inició recolector.

### ¿Diez ciclos sostienen la concurrencia?

La corrida parcial real observó medias 7,340 ms en notificaciones, 8,545 ms en historial y 4,060 ms en especialidades. Con la mezcla exacta:

`tiempo HTTP/ciclo = 2×7,340 + 2×8,545 + 4,060 = 35,830 ms`.

Diez ciclos por hilo darían **358,3 ms de HTTP**. El tramo medio real desde primer inicio hasta último final de cada hilo, incluidos huecos entre solicitudes, fue **48,11 ms por ciclo**; extrapolar diez ciclos da **481,1 ms**. La extrapolación no incluye efectos de mayor concurrencia, caché, generador o saturación y no es una medición nueva.

| Fase | Incorporación durante rampa | Activos medios aproximados con solo HTTP | Con huecos extrapolados |
|---|---:|---:|---:|
| 100 | 5 hilos/s | 1,79 | 2,41 |
| 200 | 10 hilos/s | 3,58 | 4,81 |
| 300 | 15 hilos/s | 5,37 | 7,22 |

Se usa `activos ≈ tasa de incorporación × duración del hilo`. Son medias de una aproximación sin saturación, no máximos medidos ni solicitudes simultáneas del servidor. Con ese perfil rápido, cada hilo acaba en menos de medio segundo mientras la rampa dura veinte. La corrección del contador por sí sola no mantendría 100–300 hilos activos ni un período sostenido de sobrecarga. No se concluye que la aplicación soporte una cifra universal ni que 7 hilos no puedan llegar a causar contención en otras condiciones.

### Propuesta concreta para un diseño sostenido — no aplicada

Mantener fases **100, 200 y 300**, **rampa de 20 s**, la misma mezcla, credenciales por hilo, aserciones, recursos y timeouts. Sustituir el límite de diez ciclos por ciclos completos continuos hasta un **plazo global común de 140 s desde el arranque del grupo**: veinte de rampa y ciento veinte de régimen sostenido. Evaluar el plazo al principio de cada ciclo exterior de cinco GET, sin cortar un ciclo empezado. Dejar que finalicen los últimos ciclos y registrar por separado esa cola posterior. No añadir pausas ni logins al JTL.

La muestra esperada ya no sería N×50: sería `M = 5×suma(ciclos completados por hilo)`, variable según latencias y coste del generador. La corrida completa de ciclos tendría exactamente `0,4M` notificaciones, `0,4M` historial y `0,2M` especialidades. No se estima un número garantizado de muestras extrapolando millones de peticiones rápidas de esta corrida parcial.

El analizador futuro tendría que sustituir el requisito de exactamente diez ciclos por validación de ciclos completos y secuencias, participación de todos los hilos y cobertura temporal. Conservaría el JTL íntegro y reportaría **rampa [0,20), sostenido [20,140) y cola posterior**, con cantidades reales y mezcla de cada ventana. Los cortes temporales pueden atravesar ciclos; no se forzaría una mezcla exacta por ventana manipulando muestras. Se evaluarían por separado y en conjunto throughput, percentiles, errores, duración y contenido con los mismos umbrales y menos del 1 % de fallidas. También se medirían hilos activos e intervalos HTTP solapados; no se declararían 100/200/300 solicitudes simultáneas solo por configurar esos usuarios.

**Esta propuesta no se implementó.** El JMX corregido conserva diez ciclos, cinco GET por ciclo y scheduler desactivado. Elegir el diseño sostenido requiere autorización antes de modificarlo o ejecutarlo. El resultado real vigente sigue siendo la corrida parcial de 500 GET; no se repitió ni se ejecutaron 200/300. Todos los resultados previos se conservan.

## Variante sostenida independiente — validación previa bloqueada

En una tarea posterior autorizada se crearon archivos separados para 100 hilos, rampa 20 s y 120 s adicionales sostenidos. **El plan corregido de diez ciclos y sus scripts no se sobrescribieron.** Las referencias y todos los resultados reales anteriores se verificaron por hashes.

Archivos: `e6-sostenido.jmx`, `e6-reloj-sostenido.groovy`, `ejecutar-e6-sostenido.ps1`, `analizar-e6-sostenido.js`, `validar-e6-sostenido.ps1` y [README-E6-SOSTENIDO.md](README-E6-SOSTENIDO.md).

El diseño nuevo conserva la identidad de cada hilo y los GET notificaciones/historial/especialidades con las mismas rutas, aserciones y umbrales. El ejecutor solo admite fase 100, prepara tokens fuera del JTL y conserva integridad, recursos y recuperación externa. La validación de muestras es por hilos y ciclos completos variables, sin exigir las antiguas 5.000.

### Temporización y análisis implementados, todavía sin medición

El primer hilo registra el inicio efectivo local antes del primer GET, no el arranque previo de Java. Se guardan los límites `t0`, `t0+20s` y `t0+140s`. El reloj monotónico controla admisión de ciclos y un marcador epoch permite correlacionar muestras y recursos. El While admite ciclos completos hasta el plazo; los ya admitidos terminan y se reportan como cierre cuando corresponda. Los samplers locales del reloj se ignoran en JTL. No hay un total fijo ni login en la medición.

La clasificación de muestras es siempre por **inicio**: rampa `[t0,t0+20)`, sostenido `[t0+20,t0+140)` y cierre posterior. Una petición cruzada conserva su cohorte de inicio para latencias y errores. Para distinguir respuestas recibidas, se calcula aparte el throughput de HTTP 200 con contenido válido por su instante de finalización dentro del intervalo, aunque comenzaran en otra cohorte. No se igualan esos dos recuentos.

El analizador implementa mezcla observada por endpoint, fallidas únicas frente a categorías solapadas, HTTP/timeout/transporte/contenido/duración, medias y percentiles de rango más cercano, evolución de hilos y barrido de intervalos HTTP incluyendo cruces de frontera. Los recursos se clasifican por timestamp de captura. El tiempo de timeout sigue siendo espera truncada del cliente y no tiempo final del servidor.

### Única validación previa y punto de detención

La validación propuesta usa una copia sin HTTPSamplers, dos hilos, un segundo de rampa y dos sostenidos, con los mismos While/Loop y reloj real de la variante. Debía probar repetición, parada por plazo, cierre de todos los hilos y ausencia de procesos. **No llegó a ejecutar esos samplers.**

Las comprobaciones ejecutadas antes del bloqueo fueron:

| Comprobación | Resultado |
|---|---|
| Parser de los dos scripts PowerShell nuevos | Sin errores |
| Sintaxis JavaScript (`node --check`) | Correcta |
| Clasificación de inicios justo antes/en los límites | Correcta con datos ficticios |
| Recepción por final de una petición que cruza el límite | Correcta con datos ficticios |
| Finales antes que inicios al calcular solapamiento | Correcta con datos ficticios |
| Ausencia de procesos previos | **Falló; detención** |
| Compilar/ejercitar reloj Groovy en JMeter ficticio | No alcanzado |
| Repetición sostenida y parada efectiva | No validado |
| Cierre y ausencia de hilos tras prueba ficticia | No validado; no se inició JMeter |

Comando invocado una sola vez:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\validar-e6-sostenido.ps1 -JMeterHome "$env:LOCALAPPDATA\Programs\JMeter"
```

El ErrorRecord de **2026-11-11T20:34:58.5560754Z** conserva tipo `System.Management.Automation.RuntimeException`, mensaje `E6: proceso de carga previo activo.`, archivo `validar-e6-sostenido.ps1`, línea 18. La etapa original quedó como `sintaxis` porque aún no se actualizaba antes de consultar procesos; se conserva sin reescribir y el validador utilizado se copió junto a la evidencia. Después se mejoró únicamente el registro de etapa/PIDs para futuras comprobaciones, sin reejecutar.

Una lectura posterior, a **20:37:39.4913246Z**, encontró `docker.exe`, PID **35180**, clasificado `docker_stats`. El PID exacto del instante del rechazo no quedó conservado; no se demuestra que fuera el mismo proceso ni se atribuye a pruebas propias o al usuario. No se mató ningún proceso ni se cambió el detector para ignorarlo.

Evidencia: `../diagnosticos/e6-sostenido-ficticio-20261111-153457-ab0a9736b1464c80b6f36b5c7b7606d0/`:

- [ErrorRecord seguro](../diagnosticos/e6-sostenido-ficticio-20261111-153457-ab0a9736b1464c80b6f36b5c7b7606d0/error-seguro.jsonl).
- [Observación posterior de procesos](../diagnosticos/e6-sostenido-ficticio-20261111-153457-ab0a9736b1464c80b6f36b5c7b7606d0/observacion-procesos-posterior.json).
- `validador-utilizado.ps1` conserva exactamente el código que generó aquel error.

### Resultado real disponible de esta tarea

**JMeter iniciado: no. Logins: 0. GET: 0. HTTP al backend: 0. Recolector propio iniciado: no.** No se ejecutó el comando real de la variante y no hubo repetición automática después del bloqueo.

| Métrica solicitada | Rampa nueva | Sostenido nuevo |
|---|---|---|
| Muestras y proporciones por endpoint | Sin medición | Sin medición |
| HTTP 200/otros/timeout/transporte/contenido/duración | No evaluables | No evaluables |
| Throughput total y de HTTP 200 válidos recibidos | No evaluable | No evaluable |
| Media, p90, p95, p99 | No disponibles | No disponibles |
| Hilos e intervalos HTTP solapados | No medidos | No medidos |
| CPU y memoria backend/PostgreSQL | Sin capturas | Sin capturas |

No se capturaron nuevas huellas antes/después ni se enviaron controles de recuperación, ya que no se alcanzó perf. No se presenta la ausencia de muestras como 0 % fallido ni como prueba de recuperación. La salud actual del entorno no se volvió a comprobar en esta tarea. No se inició, reinició, detuvo o modificó ningún servicio.

**La fase sostenida de 100 queda pendiente por validación previa incompleta.** Hay que resolver la concurrencia observada y superar la prueba ficticia antes de enviar HTTP; no se declara cubierta la sobrecarga. La última corrida real continúa siendo la parcial de 500 GET y no se mezcla con esta implementación. No se ejecutaron 200/300 ni E7 ni se cambiaron umbrales, backend, BCrypt o recursos para aprobar.
