# E3 · Registro concurrente de clientes

E3 envía un `POST /api/auth/registro/cliente` por hilo. La guía no fija loops para registro. Se eligió una creación por usuario para reconciliar cada petición con un usuario y un perfil. Las corridas previstas son 20 y 40 usuarios con rampa de 10 s, una iteración y cohortes independientes.

## Contrato y datos

`AuthController` expone el registro bajo `/api/auth`. `RegistroClienteRequest` exige nombre y apellido no vacíos, correo válido, contraseña de al menos ocho caracteres, tipo de documento y dirección no vacía. `telefono` y `numeroDocumento` son opcionales en el DTO. Los tipos son `CC`, `CE` y `PASAPORTE`. La dirección se recibe como texto y se persiste directamente. El plan usa CC, documento numérico de 19 dígitos, dirección sintética y teléfono nulo.

El controlador responde HTTP 201 con `exito` booleano verdadero, `datos` con ID de usuario, nombre, apellido, correo, rol CLIENTE y foto nula para un registro nuevo, además de `mensaje` y `timestamp`. Se valida el contrato y se contrasta el ID de la respuesta con el usuario persistido.

`RegistrarClienteUseCaseImpl` comprueba correo, cifra la contraseña con BCrypt y guarda usuario y cliente en una transacción. Una excepción no controlada provoca rollback. La base impone unicidad de `usuario.correo`, `usuario.numero_documento` y `cliente.usuario_id`. El último campo referencia a `usuario.id`. Véanse `V1__esquema_inicial.sql` y `V6__permitir_login_google.sql`. El flujo no llama a proveedores externos. El Compose perf configura notificaciones desactivadas y el perfil perf usa Haversine.

## Ejecución

Desde la raíz del repositorio, con perf saludable:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e3.ps1 -Usuarios 20 -Rampa 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e3.ps1 -Usuarios 40 -Rampa 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Ejecuta 40 únicamente si 20 termina sin fallos funcionales, de transporte, integridad o ejecutor. Los incumplimientos exclusivos de duración quedan registrados. No se encontró umbral específico para registro. Se usa provisionalmente **1.500 ms** a partir del umbral de escrituras del escenario mixto. Los timeouts de conexión y respuesta se parametrizan con `-TimeoutConexionMs` y `-TimeoutRespuestaMs`. Se analiza toda la rampa.

El ejecutor valida el proyecto perf, los contenedores, la red y JDBC. La URL predeterminada es `http://localhost:18080` y se rechaza el puerto local 8080. La preparación genera un ID de corrida, una identidad por hilo y contraseñas sintéticas. Consulta colisiones de correo o documento en modo de solo lectura antes de medir. Las contraseñas pasan a Java por entorno del proceso y se retiran en `finally`. El JTL guarda el ID de usuario de la respuesta como dato de reconciliación. Se excluyen cuerpos y cabeceras.

Cada carpeta `perf/resultados/e3-<fecha>-<id>` contiene la cohorte sin contraseñas, JTL, log, dashboard, límites, recursos y reconciliación por ordinal del hilo. La consulta final busca por correo o documento, incluso si hubo timeout o falta la muestra. La verificación contrasta cantidades globales de usuario, cliente, notificaciones y huérfanos, IDs y campos de cada fila. Los registros quedan en perf para revisión.

## Recursos y alcance

El auxiliar `recursos-perf.ps1` obtiene los IDs reales de Compose y muestrea ambos contenedores secuencialmente mediante `docker stats --no-stream`. Registra el instante de recepción en epoch ms, CPU, memoria usada, límite y porcentaje. Empieza antes de Java y se detiene en `finally`. El límite efectivo se registra desde Docker. La memoria de contenedor incluye más que el heap Java. La frecuencia real depende de cada consulta a Docker.

Las corridas son cortas y secuenciales, con BCrypt incluido, generador y servicios en la misma máquina, acceso directo sin nginx y PostgreSQL en tmpfs con fsync y synchronous_commit desactivados. La comprobación de persistencia constata filas después de las peticiones y no mide durabilidad ante una caída. Las entradas únicas permiten comprobar la ausencia de duplicados en la cohorte. Una carrera por la misma clave requeriría otra prueba.

## Preparación local del generador corregido

Antes de la rampa, un setUp sin HTTP carga los cuerpos ya serializados y expectativas, inicializa Jackson y ejercita las mismas fuentes de preprocesador/postprocesador/aserciones con una respuesta ficticia. Esto llena la caché real de JMeter; activar cacheKey por sí solo no evita la compilación inicial. El tramo previo al POST solo asigna el cuerpo y referencias preparados. No hay barreras ni pausas añadidas.

Setup y teardown llaman a SampleResult.setIgnore(), por lo que el JTL global conserva exclusivamente los POST medidos. Se comprobó tanto contra el simulador como en la nueva corrida real: veinte muestras, sin preparación. `temporizacion-generador.csv` guarda ordinal y tiempos numéricos de preparación/inicio/final HTTP. Se acumula en memoria y se escribe al terminar. El log permite relacionar el arranque de cada hilo sin guardar credenciales.

Validación local, sin backend y en un puerto loopback efímero distinto de AppoinTech:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\validar-generador-e3.ps1 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

El auxiliar usa veinte identidades ficticias, el mismo plan y rampa, comprueba veinte respuestas válidas, separación de llegadas de al menos ocho segundos y máximo dos llegadas por ventana móvil de cien milisegundos. Son criterios del generador para detectar agrupación, no umbrales de AppoinTech. Cierra el servidor y limpia variables en finally; guarda evidencias independientes. No presenta sus latencias como rendimiento del backend.

El ejecutor real añade `cpu-stat-antes.json` y `cpu-stat-despues.json` cuando cgroup v2 está accesible. Los deltas rodean Java, incluyendo preparación y dashboard; no equivalen a contadores exclusivos del tramo HTTP. No se cambian límites ni se añaden agentes. La corrida real usa el comando de veinte usuarios ya documentado; no se ejecuta cuarenta automáticamente.

## Disponibilidad y cierre del recolector

El arranque espera `muestreo.listo`, creado después de la primera captura completa de ambos servicios, y verifica que el Job siga activo. No abre recursos.csv mientras el escritor funciona. Un timeout de disponibilidad o terminación prematura impide iniciar JMeter.

En finally se pide el cierre, se espera el Job, se reciben sus errores y se retira. Solo entonces se lee el CSV y se validan encabezado, tipos, unidades y parejas backend/base por captura. `validacion-recursos.json` conserva el resultado. Los fallos de captura, cierre o validación siguen provocando error del ejecutor; no se deshabilita la captura ni se ignoran errores.

La corrección se verificó en PowerShell 5.1 con captura aislada normal, excepción simulada dentro del try y detección de terminación prematura, sin HTTP. La nueva corrida medida de cuarenta y el intento previo sin JMeter quedan diferenciados en INFORME-E3.md.
