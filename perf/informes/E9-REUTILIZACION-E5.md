# E9: falla controlada del sistema — evidencia reutilizada de E5

**Este es un informe documental de las mismas cuatro corridas E5 (50, 100, 200 y 300 usuarios), no una quinta ejecucion ni cuatro fases nuevas.** No se ejecuto JMeter, HTTP, Docker ni carga adicional al redactarlo. Los JTL y resultados originales permanecen intactos. No se usan E6/E7/E8 ni otros ensayos para completar los resultados de E9.

## Requisito real del profesor y alcance

Fuente: [ESCENARIOS OBLIGATORIOS DE PRUEBAS DE CARGA Y ESTRES.pdf](</C:/Users/Juan David/Downloads/ESCENARIOS OBLIGATORIOS DE PRUEBAS DE CARGA Y ESTRÉS.pdf>), pagina 6, escenario 9. Objetivo: comportamiento ante sobrecarga extrema; escenario: incrementar carga hasta provocar errores. Validaciones: tipo de error, capacidad de recuperacion y estabilidad parcial. La pagina 1 exige ademas el flujo frontend ↔ backend ↔ base de datos. SHA256 del PDF: c02d299a4cca2aa64b6070b9336fa5655241140eb90ef9e8f57931c95e79c801. Texto extraido y fuente preservados en [analisis documental](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/analisis/cierre-e8-e9-2026-11-12T03-55-50-054Z/requisito-profesor.txt>).
E9 no fija un numero minimo de usuarios, una duracion, un HTTP500 obligatorio ni una inyeccion/reinicio forzado. No se inventan estos requisitos. La evidencia reutilizada es login REST directo en perf; no demuestra el recorrido frontend ni una caida interna del servidor.

## Protocolo original reutilizado

Un POST /api/auth/login por iteracion, 20 iteraciones por hilo, rampa de 10s incluida completa. Una identidad CLIENTE sintetica estable y distinta por hilo, primeras N reutilizadas entre fases. Carga cerrada sin tasa abierta fija: cada hilo espera respuesta o timeout antes de emitir otra iteracion. Umbral login original 2000ms; conexion 5000ms, respuesta 10000ms. No se modifican BCrypt, negocio, timeouts, recursos ni umbrales tras los resultados. Los controles usan otra cuenta y quedan fuera del JTL.
Eran procesos separados y secuenciales con recuperacion entre fases, no una rampa unica continua 50→300. El calentamiento previo y actividad del anfitrion pueden ser distintos; no es un experimento que aisle solo N. No se descarta la rampa ni se recortan timeouts.

## Throughput y latencias por fase

Para cada fase: a=min(timestamp), b=max(timestamp+elapsed), T=(b-a)/1000. Throughput total=N muestras/T, incluyendo solicitudes fallidas y timeouts. HTTP200 recibidas/s=N codigos HTTP200 observados/T; toda finalizacion del conjunto esta en [a,b], incluido el ultimo final. No es la tasa real de finalizacion del servidor. Numeradores y denominadores se muestran completos; ambos usan la misma ventana.

|Usuarios|Muestras|Ventana s|Total muestras/s (N/T)|HTTP200 recibidas/s (N200/T)|Conformes|p95/p99 cliente ms|Hilos max / HTTP solapadas max|
|---|---:|---:|---|---|---:|---|---|
|50|1000|40.693|1000/40.693 = 24.5743|1000/40.693 = 24.5743|709|2957/3789|50/50|
|100|2000|79.553|2000/79.553 = 25.1405|1980/79.553 = 24.8891|498|6966/9961|100/100|
|200|4000|158.641|4000/158.641 = 25.2142|2663/158.641 = 16.7863|215|10015/10017|200/200|
|300|6000|210.158|6000/210.158 = 28.5499|642/210.158 = 3.0548|64|10017/10022|300/300|

## Errores separados y sin doble conteo

|Usuarios|Asercion duracion explicita (200 valido lento)|HTTP numerico distinto de 200|SocketTimeoutException|Otros transportes / contenido|Elapsed>2000 incl. timeout (derivado)|Fallidas unicas|% fallidas|
|---|---:|---:|---:|---|---:|---:|---:|
|50|291|0|0|0/0|291|291|29.100|
|100|1482|0|20|0/0|1502|1502|75.100|
|200|2448|0|1337|0/0|3785|3785|94.625|
|300|578|0|5358|0/0|5936|5936|98.933|

