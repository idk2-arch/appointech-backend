# Reglas permanentes del proyecto

## Arquitectura hexagonal
- El dominio (`domain/models`, `domain/exception`, `domain/ports`) no importa Spring, JPA, Jackson ni clientes HTTP.
- Puertos de entrada en `domain/ports/in` y de salida en `domain/ports/out`; la lógica va en `application/usecases`; controladores, DTO, repositorios JPA y clientes externos (como OpenRouteService o el microservicio de notificaciones) son adaptadores en `infrastructure`.
- Los controladores solo traducen HTTP a casos de uso; nunca contienen lógica de negocio ni acceden a repositorios directamente.

## SOLID
- **S**: una clase, una responsabilidad.
- **O**: extender por nuevas implementaciones de puertos, no modificando las existentes.
- **L**: toda implementación de un puerto es intercambiable sin cambiar el comportamiento esperado.
- **I**: puertos pequeños y específicos.
- **D**: los casos de uso dependen de puertos (interfaces), nunca de implementaciones concretas ni de frameworks.

## 12 factores
- Configuración (III) por variables de entorno (`application.properties` solo con `${VARIABLE}`); ningún secreto, clave ni contraseña en el código ni en archivos versionados.
- Dependencias (II) declaradas en `pom.xml`; no agregar librerías sin avisar.
- Servicios de apoyo (IV): base de datos, microservicio de notificaciones y OpenRouteService se tratan como recursos externos reemplazables por configuración.
- Procesos sin estado (VI), logs a stdout (XI), paridad dev/prod (X); el build y el Docker (`Dockerfile` y `docker-compose`) siguen funcionando.

## Reglas de trabajo
- No editar migraciones Flyway ya aplicadas (V1 a Vn); los cambios de base de datos van en una migración nueva.
- No cambiar formatos de respuesta existentes: los cambios en DTO son solo aditivos (campos nuevos al final).
- Probar todo: pruebas unitarias para casos de uso y de controlador; correr `mvn test` antes de terminar.
- Nunca hacer commit ni push; nunca crear ramas (el trabajo va en `desarrollo`); no tocar el frontend.
- Nunca imprimir ni escribir claves reales (`JWT_SECRET`, `ORS_API_KEY`, contraseñas).
- Al terminar cada tarea, resumir: archivos tocados, pruebas que pasan y cómo se aplicó hexagonal, SOLID y 12 factores.
