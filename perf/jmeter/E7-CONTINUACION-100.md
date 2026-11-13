# E7: continuacion restringida a 100 hilos

Esta ampliacion no repite la corrida real de 50. Sus archivos se conservan y su JTL se analiza por separado con `concurrencia-e7.js`; los resultados de ese analisis se escriben fuera de su directorio original.

## Concurrencia observada en 50

6000 muestras en 51,364 s: 116,813 HTTP/s (todas las respuestas HTTP, incluidos 201 y 204). Hilos activos maximos: 50; HTTP simultaneas maximas: 50; media temporal: 19,563. Al menos 10 HTTP durante 68,928% de la ventana; al menos 25 durante 37,369%; exactamente 50 durante 5,928%. Sin HTTP durante 20,725% de la ventana.

La carga tuvo concurrencia efectiva, no fue casi serial. Pero los 50 hilos no estuvieron continuamente haciendo HTTP: el observador SQL produce pausas y rafagas. Sumas por hilo: 1004,824 s de sampler HTTP y 1508,636 s entre muestras; no son segundos de reloj. El diario mide 1487,563 s de postprocesamiento, de los cuales 1481,613 s pertenecen a escrituras (incluyen validacion, cola, SQL e IPC, no SQL puro). No se presenta como saturacion maxima ni throughput sin instrumentacion.

## Cambios de instrumentacion

- `ejecutar-e7.ps1 -Usuarios 100` y `e7-motor.groovy` admiten 100, pero no 150. La puerta inicial comprueba el analisis de 50 y el hash de su JTL, mas la validacion ficticia de la fase solicitada.
- La lista de 50 IDs reales se repite dos veces. Cada tecnico tiene dos hilos; el hilo 51 usa el tecnico del hilo 1, con fechas diferentes. No se crean identidades ni se cambian roles.
- Se elige un horizonte posterior a todas las reservas registradas de E7, incluso las ya borradas o pertenecientes a intentos fallidos. El segundo hilo de cada tecnico usa base+20..39, mientras el primero usa base..19. Se prueban 2000 tuplas distintas antes del HTTP. No se asigna ningun ID de franja: se extrae de cada POST y se verifica su no reutilizacion en el analisis posterior.
- Barrera, 20 ciclos, seis operaciones y checkpoints SQL permanecen. El observador sigue comprobando cada mutacion antes de permitir la siguiente; mismo administrador compartido, misma instrumentacion y mismos limites de transporte.
- Se conserva la captura de recursos durante las consultas finales y los controles de recuperacion; el cierre y la validacion del CSV siguen en finally. No cambian los limites de contenedores.
- Excesos de duracion CRUD son **no evaluables** porque E7 no tiene un umbral SLA acordado. No se inventa uno ni se heredan los umbrales de lecturas E6. Se mantienen conexion 5000 ms y respuesta 10000 ms; p95 y timeouts se informan por separado.

La validacion se ejecuta con backend ficticio (`validar-e7.ps1 -Usuarios 100`) y no envia HTTP. La unica corrida real autorizada debe ir precedida por reservas, aislamiento, salud, limites, ausencia de carga propia y autenticacion fuera del JTL. Cualquier preparacion fallida impide la carga. No se reintentan escrituras inciertas, no se eliminan residuos dudosos y no se ejecutan 150 ni E8.

La cronologia de preparacion y calentamiento se registra; comparar estas corridas secuenciales no aisla causalmente el efecto del numero de usuarios.
