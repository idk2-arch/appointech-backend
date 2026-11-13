# E4: flujo CLIENTE de solicitud y cancelación

E4 mide cinco pasos por iteración: login, especialidades, creación de solicitud, cancelación propia y detalle con historial. Termina en cancelación; no cubre una reparación completa, asignación de técnico ni cobro. Primera configuración: diez usuarios, treinta segundos de rampa y cinco loops; cincuenta flujos y hasta 250 muestras. El ejecutor admite diez o veinte usuarios, rampa de cinco o treinta segundos y cinco loops; conserva diez/treinta como valores predeterminados.

## Requisitos y comando

Windows PowerShell 5.1, Java/JMeter 5.6.3 sin plugins y perf iniciado por separado. Desde la raíz:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e4.ps1 -Usuarios 10 -Rampa 30 -Loops 5 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Puede indicarse otra ruta válida a jmeter.bat, incluso con espacios. Se usa PSScriptRoot y argumentos separados; no hay contraseñas en argumentos. El destino está fijado a localhost:18080, sin opción al nginx de desarrollo en 8080. No inicia, reinicia ni detiene contenedores.

Segunda configuración autorizada, una sola corrida:

```powershell
powershell.exe -NoProfile -File .\perf\jmeter\ejecutar-e4.ps1 -Usuarios 20 -Rampa 5 -Loops 5 -JMeterBat "$env:LOCALAPPDATA\Programs\JMeter\bin\jmeter.bat"
```

Son cien flujos y hasta quinientas muestras. La guía fija diez–veinte usuarios y cinco loops, pero no la rampa. Elegimos cinco segundos porque la primera corrida de diez con treinta segundos registró como máximo un hilo activo. Cambian usuarios y rampa: buscamos concurrencia, no aislamos el efecto de duplicar usuarios. La concurrencia se comprobará en los datos, no se dará por supuesta. No cambian umbrales, timeouts, recursos ni lógica funcional.

El ejecutor valida etiquetas de appointech-perf, salud de backend-perf/db-perf, red exclusiva, tmpfs y JDBC a appointech_perf mediante el helper de E2, sin modificarlo. Verifica variables de servicios externos y perfil empaquetado del contenedor: correo desactivado, Haversine y pasarela simulada. Rechaza overrides/montajes no verificados. Ninguna ruta E4 invoca pago, fotos, IA ni geocodificación.

## Contratos y preparación

1. POST /api/auth/login: correo y contrasena; HTTP 200, exito booleano true y datos.token no vacío. Un login medido en cada iteración; no se pregeneran tokens reales.
2. GET /api/especialidades: HTTP 200 y arreglo raíz con la especialidad esperada.
3. POST /api/solicitudes: especialidadId, descripcionProblema, direccionServicio, latitud y longitud. HTTP 201 y CitaResponse directo en PENDIENTE, sin técnico ni fechas.
4. POST /api/cliente/citas/{id}/cancelar: motivo de hasta 500 caracteres. HTTP 200 y CitaResponse del mismo ID en CANCELADA.
5. GET /api/cliente/citas/{id}: HTTP 200 y CitaDetalleResponse directo, estado CANCELADA e historial con una transición PENDIENTE→CANCELADA, actor de la cuenta y motivo de la iteración. El detalle no tiene clienteId ni especialidadId; la pertenencia se concilia en base.

Umbrales: login 2.000 ms; especialidades 300 ms; creación 1.500 ms; **cancelación 1.500 ms y detalle 800 ms son provisionales**, reutilizados de las categorías existentes de escritura y lectura. Timeouts independientes: conexión 5.000 ms, respuesta 10.000 ms. No se modifican después de ver resultados.

Se verifican en db-perf los diez clientes sintéticos distintos, activos y completos, y una especialidad activa con técnicos activos y costo de referencia positivo. Credenciales desde clientes.csv, sin mostrarlas. Cada hilo mantiene su cuenta; cada loop tiene marca nueva E4-UUID-hNN-iN y solicitud propia. Las cincuenta marcas se comprueban sin colisiones antes de medir. Expectativas persistidas sin credenciales.

