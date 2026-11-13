# E2 · Consulta masiva de notificaciones

E2 mide `GET /api/notificaciones?page=0&size=20` con una identidad cliente distinta por hilo. Cada hilo conserva su token durante 10 iteraciones y hace un GET por iteración. La rampa predeterminada es de 15 s. Los 30 y 80 usuarios producen 300 y 800 muestras respectivamente cuando se completan todos los loops. Se analiza toda la ejecución, incluida la rampa.

## Requisitos y comandos

Desde la raíz del repositorio, usa Windows PowerShell 5.1, Docker Compose, Java y JMeter 5.6.3 con las bibliotecas incluidas. Deben estar saludables los contenedores existentes `backend-perf` y `db-perf`. El ejecutor toma `jmeter.bat` de PATH o de `-JMeterBat` para localizar `ApacheJMeter.jar`. Lanza el JAR con Java en modo no gráfico.

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e2.ps1 -Usuarios 30 -Rampa 15 -Loops 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e2.ps1 -Usuarios 80 -Rampa 15 -Loops 10 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

El destino predeterminado es `http://localhost:18080`. `-Puerto` admite otro puerto local publicado por `backend-perf`. El ejecutor rechaza 8080 y valida etiquetas Compose, estado saludable, publicación IPv4, red exclusiva, perfil `perf`, JDBC a `db-perf:5432/appointech_perf` y montaje tmpfs de la base antes de consultar datos o autenticar. Los comandos Docker del ejecutor se limitan a inspección y SQL de solo lectura. Los servicios se administran por separado.

Las propiedades del plan son `protocolo=http`, `host=localhost`, `puerto=18080`, `usuarios=30`, `rampa=15`, `loops=10`, `timeoutConexionMs=5000`, `timeoutRespuestaMs=10000` y `umbralLecturaMs=800`. Los timeouts se ajustan con `-TimeoutConexionMs` y `-TimeoutRespuestaMs`. El umbral de 800 ms es una aserción sobre el tiempo de la muestra. El parámetro interno `identidadesEnv` identifica el prefijo de variables de entorno de esa ejecución.

## Preparación y medición

1. El auxiliar consulta `usuario`, `cliente` y `notificacion` en una transacción `READ ONLY`. Selecciona `cliente1@perf.test` hasta `clienteN@perf.test` con rol `CLIENTE`, activos y con perfil. Exige tres notificaciones y dos sin leer por identidad. Obtiene sus campos e IDs ordenados por `creada_en DESC, id DESC`.
2. PowerShell inicia sesión secuencialmente con `clientes.csv`. Exige HTTP 200, `exito` booleano verdadero y un token no vacío. Usa cada token en un GET previo al backend y coteja la página y sus IDs con los de esa identidad. El servidor acredita el token mediante el endpoint autenticado. Un fallo en la cohorte o en cualquier verificación impide arrancar JMeter.
3. El ejecutor entrega los paquetes de identidad, rol, token y expectativas a Java por variables de entorno del proceso con un prefijo único. No escribe un CSV de tokens ni incluye valores de autenticación en argumentos. Retira las variables en `finally`. Cada hilo usa su índice de JMeter más uno para seleccionar su paquete y conserva la selección en variables del hilo.
4. Los POST y GET de preparación ocurren en PowerShell antes de lanzar Java. `preparacion.csv` registra ordinal, operación, duración y resultado. `preparacion.json` registra el tiempo total y si se completó. El `-l` global de JMeter solo puede recoger los GET del plan E2, que carece de grupo setUp. La preparación puede calentar el backend.

Las aserciones comprueban HTTP 200, JSON válido con arreglo raíz, tres filas, `X-Total-Count: 3` e IDs positivos, únicos, ordenados y propios del usuario. Cotejan campos contra la base y comprueban el enum `TipoNotificacion`, las cadenas, `leida` booleano con dos falsas, `citaId` nulo o entero positivo y `creadaEn` como fecha y hora local ISO. El contrato del listado está en `NotificacionController` y `NotificacionResponse`. El orden de persistencia está en `NotificacionJpaRepository`.

## Artefactos e interpretación

Cada ejecución crea una carpeta `perf/resultados/e2-<fecha>-<id>` con JTL, log, dashboard HTML, configuración efectiva y registros de preparación. La salida se valida por código de proceso, columnas del JTL, etiqueta, URL, cantidad de muestras y existencia del dashboard. El formato excluye cuerpos, cabeceras y variables de autenticación. Los mensajes de contenido son fijos y los detalles de transporte conservan código y mensaje, con redacción del token y valores Bearer. Mantén desactivadas las trazas HTTP.

Separa los fallos por código HTTP distinto de 200, código no HTTP con detalle de transporte, mensaje de aserción `Contenido` y `elapsed` superior a 800 ms. El JTL CSV guarda el primer mensaje de fallo. Un HTTP 200 fallido puede incumplir contenido o duración, y las categorías pueden solaparse.

## Integridad antes y después

Tras confirmar el aislamiento, ejecuta la misma consulta antes del ejecutor y después de que termine, con la misma base y sin tráfico externo que modifique notificaciones. Ajusta `-U` si el usuario de db-perf difiere del valor predeterminado.

```powershell
$sqlE2 = @'
BEGIN READ ONLY;
SELECT count(*) AS total,
       count(*) FILTER (WHERE leida) AS leidas,
       count(*) FILTER (WHERE NOT leida) AS sin_leer,
       md5(coalesce(string_agg(md5(to_jsonb(n)::text), '' ORDER BY n.id), '')) AS huella
FROM notificacion n;
ROLLBACK;
'@
function Leer-IntegridadE2 {
    $estado = $sqlE2 | docker compose --env-file perf/.env.perf -f perf/docker-compose.perf.yml exec -T db-perf psql -X -q -U perf -d appointech_perf -v ON_ERROR_STOP=1 -At
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la comprobacion en db-perf.' }
    $estado
}
$antesE2 = Leer-IntegridadE2
# Ejecutar aqui una sola de las corridas.
$despuesE2 = Leer-IntegridadE2
Compare-Object $antesE2 $despuesE2
```

La salida contiene solo cantidades y una huella agregada de las filas. La coincidencia permite comparar el estado final, incluidas las marcas de lectura. Los cambios revertidos durante la corrida requieren observación adicional.

## Alcance

E2 consulta tres registros por usuario en el backend directo, sin nginx, con PostgreSQL perf en tmpfs. No incluye captura de recursos. El máximo de hilos activos registrado puede ser inferior a los usuarios configurados y no equivale a las peticiones simultáneas en vuelo. Las corridas cortas caracterizan esta consulta con la cohorte elegida y la preparación previa.
