# Diagnóstico de E3: llegada agrupada y respuestas fuera de plazo

## Alcance y conclusión

Se inspeccionaron exclusivamente el código, las bibliotecas locales, los artefactos de `e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca` y los logs históricos aún disponibles de sus contenedores. No se enviaron solicitudes, ejecutaron pruebas, consultaron nuevas identidades, reiniciaron servicios ni cambiaron código, recursos, BCrypt, timeouts o umbrales. El informe fallido y sus evidencias se conservan.

Hay dos hechos distintos: el arranque del generador agrupó los primeros ocho POST, y el backend terminó veinte invocaciones del controlador casi juntas después de unos 5,5 s dentro del método. Los doce timeouts son reales desde el cliente, aunque todas las operaciones persistieron. La evidencia no permite asignar el tiempo de CPU a BCrypt ni identificar una única causa del retraso del servidor.

## 1. Reconstrucción temporal

El log de JMeter dice explícitamente `Sample TimeStamps are START times`; la propiedad local `sampleresult.timestamp.start=true` concuerda. Por tanto, el final es `timeStamp + elapsed`. Los tiempos del log de JMeter se interpretan como America/Bogota, UTC−05:00; coinciden con los epoch del JTL. Los logs Docker aportan UTC y los de la aplicación incluyen `-05:00`.

Origen de la tabla: primer inicio HTTP, **2026-11-10T23:37:21.102Z**, equivalente a **2026-11-10T18:37:21.102-05:00**. Todos los tiempos relativos están en segundos. El ordinal n identifica el hilo `E3 - Clientes 1-n`. `null` significa que no se recibió un ID de respuesta. Cada fila tiene exactamente un usuario y un cliente vinculados, con campos correctos según la reconciliación original.

| Ordinal | Arranque hilo | Inicio POST | Duración | Final HTTP | Resultado | ID respuesta / persistido |
|---|---:|---:|---:|---:|---|---|
| 1 | -3,823 | 0,000 | 10,086 | 10,086 | timeout | null / 5067 |
| 2 | -3,342 | 0,000 | 10,086 | 10,086 | timeout | null / 5062 |
| 3 | -2,848 | 0,000 | 10,086 | 10,086 | timeout | null / 5065 |
| 4 | -2,346 | 0,000 | 10,086 | 10,086 | timeout | null / 5070 |
| 5 | -1,850 | 0,000 | 10,086 | 10,086 | timeout | null / 5053 |
| 6 | -1,356 | 0,000 | 10,086 | 10,086 | timeout | null / 5054 |
| 7 | -0,862 | 0,000 | 10,086 | 10,086 | timeout | null / 5055 |
| 8 | -0,359 | 0,000 | 10,086 | 10,086 | timeout | null / 5058 |
| 9 | 0,141 | 0,147 | 10,004 | 10,151 | timeout | null / 5056 |
| 10 | 0,636 | 0,642 | 10,006 | 10,648 | timeout | null / 5071 |
| 11 | 1,135 | 1,140 | 10,005 | 11,145 | timeout | null / 5063 |
| 12 | 1,639 | 1,643 | 10,003 | 11,646 | timeout | null / 5066 |
| 13 | 2,142 | 2,148 | 9,974 | 12,122 | 201 | 5061 / 5061 |
| 14 | 2,627 | 2,630 | 9,512 | 12,142 | 201 | 5064 / 5064 |
| 15 | 3,131 | 3,135 | 9,012 | 12,147 | 201 | 5052 / 5052 |
| 16 | 3,629 | 3,633 | 8,508 | 12,141 | 201 | 5069 / 5069 |
| 17 | 4,124 | 4,129 | 8,018 | 12,147 | 201 | 5057 / 5057 |
| 18 | 4,628 | 4,632 | 7,516 | 12,148 | 201 | 5059 / 5059 |
| 19 | 5,129 | 5,150 | 6,998 | 12,148 | 201 | 5068 / 5068 |
| 20 | 5,624 | 5,627 | 6,521 | 12,148 | 201 | 5060 / 5060 |

La rampa efectiva del Thread Group fue 10 s: lo confirman el XML, `ejecucion.json` y el log `threads=20 ramp-up=10 delayedStart=false`. Los hilos arrancaron entre 23:37:17.279Z y 23:37:26.726Z: separación de **9,447 s**, aproximadamente 0,5 s entre hilos. No se debe exigir que la distancia primero–último sea exactamente 10 s para veinte arranques.

