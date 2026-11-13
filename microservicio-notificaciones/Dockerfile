# syntax=docker/dockerfile:1
# Etapa 1: construccion
FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /app
COPY pom.xml .
RUN --mount=type=cache,id=appointech-maven,target=/root/.m2,sharing=locked \
    intento=1; \
    until mvn -B -q -U -Dmaven.wagon.http.retryHandler.count=5 -Daether.connector.http.retryHandler.count=5 -Daether.connector.http.retryHandler.serviceUnavailable=429,503 dependency:go-offline; do \
        if [ "$intento" -ge 5 ]; then \
            echo "Maven fallo tras $intento intentos." >&2; \
            exit 1; \
        fi; \
        espera=$((intento * 10)); \
        echo "Maven fallo (intento $intento/5); reintentando en ${espera}s..." >&2; \
        sleep "$espera"; \
        intento=$((intento + 1)); \
    done
COPY src ./src
RUN --mount=type=cache,id=appointech-maven,target=/root/.m2,sharing=locked \
    intento=1; \
    until mvn -B -q -U -Dmaven.wagon.http.retryHandler.count=5 -Daether.connector.http.retryHandler.count=5 -Daether.connector.http.retryHandler.serviceUnavailable=429,503 -DskipTests package; do \
        if [ "$intento" -ge 5 ]; then \
            echo "Maven fallo tras $intento intentos." >&2; \
            exit 1; \
        fi; \
        espera=$((intento * 10)); \
        echo "Maven fallo (intento $intento/5); reintentando en ${espera}s..." >&2; \
        sleep "$espera"; \
        intento=$((intento + 1)); \
    done

# Etapa 2: ejecucion (imagen liviana, usuario sin privilegios)
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home appointech
COPY --from=construccion /app/target/*.jar app.jar
USER appointech
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD bash -c 'exec 3<>/dev/tcp/localhost/8081' || exit 1
ENTRYPOINT ["java","-jar","app.jar"]
