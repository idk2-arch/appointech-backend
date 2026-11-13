# Despliegue de presentación de AppoinTech

Los manifiestos usan JSON, formato válido de YAML para Kubernetes. No contienen secretos.
No ejecutar todavía contra AWS sin preparar imágenes, DNS y secretos. No se ha desplegado desde esta sesión.

## Qué cambia y qué falta

Una EC2 amd64 con k3s, Traefik y local-path; PostgreSQL 15-alpine (igual que Compose), PVC de 5 Gi.
Una réplica por aplicación. No se instala autoescalado: en esta máquina se escala manualmente.
El CronJob anterior se retira porque la aplicación ya tiene LimpiezaLogsJob.
Si ya aplicaste los recursos antiguos, elimina solo HPA y CronJob:
`kubectl -n appointech delete hpa backend --ignore-not-found`
y `kubectl -n appointech delete cronjob limpieza-logs --ignore-not-found`.
Cambiar una plantilla PVC existente de 1 Gi a 5 Gi puede requerir migración y backup;
no borrar un StatefulSet/PVC existente para forzar el cambio.

El backend no tenía Actuator ni salud pública. Se agrega GET /api/salud para startup/liveness/readiness.
Es salud del proceso, no chequea PostgreSQL ni proveedores; valida la DB además con /api/especialidades.
No hay context-path: los controladores ya incluyen /api, por eso Ingress no reescribe rutas.
Spring Boot usa configuración base más perfil prod (sin archivo específico): no se activa otro perfil.
Swagger y OpenAPI quedan deshabilitados mediante propiedades Spring. open-in-view permanece false.

IMPORTANTE: reconstruir backend y notificaciones con los Dockerfile modificados antes de usar estos
manifiestos. El latest remoto inspeccionado es anterior a estos cambios: no tiene la nueva salud y
todavía fija heap al 75%. Ahora JAVA_TOOL_OPTIONS fija 70% sin un argumento posterior que lo anule.

Fotos: Compose incluye MinIO. Aquí se elige S3 externo para evitar otra carga persistente en la EC2.
Crear bucket privado, configurar región y preferentemente rol IAM de instancia con acceso limitado al
bucket (GetObject/PutObject/DeleteObject). Endpoint vacío selecciona AWS; access/secret vacíos usan el rol.
Sin bucket/permiso las fotos fallarán. PostgreSQL no respalda las imágenes: respaldar también S3.

## Frontend e imágenes

Se revisó C:/Users/Juan David/Downloads/appointech-frontend:
Dockerfile ejecuta npm run build; angular.json usa production por defecto.
No hay environment.ts ni runtime config para la API. La constante real está en
src/app/core/config/api.config.ts y hoy contiene localhost.
El workflow .github/workflows/ci.yml solo compila, prueba y analiza; no publica imágenes.
Compose usa build y FRONTEND_PATH, sin image. Por tanto NO hay nombre real de imagen GHCR
deducible de esos archivos. ghcr.io/idk2-arch/appointech-frontend:latest es solo un nombre propuesto:
la consulta al registro devolvió denied; no confirma existencia ni visibilidad.

Aplicar desde PowerShell en el repositorio frontend (no se ha editado ese repositorio):
```powershell
git apply "C:\Users\Juan David\Downloads\appointech-backend\appointech-backend\k8s\frontend-api.patch"
npm.cmd run build -- --configuration production
```
El parche cambia exactamente API_BASE_URL a '/api'. resolverUrlFoto ya resuelve rutas usando
document.baseURI, compatible con este cambio. Actualizar los tests de fotos que esperan localhost
para esperar el origen del navegador de pruebas. Revisar el bundle desplegado: en DevTools Network
las llamadas deben ir a https://HOST/api, nunca localhost. Borrar datos del service worker antiguo
si sigue sirviendo el bundle previo.
Geoapify es del frontend: GEOAPIFY_API_KEY se prepara durante el build con el mecanismo existente
de CI. No es variable del backend ni se inyecta en Kubernetes. Su valor acaba visible al navegador:
restringir su uso al dominio desde el proveedor. No copiar archivos locales de claves al repo backend.

