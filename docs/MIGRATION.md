# Migracion

## Flyway

No se modificaron migraciones `V1` a `V3`. Se agrego:

- `V4__savings_ledger.sql`: configuracion y libro mayor inmutable de ahorro.
- `V5__auth_lifecycle_tokens.sql`: verificacion de correo y recuperacion de contrasena.
- `V6__profile_settings.sql`: ciudad, pais y preferencias versionadas en `user_settings`.

Validacion local:

```powershell
.\mvnw.cmd -f finanzas-backend\pom.xml test
```

La clase `FlywayPostgresMigrationTest` usa Testcontainers y migra una base PostgreSQL real desde V3 hasta V6 cuando Docker esta disponible.

## Estado Local Legacy

`LegacyStateMigrationService` conserva compatibilidad con `data/app-state.bin`, crea respaldo antes de migrar y mueve el estado al directorio estable configurado por `FINANZAS_DATA_DIR` o `~/.finanzasapp`.

## Backfill De Ahorro

No se ejecuto backfill automatico de ahorro historico. La decision fue no inventar movimientos de ahorro sobre ingresos pasados sin una previsualizacion explicita del usuario. El backend nuevo empieza a registrar movimientos automaticos desde ingresos creados/editados despues de `V4`.

Pendiente: asistente de backfill idempotente con dry-run, resumen, claves de importacion y reporte por registro.
