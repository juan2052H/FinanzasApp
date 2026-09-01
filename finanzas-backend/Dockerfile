FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn .mvn
COPY mvnw mvnw
COPY finanzas-backend/pom.xml finanzas-backend/pom.xml
RUN chmod +x mvnw
RUN ./mvnw -f finanzas-backend/pom.xml -B -DskipTests dependency:go-offline

COPY finanzas-backend finanzas-backend
RUN ./mvnw -f finanzas-backend/pom.xml -B -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system finanzas \
    && useradd --system --gid finanzas --home-dir /app finanzas \
    && mkdir -p /app/storage \
    && chown -R finanzas:finanzas /app

COPY --from=build /workspace/finanzas-backend/target/finanzas-backend-*.jar /app/app.jar

USER finanzas
EXPOSE 8080
HEALTHCHECK --interval=20s --timeout=5s --retries=10 CMD curl -fsS http://localhost:8080/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
