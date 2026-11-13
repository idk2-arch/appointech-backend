# E7: inspección y diseño revisable de sobrecarga CRUD de franjas

Estado: **diseño, no implementado ni ejecutado**. Esta inspección no envió HTTP, no ejecutó JMeter y no modificó datos. Solo se inspeccionaron fuentes y se consultó la base aislada `appointech_perf` en una transacción `REPEATABLE READ READ ONLY`, cerrada con `ROLLBACK`. No se ejecutó ninguna migración ni se sembraron cuentas o franjas.

## 1. Evidencia y límites de lo confirmado

Inventario observado el **2026-11-11 a las 21:45:07 UTC**: [inventario de solo lectura](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/inspecciones/e7-diseno-20261111-164507/inventario-solo-lectura.json>). Se comprobó el aislamiento mediante el helper existente: proyecto `appointech-perf`, red exclusiva `appointech-perf_perf-net`, perfil `perf`, JDBC a `db-perf/appointech_perf`, tmpfs y puerto local 18080 [S15]. No se consultó la base de desarrollo. Las comprobaciones deberán repetirse antes de cualquier futura ejecución; este documento no autoriza escrituras.

| Identidad o conjunto observado | Rol | Cantidad activa | Uso posible en E7 |
|---|---|---:|---|
| `admin@appointech.com`, `usuario.id=1` | ADMINISTRADOR | 1 | Única cuenta administrativa existente observada; origen en V4, no cuenta `@perf.test` dedicada |
| `tecnico1@perf.test` … `tecnico50@perf.test` | TECNICO | 50 | Objetos técnicos de la carga; sus credenciales no autorizan este CRUD |
| `cliente1@perf.test` … `cliente5000@perf.test` | CLIENTE | 5.000 | No autorizan este CRUD |
| Administradores con correo `@perf.test` | ADMINISTRADOR | **0** | No inventar `admin1..150` ni asumir que existen |

Existen además 80 clientes fuera del conjunto sintético numerado anterior: 5.080 clientes totales, 50 técnicos y un administrador. Quedan fuera de E7. La semilla solo agrega 5.000 clientes y 50 técnicos [S13]; V4 define la cuenta administrativa [S14]. El CSV local `clientes.csv` tiene 5.000 filas de clientes; `tokens.csv` tiene 240 filas de tokens, cuyo rol/vigencia no se verificó y que no se reutilizarán como si fueran de administrador.

El administrador tiene un hash local no vacío. **No se probó su contraseña ni se obtuvo un JWT en esta inspección**; existencia y rol en SQL no equivalen a autenticación comprobada. Una ejecución futura necesitará una credencial válida de esa cuenta por un medio privado. No se crearán cuentas, cambiarán roles ni generarán JWT artificialmente para eludir esta condición [S16].

La propuesta usa **una identidad administrativa compartida** para 50, 100 y 150 hilos. No representa 150 administradores distintos ni 150 sesiones independientes. En una ejecución futura, un login previo y fuera de la medición podría entregar el mismo token a los hilos, manteniéndolo en memoria; `POST /api/auth/login` devuelve `datos.token`, no `data.token` [S23], [S24]. Se verificarían firma mediante el servidor, sujeto, rol, identidad y vigencia suficiente antes de iniciar CRUD; no se guardarán token, contraseña o cabeceras Authorization en artefactos. La API es stateless, pero los hilos comparten identidad y atribución de auditoría [S02], [S16], [S27], [S28]. Esta autenticación tampoco se realiza ahora.

## 2. Contrato confirmado en código

Base: `http://localhost:18080/api/admin/tecnicos/{tecnicoId}`. Las cuatro operaciones requieren `Authorization: Bearer <JWT>` y **ADMINISTRADOR** por `@PreAuthorize` a nivel del controlador [S01]. Sin autenticación válida: 401; con rol CLIENTE o TECNICO: 403. Hay pruebas HTTP existentes que respaldan permisos, cuerpos y códigos; se leyeron, no se ejecutaron [S17].

