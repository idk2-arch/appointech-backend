# E8: preparación de resistencia continua de 50 usuarios

**Estado: diseño y preparación local; E8 no ejecutado.** En este turno no se envía HTTP, no se ejecuta JMeter —ni para carga ni para dashboard—, no se autentican usuarios, no se consulta el backend y no se modifica la base. Se entregan configuración, calendario teórico, inspección de vigencia por claims, prueba ficticia del recolector y comandos de preparación. No se presenta una captura futura como ya validada.

Configuración: [e8-resistencia.json](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/diseno/e8-resistencia.json>). Inspección local: [inspeccionar-e8.ps1](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/inspeccionar-e8.ps1>). El JMX/reloj de E8 y el analizador temporal descritos aquí deben implementarse y validarse sin HTTP antes de autorizar una ejecución real; **no existe todavía un lanzador E8 que pueda invocarse**. Los comandos de abajo distinguen lo disponible de lo pendiente. No sustituir esa implementación por ejecutar E6 con una duración distinta.

## 1. Elección de carga y fuentes revisadas

Los planes mixtos generados por [generar-planes.js](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/generar-planes.js:156>) combinan lecturas públicas, login, disponibilidad, crear/confirmar citas, notificaciones y administración. Las escrituras de solicitudes/citas consumen agenda, cambian estados, generan auditoría/notificaciones y pueden añadir 409 por competencia. Repetirlos diez minutos sin una cohorte y limpieza específicas confundiría resistencia con crecimiento del dataset. Además, la documentación histórica contiene valores distintos de las fuentes actuales: se toman los contratos y aserciones del código, no los ejemplos antiguos del README.

Se elige **lectura 40/40/20 de E6**, reutilizando los tres samplers/rutas, sus expectativas SQL y el validador de contenido y pertenencia [e6-sostenido.jmx](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/e6-sostenido.jmx>), [e6-lecturas.groovy](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/e6-lecturas.groovy:48>) y [e6-expectativas.sql](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/e6-expectativas.sql:1>):

| Endpoint | Peso | Éxito | Umbral existente | Autenticación |
|---|---:|---:|---:|---|
| GET `/api/notificaciones?page=0&size=20` | 40% | 200 | 800 ms | Token propio de cliente |
| GET `/api/cliente/citas` | 40% | 200 | 800 ms | CLIENTE y pertenencia |
| GET `/api/especialidades` | 20% | 200 | 300 ms | Público |

Notificaciones e historial deben conservar cantidad, IDs, orden y campos esperados; también `X-Total-Count` en notificaciones, tipos y estados válidos. El catálogo debe coincidir con SQL. No basta con HTTP 200. Las consultas son read-only: [ConsultarNotificacionesUseCaseImpl](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/application/usecases/ConsultarNotificacionesUseCaseImpl.java:27>) y [ConsultarCitasDelClienteUseCaseImpl](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/application/usecases/ConsultarCitasDelClienteUseCaseImpl.java:45>). No se invocan los PATCH de marcar leído ni los POST de citas.

Quedan fuera: CRUD, reserva/cancelación/reprogramación, bloqueos de escritura, colisiones de agenda, disponibilidad/rutas externas, pago, administración y autenticación sostenida. Tampoco se representan búsquedas de alta cardinalidad: se repiten páginas y datos estables. Es una resistencia controlada de lecturas con cachés calientes, **no una prueba mixta ni de saturación máxima**. Diez minutos permiten observar deriva durante esta ventana, pero no certificar ausencia de fugas a largo plazo.

## 2. Usuarios, reloj, calentamiento y parada

50 clientes sintéticos distintos, `cliente1..50@perf.test`, con identidad y token estables por hilo. No se usan los tokens administrativos de E7. Se cotejarán correo, rol, usuarioId/clienteId y pertenencia con SQL actual; no se asignarán tokens por posición del CSV sin comprobar su sujeto. Si alguna identidad carece de citas/notificaciones que requiere el validador E6, se detiene la preparación: no se crean datos para hacer pasar la prueba.

