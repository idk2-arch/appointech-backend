# E4: primera corrida del flujo CLIENTE

## Alcance, entorno y configuración

Se ejecutó una sola corrida real: **10 usuarios configurados, rampa de 30 s y 5 iteraciones por hilo**, cincuenta flujos y 250 HTTP. Cada iteración hizo login → consulta de especialidades → creación de solicitud → cancelación propia → consulta de detalle. El flujo termina en cancelación: **no cubre la ejecución completa de una reparación**, asignación de técnico, diagnóstico o cobro. No se ejecutó veinte usuarios ni E5.

Destino directo `http://localhost:18080`, publicado por backend-perf del proyecto appointech-perf, JDBC a db-perf/appointech_perf y red exclusiva. Se confirmaron salud, tmpfs y perfil perf. También se leyó el perfil empaquetado: Haversine, pasarela simulada e IA desactivada; NOTIFICACIONES_URL vacía impide correo. Se descartaron overrides de configuración no verificados; JAVA_TOOL_OPTIONS solo fijaba America/Bogota. Las rutas E4 no llaman pasarela, fotos, IA ni geocodificación. No se cambiaron recursos, BCrypt o código de negocio ni se reiniciaron servicios.

La consulta de solo lectura confirmó diez cuentas sintéticas CLIENTE distintas y completas. Se eligió la especialidad **ID 1, Lavadoras, costo de referencia 80.000 COP y 16 técnicos activos elegibles**. Los IDs se obtuvieron de la base, no se supusieron. Las cincuenta marcas nuevas tuvieron cero colisiones.

Umbrales conservados: login 2.000 ms, especialidades 300 ms, creación 1.500 ms, **cancelación 1.500 ms provisional** y **detalle 800 ms provisional**, estos dos reutilizados de las categorías existentes de escritura y lectura. Timeouts independientes de conexión/respuesta: 5.000/10.000 ms. No se ajustaron después de medir.

Comando realmente ejecutado, una vez desde la raíz:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e4.ps1 -Usuarios 10 -Rampa 30 -Loops 5 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

## Revisión y separación de preparación

Se releyeron controladores, DTO, seguridad, casos de uso, esquema y servicios Angular. Se validó el XML y la sintaxis en PowerShell 5.1.26100.9711. La revisión local ejecutó el mismo soporte Groovy con cinco respuestas ficticias y loops=0, usando loopback puerto 9: **cero solicitudes HTTP**. Así se compiló y ejercitó la preparación/validación antes de la corrida real. Se corrigió un detector de logs del auxiliar de revisión que confundía mensajes INFO terminados en «on error» con nivel ERROR; las revisiones locales quedaron conservadas y no enviaron negocio.

Los cuerpos se serializan antes de la rampa; setup prepara cuentas/expectativas y ejercita las mismas fuentes Groovy sin login ni registro reales. El preprocesador medido asigna referencias y cuerpos preparados. No hay barreras, pausas añadidas ni recorte de muestras. Setup/teardown usan SampleResult.setIgnore(): el JTL contiene exclusivamente los cinco tipos de HTTP medidos.

Preparación SQL/cohorte y aislamiento: **2.857 ms**, cero HTTP. Preparación local en Java: **258 ms**, cero HTTP. El login forma parte de cada una de las cincuenta iteraciones medidas. Los paquetes de entorno no se guardaron en reportes y fueron limpiados en finally. Los resultados solo correlacionan marca, hilo, iteración e ID de cita; sin contraseñas, tokens o cuerpos de autenticación.

## Flujos y errores

| Resultado | Cantidad |
|---|---:|
| Flujos previstos y registrados | 50 |
| Completos funcionalmente | 50 |
| Completos aprobados, incluida duración | 50 |
| Completos fallidos solo por duración | 0 |
| Incompletos | 0 |
| Fallidos, unión de incompletos y completos no aprobados | 0 |
| Hilos con exactamente cinco flujos propios | 10 |
| Muestras medidas | 250 |
| Muestras success=false | 0 (0 %) |