| Operación | Método y ruta relativa | Entrada | Éxito y contenido |
|---|---|---|---|
| Crear | `POST /franjas` | JSON completo abajo | **201**, objeto `FranjaTrabajoResponse` en la raíz |
| Consultar | `GET /franjas?desde=yyyy-MM-dd&hasta=yyyy-MM-dd` | Sin cuerpo; fechas inclusivas | **200**, array JSON de franjas, orden por fecha e inicio |
| Actualizar | `PUT /franjas/{franjaId}` | Mismo JSON completo, sin `id` en cuerpo | **200**, objeto raíz con el mismo `id` |
| Eliminar | `DELETE /franjas/{franjaId}` | Sin cuerpo | **204**, cuerpo vacío |

Referencias: rutas y respuestas [S01], DTO y mapeo [S03], [S04], [S25], consultas ordenadas y búsqueda conjunta por ID y técnico [S09]. **No existe GET individual de franja por ID en este controlador**: se consulta el día exacto y se filtra por el ID recibido; no se inventará `/franjas/{id}` como GET. `PUT` es el reemplazo de los tres campos editables, no PATCH. El PATCH de radio de atención y la configuración global están fuera de E7.

Ejemplo de creación, válido para la duración observada de 120 minutos; la fecha es ilustrativa y se asignará desde el manifiesto futuro:

```json
{"fecha":"2026-12-11","horaInicio":"08:00","horaFin":"10:00"}
```

Ejemplo de actualización del **mismo registro**, en el mismo día reservado:

```json
{"fecha":"2026-12-11","horaInicio":"09:00","horaFin":"11:00"}
```

La respuesta incluye `id`, `tecnicoId`, `fecha`, `horaInicio`, `horaFin`, `bloques`, `bloquesOcupados` y `creadaEn`. No lleva envoltura `datos` ni incluye `creadaPorId`. Con estos cuerpos se espera un bloque y cero ocupados. `id` se extraería con **`$.id`**, se conservaría como entero Long/cadena decimal sin redondearlo y se validaría positivo. No se obtendría de `MAX(id)`, del último elemento del array, de un ID de técnico ni de una secuencia supuesta. La base lo genera con BIGSERIAL/IDENTITY [S03], [S04], [S10], [S26].

### Validaciones y errores

- Fecha, hora inicial y final obligatorias. El mapper recorta espacios y usa `LocalDate.parse` y `LocalTime.parse`. El diseño enviará `yyyy-MM-dd` y `HH:mm`; el código usa parseo ISO, no una expresión regular que limite exclusivamente a cinco caracteres la hora [S03].
- Inicio estrictamente anterior al fin, en el mismo día: no se cruzará medianoche. Duración al menos igual a `duracion_bloque_minutos`. Fecha no pasada; si fuera hoy, inicio no anterior al reloj del backend. El diseño usará fechas futuras. La zona predeterminada del reloj es `America/Bogota` [S05], [S18].
- No se exige que la duración sea un múltiplo del bloque; el remanente se descarta al calcular bloques. Tampoco se impone a cada franja la jornada global 08–18: para estas reglas solo se usa su duración. El diseño se mantiene dentro de esa jornada de todas formas [S05], [S06].
- No puede cruzarse con otra franja del mismo técnico/fecha, salvo el propio ID al actualizar, ni con un bloque de indisponibilidad. Los intervalos contiguos no se cruzan [S05], [S06].
- PUT y DELETE no se permiten si la franja actual tiene citas solapadas en estados **CONFIRMADA, EN_CAMINO o EN_PROCESO**. No se cancelará ni reasignará una cita para limpiar la carga. La regla comprueba citas de la franja actual; el diseño comprobará también que los intervalos nuevos estén libres [S06].
- El técnico debe existir. PUT y DELETE buscan el par `franjaId + tecnicoId`, evitando actuar sobre una franja de otro técnico. No hay validación explícita de técnico activo en ese helper: E7 seleccionará solo técnicos sintéticos activos como condición del diseño [S07], [S09].
- Datos inválidos o filtro invertido/mal formado: **400**. Técnico o franja no encontrados: **404**. Cruce o citas que impiden editar/borrar: **409**. Estos errores de negocio usan `ProblemDetail` con `status`, `detail` e `instance`; no se asumirá ese mismo formato para todos los errores de transporte, Spring, proxy o seguridad [S11].
- GET sin fechas toma hoy a hoy+30 días; `DAYS.between(desde,hasta)` no puede superar 90. Esto limita el ancho de cada consulta, no la distancia de una fecha futura respecto a hoy. Todas las lecturas del ciclo llevarán `desde=hasta=fechaReservada` [S08].

