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
- Cliente API agrega creacion de workspaces y flujo de invitaciones internas.
- Header Swing agrega selector persistente de workspace activo y creacion de workspace.
- Panel de hogar agrega bandeja de invitaciones recibidas/enviadas con aceptar, rechazar, cancelar y refrescar.
- Panel de hogar permite cambiar roles de miembros segun permisos OWNER/ADMIN.
- Panel de hogar usa `MemberOption` con UUID/email/rol para combos backend, evitando colisiones por nombres duplicados.
- Backend agrega transferencia de OWNER y salida segura de workspace; OWNER compartido debe transferir antes de abandonar.
- Liquidaciones backend tienen regresion que confirma que los miembros siguen disponibles para nuevos gastos.
- Backend agrega tokens de verificacion de correo y recuperacion de contrasena con hash, TTL, mail sink local y revocacion de refresh tokens tras reset.
- Login Swing permite solicitar recuperacion/verificacion y confirmar tokens del backend.
- Perfil backend ahora incluye ciudad y pais; las preferencias de usuario usan `user_settings` con tema, locale, zona horaria, formato monetario y notificaciones.
- Configuracion Swing permite cambiar tema claro/oscuro/sistema y guarda moneda/region/preferencias en local o backend.
- Seguridad de cuenta agrega cambio de contrasena autenticado, listado/revocacion de sesiones, exportacion JSON de cuenta y eliminacion anonimizada con confirmacion.
- Flyway agrega `V7__account_deletion.sql` con `users.deleted_at`.
- Persistencia local agrega `ObjectInputFilter` y limites de restauracion ZIP.
- OpenAPI se actualizo y se sincronizo entre backend y cliente.
