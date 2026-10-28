# Despliegue: puertos, balanceador, Kubernetes, CI/CD, SonarQube y Vercel

## Levantar todo con Docker Compose

Levanta frontend, backend, notificaciones, balanceador y base de datos con un solo comando. El frontend debe estar en `../appointech-frontend` (o en la ruta de `FRONTEND_PATH`) con su propio Dockerfile.

1. Copia `.env.example` a `.env` y cambia `DB_PASSWORD` y `JWT_SECRET` (son obligatorios).
2. Construye y arranca: `docker compose up -d --build`
3. Abre:
   - Frontend: http://localhost:4200
   - API (por el balanceador): http://localhost:8080/api
   - La base de datos y las notificaciones no se publican; solo se ven dentro de la red `app-network`.
4. Para detener: `docker compose down` (los datos se conservan en el volumen `db-data`; `docker compose down -v` los borra).

## 1. Puertos internos y externos en Docker

El puerto **interno** es el del contenedor. El **externo** es el que abres desde tu computador. En `docker-compose.yml` se escriben como `externo:interno`.

| Servicio | Puerto interno | Puerto externo | Quién lo usa |
|---|---|---|---|
| balanceador (nginx) | 80 | 8080 | El frontend y Postman (`http://localhost:8080/api`) |
| backend | 8080 | no se publica | Solo el balanceador |
| notificaciones | 8081 | 8081 | El backend por la red de Docker; el externo es para pruebas |
| almacenamiento (MinIO, S3) | 9000 | no se publica | Solo el backend, por la red de Docker (bucket de fotos de perfil) |
| PostgreSQL (`appointech-db`) | 5432 | 5432 | El backend (`host.docker.internal:5432`) |
| SonarQube | 9000 | 9000 | Navegador |
| frontend (Angular) | 4200 | 4200 | Navegador |

`expose` abre un puerto solo hacia los otros contenedores. `ports` lo abre hacia tu máquina. Los externos se cambian con `PUERTO_EXTERNO_API` y `PUERTO_EXTERNO_NOTIFICACIONES` en `.env`.

## 2. Balanceador de carga

nginx reparte las peticiones entre las copias del backend. Se crean varias copias así:

```bash
docker compose up --build --scale backend=3
```

En Kubernetes lo hace el Service junto con el Ingress (ver abajo).

## 3. Kubernetes

Manifiestos en `k8s/`: namespace, ConfigMap y Secret, PostgreSQL, backend (2 réplicas), notificaciones, Ingress, autoescalado (HPA) y un CronJob de limpieza de logs.

Prueba local con Docker Desktop (Settings, Kubernetes, Enable) o minikube:

```bash
docker build -t appointech-backend:local .
docker build -t appointech-notificaciones:local ./microservicio-notificaciones
```

Cambia en `k8s/03-backend.yaml` y `k8s/04-notificaciones.yaml` la imagen por `appointech-backend:local` y `appointech-notificaciones:local`. Luego:

```bash
kubectl apply -f k8s/
kubectl get pods -n appointech
kubectl port-forward -n appointech svc/backend 8080:80
```

Para usar el Ingress hace falta un controlador nginx y la entrada `127.0.0.1 appointech.local` en el archivo hosts.

El Secret de `k8s/01-configuracion.yaml` es solo de ejemplo. No subas contraseñas reales a GitHub.

## 4. Integración y despliegue continuo (CI/CD)

Flujo de `.github/workflows/ci.yml`:

1. **construir-y-probar:** levanta PostgreSQL, compila y ejecuta las pruebas con `./mvnw verify`. Corre en cada push y pull request.
2. **SonarQube:** se ejecuta solo si existen los secretos `SONAR_TOKEN` y `SONAR_HOST_URL`.
3. **publicar-imagenes:** en `main` y `desarrollo`, publica las imágenes en GitHub Container Registry (`ghcr.io`).
4. **desplegar-kubernetes:** en `main`, aplica `k8s/` solo si existe el secreto `KUBE_CONFIG` (kubeconfig en base64).

Después de publicar, cambia `OWNER/REPOSITORIO` en los manifiestos por tu usuario y repositorio en minúsculas.

## 5. SonarQube

```bash
docker compose -f docker-compose.sonarqube.yml up -d
```

Abre `http://localhost:9000` (usuario y clave iniciales `admin`/`admin`), crea un token y analiza:

```bash
./mvnw verify org.sonarsource.scanner.maven:sonar-maven-plugin:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.token=TU_TOKEN
```

En WSL puede hacer falta `sudo sysctl -w vm.max_map_count=262144`.

## 6. Vercel

Vercel hospeda el **frontend Angular**, no el backend Spring. Pasos:

1. Sube el frontend a GitHub con `vercel.json` (incluido en el ZIP del frontend).
2. En vercel.com: Add New, Project, importa el repositorio. Vercel detecta Angular.
3. El backend debe estar en una URL pública, por ejemplo un servidor o el clúster de Kubernetes con Ingress.
4. Dos ajustes en archivos existentes, que requieren tu autorización porque modifican líneas y no solo agregan:
   - `src/app/core/config/api.config.ts`: `API_BASE_URL` apunta a `localhost`.
   - `SecurityConfig` del backend: el CORS solo permite `http://localhost:4200`.

