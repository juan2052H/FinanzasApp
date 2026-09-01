# ADR 0004: OpenAPI Canonico En Backend

## Estado

Aceptado.

## Contexto

El OpenAPI existe duplicado en backend y raiz del cliente, lo que puede producir deriva.

## Decision

La copia canonica vive en `finanzas-backend/src/main/resources/openapi/finanzas-api.yaml`. La copia raiz se sincroniza con `scripts/sync-openapi.ps1` y la CI compara ambas copias.

## Consecuencias

- El backend manda el contrato.
- El cliente conserva una copia para empaquetado/documentacion.
- La CI falla si las copias divergen.