| Fase | Duración propuesta | ¿Dentro de la medición principal? |
|---|---:|---|
| Preparación SQL, tokens y controles | Máximo 180 s | No; se informa aparte |
| Arranque local y llegada de 50 hilos a barrera | 20 s de rampa; barrera acotada | No; todavía sin carga medida |
| Calentamiento con los 50 hilos | 60 s | No; etiquetas/fase diferenciadas |
| Resistencia medida | **600 s exactos** | Sí, diez intervalos de 60 s |
| Drenaje de HTTP ya admitidas | Máximo 20 s | Fuera, reportado como cierre |
| Recuperación y comprobaciones | Máximo 60 s | Fuera, con recursos aún activos |

El reloj común monotónico comienza cuando los 50 hilos llegan a la barrera, antes de la primera petición de calentamiento. `inicioMedicion = inicioCalentamiento + 60 s`; `finMedicion = inicioMedicion + 600 s`. Guardar además anclas epoch UTC para clasificar el JTL. No tomar el primer GET como origen ni dejar que cada hilo tenga su propio plazo.

Los 50 hilos siguen vivos durante todo `[inicioMedicion,finMedicion)`, incluidos temporizadores y esperas HTTP. Un hilo que termina sus turnos debe esperar al plazo común, no salir antes. Se registra un monitor local de hilos vivos cada segundo, además de `allThreads/grpThreads` en el JTL: contar solo los hilos de las muestras no demuestra presencia continua. Si se pierde un hilo o falla la barrera, la fase no cumple el diseño; no se disimula creando reemplazos.

Al llegar al fin no se admiten nuevas peticiones; solo se espera el cierre de las ya iniciadas. No se termina un ciclo de cinco GET empezando nuevas peticiones fuera de los 600 s, como podía ocurrir al drenar ciclos E6. Toda muestra que empiece fuera debe clasificarse aparte y señalarse como desviación, no desaparecer del análisis. El watcher final confirma JMeter/observadores/escritores cerrados antes de leer sus CSV.

## 3. Ritmo repetible y volumen

Objetivo: **10 HTTP/s globales**, equivalente a una petición cada 5 s por cada uno de los 50 hilos. Es conservador frente a las centenas de HTTP/s observadas en E6/E7 y limita el generador en la misma máquina: no busca saturación, sino estabilidad a carga fija. No se cambian CPU, memoria, pool, JVM del backend o umbrales para obtener un aprobado. Tampoco se propone ajustar el heap existente de JMeter; se evita GUI/listeners de cuerpos y se genera el dashboard después de cerrar la captura.

Para hilo `h=1..50` y turno `k`, relativos al inicio de cada fase:

```text
instantePlanificadoMs = k*5000 + (h-1)*100
patron = [notificaciones, historial, especialidades, notificaciones, historial]
endpoint = patron[(k + ((h-1) mod 5)) mod 5]
```

El desfase distribuye una petición cada 100 ms. Rotar la secuencia evita sincronizar todos los hilos sobre el mismo endpoint: cada ronda de 5 s tiene 20/20/10 peticiones y cada minuto 240/240/120. Con un inicio idéntico de secuencia por hilo, los minutos de 12 turnos no tendrían exactamente esa mezcla, aunque el total de 120 sí la tuviera.

| Fase | Turnos/hilo | HTTP totales | Notificaciones | Historial | Especialidades |
|---|---:|---:|---:|---:|---:|
| Calentamiento 60 s | 12 | 600 | 240 | 240 | 120 |
| Medición 600 s | 120 | **6.000** | **2.400** | **2.400** | **1.200** |

La secuencia se reinicia al entrar en medición. El reloj debe fijar el contador esperado antes del preprocesador E6; se reutiliza su validación, no su controlador de ciclos. Los campos propios de E8 (`fase`, `turno`, `instantePlanificado`, `inicioReal`) no se confunden con `e6Iteracion/e6Paso`.

No hay recuperación de turnos atrasados ni reintentos. Si un hilo está ocupado o el generador llega tarde a un turno perdido, se omite ese turno, se conserva su endpoint previsto y se registra la causa. No se acumulan permisos para lanzar una ráfaga. El ritmo es un objetivo de calendario, no una garantía física de precisión submilisegundo en Windows. Se informa retraso planificado/real, cantidades por minuto y pérdida de ritmo; no se baja la tasa a mitad de prueba para obtener un resultado favorable. Las omisiones no son respuestas conformes ni muestras HTTP inventadas.

