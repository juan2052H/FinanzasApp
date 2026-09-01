# Despliegue

## Variables

Usa `.env.example` como base. En produccion no uses los valores de desarrollo:

- `SPRING_PROFILES_ACTIVE=prod`
- `FINANZAS_DB_URL=jdbc:postgresql://postgres:5432/finanzas`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `FINANZAS_JWT_SECRET`
- `FINANZAS_STORAGE_DIR`
- `FINANZAS_PUBLIC_BASE_URL`
- `FINANZAS_EMAIL_MODE=file|log|disabled`
- `FINANZAS_EMAIL_SINK_DIR`
- `FINANZAS_EMAIL_VERIFICATION_HOURS`
- `FINANZAS_PASSWORD_RESET_MINUTES`
- `GOOGLE_OAUTH_CLIENT_ID`

En dev, los emails de verificacion/reset se escriben como `.eml` en `FINANZAS_EMAIL_SINK_DIR`.

## Docker Compose

```powershell
docker compose up -d postgres backend
docker compose ps
Invoke-WebRequest -Uri http://localhost:8080/health/readiness -UseBasicParsing
```

Con proxy TLS opcional:

```powershell
$env:FINANZAS_DOMAIN="finanzas.example.com"
docker compose --profile tls-proxy up -d
```

## Backend Sin Docker

```powershell
.\mvnw.cmd -f finanzas-backend\pom.xml test
.\mvnw.cmd -f finanzas-backend\pom.xml spring-boot:run
```

Linux/macOS:

```bash
./mvnw -f finanzas-backend/pom.xml test
./mvnw -f finanzas-backend/pom.xml spring-boot:run
```

## Cliente

Modo local:

```powershell
.\mvnw.cmd -f pom.xml exec:java
```

Modo API:

```powershell
$env:FINANZAS_API_ENABLED="true"
$env:FINANZAS_API_BASE_URL="http://localhost:8080"
.\mvnw.cmd -f pom.xml exec:java
```

## CI

`.github/workflows/ci.yml` compila y prueba cliente/backend, verifica OpenAPI, empaqueta JARs, revisa migraciones y construye la imagen Docker del backend.

## Evidencia Local

En este entorno no hay Docker disponible en PATH, por lo que Testcontainers se omitio localmente y Docker Compose no pudo demostrarse aqui. La prueba y la CI quedan preparadas para un runner con Docker.
