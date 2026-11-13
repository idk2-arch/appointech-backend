# Informe final de pruebas de carga y estrés de AppoinTech

Fecha de cierre documental: 12 de noviembre de 2026. Evidencia histórica de las corridas del 10–12 de noviembre; horas de trazas en UTC salvo indicación contraria.

La evidencia muestra integridad funcional en las operaciones conciliadas, incumplimientos de duración en registro y flujo de negocio, degradación marcada del login bajo estrés y deterioro de las consultas al aumentar la carga. E7 completó sus ciclos CRUD sin inconsistencias detectadas y E8 mostró estabilidad durante diez minutos a ritmo limitado. **Esto no acredita capacidad de producción, ausencia de fugas ni cobertura completa del recorrido frontend.**

Este cierre se elaboró exclusivamente mediante lectura de archivos y cálculo offline. No se ejecutaron HTTP, JMeter, Docker, SQL ni nuevas pruebas; no se modificaron resultados originales. E9 reutiliza explícitamente las mismas cuatro corridas E5 y no suma nuevas ejecuciones.

## Fuente de requisitos y criterios de evaluación

La [transcripción preservada de la guía del profesor](../analisis/cierre-e8-e9-2026-11-12T03-55-50-054Z/requisito-profesor.txt) conserva las páginas 1–6; su [registro de fuente](../analisis/cierre-e8-e9-2026-11-12T03-55-50-054Z/requisito-profesor-fuente.json) identifica el PDF y su SHA-256. La página 1 pide JMeter, APIs REST y flujo frontend ↔ backend ↔ base de datos. Los valores de usuarios, rampas y loops se presentan como configuración sugerida. Los umbrales numéricos de respuesta que siguen son criterios del proyecto, **no exigencias textuales del profesor**.

| Término | Uso en este informe |
|---|---|
| Ejecutado | Hay muestras de una corrida real en el JTL. No significa aceptación ni código de salida satisfactorio. |
| Cumple | Satisface el criterio expresamente evaluado, dentro de esa configuración y ventana. |
| No cumple | Existe evidencia de incumplimiento del criterio; se mantiene aunque la respuesta HTTP o el código del proceso sean correctos. |
| Cobertura parcial | Falta parte del alcance, instrumentación o evidencia necesaria. No se convierte la falta de medición en cero errores. |

Una prueba de estrés que provoca lentitud o timeouts puede estar correctamente ejecutada y cumplir su objetivo de identificar degradación, aunque **no cumpla los criterios de rendimiento**. Los defectos del plan o del recolector se identifican por separado.

| Operación/escenario | Criterio de duración previo | Otros criterios |
|---|---|---|
| Login E1/E4/E5 | Cada muestra ≤2.000 ms | HTTP esperado, contenido/token válido; menos de 1% de muestras fallidas en la aceptación original. |
| Notificaciones E2; notificaciones/historial E6/E8 | Cada muestra ≤800 ms | HTTP 200 y contenido/pertenencia correctos. |
| Especialidades E4/E6/E8 | Cada muestra ≤300 ms | HTTP 200 y catálogo esperado. |
| Registro E3; creación E4 | Cada muestra ≤1.500 ms | HTTP 201, contenido, persistencia e integridad; E3 usa umbral provisional de escrituras. |
| Cancelación/detalle E4 | ≤1.500/800 ms, provisionales | Estado, propietario, ID e historial correctos. |
| E7 CRUD | Sin SLA de duración definido | Contratos por operación, ciclos completos, checkpoints SQL, cero incertidumbres/residuos e integridad. No se trasladan umbrales E6 a CRUD. |
| E8 | Umbrales por ruta anteriores durante 600 s medidos | Calentamiento separado; estabilidad temporal, memoria observada, integridad y controles de recuperación. |
| E9 | Conserva criterios E5; no agrega SLA | Identificar errores, recuperación funcional y estabilidad parcial bajo sobrecarga. |

Donde se aplicó el criterio de menos de 1% de fallidas, se evalúa además el límite **por muestra**: un p95 aceptable no compensa muestras fuera del umbral. `success=false` puede representar fallo de duración con HTTP/contenido correctos. Los fallos de transporte no son códigos HTTP 500.

## Método, entorno y trazabilidad

