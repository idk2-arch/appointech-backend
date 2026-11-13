# E5: saturación progresiva del login

**Fases 50, 100, 200 y 300 ejecutadas una vez cada una: [resultados](INFORME-E5.md).** La revisión estática original se conserva en REVISION-E5.json; no representa el estado de ejecución posterior.

E5 envía exclusivamente POST /api/auth/login con JSON correo/contrasena. Valida HTTP exactamente 200, JSON válido sin contenido adicional, exito booleano true y datos.token cadena no vacía. Mantiene Duration Assertion de **2.000 ms**, timeout de conexión **5.000 ms** y respuesta **10.000 ms**. No cambia BCrypt, contratos, negocio ni recursos. El login inspeccionado consulta usuario/perfil, verifica BCrypt y emite JWT; no registra cuentas ni actualiza último acceso/refresh tokens.

## Configuración propuesta y orden

| Fase | Usuarios/hilos configurados | Rampa | Loops por hilo | Solicitudes previstas |
|---|---:|---:|---:|---:|
| 1 | 50 | 10 s | 20 | 1.000 |
| 2 | 100 | 10 s | 20 | 2.000 |
| 3 | 200 | 10 s | 20 | 4.000 |
| 4 | 300 | 10 s | 20 | 6.000 |

Solo cambia N: mismos endpoints, protocolo, timeouts, umbral, loops, rampa, preparación, controles y recursos. Veinte loops aportan más observaciones que los cinco de E1 y mantienen demanda después de arrancar cada hilo. Rampa diez segundos conserva la referencia temporal de E1; al aumentar N aumenta la velocidad de incorporación. No es una repetición directamente comparable con E1 ni E4: cambian el diseño y la mezcla de tráfico.

**Sin scheduler, duración adicional ni temporizadores.** Cada fase acaba cuando sus hilos completan veinte muestras o falla el ejecutor; no hay duración fija de sesenta segundos ni se recorta la rampa. La duración real depende de las respuestas/timeouts y se medirá. Es carga cerrada: cada hilo espera su respuesta antes de la siguiente petición. Si el servidor se ralentiza, también cambia el ritmo de llegada; no es tasa abierta fija ni demuestra capacidad sostenida. El umbral y timeouts no son límites de duración de fase.

## Requisitos y comandos exactos

Desde la raíz, Windows PowerShell 5.1, Java/JMeter 5.6.3 sin plugins, Docker Compose y perf iniciado por separado. El ejecutor usa PSScriptRoot, rutas absolutas y argumentos separados; admite espacios. Resuelve Java con JAVA_HOME o PATH y JMeter con -JMeterBat o PATH. No inicia, detiene, elimina ni reconstruye contenedores.

**Comando recomendado cuando se autorice ejecutar las cuatro fases:**

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase Todas -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Ejecuta 50 → recuperación → 100 → recuperación → 200 → recuperación → 300 → recuperación. Cualquier condición de parada impide las siguientes; nunca repite una fase automáticamente. Aceptar un umbral mayor después de ver resultados no forma parte del protocolo.

Para autorizar y ejecutar **solo una fase**:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase 50 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase 100 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase 200 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e5.ps1 -Fase 300 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Estos cuatro comandos son alternativas de fase individual, **no un bloque para pegar sin comprobar resultados**. Una invocación individual no conoce la historia de invocaciones anteriores; antes de la siguiente hay que revisar código de salida, parada.json y recuperación. Para parada automática de toda la secuencia, usar Todas. No mezclar fases individuales con Todas para duplicar carga accidentalmente.

Otra ruta de instalación se indica, por ejemplo, con `-JMeterBat "C:\Herramientas de carga\JMeter\bin\jmeter.bat"`; es un ejemplo de sintaxis, no una ubicación detectada. No se pasan secretos en argumentos. -Credenciales admite una ruta explícita; por defecto usa perf/jmeter/clientes.csv. El destino está fijado a **http://localhost:18080** y el ejecutor no ofrece 8080/desarrollo como alternativa.

## Datos, preparación y seguridad

