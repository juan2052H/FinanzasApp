# API

El contrato canonico esta en:

- `finanzas-backend/src/main/resources/openapi/finanzas-api.yaml`

La copia consumible por el cliente se sincroniza con:

```powershell
.\scripts\sync-openapi.ps1
```

La CI verifica que ambas copias sean iguales con `cmp`.

## Endpoints Cambiados O Agregados

- `GET /api/users/me/avatar`: devuelve PNG autenticado con `ETag` y `Last-Modified`.
- `POST /api/users/me/avatar`: sube multipart, valida PNG/JPEG real y normaliza a PNG cuadrado.
- `DELETE /api/users/me/avatar`: elimina avatar remoto.
- `POST /api/auth/email/verification/request`: solicita email de verificacion sin revelar si la cuenta existe.
- `POST /api/auth/email/verification/confirm`: confirma correo con token de un solo uso.
- `POST /api/auth/password/reset/request`: solicita recuperacion de contrasena sin enumerar cuentas.
- `POST /api/auth/password/reset/confirm`: actualiza contrasena y revoca refresh tokens activos.
- `GET /api/users/me`: devuelve perfil con ciudad, pais, moneda, locale y estado de verificacion.
- `PATCH /api/users/me`: actualiza perfil en backend; si cambia correo reinicia la verificacion.
- `GET /api/users/me/settings`: devuelve tema, locale, zona horaria, formato monetario y preferencias de notificacion.
- `PATCH /api/users/me/settings`: persiste preferencias del usuario con validacion de locale/zona/formato.
- `GET /api/workspaces/{workspaceId}/categories?type=&includeArchived=&q=`: busqueda/filtros.
- `POST /api/workspaces/{workspaceId}/categories/{categoryId}/restore`: restaura sin cambiar UUID.
- `GET/PUT /api/workspaces/{workspaceId}/savings/config`.
- `GET /api/workspaces/{workspaceId}/savings/summary`.
- `GET /api/workspaces/{workspaceId}/savings/movements`.
- `POST /api/workspaces/{workspaceId}/savings/deposits`.
- `POST /api/workspaces/{workspaceId}/savings/withdrawals`.
- `POST /api/workspaces/{workspaceId}/savings/goals/{goalId}/allocations`.
- `POST /api/workspaces/{workspaceId}/savings/goals/{goalId}/releases`.
- `GET /api/workspaces/{workspaceId}/savings/income-impact`.
- `GET /api/workspaces/{workspaceId}/reports/export.pdf`.
- `GET /api/workspaces/{workspaceId}/reports/export.xlsx`.
- `GET /health/readiness`.

## Errores

Los errores REST usan `ProblemDetail` con `errorCode`, `correlationId` y, cuando aplica, errores por campo:

```json
{
  "type": "https://finanzasapp.local/problems/validation-error",
  "title": "VALIDATION_ERROR",
  "status": 422,
  "detail": "La solicitud tiene campos invalidos.",
  "errorCode": "VALIDATION_ERROR",
  "correlationId": "..."
}
```

## Notas De Contrato

- Se retiraron del OpenAPI rutas de analytics que no existen en controladores (`cash-flow`, `categories`, `health`).
- PDF/XLSX estan expuestos por backend, pero el generador actual sigue siendo minimo. La sustitucion por PDFBox/OpenPDF y Apache POI queda en limitaciones.
- En dev/test los emails se entregan a `FINANZAS_EMAIL_SINK_DIR` cuando `FINANZAS_EMAIL_MODE=file`.
- `user_settings` se expone por API desde V6; la copia OpenAPI del cliente se sincroniza desde el YAML del backend.