Las aserciones de duracion explicitas se verificaron en failureMessage y coinciden con HTTP200 de contenido valido que exceden 2000ms. No se deduce que todo 200 fallido sea lento sin revisar contenido/asercion. En estos cuatro JTL esa es la unica causa de los 200 fallidos.
SocketTimeoutException aparece como **Non HTTP response code: java.net.SocketTimeoutException**, mensaje **Read timed out**. No es un codigo HTTP distinto de 200 ni una respuesta HTTP500. Los timeouts no tienen contenido HTTP recibido verificable. La columna elapsed>2000 es un exceso temporal derivado, no evidencia de una segunda asercion exportada: failureMessage conserva el primer fallo y en el timeout muestra Transporte. Por ello no se suma esa columna a SocketTimeoutException.
Aqui fallidas unicas = 200 validos lentos + SocketTimeoutException: 291+0; 1482+20; 2448+1337; 578+5358. Todas las respuestas HTTP efectivamente recibidas fueron 200 con contenido valido. Ninguna fase cumple menos de 1% fallido ni duracion por muestra; HTTP200 recibido no equivale a respuesta conforme.
Un timeout indica cuando JMeter dejo de esperar, **no cuando termino ni si se cancelo el trabajo del servidor**. Una operacion puede seguir ejecutandose despues. Los percentiles proximos a 10s estan dominados por el limite del cliente y no son percentiles completos del tiempo del servidor. La siguiente iteracion puede comenzar al agotarse la espera de la anterior; no permite reconstruir backlog, operaciones completadas tarde o ejecucion simultanea real del servidor.
El throughput total crece 24.574→25.140→25.214→28.550/s, pero el recibido HTTP200 baja 24.574→24.889→16.786→3.055/s. El aumento total de 300 incluye 5358 esperas agotadas: no representa mejora de autenticaciones exitosas. Se observa degradacion de estas corridas y estos recursos, sin identificar un limite universal o una causa exclusiva.

En la fase300 el ultimo HTTP200 recibido del JTL termino a 2026-11-11T14:32:08.234Z, 103.269s desde el primer inicio; el ultimo final cliente fue a 2026-11-11T14:33:55.123Z. No se recibieron mas HTTP200 en el resto de esa ventana. Por ello no se afirma disponibilidad funcional durante toda la fase a partir de 642 respuestas agregadas. Los timeouts tampoco permiten conocer respuestas tardias del servidor que JMeter ya no espero.

Los 2000ms y el criterio de menos del 1% fallido son criterios previos de E5; no parametros numericos nuevos exigidos por el escenario9 del profesor.

## Recursos disponibles y observados

Estados y limites originales: backend 2 CPUs de cuota y 1GiB; PostgreSQL 1GiB sin cuota CPU configurada. Misma maquina para JMeter, backend y BD; no se acreditan cores dedicados ni RAM libre del anfitrion. El JTL no mide uso del generador, heap/GC, pool, locks, I/O o red. No se inventa hardware disponible. Se accede a localhost:18080 sin nginx y a appointech_perf en red exclusiva; PostgreSQL usa tmpfs.

|Usuarios|Capturas por servicio durante HTTP|CPU backend max %|Mem backend max MiB|CPU PG max %|Mem PG max MiB|Pares totales|Hueco max ms|
|---|---:|---:|---:|---:|---:|---:|---:|
|50|20|220.33|505.5|9.50|157.5|24|2057|
|100|38|221.59|522.1|37.17|160.1|43|2596|
|200|76|228.98|576.1|11.30|163.3|82|2216|
|300|102|278.05|634.3|52.47|165.8|108|2946|

Son maximos muestreados, no picos absolutos. CPU conserva el porcentaje crudo Docker (puede superar 100%, sin normalizar a la cuota). No se usa un pico para afirmar que se cambio o violo el limite: se contrastaron estados/configuracion iguales antes y despues. La memoria es del contenedor, no heap Java. La alta CPU coincide con el deterioro pero no prueba que BCrypt, el pool o PostgreSQL sean la causa unica.
La competencia de recursos en una sola maquina mezcla carga del generador, backend, BD y Docker/OS. tmpfs evita reproducir el comportamiento de almacenamiento persistente real; no se prueban latencia de disco, durabilidad frente a perdida de tmpfs o recuperacion por reinicio. Ninguna evidencia aqui autoriza extrapolar capacidad a produccion.