## 7. Distancia por carretera (OpenRouteService)

Las franjas se filtran por el radio de cobertura usando la distancia por carretera que calcula OpenRouteService (ORS). No se geocodifican direcciones: el cliente envía `latitud` y `longitud` en la solicitud y el administrador registra la base de cada técnico (`latitudBase`, `longitudBase`, o `PATCH /api/admin/tecnicos/{id}/ubicacion`). Un técnico sin coordenadas no se ofrece.

| Variable | Dónde se define | Descripción |
|---|---|---|
| `ORS_API_KEY` | `.env`, `docker-compose.yml`, Secret `appointech-secretos` en `k8s/01-configuracion.yaml` | Clave de la API de OpenRouteService. Es opcional: sin ella la app arranca y usa Haversine (línea recta). |

Propiedades de `application.properties` (con valor por defecto):

- `mapas.proveedor`: `ors` o `haversine`.
- `ors.base-url`: `https://api.heigit.org`.
- `ors.matrix-path`: `/openrouteservice/v2/matrix/driving-car`.
- `ors.auth-prefix`: texto antepuesto a la clave en el encabezado `Authorization` (vacío por defecto: la clave va sola, sin `Bearer`).
- `ors.timeout-conexion-ms` (3000) y `ors.timeout-lectura-ms` (5000).

Si ORS falla (timeout, 401, 429, 5xx) o no hay clave, se usa Haversine para todos los destinos y se registra un warning sin la clave. Si ORS no encuentra ruta para un destino, solo ese usa Haversine. La clave nunca debe subirse a GitHub: en Kubernetes crea el Secret real aparte.

Propiedades opcionales de la agenda del técnico (todas con valor por defecto, no hace falta definirlas): `RUTA_MARGEN_MINUTOS` (0), `CONTACTO_ADMIN_TELEFONO` (vacío; teléfono de WhatsApp del administrador de respaldo para clientes fuera de cobertura), `RUTA_TOLERANCIA_MINUTOS` (5, retraso en minutos que aún cuenta como llegar a tiempo; 0 = regla estricta), `HAVERSINE_VELOCIDAD_KMH` (40, solo estima minutos cuando se usa Haversine) y `APP_ZONA_HORARIA` (America/Bogota, define "hoy" y "ahora" en bloques y rutas).

## 8. Fotos de perfil y almacenamiento de objetos (S3)

Todos los usuarios (cliente, técnico y administrador) pueden tener foto de perfil. La imagen llega en base64 y se guarda en un **bucket compatible con S3**; en la base de datos solo queda la clave del objeto (`usuario.foto_clave`, un UUID, migración `V12`). El bucket es un **servicio de respaldo** (factor IV): se reemplaza cambiando variables de entorno, sin tocar código. El backend no guarda archivos en su disco (factor VI) y sirve las fotos él mismo, así que el bucket nunca se expone.

| Variable | Descripción |
|---|---|
| `ALMACENAMIENTO_ENDPOINT` | URL del servicio S3. En Docker Compose, `http://almacenamiento:9000`. Vacío = AWS. |
| `ALMACENAMIENTO_ACCESS_KEY` | Usuario / clave de acceso. Obligatoria en Compose; sin valor por defecto. |
| `ALMACENAMIENTO_SECRET_KEY` | Clave secreta (MinIO exige 8 caracteres o más). Obligatoria en Compose; sin valor por defecto. |
| `ALMACENAMIENTO_BUCKET` | Nombre del bucket (por defecto `fotos-perfil`). El backend lo crea al arrancar si no existe. |
| `ALMACENAMIENTO_REGION` | Región (por defecto `us-east-1`). |

- **Compose:** el servicio `almacenamiento` usa MinIO con etiqueta fija, volumen `almacenamiento-data`, healthcheck, `mem_limit` y el anclaje `x-logging`; el backend espera a que esté `healthy`. La imagen oficial `minio/minio` ya no se publica en Docker Hub ni en Quay, así que se usa `bitnamilegacy/minio` (MinIO real, sin actualizaciones de seguridad). Para producción usa un S3 gestionado o una imagen mantenida: solo cambian las variables.
- **Si el bucket falla (factor IX):** subir o borrar una foto responde `503` y deja un `WARN` de tipo `INTEGRACION` en el registro de eventos (sin endpoint, claves ni imagen). Ver fotos ya cargadas también responde `503`. El resto de la API sigue funcionando; si el bucket no responde al arrancar, la aplicación arranca igual y deja el `WARN`.
- **Tamaño de las peticiones:** la imagen decodificada pesa como máximo 1 MB; en base64 ~1,4 MB. nginx (`nginx/nginx.conf`) sube `client_max_body_size` a `2m` solo en `PUT /api/perfil/foto` y `PUT /api/admin/usuarios/{id}/foto`, y el Ingress de Kubernetes lleva `proxy-body-size: "2m"`. Spring no limita el cuerpo JSON; la API rechaza con 400 cualquier `imagenBase64` de más de 1.500.000 caracteres.
- **Kubernetes:** los manifiestos de `k8s/` no incluyen todavía un bucket ni las variables `ALMACENAMIENTO_*`; hay que apuntarlas a un S3 gestionado (ConfigMap `appointech-config` y Secret `appointech-secretos`).
