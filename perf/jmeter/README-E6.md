# E6 — Sobrecarga de consultas REST

Variante nueva en archivos separados: [README-E6-SOSTENIDO.md](README-E6-SOSTENIDO.md). Está creada para fase 100 con 20 s de rampa y 120 s sostenidos, pero su validación previa quedó bloqueada por proceso activo. No se ejecutó carga real de esa variante. Este documento y `e6-lecturas.jmx` siguen siendo la referencia de diez ciclos.

**Estado actual: bucle interior corregido y validado con samplers ficticios, sin HTTP al backend.** La última corrida real sigue siendo la parcial de 500/5.000 GET y no constituye una fase E6 válida. No se ejecutó otra carga real. Los intentos y resultados E1–E6 se conservan. El ZIP original corresponde a la versión inicial; usar los archivos corregidos del repositorio. Véase [INFORME-E6.md](INFORME-E6.md).

Antecedentes: el defecto Jackson se corrigió, el PID 40980 desapareció y un diagnóstico posterior pasó. La corrida real con setUp válido emitió solo un ciclo por hilo y fue rechazada por el ejecutor. La corrección del bucle descrita aquí es posterior y todavía no se ha usado contra AppoinTech. 100 sigue sin una fase completa válida; 200 y 300 permanecen pendientes.

Diagnóstico sin HTTP y manejo de ErrorRecord: [DIAGNOSTICO-PREPARACION-E6.md](DIAGNOSTICO-PREPARACION-E6.md). El modo `-DiagnosticoSinHttp` comprueba dependencias y SQL de lectura y ejecuta solo `java -version`; nunca autentica ni inicia JMeter. Sus bloqueos históricos por Docker stats se conservan en ese diagnóstico. `diagnosticar-e6-local.ps1`, `errores-e6.ps1` y `procesos-e6.ps1` son los auxiliares correspondientes.

## Corrección confirmada del bucle y prueba sin HTTP

El `LoopController` **interior** tenía `LoopController.continue_forever=false`. En JMeter 5.6.3, al terminar sus cinco repeticiones esa opción hace que se marque `done`; el padre lo elimina de su lista. Los siguientes ciclos exteriores ya no tenían samplers. La propiedad `-Jloops=10` sí llegaba al controlador principal: el defecto no era una propiedad omitida ni una parada del backend.

Se cambió **solo** el `continue_forever` interior a `true`. Conserva `loops=5`, devuelve el control al padre al terminar cada bloque y permite reutilizarlo en el siguiente ciclo. No hace infinito el plan: el controlador principal conserva `continue_forever=false` y diez ciclos. El XML completo se comparó y no tiene otros cambios funcionales.

`validar-bucles-e6.ps1` copia temporalmente el plan, mantiene ambos controladores y sustituye el HTTP por un sampler JSR223 ficticio local. Retira setUp, tearDown y validadores REST de esa copia, sin leer credenciales ni tocar servicios. Verifica que no queden samplers HTTP y que los controladores no cambien al construirla. Usa rampa **0 solo para agilizar la prueba local**, mediante propiedad; el plan de backend conserva **20 s**. Los JTL ficticios quedan exclusivamente en `perf/diagnosticos/e6-bucles-*`, nunca en resultados de AppoinTech.

| Versión | Hilos | Ciclos solicitados | Muestras | Notificaciones / historial / especialidades |
|---|---:|---:|---:|---:|
| Original | 2 | 3 | 10 de 30 | 4 / 4 / 2 |
| Original | 2 | 10 | 10 de 100 | 4 / 4 / 2 |
| Corregida | 2 | 3 | 30 de 30 | 12 / 12 / 6 |
| Corregida | 2 | 10 | 100 de 100 | 40 / 40 / 20 |

La prueba registra además el valor efectivo del controlador principal (3 y 10), los ciclos/pasos por hilo y código Java 0. Los casos corregidos pasaron sin fallos. Sus latencias son ejecución local ficticia y **no son resultados del backend**.

