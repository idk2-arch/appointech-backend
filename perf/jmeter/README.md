# Carga y estrés con Apache JMeter

## Diseno vigente: tokens previos y regimen sostenido

El setUp Thread Group autentica secuencialmente N clientes, con N igual a los usuarios del mayor escalon,
y guarda solo tokens sinteticos en `perf/jmeter/tokens.csv` (ignorado por Git). Tambien prepara un token de
administrador. Los hilos de lectura/escritura conservan un token del CSV durante toda su vida y no hacen login.
Un grupo independiente, cercano al 2 % de usuarios/trafico, prueba login con la misma rampa suave.

El plan principal ejecuta 4x/6x/8x. Con base 30 son exactamente 120/180/240 usuarios (el reparto compensa
redondeos). Configuracion de la medicion: 30 s de rampa y arranque, mas 60 s de meseta por escalon.
Se excluyen los primeros 30 s, incluyendo toda la rampa; el arranque se reporta aparte. No se cambia el backend.

El ejecutor usa `-UsuariosBase 30 -RampaEscalonSeg 30 -CalentamientoSeg 30 -DuracionEscalonSeg 90`.
La credencial de admin sigue entrando por entorno/parametro y nunca por una propiedad `-J` registrada.
N, ruta CSV y ventanas se calculan automaticamente; no hay que pasarlos mediante `-Propiedades`.

Resultados finales: `sostenido.md`, `sostenido.json`, `fases.csv`, `desglose-fases.csv`, `ventanas.csv`,
`pool.csv`, recursos y JTL. El reporte principal aplica las mismas ventanas a peticiones, CPU y pool.
Clasifica las peticiones por instante de inicio e incluye respuestas tardias, para no censurar la cola de latencia. La marca temporal procede
de un sampler al final del setUp, despues de preparar todos los tokens; no incluye la preparacion en los percentiles.
El limite <1 % se evalua sobre el sostenido, incluyendo el grupo separado de login. `resumen.md` y
`metricas.json` conservan los totales sin filtrar como referencia, no son la tabla de regimen sostenido.

Para una verificacion breve se pueden reducir duracion, rampa y calentamiento, manteniendo calentamiento
mayor o igual que rampa y meseta positiva. Los tokens se regeneran en cada ejecucion y no se imprimen.

## Diseno anterior (historico)

El generador actual produce carga, estres x1/x2/x4/x6/x8 y el auxiliar x1/x2/x6/x8.
Cada usuario virtual autentica una vez, conserva identidad y token, y despues ejecuta su escenario.
El antiguo grupo dedicado a login constante se redistribuye a lectura publica. Un Throughput Controller
global por lectura permite login ocasional al 2,5 %; su proporcion real se informa separada de los logins iniciales.
Todos los hilos de admin usan su propia sesion. El login de preparacion comprueba y publica el token.

Umbrales por peticion: especialidades y conteo de notificaciones 300 ms; disponibilidad, listado de
notificaciones y lecturas de admin 800 ms; crear solicitud y confirmar 1500 ms; login 2000 ms.
El limite de errores es menor que 1 % en cada escalon, incluyendo los logins iniciales.

`ejecutar-jmeter.ps1` guarda cada ejecucion en una carpeta `realista-<prueba>-<fecha>` nueva, con
`resumen.md`, `metricas.json`, `resumen.csv`, `desglose.csv`, JTL, recursos y dashboard HTML.
No borra resultados anteriores. Recibe la credencial mediante `ADMIN_CONTRASENA` o su parametro,
y la transmite a JMeter por entorno, sin registrarla como propiedad `-J`.
Los ejemplos y tablas historicos que siguen describen el escenario anterior; para regenerar los
planes actuales usa `node perf/jmeter/generar-planes.js`.

No reserialices los planes con formato XML que inserte espacios en propiedades vacias: `filename`
de JSR223 debe ser una cadena vacia; de lo contrario JMeter busca un archivo de script inexistente.
El generador emite estas propiedades como etiquetas autocerradas, tambien en el plan auxiliar.

Herramienta principal de rendimiento de AppoinTech. Todo corre contra el entorno **aislado** (`perf/docker-compose.perf.yml`): base PostgreSQL temporal (tmpfs) con 5000 clientes, 50 técnicos, franjas de 14 días y algunas citas. Nunca toca tu base de desarrollo.

> `perf/k6/` quedó de una primera versión con k6; ya no es el entregable principal y no se amplía.

## Archivos