No hubo paso fallido que desglosar por motivo. Cada marca tiene exactamente los cinco pasos, una identidad estable y un ID creado propio. Los pasos funcionalmente inválidos no habrían sido borrados por una respuesta posterior correcta. Los fallos exclusivos de duración permiten continuar; no se reintenta ningún POST. Ese mecanismo no fue necesario ante fallos reales en esta corrida porque no ocurrieron.

| Paso | Muestras | HTTP recibido | HTTP inesperado | Transporte | Contenido | Duración |
|---|---:|---|---:|---:|---:|---:|
| Login | 50 | 200 × 50 | 0 | 0 | 0 | 0 |
| Especialidades | 50 | 200 × 50 | 0 | 0 | 0 | 0 |
| Crear solicitud | 50 | 201 × 50 | 0 | 0 | 0 | 0 |
| Cancelar | 50 | 200 × 50 | 0 | 0 | 0 | 0 |
| Detalle | 50 | 200 × 50 | 0 | 0 | 0 | 0 |

Las categorías pueden solaparse en general; aquí todas fueron cero. Los contratos se validaron además de HTTP: JSON y tipos, token no vacío, especialidad esperada, campos de la solicitud, PENDIENTE al crear, CANCELADA al cancelar, ID propio y transición/actor/motivo del historial en el detalle. Las respuestas de solicitud/detalle son objetos directos, no envoltorios exito/datos.

JMeter y ejecutor terminaron con **código 0**. El código de salida por sí solo no se tomó como aceptación: se comprobaron muestras, flujos, aserciones, integridad y artefactos.

## Tiempos y throughput

| Paso | Media ms | p95 ms | p99 ms | Máximo ms | Ventana del paso s | Solicitudes/s |
|---|---:|---:|---:|---:|---:|---:|
| Login | 62,26 | 65 | 322 | 322 | 27,392 | 1,825 |
| Especialidades | 6,58 | 11 | 85 | 85 | 27,080 | 1,846 |
| Crear solicitud | 46,64 | 33 | 1.443 | 1.443 | 27,006 | 1,851 |
| Cancelar | 14,48 | 21 | 64 | 64 | 25,573 | 1,955 |
| Detalle | 9,86 | 14 | 95 | 95 | 25,517 | 1,959 |
| **Conjunto** | **27,964** | **59** | **104** | **1.443** | **27,447** | **9,108** |

Cada throughput de paso divide sus cincuenta muestras por el intervalo entre el inicio de su primera muestra y el final de su última. Normalizado a la ventana global, cada paso aporta 50/27,447 = **1,822 solicitudes/s**. Conjunto: 250/27,447. No se utilizó la duración aproximada del resumen de consola.

timeStamp representa inicio, configurado explícitamente. Ventana global: **2026-11-11T04:35:40.141Z–04:36:07.588Z**, equivalente al 10 de noviembre a las 23:35:40.141–23:36:07.588 UTC−05:00. Final = timeStamp + elapsed. Se conserva toda la rampa.

Percentiles por rango más cercano ceil(p×N), desde 1. Por paso N=50: posiciones 48/50 para p95/p99, por eso p99 coincide con el máximo. Para N=250: posiciones 238/248; el p99 global de 104 ms no es el máximo de 1.443 ms. Mediana global: 12,5 ms. Todas las respuestas se recibieron; no hay tiempos hasta timeout que confundir con latencia final del servidor. La creación más lenta, 1.443 ms, quedó 57 ms por debajo de su umbral; esta corrida corta no demuestra que siempre lo cumpla.

## Distribución real y concurrencia observada

Los primeros logins de los diez hilos abarcaron **26,934 s**, con retrasos arranque de hilo→HTTP de **2–69 ms**. Es coherente con treinta segundos de rampa y diez arranques separados aproximadamente tres segundos: la distancia primero–último no tiene que ser treinta segundos exactos.

| Intervalo relativo al primer HTTP | Inicios medidos |
|---|---:|
| [0,5) s | 50 |
| [5,10) s | 50 |
| [10,15) s | 27 |
| [15,20) s | 48 |
| [20,25) s | 50 |
| [25,30) s | 25 |

La distribución por segundo y cada paso/marca están en distribucion.csv y analisis.json. Los pasos de un mismo usuario y sus cinco iteraciones se ejecutaron seguidos, sin pausas: esos grupos de solicitudes no son un arranque simultáneo de diez logins.

