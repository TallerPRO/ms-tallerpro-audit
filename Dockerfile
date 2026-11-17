# syntax=docker/dockerfile:1
# ms-tallerpro-audit - Spring Boot 3.3.4 / Java 17 (pom: <java.version>17</java.version>).
# Build multi-stage: el codigo fuente y Maven quedan en la etapa "build"; la imagen final
# solo contiene la JRE y el jar. Ningun secreto ni .env entra en la imagen (ver .dockerignore).

# ---- Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Usuario sin privilegios; el jar queda propiedad de root (solo lectura para la app).
RUN addgroup -S spring && adduser -S -G spring -H -s /sbin/nologin spring
COPY --from=build /app/target/ms-tallerpro-audit-*.jar /app/app.jar
ENV SERVER_PORT=8086 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
USER spring:spring
EXPOSE 8086
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
