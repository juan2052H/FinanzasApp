# ADR 0003: Avatar Portable Con Endpoint Autenticado

## Estado

Aceptado.

## Contexto

El backend exponia rutas fisicas y el cliente intentaba leerlas como rutas locales, lo que fallaba entre equipos y filtraba detalles del servidor.

## Decision

El backend guarda avatares bajo `FINANZAS_STORAGE_DIR/avatars/{userId}/avatar.png`, devuelve `avatars/{userId}/avatar.png` como referencia portable y expone lectura/borrado autenticados en `/api/users/me/avatar`.

## Consecuencias

- La subida valida extension, MIME declarado, formato real y dimensiones.
- La imagen se recorta/escala a PNG cuadrado.
- `GET` devuelve `ETag` y `Last-Modified`.
- El cliente cachea una copia local por usuario/ETag y muestra iniciales si falla.
