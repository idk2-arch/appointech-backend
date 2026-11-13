# Validación y archivos de esta entrega

## Resultados

- Leído CLAUDE.md del backend y del frontend; no se encontró AGENTS.md en el repo backend.
- No se abrió .env ni .env.prod. No se copiaron secretos al contenido nuevo.
  Una búsqueda del frontend alcanzó accidentalmente su configuración local Geoapify y mostró
  un valor sensible en la salida; se informó al usuario y no se reprodujo en los archivos entregados.
- node k8s/validar-manifiestos.cjs: OK. Revisión de estructura, referencias, puertos/rutas,
  probes, perfiles, recursos y persistencia; no reemplaza validación de esquemas del servidor.
- configurar.ps1 ejecutado en copia temporal con otro host, imágenes, ACME prod y 2 réplicas:
  validación local OK. Los manifiestos originales conservaron sus valores de ejemplo.
- kubectl apply --dry-run=client -f k8s/: bloqueado al consultar OpenAPI/API por falta de
  contexto/credenciales. No se afirma que haya pasado contra Kubernetes.
- Docker Desktop: servidor Linux disponible; kubectl config get-contexts sin contextos.
  No se aplicó en Docker Desktop Kubernetes ni en AWS.
- GHCR: backend y notificaciones existentes; metadatos linux/amd64 comprobados.
  Los tags latest inspeccionados son anteriores al código nuevo: reconstruir antes de desplegar.
  Frontend: ni workflow ni Compose ni Dockerfile declaran un nombre publicado; consulta al nombre
  propuesto devuelve denied (no distingue privado de inexistente).
- Maven completo: 1229 pruebas, 0 fallos, 1 error en AppointechBackendApplicationTests.contextLoads:
  falta DB_URL; PostgreSQL rechaza el placeholder de URL sin resolver.
- Maven excluyendo solo ese test de contexto: 1229 pruebas, 0 fallos, 0 errores, BUILD SUCCESS
  (el total incluye la nueva prueba HTTP SaludControllerTest).
  Comando: .\mvnw.cmd -B '-Dtest=!AppointechBackendApplicationTests' test.
  Repetir suite completa con PostgreSQL y variables configuradas por el operador; sin leer .env.
- git diff --check en archivos de la entrega: sin errores de whitespace.
- git check-ignore: .env.prod, secret.yaml, secretos reales, PEM y dumps cubiertos.
- Revisión del contenido nuevo: placeholders o datos públicos de despliegue; sin claves reales.
  La búsqueda de patrones de token GitHub/llaves privadas en los nuevos archivos k8s dio cero.
  .dockerignore de ambos servicios excluye también .env.*, PEM, dumps y logs.

## Archivos creados

- k8s/05-frontend.yaml
- k8s/06-ingress.yaml
- k8s/infra/clusterissuers.yaml
- k8s/secret.example.yaml (plantilla comentada y List vacía para no crear secretos al aplicar)
- k8s/secret.env.example
- k8s/configurar.ps1
- k8s/frontend-api.patch
- k8s/validar-manifiestos.cjs
- k8s/DESPLIEGUE-K8S.md
- k8s/VALIDACION.md
- src/main/java/com/appointech/appointech_backend/infrastructure/controllers/SaludController.java
- src/test/java/com/appointech/appointech_backend/infrastructure/controllers/SaludControllerTest.java

## Archivos modificados

- k8s/01-configuracion.yaml, 02-postgres.yaml, 03-backend.yaml, 04-notificaciones.yaml.
- .gitignore; .dockerignore; microservicio-notificaciones/.dockerignore.
- Dockerfile; microservicio-notificaciones/Dockerfile: permiten que JAVA_TOOL_OPTIONS controle heap.
- .github/workflows/ci.yml: plataformas linux/amd64 explícitas.
- src/main/resources/application.properties: Google client ID por variable.
- src/main/java/com/appointech/appointech_backend/infrastructure/config/SecurityConfig.java:
  origen CORS por entorno y permiso GET de salud.

## Archivos retirados

- k8s/05-ingress.yaml: sustituido por 06-ingress.yaml con frontend/API/TLS.
- k8s/06-autoescalado.yaml: se escala manualmente en esta instancia.
- k8s/07-cronjob-limpieza-logs.yaml: la aplicación ya contiene su tarea de limpieza.

k8s/00-namespace.yaml ya existía y se conserva.
Se preservaron los cambios previos del usuario; no se cambiaron dominio, casos de uso ni migraciones.
Hexagonal/SOLID: salud y seguridad en infraestructura; sin lógica de negocio nueva.
12 factores: configuración externa, Secret separado, logs a stdout y pods de aplicación sin estado.
No se editó Compose ni el repositorio frontend. Su cambio queda como parche aplicable.
No se hicieron commit, push, ramas, publicación de imágenes ni despliegues.

## Decisiones manuales

Nombre/tag y publicación de frontend; reconstruir las tres imágenes para amd64.
Host DuckDNS, correo ACME y versión cert-manager compatible con el Kubernetes real.
Configurar Google OAuth y su mismo client ID en frontend/backend.
Elegir SMTP, claves ORS/Groq opcionales y bucket/rol S3 para fotos.
Cambiar el administrador inicial y preparar cuentas/franjas desde el panel.
Verificar visibilidad GHCR y crear imagePullSecret solo si hace falta.
Probar con staging; pasar a prod cuando DNS y HTTP-01 estén confirmados.