Sin embargo, los POST comenzaron entre 23:37:21.102Z y 23:37:26.729Z: solo **5,627 s** de separación. Los ocho primeros tienen el mismo timestamp de inicio. La espera entre arranque e inicio de muestra fue de 3.823, 3.342, 2.848, 2.346, 1.850, 1.356, 862 y 359 ms respectivamente; para los demás fue de 3–21 ms.

| Intervalo desde el primer POST | Inicios |
|---|---:|
| [0, 1) s | 10 |
| [1, 2) s | 2 |
| [2, 3) s | 2 |
| [3, 4) s | 2 |
| [4, 5) s | 2 |
| [5, 6) s | 2 |

El máximo calculado de **muestras HTTP en curso según JMeter** es veinte, desde el último inicio hasta el primer timeout. No demuestra ejecución simultánea de veinte solicitudes en el servidor, ni que los bytes salieran exactamente al inicio de cada muestra.

### Trabajo del generador

El ejecutor genera las identidades y comprueba colisiones antes de Java, una vez por corrida. El plan no genera datos nuevos ni consulta la base dentro de los hilos. Cada preprocesador obtiene su variable de entorno, crea un ObjectMapper, analiza la identidad y serializa el cuerpo una vez por solicitud. Es trabajo anterior al cronómetro HTTP; con un loop no se repite por iteraciones adicionales.

Los cuatro componentes Groovy tienen caché de compilación habilitada. El bytecode local de JMeter muestra una caché compartida de scripts compilados con `Cache.get` y una función de compilación en el primer acceso. No hay barreras, temporizadores, sleeps ni synchronized explícitos en el plan. El primer uso de Groovy/Jackson y la inicialización del sampler son explicaciones compatibles con que ocho hilos esperen y salgan juntos; faltan trazas de esos primeros segundos para adjudicar los 3,823 s a un componente concreto. Caché habilitada no significa compilación previa a los hilos.

El ObjectMapper por petición es trabajo evitable del generador y candidato a preparación previa. No está demostrado que sus creaciones expliquen por sí solas el agrupamiento. Postprocesadores y aserciones ocurren después del HTTP y no explican la llegada inicial; pueden retrasar el mensaje de fin del hilo respecto del final del JTL.

## 2. Camino del registro

1. Seguridad permite esta ruta; sin Authorization, el filtro JWT continúa sin autenticar ni consultar usuarios. Spring deserializa el DTO y aplica `@Valid` antes de invocar el método del controlador.
2. El controlador invoca el caso de uso `@Transactional`. Primero se consulta `findByCorreo`; después se llama **una sola vez** al puerto de cifrado, que delega una sola vez en `PasswordEncoder.encode`.
3. `SecurityConfig` crea `new BCryptPasswordEncoder()`. El constructor de la biblioteca local Spring Security Crypto 7.1.1 resuelve su valor predeterminado a **strength 10**, versión 2a. No hay configuración de costo alternativa en este camino. Esto confirma el costo configurado en el código inspeccionado; los artefactos no conservan hashes completos ni una lectura histórica del bean en ejecución. No hubo medición del porcentaje de CPU de BCrypt.
4. Se guarda usuario y luego cliente. Los adaptadores convierten campos entre dominio y entidades; no vuelven a cifrar, recorrer listas ni emitir llamadas externas. Cliente reconstruye una referencia UsuarioEntity con el ID recién guardado, sin cascade configurado. No se encontraron listeners JPA o triggers definidos en las migraciones para estas tablas.
5. Ambas entidades usan `GenerationType.IDENTITY`, coherente con BIGSERIAL. Obtener los IDs requiere inserciones; no hay un lote de registros en esta ruta. Correo y documento tienen restricciones únicas y cliente.usuario_id es único y FK. Son posibles puntos de espera en general, pero las claves eran distintas y no hay evidencia de conflictos o bloqueos de índices en esta corrida. No hay N+1 por colección: se espera una búsqueda y dos inserciones por registro; sin traza SQL no se certifica el número exacto de sentencias ejecutadas por Hibernate.
6. La consulta inicial necesariamente adquiere una conexión antes de BCrypt. Todo el cifrado sucede dentro de la transacción. El instante exacto de adquisición puede ser anterior según JpaTransactionManager/Hibernate; no se instrumentó. Con el manejo transaccional habitual, la conexión adquirida permanece asociada hasta terminar la transacción, de modo que trabajo CPU puede retener conexiones. No se demuestra cuánto tiempo de retención hubo aquí.
7. Al retornar el caso de uso, el interceptor transaccional confirma antes de devolver al controlador. El controlador construye un UsuarioResponse de campos simples y ApiResponse; foto nula se traduce a null sin I/O. Jackson serializa el DTO después de retornar el método del controlador; no serializa entidades ni relaciones lazy.