Comando reproducible **solo ficticio**, sin HTTP:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\validar-bucles-e6.ps1 -JMeterHome "$env:LOCALAPPDATA\Programs\JMeter" -Etiqueta corregido
```

Evidencia: [validación corregida](../diagnosticos/e6-bucles-corregido-20261111-150837-0af222db68ef4d2eafdf7d77a2142b58/validacion-bucles.json). El validador estático comprueba ahora el flag interior y el del controlador principal para impedir esta regresión.

## Mezcla elegida y fases

Cada hilo mantiene un CLIENTE sintético distinto. Hace diez ciclos de cinco GET en orden **notificaciones → historial → especialidades → notificaciones → historial**. No hay login en JMeter, temporizadores, duración adicional ni listeners gráficos. Es carga cerrada: cada hilo espera su respuesta o timeout antes del siguiente GET.

| Lectura | Proporción | Umbral | Datos y coste esperado según código |
|---|---:|---:|---|
| `/api/notificaciones?page=0&size=20` | 40 % | 800 ms | Página de hasta veinte notificaciones propias; usuario por correo, consulta paginada y total. Las cuentas seleccionadas tienen 3–23, por lo que algunas sí ejercitan truncado de página. |
| `/api/cliente/citas` | 40 % | 800 ms | Historial propio completo, sin paginación; 1–11 citas por cuenta seleccionada. Por cada cita el caso de uso resuelve técnico, especialidad y eventualmente quién asignó. Aporta más accesos y mapeo que la página pequeña. |
| `/api/especialidades` | 20 % | 300 ms | Catálogo público de seis especialidades activas, sin paginación. Lectura pequeña sin token en la petición. |

Los 800 ms del historial proceden del umbral general existente de lecturas en `generar-planes.js`; no se presenta como un SLA específico ya definido para ese endpoint. Notificaciones 800 ms y especialidades 300 ms coinciden con los escenarios anteriores. No se ajustarán tras ver resultados.

| Fase | Usuarios configurados | Rampa | Ciclos por hilo | GET por hilo | GET previstos |
|---|---:|---:|---:|---:|---:|
| 100 | 100 | 20 s | 10 | 50 | 5.000 |
| 200 | 200 | 20 s | 10 | 50 | 10.000 |
| 300 | 300 | 20 s | 10 | 50 | 15.000 |

Diez ciclos aportan miles de GET por fase y proporciones exactas sin aleatoriedad. Cambia solo el número de usuarios. Timeouts explícitos e iguales: conexión 5.000 ms y respuesta 10.000 ms. No hay una duración fija: depende de las respuestas. Se conserva toda la rampa. Usuarios configurados no equivalen a hilos activos observados ni a solicitudes simultáneas.

### Estimación de concurrencia y propuesta sostenida — NO aplicada

Con la corrida parcial real: ciclo = `2×7,340 + 2×8,545 + 4,060 = 35,830 ms` HTTP. Diez ciclos sumarían **358,3 ms por hilo**. La ventana media desde primer inicio hasta último final por hilo fue 48,11 ms para un ciclo, incluyendo huecos; extrapolar diez da **481,1 ms**. No es una predicción bajo saturación ni una medición nueva.

Con incorporación uniforme `N/20` hilos/s, la aproximación `hilos activos medios ≈ (N/20) × duración del hilo` da:

| Usuarios | Solo tiempos HTTP | Incluyendo huecos extrapolados |
|---|---:|---:|
| 100 | 1,79 | 2,41 |
| 200 | 3,58 | 4,81 |
| 300 | 5,37 | 7,22 |

Diez ciclos corregidos no mantendrían una cohorte de 100–300 hilos activa durante un intervalo largo con esas latencias. La sobrecarga real, el coste del generador y nuevas latencias pueden cambiarlo, pero no conviene depender de una ralentización para sostener la demanda.

**Propuesta para autorización futura:** fases de 100/200/300, rampa común de 20 s y **120 s sostenidos después de la rampa**. Sustituir el límite de diez ciclos por repetición de ciclos completos hasta un plazo global `t0+140 s`, comprobado únicamente al inicio de cada ciclo de cinco GET. Los hilos no se agotan durante la rampa; cada ciclo iniciado se completa y cualquier finalización posterior al plazo se reporta aparte. No añadir pausas ni cambiar mezcla, tokens fuera del JTL, umbrales, timeouts o recursos.

Las muestras dejarían de ser 5.000/10.000/15.000 fijas: serían `M=5×suma(ciclos completados por hilo)`, con `0,4M/0,4M/0,2M` para el conjunto de ciclos completos. El número depende de respuesta y coste del generador; no se promete un throughput a partir de las latencias actuales. El analizador tendría que aceptar ciclos variables, exigir secuencia completa por hilo y comprobar cobertura temporal y participación durante el régimen sostenido. Mantendría todas las muestras, informando por separado rampa `[0,20)`, sostenido `[20,140)` y finalizaciones posteriores; los límites de ventana pueden cortar ciclos y variar ligeramente la mezcla dentro de cada ventana. El conjunto completo conserva la mezcla exacta.

Se evaluarían códigos, contenido, pertenencia, duración, fallos únicos, percentiles y throughput del conjunto y del régimen sostenido, con los mismos umbrales y criterio de menos del 1 %. Se informarían hilos activos y solapamiento HTTP observado, sin equipararlos a ejecución simultánea del servidor. Esta propuesta **no está implementada ni ejecutada**: el plan actual corregido conserva diez ciclos y scheduler desactivado.

## Inspección de datos y candidatos

La inspección de 2026-11-11 está en [INSPECCION-E6.json](INSPECCION-E6.json). No es una expectativa permanente: el ejecutor volverá a leer y validar la base antes de cada fase.

- 5.131 usuarios, 5.080 clientes, 50 técnicos, 490 citas, 3.300 notificaciones y seis especialidades activas.
- Hay 340 clientes sintéticos activos con perfil, citas y notificaciones. Se eligen los primeros N de esa intersección ordenados por sufijo numérico de `clienteN@perf.test`, no los primeros N correos suponiendo que tengan citas.
- La cohorte de 300 tiene 1–11 citas y 3–23 notificaciones por identidad. No se fuerza la antigua regla E2 de exactamente tres notificaciones/dos sin leer: E4 creó datos válidos adicionales. Las expectativas actuales salen de SQL antes de cada fase, sin crear ni alterar datos.
- `GET /api/admin/usuarios?page=0&size=20`: tiene veinte filas; existen 256 páginas completas. `page >= 0`, `size` 1–100, defecto 20; ambas se enviarán si se incorpora en otra versión. Rol ADMINISTRADOR, HTTP 200, arreglo raíz y `X-Total-Count`. Orden `id ASC`, `findAll(PageRequest...)` y consulta de total. El DTO real es `UsuarioAdminResponse`: `id,nombre,apellido,correo,telefono,tipoDocumento,numeroDocumento,rol,activo,creadoEn,fotoUrl`. Los JSONPath útiles son `$[*].id`, `$[*].rol` y `$[*].activo`, no `datos` ni `content`.
- **Admin se excluyó de esta mezcla**: no hay administradores sintéticos activos `@perf.test` en db-perf ni credenciales administrativas en `clientes.csv`. No se usan cuentas reales ni se crean usuarios para completar el diseño. Añadirlo requeriría una cuenta sintética autorizada y expectativas de páginas pobladas; una cuenta admin compartida limitaría la diversidad de identidad aunque los datos fueran de solo lectura. No es un bloqueo para ejecutar la mezcla CLIENTE elegida.

## Contratos y aserciones reales

Los tres GET devuelven **HTTP 200 y arreglo en la raíz**; no se exige `exito`, `datos` ni una envoltura de paginación.

**Notificaciones:** requiere usuario autenticado. Parámetros admitidos: `page >= 0`, `size` 1–100; `soloNoLeidas` opcional `true/false` sin distinguir mayúsculas (omitido aquí). Orden `creadaEn DESC, id DESC`. Campos del DTO: `id,tipo,titulo,mensaje,citaId,leida,creadaEn`. Se comparan página, orden, valores y tipos contra SQL de ese usuario; `X-Total-Count` debe coincidir con el total completo. IDs positivos y únicos; `leida` booleano; tipo perteneciente al enum real; cita nula o ID de la expectativa; fecha local ISO comparada semánticamente con `LocalDateTime`, tolerando distinta precisión textual equivalente. JSONPath: `$[*].id`, `$[*].leida`, `$[*].tipo`.

**Historial:** `GET /api/cliente/citas`, sin parámetros ni paginación, rol CLIENTE. Orden `creadaEn DESC,id DESC`. Se comparan todos los campos proyectados de `CitaResponse` contra SQL y los IDs vinculados al `cliente_id` de la identidad. Campos: `id,tecnicoId,especialidadId,descripcionProblema,direccionServicio,fechaHoraInicio,fechaHoraFin,estado,tecnicoNombre,tecnicoApellido,tecnicoTelefono,especialidadNombre,tecnicoFotoUrl,agendamientoManual,motivoSolicitudEspecial,asignadaManualmente,asignacionManual`. La respuesta cliente lleva `asignacionManual=null` incluso si la cita fue asignada manualmente; los otros indicadores se cotejan. Fechas pueden ser nulas según datos; IDs, nulos, cadenas, booleanos y estados se comparan con la expectativa tipada. JSONPath: `$[*].id`, `$[*].estado`, `$[*].especialidadId`. El cuerpo no expone `clienteId`: la pertenencia se demuestra por la asociación SQL y coincidencia exacta de IDs ordenados, no por un campo inventado.

**Especialidades:** público según `SecurityConfig`, sin parámetros. Campos `id,nombre,descripcion` (descripción puede ser nula). Se verifica conjunto completo activo por ID y campos, sin imponer un orden que el repositorio no garantiza. JSONPath `$[*].id`, `$[*].nombre`. Se quita Authorization antes de cada petición pública.

En todas: JSON válido sin contenido adicional, arreglo no vacío, IDs positivos/únicos, cantidad esperada, campos y tipos según SQL/DTO y Duration Assertion. Las respuestas válidas lentas conservan contenido válido y cuentan aparte de transporte/HTTP/contenido. Los mensajes de aserción no incluyen datos. Los códigos y detalles originales de transporte se conservan saneados. Categorías pueden solaparse; `success=false` cuenta muestras únicas. No se interpreta automáticamente un HTTP 200 fallido como solo duración.

## Preparación, seguridad y separación del JTL

1. Dependencias, XML, ausencia de otro JMeter/docker stats, aislamiento por etiquetas/red/JDBC/tmpfs/puerto y límites efectivos contra la referencia de inspección. Nunca usar `localhost:8080`. No se inicia, reinicia, detiene ni elimina un contenedor.
2. Capturar estado e integridad global de tablas antes de autenticar. Leer cohorte y expectativas bajo transacción SQL repetible de solo lectura. Abortará si faltan clientes, perfiles, datos o credenciales; no rellenará la base ni adaptará silenciosamente listas vacías.
3. Nuevo login secuencial de la primera identidad. Validar HTTP 200, `exito` booleano true y `datos.token` cadena no vacía. El servidor valida el token al consultar sus notificaciones y su historial CLIENTE, cotejando IDs con SQL; no se acredita identidad/rol solo decodificando JWT.
4. Exigir tres controles consecutivos de notificaciones correctas y <=800 ms, fuera del JTL, con salud/islamiento confirmados. Si no se obtiene respuesta normal en el plazo de comprobación, no iniciar carga. Es relevante después de E5 y su recuperación no inmediata.
5. Repetir login y las dos comprobaciones de pertenencia para las N−1 cuentas restantes; comprobar catálogo público. No reutilizar `tokens.csv`. Todas las solicitudes de preparación están fuera de JMeter y se registran solo ordinal, operación, tiempo y resultado. Preparación satisfactoria: N logins, 2N GET de pertenencia, tres controles normales y un GET público; si hay intentos adicionales, el CSV los conserva. La preparación puede calentar datos/cachés.
6. Por tamaño de expectativas se evita transmitir cientos de objetos mediante entorno Windows. Se escribe un único paquete temporal JSON sin contraseñas en `%TEMP%/appointech-e6-UUID/`, **fuera de Git y de resultados**, con ACL de herencia desactivada y acceso solo al SID del usuario actual. El argumento Java contiene únicamente la ruta, nunca tokens/contraseñas. Si la ACL falla, no inicia carga. No copiar ese paquete para diagnóstico.
7. El setUp JMeter lee el paquete una sola vez y lo elimina, conserva expectativas/tokens en memoria, y ejercita las mismas fuentes Groovy con respuestas ficticias para compilar/inicializar antes de la rampa, sin HTTP. No se presupone que caché=true evita la primera compilación. El ejecutor vuelve a eliminar el paquete en `finally`, aun si Java/preparación falla; el directorio temporal queda vacío y se retira. Una terminación forzosa del proceso anfitrión puede impedir un `finally`; es una limitación operativa, no una garantía de borrado ante caída del sistema.
8. Cada hilo toma `identidades[threadNum]` siempre. No hay SQL, login ni serialización de paquetes dentro de los GET medidos. El setUp y tearDown llaman `SampleResult.setIgnore()`, por lo que el recolector global `-l` no guarda esas muestras locales. El JTL y dashboard contienen exclusivamente los tres GET. La asociación ordinal/usuario/cliente se registra sin tokens en `cohorte.csv`.

Los flags JTL desactivan cuerpos de petición/respuesta incluso en error, cabeceras, subresultados y datos del sampler. Loggers wire/headers/protocol apagados. Se escanean archivos buscando contraseñas CSV y tokens antes de aprobar la ejecución. El CSV de credenciales existente no se copia. No se pasan secretos como argumentos ni variables de entorno temporales. Nunca pegar el paquete, credenciales o cuerpos en informes/logs.

## Comandos exactos — todavía NO ejecutados

Desde la raíz, Windows PowerShell 5.1, Java/JMeter 5.6.3 y Docker Compose. Perf debe estar iniciado por separado.

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e6.ps1 -Fase 100 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Tras revisar recuperación e integridad de 100, y solo cuando se autorice:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e6.ps1 -Fase 200 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Tras revisar 200, y solo cuando se autorice:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e6.ps1 -Fase 300 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Son comandos individuales; no pegarlos como secuencia automática. Cada invocación ejecuta solo su fase, no conoce automáticamente el resultado de las anteriores y nunca reintenta. Otra instalación se indica con `-JMeterBat "C:\ruta con espacios\JMeter\bin\jmeter.bat"`; es un ejemplo de sintaxis. Destino fijo perf y propiedades comunes fijas; no hay opción de bajar umbrales, cambiar loops o destino para aprobar.

## Integridad, recursos, recuperación y parada

`Huella-E6` usa un snapshot repetible de solo lectura: cantidad y MD5 agregado de hashes de filas completas ordenadas por ID de **usuario, cliente, tecnico, especialidad, cita, cita_historial_estado, pago y notificacion**. Solo persiste agregados, no mensajes ni valores personales. Incluye cambios de `leida`, campos y vínculos aunque no cambie la cantidad. Las huellas son una detección agregada de cambios, no una auditoría causal ni una garantía criptográfica. Una actividad externa concurrente puede causar diferencias y debe investigarse; no se atribuye automáticamente al GET.

Se reutiliza `recursos-perf.ps1` sin cambios: contenedores por IDs de Compose, disponibilidad mediante señal separada, cierre/espera en finally y lectura del CSV únicamente tras cerrar escritor. Se conservan CPU, memoria usada/límite/porcentaje, timestamp real y límites efectivos. Cadencia observada puede rondar dos segundos; memoria de contenedor no es heap Java, máximos muestreados no son picos absolutos.

Tras Java se intentan controles externos al JTL aun si falla el ejecutor/recolector: hasta tres consecutivos funcionales con ambos servicios saludables dentro de aproximadamente 60 s, separados por cinco segundos. Antes de cada uno se reafirma aislamiento. En recuperación un control lento válido no falla solo por duración; antes de cargar sí se exige <=800 ms. `recuperacion.csv` conserva todos los intentos fallidos y satisfactorios. No demuestra drenaje completo del servidor ni recursos de vuelta a línea base.

**Detener la progresión** ante preparación fallida, respuesta normal previa no acreditada, aislamiento dudoso, cambio de límites, reinicio/recreación, OOM, salud persistentemente mala, recuperación fallida, diferencia de integridad, JMeter/ejecutor/recolector fallido, JTL incompleto, dashboard ausente, mezcla incoherente, secreto expuesto, contenido inesperado o HTTP 401/403. Conservar evidencias y no repetir. No hay monitor de salud continuo: se acredita antes/después/durante controles y se comparan IDs/StartedAt/RestartCount/OOM; no se niegan episodios transitorios sin evidencia.

Los incumplimientos de duración solos y los 5xx/timeouts son hallazgos del estrés y no modifican umbrales. Solo puede plantearse la fase siguiente si pasan recuperación, integridad y el resto de criterios operativos. Código Java cero no significa todas las muestras aprobadas. La referencia de aceptación sigue siendo contenido correcto, umbral de cada tipo y menos del 1 % de muestras fallidas; el informe mostrará incumplimientos.

## Archivos y análisis

- `e6-lecturas.jmx`, `e6-lecturas.groovy`: plan y preparación/validación local.
- `e6-expectativas.sql`, `preparar-e6.ps1`: selección y expectativas desde esquema real, login externo, controles y paquete privado.
- `ejecutar-e6.ps1`: una fase, carpetas únicas, Java por ruta/JAVA_HOME/PATH, argumentos separados compatibles con espacios, recursos y limpieza.
- `analizar-e6.ps1`: reanálisis local sin HTTP. Valida columnas, rutas/etiquetas, N×50, mezcla exacta, pares ciclo/paso y umbrales; produce `analisis.json`, `por-endpoint.json`, `evolucion-10s.json`, `inicios.csv`, `primeros-inicios.csv`. Percentiles de rango más cercano y ventana HTTP conservando rampa, códigos, fallos de HTTP/transporte/contenido/duración, hilos y barrido de intervalos HTTP con finales antes de inicios al coincidir.
- `INSPECCION-E6.json`, `REVISION-E6.json`: datos agregados y evidencia de revisión, sin resultados de carga.
- `validar-e6.ps1`: reproduce parser XML/PowerShell y compilación Groovy sin ejecutar el plan ni HTTP. Comando local: `powershell.exe -NoProfile -File .\perf\jmeter\validar-e6.ps1 -JMeterHome "$env:LOCALAPPDATA\Programs\JMeter"`.

Cada futura corrida crea `perf/resultados/e6-fecha-UUID/fase-N/` con JTL, log, dashboard, configuración efectiva sin secretos, preparación, cohorte, controles, recursos, integridad, estados, privacidad y diagnóstico. Si falla, `parada.json` exige revisión; no se borran artefactos. El analizador puede abortar por JTL parcial conservando el original, no fabrica muestras faltantes. Los errores de recuperación se conservan incluso si los GET medidos terminaron.

## Evidencias de código y alcance confirmado

Rutas desde la raíz, bajo `src/main/java/com/appointech/appointech_backend/`:

- `infrastructure/controllers/NotificacionController.java`, `NotificacionMapper.java`, `infrastructure/dto/NotificacionResponse.java`, `application/usecases/ConsultarNotificacionesUseCaseImpl.java`, `infrastructure/adapters/NotificacionRepositoryAdapter.java`, `infrastructure/repositories/NotificacionJpaRepository.java`.
- `infrastructure/controllers/ClienteCitaController.java`, `CitaResponseMapper.java`, `FotoUrlMapper.java`, `infrastructure/dto/CitaResponse.java`, `application/usecases/ConsultarCitasDelClienteUseCaseImpl.java`, `ObtenerDetalleCitaUseCaseImpl.java`, `infrastructure/adapters/CitaRepositoryAdapter.java`, `infrastructure/repositories/CitaJpaRepository.java`.
- `infrastructure/controllers/AdminUsuarioController.java`, `infrastructure/dto/UsuarioAdminResponse.java`, `application/usecases/ListarUsuariosUseCaseImpl.java`, `infrastructure/adapters/UsuarioRepositoryAdapter.java`, `domain/models/PaginacionOpcional.java`.
- `infrastructure/controllers/EspecialidadController.java`, `infrastructure/dto/EspecialidadResponse.java`, `application/usecases/ListarEspecialidadesUseCaseImpl.java`, `infrastructure/adapters/EspecialidadRepositoryAdapter.java`, `infrastructure/config/SecurityConfig.java`.
- `infrastructure/controllers/AuthController.java`, `infrastructure/dto/AuthResponse.java`, `infrastructure/security/JwtAuthenticationFilter.java`, `domain/models/TipoNotificacion.java` y entidades JPA para las columnas SQL.
- `perf/docker-compose.perf.yml`, `perf/jmeter/preparar-e2.ps1`, `recursos-perf.ps1`, `generar-planes.js`, `e5-login.groovy` para aislamiento, patrón de preparación, captura y umbrales.

Confirmado leyendo código: métodos/rutas, autorización, DTO, ordenamientos, consultas derivadas y consultas adicionales potenciales del historial. Confirmado con SQL de solo lectura: tablas/columnas, cantidades, cohortes y páginas pobladas. No se midió número SQL efectivo por petición ni planes de ejecución; caché JPA y datos pueden variar el número real de accesos.

Pendiente de ejecución autorizada: salud y respuesta normal futura, logins/roles efectivos, transferencia con ACL y borrado real, calentamiento de scripts, validación HTTP, mezcla registrada, recursos, recuperación y estado inmutable. Compilar Groovy no ejecuta el plan ni demuestra el funcionamiento integral. Acceso directo sin nginx, datos pequeños por cliente, catálogo pequeño, base en tmpfs, posible concurrencia observada inferior a usuarios y ejecuciones cortas/secuenciales limitan las conclusiones. No se atribuirán demoras a una causa exclusiva sin mediciones por etapa. No se modificó negocio, recursos, BCrypt, DTO ni migraciones; sin commits, push o cambios de rama.