**Máximo de allThreads observado en el JTL: 1**, aunque se configuraron diez usuarios. Los flujos rápidos permitieron que los hilos terminaran antes de los siguientes arranques en las muestras observadas. Por tanto, **no se ha probado una concurrencia sostenida de diez usuarios ni diez solicitudes simultáneas**. Este límite es esencial para interpretar los buenos tiempos. No se cambió la rampa para producir otro resultado.

## Integridad y efectos auxiliares

La conciliación de solo lectura verificó las **50 marcas individualmente**:

- Una sola cita por marca, propietario correcto, especialidad esperada, dirección/coordenadas y ausencia de técnico/horarios correctas; cincuenta estados finales CANCELADA.
- Los cincuenta IDs recibidos coinciden con las filas persistidas. Ningún recurso fue compartido entre hilos ni reutilizado entre iteraciones.
- Exactamente un historial por cita, PENDIENTE→CANCELADA, con actor del cliente y motivo igual a la marca.
- Cincuenta pagos PENDIENTE, cotizados a 80.000 COP, sin monto final, método, pago efectuado, referencia o transacción externa; cero comprobantes. **No hubo cobros.**
- Cincuenta notificaciones SOLICITUD_RECIBIDA y cincuenta CITA_CANCELADA, destinatario esperado, no leídas y sin otros avisos asociados. Los avisos se verifican aparte de la transacción principal: se publican después del commit en otra transacción y su guardado es de mejor esfuerzo.

| Cantidad global | Antes | Después | Diferencia |
|---|---:|---:|---:|
| Usuarios | 5.131 | 5.131 | 0 |
| Clientes | 5.080 | 5.080 | 0 |
| Citas | 340 | 390 | +50 |
| Historiales | 0 | 50 | +50 |
| Pagos | 340 | 390 | +50 |
| Notificaciones | 3.000 | 3.100 | +100 |
| Logs | 1 | 101 | +100 |

Las huellas agregadas de usuarios, clientes y citas preexistentes permanecieron idénticas. No se borraron recursos ni se modificaron perfiles. La diferencia de logs corresponde a creación y cancelación/auditoría por flujo; no se exponen mensajes ni datos personales en este informe.

### Corrección de la consulta de conciliación

La consulta auxiliar inicial usaba aliases camelCase no entrecomillados en json_to_recordset. PostgreSQL los convirtió a minúsculas y produjo expectativas nulas, dando falsos negativos de propietario, especialidad y notificaciones. Se corrigieron **solo los aliases de la consulta de lectura** y se hizo otra lectura, sin HTTP ni cambios de datos. El JTL y el plan medido no cambiaron, y no se repitió carga.

Se conservan reconciliacion.json y analisis-inicial.json como evidencias del defecto del auxiliar. **El resultado válido es reconciliacion-verificada.json**, confirmado en correccion-reconciliacion.json e integridad-por-flujo.csv. Los cien avisos ya figuraban en el primer snapshot global posterior; las tres lecturas iniciales por avisos faltantes fueron motivadas por el defecto de alias, no prueban demora real de entrega. La comprobación corregida fue posterior al GET final; no se inventa un instante de entrega por notificación.

El ejecutor hizo cuatro consultas SQL de preparación, tres lecturas iniciales de cohorte y un snapshot posterior; se añadió una lectura de cohorte corregida. Son consultas fuera del JTL. La inspección previa tuvo además una consulta agregada de cuentas/especialidades. **HTTP de preparación o reconciliación: cero.**

## Recursos

Se reutilizó sin cambios el recolector corregido de E3: señal separada, cierre en finally, espera del Job y validación del CSV solo después del cierre. Resultado: **36 filas, 18 capturas por servicio**, dos anteriores a HTTP, catorce durante y dos posteriores. Intervalos reales globales **1,978–2,052 s**, media **2,019 s**.

| Servicio | Capturas HTTP | Máximo CPU muestreado | Máxima memoria de contenedor HTTP | Límites efectivos |
|---|---:|---:|---:|---|
| backend-perf | 14 | 102,39 % | 498,8 MiB | 2 CPU y 1 GiB |
| db-perf | 14 | 7,42 % | 150,5 MiB | Sin cuota CPU explícita y 1 GiB |