Se contrastaron informes finales/revisados, configuraciones, conciliaciones e integridad ya registradas. Se recalcularon en lectura los recuentos, códigos, fallidas, medias, p95/p99 y ventanas de los JTL. El [verificador offline](../analisis/verificar-informe-final.js) produce el [resumen verificable con SHA-256](../analisis/verificacion-informe-final.json); comprueba que cada JTL conserva su hash entre las lecturas anterior y posterior. No vuelve a ejecutar analizadores que escriben dentro de `resultados`. Las pruebas ficticias se excluyen del rendimiento de AppoinTech.

Para corridas finitas, ventana HTTP = `max(timeStamp+elapsed) − min(timeStamp)`; throughput total = muestras/ventana en segundos. Percentiles: rango más cercano, posición `ceil(p×N)`, desde 1. Se incluyen rampa y timeouts, sin recorte. En E6 se separan rampa de 20 s, sostenido de 120 s y cierre de ciclos admitidos antes del plazo; en E8, calentamiento de 60 s y medición de 600 s. Latencias/fallos se asignan por **inicio**, respuestas recibidas por **fin** en intervalos [a,b); el cierre final incluye el último fin. Los cruces de frontera explican que sus numeradores difieran. No se mezcla una tasa de inicios con una tasa de respuestas conformes.

Las ejecuciones fueron contra `http://localhost:18080`, backend con perfil `perf` directo, sin nginx, proyecto `appointech-perf`, red exclusiva y JDBC a `db-perf:5432/appointech_perf`. [Compose de referencia](../docker-compose.perf.yml). Límites históricos comprobados: backend 2 CPU y 1 GiB; PostgreSQL 1 GiB sin cuota CPU explícita. El pool observado en E3 fue 30 conexiones; no se deduce la configuración efectiva de todas las corridas de los valores predeterminados actuales del Compose. JMeter, backend, BD y Docker compartían máquina; no se dispone de medición completa del generador ni de recursos dedicados del anfitrión.

PostgreSQL usó `tmpfs`, `fsync=off` y `synchronous_commit=off`. Los datos fueron sintéticos; el perfil desactiva o simula servicios externos. Persistencia verificada significa filas comprobadas tras las operaciones, **no durabilidad frente a caída**. Las corridas secuenciales y sus preparaciones tuvieron distinto calentamiento; las comparaciones no aíslan causalmente el número de usuarios.

## Matriz E1–E9

Las referencias R1–R8 remiten al inventario enlazado al final, con informes y JTL originales. En E9 la referencia es R5: son exactamente los mismos archivos.

