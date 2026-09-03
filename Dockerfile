# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# Resolve dependencies first so they are cached unless pom.xml changes
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp dependency:go-offline

# Build the application
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# curl is used by the container healthcheck
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring spring

COPY --from=build /build/target/*.jar app.jar

USER spring:spring
EXPOSE 8080

# MaxRAMPercentage lets the JVM size the heap from the container memory limit
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
