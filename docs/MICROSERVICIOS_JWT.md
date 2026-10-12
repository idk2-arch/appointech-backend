# API externa con microservicios y JWT

## Idea

El backend principal emite el JWT al iniciar sesión. Otros servicios de la arquitectura confían en ese token si lo pueden verificar, y así no hay que volver a pedir usuario y contraseña.

```
Cliente (Angular)
   | Authorization: Bearer <JWT>
   v
Balanceador (nginx)  ->  Backend principal  --(mismo JWT)-->  Microservicio de notificaciones
                                |                                    |
                           PostgreSQL                          verifica la firma
```

## Qué se implementó

`microservicio-notificaciones/` es un servicio Spring Boot independiente, con su propio `pom.xml`, `Dockerfile` y puerto (8081):

- `GET /api/notificaciones/salud`: público, para comprobar que está vivo.
- `POST /api/notificaciones/cita-confirmada`: exige un JWT válido. Registra el evento en su log.

El backend principal lo llama al confirmar una cita (`NotificadorCitaHttpAdapter`), reenviando el JWT del cliente. La llamada se activa con la variable `NOTIFICACIONES_URL`. Si está vacía no se llama, y si el microservicio falla la cita igual queda confirmada y se guarda un log WARN de tipo INTEGRACION.

## Cómo se valida el JWT

Los dos servicios usan el mismo `JWT_SECRET`. El microservicio comprueba la firma HS256 y lee el rol del token, igual que el backend.

Límites de este diseño:
- Compartir el secreto significa que cualquier servicio que lo tenga puede crear tokens. En producción se prefiere firma asimétrica (RS256): un servicio firma con la clave privada y los demás solo verifican con la pública.
- Reenviar el token del usuario sirve para este ejemplo. Entre servicios internos se suele usar un token de servicio propio.

## Probarlo

```bash
docker compose up --build
curl http://localhost:8081/api/notificaciones/salud
curl -i -X POST http://localhost:8081/api/notificaciones/cita-confirmada -H "Content-Type: application/json" -d "{}"
```

El segundo debe devolver 403 sin token. Con un JWT válido en `Authorization: Bearer ...` devuelve 202. Al confirmar una cita desde el frontend, `docker logs appointech-notificaciones` muestra el evento recibido.
