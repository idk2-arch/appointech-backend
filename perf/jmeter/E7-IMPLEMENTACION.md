# E7 CRUD: implementacion restringida a 50 hilos

Nota posterior: se autorizo e implemento la [continuacion de 100 hilos](E7-CONTINUACION-100.md). El resultado de 50 documentado aqui es historico y se conserva sin repetirlo; la ampliacion no permite 150 ni E8.

Nota posterior adicional: se autorizo la [fase unica de 150](E7-CONTINUACION-150.md), sin repetir las anteriores. El lanzador y validador aceptan esa fase explicitamente; no existe progresion automatica a E8. Las restricciones de 50 y 100 descritas en sus notas son historicas.

Implementa el [diseno inspeccionado](../diseno/E7-CRUD-FRANJAS.md). No cambia codigo de negocio, recursos Docker ni los umbrales de otros escenarios. No admite fases de 100 o 150.

## Componentes

- `e7-crud.jmx`: un grupo de 50 hilos, arranque de 20 s, barrera local de 60 s y 20 ciclos de seis pasos: POST, GET, PUT, GET, DELETE, GET. Autenticacion fuera del JTL.
- `e7-ficticio.jmx`: la misma estructura de bucles y postprocesador, sustituyendo el sampler HTTP por un servidor en memoria. No contiene samplers HTTP y su observador SQL es ficticio.
- `e7-motor.groovy`: construye solo las rutas y cuerpos del contrato inspeccionado; extrae el ID de la respuesta raiz, valida identidad y contenido, conserva el ID por ciclo y registra intenciones/resultados/omisiones sin cuerpos ni secretos.
- `validar-e7.ps1`: XML, PowerShell, JavaScript, compilacion Groovy, contratos congelados y cinco ejecuciones ficticias. Publica una puerta de validacion con hashes. Si falla no permite carga real.
- `ejecutar-e7.ps1`: aislamiento, exclusividad, limites de referencia, 50 tecnicos reales distintos, reservas SQL sin cruces, huellas, autenticacion y una sola corrida de 50; no repite escrituras ni hace limpieza de residuos inciertos.
- `recursos-e7.ps1`: escritor persistente con FileShare.ReadWrite; prepara cada par completo antes de escribirlo, cierra el job y valida el CSV. No altera el recolector ni los artefactos E6.
- `analizar-e7.js`: concilia 120 pasos realizados u omitidos por hilo, IDs, diario, checkpoints y ciclos; informa por cada operacion codigos, contenido, duracion media/p95/p99, timeouts y omisiones.

## Integridad por ciclo y efecto de la instrumentacion

Despues de cada POST y PUT valido, antes de permitir el siguiente paso, un observador agrupa solicitudes y ejecuta exclusivamente SQL `READ ONLY` dentro de `db-perf`. Comprueba ID, tecnico, fecha, horas, `creada_por` y preservacion de `creada_en`. Despues de DELETE verifica ausencia del ID y del dia reservado del tecnico. Se guardan los resultados en `conciliacion-sql.jsonl` antes de liberar al hilo.

Estas lecturas son necesarias para comprobar estados que ya no existiran al acabar el ciclo. **La prueba esta instrumentada**: el tiempo `elapsed` del sampler es HTTP, pero la duracion total y el ritmo incluyen las esperas de conciliacion SQL, procesos Docker y diario local. No se presenta ese ritmo como throughput CRUD sin instrumentacion. Cada hilo tiene un tecnico distinto; todos usan la misma identidad administrativa existente. La precision de `creadaEn` de la respuesta admite hasta 1 microsegundo de redondeo al persistir; la conservacion del timestamp almacenado se comprueba exactamente entre checkpoints SQL.

El SQL usa `statement_timeout=5s` solo para la transaccion de lectura del observador; no modifica configuracion global ni timeouts del backend. Conserva 5000 ms de conexion y 10000 ms de respuesta HTTP. Desactiva reintentos HTTP y redirecciones. E7 no tenia un SLA de duracion CRUD acordado: reporta la duracion, sin crear una nueva asercion ni copiar los umbrales de lecturas E6. Watchdog local de JMeter: 3600 s; no es un umbral SLA.

Ante respuesta de escritura incierta, fallo de contenido o checkpoint: no se admiten mas pasos dependientes ni operaciones nuevas. Los ya admitidos terminan; los demas se registran omitidos. No se reintenta POST/PUT/DELETE. Al acabar se consulta el estado reservado hasta 60 s; cualquier incertidumbre queda explicitamente pendiente, incluso si se observa ausencia. No hay DELETE posterior de residuos ni restauracion de datos previos. Si la integridad o los checkpoints fallan, no se progresa.

