# Backup Y Restauracion

## Modo Local

El estado local vive en:

```text
~/.finanzasapp/app-state.bin
~/.finanzasapp/profile-images/
```

Tambien puede cambiarse con:

```powershell
$env:FINANZAS_DATA_DIR="C:\ruta\segura\finanzas-data"
```

La app soporta respaldo/restauracion ZIP desde `PersistenceService`. La restauracion valida rutas dentro de `profile-images/`, limita cantidad de entradas y limita bytes por entrada para reducir riesgo de ZIP slip/ZIP bomb.

## Base De Datos

Ejemplo de respaldo PostgreSQL:

```powershell
docker compose exec postgres pg_dump -U finanzas -d finanzas -Fc -f /tmp/finanzas.dump
docker compose cp postgres:/tmp/finanzas.dump .\backups\finanzas.dump
```

Ejemplo de restauracion:

```powershell
docker compose cp .\backups\finanzas.dump postgres:/tmp/finanzas.dump
docker compose exec postgres pg_restore -U finanzas -d finanzas --clean --if-exists /tmp/finanzas.dump
```

## Archivos

Los avatares remotos se guardan bajo `FINANZAS_STORAGE_DIR` y Docker Compose lo monta en el volumen `finanzas-backend-storage`.
