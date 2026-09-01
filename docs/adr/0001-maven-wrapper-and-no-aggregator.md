# ADR 0001: Maven Wrapper Sin POM Agregador

## Estado

Aceptado.

## Contexto

El proyecto tiene un cliente Swing en la raiz y un backend Spring Boot en `finanzas-backend`. El empaquetado desktop ya depende del POM raiz.

## Decision

Agregar Maven Wrapper verificable en la raiz y conservar POMs separados. No se agrega POM agregador por ahora para evitar cambiar el ciclo de empaquetado del cliente.

## Consecuencias

- Los comandos requeridos funcionan con `./mvnw -f pom.xml test` y `./mvnw -f finanzas-backend/pom.xml test`.
- CI ejecuta ambos modulos de forma explicita.
- Un agregador puede agregarse despues si se valida el empaquetado desktop.
