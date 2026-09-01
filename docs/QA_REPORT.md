# Informe QA

## Comandos Ejecutados

- `.\mvnw.cmd -version`: Maven Wrapper operativo con Apache Maven 3.9.16.
- `.\mvnw.cmd -f pom.xml test`: cliente Swing/API, 20 pruebas, 0 fallos.
- `.\mvnw.cmd -f finanzas-backend\pom.xml test`: backend, 46 pruebas, 0 fallos, 1 omitida por falta de Docker local.
- `.\mvnw.cmd -f pom.xml -DskipTests package`: empaquetado cliente, `BUILD SUCCESS`.
- `.\mvnw.cmd -f finanzas-backend\pom.xml -DskipTests package`: empaquetado backend, `BUILD SUCCESS`.
- `.\scripts\generate-inventory.ps1`: inventario de modulos/endpoints/tablas/pruebas.
- `docker --version` y `docker compose version`: Docker no esta instalado/no esta en PATH.

## Evidencia Automatizada

- Ahorro: `SavingsServiceTest` cubre SAV-001 a SAV-007, SAV-009 y redondeo de centavos.
- Avatar: `AvatarStorageServiceTest` cubre PNG, JPEG, GIF renombrado, corrupto, tamano excedido, dimensiones extremas, reemplazo, borrado e aislamiento.
- Flyway/PostgreSQL: `FlywayPostgresMigrationTest` prueba V3 -> V7 con Testcontainers cuando Docker esta disponible.
- Colaboracion: `WorkspaceCollaborationServiceTest` cubre invitar, aceptar, rechazo por correo ajeno y cambio de rol.
- Ownership y salida: `WorkspaceCollaborationServiceTest` cubre transferencia de OWNER, bloqueo de salida de OWNER compartido y salida de miembro no OWNER.
- Liquidaciones: `SharedExpenseServiceTest` cubre que una liquidacion no elimina miembros y permite crear otro gasto con los mismos usuarios, aun con nombres duplicados.
- Auth lifecycle: `AccountTokenServiceTest` y `AuthApplicationServiceTest` cubren tokens hasheados, expiracion, verificacion de correo y reset con revocacion de refresh tokens.
- Seguridad de cuenta: `RefreshTokenServiceTest`, `AuthApplicationServiceTest`, `UserAccountServiceTest` y `FinanzasApiClientTest` cubren cambio de contrasena, sesiones activas, revocacion, exportacion y eliminacion anonimizada.
- Settings: `UserSettingsServiceTest`, `DataManagerRegressionTest` y `FinanzasApiClientTest` cubren persistencia local/backend, tema, locale, zona horaria, formato monetario y notificaciones.
- Cliente: regresiones de persistencia, avatar local, precision monetaria, categorias, login local y rutas HTTP de workspace/invitaciones/settings.

## Riesgos Abiertos

- No se pudo ejecutar Docker Compose ni Testcontainers localmente porque Docker no esta disponible en PATH.
- La prueba Testcontainers quedo omitida localmente; debe ejecutarse en CI/maquina con Docker.
- Exportaciones PDF/XLSX backend existen, pero aun son generadores minimos.
- No hay suite Swing E2E con AssertJ Swing/Xvfb.
- No hay E2E real de dos clientes aceptando invitacion y sincronizando.

## Resultado

La compilacion y pruebas unitarias locales estan verdes. La validacion de contenedores queda preparada pero no demostrada en este equipo.