## 3. Tablas, claves e integridad

Los catálogos de la base aislada confirmaron las restricciones e índices de `franja_tecnico`; el inventario guarda sus definiciones. No hay UNIQUE de `(tecnico_id,fecha,hora_inicio,hora_fin)` ni EXCLUDE de intervalos: **el único índice UNIQUE observado es el de la PK `id`**. El índice `(tecnico_id,fecha)` es no único. El control de cruces es del caso de uso, protegido por transacción y bloqueo pesimista de la fila `tecnico`, no por una unicidad natural en SQL [S07], [S10], [S12].

| Tabla | Claves, restricciones y relaciones relevantes | Comprobación prevista |
|---|---|---|
| `franja_tecnico` | PK `id`; NOT NULL técnico, fecha, horas y `creada_en`; CHECK inicio<fin; FK `tecnico_id → tecnico(id)` ON DELETE CASCADE; FK nullable `creada_por → usuario(id)` ON DELETE SET NULL | Mismo ID en POST/PUT/DELETE; autor correcto; nada preexistente alterado; cero residuos propios después de limpiar |
| `tecnico` | PK `id`; UNIQUE `usuario_id → usuario(id)` ON DELETE CASCADE; CHECK radio 1..100 | Los 50 técnicos y sus radios permanecen intactos |
| `usuario` | PK `id`; UNIQUE correo y número de documento según V1; rol y activo | Cuenta administrativa y técnicos sin cambios; no inferir `usuario.id = tecnico.id` |
| `cita` | PK `id`; FK a cliente, técnico y especialidad; tiempos/estado determinan ocupación; **no FK a `franja_tecnico`** | Comparación por técnico e intersección temporal, no por `franja_id`; filas y estados intactos |
| `bloque_indisponibilidad` | PK `id`; FK técnico CASCADE; CHECK fin>inicio | Sin intersección con reservas; filas preexistentes intactas |
| `tecnico_especialidad` | PK compuesta `(tecnico_id,especialidad_id)` y FKs | Relaciones intactas; no cambiar especialidades para la carga |
| `configuracion_franjas` | PK `id`; duración 15..480 y checks de jornada en V15; se usa la fila de mayor ID | Congelar duración/configuración y su huella durante cada fase |
| `log_sistema` | PK `id`, checks de nivel, índices; usuario como texto, sin FK al ID de la franja | Crecimiento esperado por auditoría; no usarlo como prueba exclusiva de commit |

Fuentes SQL: V1 [S12], V20 [S10], V15 [S19], V7 [S20]. `pago`, `calificacion`, `cita_historial_estado`, notificaciones y demás tablas de negocio tampoco deberían modificarse con este CRUD. Deben incluirse en la huella previa/posterior, no solo contar franjas.

Cada mutación de franja registra auditoría. El adaptador principal es asíncrono; el guardado usa `REQUIRES_NEW`, puede sobrevivir al rollback del CRUD y puede fallar dejando un aviso en stdout. Por eso ni un mensaje de auditoría demuestra que el POST haya hecho commit, ni la ausencia inmediata de un mensaje demuestra que no lo hizo [S21], [S29]. No se exigirán logs idénticos, ni se borrarán logs o reiniciarán secuencias. Los saltos de BIGSERIAL son normales incluso con rollback.

## 4. Generación sin colisiones accidentales

Se diseñará un manifiesto **local**, no una tabla nueva ni una columna de negocio. Un `runId` UUID identifica artefactos; **no se envía como ID de franja**. Cada operación tendrá la clave local `(runId, fase, hilo, ciclo, operación)`. El UUID no sustituye la reserva de fechas ni hace idempotente al servidor.

Antes de una ejecución autorizada, exclusión de corridas propias, aislamiento confirmado y una instantánea SQL de solo lectura cubrirán franjas, citas y bloqueos de los 50 técnicos. Se reservará un horizonte vacío para toda la corrida y se persistirá su manifiesto antes del primer POST. No basta con que hoy no haya una fila de igual hora: se comprobarán todos los cruces posibles de creación y edición, y ambos intervalos frente a citas/bloqueos. No habrá otras cargas de agenda o escrituras administrativas concurrentes en perf. Si no se puede asegurar esa exclusividad, no comenzar.