Antes: backend máximo 0,22 %/472 MiB, base 0,38 %/147,6 MiB. Después: backend 0,13 %/494,4 MiB, base 0,42 %/150,6 MiB. Se separan de la ventana HTTP. CPU Docker expresa cien por ciento por CPU lógica; no son picos absolutos. Memoria del contenedor no equivale a heap Java. No se capturaron perfiles CPU, GC ni esperas SQL que permitan atribuir las demoras a BCrypt, CPU o base.

## Aceptación, limitaciones y estado final

En **esta configuración y esta corrida**, cumplen contrato HTTP/contenido, pertenencia e integridad principal, efectos auxiliares verificados, umbrales por muestra y menos del 1 % de muestras fallidas: **0/250**. Flujos aprobados: **50/50**. Los buenos resultados no eliminan los fallos históricos de E3, cuyos archivos permanecen intactos.

El backend ya había sido utilizado y no se reinició: no se demuestra arranque frío ni se separa el calentamiento de otros factores. Son cincuenta flujos cortos con máximo de un hilo activo observado, generador y servicios en la misma máquina, acceso directo sin nginx y PostgreSQL en tmpfs con fsync/synchronous_commit desactivados. Se verifica existencia de filas, no durabilidad ante caída. No se demuestra capacidad sostenida, una reparación completa ni el comportamiento de veinte usuarios.

Perf quedó iniciado y saludable; ambos contenedores sin reinicios ni OOM observados, cero docker stats activos. El recolector terminó sin error y quedaron cero variables temporales de autenticación. La revisión del ejecutor cubrió 147 archivos sin detectar contraseñas o tokens; se revisaron aparte los artefactos de análisis añadidos. No hubo commit, push, cambio de rama, cambios de negocio ni limpieza automática.

## Evidencias

Directorio: `../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/`.

- [JTL](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/e4.jtl), [log](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/jmeter.log), [dashboard](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/reporte/index.html), [análisis](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/analisis.json).
- [Configuración](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/ejecucion.json), [aislamiento](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/aislamiento.json), [preparación](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/preparacion.json), [preparación del generador](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/preparacion-generador.json).
- [Flujos y pasos](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/flujos.json), [distribución](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/distribucion.csv), [integridad individual](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/integridad-por-flujo.csv), [conciliación verificada](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/reconciliacion-verificada.json), [corrección de consulta](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/correccion-reconciliacion.json).
- [Antes](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/antes.json), [después](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/despues.json), [recursos](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/recursos.csv), [validación del recolector](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/validacion-recursos.json), [estado final](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/estado-final.json).
- [Verificación final de artefactos y preservación E1–E3](../resultados/e4-20261110-233531-13b32da07fdb4cf3853b0b467af59fc3/verificacion-final.json).
- [Revisión local sin HTTP](../resultados/revision-e4-a01f9b6fdbe343b4a9de04c2244204a8/resumen.json), [plan](e4-flujo.jmx), [ejecutor](ejecutar-e4.ps1), [soporte Groovy](e4-soporte.groovy), [consultas de preparación](e4-preparacion.ps1) y [README](README-E4.md).

## Segunda corrida E4: veinte usuarios y rampa de cinco segundos

Se ejecutó **una sola corrida nueva**, conservando la primera de diez y todas sus evidencias. La guía fija diez–veinte usuarios y cinco loops, pero no fija rampa. **Elegimos cinco segundos porque la corrida previa de diez con treinta segundos mostró como máximo un hilo activo en el JTL.** La segunda busca concurrencia: cambian simultáneamente usuarios y rampa, por lo que no aísla el efecto de duplicar usuarios.

### Preparación y comando

El plan ya parametrizaba usuarios/rampa/loops. Se amplió únicamente la validación del ejecutor a usuarios 10/20 y rampas 5/30, manteniendo cinco loops y los valores predeterminados anteriores. No se modificaron el plan, Groovy, contratos, timeouts, umbrales, negocio, BCrypt o recursos.

