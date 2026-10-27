# Etapa 1: construccion
FROM maven:3.9-eclipse-temurin-21 AS construccion
WORKDIR /app
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Etapa 2: ejecucion (imagen liviana, usuario sin privilegios)
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home appointech
COPY --from=construccion /app/target/*.jar app.jar
USER appointech
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 CMD bash -c 'exec 3<>/dev/tcp/localhost/8081' || exit 1
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75","-jar","app.jar"]