Los cuerpos se serializan antes de medir. Un setup sin HTTP carga los paquetes de entorno, inicializa Jackson y ejercita las mismas fuentes Groovy con cinco respuestas ficticias para llenar su caché real. No basta cacheKey por sí solo. El límite conservador del entorno Windows se comprueba por longitud sin mostrar valores. Se limpian variables temporales en finally. Tokens únicamente en memoria de cada hilo.

Setup y teardown usan SampleResult.setIgnore(): el recolector global -l conserva únicamente los cinco tipos de HTTP medidos. No hay barreras, recorte de rampa ni pausas añadidas; la rampa reduce el arranque conjunto, pero debe contrastarse con los inicios reales y no garantiza concurrencia igual a diez.

## Fallos, correlación e integridad

Se limpian token e ID al empezar cada iteración. Un fallo exclusivo de duración con contenido válido permite continuar. Un fallo HTTP, transporte o contenido queda registrado y nunca se borra por una respuesta posterior correcta. Sin ID válido no se cancela ni consulta un ID anterior. Si la cancelación falla con ID conocido, se permite el GET final para observar el estado, sin convertir el flujo en aprobado. Nunca se reintenta un POST.

JTL: e4Marca, e4Hilo, e4Iteracion y e4CitaId permiten correlacionar sin cuerpos o tokens. flujos.json conserva cinco categorías por paso y tiempos, sin credenciales. Se distinguen flujos completos funcionalmente, completos aprobados, completos con fallo de duración e incompletos; los conjuntos se explican en el informe.

Finalmente, consultas SQL de solo lectura reconcilian las cincuenta marcas, incluso las que no recibieron ID: cita única, propietario, especialidad, estado, campos, historial y motivo. Se comprueba el pago PENDIENTE cotizado sin método, monto final, transacción externa ni comprobante. Notificaciones SOLICITUD_RECIBIDA/CITA_CANCELADA se verifican aparte: son posteriores al commit y no pertenecen a la transacción principal. Hasta tres lecturas separadas por un segundo permiten observar avisos posteriores al GET, sin repetir carga ni ocultar ausencias. Se guardan cantidades y huellas agregadas antes/después de usuarios, clientes y citas previas.

No se limpia automáticamente. Las consultas de preparación/reconciliación no son HTTP ni entran en el JTL. Se registra su cantidad por separado. El recolector corregido usa muestreo.listo, termina en finally y solo interpreta recursos.csv después del cierre. Sus errores siguen siendo fallos del ejecutor.

## Revisión y límites

La revisión local `powershell.exe -NoProfile -File target/revisar-e4.ps1` usa identidades ficticias, el mismo plan con loops=0 y puerto loopback 9: ejecuta preparación/aserciones ficticias, sin HTTP medido ni backend. No es una corrida de rendimiento. La distribución real se verifica después en el JTL y log; no se presume validada solo por el XML.

Analizar toda la ejecución. Ventana HTTP: mínimo timeStamp hasta máximo timeStamp+elapsed; throughput=muestras/ventana. Percentiles por rango más cercano ceil(p×N). Las categorías de errores pueden solaparse. Un código de salida cero no implica aceptación. Criterios: contrato y persistencia correctos, umbral de cada muestra y menos del 1 % de muestras fallidas; informar también flujos incompletos/fallidos.

Backend previamente utilizado, misma máquina que JMeter, acceso directo sin nginx, PostgreSQL en tmpfs y durabilidad relajada, captura de recursos discreta. No prueba arranque frío ni capacidad sostenida. Memoria de contenedor no es heap Java; máximos muestreados no son picos absolutos. No cambiar código de negocio, BCrypt, recursos ni informes E1–E3.