La medición perf intercepta solo controladores y evita avisos duplicados mediante atributo de petición y ThreadLocal. Su duración incluye el caso de uso y la confirmación transaccional, pero excluye deserialización/validación previas y serialización/escritura HTTP posteriores. No desglosa BCrypt, consulta, inserciones y commit. No hay evidencia de avisos repetidos por múltiples capas en esta corrida.

## 3. Correlación de logs y recursos

Se consultó el intervalo **2026-11-10T23:36:55Z–23:38:15Z**, incluyendo preparación y periodo posterior. Había 55 líneas de backend y cero líneas de PostgreSQL en ese intervalo. No se encontraron ERROR, avisos de agotamiento/timeout de Hikari, starvation, SQLState, OOM o errores de conexión en la selección examinada. Cero líneas de PostgreSQL no demuestra consultas rápidas: no había trazas históricas de cada consulta o bloqueo disponibles en estos artefactos.

- **23:37:19.279Z**, antes del primer POST: pool total 30, idle 30, active 0, waiting 0.
- **23:37:27.166982522Z**, durante HTTP: `Connection not added`, total 30, idle 13, active 17, waiting 2.
- **23:37:27.168417153Z**, y otros cinco registros inmediatamente posteriores: total 30, idle 11, active 19, waiting 0. Son siete registros contando el anterior de waiting=2. Son instantáneas de contadores concurrentes, no una serie continua: demuestran espera puntual, no saturación sostenida ni duración de espera.
- **23:37:32.430–32.433Z**, según el timestamp interno de la aplicación: veinte avisos de `AuthController.registrarCliente`, duraciones **5.504–5.516 ms**. Restar duración al timestamp sitúa aproximadamente la entrada de los métodos entre **23:37:26.915Z y 26.928Z**. Es una estimación con precisión de milisegundos, no una traza de entrada ni asociación por ordinal. Docker recibió algunas líneas hasta 32.520Z.
- Los doce timeouts del JTL terminaron entre **23:37:31.188Z y 32.748Z**; las ocho HTTP 201, entre **33.224Z y 33.250Z**.
- **23:37:49.230Z**, después de HTTP: total 30, idle 30, active 0, waiting 0.

Las entradas estimadas del controlador están agrupadas aunque los POST se iniciaron durante 5,627 s. Es compatible con trabajo o espera común antes del controlador, por ejemplo inicialización de deserialización/validación; no identifica cuál. Tampoco permite vincular cada aviso con una identidad. La diferencia entre final del controlador y fin HTTP puede incluir serialización, escritura, transporte y trabajo del cliente; no se asigna íntegramente a ninguna fase.

| Captura UTC, 2026-11-10 | Fase | CPU backend | CPU PostgreSQL |
|---|---|---:|---:|
| 23:37:21.469Z | HTTP | 69,94 % | 0,00 % |
| 23:37:23.467Z | HTTP | 137,48 % | 0,00 % |
| 23:37:25.490Z | HTTP | 194,49 % | 0,00 % |
| 23:37:27.530Z | HTTP | 175,44 % | 0,34 % |
| 23:37:29.582Z | HTTP | 202,87 % | 8,34 % |
| 23:37:31.598Z | HTTP | 200,28 % | 40,42 % |
| 23:37:33.642Z | Posterior | 166,83 % | 0,03 % |
| 23:37:35.840Z | Posterior | 106,27 % | 5,79 % |

Límites: backend 2 CPU y 1 GiB; PostgreSQL 1 GiB sin cuota CPU explícita. Docker expresa 100 % por CPU lógica. El máximo muestreado durante HTTP es 202,87 %, con memoria máxima de contenedor 448,7 MiB; PostgreSQL alcanza 40,42 % y 153,5 MiB. Hay seis muestras por servicio durante HTTP, aproximadamente cada dos segundos. No son picos absolutos ni una medición del heap Java.