Publicación manual cuando decidas hacerlo (no ejecutada aquí):
```powershell
docker login ghcr.io
docker buildx build --platform linux/amd64 -t ghcr.io/idk2-arch/appointech-backend-backend:presentacion --push .
docker buildx build --platform linux/amd64 -t ghcr.io/idk2-arch/appointech-backend-notificaciones:presentacion --push ./microservicio-notificaciones
# Ejecutar desde el repo frontend, después de preparar su configuración de build:
docker buildx build --platform linux/amd64 -t ghcr.io/idk2-arch/appointech-frontend:presentacion --push .
```
La configuración Geoapify existente debe estar disponible al build según el Dockerfile del frontend;
no versionar su archivo local. Preferir tags inmutables por versión o digest.
Se pudo inspeccionar backend y notificaciones actuales en GHCR: ambos existen y son linux/amd64.
El workflow backend declara ahora platforms: linux/amd64 para que las siguientes publicaciones
también lo garanticen. No se ejecutó ningún push ni publicación.
Los nodeSelector impiden ejecutar los pods en ARM; no convierten una imagen ARM a amd64.
Verificar cada imagen antes del despliegue:
`docker buildx imagetools inspect IMAGEN --format '{{.Image.os}}/{{.Image.architecture}}'`.

## Variables: nombre → recurso (sin valores)

Backend → ConfigMap appointech-config:
`DB_URL`, `DB_USER`, `DB_NAME`, `NOTIFICACIONES_URL`, `GOOGLE_CLIENT_ID`, `CORS_ORIGINS`, `ALMACENAMIENTO_ENDPOINT`, `ALMACENAMIENTO_BUCKET`, `ALMACENAMIENTO_REGION`, `GROQ_MODEL`, `GROQ_BASE_URL`, `GROQ_TIMEOUT_MS`, `HAVERSINE_VELOCIDAD_KMH`, `RUTA_MARGEN_MINUTOS`, `RUTA_TOLERANCIA_MINUTOS`, `APP_ZONA_HORARIA`, `FORWARD_HEADERS_STRATEGY`, `CONTACTO_ADMIN_TELEFONO`, `PASARELA_PAGOS`, `PASARELA_SIMULADA_APROBAR`, `PAGOS_UMBRAL_DIFERENCIA_PORCENTAJE`, `PAGOS_URL_RETORNO`, `CALIFICACION_PLAZO_DIAS`, `REGISTRO_LOG_COLA_MAXIMA`, `SPRING_PROFILES_ACTIVE`, `SPRINGDOC_API_DOCS_ENABLED`, `SPRINGDOC_SWAGGER_UI_ENABLED`, `JAVA_TOOL_OPTIONS`, `TZ`.

Backend → Secret appointech-secretos:
`DB_PASSWORD`, `JWT_SECRET`, `ORS_API_KEY`, `GROQ_API_KEY`, `ALMACENAMIENTO_ACCESS_KEY`, `ALMACENAMIENTO_SECRET_KEY`.

Notificaciones → ConfigMap: MAIL_HOST, MAIL_PORT, MAIL_FROM, JAVA_TOOL_OPTIONS, TZ.
Notificaciones → Secret: JWT_SECRET (el MISMO que backend), MAIL_USER, MAIL_PASSWORD.
Se usan referencias individuales para no entregar claves DB/S3/Groq al servicio de correo.

PostgreSQL: POSTGRES_DB ← ConfigMap DB_NAME; POSTGRES_USER ← ConfigMap DB_USER;
POSTGRES_PASSWORD ← Secret DB_PASSWORD. DB_URL debe conservar el mismo nombre de base.
DB_USER es identificador, no contraseña; se eligió ConfigMap. GOOGLE_CLIENT_ID también es público.
Las variables SPRINGDOC_* y SPRING_PROFILES_ACTIVE son propiedades estándar Spring por entorno.
CORS_ORIGINS es leída ahora por SecurityConfig; GOOGLE_CLIENT_ID por application.properties.
NOTIFICACIONES_URL corresponde al binding relajado de notificaciones.url ya usado por el adaptador.
No existe GEOAPIFY en backend. ORS_API_KEY y GROQ_API_KEY son opcionales.
MAIL_PORT debe ser un entero; SMTP vacío deja el correo sin envío.
ALMACENAMIENTO_* corresponde al adaptador S3 existente.
PASARELA_PAGOS conserva el modo por defecto del código; decidir qué proveedor funcional mostrar.
FRONTEND_PATH, PUERTO_EXTERNO_API y PUERTO_EXTERNO_FRONTEND son controles de Compose, no de la app;
no se trasladan a pods.

