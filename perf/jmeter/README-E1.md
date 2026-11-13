# E1 · Login concurrente

E1 mide la latencia y los fallos del login directo al backend perf. Cada hilo hace un POST por iteración usando una cuenta sintética distinta del CSV compartido. El plan usa componentes incluidos en JMeter 5.6.3 y las bibliotecas Groovy y Jackson de esa distribución.

La prueba cubre autenticación por correo y contraseña y emisión de JWT. Quedan fuera nginx, los flujos posteriores al login y la capacidad de otros endpoints. Los usuarios configurados son hilos programados. La concurrencia observada se consulta en el dashboard y en `grpThreads` y `allThreads` del JTL. Esos campos muestran hilos activos al muestrear, que pueden diferir de las peticiones simultáneamente en vuelo.

## Requisitos y aislamiento

Usa Windows PowerShell 5.1, Java compatible con JMeter 5.6.3, `jmeter.bat` y el entorno perf ya iniciado por quien opera la prueba. Desde la raíz, comprueba el proyecto Compose y el destino antes de lanzar E1.

```powershell
docker compose --env-file perf/.env.perf -f perf/docker-compose.perf.yml ps
docker compose --env-file perf/.env.perf -f perf/docker-compose.perf.yml port backend-perf 8080
docker compose --env-file perf/.env.perf -f perf/docker-compose.perf.yml exec -T backend-perf printenv SPRING_PROFILES_ACTIVE DB_URL
```

Comprueba en `perf/docker-compose.perf.yml` que el proyecto es `appointech-perf`, que ambos servicios usan `perf-net` y que `db-perf` monta sus datos en tmpfs. Verifica que el perfil efectivo es `perf` y que JDBC apunta a `db-perf:5432` y a la base de pruebas esperada. Usa el puerto publicado de `backend-perf`, normalmente 18080, o indica el valor efectivo con `-Puerto`. El número de puerto por sí solo no acredita el aislamiento. El ejecutor rechaza el puerto local 8080 del entorno de desarrollo.

## Comandos desde la raíz

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e1.ps1 -Usuarios 20 -Rampa 10 -Loops 5
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e1.ps1 -Usuarios 50 -Rampa 10 -Loops 5
```

Se esperan **100 y 250 solicitudes**, respectivamente, si todos los hilos completan sus cinco iteraciones. El ejecutor busca `jmeter.bat` en PATH o acepta su ruta como localizador de la instalación. Ejecuta el `ApacheJMeter.jar` vecino con `java.exe` de `JAVA_HOME` o PATH. La invocación directa conserva el código de salida de Java y evita la pausa interactiva del lanzador BAT cuando falla. Puedes indicar una ruta con espacios y un CSV alternativo así.

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e1.ps1 -Usuarios 20 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat" -Credenciales ".\perf\jmeter\clientes.csv"
```

El CSV debe estar en UTF-8, con encabezado `correo,contrasena` y suficientes filas de cuentas sintéticas. Para valores con comas o comillas, usa campos CSV entre comillas y duplica las comillas internas. El ejecutor pasa la ruta absoluta como un argumento. El plan serializa los valores en JSON, consume una fila por iteración, no recicla el CSV y detiene el hilo al agotarlo.

La conexión tiene un timeout de 5.000 ms y la respuesta uno de 10.000 ms, ajustables con `-TimeoutConexionMs` y `-TimeoutRespuestaMs`. `-UmbralLoginMs` controla la aserción de duración de 2.000 ms. Ese umbral se evalúa sobre el tiempo transcurrido de la muestra y no cancela la petición.

| Propiedad `-J` del plan | Parámetro PowerShell | Predeterminado |
|---|---|---|
| `protocolo` | `-Protocolo` | `http` |
| `host` | `-Servidor` | `localhost` |
| `puerto` | `-Puerto` | `18080` |
| `usuarios` | `-Usuarios` | `20` |
| `rampa` | `-Rampa` | `10` segundos |
| `loops` | `-Loops` | `5` |
| `umbralLoginMs` | `-UmbralLoginMs` | `2000` ms |
| `timeoutConexionMs` | `-TimeoutConexionMs` | `5000` ms |
| `timeoutRespuestaMs` | `-TimeoutRespuestaMs` | `10000` ms |
| `credenciales` | `-Credenciales` | `clientes.csv` junto al plan, resuelto a ruta absoluta por el ejecutor |

