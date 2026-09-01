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