La CPU ya era elevada en capturas anteriores a la entrada estimada de los controladores. Por ello no debe atribuirse todo ese consumo al BCrypt ejecutado dentro del caso de uso. Inicialización de clases, compilación/JIT y otras fases son alternativas no medidas; JMeter además comparte máquina con Docker.

La inspección actual muestra ambos contenedores saludables, reinicios 0, OOMKilled=false y StartedAt anteriores a la corrida. No se observa evidencia de reinicio u OOM en ella. El cpu.stat actual sí tiene throttling acumulado, pero no existe una referencia anterior: **no puede adjudicarse a E3 ni calcularse su delta durante la prueba**. No hay registros históricos suficientes de GC, bloqueos, tiempos SQL, espera de pool o perfiles de pila para reconstruir esas fases; el estado actual no los sustituye.

## 4. Respuestas y persistencia

La reconciliación original confirma los ocho pares ID respuesta/usuario persistido de la tabla y los veinte perfiles correctos. Los ordinales 1–12 agotaron la espera pero quedaron persistidos, sin duplicados ni huérfanos. Antes/después: usuarios 5.051→5.071, clientes 5.000→5.020, notificaciones 3.000→3.000. Se usan los artefactos existentes; no se hizo otra operación de registro.

No hay timestamp de commit por identidad. `creado_en` se asigna en la aplicación antes de guardar, después del cifrado: no acredita el momento de commit. Los avisos agregados son compatibles con que algunas transacciones terminen después del timeout del cliente, pero no permiten decir cuáles ni cuándo se confirmó cada una. Un timeout no canceló de manera demostrada el trabajo del servidor; las filas posteriores prueban su persistencia final.

| Métrica de elapsed | 8 HTTP 201 válidos | 12 timeouts |
|---|---:|---:|
| Media | 8.257,375 ms | 10.058,833 ms |
| Mediana | 8.263 ms | 10.086 ms |
| Mínimo | 6.521 ms | 10.003 ms |
| p90 / p95 / p99 | 9.974 / 9.974 / 9.974 ms | 10.086 / 10.086 / 10.086 ms |
| Máximo | 9.974 ms | 10.086 ms |

Percentiles por rango más cercano, ceil(p × N); mediana por promedio de posiciones centrales. Para los timeouts se mide **tiempo hasta abandonar la espera**, no latencia final del servidor. Por tanto, los percentiles globales originales de 10.086 ms no representan los tiempos finales de las veinte operaciones del servidor. Las ocho respuestas recibidas son válidas pero incumplen 1.500 ms. Las doce restantes tienen `SocketTimeoutException: Read timed out`, no un error HTTP recibido; sus cuerpos no son evaluables. Las veinte duraciones exceden el umbral, solapándose con los doce fallos de transporte.

## 5. Hipótesis, respaldo y datos faltantes

| Prioridad | Hipótesis o hecho | Respaldo | Dato que falta |
|---|---|---|---|
| 1 | Arranque del generador agrupa llegadas | Demostrado: ocho inicios iguales, espera previa decreciente y rampa de hilos correcta | Trazas de pila/tiempos de compilación e inicialización para localizar el componente |
| 2 | Inicialización o espera compartida antes del controlador amplifica la concentración | Entradas estimadas de veinte métodos en unos 13 ms, CPU alta antes de ellas | Tiempos de entrada HTTP, deserialización y validación; JFR/clases cargadas y monitores |
| 3 | Costo CPU dentro del registro, incluido BCrypt, contribuye a respuestas lentas | Una codificación strength 10 por registro; métodos de unos 5,5 s y CPU cercana a 2 CPU en parte de la ventana | Perfil CPU/pilas y desglose de encode frente a JPA/commit; no basta la coincidencia |
| 4 | Retención de conexión durante BCrypt o espera transitoria de pool contribuye | Consulta antes de encode dentro de transacción; un registro waiting=2 | Histograma de adquisición/retención y serie durante la ventana; no hay evidencia de agotamiento sostenido |
| 5 | SQL lento, locks, GC, throttling o serialización explican parte del retraso | Posibles, sin prueba histórica suficiente; no hubo errores SQL registrados | Tiempos SQL/locks, pausas GC, delta cgroup y trazas de serialización |

No se confirma una causa exclusiva. Los 1.500 ms son un criterio provisional incumplido, pero no explican los doce timeouts de lectura a unos diez segundos. Tampoco el antiguo defecto del encabezado JTL explica las demoras: ocurrió después de medir.

## 6. Correcciones propuestas y experimento mínimo siguiente