Se tomará `B` como una fecha futura de Bogotá al menos hoy+30 y posterior a todas las fechas/finales relevantes observados, con comprobación de vacío sobre todo el horizonte reservado. En otra corrida se elegirá otro horizonte, incluso si la anterior terminó con todas sus franjas borradas; se conservarán manifiestos para evitar reutilizar reservas con resultados inciertos. No se “liberará” un horizonte borrando datos previos.

Para hilos numerados `h=1..U`, ciclos `c=1..20`, y lista `T` de **IDs reales** de técnicos sintéticos, ordenada por ordinal del correo:

```text
tecnicoId = T[(h-1) mod 50]
lane      = floor((h-1)/50)
offset    = 0 para U=50; 20 para U=100; 60 para U=150
fecha     = B + offset + 20*lane + (c-1) días
```

Con ello, para un técnico, la fase 50 ocupa B..B+19; la de 100 B+20..B+59; la de 150 B+60..B+119. Dentro de un técnico no se repite fecha entre hilos, ciclos o fases. Técnicos distintos pueden usar la misma fecha sin conflicto. Cada GET es de un solo día, aunque el manifiesto abarque 120 días. **No se presupone que T sea 1..50**, aunque actualmente sus extremos sean 1 y 50: `tecnico1` tiene `usuarioId=5002`, distinto de su `tecnicoId=1`.

Con duración congelada `D` (actualmente 120), creación `08:00 → 08:00+D` y edición `09:00 → 09:00+D`, ambas con un bloque. Para el rango válido D≤480, ambas caben en el día. Una sola fecha por ciclo y técnico, libre antes de empezar, permite editar sin cruzar a otra fila; el solapamiento con la versión anterior del mismo ID es permitido por la aplicación. Se recalculará B antes de ejecutar; el ejemplo de diciembre no es una reserva ya efectuada.

## 5. Flujo nominal: 20 ciclos completos por hilo

Propuesta de arranque: preparación/autenticación fuera de la medición; arranque local de hilos durante 20 s y barrera antes del primer CRUD, con plazo acotado (propuesto: 60 s). Si no llegan todos o falla una preparación, liberar/abortar a todos **sin iniciar CRUD**. Una rampa de HTTP sin barrera podría permitir que hilos rápidos terminen sus 20 ciclos antes de arrancar los últimos, por lo que no demostraría la concurrencia solicitada. No se implementa ahora esa barrera.

Tras la barrera, cada hilo ejecutaría secuencialmente estos seis pasos en cada ciclo:

| Paso | Petición | Condición para continuar |
|---|---|---|
| 1 | POST de la franja propia | 201; JSON íntegro y consistente con técnico/fecha/horas; `bloques=1`, `bloquesOcupados=0`; capturar `$.id` |
| 2 | GET del día reservado | 200; una coincidencia exacta con el ID creado y cuerpo inicial; no elegir el primer elemento del array |
| 3 | PUT usando ese mismo `tecnicoId/franjaId` | 200; mismo ID y técnico, fecha reservada y horas nuevas; conservar `creadaEn`; autor y propiedad verificados con el manifiesto/SQL |
| 4 | GET del mismo día | 200; misma fila, versión editada, sin duplicados ni filas iniciales adicionales |
| 5 | DELETE del mismo `tecnicoId/franjaId` | 204 y cuerpo vacío; únicamente sobre ID propio confirmado |
| 6 | GET del mismo día | 200; ID propio ausente; con una reserva exclusiva y vacía de base, se espera array vacío |

Reiniciar variables de ID y estado al **comenzar** cada ciclo; nunca dejar que un extractor fallido conserve el ID del ciclo anterior. Registrar intención local antes de cada escritura e inmediatamente su resultado/ID después de responder. No avanzar un paso dependiente si la extracción, la identidad o la validación fallan.

| Hilos | Hilos por técnico | Ciclos nominales | POST | GET | PUT | DELETE | HTTP medidos nominales |
|---:|---:|---:|---:|---:|---:|---:|---:|
| 50 | 1 | 1.000 | 1.000 | 3.000 | 1.000 | 1.000 | 6.000 |
| 100 | 2 | 2.000 | 2.000 | 6.000 | 2.000 | 2.000 | 12.000 |
| 150 | 3 | 3.000 | 3.000 | 9.000 | 3.000 | 3.000 | 18.000 |

