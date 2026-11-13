# E8 implementado: resistencia de lecturas

Complementa `E8-RESISTENCIA.md` y conserva su configuracion, contratos, umbrales y limites. El campo `permisosTurnoActual` del JSON describe el turno de diseno anterior; la autorizacion posterior del usuario permite una unica corrida real despues de validar. No cambia el destino exclusivamente perf.

## Comandos PowerShell

Desde la raiz del repositorio:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/validar-e8.ps1
# Solo con validacion satisfactoria y fuentes sin cambios:
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/ejecutar-e8.ps1
```

El lanzador vuelve a comprobar la puerta ficticia y las huellas de las fuentes; mutex compartido con las otras fases, procesos propios, aislamiento, salud y limites de referencia E6. Descubre y captura las 15 tablas, prepara 50 identidades CLIENTE y renueva sus tokens fuera del JTL mediante los helpers E6. Los controles protegidos verifican firma/rol y pertenencia con el servidor. Un paquete temporal con ACL del usuario mantiene expectativas/tokens; se elimina al cargarlo en memoria y al cerrar. No se sobrescribe el CSV historico de tokens.

Claims: presupuesto minimo 1060s antes de carga, 860s antes del calentamiento y 800s antes de medicion, comprobando tambien sujeto/rol/id contra las identidades SQL. Nunca se renuevan tokens dentro de la carga. La preparacion tiene limite de 180s. Las marcas `E8-REAL-INICIADO.json` bloquean una segunda corrida real incluso si la primera se interrumpe.

## Reloj y ritmo

`e8-resistencia.jmx` y `e8-ficticio.jmx` usan los mismos controladores y motor. El ficticio sustituye exclusivamente la respuesta HTTP por datos sinteticos y no contiene HTTPSamplerProxy. El real conserva GET E6, keep-alive, cero reintentos y timeouts 5000/10000ms. El validador E6 compara IDs, campos, orden, tipos, estados y total paginado; DurationAssertion conserva 800/800/300ms.

Los 50 hilos llegan a una barrera tras 20s de arranque local. Reloj monotono comun; calentamiento 60s, medicion 600s; secuencia reiniciada en la medicion y rotada por hilo. Cada hilo permanece vivo hasta el final aunque termine su ultimo turno. Monitor independiente cada segundo y cierre registrado. No hay checkpoints SQL durante HTTP; las huellas inicial/final y los controles de recuperacion son externos al JTL.

Se planifica un turno cada 100ms, 10 por segundo global, un turno/hilo cada 5000ms. Se omiten turnos con retraso de 50ms o mas, dejando guarda para preprocesar antes de la siguiente frontera. El arbitro limita a diez admisiones por segundo monotono y evita agrupaciones a menos de 50ms. No acumula permisos ni compensa omisiones. Los 100ms son separacion nominal del calendario; la separacion observada puede variar por jitter de Windows. Exigir ademas 100ms desde cada admision efectiva acumulaba ese jitter y reducia innecesariamente el ritmo; la prueba ficticia detecto y corrigio ese defecto antes del primer HTTP.

Las omisiones conservan hilo, fase, turno, endpoint y causa en `turnos-*.jsonl`. No se inventan muestras para alcanzar 6000. La tasa observada y su desviacion se publican. **50 hilos activos no equivalen a 50 peticiones HTTP simultaneas** a este ritmo.

## Validacion y analisis

El ficticio ejecuta 50 hilos con el mismo ritmo de 10/s y ventanas acotadas de 10s +20s; no demuestra resistencia real de diez minutos. Se comprueba aparte el calendario teorico completo de 6000 turnos: 240/240/120 por cada uno de los diez minutos. Incluye respuestas sinteticas con la extraccion/validacion E6, limites inicio/fin, arbitro global por segundo real monotono, parada, cierre de JMeter/monitor/recolector y deteccion de cobertura truncada. Los negativos exigen rechazo explicito de JTL vacio y medicion con hilos incompletos. Se conservan los intentos ficticios fallidos y sus diagnosticos.

El analizador concilia muestras HTTP con todos los turnos admitidos/omitidos y verifica 50 hilos hasta el fin. Latencias, errores y percentiles por inicio; respuestas HTTP200 y conformes por finalizacion, incluyendo cruces de ventanas que se publican por separado. Conforme = HTTP200 + contenido valido + duracion dentro del umbral propio. Cuantiles nearest-rank por endpoint. Tasas con numerador/denominador explicitos. Ningun SLA p99 nuevo.

Diez ventanas de 60s muestran hilos, iniciadas/s, HTTP200 recibidas/s, conformes/s, codigos, contenido/duracion, timeouts, p95/p99, solapamiento HTTP maximo/media y recursos backend/PostgreSQL. `medicion.jtl` es una copia derivada de inicios medidos para el dashboard; `e8.jtl` conserva calentamiento y cierre. El dashboard no define las tasas por finalizacion; mandan el JSON y el informe.

Se usa el escritor persistente E7, preparado antes de arrancar JMeter y mantenido durante drenaje y recuperacion. Su cierre precede a cualquier lectura de recursos CSV y a generar el dashboard. Se exige primer par anterior al calentamiento y ultimo posterior a recuperacion, pares unicos, huecos <=6s (aviso >4s); controles de instrumentacion, no SLA del backend. Si la captura esta truncada o no se puede parsear, se conserva el JTL, se analiza HTTP y se declara la limitacion; no se inventa ni interpola CPU/memoria.

Recuperacion: tres series de los tres GET con una identidad preparada, contenido completo y umbrales originales, salud/limites/reinicios y huellas/cantidades iguales en las 15 tablas. Las esperas entre series no son tiempo HTTP. No se exige devolver RSS exactamente al valor inicial ni se concluye ausencia de fugas a largo plazo. Se excluyen auditoria esperada y metadatos Flyway, como E7. No hay limpieza, reseeding ni SQL de escritura.

La corrida se detiene en E8. Un fallo no dispara repeticion ni E9.

## Resultado de la unica corrida real de este turno

E8 fue interrumpida en calentamiento: cuatro HTTP (tres 401 y un 200), cero muestras medidas. El JMX inicial omitio HeaderManager, por lo que el preprocesador no pudo configurar Authorization. La validacion ficticia inicial no cubria esa rama y fue insuficiente. Tambien se corrigieron el cierre al parar, el monitor que antes contaba hilos logicamente y el manejo de arrays del control de recuperacion PowerShell. Estas correcciones se validaron solo ficticiamente despues; no hubo segunda carga real.

La validacion actual comprueba HeaderManager en XML, ejercita la rama real de autenticacion sobre un HTTPSamplerProxy local que nunca llama sample(), y verifica parada por HTTP401 con cierre automatico. El monitor consulta numberOfActiveThreads() del grupo real; es daemon y tearDown se ejecuta al parar. Se comparan hashes antes y despues de toda la validacion para impedir acreditar fuentes cambiadas durante ella.

Informe/evidencias: `perf/resultados/e8-50-20261111-213358-1fb139ce0251410aa9d24dd1382a784a/INFORME.md`. Los nueve controles posteriores de recuperacion pasaron, salud/limites siguen estables y las 15 tablas conservan cantidades/huellas. Se conserva el diagnostico original; el control posterior no convierte E8 en satisfactoria. La marca persistente de corrida sigue bloqueando repetirla. Los diez minutos son **no ejecutados**, no diez intervalos con tasas cero.

## Continuacion expresamente autorizada tras el fallo del plan

La nueva solicitud autoriza una unica corrida adicional y exige prueba de cabecera en el transporte HTTP. `validar-e8.ps1` mantiene los casos sin red y agrega otra etapa con **el JMX final real**, destino limitado a `127.0.0.1` y puerto efimero de un servidor Node ficticio. Usa exclusivamente tokens sinteticos con sufijo FICTICIO. No modifica el plan para saltar pre/postprocesadores: el sampler HTTP de JMeter envia realmente la cabecera al servidor y los validadores E6 comprueban la respuesta propia. El servidor exige Bearer correcto para ambas rutas protegidas en las 50 identidades y ausencia de Authorization para especialidades. Nunca guarda ni imprime sus valores. Sus conteos distinguen HTTP local ficticio de HTTP al backend real (cero durante validacion).

En esta etapa se enviaron 300 HTTP ficticias: ambas rutas protegidas verificadas por identidad y 60 publicas sin Authorization, cero errores de cabecera y cero tokens detectados en JTL/logs. Evidencias: `perf/diagnosticos/e8-ficticio-20261111-220523-042ecc5f3169437ab142bbabf02ebde9/servidor-local/`.

El JMX admite propiedades de destino para esa prueba; el lanzador real fuerza `localhost:18080`, `validacionWire=false`, 60s +600s y mantiene el aislamiento perf. El modo wire solo acepta loopback, un puerto diferente de 18080 y datos FICTICIO. La cabecera se configura por peticion con la identidad estable del hilo; el GET publico la elimina. Se comprueba de nuevo antes del sampler real.

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/validar-e8.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/ejecutar-e8.ps1 -ContinuarTrasFalloPlan
```

