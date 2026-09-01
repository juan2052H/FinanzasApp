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
| Perfil backend | Alta | UC-004 | `UserController`, `AuthDtos`, `UserEntity`, `DataManager`, `ConfiguracionPanel` | V5-V6 | `/api/users/me` | Cliente HTTP + backend tests | Editar perfil | Parcial alto | `emailVerified`, ciudad y pais expuestos; perfil backend ya no se guarda solo local |
| Refresh token automatico cliente | Alta | UC-002, UC-028 | `FinanzasApiClient`, `DataManager` | N/A | `/auth/refresh` | Compilacion | Forzar 401 | Parcial | Retry unico tras 401 |
| Categorias buscador/restauracion/color | Alta | UC-016 | `CategoryService`, `CategoryController`, `ConfiguracionPanel`, modelo | N/A | `/categories`, `/restore` | Cliente tests existentes | Buscar cafe/Cafe | Parcial | Falta test API dedicado |
| OpenAPI sin deriva | Alta | UC-023 | OpenAPI backend/cliente, `sync-openapi.ps1`, CI | N/A | Todos | `cmp` en CI | Comparar archivos | Implementado | Rutas nuevas documentadas |
| Reportes PDF/XLSX backend | Media | UC-023 | `ReportController`, `ReportService`, OpenAPI | N/A | `/reports/export.pdf`, `/export.xlsx` | Compilacion | Abrir archivos | Parcial | Generadores minimos |
| Seguridad local serializacion/ZIP | Alta | UC-026 | `PersistenceService` | N/A | N/A | Cliente tests | Restaurar ZIP malicioso pendiente | Parcial | ObjectInputFilter y limites |
| Testcontainers PostgreSQL | Alta | QA | `FlywayPostgresMigrationTest` | V1-V7 | N/A | Omitido sin Docker | Ejecutar en Docker/CI | Parcial | Prueba existe; Docker ausente |
| Workspaces selector activo | Alta | UC-006 | `DataManager`, `HeaderPanel`, `FinanzasApiClient` | N/A | `/workspaces` | `FinanzasApiClientTest` | Cambiar workspace desde header | Implementado cliente | Selector persistido por usuario, crea workspace y sincroniza snapshot |
| Invitaciones Swing completas | Alta | UC-008 | `FinanzasHogarPanel`, `DataManager`, `FinanzasApiClient`, `WorkspaceCollaborationServiceTest` | N/A | `/invitations/*`, `/members` | Cliente HTTP + backend service tests | Invitar/aceptar/rechazar/cancelar/cambiar rol desde UI | Parcial alto | Bandeja interna y roles editables listos; falta E2E real dos clientes |
| Sincronizacion incremental continua | Alta | UC-028 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | Snapshot manual/parcial |
| Email provider/sink | Media | UC-008, UC-003 | `EmailDeliveryService`, `.env.example`, `docker-compose.yml` | N/A | Auth email/reset | `AuthApplicationServiceTest` | Revisar `storage/mail` | Parcial | Sink file/log/disabled; SMTP real pendiente |
| Password reset/verificacion/sesiones | Alta | UC-003 | `AccountTokenService`, `AuthApplicationService`, `AuthController`, `LoginFrame`, `ConfiguracionPanel` | V5 | `/auth/email/verification/*`, `/auth/password/reset/*`, `/auth/password`, `/auth/sessions*` | `AccountTokenServiceTest`, `RefreshTokenServiceTest`, `AuthApplicationServiceTest`, `FinanzasApiClientTest` | Solicitar token desde mail sink y administrar sesiones en ajustes | Implementado | Reset/verificacion/cambio de contrasena y listado/revocacion de sesiones |
| Exportacion/eliminacion de cuenta | Alta | UC-029 | `UserAccountService`, `UserController`, `UserEntity`, `DataManager`, `ConfiguracionPanel` | V7 | `/api/users/me/export`, `DELETE /api/users/me` | `UserAccountServiceTest`, `FinanzasApiClientTest` | Exportar JSON y eliminar cuenta backend desde ajustes | Implementado | Cuenta se anonimiza, revoca sesiones, elimina avatar/preferencias y bloquea owners con workspaces compartidos |
| Tema claro/oscuro/settings | Media | UC-025 | `UserSettingsService`, `UserSettingsEntity`, `ConfiguracionPanel`, `AppColors`, `DataManager` | V6 | `/api/users/me/settings` | `UserSettingsServiceTest`, `DataManagerRegressionTest`, `FinanzasApiClientTest` | Cambiar tema/moneda/region | Parcial alto | Persistencia local/backend y cambio de tema central; quedan colores fijos heredados |
| Facturas/compras | Media | UC-017 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | No implementado |
| Simulador avanzado/calculadora | Media | UC-021, UC-022 | Simulador existente parcial | N/A | N/A | Tests simulacion | Pendiente | Parcial | Calculadora no completa |
| Preparacion DIAN | Media | UC-024 | N/A | N/A | Pendiente | N/A | Pendiente | Pendiente | No se consultaron/implementaron fuentes DIAN |
| Branding completo | Baja | N/A | N/A | N/A | N/A | N/A | Pendiente | Pendiente | No implementado |
| Scheduler recurrencias multiinstancia | Media | UC-020 | Recurrencia manual existente | N/A | `/recurring-transactions/*/run` | Tests dominio | Pendiente | Parcial | Scheduler/locking pendiente |
| Migracion local a backend | Alta | UC-027 | Legacy local existente | N/A | Pendiente | Parcial local | Pendiente | Pendiente | No hay asistente import backend |
| Dos usuarios/dos clientes E2E | Alta | UC-006,008,018,019,028 | Backend parcial | N/A | Varios | N/A | Pendiente | Pendiente | No automatizado |
| No secretos/datos versionados | Alta | N/A | `.gitignore` | N/A | N/A | `git status` | Revisar status final | Implementado | Excluye storage/data/target |
