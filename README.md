# FinanzasApp

Aplicacion Java Swing para finanzas personales y de hogar, con dos modos:

- **Local**: funciona sin backend y guarda estado en `FINANZAS_DATA_DIR` o `~/.finanzasapp`.
- **API**: usa `finanzas-backend` como fuente de verdad con Spring Boot, JWT, PostgreSQL y Flyway.

## Requisitos

- Cliente: Java 11+.
- Backend: Java 21.
- Maven: no hace falta instalarlo; usa `mvnw`/`mvnw.cmd`.
- Docker: requerido para Compose y Testcontainers.

## Configuracion

Parte de `.env.example` y define valores reales solo en tu entorno:

```powershell
$env:FINANZAS_API_ENABLED="true"
$env:FINANZAS_API_BASE_URL="http://localhost:8080"
$env:FINANZAS_DATA_DIR="$HOME\.finanzasapp"
```

Variables principales:

- `FINANZAS_API_ENABLED`: activa modo API en el cliente.
- `FINANZAS_API_BASE_URL`: URL base del backend.
- `FINANZAS_DATA_DIR`: estado/caches locales.
- `FINANZAS_DB_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: base PostgreSQL.
- `FINANZAS_JWT_SECRET`: secreto fuerte para JWT.
- `FINANZAS_STORAGE_DIR`: storage backend para avatares/archivos.
- `GOOGLE_OAUTH_CLIENT_ID`: OAuth real, opcional.

## Comandos

Windows:

```powershell
.\mvnw.cmd -f pom.xml test
.\mvnw.cmd -f finanzas-backend\pom.xml test
docker compose up -d postgres backend
.\mvnw.cmd -f pom.xml exec:java
```

Linux/macOS:

```bash
./mvnw -f pom.xml test
./mvnw -f finanzas-backend/pom.xml test
docker compose up -d postgres backend
./mvnw -f pom.xml exec:java
```

Modo API del cliente:

```powershell
$env:FINANZAS_API_ENABLED="true"
$env:FINANZAS_API_BASE_URL="http://localhost:8080"
.\mvnw.cmd -f pom.xml exec:java
```

## Funciones Implementadas

- Registro/login local y backend.
- Refresh token backend con renovacion automatica del cliente tras 401.
- Workspaces, miembros, invitaciones backend, categorias, transacciones, presupuestos, metas, recurrencias, hogar compartido, notificaciones y reportes.
- Libro mayor de ahorro backend con porcentaje configurable, asignacion automatica por ingreso, retiros, depositos manuales y asignacion/liberacion de metas.
- Dashboard en modo API lee ahorro desde el libro mayor.
- Avatar remoto autenticado: subida multipart, validacion real PNG/JPEG, normalizacion a PNG cuadrado, `GET` con `ETag`, cache local y borrado.
- Categorias con busqueda visible, filtros, selector de color, validacion y restauracion sin duplicar UUID.
- Persistencia local con filtro de deserializacion y restauracion ZIP con limites.
- Dockerfile backend, Compose, CI y OpenAPI sincronizado.

## Documentacion

- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/SECURITY.md`
- `docs/DEPLOYMENT.md`
- `docs/MIGRATION.md`
- `docs/BACKUP_RESTORE.md`
- `docs/USE_CASES.md`
- `docs/TRACEABILITY_MATRIX.md`
- `docs/QA_REPORT.md`
- `docs/KNOWN_LIMITATIONS.md`
- `docs/CHANGELOG.md`
- `docs/adr/`

## Estado

Las pruebas unitarias locales pasan. Docker/Testcontainers no se pudieron demostrar en esta maquina porque Docker no esta disponible; la prueba y la CI estan preparadas para ejecutarse en un runner con Docker.