## Resultados

Cada ejecución crea `perf/resultados/e1-<fecha>-<id>/` con `e1.jtl`, `jmeter.log`, `reporte/index.html` y `ejecucion.json`. Este último guarda las propiedades y rutas efectivas sin los valores del CSV. El JTL guarda tiempos, estado, hilos, bytes, URL y el primer mensaje de aserción fallida. Excluye cuerpos, cabeceras y variables de autenticación. Usa la configuración de logging estándar de JMeter y evita habilitar trazas HTTP.

Analiza **toda la ejecución, incluida la rampa**. El recorte de 30 s corresponde a los escenarios mixtos.

- Transporte. `responseCode=TRANSPORT_ERROR` indica que la muestra no recibió un estado HTTP.
- Funcional. Un estado HTTP distinto de 200 o un mensaje de fallo `Contenido` identifica un incumplimiento del contrato. Se comprueban el JSON, el booleano `exito` y la extracción de `datos.token`.
- Duración. Compara `elapsed` con `umbralLoginMs`. En una muestra con varios fallos, `failureMessage` solo muestra el primero. Cuenta los excesos de duración por separado e informa de los solapamientos al comparar categorías.

Un login válido pero lento conserva `responseCode=200`, queda con `success=false` y tiene `elapsed` superior al umbral. El ejecutor comprueba el conjunto de columnas del JTL al terminar.

El código de salida comprueba la ejecución de JMeter. Los fallos de las muestras se consultan en el JTL. El ejecutor también comprueba que se produjeron el dashboard y las muestras previstas. La captura de CPU y memoria queda pendiente para E1. El recolector existente está integrado en el ejecutor mixto y usa nombres fijos de contenedor. Para incorporarla habría que resolver los IDs de `backend-perf` y `db-perf` del Compose perf y muestrear ambos durante la corrida. El ejecutor E1 no gestiona contenedores.

## Comprobación de usuarios y perfiles

El esquema `src/main/resources/db/migration/V1__esquema_inicial.sql` define `usuario`, `cliente` y `tecnico`. Los perfiles enlazan al usuario por `usuario_id`. Ejecuta la misma consulta antes y después, en la misma instancia perf, sin reiniciar la base ni introducir tráfico ajeno. Ajusta usuario y base de los argumentos `-U` y `-d` si el Compose efectivo usa otros valores.

```powershell
$consultaE1 = @'
SELECT 'usuario', count(*), md5(coalesce(string_agg(md5(to_jsonb(u)::text), '' ORDER BY u.id), ''))
FROM usuario u
UNION ALL
SELECT 'cliente', count(*), md5(coalesce(string_agg(md5(to_jsonb(c)::text), '' ORDER BY c.id), ''))
FROM cliente c
UNION ALL
SELECT 'tecnico', count(*), md5(coalesce(string_agg(md5(to_jsonb(t)::text), '' ORDER BY t.id), ''))
FROM tecnico t
ORDER BY 1;
'@

function Leer-EstadoE1 {
    $salida = $consultaE1 | docker compose --env-file perf/.env.perf -f perf/docker-compose.perf.yml exec -T db-perf psql -X -U perf -d appointech_perf -v ON_ERROR_STOP=1 -At
    if ($LASTEXITCODE -ne 0) { throw 'Fallo la consulta de comprobacion en db-perf.' }
    $salida
}

$antesE1 = Leer-EstadoE1
# Ejecuta aqui uno de los comandos E1.
$despuesE1 = Leer-EstadoE1
Compare-Object $antesE1 $despuesE1
```

La salida tiene tabla, cantidad y huella agregada, sin exponer los valores de las filas. Las cantidades iguales por sí solas no detectan actualizaciones. Las huellas permiten detectar diferencias de estado final. Para comprobar escrituras transitorias o cambios revertidos hace falta observar la actividad de la base durante la prueba.