| Escenario | Requisito del profesor | Configuración real y ejecución | Evidencia | Resultado funcional | Cumplimiento de rendimiento | Limitaciones/cobertura |
|---|---|---|---|---|---|---|
| E1 login | p1–2: 20–50 usuarios, rampa 10 s, 5 loops; respuesta/token, estabilidad, sin 500 | **Ejecutado**: 20 y 50, 5 POST/hilo, rampa 10 s; 100/250 muestras | R1 | **Cumple**: todas HTTP 200, contenido/token válidos; huellas sin cambios | **Cumple** en ambas: 0 fallidas, máximos 763/498 ms <2.000 | Cortas; máximo de hilos activos registrado 6/11, no 20/50 sostenidos; sin recursos medidos ni navegador |
| E2 lectura | p2: 30–80, rampa 15 s, 10 loops GET; consistencia/corrección | **Ejecutado**: 30/80, notificaciones paginadas; 300/800 GET | R2 | **Cumple**: pertenencia y contenido, huellas y marcas de lectura sin cambios | **Cumple**: 0 fallidas; máximos 203/284 ms <800 | Tres notificaciones por identidad, preparación calienta datos; hilos activos máx. 7/19; sin recursos |
| E3 registro | p2–3: 20–40, rampa 10 s, POST; éxito, persistencia, integridad, sin duplicados | **Ejecutado**: 20 original, 20 posterior con preparación corregida, 40; una creación/hilo | R3 | Original: 8 respuestas válidas y 12 timeouts, aunque persistieron 20 pares usuario/cliente. Posteriores: 20/20 y 40/40 HTTP 201 válidos e íntegros | **No cumple**: original 20/20 fallidas; posteriores 1/20 (5%) y 8/40 (20%) por duración >1.500 ms | Umbral provisional; pocas escrituras; claves distintas no prueban carreras por la misma clave. Fallos de preparación separados de las tres corridas |
| E4 negocio | p3–4: login→consulta→registro→actualización→consulta final; 10–20, 5 loops | **Ejecutado**: 10/rampa30 y 20/rampa5; login→especialidades→crear→cancelar→detalle, 50/100 flujos | R4 | **Cumple** para recorrido REST adaptado: 50/50 y 100/100 completos; persistencia conciliada | 10: **cumple**, 0/250. 20: **no cumple**, 11/500 (2,20%) por duración; 89/100 flujos aprobados | **Cobertura parcial** del negocio total y frontend; termina en cancelación, sin reparación/cobro. Primera corrida máx. 1 hilo activo; cambian usuarios y rampa |
| E5 estrés login | p4: progresión 50/100/200/300 hasta degradación; tiempos, errores/timeouts | **Ejecutado**: cuatro procesos secuenciales, rampa10, 20 POST/hilo, recuperación entre fases | R5 | HTTP 200 recibidas válidas: 1.000/1.980/2.663/642; timeouts desde 100; recuperación funcional registrada | **No cumple** SLA en ninguna fase; fallidas 29,10/75,10/94,625/98,933%. Objetivo de observar degradación cubierto | No es una rampa continua ni prueba de caída interna; timeout trunca observación del servidor; controles sin recursos simultáneos |
| E6 estrés consultas | p4–5: 100–300, rampa20; miles de consultas críticas, lentitud/bloqueos/recursos | **Ejecutado** sostenido: 100/200/300, 20+120 s, ciclos N,H,E,N,H; 102.525/100.070/102.705 GET | R6 | **Cumple** HTTP/contenido en las tres; cero timeout; integridad sin cambios | **No cumple** duración por muestra en las tres; 77/6.741/11.846 fallidas. Con 100, porcentaje <1%; 200/300 tampoco cumplen ese criterio | **Cobertura parcial** de recursos de 300; no se demuestran bloqueos ni causa exclusiva en BD. Intentos parciales previos excluidos de esta comparación |
| E7 estrés CRUD | p5: 50–150, 20 loops; crear/editar/eliminar; pérdida/inconsistencia/bloqueos/concurrencia | **Ejecutado**: 50/100/150, 20 ciclos/hilo, seis HTTP/ciclo y tres checkpoints SQL; rampa configurada20 y coordinación instrumentada | R7 | **Cumple** integridad observada: 1.000/2.000/3.000 ciclos; 6.000/12.000/18.000 HTTP; cero fallos, incertidumbres o residuos | Duraciones informadas; cumplimiento de SLA **no evaluable**, no existe umbral CRUD. No equivale a capacidad sin instrumentación | Cuenta admin compartida, 50 técnicos; checkpoints condicionan ritmo y contención por técnico; no cubre navegador |
| E8 resistencia | p5–6: 50 usuarios, 10–15 min; estabilidad, memoria, degradación y recuperación | **Ejecutado** válido: 50 hilos, 60 s calentamiento +600 s medición, turnos cada5 s/hilo, tres rutas | R8 | **Cumple** contratos/integridad observados; 5.996 respuestas medidas conformes y nueve GET de recuperación | **Cumple** umbrales de las respuestas medidas; estabilidad observada en 10 min; ritmo real 9,9933/s, cuatro turnos omitidos | Extremo inferior de duración sugerida; HTTP solapadas máx.4, no50; no prueba saturación ni ausencia de fugas. Intento anterior defectuoso separado |
| E9 falla controlada | p6: incrementar hasta errores, identificar tipo, recuperación y estabilidad parcial | **Ejecutado mediante reutilización E5**, sin nueva corrida ni nuevas fases | R5 + [informe E9](E9-REUTILIZACION-E5.md) | Errores de duración y transporte identificados; recuperación funcional, salud histórica y algunas respuestas válidas | Conserva **no cumple** de E5; no se inventa SLA E9 ni HTTP500 obligatorio | **Cobertura parcial**: frontend ausente, sin caída interna demostrada, sin recursos durante controles ni prueba de vaciado de backlog; no es evidencia independiente |

## Hallazgos cuantitativos

Se usa punto decimal y cifras sin separador de miles en las tablas siguientes para facilitar su contraste automático.

### E3 y E4: respuesta funcional frente a duración

