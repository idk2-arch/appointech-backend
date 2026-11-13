# E6 sostenido — fase 100

**Implementación independiente creada; validación previa detenida por proceso activo. No se ejecutó HTTP ni la fase real.** La repetición sostenida y el cierre temporal todavía requieren validación ejecutable completa. No utilizar este borrador para cargar el backend hasta resolver el bloqueo y superar esa validación.

El plan corregido de diez ciclos (`e6-lecturas.jmx`), sus scripts y todos los resultados anteriores permanecen como referencia sin cambios. Esta variante usa `e6-sostenido.jmx`, `e6-reloj-sostenido.groovy`, `ejecutar-e6-sostenido.ps1` y `analizar-e6-sostenido.js`. Reutiliza sin modificar las expectativas SQL, autenticación externa, aserciones de `e6-lecturas.groovy`, aislamiento, ErrorRecord seguro y captura de recursos.

## Protocolo implementado, aún no medido

- Solo fase 100; el ejecutor rechaza 200 y 300.
- Cien identidades CLIENTE distintas, rampa de veinte segundos y ciento veinte segundos adicionales de sostenido.
- Ciclos completos continuos: notificaciones, historial, especialidades, notificaciones, historial; mezcla global 40/40/20. No se fija número de muestras ni se exige 5.000.
- Misma preparación externa: tokens nuevos, HTTP/rol/pertenencia acreditados antes del JTL. Sin login dentro del grupo medido; paquete privado retirado al cargarlo y en finally.
- Umbrales intactos: notificaciones/historial 800 ms, especialidades 300 ms; conexión/respuesta 5.000/10.000 ms. Destino fijo `localhost:18080`, sin nginx ni desarrollo.

Un inicializador local ignorado por JTL registra `t0` cuando el primer hilo entra, antes del primer GET. No se llama a ese instante el primer GET ni se confunde con el arranque previo de Java o del setUp. `tiempo-inicio.json` conserva t0, t0+20 s y t0+140 s en epochMs. El reloj de admisión usa `System.nanoTime()` para el plazo y epoch para correlacionar JTL/recursos. Se compila el reloj en setUp antes de la rampa.

El Thread Group principal se ejecuta una vez; un While repite el bloque interior de cinco GET mientras `e6Continuar` sea true. Se decide la admisión de un nuevo ciclo al terminar el anterior, nunca después del plazo. Un ciclo ya admitido se completa: puede producir peticiones o respuestas de cierre después de t0+140 s. `e6CicloAdmitidoEpochMs` registra su admisión y el analizador exige que sea anterior al plazo. No hay interrupción abrupta ni cambio de límites para terminar antes. El cierre local por hilo queda en memoria y se escribe una sola vez al final en `hilos-tiempo.json`; no hay escrituras por GET.

Los samplers locales de reloj usan `SampleResult.setIgnore()`: no entran al JTL/dashboard. Solo se conservan los tres GET con las aserciones originales. La configuración efectiva declara muestras variables. El analizador exige cien hilos representados, ciclos consecutivos completos y cinco pasos únicos por ciclo, no diez ciclos ni un total fijo.

## Regla temporal y throughput

Clasificación única por **inicio** de muestra:

- Rampa: `[t0,t0+20s)`.
- Sostenido: `[t0+20s,t0+140s)`.
- Cierre: inicios posteriores, hasta terminar los ciclos admitidos.

Una petición que cruza un límite conserva su cohorte de inicio para recuentos, errores y latencias. Se mantienen todas las muestras. Throughput total de inicios = muestras de la cohorte / duración del intervalo (20 o 120 segundos). El throughput de HTTP 200 válidos **recibidos dentro del intervalo** se calcula por final `timeStamp+elapsed`, incluso si la petición comenzó antes; por ello puede pertenecer a otra cohorte de inicio. Se informa también cuántos HTTP 200 válidos pertenecen a la cohorte. No se presentan ambos contadores como equivalentes. En cierre se incluye la última finalización.

Medias/percentiles son de la cohorte de inicio; rango más cercano `ceil(p×N)`. Timeouts representan espera truncada por el límite, no final del servidor. Fallidas únicas usa `success=false`; HTTP, transporte, timeout, contenido y duración pueden solaparse. El cierre se reporta aparte, sin ocultar fallos.

El analizador produce agregados por fase y endpoint, proporciones observadas, evolución cada diez segundos y barrido de intervalos HTTP, contando finales antes que inicios al coincidir. Incluye intervalos que cruzan límites al calcular muestras en curso. Hilos activos proceden del JTL y no prueban ejecución simultánea del servidor. CPU/memoria se segmentan por timestamp real de captura; máximos muestreados no son picos absolutos y memoria de contenedor no es heap.

## Validación local y bloqueo actual

`validar-e6-sostenido.ps1` prepara una copia sin HTTPSamplers ni preparación autenticada, conserva While/Loop/reloj y usa dos hilos, un segundo de rampa y dos sostenidos con respuestas ficticias. Comprueba ciclos repetidos, admisión anterior al plazo, cierre de todos los hilos, reglas temporales y ausencia de procesos al terminar. Estos puntos son **objetivos de la prueba, todavía no resultados aprobados**.

La única invocación de esa validación se detuvo antes de iniciar JMeter al detectar un proceso previo. Las pruebas puramente locales de JavaScript sí pasaron: inicio 999 queda en rampa, inicio 1000 en sostenido, inicio 3000 en cierre; una respuesta cruzada se contabiliza por final para recepción; intervalos consecutivos no se consideran solapados. XML y PowerShell se analizaron sintácticamente; el reloj Groovy y el comportamiento temporal no llegaron a compilarse/ejercitarse por esa detención.

Evidencias en `../diagnosticos/e6-sostenido-ficticio-20261111-153457-ab0a9736b1464c80b6f36b5c7b7606d0/`: `error-seguro.jsonl`, `validador-utilizado.ps1` y `observacion-procesos-posterior.json`. La lectura posterior observó `docker.exe`, PID 35180, `docker_stats`. No se conserva el PID del instante exacto del rechazo, por lo que no se demuestra que sea el mismo proceso. No se terminó ninguno ni se ignoró el bloqueo. Se mejoró después el registro de etapa/PID para futuras validaciones; no se reejecutó.

## Comandos futuros; no ejecutados contra backend

Primero, solo tras resolver la concurrencia existente y con autorización:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\validar-e6-sostenido.ps1 -JMeterHome "$env:LOCALAPPDATA\Programs\JMeter"
```

Solo si esa validación completa pasa, más aislamiento/salud/límites/respuesta normal:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e6-sostenido.ps1 -Fase 100 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

El ejecutor mantiene controles previos normales, cantidades/huellas de las ocho tablas antes/después, recursos y tres controles funcionales posteriores fuera del JTL. No inicia/reinicia servicios ni repite carga. Ante preparación, recursos, integridad, recuperación, autenticación o contrato inesperados conserva errores y detiene progresión. Los incumplimientos de duración no cambian el umbral. Una invocación Java 0 no significa que las muestras pasen.

El informe actual debe leerse como implementación bloqueada: no hay muestras reales, recursos de backend/PostgreSQL, integridad de una nueva ejecución ni recuperación posterior que reportar. La última corrida real sigue siendo la parcial de 500 GET. Fases 200/300 y E7 no se ejecutaron. Acceso directo, base en tmpfs, preparación que calienta datos, misma máquina y coste del generador limitan futuras conclusiones; si el solapamiento observado resulta bajo se informará sin declarar sobrecarga cubierta.
