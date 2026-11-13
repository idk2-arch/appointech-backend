# Diagnóstico de la detención anterior a Java en E6

## Alcance y conclusión

No se ejecutó el plan JMeter, ninguna corrida de carga ni autenticación. **Logins: 0; GET: 0; HTTP: 0.** Los dos intentos anteriores se conservaron íntegros.

La causa exacta del intento histórico `e6-20261111-103815-12ea678c8fa4455f985e3a9828a6e741` no puede recuperarse: su manejador eliminó mensaje, línea y ErrorRecord. Sí se consiguió una nueva detención observable, sin HTTP: a **2026-11-11T15:58:17.9807694Z** se detectó `docker.exe`, PID **40980**, clasificado como `docker_stats`. El ejecutor bloqueó correctamente la etapa `procesos`, en `ejecutar-e6.ps1:36`, antes de aislamiento, autenticación o Java.

**No se atribuye retroactivamente ese PID al intento histórico.** Es la causa demostrada del diagnóstico actual. Se detuvieron las comprobaciones al encontrarlo; no se mató el proceso ni se reintentó después.

## Reconstrucción del intento histórico

Su `diagnostico.json` muestra `codigoJMeter=null`, `procesoJavaLanzado=false`, `muestrasMedidas=0`, `fallo=true` y tipo `System.Management.Automation.RuntimeException`. La preparación duró 444 ms y no existe `estado-antes.json`.

El archivo de estado se escribe inmediatamente después de acreditar aislamiento. Por tanto, el fallo quedó acotado a:

1. Carga de helpers y ensamblado .NET, resolución de JMeter/Java y dependencias, lectura del XML.
2. Consulta CIM y rechazo de procesos JMeter/recolector concurrentes.
3. Docker Compose, etiquetas/red/puerto/JDBC/tmpfs y salud; consulta del estado y escritura inicial del JSON.

No había llegado a leer expectativas/integridad para esa invocación ni a autenticar, crear paquete de tokens, iniciar recursos o Java. No se necesita una preparación autenticada para investigar este tramo y no se ejecutó.

El manejador de aquella versión retenía únicamente etapa genérica y nombre del tipo. El `catch` final reemplazaba el error por `E6 detenido...`. La versión posterior a ese intento guardaba mensajes propios `E6:`, pero seguía sustituyendo los de proveedores por una frase genérica; tampoco preservaba todos los errores de helpers.

## Cambios realizados

### ErrorRecord seguro conservado

Nuevo `errores-e6.ps1`, usado por ejecutor y preparación:

- Conserva fecha UTC, etapa, tipo original, mensaje saneado, FullyQualifiedErrorId saneado, categoría, nombre del archivo, línea, columna y cadena de tipos/mensajes de excepciones internas.
- No serializa ErrorRecord completo, TargetObject, ErrorDetails, PositionMessage, texto de la línea, ScriptStackTrace, Exception.Data ni variables privadas.
- Redacta valores secretos conocidos, JWT, Bearer, campos de contraseña/token, correos, cuerpos de login y rutas de paquetes privados. No imprime las coincidencias.
- Escribe `errores-seguros.jsonl` antes de abandonar la etapa; el diagnóstico principal contiene el mismo registro útil. El error final comunica la primera causa saneada, no solo una frase genérica.
- Los helpers repropagan el ErrorRecord de transporte/autenticación después de conservar una versión segura. Una limpieza del paquete no reemplaza la excepción original de creación.

### Detector de procesos

Se encontró además un defecto reproducible del detector antiguo: buscar `ApacheJMeter|jmeter` en cualquier línea Java rechazaba una validación Groovy que solo incluyera bibliotecas de JMeter en el classpath. La expresión PowerShell del recolector también aceptaba menciones posteriores al argumento de otro script.

Nuevo `procesos-e6.ps1`: reconoce invocación del JAR o clase principal de JMeter y el archivo ejecutado del recolector, manteniendo detección de Docker stats. Solo persiste nombre, PID y motivo; no guarda líneas de comandos. No se desactiva el rechazo de una carga o recolector real. Ocho casos ficticios verificaron positivos reales y negativos, incluidos ambos falsos positivos antiguos. Este defecto demostrado tampoco se presenta como causa histórica confirmada.

### Diagnóstico sin HTTP

Nuevo switch `-DiagnosticoSinHttp` en el mismo ejecutor. Reutiliza rutas, dependencias, XML, procesos, aislamiento, límites, SQL de expectativas y huellas. Prueba arranque de Java solo con `-version` y finaliza antes de autenticar. No puede llegar al bloque de JMeter ni del recolector. Guarda evidencia en `perf/diagnosticos/`, separada de resultados medidos.