| Archivo | Qué es |
| --- | --- |
| `appointech-carga.jmx` | Plan de carga: 6 grupos de hilos (uno por escenario) con rampa de subida y meseta. |
| `appointech-estres.jmx` | Plan de estrés: escalones x1, x2, x4, x8 de `usuarios_base` (24 grupos con retardo de inicio). |
| `clientes.csv` | 5000 clientes (`cliente1@perf.test`…) y su contraseña de prueba, para el CSV Data Set Config. |
| `ejecutar-jmeter.ps1` | Ejecuta sin interfaz, muestrea `docker stats`/logs del pool y escribe el resumen por escenario y escalón. |
| `generar-planes.js` | Regenera los `.jmx` y el CSV (`node perf/jmeter/generar-planes.js`). Los `.jmx` son XML estándar y se pueden editar. |

## Requisitos (Windows / PowerShell)

1. **Java 17 o superior** (`java -version`).
2. **JMeter 5.6.x**: `winget install DEVCOM.JMeter` (queda en `%LOCALAPPDATA%\Programs\JMeter`) o descarga el ZIP de <https://jmeter.apache.org/download_jmeter.cgi>. Agrega su carpeta `bin` al PATH:
   ```powershell
   $env:Path += ";$env:LOCALAPPDATA\Programs\JMeter\bin"     # solo esta terminal
   jmeter.bat --version
   ```
   (Con el ZIP, `jmeter.bat` está en `<carpeta>\bin`.)
3. **Docker Desktop** y el entorno perf levantado: `powershell -File perf\iniciar.ps1` (API en `http://localhost:18080`).
4. La clave del administrador sembrado por la migración V4 (no se guarda en el repositorio): pásala con `-Jadmin_contrasena=...` o `-AdminContrasena`.

## Abrir los planes en modo gráfico (solo para revisarlos)

```powershell
jmeter.bat -t perf\jmeter\appointech-carga.jmx
```
No ejecutes la prueba real desde la interfaz: consume memoria y distorsiona los tiempos. Los listeners de árbol (*View Results Tree*) y el *Simple Data Writer* vienen **deshabilitados**; el `.jtl` lo escribe la opción `-l` de la línea de comandos (usarlos a la vez escribiría dos veces el mismo archivo).

## Cómo funciona el plan

- **HTTP Request Defaults** (host, puerto, protocolo, timeouts) y **HTTP Header Manager** (`Content-Type: application/json`).
- **setUp Thread Group**: un solo login del administrador; el token (`$.datos.token`, JSON Path Extractor) se publica como propiedad `adminToken`.
- **Escenarios** (cada uno con su Thread Group; el reparto es el % de los usuarios configurados):

| Escenario | % | Qué hace |
| --- | --- | --- |
| `lectura_publica` | 15 | `GET /api/especialidades` |
| `login` | 10 | `POST /api/auth/login` con la siguiente fila del CSV |
| `disponibilidad` | 25 | login una vez por hilo; `GET /api/disponibilidad/franjas-con-motivo` y `GET /api/disponibilidad/franjas` |
| `crear_confirmar` | 10 | consulta franjas, elige una al azar (JSR223), `POST /api/solicitudes` y `POST /api/citas/confirmar` (acepta 200 o 409: otra persona tomó la franja) |
| `notificaciones` | 20 | `GET /api/notificaciones` y `GET /api/notificaciones/no-leidas/conteo` |
| `admin` | 20 | `GET /api/admin/usuarios?page&size`, `GET /api/admin/tecnicos/{id}/franjas`, `GET /api/admin/solicitudes-manuales` con el token del setUp |

- Los hilos de cliente hacen login con el CSV, extraen `datos.token` y lo envían como `Authorization: Bearer ${token}`.
- **Aserciones** por petición: *Response Assertion* de código (200/201) y *Duration Assertion* con los umbrales (`umbral_lectura` 500 ms, `umbral_login` 1500 ms, `umbral_escritura` 1500 ms). Una respuesta más lenta que el umbral cuenta como error, por eso el % de errores también refleja lentitud.
- Pausa aleatoria entre peticiones (`pensar_min` 500 ms + hasta `pensar_rango` 1000 ms).
- Etiquetas de las muestras: carga `escenario|acción`; estrés `xN|escenario|acción` (así el reporte separa escalones y escenarios).

## Propiedades (`-J`)