## Recuperacion historica y estabilidad parcial

|Usuarios|Controles externos al JTL|HTTP ms observados|Resultado|
|---|---:|---|---|
|50|3|143, 64, 59|3 consecutivos validos|
|100|3|354, 87, 186|3 consecutivos validos|
|200|3|153, 67, 61|3 consecutivos validos|
|300|4|10567, 99, 111, 66|Primer transporte fallido; luego 3 consecutivos validos|

Ultimo final observado del JTL300: 2026-11-11T14:33:55.123Z. La tabla usa inicio/fin de cada control registrados por el ejecutor; esos intervalos incluyen aislamiento/salud y no son solo duracion HTTP. La columna HTTP ms es la duracion medida de la peticion de control.

|Intento 300|Inicio UTC|Fin UTC|Inicio tras fin JTL s|HTTP / tipo|HTTP ms|Salud|Consecutivos|
|---|---|---|---:|---|---:|---|---:|
|1|2026-11-11T14:34:01.247Z|2026-11-11T14:34:12.775Z|6.124|sin respuesta / Transporte|10567|saludable|0|
|2|2026-11-11T14:34:17.847Z|2026-11-11T14:34:19.077Z|22.724|200 / ninguno|99|saludable|1|
|3|2026-11-11T14:34:24.089Z|2026-11-11T14:34:26.097Z|28.966|200 / ninguno|111|saludable|2|
|4|2026-11-11T14:34:31.107Z|2026-11-11T14:34:31.827Z|35.984|200 / ninguno|66|saludable|3|

El primer control de 300 fallo por transporte y reinicio la cuenta de consecutivos. Su registro no conserva la clase de excepcion: **no se le atribuye SocketTimeoutException retroactivamente**. Luego se obtuvieron tres HTTP200 de contenido valido, 99/111/66ms. Al final del cuarto control, 36.704s despues del ultimo final cliente, quedo acreditada la recuperacion funcional definida, no una recuperacion inmediata. No se conoce el instante exacto en que el servidor liquido todo trabajo pendiente.
Los estados antes/despues son iguales en las cuatro fases (IDs, StartedAt, limites), activos/healthy, RestartCount=0 y OOMKilled=false; estado-final confirma salud y ausencia de procesos de carga/stats al cierre registrado. Estos son estados historicos de esas fases, no una consulta de salud actual. Salud Docker no garantiza que una autenticacion responda a tiempo: el primer control300 fallo aunque salud=true. No hubo monitor continuo de salud durante Java, por lo que no se niegan episodios transitorios no registrados.

**Limite importante de la traza de recursos de recuperacion:** el recolector E5 cerro antes de los controles.

|Usuarios|Ultimo recurso UTC|Primer control UTC|Fin de tres consecutivos UTC|Recursos cubren controles|
|---|---|---|---|---|
|50|2026-11-11T05:32:23.450Z|2026-11-11T05:32:23.803Z|2026-11-11T05:32:35.772Z|NO|
|100|2026-11-11T14:09:40.216Z|2026-11-11T14:09:41.930Z|2026-11-11T14:09:59.051Z|NO|
|200|2026-11-11T14:21:15.757Z|2026-11-11T14:21:16.347Z|2026-11-11T14:21:29.373Z|NO|
|300|2026-11-11T14:34:00.816Z|2026-11-11T14:34:01.247Z|2026-11-11T14:34:31.827Z|NO|

En300 la ultima captura es 2026-11-11T14:34:00.816Z, 0.431s antes del primer control. Las tres capturas posteriores al JTL aun incluyen CPU backend maxima 198.20% y memoria maxima 631.1MiB. No cubren los tres controles satisfactorios ni prueban retorno a una linea base de recursos. No se rellena ese hueco con E8 u otras corridas.

## Contraste con el profesor: cobertura y faltantes