Se comprobó sintaxis PowerShell y correspondencia de propiedades. Antes de medir se confirmaron salud, proyecto/red exclusivos de appointech-perf, puerto localhost:18080 publicado por backend-perf, JDBC a db-perf/appointech_perf y tmpfs. Se releyó el perfil empaquetado y configuración efectiva: correo/IA externos desactivados, Haversine y pasarela simulada, sin overrides no verificados.

Se verificaron **veinte cuentas CLIENTE distintas, activas y completas**, especialidad Lavadoras ID 1, costo 80.000 COP y dieciséis técnicos elegibles. Nueva cohorte de **cien marcas, cero colisiones**; preparación de datos/SQL 2.993 ms y cero HTTP. Preparación local del generador 503 ms, cero HTTP. El entorno de Windows se verificó con 31.643 caracteres, bajo el límite conservador del ejecutor.

Comando realmente ejecutado una vez:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e4.ps1 -Usuarios 20 -Rampa 5 -Loops 5 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

### Flujos, aceptación y comparación

| Métrica | 10 usuarios / rampa 30 s | 20 usuarios / rampa 5 s |
|---|---:|---:|
| Flujos previstos / completos funcionalmente | 50 / 50 | 100 / 100 |
| Flujos aprobados, incluida duración | 50 | **89** |
| Flujos incompletos | 0 | 0 |
| Flujos completos fallidos solo por duración | 0 | **11** |
| Muestras | 250 | 500 |
| HTTP 200 / HTTP 201 | 200 / 50 | 400 / 100 |
| HTTP inesperado / transporte / contenido | 0 / 0 / 0 | 0 / 0 / 0 |
| Muestras fallidas y sobre umbral | 0 (0 %) | **11 (2,20 %)** |
| Media global | 27,964 ms | 344,098 ms |
| p95 / p99 global | 59 / 104 ms | 828 / 2.996 ms |
| Máximo global | 1.443 ms | 3.594 ms |
| Ventana HTTP | 27,447 s | 11,650 s |
| Throughput global | 9,108/s | 42,918/s |
| Máximo de hilos activos observado | 1 | **20** |
| Máximo de muestras HTTP en curso según JMeter | 1 | **20** |

Los cien flujos terminaron con los cinco pasos funcionalmente válidos. **Once incumplieron duración y no se consideran aprobados**. En esta corrida cada una de las once muestras lentas pertenece a un flujo distinto; cero incompletos. Pasa integridad/contrato, pero **no pasan todos los umbrales por muestra ni menos del 1 % de muestras fallidas**. JMeter y ejecutor devolvieron 0: no equivale a aprobación.

| Paso | Muestras | Códigos | HTTP inesperado | Transporte | Contenido | Duración | Muestras fallidas |
|---|---:|---|---:|---:|---:|---:|---:|
| Login | 100 | 200 × 100 | 0 | 0 | 0 | 8 | 8 (8 %) |
| Especialidades | 100 | 200 × 100 | 0 | 0 | 0 | 2 | 2 (2 %) |
| Crear solicitud | 100 | 201 × 100 | 0 | 0 | 0 | 0 | 0 |
| Cancelar | 100 | 200 × 100 | 0 | 0 | 0 | 1 | 1 (1 %) |
| Detalle | 100 | 200 × 100 | 0 | 0 | 0 | 0 | 0 |

Se comprobaron las aserciones originales: las once muestras tienen respuesta y contenido válidos y fallaron exclusivamente Duration Assertion. No hay timeouts ni cuerpos sin recibir que conciliar. Las categorías podrían solaparse en otra corrida; aquí no hubo otros errores.

| Hilo | Iteración | Paso que hizo fallar el flujo | Duración ms | Umbral ms |
|---|---:|---|---:|---:|
| 1 | 4 | Login | 3.508 | 2.000 |
| 2 | 3 | Especialidades | 3.241 | 300 |
| 3 | 1 | Cancelar | 3.594 | 1.500 provisional |
| 4 | 1 | Login | 3.450 | 2.000 |
| 5 | 1 | Login | 3.203 | 2.000 |
| 6 | 1 | Login | 2.952 | 2.000 |
| 7 | 1 | Login | 2.996 | 2.000 |
| 7 | 3 | Especialidades | 310 | 300 |
| 8 | 1 | Login | 2.465 | 2.000 |
| 9 | 1 | Login | 2.203 | 2.000 |
| 10 | 1 | Login | 2.164 | 2.000 |