## Preparar configuración en Windows

Desde el repo backend:
```powershell
./k8s/configurar.ps1 -HostPublico appointech.duckdns.org -CorreoAcme TU_CORREO_ACME -ImagenFrontend ghcr.io/idk2-arch/appointech-frontend:presentacion -ImagenBackend ghcr.io/idk2-arch/appointech-backend-backend:presentacion -ImagenNotificaciones ghcr.io/idk2-arch/appointech-backend-notificaciones:presentacion -Acme staging
```
Este es el único punto de parametrización de host, imágenes, réplicas y entorno ACME.
Edita también 01-configuracion.yaml para GOOGLE_CLIENT_ID, correo SMTP y bucket/región S3.
No introducir secretos en ese archivo. El host actual de ejemplo es appointech.duckdns.org.

Crear .env.prod LOCALMENTE copiando k8s/secret.env.example fuera de k8s/.
Completar con editor sin mostrarlo en consola. Usar JWT aleatorio de al menos 32 bytes y contraseña
DB fuerte. Este archivo solo contiene las ocho claves de la plantilla: NO copiar toda una configuración
de Compose con DB_URL/NOTIFICACIONES_URL apuntando a otros hosts.
Formato KEY=valor, sin export, sin comillas, UTF-8 sin BOM, sin valores multilínea ni interpolación.
Las claves opcionales pueden quedar vacías, pero las ocho deben existir. No ejecutar Get-Content,
cat, kubectl get secret -o yaml ni comandos que impriman valores.
.env.prod, secret.yaml, *.pem y *.dump están ignorados. Nunca poner secretos reales dentro de k8s/:
scp -r copia también archivos ignorados.
secret.example.yaml es un bloque comentado con placeholders y una List vacía: apply no crea
un Secret de mentira ni sobrescribe el real.

## Probar primero Docker Desktop Kubernetes

En Docker Desktop habilitar Kubernetes, esperar a que esté listo:
```powershell
kubectl config get-contexts
kubectl --context docker-desktop get nodes
kubectl --context docker-desktop get storageclass
```
En esta sesión Docker funciona, pero NO hay contextos Kubernetes: no se aplicó en local.
Para probar, instalar Traefik y cert-manager en el clúster local antes de aplicar sus recursos.
Traefik puede instalarse con Helm:
`helm repo add traefik https://traefik.github.io/charts`,
`helm repo update`,
`helm install traefik traefik/traefik --namespace traefik --create-namespace`.
Docker Desktop normalmente usa hostpath, no local-path: en una COPIA temporal de k8s/
cambiar storageClassName por el StorageClass local comprobado.
En esa copia configurar host appointech.local con configurar.ps1 y añadir 127.0.0.1 appointech.local
al archivo hosts de Windows como administrador.
Usar siempre --context docker-desktop. Crear namespace y Secret con los pasos siguientes.
Para prueba local quitar la anotación cert-manager.io/cluster-issuer del Ingress websecure:
HTTP-01 público no funciona con un host local. Traefik servirá certificado no confiable;
usar curl.exe -k https://appointech.local/api/salud solo en esta prueba.
No instalar los ClusterIssuer de producción en local.
No alterar el PVC productivo para ajustarlo a Docker Desktop.

## Preparar AWS y copiar

En DuckDNS apuntar el registro A a 18.225.122.32; eliminar AAAA si no hay IPv6 funcional.
Abrir inbound TCP 80 y 443 al público en Security Group; 22 solo desde tu IP.
No abrir 5432, 8080 ni 8081. Verificar espacio en disco, amd64 y versión real del clúster.
En Google Cloud autorizar origen JavaScript https://HOST para el cliente OAuth y configurar
la pantalla de consentimiento/usuarios de prueba cuando corresponda. El ID debe coincidir con el frontend.
No agregar localhost como origen de producción.

PowerShell (reemplazar ruta de llave):
```powershell
$LlaveEc2 = "C:\ruta\llave.pem"
ssh -i "$LlaveEc2" ubuntu@18.225.122.32 "mkdir -p ~/appointech"
scp -i "$LlaveEc2" -r ./k8s ubuntu@18.225.122.32:~/appointech/
scp -i "$LlaveEc2" ./.env.prod ubuntu@18.225.122.32:~/appointech/.env.prod
ssh -i "$LlaveEc2" ubuntu@18.225.122.32
```
No se copia el resto del repo ni ningún .env de desarrollo.

