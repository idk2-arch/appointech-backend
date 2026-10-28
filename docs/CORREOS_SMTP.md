# Correos por SMTP (microservicio de notificaciones)

El microservicio envía correos reales en dos eventos. El backend principal lo llama con el mismo JWT del usuario:

| Evento | Endpoint del microservicio | Asunto |
|---|---|---|
| Cita confirmada | `POST /api/notificaciones/cita-confirmada` | Tu cita en AppoinTech fue confirmada |
| Cita finalizada (con diagnóstico) | `POST /api/notificaciones/cita-finalizada` | Tu servicio fue finalizado |

Ambos exigen JWT válido (sin token responden 401) y validan `correoCliente` (obligatorio, con formato de correo; si no, 400).
Si el envío falla, el microservicio registra un WARN (sin contraseñas ni contenido del correo) y responde 202 igual:
un correo caído nunca revierte ni bloquea el cambio de estado de la cita.

## Estructura del microservicio (hexagonal)

Paquete raíz `com.appointech.notifications`. Las rutas HTTP y los nombres de los campos JSON siguen en español porque son el contrato con el backend; el código interno está en inglés.

```
NotificationsApplication              (arranque; el component scan parte de aquí)
domain/                               sin Spring ni jakarta.mail
  AppointmentConfirmedEvent, AppointmentCompletedEvent
  ports/EmailSender                   puerto de salida
application/                          sin Spring
  EmailNotificationService            arma el texto en español y usa el puerto; atrapa los fallos de envío (WARN)
infrastructure/
  controllers/NotificationController  rutas /api/notificaciones/*; traduce JSON (@JsonProperty) a eventos de dominio
  controllers/Appointment*Request     cuerpos HTTP con Bean Validation (correoCliente obligatorio y con formato de correo)
  adapters/SmtpEmailSender            adaptador SMTP con JavaMailSender
  security/JwtFilter, SecurityConfig  JWT compartido; sin token responde 401
  config/NotificationConfig           registra EmailNotificationService como bean (la capa application no usa anotaciones de Spring)
```

`HexagonalArchitectureTest` falla si `domain` o `application` importan Spring, jakarta.mail o `infrastructure`.
En el backend principal: `NotificadorCitaPort.notificarCitaFinalizada` lo implementa `NotificadorCitaHttpAdapter`,
y `ActualizarEstadoCitaUseCaseImpl` lo invoca al pasar a FINALIZADA, después de guardar el historial.

## 12 factores: configuración por entorno

Todo el correo se configura con variables de entorno (factor III); ningún valor ni secreto va en el código ni en archivos versionados.
El servidor SMTP es un servicio de apoyo reemplazable (factor IV): cambiar de proveedor es cambiar variables.

| Variable | Uso |
|---|---|
| `MAIL_HOST`, `MAIL_PORT` | Servidor SMTP (STARTTLS, normalmente 587) |
| `MAIL_USER`, `MAIL_PASSWORD` | Credenciales (con Gmail, una contraseña de aplicación) |
| `MAIL_FROM` | Remitente que verá el cliente |

Defínelas en `.env` (ignorado por git; ver `.env.example`); `docker-compose.yml` las pasa al servicio `notificaciones`.
Si `MAIL_HOST` está vacío el servicio arranca igual, pero cada envío falla con un WARN y no se manda correo.
En Kubernetes añade las mismas variables al Deployment `notificaciones` (la contraseña en el Secret).