Antes de cada fase se reutiliza Confirmar-E2Aislamiento de preparar-e2.ps1 sin modificarlo: etiquetas de proyecto/servicio, salud, red exclusiva, publicación de 18080 por backend-perf, JDBC db-perf/appointech_perf y tmpfs. No basta que el puerto responda. Una consulta SQL de solo lectura comprueba N usuarios CLIENTE activos con perfil y contraseña, y la cuenta de control. No escribe datos ni entra en el JTL.

CSV UTF-8 con encabezado correo,contrasena, cuentas sintéticas únicas cliente1@perf.test…clienteN@perf.test y **cliente5000@perf.test para control**. No se imprime ninguna contraseña. Cada hilo mantiene una cuenta distinta durante sus veinte iteraciones; las mismas primeras N se reutilizan entre fases. No se necesita una cuenta distinta por petición ni CSV de 6.000 filas. El parser de JMeter CSVSaveService admite comillas y los cuerpos se construyen con Jackson, incluyendo caracteres especiales.

El setup, antes de la rampa, lee el CSV una vez, prepara cuerpos en memoria y ejercita las mismas fuentes Groovy mediante los elementos JMeter y su caché real con una respuesta ficticia. No envía logins de calentamiento. Evita ObjectMapper/serialización/compilación inicial por petición. No usa cientos de variables de entorno ni archivos nuevos con secretos. La reducción efectiva del agrupamiento debe comprobarse en los inicios reales: preparación previa no garantiza una rampa HTTP perfecta.

Token limpiado antes y después de validar cada muestra; no se reutiliza ni guarda. Setup y teardown usan SampleResult.setIgnore(), por lo que -l guarda únicamente los POST medidos. Las comprobaciones de recuperación usan un HttpClient externo a JMeter y van a recuperacion.csv, nunca a ese JTL. No se registran cuerpos, respuestas, Authorization ni tokens; loggers wire/headers/protocol están apagados. El CSV existente queda fuera del directorio de resultados y no se copia. El ejecutor comprueba los artefactos buscando las contraseñas usadas y patrones JWT/Bearer sin imprimir coincidencias.

## Recuperación y criterios de parada

Se registran estado, RestartCount, StartedAt, OOMKilled y límites efectivos antes/después. Se comparan contra el inicio de fase y la sesión: no se atribuyen contadores históricos a esa fase sin diferencia previa. Se detectan también recreación/cambio de IDs y cambios de CPU, memoria o cpuset. No se cambian límites para obtener métricas.

Tras cada fase se comprueba recuperación **fuera del JTL**: hasta aproximadamente sesenta segundos, separación de cinco segundos entre intentos, y tres comprobaciones consecutivas con ambos servicios saludables y un login de control válido. Antes de cada HTTP se vuelve a confirmar aislamiento. El control usa cliente5000, ajeno a los hilos de carga; HTTP tiene timeout total de hasta diez segundos, acotado al tiempo restante. Registra timestamp, duración, código, contenido válido y si superó 2.000 ms, sin token/cuerpo. Un control válido pero lento no falla solo por duración. Se cuentan sus solicitudes por httpIntentado y se excluyen del throughput medido.

**Detener fases siguientes** ante:

- Preparación/dependencias/cuentas inválidas, aislamiento no confirmado o salud no apta antes de cargar.
- Reinicio, recreación, OOM o cambio de límites detectados por estados comparados. No se reinicia automáticamente para continuar.
- Incapacidad de conseguir tres controles consecutivos en el plazo: salud persistentemente mala o autenticación que no se recupera.
- Código de JMeter no cero, errores de su log, JTL incompleto, identidad/iteraciones inconsistentes, falta de dashboard, fallo del recolector/cierre/validación o exposición de secretos.
- Respuesta 401/403 o HTTP 200 con contrato inválido: revisar credenciales/seguridad/contrato antes de aumentar carga.

Los **incumplimientos de duración por sí solos no detienen** la secuencia ni cambian el umbral. HTTP 5xx y errores de transporte se registran como resultados de estrés; se permite avanzar solo si no hay condiciones anteriores y se acredita recuperación. Un código JMeter cero no significa que las muestras pasen. No se reintentan POST medidos: cada envío corresponde a una iteración; los controles posteriores son otra categoría y otra cuenta.