## Prerrequisitos TLS en Ubuntu (SSH)

Elegimos cert-manager con HTTP-01: separa renovación y estado del certificado del pod Traefik,
sin modificar su chart ni guardar acme.json en un volumen propio. Consume unos pods adicionales;
en esta EC2 cabe con las solicitudes acotadas. El solver usa su propio Ingress HTTP sin redirect.
Fuentes: https://cert-manager.io/docs/configuration/acme/http01/ y
https://cert-manager.io/docs/usage/ingress/ .
Consultar https://cert-manager.io/docs/installation/kubectl/ y elegir versión compatible con
la versión REAL de Kubernetes antes de instalar; no asumir que un release antiguo soporta k3s 1.37.
```bash
cd ~/appointech
chmod 600 .env.prod
# Usar sudo k3s kubectl en toda esta guía para no copiar el kubeconfig administrador.
sudo k3s kubectl get nodes -o wide
sudo k3s kubectl get storageclass
sudo k3s kubectl get ingressclass
sudo k3s kubectl get crd middlewares.traefik.io
# Sustituir VERSION_COMPATIBLE por la versión elegida en la documentación oficial.
sudo k3s kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/VERSION_COMPATIBLE/cert-manager.yaml
sudo k3s kubectl -n cert-manager rollout status deployment/cert-manager --timeout=180s
sudo k3s kubectl -n cert-manager rollout status deployment/cert-manager-webhook --timeout=180s
sudo k3s kubectl -n cert-manager rollout status deployment/cert-manager-cainjector --timeout=180s
sudo k3s kubectl apply -f k8s/infra/clusterissuers.yaml
```
Sustituir email antes de aplicar. El script configurar.ps1 ya lo prepara.
No se usa HelmChartConfig para ACME. Traefik debe tener entrypoints web y websecure de k3s.

## Secret, imágenes privadas y aplicación

```bash
sudo k3s kubectl apply -f k8s/00-namespace.yaml
sudo k3s kubectl -n appointech create secret generic appointech-secretos --from-env-file=.env.prod
```
Para reemplazarlo sin imprimir valores:
`sudo k3s kubectl -n appointech create secret generic appointech-secretos --from-env-file=.env.prod --dry-run=client -o yaml | sudo k3s kubectl apply -f -`.
No usar tee ni redirección a archivos versionados.

Si imágenes públicas funcionan, no hace falta imagePullSecret. Si son privadas, generar PAT classic
con SOLO read:packages y autorizar SSO si aplica. No poner el token literal en el historial:
```bash
read -rp "Usuario GHCR: " GHCR_USER
read -rsp "Token GHCR read:packages: " GHCR_TOKEN; echo
sudo k3s kubectl -n appointech create secret docker-registry ghcr-lectura --docker-server=ghcr.io --docker-username="$GHCR_USER" --docker-password="$GHCR_TOKEN"
unset GHCR_TOKEN GHCR_USER
```
El argumento se expone brevemente al listado de procesos del servidor; ejecutar en tu instancia privada.
Agregar imagePullSecrets: [{name: ghcr-lectura}] en spec.template.spec de LOS TRES Deployments
antes de aplicar; este ajuste queda en manifiestos y sobrevive a reaplicaciones.
Alternativa: hacer públicos los tres paquetes si así lo decides.

```bash
sudo k3s kubectl apply --dry-run=client -f k8s/
sudo k3s kubectl apply -f k8s/
sudo k3s kubectl -n appointech rollout status statefulset/postgres --timeout=180s
sudo k3s kubectl -n appointech rollout status deployment/backend --timeout=600s
sudo k3s kubectl -n appointech rollout status deployment/notificaciones --timeout=600s
sudo k3s kubectl -n appointech rollout status deployment/frontend --timeout=180s
sudo k3s kubectl -n appointech get pods,svc,pvc,ingress
sudo k3s kubectl -n appointech logs deployment/backend --tail=80
sudo k3s kubectl -n appointech get certificate,certificaterequest,orders,challenges
sudo k3s kubectl -n appointech describe certificate appointech-tls
curl -I http://appointech.duckdns.org/
curl -k https://appointech.duckdns.org/api/salud
curl -k https://appointech.duckdns.org/api/especialidades
```
Staging produce certificado no confiable: probar allí primero, sin consumir emisiones de producción.
Para certificado confiable, volver a ejecutar configurar.ps1 con -Acme prod, copiar la carpeta actualizada
y reaplicar. Verificar Certificate Ready=True, issuer letsencrypt-prod y acceso SIN -k:
`curl -I https://appointech.duckdns.org/`.
Comprobar COOP same-origin-allow-popups con curl -I y login Google desde el navegador.
Las sondas van directas al pod HTTP; /api/salud público solo devuelve estado del proceso.

