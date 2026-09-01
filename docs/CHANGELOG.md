# Changelog

## 2026-08-31

- Se inicializo Git y rama `codex/finalizacion-integral`.
- Se agrego `.gitignore` para secretos, artefactos, datos locales y storage privado.
- Se agrego Maven Wrapper verificable con Maven 3.9.16.
- Se genero linea base en `docs/baseline/`.
- Se agrego Dockerfile multi-stage del backend y Docker Compose con PostgreSQL, backend, healthchecks, storage persistente y perfil TLS opcional.
- Se agrego CI para pruebas, paquetes, OpenAPI, migraciones y build Docker.
- Se agrego `ProblemDetail`, correlation ID, readiness y validacion de secretos debiles en `prod`.
- Se agrego libro mayor de ahorro (`V4__savings_ledger.sql`) con configuracion, movimientos inmutables, idempotencia, retiros y asignacion/liberacion de metas.
- Analytics/reportes backend usan el libro mayor de ahorro.
- Cliente API renueva access token tras 401 con una sola renovacion concurrente.
- Categorias agregan busqueda normalizada, filtros, color visual, restauracion sin cambiar UUID y validacion de color.
- Avatar backend ahora valida contenido real, normaliza PNG cuadrado, usa clave portable, ETag/Last-Modified y permite borrado.
- Cliente API sube/descarga avatar remoto y cachea por usuario/ETag.
- Persistencia local agrega `ObjectInputFilter` y limites de restauracion ZIP.
- OpenAPI se actualizo y se sincronizo entre backend y cliente.