Son cantidades planificadas si no hay abortos, sin contar login, conciliación ni recuperación. Mezcla propuesta: GET 50% y cada escritura 16⅔%; no se hereda el 40/40/20 de E6. E7 está limitado por 20 ciclos, **no por 120 s sostenidos**. Se medirá su duración real y se propondrá un watchdog, sin confundir su vencimiento con que se completaron los ciclos.

El bloqueo `PESSIMISTIC_WRITE` del técnico serializa POST/PUT/DELETE dentro de la transacción [S07], [S22]. A 100 y 150 hilos hay dos y tres competidores por fila de técnico, aunque sus fechas sean diferentes. Por tanto, esta prueba evalúa sobrecarga CRUD sobre **50 agendas compartidas**, con contención intencional de filas; no 100/150 técnicos independientes. GET es read-only sin el mismo bloqueo explícito. Compartir la cuenta administrativa añade una identidad común de autenticación/auditoría, pero el bloqueo observado es del técnico, no del administrador. No se crearán técnicos adicionales para cambiar este escenario.

## 6. Errores, timeouts y resultados inciertos

El timeout del cliente **no prueba rollback ni cancela con certeza la transacción del servidor**. Se propone conservar los límites de transporte explícitos de perf (5 s conexión, 10 s respuesta) como configuración revisable, sin llamar a esos valores SLA de CRUD. No hay un presupuesto de latencia CRUD definido por estas clases; se informarán p95 y fallos semánticos por separado hasta que se acuerden umbrales de rendimiento.

Primera incertidumbre de escritura: marcar el ciclo y la fase como no conformes, cancelar el lanzamiento de nuevas operaciones de escritura, detener pasos dependientes y próximas fases; conservar las operaciones ya en vuelo y esperar su cierre acotado. No ejecutar ciegamente los 20 ciclos restantes. Las lecturas de conciliación y eventual limpieza de la futura ejecución irían fuera de la medición principal, con conteos separados.

| Resultado | Tratamiento diseñado |
|---|---|
| 201/200 con cuerpo inválido, truncado o ID no extraíble | Tratar como escritura posiblemente aplicada; no volver a POST ni reutilizar un ID anterior |
| POST con timeout, desconexión o 5xx | Buscar mediante GET del día y SQL read-only la tupla exacta reservada. Cero coincidencias no demuestra ausencia definitiva mientras pueda haber una transacción pendiente; no reintentar el POST automáticamente |
| Una coincidencia exacta tras POST incierto | Adoptar ID solo si no existía en la instantánea, pertenece al técnico/fecha/horas reservados y SQL confirma `creada_por=administradorId` y tiempo compatible; guardar evidencia. El HTTP original sigue siendo fallo, aunque se recupere el estado |
| Más de una coincidencia, propiedad no demostrada o falta de acceso para verificarla | No elegir la mayor ID ni borrar candidatos. Abortar, conservar evidencia y dejar residuo identificado como pendiente |
| PUT incierto | Consultar el ID ya conocido: versión nueva indica aplicación; antigua exige comprobar que no sigue pendiente; ausencia o estado distinto es anomalía. No enviar otra edición para “ver si funciona” |
| DELETE incierto | GET/SQL sobre el ID y la reserva. Si está ausente después del asentamiento, registrar borrado confirmado por lectura, sin transformar el timeout original en éxito de latencia. Si sigue presente, no repetir sin resolver primero la transacción y la propiedad |
| 400/401/403/404/409 inesperado | Fallo del ciclo, no éxito tolerado de sobrecarga. Detener dependencias y comprobar invariantes; no cambiar datos existentes para resolver el conflicto. Un 404 de un DELETE repetido no equivale al 204 nominal |
| GET con error/timeout | No implica escritura nueva, pero no autoriza avanzar sin verificar el estado del paso anterior. Solo lecturas acotadas para conciliación; contar aparte cualquier reintento |