50 hilos activos **no equivalen a 50 HTTP simultáneas** con este ritmo. A 100 ms de respuesta media y 10/s, el promedio sería aproximadamente una petición en curso; debe medirse, no presentarse como concurrencia HTTP de 50. Si las respuestas tardan más de 5 s, los hilos perderán turnos: se reportarán como carga no alcanzada, sin compensación.

El JTL bruto tendrá aproximadamente 6.600 muestras de calentamiento+medición, más controles separados. A unos cientos de bytes por fila, el orden esperado es pocos MiB, no los cientos de miles de muestras de los escalones sin pacing. Sin cuerpos, cabeceras ni tokens; formato CSV con columnas temporales/tipos/identidad sintética. Se conservará el bruto y se derivará un JTL de solo medición para el dashboard. No usar el dashboard agregado como resumen del sostenido.

## 4. Vigencia de tokens: constatación y puertas futuras

La inspección local encontró **240 tokens decodificables y 240 vencidos**, incluidos los 50 clientes objetivo. Sus expiraciones van de **2026-11-10T23:40:59Z a 23:41:16Z**. La hora de inspección y el TTL exacto están en `preparacion-local.json`; no se mostraron tokens. Decodificar claims **no verifica firma**. Los paquetes privados E6/E7 se eliminaron al cerrar sus corridas y no se supone que sus valores puedan recuperarse de los resultados.

Se reutilizarían tokens solo si están disponibles privadamente, pertenecen a las 50 identidades correctas, tienen vigencia suficiente y pasan controles del servidor. Con el pool actual hay que obtener **50 tokens nuevos fuera del JTL**, sin administrador y sin regenerar los 240 del plan mixto. No se modifica ni se sobrescribe el CSV histórico. Se reutilizará `Preparar-E6Tokens`, que hace login y controles protegidos cotejados con SQL; su tráfico y calentamiento se informan aparte. No se ejecuta ahora.

La configuración del código es `jwt.expiracion-ms=86400000`, pero la decisión usa el `exp` observado y la identidad real, no una duración nominal ni la hora de modificación del CSV [JwtTokenGeneratorAdapter](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/src/main/java/com/appointech/appointech_backend/infrastructure/adapters/JwtTokenGeneratorAdapter.java:27>). No se amplía la expiración del servidor ni se fabrica un JWT.

| Puerta | TTL mínimo requerido, sobre todos los tokens |
|---|---:|
| Preparación | **1.060 s** = 180+20+60+600+20+60+120 |
| Inicio de calentamiento, tras barrera | **860 s** = 60+600+20+60+120 |
| Inicio de medición | **800 s** = 600+20+60+120 |

Es margen de planificación, no un nuevo SLA HTTP. Registrar solo cantidad válida, menor TTL y expiración mínima/máxima. Validar `sub`, `rol`, `id`, `iat`, `nbf` si existe y correspondencia SQL; un GET protegido comprueba firma y rol mediante el servidor antes de la carga. Revisar reloj UTC sin introducir silenciosamente tolerancias de firma. Repetir las puertas usando la hora real de cada transición. Durante la medición, monitorear TTL relativo al plazo y clasificar 401/403; **no refrescar tokens, no hacer login y no continuar solo con el endpoint público**. Una pérdida de autenticación invalida la fase y detiene nuevas admisiones. Sin renovar ahora, las tres puertas fallan.

Los secretos permanecerán en un paquete temporal con ACL del usuario y en memoria, no en `-J`, XML o logs. Reutilizar la creación/eliminación privada existente. Borrar el paquete al cerrar, incluso al fallar. Registrar los hashes de fuentes y configuración, nunca el hash/token de cada JWT como sustituto de anonimización.

## 5. Análisis de diez intervalos de un minuto

`M_i = [inicioMedicion+60*i, inicioMedicion+60*(i+1))`, i=0..9. Denominador exacto: 60 s; total: 600 s. Guardar una matriz fase de inicio → fase de fin y cruces de cada límite para reconciliar muestras. No censurar respuestas lentas que terminan después del minuto o del final de medición.