**Generador, propuesta sin aplicar:** preparar cuerpos serializados fuera del grupo medido y precalentar o eliminar el primer uso de Groovy/Jackson antes de la rampa, sin enviar HTTP ni escribir secretos. Conservar una identidad por solicitud, la rampa, timeouts y umbral. Añadir diagnóstico sin secretos de arranque de hilo/preprocesador/inicio de sampler para comprobar la llegada real. El objetivo es evitar el agrupamiento observado; no ocultar latencias recortando muestras. El validador de encabezado ya corregido se conserva y no se vuelve a ejecutar carga para validarlo.

**Backend, sin cambio propuesto todavía:** no hay evidencia para cambiar BCrypt, límites, pool o SQL. Sacar CPU fuera de una transacción podría reducir retención de conexiones, pero altera el diseño transaccional y requiere revisar semántica y una medición que justifique el cambio; no se implementó.

**Experimento mínimo sujeto a autorización:** primero validar offline el arranque del generador corregido con un destino local simulado sin escrituras de negocio, incluyendo compilación y serialización previas. Después, una sola nueva corrida de veinte identidades nuevas, misma rampa y límites, con captura de JFR/pilas y tiempos por fase sin cuerpos, contraseñas ni hashes: entrada HTTP, antes/después del controlador, BCrypt, adquisición de conexión y SQL/commit. Medir deltas de cpu.stat antes/después y mantener recursos con timestamps. No aumentar timeouts ni umbral. No ejecutar cuarenta hasta reconciliar la cohorte y cumplir la condición de continuidad. Ninguna de estas pruebas se ejecutó en este diagnóstico.

## Evidencias y código

- [JTL original](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/e3.jtl), [log JMeter](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/jmeter.log), [propiedades efectivas](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/ejecucion.json), [reconciliación](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/reconciliacion.csv), [recursos originales](../resultados/e3-20261110-183705-a552e41dd0f5477c86b21529f2a5b5ca/recursos.csv) e [informe conservado](INFORME-E3.md).
- Evidencias derivadas sin credenciales: [tabla temporal](../resultados/diagnostico-e3-a552e41dd0f5477c86b21529f2a5b5ca/tabla-temporal.csv), [logs seleccionados](../resultados/diagnostico-e3-a552e41dd0f5477c86b21529f2a5b5ca/logs-seleccionados.json), [pool durante HTTP](../resultados/diagnostico-e3-a552e41dd0f5477c86b21529f2a5b5ca/hikari-durante.json), [estado actual acumulado](../resultados/diagnostico-e3-a552e41dd0f5477c86b21529f2a5b5ca/estado-actual.json), [bytecode JSR223](../resultados/diagnostico-e3-a552e41dd0f5477c86b21529f2a5b5ca/jsr223-bytecode.txt) y [bytecode BCrypt](../resultados/diagnostico-e3-a552e41dd0f5477c86b21529f2a5b5ca/bcrypt-bytecode.txt).
- Generador: [plan E3](e3-registro.jmx), [ejecutor E3](ejecutar-e3.ps1).
- Código: `src/main/java/com/appointech/appointech_backend/`: `infrastructure/controllers/AuthController.java`, `FotoUrlMapper.java`; `application/usecases/RegistrarClienteUseCaseImpl.java`; `infrastructure/config/SecurityConfig.java`; `infrastructure/security/JwtAuthenticationFilter.java`; `infrastructure/adapters/BCryptPasswordEncoderAdapter.java`, `UsuarioRepositoryAdapter.java`, `ClienteRepositoryAdapter.java`; `infrastructure/entities/UsuarioEntity.java`, `ClienteEntity.java`; `infrastructure/dto/ApiResponse.java`; `infrastructure/perf/MedidorTiemposInterceptor.java`, `PuntoCorteCapasAplicacion.java`.
- Configuración y esquema: `src/main/resources/application-perf.properties`, `application.properties`, `db/migration/V1__esquema_inicial.sql`, `V6__permitir_login_google.sql`; `perf/docker-compose.perf.yml`. No se modificaron.

Se verificaron cálculos y coherencia entre log/JTL/reconciliación mediante lectura y análisis local. No se ejecutaron pruebas funcionales ni de carga. Hexagonal y SOLID se respetaron al no modificar capas ni puertos; configuración y secretos permanecieron fuera del diagnóstico. Perf sigue iniciado y saludable, sin commit, push o cambio de rama.