La conciliación tendrá espera acotada (propuesta: 60 s, lecturas espaciadas y consultas SQL de actividad/bloqueos solo sobre perf). Sin correlación transaccional del servidor ni idempotency key persistente, varias lecturas vacías y el transcurso del tiempo **no bastan por sí solos** para probar que un POST no hará commit después. Si no puede establecerse un estado definitivo, permanecerá “incierto”, no se reutilizará esa reserva y no habrá borrado automático ni siguiente fase. No se implementa una garantía de exactly-once que el contrato no ofrece.

Solo después de resolver las escrituras en vuelo, una limpieza **de la ejecución futura autorizada** podrá operar sobre una lista blanca de IDs propios demostrados, verificando de nuevo técnico, reserva, autor y ausencia en la instantánea previa. No hay columna `runId` en la tabla: la lista blanca y el manifiesto son indispensables. No se permite DELETE por rango de fechas, `id > maxPrevio`, correo, técnico entero o `TRUNCATE`; tampoco borrar bloques/citas/franjas previas, cancelar reservas ajenas ni reiniciar la base. Una nueva incertidumbre de limpieza vuelve al estado pendiente. **Ninguna limpieza se realiza en esta inspección.**

## 7. Integridad, medición y condiciones para una implementación posterior

Antes y después de cada fase futura se compararán instantáneas canónicas por PK y hashes de filas, no solo conteos. Deben permanecer idénticos todos los registros preexistentes de franjas y las demás tablas de negocio: usuarios, técnicos, configuración, citas, bloqueos, relaciones, pagos, calificaciones, historiales y notificaciones. Antes de limpiar se verificará que cada ID propio tenga como máximo una fila y que no haya referencias huérfanas o cruces nuevos. Después, cero IDs/tuplas propios residuales y conjunto original de franjas idéntico. Un residuo incierto se informa y falla la integridad de cierre; no se borra sin prueba de propiedad. La auditoría y los avances de secuencia son las excepciones esperadas, documentadas por separado [S10], [S12], [S20], [S21].

Artefactos previstos, aún no implementados: manifiesto de reservas, diario de estados por operación, extracción de IDs, JTL de CRUD, conciliación/limpieza separada, SQL de integridad read-only, estados y límites de contenedores, recuperación y recursos. No guardar credenciales, JWT ni cuerpos completos con datos ajenos. Una verificación futura de permisos negativos estará fuera de la carga y requerirá su autorización; los 403 no se mezclarán con operaciones CRUD válidas.

Por fase y método/paso: esperadas/intentadas/completadas, ciclos completos/incompletos, códigos HTTP, validación de contenido y propiedad, p50/p95/p99, timeouts, hilos activos y solapamiento HTTP. Iniciadas/s se cuentan por inicio; respuestas HTTP de éxito y conformes/s, por finalización, con numeradores, denominadores y límites de ventana explícitos. “Conforme” distinguirá éxito HTTP+cuerpo de conformidad adicional con un eventual umbral acordado; no se copiarán sin acuerdo los umbrales 300/800 ms de lecturas E6.

CPU y memoria corresponden al contenedor/ventana compartidos, no a un endpoint concreto. Verificar pares backend/base, continuidad temporal, cierre del escritor, integridad del CSV y cobertura del drenaje/recuperación. No leer CSV abiertos con un acceso que impida escribir. El fallo del recolector de E6-300 sigue sin una causa propietaria identificada: antes de implementar una nueva carga debe revisarse esa instrumentación y validarse su ciclo de vida con un ejercicio ficticio sin HTTP, con prueba de cierre. **Ese ejercicio tampoco se ejecuta ahora.**

Las fases de 50, 100 y 150 serían ejecuciones separadas con puertas de revisión, no una progresión automática. Un error de contrato, credenciales/permisos, resultado incierto, integridad, salud o captura incompleta impide comenzar la siguiente. Autenticación, calentamiento, CRUD medido y recuperación deben informarse por separado; no atribuir una diferencia entre fases exclusivamente al número de hilos si cambia el estado térmico, cachés o preparación.

Pendiente de revisión antes de implementar: aceptar la cuenta administrativa compartida y el escenario de 50 agendas; disponer de su credencial válida; aprobar el flujo de seis pasos y la barrera; fijar presupuestos de duración/watchdog y resolver la captura de recursos. No se han creado planes JMX, samplers, ejecutores, scripts de escritura o datos E7.

## 8. Conservación de E1–E6