| Corrida | Muestras | HTTP funcionalmente válidas | Fallidas únicas | p95/p99 ms | Máximo ms | Evaluación |
|---|---:|---:|---:|---:|---:|---|
| E3-20 original | 20 | 8 | 20 (100%) | 10086/10086 | 10086 | 12 timeouts; 8 HTTP201 lentas; las 20 superan 1500 ms |
| E3-20 posterior | 20 | 20 | 1 (5%) | 1275/1676 | 1676 | Una HTTP201 falla solo duración |
| E3-40 | 40 | 40 | 8 (20%) | 2666/2950 | 2950 | Ocho HTTP201 fallan solo duración |
| E4-10/rampa30 | 250 | 250 | 0 | 59/104 | 1443 | 50 flujos funcionales y aprobados |
| E4-20/rampa5 | 500 | 500 | 11 (2.20%) | 828/2996 | 3594 | 100 funcionales, 89 aprobados; 8 login, 2 especialidades, 1 cancelación lentos |

E3 persistió exactamente 20/20/40 usuarios y perfiles en sus respectivas cohortes, sin duplicados ni huérfanos detectados; los doce timeouts originales también persistieron. Por ello un reintento ciego podría duplicar efectos si no existe idempotencia. En E4 se conciliaron 150 citas canceladas en total, historiales y pagos pendientes, con 300 notificaciones previstas; no hubo cobros. Los controles de integridad y las filas esperadas no convierten una respuesta lenta en una muestra aprobada.

E3 usa pocas muestras y E4 mezcla umbrales por operación; los percentiles globales son descriptivos. La primera E4 registró máximo un hilo activo y la segunda veinte. Los cambios simultáneos de rampa y usuarios impiden atribuir las diferencias solo a duplicar usuarios.

### E5 y E9: degradación del login en las mismas corridas

| Usuarios | N / ventana s | HTTP200 | 200 válidas lentas >2000 ms | SocketTimeoutException | Fallidas únicas | p95/p99 ms | Total/s | HTTP200 recibidas/s |
|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 50 | 1000 / 40.693 | 1000 | 291 | 0 | 291 (29.100%) | 2957/3789 | 24.5743 | 24.5743 |
| 100 | 2000 / 79.553 | 1980 | 1482 | 20 | 1502 (75.100%) | 6966/9961 | 25.1405 | 24.8891 |
| 200 | 4000 / 158.641 | 2663 | 2448 | 1337 | 3785 (94.625%) | 10015/10017 | 25.2142 | 16.7863 |
| 300 | 6000 / 210.158 | 642 | 578 | 5358 | 5936 (98.933%) | 10017/10022 | 28.5499 | 3.0548 |

Conciliación: fallidas únicas = HTTP200 válidas lentas + timeouts; no se suman de nuevo los timeouts como fallos de duración. No hubo códigos HTTP numéricos distintos de 200 ni fallos de contenido entre respuestas recibidas. El total/s aumenta porque cuenta también esperas agotadas; las HTTP200/s caen drásticamente y tampoco todas cumplen duración. En 300 solo 64 muestras fueron conformes en contenido y duración.

El cliente tenía timeout de respuesta de 10000 ms; los p95 cercanos a ese valor miden esperas truncadas, no la latencia final completa del servidor. En 300 el último HTTP200 terminó a 103.269 s desde el primer inicio; el último final cliente ocurrió a 210.158 s. No se afirma disponibilidad funcional durante toda la fase a partir de 642 éxitos agregados.

### E6: degradación de consultas entre 100 y 200/300

| Usuarios | Total JTL | Rampa + sostenido + cierre (inicios) | Fallidas totales por duración | Inicios sostenidos/s | HTTP200 recibidas sostenidas/s | Conformes recibidas sostenidas/s | p95 sostenido N/H/E ms |
|---:|---:|---|---:|---:|---:|---:|---|
| 100 | 102525 | 9526 + 92781 + 218 | 77 (0.0751%) | 773.175 | 773.175 | 772.550 | 268/282/177 |
| 200 | 100070 | 13683 + 85991 + 396 | 6741 (6.7363%) | 716.592 | 716.600 | 661.983 | 1187/1177/270 |
| 300 | 102705 | 10996 + 91106 + 603 | 11846 (11.5340%) | 759.217 | 759.192 | 666.625 | 2160/2176/263 |

N/H/E = notificaciones/historial/especialidades. Todas las muestras fueron HTTP200 de contenido válido, sin timeout, transporte u otros errores HTTP. En sostenido hubo 75/6596/11092 excesos de duración **por inicio**, subconjuntos de 77/6741/11846 globales. El throughput conforme recibido usa numeradores distintos: 92706/79438/79995, divididos por 120 s, porque clasifica por fin. No se confunden estas cohortes.

