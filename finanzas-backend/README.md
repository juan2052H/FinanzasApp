# finanzas-backend

Backend REST Spring Boot para FinanzasApp.

## Requisitos

- Java 21.
- PostgreSQL 16.
- Maven Wrapper desde la raiz del repo.

## Ejecutar

Desde la raiz:

```powershell
docker compose up -d postgres
.\mvnw.cmd -f finanzas-backend\pom.xml spring-boot:run
```

Con Compose completo:

```powershell
docker compose up -d postgres backend
Invoke-WebRequest -Uri http://localhost:8080/health/readiness -UseBasicParsing
```

## Pruebas

```powershell
.\mvnw.cmd -f finanzas-backend\pom.xml test
```

Con Docker disponible, `FlywayPostgresMigrationTest` valida migracion PostgreSQL real desde V3 hasta V5 mediante Testcontainers.

## API Principal

Contrato canonico: `src/main/resources/openapi/finanzas-api.yaml`.

- Auth: `/api/auth/register`, `/login`, `/refresh`, `/logout`, `/google`, verificacion de correo y reset de contrasena.
- Perfil: `/api/users/me`, `/api/users/me/avatar`.
- Workspaces/miembros/invitaciones.
- Categorias con filtros/restauracion.
- Transacciones, presupuestos, metas y recurrencias.
- Libro mayor de ahorro en `/api/workspaces/{workspaceId}/savings/*`.
- Hogar compartido, splits y liquidaciones.
- Notificaciones, auditoria, reportes CSV/PDF/XLSX y analytics summary.

## Seguridad

- Password hashing con BCrypt.
- JWT con secreto externo.
- Refresh tokens hasheados y rotados.
- Tokens de verificacion/reset hasheados, con TTL y consumo unico.
- Mail sink local para verificacion/reset en `FINANZAS_EMAIL_SINK_DIR`.
- Validacion de secretos debiles en `prod`.
- `ProblemDetail` + `correlationId`.
- Readiness con consulta real a DB.
- Avatar sin rutas fisicas expuestas y validacion real de imagen.

## Produccion

No uses valores por defecto. Define `SPRING_PROFILES_ACTIVE=prod`, `FINANZAS_JWT_SECRET` fuerte, password de DB fuerte y storage persistente.
