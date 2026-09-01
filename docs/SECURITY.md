# Seguridad

## Implementado

- JWT HMAC con secreto externo y validacion de seguridad en perfil `prod`.
- Refresh tokens persistidos como hash y rotados al renovar.
- Rate limiting local en login backend por correo.
- `@RestControllerAdvice` con `ProblemDetail` y codigos de error estables.
- Correlation ID por request (`X-Correlation-ID`) y MDC.
- Validacion de membresia por workspace en endpoints financieros existentes.
- Avatares autenticados, con validacion de extension, MIME declarado, formato real via `ImageIO`, dimensiones maximas, limite de 5 MB y clave portable.
- Persistencia local con `ObjectInputFilter` para limitar clases permitidas al leer `app-state.bin`.
- Restauracion ZIP con proteccion de path traversal, limite de entradas y limite de bytes por entrada/imagen.
- Verificacion de correo y recuperacion de contrasena con tokens de un solo uso almacenados como hash.
- Cambio de contrasena autenticado con validacion de password actual y revocacion de refresh tokens.
- Listado y revocacion de sesiones activas por usuario.
- Exportacion de cuenta autenticada y eliminacion anonimizada con confirmacion de email/password, borrado de avatar/preferencias y bloqueo cuando la cuenta aun posee workspaces compartidos.
- `.gitignore` excluye secretos, datos locales, respaldos reales, storage, target/out/dist y logs.

## Pendiente

- Rate limiting distribuido para despliegues multiinstancia.
- Pruebas IDOR amplias por todos los endpoints y roles.
- Pruebas automatizadas de logs sin datos sensibles.
- Proveedor SMTP/transaccional real y plantillas HTML firmadas.
- Migracion completa desde serializacion Java a persistencia local no ejecutable.
- Analisis de vulnerabilidades de dependencias en CI.