## Usuarios demo y franjas

No se incluyen usuarios, contraseñas ni semillas de pruebas.
Desde la interfaz registrar cliente y técnico con datos de presentación elegidos por ti.
Endpoints reales: POST /api/auth/registro/cliente y /registro/tecnico; DTO en RegistroClienteRequest
y RegistroTecnicoRequest. Login: POST /api/auth/login. No registrar cuentas reales de asistentes.
Flyway contiene un administrador inicial: establecer una contraseña propia antes de exponer la demo.
Si desconoces su acceso, registrar una cuenta con contraseña propia y, por una sesión psql interactiva
controlada, asignarle el rol ADMINISTRADOR (identificar su id; nunca cambiar migraciones aplicadas).
Revocar/desactivar el administrador inicial desde gestión de usuarios.

Para promover tu cuenta registrada, abrir psql desde SSH:
```bash
sudo k3s kubectl -n appointech exec -it postgres-0 -- sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```
Dentro de psql, identificar el ID de tu propia cuenta y promoverla:
```sql
SELECT id, rol FROM usuario ORDER BY id;
\prompt 'ID de tu cuenta registrada: ' id_admin
UPDATE usuario SET rol = 'ADMINISTRADOR' WHERE id = :'id_admin'::bigint;
\q
```
Cerrar sesión en la web y volver a entrar para renovar el JWT con el rol actualizado.
Desactivar después el administrador inicial desde el panel. No cambiar hashes ni migraciones.

Con el administrador: asignar especialidades al técnico, verificar ubicación y radio/cobertura,
configurar reglas de franjas y crear franjas para fechas futuras en America/Bogota desde el panel.
Endpoint real POST /api/admin/tecnicos/{tecnicoId}/franjas con cuerpo:
`{"fecha":"AAAA-MM-DD","horaInicio":"08:00","horaFin":"12:00"}`, usando fecha real futura
y Bearer obtenido en login. No guardar el token en archivos versionados.
GET del mismo endpoint confirma las franjas. Sin franjas creadas por el administrador no hay citas.
Comprobar recorrido cliente → solicitud → técnico → cambio de estado y correo (si SMTP configurado).

## Actualizar, escalar y Flyway

Publicar imágenes tú mismo, nunca desde esta guía automáticamente.
Con latest y imagePullPolicy Always:
```bash
sudo k3s kubectl -n appointech rollout restart deployment/backend deployment/notificaciones deployment/frontend
sudo k3s kubectl -n appointech rollout status deployment/backend --timeout=600s
```
Para tags nuevos, cambiar imágenes con configurar.ps1 y reaplicar; reiniciar además si solo cambió
ConfigMap/Secret, ya que el entorno se lee al crear el pod. El nginx montado con subPath también
requiere reinicio al cambiar su ConfigMap.
La estrategia maxSurge=0 evita pods extra por actualización, pero con una réplica habrá interrupción.
Backend requests 512Mi/250m, limits 1Gi/1 CPU; notificaciones 256Mi–512Mi; DB 256Mi–768Mi.
Para dos réplicas: -ReplicasBackend 2 en configurar.ps1 y reaplicar; temporalmente:
`sudo k3s kubectl -n appointech scale deployment/backend --replicas=2`.
Verificar `sudo k3s kubectl top pods -n appointech` y `top nodes`.
Dos réplicas comparten las 2 CPU; no implica doble capacidad. Vigilar conexiones DB y memoria.
Volver a 1 si hay presión; no se instala HPA.

Flyway usa bloqueo de migración en PostgreSQL para serializar el historial al arrancar varias réplicas.
No se desactiva Flyway ni se cambia schema history. Dejar que una réplica termine las migraciones
antes de escalar; la startupProbe tolera hasta 10 minutos. Si excede, aumentar el presupuesto e investigar
el bloqueo. Migraciones deben ser compatibles con la versión que aún atiende tráfico; backup antes de
cambios de esquema. La migración V23 ya estaba en el workspace; no se modificó.