| Métrica por minuto | Definición |
|---|---|
| Hilos | Mínimo/máximo del monitor local de 1 s, presencia de los 50, y máximo observado en JTL; no sumar hilos por muestra |
| Iniciadas/s | Peticiones cuyo inicio real cae en el minuto / 60 |
| HTTP/s recibidas | Todas las respuestas con fin en el minuto / 60; separar códigos HTTP de fallos de transporte |
| HTTP 200 recibidas/s | Respuestas 200 con fin en el minuto / 60 |
| Conformes/s recibidas | HTTP 200 + contenido/propiedad válidos + elapsed≤umbral del endpoint, con fin en el minuto / 60 |
| p95/p99 por endpoint | elapsed de la cohorte que empezó en el minuto; rango más cercano ceil(p*n); incluir sus respuestas tardías y publicar n |
| Errores | HTTP 4xx/5xx, 401/403 separados; conexión/timeout de lectura/otros transportes; contenido, pertenencia/orden y exceso de duración separados |
| CPU y memoria | Backend y PostgreSQL por separado; capturas, CPU media/máxima muestreada, memoria mediana/máxima y diferencia respecto al minuto inicial |
| Ritmo y concurrencia | Turnos previstos/admitidos/omitidos, retraso de inicio y solapamiento HTTP máximo/media integrado por intervalos |

“Recibida” usa el fin del sampler `timeStamp+elapsed`, no la hora del callback de validación. Mantener visibles las respuestas de calentamiento que terminan durante el minuto 1: se publican en la tasa total por finalización y también se separan de las de origen medido. Ofrecer la tasa conforme restringida a peticiones iniciadas en medición para evitar atribuir calentamiento a rendimiento medido. Las latencias y fallos por inicio nunca cambian de cohorte. No sumar categorías de error solapadas como si fueran muestras distintas.

Objetivos ideales/minuto: 600 muestras, 240/240/120 por endpoint; los valores reales y omisiones son obligatorios. Si hay pérdidas de ritmo o hilos, informar que no se aplicó completamente la carga propuesta, no un aprobado de resistencia bajo otra carga. Con unas 120 especialidades/minuto, p99 describe muy pocas observaciones de cola; no sobrerrepresentar su precisión.

## 6. Recolector y cobertura: lección de E6-300

No reutilizar el patrón E6 de reabrir `recursos.csv` en cada `Add-Content`. Usar el escritor persistente de [recursos-e7.ps1](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/recursos-e7.ps1>), con ambos servicios validados antes de escribir el par, disponibilidad antes de arrancar la carga y cierre en finally. No leer los CSV activos; analizar después de `Wait-Job`/cierre. Generar el dashboard después de la captura, no mientras se mide.

Se verificó localmente el ciclo de vida con datos ficticios y **sin Docker stats real, backend ni JMeter**: 38 pares completos y detección de pérdida final y de una captura sin pareja. Hubo un primer intento fallido por ruta relativa del job; el helper de prueba ahora convierte su salida a ruta absoluta, sin cambiar el recolector E7. La prueba es corta y no demuestra continuidad real durante 600 s.

Antes de una ejecución futura, comprobar aislamiento y que los IDs muestreados sean exactamente backend-perf/db-perf. Iniciar captura antes del calentamiento, mantenerla en drenaje y recuperación y cerrarla después de esta. Validar: primer par≤inicioCalentamiento; último par≥finRecuperación; dos servicios únicos por captura, filas numéricas válidas y cronología monótona. Con cadencia observada alrededor de 2,1 s, se esperan unas 28–30 capturas/servicio/minuto, no 60. Advertir huecos >4 s y marcar cobertura incompleta si un hueco supera 6 s o falta un extremo/pareja. Son controles de calidad de instrumentación, no cambios del SLA del backend.

Alinear CPU/memoria con las mismas ventanas UTC del JTL. No atribuir recursos a endpoints individuales. CPU es porcentaje Docker sin normalizar al límite; memoria no es heap. Informar tendencia, muestras y huecos; un crecimiento de memoria o caché no demuestra por sí solo fuga. Si falla el escritor, queda E8 sin validación completa de recursos: no interpolar datos, no reintentar la carga automáticamente y no atribuir el conflicto a una observación posterior de otro PID. [Nota E6-300](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/NOTA-E6-300-RECURSOS-PARCIALES.md>).

