# Matriz De Trazabilidad

| Requisito | Prioridad | Caso de uso | Componentes modificados | Migracion | Endpoint | Prueba automatizada | Prueba manual | Estado | Evidencia |
|---|---|---|---|---|---|---|---|---|---|
| Inventario inicial | Alta | N/A | `scripts/generate-inventory.ps1` | N/A | N/A | Script ejecutado | Revisar `docs/baseline/inventory.md` | Implementado | `docs/baseline/inventory.md` |
| Linea base de pruebas | Alta | N/A | `docs/baseline` | N/A | N/A | Maven wrapper | Revisar logs baseline | Implementado | `client-test.txt`, `backend-test.txt` |
| Git, rama y `.gitignore` | Alta | N/A | `.gitignore` | N/A | N/A | `git status` | Revisar ignorados | Implementado | Rama `codex/finalizacion-integral` |
| Maven Wrapper verificable | Alta | N/A | `.mvn`, `mvnw`, `mvnw.cmd` | N/A | N/A | `mvnw test` | `mvnw -version` | Implementado | Maven 3.9.16 |
| POM agregador evaluado | Media | N/A | ADR | N/A | N/A | N/A | Leer ADR | Implementado como decision | `ADR-0001` |
| Dockerfile backend | Alta | N/A | `finanzas-backend/Dockerfile` | N/A | `/health/readiness` | CI build | Docker local pendiente | Parcial | Docker no disponible localmente |
| Docker Compose integral | Alta | N/A | `docker-compose.yml`, `deploy/Caddyfile` | N/A | Backend/Postgres | CI build image | Compose pendiente | Parcial | Config listo, no ejecutado local |
| Perfiles dev/test/prod | Alta | N/A | `application.yml` | N/A | N/A | Backend tests | Arranque prod pendiente | Parcial | Validador prod agregado |
| CI | Alta | N/A | `.github/workflows/ci.yml` | N/A | N/A | Workflow | Runner externo | Implementado | Compila/test/package/OpenAPI/Docker |
| ProblemDetail/correlation ID | Alta | Todos API | `ApiExceptionHandler`, `CorrelationIdFilter` | N/A | Todos | Compilacion | Probar error 422 | Parcial | Falta suite contrato-controlador |
| Health/readiness real | Alta | Despliegue | `HealthController` | N/A | `/health/readiness` | Compilacion | `Invoke-WebRequest` con DB | Parcial | Endpoint agregado |
| Ahorro libro mayor | Maxima | UC-009..014 | `SavingsService`, entidades/repos/controlador | V4 | `/savings/*` | `SavingsServiceTest` | Crear ingresos/retiros | Implementado backend | SAV-001..007,009 y redondeo |
| SAV-008 concurrencia | Maxima | UC-013 | `SavingsService` | V4 | `/withdrawals` | No hay prueba concurrente real | Pendiente | Pendiente | Requiere test transaccional DB |
| SAV-010 analytics/reportes | Maxima | UC-023 | `AnalyticsService`, `ReportService`, `DataManager` | V4 | `/analytics/summary`, `/reports/summary` | Compilacion/tests ahorro | Revisar resumen | Parcial | Backend y dashboard API usan libro mayor |
| Avatar remoto completo | Maxima | UC-004 | `AvatarStorageService`, `UserController`, API client, cache local | N/A | `GET/POST/DELETE /api/users/me/avatar` | `AvatarStorageServiceTest` | Subir desde dos clientes | Parcial alto | Falta E2E dos clientes/cache 304 |
| Perfil backend | Alta | UC-004 | `UserController`, `AuthDtos` | N/A | `/api/users/me` | Compilacion | Editar perfil | Parcial | Ciudad/pais/settings pendientes |
| Refresh token automatico cliente | Alta | UC-002, UC-028 | `FinanzasApiClient`, `DataManager` | N/A | `/auth/refresh` | Compilacion | Forzar 401 | Parcial | Retry unico tras 401 |
| Categorias buscador/restauracion/color | Alta | UC-016 | `CategoryService`, `CategoryController`, `ConfiguracionPanel`, modelo | N/A | `/categories`, `/restore` | Cliente tests existentes | Buscar cafe/Cafe | Parcial | Falta test API dedicado |
| OpenAPI sin deriva | Alta | UC-023 | OpenAPI backend/cliente, `sync-openapi.ps1`, CI | N/A | Todos | `cmp` en CI | Comparar archivos | Implementado | Rutas nuevas documentadas |
| Reportes PDF/XLSX backend | Media | UC-023 | `ReportController`, `ReportService`, OpenAPI | N/A | `/reports/export.pdf`, `/export.xlsx` | Compilacion | Abrir archivos | Parcial | Generadores minimos |
| Seguridad local serializacion/ZIP | Alta | UC-026 | `PersistenceService` | N/A | N/A | Cliente tests | Restaurar ZIP malicioso pendiente | Parcial | ObjectInputFilter y limites |
| Testcontainers PostgreSQL | Alta | QA | `FlywayPostgresMigrationTest` | V1-V4 | N/A | Omitido sin Docker | Ejecutar en Docker/CI | Parcial | Prueba existe; Docker ausente |
| Workspaces selector activo | Alta | UC-006 | N/A | N/A | `/workspaces` | N/A | Pendiente | Pendiente | Sigue primer workspace |
| Invitaciones Swing completas | Alta | UC-008 | Backend existente; UI parcial | N/A | `/invitations/*` | N/A | Pendiente | Pendiente | Falta bandeja/aceptar/rechazar |
| Sincronizacion incremental continua | Alta | UC-028 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | Snapshot manual/parcial |
| Email provider/sink | Media | UC-008, UC-003 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | Invitacion interna aun posible por API |
| Password reset/verificacion/sesiones | Alta | UC-003 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | No implementado |
| Tema claro/oscuro/settings | Media | UC-025 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | `user_settings` no conectado |
| Facturas/compras | Media | UC-017 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | No implementado |
| Simulador avanzado/calculadora | Media | UC-021, UC-022 | Simulador existente parcial | N/A | N/A | Tests simulacion | Pendiente | Parcial | Calculadora no completa |
| Preparacion DIAN | Media | UC-024 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | No se consultaron/implementaron fuentes DIAN |
| Branding completo | Baja | N/A | N/A | N/A | N/A | N/A | Pendiente | Pendiente | No implementado |
| Scheduler recurrencias multiinstancia | Media | UC-020 | Recurrencia manual existente | N/A | `/recurring-transactions/*/run` | Tests dominio | Pendiente | Parcial | Scheduler/locking pendiente |
| Migracion local a backend | Alta | UC-027 | Legacy local existente | N/A | Pendiente | Parcial local | Pendiente | Pendiente | No hay asistente import backend |
| Dos usuarios/dos clientes E2E | Alta | UC-006,008,018,019,028 | Backend parcial | N/A | Varios | N/A | Pendiente | Pendiente | No automatizado |
| No secretos/datos versionados | Alta | N/A | `.gitignore` | N/A | N/A | `git status` | Revisar status final | Implementado | Excluye storage/data/target |