Los p95 de notificaciones e historial pasan de 268/282 ms a 1187/1177 y 2160/2176 ms, superando el umbral de 800 ms en 200/300. Especialidades conserva p95 <300 ms en sostenido, pero contiene muestras lentas: 45/477/439. Por tanto **100 tampoco cumple todas las duraciones por muestra**, pese a cumplir el porcentaje global <1%. Los solapamientos HTTP medios sostenidos fueron 92.393/192.710/291.911, con máximos 100/200/300; son intervalos observados por el cliente, no ejecución simultánea demostrada del servidor.

La caída de rendimiento conforme y el aumento de latencias documentan degradación. No identifican por sí solos bloqueo SQL, agotamiento de pool ni una causa única. La preparación interrumpida anterior a 200 añadió calentamiento; no es un experimento que aísle únicamente N.

### E7: integridad CRUD bajo instrumentación

| Usuarios | Ciclos completos | HTTP / ventana s | Throughput HTTP/s | Checkpoints SQL | Fallidas / inciertas / residuos |
|---:|---:|---|---:|---:|---|
| 50 | 1000 | 6000 / 51.364 | 116.813 | 3000 | 0 / 0 / 0 |
| 100 | 2000 | 12000 / 55.185 | 217.450 | 6000 | 0 / 0 / 0 |
| 150 | 3000 | 18000 / 57.556 | 312.739 | 9000 | 0 / 0 / 0 |

Cada ciclo creó una franja, consultó, editó, consultó, borró y verificó ausencia, conservando el ID obtenido por POST. Se comprobó propietario, estado, preservación de `creada_en` y ausencia tras DELETE. No hubo reintentos de escritura ni limpieza posterior; las 15 tablas comparadas conservaron cantidades/huellas y las 1260 franjas preexistentes permanecieron intactas. Auditoría y secuencias no se tratan como inmutables.

En 150, p95/p99 de crear fueron 266/1256 ms, editar 219/795 y borrar 162/197. Se informan, sin inventar aceptación por SLA. Los checkpoints SQL están fuera del elapsed HTTP pero dentro del ritmo y ventana; en 150 hubo cero HTTP en vuelo durante 41.040% de la ventana, media de solapamiento 22.911 y máximo150. Una cuenta administrativa y tres hilos por técnico no equivalen a 150 clientes independientes ni a carga CRUD sostenida sin observación. El aumento de throughput no demuestra mejora causal del backend.

### E8: estabilidad observada a ritmo limitado

La corrida válida tuvo 600 HTTP de calentamiento y 5996 HTTP medidos en 600 s, todos HTTP200 y conformes. Ritmo medido: `5996/600 = 9.9933/s`; se omitieron cuatro de 6000 turnos (0.0667%), sin recuperarlos en ráfaga. No se presenta como 10.0000/s exactos. Los 50 hilos permanecieron activos, pero HTTP solapadas máximas4 y media0.18931.

| Indicador | Minuto 1 | Minuto 10 |
|---|---:|---:|
| Iniciadas y conformes recibidas/s | 9.9667 | 10.0000 |
| Notificaciones p95/p99 ms | 39/97 | 23/26 |
| Historial p95/p99 ms | 53/281 | 24/28 |
| Especialidades p95/p99 ms | 22/108 | 19/20 |
| CPU backend media % Docker | 4.317 | 2.574 |
| Memoria backend media MiB | 635.814 | 636.117 |
| Memoria PostgreSQL media MiB | 160.917 | 159.640 |

No se observa deterioro progresivo de percentiles ni una deriva grande y sostenida de memoria de contenedor durante estos diez minutos. El incremento de media backend entre minutos extremos es 0.303 MiB. **No demuestra ausencia de fugas:** faltan heap, objetos retenidos, GC, mayor duración y réplicas. Las 15 tablas conservaron cantidades/huellas y los nueve GET de recuperación fueron correctos y dentro de umbral. Los resultados se limitan a tres rutas de lectura y datos repetidos; no son una prueba de saturación.

## Recursos y recuperación

CPU es porcentaje crudo Docker, con 100% por CPU lógica, sin normalizar a la cuota. Los máximos muestreados pueden superar 200% y no demuestran cambio del límite. Memoria es de contenedor, no heap Java. Las capturas, generalmente cada ~2 s, no son picos absolutos ni permiten atribuir consumo a cada endpoint.