No se reejecutaron pruebas ni se modificaron JTL, datos de negocio, planes o resultados E1–E6. Se añade únicamente una [nota documental E6-300](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/NOTA-E6-300-RECURSOS-PARCIALES.md>). Su captura fue parcial: 59 pares completos y una fila extra de backend; faltan los últimos 25,443 s de backend y 27,527 s de base antes del final sostenido. No se atribuye el conflicto de archivo a un PID observado posteriormente. El calentamiento adicional antes de E6-200 impide una comparación causal aislada entre 100 y 200.

## Fuentes de código

Los enlaces se refieren al estado inspeccionado del workspace; un cambio de fuentes exige revisar el diseño.

- [S01]: controlador CRUD, rutas y rol.
- [S02]: seguridad stateless y filtro JWT.
- [S03]: parseo y respuesta.
- [S04]: DTOs y entidad con ID generado.
- [S05]: validación y división en bloques.
- [S06]: cruces, citas y ocupación.
- [S07]: transacciones, ownership y auditoría CRUD.
- [S08]: rango de consulta.
- [S09]: consultas y adaptación de repositorio.
- [S10]: DDL de franja.
- [S11]: códigos de error.
- [S12]: claves y relaciones iniciales.
- [S13]: semilla sintética perf.
- [S14]: administrador de V4.
- [S15]: aislamiento y consultas SQL de solo lectura existentes.
- [S16]: autenticación y contrato del login.
- [S17]: pruebas HTTP existentes, solo leídas.
- [S18]: reloj del backend.
- [S19]: constraints de configuración.
- [S20]: tabla de logs.
- [S21]: auditoría asíncrona y transacciones independientes.
- [S22]: bloqueo pesimista del técnico.
- [S23]: endpoint de login.
- [S24]: envoltura `datos` de las respuestas de autenticación.
- [S25]: cuerpo JSON de crear/editar franja.
- [S26]: entidad y estrategia de generación del ID.
- [S27]: filtro de autorización JWT.
- [S28]: sujeto, rol y expiración del JWT.
- [S29]: guardado de auditoría en transacción independiente.

[S01]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/controllers/AdminFranjaTrabajoController.java:30>
[S02]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/config/SecurityConfig.java:53>
[S03]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/controllers/FranjaTrabajoMapper.java:17>
[S04]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/dto/FranjaTrabajoResponse.java:6>
[S05]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/domain/models/FranjaTrabajo.java:28>
[S06]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/application/services/ReglasFranjasTrabajo.java:30>
[S07]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/application/usecases/AdministrarFranjasTrabajoUseCaseImpl.java:47>
[S08]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/application/usecases/ConsultarFranjasTrabajoUseCaseImpl.java:39>
[S09]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/adapters/FranjaTrabajoRepositoryAdapter.java:24>
[S10]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/resources/db/migration/V20__franjas_de_trabajo_y_radio_por_tecnico.sql:4>
[S11]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/config/GlobalExceptionHandler.java:66>
[S12]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/resources/db/migration/V1__esquema_inicial.sql:13>
[S13]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/resources/db/perf/R__datos_carga_perf.sql:16>
[S14]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/resources/db/migration/V4__primer_administrador.sql:1>
[S15]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/preparar-e2.ps1:9>
[S16]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/application/usecases/AutenticarUsuarioUseCaseImpl.java:39>
[S17]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/test/java/com/appointech/appointech_backend/infrastructure/controllers/AdminFranjaTrabajoControllerHttpTest.java:110>
[S18]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/config/AgendaConfig.java:17>
[S19]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/resources/db/migration/V15__reglas_franjas_validadas.sql:3>
[S20]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/resources/db/migration/V7__tabla_log_sistema.sql:1>
[S21]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/adapters/RegistroLogAsincronoAdapter.java:38>
[S22]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/repositories/TecnicoJpaRepository.java:33>
[S23]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/controllers/AuthController.java:86>
[S24]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/dto/ApiResponse.java:5>
[S25]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/dto/FranjaTrabajoRequest.java:4>
[S26]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/entities/FranjaTecnicoEntity.java:26>
[S27]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/security/JwtAuthenticationFilter.java:39>
[S28]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/adapters/JwtTokenGeneratorAdapter.java:30>
[S29]: </C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/adapters/RegistroLogAdapter.java:28>