## 7. Criterios, integridad y recuperación

- Preparación válida: perfil/red/base/puerto exclusivamente perf, mismo estado/IDs/límites de referencia, sin OOM/reinicios/carga propia; 50 identidades correctas y tokens aptos; fuentes/contratos sin cambios. No reiniciar ni reconstruir servicios para mejorar el resultado.
- Calentamiento separado y fijo de 60 s; no extenderlo hasta “mejorar” p95. Fallos de identidad, contrato o salud detienen antes de medición.
- Medición válida: 50 hilos presentes durante los 600 s, reloj/ritmo registrados, sin renovación de tokens, sin escrituras de negocio y sin ocultar omisiones o errores. Los 6.000 son objetivo nominal; no fingir muestras para llegar a él.
- Umbrales de solicitud E6 intactos: 800/800/300 ms; timeouts 5000/10000 ms. Conservar el criterio global de errores **<1%** del régimen sostenido existente en planes mixtos, contando cada muestra fallida una vez; minutos y endpoints se desglosan sin inventar un nuevo SLA p99. Un 200 correcto pero lento no es conforme. Un 401/403 o fallo de contenido/propiedad invalida la identidad/contrato, aunque el porcentaje global sea bajo.
- No se añade un umbral de CPU/memoria para obtener aprobado. OOM, reinicio, degradación de salud, integridad o captura incompleta impiden una conclusión satisfactoria. No bajar el ritmo ni aumentar recursos/timeouts durante la prueba.
- Tomar cantidades y hashes canónicos por PK de las 15 tablas de negocio con `Huella-E7` antes de preparar/calentar y después del drenaje/recuperación; excluir auditoría esperada y metadatos Flyway, como E7. Comprobar además IDs y campos de los tres endpoints con las expectativas SQL y `leida` sin cambios. GET no debe alterar datos; sin limpieza, reseeding o reset de secuencias.
- Tras dejar de admitir HTTP, drenar hasta 20 s. Mantener monitor/recursos. Realizar tres series consecutivas de GET de los tres endpoints con una identidad preparada, dentro de 60 s, con los mismos umbrales y contenido. Registrar tiempos/códigos por endpoint, salud, reinicios/OOM y memoria/CPU tras parar. Son controles fuera de la medición; la espera entre series no es latencia HTTP. Confirmar huellas iguales y cierre de generador/recolector. No exigir que RSS vuelva exactamente al valor inicial, pues puede retener caché.

## 8. PowerShell: comandos disponibles de preparación local

Ejecutables desde la raíz del repositorio. Ninguno de este bloque envía HTTP, llama a Docker o ejecuta JMeter:

```powershell
$e8Config = Join-Path $PWD 'perf/diseno/e8-resistencia.json'
$e8Salida = Join-Path $PWD ('perf/diagnosticos/e8-revision-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/inspeccionar-e8.ps1 `
  -Configuracion $e8Config -Salida $e8Salida

# Solo puerta temporal. Con el CSV actual devuelve 2: tokens vencidos; no llama al servidor.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/inspeccionar-e8.ps1 `
  -Configuracion $e8Config -ExigirVigencia -Etapa Preparacion -Salida ($e8Salida + '-tokens')

# Prueba local corta del escritor; no es una corrida E8 ni una medición de recursos reales.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/validar-recolector-e8-sin-http.ps1 `
  -Salida ($e8Salida + '-recolector')
```

Para un paquete privado E6 disponible en una futura preparación, el mismo inspector acepta `-PaqueteE6 <ruta>` y `-Etapa Calentamiento` o `-Etapa Medicion`, exigiendo 860 u 800 s. El éxito del chequeo de claims no sustituye firma/controles del servidor ni autoriza HTTP.

### Bloque futuro de preparación de tokens: contiene HTTP, NO ejecutado ni autorizado en este turno

El siguiente bloque es copiable para **otra ejecución expresamente autorizada**, cuando la implementación y validación ficticia de E8 estén listas. Usa solo helpers existentes; prepara 50 tokens fuera de JMeter y no carga ni mide E8. No cambia CSV históricos. No pegar secretos en la línea de comandos.

```powershell
$ErrorActionPreference = 'Stop'
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass -Force
Add-Type -AssemblyName System.Net.Http
. ./perf/jmeter/preparar-e2.ps1
. ./perf/jmeter/preparar-e6.ps1
$e8Compose = @('compose','--env-file',(Join-Path $PWD 'perf/.env.perf'),
  '-f',(Join-Path $PWD 'perf/docker-compose.perf.yml'))