La opcion de continuacion solo reconoce el intento anterior concreto y su SHA256 JTL, y exige que su auditoria diga FALLIDO_SIN_MEDICION. No es una excepcion de concurrencia: conserva el mutex y las comprobaciones de procesos propios. Otra marca de carga ya iniciada impide repetir de nuevo. El intento previo permanece intacto y no se agrega a las metricas de resistencia.

Antes de la nueva carga se renuevan 50 tokens, se realizan los controles protegidos E6 y se agrega un GET autenticado por identidad con comprobacion completa de contenido contra SQL, fuera del JTL. Se detiene ante 401/contenido ajeno. Las fuentes usadas quedan copiadas antes de JMeter en fuentes-ejecutadas/ de la nueva corrida.

La continuacion termino los 600s medidos: 5996 solicitudes, todas conformes, cero 401/errores/timeouts; cuatro turnos omitidos, 9.9933 HTTP/s. Se mantuvieron 50 hilos; maximo HTTP solapado 4. Captura real: 348 pares, hueco maximo 3525ms y cobertura completa hasta recuperacion. Las 15 tablas conservaron cantidades/huellas; nueve GET de recuperacion correctos. Informe: `perf/resultados/e8-50-20261111-220757-9dff09133751417fb7781a0f08e832cb/INFORME.md`.

El dashboard fallo inicialmente por no conservar las comillas de las columnas adicionales en el encabezado CSV derivado. Se reparo offline sin modificar el JTL original ni repetir HTTP; tambien se evita interpretar avisos stderr de Java como excepciones PowerShell usando captura de proceso. Son cambios posteriores de generacion del reporte, documentados frente a las fuentes archivadas que se ejecutaron. Se conserva el verificacion-final.json original con completa=false y se concilia el cierre en auditoria-continuacion-final.json; dashboard-final/index.html contiene exclusivamente 5996 inicios medidos. El throughput y percentiles de aceptacion siguen las definiciones del analisis, no las estimaciones propias del dashboard. No se avanzo a E9.