|Requisito de la guia|Evidencia exclusiva E5|Cobertura y limite|
|---|---|---|
|Incrementar carga hasta errores (p6)|50→100→200→300, aserciones de duracion desde50 y timeouts desde100|Cubierto para errores observados por el cliente en fases separadas; no prueba caida interna del servidor|
|Tipo de error (p6)|Duracion explicita, Read timed out/SocketTimeoutException, cero HTTP no200|Cubierto para lo recibido/observado; no hay HTTP500 que reportar ni trazas que identifiquen causa interna de cada timeout|
|Capacidad de recuperacion (p6)|3 logins funcionales consecutivos y salud;300 necesitó4 controles|Acreditada funcionalmente, con demora inicial; no hay recursos durante esos controles ni prueba del vaciado completo de backlog/retorno a baseline|
|Estabilidad parcial (p6)|HTTP200 validos coexistieron con timeouts; salud y sin reinicios/OOM|Evidencia parcial; no monitor continuo, no verificaciones CRUD/integridad o transacciones de otras funciones dentro de E5|
|JMeter y APIs REST backend (p1)|Planes/logs/JTL de POST /api/auth/login|Cubierto por las cuatro ejecuciones originales, sin repetirlas|
|Flujo frontend ↔ backend ↔ base de datos (p1)|E5 invoca backend directo y perf acredita BD|No cubre navegador/frontend, nginx, renderizado o recorrido extremo a extremo|

La guia no exige especificamente provocar HTTP500, reiniciar servicios o ejecutar una quinta prueba; no se atribuyen esos faltantes como obligaciones textuales. Lo que no queda cubierto es la validacion extremo a extremo y la profundidad de la estabilidad/recuperacion del servidor mas alla de los controles disponibles. Si se exige demostrar una caida del servidor o error interno especifico bajo el titulo falla del sistema, estas evidencias no lo prueban; solo demuestran incumplimiento de duracion y abandono de espera del cliente. Este informe deja esa distincion visible y no declara cobertura total.

## Proveniencia de las mismas cuatro corridas

- **50**: [JTL](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/e5.jtl>); SHA256 28cb5413deca8d9e8df1921fec9289eb8e25b7dc0f54ca48d45be9f199236df4. [configuracion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/ejecucion.json>), [recuperacion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/recuperacion.csv>), [recursos](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/recursos.csv>), [estado antes](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/estado-antes.json>), [estado despues](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/estado-despues.json>), [estado final](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-003133-8ba407386dc24c2b937816a930389c0b/fase-50/estado-final.json>).
- **100**: [JTL](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/e5.jtl>); SHA256 f043cc8d3f72dc5edd713aff5afb28dbaebab7c48f8863f30eecc69ade64082b. [configuracion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/ejecucion.json>), [recuperacion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/recuperacion.csv>), [recursos](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/recursos.csv>), [estado antes](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/estado-antes.json>), [estado despues](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/estado-despues.json>), [estado final](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-090805-cf3ecc2d466e43e0a8dd6799958785c1/fase-100/estado-final.json>).
- **200**: [JTL](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/e5.jtl>); SHA256 db7becc9f1cdb73e8e1c7b1765227f6523be5b7630b47c07df52c457744ad51b. [configuracion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/ejecucion.json>), [recuperacion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/recuperacion.csv>), [recursos](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/recursos.csv>), [estado antes](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/estado-antes.json>), [estado despues](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/estado-despues.json>), [estado final](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-091823-fa9b3874d23246e4a5a0b3e39c25eb45/fase-200/estado-final.json>).
- **300**: [JTL](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/e5.jtl>); SHA256 053e5f805f87d13c88b92f82015d067086ace6ad12aff18b13af235de54c635a. [configuracion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/ejecucion.json>), [recuperacion](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/recuperacion.csv>), [recursos](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/recursos.csv>), [estado antes](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/estado-antes.json>), [estado despues](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/estado-despues.json>), [estado final](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/resultados/e5-20261111-093015-37a5807c10bf495da015ffd5e5ccef9f/fase-300/estado-final.json>).

Calculos completos (numeradores, denominadores, clases, limites y cronologia): [e9-reanalisis-e5.json](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/analisis/cierre-e8-e9-2026-11-12T03-55-50-054Z/e9-reanalisis-e5.json>). Informe E5 historico: [INFORME-E5.md](</C:/Users/Juan David/Downloads/appointech-backend/appointech-backend/perf/jmeter/INFORME-E5.md>). No se ejecutaron sus analizadores que escriben dentro de los resultados. No se avanzo a otro escenario ni se hicieron comprobaciones HTTP actuales.