No se repitieron POST ni se borraron estos fallos al completarse correctamente los pasos siguientes. Los otros 89 flujos cumplieron también duración.

### Tiempos por endpoint y cálculo

| Paso | Media ms | p95 ms | p99 ms | Máximo ms | Ventana del paso s | Solicitudes/s |
|---|---:|---:|---:|---:|---:|---:|
| Login | 810,34 | 2.465 | 3.450 | 3.508 | 11,559 | 8,651 |
| Especialidades | 132,92 | 190 | 310 | 3.241 | 11,439 | 8,742 |
| Crear solicitud | 315,98 | 584 | 705 | 710 | 11,457 | 8,728 |
| Cancelar | 294,03 | 564 | 690 | 3.594 | 11,463 | 8,724 |
| Detalle | 167,22 | 384 | 398 | 518 | 11,452 | 8,732 |
| **Conjunto** | **344,098** | **828** | **2.996** | **3.594** | **11,650** | **42,918** |

Percentiles por rango más cercano ceil(p×N), posiciones desde 1. Para cada endpoint N=100: p95/p99 son posiciones 95/99, por eso p99 puede excluir el máximo. Para N=500: posiciones 475/495. Mediana global 207 ms. Se conservaron todas las muestras y la rampa; todas son respuestas recibidas, no tiempos hasta timeout.

Ventana global **2026-11-11T04:48:10.320Z–04:48:21.970Z**, equivalente al 10 de noviembre a las 23:48:10.320–23:48:21.970 UTC−05:00. timeStamp es inicio; final=timeStamp+elapsed. Throughput global=500/11,650. Cada tasa por paso usa su propia ventana primera–última muestra; normalizada a la ventana global, cada endpoint aporta **8,584 solicitudes/s**. El resumen aproximado de consola no se usa como ventana HTTP.

### Distribución de inicios y solapamiento

Los veinte primeros logins abarcaron **4,652 s**, coherentes con rampa de cinco segundos y arranques aproximadamente cada 250 ms; retraso arranque de hilo→primer login de **2–116 ms**. No se exige separación milimétrica exacta ni cinco segundos entre primero y último.

| Segundo relativo al primer HTTP | Inicios de todos los endpoints |
|---|---:|
| [0,1) | 34 |
| [1,2) | 4 |
| [2,3) | 4 |
| [3,4) | 6 |
| [4,5) | 52 |
| [5,6) | 53 |
| [6,7) | 66 |
| [7,8) | 44 |
| [8,9) | 56 |
| [9,10) | 64 |
| [10,11) | 60 |
| [11,12) | 57 |

Máximo allThreads observado: **20**, frente a uno anterior; por tanto no persiste el supuesto de trabajo globalmente serializado. Cada hilo sí conserva el orden de sus cinco pasos y sus cinco loops.

Se calculó por barrido de eventos el máximo de **muestras HTTP en curso según JMeter: 20**, alcanzado en epoch ms **1794372494972**. Intervalos semiabiertos [timeStamp,timeStamp+elapsed): cuando coinciden, se procesan finales antes que inicios, sin contar intervalos de duración cero. Aplicado al JTL anterior, el máximo fue uno. **Esto no demuestra ejecución simultánea de veinte solicitudes en el servidor**: puede incluir transporte o esperas de atención. Ambos cálculos están en solapamiento.json.

### Integridad de las cien marcas y efectos auxiliares

Se conciliaron los cien recursos individualmente, incluidas las once operaciones lentas, sin nuevas solicitudes HTTP ni cambios de datos:

- Cien citas distintas, exactamente una por marca; propietario, especialidad, dirección/coordenadas y estado CANCELADA correctos. Los cien IDs recibidos coinciden con la base.
- Cien historiales únicos PENDIENTE→CANCELADA, actor del dueño y motivo igual a la marca de hilo/iteración; cero duplicados o vínculos incorrectos.
- Cien pagos PENDIENTE con cotización 80.000 COP, sin monto final, método, pago, referencia o transacción externa y sin comprobantes: **no hubo cobros**.
- Cien notificaciones SOLICITUD_RECIBIDA y cien CITA_CANCELADA al usuario esperado, no leídas y sin otras notificaciones de la cohorte. Se verifican aparte de la transacción principal y después de los GET: no se supone que deban estar presentes antes del detalle.
- Cero cambios en las huellas de usuarios, clientes o citas preexistentes, incluidas las cincuenta de la corrida anterior. No se hizo limpieza automática.