## Backup y restauración (SSH Ubuntu)

Backup consistente de PostgreSQL en formato custom, sin sacar contraseña a consola:
```bash
mkdir -p ~/backups-appointech
chmod 700 ~/backups-appointech
umask 077
sudo k3s kubectl -n appointech exec postgres-0 -- sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc' > ~/backups-appointech/appointech.dump
test -s ~/backups-appointech/appointech.dump
```
Desde PowerShell copiarlo fuera de la instancia:
`scp -i "$LlaveEc2" ubuntu@18.225.122.32:~/backups-appointech/appointech.dump ./appointech.dump`.
No usar redirección binaria de kubectl desde Windows PowerShell 5: puede corromper el dump.
Los backups contienen datos personales; mantenerlos privados.

Restaurar reemplaza los datos actuales: confirmar destino y tener copia antes:
```bash
sudo k3s kubectl -n appointech scale deployment/backend --replicas=0
sudo k3s kubectl -n appointech exec -i postgres-0 -- sh -c 'PGPASSWORD="$POSTGRES_PASSWORD" pg_restore --exit-on-error --clean --if-exists --no-owner -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < ~/backups-appointech/appointech.dump
sudo k3s kubectl -n appointech scale deployment/backend --replicas=1
```
Restaurar preferentemente en base vacía compatible y comprobar logs/Flyway.
Los PVC sobreviven a reinicios de pods, no a perder el disco EC2, borrar PVC o desinstalar k3s.
No cambiar DB_PASSWORD del Secret esperando que PostgreSQL cambie la contraseña ya inicializada:
rotarla en la DB y luego sincronizar Secret y reiniciar backend.

## Diagnóstico

- Pending: describe pod y get pvc; comprobar StorageClass local-path, capacidad y recursos.
- ImagePullBackOff: describe pod; nombre/tag, permiso GHCR y imagePullSecrets. Confirmar amd64.
- CrashLoopBackOff: logs POD --previous; Secret completo, JWT ≥32 bytes, URL DB, Flyway.
- Startup 404/401: imagen backend antigua; reconstruir con /api/salud y seguridad actualizadas.
- 502/503: get endpointslice, readiness, puertos de Services y logs de Traefik en kube-system.
- TLS pendiente: describe certificate, order y challenge; DNS A/AAAA, puerto 80 y solver Traefik.
  No redirect global propio que intercepte /.well-known/acme-challenge/.
- 404 API: no stripPrefix; /api se conserva. Revisar Network del navegador y bundle anterior.
- Google bloqueado: ID coincidente, origen https autorizado y COOP en respuesta nginx.
- SMTP sin correo: revisar MAIL_* y salida al proveedor; algunos proveedores bloquean envíos desde EC2.
- Fotos fallan: bucket, región, rol IAM y acceso al endpoint S3. No poner credenciales en logs.
- Sin citas: fecha/zona, cobertura, especialidad y franjas del técnico.
- OOMKilled: describe pod; consumo y límite. No elevar heap por encima del presupuesto del contenedor.
- No pegar logs completos sin revisarlos; no usar describe Secret para compartir información.

## Desinstalar y dejar de gastar

Hacer backup fuera de EC2 primero y comprobar su restauración.
Solo aplicación: `sudo k3s kubectl delete namespace appointech`; esto puede destruir PVC/datos.
Instalación completa: `sudo /usr/local/bin/k3s-uninstall.sh` (destruye recursos/datos locales).
En consola AWS: terminar la instancia, revisar y borrar EBS/snapshots que no quieras conservar,
desasociar y LIBERAR la IP elástica (detener EC2 no elimina todos los cargos).
Revisar bucket S3, backups y otros recursos facturables, eliminar los exclusivos de la demo.
Retirar DNS DuckDNS, revocar tokens temporales GHCR y accesos de presentación.

## Validación de esta entrega

kubectl existe, pero apply --dry-run=client -f k8s/ no pudo descargar el esquema:
no hay contexto autorizado. Se valida estructura local con validar-manifiestos.cjs.
No se afirma validación del API ni despliegue local/remoto.
Ver VALIDACION.md para resultados finales de pruebas y comprobaciones.

