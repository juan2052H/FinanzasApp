# ADR 0002: Libro Mayor Inmutable De Ahorro

## Estado

Aceptado.

## Contexto

El calculo anterior trataba `ingresos - gastos` como ahorro. Eso mezclaba saldo disponible con ahorro real y rompia retiros, metas y reportes.

## Decision

Crear `savings_config` y `savings_movements` desde `V4`. El ahorro se calcula como movimientos `CREDIT - DEBIT`. Las correcciones se hacen con nuevos movimientos, no editando historial.

## Consecuencias

- Crear ingresos genera `AUTO_ALLOCATION`.
- Editar ingresos crea delta o reversa.
- Eliminar ingresos revierte exactamente el saldo automatico asociado.
- Retiros y metas no crean dinero.
- Backfill historico queda como operacion explicita con dry-run pendiente.