| Cantidad | Antes | Después | Diferencia |
|---|---:|---:|---:|
| Usuarios | 5.131 | 5.131 | 0 |
| Clientes | 5.080 | 5.080 | 0 |
| Citas | 390 | 490 | +100 |
| Historial | 50 | 150 | +100 |
| Pagos | 390 | 490 | +100 |
| Notificaciones | 3.100 | 3.300 | +200 |
| Logs | 101 | 301 | +200 |

La consulta de conciliación ya usa los aliases JSON corregidos en la fase anterior; bastó una lectura de cohorte posterior. Los efectos auxiliares se observaron en esa lectura de solo lectura, fuera del JTL. No existieron timeouts que ocultaran IDs o justificaran reintentos.

### Recursos y cierre

El recolector corregido mantuvo señal de disponibilidad separada y validó el CSV solo después del cierre. CSV válido: **22 filas, once capturas por servicio**; tres anteriores, seis durante HTTP y dos posteriores. Cadencia global real **1,988–2,096 s**, media **2,0256 s**.

| Servicio | Capturas HTTP | Máximo CPU muestreado | Máxima memoria HTTP | Límites efectivos |
|---|---:|---:|---:|---|
| backend-perf | 6 | 206,45 % | 497,5 MiB | 2 CPU, 1 GiB |
| db-perf | 6 | 41,34 % | 176,3 MiB | Sin cuota CPU explícita, 1 GiB |

Antes: máximos backend 0,25 %/494,5 MiB y base 0,02 %/147,5 MiB. Después: backend 45,20 %/496,8 MiB y base 9,00 %/177,2 MiB. Se asignan fases por timestamp de captura y no se mezclan con HTTP. Docker expresa cien por ciento por CPU lógica. Seis capturas son escasas y sus máximos no son picos absolutos; memoria de contenedor no es heap Java. No se midió el porcentaje de CPU de BCrypt ni se atribuyen automáticamente las demoras a CPU o SQL.

Dashboard y JTL generados; solo los quinientos HTTP medidos, cien marcas y veinte cuentas, cinco loops por hilo. Cero variables temporales restantes, sin errores de recolector. La revisión del ejecutor cubrió 147 archivos sin exposiciones; se revisaron adicionalmente los artefactos de análisis. Perf quedó iniciado y saludable, sin reinicios ni OOM observados y sin docker stats activos.

### Evidencias de la segunda corrida

Carpeta: `../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/`.

- [JTL](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/e4.jtl), [log](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/jmeter.log), [dashboard](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/reporte/index.html), [análisis](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/analisis.json).
- [Flujos](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/flujos.json), [fallos por hilo/iteración](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/fallos-por-flujo.csv), [distribución](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/distribucion.csv), [solapamiento](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/solapamiento.json).
- [Conciliación individual](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/reconciliacion.json), [integridad por flujo](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/integridad-por-flujo.csv), [antes](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/antes.json), [después](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/despues.json).
- [Configuración efectiva](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/ejecucion.json), [aislamiento](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/aislamiento.json), [recursos](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/recursos.csv), [CSV validado](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/validacion-recursos.json), [estado final](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/estado-final.json).

El backend ya estaba utilizado y no se reinició. La mayor concurrencia observada y las diferencias de latencia no se pueden atribuir exclusivamente a duplicar usuarios: también cambió la rampa de treinta a cinco segundos. Estas corridas no prueban arranque frío ni capacidad sostenida. Se mantienen misma máquina, acceso sin nginx y PostgreSQL en tmpfs con durabilidad relajada. El flujo termina en cancelación, sin reparación completa ni cobro. No se ejecutó E5 ni se repitió la carga al encontrar los once incumplimientos.

[Verificación final de la segunda corrida y preservación de evidencias anteriores](../resultados/e4-20261110-234758-5730db81d0964c3e81b13628d304bc36/verificacion-final.json).