No cambiaron mezcla 40/40/20, rampa 20 s, diez ciclos de cinco GET, umbrales 800/800/300 ms, timeouts 5.000/10.000 ms, recursos, BCrypt o backend.

## Validaciones y evidencias

### Primer recorrido, completado sin HTTP

`../diagnosticos/e6-local-20261111-105437-06a499c328d24e179930983d7dbcc2a6/fase-100/`

- Dependencias reales y rutas con espacios verificadas.
- Aislamiento y servicios saludables confirmados, límites sin cambios.
- Cohorte de 100 y ocho tablas de integridad leídas en transacciones READ ONLY; cantidades y huellas antes/después coincidentes.
- Java arrancó con `-version` y código 0, usando ProcessStartInfo con ruta ejecutable separada de argumentos y sin ventana.
- JMeter y recolector no iniciados; logins/GET/HTTP: cero.
- Evidencias: [comprobaciones-locales.json](../diagnosticos/e6-local-20261111-105437-06a499c328d24e179930983d7dbcc2a6/fase-100/comprobaciones-locales.json), `diagnostico.json`, `estado-antes.json`, `integridad-antes.json`, `integridad-despues.json`, `integridad.json`.

### Pruebas locales ficticias, completadas

`../diagnosticos/e6-pruebas-locales-20261111-105805-f36f5922ca284911ac67c0b27c01948c/`

- Parser Windows PowerShell **5.1.26100.9711**, sin errores en los cinco scripts modificados/nuevos.
- Binding del bloque de parámetros real: acepta fase 100 y rutas con espacios, rechaza fase 999. Solo se evaluó el bloque de parámetros, no el cuerpo del ejecutor.
- Ocho casos de clasificación de procesos, todos correctos. Los motores JMeter y recolectores reales siguen rechazándose.
- ErrorRecord ficticio con dos niveles: mantiene tipo, detalle útil y línea; elimina contraseña/correo/cuerpo ficticios. No se guardan objetos privados.
- Arranque de PowerShell con `exit 0`, sin script de negocio, sin ventana y sin HTTP.
- Evidencias: [validacion.json](../diagnosticos/e6-pruebas-locales-20261111-105805-f36f5922ca284911ac67c0b27c01948c/validacion.json), `errores-esperados.jsonl`.

### Recorrido posterior al ajuste de procesos, detenido

`../diagnosticos/e6-local-20261111-105817-81e1ee9d19f34959a1090fd06fe8ca9b/fase-100/`

El diagnóstico se detuvo en la detección de Docker stats, antes de consultar servicios o preparar credenciales. Código 1. El nuevo registro conservó:

| Campo | Valor |
|---|---|
| Etapa | procesos |
| Tipo | System.Management.Automation.RuntimeException |
| Mensaje | E6: hay JMeter o recolector concurrente. |
| Archivo / línea | ejecutar-e6.ps1 / 36 |
| Proceso | docker.exe, PID 40980, docker_stats |
| Java/JMeter/HTTP | No iniciados / 0 HTTP |

Evidencias: [errores-seguros.jsonl](../diagnosticos/e6-local-20261111-105817-81e1ee9d19f34959a1090fd06fe8ca9b/fase-100/errores-seguros.jsonl), [procesos-detectados.json](../diagnosticos/e6-local-20261111-105817-81e1ee9d19f34959a1090fd06fe8ca9b/fase-100/procesos-detectados.json), `diagnostico.json`, `preparacion.json`.

No se reintentó después del fallo ni se inició autenticación. La validación del cambio de diagnóstico queda respaldada por este ErrorRecord completo; el recorrido completo con el detector nuevo quedó bloqueado y no se presenta como terminado.

## Reproducción y pendientes

Pruebas locales sin servicios/HTTP:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\diagnosticar-e6-local.ps1
```

Modo de diagnóstico del ejecutor, sin HTTP/JMeter (ya se ejecutó; no repetir mientras exista un recolector activo):

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e6.ps1 -Fase 100 -DiagnosticoSinHttp -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Falta el ErrorRecord/PID original del intento de las 10:38 para atribuir su causa exacta. Un estado actual o un PID posterior no lo reemplaza. También falta acreditar un recorrido completo con el detector nuevo cuando no haya procesos concurrentes; no se intentará hasta resolver esa condición. No hace falta reducir validaciones ni cambiar rendimiento para ello.

El diagnóstico completo previo confirmó perf saludable y sin cambios; la última comprobación se detuvo antes de volver a consultar salud, por lo que no se afirma una salud medida después de ese bloqueo. No se inició, detuvo o reinició ningún servicio ni se mató el proceso detectado. **E6 sigue sin medición válida; no se lanzó fase 100, 200, 300 ni otro escenario.**