| Propiedad | Defecto | Uso |
| --- | --- | --- |
| `host`, `puerto`, `protocolo` | `localhost`, `18080`, `http` | Destino (backend perf directo, o el balanceador nginx) |
| `admin_contrasena` | — (obligatoria) | Clave del administrador |
| `admin_correo` | `admin@appointech.com` | |
| `usuarios` | 50 | Carga: usuarios en la meseta (se reparten por escenario) |
| `rampa` | 30 | Carga: segundos de subida |
| `duracion` | 120 | Carga: segundos totales **incluida la rampa** |
| `usuarios_base`, `escalon_seg`, `rampa_escalon` | 20, 60, 5 | Estrés: usuarios x1, segundos por escalón, rampa de cada escalón |
| `umbral_lectura`, `umbral_login`, `umbral_escritura` | 500, 1500, 1500 | Duration Assertion (ms) |
| `pensar_min`, `pensar_rango` | 500, 1000 | Pausa entre peticiones (ms) |
| `csv` | `clientes.csv` | Ruta del CSV |

## Ejecución sin interfaz

Directa (la carpeta del backend como directorio actual; borra antes `perf\resultados\carga.jtl` y la carpeta del reporte, JMeter exige que no existan):

```powershell
jmeter.bat -n -t perf/jmeter/appointech-carga.jmx -l perf/resultados/carga.jtl -e -o perf/resultados/reporte-carga -Jusuarios=50 -Jduracion=120 -Jadmin_contrasena=<clave>
jmeter.bat -n -t perf/jmeter/appointech-estres.jmx -l perf/resultados/estres.jtl -e -o perf/resultados/reporte-estres -Jusuarios_base=20 -Jescalon_seg=60 -Jadmin_contrasena=<clave>
```

Con el script (hace lo mismo, limpia resultados anteriores, muestrea recursos y resume):

```powershell
powershell -File perf\jmeter\ejecutar-jmeter.ps1 -Prueba carga  -AdminContrasena <clave> -Propiedades usuarios=50,rampa=30,duracion=120
powershell -File perf\jmeter\ejecutar-jmeter.ps1 -Prueba estres -AdminContrasena <clave> -Propiedades usuarios_base=20,escalon_seg=60
```