Las huellas cubren todas las tablas publicas de negocio (cantidades y contenido canonico); se excluyen `log_sistema`, por su crecimiento asincrono esperado, y metadatos Flyway. No se resetean secuencias. Recuperacion y autenticacion no aparecen como muestras CRUD.

## Validacion ficticia

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/validar-e7.ps1 -JMeterHome "$env:LOCALAPPDATA/Programs/JMeter"
```

Casos: normal de 50 hilos con 6000 muestras/1000 ciclos; POST aplicado con timeout (1 muestra, 119 omisiones); PUT aplicado con timeout (3, 117); DELETE aplicado con timeout (5, 115); POST aplicado con respuesta sin ID (1, 119). Los casos inciertos usan un hilo para verificar exactamente la parada de sus 120 pasos previstos. Los IDs ficticios no son correlativos por hilo; no se asume un ID de base ni se reutiliza uno anterior. Se comprueban tambien cierre de JMeter y del observador, pares de recursos y ausencia de procesos propios restantes.

La puerta `E7-VALIDACION-ULTIMA.json` solo es valida para los hashes concretos de los scripts y XML probados. Cualquier cambio requiere validacion antes de HTTP.

## Ejecucion real autorizada

```powershell
# La credencial debe existir de antemano; nunca incluir su valor en la linea de comandos.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File perf/jmeter/ejecutar-e7.ps1 -CredencialesJson 'C:/ruta/privada/credencial.json'
```

El archivo privado debe contener `correo` y `contrasena` de `admin@appointech.com`; tambien se reconocen `ADMIN_CONTRASENA` o `APPOINTECH_JMETER_ADMIN_PASSWORD` del entorno. No se crean cuentas ni se cambian roles o passwords. Sin credencial, o si falla el unico login/control protegido, se detiene sin JMeter real. El JWT queda en un paquete temporal con ACL del usuario, nunca en argumentos Java o XML. Se elimina al cerrar. El login es `datos.token`; la franja creada es `$.id`.

Cada invocacion escribe un directorio nuevo en `perf/resultados/e7-50-*`, y evita reutilizar el horizonte de reservas de invocaciones anteriores. Un fallo no habilita automaticamente otra invocacion: revisar evidencia y respetar la autorizacion de una unica corrida. El lanzador no llama a E6, 100, 150 ni a otras pruebas.

Artefactos: JTL, dashboard, consola Java, diario por hilo, conciliacion SQL, configuracion sin JWT, reservas y prueba de cruces, estado/limites, conteos y huellas antes/despues, recursos validados, recuperacion, logs y diagnostico. El escaneo final redacta cualquier secreto detectado y marca fallo si hubo exposicion. No se leen CSV mientras sus escritores estan activos.

Los resultados de esta sesion se documentan por separado en el directorio de evidencias; esta guia no declara una corrida satisfactoria por el mero hecho de que el plan compile.

## Resultado de la unica corrida real de esta sesion

Directorio: [e7-50-20261111-182840](../resultados/e7-50-20261111-182840-7a34d9f7ac124f42b3bcfbefa82b8472/INFORME.md). Tras la validacion ficticia satisfactoria se comprobo el destino, reservas, limites, identidad y rol. La credencial existente se extrajo como literal mediante el AST de `pruebas-api.ps1:47`, sin ejecutar ese script, sin publicarla y sin crear cuentas. Un solo login, fuera del JTL; los 50 hilos compartieron el administrador existente y cada uno tuvo un tecnico diferente.

6000 muestras validas, 1000 ciclos completos, cero fallos, timeouts, escrituras inciertas u omisiones. Se verificaron tres checkpoints SQL por cada ciclo (crear, editar, borrar), mismo ID/autor y timestamp almacenado conservado; cero filas de la cohorte al terminar. Las cantidades y huellas de 15 tablas de negocio coinciden, incluidas las 1260 franjas previas.

El archivo original `integridad.json` tiene un metadato `tablas=1` que contaba una envoltura PowerShell: ambas instantaneas incluyen las 15 tablas completas. Se preservaron esos originales y se publico `integridad-verificada.json` con la comparacion correcta. Se corrigio el aplanado para siguientes usos y se revalido sin backend, sin repetir carga real. `fuentes-ejecutadas/` y `validacion-previa-utilizada.json` conservan los hashes y fuentes realmente usados en la corrida, antes de esa correccion de metadatos.

Recursos: 40 pares validos, escritor y observador cerrados, JMeter codigo 0. La captura cubre la ventana CRUD, drenaje y dashboard; el recolector cerro antes de los controles posteriores, por lo que no se dispone de CPU/memoria de la recuperacion. Tres controles de recuperacion correctos en 12, 10 y 16 ms. Sin exposiciones detectadas; paquete privado eliminado. Se conservaron JTL, dashboard, recursos, logs y E1-E6. No se ejecutaron 100 ni 150.
