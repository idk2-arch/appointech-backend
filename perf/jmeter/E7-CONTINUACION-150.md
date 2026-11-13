# E7: unica fase autorizada de 150 hilos

La ampliacion acepta 150 en el lanzador, validador y motor de instrumentacion. No modifica negocio, DDL, identidades, permisos, limites de contenedores ni SLA. Conserva 20 ciclos y el flujo POST, GET, PUT, GET, DELETE, GET. Se esperan 18000 HTTP y 9000 checkpoints SQL para 3000 ciclos completos.

150 hilos comparten la cuenta administrativa existente, fuera de la autenticacion medida, y los mismos 50 tecnicos sinteticos: tres hilos por tecnico. El caso de uso mantiene su bloqueo pesimista por tecnico; la contencion de esa fila es parte del escenario.

Cada hilo utiliza 20 fechas exclusivas: la primera tanda de 50 hilos usa base..base+19, la segunda base+20..39 y la tercera base+40..59. Se elige base despues de todas las reservas de las cohortes E7 anteriores, incluso si fueron borradas. Las 3000 tuplas tecnico/fecha se verifican contra franjas, citas y bloques mediante SQL de solo lectura antes de HTTP. No se reutilizan IDs: cada uno procede del POST; la comparacion posterior detecta reutilizacion con las corridas anteriores.

Antes de enviar HTTP se exige validacion ficticia de la fase y hashes de sus scripts, aislamiento de perf, salud, limites de referencia y ausencia de procesos propios activos. El caso ficticio tiene tres hilos por tecnico, 18000 muestras y 9000 checkpoints; las escrituras inciertas y respuesta sin ID se prueban por separado, sin backend.

Los checkpoints SQL siguen entre operaciones y fuera del elapsed HTTP. Deben verificar propietario, estado creado/editado, preservacion de creada_en y ausencia tras DELETE. No se reintentan escrituras inciertas ni se hace limpieza a ciegas. Tras el cierre hay espera acotada y consultas de conciliacion; cualquier incertidumbre o residuo impide declarar integridad y detiene la progresion.

El recolector permanece activo durante recuperacion; se conservan JTL, dashboard, diario, logs saneados, recursos, hashes antes/despues y fuentes efectivamente ejecutadas. La captura se valida como pares completos. La auditoria esperada y los avances de secuencia no se tratan como tablas de negocio inmutables.

Los resultados de 50 y 100 permanecen intactos, inventariados antes de la ampliacion. Su JTL solo se relee para generar la comparacion en un directorio nuevo. La comparacion informa media, p95 y p99 por operacion, ventana, throughput y evolucion del solapamiento. No aisla un efecto causal del numero de usuarios: los checkpoints SQL afectan el ritmo y las ejecuciones secuenciales pueden tener distinto calentamiento.

Detener despues de la unica corrida real de 150. No repetir 50 ni 100, no repetir automaticamente 150, y no avanzar a E8.