Salidas en `perf\resultados\`: `carga.jtl`/`estres.jtl`, `reporte-carga\index.html`/`reporte-estres\index.html`, `resumen-jmeter-<prueba>.md` y `recursos-jmeter-<prueba>-<fecha>.csv`.

## Cómo leer el reporte HTML (`index.html`)

- **Dashboard → APDEX**: satisfacción por etiqueta (satisfecho ≤ 500 ms, tolerable ≤ 1500 ms; el script ya fija esos umbrales). Cerca de 1.0 es bueno.
- **Statistics**: por etiqueta, número de muestras, % de error, promedio, **percentiles 90/95/99**, máximo y throughput. Compara el p95 contra los umbrales (lecturas < 500 ms, login < 1500 ms).
- **Errors**: tipos de error (409 esperados en `confirmar cita` no son errores; *Duration Assertion* = respuesta más lenta que el umbral; 500/timeouts = problema real).
- **Charts → Over Time → Response Times Over Time / Active Threads Over Time**: en el estrés, la curva de tiempos que se dispara al subir de escalón es el punto de quiebre. **Throughput**: si deja de crecer al subir usuarios, el sistema se saturó. **Response Time Percentiles** y **Response Times vs Threads** completan el análisis.
- En el estrés, ordena *Statistics* por etiqueta: `x1|…`, `x2|…`, `x4|…`, `x8|…` permiten ver en qué escalón suben errores o tiempos. El resumen `.md` ya lo calcula (primer escalón con errores > 1 %, con errores ≥ límite y con p95 sobre el umbral).

## Ver recursos mientras corre (`docker stats`)

En otra terminal:

```powershell
docker stats appointech-perf-backend-perf-1 appointech-perf-db-perf-1
docker logs -f appointech-perf-backend-perf-1 | Select-String "LENTO|Connection is not available|waiting="
```

- **CPU del backend**: el límite es `-Cpus` de `iniciar.ps1` (2 por defecto); `CPU %` puede llegar a 200 %: cerca del límite = CPU agotada.
- **MEM %** del backend cerca de 100 % = memoria (límite `-Memoria`, 1g).
- **Pool de conexiones**: líneas `perf-pool - … active=10, waiting=N` (waiting > 0 = hilos esperando conexión) y `Connection is not available, request timed out after 5000ms` (pool agotado; tamaño con `-PoolMax`).
- `[LENTO]` marca los métodos de casos de uso, controladores y adaptadores que superan 300 ms (`-UmbralLentoMs`).
- `ejecutar-jmeter.ps1` automatiza esto: la tabla "Recursos por escalón" da el máximo de CPU/memoria/pool por escalón y el recurso que llegó al límite.

## Resultados reales de una corrida local (referenciales)

Entorno: backend perf con 2 CPU, 1 GB, pool de 10 conexiones; base temporal; todo en el mismo PC con Docker Desktop y JMeter en la misma máquina. Cifras de **esta** máquina, no extrapolables a AWS.

**Carga** — 30 usuarios, subida 10 s, 60 s en total, 1697 peticiones, errores 0 %, p95 global 57 ms:

| Escenario | p95 | Errores | Umbral p95 |
| --- | --- | --- | --- |
| lectura_publica | 6 ms | 0 % | 500 ms |
| login | 72 ms | 0 % | 1500 ms |
| disponibilidad | 37 ms | 0 % | 500 ms |
| crear_confirmar | 34 ms | 0 % | 1500 ms |
| notificaciones | 7 ms | 0 % | 500 ms |
| admin | 42 ms | 0 % | 500 ms |

**Estrés** — `usuarios_base=30`, 45 s por escalón (30, 60, 120 y 240 usuarios), 11 084 peticiones:

| Escalón | Usuarios | p95 global | Errores | CPU backend máx. | Pool esperando máx. |
| --- | --- | --- | --- | --- | --- |
| x1 | 30 | 56 ms | 0 % | 23 % | 0 |
| x2 | 60 | 56 ms | 0 % | 38 % | 0 |
| x4 | 120 | 211 ms | 2.45 % | 100 % | 61 |
| x8 | 240 | 7766 ms | 81.98 % | 100 % | 200 |

Punto de quiebre: los errores superan el 1 % en **x4 (120 usuarios)** y el 10 % en **x8**; el p95 de varios escenarios pasa su umbral en x8 (≈ 5 s = el `connection-timeout` de 5 s del pool). Recursos al límite: **CPU del backend (2 núcleos) y pool de 10 conexiones**; la memoria se mantuvo en ~53 %. Con `usuarios_base=10` (10–80 usuarios) no hubo quiebre (0.08 % de errores; el x8 ya mostraba CPU 100 % y 17 esperando en el pool). Resúmenes completos: `perf/resultados/resumen-jmeter-estres-base30.md` y `-base10.md`.

## Hallazgos del backend vistos en las pruebas

- `GET /api/admin/usuarios?page=…&size=…` **ignora la paginación** y devuelve todos los usuarios (≈1.2 MB con 5050): es el endpoint de admin más pesado.
- Cada petición puede usar **dos conexiones a la vez** (la del request y la de `RegistroLogAdapter`, que escribe el log en la base con `REQUIRES_NEW`), lo que agota el pool de 10 antes de lo esperado (hipótesis coherente con los datos; se confirma subiendo `-PoolMax` y repitiendo).
- `bcrypt` en el login es costoso en CPU (costo 10): con muchos logins simultáneos domina el uso de CPU.

## Limitaciones

Los números locales son referenciales: JMeter, Docker, el backend y la base comparten el mismo equipo y su CPU; la base es tmpfs con `fsync` apagado; sin balanceador nginx ni red real. La prueba seria debe hacerse en AWS con el generador de carga en otra máquina.

## Antes y después de las mejoras de rendimiento

Misma prueba (`usuarios_base=30`, 45 s por escalón: 30/60/120/240 usuarios, 2 CPU, 1 GB, base perf recién creada). Cada corrida es una sola ejecución: diferencias pequeñas son ruido, y el x1 incluye el arranque en frío de la JVM.

| Configuración | x4 (120 usuarios): p95 / errores | x8 (240 usuarios): p95 / errores | Errores de conexión al pool |
| --- | --- | --- | --- |
| Antes (pool 10) | 211 ms / 2,45 % | 7766 ms / 81,98 % | 765 |
| Log asíncrono + paginación (pool 10) | 540 ms / 3,76 % | 5151 ms / 67,51 % | 389 |
| Mismo cambio con pool 30 | 194 ms / 0,19 % | 1827 ms / 33,3 % | 0 |
| **Todo** (+ consulta de técnicos sin N+1 e índice V23), pool 10 | 306 ms / 1,73 % | 1269 ms / 15,14 % | 0 |
| **Todo**, pool 30 | **122 ms / 0,64 %** | **1081 ms / 9,6 %** | 0 |

Conclusiones: el mayor efecto vino de eliminar el N+1 al buscar técnicos por especialidad (las consultas a `tecnico_especialidad` bajaron de ~79 000 a ~10 800 por corrida) y de subir el pool; el log asíncrono y la paginación por sí solos no movieron x4 porque el cuello de botella sigue siendo la CPU del backend (100 % desde x4). Con todo aplicado y pool 30, x8 queda justo bajo el 10 % de errores.