$e8Aislamiento = Confirmar-E2Aislamiento $e8Compose 18080
$e8AuthDir = Join-Path $PWD ('perf/diagnosticos/e8-auth-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Path $e8AuthDir -ErrorAction Stop | Out-Null
$e8Datos = Leer-E6Datos $e8Compose $e8Aislamiento 50 $e8AuthDir
foreach ($e8Identidad in $e8Datos.identidades) {
  if ($e8Identidad.correo -cne ('cliente' + $e8Identidad.ordinal + '@perf.test')) {
    throw 'Cohorte distinta de cliente1..50: detener sin preparar tokens.'
  }
}
$e8Enum = Get-Content 'src/main/java/com/appointech/appointech_backend/domain/models/TipoNotificacion.java' -Raw
$e8Tipos = [regex]::Match($e8Enum,'(?s)enum TipoNotificacion\s*\{([^}]+)\}').Groups[1].Value -split ',' |
  ForEach-Object { $_.Trim() }
$e8Datos | Add-Member -NotePropertyName tiposNotificacion -NotePropertyValue @($e8Tipos)
$e8Datos | Add-Member -NotePropertyName estadosCita -NotePropertyValue @(
  'PENDIENTE','CONFIRMADA','EN_CAMINO','EN_PROCESO','FINALIZADA','CANCELADA')
$e8Credenciales = Import-Csv 'perf/jmeter/clientes.csv'
# A PARTIR DE ESTA LINEA HAY HTTP: 50 logins y controles protegidos, fuera del JTL.
Preparar-E6Tokens $e8Datos $e8Credenciales (Join-Path $e8AuthDir 'autenticacion.csv')
$e8Paquete = Escribir-E6PaquetePrivado $e8Datos (Join-Path $e8AuthDir 'errores-seguros.jsonl')
try {
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/inspeccionar-e8.ps1 `
    -PaqueteE6 $e8Paquete -Etapa Preparacion -ExigirVigencia -Salida (Join-Path $e8AuthDir 'vigencia')
  if ($LASTEXITCODE -ne 0) { throw 'Tokens no aptos para cubrir E8.' }
  # No invocar E6 aquí. Un futuro lanzador E8 debe integrar reloj, pacing y las tres puertas.
} finally {
  if (Test-Path -LiteralPath $e8Paquete) { Remove-Item -LiteralPath $e8Paquete -Force }
  $e8Padre = Split-Path -Parent $e8Paquete
  if (Test-Path -LiteralPath $e8Padre) { Remove-Item -LiteralPath $e8Padre -Force }
  $e8Datos = $null; $e8Credenciales = $null
}
```

El bloque valida y elimina el paquete al terminar: no deja una credencial persistida lista para una invocación posterior. La implementación futura deberá mantenerlo privadamente dentro del mismo ciclo de vida, revalidar las puertas y eliminarlo al cerrar, como E6. No se proporciona un comando `jmeter -t e8...` apuntando a un archivo inexistente ni se afirma que ejecutar E6 sin pacing sea E8.

## 9. Entregado y pendiente de ejecución

Entregado: diseño/configuración, 6.600 turnos teóricos con distribución validada, inspección local de expiración y prueba del escritor. Preservación SHA-256 de todos los archivos en `perf/resultados`; sin modificaciones a E1–E7.

Antes de un HTTP real posterior: implementar JMX/reloj E8 y análisis minuto a minuto; probar sin backend duración, 50 hilos vivos hasta el final, ritmos, expiración entre fases, supresión de turnos perdidos, límites de ventana y fallos del recolector; luego validar aislamiento, datos y tokens reales. No se considera verificado un futuro intervalo de 600 s solo por la prueba local corta. **Detener aquí, antes de E8.**