No hay monitor de salud continuo durante el proceso Java: se observa antes/después y durante recuperación. RestartCount/StartedAt ayudan a detectar reinicios intermedios; los episodios transitorios de salud o pausas sin evidencia histórica no se reconstruyen ni se niegan. Si falla un proceso de lanzamiento antes de iniciar carga, se conserva parada.json; no se fuerza JMeter.

## Evidencias y análisis por fase

Cada invocación crea perf/resultados/e5-fecha-UUID/ y subdirectorios fase-50, fase-100, fase-200, fase-300 según lo solicitado. Cada fase tiene configuración, e5.jtl, jmeter.log, reporte/index.html, recursos.csv, límites, estados antes/después, recuperación y privacidad. La sesión conserva resumen.json y, si se detiene, parada.json. Una fase incompleta conserva lo que haya generado; no se rellena con datos inventados.

Se reutiliza recursos-perf.ps1 corregido: disponibilidad mediante muestreo.listo, sin abrir CSV durante escritura; cierre en finally incluso al fallar JMeter; Wait-Job/Receive-Job y validación del CSV cerrado. Sus errores impiden avanzar. Se capturan backend-perf/db-perf mediante los IDs de Compose, timestamp, CPU, memoria usada/límite y porcentaje. La cadencia real puede rondar dos segundos pese al objetivo aproximado de uno. La memoria de contenedor no es heap Java y el máximo muestreado no es un pico absoluto.

analizar-e5.ps1 genera analisis.json, inicios.csv por segundo e hilos.csv. Comprueba N×20 muestras, veinte iteraciones únicas por hilo y solo la etiqueta/URL medida. Informa ventana desde mínimo timeStamp (inicio explícito) hasta máximo timeStamp+elapsed, throughput, media/mediana, p95/p99 por rango más cercano ceil(p×N), máximo, códigos y fallos HTTP/transporte/contenido/duración. Conserva códigos y detalles de transporte originales saneados en el JTL; no clasifica todo HTTP 200 fallido como lento. e5ContenidoValido distingue los HTTP 200 válidos que superan 2.000 ms. Categorías pueden solaparse. Los timeouts miden tiempo hasta abandonar la espera, no final del servidor.

También informa máximo allThreads observado y máximo de muestras HTTP en curso por barrido de intervalos [timeStamp,timeStamp+elapsed), finales antes que inicios al coincidir. **No son usuarios configurados equivalentes a solicitudes simultáneas ni ejecución simultánea demostrada en el servidor.** Recursos se separan en antes/HTTP/después por instante de captura, con número de capturas e intervalos reales. Los datos de recuperación no entran en la ventana HTTP.

Reanálisis local de una fase ya existente, sin enviar HTTP:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\analizar-e5.ps1 -Directorio "C:\ruta real de resultados\e5-sesion\fase-50"
```

La ruta es un marcador de documentación: sustituir por la carpeta efectivamente generada, no inventar una corrida.

## Revisión realizada y pendiente

La revisión previa verificó estáticamente XML, un único HTTP sampler, propiedades con el ejecutor, flags sin cuerpos/cabeceras, umbral, ausencia de timers/scheduler y reutilización del recolector. PowerShell se analizó con Parser de Windows PowerShell 5.1. Groovy se compiló con FileSystemCompiler y bibliotecas locales, **sin ejecutar el script, JMeter ni HTTP durante esa revisión**. Se inspeccionó la firma CSVSaveService.csvReadFile. La ejecución posterior de fase 50 se documenta por separado en INFORME-E5.md.

Pendiente de ejecución: CSV/cuentas vigentes en db-perf, salud/aislamiento efectivos en ese momento, llenado real de caché, distribución de llegadas, cierre ante fallos reales, recursos, errores, recuperación y ausencia de secretos en artefactos futuros. La compilación no sustituye esas pruebas. No se prueban aquí serializaciones locales lanzando una carga encubierta.

Las fases son secuenciales y el backend ya puede estar utilizado; recuperación y control pueden calentar cachés. No se aísla calentamiento, carga de la máquina anfitriona o efecto del orden. Login incluye BCrypt sin cambiar su costo; no se atribuye CPU a BCrypt solo por observar CPU alta. Acceso directo sin nginx y base perf en tmpfs; el entorno no representa durabilidad ni una reparación completa. E1–E4 y sus informes permanecen intactos. No commit, push o cambio de rama.