| Evidencia/ventana | Backend CPU máx. % / memoria máx. MiB | PostgreSQL CPU máx. % / memoria máx. MiB | Cobertura |
|---|---|---|---|
| E3-40 durante HTTP | 203.75 / 452.9 | 14.09 / 153.2 | Solo cuatro capturas por servicio dentro de HTTP |
| E5-50 durante HTTP | 220.33 / 505.5 | 9.50 / 157.5 | 20 capturas/servicio |
| E5-300 durante HTTP | 278.05 / 634.3 | 52.47 / 165.8 | 102 capturas/servicio; recolector acaba antes de controles |
| E6-100 sostenido | 232.53 / 621.7 | 179.76 / 188.3 | 58 capturas/servicio |
| E6-200 sostenido | 237.72 / 634.4 | 174.13 / 188.9 | 59 capturas/servicio |
| E6-300 sostenido, parte disponible | 230.66 / 633.8 | 183.50 / 188.6 | 47/46 capturas; faltan los últimos 25.443/27.527 s y todo cierre |
| E7-150 durante HTTP | 220.21 / 644.9 | 159.37 / 207.8 | 28 capturas/servicio en HTTP; 52 pares totales |

En E6-300 el archivo conserva 119 filas, 59 pares completos y una captura adicional de backend. El recolector falló por conflicto de escritura; no se interpoló ni reparó el CSV. Los máximos anteriores solo describen la parte disponible. La carga está ejecutada y su JTL conciliado; la validación integral de recursos queda **parcial**. E1/E2 carecen de captura de recursos. E8 conserva 348 pares completos para calentamiento, medición, drenaje y recuperación, con hueco máximo3525 ms; sus medias temporales se muestran arriba.

| Escenario | Recuperación funcional histórica | Alcance de la conclusión |
|---|---|---|
| E5/E9, 50/100/200 | Tres logins válidos consecutivos: 143/64/59; 354/87/186; 153/67/61 ms | Recolector cerrado antes de controles; no prueba retorno de recursos a base |
| E5/E9, 300 | Primer control falla por transporte (10567 ms), luego 99/111/66 ms válidos | Tres consecutivos acreditados al terminar el cuarto control, 36.704 s tras último fin cliente JTL; no recuperación inmediata |
| E6 100/200/300 | Tres HTTP200 válidas por fase: 11/10/5; 24/9/12; 22/7/30 ms | Integridad igual; no prueba vaciado de todo trabajo pendiente ni subsana recursos parciales de300 |
| E7 50/100/150 | Tres GET válidos: 12/10/16; 20/22/14; 6/12/15 ms | Recursos no cubren controles de50; sí los de100/150 |
| E8 | Nueve GET válidos y dentro de umbral | Lecturas de control; no recuperación de caída o pérdida de almacenamiento |

Los estados históricos registrados mantuvieron identidad/límites, salud, cero reinicios y OOM no observado en las fases descritas; no se consultó salud actual. Salud Docker no garantiza respuesta funcional a tiempo: el primer control E5-300 falló aunque se registró salud positiva. Su clase de excepción no quedó conservada, por lo que **no se le atribuye retroactivamente SocketTimeoutException**. Tampoco se conoce cuándo el servidor completó el trabajo abandonado por el cliente. Los timeouts JTL de E5 sí conservan esa clase y `Read timed out`.

## Conciliación y discrepancias que deben permanecer visibles

Los recuentos, códigos y percentiles recalculados de los JTL coinciden con las cifras finales citadas, con los redondeos indicados. Se preservan las siguientes diferencias entre artefactos; no se silencian ni se reescriben sus fuentes:

| Diferencia | Resolución documental y efecto |
|---|---|
| Secciones iniciales de E3 dejan40 pendiente; E4 dice solo10; informe histórico E6 deja500 GET y200/300 pendientes | Son estados históricos superados por apéndices/corridas posteriores. Este cierre usa tres JTL E3, dos E4 y tres E6 sostenidos, sin borrar los intentos previos. |
| E3 original: ejecutor1, JMeter0 y20 fallidas | Validador CSV rechazó cabecera entrecomillada; defecto auxiliar distinto de los12 timeouts y8 respuestas lentas reales. Código0 nunca equivale a cumplimiento. |
| E4 conciliación inicial con falsos negativos | Aliases camelCase SQL sin comillas causaron expectativas nulas. Se usa `reconciliacion-verificada.json` y conciliación individual; el original permanece. No se interpreta como fallo de datos o retraso probado de notificaciones. |
| E6 original solo500 muestras en vez de5000 previstas | Defecto real de bucle del plan anterior; sus cifras no se suman a las fases sostenidas posteriores ni se presentan como sobrecarga completa. |
| E6-100 rampa: 471.30/s en informe anterior frente a476.30/s de inicios | 9426 finales/20 s frente a9526 inicios/20 s;100 cruzan frontera. Se usa el informe revisado y se etiqueta cada tasa. |
| E6-300 `muestrasMedidas=0`, preparación inválida en diagnóstico, frente a102705 filas JTL | El error del recolector omitió asignaciones posteriores a JMeter. Setup, JTL y revisión posterior acreditan carga; no se corrigen los ceros originales. La validación global queda parcial por recursos. |
| E6-300 CSV119 filas, no120 ni una serie final completa | 59 pares y una fila extra; hueco final y cierre sin capturas. No se inventa máxima CPU/memoria de toda la fase. |
| E8 intento anterior:4 muestras, tres401 | Fallo HeaderManager durante calentamiento del plan anterior; excluido de la corrida válida, preservado como incidente de ejecución. No se atribuye a degradación por resistencia. |
| E8 total6596 frente a5996 medidas | 600 de calentamiento +5996 medidas; cuatro turnos omitidos, sin inicios de cierre. El dashboard se reparó offline; el estado del lanzador original permanece documentado en la auditoría de continuación. |
| E9 frente a E5 | Mismos1000+2000+4000+6000 =13000 intentos; no26000 ni otras cuatro corridas independientes. |

No queda una cifra principal sin conciliar entre este documento y los JTL releídos. **Sí quedan no conciliables como si fueran métricas equivalentes** los contadores iniciales E6-300 y la captura final ausente: se explica su causa documental y se mantiene la limitación, sin fabricar mediciones. Las huellas prueban igualdad en los estados comparados, no ausencia de escrituras transitorias revertidas ni integridad de cualquier operación posible.

## Recomendaciones priorizadas y limitaciones de cierre

1. **P1 — Respuesta y protección ante sobrecarga.** Investigar login y registro con perfiles CPU/BCrypt, colas de petición, esperas de pool y trazas correlacionadas. Evaluar admisión limitada, contrapresión y respuesta controlada de sobrecarga. No reducir seguridad BCrypt ni ampliar timeouts para convertir fallos en aprobación. Definir idempotencia/conciliación para escrituras con respuesta incierta, dado E3.
2. **P1 — Consultas bajo concurrencia.** Analizar planes SQL, índices, paginación, contención y métricas Hikari/Tomcat, junto con el coste del generador. E6 muestra deterioro de N/H pero no permite elegir una causa única ni justificar aumentar el pool sin diagnóstico.
3. **P1 — Integridad de la instrumentación.** Resolver acceso concurrente al CSV y garantizar cierre antes de lectura, pares completos y captura hasta recuperación. Mantener hashes, configuraciones y diagnósticos originales. E6-300 seguirá parcial; estas mejoras no completan retroactivamente su serie.
4. **P2 — Criterios acordados.** Formalizar SLA CRUD y los umbrales provisionales E3/cancelación/detalle; distinguir éxito funcional, conformidad temporal y objetivo de estrés. Mantener percentiles y proporciones por ruta, no solo globales.
5. **P2 — Representatividad futura.** En una campaña futura separada, usar generador independiente, almacenamiento persistente y frontend/nginx, cubrir reparación completa y servicios externos controlados; ampliar datos, distribución de accesos, duraciones y réplicas con calentamiento comparable. Medir recursos del anfitrión/generador y heap/GC. No se ejecuta ni programa esa campaña en este cierre.

Las conclusiones se restringen a esta máquina, perfil perf, datos sintéticos y ventanas observadas. PostgreSQL en memoria y durabilidad relajada, ausencia de nginx/navegador, preparaciones que calientan datos y acceso directo impiden extrapolar producción. Los checkpoints E7 alteran el ritmo; E8 usa lecturas repetidas a ~10/s y solo diez minutos; E9 aporta cobertura parcial mediante evidencia reutilizada. Sin capturas completas de E6-300, trazas internas continuas ni medición de heap/GC, no se afirman capacidad productiva, causa definitiva, ausencia de fugas, caída interna, recuperación total o ausencia de episodios transitorios no observados.

## Inventario de informes y JTL originales

Cada enlace de JTL apunta al archivo medido, sin copia ni modificación. Los informes originales enlazan configuraciones, integridad, recursos y logs de cada carpeta. Los SHA-256 de la lectura de cierre están en el [resumen offline](../analisis/verificacion-informe-final.json).

| Ref. | Informes de referencia | JTL originales |
|---|---|---|
| R1 | [Informe E1](../jmeter/INFORME-E1.md) | [20](../resultados/e1-20261110-173448-cdb7e37a9d934eee960c165c926a1b8f/e1.jtl), [50](../resultados/e1-20261110-173809-f42763b448d64438bf18c9875ce877d0/e1.jtl) |
| R2 | [Informe E2](../jmeter/INFORME-E2.md) | [30](../resultados/e2-20261110-181106-d14cb52a786b4f92bcdea378fd108bb2/e2.jtl), [80](../resultados/e2-20261110-181450-fd5a622d79614b1fac519c9715ce7a1e/e2.jtl) |
| R3 | [Informe E3 y apéndices](../jmeter/INFORME-E3.md) | [20 original](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/e3.jtl), [20 posterior](../resultados/e3-20261110-224819-8f7506b7d4544029874112ad851d2dd4/e3.jtl), [40](../resultados/e3-20261110-230629-7694a5f7a7de46a5b85f846ba689d1e7/e3.jtl) |
| R4 | [Informe E4 y segunda corrida](../jmeter/INFORME-E4.md) | [10](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/e4.jtl), [20](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/e4.jtl) |
| R5 | [Informe E5](../jmeter/INFORME-E5.md), [E9: reutilización y reanálisis E5](E9-REUTILIZACION-E5.md) | [50](../resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/e5.jtl), [100](../resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/e5.jtl), [200](../resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/e5.jtl), [300](../resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/e5.jtl) |
| R6 | Revisados: [100](../resultados/e6-sostenido-20261111-155946-b974f542a3964824986aff289fedfaee/fase-100/INFORME-REVISADO.md), [200](../resultados/e6-sostenido-20261111-161532-08984f90623245f6b959fef905e47df2/fase-200/INFORME-REVISADO.md), [300](../resultados/e6-sostenido-20261111-163033-3edb7724625d47959406bb744830cd8f/fase-300/INFORME-REVISADO.md); [histórico/defecto de bucle](../jmeter/INFORME-E6.md); [nota de recursos parciales](../jmeter/NOTA-E6-300-RECURSOS-PARCIALES.md) | [100](../resultados/e6-sostenido-20261111-155946-b974f542a3964824986aff289fedfaee/fase-100/e6.jtl), [200](../resultados/e6-sostenido-20261111-161532-08984f90623245f6b959fef905e47df2/fase-200/e6.jtl), [300](../resultados/e6-sostenido-20261111-163033-3edb7724625d47959406bb744830cd8f/fase-300/e6.jtl) |
| R7 | [50](../resultados/e7-50-20261111-182840-7a34d9f7ac124f42b3bcfbefa82b8472/INFORME.md), [100](../resultados/e7-100-20261111-185538-d32a9fe776dc48e4bd0c5efe4a6dad8b/INFORME.md), [150](../resultados/e7-150-20261111-194906-2cd69ea2b9e64faba9758845b72c86a7/INFORME.md), [comparativo final](../resultados/e7-150-20261111-194906-2cd69ea2b9e64faba9758845b72c86a7/INFORME-COMPARATIVO.md) | [50](../resultados/e7-50-20261111-182840-7a34d9f7ac124f42b3bcfbefa82b8472/e7.jtl), [100](../resultados/e7-100-20261111-185538-d32a9fe776dc48e4bd0c5efe4a6dad8b/e7.jtl), [150](../resultados/e7-150-20261111-194906-2cd69ea2b9e64faba9758845b72c86a7/e7.jtl) |
| R8 | [Cierre E8](E8-CIERRE.md), [informe de corrida válida](../resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/INFORME.md), [auditoría de continuación](../resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/auditoria-continuacion-final.json) | [Válido, incluye calentamiento](../resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/e8.jtl); [intento anterior excluido](../resultados/e8-50-20261111-213358-1fb139ce0251410aa9d24dd1382a784a/e8.jtl) |

El cierre documenta ejecución, aceptación y cobertura por separado. Las limitaciones y discrepancias anteriores forman parte del resultado entregable y no se eliminan para declarar aprobación global.
